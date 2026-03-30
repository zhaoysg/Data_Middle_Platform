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
 * 静态脱敏任务实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_mask_task")
@ApiModel(description = "静态脱敏任务")
public class SecMaskTask {

    @ApiModelProperty("任务ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("任务编码（唯一）")
    private String taskCode;

    @ApiModelProperty("任务类型：STATIC-静态脱敏 / DYNAMIC-动态脱敏")
    private String taskType;

    @ApiModelProperty("源端数据源ID")
    private Long sourceDsId;

    @ApiModelProperty("源端查询SQL（支持 ${table} 占位符）")
    private String sourceSql;

    @ApiModelProperty("源端Schema")
    private String sourceSchema;

    @ApiModelProperty("目标端数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标端Schema")
    private String targetSchema;

    @ApiModelProperty("目标端表名")
    private String targetTable;

    @ApiModelProperty("写入模式：APPEND-追加 / TRUNCATE-清空重写 / UPSERT-upsert")
    private String targetMode;

    @ApiModelProperty("脱敏规则JSON")
    private String maskRules;

    @ApiModelProperty("触发方式：MANUAL-手动 / SCHEDULED-定时 / EVENT-事件触发")
    private String triggerType;

    @ApiModelProperty("Cron表达式")
    private String triggerCron;

    @ApiModelProperty("每批处理行数")
    private Integer batchSize;

    @ApiModelProperty("任务状态：DRAFT-草稿 / PUBLISHED-已发布 / RUNNING-执行中 / SUCCESS-成功 / FAILED-失败 / CANCELLED-已取消")
    private String status;

    @ApiModelProperty("上次执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRunTime;

    @ApiModelProperty("上次执行状态")
    private String lastRunStatus;

    @ApiModelProperty("上次处理行数")
    private Integer lastRunRows;

    @ApiModelProperty("上次执行错误信息")
    private String lastRunError;

    @ApiModelProperty("累计执行次数")
    private Integer totalRunCount;

    @ApiModelProperty("备注说明")
    private String remark;

    @ApiModelProperty("删除标记")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("创建者")
    private Long createUser;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新者")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
