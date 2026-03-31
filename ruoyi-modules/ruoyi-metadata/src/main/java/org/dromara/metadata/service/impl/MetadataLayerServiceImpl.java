package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.DataLayer;
import org.dromara.metadata.domain.bo.DataLayerBo;
import org.dromara.metadata.domain.vo.DataLayerVo;
import org.dromara.metadata.mapper.DataLayerMapper;
import org.dromara.metadata.service.IDataLayerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数仓分层服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class MetadataLayerServiceImpl implements IDataLayerService {

    private final DataLayerMapper baseMapper;

    @Override
    public TableDataInfo<DataLayerVo> pageLayerList(DataLayerBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DataLayer> wrapper = buildQueryWrapper(bo);
        Page<DataLayerVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public DataLayerVo getLayerById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertLayer(DataLayerBo bo) {
        if (StringUtils.isNotBlank(bo.getLayerCode())) {
            DataLayer exist = baseMapper.selectOne(
                Wrappers.<DataLayer>lambdaQuery().eq(DataLayer::getLayerCode, bo.getLayerCode()));
            if (exist != null) {
                throw new ServiceException("分层编码已存在");
            }
        }
        DataLayer entity = MapstructUtils.convert(bo, DataLayer.class);
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateLayer(DataLayerBo bo) {
        if (ObjectUtil.isNull(bo.getId())) {
            throw new ServiceException("分层ID不能为空");
        }
        if (StringUtils.isNotBlank(bo.getLayerCode())) {
            boolean exist = baseMapper.exists(Wrappers.<DataLayer>lambdaQuery()
                .eq(DataLayer::getLayerCode, bo.getLayerCode())
                .ne(DataLayer::getId, bo.getId()));
            if (exist) {
                throw new ServiceException("分层编码已存在");
            }
        }
        DataLayer entity = MapstructUtils.convert(bo, DataLayer.class);
        return baseMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteLayer(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }

    @Override
    public List<DataLayerVo> listLayer(DataLayerBo bo) {
        LambdaQueryWrapper<DataLayer> wrapper = buildQueryWrapper(bo);
        return baseMapper.selectVoList(wrapper);
    }

    private LambdaQueryWrapper<DataLayer> buildQueryWrapper(DataLayerBo bo) {
        return Wrappers.<DataLayer>lambdaQuery()
            .like(StringUtils.isNotBlank(bo.getLayerName()), DataLayer::getLayerName, bo.getLayerName())
            .like(StringUtils.isNotBlank(bo.getLayerCode()), DataLayer::getLayerCode, bo.getLayerCode())
            .eq(StringUtils.isNotBlank(bo.getStatus()), DataLayer::getStatus, bo.getStatus())
            .orderByAsc(DataLayer::getSortOrder);
    }

    @Override
    public List<DataLayerVo> listAll() {
        return baseMapper.selectVoList(
            Wrappers.<DataLayer>lambdaQuery().orderByAsc(DataLayer::getSortOrder)
        );
    }

    @Override
    public List<DataLayerVo> listEnabled() {
        return baseMapper.selectVoList(
            Wrappers.<DataLayer>lambdaQuery()
                .eq(DataLayer::getStatus, "0")
                .orderByAsc(DataLayer::getSortOrder)
        );
    }
}
