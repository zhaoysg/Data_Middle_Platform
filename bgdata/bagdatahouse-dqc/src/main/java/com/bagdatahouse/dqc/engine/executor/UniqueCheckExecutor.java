package com.bagdatahouse.dqc.engine.executor;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.bagdatahouse.dqc.engine.RuleContext;
import com.bagdatahouse.dqc.engine.util.SqlSyntaxHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Executor for UNIQUE_CHECK, DUPLICATE_CHECK, and CARDINALITY rule types.
 * Uses SqlSyntaxHelper for cross-database SQL syntax adaptation.
 */
@Component
public class UniqueCheckExecutor implements RuleExecutor {

    private static final Logger log = LoggerFactory.getLogger(UniqueCheckExecutor.class);

    private static final BigDecimal DEFAULT_CARDINALITY_THRESHOLD = BigDecimal.valueOf(10.0);

    @Override
    public String getRuleType() {
        return "UNIQUE_CHECK";
    }

    @Override
    public ExecutionResult execute(RuleContext context, DataSourceAdapter adapter) {
        LocalDateTime startTime = LocalDateTime.now();
        String ruleType = context.getRuleType().toUpperCase();

        try {
            String tableName = SqlSyntaxHelper.validateTableName(context.getTargetTable());
            String columnName = SqlSyntaxHelper.validateColumnName(context.getTargetColumn());

            String sql;
            ExecutionResult result;

            switch (ruleType) {
                case "UNIQUE_CHECK" -> {
                    sql = SqlSyntaxHelper.buildUniqueCountSql(adapter, tableName, columnName);
                    result = handleUniqueCheck(context, adapter, sql, startTime);
                }
                case "DUPLICATE_CHECK" -> {
                    sql = SqlSyntaxHelper.buildDuplicateCheckSql(adapter, tableName, columnName);
                    result = handleDuplicateCheck(context, adapter, sql, startTime);
                }
                case "CARDINALITY" -> {
                    sql = SqlSyntaxHelper.buildCardinalitySql(adapter, tableName, columnName);
                    result = handleCardinality(context, adapter, sql, startTime);
                }
                default -> {
                    sql = SqlSyntaxHelper.buildUniqueCountSql(adapter, tableName, columnName);
                    result = handleUniqueCheck(context, adapter, sql, startTime);
                }
            }

            return result;

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for uniqueness check: {}", e.getMessage());
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Invalid parameters: " + e.getMessage(), getElapsedMs(startTime), startTime);
        } catch (Exception e) {
            log.error("Error executing uniqueness check rule: {}", context.getRuleId(), e);
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Execution error: " + e.getMessage(), getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleUniqueCheck(RuleContext context, DataSourceAdapter adapter,
                                               String sql, LocalDateTime startTime) {
        log.debug("Executing UNIQUE_CHECK SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        if (results == null || results.isEmpty()) {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    "Query returned no results", getElapsedMs(startTime), startTime);
        }

        Map<String, Object> row = results.get(0);
        Long totalCount = extractLongValue(row, "total");
        Long uniqueCount = extractLongValue(row, "uniqueCount");
        Long duplicateCount = totalCount - uniqueCount;
        Long passCount = uniqueCount;

        boolean passed = duplicateCount == 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Found %d duplicate values out of %d total records", duplicateCount, totalCount);
        }

        BigDecimal actualValue = BigDecimal.valueOf(duplicateCount);

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, passCount, duplicateCount, actualValue, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleDuplicateCheck(RuleContext context, DataSourceAdapter adapter,
                                                  String sql, LocalDateTime startTime) {
        log.debug("Executing DUPLICATE_CHECK SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        Long errorCount = results != null ? (long) results.size() : 0L;
        Long totalCount = getTotalCount(context.getTargetTable(), adapter);
        Long passCount = totalCount - errorCount;

        boolean passed = errorCount == 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Found %d duplicate groups with more than 1 occurrence", errorCount);
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

    private ExecutionResult handleCardinality(RuleContext context, DataSourceAdapter adapter,
                                               String sql, LocalDateTime startTime) {
        log.debug("Executing CARDINALITY SQL: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);

        if (results == null || results.isEmpty()) {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    "Query returned no results", getElapsedMs(startTime), startTime);
        }

        Map<String, Object> row = results.get(0);
        BigDecimal cardinalityRate = extractBigDecimal(row, "cardinalityRate");
        Long totalCount = extractLongValue(row, "total");
        Long uniqueCount = extractLongValue(row, "uniqueCount");

        if (cardinalityRate == null) {
            cardinalityRate = BigDecimal.ZERO;
        }

        BigDecimal threshold = context.getThresholdMin() != null ?
                context.getThresholdMin() : DEFAULT_CARDINALITY_THRESHOLD;

        boolean passed = cardinalityRate.compareTo(threshold) >= 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Cardinality rate %.2f%% is below threshold %.2f%%", cardinalityRate, threshold);
        }

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, uniqueCount, totalCount - uniqueCount, cardinalityRate, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private Long getTotalCount(String tableName, DataSourceAdapter adapter) {
        try {
            String countSql = SqlSyntaxHelper.buildCountSql(adapter, tableName);
            List<Map<String, Object>> results = adapter.executeQuery(countSql);
            if (results != null && !results.isEmpty()) {
                return extractLongValue(results.get(0), "total");
            }
        } catch (Exception e) {
            log.warn("Failed to get total count for table: {}", tableName, e);
        }
        return 0L;
    }

    private Long extractLongValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
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
            return BigDecimal.ZERO;
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
            return BigDecimal.ZERO;
        }
    }

    private long getElapsedMs(LocalDateTime startTime) {
        return System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli();
    }
}
