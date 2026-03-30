package com.bagdatahouse.core.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cron表达式验证VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "Cron表达式验证VO")
public class CronValidationVO {

    @ApiModelProperty("Cron表达式")
    private String cronExpression;

    @ApiModelProperty("是否有效")
    private Boolean valid;

    @ApiModelProperty("错误信息")
    private String errorMessage;

    @ApiModelProperty("下次执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextExecutionTime;

    @ApiModelProperty("未来10次执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private List<LocalDateTime> next10Executions;

    @ApiModelProperty("Cron描述（人类可读）")
    private String description;

    @ApiModelProperty("触发类型描述")
    private String triggerTypeDescription;
}
