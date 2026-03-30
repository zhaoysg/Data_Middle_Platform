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
 * Quality check plan entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dqc_plan")
@ApiModel(description = "Quality check plan")
public class DqcPlan {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Plan name")
    private String planName;

    @ApiModelProperty("Plan code")
    private String planCode;

    @ApiModelProperty("Plan description")
    private String planDesc;

    @ApiModelProperty("Bind type (layer/domain/table)")
    private String bindType;

    @ApiModelProperty("Bind value (layer code/domain id/table pattern)")
    private String bindValue;

    @ApiModelProperty("Layer code")
    private String layerCode;

    @ApiModelProperty("Department ID")
    private Long deptId;

    @ApiModelProperty("Trigger type: manual/schedule/event")
    private String triggerType;

    @ApiModelProperty("Cron expression for scheduling")
    private String triggerCron;

    @ApiModelProperty("Alert on success")
    private Boolean alertOnSuccess;

    @ApiModelProperty("Alert on failure")
    private Boolean alertOnFailure;

    @ApiModelProperty("Auto block on failure")
    private Boolean autoBlock;

    @ApiModelProperty("Status: DRAFT-草稿/PUBLISHED-已发布/DISABLED-已停用")
    private String status;

    @ApiModelProperty("Total rule count")
    private Integer ruleCount;

    @ApiModelProperty("Total table count")
    private Integer tableCount;

    @ApiModelProperty("Last execution ID")
    private Long lastExecutionId;

    @ApiModelProperty("Last execution time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecutionTime;

    @ApiModelProperty("Last execution quality score")
    private Integer lastExecutionScore;

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

    @ApiModelProperty("数据层名称(展示用)")
    @TableField(exist = false)
    private String layerName;

    @ApiModelProperty("部门名称(展示用)")
    @TableField(exist = false)
    private String deptName;

    @ApiModelProperty("状态名称(展示用)")
    @TableField(exist = false)
    private String statusName;

    @ApiModelProperty("触发类型名称(展示用)")
    @TableField(exist = false)
    private String triggerTypeName;

    @ApiModelProperty("创建人名称(展示用)")
    @TableField(exist = false)
    private String createUserName;

    @ApiModelProperty("最后执行状态(展示用:EXCELLENT/GOOD/WARNING/CRITICAL)")
    @TableField(exist = false)
    private String lastExecutionStatus;
}
