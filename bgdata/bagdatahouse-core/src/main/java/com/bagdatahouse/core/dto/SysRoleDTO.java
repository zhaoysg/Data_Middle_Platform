package com.bagdatahouse.core.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 系统角色 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "系统角色DTO")
public class SysRoleDTO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("角色名称")
    private String roleName;

    @ApiModelProperty("角色编码")
    private String roleCode;

    @ApiModelProperty("角色类型：CUSTOM-自定义，SYSTEM-系统角色")
    private String roleType;

    @ApiModelProperty("数据权限：CUSTOM-自定义，DEPT-本部门，ALL-全部")
    private String dataScope;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("菜单ID列表（用于分配菜单权限）")
    private List<Long> menuIds;

    // ========== 展示用字段 ==========
    @ApiModelProperty("角色类型名称(展示用)")
    private String roleTypeName;

    @ApiModelProperty("数据权限名称(展示用)")
    private String dataScopeName;
}
