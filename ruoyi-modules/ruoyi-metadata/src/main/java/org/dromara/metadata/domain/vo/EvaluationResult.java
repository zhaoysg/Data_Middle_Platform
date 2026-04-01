package org.dromara.metadata.domain.vo;

import java.math.BigDecimal;

/**
 * 规则评估结果记录
 *
 * @param pass 是否通过
 * @param resultValue 结果值
 * @param thresholdValue 阈值/比较值
 * @param message 评估消息
 */
public record EvaluationResult(
    boolean pass,
    BigDecimal resultValue,
    BigDecimal thresholdValue,
    String message
) {
    /**
     * 创建成功结果
     */
    public static EvaluationResult pass(BigDecimal resultValue, String message) {
        return new EvaluationResult(true, resultValue, null, message);
    }

    /**
     * 创建失败结果
     */
    public static EvaluationResult fail(BigDecimal resultValue, BigDecimal thresholdValue, String message) {
        return new EvaluationResult(false, resultValue, thresholdValue, message);
    }

    /**
     * 创建跳过结果（首轮执行等）
     */
    public static EvaluationResult skip(String message) {
        return new EvaluationResult(true, null, null, message);
    }
}
