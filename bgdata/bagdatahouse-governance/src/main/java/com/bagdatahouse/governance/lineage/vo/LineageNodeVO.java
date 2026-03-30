package com.bagdatahouse.governance.lineage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "血缘图谱节点")
public class LineageNodeVO {

    @ApiModelProperty("节点ID（数据源ID_表名_字段名）")
    private String nodeId;

    @ApiModelProperty("节点类型：TABLE/COLUMN")
    private String nodeType;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("数据源名称")
    private String dsName;

    @ApiModelProperty("数据源类型")
    private String dsType;

    @ApiModelProperty("表名")
    private String tableName;

    @ApiModelProperty("表中文名")
    private String tableAlias;

    @ApiModelProperty("字段名")
    private String columnName;

    @ApiModelProperty("字段中文名")
    private String columnAlias;

    @ApiModelProperty("数据层")
    private String dataLayer;

    @ApiModelProperty("x坐标")
    private Double x;

    @ApiModelProperty("y坐标")
    private Double y;

    @ApiModelProperty("层级（用于布局）")
    private Integer level;
}
