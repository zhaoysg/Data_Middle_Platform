package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.GovLineage;
import org.dromara.metadata.domain.MetadataColumn;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.domain.SecColumnSensitivity;
import org.dromara.metadata.domain.bo.GovLineageBo;
import org.dromara.metadata.domain.vo.GovLineageVo;
import org.dromara.metadata.mapper.GovLineageMapper;
import org.dromara.metadata.mapper.MetadataColumnMapper;
import org.dromara.metadata.mapper.MetadataTableMapper;
import org.dromara.metadata.mapper.SecColumnSensitivityMapper;
import org.dromara.metadata.service.IGovLineageService;
import org.dromara.metadata.support.DatasourceHelper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 治理血缘关系服务实现
 *
 * <p>安全修复:
 * <ul>
 *   <li>S1: 所有查询/写入操作添加数据源权限过滤</li>
 *   <li>BFS N+1: listUpstream/listDownstream 使用批量预加载替代逐条查询</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GovLineageServiceImpl implements IGovLineageService {

    private final GovLineageMapper baseMapper;
    private final DatasourceHelper datasourceHelper;
    private final MetadataColumnMapper columnMapper;
    private final MetadataTableMapper tableMapper;
    private final SecColumnSensitivityMapper sensitivityMapper;

    private static final int MAX_DEPTH = 5;

    @Override
    public TableDataInfo<GovLineageVo> queryPageList(GovLineageBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<GovLineage> wrapper = buildQueryWrapper(bo);
        applyAccessibleDatasourceFilter(wrapper);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public GovLineageVo queryById(Long id) {
        GovLineageVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("血缘关系不存在或无权访问: " + id);
        }
        requireAccessibleRecord(vo.getSrcDsId());
        requireAccessibleRecord(vo.getTgtDsId());
        return vo;
    }

    @Override
    public Long insertByBo(GovLineageBo bo) {
        requireAccessibleRecord(bo.getSrcDsId());
        requireAccessibleRecord(bo.getTgtDsId());
        GovLineage entity = MapstructUtils.convert(bo, GovLineage.class);
        if (entity.getVerifyStatus() == null) {
            entity.setVerifyStatus("UNVERIFIED");
        }
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateByBo(GovLineageBo bo) {
        GovLineageVo existing = queryById(bo.getId());
        requireAccessibleRecord(existing.getSrcDsId());
        requireAccessibleRecord(existing.getTgtDsId());
        if (bo.getSrcDsId() != null) {
            requireAccessibleRecord(bo.getSrcDsId());
        }
        if (bo.getTgtDsId() != null) {
            requireAccessibleRecord(bo.getTgtDsId());
        }
        GovLineage entity = MapstructUtils.convert(bo, GovLineage.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<GovLineageVo> existing = baseMapper.selectVoList(
            Wrappers.<GovLineage>lambdaQuery().in(GovLineage::getId, ids)
        );
        for (GovLineageVo vo : existing) {
            requireAccessibleRecord(vo.getSrcDsId());
            requireAccessibleRecord(vo.getTgtDsId());
        }
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public List<GovLineageVo> listUpstream(Long dsId, String tableName, int depth) {
        requireAccessibleRecord(dsId);
        int actualDepth = depth > 0 ? Math.min(depth, MAX_DEPTH) : MAX_DEPTH;
        Set<String> visited = new HashSet<>();
        List<Long> resultIds = new ArrayList<>();
        Queue<LineageNode> queue = new LinkedList<>();
        queue.offer(new LineageNode(dsId, tableName, null, 0));

        List<Long> accessibleDsIds = datasourceHelper.listAccessibleDatasourceIds();
        while (!queue.isEmpty()) {
            LineageNode node = queue.poll();
            String key = node.dsId + ":" + node.tableName;
            if (visited.contains(key) || node.level >= actualDepth) {
                continue;
            }
            visited.add(key);

            List<GovLineage> upstreams = baseMapper.selectList(
                Wrappers.<GovLineage>lambdaQuery()
                    .in(GovLineage::getTgtDsId, accessibleDsIds)
                    .eq(GovLineage::getTgtDsId, node.dsId)
                    .eq(GovLineage::getTgtTableName, node.tableName)
                    .eq(StringUtils.isNotBlank(node.columnName), GovLineage::getTgtColumnName, node.columnName)
            );

            for (GovLineage lineage : upstreams) {
                resultIds.add(lineage.getId());
                queue.offer(new LineageNode(
                    lineage.getSrcDsId(),
                    lineage.getSrcTableName(),
                    lineage.getSrcColumnName(),
                    node.level + 1
                ));
            }
        }

        return batchConvertToVoWithLevel(resultIds, actualDepth);
    }

    @Override
    public List<GovLineageVo> listDownstream(Long dsId, String tableName, int depth) {
        requireAccessibleRecord(dsId);
        int actualDepth = depth > 0 ? Math.min(depth, MAX_DEPTH) : MAX_DEPTH;
        Set<String> visited = new HashSet<>();
        List<Long> resultIds = new ArrayList<>();
        Map<Long, Integer> levelMap = new HashMap<>();
        Queue<LineageNode> queue = new LinkedList<>();
        queue.offer(new LineageNode(dsId, tableName, null, 0));

        List<Long> accessibleDsIds = datasourceHelper.listAccessibleDatasourceIds();
        while (!queue.isEmpty()) {
            LineageNode node = queue.poll();
            String key = node.dsId + ":" + node.tableName;
            if (visited.contains(key) || node.level >= actualDepth) {
                continue;
            }
            visited.add(key);

            List<GovLineage> downstreams = baseMapper.selectList(
                Wrappers.<GovLineage>lambdaQuery()
                    .in(GovLineage::getSrcDsId, accessibleDsIds)
                    .eq(GovLineage::getSrcDsId, node.dsId)
                    .eq(GovLineage::getSrcTableName, node.tableName)
                    .eq(StringUtils.isNotBlank(node.columnName), GovLineage::getSrcColumnName, node.columnName)
            );

            for (GovLineage lineage : downstreams) {
                resultIds.add(lineage.getId());
                queue.offer(new LineageNode(
                    lineage.getTgtDsId(),
                    lineage.getTgtTableName(),
                    lineage.getTgtColumnName(),
                    node.level + 1
                ));
            }
        }

        return batchConvertToVoWithLevel(resultIds, actualDepth);
    }

    @Override
    public List<GovLineageVo> listLineage(GovLineageBo bo) {
        LambdaQueryWrapper<GovLineage> wrapper = buildQueryWrapper(bo);
        applyAccessibleDatasourceFilter(wrapper);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public TableDataInfo<GovLineageVo> pageLineageList(GovLineageBo bo, PageQuery pageQuery) {
        return queryPageList(bo, pageQuery);
    }

    @Override
    public GovLineageVo getLineageById(Long id) {
        return queryById(id);
    }

    @Override
    public Long insertLineage(GovLineageBo bo) {
        return insertByBo(bo);
    }

    @Override
    public int updateLineage(GovLineageBo bo) {
        return updateByBo(bo);
    }

    @Override
    public int deleteLineage(Long[] ids) {
        return deleteByIds(ids == null ? List.of() : List.of(ids));
    }

    /**
     * 批量将 ID 列表转换为 VO 并设置层级。
     * 解决 BFS N+1 问题：所有血缘记录的详情通过一次 DB 查询获取。
     * 同时自动注入敏感字段统计和数仓分层信息。
     * 已应用数据源权限过滤（修复：batchConvertToVoWithLevel 批量转换缺少 datasourceHelper 权限过滤）
     */
    private List<GovLineageVo> batchConvertToVoWithLevel(List<Long> lineageIds, int maxDepth) {
        if (lineageIds == null || lineageIds.isEmpty()) {
            return List.of();
        }
        List<Long> uniqueIds = lineageIds.stream().distinct().toList();
        List<Long> accessibleDsIds = datasourceHelper.listAccessibleDatasourceIds();

        List<GovLineageVo> voList;
        if (accessibleDsIds.isEmpty()) {
            return List.of();
        }
        // 权限过滤：srcDsId 或 tgtDsId 在可访问范围内
        voList = baseMapper.selectVoList(
            Wrappers.<GovLineage>lambdaQuery()
                .in(GovLineage::getId, uniqueIds)
                .and(w -> w
                    .in(GovLineage::getSrcDsId, accessibleDsIds)
                    .or()
                    .in(GovLineage::getTgtDsId, accessibleDsIds)
                )
        );

        // 权限过滤后的 (dsId, tableName) 才查询敏感性和分层
        Set<String> srcKeys = voList.stream()
            .map(v -> v.getSrcDsId() + "|" + v.getSrcTableName())
            .filter(k -> k != null && !k.endsWith("|null"))
            .collect(Collectors.toSet());
        Set<String> tgtKeys = voList.stream()
            .map(v -> v.getTgtDsId() + "|" + v.getTgtTableName())
            .filter(k -> k != null && !k.endsWith("|null"))
            .collect(Collectors.toSet());

        Map<String, SensitivitySummary> sensMap = buildSensitivitySummaryMap(srcKeys, tgtKeys, accessibleDsIds);
        Map<String, String> layerMap = buildLayerMap(srcKeys, tgtKeys, accessibleDsIds);

        for (GovLineageVo vo : voList) {
            enrichSensitivity(vo, sensMap);
            enrichLayer(vo, layerMap);
        }

        List<GovLineageVo> result = new ArrayList<>();
        for (Long id : lineageIds) {
            GovLineageVo vo = voList.stream().filter(v -> v.getId().equals(id)).findFirst().orElse(null);
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }

    /**
     * 敏感字段汇总信息
     */
    private record SensitivitySummary(int count, String highestLevel) {}

    /**
     * 批量构建敏感字段统计映射。
     * 仅对用户有权限的数据源进行查询。
     */
    private Map<String, SensitivitySummary> buildSensitivitySummaryMap(
            Set<String> srcKeys, Set<String> tgtKeys, List<Long> accessibleDsIds) {
        Map<String, SensitivitySummary> result = new HashMap<>();
        if (srcKeys.isEmpty() && tgtKeys.isEmpty()) {
            return result;
        }

        Set<String> allKeys = new HashSet<>(srcKeys);
        allKeys.addAll(tgtKeys);

        try {
            // 批量查询所有可访问数据源的敏感字段
            var cols = sensitivityMapper.selectList(
                Wrappers.<SecColumnSensitivity>lambdaQuery()
                    .in(SecColumnSensitivity::getDsId, accessibleDsIds)
                    .in(SecColumnSensitivity::getTableName,
                        allKeys.stream().map(k -> k.split("\\|", 2)[1]).collect(Collectors.toList()))
            );

            // 按 (dsId, tableName) 分组聚合
            Map<String, List<SecColumnSensitivity>> grouped = cols.stream()
                .collect(Collectors.groupingBy(c -> c.getDsId() + "|" + c.getTableName()));

            for (Map.Entry<String, List<SecColumnSensitivity>> entry : grouped.entrySet()) {
                List<SecColumnSensitivity> list = entry.getValue();
                String highestLevel = "LOW";
                for (SecColumnSensitivity col : list) {
                    String lvl = col.getLevelCode();
                    if ("HIGH".equals(lvl)) {
                        highestLevel = "HIGH";
                        break;
                    } else if ("MEDIUM".equals(lvl) && "LOW".equals(highestLevel)) {
                        highestLevel = "MEDIUM";
                    }
                }
                result.put(entry.getKey(), new SensitivitySummary(list.size(), highestLevel));
            }
        } catch (Exception e) {
            log.warn("批量查询敏感字段统计失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 批量构建数仓分层映射。
     * 仅对用户有权限的数据源进行查询。
     */
    private Map<String, String> buildLayerMap(Set<String> srcKeys, Set<String> tgtKeys, List<Long> accessibleDsIds) {
        Map<String, String> result = new HashMap<>();
        Set<String> allKeys = new HashSet<>(srcKeys);
        allKeys.addAll(tgtKeys);
        if (allKeys.isEmpty()) {
            return result;
        }

        List<String> tableNames = allKeys.stream()
            .map(k -> k.split("\\|", 2)[1])
            .distinct()
            .collect(Collectors.toList());

        try {
            var tables = tableMapper.selectList(
                Wrappers.<MetadataTable>lambdaQuery()
                    .in(MetadataTable::getDsId, accessibleDsIds)
                    .in(MetadataTable::getTableName, tableNames)
                    .isNotNull(MetadataTable::getDataLayer)
            );

            for (MetadataTable t : tables) {
                String key = t.getDsId() + "|" + t.getTableName();
                result.put(key, t.getDataLayer());
            }
        } catch (Exception e) {
            log.warn("批量查询数仓分层失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 为 VO 注入敏感字段统计信息（同时注入源端和目标端信息）
     * 前端图谱中节点ID格式为 "dsId:tableName"，因此两端都需要注入
     */
    private void enrichSensitivity(GovLineageVo vo, Map<String, SensitivitySummary> sensMap) {
        // 源表敏感信息
        if (vo.getSrcDsId() != null && vo.getSrcTableName() != null) {
            String key = vo.getSrcDsId() + "|" + vo.getSrcTableName();
            SensitivitySummary sum = sensMap.get(key);
            if (sum != null) {
                if ("HIGH".equals(sum.highestLevel())) {
                    vo.setSensitivityLevel("HIGH");
                } else if ("MEDIUM".equals(sum.highestLevel())) {
                    if (vo.getSensitivityLevel() == null || !"HIGH".equals(vo.getSensitivityLevel())) {
                        vo.setSensitivityLevel("MEDIUM");
                    }
                }
                Integer existing = vo.getSensitiveCount();
                if (existing == null || sum.count() > existing) {
                    vo.setSensitiveCount(sum.count());
                }
            }
        }
        // 目标表敏感信息
        if (vo.getTgtDsId() != null && vo.getTgtTableName() != null) {
            String key = vo.getTgtDsId() + "|" + vo.getTgtTableName();
            SensitivitySummary sum = sensMap.get(key);
            if (sum != null) {
                if ("HIGH".equals(sum.highestLevel())) {
                    vo.setSensitivityLevel("HIGH");
                } else if ("MEDIUM".equals(sum.highestLevel())) {
                    if (vo.getSensitivityLevel() == null || !"HIGH".equals(vo.getSensitivityLevel())) {
                        vo.setSensitivityLevel("MEDIUM");
                    }
                }
                Integer existing = vo.getSensitiveCount();
                if (existing == null || sum.count() > existing) {
                    vo.setSensitiveCount(sum.count());
                }
            }
        }
    }

    /**
     * 为 VO 注入数仓分层信息（同时处理源表和目标表）
     * 前端图谱中节点ID格式为 "dsId:tableName"，因此两端都需要注入
     */
    private void enrichLayer(GovLineageVo vo, Map<String, String> layerMap) {
        // 源表分层信息
        if (vo.getSrcDsId() != null && vo.getSrcTableName() != null) {
            String srcKey = vo.getSrcDsId() + "|" + vo.getSrcTableName();
            String layer = layerMap.get(srcKey);
            if (layer != null) {
                vo.setLayerCode(layer);
                vo.setLayerName(resolveLayerName(layer));
            }
        }
        // 目标表分层信息（仅在源表没有设置时覆盖）
        if (vo.getTgtDsId() != null && vo.getTgtTableName() != null && vo.getLayerCode() == null) {
            String tgtKey = vo.getTgtDsId() + "|" + vo.getTgtTableName();
            String layer = layerMap.get(tgtKey);
            if (layer != null) {
                vo.setLayerCode(layer);
                vo.setLayerName(resolveLayerName(layer));
            }
        }
    }

    private String resolveLayerName(String code) {
        if (code == null) return null;
        return switch (code) {
            case "ODS" -> "操作数据层";
            case "DWD" -> "明细宽表层";
            case "DWS" -> "汇总宽表层";
            case "ADS", "DIM" -> "应用数据层";
            default -> code;
        };
    }

    /**
     * 构建查询条件。
     */
    private LambdaQueryWrapper<GovLineage> buildQueryWrapper(GovLineageBo bo) {
        LambdaQueryWrapper<GovLineage> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ObjectUtil.isNotNull(bo.getSrcDsId()), GovLineage::getSrcDsId, bo.getSrcDsId())
            .like(StringUtils.isNotBlank(bo.getSrcTableName()), GovLineage::getSrcTableName, bo.getSrcTableName())
            .like(StringUtils.isNotBlank(bo.getSrcColumnName()), GovLineage::getSrcColumnName, bo.getSrcColumnName())
            .eq(ObjectUtil.isNotNull(bo.getTgtDsId()), GovLineage::getTgtDsId, bo.getTgtDsId())
            .like(StringUtils.isNotBlank(bo.getTgtTableName()), GovLineage::getTgtTableName, bo.getTgtTableName())
            .like(StringUtils.isNotBlank(bo.getTgtColumnName()), GovLineage::getTgtColumnName, bo.getTgtColumnName())
            .eq(StringUtils.isNotBlank(bo.getLineageType()), GovLineage::getLineageType, bo.getLineageType())
            .eq(StringUtils.isNotBlank(bo.getVerifyStatus()), GovLineage::getVerifyStatus, bo.getVerifyStatus())
            .orderByDesc(GovLineage::getCreateTime);
        return wrapper;
    }

    /**
     * 应用数据源权限过滤：只返回用户有权限访问的数据源的血缘关系。
     * 即：srcDsId 和 tgtDsId 都在用户可访问的 dsId 集合中。
     */
    private void applyAccessibleDatasourceFilter(LambdaQueryWrapper<GovLineage> wrapper) {
        List<Long> accessibleDsIds = datasourceHelper.listAccessibleDatasourceIds();
        if (accessibleDsIds.isEmpty()) {
            wrapper.eq(GovLineage::getId, -1L);
            return;
        }
        wrapper.and(w -> w
            .in(GovLineage::getSrcDsId, accessibleDsIds)
            .or()
            .in(GovLineage::getTgtDsId, accessibleDsIds)
        );
    }

    /**
     * 校验用户是否有权访问指定数据源的记录。
     */
    private void requireAccessibleRecord(Long dsId) {
        if (dsId == null) {
            return;
        }
        List<Long> accessibleDsIds = datasourceHelper.listAccessibleDatasourceIds();
        if (!accessibleDsIds.contains(dsId)) {
            throw new ServiceException("无权访问该数据源的血缘关系: dsId=" + dsId);
        }
    }

    /**
     * 血缘节点（用于BFS遍历）
     */
    private static class LineageNode {
        Long dsId;
        String tableName;
        String columnName;
        int level;

        LineageNode(Long dsId, String tableName, String columnName, int level) {
            this.dsId = dsId;
            this.tableName = tableName;
            this.columnName = columnName;
            this.level = level;
        }
    }

    @Override
    public int autoDiscover(Long dsId, String tableName) {
        if (dsId == null || tableName == null) {
            return 0;
        }
        requireAccessibleRecord(dsId);

        List<GovLineage> discovered = new ArrayList<>();

        var columns = columnMapper.selectList(
            Wrappers.<MetadataColumn>lambdaQuery()
                .eq(MetadataColumn::getDsId, dsId)
                .eq(MetadataColumn::getTableName, tableName)
        );

        for (MetadataColumn col : columns) {
            String comment = col.getColumnComment();
            if (StringUtils.isBlank(comment)) {
                continue;
            }
            String upper = comment.toUpperCase();
            if (upper.contains("REFERENCES") || upper.contains("FK") || upper.contains("FOREIGN KEY")) {
                var bo = new GovLineageBo();
                bo.setTgtDsId(dsId);
                bo.setTgtTableName(tableName);
                bo.setTgtColumnName(col.getColumnName());
                bo.setLineageType("DERIVED");
                bo.setTransformType("UNKNOWN");
                bo.setVerifyStatus("UNVERIFIED");
                discovered.add(MapstructUtils.convert(bo, GovLineage.class));
            }
        }

        if (!discovered.isEmpty()) {
            for (GovLineage lineage : discovered) {
                baseMapper.insert(lineage);
            }
            log.info("自动发现血缘: dsId={}, table={}, discovered={}", dsId, tableName, discovered.size());
        }

        return discovered.size();
    }
}
