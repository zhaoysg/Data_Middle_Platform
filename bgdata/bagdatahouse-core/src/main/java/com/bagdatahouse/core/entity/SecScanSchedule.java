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
 * 敏感字段定时扫描任务实体
 * <p>
 * 存储定时扫描周期配置，支持：
 * - ONCE：单次扫描
 * - DAILY：每日扫描
 * - WEEKLY：每周扫描
 * - MONTHLY：每月扫描
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_scan_schedule")
@ApiModel(description = "敏感字段定时扫描任务")
public class SecScanSchedule {

    @ApiModelProperty("任务ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("任务编码（唯一）")
    private String taskCode;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("扫描范围：ALL-全部表/SPECIFIC-指定表/LAYER-按数据层")
    private String scanScope;

    @ApiModelProperty("指定表名列表（逗号分隔），scanScope=SPECIFIC 时使用")
    private String tableNames;

    @ApiModelProperty("数据层，scanScope=LAYER 时使用：ODS/DWD/DWS/ADS")
    private String layerCode;

    @ApiModelProperty("是否启用内容级扫描")
    private Boolean enableContentScan;

    @ApiModelProperty("采样数量上限")
    private Integer sampleSize;

    @ApiModelProperty("扫描周期：ONCE/DAILY/WEEKLY/MONTHLY")
    private String scanCycle;

    @ApiModelProperty("Cron 表达式（可选，手动指定）")
    private String cronExpression;

    @ApiModelProperty("状态：ENABLED-启用/DISABLED-停用")
    private String status;

    @ApiModelProperty("最近执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRunTime;

    @ApiModelProperty("最近执行状态：SUCCESS/FAILED/RUNNING")
    private String lastRunStatus;

    @ApiModelProperty("最近执行批次号")
    private String lastBatchNo;

    @ApiModelProperty("最近执行耗时（秒）")
    private Integer lastDurationSeconds;

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

    @ApiModelProperty("删除标记")
    @TableLogic
    private Integer deleted;
}
