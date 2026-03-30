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
 * 术语-字段映射实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("gov_glossary_mapping")
@ApiModel(description = "术语-字段映射")
public class GovGlossaryMapping {

    @ApiModelProperty("映射ID")
    @TableId(type = IdType.AUTO)
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

    @ApiModelProperty("审批时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedTime;

    @ApiModelProperty("驳回原因")
    private String rejectReason;

    @ApiModelProperty("删除标记")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("创建者")
    private Long createUser;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新者")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
