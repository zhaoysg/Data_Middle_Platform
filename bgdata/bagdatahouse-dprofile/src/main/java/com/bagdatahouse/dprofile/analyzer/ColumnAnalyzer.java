package com.bagdatahouse.dprofile.analyzer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.core.entity.DprofileColumnStats;
import com.bagdatahouse.core.entity.DprofileTableStats;
import com.bagdatahouse.core.mapper.DprofileColumnStatsMapper;
import com.bagdatahouse.core.mapper.DprofileTableStatsMapper;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import com.bagdatahouse.dprofile.analyzer.DistributionAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 列级探查分析器
 * 负责对目标表的所有列执行统计信息采集，包括：
 * - 空值统计（总数、空值数、空值率）
 * - 唯一性统计（唯一值数量、唯一率）
 * - 数值统计（最小值、最大值、平均值、中位数、标准差）
 * - 字符串统计（最小/最大/平均长度）
 * - 分布统计（Top-N 值、直方图）
 * - 异常检测（零值率、负值率、异常值数量）
 */
@Slf4j
@Component
public class ColumnAnalyzer {

    @Autowired
    private DataSourceAdapterRegistry adapterRegistry;

    @Autowired
    private DprofileTableStatsMapper tableStatsMapper;

    @Autowired
    private DprofileColumnStatsMapper columnStatsMapper;

    @Autowired
    private DistributionAnalyzer distributionAnalyzer;

    // ==================== 对外暴露的查询方法 ====================

    /**
     * 根据表统计ID获取列级统计记录
     */
    public List<DprofileColumnStats> getColumnStatsByTableStatsId(Long tableStatsId) {
        if (tableStatsId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DprofileColumnStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DprofileColumnStats::getTableStatsId, tableStatsId)
                .orderByAsc(DprofileColumnStats::getColumnName);
        return columnStatsMapper.selectList(wrapper);
    }

