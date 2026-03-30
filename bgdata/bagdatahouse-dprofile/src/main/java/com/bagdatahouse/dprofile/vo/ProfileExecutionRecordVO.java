package com.bagdatahouse.dprofile.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 探查执行记录 VO（用于追踪任务执行状态）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "探查执行记录")
public class ProfileExecutionRecordVO {

    @ApiModelProperty("执行ID")
    private Long executionId;

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("目标数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标表名")
    private String tableName;

    @ApiModelProperty("执行状态: PENDING/RUNNING/SUCCESS/FAILED/SKIPPED")
    private String status;

    @ApiModelProperty("探查阶段: TABLE_STATS/COLUMN_STATS/COMPLETED")
    private String phase;

    @ApiModelProperty("进度百分比(0-100)")
    private Integer progress;

    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("耗时(ms)")
    private Long elapsedMs;

    @ApiModelProperty("执行信息")
    private String message;

    @ApiModelProperty("错误信息")
    private String errorMessage;

    @ApiModelProperty("结果记录ID")
    private Long tableStatsId;
}
