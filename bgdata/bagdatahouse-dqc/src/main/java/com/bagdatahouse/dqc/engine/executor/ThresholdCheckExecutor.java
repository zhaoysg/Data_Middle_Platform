package com.bagdatahouse.dqc.engine.executor;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.bagdatahouse.dqc.engine.RuleContext;
import com.bagdatahouse.dqc.engine.util.SqlSyntaxHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Executor for THRESHOLD_MIN, THRESHOLD_MAX, THRESHOLD_RANGE, and THRESHOLD rule types.
 * Uses SqlSyntaxHelper for cross-database SQL syntax adaptation.
 */
@Component
public class ThresholdCheckExecutor implements RuleExecutor {

    private static final Logger log = LoggerFactory.getLogger(ThresholdCheckExecutor.class);

    @Override
    public String getRuleType() {
        return "THRESHOLD_MIN";
    }

    @Override
    public ExecutionResult execute(RuleContext context, DataSourceAdapter adapter) {
        LocalDateTime startTime = LocalDateTime.now();
        String ruleType = context.getRuleType().toUpperCase();

        try {
            String tableName = SqlSyntaxHelper.validateTableName(context.getTargetTable());
            String columnName = SqlSyntaxHelper.validateColumnNameOptional(context.getTargetColumn());

            String sql;
            ExecutionResult result;

            switch (ruleType) {
                case "THRESHOLD_MIN" -> {
                    sql = SqlSyntaxHelper.buildMinValueSql(adapter, tableName, columnName);
                    result = handleThresholdMin(context, adapter, sql, startTime);
                }
                case "THRESHOLD_MAX" -> {
                    sql = SqlSyntaxHelper.buildMaxValueSql(adapter, tableName, columnName);
                    result = handleThresholdMax(context, adapter, sql, startTime);
                }
                case "THRESHOLD_RANGE", "THRESHOLD" -> {
                    sql = buildThresholdRangeSql(adapter, tableName, columnName, context.getThresholdMin(), context.getThresholdMax());
                    result = handleThresholdRange(context, adapter, sql, startTime);
                }
                case "COUNT_THRESHOLD" -> {
                    sql = buildCountThresholdSql(adapter, context);
                    result = handleCountThreshold(context, adapter, sql, startTime);
                }
                default -> {
                    sql = SqlSyntaxHelper.buildMinValueSql(adapter, tableName, columnName);
                    result = handleThresholdMin(context, adapter, sql, startTime);
                }
            }

            return result;

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for threshold check: {}", e.getMessage());
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Invalid parameters: " + e.getMessage(), getElapsedMs(startTime), startTime);
        } catch (Exception e) {
            log.error("Error executing threshold check rule: {}", context.getRuleId(), e);
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Execution error: " + e.getMessage(), getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleThresholdMin(RuleContext context, DataSourceAdapter adapter,
                                             String sql, LocalDateTime startTime) {
        log.debug("Executing THRESHOLD_MIN SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        if (results == null || results.isEmpty()) {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    "Query returned no results", getElapsedMs(startTime), startTime);
        }

        Map<String, Object> row = results.get(0);
        BigDecimal minValue = extractBigDecimal(row, "minValue");
        BigDecimal threshold = context.getThresholdMin();
        Long totalCount = getTotalCount(context.getTargetTable(), adapter);

        if (minValue == null) {
            minValue = BigDecimal.ZERO;
        }

        boolean passed = minValue.compareTo(threshold) >= 0;
        String errorDetail = null;
        int qualityScore;
        Long errorCount = passed ? 0L : 1L;
        Long passCount = passed ? totalCount : 0L;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Minimum value %.4f is below threshold %.4f", minValue, threshold);
        }

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, passCount, errorCount, minValue, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleThresholdMax(RuleContext context, DataSourceAdapter adapter,
                                             String sql, LocalDateTime startTime) {
        log.debug("Executing THRESHOLD_MAX SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        if (results == null || results.isEmpty()) {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    "Query returned no results", getElapsedMs(startTime), startTime);
        }

        Map<String, Object> row = results.get(0);
        BigDecimal maxValue = extractBigDecimal(row, "maxValue");
        BigDecimal threshold = context.getThresholdMax();
        Long totalCount = getTotalCount(context.getTargetTable(), adapter);

        if (maxValue == null) {
            maxValue = BigDecimal.ZERO;
        }

        boolean passed = maxValue.compareTo(threshold) <= 0;
        String errorDetail = null;
        int qualityScore;
        Long errorCount = passed ? 0L : 1L;
        Long passCount = passed ? totalCount : 0L;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Maximum value %.4f exceeds threshold %.4f", maxValue, threshold);
        }

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, passCount, errorCount, maxValue, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleThresholdRange(RuleContext context, DataSourceAdapter adapter,
                                                String sql, LocalDateTime startTime) {
        log.debug("Executing THRESHOLD_RANGE SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        if (results == null || results.isEmpty()) {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    "Query returned no results", getElapsedMs(startTime), startTime);
        }

        Map<String, Object> row = results.get(0);
        Long outOfRangeCount = extractLongValue(row, "outOfRangeCount");
        Long totalCount = getTotalCount(context.getTargetTable(), adapter);
        Long passCount = totalCount - outOfRangeCount;

        boolean passed = outOfRangeCount == 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            BigDecimal thresholdMin = context.getThresholdMin();
            BigDecimal thresholdMax = context.getThresholdMax();
            errorDetail = String.format("Found %d rows outside range [%.4f, %.4f]",
                    outOfRangeCount, thresholdMin, thresholdMax);
        }

        BigDecimal actualValue = BigDecimal.valueOf(outOfRangeCount);

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, passCount, outOfRangeCount, actualValue, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleCountThreshold(RuleContext context, DataSourceAdapter adapter,
                                                 String sql, LocalDateTime startTime) {
        log.debug("Executing COUNT_THRESHOLD SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        if (results == null || results.isEmpty()) {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    "Query returned no results", getElapsedMs(startTime), startTime);
        }

        Map<String, Object> row = results.get(0);
        Long countValue = extractLongValue(row, "countValue");
        BigDecimal thresholdMin = context.getThresholdMin();
        BigDecimal thresholdMax = context.getThresholdMax();

        Long totalCount = countValue;
        boolean passed;
        if (thresholdMin != null && thresholdMax != null) {
            passed = countValue >= thresholdMin.longValue() && countValue <= thresholdMax.longValue();
        } else if (thresholdMin != null) {
            passed = countValue >= thresholdMin.longValue();
        } else if (thresholdMax != null) {
            passed = countValue <= thresholdMax.longValue();
        } else {
            passed = countValue == 0;
        }

        Long errorCount = passed ? 0L : 1L;
        Long passCount = passed ? countValue : 0L;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            if (thresholdMin != null && thresholdMax != null) {
                errorDetail = String.format("Count value %d is outside range [%d, %d]",
                        countValue, thresholdMin.longValue(), thresholdMax.longValue());
            } else if (thresholdMin != null) {
                errorDetail = String.format("Count value %d is below threshold %d", countValue, thresholdMin.longValue());
            } else if (thresholdMax != null) {
                errorDetail = String.format("Count value %d exceeds threshold %d", countValue, thresholdMax.longValue());
            } else {
                errorDetail = String.format("Count value %d is not zero", countValue);
            }
        }

        BigDecimal actualValue = BigDecimal.valueOf(countValue);

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, passCount, errorCount, actualValue, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private String buildThresholdRangeSql(DataSourceAdapter adapter, String tableName, String columnName,
                                          BigDecimal min, BigDecimal max) {
        String minVal = min != null ? min.toPlainString() : null;
        String maxVal = max != null ? max.toPlainString() : null;
        return SqlSyntaxHelper.buildRangeCheckSql(adapter, tableName, columnName, minVal, maxVal);
    }

    private String buildCountThresholdSql(DataSourceAdapter adapter, RuleContext context) {
        String ruleExpr = context.getRuleExpr();
        if (ruleExpr != null && !ruleExpr.isBlank()) {
            return interpolateRuleExpr(ruleExpr, context);
        }
        String tableName = SqlSyntaxHelper.validateTableName(context.getTargetTable());
        String columnName = context.getTargetColumn();
        if (columnName != null && !columnName.isBlank()) {
            return String.format("SELECT COUNT(*) AS countValue FROM %s WHERE %s",
                    SqlSyntaxHelper.quoteTable(adapter, tableName),
                    SqlSyntaxHelper.quote(adapter, columnName));
        }
        return SqlSyntaxHelper.buildCountSql(adapter, tableName).replace("AS total", "AS countValue");
    }

    private String interpolateRuleExpr(String ruleExpr, RuleContext context) {
        String result = ruleExpr;
        if (context.getTargetTable() != null) {
            result = result.replace("${table}", context.getTargetTable());
        }
        if (context.getTargetColumn() != null) {
            result = result.replace("${column}", context.getTargetColumn());
        }
        if (context.getTargetDsId() != null) {
            result = result.replace("${dsId}", context.getTargetDsId().toString());
        }
        return result;
    }

    private Long getTotalCount(String tableName, DataSourceAdapter adapter) {
        try {
            String countSql = SqlSyntaxHelper.buildCountSql(adapter, tableName);
            List<Map<String, Object>> results = adapter.executeQuery(countSql);
            if (results != null && !results.isEmpty()) {
                return extractLongValue(results.get(0).get("total"));
            }
        } catch (Exception e) {
            log.warn("Failed to get total count for table: {}", tableName, e);
        }
        return 0L;
    }

    private Long extractLongValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
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

    private BigDecimal extractBigDecimal(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private long getElapsedMs(LocalDateTime startTime) {
        return System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli();
    }
}
