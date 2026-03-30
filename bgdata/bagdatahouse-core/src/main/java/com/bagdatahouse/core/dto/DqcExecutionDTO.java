package com.bagdatahouse.core.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DQC质检执行DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "DQC质检执行DTO")
public class DqcExecutionDTO {

    @ApiModelProperty("方案ID")
    private Long planId;

    @ApiModelProperty("触发类型: manual/schedule/event")
    private String triggerType;

    @ApiModelProperty("触发参数(JSON字符串)")
    private String triggerParams;
}
