package com.bagdatahouse.server.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SysMenuDTO;
import com.bagdatahouse.core.entity.SysMenu;
import com.bagdatahouse.core.vo.MenuTreeVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 系统菜单服务接口
 */
public interface SysMenuService {

    /**
     * 获取菜单树
     */
    Result<List<MenuTreeVO>> getMenuTree();

    /**
     * 分页查询菜单
     */
    Result<Page<SysMenu>> page(Integer pageNum, Integer pageSize, String menuName, String menuType, Integer status);

    /**
     * 根据ID查询菜单
     */
    Result<SysMenu> getById(Long id);

    /**
     * 新增菜单
     */
    Result<Long> create(SysMenuDTO dto);

    /**
     * 更新菜单
     */
    Result<Void> update(Long id, SysMenuDTO dto);

    /**
     * 删除菜单
     */
    Result<Void> delete(Long id);

    /**
     * 获取菜单选择器数据（树形）
     */
    Result<List<Object>> listOptions();

    /**
     * 根据角色ID查询菜单树（用于角色权限分配）
     */
    Result<List<SysMenu>> getMenusByRoleId(Long roleId);
}
