package com.bagdatahouse.core.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 方案执行状态VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "方案执行状态VO")
public class PlanExecutionStatusVO {

    @ApiModelProperty("方案ID")
    private Long planId;

    @ApiModelProperty("方案名称")
    private String planName;

    @ApiModelProperty("方案编码")
    private String planCode;

    @ApiModelProperty("触发类型")
    private String triggerType;

    @ApiModelProperty("是否正在执行")
    private Boolean isRunning;

    @ApiModelProperty("上次执行ID")
    private Long lastExecutionId;

    @ApiModelProperty("上次执行编号")
    private String lastExecutionNo;

    @ApiModelProperty("上次执行状态")
    private String lastExecutionStatus;

    @ApiModelProperty("上次执行开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecutionStartTime;

    @ApiModelProperty("上次执行结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecutionEndTime;

    @ApiModelProperty("上次执行耗时(毫秒)")
    private Long lastExecutionElapsedMs;

    @ApiModelProperty("上次质量评分")
    private Integer lastQualityScore;

    @ApiModelProperty("上次执行规则总数")
    private Integer lastTotalRules;

    @ApiModelProperty("上次执行通过规则数")
    private Integer lastPassedRules;

    @ApiModelProperty("上次执行失败规则数")
    private Integer lastFailedRules;

    @ApiModelProperty("下次执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextExecutionTime;

    @ApiModelProperty("Cron表达式")
    private String triggerCron;

    @ApiModelProperty("Cron表达式是否有效")
    private Boolean cronValid;

    @ApiModelProperty("距下次执行时间(秒)")
    private Long secondsToNextExecution;

    @ApiModelProperty("方案状态")
    private String status;

    @ApiModelProperty("方案状态名称")
    private String statusName;
}
