package com.bagdatahouse.governance.glossary.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 术语库统计信息 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "术语库统计信息")
public class GlossaryStatsVO {

    @ApiModelProperty("术语总数")
    private Long totalTermCount;

    @ApiModelProperty("已发布术语数")
    private Long publishedTermCount;

    @ApiModelProperty("草稿术语数")
    private Long draftTermCount;

    @ApiModelProperty("已废弃术语数")
    private Long deprecatedTermCount;

    @ApiModelProperty("分类总数")
    private Long totalCategoryCount;

    @ApiModelProperty("顶级分类数")
    private Long topCategoryCount;

    @ApiModelProperty("映射总数")
    private Long totalMappingCount;

    @ApiModelProperty("待审批映射数")
    private Long pendingMappingCount;

    @ApiModelProperty("已审批映射数")
    private Long approvedMappingCount;

    @ApiModelProperty("术语数按业务域分布")
    private Map<String, Long> termCountByBizDomain;

    @ApiModelProperty("术语数按数据类型分布")
    private Map<String, Long> termCountByDataType;

    @ApiModelProperty("术语数按敏感等级分布")
    private Map<String, Long> termCountBySensitivityLevel;
}
