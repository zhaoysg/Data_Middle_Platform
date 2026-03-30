package com.bagdatahouse.core.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统菜单 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "系统菜单DTO")
public class SysMenuDTO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("父菜单ID")
    private Long parentId;

    @ApiModelProperty("菜单名称")
    private String menuName;

    @ApiModelProperty("菜单编码")
    private String menuCode;

    @ApiModelProperty("菜单类型：CATALOG-目录，MENU-菜单，BUTTON-按钮")
    private String menuType;

    @ApiModelProperty("路由路径")
    private String path;

    @ApiModelProperty("组件路径")
    private String component;

    @ApiModelProperty("菜单图标")
    private String icon;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("是否可见：0-隐藏，1-显示")
    private Integer visible;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("权限标识")
    private String perms;

    @ApiModelProperty("是否缓存：0-否，1-是")
    private Integer cached;

    @ApiModelProperty("备注")
    private String remark;

    // ========== 展示用字段 ==========
    @ApiModelProperty("菜单类型名称(展示用)")
    private String menuTypeName;

    @ApiModelProperty("父菜单名称(展示用)")
    private String parentName;
}
