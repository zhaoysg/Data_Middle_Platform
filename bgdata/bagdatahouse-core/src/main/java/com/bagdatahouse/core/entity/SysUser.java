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
 * System user entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_user")
@ApiModel(description = "System user")
public class SysUser {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Username")
    private String username;

    @ApiModelProperty("Password (encrypted)")
    private String password;

    @ApiModelProperty("Nickname")
    private String nickname;

    @ApiModelProperty("Email")
    private String email;

    @ApiModelProperty("Phone number")
    private String phone;

    @ApiModelProperty("Avatar URL")
    private String avatar;

    @ApiModelProperty("Department ID")
    private Long deptId;

    @ApiModelProperty("Status: 0-disabled, 1-enabled")
    private Integer status;

    @ApiModelProperty("Soft delete flag")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("Last login IP")
    private String lastLoginIp;

    @ApiModelProperty("Last login time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

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
