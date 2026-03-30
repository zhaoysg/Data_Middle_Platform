package com.bagdatahouse.governance.glossary.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 术语查询请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "术语查询请求")
public class GlossaryTermQueryDTO {

    @ApiModelProperty("术语名称（模糊匹配）")
    private String termName;

    @ApiModelProperty("术语编码（精确匹配）")
    private String termCode;

    @ApiModelProperty("英文名（模糊匹配）")
    private String termNameEn;

    @ApiModelProperty("别名（模糊匹配）")
    private String termAlias;

    @ApiModelProperty("所属分类ID")
    private Long categoryId;

    @ApiModelProperty("业务域")
    private String bizDomain;

    @ApiModelProperty("状态：DRAFT-草稿/PUBLISHED-已发布/DEPRECATED-已废弃")
    private String status;

    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;

    @ApiModelProperty("数据类型")
    private String dataType;

    @ApiModelProperty("敏感等级")
    private String sensitivityLevel;

    @ApiModelProperty("负责人用户ID")
    private Long ownerId;

    @ApiModelProperty("部门ID")
    private Long deptId;
}
