package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data source configuration entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dq_datasource")
@ApiModel(description = "Data source configuration")
public class DqDatasource {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Data source name")
    private String dsName;

    @ApiModelProperty("Data source code")
    private String dsCode;

    @ApiModelProperty("Data source type")
    private String dsType;

    @ApiModelProperty("Host address")
    private String host;

    @ApiModelProperty("Port number")
    private Integer port;

    @ApiModelProperty("Database name")
    private String databaseName;

    @ApiModelProperty("Schema name (for PostgreSQL, optional)")
    private String schemaName;

    @ApiModelProperty("Username")
    private String username;

    @ApiModelProperty("Password")
    @JsonIgnore
    private String password;

    @ApiModelProperty("Connection parameters (JSON)")
    private String connectionParams;

    @ApiModelProperty("Data layer code")
    private String dataLayer;

    @ApiModelProperty("Department ID")
    private Long deptId;

    @ApiModelProperty("Owner user ID")
    private Long ownerId;

    @ApiModelProperty("Status: 0-disabled, 1-enabled")
    private Integer status;

    @ApiModelProperty("Last test time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTestTime;

    @ApiModelProperty("Last test result")
    private String lastTestResult;

    @ApiModelProperty("Remark")
    private String remark;

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
