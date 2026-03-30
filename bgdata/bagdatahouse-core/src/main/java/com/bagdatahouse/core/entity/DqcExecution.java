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
 * Quality execution record entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dqc_execution")
@ApiModel(description = "Quality execution record")
public class DqcExecution {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Execution number")
    private String executionNo;

    @ApiModelProperty("Plan ID")
    private Long planId;

    @ApiModelProperty("Plan name")
    private String planName;

    @ApiModelProperty("Plan code")
    private String planCode;

    @ApiModelProperty("Layer code")
    private String layerCode;

    @ApiModelProperty("Department ID")
    private Long deptId;

    @ApiModelProperty("Trigger type: manual/schedule/event")
    private String triggerType;

    @ApiModelProperty("Trigger user ID")
    private Long triggerUser;

    @ApiModelProperty("Trigger parameters (JSON string)")
    private String triggerParams;

    @ApiModelProperty("Start time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @ApiModelProperty("End time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty("Elapsed time in milliseconds")
    private Long elapsedMs;

    @ApiModelProperty("Status: running/success/failed/cancelled")
    private String status;

    @ApiModelProperty("Total rule count")
    private Integer totalRules;

    @ApiModelProperty("Passed rule count")
    private Integer passedRules;

    @ApiModelProperty("Failed rule count")
    private Integer failedRules;

    @ApiModelProperty("Skipped rule count")
    private Integer skippedRules;

    @ApiModelProperty("Total table count")
    private Integer totalTables;

    @ApiModelProperty("Overall quality score")
    private Integer qualityScore;

    @ApiModelProperty("Score breakdown (JSON string)")
    private String scoreBreakdown;

    @ApiModelProperty("Dimension scores (JSON string)")
    private String dimensionScores;

    @ApiModelProperty("Error detail summary")
    private String errorDetail;

    @ApiModelProperty("Log file path")
    private String logPath;

    @ApiModelProperty("Blocked flag")
    private Boolean blocked;

    @ApiModelProperty("Soft delete flag")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
