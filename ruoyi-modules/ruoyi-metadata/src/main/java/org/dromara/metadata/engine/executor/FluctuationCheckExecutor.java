package org.dromara.metadata.engine.executor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.mapper.DqcExecutionDetailMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 波动检查执行器
 * <p>
 * 规则类型: FLUCTUATION
 * <p>
 * 检查当前值与历史基线的波动是否在允许范围内。
 * 首轮执行时跳过检测。
 * <p>
 * 元数据驱动：使用 MetadataContext 获取表名、字段名
 */
@Slf4j
@RequiredArgsConstructor
public class FluctuationCheckExecutor extends AbstractRuleExecutor {

    public static final String TYPE = "FLUCTUATION";

    private final DqcExecutionDetailMapper detailMapper;

    @Override
    protected String getRuleType() {
        return TYPE;
    }

    @Override
    public void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter) {
        execute(rule, detail, adapter, MetadataContext.of(null, null, null, null), () -> false);
    }

    @Override
    public void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter,
                        Supplier<Boolean> cancelChecker) {
        execute(rule, detail, adapter, MetadataContext.of(null, null, null, null), cancelChecker);
    }

    @Override
    public void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter,
                        MetadataContext context, Supplier<Boolean> cancelChecker) {
        long start = System.currentTimeMillis();

        // 渲染SQL（使用元数据上下文）
        String sql = renderSql(rule, adapter, context);
        detail.setExecuteSql(sql);

        try {
            // 执行查询
            List<Map<String, Object>> resultSet = adapter.executeQuery(sql);
            Object result = extractResultValue(resultSet);

            detail.setActualValue(result != null ? result.toString() : null);
            detail.setElapsedMs(System.currentTimeMillis() - start);

            // 获取历史基线
            BigDecimal previousValue = findPreviousResultValue(rule.getId());

            // 评估波动
            var evaluation = evaluateFluctuation(result, rule, previousValue);
            detail.setPassFlag(evaluation.pass() ? "1" : "0");
            detail.setResultValue(evaluation.resultValue());
            detail.setThresholdValue(evaluation.thresholdValue() != null ? evaluation.thresholdValue().toPlainString() : null);

            if (!evaluation.pass() && evaluation.message() != null
                && !evaluation.message().contains("首轮执行") && !evaluation.message().contains("历史基线为0")) {
                detail.setErrorLevel(rule.getErrorLevel());
                detail.setErrorMsg(evaluation.message());
            } else if (evaluation.message() != null && evaluation.message().contains("首轮执行")) {
                // 首轮执行，记录消息但不标记为失败
                detail.setErrorMsg(evaluation.message());
            }

        } catch (Exception e) {
            log.error("FLUCTUATION执行失败: ruleId={}, error={}", rule.getId(), e.getMessage());
            detail.setElapsedMs(System.currentTimeMillis() - start);
            detail.setPassFlag("0");
            detail.setErrorLevel(rule.getErrorLevel());
            detail.setErrorMsg(e.getMessage());
        }
    }

    /**
     * 查找上一轮执行的结果值作为历史基线
     */
    private BigDecimal findPreviousResultValue(Long ruleId) {
        List<DqcExecutionDetail> details = detailMapper.selectList(
            Wrappers.<DqcExecutionDetail>lambdaQuery()
                .eq(DqcExecutionDetail::getRuleId, ruleId)
                .isNotNull(DqcExecutionDetail::getResultValue)
                .orderByDesc(DqcExecutionDetail::getId)
                .last("LIMIT 1")
        );
        if (details.isEmpty()) {
            return null;
        }
        return details.get(0).getResultValue();
    }
}