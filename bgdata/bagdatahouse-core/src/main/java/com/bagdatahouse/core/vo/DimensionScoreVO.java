package com.bagdatahouse.core.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 维度评分VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "维度评分VO")
public class DimensionScoreVO {

    @ApiModelProperty("维度编码")
    private String dimensionCode;

    @ApiModelProperty("维度名称")
    private String dimensionName;

    @ApiModelProperty("维度描述")
    private String dimensionDesc;

    @ApiModelProperty("维度权重")
    private BigDecimal weight;

    @ApiModelProperty("评分")
    private BigDecimal score;

    @ApiModelProperty("评分等级")
    private String scoreGrade;

    @ApiModelProperty("评分颜色")
    private String scoreColor;

    @ApiModelProperty("涉及的规则数")
    private Integer ruleCount;

    @ApiModelProperty("通过规则数")
    private Integer passedCount;

    @ApiModelProperty("失败规则数")
    private Integer failedCount;

    @ApiModelProperty("通过率")
    private BigDecimal passRate;

    @ApiModelProperty("涉及的表数")
    private Integer tableCount;

    @ApiModelProperty("评分趋势")
    private String scoreTrend;

    @ApiModelProperty("评分变化")
    private BigDecimal scoreChange;

    @ApiModelProperty("需要关注的表")
    private List<TableScore> attentionTables;

    /**
     * 维度评分详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class TableScore {
        @ApiModelProperty("表名")
        private String tableName;

        @ApiModelProperty("数据源ID")
        private Long dsId;

        @ApiModelProperty("评分")
        private BigDecimal score;

        @ApiModelProperty("问题描述")
        private String issueDescription;
    }
}
