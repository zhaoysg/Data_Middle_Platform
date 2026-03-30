package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 数据安全分级统计 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据安全分级统计")
public class SecStatsVO {

    @ApiModelProperty("分类总数")
    private Long totalClassificationCount;

    @ApiModelProperty("分级总数")
    private Long totalLevelCount;

    @ApiModelProperty("规则总数（含内置）")
    private Long totalRuleCount;

    @ApiModelProperty("内置规则数")
    private Long builtinRuleCount;

    @ApiModelProperty("自定义规则数")
    private Long customRuleCount;

    @ApiModelProperty("敏感字段总数")
    private Long totalSensitiveFieldCount;

    @ApiModelProperty("待审核字段数")
    private Long pendingReviewCount;

    @ApiModelProperty("已审核字段数")
    private Long approvedCount;

    @ApiModelProperty("已驳回字段数")
    private Long rejectedCount;

    @ApiModelProperty("脱敏模板总数")
    private Long totalMaskTemplateCount;

    @ApiModelProperty("按数据分类分布")
    private Map<String, Long> countByClassification;

    @ApiModelProperty("按数据分级分布")
    private Map<String, Long> countByLevel;

    @ApiModelProperty("按审核状态分布")
    private Map<String, Long> countByReviewStatus;

    @ApiModelProperty("按脱敏方式分布")
    private Map<String, Long> countByMaskType;

    @ApiModelProperty("按匹配类型分布")
    private Map<String, Long> countByMatchType;
}
