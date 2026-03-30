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
 * Quality rule definition entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dqc_rule_def")
@ApiModel(description = "Quality rule definition")
public class DqcRuleDef {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Rule name")
    private String ruleName;

    @ApiModelProperty("Rule code")
    private String ruleCode;

    @ApiModelProperty("Template ID")
    private Long templateId;

    @ApiModelProperty("Rule type")
    private String ruleType;

    @ApiModelProperty("Apply level (table/column)")
    private String applyLevel;

    @ApiModelProperty("Data layer code (e.g. from dq_datasource.data_layer)")
    private String layerCode;

    @ApiModelProperty("Dimensions (JSON array)")
    private String dimensions;

    @ApiModelProperty("Rule expression/SQL")
    private String ruleExpr;

    @ApiModelProperty("Target data source ID")
    private Long targetDsId;

    @ApiModelProperty("Target table name")
    private String targetTable;

    @ApiModelProperty("Target column name")
    private String targetColumn;

    @ApiModelProperty("Compare data source ID")
    private Long compareDsId;

    @ApiModelProperty("Compare table name")
    private String compareTable;

    @ApiModelProperty("Compare column name")
    private String compareColumn;

    @ApiModelProperty("Minimum threshold")
    private BigDecimal thresholdMin;

    @ApiModelProperty("Maximum threshold")
    private BigDecimal thresholdMax;

    @ApiModelProperty("Fluctuation threshold percentage")
    private BigDecimal fluctuationThreshold;

    @ApiModelProperty("Regex pattern for validity checks")
    private String regexPattern;

    @ApiModelProperty("Error level: low/medium/high/critical")
    private String errorLevel;

    @ApiModelProperty("Rule strength")
    private String ruleStrength;

    @ApiModelProperty("Alert receivers (JSON array)")
    private String alertReceivers;

    @ApiModelProperty("Sort order")
    private Integer sortOrder;

    @ApiModelProperty("Enabled flag")
    private Boolean enabled;

    @ApiModelProperty("Department ID")
    private Long deptId;

    @ApiModelProperty("Soft delete flag")
    @TableLogic
    private Integer deleted;

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

    @ApiModelProperty("Custom function class full name (for CUSTOM_FUNC type)")
    private String customFunctionClass;

    @ApiModelProperty("Custom function parameters in JSON format")
    private String customFunctionParams;

    // ========== Display-only fields (not persisted) ==========
    @ApiModelProperty("Rule type name (display)")
    @TableField(exist = false)
    private String ruleTypeName;

    @ApiModelProperty("Apply level name (display)")
    @TableField(exist = false)
    private String applyLevelName;

    @ApiModelProperty("Rule strength name (display)")
    @TableField(exist = false)
    private String ruleStrengthName;

    @ApiModelProperty("Error level name (display)")
    @TableField(exist = false)
    private String errorLevelName;

    @ApiModelProperty("Target datasource name (display)")
    @TableField(exist = false)
    private String targetDsName;

    @ApiModelProperty("Template name (display)")
    @TableField(exist = false)
    private String templateName;
}
