package com.bagdatahouse.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.server.util.JwtUtil;
import com.bagdatahouse.common.util.PasswordUtil;
import com.bagdatahouse.core.dto.CurrentUser;
import com.bagdatahouse.core.dto.LoginRequest;
import com.bagdatahouse.core.dto.LoginResponse;
import com.bagdatahouse.core.entity.SysRole;
import com.bagdatahouse.core.entity.SysUser;
import com.bagdatahouse.core.mapper.SysUserMapper;
import com.bagdatahouse.server.context.LoginContext;
import com.bagdatahouse.server.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务实现
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    public AuthServiceImpl(SysUserMapper sysUserMapper, JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.sysUserMapper = sysUserMapper;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public Result<LoginResponse> login(LoginRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword();

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "用户名或密码不能为空");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResponseCode.USER_DISABLED);
        }

        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw new BusinessException(ResponseCode.PASSWORD_ERROR);
        }

        List<String> roles = Collections.emptyList();
        List<String> permissions = Collections.emptyList();

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        SysUser updateUser = new SysUser();
        updateUser.setId(user.getId());
        updateUser.setLastLoginIp(getClientIp());
        updateUser.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(updateUser);

        LoginResponse response = LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(roles)
                .permissions(permissions)
                .build();

        return Result.success(response);
    }

    @Override
    public Result<Void> logout() {
        LoginContext.clear();
        return Result.success();
    }

    @Override
    public Result<CurrentUser> getCurrentUser() {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "用户未登录");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }

        CurrentUser currentUser = CurrentUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .deptId(user.getDeptId())
                .lastLoginIp(user.getLastLoginIp())
                .lastLoginTime(user.getLastLoginTime() != null ? user.getLastLoginTime().toString() : null)
                .roles(Collections.emptyList())
                .permissions(Collections.emptyList())
                .build();

        return Result.success(currentUser);
    }

    @Override
    public Result<LoginResponse> refreshToken(String oldToken) {
        if (!jwtUtil.validateToken(oldToken)) {
            throw new BusinessException(ResponseCode.TOKEN_INVALID, "Token无效或已过期");
        }

        Long userId = jwtUtil.getUserIdFromToken(oldToken);
        String username = jwtUtil.getUsernameFromToken(oldToken);
        SysUser user = sysUserMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResponseCode.USER_DISABLED);
        }

        String newToken = jwtUtil.generateToken(userId, username);

        LoginResponse response = LoginResponse.builder()
                .accessToken(newToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(Collections.emptyList())
                .permissions(Collections.emptyList())
                .build();

        return Result.success(response);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    private String getClientIp() {
        try {
            jakarta.servlet.http.HttpServletRequest request = getRequest();
            if (request == null) {
                return "127.0.0.1";
            }
            String ip = request.getHeader("X-Forwarded-For");
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return StringUtils.isBlank(ip) ? "127.0.0.1" : ip;
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private jakarta.servlet.http.HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
