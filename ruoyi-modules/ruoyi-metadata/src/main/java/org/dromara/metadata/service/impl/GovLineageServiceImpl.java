package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.GovLineage;
import org.dromara.metadata.domain.bo.GovLineageBo;
import org.dromara.metadata.domain.vo.GovLineageVo;
import org.dromara.metadata.mapper.GovLineageMapper;
import org.dromara.metadata.service.IGovLineageService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 治理血缘关系服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GovLineageServiceImpl implements IGovLineageService {

    private final GovLineageMapper baseMapper;

    private static final int MAX_DEPTH = 5;

    @Override
    public TableDataInfo<GovLineageVo> queryPageList(GovLineageBo bo, PageQuery pageQuery) {
        Wrapper<GovLineage> wrapper = buildQueryWrapper(bo);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public GovLineageVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public Long insertByBo(GovLineageBo bo) {
        GovLineage entity = MapstructUtils.convert(bo, GovLineage.class);
        if (entity.getVerifyStatus() == null) {
            entity.setVerifyStatus("UNVERIFIED");
        }
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateByBo(GovLineageBo bo) {
        GovLineage entity = MapstructUtils.convert(bo, GovLineage.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public List<GovLineageVo> listUpstream(Long dsId, String tableName, int depth) {
        int actualDepth = depth > 0 ? Math.min(depth, MAX_DEPTH) : MAX_DEPTH;
        Set<String> visited = new HashSet<>();
        List<GovLineageVo> result = new ArrayList<>();
        Queue<LineageNode> queue = new LinkedList<>();
        queue.offer(new LineageNode(dsId, tableName, null, 0));

        while (!queue.isEmpty()) {
            LineageNode node = queue.poll();
            String key = node.dsId + ":" + node.tableName;
            if (visited.contains(key) || node.level >= actualDepth) {
                continue;
            }
            visited.add(key);

            // 查询上游血缘 - 当前节点作为target，找source
            List<GovLineage> upstreams = baseMapper.selectList(
                Wrappers.<GovLineage>lambdaQuery()
                    .eq(GovLineage::getTgtDsId, node.dsId)
                    .eq(GovLineage::getTgtTableName, node.tableName)
                    .eq(StringUtils.isNotBlank(node.columnName), GovLineage::getTgtColumnName, node.columnName)
            );

            for (GovLineage lineage : upstreams) {
                GovLineageVo vo = convertToVo(lineage);
                vo.setLevel(node.level + 1);
                result.add(vo);

                // 继续追溯上游
                queue.offer(new LineageNode(
                    lineage.getSrcDsId(),
                    lineage.getSrcTableName(),
                    lineage.getSrcColumnName(),
                    node.level + 1
                ));
            }
        }

        return result;
    }

    @Override
    public List<GovLineageVo> listDownstream(Long dsId, String tableName, int depth) {
        int actualDepth = depth > 0 ? Math.min(depth, MAX_DEPTH) : MAX_DEPTH;
        Set<String> visited = new HashSet<>();
        List<GovLineageVo> result = new ArrayList<>();
        Queue<LineageNode> queue = new LinkedList<>();
        queue.offer(new LineageNode(dsId, tableName, null, 0));

        while (!queue.isEmpty()) {
            LineageNode node = queue.poll();
            String key = node.dsId + ":" + node.tableName;
            if (visited.contains(key) || node.level >= actualDepth) {
                continue;
            }
            visited.add(key);

            // 查询下游血缘 - 当前节点作为source，找target
            List<GovLineage> downstreams = baseMapper.selectList(
                Wrappers.<GovLineage>lambdaQuery()
                    .eq(GovLineage::getSrcDsId, node.dsId)
                    .eq(GovLineage::getSrcTableName, node.tableName)
                    .eq(StringUtils.isNotBlank(node.columnName), GovLineage::getSrcColumnName, node.columnName)
            );

            for (GovLineage lineage : downstreams) {
                GovLineageVo vo = convertToVo(lineage);
                vo.setLevel(node.level + 1);
                result.add(vo);

                // 继续追溯下游
                queue.offer(new LineageNode(
                    lineage.getTgtDsId(),
                    lineage.getTgtTableName(),
                    lineage.getTgtColumnName(),
                    node.level + 1
                ));
            }
        }

        return result;
    }

    @Override
    public List<GovLineageVo> listLineage(GovLineageBo bo) {
        Wrapper<GovLineage> wrapper = buildQueryWrapper(bo);
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
        return deleteByIds(List.of(ids));
    }

    /**
     * 转换为VO并添加关联名称
     */
    private GovLineageVo convertToVo(GovLineage lineage) {
        GovLineageVo vo = baseMapper.selectVoById(lineage.getId());
        if (vo == null) {
            vo = MapstructUtils.convert(lineage, GovLineageVo.class);
        }
        return vo;
    }

    /**
     * 构建查询条件
     */
    private Wrapper<GovLineage> buildQueryWrapper(GovLineageBo bo) {
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
}
