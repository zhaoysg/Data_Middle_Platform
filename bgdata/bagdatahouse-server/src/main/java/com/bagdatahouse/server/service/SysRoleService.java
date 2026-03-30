package com.bagdatahouse.server.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SysRoleDTO;
import com.bagdatahouse.core.entity.SysRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 系统角色服务接口
 */
public interface SysRoleService {

    /**
     * 分页查询角色
     */
    Result<Page<SysRole>> page(Integer pageNum, Integer pageSize, String roleName, String roleCode, Integer status);

    /**
     * 根据ID查询角色
     */
    Result<SysRole> getById(Long id);

    /**
     * 新增角色
     */
    Result<Long> create(SysRoleDTO dto);

    /**
     * 更新角色
     */
    Result<Void> update(Long id, SysRoleDTO dto);

    /**
     * 删除角色
     */
    Result<Void> delete(Long id);

    /**
     * 修改角色状态
     */
    Result<Void> updateStatus(Long id, Integer status);

    /**
     * 获取角色选择器数据
     */
    Result<List<Object>> listOptions();

    /**
     * 获取角色菜单权限
     */
    Result<List<Long>> getRoleMenus(Long id);

    /**
     * 分配菜单权限
     */
    Result<Void> assignMenus(Long id, List<Long> menuIds);
}
