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
 * 质量评分概览VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "质量评分概览VO")
public class QualityScoreOverviewVO {

    @ApiModelProperty("时间范围")
    private String dateRange;

    @ApiModelProperty("开始日期")
    private String startDate;

    @ApiModelProperty("结束日期")
    private String endDate;

    @ApiModelProperty("综合评分")
    private BigDecimal overallScore;

    @ApiModelProperty("评分等级: EXCELLENT/GOOD/WARNING/CRITICAL")
    private String scoreGrade;

    @ApiModelProperty("评分等级颜色")
    private String scoreColor;

    @ApiModelProperty("评分趋势: UP/DOWN/FLAT")
    private String scoreTrend;

    @ApiModelProperty("评分变化值")
    private BigDecimal scoreChange;

    @ApiModelProperty("评分变化百分比")
    private BigDecimal scoreChangePercent;

    @ApiModelProperty("规则通过率")
    private BigDecimal rulePassRate;

    @ApiModelProperty("总规则数")
    private Integer totalRules;

    @ApiModelProperty("通过规则数")
    private Integer passedRules;

    @ApiModelProperty("失败规则数")
    private Integer failedRules;

    @ApiModelProperty("六维度评分详情")
    private DimensionScores dimensionScores;

    @ApiModelProperty("各数据层评分")
    private Map<String, BigDecimal> layerScores;

    @ApiModelProperty("评分分布统计")
    private ScoreDistribution distribution;

    @ApiModelProperty("质量异常列表")
    private List<QualityIssue> issues;

    @ApiModelProperty("综合评价")
    private String overallAssessment;

    /** 六维度评分 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class DimensionScores {
        private BigDecimal completeness;
        private BigDecimal uniqueness;
        private BigDecimal accuracy;
        private BigDecimal consistency;
        private BigDecimal timeliness;
        private BigDecimal validity;
    }

    /** 评分分布 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class ScoreDistribution {
        private Integer excellentCount;
        private Integer goodCount;
        private Integer warningCount;
        private Integer criticalCount;
        private BigDecimal excellentPercent;
        private BigDecimal goodPercent;
    }

    /** 质量问题 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class QualityIssue {
        private String tableName;
        private Long dsId;
        private BigDecimal score;
        private String issueType;
        private String issueDescription;
        private String dimension;
    }
}
