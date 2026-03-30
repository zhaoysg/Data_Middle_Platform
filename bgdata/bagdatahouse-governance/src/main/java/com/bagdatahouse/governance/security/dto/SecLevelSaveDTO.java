package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据分级保存 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据分级保存参数")
public class SecLevelSaveDTO {

    @ApiModelProperty("分级ID（编辑时传入）")
    private Long id;

    @ApiModelProperty("分级编码")
    private String levelCode;

    @ApiModelProperty("分级名称")
    private String levelName;

    @ApiModelProperty("分级说明")
    private String levelDesc;

    @ApiModelProperty("等级值 1-4")
    private Integer levelValue;

    @ApiModelProperty("展示颜色")
    private String color;

    @ApiModelProperty("展示图标")
    private String icon;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("创建者")
    private Long createUser;
}