    /**
     * 执行列级探查（针对指定列或全部列）
     *
     * @param taskId        探查任务ID
     * @param dsId          数据源ID
     * @param tableName     目标表名
     * @param executionId   执行ID（关联表级探查）
     * @param tableStatsId  表统计记录ID
     * @param columnNames   指定列名（null 表示全部列）
     * @param profileTime   探查时间
     * @return 采集的列统计记录列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<DprofileColumnStats> analyzeColumns(
            Long taskId,
            Long dsId,
            String tableName,
            Long executionId,
            Long tableStatsId,
            List<String> columnNames,
            LocalDateTime profileTime) {
        return analyzeColumns(taskId, dsId, tableName, null, executionId, tableStatsId, columnNames, profileTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<DprofileColumnStats> analyzeColumns(
            Long taskId,
            Long dsId,
            String tableName,
            String schema,
            Long executionId,
            Long tableStatsId,
            List<String> columnNames,
            LocalDateTime profileTime) {

        if (dsId == null) {
            throw new BusinessException(400, "数据源ID不能为空");
        }
        if (!StringUtils.hasText(tableName)) {
            throw new BusinessException(400, "目标表名不能为空");
        }

        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            throw new BusinessException(400, "数据源适配器未找到，dsId=" + dsId);
        }

        // 获取表的总行数（PostgreSQL 使用 schema-aware 重载）
        long totalRows;
        if (schema != null && adapter instanceof com.bagdatahouse.datasource.adapter.PostgresAdapter) {
            totalRows = ((com.bagdatahouse.datasource.adapter.PostgresAdapter) adapter).getRowCount(schema, tableName);
        } else {
            totalRows = adapter.getRowCount(tableName);
        }
        if (totalRows == 0) {
            log.warn("表为空，跳过列级探查: {}.{}", dsId, tableName);
            return Collections.emptyList();
        }

        // 获取列信息（PostgreSQL 使用 schema-aware 重载）
        List<DataSourceAdapter.ColumnInfo> allColumns;
        if (schema != null) {
            allColumns = adapter.getColumns(schema, tableName);
        } else {
            allColumns = adapter.getColumns(tableName);
        }
        List<DataSourceAdapter.ColumnInfo> targetColumns;
        if (columnNames == null || columnNames.isEmpty()) {
            targetColumns = allColumns;
        } else {
            Set<String> nameSet = new HashSet<>(columnNames);
            targetColumns = new ArrayList<>();
            for (DataSourceAdapter.ColumnInfo col : allColumns) {
                if (nameSet.contains(col.columnName())) {
                    targetColumns.add(col);
                }
            }
        }

        List<DprofileColumnStats> results = new ArrayList<>();

        for (DataSourceAdapter.ColumnInfo colInfo : targetColumns) {
            try {
                DprofileColumnStats stats = analyzeSingleColumn(
                        adapter, colInfo, tableName, schema,
                        dsId, executionId, tableStatsId, totalRows, profileTime);
                if (stats != null) {
                    columnStatsMapper.insert(stats);
                    results.add(stats);
                }
            } catch (Exception e) {
                log.warn("列级探查失败: {}.{}.{}, 错误: {}",
                        dsId, tableName, colInfo.columnName(), e.getMessage());
            }
        }

        return results;
    }

    /**
     * 对单个列执行统计采集
     */
    private DprofileColumnStats analyzeSingleColumn(
            DataSourceAdapter adapter,
            DataSourceAdapter.ColumnInfo colInfo,
            String tableName,
            String schema,
            Long dsId,
            Long executionId,
            Long tableStatsId,
            long totalRows,
            LocalDateTime profileTime) {

        String columnName = colInfo.columnName();
        String quotedCol = adapter.quoteIdentifier(columnName);
        String quotedTable;
        if (schema != null) {
            quotedTable = adapter.quoteIdentifier(schema) + "." + adapter.quoteIdentifier(tableName);
        } else {
            quotedTable = adapter.quoteIdentifier(tableName);
        }

        // 构建基础统计 SQL
        String nullFunction = adapter.getNullFunction(quotedCol, "'__NULL_MARKER__'");

        // 1. 基础统计（空值数、唯一值）
        long nullCount = 0;
        long uniqueCount = 0;
        try {
            String basicSql = String.format(
                    "SELECT " +
                            "SUM(CASE WHEN %s IS NULL THEN 1 ELSE 0 END) as null_count, " +
                            "COUNT(DISTINCT %s) as unique_count, " +
                            "COUNT(*) as total_count " +
                            "FROM %s",
                    quotedCol, quotedCol, quotedTable);
            List<Map<String, Object>> basicResult = adapter.executeQuery(basicSql);
            if (!basicResult.isEmpty()) {
                Map<String, Object> row = basicResult.get(0);
                nullCount = extractLong(row.get("null_count"));
                uniqueCount = extractLong(row.get("unique_count"));
            }
        } catch (Exception e) {
            log.warn("基础统计查询失败: {}.{}", columnName, e.getMessage());
        }

        // 2. 根据数据类型执行不同的统计
        String dataType = colInfo.dataType() != null ? colInfo.dataType().toLowerCase() : "";
        boolean isNumeric = isNumericType(dataType);
        boolean isString = isStringType(dataType);
        boolean isDateTime = isDateTimeType(dataType);

        String minValue = null;
        String maxValue = null;
        BigDecimal avgValue = null;
        BigDecimal medianValue = null;
        BigDecimal stdDev = null;
        Integer minLength = null;
        Integer maxLength = null;
        BigDecimal avgLength = null;
        String topValuesJson = null;
        String histogramJson = null;
        Long zeroCount = null;
        Long negativeCount = null;
        BigDecimal zeroRate = null;

        if (isNumeric) {
            // 数值类型统计
            try {
                String numericSql = String.format(
                        "SELECT " +
                                "MIN(%s) as min_val, " +
                                "MAX(%s) as max_val, " +
                                "AVG(%s) as avg_val, " +
                                "STDDEV(%s) as std_dev, " +
                                "SUM(CASE WHEN %s = 0 THEN 1 ELSE 0 END) as zero_count, " +
                                "SUM(CASE WHEN %s < 0 THEN 1 ELSE 0 END) as negative_count " +
                                "FROM %s WHERE %s IS NOT NULL",
                        quotedCol, quotedCol, quotedCol, quotedCol,
                        quotedCol, quotedCol, quotedTable, quotedCol);
                List<Map<String, Object>> numericResult = adapter.executeQuery(numericSql);
                if (!numericResult.isEmpty()) {
                    Map<String, Object> row = numericResult.get(0);
                    minValue = String.valueOf(row.get("min_val"));
                    maxValue = String.valueOf(row.get("max_val"));
                    avgValue = toDecimal(row.get("avg_val"));
                    stdDev = toDecimal(row.get("std_dev"));
                    zeroCount = extractLong(row.get("zero_count"));
                    negativeCount = extractLong(row.get("negative_count"));
                }
            } catch (Exception e) {
                log.warn("数值统计失败: {}.{}", columnName, e.getMessage());
            }

            // 中位数计算（使用用户变量或派生表）
            try {
                String medianSql = String.format(
                        "SELECT %s as median_val FROM %s " +
                                "WHERE %s IS NOT NULL " +
                                "ORDER BY %s " +
                                "LIMIT 1 OFFSET (SELECT FLOOR(COUNT(*) / 2)) FROM %s WHERE %s IS NOT NULL)",
                        quotedCol, quotedTable, quotedCol, quotedCol, quotedTable, quotedCol);
                // 由于中位数 SQL 各数据库差异较大，这里用近似方法
                medianValue = calculateApproxMedian(adapter, tableName, quotedCol, quotedTable);
            } catch (Exception e) {
                log.debug("中位数计算失败: {}.{}", columnName, e.getMessage());
            }

            // 零值率和负值率
            long nonNullRows = totalRows - nullCount;
            if (nonNullRows > 0) {
                if (zeroCount != null) {
                    zeroRate = BigDecimal.valueOf(zeroCount)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(nonNullRows), 4, RoundingMode.HALF_UP);
                }
            }

        } else if (isString) {
            // 字符串类型统计
            try {
                String lengthSql = String.format(
                        "SELECT " +
                                "MIN(LENGTH(%s)) as min_len, " +
                                "MAX(LENGTH(%s)) as max_len, " +
                                "AVG(LENGTH(%s)) as avg_len " +
                                "FROM %s WHERE %s IS NOT NULL",
                        quotedCol, quotedCol, quotedCol, quotedTable, quotedCol);
                List<Map<String, Object>> lengthResult = adapter.executeQuery(lengthSql);
                if (!lengthResult.isEmpty()) {
                    Map<String, Object> row = lengthResult.get(0);
                    minLength = extractInt(row.get("min_len"));
                    maxLength = extractInt(row.get("max_len"));
                    avgLength = toDecimal(row.get("avg_len"));
                }
            } catch (Exception e) {
                log.warn("字符串长度统计失败: {}.{}", columnName, e.getMessage());
            }

            // 获取 Top-N 值分布
            topValuesJson = fetchTopValues(adapter, tableName, quotedCol, quotedTable, 10);

            // 简单直方图（按字符串长度分布）
            histogramJson = fetchLengthHistogram(adapter, tableName, quotedCol, quotedTable);

        } else if (isDateTime) {
            // 日期时间类型
            try {
                String dateSql = String.format(
                        "SELECT " +
                                "MIN(%s) as min_val, " +
                                "MAX(%s) as max_val " +
                                "FROM %s WHERE %s IS NOT NULL",
                        quotedCol, quotedCol, quotedTable, quotedCol);
                List<Map<String, Object>> dateResult = adapter.executeQuery(dateSql);
                if (!dateResult.isEmpty()) {
                    Map<String, Object> row = dateResult.get(0);
                    minValue = String.valueOf(row.get("min_val"));
                    maxValue = String.valueOf(row.get("max_val"));
                }
            } catch (Exception e) {
                log.warn("日期统计失败: {}.{}", columnName, e.getMessage());
            }
        }

