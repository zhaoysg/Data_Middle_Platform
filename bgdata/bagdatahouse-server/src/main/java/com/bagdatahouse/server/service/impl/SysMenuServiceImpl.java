package com.bagdatahouse.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SysMenuDTO;
import com.bagdatahouse.core.entity.SysMenu;
import com.bagdatahouse.core.mapper.SysMenuMapper;
import com.bagdatahouse.core.mapper.SysRoleMapper;
import com.bagdatahouse.core.vo.MenuTreeVO;
import com.bagdatahouse.server.context.LoginContext;
import com.bagdatahouse.server.service.SysMenuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统菜单服务实现
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu>
        implements SysMenuService {

    private final SysRoleMapper sysRoleMapper;

    public SysMenuServiceImpl(SysRoleMapper sysRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public Result<List<MenuTreeVO>> getMenuTree() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getStatus, 1);
        wrapper.orderByAsc(SysMenu::getSortOrder, SysMenu::getId);
        List<SysMenu> allMenus = baseMapper.selectList(wrapper);
        List<MenuTreeVO> tree = buildMenuTree(allMenus, 0L);
        return Result.success(tree);
    }

    private List<MenuTreeVO> buildMenuTree(List<SysMenu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .map(menu -> {
                    MenuTreeVO vo = toMenuTreeVO(menu);
                    vo.setChildren(buildMenuTree(allMenus, menu.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private MenuTreeVO toMenuTreeVO(SysMenu menu) {
        return MenuTreeVO.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .menuName(menu.getMenuName())
                .menuCode(menu.getMenuCode())
                .menuType(menu.getMenuType())
                .path(menu.getPath())
                .component(menu.getComponent())
                .icon(menu.getIcon())
                .sortOrder(menu.getSortOrder())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .perms(menu.getPerms())
                .cached(menu.getCached())
                .children(new ArrayList<>())
                .build();
    }

    @Override
    public Result<Page<SysMenu>> page(Integer pageNum, Integer pageSize, String menuName, String menuType, Integer status) {
        Page<SysMenu> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(menuName)) {
            wrapper.like(SysMenu::getMenuName, menuName);
        }
        if (StringUtils.isNotBlank(menuType)) {
            wrapper.eq(SysMenu::getMenuType, menuType);
        }
        if (status != null) {
            wrapper.eq(SysMenu::getStatus, status);
        }
        wrapper.orderByAsc(SysMenu::getSortOrder, SysMenu::getId);
        Page<SysMenu> result = baseMapper.selectPage(page, wrapper);
        // 填充父菜单名称
        List<SysMenu> records = result.getRecords();
        if (!records.isEmpty()) {
            List<Long> parentIds = records.stream()
                    .map(SysMenu::getParentId)
                    .filter(pid -> pid != null && pid != 0)
                    .distinct()
                    .collect(Collectors.toList());
            if (!parentIds.isEmpty()) {
                Map<Long, String> parentNameMap = baseMapper.selectBatchIds(parentIds)
                        .stream()
                        .collect(Collectors.toMap(SysMenu::getId, SysMenu::getMenuName, (a, b) -> a));
                records.forEach(menu -> {
                    if (menu.getParentId() != null && menu.getParentId() != 0) {
                        menu.setParentName(parentNameMap.getOrDefault(menu.getParentId(), ""));
                    }
                });
            }
        }
        return Result.success(result);
    }

    @Override
    public Result<SysMenu> getById(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "菜单ID不能为空");
        }
        SysMenu menu = baseMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "菜单不存在");
        }
        return Result.success(menu);
    }

    @Override
    @Transactional
    public Result<Long> create(SysMenuDTO dto) {
        String menuName = dto.getMenuName();
        if (StringUtils.isBlank(menuName)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "菜单名称不能为空");
        }
        String menuType = dto.getMenuType();
        if (StringUtils.isBlank(menuType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "菜单类型不能为空");
        }

        SysMenu menu = new SysMenu();
        menu.setParentId(dto.getParentId() != null ? dto.getParentId() : 0);
        menu.setMenuName(menuName);
        menu.setMenuCode(dto.getMenuCode());
        menu.setMenuType(menuType);
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setIcon(dto.getIcon());
        menu.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        menu.setVisible(dto.getVisible() != null ? dto.getVisible() : 1);
        menu.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        menu.setPerms(dto.getPerms());
        menu.setCached(dto.getCached() != null ? dto.getCached() : 0);
        menu.setCreateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        menu.setCreateUser(currentUserId != null ? currentUserId : 1L);

        baseMapper.insert(menu);
        return Result.success(menu.getId());
    }

    @Override
    @Transactional
    public Result<Void> update(Long id, SysMenuDTO dto) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "菜单ID不能为空");
        }
        SysMenu menu = baseMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "菜单不存在");
        }
        // 不能将自己设为自己的父菜单
        if (dto.getParentId() != null && dto.getParentId().equals(id)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "不能将自己设为父菜单");
        }
        // 不能将自己的子菜单设为父菜单（防止循环）
        if (dto.getParentId() != null && dto.getParentId() != 0) {
            List<Long> childIds = getAllChildIds(id);
            if (childIds.contains(dto.getParentId())) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "不能将子菜单设为父菜单");
            }
        }

        if (dto.getParentId() != null) {
            menu.setParentId(dto.getParentId());
        }
        if (StringUtils.isNotBlank(dto.getMenuName())) {
            menu.setMenuName(dto.getMenuName());
        }
        if (dto.getMenuCode() != null) {
            menu.setMenuCode(dto.getMenuCode());
        }
        if (dto.getMenuType() != null) {
            menu.setMenuType(dto.getMenuType());
        }
        if (dto.getPath() != null) {
            menu.setPath(dto.getPath());
        }
        if (dto.getComponent() != null) {
            menu.setComponent(dto.getComponent());
        }
        if (dto.getIcon() != null) {
            menu.setIcon(dto.getIcon());
        }
        if (dto.getSortOrder() != null) {
            menu.setSortOrder(dto.getSortOrder());
        }
        if (dto.getVisible() != null) {
            menu.setVisible(dto.getVisible());
        }
        if (dto.getStatus() != null) {
            menu.setStatus(dto.getStatus());
        }
        if (dto.getPerms() != null) {
            menu.setPerms(dto.getPerms());
        }
        if (dto.getCached() != null) {
            menu.setCached(dto.getCached());
        }

        menu.setUpdateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        menu.setUpdateUser(currentUserId != null ? currentUserId : 1L);
        baseMapper.updateById(menu);

        return Result.success();
    }

    private List<Long> getAllChildIds(Long parentId) {
        List<Long> allChildIds = new ArrayList<>();
        List<Long> currentLevel = new ArrayList<>();
        currentLevel.add(parentId);

        while (!currentLevel.isEmpty()) {
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(SysMenu::getParentId, currentLevel);
            List<SysMenu> children = baseMapper.selectList(wrapper);

            allChildIds.addAll(currentLevel);
            currentLevel = children.stream()
                    .map(SysMenu::getId)
                    .collect(Collectors.toList());
        }

        return allChildIds;
    }

    @Override
    @Transactional
    public Result<Void> delete(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "菜单ID不能为空");
        }
        SysMenu menu = baseMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "菜单不存在");
        }
        // 检查是否有子菜单
        LambdaQueryWrapper<SysMenu> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(SysMenu::getParentId, id);
        if (baseMapper.selectCount(childWrapper) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "该菜单下存在子菜单，请先删除子菜单");
        }
        // 删除角色-菜单关联
        sysRoleMapper.deleteRoleMenus(id);
        // 删除菜单
        baseMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<List<Object>> listOptions() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getSortOrder, SysMenu::getId);
        List<SysMenu> menus = baseMapper.selectList(wrapper);

        // 转换为树形结构的选择器数据
        List<Object> treeData = buildSelectorTree(menus, 0L);
        return Result.success(treeData);
    }

    private List<Object> buildSelectorTree(List<SysMenu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .map(menu -> {
                    java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("value", menu.getId());
                    item.put("label", menu.getMenuName());
                    item.put("menuType", menu.getMenuType());

                    List<Object> children = buildSelectorTree(allMenus, menu.getId());
                    if (!children.isEmpty()) {
                        item.put("children", children);
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Result<List<SysMenu>> getMenusByRoleId(Long roleId) {
        if (roleId == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色ID不能为空");
        }
        // 获取该角色的所有菜单ID
        List<Long> menuIds = sysRoleMapper.selectMenuIdsByRoleId(roleId);
        if (menuIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        // 查询这些菜单（状态为启用）
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getId, menuIds);
        wrapper.eq(SysMenu::getStatus, 1);
        wrapper.orderByAsc(SysMenu::getSortOrder, SysMenu::getId);
        List<SysMenu> menus = baseMapper.selectList(wrapper);
        return Result.success(menus);
    }
}
