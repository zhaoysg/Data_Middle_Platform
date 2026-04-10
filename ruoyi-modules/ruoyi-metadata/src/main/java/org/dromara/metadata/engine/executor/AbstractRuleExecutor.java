package org.dromara.metadata.engine.executor;

import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.vo.EvaluationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class AbstractRuleExecutor implements RuleExecutor {

    protected static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");
    protected static final int CANCEL_CHECK_INTERVAL = 100;
    private int rowCounter = 0;

    @Override
    public String getType() {
        return getRuleType();
    }

    protected abstract String getRuleType();

    protected String renderSql(DqcRuleDef rule, DataSourceAdapter adapter, MetadataContext context) {
        String sqlTemplate = rule.getRuleExpr();
        if (sqlTemplate == null || sqlTemplate.isBlank()) {
            return "";
        }
        String result = sqlTemplate;

        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("table", quoteQualifiedIdentifier(adapter, context.getTableName()));
        replacements.put("table_name", quoteQualifiedIdentifier(adapter, context.getTableName()));
        replacements.put("column", quoteIdentifier(adapter, context.getColumnName()));
        replacements.put("column_name", quoteIdentifier(adapter, context.getColumnName()));
        replacements.put("update_column", quoteIdentifier(adapter, context.getColumnName()));
        replacements.put("compare_table", quoteQualifiedIdentifier(adapter, context.getCompareTableName()));
        replacements.put("compare_column", quoteIdentifier(adapter, context.getCompareColumnName()));
        replacements.put("column_a", quoteIdentifier(adapter, context.getColumnName()));
        replacements.put("column_b", quoteIdentifier(adapter, context.getCompareColumnName()));
        replacements.put("column_c", quoteIdentifier(adapter, resolveThirdColumn(context)));
        replacements.put("pk_column", quoteIdentifier(adapter, context.getColumnName()));
        replacements.put("pattern", quoteStringLiteral(rule.getRegexPattern()));
        replacements.put("enum_values", buildEnumLiteralList(rule.getRegexPattern()));
        replacements.put("min_value", toSqlNumber(rule.getThresholdMin()));
        replacements.put("max_value", toSqlNumber(rule.getThresholdMax()));
        replacements.put("min_len", toSqlNumber(rule.getThresholdMin()));
        replacements.put("max_len", toSqlNumber(rule.getThresholdMax()));
        replacements.put("z_threshold", toSqlNumber(firstNonNull(rule.getThresholdMax(), rule.getThresholdMin())));
        replacements.put("threshold_min", toSqlNumber(rule.getThresholdMin()));
        replacements.put("threshold_max", toSqlNumber(rule.getThresholdMax()));
        replacements.put("threshold_pct", toSqlNumber(firstNonNull(rule.getThresholdMax(), rule.getThresholdMin())));
        replacements.put("threshold_hours", toSqlNumber(firstNonNull(rule.getThresholdMax(), rule.getThresholdMin())));
        replacements.put("fluctuation_threshold", toSqlNumber(rule.getFluctuationThreshold()));

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        List<String> unresolved = new ArrayList<>();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!unresolved.contains(placeholder)) {
                unresolved.add(placeholder);
            }
        }
        if (!unresolved.isEmpty()) {
            throw new IllegalArgumentException("Unresolved placeholders in rule SQL: " + String.join(", ", unresolved));
        }
        return result;
    }

    protected EvaluationResult evaluateResult(Object result, DqcRuleDef rule) {
        if (result == null) {
            return EvaluationResult.fail(null, buildThresholdValue(rule), "Result is null");
        }

        LocalDateTime resultTime = toLocalDateTime(result);
        if (resultTime != null && (rule.getThresholdMin() != null || rule.getThresholdMax() != null)) {
            BigDecimal ageHours = BigDecimal.valueOf(Duration.between(resultTime, LocalDateTime.now()).toMinutes())
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            boolean pass = isWithinThreshold(ageHours, rule.getThresholdMin(), rule.getThresholdMax());
            String thresholdText = String.format("LastUpdate=%s, Delay=%sH, Threshold=%s",
                resultTime, ageHours.stripTrailingZeros().toPlainString(), buildThresholdText(rule));
            return pass ? EvaluationResult.pass(ageHours, thresholdText)
                        : EvaluationResult.fail(ageHours, buildThresholdValue(rule), thresholdText);
        }

        BigDecimal actual = toDecimal(result);
        if (actual == null) {
            return EvaluationResult.fail(null, buildThresholdValue(rule), "Result is not a comparable value");
        }
        boolean pass = isWithinThreshold(actual, rule.getThresholdMin(), rule.getThresholdMax());
        String thresholdText = String.format("Actual=%s, Threshold=%s",
            actual.stripTrailingZeros().toPlainString(), buildThresholdText(rule));
        return pass ? EvaluationResult.pass(actual, thresholdText)
                    : EvaluationResult.fail(actual, buildThresholdValue(rule), thresholdText);
    }

    protected EvaluationResult evaluateFluctuation(Object result, DqcRuleDef rule, BigDecimal previousValue) {
        if (result == null) {
            return EvaluationResult.fail(null, rule.getFluctuationThreshold(), "Result is null");
        }

        BigDecimal currentValue = toDecimal(result);
        if (currentValue == null) {
            return EvaluationResult.fail(null, rule.getFluctuationThreshold(), "Result is not a number");
        }

        if (previousValue == null) {
            log.info("First execution, skip fluctuation check: ruleId={}", rule.getId());
            return EvaluationResult.skip("First execution, no baseline");
        }

        if (BigDecimal.ZERO.compareTo(previousValue) == 0) {
            return EvaluationResult.skip("Baseline is zero, skip calculation");
        }

        BigDecimal diffPct = currentValue.subtract(previousValue)
            .abs().multiply(BigDecimal.valueOf(100))
            .divide(previousValue.abs(), 4, RoundingMode.HALF_UP);
        boolean pass = diffPct.compareTo(rule.getFluctuationThreshold()) <= 0;
        String thresholdText = String.format("Baseline=%s, Fluctuation=%s%%, Threshold<=%s%%",
            previousValue.stripTrailingZeros().toPlainString(),
            diffPct.stripTrailingZeros().toPlainString(),
            rule.getFluctuationThreshold().stripTrailingZeros().toPlainString());
        return pass ? EvaluationResult.pass(diffPct, thresholdText)
                    : EvaluationResult.fail(diffPct, rule.getFluctuationThreshold(), thresholdText);
    }

    protected Object extractResultValue(List<Map<String, Object>> resultSet) {
        if (resultSet == null || resultSet.isEmpty()) return null;
        Map<String, Object> row = resultSet.get(0);
        if (row == null || row.isEmpty()) return null;

        List<String> preferredKeys = List.of(
            "result", "result_value", "value", "cnt", "count", "total",
            "dup_count", "invalid_count", "enum_violation", "pattern_violation",
            "min_val", "max_val", "avg_val", "anomaly_pct", "violation_count",
            "null_inconsistency", "row_diff", "key_diff", "last_update", "last_update_time",
            "update_delay_hours", "timeliness_hours"
        );
        for (String key : preferredKeys) {
            if (row.containsKey(key)) return row.get(key);
        }
        for (Object value : row.values()) {
            if (value instanceof Number) return value;
        }
        for (Object value : row.values()) {
            if (toLocalDateTime(value) != null) return value;
        }
        return row.values().iterator().next();
    }

    protected BigDecimal toDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    protected LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof LocalDateTime) return (LocalDateTime) obj;
        if (obj instanceof Timestamp) return ((Timestamp) obj).toLocalDateTime();
        if (obj instanceof OffsetDateTime) return ((OffsetDateTime) obj).toLocalDateTime();
        String text = obj.toString();
        if (text == null || text.isBlank()) return null;
        try {
            return Timestamp.valueOf(text.trim()).toLocalDateTime();
        } catch (Exception ignored) { }
        try {
            return LocalDateTime.parse(text.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    protected boolean isWithinThreshold(BigDecimal actual, BigDecimal min, BigDecimal max) {
        if (actual == null) return false;
        if (min != null && actual.compareTo(min) < 0) return false;
        if (max != null && actual.compareTo(max) > 0) return false;
        return true;
    }

    protected String buildThresholdText(DqcRuleDef rule) {
        if (rule.getThresholdMin() == null && rule.getThresholdMax() == null) {
            return "No threshold configured";
        }
        return String.format("[%s, %s]",
            rule.getThresholdMin() != null ? rule.getThresholdMin().stripTrailingZeros().toPlainString() : "-inf",
            rule.getThresholdMax() != null ? rule.getThresholdMax().stripTrailingZeros().toPlainString() : "+inf");
    }

    protected BigDecimal buildThresholdValue(DqcRuleDef rule) {
        return rule.getThresholdMax() != null ? rule.getThresholdMax() : rule.getThresholdMin();
    }

    protected String quoteIdentifier(DataSourceAdapter adapter, String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        return adapter.quoteIdentifier(identifier.trim());
    }

    protected String quoteQualifiedIdentifier(DataSourceAdapter adapter, String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        String[] parts = identifier.trim().split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (!part.isEmpty()) {
                if (i > 0) sb.append(".");
                sb.append(adapter.quoteIdentifier(part));
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    protected String quoteStringLiteral(String value) {
        if (value == null || value.isBlank()) return null;
        return "'" + value.trim().replace("'", "''") + "'";
    }

    protected String buildEnumLiteralList(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.contains("'")) return trimmed;
        String[] parts = trimmed.split(",");
        List<String> literals = new ArrayList<>();
        for (String part : parts) {
            String item = part.trim();
            if (!item.isEmpty()) {
                literals.add("'" + item.replace("'", "''") + "'");
            }
        }
        return literals.isEmpty() ? null : String.join(", ", literals);
    }

    protected String toSqlNumber(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    protected BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    protected String resolveThirdColumn(MetadataContext context) {
        if (context != null && context.getCompareTableName() != null) {
            return context.getCompareTableName();
        }
        return null;
    }

    protected boolean checkCancelled(Supplier<Boolean> cancelChecker) {
        if (++rowCounter % CANCEL_CHECK_INTERVAL == 0) {
            rowCounter = 0;
            return cancelChecker.get();
        }
        return false;
    }

    public void resetCancelCounter() {
        this.rowCounter = 0;
    }
}
