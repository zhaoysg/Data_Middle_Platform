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
 * Monitor task execution entity
 * 任务执行记录实体，对应 monitor_task_execution 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("monitor_task_execution")
@ApiModel(description = "Monitor task execution record")
public class MonitorTaskExecution {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Task ID")
    private Long taskId;

    @ApiModelProperty("Task name")
    private String taskName;

    @ApiModelProperty("Task type: DQC_PROFILE/SCHEDULE/ALERT")
    private String taskType;

    @ApiModelProperty("Data source ID")
    private Long dsId;

    @ApiModelProperty("Trigger type: MANUAL/SCHEDULE/API")
    private String triggerType;

    @ApiModelProperty("Trigger user ID")
    private Long triggerUser;

    @ApiModelProperty("Start time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @ApiModelProperty("End time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty("Elapsed time in milliseconds")
    private Long elapsedMs;

    @ApiModelProperty("Status: RUNNING/SUCCESS/FAILED/TIMEOUT/CANCELLED")
    private String status;

    @ApiModelProperty("Progress percentage (0-100)")
    private Integer progress;

    @ApiModelProperty("Execution log content")
    private String logContent;

    @ApiModelProperty("Error message")
    private String errorMsg;

    @ApiModelProperty("Plan ID (for DQC executions)")
    @TableField(exist = false)
    private Long planId;

    @ApiModelProperty("Plan name (for display)")
    @TableField(exist = false)
    private String planName;

    @ApiModelProperty("Data source name (for display)")
    @TableField(exist = false)
    private String dsName;

    @ApiModelProperty("Trigger user name (for display)")
    @TableField(exist = false)
    private String triggerUserName;

    @ApiModelProperty("Task type name (for display)")
    @TableField(exist = false)
    private String taskTypeName;

    @ApiModelProperty("Soft delete flag")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
