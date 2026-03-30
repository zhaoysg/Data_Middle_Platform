package com.bagdatahouse.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警记录DTO（用于更新告警状态）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "告警记录DTO")
public class MonitorAlertRecordDTO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("告警编号")
    private String alertNo;

    @ApiModelProperty("告警级别")
    private String alertLevel;

    @ApiModelProperty("告警标题")
    private String alertTitle;

    @ApiModelProperty("告警内容")
    private String alertContent;

    @ApiModelProperty("目标类型")
    private String targetType;

    @ApiModelProperty("目标ID")
    private String targetId;

    @ApiModelProperty("目标名称")
    private String targetName;

    @ApiModelProperty("触发值")
    private BigDecimal triggerValue;

    @ApiModelProperty("阈值")
    private BigDecimal thresholdValue;

    @ApiModelProperty("状态: PENDING/SENT/READ/RESOLVED")
    private String status;

    @ApiModelProperty("已发送渠道")
    private String sentChannels;

    @ApiModelProperty("发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentTime;

    @ApiModelProperty("已读时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    @ApiModelProperty("已读用户ID")
    private Long readUser;

    @ApiModelProperty("解决时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedTime;

    @ApiModelProperty("解决人用户ID")
    private Long resolveUser;

    @ApiModelProperty("解决说明")
    private String resolveComment;

    @ApiModelProperty("规则ID")
    private Long ruleId;

    @ApiModelProperty("规则名称")
    private String ruleName;

    @ApiModelProperty("规则编码")
    private String ruleCode;
}
