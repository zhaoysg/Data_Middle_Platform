package com.bagdatahouse.core.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 质量报告VO（综合版 — 从 dqc_execution_detail 实时聚合真实维度评分）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "质量报告VO（综合版）")
public class QualityReportVO {

    @ApiModelProperty("报告标题")
    private String reportTitle;

    @ApiModelProperty("报告生成时间")
    private String generateTime;

    @ApiModelProperty("报告时间范围")
    private String dateRange;

    @ApiModelProperty("关联方案ID")
    private Long planId;

    @ApiModelProperty("关联方案名称")
    private String planName;

    // ==================== 概览统计 ====================

    @ApiModelProperty("综合评分（百分制）")
    private BigDecimal overallScore;

    @ApiModelProperty("评分等级 EXCELLENT/GOOD/WARNING/CRITICAL")
    private String scoreGrade;

    @ApiModelProperty("评分颜色（前端颜色值）")
    private String scoreColor;

    @ApiModelProperty("规则通过率（%）")
    private BigDecimal rulePassRate;

    @ApiModelProperty("总规则数")
    private Integer totalRules;

    @ApiModelProperty("通过规则数")
    private Integer passedRules;

    @ApiModelProperty("失败规则数")
    private Integer failedRules;

    @ApiModelProperty("跳过规则数")
    private Integer skippedRules;

    @ApiModelProperty("阻断执行数")
    private Integer blockedCount;

    @ApiModelProperty("执行总数")
    private Integer executionCount;

    @ApiModelProperty("平均执行耗时(ms)")
    private Long avgElapsedMs;

    // ==================== 六维度评分（真实计算）====================

    @ApiModelProperty("六维度评分详情")
    private DimensionScoresReport dimensionScores;

    // ==================== 评分趋势 ====================

    @ApiModelProperty("评分趋势数据点列表（按时间倒序）")
    private List<ScoreTrendPoint> scoreTrend;

    @ApiModelProperty("评分趋势分析")
    private TrendAnalysis trendAnalysis;

    // ==================== 问题分析 ====================

    @ApiModelProperty("失败规则TopN")
    private List<FailedRuleInfo> topFailedRules;

    @ApiModelProperty("需要关注的表（评分低于阈值）")
    private List<AttentionTable> attentionTables;

    @ApiModelProperty("质量异常摘要")
    private String issueSummary;

    // ==================== 执行记录 ====================

    @ApiModelProperty("最近执行记录")
    private List<ExecutionSummary> recentExecutions;

    // ==================== 数据层评分 ====================

    @ApiModelProperty("各数据层评分分布")
    private Map<String, BigDecimal> layerScores;

    // ==================== 评分分布 ====================

    @ApiModelProperty("评分分布统计")
    private ScoreDistribution distribution;

    // ==================== 综合评价 ====================

    @ApiModelProperty("综合评价建议")
    private String overallAssessment;

    @ApiModelProperty("改进建议列表")
    private List<String> improvementSuggestions;

    // ==================== 内部类定义 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DimensionScoresReport {
        @ApiModelProperty("完整性评分")
        private BigDecimal completeness;

        @ApiModelProperty("唯一性评分")
        private BigDecimal uniqueness;

        @ApiModelProperty("准确性评分")
        private BigDecimal accuracy;

        @ApiModelProperty("一致性评分")
        private BigDecimal consistency;

        @ApiModelProperty("及时性评分")
        private BigDecimal timeliness;

        @ApiModelProperty("有效性评分")
        private BigDecimal validity;

        @ApiModelProperty("各维度评分映射（用于前端图表）")
        private Map<String, BigDecimal> toMap() {
            return Map.of(
                    "完整性", completeness != null ? completeness : BigDecimal.ZERO,
                    "唯一性", uniqueness != null ? uniqueness : BigDecimal.ZERO,
                    "准确性", accuracy != null ? accuracy : BigDecimal.ZERO,
                    "一致性", consistency != null ? consistency : BigDecimal.ZERO,
                    "及时性", timeliness != null ? timeliness : BigDecimal.ZERO,
                    "有效性", validity != null ? validity : BigDecimal.ZERO
            );
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreTrendPoint {
        @ApiModelProperty("日期")
        private String date;

        @ApiModelProperty("评分值")
        private BigDecimal score;

        @ApiModelProperty("通过率")
        private BigDecimal passRate;

        @ApiModelProperty("通过规则数")
        private Integer passedCount;

        @ApiModelProperty("失败规则数")
        private Integer failedCount;

        @ApiModelProperty("总规则数")
        private Integer totalCount;

        @ApiModelProperty("综合评分（百分制）")
        private BigDecimal overallScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendAnalysis {
        @ApiModelProperty("平均评分")
        private BigDecimal avgScore;

        @ApiModelProperty("最高评分")
        private BigDecimal maxScore;

        @ApiModelProperty("最低评分")
        private BigDecimal minScore;

        @ApiModelProperty("评分波动（标准差）")
        private BigDecimal volatility;

        @ApiModelProperty("趋势方向 UP/DOWN/FLAT")
        private String trendDirection;

        @ApiModelProperty("趋势变化值")
        private BigDecimal trendChange;

        @ApiModelProperty("趋势变化百分比")
        private BigDecimal trendChangePercent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FailedRuleInfo {
        @ApiModelProperty("规则名称")
        private String ruleName;

        @ApiModelProperty("规则编码")
        private String ruleCode;

        @ApiModelProperty("失败次数")
        private Integer errorCount;

        @ApiModelProperty("最近失败时间")
        private String lastFailTime;

        @ApiModelProperty("涉及表数")
        private Integer tableCount;

        @ApiModelProperty("所属维度")
        private String dimension;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttentionTable {
        @ApiModelProperty("表名")
        private String tableName;

        @ApiModelProperty("数据源ID")
        private Long dsId;

        @ApiModelProperty("数据层")
        private String layerCode;

        @ApiModelProperty("评分")
        private BigDecimal score;

        @ApiModelProperty("问题描述")
        private String issueDescription;

        @ApiModelProperty("涉及维度")
        private String dimension;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecutionSummary {
        @ApiModelProperty("执行ID")
        private Long executionId;

        @ApiModelProperty("执行编号")
        private String executionNo;

        @ApiModelProperty("状态")
        private String status;

        @ApiModelProperty("开始时间")
        private String startTime;

        @ApiModelProperty("耗时")
        private Long elapsedMs;

        @ApiModelProperty("总规则数")
        private Integer totalRules;

        @ApiModelProperty("通过规则数")
        private Integer passedRules;

        @ApiModelProperty("失败规则数")
        private Integer failedRules;

        @ApiModelProperty("质量评分")
        private Integer qualityScore;

        @ApiModelProperty("是否阻断")
        private Boolean blocked;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreDistribution {
        @ApiModelProperty("优秀(90-100)数量")
        private Integer excellentCount;

        @ApiModelProperty("良好(70-89)数量")
        private Integer goodCount;

        @ApiModelProperty("警告(60-69)数量")
        private Integer warningCount;

        @ApiModelProperty("危险(0-59)数量")
        private Integer criticalCount;

        @ApiModelProperty("优秀占比")
        private BigDecimal excellentPercent;

        @ApiModelProperty("良好占比")
        private BigDecimal goodPercent;

        @ApiModelProperty("警告占比")
        private BigDecimal warningPercent;

        @ApiModelProperty("危险占比")
        private BigDecimal criticalPercent;
    }
}
