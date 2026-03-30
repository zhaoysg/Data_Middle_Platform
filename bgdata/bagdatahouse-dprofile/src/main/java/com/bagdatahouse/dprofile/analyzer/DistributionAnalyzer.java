package com.bagdatahouse.dprofile.analyzer;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 分布分析器
 * 负责对列级统计数据进行分布分析和异常检测，包括：
 * - Top-N 频率分布
 * - 长度分布直方图
 * - 数值分布直方图
 * - 异常值检测（基于 3σ 法则 / IQR 法则）
 */
@Slf4j
@Component
public class DistributionAnalyzer {

    @Autowired
    private DataSourceAdapterRegistry adapterRegistry;

    /**
     * 分析列值的频率分布（Top-N）
     */
    public Map<String, Object> analyzeFrequency(Long dsId, String tableName,
                                               String columnName, int topN) {
        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        String quotedCol = adapter.quoteIdentifier(columnName);
        String quotedTable = adapter.quoteIdentifier(tableName);

        try {
            // 获取总行数
            String totalSql = "SELECT COUNT(*) as total FROM " + quotedTable;
            List<Map<String, Object>> totalResult = adapter.executeQuery(totalSql);
            long totalRows = 0;
            if (!totalResult.isEmpty()) {
                Object val = totalResult.get(0).get("total");
                totalRows = val instanceof Number ? ((Number) val).longValue() : 0;
            }

            // Top-N 频率
            String freqSql = String.format(
                    "SELECT %s as col_val, COUNT(*) as cnt " +
                            "FROM %s WHERE %s IS NOT NULL " +
                            "GROUP BY %s ORDER BY cnt DESC LIMIT %d",
                    quotedCol, quotedTable, quotedCol, quotedCol, topN);
            List<Map<String, Object>> freqResult = adapter.executeQuery(freqSql);

            List<Map<String, Object>> distribution = new ArrayList<>();
            for (Map<String, Object> row : freqResult) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("value", row.get("col_val"));
                long count = row.get("cnt") instanceof Number
                        ? ((Number) row.get("cnt")).longValue() : 0;
                item.put("count", count);
                item.put("rate", totalRows > 0
                        ? BigDecimal.valueOf(count * 100.0 / totalRows)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);
                distribution.add(item);
            }
            result.put("distribution", distribution);
            result.put("totalRows", totalRows);

        } catch (Exception e) {
            log.warn("频率分布分析失败: {}.{}.{}", dsId, tableName, columnName, e);
        }

