package com.bagdatahouse.governance.lineage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.entity.GovLineage;
import com.bagdatahouse.core.entity.SysDept;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.mapper.GovLineageMapper;
import com.bagdatahouse.core.mapper.SysDeptMapper;
import com.bagdatahouse.governance.lineage.dto.LineageQueryDTO;
import com.bagdatahouse.governance.lineage.dto.LineageSaveDTO;
import com.bagdatahouse.governance.lineage.service.LineageService;
import com.bagdatahouse.governance.lineage.vo.LineageEdgeVO;
import com.bagdatahouse.governance.lineage.vo.LineageGraphVO;
import com.bagdatahouse.governance.lineage.vo.LineageNodeVO;
import com.bagdatahouse.governance.lineage.vo.LineageStatsVO;
import com.bagdatahouse.governance.lineage.vo.LineageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Collections;

/**
 * 数据血缘管理服务实现
 */
@Slf4j
@Service
public class LineageServiceImpl extends ServiceImpl<GovLineageMapper, GovLineage>
        implements LineageService {

    @Autowired
    private GovLineageMapper lineageMapper;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GovLineage> save(LineageSaveDTO dto) {
        validateDto(dto, null);

        GovLineage lineage = new GovLineage();
        BeanUtils.copyProperties(dto, lineage);
        lineage.setStatus("ACTIVE");
        lineage.setCreateTime(LocalDateTime.now());
        lineageMapper.insert(lineage);

        return Result.success(lineage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchSave(List<LineageSaveDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Result.success();
        }

        for (LineageSaveDTO dto : dtoList) {
            validateDto(dto, null);
            GovLineage lineage = new GovLineage();
            BeanUtils.copyProperties(dto, lineage);
            lineage.setStatus("ACTIVE");
            lineage.setCreateTime(LocalDateTime.now());
            lineageMapper.insert(lineage);
        }

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, LineageSaveDTO dto) {
        GovLineage existing = lineageMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "血缘记录不存在");
        }

        validateDto(dto, id);

        GovLineage lineage = new GovLineage();
        BeanUtils.copyProperties(dto, lineage);
        lineage.setId(id);
        lineage.setUpdateTime(LocalDateTime.now());
        lineageMapper.updateById(lineage);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        GovLineage existing = lineageMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "血缘记录不存在");
        }

        lineageMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        lineageMapper.deleteBatchIds(ids);
        return Result.success();
    }

    @Override
    public Result<GovLineage> getById(Long id) {
        GovLineage entity = lineageMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "血缘记录不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<Page<GovLineage>> page(Integer pageNum, Integer pageSize, LineageQueryDTO queryDTO) {
        Page<GovLineage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GovLineage> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(GovLineage::getCreateTime);
        Page<GovLineage> result = lineageMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<LineageVO> getDetail(Long id) {
        GovLineage lineage = lineageMapper.selectById(id);
        if (lineage == null) {
            throw new BusinessException(2001, "血缘记录不存在");
        }

        LineageVO vo = buildVO(lineage);
        return Result.success(vo);
    }

    @Override
    public Result<List<GovLineage>> getBySourceAndTarget(Long sourceDsId, String sourceTable,
                                                         Long targetDsId, String targetTable) {
        LambdaQueryWrapper<GovLineage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovLineage::getSourceDsId, sourceDsId)
                .eq(GovLineage::getSourceTable, sourceTable)
                .eq(GovLineage::getTargetDsId, targetDsId)
                .eq(GovLineage::getTargetTable, targetTable)
                .eq(GovLineage::getStatus, "ACTIVE");
        List<GovLineage> list = lineageMapper.selectList(wrapper);
        return Result.success(list);
    }

    @Override
    public Result<LineageGraphVO> getGraph(LineageQueryDTO queryDTO) {
        LambdaQueryWrapper<GovLineage> wrapper = buildQueryWrapper(queryDTO);
        wrapper.eq(GovLineage::getStatus, "ACTIVE");
        List<GovLineage> lineages = lineageMapper.selectList(wrapper);

        Set<String> nodeIdSet = new HashSet<>();
        List<LineageNodeVO> nodes = new ArrayList<>();
        List<LineageEdgeVO> edges = new ArrayList<>();

        Map<Long, DqDatasource> dsMap = buildDatasourceMap(lineages);
        Map<Long, String> dsLayerMap = buildDsLayerMap(lineages);

        // 构建入度映射，用于拓扑分层
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacencyFromSource = new HashMap<>();

        for (GovLineage lineage : lineages) {
            String sourceNodeId = buildNodeId(lineage.getSourceDsId(), lineage.getSourceTable(),
                    lineage.getSourceColumn());
            String targetNodeId = buildNodeId(lineage.getTargetDsId(), lineage.getTargetTable(),
                    lineage.getTargetColumn());

            if (!nodeIdSet.contains(sourceNodeId)) {
                nodeIdSet.add(sourceNodeId);
                nodes.add(buildNode(sourceNodeId, "SOURCE", lineage.getSourceDsId(),
                        lineage.getSourceTable(), lineage.getSourceColumn(),
                        dsMap.get(lineage.getSourceDsId()), dsLayerMap.get(lineage.getSourceDsId())));
            }

            if (!nodeIdSet.contains(targetNodeId)) {
                nodeIdSet.add(targetNodeId);
                nodes.add(buildNode(targetNodeId, "TARGET", lineage.getTargetDsId(),
                        lineage.getTargetTable(), lineage.getTargetColumn(),
                        dsMap.get(lineage.getTargetDsId()), dsLayerMap.get(lineage.getTargetDsId())));
            }

            edges.add(LineageEdgeVO.builder()
                    .edgeId(String.valueOf(lineage.getId()))
                    .sourceNodeId(sourceNodeId)
                    .targetNodeId(targetNodeId)
                    .lineageId(lineage.getId())
                    .transformType(lineage.getTransformType())
                    .transformExpr(lineage.getTransformExpr())
                    .build());

            inDegree.computeIfAbsent(targetNodeId, k -> 0);
            inDegree.put(targetNodeId, inDegree.get(targetNodeId) + 1);

            if (!inDegree.containsKey(sourceNodeId)) {
                inDegree.put(sourceNodeId, 0);
            }

            adjacencyFromSource.computeIfAbsent(sourceNodeId, k -> new ArrayList<>()).add(targetNodeId);
        }

        // Kahn 算法计算拓扑层级（BFS分层）
        Map<String, Integer> levels = computeTopologicalLevels(nodes, inDegree, adjacencyFromSource);
        for (LineageNodeVO node : nodes) {
            Integer level = levels.get(node.getNodeId());
            if (level != null) {
                node.setLevel(level);
            }
        }

        applyLayout(nodes);

        return Result.success(new LineageGraphVO(nodes, edges));
    }

    @Override
    public Result<List<LineageNodeVO>> getDownstream(Long dsId, String tableName, Integer maxDepth) {
        int depth = (maxDepth != null && maxDepth > 0) ? maxDepth : 3;
        Set<String> visited = new HashSet<>();
        List<LineageNodeVO> result = new ArrayList<>();
        Map<Long, DqDatasource> dsMap = new HashMap<>();
        Map<Long, String> dsLayerMap = new HashMap<>();

        traverseDownstream(dsId, tableName, null, depth, visited, result, dsMap, dsLayerMap);
        applyLayout(result);

        return Result.success(result);
    }

    @Override
    public Result<List<LineageNodeVO>> getUpstream(Long dsId, String tableName, Integer maxDepth) {
        int depth = (maxDepth != null && maxDepth > 0) ? maxDepth : 3;
        Set<String> visited = new HashSet<>();
        List<LineageNodeVO> result = new ArrayList<>();
        Map<Long, DqDatasource> dsMap = new HashMap<>();
        Map<Long, String> dsLayerMap = new HashMap<>();

        traverseUpstream(dsId, tableName, null, depth, visited, result, dsMap, dsLayerMap);
        applyLayout(result);

        return Result.success(result);
    }

    @Override
    public Result<LineageStatsVO> getStats() {
        long total = lineageMapper.selectCount(null);

        LambdaQueryWrapper<GovLineage> tableWrapper = new LambdaQueryWrapper<>();
        tableWrapper.eq(GovLineage::getLineageType, "TABLE");
        long tableCount = lineageMapper.selectCount(tableWrapper);

        LambdaQueryWrapper<GovLineage> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(GovLineage::getLineageType, "COLUMN");
        long columnCount = lineageMapper.selectCount(columnWrapper);

        LambdaQueryWrapper<GovLineage> manualWrapper = new LambdaQueryWrapper<>();
        manualWrapper.eq(GovLineage::getLineageSource, "MANUAL");
        long manualCount = lineageMapper.selectCount(manualWrapper);

        LambdaQueryWrapper<GovLineage> autoWrapper = new LambdaQueryWrapper<>();
        autoWrapper.eq(GovLineage::getLineageSource, "AUTO_PARSER");
        long autoCount = lineageMapper.selectCount(autoWrapper);

        List<GovLineage> all = lineageMapper.selectList(null);
        Set<String> sourceTables = all.stream()
                .map(l -> l.getSourceDsId() + "_" + l.getSourceTable())
                .collect(Collectors.toSet());
        Set<String> targetTables = all.stream()
                .map(l -> l.getTargetDsId() + "_" + l.getTargetTable())
                .collect(Collectors.toSet());

        long columnLineages = all.stream()
                .filter(l -> "COLUMN".equals(l.getLineageType()) && StringUtils.hasText(l.getSourceColumn()))
                .count();

        Map<String, Long> byDsSource = all.stream()
                .collect(Collectors.groupingBy(
                        l -> Optional.ofNullable(l.getSourceDsId())
                                .map(datasourceMapper::selectById)
                                .map(DqDatasource::getDsName)
                                .orElse("Unknown"),
                        Collectors.counting()
                ));

        Map<String, Long> byTransform = all.stream()
                .filter(l -> StringUtils.hasText(l.getTransformType()))
                .collect(Collectors.groupingBy(GovLineage::getTransformType, Collectors.counting()));

        LineageStatsVO stats = LineageStatsVO.builder()
                .totalCount(total)
                .tableLineageCount(tableCount)
                .columnLineageCount(columnCount)
                .manualCount(manualCount)
                .autoParserCount(autoCount)
                .sourceTableCount(sourceTables.size())
                .targetTableCount(targetTables.size())
                .columnCount(columnLineages)
                .byDataSource(byDsSource)
                .byTransformType(byTransform)
                .build();

        return Result.success(stats);
    }

    private LambdaQueryWrapper<GovLineage> buildQueryWrapper(LineageQueryDTO queryDTO) {
        LambdaQueryWrapper<GovLineage> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO == null) {
            return wrapper;
        }

        if (StringUtils.hasText(queryDTO.getLineageType())) {
            wrapper.eq(GovLineage::getLineageType, queryDTO.getLineageType());
        }
        if (queryDTO.getSourceDsId() != null) {
            wrapper.eq(GovLineage::getSourceDsId, queryDTO.getSourceDsId());
        }
        if (StringUtils.hasText(queryDTO.getSourceTable())) {
            wrapper.like(GovLineage::getSourceTable, queryDTO.getSourceTable());
        }
        if (StringUtils.hasText(queryDTO.getSourceColumn())) {
            wrapper.like(GovLineage::getSourceColumn, queryDTO.getSourceColumn());
        }
        if (queryDTO.getTargetDsId() != null) {
            wrapper.eq(GovLineage::getTargetDsId, queryDTO.getTargetDsId());
        }
        if (StringUtils.hasText(queryDTO.getTargetTable())) {
            wrapper.like(GovLineage::getTargetTable, queryDTO.getTargetTable());
        }
        if (StringUtils.hasText(queryDTO.getTargetColumn())) {
            wrapper.like(GovLineage::getTargetColumn, queryDTO.getTargetColumn());
        }
        if (StringUtils.hasText(queryDTO.getTransformType())) {
            wrapper.eq(GovLineage::getTransformType, queryDTO.getTransformType());
        }
        if (StringUtils.hasText(queryDTO.getLineageSource())) {
            wrapper.eq(GovLineage::getLineageSource, queryDTO.getLineageSource());
        }
        if (StringUtils.hasText(queryDTO.getStatus())) {
            wrapper.eq(GovLineage::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getDeptId() != null) {
            wrapper.eq(GovLineage::getDeptId, queryDTO.getDeptId());
        }

        return wrapper;
    }

    private void validateDto(LineageSaveDTO dto, Long excludeId) {
        LambdaQueryWrapper<GovLineage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovLineage::getSourceDsId, dto.getSourceDsId())
                .eq(GovLineage::getSourceTable, dto.getSourceTable())
                .eq(GovLineage::getTargetDsId, dto.getTargetDsId())
                .eq(GovLineage::getTargetTable, dto.getTargetTable())
                .eq(GovLineage::getStatus, "ACTIVE");

        if (StringUtils.hasText(dto.getSourceColumn()) && StringUtils.hasText(dto.getTargetColumn())) {
            wrapper.eq(GovLineage::getSourceColumn, dto.getSourceColumn())
                    .eq(GovLineage::getTargetColumn, dto.getTargetColumn());
        }

        if (excludeId != null) {
            wrapper.ne(GovLineage::getId, excludeId);
        }

        long count = lineageMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(2001, "相同的血缘关系已存在（源数据源+源表+源字段 → 目标数据源+目标表+目标字段）");
        }

        if ("COLUMN".equals(dto.getLineageType())) {
            if (!StringUtils.hasText(dto.getSourceColumn()) || !StringUtils.hasText(dto.getTargetColumn())) {
                throw new BusinessException(2001, "字段级血缘必须填写源字段名和目标字段名");
            }
        }

        DqDatasource sourceDs = datasourceMapper.selectById(dto.getSourceDsId());
        if (sourceDs == null) {
            throw new BusinessException(2001, "源数据源不存在");
        }

        DqDatasource targetDs = datasourceMapper.selectById(dto.getTargetDsId());
        if (targetDs == null) {
            throw new BusinessException(2001, "目标数据源不存在");
        }
    }

    private LineageVO buildVO(GovLineage lineage) {
        LineageVO.LineageVOBuilder builder = LineageVO.builder()
                .id(lineage.getId())
                .lineageType(lineage.getLineageType())
                .sourceDsId(lineage.getSourceDsId())
                .sourceTable(lineage.getSourceTable())
                .sourceColumn(lineage.getSourceColumn())
                .sourceColumnAlias(lineage.getSourceColumnAlias())
                .targetDsId(lineage.getTargetDsId())
                .targetTable(lineage.getTargetTable())
                .targetColumn(lineage.getTargetColumn())
                .targetColumnAlias(lineage.getTargetColumnAlias())
                .transformType(lineage.getTransformType())
                .transformExpr(lineage.getTransformExpr())
                .jobId(lineage.getJobId())
                .jobName(lineage.getJobName())
                .lineageSource(lineage.getLineageSource())
                .deptId(lineage.getDeptId())
                .status(lineage.getStatus() != null ? lineage.getStatus() : "ACTIVE")
                .createUser(lineage.getCreateUser())
                .createUserName(null)
                .createTime(lineage.getCreateTime())
                .updateUser(lineage.getUpdateUser())
                .updateTime(lineage.getUpdateTime());

        DqDatasource sourceDs = datasourceMapper.selectById(lineage.getSourceDsId());
        if (sourceDs != null) {
            builder.sourceDsName(sourceDs.getDsName())
                    .sourceDsCode(sourceDs.getDsCode())
                    .sourceDsType(sourceDs.getDsType());
        }

        DqDatasource targetDs = datasourceMapper.selectById(lineage.getTargetDsId());
        if (targetDs != null) {
            builder.targetDsName(targetDs.getDsName())
                    .targetDsCode(targetDs.getDsCode())
                    .targetDsType(targetDs.getDsType());
        }

        if (lineage.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(lineage.getDeptId());
            if (dept != null) {
                builder.deptName(dept.getDeptName());
            }
        }

        if (StringUtils.hasText(lineage.getLineageType())) {
            builder.lineageTypeLabel(getLineageTypeLabel(lineage.getLineageType()));
        }
        if (StringUtils.hasText(lineage.getTransformType())) {
            builder.transformTypeLabel(getTransformTypeLabel(lineage.getTransformType()));
        }
        if (StringUtils.hasText(lineage.getLineageSource())) {
            builder.lineageSourceLabel(getLineageSourceLabel(lineage.getLineageSource()));
        }

        return builder.build();
    }

    private String getLineageTypeLabel(String type) {
        if (type == null) return "";
        return "TABLE".equals(type) ? "表级血缘" : "字段级血缘";
    }

    private String getTransformTypeLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "DIRECT" -> "直接传递";
            case "SUM" -> "求和聚合";
            case "AVG" -> "求平均聚合";
            case "COUNT" -> "计数聚合";
            case "MAX" -> "取最大值";
            case "MIN" -> "取最小值";
            case "CONCAT" -> "字符串拼接";
            case "CASE_WHEN" -> "条件转换";
            case "CUSTOM_EXPR" -> "自定义表达式";
            default -> type;
        };
    }

    private String getLineageSourceLabel(String source) {
        if (source == null) return "";
        return switch (source) {
            case "MANUAL" -> "手动录入";
            case "AUTO_PARSER" -> "自动解析";
            default -> source;
        };
    }

    private Map<Long, DqDatasource> buildDatasourceMap(List<GovLineage> lineages) {
        Set<Long> dsIds = new HashSet<>();
        for (GovLineage l : lineages) {
            if (l.getSourceDsId() != null) dsIds.add(l.getSourceDsId());
            if (l.getTargetDsId() != null) dsIds.add(l.getTargetDsId());
        }
        if (dsIds.isEmpty()) return Collections.emptyMap();

        List<DqDatasource> dsList = datasourceMapper.selectBatchIds(dsIds);
        return dsList.stream().collect(Collectors.toMap(DqDatasource::getId, d -> d));
    }

    private Map<Long, String> buildDsLayerMap(List<GovLineage> lineages) {
        Set<Long> dsIds = new HashSet<>();
        for (GovLineage l : lineages) {
            if (l.getSourceDsId() != null) dsIds.add(l.getSourceDsId());
            if (l.getTargetDsId() != null) dsIds.add(l.getTargetDsId());
        }
        if (dsIds.isEmpty()) return Collections.emptyMap();

        List<DqDatasource> dsList = datasourceMapper.selectBatchIds(dsIds);
        return dsList.stream().collect(Collectors.toMap(DqDatasource::getId, DqDatasource::getDataLayer));
    }

    private String buildNodeId(Long dsId, String table, String column) {
        if (StringUtils.hasText(column)) {
            return dsId + "_" + table + "_" + column;
        }
        return dsId + "_" + table;
    }

    private LineageNodeVO buildNode(String nodeId, String direction, Long dsId,
                                    String table, String column,
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

    private void applyLayout(List<LineageNodeVO> nodes) {
        if (nodes == null || nodes.isEmpty()) return;

        // 坐标将由前端 D3 力导向图/分层布局重新计算
        // 后端仅标记层级（用于分层布局参考），不预设 x/y
        double angleStep = 2 * Math.PI / nodes.size();
        double radius = 300;
        for (int i = 0; i < nodes.size(); i++) {
            LineageNodeVO node = nodes.get(i);
            if (node.getLevel() != null) {
                continue;
            }
            double angle = angleStep * i;
            node.setX(radius * Math.cos(angle));
            node.setY(radius * Math.sin(angle));
        }
    }

    /**
     * Kahn 算法：BFS 拓扑分层，用于为 DAG 中的每个节点分配层级（level）
     * level = 0 表示没有上游的根节点，数字越大越靠近下游
     */
    private Map<String, Integer> computeTopologicalLevels(
            List<LineageNodeVO> nodes,
            Map<String, Integer> inDegree,
            Map<String, List<String>> adjacency) {

        Map<String, Integer> levels = new HashMap<>();
        Set<String> nodeIds = new HashSet<>();
        for (LineageNodeVO n : nodes) {
            nodeIds.add(n.getNodeId());
            levels.put(n.getNodeId(), 0);
        }

        // 队列初始化：所有入度为 0 的节点（根节点）
        Queue<String> queue = new LinkedList<>();
        for (String nid : nodeIds) {
            if (inDegree.getOrDefault(nid, 0) == 0) {
                queue.offer(nid);
            }
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentLevel = levels.get(current);
            List<String> neighbors = adjacency.getOrDefault(current, Collections.emptyList());
            for (String neighbor : neighbors) {
                int newLevel = currentLevel + 1;
                if (newLevel > levels.getOrDefault(neighbor, 0)) {
                    levels.put(neighbor, newLevel);
                }
                int newInDegree = inDegree.getOrDefault(neighbor, 0) - 1;
                inDegree.put(neighbor, newInDegree);
                if (newInDegree == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        return levels;
    }

    private void traverseDownstream(Long dsId, String table, String column,
                                    int remainingDepth, Set<String> visited,
                                    List<LineageNodeVO> result,
                                    Map<Long, DqDatasource> dsMap,
                                    Map<Long, String> dsLayerMap) {
        if (remainingDepth <= 0) return;

        LambdaQueryWrapper<GovLineage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovLineage::getSourceDsId, dsId)
                .eq(GovLineage::getSourceTable, table)
                .eq(GovLineage::getStatus, "ACTIVE");

        if (StringUtils.hasText(column)) {
            wrapper.eq(GovLineage::getSourceColumn, column);
        }

        List<GovLineage> lineages = lineageMapper.selectList(wrapper);

        DqDatasource targetDs = datasourceMapper.selectById(dsId);
        if (targetDs != null) {
            dsMap.put(dsId, targetDs);
            dsLayerMap.put(dsId, targetDs.getDataLayer());
        }

        for (GovLineage lineage : lineages) {
            String nodeId = buildNodeId(lineage.getTargetDsId(), lineage.getTargetTable(),
                    lineage.getTargetColumn());

            if (visited.contains(nodeId)) continue;
            visited.add(nodeId);

            DqDatasource ds = datasourceMapper.selectById(lineage.getTargetDsId());
            if (ds != null) {
                dsMap.put(lineage.getTargetDsId(), ds);
                dsLayerMap.put(lineage.getTargetDsId(), ds.getDataLayer());
            }

            LineageNodeVO node = buildNode(nodeId, "TARGET", lineage.getTargetDsId(),
                    lineage.getTargetTable(), lineage.getTargetColumn(),
                    ds, dsLayerMap.get(lineage.getTargetDsId()));
            node.setLevel(3 - remainingDepth);
            result.add(node);

            traverseDownstream(lineage.getTargetDsId(), lineage.getTargetTable(),
                    lineage.getTargetColumn(), remainingDepth - 1, visited, result, dsMap, dsLayerMap);
        }
    }

    private void traverseUpstream(Long dsId, String table, String column,
                                  int remainingDepth, Set<String> visited,
                                  List<LineageNodeVO> result,
                                  Map<Long, DqDatasource> dsMap,
                                  Map<Long, String> dsLayerMap) {
        if (remainingDepth <= 0) return;

        LambdaQueryWrapper<GovLineage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovLineage::getTargetDsId, dsId)
                .eq(GovLineage::getTargetTable, table)
                .eq(GovLineage::getStatus, "ACTIVE");

        if (StringUtils.hasText(column)) {
            wrapper.eq(GovLineage::getTargetColumn, column);
        }

        List<GovLineage> lineages = lineageMapper.selectList(wrapper);

        DqDatasource sourceDs = datasourceMapper.selectById(dsId);
        if (sourceDs != null) {
            dsMap.put(dsId, sourceDs);
            dsLayerMap.put(dsId, sourceDs.getDataLayer());
        }

        for (GovLineage lineage : lineages) {
            String nodeId = buildNodeId(lineage.getSourceDsId(), lineage.getSourceTable(),
                    lineage.getSourceColumn());

            if (visited.contains(nodeId)) continue;
            visited.add(nodeId);

            DqDatasource ds = datasourceMapper.selectById(lineage.getSourceDsId());
            if (ds != null) {
                dsMap.put(lineage.getSourceDsId(), ds);
                dsLayerMap.put(lineage.getSourceDsId(), ds.getDataLayer());
            }

            LineageNodeVO node = buildNode(nodeId, "SOURCE", lineage.getSourceDsId(),
                    lineage.getSourceTable(), lineage.getSourceColumn(),
                    ds, dsLayerMap.get(lineage.getSourceDsId()));
            node.setLevel(3 - remainingDepth);
            result.add(node);

            traverseUpstream(lineage.getSourceDsId(), lineage.getSourceTable(),
                    lineage.getSourceColumn(), remainingDepth - 1, visited, result, dsMap, dsLayerMap);
        }
    }
}
