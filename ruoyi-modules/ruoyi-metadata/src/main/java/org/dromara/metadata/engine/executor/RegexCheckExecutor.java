package org.dromara.metadata.engine.executor;

import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcRuleDef;

import java.util.List;
import java.util.Map;

/**
 * 正则检查执行器
 * <p>
 * 规则类型: REGEX
 * <p>
 * 检查目标列中不匹配正则表达式的值数量。
 */
@Slf4j
public class RegexCheckExecutor extends AbstractRuleExecutor {

    public static final String TYPE = "REGEX";

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
            log.error("REGEX执行失败: ruleId={}, error={}", rule.getId(), e.getMessage());
            detail.setElapsedMs(System.currentTimeMillis() - start);
            detail.setPassFlag("0");
            detail.setErrorLevel(rule.getErrorLevel());
            detail.setErrorMsg(e.getMessage());
        }
    }
}