        return result;
    }

    /**
     * 分析字符串长度分布
     */
    public Map<String, Object> analyzeLengthDistribution(Long dsId, String tableName,
                                                        String columnName) {
        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        String quotedCol = adapter.quoteIdentifier(columnName);
        String quotedTable = adapter.quoteIdentifier(tableName);

        try {
            // 获取总行数
            String totalSql = "SELECT COUNT(*) as total FROM " + quotedTable
                    + " WHERE " + quotedCol + " IS NOT NULL";
            List<Map<String, Object>> totalResult = adapter.executeQuery(totalSql);
            long totalRows = 0;
            if (!totalResult.isEmpty()) {
                Object val = totalResult.get(0).get("total");
                totalRows = val instanceof Number ? ((Number) val).longValue() : 0;
            }

            // 长度区间分布
            String sql = String.format(
                    "SELECT " +
                            "CASE " +
                            "  WHEN LENGTH(%s) <= 5 THEN '1-5' " +
                            "  WHEN LENGTH(%s) <= 10 THEN '6-10' " +
                            "  WHEN LENGTH(%s) <= 20 THEN '11-20' " +
                            "  WHEN LENGTH(%s) <= 50 THEN '21-50' " +
                            "  WHEN LENGTH(%s) <= 100 THEN '51-100' " +
                            "  ELSE '100+' END as len_range, " +
                            "COUNT(*) as cnt " +
                            "FROM %s WHERE %s IS NOT NULL " +
                            "GROUP BY len_range ORDER BY MIN(LENGTH(%s))",
                    quotedCol, quotedCol, quotedCol, quotedCol, quotedCol,
                    quotedTable, quotedCol, quotedCol);
            List<Map<String, Object>> histResult = adapter.executeQuery(sql);

            List<Map<String, Object>> histogram = new ArrayList<>();
            for (Map<String, Object> row : histResult) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("range", row.get("len_range"));
                long count = row.get("cnt") instanceof Number
                        ? ((Number) row.get("cnt")).longValue() : 0;
                item.put("count", count);
                item.put("rate", totalRows > 0
                        ? BigDecimal.valueOf(count * 100.0 / totalRows)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);
                histogram.add(item);
            }
            result.put("histogram", histogram);
            result.put("totalRows", totalRows);

        } catch (Exception e) {
            log.warn("长度分布分析失败: {}.{}.{}", dsId, tableName, columnName, e);
        }

        return result;
    }

    /**
     * 分析数值列的分布（用于直方图）
     */
    public Map<String, Object> analyzeNumericDistribution(Long dsId, String tableName,
                                                          String columnName, int bucketCount) {
        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        String quotedCol = adapter.quoteIdentifier(columnName);
        String quotedTable = adapter.quoteIdentifier(tableName);

        try {
            // 获取 min/max 和总行数
            String statsSql = String.format(
                    "SELECT MIN(%s) as min_val, MAX(%s) as max_val, " +
                            "COUNT(*) as total " +
                            "FROM %s WHERE %s IS NOT NULL",
                    quotedCol, quotedCol, quotedTable, quotedCol);
            List<Map<String, Object>> statsResult = adapter.executeQuery(statsSql);

            if (statsResult.isEmpty()) {
                return result;
            }

            Map<String, Object> statsRow = statsResult.get(0);
            Object minVal = statsRow.get("min_val");
            Object maxVal = statsRow.get("max_val");
            Object totalVal = statsRow.get("total");
            long totalRows = totalVal instanceof Number ? ((Number) totalVal).longValue() : 0;

            if (minVal == null || maxVal == null) {
                return result;
            }

            double min = parseDouble(minVal);
            double max = parseDouble(maxVal);
            double bucketSize = (max - min) / bucketCount;

            if (bucketSize <= 0) {
                // 所有值相同，单桶
                Map<String, Object> singleBucket = new LinkedHashMap<>();
                singleBucket.put("range", String.format("%.2f", min));
                singleBucket.put("count", totalRows);
                singleBucket.put("rate", 100.0);
                result.put("histogram", Collections.singletonList(singleBucket));
                result.put("totalRows", totalRows);
                result.put("min", min);
                result.put("max", max);
                return result;
            }

            // 构建直方图桶
            StringBuilder caseExpr = new StringBuilder("CASE ");
            for (int i = 0; i < bucketCount; i++) {
                double lower = min + i * bucketSize;
                double upper = min + (i + 1) * bucketSize;
                if (i == bucketCount - 1) {
                    // 最后一个桶包含上界
                    caseExpr.append(String.format("WHEN %s <= %.6f THEN '%.2f-%.2f' ",
                            quotedCol, upper, lower, upper));
                } else {
                    caseExpr.append(String.format("WHEN %s < %.6f THEN '%.2f-%.2f' ",
                            quotedCol, upper, lower, upper));
                }
            }
            caseExpr.append("ELSE 'outlier' END");

            String histogramSql = String.format(
                    "SELECT %s as bucket, COUNT(*) as cnt " +
                            "FROM %s WHERE %s IS NOT NULL " +
                            "GROUP BY %s ORDER BY MIN(CAST(%s AS DOUBLE))",
                    caseExpr, quotedTable, quotedCol, caseExpr, quotedCol);
            List<Map<String, Object>> histResult = adapter.executeQuery(histogramSql);

            List<Map<String, Object>> histogram = new ArrayList<>();
            for (Map<String, Object> row : histResult) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("range", row.get("bucket"));
                long count = row.get("cnt") instanceof Number
                        ? ((Number) row.get("cnt")).longValue() : 0;
                item.put("count", count);
                item.put("rate", totalRows > 0
                        ? BigDecimal.valueOf(count * 100.0 / totalRows)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);
                histogram.add(item);
            }
            result.put("histogram", histogram);
            result.put("totalRows", totalRows);
            result.put("min", min);
            result.put("max", max);

        } catch (Exception e) {
            log.warn("数值分布分析失败: {}.{}.{}", dsId, tableName, columnName, e);
        }

        return result;
    }

    /**
     * 异常值检测（基于 IQR 法则）
     * IQR = Q3 - Q1
     * 异常值定义为小于 Q1 - 1.5*IQR 或大于 Q3 + 1.5*IQR 的值
     *
     * @return 包含 Q1, Q3, IQR, 下界, 上界, 异常值数量的 Map
     */
    public Map<String, Object> detectOutliersByIQR(Long dsId, String tableName,
                                                   String columnName) {
        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        String quotedCol = adapter.quoteIdentifier(columnName);
        String quotedTable = adapter.quoteIdentifier(tableName);

        try {
            // 使用近似百分位数（MySQL/SQL Server/PostgreSQL 均支持）
            String percentileSql = String.format(
                    "SELECT " +
                            "PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY CAST(%s AS DOUBLE)) as q1, " +
                            "PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY CAST(%s AS DOUBLE)) as q3, " +
                            "COUNT(*) as total " +
                            "FROM %s WHERE %s IS NOT NULL",
                    quotedCol, quotedCol, quotedTable, quotedCol);
            List<Map<String, Object>> percentResult = adapter.executeQuery(percentileSql);

            if (percentResult.isEmpty()) {
                return result;
            }

            Map<String, Object> row = percentResult.get(0);
            Object q1Obj = row.get("q1");
            Object q3Obj = row.get("q3");
            Object totalObj = row.get("total");

            if (q1Obj == null || q3Obj == null) {
                return result;
            }

            double q1 = parseDouble(q1Obj);
            double q3 = parseDouble(q3Obj);
            double iqr = q3 - q1;
            double lowerBound = q1 - 1.5 * iqr;
            double upperBound = q3 + 1.5 * iqr;
            long totalRows = totalObj instanceof Number ? ((Number) totalObj).longValue() : 0;

            // 统计异常值数量
            String outlierSql = String.format(
                    "SELECT COUNT(*) as outlier_cnt FROM %s " +
                            "WHERE %s IS NOT NULL " +
                            "AND (CAST(%s AS DOUBLE) < %.6f OR CAST(%s AS DOUBLE) > %.6f)",
                    quotedTable, quotedCol, quotedCol, lowerBound, quotedCol, upperBound);
            List<Map<String, Object>> outlierResult = adapter.executeQuery(outlierSql);

            long outlierCount = 0;
            if (!outlierResult.isEmpty()) {
                Object outlierObj = outlierResult.get(0).get("outlier_cnt");
                outlierCount = outlierObj instanceof Number ? ((Number) outlierObj).longValue() : 0;
            }

            result.put("q1", q1);
            result.put("q3", q3);
            result.put("iqr", iqr);
            result.put("lowerBound", lowerBound);
            result.put("upperBound", upperBound);
            result.put("outlierCount", outlierCount);
            result.put("outlierRate", totalRows > 0
                    ? BigDecimal.valueOf(outlierCount * 100.0 / totalRows)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);
            result.put("totalRows", totalRows);

        } catch (Exception e) {
            log.debug("IQR 异常检测失败（使用 3σ 法则作为备选）: {}.{}.{}", dsId, tableName, columnName);
            return detectOutliersByStdDev(dsId, tableName, columnName);
        }

        return result;
    }

    /**
     * 异常值检测（基于 3σ 法则备选方案）
     * 适用于近似正态分布的数据
     */
    private Map<String, Object> detectOutliersByStdDev(Long dsId, String tableName,
                                                        String columnName) {
        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        String quotedCol = adapter.quoteIdentifier(columnName);
        String quotedTable = adapter.quoteIdentifier(tableName);

        try {
            String sql = String.format(
                    "SELECT AVG(CAST(%s AS DOUBLE)) as avg_val, " +
                            "STDDEV(CAST(%s AS DOUBLE)) as std_val, " +
                            "COUNT(*) as total " +
                            "FROM %s WHERE %s IS NOT NULL",
                    quotedCol, quotedCol, quotedTable, quotedCol);
            List<Map<String, Object>> statsResult = adapter.executeQuery(sql);

            if (statsResult.isEmpty()) {
                return result;
            }

            Map<String, Object> row = statsResult.get(0);
            Object avgObj = row.get("avg_val");
            Object stdObj = row.get("std_val");
            Object totalObj = row.get("total");

            if (avgObj == null || stdObj == null) {
                return result;
            }

            double avg = parseDouble(avgObj);
            double std = parseDouble(stdObj);
            long totalRows = totalObj instanceof Number ? ((Number) totalObj).longValue() : 0;

            double lowerBound = avg - 3 * std;
            double upperBound = avg + 3 * std;

            // 统计异常值数量
            String outlierSql = String.format(
                    "SELECT COUNT(*) as outlier_cnt FROM %s " +
                            "WHERE %s IS NOT NULL " +
                            "AND (ABS(CAST(%s AS DOUBLE) - %.6f) > %.6f * 3)",
                    quotedTable, quotedCol, quotedCol, avg, std);
            List<Map<String, Object>> outlierResult = adapter.executeQuery(outlierSql);

            long outlierCount = 0;
            if (!outlierResult.isEmpty()) {
                Object outlierObj = outlierResult.get(0).get("outlier_cnt");
                outlierCount = outlierObj instanceof Number ? ((Number) outlierObj).longValue() : 0;
            }

            result.put("avg", avg);
            result.put("std", std);
            result.put("lowerBound", lowerBound);
            result.put("upperBound", upperBound);
            result.put("outlierCount", outlierCount);
            result.put("outlierRate", totalRows > 0
                    ? BigDecimal.valueOf(outlierCount * 100.0 / totalRows)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);
            result.put("totalRows", totalRows);
            result.put("method", "3sigma");

        } catch (Exception e) {
            log.warn("3σ 异常检测也失败: {}.{}.{}", dsId, tableName, columnName, e);
        }

        return result;
    }

    // ==================== 工具方法 ====================

    private double parseDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString().replaceAll("[^\\d.eE+-]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
