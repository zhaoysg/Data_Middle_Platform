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
import org.dromara.metadata.domain.GovGlossaryMapping;
import org.dromara.metadata.domain.bo.GovGlossaryMappingBo;
import org.dromara.metadata.domain.vo.GovGlossaryMappingVo;
import org.dromara.metadata.mapper.GovGlossaryMappingMapper;
import org.dromara.metadata.service.IGovGlossaryMappingService;
import org.dromara.metadata.support.DatasourceHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 治理 Glossary 映射服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GovGlossaryMappingServiceImpl implements IGovGlossaryMappingService {

    private final GovGlossaryMappingMapper baseMapper;
    private final DatasourceHelper datasourceHelper;

    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<GovGlossaryMappingVo> queryPageList(
            GovGlossaryMappingBo bo, PageQuery pageQuery) {
        Wrapper<GovGlossaryMapping> wrapper = buildQueryWrapper(bo);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return page;
    }

    @Override
    public GovGlossaryMappingVo queryById(Long id) {
        GovGlossaryMappingVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("映射不存在: " + id);
        }
        requireAccessibleRecord(vo.getDsId());
        return vo;
    }

    @Override
    public Long insertByBo(GovGlossaryMappingBo bo) {
        if (bo.getDsId() != null) {
            requireAccessibleRecord(bo.getDsId());
        }
        GovGlossaryMapping entity = MapstructUtils.convert(bo, GovGlossaryMapping.class);
        if (entity.getConfidence() == null) {
            entity.setConfidence(100);
        }
        if (entity.getDelFlag() == null) {
            entity.setDelFlag("0");
        }
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateByBo(GovGlossaryMappingBo bo) {
        GovGlossaryMappingVo existing = queryById(bo.getId());
        if (bo.getDsId() != null && !bo.getDsId().equals(existing.getDsId())) {
            requireAccessibleRecord(bo.getDsId());
        }
        GovGlossaryMapping entity = MapstructUtils.convert(bo, GovGlossaryMapping.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<GovGlossaryMappingVo> existing = baseMapper.selectVoList(
            Wrappers.<GovGlossaryMapping>lambdaQuery().in(GovGlossaryMapping::getId, ids)
        );
        for (GovGlossaryMappingVo vo : existing) {
            requireAccessibleRecord(vo.getDsId());
        }
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public List<GovGlossaryMappingVo> listByTermId(Long termId) {
        if (termId == null) {
            return List.of();
        }
        return baseMapper.selectVoList(
            Wrappers.<GovGlossaryMapping>lambdaQuery()
                .eq(GovGlossaryMapping::getTermId, termId)
                .orderByDesc(GovGlossaryMapping::getCreateTime)
        );
    }

    @Override
    public List<GovGlossaryMappingVo> listByTable(Long dsId, String tableName) {
        if (dsId == null || tableName == null) {
            return List.of();
        }
        requireAccessibleRecord(dsId);
        return baseMapper.selectVoList(
            Wrappers.<GovGlossaryMapping>lambdaQuery()
                .eq(GovGlossaryMapping::getDsId, dsId)
                .eq(GovGlossaryMapping::getTableName, tableName)
                .orderByDesc(GovGlossaryMapping::getCreateTime)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchInsert(List<GovGlossaryMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return 0;
        }
        for (GovGlossaryMapping m : mappings) {
            if (m.getDsId() != null) {
                requireAccessibleRecord(m.getDsId());
            }
            if (m.getConfidence() == null) {
                m.setConfidence(100);
            }
            if (m.getDelFlag() == null) {
                m.setDelFlag("0");
            }
        }
        int count = 0;
        for (GovGlossaryMapping m : mappings) {
            baseMapper.insert(m);
            count++;
        }
        return count;
    }

    private Wrapper<GovGlossaryMapping> buildQueryWrapper(GovGlossaryMappingBo bo) {
        LambdaQueryWrapper<GovGlossaryMapping> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ObjectUtil.isNotNull(bo.getTermId()), GovGlossaryMapping::getTermId, bo.getTermId())
            .like(StringUtils.isNotBlank(bo.getTermName()), GovGlossaryMapping::getTermName, bo.getTermName())
            .eq(ObjectUtil.isNotNull(bo.getDsId()), GovGlossaryMapping::getDsId, bo.getDsId())
            .like(StringUtils.isNotBlank(bo.getTableName()), GovGlossaryMapping::getTableName, bo.getTableName())
            .like(StringUtils.isNotBlank(bo.getColumnName()), GovGlossaryMapping::getColumnName, bo.getColumnName())
            .eq(StringUtils.isNotBlank(bo.getAssetType()), GovGlossaryMapping::getAssetType, bo.getAssetType())
            .eq(StringUtils.isNotBlank(bo.getMappingType()), GovGlossaryMapping::getMappingType, bo.getMappingType())
            .orderByDesc(GovGlossaryMapping::getCreateTime);
        return wrapper;
    }

    /**
     * 校验用户是否有权访问指定数据源的映射记录
     */
    private void requireAccessibleRecord(Long dsId) {
        if (dsId == null) {
            return;
        }
        List<Long> accessibleDsIds = datasourceHelper.listAccessibleDatasourceIds();
        if (!accessibleDsIds.contains(dsId)) {
            throw new ServiceException("无权访问该数据源的术语映射: dsId=" + dsId);
        }
    }

    // ==================== RuoYi 标准方法实现 ====================

    @Override
    public List<GovGlossaryMappingVo> listMapping(GovGlossaryMappingBo bo) {
        Wrapper<GovGlossaryMapping> wrapper = buildQueryWrapper(bo);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public GovGlossaryMappingVo getMappingById(Long id) {
        return queryById(id);
    }

    @Override
    public Long insertMapping(GovGlossaryMappingBo bo) {
        return insertByBo(bo);
    }

    @Override
    public int updateMapping(GovGlossaryMappingBo bo) {
        return updateByBo(bo);
    }

    @Override
    public int deleteMapping(Long[] ids) {
        return deleteByIds(ids == null ? List.of() : List.of(ids));
    }
}
