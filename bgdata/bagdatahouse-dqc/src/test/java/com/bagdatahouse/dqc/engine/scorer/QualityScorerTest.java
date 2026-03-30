package com.bagdatahouse.dqc.engine.scorer;

import com.bagdatahouse.core.entity.DqcExecutionDetail;
import com.bagdatahouse.core.entity.DqcQualityScore;
import com.bagdatahouse.core.mapper.DqcQualityScoreMapper;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.entity.DqDatasource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QualityScorer 单元测试
 * 
 * 测试质量评分计算器的核心功能：
 * - 评分计算算法
 * - 维度映射
 * - 评分等级判定
 * - 敏感字段合规率计算
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("质量评分计算器测试")
class QualityScorerTest {

    private QualityScorer qualityScorer;

    @Mock
    private DqcQualityScoreMapper qualityScoreMapper;

    @Mock
    private DqDatasourceMapper datasourceMapper;

    @BeforeEach
    void setUp() {
        qualityScorer = new QualityScorer();
        ReflectionTestUtils.setField(qualityScorer, "qualityScoreMapper", qualityScoreMapper);
        ReflectionTestUtils.setField(qualityScorer, "datasourceMapper", datasourceMapper);
    }

    // ==================== 测试辅助方法 ====================

    private DqcExecutionDetail createDetail(String ruleType, String status, 
            Long totalCount, Long errorCount, String ruleStrength, String sensitivityLevel) {
        return DqcExecutionDetail.builder()
                .ruleType(ruleType)
                .status(status)
                .totalCount(totalCount)
                .errorCount(errorCount)
                .ruleStrength(ruleStrength)
                .sensitivityLevel(sensitivityLevel)
                .targetDsId(1L)
                .targetTable("test_table")
                .build();
    }

    private DqcExecutionDetail createDetailWithThresholds(String ruleType, String status,
            Long totalCount, Long errorCount, BigDecimal actualValue, 
            BigDecimal thresholdMin, BigDecimal thresholdMax) {
        return DqcExecutionDetail.builder()
                .ruleType(ruleType)
                .status(status)
                .totalCount(totalCount)
                .errorCount(errorCount)
                .actualValue(actualValue)
                .thresholdMin(thresholdMin)
                .thresholdMax(thresholdMax)
                .targetDsId(1L)
                .targetTable("test_table")
                .build();
    }

    // ==================== calculateBreakdown 测试 ====================

    @Nested
    @DisplayName("calculateBreakdown 方法测试")
    class CalculateBreakdownTest {

        @Test
        @DisplayName("空列表应返回满分")
        void testEmptyListReturnsPerfectScore() {
            ScoreBreakdown result = qualityScorer.calculateBreakdown(Collections.emptyList());
            
            assertThat(result.overallScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.stats().totalRules()).isZero();
            assertThat(result.stats().passedRules()).isZero();
        }

        @Test
        @DisplayName("null列表应返回满分")
        void testNullListReturnsPerfectScore() {
            ScoreBreakdown result = qualityScorer.calculateBreakdown(null);
            
            assertThat(result.overallScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.stats().totalRules()).isZero();
        }

