package com.bagdatahouse.core.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 执行触发VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "执行触发VO")
public class ExecutionTriggerVO {

    @ApiModelProperty("执行ID")
    private Long executionId;

    @ApiModelProperty("执行编号")
    private String executionNo;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("耗时(毫秒)")
    private Long elapsedMs;

    @ApiModelProperty("总规则数")
    private Integer totalRules;

    @ApiModelProperty("通过规则数")
    private Integer passedRules;

    @ApiModelProperty("失败规则数")
    private Integer failedRules;

    @ApiModelProperty("跳过规则数")
    private Integer skippedRules;

    @ApiModelProperty("质量评分")
    private Integer qualityScore;

    @ApiModelProperty("是否阻断")
    private Boolean blocked;

    @ApiModelProperty("方案名称")
    private String planName;

    @ApiModelProperty("方案编码")
    private String planCode;
}
