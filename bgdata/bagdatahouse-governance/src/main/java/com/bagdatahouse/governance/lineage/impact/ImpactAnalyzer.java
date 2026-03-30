package com.bagdatahouse.governance.lineage.impact;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.entity.DqcPlan;
import com.bagdatahouse.core.entity.GovLineage;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.mapper.DqcPlanMapper;
import com.bagdatahouse.core.mapper.GovLineageMapper;
import com.bagdatahouse.governance.lineage.vo.ImpactAnalysisResultVO;
import com.bagdatahouse.governance.lineage.vo.LineageEdgeVO;
import com.bagdatahouse.governance.lineage.vo.LineageNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 影响分析器：血缘下钻分析，支持影响分析（对下游）和回溯分析（对上游）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImpactAnalyzer {

    private final GovLineageMapper lineageMapper;
    private final DqDatasourceMapper datasourceMapper;
    private final DqcPlanMapper planMapper;

    private static final int DEFAULT_MAX_DEPTH = 5;

    /**
     * 影响分析：从指定表/字段出发，追踪所有下游影响
     */
    public ImpactAnalysisResultVO analyzeDownstream(Long dsId, String tableName,
                                                    String column, Integer maxDepth) {
        int depth = resolveMaxDepth(maxDepth);
        String targetNodeId = buildNodeId(dsId, tableName, column);

        DqDatasource ds = datasourceMapper.selectById(dsId);
        if (ds == null) {
            throw new BusinessException(2001, "数据源不存在");
        }

        Map<Long, DqDatasource> dsMap = new HashMap<>();
        dsMap.put(dsId, ds);

        Set<String> visitedNodes = new HashSet<>();
        visitedNodes.add(targetNodeId);

        List<LineageNodeVO> nodes = new ArrayList<>();
        List<LineageEdgeVO> edges = new ArrayList<>();

        LineageNodeVO rootNode = buildNode(targetNodeId, "SOURCE",
                dsId, tableName, column, ds, ds.getDataLayer());
        rootNode.setLevel(0);
        nodes.add(rootNode);

        traverseDownstream(dsId, tableName, column, depth, 1,
                visitedNodes, nodes, edges, dsMap);

        Map<String, Integer> layerMap = buildDsLayerMap(dsMap);

        return buildResult("DOWNSTREAM", targetNodeId, depth, nodes, edges, dsMap, layerMap);
    }

    /**
     * 回溯分析：从指定表/字段出发，追溯所有上游来源
     */
    public ImpactAnalysisResultVO analyzeUpstream(Long dsId, String tableName,
                                                   String column, Integer maxDepth) {
        int depth = resolveMaxDepth(maxDepth);
        String targetNodeId = buildNodeId(dsId, tableName, column);

        DqDatasource ds = datasourceMapper.selectById(dsId);
        if (ds == null) {
            throw new BusinessException(2001, "数据源不存在");
        }

        Map<Long, DqDatasource> dsMap = new HashMap<>();
        dsMap.put(dsId, ds);

        Set<String> visitedNodes = new HashSet<>();
        visitedNodes.add(targetNodeId);

        List<LineageNodeVO> nodes = new ArrayList<>();
        List<LineageEdgeVO> edges = new ArrayList<>();

        LineageNodeVO rootNode = buildNode(targetNodeId, "TARGET",
                dsId, tableName, column, ds, ds.getDataLayer());
        rootNode.setLevel(0);
        nodes.add(rootNode);

        traverseUpstream(dsId, tableName, column, depth, 1,
                visitedNodes, nodes, edges, dsMap);

        Map<String, Integer> layerMap = buildDsLayerMap(dsMap);

        return buildResult("UPSTREAM", targetNodeId, depth, nodes, edges, dsMap, layerMap);
    }

    /**
     * 从节点详情面板点击分析：自动判断方向
     */
    public ImpactAnalysisResultVO analyzeFromNode(String direction, Long dsId,
                                                   String tableName, String column,
                                                   Integer maxDepth) {
        if ("DOWNSTREAM".equalsIgnoreCase(direction)) {
            return analyzeDownstream(dsId, tableName, column, maxDepth);
        } else {
            return analyzeUpstream(dsId, tableName, column, maxDepth);
        }
    }

    // ==================== 私有方法 ====================

    private int resolveMaxDepth(Integer maxDepth) {
        if (maxDepth == null || maxDepth <= 0) {
            return DEFAULT_MAX_DEPTH;
        }
        return Math.min(maxDepth, 10);
    }

    private void traverseDownstream(Long dsId, String tableName, String column,
                                     int maxDepth, int currentDepth,
                                     Set<String> visitedNodes,
                                     List<LineageNodeVO> nodes,
                                     List<LineageEdgeVO> edges,
                                     Map<Long, DqDatasource> dsMap) {
        if (currentDepth > maxDepth) return;

        LambdaQueryWrapper<GovLineage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovLineage::getSourceDsId, dsId)
                .eq(GovLineage::getSourceTable, tableName)
                .eq(GovLineage::getStatus, "ACTIVE");

        if (StringUtils.hasText(column)) {
            wrapper.eq(GovLineage::getSourceColumn, column);
        }

        List<GovLineage> lineages = lineageMapper.selectList(wrapper);

        for (GovLineage lineage : lineages) {
            String targetNodeId = buildNodeId(lineage.getTargetDsId(),
                    lineage.getTargetTable(), lineage.getTargetColumn());

            if (visitedNodes.contains(targetNodeId)) continue;
            visitedNodes.add(targetNodeId);

            DqDatasource targetDs = datasourceMapper.selectById(lineage.getTargetDsId());
            if (targetDs != null) {
                dsMap.put(lineage.getTargetDsId(), targetDs);
            }

            LineageNodeVO node = buildNode(targetNodeId, "TARGET",
                    lineage.getTargetDsId(),
                    lineage.getTargetTable(),
                    lineage.getTargetColumn(),
                    targetDs,
                    targetDs != null ? targetDs.getDataLayer() : null);
            node.setLevel(currentDepth);
            nodes.add(node);

            edges.add(LineageEdgeVO.builder()
                    .edgeId(String.valueOf(lineage.getId()))
                    .sourceNodeId(buildNodeId(lineage.getSourceDsId(),
                            lineage.getSourceTable(), lineage.getSourceColumn()))
                    .targetNodeId(targetNodeId)
                    .lineageId(lineage.getId())
                    .transformType(lineage.getTransformType())
                    .transformExpr(lineage.getTransformExpr())
                    .build());

            traverseDownstream(lineage.getTargetDsId(),
                    lineage.getTargetTable(),
                    lineage.getTargetColumn(),
                    maxDepth, currentDepth + 1,
                    visitedNodes, nodes, edges, dsMap);
        }
    }

    private void traverseUpstream(Long dsId, String tableName, String column,
                                  int maxDepth, int currentDepth,
                                  Set<String> visitedNodes,
                                  List<LineageNodeVO> nodes,
                                  List<LineageEdgeVO> edges,
                                  Map<Long, DqDatasource> dsMap) {
        if (currentDepth > maxDepth) return;

        LambdaQueryWrapper<GovLineage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovLineage::getTargetDsId, dsId)
                .eq(GovLineage::getTargetTable, tableName)
                .eq(GovLineage::getStatus, "ACTIVE");

        if (StringUtils.hasText(column)) {
            wrapper.eq(GovLineage::getTargetColumn, column);
        }

        List<GovLineage> lineages = lineageMapper.selectList(wrapper);

        for (GovLineage lineage : lineages) {
            String sourceNodeId = buildNodeId(lineage.getSourceDsId(),
                    lineage.getSourceTable(), lineage.getSourceColumn());

            if (visitedNodes.contains(sourceNodeId)) continue;
            visitedNodes.add(sourceNodeId);

            DqDatasource sourceDs = datasourceMapper.selectById(lineage.getSourceDsId());
            if (sourceDs != null) {
                dsMap.put(lineage.getSourceDsId(), sourceDs);
            }

            LineageNodeVO node = buildNode(sourceNodeId, "SOURCE",
                    lineage.getSourceDsId(),
                    lineage.getSourceTable(),
                    lineage.getSourceColumn(),
                    sourceDs,
                    sourceDs != null ? sourceDs.getDataLayer() : null);
            node.setLevel(currentDepth);
            nodes.add(node);

            edges.add(LineageEdgeVO.builder()
                    .edgeId(String.valueOf(lineage.getId()))
                    .sourceNodeId(sourceNodeId)
                    .targetNodeId(buildNodeId(lineage.getTargetDsId(),
                            lineage.getTargetTable(), lineage.getTargetColumn()))
                    .lineageId(lineage.getId())
                    .transformType(lineage.getTransformType())
                    .transformExpr(lineage.getTransformExpr())
                    .build());

            traverseUpstream(lineage.getSourceDsId(),
                    lineage.getSourceTable(),
                    lineage.getSourceColumn(),
                    maxDepth, currentDepth + 1,
                    visitedNodes, nodes, edges, dsMap);
        }
    }

    private ImpactAnalysisResultVO buildResult(String direction, String targetNodeId,
                                                int maxDepth,
                                                List<LineageNodeVO> nodes,
                                                List<LineageEdgeVO> edges,
                                                Map<Long, DqDatasource> dsMap,
                                                Map<String, Integer> layerMap) {

        long tableCount = nodes.stream()
                .filter(n -> "TABLE".equals(n.getNodeType()))
                .count();
        long columnCount = nodes.stream()
                .filter(n -> "COLUMN".equals(n.getNodeType()))
                .count();

        long maxLevel = nodes.stream()
                .filter(n -> n.getLevel() != null)
                .mapToLong(LineageNodeVO::getLevel)
                .max()
                .orElse(0);

        Map<Integer, Long> levelDist = nodes.stream()
                .collect(Collectors.groupingBy(
                        n -> n.getLevel() != null ? n.getLevel() : 0,
                        Collectors.counting()
                ));

        Map<String, Long> layerDist = nodes.stream()
                .filter(n -> StringUtils.hasText(n.getDataLayer()))
                .collect(Collectors.groupingBy(
                        LineageNodeVO::getDataLayer,
                        Collectors.counting()
                ));

        long affectedPlanCount = calculateAffectedPlans(nodes, dsMap);

        ImpactAnalysisResultVO.ImpactScope scope = ImpactAnalysisResultVO.ImpactScope.builder()
                .affectedTableCount(tableCount)
                .affectedColumnCount(columnCount)
                .affectedPlanCount(affectedPlanCount)
                .depthLevel((int) maxLevel)
                .totalNodeCount(nodes.size())
                .totalEdgeCount(edges.size())
                .build();

        return ImpactAnalysisResultVO.builder()
                .direction(direction)
                .targetNodeId(targetNodeId)
                .maxDepth(maxDepth)
                .scope(scope)
                .nodes(nodes)
                .edges(edges)
                .levelDistribution(levelDist)
                .layerDistribution(layerDist)
                .build();
    }

    private long calculateAffectedPlans(List<LineageNodeVO> nodes,
                                         Map<Long, DqDatasource> dsMap) {
        if (nodes.isEmpty()) return 0;

        Set<String> affectedDsTables = new HashSet<>();
        for (LineageNodeVO node : nodes) {
            if (node.getDsId() != null && StringUtils.hasText(node.getTableName())) {
                affectedDsTables.add(node.getDsId() + "_" + node.getTableName());
            }
        }

        if (affectedDsTables.isEmpty()) return 0;

        LambdaQueryWrapper<DqcPlan> wrapper = new LambdaQueryWrapper<>();
        List<DqcPlan> allPlans = planMapper.selectList(wrapper);

        long count = allPlans.stream()
                .filter(plan -> {
                    String bindValue = plan.getBindValue();
                    if (!StringUtils.hasText(bindValue)) return false;
                    return affectedDsTables.stream().anyMatch(dt -> bindValue.contains(dt));
                })
                .count();

        return count;
    }

    private String buildNodeId(Long dsId, String table, String column) {
        if (StringUtils.hasText(column)) {
            return dsId + "_" + table + "_" + column;
        }
        return dsId + "_" + table;
    }

    private LineageNodeVO buildNode(String nodeId, String direction,
                                     Long dsId, String table, String column,
                                     DqDatasource ds, String layer) {
        return LineageNodeVO.builder()
                .nodeId(nodeId)
                .nodeType(StringUtils.hasText(column) ? "COLUMN" : "TABLE")
                .dsId(dsId)
                .dsName(ds != null ? ds.getDsName() : null)
                .dsType(ds != null ? ds.getDsType() : null)
                .tableName(table)
                .columnName(column)
                .dataLayer(layer)
                .build();
    }

    private Map<String, Integer> buildDsLayerMap(Map<Long, DqDatasource> dsMap) {
        Map<String, Integer> result = new HashMap<>();
        Map<String, Integer> layerOrder = Map.of(
                "ODS", 0, "DWD", 1, "DWS", 2, "ADS", 3
        );
        for (Map.Entry<Long, DqDatasource> entry : dsMap.entrySet()) {
            DqDatasource ds = entry.getValue();
            if (ds != null && StringUtils.hasText(ds.getDataLayer())) {
                result.put(ds.getDsName(), layerOrder.getOrDefault(ds.getDataLayer(), 99));
            }
        }
        return result;
    }
}
