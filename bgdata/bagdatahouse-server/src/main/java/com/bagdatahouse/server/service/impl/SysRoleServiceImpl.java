package com.bagdatahouse.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SysRoleDTO;
import com.bagdatahouse.core.entity.SysRole;
import com.bagdatahouse.core.mapper.SysRoleMapper;
import com.bagdatahouse.server.context.LoginContext;
import com.bagdatahouse.server.service.SysRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色服务实现
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
        implements SysRoleService {

    public SysRoleServiceImpl() {
    }

    @Override
    public Result<Page<SysRole>> page(Integer pageNum, Integer pageSize, String roleName, String roleCode, Integer status) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(roleName)) {
            wrapper.like(SysRole::getRoleName, roleName);
        }
        if (StringUtils.isNotBlank(roleCode)) {
            wrapper.eq(SysRole::getRoleCode, roleCode);
        }
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }
        wrapper.orderByDesc(SysRole::getSortOrder, SysRole::getCreateTime);
        Page<SysRole> result = baseMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<SysRole> getById(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色ID不能为空");
        }
        SysRole role = baseMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "角色不存在");
        }
        return Result.success(role);
    }

    @Override
    @Transactional
    public Result<Long> create(SysRoleDTO dto) {
        String roleName = dto.getRoleName();
        if (StringUtils.isBlank(roleName)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色名称不能为空");
        }
        String roleCode = dto.getRoleCode();
        if (StringUtils.isBlank(roleCode)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色编码不能为空");
        }
        // 检查角色编码唯一性
        LambdaQueryWrapper<SysRole> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(SysRole::getRoleCode, roleCode);
        if (baseMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色编码已存在");
        }
        // 检查角色名称唯一性
        LambdaQueryWrapper<SysRole> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(SysRole::getRoleName, roleName);
        if (baseMapper.selectCount(nameWrapper) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色名称已存在");
        }

        SysRole role = new SysRole();
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setRoleType(dto.getRoleType() != null ? dto.getRoleType() : "CUSTOM");
        role.setDataScope(dto.getDataScope() != null ? dto.getDataScope() : "CUSTOM");
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        role.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        role.setRemark(dto.getRemark());
        role.setCreateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        role.setCreateUser(currentUserId != null ? currentUserId : 1L);
        baseMapper.insert(role);

        // 分配菜单权限
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            baseMapper.deleteRoleMenus(role.getId());
            baseMapper.insertRoleMenus(role.getId(), dto.getMenuIds());
        }

        return Result.success(role.getId());
    }

    @Override
    @Transactional
    public Result<Void> update(Long id, SysRoleDTO dto) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色ID不能为空");
        }
        SysRole role = baseMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "角色不存在");
        }
        if (StringUtils.isNotBlank(dto.getRoleName())) {
            // 检查名称唯一性（排除自己）
            LambdaQueryWrapper<SysRole> nameWrapper = new LambdaQueryWrapper<>();
            nameWrapper.eq(SysRole::getRoleName, dto.getRoleName())
                    .ne(SysRole::getId, id);
            if (baseMapper.selectCount(nameWrapper) > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "角色名称已存在");
            }
            role.setRoleName(dto.getRoleName());
        }
        if (dto.getRoleCode() != null) {
            // 检查编码唯一性（排除自己）
            LambdaQueryWrapper<SysRole> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(SysRole::getRoleCode, dto.getRoleCode())
                    .ne(SysRole::getId, id);
            if (baseMapper.selectCount(codeWrapper) > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "角色编码已存在");
            }
            role.setRoleCode(dto.getRoleCode());
        }
        if (dto.getRoleType() != null) {
            role.setRoleType(dto.getRoleType());
        }
        if (dto.getDataScope() != null) {
            role.setDataScope(dto.getDataScope());
        }
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
        if (dto.getSortOrder() != null) {
            role.setSortOrder(dto.getSortOrder());
        }
        if (dto.getRemark() != null) {
            role.setRemark(dto.getRemark());
        }
        role.setUpdateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        role.setUpdateUser(currentUserId != null ? currentUserId : 1L);
        baseMapper.updateById(role);

        // 更新菜单权限
        if (dto.getMenuIds() != null) {
            baseMapper.deleteRoleMenus(id);
            if (!dto.getMenuIds().isEmpty()) {
                baseMapper.insertRoleMenus(id, dto.getMenuIds());
            }
        }

        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> delete(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色ID不能为空");
        }
        SysRole role = baseMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "角色不存在");
        }
        // 删除角色-菜单关联
        baseMapper.deleteRoleMenus(id);
        // 删除角色-用户关联
        baseMapper.deleteUserRoles(id);
        // 删除角色
        baseMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> updateStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "状态值无效");
        }
        SysRole role = baseMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "角色不存在");
        }
        role.setStatus(status);
        role.setUpdateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        role.setUpdateUser(currentUserId != null ? currentUserId : 1L);
        baseMapper.updateById(role);
        return Result.success();
    }

    @Override
    public Result<List<Object>> listOptions() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1);
        wrapper.orderByDesc(SysRole::getSortOrder, SysRole::getCreateTime);
        List<SysRole> roles = baseMapper.selectList(wrapper);
        List<Object> options = roles.stream().map(role -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("value", role.getId());
            item.put("label", role.getRoleName());
            item.put("roleCode", role.getRoleCode());
            item.put("roleType", role.getRoleType());
            return item;
        }).collect(Collectors.toList());
        return Result.success(options);
    }

    @Override
    public Result<List<Long>> getRoleMenus(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色ID不能为空");
        }
        List<Long> menuIds = baseMapper.selectMenuIdsByRoleId(id);
        return Result.success(menuIds);
    }

    @Override
    @Transactional
    public Result<Void> assignMenus(Long id, List<Long> menuIds) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色ID不能为空");
        }
        SysRole role = baseMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "角色不存在");
        }
        baseMapper.deleteRoleMenus(id);
        if (menuIds != null && !menuIds.isEmpty()) {
            baseMapper.insertRoleMenus(id, menuIds);
        }
        return Result.success();
    }
}
