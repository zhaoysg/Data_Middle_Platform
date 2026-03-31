package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
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
import org.dromara.metadata.domain.DataDomain;
import org.dromara.metadata.domain.bo.DataDomainBo;
import org.dromara.metadata.domain.vo.DataDomainVo;
import org.dromara.metadata.mapper.DataDomainMapper;
import org.dromara.metadata.service.IMetadataDomainService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据域服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class DataDomainServiceImpl implements IMetadataDomainService {

    private final DataDomainMapper baseMapper;

    @Override
    public TableDataInfo<DataDomainVo> pageDomainList(DataDomainBo bo, PageQuery pageQuery) {
        Wrapper<DataDomain> wrapper = buildQueryWrapper(bo);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<DataDomainVo> listAll() {
        return baseMapper.selectVoList(
            Wrappers.lambdaQuery(DataDomain.class)
                .orderByAsc(DataDomain::getDomainName)
        );
    }

    @Override
    public DataDomainVo getDomainById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public Long insertDomain(DataDomainBo bo) {
        DataDomain entity = MapstructUtils.convert(bo, DataDomain.class);
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateDomain(DataDomainBo bo) {
        DataDomain entity = MapstructUtils.convert(bo, DataDomain.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteDomain(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }

    @Override
    public List<DataDomainVo> listEnabled() {
        return baseMapper.selectVoList(
            Wrappers.lambdaQuery(DataDomain.class)
                .eq(DataDomain::getStatus, "0")
                .orderByAsc(DataDomain::getDomainName)
        );
    }

    @Override
    public List<DataDomainVo> listDomain(DataDomainBo bo) {
        Wrapper<DataDomain> wrapper = buildQueryWrapper(bo);
        return baseMapper.selectVoList(wrapper);
    }

    private Wrapper<DataDomain> buildQueryWrapper(DataDomainBo bo) {
        LambdaQueryWrapper<DataDomain> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(bo.getDomainName()), DataDomain::getDomainName, bo.getDomainName())
            .like(StringUtils.isNotBlank(bo.getDomainCode()), DataDomain::getDomainCode, bo.getDomainCode())
            .eq(ObjectUtil.isNotNull(bo.getDeptId()), DataDomain::getDeptId, bo.getDeptId())
            .eq(StringUtils.isNotBlank(bo.getStatus()), DataDomain::getStatus, bo.getStatus())
            .orderByAsc(DataDomain::getDomainName);
        return wrapper;
    }
}
