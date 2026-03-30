package com.bagdatahouse.governance.standard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据标准保存/更新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据标准保存请求")
public class DataStandardSaveDTO {

    @ApiModelProperty("标准ID（更新时传入）")
    private Long id;

    @ApiModelProperty("标准编码（新增必填，唯一）")
    private String standardCode;

    @ApiModelProperty("标准名称（新增必填）")
    private String standardName;

    @ApiModelProperty("标准类型（新增必填）：CODE_STANDARD/NAMING_STANDARD/PRIMARY_DATA")
    private String standardType;

    @ApiModelProperty("标准分类")
    private String standardCategory;

    @ApiModelProperty("标准描述")
    private String standardDesc;

    @ApiModelProperty("标准内容（详细规则描述）")
    private String standardContent;

    @ApiModelProperty("校验表达式（正则表达式）")
    private String ruleExpr;

    @ApiModelProperty("示例值")
    private String exampleValue;

    @ApiModelProperty("适用对象：TABLE_NAME-表名/COLUMN_NAME-列名/DATA_VALUE-数据值")
    private String applicableObject;

    @ApiModelProperty("不合规处理方式：ALERT-仅告警/BLOCK-阻断创建")
    private String enforceAction;

    @ApiModelProperty("状态：DRAFT/PUBLISHED/DEPRECATED")
    private String status;

    @ApiModelProperty("业务域")
    private String bizDomain;

    @ApiModelProperty("负责人用户ID")
    private Long ownerId;

    @ApiModelProperty("部门ID")
    private Long deptId;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;

    @ApiModelProperty("创建人用户ID")
    private Long createUser;

    @ApiModelProperty("绑定关系列表")
    private List<DataStandardBindingDTO> bindings;
}
