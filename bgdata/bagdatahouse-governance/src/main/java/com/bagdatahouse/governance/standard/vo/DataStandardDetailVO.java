package com.bagdatahouse.governance.standard.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据标准详情 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据标准详情")
public class DataStandardDetailVO {

    @ApiModelProperty("标准ID")
    private Long id;

    @ApiModelProperty("标准编码")
    private String standardCode;

    @ApiModelProperty("标准名称")
    private String standardName;

    @ApiModelProperty("标准类型")
    private String standardType;

    @ApiModelProperty("标准类型中文名")
    private String standardTypeLabel;

    @ApiModelProperty("标准分类")
    private String standardCategory;

    @ApiModelProperty("标准描述")
    private String standardDesc;

    @ApiModelProperty("标准内容")
    private String standardContent;

    @ApiModelProperty("校验表达式")
    private String ruleExpr;

    @ApiModelProperty("示例值")
    private String exampleValue;

    @ApiModelProperty("适用对象")
    private String applicableObject;

    @ApiModelProperty("适用对象中文名")
    private String applicableObjectLabel;

    @ApiModelProperty("不合规处理方式")
    private String enforceAction;

    @ApiModelProperty("不合规处理方式中文名")
    private String enforceActionLabel;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("状态中文名")
    private String statusLabel;

    @ApiModelProperty("是否启用")
    private Integer enabled;

    @ApiModelProperty("业务域")
    private String bizDomain;

    @ApiModelProperty("负责人用户ID")
    private Long ownerId;

    @ApiModelProperty("负责人用户名")
    private String ownerName;

    @ApiModelProperty("部门ID")
    private Long deptId;

    @ApiModelProperty("部门名称")
    private String deptName;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("绑定数量")
    private Integer bindingCount;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建人用户名")
    private String createUserName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新人ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("绑定列表")
    private List<DataStandardBindingVO> bindings;
}
