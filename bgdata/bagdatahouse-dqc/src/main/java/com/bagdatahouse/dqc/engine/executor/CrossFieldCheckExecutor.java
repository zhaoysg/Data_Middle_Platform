package com.bagdatahouse.dqc.engine.executor;

import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.bagdatahouse.dqc.engine.RuleContext;
import com.bagdatahouse.dqc.engine.util.SqlSyntaxHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Executor for cross-field and cross-table check rule types.
 * Uses SqlSyntaxHelper for cross-database SQL syntax adaptation.
 */
@Component
public class CrossFieldCheckExecutor implements RuleExecutor {

    private static final Logger log = LoggerFactory.getLogger(CrossFieldCheckExecutor.class);

    @Autowired
    private DataSourceAdapterRegistry adapterRegistry;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Override
    public String getRuleType() {
        return "CROSS_FIELD_COMPARE";
    }

    @Override
    public ExecutionResult execute(RuleContext context, DataSourceAdapter adapter) {
        LocalDateTime startTime = LocalDateTime.now();
        String ruleType = context.getRuleType().toUpperCase();

        try {
            String tableName = SqlSyntaxHelper.validateTableName(context.getTargetTable());

            String sql;
            ExecutionResult result;

            switch (ruleType) {
                case "CROSS_FIELD_COMPARE" -> {
                    sql = SqlSyntaxHelper.buildCrossFieldCompareSql(adapter, tableName,
                            context.getTargetColumn(), context.getCompareColumn());
                    result = handleCrossFieldCompare(context, adapter, sql, startTime);
                }
                case "CROSS_FIELD_SUM" -> {
                    sql = SqlSyntaxHelper.buildCrossFieldSumSql(adapter, tableName,
                            context.getTargetColumn(), context.getCompareColumn(),
                            context.getRuleExpr() != null ? context.getRuleExpr() : context.getTargetColumn());
                    result = handleCrossFieldSum(context, adapter, sql, startTime);
                }
                case "CROSS_FIELD_NULL_CHECK" -> {
                    sql = SqlSyntaxHelper.buildCrossFieldNullCheckSql(adapter, tableName,
                            context.getTargetColumn(), context.getCompareColumn());
                    result = handleCrossFieldNullCheck(context, adapter, sql, startTime);
                }
                case "CROSS_TABLE_COUNT" -> {
                    sql = SqlSyntaxHelper.buildCrossTableCountDiffSql(adapter,
                            context.getTargetTable(), context.getCompareTable());
                    result = handleCrossTableCount(context, adapter, sql, startTime);
                }
                case "CROSS_TABLE_PRIMARY_KEY" -> {
                    sql = buildCrossTablePrimaryKeySql(adapter, context);
                    result = handleCrossTablePrimaryKey(context, adapter, sql, startTime);
                }
                default -> {
                    sql = SqlSyntaxHelper.buildCrossFieldCompareSql(adapter, tableName,
                            context.getTargetColumn(), context.getCompareColumn());
                    result = handleCrossFieldCompare(context, adapter, sql, startTime);
                }
            }

            return result;

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for cross-field check: {}", e.getMessage());
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Invalid parameters: " + e.getMessage(), getElapsedMs(startTime), startTime);
        } catch (Exception e) {
            log.error("Error executing cross-field check rule: {}", context.getRuleId(), e);
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Execution error: " + e.getMessage(), getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleCrossFieldCompare(RuleContext context, DataSourceAdapter adapter,
                                               String sql, LocalDateTime startTime) {
        log.debug("Executing CROSS_FIELD_COMPARE SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        Long errorCount = extractLongValue(results);
        Long totalCount = getTotalCount(context.getTargetTable(), adapter);
        Long passCount = totalCount - errorCount;

        boolean passed = errorCount == 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Found %d rows where %s is not greater than %s",
                    errorCount, context.getTargetColumn(), context.getCompareColumn());
        }

        BigDecimal actualValue = BigDecimal.valueOf(errorCount);

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, passCount, errorCount, actualValue, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleCrossFieldSum(RuleContext context, DataSourceAdapter adapter,
                                             String sql, LocalDateTime startTime) {
        log.debug("Executing CROSS_FIELD_SUM SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        Long errorCount = extractLongValue(results);
        Long totalCount = getTotalCount(context.getTargetTable(), adapter);
        Long passCount = totalCount - errorCount;

        boolean passed = errorCount == 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Found %d rows where %s + %s != %s",
                    errorCount, context.getTargetColumn(), context.getCompareColumn(),
                    context.getRuleExpr());
        }

        BigDecimal actualValue = BigDecimal.valueOf(errorCount);

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, passCount, errorCount, actualValue, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleCrossFieldNullCheck(RuleContext context, DataSourceAdapter adapter,
                                                  String sql, LocalDateTime startTime) {
        log.debug("Executing CROSS_FIELD_NULL_CHECK SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        Long errorCount = extractLongValue(results);
        Long totalCount = getTotalCount(context.getTargetTable(), adapter);
        Long passCount = totalCount - errorCount;

        boolean passed = errorCount == 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Found %d rows with inconsistent null values between %s and %s",
                    errorCount, context.getTargetColumn(), context.getCompareColumn());
        }

        BigDecimal actualValue = BigDecimal.valueOf(errorCount);

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, passCount, errorCount, actualValue, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleCrossTableCount(RuleContext context, DataSourceAdapter adapter,
                                               String sql, LocalDateTime startTime) {
        log.debug("Executing CROSS_TABLE_COUNT SQL: {}", sql);

        Long sourceCount = getTableCount(context.getTargetTable(), adapter);
        Long targetCount = 0L;

        if (context.getCompareDsId() != null && context.getCompareTable() != null) {
            DataSourceAdapter targetAdapter = getAdapter(context.getCompareDsId());
            if (targetAdapter != null) {
                targetCount = getTableCount(context.getCompareTable(), targetAdapter);
            }
        }

        Long diff = Math.abs(sourceCount - targetCount);
        boolean passed = diff == 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Row count mismatch: source=%d, target=%d, diff=%d",
                    sourceCount, targetCount, diff);
        }

