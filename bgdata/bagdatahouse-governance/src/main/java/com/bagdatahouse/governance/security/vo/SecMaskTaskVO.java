package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 脱敏任务列表项 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "脱敏任务列表项")
public class SecMaskTaskVO {

    @ApiModelProperty("任务ID")
    private Long id;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("任务编码")
    private String taskCode;

    @ApiModelProperty("任务类型")
    private String taskType;

    @ApiModelProperty("任务类型中文名")
    private String taskTypeLabel;

    @ApiModelProperty("源端数据源ID")
    private Long sourceDsId;

    @ApiModelProperty("源端数据源名称")
    private String sourceDsName;

    @ApiModelProperty("源端Schema")
    private String sourceSchema;

    @ApiModelProperty("目标端数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标端数据源名称")
    private String targetDsName;

    @ApiModelProperty("目标端Schema")
    private String targetSchema;

    @ApiModelProperty("目标端表名")
    private String targetTable;

    @ApiModelProperty("写入模式")
    private String targetMode;

    @ApiModelProperty("触发方式")
    private String triggerType;

    @ApiModelProperty("触发方式中文名")
    private String triggerTypeLabel;

    @ApiModelProperty("Cron表达式")
    private String triggerCron;

    @ApiModelProperty("任务状态")
    private String status;

    @ApiModelProperty("任务状态中文名")
    private String statusLabel;

    @ApiModelProperty("上次执行时间")
    private LocalDateTime lastRunTime;

    @ApiModelProperty("上次执行状态")
    private String lastRunStatus;

    @ApiModelProperty("上次处理行数")
    private Integer lastRunRows;

    @ApiModelProperty("累计执行次数")
    private Integer totalRunCount;

    @ApiModelProperty("备注")
    private String remark;

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
