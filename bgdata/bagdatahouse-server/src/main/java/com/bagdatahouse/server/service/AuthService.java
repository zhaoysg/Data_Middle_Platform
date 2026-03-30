package com.bagdatahouse.server.service;

import com.bagdatahouse.core.dto.CurrentUser;
import com.bagdatahouse.core.dto.LoginRequest;
import com.bagdatahouse.core.dto.LoginResponse;
import com.bagdatahouse.common.result.Result;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    Result<LoginResponse> login(LoginRequest request);

    /**
     * 用户登出
     */
    Result<Void> logout();

    /**
     * 获取当前登录用户信息
     */
    Result<CurrentUser> getCurrentUser();

    /**
     * 刷新Token
     */
    Result<LoginResponse> refreshToken(String oldToken);

    /**
     * 验证Token是否有效
     */
    boolean validateToken(String token);
}
