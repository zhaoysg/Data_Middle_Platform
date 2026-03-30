package com.bagdatahouse.governance.glossary.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 术语保存/更新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "术语保存请求")
public class GlossaryTermSaveDTO {

    @ApiModelProperty("术语ID（更新时传入）")
    private Long id;

    @ApiModelProperty("术语编码（新增必填，唯一）")
    private String termCode;

    @ApiModelProperty("术语名称（中文，新增必填）")
    private String termName;

    @ApiModelProperty("英文名")
    private String termNameEn;

    @ApiModelProperty("别名/简称，多个用逗号分隔")
    private String termAlias;

    @ApiModelProperty("所属分类ID")
    private Long categoryId;

    @ApiModelProperty("业务域")
    private String bizDomain;

    @ApiModelProperty("术语定义/业务解释（富文本）")
    private String definition;

    @ApiModelProperty("计算公式")
    private String formula;

    @ApiModelProperty("数据类型：STRING/NUMBER/DATE/BOOLEAN")
    private String dataType;

    @ApiModelProperty("单位")
    private String unit;

    @ApiModelProperty("示例值")
    private String exampleValue;

    @ApiModelProperty("敏感等级：PUBLIC/INTERNAL/CONFIDENTIAL/SECRET")
    private String sensitivityLevel;

    @ApiModelProperty("状态：DRAFT-草稿/PUBLISHED-已发布/DEPRECATED-已废弃")
    private String status;

    @ApiModelProperty("术语负责人用户ID")
    private Long ownerId;

    @ApiModelProperty("部门ID")
    private Long deptId;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;

    @ApiModelProperty("创建人用户ID")
    private Long createUser;

    @ApiModelProperty("字段映射列表")
    private List<GlossaryMappingDTO> mappings;
}
