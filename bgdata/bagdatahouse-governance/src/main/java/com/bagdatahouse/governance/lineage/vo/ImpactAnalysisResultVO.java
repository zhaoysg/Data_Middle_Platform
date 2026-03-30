package com.bagdatahouse.governance.lineage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 影响分析结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "影响分析结果")
public class ImpactAnalysisResultVO {

    @ApiModelProperty("分析方向：DOWNSTREAM-影响分析（对下游的影响）/ UPSTREAM-回溯分析（对上游的回溯）")
    private String direction;

    @ApiModelProperty("被分析节点（表/字段）")
    private String targetNodeId;

    @ApiModelProperty("分析的最大深度")
    private Integer maxDepth;

    @ApiModelProperty("影响范围统计")
    private ImpactScope scope;

    @ApiModelProperty("血缘图谱节点列表")
    private List<LineageNodeVO> nodes;

    @ApiModelProperty("血缘图谱边列表")
    private List<LineageEdgeVO> edges;

    @ApiModelProperty("层级分布（key=层级，value=该层节点数量）")
    private Map<Integer, Long> levelDistribution;

    @ApiModelProperty("数据层分布")
    private Map<String, Long> layerDistribution;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ApiModel(description = "影响范围统计")
    public static class ImpactScope {

        @ApiModelProperty("涉及的表数量（含直接和间接影响）")
        private long affectedTableCount;

        @ApiModelProperty("涉及的字段数量")
        private long affectedColumnCount;

        @ApiModelProperty("涉及的质检方案数量（受影响的质检方案）")
        private long affectedPlanCount;

        @ApiModelProperty("涉及的下游层级数量")
        private int depthLevel;

        @ApiModelProperty("涉及的节点数量")
        private long totalNodeCount;

        @ApiModelProperty("涉及的边数量")
        private long totalEdgeCount;
    }
}
