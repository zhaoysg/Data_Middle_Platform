package com.bagdatahouse.dqc.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源表字段（用于探查任务等场景，直连库读取）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("数据源表字段")
public class DatasourceTableColumnVO {

    @ApiModelProperty("列名")
    private String columnName;

    @ApiModelProperty("数据类型")
    private String dataType;

    @ApiModelProperty("列注释")
    private String columnComment;

    @ApiModelProperty("是否可空")
    private Boolean nullable;

    @ApiModelProperty("是否主键")
    private Boolean primaryKey;
}
