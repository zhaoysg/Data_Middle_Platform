package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据分类与分级绑定列表项 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据分类与分级绑定列表项")
public class SecClassLevelBindingVO {

    @ApiModelProperty("绑定ID")
    private Long id;

    @ApiModelProperty("分类ID")
    private Long classId;

    @ApiModelProperty("分类名称")
    private String className;

    @ApiModelProperty("分级ID")
    private Long levelId;

    @ApiModelProperty("分级编码")
    private String levelCode;

    @ApiModelProperty("分级名称")
    private String levelName;

    @ApiModelProperty("分级值（数值越大越敏感）")
    private Integer levelValue;

    @ApiModelProperty("是否为推荐绑定：0-可选，1-推荐")
    private Integer isRecommended;

    @ApiModelProperty("绑定说明")
    private String bindingDesc;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
