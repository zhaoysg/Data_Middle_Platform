package com.bagdatahouse.governance.glossary.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 术语-字段映射 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "术语-字段映射")
public class GlossaryMappingVO {

    @ApiModelProperty("映射ID")
    private Long id;

    @ApiModelProperty("术语ID")
    private Long termId;

    @ApiModelProperty("术语名称")
    private String termName;

    @ApiModelProperty("术语编码")
    private String termCode;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("数据源名称")
    private String dsName;

    @ApiModelProperty("表名")
    private String tableName;

    @ApiModelProperty("表中文名")
    private String tableAlias;

    @ApiModelProperty("字段名")
    private String columnName;

    @ApiModelProperty("映射类型")
    private String mappingType;

    @ApiModelProperty("映射类型中文名")
    private String mappingTypeLabel;

    @ApiModelProperty("映射说明")
    private String mappingDesc;

    @ApiModelProperty("匹配置信度")
    private BigDecimal confidence;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("状态中文名")
    private String statusLabel;

    @ApiModelProperty("审批人ID")
    private Long approvedBy;

    @ApiModelProperty("审批人姓名")
    private String approvedByName;

    @ApiModelProperty("审批时间")
    private LocalDateTime approvedTime;

    @ApiModelProperty("驳回原因")
    private String rejectReason;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建人姓名")
    private String createUserName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
