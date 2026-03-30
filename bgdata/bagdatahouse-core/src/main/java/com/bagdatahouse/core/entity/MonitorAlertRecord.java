package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
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
 * Alert record entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("monitor_alert_record")
@ApiModel(description = "Alert record")
public class MonitorAlertRecord {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Alert number")
    private String alertNo;

    @ApiModelProperty("Rule ID")
    private Long ruleId;

    @ApiModelProperty("Rule name")
    private String ruleName;

    @ApiModelProperty("Rule code")
    private String ruleCode;

    @ApiModelProperty("Alert level: info/warning/error/critical")
    private String alertLevel;

    @ApiModelProperty("Alert title")
    private String alertTitle;

    @ApiModelProperty("Alert content")
    private String alertContent;

    @ApiModelProperty("Target type")
    private String targetType;

    @ApiModelProperty("Target ID")
    private String targetId;

    @ApiModelProperty("Target name")
    private String targetName;

    @ApiModelProperty("Sensitive field level: L1/L2/L3/L4 (SENSITIVE type alert)")
    private String sensitivityLevel;

    @ApiModelProperty("Sensitive table name (SENSITIVE type alert)")
    private String sensitivityTable;

    @ApiModelProperty("Sensitive column name (SENSITIVE type alert)")
    private String sensitivityColumn;

    @ApiModelProperty("Sensitive datasource ID (SENSITIVE type alert)")
    private Long sensitivityDsId;

    @ApiModelProperty("Scan batch number (for SENSITIVE_FIELD_SPIKE alert tracing)")
    private String scanBatchNo;

    @ApiModelProperty("Trigger value")
    private BigDecimal triggerValue;

    @ApiModelProperty("Threshold value")
    private BigDecimal thresholdValue;

    @ApiModelProperty("Status: pending/sent/read/resolved")
    private String status;

    @ApiModelProperty("Sent channels (JSON array)")
    private String sentChannels;

    @ApiModelProperty("Sent time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentTime;

    @ApiModelProperty("Read time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    @ApiModelProperty("Read user ID")
    private Long readUser;

    @ApiModelProperty("Resolved time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedTime;

    @ApiModelProperty("Resolve user ID")
    private Long resolveUser;

    @ApiModelProperty("Resolve comment")
    private String resolveComment;

    @ApiModelProperty("Soft delete flag")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
