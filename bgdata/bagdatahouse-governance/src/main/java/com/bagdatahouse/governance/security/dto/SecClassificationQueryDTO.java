package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据分类查询 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据分类查询参数")
public class SecClassificationQueryDTO {

    @ApiModelProperty("分类编码")
    private String classCode;

    @ApiModelProperty("分类名称（模糊匹配）")
    private String className;

    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;
}
