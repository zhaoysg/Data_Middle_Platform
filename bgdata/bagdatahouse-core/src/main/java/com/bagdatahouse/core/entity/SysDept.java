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
 * Department entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_dept")
@ApiModel(description = "Department")
public class SysDept {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Parent department ID")
    private Long parentId;

    @ApiModelProperty("Department name")
    private String deptName;

    @ApiModelProperty("Department code")
    private String deptCode;

    @ApiModelProperty("Department type")
    private String deptType;

    @ApiModelProperty("Sort order")
    private Integer sortOrder;

    @ApiModelProperty("Leader user ID")
    private Long leaderId;

    @ApiModelProperty("Phone number")
    private String phone;

    @ApiModelProperty("Email")
    private String email;

    @ApiModelProperty("Status: 0-disabled, 1-enabled")
    private Integer status;

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
