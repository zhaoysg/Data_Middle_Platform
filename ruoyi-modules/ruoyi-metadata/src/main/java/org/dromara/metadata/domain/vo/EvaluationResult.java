package org.dromara.metadata.domain.vo;

import java.math.BigDecimal;

/**
 * Evaluation result record
 *
 * @param pass pass flag
 * @param resultValue result value
 * @param thresholdValue threshold value
 * @param message message
 */
public record EvaluationResult(
    boolean pass,
    BigDecimal resultValue,
    BigDecimal thresholdValue,
    String message
) {
    /**
     * Create pass result
     */
    public static EvaluationResult pass(BigDecimal resultValue, String message) {
        return new EvaluationResult(true, resultValue, null, message);
    }

    /**
     * Create fail result
     */
    public static EvaluationResult fail(BigDecimal resultValue, BigDecimal thresholdValue, String message) {
        return new EvaluationResult(false, resultValue, thresholdValue, message);
    }

    /**
     * Create skip result (for first execution)
     */
    public static EvaluationResult skip(String message) {
        return new EvaluationResult(true, null, null, message);
    }
}
