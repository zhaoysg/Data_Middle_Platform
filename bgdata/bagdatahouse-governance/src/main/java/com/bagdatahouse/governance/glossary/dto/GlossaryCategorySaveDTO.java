package com.bagdatahouse.governance.glossary.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 术语分类保存/更新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "术语分类保存请求")
public class GlossaryCategorySaveDTO {

    @ApiModelProperty("分类ID（更新时传入）")
    private Long id;

    @ApiModelProperty("父分类ID，0表示顶级")
    private Long parentId;

    @ApiModelProperty("分类名称（新增必填）")
    private String categoryName;

    @ApiModelProperty("分类编码（新增必填，唯一）")
    private String categoryCode;

    @ApiModelProperty("分类描述")
    private String categoryDesc;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("创建人用户ID")
    private Long createUser;
}