        BigDecimal actualValue = BigDecimal.valueOf(diff);

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    sourceCount + targetCount, sourceCount, diff, actualValue, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleCrossTablePrimaryKey(RuleContext context, DataSourceAdapter adapter,
                                                   String sql, LocalDateTime startTime) {
        log.debug("Executing CROSS_TABLE_PRIMARY_KEY SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        Long errorCount = results != null ? (long) results.size() : 0L;
        Long totalCount = getTableCount(context.getTargetTable(), adapter) +
                          getTableCount(context.getCompareTable(), adapter);
        Long passCount = totalCount - errorCount;

        boolean passed = errorCount == 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Found %d primary keys with inconsistent values between tables",
                    errorCount);
        }

        BigDecimal actualValue = BigDecimal.valueOf(errorCount);

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, passCount, errorCount, actualValue, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private String buildCrossTablePrimaryKeySql(DataSourceAdapter adapter, RuleContext context) {
        String sourceTable = SqlSyntaxHelper.quoteTable(adapter, context.getTargetTable());
        String targetTable = context.getCompareTable() != null ?
                SqlSyntaxHelper.quoteTable(adapter, context.getCompareTable()) :
                SqlSyntaxHelper.quoteTable(adapter, "target_table");
        String pkColumn = SqlSyntaxHelper.quote(adapter, context.getTargetColumn());
        return String.format(
                "SELECT %s FROM (SELECT %s FROM %s UNION ALL SELECT %s FROM %s) t GROUP BY %s HAVING COUNT(*) = 1",
                pkColumn, pkColumn, sourceTable, pkColumn, targetTable, pkColumn);
    }

    private Long getTableCount(String tableName, DataSourceAdapter adapter) {
        try {
            String sql = SqlSyntaxHelper.buildRowCountSql(adapter, tableName);
            List<Map<String, Object>> results = adapter.executeQuery(sql);
            if (results != null && !results.isEmpty()) {
                return extractLongValue(results.get(0).get("rowCount"));
            }
        } catch (Exception e) {
            log.warn("Failed to get count for table: {}", tableName, e);
        }
        return 0L;
    }

    private Long getTotalCount(String tableName, DataSourceAdapter adapter) {
        return getTableCount(tableName, adapter);
    }

    private DataSourceAdapter getAdapter(Long dsId) {
        if (dsId == null) {
            return null;
        }

        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter != null) {
            return adapter;
        }

        DqDatasource datasource = datasourceMapper.selectById(dsId);
        if (datasource != null) {
            return adapterRegistry.getAdapter(datasource.getDsType());
        }

        return null;
    }

    private Long extractLongValue(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return 0L;
        }
        Object value = results.get(0).values().iterator().next();
        return extractLongValue(value);
    }

    private Long extractLongValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private long getElapsedMs(LocalDateTime startTime) {
        return System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli();
    }
}
