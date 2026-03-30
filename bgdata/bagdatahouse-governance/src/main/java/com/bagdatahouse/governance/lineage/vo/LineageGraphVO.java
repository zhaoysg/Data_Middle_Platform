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
@ApiModel(description = "血缘图谱数据")
public class LineageGraphVO {

    @ApiModelProperty("图谱节点列表")
    private List<LineageNodeVO> nodes;

    @ApiModelProperty("图谱边列表")
    private List<LineageEdgeVO> edges;
}
