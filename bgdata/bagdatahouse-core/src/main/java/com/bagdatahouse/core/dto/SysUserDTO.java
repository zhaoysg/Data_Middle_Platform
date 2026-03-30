package com.bagdatahouse.core.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 系统用户 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "系统用户DTO")
public class SysUserDTO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码（新增时必填）")
    private String password;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("部门ID")
    private Long deptId;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("角色ID列表")
    private List<Long> roleIds;

    // ========== 展示用字段 ==========
    @ApiModelProperty("部门名称(展示用)")
    private String deptName;

    @ApiModelProperty("角色名称列表(展示用)")
    private List<String> roleNames;

    @ApiModelProperty("最后登录IP(展示用)")
    private String lastLoginIp;

    @ApiModelProperty("最后登录时间(展示用)")
    private String lastLoginTime;
}
