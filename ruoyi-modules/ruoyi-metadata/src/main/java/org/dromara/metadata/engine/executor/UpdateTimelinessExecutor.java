package org.dromara.metadata.engine.executor;

import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.vo.EvaluationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 更新及时性检查执行器
 * <p>
 * 规则类型: UPDATE_TIMELINESS
 * <p>
 * 检查数据最后一次更新时间距离现在的时长是否在允许范围内。
 * 通常用于监控数据管道或ETL任务的执行频率。
 * <p>
 * 元数据驱动：使用 MetadataContext 获取表名
 */
@Slf4j
public class UpdateTimelinessExecutor extends AbstractRuleExecutor {

    public static final String TYPE = "UPDATE_TIMELINESS";

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

            // 评估更新时间延迟
            var evaluation = evaluateTimeliness(result, rule);
            detail.setPassFlag(evaluation.pass() ? "1" : "0");
            detail.setResultValue(evaluation.resultValue());
            detail.setThresholdValue(evaluation.thresholdValue() != null ? evaluation.thresholdValue().toPlainString() : null);

            if (!evaluation.pass()) {
                detail.setErrorLevel(rule.getErrorLevel());
                detail.setErrorMsg(evaluation.message());
            }

        } catch (Exception e) {
            log.error("UPDATE_TIMELINESS执行失败: ruleId={}, error={}", rule.getId(), e.getMessage());
            detail.setElapsedMs(System.currentTimeMillis() - start);
            detail.setPassFlag("0");
            detail.setErrorLevel(rule.getErrorLevel());
            detail.setErrorMsg(e.getMessage());
        }
    }

    /**
     * 评估数据更新及时性
     *
     * @param result 查询结果（通常是最后更新时间）
     * @param rule 规则定义
     * @return 评估结果
     */
    protected EvaluationResult evaluateTimeliness(Object result, DqcRuleDef rule) {
        if (result == null) {
            return EvaluationResult.fail(null, buildThresholdValue(rule),
                "查询结果为空，无法判断更新及时性");
        }

        LocalDateTime lastUpdateTime = toLocalDateTime(result);
        if (lastUpdateTime == null) {
            // 如果结果不是时间类型，尝试作为数值处理（小时数）
            BigDecimal hours = toDecimal(result);
            if (hours == null) {
                return EvaluationResult.fail(null, buildThresholdValue(rule),
                    "无法解析更新时间");
            }
            // 数值类型直接判断是否在阈值内
            boolean pass = isWithinThreshold(hours, rule.getThresholdMin(), rule.getThresholdMax());
            String thresholdText = String.format("延迟=%s小时, 阈值=%s",
                hours.stripTrailingZeros().toPlainString(),
                buildThresholdText(rule));
            return pass
                ? EvaluationResult.pass(hours, thresholdText)
                : EvaluationResult.fail(hours, buildThresholdValue(rule), thresholdText);
        }

        // 时间类型：计算距离现在的小时数
        Duration duration = Duration.between(lastUpdateTime, LocalDateTime.now());
        BigDecimal delayHours = BigDecimal.valueOf(duration.toMinutes())
            .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        boolean pass = isWithinThreshold(delayHours, rule.getThresholdMin(), rule.getThresholdMax());
        String thresholdText = String.format("最后更新时间=%s, 延迟=%s小时, 阈值=%s",
            lastUpdateTime,
            delayHours.stripTrailingZeros().toPlainString(),
            buildThresholdText(rule));

        return pass
            ? EvaluationResult.pass(delayHours, thresholdText)
            : EvaluationResult.fail(delayHours, buildThresholdValue(rule), thresholdText);
    }
}