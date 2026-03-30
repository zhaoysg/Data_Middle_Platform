package com.bagdatahouse.server.controller;

import com.bagdatahouse.core.dto.CurrentUser;
import com.bagdatahouse.core.dto.LoginRequest;
import com.bagdatahouse.core.dto.LoginResponse;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.server.service.AuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Api(tags = "系统认证")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ApiOperation("用户登出")
    public Result<Void> logout() {
        return authService.logout();
    }

    @GetMapping("/current-user")
    @ApiOperation("获取当前登录用户信息")
    public Result<CurrentUser> getCurrentUser() {
        return authService.getCurrentUser();
    }

    @PostMapping("/refresh-token")
    @ApiOperation("刷新Token")
    public Result<LoginResponse> refreshToken(
            @ApiParam("旧Token") @RequestParam String oldToken) {
        return authService.refreshToken(oldToken);
    }

    @GetMapping("/validate")
    @ApiOperation("验证Token是否有效")
    public Result<Boolean> validateToken(
            @ApiParam("Token") @RequestParam String token) {
        boolean valid = authService.validateToken(token);
        return Result.success(valid);
    }
}
