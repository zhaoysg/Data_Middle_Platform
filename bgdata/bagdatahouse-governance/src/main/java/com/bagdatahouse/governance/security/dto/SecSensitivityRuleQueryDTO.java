package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 敏感字段识别规则查询 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段识别规则查询参数")
public class SecSensitivityRuleQueryDTO {

    @ApiModelProperty("规则编码")
    private String ruleCode;

    @ApiModelProperty("规则名称（模糊匹配）")
    private String ruleName;

    @ApiModelProperty("所属分类ID")
    private Long classId;

    @ApiModelProperty("推荐分级ID")
    private Long levelId;

    @ApiModelProperty("是否启用")
    private Integer enabled;

    @ApiModelProperty("匹配类型")
    private String matchType;

    @ApiModelProperty("是否内置：0-自定义，1-内置")
    private Integer builtin;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;
}
