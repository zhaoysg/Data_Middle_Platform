package com.bagdatahouse.dqc.engine.scorer;

import com.bagdatahouse.core.entity.DqcExecutionDetail;
import com.bagdatahouse.core.entity.DqcQualityScore;
import com.bagdatahouse.core.mapper.DqcQualityScoreMapper;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.entity.DqDatasource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 质量评分计算器
 *
 * 支持华为六维质量评分模型：
 * - 完整性（COMPLETENESS）：数据完整程度，包括空值率、行数等
 * - 唯一性（UNIQUENESS）：数据唯一程度，包括主键重复、唯一约束等
 * - 准确性（ACCURACY）：数据准确程度，包括数值范围、格式校验等
 * - 一致性（CONSISTENCY）：数据一致程度，包括跨表一致性、时间戳一致性等
 * - 及时性（TIMELINESS）：数据更新及时程度，包括增量时间、数据延迟等
 * - 有效性（VALIDITY）：数据有效程度，包括格式校验、正则匹配等
 *
 * 评分算法特点：
 * 1. 基于规则实际偏差计算精确评分，而非简单的通过/失败二值
 * 2. 支持维度权重配置，可根据业务需求调整各维度占比
 * 3. 考虑强规则/弱规则的权重影响
 * 4. 支持评分趋势分析
 */
@Component
public class QualityScorer {

    private static final Logger log = LoggerFactory.getLogger(QualityScorer.class);

    @Autowired
    private DqcQualityScoreMapper qualityScoreMapper;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 维度常量
    public static final String DIM_COMPLETENESS = "COMPLETENESS";
    public static final String DIM_UNIQUENESS = "UNIQUENESS";
    public static final String DIM_ACCURACY = "ACCURACY";
    public static final String DIM_CONSISTENCY = "CONSISTENCY";
    public static final String DIM_TIMELINESS = "TIMELINESS";
    public static final String DIM_VALIDITY = "VALIDITY";
    // 阶段三-T3: 敏感字段合规率维度
    public static final String DIM_SENSITIVITY = "SENSITIVITY";

    // 维度权重配置（默认权重，可根据业务需求调整）
    private static final Map<String, BigDecimal> DEFAULT_DIMENSION_WEIGHTS = new LinkedHashMap<>();
    static {
        DEFAULT_DIMENSION_WEIGHTS.put(DIM_COMPLETENESS, new BigDecimal("0.20")); // 完整性权重 20%
        DEFAULT_DIMENSION_WEIGHTS.put(DIM_UNIQUENESS, new BigDecimal("0.20"));   // 唯一性权重 20%
        DEFAULT_DIMENSION_WEIGHTS.put(DIM_ACCURACY, new BigDecimal("0.20"));    // 准确性权重 20%
        DEFAULT_DIMENSION_WEIGHTS.put(DIM_CONSISTENCY, new BigDecimal("0.15")); // 一致性权重 15%
        DEFAULT_DIMENSION_WEIGHTS.put(DIM_TIMELINESS, new BigDecimal("0.10"));   // 及时性权重 10%
        DEFAULT_DIMENSION_WEIGHTS.put(DIM_VALIDITY, new BigDecimal("0.10"));     // 有效性权重 10%
        DEFAULT_DIMENSION_WEIGHTS.put(DIM_SENSITIVITY, new BigDecimal("0.05")); // 敏感字段合规率权重 5%
    }

    // 强规则权重系数（强规则失败扣分更多）
    private static final BigDecimal STRONG_RULE_PENALTY_FACTOR = new BigDecimal("1.5");
    private static final BigDecimal WEAK_RULE_PENALTY_FACTOR = BigDecimal.ONE;

    // 评分等级阈值
    private static final int EXCELLENT_THRESHOLD = 90;
    private static final int GOOD_THRESHOLD = 70;
    private static final int WARNING_THRESHOLD = 60;

