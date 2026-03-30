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
 * Executor for REGEX_PHONE, REGEX_EMAIL, REGEX_IDCARD, and generic REGEX rule types.
 * Uses SqlSyntaxHelper for cross-database SQL syntax adaptation.
 */
@Component
public class RegexCheckExecutor implements RuleExecutor {

    private static final Logger log = LoggerFactory.getLogger(RegexCheckExecutor.class);

    private static final String PHONE_REGEX = "^1[3-9][0-9]{9}$";
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final String ID_CARD_REGEX = "^[1-9][0-9]{5}(19|20)[0-9]{2}((0[1-9])|(1[0-2]))((0[1-9])|([1-2][0-9])|(3[0-1]))[0-9]{3}[0-9Xx]$";

    @Override
    public String getRuleType() {
        return "REGEX_PHONE";
    }

    @Override
    public ExecutionResult execute(RuleContext context, DataSourceAdapter adapter) {
        LocalDateTime startTime = LocalDateTime.now();
        String ruleType = context.getRuleType().toUpperCase();

        try {
            String tableName = SqlSyntaxHelper.validateTableName(context.getTargetTable());
            String columnName = SqlSyntaxHelper.validateColumnName(context.getTargetColumn());

            String regexPattern = getRegexPattern(context, ruleType);
            String sql = SqlSyntaxHelper.buildRegexCheckSql(
                    adapter, tableName, columnName, regexPattern, ruleType);

            log.debug("Executing REGEX check SQL: {}", sql);
            List<Map<String, Object>> results = adapter.executeQuery(sql);

            Long errorCount = 0L;
            if (results != null && !results.isEmpty()) {
                Object value = results.get(0).values().iterator().next();
                errorCount = extractLongValue(value);
            }

            Long totalCount = getTotalCount(adapter, context.getTargetTable());
            Long passCount = totalCount - errorCount;

            boolean passed = errorCount == 0;
            String errorDetail = null;
            int qualityScore;

            if (passed) {
                qualityScore = 100;
            } else {
                qualityScore = 0;
                errorDetail = String.format("Found %d rows with invalid %s format", errorCount,
                        getFormatName(ruleType));
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
            log.error("Invalid parameters for REGEX check: {}", e.getMessage());
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Invalid parameters: " + e.getMessage(), getElapsedMs(startTime), startTime);
        } catch (Exception e) {
            log.error("Error executing REGEX check rule: {}", context.getRuleId(), e);
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Execution error: " + e.getMessage(), getElapsedMs(startTime), startTime);
        }
    }

    private String getRegexPattern(RuleContext context, String ruleType) {
        if (context.getRegexPattern() != null && !context.getRegexPattern().isBlank()) {
            return context.getRegexPattern();
        }

        return switch (ruleType) {
            case "REGEX_PHONE" -> PHONE_REGEX;
            case "REGEX_EMAIL" -> EMAIL_REGEX;
            case "REGEX_IDCARD" -> ID_CARD_REGEX;
            case "REGEX" -> throw new IllegalArgumentException("Regex pattern is required for generic REGEX rule");
            default -> throw new IllegalArgumentException("Unsupported regex rule type: " + ruleType);
        };
    }

    private String getFormatName(String ruleType) {
        return switch (ruleType) {
            case "REGEX_PHONE" -> "phone number";
            case "REGEX_EMAIL" -> "email";
            case "REGEX_IDCARD" -> "ID card";
            default -> "pattern";
        };
    }

    private Long getTotalCount(DataSourceAdapter adapter, String tableName) {
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
