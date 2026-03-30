package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Quality rule template entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dqc_rule_template")
@ApiModel(description = "Quality rule template")
public class DqcRuleTemplate {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Template code")
    private String templateCode;

    @ApiModelProperty("Template name")
    private String templateName;

    @ApiModelProperty("Template description")
    private String templateDesc;

    @ApiModelProperty("Rule type")
    private String ruleType;

    @ApiModelProperty("Apply level (table/column)")
    private String applyLevel;

    @ApiModelProperty("Default expression")
    private String defaultExpr;

    @ApiModelProperty("Default threshold (JSON string)")
    private String defaultThreshold;

    @ApiModelProperty("Parameter specification (JSON string)")
    private String paramSpec;

    @ApiModelProperty("Dimension (completeness/accuracy/consistency/etc)")
    private String dimension;

    @ApiModelProperty("Built-in template flag")
    private Boolean builtin;

    @ApiModelProperty("Enabled flag")
    private Boolean enabled;

    @ApiModelProperty("Creator user ID")
    private Long createUser;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("Updater user ID")
    private Long updateUser;

    @ApiModelProperty("Update time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
