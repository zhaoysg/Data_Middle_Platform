package org.dromara.metadata.engine.executor;

import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.vo.EvaluationResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 跨字段检查执行器
 * <p>
 * 规则类型: CROSS_FIELD
 * <p>
 * 检查多个字段之间的关系是否满足预期（如两列之和等于第三列）。
 */
@Slf4j
public class CrossFieldCheckExecutor extends AbstractRuleExecutor {

    public static final String TYPE = "CROSS_FIELD";

    @Override
    protected String getRuleType() {
        return TYPE;
    }

    @Override
    public void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter) {
        long start = System.currentTimeMillis();

        // 渲染SQL
        String sql = renderSql(rule, adapter);
        detail.setExecuteSql(sql);

        try {
            // 执行查询
            List<Map<String, Object>> resultSet = adapter.executeQuery(sql);
            Object result = extractResultValue(resultSet);

            detail.setActualValue(result != null ? result.toString() : null);
            detail.setElapsedMs(System.currentTimeMillis() - start);

            // 评估结果
            var evaluation = evaluateResult(result, rule);
            detail.setPassFlag(evaluation.pass() ? "1" : "0");
            detail.setResultValue(evaluation.resultValue());
            detail.setThresholdValue(evaluation.thresholdValue() != null ? evaluation.thresholdValue().toPlainString() : null);

            if (!evaluation.pass()) {
                detail.setErrorLevel(rule.getErrorLevel());
                detail.setErrorMsg(evaluation.message());
            }

        } catch (Exception e) {
            log.error("CROSS_FIELD执行失败: ruleId={}, error={}", rule.getId(), e.getMessage());
            detail.setElapsedMs(System.currentTimeMillis() - start);
            detail.setPassFlag("0");
            detail.setErrorLevel(rule.getErrorLevel());
            detail.setErrorMsg(e.getMessage());
        }
    }

    /**
     * 覆盖父类方法，添加CROSS_FIELD特有的占位符
     */
    @Override
    protected String renderSql(DqcRuleDef rule, DataSourceAdapter adapter) {
        String sqlTemplate = rule.getRuleExpr();
        if (sqlTemplate == null || sqlTemplate.isBlank()) {
            return "";
        }
        String result = sqlTemplate;

        java.util.LinkedHashMap<String, String> replacements = new java.util.LinkedHashMap<>();
        replacements.put("table", quoteQualifiedIdentifier(adapter, rule.getTargetTable()));
        replacements.put("table_name", quoteQualifiedIdentifier(adapter, rule.getTargetTable()));
        replacements.put("column_a", quoteIdentifier(adapter, rule.getTargetColumn()));
        replacements.put("column_b", quoteIdentifier(adapter, rule.getCompareColumn()));
        replacements.put("column_c", quoteIdentifier(adapter, rule.getCompareTable())); // compareTable存的是第三列
        replacements.put("compare_table", quoteQualifiedIdentifier(adapter, rule.getCompareTable()));
        replacements.put("compare_column", quoteIdentifier(adapter, rule.getCompareColumn()));
        replacements.put("min_value", toSqlNumber(rule.getThresholdMin()));
        replacements.put("max_value", toSqlNumber(rule.getThresholdMax()));
        replacements.put("threshold_min", toSqlNumber(rule.getThresholdMin()));
        replacements.put("threshold_max", toSqlNumber(rule.getThresholdMax()));
        replacements.put("threshold_pct", toSqlNumber(rule.getThresholdMin()));
        replacements.put("z_threshold", toSqlNumber(rule.getThresholdMin()));

        for (java.util.Map.Entry<String, String> entry : replacements.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }

        java.util.regex.Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        java.util.List<String> unresolved = new java.util.ArrayList<>();
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
}
