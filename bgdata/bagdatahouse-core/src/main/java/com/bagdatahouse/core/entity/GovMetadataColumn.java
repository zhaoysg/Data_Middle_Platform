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
 * Metadata column entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("gov_metadata_column")
@ApiModel(description = "Metadata column")
public class GovMetadataColumn {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Metadata ID (parent table)")
    private Long metadataId;

    @ApiModelProperty("Table name (denormalized: stored in DB for direct-scan linkage)")
    private String tableName;

    @ApiModelProperty("Schema name (PostgreSQL etc.)")
    private String schemaName;

    @ApiModelProperty("Column name")
    private String columnName;

    @ApiModelProperty("Column alias")
    private String columnAlias;

    @ApiModelProperty("Column comment/description")
    private String columnComment;

    @ApiModelProperty("Data type")
    private String dataType;

    @ApiModelProperty("Is nullable: 0-no, 1-yes")
    private Boolean isNullable;

    @ApiModelProperty("Column key (PRI/UNI/MUL)")
    private String columnKey;

    @ApiModelProperty("Default value")
    private String defaultValue;

    @ApiModelProperty("Is primary key: 0-no, 1-yes")
    private Boolean isPrimaryKey;

    @ApiModelProperty("Is foreign key: 0-no, 1-yes")
    private Boolean isForeignKey;

    @ApiModelProperty("Foreign key reference")
    private String fkReference;

    @ApiModelProperty("Is sensitive field: 0-no, 1-yes")
    private Boolean isSensitive;

    @ApiModelProperty("Sensitivity level: public/internal/confidential/secret")
    private String sensitivityLevel;

    @ApiModelProperty("Sort order")
    private Integer sortOrder;

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
