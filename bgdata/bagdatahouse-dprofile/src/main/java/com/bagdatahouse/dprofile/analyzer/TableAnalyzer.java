package com.bagdatahouse.dprofile.analyzer;

import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.entity.DprofileTableStats;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.mapper.DprofileTableStatsMapper;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import com.bagdatahouse.dprofile.vo.ProfileExecutionRecordVO;
import com.bagdatahouse.dprofile.vo.TableProfileResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表级探查分析器
 * 负责对目标表执行表级统计信息采集，包括：
 * - 基本信息（行数、列数、存储大小、注释）
 * - 更新信息（最后更新时间、增量行数、数据新鲜度）
 * - 字段结构（字段列表及元数据）
 * - 健康状态评估
 */
@Slf4j
@Component
public class TableAnalyzer {

    @Autowired
    private DataSourceAdapterRegistry adapterRegistry;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private DprofileTableStatsMapper tableStatsMapper;

    /**
     * 执行中或已完成的探查记录缓存（执行ID -> 记录）
     * 用于追踪执行状态
     */
    private final Map<Long, ProfileExecutionRecordVO> executionCache = new ConcurrentHashMap<>();

    /**
     * 执行表级探查（核心方法）
     *
     * @param taskId   探查任务ID（可为null，代表一次性手动探查）
     * @param taskName 任务名称
     * @param dsId     数据源ID
     * @param tableName 目标表名
     * @return 探查结果 VO
     */
    @Transactional(rollbackFor = Exception.class)
    public TableProfileResultVO analyze(Long taskId, String taskName, Long dsId, String tableName) {
        return analyze(taskId, taskName, dsId, tableName, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public TableProfileResultVO analyze(Long taskId, String taskName, Long dsId, String tableName, Long executionId) {
        return analyze(taskId, taskName, dsId, tableName, null, executionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public TableProfileResultVO analyze(Long taskId, String taskName, Long dsId, String tableName,
                                         String schema, Long executionId) {
        if (dsId == null) {
            throw new BusinessException(400, "数据源ID不能为空");
        }
        if (!StringUtils.hasText(tableName)) {
            throw new BusinessException(400, "目标表名不能为空");
        }

        long resolvedExecutionId = executionId != null ? executionId : System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();

        // 创建执行记录
        ProfileExecutionRecordVO record = ProfileExecutionRecordVO.builder()
                .executionId(resolvedExecutionId)
                .taskId(taskId)
                .taskName(taskName)
                .targetDsId(dsId)
                .tableName(tableName)
                .status("RUNNING")
                .phase("TABLE_STATS")
                .progress(0)
                .startTime(startTime)
                .build();
        executionCache.put(resolvedExecutionId, record);

        DataSourceAdapter adapter = null;
        try {
            adapter = adapterRegistry.getAdapterById(dsId);
            if (adapter == null) {
                throw new BusinessException(400, "数据源适配器未找到，dsId=" + dsId);
            }

            // 获取数据源信息（用于 VO 返回）
            DqDatasource datasource = datasourceMapper.selectById(dsId);
            String dsType = datasource != null ? datasource.getDsType() : "UNKNOWN";
            String dsName = datasource != null ? datasource.getDsName() : "";

            // ==== 阶段1: 采集基本统计 (0-40%) ====
            updateRecord(record, "RUNNING", "TABLE_STATS", 10, null);

            // 行数（PostgreSQL 使用 schema-aware 重载）
            long rowCount;
            if (schema != null && adapter instanceof com.bagdatahouse.datasource.adapter.PostgresAdapter) {
                rowCount = ((com.bagdatahouse.datasource.adapter.PostgresAdapter) adapter).getRowCount(schema, tableName);
            } else {
                rowCount = adapter.getRowCount(tableName);
            }
            updateRecord(record, "RUNNING", "TABLE_STATS", 20, "行数采集中...");

            // 列数（PostgreSQL 使用 schema-aware 重载）
            int columnCount;
            if (schema != null && adapter instanceof com.bagdatahouse.datasource.adapter.PostgresAdapter) {
                columnCount = ((com.bagdatahouse.datasource.adapter.PostgresAdapter) adapter).getColumnCount(schema, tableName);
            } else {
                columnCount = adapter.getColumnCount(tableName);
            }
            updateRecord(record, "RUNNING", "TABLE_STATS", 30, "列数采集中...");

            // 存储大小
            BigDecimal storageBytes = adapter.getStorageBytes(tableName);
            updateRecord(record, "RUNNING", "TABLE_STATS", 40, "存储大小采集中...");

            // ==== 阶段2: 采集字段结构 (40-60%) ====
            updateRecord(record, "RUNNING", "TABLE_STATS", 50, "采集字段结构...");
            List<DataSourceAdapter.ColumnInfo> columns;
            if (schema != null) {
                columns = adapter.getColumns(schema, tableName);
            } else {
                columns = adapter.getColumns(tableName);
            }
            List<TableProfileResultVO.ColumnInfoVO> columnInfoList = new ArrayList<>();
            for (DataSourceAdapter.ColumnInfo col : columns) {
                columnInfoList.add(TableProfileResultVO.ColumnInfoVO.builder()
                        .columnName(col.columnName())
                        .dataType(col.dataType())
                        .columnComment(col.columnComment())
                        .nullable(col.nullable())
                        .primaryKey(col.primaryKey())
                        .defaultValue(col.defaultValue())
                        .columnSize(col.columnSize())
                        .build());
            }
            updateRecord(record, "RUNNING", "TABLE_STATS", 60, "字段结构采集完成");

            // ==== 阶段3: 采集更新信息 (60-80%) ====
            updateRecord(record, "RUNNING", "TABLE_STATS", 70, "采集更新信息...");
            LocalDateTime lastUpdateTime = fetchLastUpdateTime(adapter, tableName, schema);
            updateRecord(record, "RUNNING", "TABLE_STATS", 80, "更新信息采集完成");

            // ==== 阶段4: 计算增量与健康状态 (80-90%) ====
            updateRecord(record, "RUNNING", "TABLE_STATS", 85, "计算增量与健康状态...");
            Long incrementRows = null;
            BigDecimal incrementRate = null;
            Long freshnessMinutes = null;

            // 获取上次探查记录
            DprofileTableStats lastStats = fetchLastTableStats(dsId, tableName);
            if (lastStats != null && lastStats.getRowCount() != null) {
                incrementRows = rowCount - lastStats.getRowCount();
                if (lastStats.getRowCount() > 0) {
                    incrementRate = BigDecimal.valueOf(incrementRows)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(lastStats.getRowCount()), 2, RoundingMode.HALF_UP);
                }
            }

            // 计算数据新鲜度
            if (lastUpdateTime != null) {
                freshnessMinutes = Duration.between(lastUpdateTime, LocalDateTime.now()).toMinutes();
            }

            // 健康状态评估
            List<String> healthHints = evaluateHealthStatus(rowCount, columnCount, columns);
            String healthStatus = healthHints.isEmpty() ? "HEALTHY" :
                    (healthHints.stream().anyMatch(h -> h.contains("空表") || h.contains("异常")) ? "WARNING" : "HEALTHY");

            updateRecord(record, "RUNNING", "TABLE_STATS", 90, "健康状态评估完成");

            // ==== 阶段5: 存储结果 (90-100%) ====
            updateRecord(record, "RUNNING", "COMPLETED", 95, "存储探查结果...");
            LocalDateTime profileTime = LocalDateTime.now();
            long elapsedMs = Duration.between(startTime, profileTime).toMillis();

            // 存储到数据库
            DprofileTableStats tableStats = DprofileTableStats.builder()
                    .taskId(taskId)
                    .executionId(resolvedExecutionId)
                    .dsId(dsId)
                    .tableName(tableName)
                    .profileTime(profileTime)
                    .rowCount(rowCount)
                    .columnCount(columnCount)
                    .storageBytes(storageBytes)
                    .tableComment(fetchTableComment(adapter, tableName, schema, dsType))
                    .updateTime(lastUpdateTime)
                    .incrementRows(incrementRows)
                    .createTime(profileTime)
                    .build();
            tableStatsMapper.insert(tableStats);
            Long tableStatsId = tableStats.getId();

            updateRecord(record, "SUCCESS", "COMPLETED", 100, "探查完成");
            record.setEndTime(LocalDateTime.now());
            record.setElapsedMs(elapsedMs);
            record.setTableStatsId(tableStatsId);

            // ==== 组装返回结果 ====
            return TableProfileResultVO.builder()
                    .executionId(resolvedExecutionId)
                    .dsId(dsId)
                    .dsType(dsType)
                    .dsName(dsName)
                    .tableName(tableName)
                    .profileTime(profileTime)
                    .elapsedMs(elapsedMs)
                    .rowCount(rowCount)
                    .columnCount(columnCount)
                    .storageBytes(storageBytes)
                    .storageSizeFormatted(formatStorageSize(storageBytes))
                    .tableComment(fetchTableComment(adapter, tableName, schema, dsType))
                    .lastUpdateTime(lastUpdateTime)
                    .incrementRows(incrementRows)
                    .incrementRate(incrementRate)
                    .freshnessMinutes(freshnessMinutes)
                    .columns(columnInfoList)
                    .healthStatus(healthStatus)
                    .healthHints(healthHints)
                    .taskId(taskId)
                    .taskName(taskName)
                    .tableStatsId(tableStatsId)
                    .build();

        } catch (Exception e) {
            log.error("表级探查失败: dsId={}, table={}, schema={}", dsId, tableName, schema, e);
            updateRecord(record, "FAILED", "TABLE_STATS", record.getProgress(),
                    "探查失败: " + e.getMessage());
            record.setErrorMessage(e.getMessage());
            record.setEndTime(LocalDateTime.now());
            record.setElapsedMs(Duration.between(startTime, LocalDateTime.now()).toMillis());
            throw new BusinessException(500, "表级探查失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取探查执行记录
     */
    public ProfileExecutionRecordVO getExecutionRecord(Long executionId) {
        return executionCache.get(executionId);
    }

    /**
     * 获取当前活跃的执行记录列表
     */
    public List<ProfileExecutionRecordVO> getActiveExecutions() {
        return executionCache.values().stream()
                .filter(r -> "RUNNING".equals(r.getStatus()) || "PENDING".equals(r.getStatus()))
                .toList();
    }

    /**
     * 查询最近一次探查结果（不执行新的探查）
     */
    public TableProfileResultVO getLastProfile(Long dsId, String tableName) {
        DprofileTableStats lastStats = fetchLastTableStats(dsId, tableName);
        if (lastStats == null) {
            return null;
        }

        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        DqDatasource datasource = datasourceMapper.selectById(dsId);

        // 获取字段信息
        List<TableProfileResultVO.ColumnInfoVO> columns = new ArrayList<>();
        if (adapter != null) {
            try {
                List<DataSourceAdapter.ColumnInfo> colList = adapter.getColumns(tableName);
                for (DataSourceAdapter.ColumnInfo col : colList) {
                    columns.add(TableProfileResultVO.ColumnInfoVO.builder()
                            .columnName(col.columnName())
                            .dataType(col.dataType())
                            .columnComment(col.columnComment())
                            .nullable(col.nullable())
                            .primaryKey(col.primaryKey())
                            .defaultValue(col.defaultValue())
                            .columnSize(col.columnSize())
                            .build());
                }
            } catch (Exception e) {
                log.warn("获取字段信息失败: dsId={}, table={}", dsId, tableName, e.getMessage());
            }
        }

        // 计算增量
        Long incrementRows = lastStats.getIncrementRows();
        BigDecimal incrementRate = null;
        if (lastStats.getRowCount() != null && lastStats.getRowCount() > 0 && incrementRows != null) {
            incrementRate = BigDecimal.valueOf(incrementRows)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(lastStats.getRowCount()), 2, RoundingMode.HALF_UP);
        }

        // 计算新鲜度
        Long freshnessMinutes = null;
        if (lastStats.getUpdateTime() != null) {
            freshnessMinutes = Duration.between(lastStats.getUpdateTime(), LocalDateTime.now()).toMinutes();
        }

        // 健康提示
        List<String> healthHints = evaluateHealthStatus(
                lastStats.getRowCount(),
                lastStats.getColumnCount(),
                null
        );
        String healthStatus = healthHints.isEmpty() ? "HEALTHY" : "WARNING";

        return TableProfileResultVO.builder()
                .executionId(lastStats.getExecutionId())
                .dsId(dsId)
                .dsType(datasource != null ? datasource.getDsType() : null)
                .dsName(datasource != null ? datasource.getDsName() : null)
                .tableName(tableName)
                .profileTime(lastStats.getProfileTime())
                .rowCount(lastStats.getRowCount())
                .columnCount(lastStats.getColumnCount())
                .storageBytes(lastStats.getStorageBytes())
                .storageSizeFormatted(formatStorageSize(lastStats.getStorageBytes()))
                .tableComment(lastStats.getTableComment())
                .lastUpdateTime(lastStats.getUpdateTime())
                .incrementRows(incrementRows)
                .incrementRate(incrementRate)
                .freshnessMinutes(freshnessMinutes)
                .columns(columns)
                .healthStatus(healthStatus)
                .healthHints(healthHints)
                .taskId(lastStats.getTaskId())
                .tableStatsId(lastStats.getId())
                .build();
    }

    /**
     * 查询表历史探查记录
     */
    public List<DprofileTableStats> listHistory(Long dsId, String tableName, int limit) {
        return tableStatsMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DprofileTableStats>()
                        .eq(dsId != null, DprofileTableStats::getDsId, dsId)
                        .eq(StringUtils.hasText(tableName), DprofileTableStats::getTableName, tableName)
                        .orderByDesc(DprofileTableStats::getProfileTime)
                        .last("LIMIT " + limit)
        );
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取表最后更新时间
     */
    private LocalDateTime fetchLastUpdateTime(DataSourceAdapter adapter, String tableName, String schema) {
        try {
            List<DataSourceAdapter.ColumnInfo> columns;
            if (schema != null) {
                columns = adapter.getColumns(schema, tableName);
            } else {
                columns = adapter.getColumns(tableName);
            }
            // 尝试找主键或常见时间字段
            for (DataSourceAdapter.ColumnInfo col : columns) {
                String colNameLower = col.columnName().toLowerCase();
                String dataType = col.dataType() != null ? col.dataType().toLowerCase() : "";
                // 匹配常见时间字段
                if ((colNameLower.contains("update") || colNameLower.contains("modify")
                        || colNameLower.equals("updatetime") || colNameLower.equals("modifiedtime")
                        || colNameLower.equals("lastmodified") || colNameLower.equals("modified_date")
                        || colNameLower.equals("update_date") || colNameLower.equals("etl_time"))
                        && (dataType.contains("timestamp") || dataType.contains("datetime")
                        || dataType.contains("date"))) {
                    String quotedTable = schema != null
                            ? adapter.quoteIdentifier(schema) + "." + adapter.quoteIdentifier(tableName)
                            : adapter.quoteIdentifier(tableName);
                    String sql = "SELECT MAX(" + adapter.quoteIdentifier(col.columnName()) + ") as lastUpdate FROM "
                            + quotedTable;
                    List<Map<String, Object>> result = adapter.executeQuery(sql);
                    if (!result.isEmpty()) {
                        Object val = result.get(0).get("lastUpdate");
                        if (val != null) {
                            if (val instanceof java.sql.Timestamp) {
                                return ((java.sql.Timestamp) val).toLocalDateTime();
                            } else if (val instanceof java.time.LocalDateTime) {
                                return (LocalDateTime) val;
                            } else if (val instanceof java.util.Date) {
                                return ((java.util.Date) val).toInstant()
                                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取表最后更新时间失败: table={}, schema={}, error={}", tableName, schema, e.getMessage());
        }
        return null;
    }

    /**
     * 获取表注释
     */
    private String fetchTableComment(DataSourceAdapter adapter, String tableName, String schema, String dsType) {
        try {
            String sql;
            if ("POSTGRESQL".equalsIgnoreCase(dsType) && schema != null) {
                // PostgreSQL: 用 obj_description 函数获取表注释
                String safeTableLit = "'" + schema.replace("'", "''") + "." + tableName.replace("'", "''") + "'";
                sql = "SELECT COALESCE(obj_description(" + safeTableLit + "::regclass, 'pg_class'), '') as tableComment";
            } else {
                sql = "SELECT TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
            }
            List<Map<String, Object>> result;
            if (sql.contains("?")) {
                result = adapter.executeQuery(sql, tableName);
            } else {
                result = adapter.executeQuery(sql);
            }
            if (!result.isEmpty()) {
                Object comment = result.get(0).get("tableComment");
                return comment != null ? comment.toString() : "";
            }
        } catch (Exception e) {
            log.debug("获取表注释失败: table={}, schema={}, error={}", tableName, schema, e.getMessage());
        }
        return "";
    }

    /**
     * 获取上次探查记录
     */
    private DprofileTableStats fetchLastTableStats(Long dsId, String tableName) {
        return tableStatsMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DprofileTableStats>()
                        .eq(DprofileTableStats::getDsId, dsId)
                        .eq(DprofileTableStats::getTableName, tableName)
                        .orderByDesc(DprofileTableStats::getProfileTime)
                        .last("LIMIT 1")
        );
    }

    /**
     * 评估健康状态
     */
    private List<String> evaluateHealthStatus(Long rowCount, Integer columnCount,
                                               List<DataSourceAdapter.ColumnInfo> columns) {
        List<String> hints = new ArrayList<>();

        if (rowCount == null || rowCount == 0) {
            hints.add("【警告】表为空（行数为0），请确认是否正常");
        } else if (rowCount < 100) {
            hints.add("【提示】表行数较少（" + rowCount + " 行），数据量较小");
        }

        if (columnCount == null || columnCount == 0) {
            hints.add("【警告】表没有定义字段");
        } else if (columnCount > 200) {
            hints.add("【提示】表字段数较多（" + columnCount + " 列），建议拆分");
        }

        if (columns != null) {
            // 检查没有注释的字段比例
            long noCommentCount = columns.stream()
                    .filter(c -> !StringUtils.hasText(c.columnComment()))
                    .count();
            if (columns.size() > 5 && noCommentCount > columns.size() * 0.7) {
                hints.add("【建议】表中有 " + noCommentCount + " 个字段（占比 "
                        + Math.round(noCommentCount * 100.0 / columns.size()) + "%）缺少注释，建议补充");
            }
        }

        return hints;
    }

    /**
     * 格式化存储大小
     */
    private String formatStorageSize(BigDecimal bytes) {
        if (bytes == null || bytes.compareTo(BigDecimal.ZERO) <= 0) {
            return "0 B";
        }
        double size = bytes.doubleValue();
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * 更新执行记录
     */
    private void updateRecord(ProfileExecutionRecordVO record, String status, String phase,
                               Integer progress, String message) {
        if (record == null) return;
        if (status != null) record.setStatus(status);
        if (phase != null) record.setPhase(phase);
        if (progress != null) record.setProgress(progress);
        if (message != null) record.setMessage(message);
        executionCache.put(record.getExecutionId(), record);
    }
}