        @Test
        @DisplayName("所有规则成功应返回满分")
        void testAllSuccessReturnsPerfectScore() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("NULL_CHECK", "SUCCESS", 1000L, 0L, "WEAK", null),
                    createDetail("UNIQUE_CHECK", "SUCCESS", 1000L, 0L, "WEAK", null),
                    createDetail("THRESHOLD_MIN", "SUCCESS", 1000L, 0L, "STRONG", null)
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            assertThat(result.overallScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.stats().totalRules()).isEqualTo(3);
            assertThat(result.stats().passedRules()).isEqualTo(3);
            assertThat(result.stats().failedRules()).isZero();
            assertThat(result.stats().skippedRules()).isZero();
            assertThat(result.stats().passRate()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("混合成功和失败应计算正确分数")
        void testMixedSuccessAndFailure() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("NULL_CHECK", "SUCCESS", 1000L, 0L, "WEAK", null),
                    createDetail("NULL_CHECK", "FAILED", 1000L, 50L, "WEAK", null)
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            assertThat(result.overallScore()).isLessThan(BigDecimal.valueOf(100));
            assertThat(result.overallScore()).isGreaterThan(BigDecimal.valueOf(0));
            assertThat(result.stats().passedRules()).isEqualTo(1);
            assertThat(result.stats().failedRules()).isEqualTo(1);
            assertThat(result.stats().passRate()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("包含跳过规则应正确统计")
        void testWithSkippedRules() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("NULL_CHECK", "SUCCESS", 1000L, 0L, "WEAK", null),
                    createDetail("UNIQUE_CHECK", "SKIPPED", 0L, 0L, "WEAK", null),
                    createDetail("THRESHOLD_MIN", "FAILED", 1000L, 100L, "WEAK", null)
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            assertThat(result.stats().totalRules()).isEqualTo(3);
            assertThat(result.stats().skippedRules()).isEqualTo(1);
            assertThat(result.stats().passRate()).isCloseTo(33.33, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("应包含所有7个维度的评分")
        void testAllDimensionsIncluded() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("NULL_CHECK", "SUCCESS", 1000L, 0L, "WEAK", null),
                    createDetail("UNIQUE_CHECK", "SUCCESS", 1000L, 0L, "WEAK", null),
                    createDetail("THRESHOLD_MIN", "SUCCESS", 1000L, 0L, "WEAK", null)
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            assertThat(result.dimensionScores()).containsKeys(
                    QualityScorer.DIM_COMPLETENESS,
                    QualityScorer.DIM_UNIQUENESS,
                    QualityScorer.DIM_ACCURACY,
                    QualityScorer.DIM_CONSISTENCY,
                    QualityScorer.DIM_TIMELINESS,
                    QualityScorer.DIM_VALIDITY,
                    QualityScorer.DIM_SENSITIVITY
            );
        }
    }

    // ==================== 维度映射测试 ====================

    @Nested
    @DisplayName("getDimension 维度映射测试")
    class GetDimensionTest {

        @Test
        @DisplayName("NULL_CHECK 应映射到 COMPLETENESS")
        void testNullCheckMapping() {
            assertThat(qualityScorer.getDimension("NULL_CHECK"))
                    .isEqualTo(QualityScorer.DIM_COMPLETENESS);
        }

        @Test
        @DisplayName("ROW_COUNT_NOT_ZERO 应映射到 COMPLETENESS")
        void testRowCountNotZeroMapping() {
            assertThat(qualityScorer.getDimension("ROW_COUNT_NOT_ZERO"))
                    .isEqualTo(QualityScorer.DIM_COMPLETENESS);
        }

        @Test
        @DisplayName("ROW_COUNT_FLUCTUATION 应映射到 COMPLETENESS")
        void testRowCountFluctuationMapping() {
            assertThat(qualityScorer.getDimension("ROW_COUNT_FLUCTUATION"))
                    .isEqualTo(QualityScorer.DIM_COMPLETENESS);
        }

        @Test
        @DisplayName("UNIQUE_CHECK 应映射到 UNIQUENESS")
        void testUniqueCheckMapping() {
            assertThat(qualityScorer.getDimension("UNIQUE_CHECK"))
                    .isEqualTo(QualityScorer.DIM_UNIQUENESS);
        }

        @Test
        @DisplayName("DUPLICATE_CHECK 应映射到 UNIQUENESS")
        void testDuplicateCheckMapping() {
            assertThat(qualityScorer.getDimension("DUPLICATE_CHECK"))
                    .isEqualTo(QualityScorer.DIM_UNIQUENESS);
        }

        @Test
        @DisplayName("CARDINALITY 应映射到 UNIQUENESS")
        void testCardinalityMapping() {
            assertThat(qualityScorer.getDimension("CARDINALITY"))
                    .isEqualTo(QualityScorer.DIM_UNIQUENESS);
        }

        @Test
        @DisplayName("THRESHOLD_MIN 应映射到 ACCURACY")
        void testThresholdMinMapping() {
            assertThat(qualityScorer.getDimension("THRESHOLD_MIN"))
                    .isEqualTo(QualityScorer.DIM_ACCURACY);
        }

        @Test
        @DisplayName("THRESHOLD_MAX 应映射到 ACCURACY")
        void testThresholdMaxMapping() {
            assertThat(qualityScorer.getDimension("THRESHOLD_MAX"))
                    .isEqualTo(QualityScorer.DIM_ACCURACY);
        }

        @Test
        @DisplayName("THRESHOLD_RANGE 应映射到 ACCURACY")
        void testThresholdRangeMapping() {
            assertThat(qualityScorer.getDimension("THRESHOLD_RANGE"))
                    .isEqualTo(QualityScorer.DIM_ACCURACY);
        }

        @Test
        @DisplayName("ENUM_CHECK 应映射到 CONSISTENCY")
        void testEnumCheckMapping() {
            assertThat(qualityScorer.getDimension("ENUM_CHECK"))
                    .isEqualTo(QualityScorer.DIM_CONSISTENCY);
        }

        @Test
        @DisplayName("CROSS_FIELD_COMPARE 应映射到 CONSISTENCY")
        void testCrossFieldCompareMapping() {
            assertThat(qualityScorer.getDimension("CROSS_FIELD_COMPARE"))
                    .isEqualTo(QualityScorer.DIM_CONSISTENCY);
        }

        @Test
        @DisplayName("TABLE_UPDATE_TIMELINESS 应映射到 TIMELINESS")
        void testTableUpdateTimelinessMapping() {
            assertThat(qualityScorer.getDimension("TABLE_UPDATE_TIMELINESS"))
                    .isEqualTo(QualityScorer.DIM_TIMELINESS);
        }

        @Test
        @DisplayName("REGEX_PHONE 应映射到 VALIDITY")
        void testRegexPhoneMapping() {
            assertThat(qualityScorer.getDimension("REGEX_PHONE"))
                    .isEqualTo(QualityScorer.DIM_VALIDITY);
        }

        @Test
        @DisplayName("REGEX_EMAIL 应映射到 VALIDITY")
        void testRegexEmailMapping() {
            assertThat(qualityScorer.getDimension("REGEX_EMAIL"))
                    .isEqualTo(QualityScorer.DIM_VALIDITY);
        }

        @Test
        @DisplayName("REGEX_IDCARD 应映射到 VALIDITY")
        void testRegexIdcardMapping() {
            assertThat(qualityScorer.getDimension("REGEX_IDCARD"))
                    .isEqualTo(QualityScorer.DIM_VALIDITY);
        }

        @Test
        @DisplayName("REGEX 应映射到 VALIDITY")
        void testRegexMapping() {
            assertThat(qualityScorer.getDimension("REGEX"))
                    .isEqualTo(QualityScorer.DIM_VALIDITY);
        }

        @Test
        @DisplayName("CUSTOM_SQL 应映射到 VALIDITY")
        void testCustomSqlMapping() {
            assertThat(qualityScorer.getDimension("CUSTOM_SQL"))
                    .isEqualTo(QualityScorer.DIM_VALIDITY);
        }

        @Test
        @DisplayName("CUSTOM_FUNC 应映射到 VALIDITY")
        void testCustomFuncMapping() {
            assertThat(qualityScorer.getDimension("CUSTOM_FUNC"))
                    .isEqualTo(QualityScorer.DIM_VALIDITY);
        }

        @Test
        @DisplayName("未知类型应默认映射到 VALIDITY")
        void testUnknownTypeDefaultsToValidity() {
            assertThat(qualityScorer.getDimension("UNKNOWN_RULE"))
                    .isEqualTo(QualityScorer.DIM_VALIDITY);
        }

        @Test
        @DisplayName("null类型应默认为 COMPLETENESS")
        void testNullTypeDefaultsToCompleteness() {
            assertThat(qualityScorer.getDimension(null))
                    .isEqualTo(QualityScorer.DIM_COMPLETENESS);
        }

        @Test
        @DisplayName("大小写应不敏感")
        void testCaseInsensitive() {
            assertThat(qualityScorer.getDimension("null_check"))
                    .isEqualTo(QualityScorer.DIM_COMPLETENESS);
            assertThat(qualityScorer.getDimension("Unique_Check"))
                    .isEqualTo(QualityScorer.DIM_UNIQUENESS);
        }
    }

    // ==================== 评分等级测试 ====================

    @Nested
    @DisplayName("getScoreGrade 评分等级测试")
    class GetScoreGradeTest {

        @Test
        @DisplayName("90分以上应返回EXCELLENT")
        void testExcellentGrade() {
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(90))).isEqualTo("EXCELLENT");
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(100))).isEqualTo("EXCELLENT");
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(95.5))).isEqualTo("EXCELLENT");
        }

        @Test
        @DisplayName("70-89分应返回GOOD")
        void testGoodGrade() {
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(70))).isEqualTo("GOOD");
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(89))).isEqualTo("GOOD");
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(75.5))).isEqualTo("GOOD");
        }

        @Test
        @DisplayName("60-69分应返回WARNING")
        void testWarningGrade() {
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(60))).isEqualTo("WARNING");
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(69))).isEqualTo("WARNING");
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(65.5))).isEqualTo("WARNING");
        }

        @Test
        @DisplayName("60分以下应返回CRITICAL")
        void testCriticalGrade() {
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(59))).isEqualTo("CRITICAL");
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(0))).isEqualTo("CRITICAL");
            assertThat(qualityScorer.getScoreGrade(BigDecimal.valueOf(30.5))).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("null应返回UNKNOWN")
        void testNullReturnsUnknown() {
            assertThat(qualityScorer.getScoreGrade(null)).isEqualTo("UNKNOWN");
        }
    }

    // ==================== 评分颜色测试 ====================

    @Nested
    @DisplayName("getScoreColor 评分颜色测试")
    class GetScoreColorTest {

        @Test
        @DisplayName("EXCELLENT应返回绿色")
        void testExcellentColor() {
            assertThat(qualityScorer.getScoreColor(BigDecimal.valueOf(90))).isEqualTo("#52C41A");
            assertThat(qualityScorer.getScoreColor(BigDecimal.valueOf(100))).isEqualTo("#52C41A");
        }

        @Test
        @DisplayName("GOOD应返回蓝色")
        void testGoodColor() {
            assertThat(qualityScorer.getScoreColor(BigDecimal.valueOf(70))).isEqualTo("#1677FF");
            assertThat(qualityScorer.getScoreColor(BigDecimal.valueOf(89))).isEqualTo("#1677FF");
        }

        @Test
        @DisplayName("WARNING应返回黄色")
        void testWarningColor() {
            assertThat(qualityScorer.getScoreColor(BigDecimal.valueOf(60))).isEqualTo("#FAAD14");
            assertThat(qualityScorer.getScoreColor(BigDecimal.valueOf(69))).isEqualTo("#FAAD14");
        }

        @Test
        @DisplayName("CRITICAL应返回红色")
        void testCriticalColor() {
            assertThat(qualityScorer.getScoreColor(BigDecimal.valueOf(59))).isEqualTo("#FF4D4F");
            assertThat(qualityScorer.getScoreColor(BigDecimal.valueOf(0))).isEqualTo("#FF4D4F");
        }

        @Test
        @DisplayName("null应返回灰色")
        void testNullColor() {
            assertThat(qualityScorer.getScoreColor(null)).isEqualTo("#8C8C8C");
        }
    }

    // ==================== 维度名称测试 ====================

    @Nested
    @DisplayName("getDimensionName 维度名称测试")
    class GetDimensionNameTest {

        @Test
        @DisplayName("应返回正确的维度中文名称")
        void testDimensionNames() {
            assertThat(qualityScorer.getDimensionName(QualityScorer.DIM_COMPLETENESS)).isEqualTo("完整性");
            assertThat(qualityScorer.getDimensionName(QualityScorer.DIM_UNIQUENESS)).isEqualTo("唯一性");
            assertThat(qualityScorer.getDimensionName(QualityScorer.DIM_ACCURACY)).isEqualTo("准确性");
            assertThat(qualityScorer.getDimensionName(QualityScorer.DIM_CONSISTENCY)).isEqualTo("一致性");
            assertThat(qualityScorer.getDimensionName(QualityScorer.DIM_TIMELINESS)).isEqualTo("及时性");
            assertThat(qualityScorer.getDimensionName(QualityScorer.DIM_VALIDITY)).isEqualTo("有效性");
            assertThat(qualityScorer.getDimensionName(QualityScorer.DIM_SENSITIVITY)).isEqualTo("敏感字段合规率");
        }

        @Test
        @DisplayName("未知维度应返回原值")
        void testUnknownDimensionReturnsOriginal() {
            assertThat(qualityScorer.getDimensionName("UNKNOWN_DIM")).isEqualTo("UNKNOWN_DIM");
        }
    }

    // ==================== getAllDimensions 测试 ====================

    @Nested
    @DisplayName("getAllDimensions 测试")
    class GetAllDimensionsTest {

        @Test
        @DisplayName("应返回7个维度")
        void testReturnsSevenDimensions() {
            List<String> dimensions = qualityScorer.getAllDimensions();
            
            assertThat(dimensions).hasSize(7);
            assertThat(dimensions).containsExactlyInAnyOrder(
                    QualityScorer.DIM_COMPLETENESS,
                    QualityScorer.DIM_UNIQUENESS,
                    QualityScorer.DIM_ACCURACY,
                    QualityScorer.DIM_CONSISTENCY,
                    QualityScorer.DIM_TIMELINESS,
                    QualityScorer.DIM_VALIDITY,
                    QualityScorer.DIM_SENSITIVITY
            );
        }
    }

    // ==================== 维度权重测试 ====================

    @Nested
    @DisplayName("getDimensionWeights 维度权重测试")
    class GetDimensionWeightsTest {

        @Test
        @DisplayName("应返回所有维度的权重配置")
        void testReturnsAllDimensionWeights() {
            Map<String, BigDecimal> weights = qualityScorer.getDimensionWeights();
            
            assertThat(weights).containsKey(QualityScorer.DIM_COMPLETENESS);
            assertThat(weights).containsKey(QualityScorer.DIM_UNIQUENESS);
            assertThat(weights).containsKey(QualityScorer.DIM_ACCURACY);
            assertThat(weights).containsKey(QualityScorer.DIM_CONSISTENCY);
            assertThat(weights).containsKey(QualityScorer.DIM_TIMELINESS);
            assertThat(weights).containsKey(QualityScorer.DIM_VALIDITY);
            assertThat(weights).containsKey(QualityScorer.DIM_SENSITIVITY);
        }

        @Test
        @DisplayName("权重总和应等于1")
        void testWeightsSumToOne() {
            Map<String, BigDecimal> weights = qualityScorer.getDimensionWeights();
            
            BigDecimal sum = weights.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            assertThat(sum).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("主要维度权重应为20%")
        void testMainDimensionWeights() {
            Map<String, BigDecimal> weights = qualityScorer.getDimensionWeights();
            
            assertThat(weights.get(QualityScorer.DIM_COMPLETENESS))
                    .isEqualByComparingTo(new BigDecimal("0.20"));
            assertThat(weights.get(QualityScorer.DIM_UNIQUENESS))
                    .isEqualByComparingTo(new BigDecimal("0.20"));
            assertThat(weights.get(QualityScorer.DIM_ACCURACY))
                    .isEqualByComparingTo(new BigDecimal("0.20"));
        }
    }

    // ==================== 敏感字段合规率测试 ====================

    @Nested
    @DisplayName("敏感字段合规率测试")
    class SensitivityComplianceTest {

        @Test
        @DisplayName("所有敏感字段成功应返回100分")
        void testAllSensitiveFieldsSuccess() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("REGEX_PHONE", "SUCCESS", 1000L, 0L, "WEAK", "L1"),
                    createDetail("REGEX_EMAIL", "SUCCESS", 1000L, 0L, "WEAK", "L2")
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal sensitivityScore = result.dimensionScores().get(QualityScorer.DIM_SENSITIVITY);
            assertThat(sensitivityScore).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("有敏感字段失败应扣分")
        void testSensitiveFieldFailureDeductions() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("REGEX_PHONE", "SUCCESS", 1000L, 0L, "WEAK", "L1"),
                    createDetail("REGEX_EMAIL", "FAILED", 1000L, 50L, "WEAK", "L2")
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal sensitivityScore = result.dimensionScores().get(QualityScorer.DIM_SENSITIVITY);
            assertThat(sensitivityScore).isLessThan(BigDecimal.valueOf(100));
            assertThat(sensitivityScore).isGreaterThan(BigDecimal.valueOf(0));
        }

        @Test
        @DisplayName("高敏感字段L3失败应受更严厉惩罚")
        void testHighSensitivityL3FailurePenalty() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("REGEX_PHONE", "FAILED", 1000L, 50L, "WEAK", "L1"),
                    createDetail("REGEX_EMAIL", "FAILED", 1000L, 50L, "WEAK", "L3")
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal sensitivityScore = result.dimensionScores().get(QualityScorer.DIM_SENSITIVITY);
            // 由于L3失败，应有惩罚因子1.5
            assertThat(sensitivityScore).isLessThan(BigDecimal.valueOf(50));
        }

        @Test
        @DisplayName("高敏感字段L4失败应受更严厉惩罚")
        void testHighSensitivityL4FailurePenalty() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("REGEX_PHONE", "FAILED", 1000L, 50L, "WEAK", "L2"),
                    createDetail("REGEX_EMAIL", "FAILED", 1000L, 50L, "WEAK", "L4")
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal sensitivityScore = result.dimensionScores().get(QualityScorer.DIM_SENSITIVITY);
            // 由于L4失败，应有惩罚因子1.5
            assertThat(sensitivityScore).isLessThan(BigDecimal.valueOf(50));
        }

        @Test
        @DisplayName("无敏感字段的维度应返回100分")
        void testNoSensitiveFieldsReturns100() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("REGEX_PHONE", "FAILED", 1000L, 50L, "WEAK", null),
                    createDetail("REGEX_EMAIL", "FAILED", 1000L, 50L, "WEAK", "")
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal sensitivityScore = result.dimensionScores().get(QualityScorer.DIM_SENSITIVITY);
            assertThat(sensitivityScore).isEqualByComparingTo(BigDecimal.valueOf(100));
        }
    }

    // ==================== 强规则与弱规则测试 ====================

    @Nested
    @DisplayName("强规则与弱规则惩罚测试")
    class RuleStrengthPenaltyTest {

        @Test
        @DisplayName("强规则失败应扣更多分")
        void testStrongRuleFailurePenalty() {
            List<DqcExecutionDetail> strongRuleDetails = List.of(
                    createDetail("NULL_CHECK", "FAILED", 1000L, 50L, "STRONG", null)
            );

            List<DqcExecutionDetail> weakRuleDetails = List.of(
                    createDetail("NULL_CHECK", "FAILED", 1000L, 50L, "WEAK", null)
            );

            ScoreBreakdown strongResult = qualityScorer.calculateBreakdown(strongRuleDetails);
            ScoreBreakdown weakResult = qualityScorer.calculateBreakdown(weakRuleDetails);

            BigDecimal strongScore = strongResult.dimensionScores().get(QualityScorer.DIM_COMPLETENESS);
            BigDecimal weakScore = weakResult.dimensionScores().get(QualityScorer.DIM_COMPLETENESS);

            // 强规则得分应低于弱规则
            assertThat(strongScore).isLessThan(weakScore);
        }

        @Test
        @DisplayName("强规则和弱规则权重因子验证")
        void testRuleStrengthWeightFactors() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("NULL_CHECK", "FAILED", 1000L, 100L, "STRONG", null)
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal completenessScore = result.dimensionScores().get(QualityScorer.DIM_COMPLETENESS);
            // 错误率100%，强规则惩罚因子1.5，得分应为 100 / 1.5 ≈ 66.67
            assertThat(completenessScore.doubleValue()).isCloseTo(66.67, org.assertj.core.data.Offset.offset(1.0));
        }
    }

    // ==================== ScoreStats 测试 ====================

    @Nested
    @DisplayName("ScoreStats passRate 测试")
    class ScoreStatsTest {

        @Test
        @DisplayName("passRate应正确计算")
        void testPassRateCalculation() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("NULL_CHECK", "SUCCESS", 1000L, 0L, "WEAK", null),
                    createDetail("UNIQUE_CHECK", "SUCCESS", 1000L, 0L, "WEAK", null),
                    createDetail("THRESHOLD_MIN", "FAILED", 1000L, 50L, "WEAK", null),
                    createDetail("ENUM_CHECK", "SKIPPED", 0L, 0L, "WEAK", null)
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            assertThat(result.stats().totalRules()).isEqualTo(4);
            assertThat(result.stats().passedRules()).isEqualTo(2);
            assertThat(result.stats().failedRules()).isEqualTo(1);
            assertThat(result.stats().skippedRules()).isEqualTo(1);
            assertThat(result.stats().passRate()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("零规则时应返回0%通过率")
        void testZeroRulesPassRate() {
            ScoreBreakdown result = qualityScorer.calculateBreakdown(Collections.emptyList());

            assertThat(result.stats().passRate()).isEqualTo(0.0);
        }
    }

    // ==================== 及时性维度测试 ====================

    @Nested
    @DisplayName("TIMELINESS 维度测试")
    class TimelinessDimensionTest {

        @Test
        @DisplayName("无延迟应返回满分")
        void testNoDelayReturnsPerfectScore() {
            List<DqcExecutionDetail> details = List.of(
                    createDetailWithThresholds("TABLE_UPDATE_TIMELINESS", "SUCCESS", 
                            1L, 0L, BigDecimal.ZERO, null, null)
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal timelinessScore = result.dimensionScores().get(QualityScorer.DIM_TIMELINESS);
            assertThat(timelinessScore).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("超过30分钟应开始扣分")
        void testDelayOver30MinutesDeductions() {
            List<DqcExecutionDetail> details = List.of(
                    createDetailWithThresholds("TABLE_UPDATE_TIMELINESS", "SUCCESS",
                            1L, 0L, BigDecimal.valueOf(60), null, null) // 60分钟延迟
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal timelinessScore = result.dimensionScores().get(QualityScorer.DIM_TIMELINESS);
            // 延迟60分钟，应扣 60/30*10 = 20分，得分80
            assertThat(timelinessScore.doubleValue()).isCloseTo(80.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("超过300分钟应返回0分")
        void testExtremeDelayReturnsZero() {
            List<DqcExecutionDetail> details = List.of(
                    createDetailWithThresholds("TABLE_UPDATE_TIMELINESS", "SUCCESS",
                            1L, 0L, BigDecimal.valueOf(600), null, null) // 600分钟延迟
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal timelinessScore = result.dimensionScores().get(QualityScorer.DIM_TIMELINESS);
            assertThat(timelinessScore).isEqualByComparingTo(BigDecimal.valueOf(0));
        }
    }

    // ==================== 准确性维度测试 ====================

    @Nested
    @DisplayName("ACCURACY 维度测试")
    class AccuracyDimensionTest {

        @Test
        @DisplayName("实际值在阈值范围内应返回高分")
        void testValueInRangeReturnsHighScore() {
            List<DqcExecutionDetail> details = List.of(
                    createDetailWithThresholds("THRESHOLD_RANGE", "FAILED",
                            1000L, 100L,
                            new BigDecimal("75"), // 实际值在范围内
                            new BigDecimal("50"), // 最小阈值
                            new BigDecimal("100")) // 最大阈值
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal accuracyScore = result.dimensionScores().get(QualityScorer.DIM_ACCURACY);
            assertThat(accuracyScore.doubleValue()).isGreaterThan(0);
        }

        @Test
        @DisplayName("无阈值信息时基于错误率计算")
        void testNoThresholdUsesErrorRate() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("THRESHOLD_MIN", "FAILED", 1000L, 100L, "WEAK", null)
            );

            ScoreBreakdown result = qualityScorer.calculateBreakdown(details);

            BigDecimal accuracyScore = result.dimensionScores().get(QualityScorer.DIM_ACCURACY);
            // 错误率10%，得分应为 100 * (1 - 0.1) = 90
            assertThat(accuracyScore.doubleValue()).isCloseTo(90.0, org.assertj.core.data.Offset.offset(1.0));
        }
    }

    // ==================== 序列化测试 ====================

    @Nested
    @DisplayName("serializeBreakdown 序列化测试")
    class SerializeBreakdownTest {

        @Test
        @DisplayName("应正确序列化为JSON")
        void testSerializeBreakdown() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("NULL_CHECK", "SUCCESS", 1000L, 0L, "WEAK", null)
            );

            ScoreBreakdown breakdown = qualityScorer.calculateBreakdown(details);
            String json = qualityScorer.serializeBreakdown(breakdown);

            assertThat(json).isNotEmpty();
            assertThat(json).contains("overallScore");
            assertThat(json).contains("dimensionScores");
            assertThat(json).contains("stats");
            assertThat(json).contains("overallGrade");
        }

        @Test
        @DisplayName("序列化结果应包含EXCELLENT等级")
        void testSerializedGrade() {
            List<DqcExecutionDetail> details = List.of(
                    createDetail("NULL_CHECK", "SUCCESS", 1000L, 0L, "WEAK", null)
            );

            ScoreBreakdown breakdown = qualityScorer.calculateBreakdown(details);
            String json = qualityScorer.serializeBreakdown(breakdown);

            assertThat(json).contains("EXCELLENT");
        }
    }

    // ==================== 兼容性方法测试 ====================

    @Nested
    @DisplayName("兼容性方法测试")
    class LegacyMethodTest {

        @Test
        @DisplayName("calculateRuleScore(String, String, int, int) 应正确计算")
        void testLegacyCalculateRuleScore() {
            BigDecimal result = qualityScorer.calculateRuleScore(
                    "NULL_CHECK", "COMPLETENESS", 80, 20);

            assertThat(result.doubleValue()).isCloseTo(80.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("calculateRuleScore 应处理零总数")
        void testLegacyCalculateRuleScoreWithZeroTotal() {
            BigDecimal result = qualityScorer.calculateRuleScore(
                    "NULL_CHECK", "COMPLETENESS", 0, 0);

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("calculateDimensionScore 应正确计算平均分")
        void testLegacyCalculateDimensionScore() {
            List<BigDecimal> scores = List.of(
                    BigDecimal.valueOf(80),
                    BigDecimal.valueOf(90),
                    BigDecimal.valueOf(100)
            );

            BigDecimal result = qualityScorer.calculateDimensionScore(scores);

            assertThat(result.doubleValue()).isCloseTo(90.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("calculateOverallScore 应正确计算加权总分")
        void testLegacyCalculateOverallScore() {
            BigDecimal result = qualityScorer.calculateOverallScore(
                    BigDecimal.valueOf(80),
                    BigDecimal.valueOf(90),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(85),
                    BigDecimal.valueOf(95),
                    BigDecimal.valueOf(90)
            );

            assertThat(result.doubleValue()).isCloseTo(90.0, org.assertj.core.data.Offset.offset(0.1));
        }
    }
}
