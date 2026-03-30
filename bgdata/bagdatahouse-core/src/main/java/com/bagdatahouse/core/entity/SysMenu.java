package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Menu entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_menu")
@ApiModel(description = "System menu")
public class SysMenu {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Parent menu ID")
    private Long parentId;

    @ApiModelProperty("Menu name")
    private String menuName;

    @ApiModelProperty("Menu code")
    private String menuCode;

    @ApiModelProperty("Menu type: directory/menu/button")
    private String menuType;

    @ApiModelProperty("Route path")
    private String path;

    @ApiModelProperty("Component path")
    private String component;

    @ApiModelProperty("Menu icon")
    private String icon;

    @ApiModelProperty("Sort order")
    private Integer sortOrder;

    @ApiModelProperty("Visible: 0-hidden, 1-visible")
    private Integer visible;

    @ApiModelProperty("Status: 0-disabled, 1-enabled")
    private Integer status;

    @ApiModelProperty("Permission string")
    private String perms;

    @ApiModelProperty("Cached: 0-no, 1-yes")
    private Integer cached;

    @ApiModelProperty("Creator user ID")
    private Long createUser;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("Updater user ID")
    private Long updateUser;

    @ApiModelProperty("Update time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @ApiModelProperty("Parent menu name (transient, not persisted)")
    @TableField(exist = false)
    private String parentName;
}
