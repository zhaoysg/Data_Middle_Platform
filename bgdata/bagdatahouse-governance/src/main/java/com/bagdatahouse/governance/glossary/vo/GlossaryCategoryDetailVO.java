package com.bagdatahouse.governance.glossary.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 术语分类详情 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "术语分类详情")
public class GlossaryCategoryDetailVO {

    @ApiModelProperty("分类ID")
    private Long id;

    @ApiModelProperty("父分类ID")
    private Long parentId;

    @ApiModelProperty("父分类名称")
    private String parentName;

    @ApiModelProperty("分类名称")
    private String categoryName;

    @ApiModelProperty("分类编码")
    private String categoryCode;

    @ApiModelProperty("分类描述")
    private String categoryDesc;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("状态中文名")
    private String statusLabel;

    @ApiModelProperty("术语数量")
    private Integer termCount;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建人姓名")
    private String createUserName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新人ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("子分类列表")
    private List<GlossaryCategoryTreeVO> children;
}
