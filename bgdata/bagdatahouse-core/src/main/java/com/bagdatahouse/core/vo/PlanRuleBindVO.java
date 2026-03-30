package com.bagdatahouse.core.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 方案规则绑定VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "方案规则绑定VO")
public class PlanRuleBindVO {

    @ApiModelProperty("方案规则绑定ID")
    private Long planRuleId;

    @ApiModelProperty("规则ID")
    private Long ruleId;

    @ApiModelProperty("规则名称")
    private String ruleName;

    @ApiModelProperty("规则编码")
    private String ruleCode;

    @ApiModelProperty("规则类型")
    private String ruleType;

    @ApiModelProperty("规则强度")
    private String ruleStrength;

    @ApiModelProperty("规则顺序")
    private Integer ruleOrder;

    @ApiModelProperty("自定义阈值")
    private String customThreshold;

    @ApiModelProperty("前置规则失败时跳过")
    private Boolean skipOnFailure;

    @ApiModelProperty("是否启用")
    private Boolean enabled;

    @ApiModelProperty("阈值最小值")
    private BigDecimal thresholdMin;

    @ApiModelProperty("阈值最大值")
    private BigDecimal thresholdMax;

    @ApiModelProperty("波动阈值")
    private BigDecimal fluctuationThreshold;

    @ApiModelProperty("目标表名（覆盖规则默认值）")
    private String targetTable;

    @ApiModelProperty("目标列名（覆盖规则默认值）")
    private String targetColumn;
}
