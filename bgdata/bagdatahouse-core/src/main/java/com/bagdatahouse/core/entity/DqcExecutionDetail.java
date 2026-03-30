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
 * Quality execution detail entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dqc_execution_detail")
@ApiModel(description = "Quality execution detail")
public class DqcExecutionDetail {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Execution ID")
    private Long executionId;

    @ApiModelProperty("Execution number")
    private String executionNo;

    @ApiModelProperty("Rule ID")
    private Long ruleId;

    @ApiModelProperty("Rule name")
    private String ruleName;

    @ApiModelProperty("Rule code")
    private String ruleCode;

    @ApiModelProperty("Rule type")
    private String ruleType;

    @ApiModelProperty("Rule strength")
    private String ruleStrength;

    @ApiModelProperty("Dimensions")
    private String dimensions;

    @ApiModelProperty("Target data source ID")
    private Long targetDsId;

    @ApiModelProperty("Target table name")
    private String targetTable;

    @ApiModelProperty("Target column name")
    private String targetColumn;

    @ApiModelProperty("Start time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @ApiModelProperty("End time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty("Elapsed time in milliseconds")
    private Long elapsedMs;

    @ApiModelProperty("Status: running/success/failed/skipped")
    private String status;

    @ApiModelProperty("Total count")
    private Long totalCount;

    @ApiModelProperty("Error count")
    private Long errorCount;

    @ApiModelProperty("Pass count")
    private Long passCount;

    @ApiModelProperty("Actual value")
    private BigDecimal actualValue;

    @ApiModelProperty("Minimum threshold")
    private BigDecimal thresholdMin;

    @ApiModelProperty("Maximum threshold")
    private BigDecimal thresholdMax;

    @ApiModelProperty("Quality score for this rule")
    private Integer qualityScore;

    @ApiModelProperty("Error detail")
    private String errorDetail;

    @ApiModelProperty("SQL content executed")
    private String sqlContent;

    @ApiModelProperty("Sensitivity level: L1/L2/L3/L4 (joined from sec_column_sensitivity)")
    private String sensitivityLevel;

    @ApiModelProperty("Sensitivity class (joined from sec_column_sensitivity)")
    private String sensitivityClass;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
