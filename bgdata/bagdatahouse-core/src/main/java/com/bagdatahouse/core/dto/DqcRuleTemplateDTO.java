package com.bagdatahouse.core.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DQC规则模板DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "DQC规则模板DTO")
public class DqcRuleTemplateDTO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("模板编码")
    private String templateCode;

    @ApiModelProperty("模板名称")
    private String templateName;

    @ApiModelProperty("模板描述")
    private String templateDesc;

    @ApiModelProperty("规则类型")
    private String ruleType;

    @ApiModelProperty("适用级别(table/column)")
    private String applyLevel;

    @ApiModelProperty("默认表达式")
    private String defaultExpr;

    @ApiModelProperty("默认阈值(JSON字符串)")
    private String defaultThreshold;

    @ApiModelProperty("参数规范(JSON字符串)")
    private String paramSpec;

    @ApiModelProperty("维度")
    private String dimension;

    @ApiModelProperty("是否内置模板")
    private Boolean builtin;

    @ApiModelProperty("是否启用")
    private Boolean enabled;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新人ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
