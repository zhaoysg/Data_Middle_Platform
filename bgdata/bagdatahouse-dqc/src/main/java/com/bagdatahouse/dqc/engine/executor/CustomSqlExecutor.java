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
import java.util.regex.Pattern;

/**
 * Executor for CUSTOM_SQL and SQL rule types.
 * Uses SqlSyntaxHelper for cross-database SQL syntax adaptation.
 * Includes SQL injection prevention validation.
 */
@Component
public class CustomSqlExecutor implements RuleExecutor {

    private static final Logger log = LoggerFactory.getLogger(CustomSqlExecutor.class);

    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
            ".*(DROP|DELETE|TRUNCATE|ALTER|CREATE|INSERT|UPDATE|GRANT|REVOKE)\\s+",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String getRuleType() {
        return "CUSTOM_SQL";
    }

    @Override
    public ExecutionResult execute(RuleContext context, DataSourceAdapter adapter) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            String ruleExpr = context.getRuleExpr();
            if (ruleExpr == null || ruleExpr.isBlank()) {
                return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                        "Custom SQL expression is required", getElapsedMs(startTime), startTime);
            }

            String sql = interpolateRuleExpr(ruleExpr, context);

            validateSql(sql);

            log.debug("Executing CUSTOM_SQL: {}", sql);
            List<Map<String, Object>> results = adapter.executeQuery(sql);

            Long errorCount = 0L;
            if (results != null && !results.isEmpty()) {
                Map<String, Object> firstRow = results.get(0);
                Object value = firstRow.values().iterator().next();
                errorCount = extractLongValue(value);
            }

            Long totalCount = getTotalCount(adapter, context);
            Long passCount = totalCount - errorCount;

            boolean passed = errorCount == 0;
            String errorDetail = null;
            int qualityScore;

            if (passed) {
                qualityScore = 100;
            } else {
                qualityScore = 0;
                errorDetail = String.format("Custom SQL check failed with error count: %d", errorCount);
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

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for CUSTOM_SQL: {}", e.getMessage());
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Invalid parameters: " + e.getMessage(), getElapsedMs(startTime), startTime);
        } catch (Exception e) {
            log.error("Error executing CUSTOM_SQL rule: {}", context.getRuleId(), e);
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Execution error: " + e.getMessage(), getElapsedMs(startTime), startTime);
        }
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
        if (context.getCompareTable() != null) {
            result = result.replace("${compareTable}", context.getCompareTable());
        }
        if (context.getCompareColumn() != null) {
            result = result.replace("${compareColumn}", context.getCompareColumn());
        }
        if (context.getThresholdMin() != null) {
            result = result.replace("${thresholdMin}", context.getThresholdMin().toPlainString());
        }
        if (context.getThresholdMax() != null) {
            result = result.replace("${thresholdMax}", context.getThresholdMax().toPlainString());
        }

        return result;
    }

    private void validateSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL expression cannot be empty");
        }

        if (DANGEROUS_SQL_PATTERN.matcher(sql).matches()) {
            throw new IllegalArgumentException(
                    "SQL contains dangerous operations (DROP, DELETE, TRUNCATE, ALTER, CREATE, INSERT, UPDATE, GRANT, REVOKE)");
        }

        String upperSql = sql.toUpperCase();
        if (upperSql.contains("DROP TABLE") || upperSql.contains("DROP DATABASE") ||
                upperSql.contains("DELETE FROM") || upperSql.contains("TRUNCATE TABLE")) {
            throw new IllegalArgumentException("SQL contains forbidden data modification operations");
        }
    }

    private Long getTotalCount(DataSourceAdapter adapter, RuleContext context) {
        String tableName = context.getTargetTable();
        if (tableName == null || tableName.isBlank()) {
            return 0L;
        }
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
