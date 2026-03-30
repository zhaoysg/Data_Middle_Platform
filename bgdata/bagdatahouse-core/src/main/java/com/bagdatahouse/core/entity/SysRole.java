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
 * Role entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_role")
@ApiModel(description = "System role")
public class SysRole {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Role name")
    private String roleName;

    @ApiModelProperty("Role code")
    private String roleCode;

    @ApiModelProperty("Role type")
    private String roleType;

    @ApiModelProperty("Data scope")
    private String dataScope;

    @ApiModelProperty("Status: 0-disabled, 1-enabled")
    private Integer status;

    @ApiModelProperty("Soft delete flag")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("Sort order")
    private Integer sortOrder;

    @ApiModelProperty("Remark")
    private String remark;

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