    /**
     * 计算并保存质量评分
     */
    @Transactional
    public void calculateAndSaveScore(Long executionId, List<DqcExecutionDetail> details) {
        if (details == null || details.isEmpty()) {
            log.warn("No execution details to calculate score for executionId={}", executionId);
            return;
        }

        DqcExecutionDetail firstDetail = details.get(0);
        Long targetDsId = firstDetail.getTargetDsId();
        String targetTable = firstDetail.getTargetTable();

        DqDatasource datasource = datasourceMapper.selectById(targetDsId);
        if (datasource == null) {
            log.warn("Datasource not found for dsId={}", targetDsId);
            return;
        }

        // 计算评分
        ScoreBreakdown breakdown = calculateBreakdown(details);

        // 统计规则执行情况
        int total = details.size();
        int passed = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int failed = (int) details.stream().filter(d -> "FAILED".equals(d.getStatus())).count();

        BigDecimal rulePassRate = total > 0
                ? BigDecimal.valueOf(100.0 * passed / total).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal displayOverall = breakdown.healthScore() != null
                ? breakdown.healthScore()
                : breakdown.overallScore();

        DqcQualityScore qualityScore = DqcQualityScore.builder()
                .checkDate(LocalDateTime.now())
                .targetDsId(targetDsId)
                .targetTable(targetTable)
                .layerCode(datasource.getDataLayer())
                .deptId(datasource.getDeptId())
                .completenessScore(breakdown.dimensionScores().get(DIM_COMPLETENESS))
                .uniquenessScore(breakdown.dimensionScores().get(DIM_UNIQUENESS))
                .accuracyScore(breakdown.dimensionScores().get(DIM_ACCURACY))
                .consistencyScore(breakdown.dimensionScores().get(DIM_CONSISTENCY))
                .timelinessScore(breakdown.dimensionScores().get(DIM_TIMELINESS))
                .validityScore(breakdown.dimensionScores().get(DIM_VALIDITY))
                .overallScore(displayOverall)
                .rulePassRate(rulePassRate)
                .ruleTotalCount(total)
                .rulePassCount(passed)
                .ruleFailCount(failed)
                .executionId(executionId)
                .createTime(LocalDateTime.now())
                .build();

        qualityScoreMapper.insert(qualityScore);
        log.info("Saved quality score for executionId={}: overallScore={}, passRate={}",
                executionId, displayOverall, rulePassRate);
    }

    /**
     * 计算评分明细
     *
     * 评分算法说明：
     * 1. 将规则按维度分组
     * 2. 计算每个维度的精确评分（考虑实际偏差值）
     * 3. 加权总分仅基于已评估（已绑定规则）的维度；未绑定规则的维度不参与加权
     * 4. 提供执行健康度（healthScore）：若有任意 FAILED 则等于规则通过率，不得虚高
     */
    public ScoreBreakdown calculateBreakdown(List<DqcExecutionDetail> details) {
        if (details == null || details.isEmpty()) {
            List<String> allDims = getAllDimensions();
            return new ScoreBreakdown(
                    BigDecimal.valueOf(100),
                    Collections.emptyMap(),
                    new ScoreStats(0, 0, 0, 0),
                    Collections.emptyList(),
                    allDims,
                    BigDecimal.valueOf(100)
            );
        }

        // 按维度分组
        Map<String, List<DqcExecutionDetail>> dimDetails = new HashMap<>();
        for (DqcExecutionDetail detail : details) {
            String dim = getDimension(detail.getRuleType());
            dimDetails.computeIfAbsent(dim, k -> new ArrayList<>()).add(detail);
        }

        List<String> evaluatedDims = new ArrayList<>();
        List<String> unevaluatedDims = new ArrayList<>();

        // 计算每个维度的评分
        Map<String, BigDecimal> dimensionScores = new LinkedHashMap<>();
        for (String dim : getAllDimensions()) {
            List<DqcExecutionDetail> dimRules = dimDetails.get(dim);
            if (dimRules != null && !dimRules.isEmpty()) {
                dimensionScores.put(dim, calculateDimensionScore(dimRules, dim));
                evaluatedDims.add(dim);
            } else {
                dimensionScores.put(dim, null); // 明确标记为未评估
                unevaluatedDims.add(dim);
            }
        }

        // 加权总分仅基于已评估维度
        BigDecimal overallScore = calculateWeightedOverallScore(dimensionScores, evaluatedDims);

        // 统计信息
        int total = details.size();
        int passed = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int failed = (int) details.stream().filter(d -> "FAILED".equals(d.getStatus())).count();
        int skipped = (int) details.stream().filter(d -> "SKIPPED".equals(d.getStatus())).count();

        // 执行健康度：有失败 → 健康度不超过规则通过率；否则等于总分
        BigDecimal healthScore;
        double passRate = total > 0 ? (double) passed / total * 100 : 0;
        if (failed > 0) {
            // 健康度 = 通过率，保证失败时不会虚高
            healthScore = BigDecimal.valueOf(passRate).setScale(2, RoundingMode.HALF_UP);
        } else {
            healthScore = overallScore;
        }

        return new ScoreBreakdown(
                overallScore.setScale(2, RoundingMode.HALF_UP),
                dimensionScores,
                new ScoreStats(total, passed, failed, skipped),
                evaluatedDims,
                unevaluatedDims,
                healthScore
        );
    }

