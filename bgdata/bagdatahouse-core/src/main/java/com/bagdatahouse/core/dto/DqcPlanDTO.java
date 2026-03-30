package com.bagdatahouse.core.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DQC质检方案DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "DQC质检方案DTO")
public class DqcPlanDTO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("方案名称")
    private String planName;

    @ApiModelProperty("方案编码")
    private String planCode;

    @ApiModelProperty("方案描述")
    private String planDesc;

    @ApiModelProperty("绑定类型(layer/domain/table)")
    private String bindType;

    @ApiModelProperty("绑定值(layer code/domain id/table pattern)")
    private String bindValue;

    @ApiModelProperty("数据层编码")
    private String layerCode;

    @ApiModelProperty("部门ID")
    private Long deptId;

    @ApiModelProperty("触发类型: manual/schedule/event")
    private String triggerType;

    @ApiModelProperty("调度Cron表达式")
    private String triggerCron;

    @ApiModelProperty("成功时告警")
    private Boolean alertOnSuccess;

    @ApiModelProperty("失败时告警")
    private Boolean alertOnFailure;

    @ApiModelProperty("失败时自动阻断")
    private Boolean autoBlock;

    @ApiModelProperty("状态: DRAFT-草稿/PUBLISHED-已发布/DISABLED-已停用")
    private String status;

    @ApiModelProperty("规则数量")
    private Integer ruleCount;

    @ApiModelProperty("表数量")
    private Integer tableCount;

    @ApiModelProperty("最后执行ID")
    private Long lastExecutionId;

    @ApiModelProperty("最后执行时间")
    private LocalDateTime lastExecutionTime;

    @ApiModelProperty("最后执行质量分数")
    private Integer lastExecutionScore;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新人ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("数据层名称(展示用)")
    private String layerName;

    @ApiModelProperty("部门名称(展示用)")
    private String deptName;
}
