package com.bagdatahouse.governance.lineage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "血缘图谱边")
public class LineageEdgeVO {

    @ApiModelProperty("边的唯一ID")
    private String edgeId;

    @ApiModelProperty("源节点ID")
    private String sourceNodeId;

    @ApiModelProperty("目标节点ID")
    private String targetNodeId;

    @ApiModelProperty("血缘关系ID")
    private Long lineageId;

    @ApiModelProperty("转换类型")
    private String transformType;

    @ApiModelProperty("转换表达式")
    private String transformExpr;
}
