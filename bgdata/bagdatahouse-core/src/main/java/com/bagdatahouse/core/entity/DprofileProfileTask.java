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
 * Profile task entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dprofile_profile_task")
@ApiModel(description = "Profile task")
public class DprofileProfileTask {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Task name")
    private String taskName;

    @ApiModelProperty("Task code")
    private String taskCode;

    @ApiModelProperty("Task description")
    private String taskDesc;

    @ApiModelProperty("Target data source ID")
    private Long targetDsId;

    @ApiModelProperty("Target table name")
    private String targetTable;

    @ApiModelProperty("Target columns (comma-separated)")
    private String targetColumns;

    @ApiModelProperty("Target schema (for PostgreSQL)")
    private String targetSchema;

    @ApiModelProperty("Profile level: basic/detailed/full")
    private String profileLevel;

    @ApiModelProperty("Trigger type: manual/schedule/event")
    private String triggerType;

    @ApiModelProperty("Cron expression for scheduling")
    private String triggerCron;

    @ApiModelProperty("Timeout in minutes")
    private Integer timeoutMinutes;

    @ApiModelProperty("Status: DRAFT/PUBLISHED/DISABLED")
    private String status;

    @ApiModelProperty("Last execution ID")
    private Long lastExecutionId;

    @ApiModelProperty("Last execution time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecutionTime;

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
}
