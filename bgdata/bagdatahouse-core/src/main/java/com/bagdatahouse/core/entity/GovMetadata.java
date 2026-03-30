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
 * Metadata entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("gov_metadata")
@ApiModel(description = "Data governance metadata")
public class GovMetadata {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Data source ID")
    private Long dsId;

    @ApiModelProperty("Table name")
    private String tableName;

    @ApiModelProperty("Table alias")
    private String tableAlias;

    @ApiModelProperty("Table comment/description")
    private String tableComment;

    @ApiModelProperty("Table type: table/view")
    private String tableType;

    @ApiModelProperty("Data layer code")
    private String dataLayer;

    @ApiModelProperty("Data domain")
    private String dataDomain;

    @ApiModelProperty("Business domain")
    private String bizDomain;

    @ApiModelProperty("Lifecycle days (0 means permanent)")
    private Integer lifecycleDays;

    @ApiModelProperty("Is partitioned table")
    private Boolean isPartitioned;

    @ApiModelProperty("Partition column")
    private String partitionColumn;

    @ApiModelProperty("Storage size in bytes")
    private BigDecimal storageBytes;

    @ApiModelProperty("Row count")
    private Long rowCount;

    @ApiModelProperty("Access frequency")
    private Integer accessFreq;

    @ApiModelProperty("Sensitivity level: public/internal/confidential/secret")
    private String sensitivityLevel;

    @ApiModelProperty("是否存在敏感字段（列表展示用，非表字段）")
    @TableField(exist = false)
    private Boolean isSensitive;

    @ApiModelProperty("Owner user ID")
    private Long ownerId;

    @ApiModelProperty("Department ID")
    private Long deptId;

    @ApiModelProperty("Tags (JSON array)")
    private String tags;

    @ApiModelProperty("Last profiled time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastProfiledAt;

    @ApiModelProperty("Last modified time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastModifiedAt;

    @ApiModelProperty("Last accessed time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessedAt;

    @ApiModelProperty("ETL source system")
    private String etlSource;

    @ApiModelProperty("Status: ACTIVE/ARCHIVED/DEPRECATED")
    private String status;

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
