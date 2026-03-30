package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 脱敏执行日志 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "脱敏执行日志")
public class SecMaskExecutionLogVO {

    @ApiModelProperty("日志ID")
    private Long id;

    @ApiModelProperty("脱敏任务ID")
    private Long taskId;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("任务编码")
    private String taskCode;

    @ApiModelProperty("本次执行时间")
    private LocalDateTime runTime;

    @ApiModelProperty("执行状态")
    private String status;

    @ApiModelProperty("执行状态中文名")
    private String statusLabel;

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

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
