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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则执行器抽象基类
 * <p>
 * 提供SQL渲染和结果评估的公共方法，所有具体执行器继承此类。
 */
@Slf4j
public abstract class AbstractRuleExecutor implements RuleExecutor {

    /** 占位符匹配模式 ${xxx} */
    protected static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");

    /** 检查取消标志的频率（每N行检查一次，避免性能损耗） */
    protected static final int CANCEL_CHECK_INTERVAL = 100;

    /** 行计数器，用于周期性检查取消标志 */
    private int rowCounter = 0;

    @Override
    public String getType() {
        return getRuleType();
    }

    /**
     * 获取规则类型 - 子类实现
     */
    protected abstract String getRuleType();

    /**
     * 渲染SQL模板
     *
     * @param rule 规则定义
     * @param adapter 数据源适配器
     * @return 渲染后的SQL
     */
    protected String renderSql(DqcRuleDef rule, DataSourceAdapter adapter) {
        String sqlTemplate = rule.getRuleExpr();
        if (sqlTemplate == null || sqlTemplate.isBlank()) {
            return "";
        }
        String result = sqlTemplate;

        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("table", quoteQualifiedIdentifier(adapter, rule.getTargetTable()));
        replacements.put("table_name", quoteQualifiedIdentifier(adapter, rule.getTargetTable()));
        replacements.put("column", quoteIdentifier(adapter, rule.getTargetColumn()));
        replacements.put("column_name", quoteIdentifier(adapter, rule.getTargetColumn()));
        replacements.put("update_column", quoteIdentifier(adapter, rule.getTargetColumn()));
        replacements.put("compare_table", quoteQualifiedIdentifier(adapter, rule.getCompareTable()));
        replacements.put("compare_column", quoteIdentifier(adapter, rule.getCompareColumn()));
        replacements.put("column_a", quoteIdentifier(adapter, rule.getTargetColumn()));
        replacements.put("column_b", quoteIdentifier(adapter, rule.getCompareColumn()));
        replacements.put("column_c", quoteIdentifier(adapter, resolveThirdColumn(rule)));
        replacements.put("pk_column", quoteIdentifier(adapter, rule.getTargetColumn()));
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
        List<String> unresolved = new java.util.ArrayList<>();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!unresolved.contains(placeholder)) {
                unresolved.add(placeholder);
            }
        }
        if (!unresolved.isEmpty()) {
            throw new IllegalArgumentException("规则SQL存在未绑定占位符: " + String.join(", ", unresolved));
        }
        return result;
    }

    /**
     * 评估执行结果
     *
     * @param result 执行结果对象
     * @param rule 规则定义
     * @return 评估结果
     */
    protected EvaluationResult evaluateResult(Object result, DqcRuleDef rule) {
        if (result == null) {
            return EvaluationResult.fail(null, buildThresholdValue(rule),
                "执行结果为空");
        }

        // 处理时间类型阈值（如更新及时性）
        LocalDateTime resultTime = toLocalDateTime(result);
        if (resultTime != null && (rule.getThresholdMin() != null || rule.getThresholdMax() != null)) {
            BigDecimal ageHours = BigDecimal.valueOf(Duration.between(resultTime, LocalDateTime.now()).toMinutes())
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            boolean pass = isWithinThreshold(ageHours, rule.getThresholdMin(), rule.getThresholdMax());
            String thresholdText = String.format("最近更新时间=%s, 延迟=%s小时, 阈值=%s",
                resultTime,
                ageHours.stripTrailingZeros().toPlainString(),
                buildThresholdText(rule));
            return pass
                ? EvaluationResult.pass(ageHours, thresholdText)
                : EvaluationResult.fail(ageHours, buildThresholdValue(rule), thresholdText);
        }

        // 处理数值类型结果
        BigDecimal actual = toDecimal(result);
        if (actual == null) {
            return EvaluationResult.fail(null, buildThresholdValue(rule),
                "执行结果不是可比较数值");
        }
        boolean pass = isWithinThreshold(actual, rule.getThresholdMin(), rule.getThresholdMax());
        String thresholdText = String.format("实际值=%s, 阈值=%s",
            actual.stripTrailingZeros().toPlainString(),
            buildThresholdText(rule));
        return pass
            ? EvaluationResult.pass(actual, thresholdText)
            : EvaluationResult.fail(actual, buildThresholdValue(rule), thresholdText);
    }

    /**
     * 评估波动检测结果（需要历史基线）
     *
     * @param result 当前执行结果
     * @param rule 规则定义
     * @param previousValue 历史基线值
     * @return 评估结果
     */
    protected EvaluationResult evaluateFluctuation(Object result, DqcRuleDef rule, BigDecimal previousValue) {
        if (result == null) {
            return EvaluationResult.fail(null, rule.getFluctuationThreshold(),
                "执行结果为空");
        }

        BigDecimal currentValue = toDecimal(result);
        if (currentValue == null) {
            return EvaluationResult.fail(null, rule.getFluctuationThreshold(),
                "波动检测结果不是数值");
        }

        // 无历史基线，首轮执行
        if (previousValue == null) {
            log.info("首轮执行，跳过波动检测: ruleId={}", rule.getId());
            return EvaluationResult.skip("首轮执行，无历史基线");
        }

        // 历史基线为0，跳过计算
        if (BigDecimal.ZERO.compareTo(previousValue) == 0) {
            return EvaluationResult.skip("历史基线为0，跳过波动计算");
        }

        // 计算波动百分比
        BigDecimal diffPct = currentValue.subtract(previousValue)
            .abs()
            .multiply(BigDecimal.valueOf(100))
            .divide(previousValue.abs(), 4, RoundingMode.HALF_UP);
        boolean pass = diffPct.compareTo(rule.getFluctuationThreshold()) <= 0;
        String thresholdText = String.format("历史基线=%s, 波动=%s%%, 阈值<=%s%%",
            previousValue.stripTrailingZeros().toPlainString(),
            diffPct.stripTrailingZeros().toPlainString(),
            rule.getFluctuationThreshold().stripTrailingZeros().toPlainString());
        return pass
            ? EvaluationResult.pass(diffPct, thresholdText)
            : EvaluationResult.fail(diffPct, rule.getFluctuationThreshold(), thresholdText);
    }

    /**
     * 从结果集中提取单个值
     *
     * @param resultSet 查询结果集
     * @return 提取的值
     */
    protected Object extractResultValue(List<Map<String, Object>> resultSet) {
        if (resultSet == null || resultSet.isEmpty()) {
            return null;
        }
        Map<String, Object> row = resultSet.get(0);
        if (row == null || row.isEmpty()) {
            return null;
        }

        List<String> preferredKeys = List.of(
            "result", "result_value", "value", "cnt", "count", "total",
            "dup_count", "invalid_count", "enum_violation", "pattern_violation",
            "min_val", "max_val", "avg_val", "anomaly_pct", "violation_count",
            "null_inconsistency", "row_diff", "key_diff", "last_update", "last_update_time",
            "update_delay_hours", "timeliness_hours"
        );
        for (String key : preferredKeys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        for (Object value : row.values()) {
            if (value instanceof Number) {
                return value;
            }
        }
        for (Object value : row.values()) {
            if (toLocalDateTime(value) != null) {
                return value;
            }
        }
        return row.values().iterator().next();
    }

    /**
     * 转换为BigDecimal
     */
    protected BigDecimal toDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal decimal) return decimal;
        if (obj instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 转换为LocalDateTime
     */
    protected LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (obj instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (obj instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        String text = obj.toString();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Timestamp.valueOf(text.trim()).toLocalDateTime();
        } catch (Exception ignored) {
            // ignore
        }
        try {
            return LocalDateTime.parse(text.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 判断值是否在阈值范围内
     */
    protected boolean isWithinThreshold(BigDecimal actual, BigDecimal min, BigDecimal max) {
        if (actual == null) {
            return false;
        }
        if (min != null && actual.compareTo(min) < 0) {
            return false;
        }
        if (max != null && actual.compareTo(max) > 0) {
            return false;
        }
        return true;
    }

    /**
     * 构建阈值文本
     */
    protected String buildThresholdText(DqcRuleDef rule) {
        if (rule.getThresholdMin() == null && rule.getThresholdMax() == null) {
            return "未配置数值阈值";
        }
        return String.format("[%s, %s]",
            rule.getThresholdMin() != null ? rule.getThresholdMin().stripTrailingZeros().toPlainString() : "-∞",
            rule.getThresholdMax() != null ? rule.getThresholdMax().stripTrailingZeros().toPlainString() : "+∞");
    }

    /**
     * 构建阈值数值
     */
    protected BigDecimal buildThresholdValue(DqcRuleDef rule) {
        if (rule.getThresholdMax() != null) {
            return rule.getThresholdMax();
        }
        return rule.getThresholdMin();
    }

    /**
     * 引用标识符
     */
    protected String quoteIdentifier(DataSourceAdapter adapter, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        return adapter.quoteIdentifier(identifier.trim());
    }

    /**
     * 引用限定标识符（表.列格式）
     */
    protected String quoteQualifiedIdentifier(DataSourceAdapter adapter, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String[] parts = identifier.trim().split("\\.");
        return java.util.Arrays.stream(parts)
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .map(adapter::quoteIdentifier)
            .reduce((left, right) -> left + "." + right)
            .orElse(null);
    }

    /**
     * 引用字符串字面量
     */
    protected String quoteStringLiteral(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "'" + value.trim().replace("'", "''") + "'";
    }

    /**
     * 构建枚举值列表
     */
    protected String buildEnumLiteralList(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.contains("'")) {
            return trimmed;
        }
        String[] parts = trimmed.split(",");
        List<String> literals = new java.util.ArrayList<>();
        for (String part : parts) {
            String item = part.trim();
            if (!item.isEmpty()) {
                literals.add("'" + item.replace("'", "''") + "'");
            }
        }
        return literals.isEmpty() ? null : String.join(", ", literals);
    }

    /**
     * 转换为SQL数值
     */
    protected String toSqlNumber(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    /**
     * 返回第一个非空值
     */
    protected BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    /**
     * 解析第三列（跨字段检查）
     */
    protected String resolveThirdColumn(DqcRuleDef rule) {
        if ("CROSS_FIELD".equals(rule.getApplyLevel())) {
            return rule.getCompareTable();
        }
        return null;
    }

    /**
     * 检查是否已取消，周期性调用以减少Redis访问
     * @param cancelChecker 取消检查器
     * @return true表示已取消
     */
    protected boolean checkCancelled(Supplier<Boolean> cancelChecker) {
        if (++rowCounter % CANCEL_CHECK_INTERVAL == 0) {
            rowCounter = 0;
            return cancelChecker.get();
        }
        return false;
    }

    /**
     * 重置计数器（在每个规则执行开始时调用）
     */
    public void resetCancelCounter() {
        this.rowCounter = 0;
    }
}
