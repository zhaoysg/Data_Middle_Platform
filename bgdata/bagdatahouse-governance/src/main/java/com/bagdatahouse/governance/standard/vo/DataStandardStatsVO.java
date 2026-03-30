package com.bagdatahouse.governance.standard.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 数据标准统计 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据标准统计")
public class DataStandardStatsVO {

    @ApiModelProperty("标准总数")
    private Long totalCount;

    @ApiModelProperty("已发布标准数")
    private Long publishedCount;

    @ApiModelProperty("草稿标准数")
    private Long draftCount;

    @ApiModelProperty("已废弃标准数")
    private Long deprecatedCount;

    @ApiModelProperty("编码标准数")
    private Long codeStandardCount;

    @ApiModelProperty("命名规范数")
    private Long namingStandardCount;

    @ApiModelProperty("主数据标准数")
    private Long primaryDataCount;

    @ApiModelProperty("启用标准数")
    private Long enabledCount;

    @ApiModelProperty("禁用标准数")
    private Long disabledCount;

    @ApiModelProperty("绑定总数")
    private Long bindingCount;

    @ApiModelProperty("合规绑定数")
    private Long compliantBindingCount;

    @ApiModelProperty("不合规绑定数")
    private Long nonCompliantBindingCount;

    @ApiModelProperty("待检测绑定数")
    private Long pendingBindingCount;

    @ApiModelProperty("各类型标准数量")
    private Map<String, Long> countByType;
}
