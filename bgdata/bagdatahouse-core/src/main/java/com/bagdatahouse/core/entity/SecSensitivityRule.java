package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 敏感字段识别规则实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_sensitivity_rule")
@ApiModel(description = "敏感字段识别规则")
public class SecSensitivityRule {

    @ApiModelProperty("规则ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("规则名称")
    private String ruleName;

    @ApiModelProperty("规则编码，唯一")
    private String ruleCode;

    @ApiModelProperty("规则描述")
    private String description;

    @ApiModelProperty("所属分类ID")
    private Long classId;

    @ApiModelProperty("推荐分级ID")
    private Long levelId;

    @ApiModelProperty("匹配类型：COLUMN_NAME/COLUMN_COMMENT/DATA_TYPE/REGEX")
    private String matchType;

    @ApiModelProperty("匹配表达式")
    private String matchExpr;

    @ApiModelProperty("匹配方式：CONTAINS/EQUALS/STARTS_WITH/REGEX")
    private String matchExprType;

    @ApiModelProperty("推荐处理方式：NONE/MASK/HIDE/ENCRYPT/DELETE")
    private String suggestionAction;

    @ApiModelProperty("推荐脱敏格式")
    private String suggestionMaskPattern;

    @ApiModelProperty("脱敏类型：CENTER/TAIL/HEAD/FULL")
    private String suggestionMaskType;

    @ApiModelProperty("规则优先级，数值越大优先级越高")
    private Integer priority;

    @ApiModelProperty("是否内置规则：0-自定义，1-内置")
    private Integer builtin;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("删除标记")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("创建者")
    private Long createUser;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新者")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
