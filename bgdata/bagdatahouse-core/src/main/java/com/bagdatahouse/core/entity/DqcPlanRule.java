package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Plan-rule binding entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dqc_plan_rule")
@ApiModel(description = "Plan-rule binding")
public class DqcPlanRule {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Plan ID")
    private Long planId;

    @ApiModelProperty("Rule ID")
    private Long ruleId;

    @ApiModelProperty("Rule execution order")
    private Integer ruleOrder;

    @ApiModelProperty("Custom threshold override (JSON string)")
    private String customThreshold;

    @ApiModelProperty("Target table name override (fallback to rule default when null)")
    private String targetTable;

    @ApiModelProperty("Target column name override (fallback to rule default when null)")
    private String targetColumn;

    @ApiModelProperty("Skip when previous rule fails")
    private Boolean skipOnFailure;

    @ApiModelProperty("Enabled flag")
    private Boolean enabled;

    @ApiModelProperty("Sensitivity level filter: L1/L2/L3/L4 (null means no filter)")
    private String sensitivityLevel;

    @ApiModelProperty("Sensitivity class filter (null means no filter)")
    private String sensitivityClass;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("Update time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
