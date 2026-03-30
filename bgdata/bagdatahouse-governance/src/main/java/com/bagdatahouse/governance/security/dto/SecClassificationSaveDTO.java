package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据分类保存 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据分类保存参数")
public class SecClassificationSaveDTO {

    @ApiModelProperty("分类ID（编辑时传入）")
    private Long id;

    @ApiModelProperty("分类编码")
    private String classCode;

    @ApiModelProperty("分类名称")
    private String className;

    @ApiModelProperty("分类说明")
    private String classDesc;

    @ApiModelProperty("排序号")
    private Integer classOrder;

    @ApiModelProperty("敏感级别（展示用）")
    private String sensitivityLevel;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("创建者")
    private Long createUser;
}
