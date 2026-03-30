package com.bagdatahouse.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.common.util.PasswordUtil;
import com.bagdatahouse.core.dto.SysUserDTO;
import com.bagdatahouse.core.entity.SysDept;
import com.bagdatahouse.core.entity.SysUser;
import com.bagdatahouse.core.mapper.SysDeptMapper;
import com.bagdatahouse.core.mapper.SysUserMapper;
import com.bagdatahouse.server.context.LoginContext;
import com.bagdatahouse.server.service.SysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统用户服务实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    private final SysDeptMapper sysDeptMapper;

    public SysUserServiceImpl(SysDeptMapper sysDeptMapper) {
        this.sysDeptMapper = sysDeptMapper;
    }

    @Override
    public Result<Page<SysUser>> page(Integer pageNum, Integer pageSize, String username, String nickname, Integer status, Long deptId) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (StringUtils.isNotBlank(nickname)) {
            wrapper.like(SysUser::getNickname, nickname);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (deptId != null) {
            wrapper.eq(SysUser::getDeptId, deptId);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = baseMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<SysUser> getById(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "用户ID不能为空");
        }
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        return Result.success(user);
    }

    @Override
    @Transactional
    public Result<Long> create(SysUserDTO dto) {
        String username = dto.getUsername().trim();
        if (StringUtils.isBlank(username)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "用户名不能为空");
        }
        LambdaQueryWrapper<SysUser> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SysUser::getUsername, username);
        if (baseMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "用户名已存在");
        }
        String password = dto.getPassword();
        if (StringUtils.isBlank(password)) {
            password = "admin123";
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encrypt(password));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : username);
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        user.setDeptId(dto.getDeptId());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        user.setCreateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        user.setCreateUser(currentUserId != null ? currentUserId : 1L);
        baseMapper.insert(user);

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            baseMapper.deleteUserRoles(user.getId());
            baseMapper.insertUserRoles(user.getId(), dto.getRoleIds());
        }

        return Result.success(user.getId());
    }

    @Override
    @Transactional
    public Result<Void> update(Long id, SysUserDTO dto) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "用户ID不能为空");
        }
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        if (StringUtils.isNotBlank(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getDeptId() != null) {
            user.setDeptId(dto.getDeptId());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        user.setUpdateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        user.setUpdateUser(currentUserId != null ? currentUserId : 1L);
        baseMapper.updateById(user);

        if (dto.getRoleIds() != null) {
            baseMapper.deleteUserRoles(id);
            if (!dto.getRoleIds().isEmpty()) {
                baseMapper.insertUserRoles(id, dto.getRoleIds());
            }
        }

        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> delete(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "用户ID不能为空");
        }
        if (id == 1L) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "超级管理员不可删除");
        }
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        baseMapper.deleteUserRoles(id);
        baseMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> updateStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "用户ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "状态值无效");
        }
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        user.setUpdateUser(currentUserId != null ? currentUserId : 1L);
        baseMapper.updateById(user);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> resetPassword(Long id, String newPassword) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "用户ID不能为空");
        }
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        String password = StringUtils.isNotBlank(newPassword) ? newPassword : "admin123";
        user.setPassword(PasswordUtil.encrypt(password));
        user.setUpdateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        user.setUpdateUser(currentUserId != null ? currentUserId : 1L);
        baseMapper.updateById(user);
        return Result.success();
    }

    @Override
    public Result<Object> listOptions() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getStatus, 1);
        wrapper.orderByDesc(SysUser::getCreateTime);
        List<SysUser> users = baseMapper.selectList(wrapper);
        List<Object> options = users.stream().map(user -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("value", user.getId());
            item.put("label", user.getNickname() + " (" + user.getUsername() + ")");
            item.put("username", user.getUsername());
            item.put("nickname", user.getNickname());
            item.put("deptId", user.getDeptId());
            return item;
        }).collect(Collectors.toList());
        return Result.success(options);
    }
}
