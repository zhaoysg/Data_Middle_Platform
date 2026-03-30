package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询所有菜单（用于构建菜单树）
     */
    @Select("SELECT * FROM sys_menu WHERE status = 1 ORDER BY sort_order ASC, id ASC")
    List<SysMenu> selectAllMenus();

    /**
     * 查询所有菜单（不分状态，用于管理）
     */
    @Select("SELECT * FROM sys_menu ORDER BY sort_order ASC, id ASC")
    List<SysMenu> selectAllMenusIncludeDisabled();

    /**
     * 根据菜单ID列表查询菜单
     */
    @Select("<script>SELECT * FROM sys_menu WHERE id IN " +
            "<foreach collection='menuIds' item='menuId' open='(' separator=',' close=')'>" +
            "#{menuId}" +
            "</foreach>" +
            " ORDER BY sort_order ASC</script>")
    List<SysMenu> selectMenusByIds(@Param("menuIds") List<Long> menuIds);

    /**
     * 查询父级菜单名称
     */
    @Select("SELECT menu_name FROM sys_menu WHERE id = #{parentId}")
    String selectParentMenuName(@Param("parentId") Long parentId);
}
