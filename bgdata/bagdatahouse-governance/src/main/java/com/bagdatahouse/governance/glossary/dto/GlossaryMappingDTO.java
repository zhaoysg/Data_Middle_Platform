package com.bagdatahouse.governance.glossary.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 术语-字段映射保存请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "术语-字段映射保存请求")
public class GlossaryMappingDTO {

    @ApiModelProperty("映射ID（更新时传入）")
    private Long id;

    @ApiModelProperty("术语ID")
    private Long termId;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("表名")
    private String tableName;

    @ApiModelProperty("字段名")
    private String columnName;

    @ApiModelProperty("映射类型：DIRECT-直接映射/TRANSFORM-转换映射/AGGREGATE-聚合映射")
    private String mappingType;

    @ApiModelProperty("映射说明")
    private String mappingDesc;

    @ApiModelProperty("匹配置信度 0-100")
    private BigDecimal confidence;

    @ApiModelProperty("状态：PENDING-待审批/APPROVED-已审批/REJECTED-已驳回")
    private String status;

    @ApiModelProperty("审批人")
    private Long approvedBy;

    @ApiModelProperty("驳回原因")
    private String rejectReason;

    @ApiModelProperty("创建人用户ID")
    private Long createUser;
}
