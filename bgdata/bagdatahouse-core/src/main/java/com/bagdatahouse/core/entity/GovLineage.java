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
 * Lineage entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("gov_lineage")
@ApiModel(description = "Data lineage")
public class GovLineage {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Lineage type: table/column")
    private String lineageType;

    @ApiModelProperty("Source data source ID")
    private Long sourceDsId;

    @ApiModelProperty("Source table name")
    private String sourceTable;

    @ApiModelProperty("Source column name")
    private String sourceColumn;

    @ApiModelProperty("Source column alias")
    private String sourceColumnAlias;

    @ApiModelProperty("Target data source ID")
    private Long targetDsId;

    @ApiModelProperty("Target table name")
    private String targetTable;

    @ApiModelProperty("Target column name")
    private String targetColumn;

    @ApiModelProperty("Target column alias")
    private String targetColumnAlias;

    @ApiModelProperty("Transform type: direct/aggregate/join/filter/etc")
    private String transformType;

    @ApiModelProperty("Transform expression")
    private String transformExpr;

    @ApiModelProperty("ETL job ID")
    private Long jobId;

    @ApiModelProperty("ETL job name")
    private String jobName;

    @ApiModelProperty("Lineage source: ddl/sql_log/config/manual")
    private String lineageSource;

    @ApiModelProperty("Department ID")
    private Long deptId;

    @ApiModelProperty("Status: ACTIVE/DEPRECATED")
    private String status;

    @ApiModelProperty("Sensitivity level: L1/L2/L3/L4/NORMAL (field-level lineage nodes only)")
    private String sensitivityLevel;

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
