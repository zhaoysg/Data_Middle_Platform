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
 * 脱敏执行日志实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_mask_execution_log")
@ApiModel(description = "脱敏执行日志")
public class SecMaskExecutionLog {

    @ApiModelProperty("日志ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("脱敏任务ID")
    private Long taskId;

    @ApiModelProperty("任务编码")
    private String taskCode;

    @ApiModelProperty("本次执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime runTime;

    @ApiModelProperty("执行状态：RUNNING-执行中 / SUCCESS-成功 / FAILED-失败 / CANCELLED-取消")
    private String status;

    @ApiModelProperty("总处理行数")
    private Integer totalRows;

    @ApiModelProperty("脱敏行数")
    private Integer maskedRows;

    @ApiModelProperty("错误行数")
    private Integer errorRows;

    @ApiModelProperty("执行耗时（毫秒）")
    private Long durationMs;

    @ApiModelProperty("错误信息")
    private String errorMessage;

    @ApiModelProperty("源端总行数")
    private Long sourceRowCount;

    @ApiModelProperty("执行的SQL摘要日志")
    private String executeSqlLog;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
