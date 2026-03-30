package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 敏感字段识别规则列表项 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段识别规则列表项")
public class SecSensitivityRuleVO {

    @ApiModelProperty("规则ID")
    private Long id;

    @ApiModelProperty("规则名称")
    private String ruleName;

    @ApiModelProperty("规则编码")
    private String ruleCode;

    @ApiModelProperty("所属分类ID")
    private Long classId;

    @ApiModelProperty("所属分类名称")
    private String className;

    @ApiModelProperty("推荐分级ID")
    private Long levelId;

    @ApiModelProperty("推荐分级名称")
    private String levelName;

    @ApiModelProperty("推荐分级颜色")
    private String levelColor;

    @ApiModelProperty("匹配类型")
    private String matchType;

    @ApiModelProperty("匹配类型中文名")
    private String matchTypeLabel;

    @ApiModelProperty("匹配表达式")
    private String matchExpr;

    @ApiModelProperty("匹配方式")
    private String matchExprType;

    @ApiModelProperty("推荐处理方式")
    private String suggestionAction;

    @ApiModelProperty("推荐处理方式中文名")
    private String suggestionActionLabel;

    @ApiModelProperty("推荐脱敏格式")
    private String suggestionMaskPattern;

    @ApiModelProperty("推荐脱敏位置")
    private String suggestionMaskType;

    @ApiModelProperty("规则优先级")
    private Integer priority;

    @ApiModelProperty("是否内置")
    private Integer builtin;

    @ApiModelProperty("是否内置中文名")
    private String builtinLabel;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("状态中文名")
    private String statusLabel;

    @ApiModelProperty("创建者ID")
    private Long createUser;

    @ApiModelProperty("创建人用户名")
    private String createUserName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新者ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
