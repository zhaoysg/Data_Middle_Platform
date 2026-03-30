package com.bagdatahouse.core.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * SENSITIVE 类型告警 DTO（由 SecurityServiceImpl 扫描/审核触发时构造）
 * 用于 AlertRecordService.createSensityAlert() / createSensityAlertBatch()
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "SENSITIVE 类型告警 DTO")
public class SensityAlertDTO {

    @ApiModelProperty("告警子类型：SENSITIVE_FIELD_SPIKE / SENSITIVE_LEVEL_CHANGE / SENSITIVE_ACCESS_ANOMALY / SENSITIVE_UNREVIEWED_LONG")
    private String alertType;

    @ApiModelProperty("告警级别：INFO / WARN / ERROR / CRITICAL")
    private String alertLevel;

    @ApiModelProperty("告警标题")
    private String alertTitle;

    @ApiModelProperty("告警内容")
    private String alertContent;

    @ApiModelProperty("关联数据源ID")
    private Long sensitivityDsId;

    @ApiModelProperty("敏感等级：L1 / L2 / L3 / L4")
    private String sensitivityLevel;

    @ApiModelProperty("敏感表名")
    private String sensitivityTable;

    @ApiModelProperty("敏感字段名")
    private String sensitivityColumn;

    @ApiModelProperty("关联扫描批次号（用于 SENSITIVE_FIELD_SPIKE 溯源）")
    private String scanBatchNo;

    @ApiModelProperty("触发值（如突增百分比、波动值）")
    private BigDecimal triggerValue;

    @ApiModelProperty("阈值（如 spikeThresholdPct 配置值）")
    private BigDecimal thresholdValue;
}
