package com.bagdatahouse.governance.standard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据标准绑定请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据标准绑定请求")
public class DataStandardBindingDTO {

    @ApiModelProperty("绑定ID（更新时传入）")
    private Long id;

    @ApiModelProperty("元数据ID")
    private Long metadataId;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("目标表名")
    private String targetTable;

    @ApiModelProperty("目标列名（为空表示应用于整表）")
    private String targetColumn;

    @ApiModelProperty("不合规处理方式：ALERT-仅告警/BLOCK-阻断创建")
    private String enforceAction;

    @ApiModelProperty("创建人用户ID")
    private Long createUser;
}
