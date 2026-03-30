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
 * 质量评分趋势VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "质量评分趋势VO")
public class QualityScoreTrendVO {

    @ApiModelProperty("时间范围")
    private String dateRange;

    @ApiModelProperty("开始日期")
    private String startDate;

    @ApiModelProperty("结束日期")
    private String endDate;

    @ApiModelProperty("数据层")
    private String layerCode;

    @ApiModelProperty("筛选维度")
    private String dimension;

    @ApiModelProperty("综合评分趋势数据")
    private List<TrendDataPoint> overallTrend;

    @ApiModelProperty("各维度趋势数据")
    private Map<String, List<TrendDataPoint>> dimensionTrends;

    @ApiModelProperty("趋势分析")
    private TrendAnalysis analysis;

    /** 评分趋势数据点 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class TrendDataPoint {
        private String date;
        private BigDecimal score;
        private BigDecimal passRate;
        private Integer ruleCount;
        private Integer passedCount;
        private Integer failedCount;
    }

    /** 趋势分析结果 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class TrendAnalysis {
        private BigDecimal avgScore;
        private BigDecimal maxScore;
        private BigDecimal minScore;
        private BigDecimal volatility;
        private String trendDirection;
        private BigDecimal trendChange;
        private BigDecimal trendChangePercent;
        private String overallAssessment;
        private List<String> attentionDimensions;
    }
}
