package com.bagdatahouse.core.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "菜单树节点")
public class MenuTreeVO {

    @ApiModelProperty("菜单ID")
    private Long id;

    @ApiModelProperty("父菜单ID")
    private Long parentId;

    @ApiModelProperty("菜单名称")
    private String menuName;

    @ApiModelProperty("菜单编码")
    private String menuCode;

    @ApiModelProperty("菜单类型")
    private String menuType;

    @ApiModelProperty("路由路径")
    private String path;

    @ApiModelProperty("组件路径")
    private String component;

    @ApiModelProperty("菜单图标")
    private String icon;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("是否可见")
    private Integer visible;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("权限标识")
    private String perms;

    @ApiModelProperty("是否缓存")
    private Integer cached;

    @ApiModelProperty("子菜单列表")
    @Builder.Default
    private List<MenuTreeVO> children = new ArrayList<>();
}