    /**
     * 计算单个维度的评分
     *
     * 算法说明：
     * 1. 遍历该维度下的所有规则
     * 2. 根据规则类型计算精确评分：
     *    - 空值检查：基于空值率与阈值的偏差计算
     *    - 唯一性检查：基于重复率计算
     *    - 阈值检查：基于实际值与阈值的接近程度计算
     *    - 波动检查：基于波动百分比计算
     * 3. 强规则失败扣分更多
     * 4. 返回该维度的加权平均分
     */
    private BigDecimal calculateDimensionScore(List<DqcExecutionDetail> details, String dimension) {
        if (details == null || details.isEmpty()) {
            return BigDecimal.valueOf(100);
        }

        // 阶段三-T3: SENSITIVITY 维度特殊计算——基于敏感字段合规率
        if (DIM_SENSITIVITY.equals(dimension)) {
            return calculateSensitivityComplianceScore(details);
        }

        List<BigDecimal> scores = new ArrayList<>();
        List<BigDecimal> weights = new ArrayList<>();

        for (DqcExecutionDetail detail : details) {
            BigDecimal ruleScore = calculateRuleScore(detail);
            BigDecimal weight = getRuleWeight(detail);

            scores.add(ruleScore);
            weights.add(weight);
        }

        // 计算加权平均
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (int i = 0; i < scores.size(); i++) {
            weightedSum = weightedSum.add(scores.get(i).multiply(weights.get(i)));
            totalWeight = totalWeight.add(weights.get(i));
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        return weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算单条规则的评分
     *
     * 算法说明：
     * 1. 如果规则成功执行且通过，返回满分 100
     * 2. 如果规则被跳过，返回 100
     * 3. 如果规则失败，根据规则类型计算偏差分：
     *    - 准确性维度：基于实际值与阈值的接近程度
     *    - 其他维度：基于错误数量与总数的比率
     */
    private BigDecimal calculateRuleScore(DqcExecutionDetail detail) {
        String status = detail.getStatus();

        // 成功或跳过返回满分
        if ("SUCCESS".equals(status) || "SKIPPED".equals(status)) {
            return BigDecimal.valueOf(100);
        }

        // 计算失败情况下的精确评分
        BigDecimal penaltyFactor = getRulePenaltyFactor(detail);

        Long totalCount = detail.getTotalCount();
        Long errorCount = detail.getErrorCount();

        if (totalCount == null || totalCount == 0) {
            // 没有数据时返回 0
            return BigDecimal.ZERO;
        }

        // 计算错误率
        double errorRate = (double) errorCount / totalCount;

        // 根据维度类型采用不同的评分策略
        String dimension = getDimension(detail.getRuleType());
        double score;

        switch (dimension) {
            case DIM_ACCURACY:
                // 准确性维度：根据实际值与阈值的接近程度计算
                score = calculateAccuracyScore(detail, errorRate);
                break;
            case DIM_COMPLETENESS:
                // 完整性维度：基于空值率计算
                score = calculateCompletenessScore(detail, errorRate);
                break;
            case DIM_UNIQUENESS:
                // 唯一性维度：基于重复率计算
                score = calculateUniquenessScore(detail, errorRate);
                break;
            case DIM_CONSISTENCY:
                // 一致性维度：基于不一致率计算
                score = calculateConsistencyScore(detail, errorRate);
                break;
            case DIM_TIMELINESS:
                // 及时性维度：基于延迟程度计算
                score = calculateTimelinessScore(detail);
                break;
            case DIM_VALIDITY:
                // 有效性维度：基于无效数据率计算
                score = calculateValidityScore(detail, errorRate);
                break;
            default:
                // 默认基于错误率计算
                score = 100 * (1 - errorRate);
        }

        // 应用权重惩罚
        score = score / penaltyFactor.doubleValue();

        return BigDecimal.valueOf(Math.max(0, Math.min(100, score))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算准确性维度评分
     * 根据实际值与阈值的接近程度计算
     */
    private double calculateAccuracyScore(DqcExecutionDetail detail, double errorRate) {
        BigDecimal actualValue = detail.getActualValue();
        BigDecimal thresholdMin = detail.getThresholdMin();
        BigDecimal thresholdMax = detail.getThresholdMax();

        if (actualValue == null || (thresholdMin == null && thresholdMax == null)) {
            return 100 * (1 - errorRate);
        }

        // 在阈值范围内，评分基于与理想值的距离
        if (thresholdMin != null && thresholdMax != null) {
            BigDecimal midPoint = thresholdMin.add(thresholdMax).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            BigDecimal range = thresholdMax.subtract(thresholdMin);

            if (range.compareTo(BigDecimal.ZERO) == 0) {
                // 固定阈值
                if (actualValue.compareTo(thresholdMin) >= 0) {
                    return 100.0;
                } else {
                    return 100 * (1 - errorRate);
                }
            }

            // 计算实际值与理想值的偏差
            BigDecimal deviation = actualValue.subtract(midPoint).abs();
            double deviationRatio = deviation.divide(range, 4, RoundingMode.HALF_UP).doubleValue();

            // 偏差越小评分越高
            return Math.max(0, 100 * (1 - deviationRatio * errorRate));
        } else if (thresholdMin != null) {
            // 仅最小值限制
            if (actualValue.compareTo(thresholdMin) >= 0) {
                double excess = actualValue.subtract(thresholdMin).doubleValue();
                double maxExcess = Math.max(thresholdMin.doubleValue() * 0.5, 1);
                return Math.max(0, 100 * (1 - excess / maxExcess * errorRate));
            } else {
                return 100 * (1 - errorRate);
            }
        } else if (thresholdMax != null) {
            // 仅最大值限制
            if (actualValue.compareTo(thresholdMax) <= 0) {
                double deficit = thresholdMax.subtract(actualValue).doubleValue();
                double maxDeficit = Math.max(thresholdMax.doubleValue() * 0.5, 1);
                return Math.max(0, 100 * (1 - deficit / maxDeficit * errorRate));
            } else {
                return 100 * (1 - errorRate);
            }
        }

        return 100 * (1 - errorRate);
    }

    /**
     * 计算完整性维度评分
     * 基于空值率计算
     */
    private double calculateCompletenessScore(DqcExecutionDetail detail, double errorRate) {
        // 空值率 = errorCount / totalCount
        // 评分 = 100 * (1 - 空值率)
        return 100 * (1 - errorRate);
    }

    /**
     * 计算唯一性维度评分
     * 基于重复率计算
     */
    private double calculateUniquenessScore(DqcExecutionDetail detail, double errorRate) {
        // 重复率 = errorCount / totalCount
        // 评分 = 100 * (1 - 重复率)
        return 100 * (1 - errorRate);
    }

    /**
     * 计算一致性维度评分
     * 基于不一致率计算
     */
    private double calculateConsistencyScore(DqcExecutionDetail detail, double errorRate) {
        return 100 * (1 - errorRate);
    }

    /**
     * 计算及时性维度评分
     * 基于数据更新时间与当前时间的差距
     */
    private double calculateTimelinessScore(DqcExecutionDetail detail) {
        // TODO: 实现基于数据更新时间的评分
        // 目前使用占位逻辑，后续可基于实际更新时间计算
        BigDecimal actualValue = detail.getActualValue();
        if (actualValue != null) {
            // 假设 actualValue 表示延迟的分钟数
            double delayMinutes = actualValue.doubleValue();
            // 每超过30分钟扣10分
            double score = 100 - (delayMinutes / 30) * 10;
            return Math.max(0, Math.min(100, score));
        }
        return 100;
    }

    /**
     * 计算有效性维度评分
     * 基于无效数据率计算
     */
    private double calculateValidityScore(DqcExecutionDetail detail, double errorRate) {
        return 100 * (1 - errorRate);
    }

    /**
     * 阶段三-T3: 计算敏感字段合规率评分
     * <p>
     * 算法说明：
     * 1. 从执行明细中筛选出有 sensitivityLevel 的记录（已关联敏感字段）
     * 2. 按等级分组：L4/L3 为高敏感（必须 PASS），L2/L1 为普通敏感
     * 3. 高敏感字段失败 → 扣更多分（强规则惩罚因子）
     * 4. 合规率 = (高敏感PASS + 普通敏感PASS) / 总敏感字段数 × 100
     */
    private BigDecimal calculateSensitivityComplianceScore(List<DqcExecutionDetail> details) {
        if (details == null || details.isEmpty()) {
            return BigDecimal.valueOf(100);
        }

        // 筛选有敏感等级的明细
        List<DqcExecutionDetail> sensitiveDetails = details.stream()
                .filter(d -> d.getSensitivityLevel() != null && !d.getSensitivityLevel().isEmpty())
                .collect(Collectors.toList());

        if (sensitiveDetails.isEmpty()) {
            // 无敏感字段时，该维度默认满分
            return BigDecimal.valueOf(100);
        }

        int totalSensitive = sensitiveDetails.size();
        int passedSensitive = 0;
        int failedSensitive = 0;

        for (DqcExecutionDetail detail : sensitiveDetails) {
            if ("SUCCESS".equals(detail.getStatus())) {
                passedSensitive++;
            } else if ("FAILED".equals(detail.getStatus())) {
                failedSensitive++;
            }
        }

        // 合规率 = PASS 数 / 总敏感字段数
        double complianceRate = totalSensitive > 0
                ? (double) passedSensitive / totalSensitive * 100
                : 100.0;

        // 检查高敏感等级（L3/L4）是否有失败
        boolean hasHighSensitiveFailure = sensitiveDetails.stream()
                .filter(d -> "FAILED".equals(d.getStatus()))
                .anyMatch(d -> "L3".equals(d.getSensitivityLevel())
                        || "L4".equals(d.getSensitivityLevel()));

        if (hasHighSensitiveFailure) {
            // 高敏感字段失败，合规率打折（应用强规则惩罚）
            complianceRate = complianceRate / STRONG_RULE_PENALTY_FACTOR.doubleValue();
        }

        return BigDecimal.valueOf(Math.max(0, Math.min(100, complianceRate)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 获取规则的权重
     */
    private BigDecimal getRuleWeight(DqcExecutionDetail detail) {
        // TODO: 可以从规则定义中读取自定义权重
        // 目前使用默认值
        return BigDecimal.ONE;
    }

    /**
     * 获取规则的惩罚因子
     * 强规则失败扣分更多
     */
    private BigDecimal getRulePenaltyFactor(DqcExecutionDetail detail) {
        if ("STRONG".equals(detail.getRuleStrength())) {
            return STRONG_RULE_PENALTY_FACTOR;
        }
        return WEAK_RULE_PENALTY_FACTOR;
    }

    /**
     * 使用加权平均计算总分，仅对已评估（已绑定规则）的维度加权。
     */
    private BigDecimal calculateWeightedOverallScore(Map<String, BigDecimal> dimensionScores,
                                                     List<String> evaluatedDims) {
        if (dimensionScores.isEmpty()) {
            return BigDecimal.valueOf(100);
        }

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (String dim : evaluatedDims) {
            BigDecimal score = dimensionScores.get(dim);
            if (score == null) continue;
            BigDecimal weight = DEFAULT_DIMENSION_WEIGHTS.getOrDefault(dim, new BigDecimal("0.10"));
            weightedSum = weightedSum.add(score.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        return weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    /**
     * 重载：保持旧签名兼容（仅用于旧方法 calculateOverallScore）
     */
    private BigDecimal calculateWeightedOverallScore(Map<String, BigDecimal> dimensionScores) {
        return calculateWeightedOverallScore(dimensionScores, new ArrayList<>(dimensionScores.keySet()));
    }

    /**
     * 获取所有维度列表
     */
    public List<String> getAllDimensions() {
        return new ArrayList<>(DEFAULT_DIMENSION_WEIGHTS.keySet());
    }

    /**
     * 获取维度权重配置
     */
    public Map<String, BigDecimal> getDimensionWeights() {
        return new LinkedHashMap<>(DEFAULT_DIMENSION_WEIGHTS);
    }

    /**
     * 获取评分等级描述
     */
    public String getScoreGrade(BigDecimal score) {
        if (score == null) {
            return "UNKNOWN";
        }
        int scoreValue = score.intValue();
        if (scoreValue >= EXCELLENT_THRESHOLD) {
            return "EXCELLENT";
        } else if (scoreValue >= GOOD_THRESHOLD) {
            return "GOOD";
        } else if (scoreValue >= WARNING_THRESHOLD) {
            return "WARNING";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * 获取评分等级颜色
     */
    public String getScoreColor(BigDecimal score) {
        if (score == null) {
            return "#8C8C8C";
        }
        int scoreValue = score.intValue();
        if (scoreValue >= EXCELLENT_THRESHOLD) {
            return "#52C41A"; // 绿色
        } else if (scoreValue >= GOOD_THRESHOLD) {
            return "#1677FF"; // 蓝色
        } else if (scoreValue >= WARNING_THRESHOLD) {
            return "#FAAD14"; // 黄色
        } else {
            return "#FF4D4F"; // 红色
        }
    }

    /**
     * 获取维度名称（中文）
     */
    public String getDimensionName(String dimensionCode) {
        return switch (dimensionCode) {
            case DIM_COMPLETENESS -> "完整性";
            case DIM_UNIQUENESS -> "唯一性";
            case DIM_ACCURACY -> "准确性";
            case DIM_CONSISTENCY -> "一致性";
            case DIM_TIMELINESS -> "及时性";
            case DIM_VALIDITY -> "有效性";
            case DIM_SENSITIVITY -> "敏感字段合规率";
            default -> dimensionCode;
        };
    }

    /**
     * 将规则类型映射到维度
     */
    public String getDimension(String ruleType) {
        if (ruleType == null) {
            return DIM_COMPLETENESS;
        }

        String upperRuleType = ruleType.toUpperCase();
        return switch (upperRuleType) {
            case "NULL_CHECK", "ROW_COUNT_NOT_ZERO", "ROW_COUNT_FLUCTUATION", "FLUCTUATION" -> DIM_COMPLETENESS;
            case "UNIQUE_CHECK", "DUPLICATE_CHECK", "CARDINALITY" -> DIM_UNIQUENESS;
            case "THRESHOLD_MIN", "THRESHOLD_MAX", "THRESHOLD_RANGE", "THRESHOLD", "COUNT_THRESHOLD" -> DIM_ACCURACY;
            case "ENUM_CHECK", "CROSS_FIELD_COMPARE", "CROSS_FIELD_SUM", "CROSS_FIELD_NULL_CHECK",
                 "CROSS_TABLE_COUNT", "CROSS_TABLE_PRIMARY_KEY" -> DIM_CONSISTENCY;
            case "TABLE_UPDATE_TIMELINESS", "TIMELINESS" -> DIM_TIMELINESS;
            case "REGEX_PHONE", "REGEX_EMAIL", "REGEX_IDCARD", "REGEX", "LENGTH_CHECK" -> DIM_VALIDITY;
            case "CUSTOM_SQL", "CUSTOM_FUNC" -> DIM_VALIDITY;
            default -> DIM_VALIDITY;
        };
    }

    /**
     * 序列化评分明细为JSON（供前端展示健康度与维度评估状态）
     */
    public String serializeBreakdown(ScoreBreakdown breakdown) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("overallScore", breakdown.overallScore());
            map.put("overallGrade", getScoreGrade(breakdown.overallScore()));
            map.put("healthScore", breakdown.healthScore());
            map.put("dimensionScores", breakdown.dimensionScores());
            map.put("evaluatedDimensions", breakdown.evaluatedDimensions());
            map.put("unevaluatedDimensions", breakdown.unevaluatedDimensions());
            map.put("stats", breakdown.stats());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize score breakdown", e);
            return "{}";
        }
    }

    /**
     * 计算单条规则的评分（旧方法，保持兼容性）
     */
    public BigDecimal calculateRuleScore(String ruleType, String dimensions, int passed, int failed) {
        int total = passed + failed;
        if (total == 0) {
            return BigDecimal.valueOf(100);
        }
        return BigDecimal.valueOf(100.0 * passed / total).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算维度评分（旧方法，保持兼容性）
     */
    public BigDecimal calculateDimensionScore(List<BigDecimal> ruleScores) {
        if (ruleScores == null || ruleScores.isEmpty()) {
            return BigDecimal.valueOf(100);
        }
        BigDecimal sum = ruleScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(ruleScores.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算总分（旧方法，保持兼容性）
     */
    public BigDecimal calculateOverallScore(BigDecimal completeness, BigDecimal uniqueness, BigDecimal accuracy,
                                            BigDecimal consistency, BigDecimal timeliness, BigDecimal validity) {
        List<BigDecimal> scores = new ArrayList<>();
        if (completeness != null) scores.add(completeness);
        if (uniqueness != null) scores.add(uniqueness);
        if (accuracy != null) scores.add(accuracy);
        if (consistency != null) scores.add(consistency);
        if (timeliness != null) scores.add(timeliness);
        if (validity != null) scores.add(validity);

        if (scores.isEmpty()) {
            return BigDecimal.valueOf(0);
        }
        return calculateDimensionScore(scores);
    }

    /**
     * 计算维度平均分
     */
    private BigDecimal calculateDimensionAverage(List<BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) {
            return null;
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    // ==================== 数据类 ====================

    /**
     * 评分明细
     *
     * @param overallScore       加权总分（仅含已绑定规则的维度）
     * @param dimensionScores    各维度评分
     * @param stats              规则执行统计
     * @param evaluatedDimensions 已评估（已绑定规则）的维度列表
     * @param unevaluatedDimensions 未评估（无绑定规则）的维度列表
     * @param healthScore        执行健康度：若有失败规则则为 rulePassRate（不得高于通过率）；否则等于 overallScore
     */
    public record ScoreBreakdown(
            BigDecimal overallScore,
            Map<String, BigDecimal> dimensionScores,
            ScoreStats stats,
            List<String> evaluatedDimensions,
            List<String> unevaluatedDimensions,
            BigDecimal healthScore
    ) {}

    /**
     * 评分统计信息
     */
    public record ScoreStats(
            int totalRules,   // 总规则数
            int passedRules,  // 通过规则数
            int failedRules,  // 失败规则数
            int skippedRules  // 跳过规则数
    ) {
        public double passRate() {
            return totalRules > 0 ? (double) passedRules / totalRules * 100 : 0;
        }
    }
}
