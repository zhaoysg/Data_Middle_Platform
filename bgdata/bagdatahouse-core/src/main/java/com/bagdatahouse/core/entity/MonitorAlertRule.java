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
 * Alert rule entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("monitor_alert_rule")
@ApiModel(description = "Alert rule")
public class MonitorAlertRule {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Rule name")
    private String ruleName;

    @ApiModelProperty("Rule code")
    private String ruleCode;

    @ApiModelProperty("Rule type: quality/availability/performance/sensitive")
    private String ruleType;

    @ApiModelProperty("Sensitive field level: L1/L2/L3/L4 (SENSITIVE type alert)")
    private String sensitivityLevel;

    @ApiModelProperty("Sensitive table name (SENSITIVE type alert)")
    private String sensitivityTable;

    @ApiModelProperty("Sensitive column name (SENSITIVE type alert)")
    private String sensitivityColumn;

    @ApiModelProperty("Sensitive datasource ID (SENSITIVE type alert)")
    private Long sensitivityDsId;

    @ApiModelProperty("Spike threshold percentage for sensitive field spike alert (default 20%)")
    private BigDecimal spikeThresholdPct;

    @ApiModelProperty("Unreviewed threshold days for long-unreviewed alert (default 7 days)")
    private Integer unreviewThresholdDays;

    @ApiModelProperty("Access anomaly off-hours definition, e.g. 22:00-08:00")
    private String accessAnomalyOffHours;

    @ApiModelProperty("Access anomaly threshold count (default 5)")
    private Integer accessAnomalyThreshold;

    @ApiModelProperty("Target type: table/column/datasource")
    private String targetType;

    @ApiModelProperty("Target ID (plan code, table name, metric code, etc.)")
    private String targetId;

    @ApiModelProperty("Condition type: threshold/fluctuation/trend")
    private String conditionType;

    @ApiModelProperty("Threshold value")
    private BigDecimal thresholdValue;

    @ApiModelProperty("Threshold max value")
    private BigDecimal thresholdMaxValue;

    @ApiModelProperty("Fluctuation percentage")
    private BigDecimal fluctuationPct;

    @ApiModelProperty("Comparison type: gt/lt/eq/ne")
    private String comparisonType;

    @ApiModelProperty("Consecutive triggers to fire alert")
    private Integer consecutiveTriggers;

    @ApiModelProperty("Alert level: info/warning/error/critical")
    private String alertLevel;

    @ApiModelProperty("Alert receivers (JSON array)")
    private String alertReceivers;

    @ApiModelProperty("Alert channels (JSON array)")
    private String alertChannels;

    @ApiModelProperty("Alert title template")
    private String alertTitleTemplate;

    @ApiModelProperty("Alert content template")
    private String alertContentTemplate;

    @ApiModelProperty("Mute until time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime muteUntil;

    @ApiModelProperty("Enabled flag")
    private Boolean enabled;

    @ApiModelProperty("Department ID")
    private Long deptId;

    @ApiModelProperty("Soft delete flag")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("Creator user ID")
    private Long createUser;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("Updater user ID")
    private Long updateUser;

    @ApiModelProperty("Update time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
