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
 * 告警规则DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "告警规则DTO")
public class MonitorAlertRuleDTO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("规则名称")
    private String ruleName;

    @ApiModelProperty("规则编码")
    private String ruleCode;

    @ApiModelProperty("规则类型: QUALITY/AVAILABILITY/PERFORMANCE/SYSTEM/FLUCTUATION")
    private String ruleType;

    @ApiModelProperty("目标类型: TABLE/COLUMN/DATASOURCE/METRIC/RULE")
    private String targetType;

    @ApiModelProperty("目标ID（方案编码/表名/指标编码等）")
    private String targetId;

    @ApiModelProperty("目标名称（冗余展示用）")
    private String targetName;

    @ApiModelProperty("敏感等级：L1/L2/L3/L4（SENSITIVE 类型告警）")
    private String sensitivityLevel;

    @ApiModelProperty("敏感表名（SENSITIVE 类型告警）")
    private String sensitivityTable;

    @ApiModelProperty("敏感字段名（SENSITIVE 类型告警）")
    private String sensitivityColumn;

    @ApiModelProperty("敏感数据源ID（SENSITIVE 类型告警）")
    private Long sensitivityDsId;

    @ApiModelProperty("扫描批次号（SENSITIVE_FIELD_SPIKE 溯源用）")
    private String scanBatchNo;

    @ApiModelProperty("条件类型: GT/LT/EQ/NE/BETWEEN/FLUCTUATION")
    private String conditionType;

    @ApiModelProperty("阈值")
    private BigDecimal thresholdValue;

    @ApiModelProperty("阈值上限")
    private BigDecimal thresholdMaxValue;

    @ApiModelProperty("波动百分比")
    private BigDecimal fluctuationPct;

    @ApiModelProperty("对比类型: D_DAY/W_WEEK/M_MONTH")
    private String comparisonType;

    @ApiModelProperty("连续触发次数")
    private Integer consecutiveTriggers;

    @ApiModelProperty("告警级别: INFO/WARN/ERROR/CRITICAL")
    private String alertLevel;

    @ApiModelProperty("告警接收人（JSON数组用户ID）")
    private String alertReceivers;

    @ApiModelProperty("告警渠道（预留）")
    private String alertChannels;

    @ApiModelProperty("告警标题模板")
    private String alertTitleTemplate;

    @ApiModelProperty("告警内容模板")
    private String alertContentTemplate;

    @ApiModelProperty("静默截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime muteUntil;

    @ApiModelProperty("是否启用")
    private Boolean enabled;

    @ApiModelProperty("所属部门ID")
    private Long deptId;

    @ApiModelProperty("创建人")
    private Long createUser;

    // ==================== SENSITIVE 告警专用字段 ====================

    @ApiModelProperty("突增阈值百分比（SENSITIVE_FIELD_SPIKE 告警）")
    private BigDecimal spikeThresholdPct;

    @ApiModelProperty("待审核超期天数（SENSITIVE_UNREVIEWED_LONG 告警）")
    private Integer unreviewThresholdDays;

    @ApiModelProperty("访问异常非工作时间小时数（SENSITIVE_ACCESS_ANOMALY 告警）")
    private String accessAnomalyOffHours;

    @ApiModelProperty("访问异常次数阈值（SENSITIVE_ACCESS_ANOMALY 告警）")
    private Integer accessAnomalyThreshold;
}
