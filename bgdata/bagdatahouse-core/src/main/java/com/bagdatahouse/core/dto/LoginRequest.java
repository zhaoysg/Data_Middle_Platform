package com.bagdatahouse.core.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
@ApiModel(description = "登录请求")
public class LoginRequest {

    @ApiModelProperty(value = "用户名", required = true)
    private String username;

    @ApiModelProperty(value = "密码", required = true)
    private String password;

    @ApiModelProperty(value = "记住我")
    private Boolean rememberMe = false;
}