        // 计算空值率和唯一率
        BigDecimal nullRate = null;
        BigDecimal uniqueRate = null;
        if (totalRows > 0) {
            nullRate = BigDecimal.valueOf(nullCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalRows), 4, RoundingMode.HALF_UP);
            uniqueRate = BigDecimal.valueOf(uniqueCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalRows), 4, RoundingMode.HALF_UP);
        }

        // 异常值检测（仅对数值类型）
        Long outlierCount = null;
        BigDecimal outlierRate = null;
        String outlierMethod = null;
        if (isNumeric && totalRows > 0) {
            try {
                Map<String, Object> outlierResult = distributionAnalyzer.detectOutliersByIQR(
                        dsId, tableName, columnName);
                if (outlierResult != null && !outlierResult.isEmpty()) {
                    outlierCount = extractLong(outlierResult.get("outlierCount"));
                    Object outlierRateObj = outlierResult.get("outlierRate");
                    if (outlierRateObj instanceof Number) {
                        outlierRate = BigDecimal.valueOf(((Number) outlierRateObj).doubleValue());
                    }
                    Object method = outlierResult.get("method");
                    outlierMethod = method != null ? method.toString() : "IQR";
                }
            } catch (Exception e) {
                log.debug("异常值检测失败: {}.{}.{}", dsId, tableName, columnName);
            }
        }

        // 构建并返回结果
        DprofileColumnStats.DprofileColumnStatsBuilder colBuilder = DprofileColumnStats.builder()
                .tableStatsId(tableStatsId)
                .executionId(executionId)
                .dsId(dsId)
                .tableName(tableName)
                .columnName(columnName)
                .profileTime(profileTime)
                .dataType(colInfo.dataType())
                .nullable(colInfo.nullable())
                .totalCount(totalRows)
                .nullCount(nullCount)
                .nullRate(nullRate)
                .uniqueCount(uniqueCount)
                .uniqueRate(uniqueRate)
                .minValue(minValue)
                .maxValue(maxValue)
                .avgValue(avgValue)
                .medianValue(medianValue)
                .stdDev(stdDev)
                .minLength(minLength)
                .maxLength(maxLength)
                .avgLength(avgLength)
                .topValues(topValuesJson)
                .histogram(histogramJson)
                .zeroCount(zeroCount)
                .zeroRate(zeroRate)
                .negativeCount(negativeCount)
                .outlierCount(outlierCount)
                .outlierRate(outlierRate)
                .outlierMethod(outlierMethod)
                .createTime(LocalDateTime.now());

        return colBuilder.build();
    }

    /**
     * 计算近似中位数（通过 SQL 近似）
     */
    private BigDecimal calculateApproxMedian(DataSourceAdapter adapter, String tableName,
                                             String quotedCol, String quotedTable) {
        try {
            // 使用派生表方式计算近似中位数
            String sql = String.format(
                    "SELECT t.%s as median_val FROM " +
                            "(SELECT @row:=@row+1 as rownum, %s " +
                            " FROM %s, (SELECT @row:=0) r " +
                            " WHERE %s IS NOT NULL " +
                            " ORDER BY %s) t " +
                            "WHERE t.rownum = (SELECT COUNT(*) / 2 FROM %s WHERE %s IS NOT NULL)",
                    quotedCol, quotedCol, quotedTable, quotedCol, quotedCol, quotedTable, quotedCol);
            List<Map<String, Object>> result = adapter.executeQuery(sql);
            if (!result.isEmpty()) {
                return toDecimal(result.get(0).get("median_val"));
            }
        } catch (Exception e) {
            log.debug("近似中位数计算失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取 Top-N 值分布
     */
    private String fetchTopValues(DataSourceAdapter adapter, String tableName,
                                  String quotedCol, String quotedTable, int topN) {
        try {
            String sql = String.format(
                    "SELECT %s as col_val, COUNT(*) as cnt " +
                            "FROM %s WHERE %s IS NOT NULL " +
                            "GROUP BY %s ORDER BY cnt DESC LIMIT %d",
                    quotedCol, quotedTable, quotedCol, quotedCol, topN);
            List<Map<String, Object>> result = adapter.executeQuery(sql);

            if (result.isEmpty()) return null;

            List<Map<String, Object>> topList = new ArrayList<>();
            for (Map<String, Object> row : result) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("value", row.get("col_val"));
                item.put("count", extractLong(row.get("cnt")));
                topList.add(item);
            }
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(topList);
        } catch (Exception e) {
            log.warn("Top值查询失败: {}, error: {}", quotedCol, e.getMessage());
            return null;
        }
    }

    /**
     * 获取字符串长度直方图
     */
    private String fetchLengthHistogram(DataSourceAdapter adapter, String tableName,
                                        String quotedCol, String quotedTable) {
        try {
            String sql = String.format(
                    "SELECT " +
                            "CASE " +
                            "  WHEN LENGTH(%s) <= 5 THEN '1-5' " +
                            "  WHEN LENGTH(%s) <= 10 THEN '6-10' " +
                            "  WHEN LENGTH(%s) <= 20 THEN '11-20' " +
                            "  WHEN LENGTH(%s) <= 50 THEN '21-50' " +
                            "  ELSE '50+' END as len_range, " +
                            "COUNT(*) as cnt " +
                            "FROM %s WHERE %s IS NOT NULL " +
                            "GROUP BY len_range ORDER BY MIN(LENGTH(%s))",
                    quotedCol, quotedCol, quotedCol, quotedCol, quotedTable, quotedCol, quotedCol);
            List<Map<String, Object>> result = adapter.executeQuery(sql);

            if (result.isEmpty()) return null;

            List<Map<String, Object>> histList = new ArrayList<>();
            for (Map<String, Object> row : result) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("range", row.get("len_range"));
                item.put("count", extractLong(row.get("cnt")));
                histList.add(item);
            }
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(histList);
        } catch (Exception e) {
            log.warn("直方图查询失败: {}, error: {}", quotedCol, e.getMessage());
            return null;
        }
    }

    // ==================== 类型判断 ====================

    private boolean isNumericType(String dataType) {
        if (dataType == null) return false;
        dataType = dataType.toLowerCase();
        return dataType.contains("int") || dataType.contains("decimal")
                || dataType.contains("numeric") || dataType.contains("float")
                || dataType.contains("double") || dataType.contains("real")
                || dataType.contains("number") || dataType.contains("smallmoney")
                || dataType.contains("money") || dataType.contains("bit");
    }

    private boolean isStringType(String dataType) {
        if (dataType == null) return false;
        dataType = dataType.toLowerCase();
        return dataType.contains("char") || dataType.contains("text")
                || dataType.contains("varchar") || dataType.contains("nchar")
                || dataType.contains("nvarchar") || dataType.contains("clob")
                || dataType.contains("blob") || dataType.contains("binary")
                || dataType.contains("varbinary");
    }

    private boolean isDateTimeType(String dataType) {
        if (dataType == null) return false;
        dataType = dataType.toLowerCase();
        return dataType.contains("date") || dataType.contains("time")
                || dataType.contains("timestamp") || dataType.contains("datetime")
                || dataType.contains("interval") || dataType.contains("year")
                || dataType.contains("smalldatetime");
    }

    // ==================== 工具方法 ====================

    private Long extractLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extractInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
