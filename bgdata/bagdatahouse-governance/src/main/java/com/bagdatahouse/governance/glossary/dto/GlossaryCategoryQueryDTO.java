package com.bagdatahouse.governance.glossary.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 术语分类查询请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "术语分类查询请求")
public class GlossaryCategoryQueryDTO {

    @ApiModelProperty("分类名称（模糊匹配）")
    private String categoryName;

    @ApiModelProperty("分类编码（精确匹配）")
    private String categoryCode;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("父分类ID（查询子分类）")
    private Long parentId;

    @ApiModelProperty("是否仅查询顶级分类")
    private Boolean topLevelOnly;
}
