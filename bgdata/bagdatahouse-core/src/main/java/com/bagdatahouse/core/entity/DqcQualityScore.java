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
 * Quality score history entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dqc_quality_score")
@ApiModel(description = "Quality score history")
public class DqcQualityScore {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Check date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime checkDate;

    @ApiModelProperty("Target data source ID")
    private Long targetDsId;

    @ApiModelProperty("Target table name")
    private String targetTable;

    @ApiModelProperty("Layer code")
    private String layerCode;

    @ApiModelProperty("Department ID")
    private Long deptId;

    @ApiModelProperty("Completeness score")
    private BigDecimal completenessScore;

    @ApiModelProperty("Uniqueness score")
    private BigDecimal uniquenessScore;

    @ApiModelProperty("Accuracy score")
    private BigDecimal accuracyScore;

    @ApiModelProperty("Consistency score")
    private BigDecimal consistencyScore;

    @ApiModelProperty("Timeliness score")
    private BigDecimal timelinessScore;

    @ApiModelProperty("Validity score")
    private BigDecimal validityScore;

    @ApiModelProperty("Sensitivity compliance score: rate of sensitive fields meeting compliance (L3+ fields must pass)")
    private BigDecimal sensitivityComplianceScore;

    @ApiModelProperty("Overall score")
    private BigDecimal overallScore;

    @ApiModelProperty("Rule pass rate")
    private BigDecimal rulePassRate;

    @ApiModelProperty("Total rule count")
    private Integer ruleTotalCount;

    @ApiModelProperty("Passed rule count")
    private Integer rulePassCount;

    @ApiModelProperty("Failed rule count")
    private Integer ruleFailCount;

    @ApiModelProperty("Execution ID")
    private Long executionId;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
