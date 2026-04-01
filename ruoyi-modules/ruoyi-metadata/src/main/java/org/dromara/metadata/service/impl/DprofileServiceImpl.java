package org.dromara.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.datasource.adapter.DataSourceAdapter.ColumnInfo;
import org.dromara.metadata.domain.DprofileColumnReport;
import org.dromara.metadata.domain.DprofileReport;
import org.dromara.metadata.domain.vo.ColumnStats;
import org.dromara.metadata.domain.vo.TableStats;
import org.dromara.metadata.mapper.DprofileColumnReportMapper;
import org.dromara.metadata.mapper.DprofileReportMapper;
import org.dromara.metadata.service.IDprofileService;
import org.dromara.metadata.support.DatasourceHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据探查服务实现 - 核心分析引擎
 * <p>
 * 提供表级和列级的数据统计分析能力，支持 BASIC/DETAILED/FULL 三种探查级别。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DprofileServiceImpl implements IDprofileService {

    private final DatasourceHelper datasourceHelper;
    private final DprofileReportMapper reportMapper;
    private final DprofileColumnReportMapper columnReportMapper;

    /**
     * HARD-LIMIT: 单表最大分析列数
     */
    private static final int MAX_COLUMNS = 200;

    /**
     * HARD-LIMIT: 采样最大行数
     */
    private static final int MAX_SAMPLE_ROWS = 10000;

    /**
     * HARD-LIMIT: 单次运行最大表数
     */
    private static final int MAX_TABLES_PER_RUN = 100;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long analyzeTable(Long dsId, String tableName, String level) {
        DataSourceAdapter adapter = datasourceHelper.getAdapter(dsId);

        // 1. Table-level stats
        TableStats tableStats = getTableStats(dsId, tableName);

        // 2. Save report
        DprofileReport report = new DprofileReport();
        report.setTaskId(null);
        report.setDsId(dsId);
        report.setTableName(tableName);
        report.setRowCount(tableStats.rowCount());
        report.setColumnCount(tableStats.columnCount());
        report.setDataSizeBytes(tableStats.dataSizeBytes());
        report.setStorageComment(tableStats.tableComment());
        report.setLastModified(tableStats.lastModified() != null
            ? LocalDateTime.parse(tableStats.lastModified().replace(" ", "T").substring(0, 19))
            : null);

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("rowCount", tableStats.rowCount());
        profileData.put("columnCount", tableStats.columnCount());
        profileData.put("level", level);
        report.setProfileData(JSON.toJSONString(profileData));

        reportMapper.insert(report);
        Long reportId = report.getId();

        // 3. Column-level stats (only for DETAILED and FULL)
        if ("DETAILED".equalsIgnoreCase(level) || "FULL".equalsIgnoreCase(level)) {
            List<ColumnStats> columnStatsList = getColumnStats(dsId, tableName);
            for (ColumnStats cs : columnStatsList) {
                DprofileColumnReport col = new DprofileColumnReport();
                col.setReportId(reportId);
                col.setDsId(dsId);
                col.setTableName(tableName);
                col.setColumnName(cs.columnName());
                col.setDataType(cs.dataType());
                col.setColumnComment(cs.columnComment());
                col.setNullable(cs.nullable() ? "Y" : "N");
                col.setIsPrimaryKey(cs.primaryKey() ? "Y" : "N");
                col.setTotalCount(tableStats.rowCount());
                col.setNullCount(cs.nullCount());
                col.setNullRate(BigDecimal.valueOf(cs.nullRate()));
                col.setUniqueCount(cs.uniqueCount());
                col.setUniqueRate(BigDecimal.valueOf(cs.uniqueRate()));
                col.setSampleValues(JSON.toJSONString(cs.sampleValues()));
                col.setTopValues(JSON.toJSONString(cs.topValues()));
                columnReportMapper.insert(col);
            }
        }

        log.info("表 {} 探查完成，报告ID: {}, 行数: {}, 列数: {}, 级别: {}",
            tableName, reportId, tableStats.rowCount(), tableStats.columnCount(), level);

        return reportId;
    }

    @Override
    public List<Long> analyzeTables(Long dsId, List<String> tableNames, String level) {
        List<Long> reportIds = new ArrayList<>();
        int count = 0;

        for (String table : tableNames) {
            if (count >= MAX_TABLES_PER_RUN) {
                log.warn("达到最大表数量限制 {}, 跳过剩余表", MAX_TABLES_PER_RUN);
                break;
            }
            try {
                Long reportId = analyzeTable(dsId, table, level);
                reportIds.add(reportId);
            } catch (Exception e) {
                log.error("分析表 {} 失败: {}", table, e.getMessage(), e);
            }
            count++;
        }

        log.info("批量分析完成，共处理 {} 张表，成功 {} 张", count, reportIds.size());
        return reportIds;
    }

    @Override
    public TableStats getTableStats(Long dsId, String tableName) {
        DataSourceAdapter adapter = datasourceHelper.getAdapter(dsId);

        long rowCount = adapter.getRowCount(tableName);
        List<ColumnInfo> columns = adapter.getColumns(tableName);

        // HARD-LIMIT: only analyze first MAX_COLUMNS columns
        if (columns.size() > MAX_COLUMNS) {
            log.warn("表 {} 列数 {} 超过限制 {}, 仅分析前 {} 列",
                tableName, columns.size(), MAX_COLUMNS, MAX_COLUMNS);
            columns = columns.subList(0, MAX_COLUMNS);
        }

        String tableComment = adapter.getTableComment(tableName);
        Optional<String> lastModified = adapter.getTableLastUpdateTime(tableName);

        return new TableStats(
            tableName,
            rowCount,
            columns.size(),
            null,
            tableComment,
            lastModified.orElse(null)
        );
    }

    @Override
    public List<ColumnStats> getColumnStats(Long dsId, String tableName) {
        DataSourceAdapter adapter = datasourceHelper.getAdapter(dsId);
        List<ColumnInfo> columns = adapter.getColumns(tableName);

        // HARD-LIMIT: only analyze first MAX_COLUMNS columns
        if (columns.size() > MAX_COLUMNS) {
            columns = columns.subList(0, MAX_COLUMNS);
        }

        JdbcTemplate jdbc = datasourceHelper.getJdbcTemplate(dsId);

        List<ColumnStats> result = new ArrayList<>();
        for (ColumnInfo col : columns) {
            try {
                ColumnStats stats = analyzeColumn(jdbc, tableName, col);
                result.add(stats);
            } catch (Exception e) {
                log.warn("分析列 {}.{} 失败: {}", tableName, col.columnName(), e.getMessage());
                result.add(createEmptyStats(col));
            }
        }
        return result;
    }

    private ColumnStats analyzeColumn(JdbcTemplate jdbc, String tableName, ColumnInfo col) {
        String colName = col.columnName();
        String quotedName = quoteIdentifier(colName);
        String quotedTable = quoteIdentifier(tableName);

        // COUNT(*) total
        long totalCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + quotedTable + " LIMIT 1", Long.class);
        if (totalCount == 0) {
            return createEmptyStats(col);
        }

        // NULL count
        long nullCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + quotedTable + " WHERE " + quotedName + " IS NULL", Long.class);

        // Unique count
        long uniqueCount = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT " + quotedName + ") FROM " + quotedTable, Long.class);

        // Sample values (MAX_SAMPLE_ROWS limit)
        List<String> samples = jdbc.queryForList(
            "SELECT " + quotedName + " FROM " + quotedTable +
            " WHERE " + quotedName + " IS NOT NULL LIMIT " + MAX_SAMPLE_ROWS, String.class);

        // Top values (distribution) - TOP 10
        Map<String, Long> topValues = new LinkedHashMap<>();
        String countSql = "SELECT " + quotedName + ", COUNT(*) as cnt FROM " + quotedTable +
            " WHERE " + quotedName + " IS NOT NULL " +
            " GROUP BY " + quotedName + " ORDER BY cnt DESC LIMIT 10";

        jdbc.query(countSql, (rs) -> {
            topValues.put(rs.getString(1), rs.getLong(2));
        });

        double nullRate = totalCount > 0 ? (double) nullCount / totalCount : 0;
        double uniqueRate = totalCount > 0 ? (double) uniqueCount / totalCount : 0;

        // Limit samples to 100 for storage
        if (samples.size() > 100) {
            samples = samples.subList(0, 100);
        }

        return new ColumnStats(
            colName,
            col.dataType(),
            col.columnComment(),
            col.nullable(),
            col.primaryKey(),
            nullCount,
            nullRate,
            uniqueCount,
            uniqueRate,
            samples,
            topValues
        );
    }

    private ColumnStats createEmptyStats(ColumnInfo col) {
        return new ColumnStats(
            col.columnName(),
            col.dataType(),
            col.columnComment(),
            col.nullable(),
            col.primaryKey(),
            0, 0, 0, 0,
            List.of(),
            Map.of()
        );
    }

    /**
     * 引用标识符（适配不同数据库）
     */
    private String quoteIdentifier(String name) {
        // 默认使用反引号，可根据数据源类型适配
        return "`" + name.replace("`", "``") + "`";
    }
}
