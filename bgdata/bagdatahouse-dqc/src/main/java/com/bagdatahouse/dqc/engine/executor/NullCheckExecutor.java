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
 * Executor for NULL_CHECK and ROW_COUNT_NOT_ZERO rule types.
 * Uses SqlSyntaxHelper for cross-database SQL syntax adaptation.
 */
@Component
public class NullCheckExecutor implements RuleExecutor {

    private static final Logger log = LoggerFactory.getLogger(NullCheckExecutor.class);

    private static final BigDecimal DEFAULT_NULL_THRESHOLD = BigDecimal.valueOf(5.0);

    @Override
    public String getRuleType() {
        return "NULL_CHECK";
    }

    @Override
    public ExecutionResult execute(RuleContext context, DataSourceAdapter adapter) {
        LocalDateTime startTime = LocalDateTime.now();
        String ruleType = context.getRuleType().toUpperCase();

        try {
            String tableName = SqlSyntaxHelper.validateTableName(context.getTargetTable());
            String columnName = SqlSyntaxHelper.validateColumnName(context.getTargetColumn());

            String sql;
            if ("ROW_COUNT_NOT_ZERO".equals(ruleType)) {
                sql = SqlSyntaxHelper.buildRowCountSql(adapter, tableName);
            } else {
                sql = SqlSyntaxHelper.buildNullCheckSql(adapter, tableName, columnName);
            }

            log.debug("Executing NULL_CHECK SQL: {}", sql);
            List<Map<String, Object>> results = adapter.executeQuery(sql);

            if (results == null || results.isEmpty()) {
                return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                        "Query returned no results", getElapsedMs(startTime), startTime);
            }

            Map<String, Object> row = results.get(0);

            if ("ROW_COUNT_NOT_ZERO".equals(ruleType)) {
                return handleRowCountResult(context, adapter, tableName, row, startTime);
            } else {
                return handleNullCheckResult(context, sql, row, startTime);
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for NULL_CHECK: {}", e.getMessage());
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Invalid parameters: " + e.getMessage(), getElapsedMs(startTime), startTime);
        } catch (Exception e) {
            log.error("Error executing NULL_CHECK rule: {}", context.getRuleId(), e);
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Execution error: " + e.getMessage(), getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleNullCheckResult(RuleContext context, String sql,
                                                   Map<String, Object> row, LocalDateTime startTime) {
        Long totalCount = extractLongValue(row, "total");
        Long nullCount = extractLongValue(row, "nullCount");
        Long passCount = totalCount - nullCount;

        BigDecimal nullRate = BigDecimal.ZERO;
        if (totalCount != null && totalCount > 0 && nullCount != null) {
            nullRate = BigDecimal.valueOf(nullCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
        }

        BigDecimal threshold = context.getThresholdMin() != null ?
                context.getThresholdMin() : DEFAULT_NULL_THRESHOLD;

        boolean passed = nullRate.compareTo(threshold) <= 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = String.format("Null rate %.2f%% exceeds threshold %.2f%% (nulls: %d, total: %d)",
                    nullRate, threshold, nullCount != null ? nullCount : 0, totalCount != null ? totalCount : 0);
        }

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                    totalCount, passCount, nullCount, nullRate, qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, getElapsedMs(startTime), startTime);
        }
    }

    private ExecutionResult handleRowCountResult(RuleContext context, DataSourceAdapter adapter,
                                                  String tableName, Map<String, Object> row,
                                                  LocalDateTime startTime) {
        Long rowCount = extractLongValue(row, "rowCount");
        Long totalCount = rowCount;
        Long passCount = rowCount;

        boolean passed = rowCount != null && rowCount > 0;
        String errorDetail = null;
        int qualityScore;

        if (passed) {
            qualityScore = 100;
        } else {
            qualityScore = 0;
            errorDetail = "Row count is zero, expected at least 1 row";
        }

        if (passed) {
            return ExecutionResult.success(context.getRuleId(), context.getRuleName(),
                    SqlSyntaxHelper.buildRowCountSql(adapter, tableName),
                    totalCount, passCount, 0L, BigDecimal.valueOf(rowCount), qualityScore,
                    getElapsedMs(startTime), startTime);
        } else {
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(),
                    SqlSyntaxHelper.buildRowCountSql(adapter, tableName),
                    errorDetail, getElapsedMs(startTime), startTime);
        }
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

    private long getElapsedMs(LocalDateTime startTime) {
        return System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli();
    }
}
