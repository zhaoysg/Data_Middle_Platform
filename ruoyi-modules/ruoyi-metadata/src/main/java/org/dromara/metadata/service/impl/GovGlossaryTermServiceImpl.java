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
import org.dromara.metadata.domain.GovGlossaryTerm;
import org.dromara.metadata.domain.bo.GovGlossaryTermBo;
import org.dromara.metadata.domain.vo.GovGlossaryTermVo;
import org.dromara.metadata.mapper.GovGlossaryTermMapper;
import org.dromara.metadata.service.IGovGlossaryTermService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 治理 Glossary 术语服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GovGlossaryTermServiceImpl implements IGovGlossaryTermService {

    private final GovGlossaryTermMapper baseMapper;

    @Override
    public TableDataInfo<GovGlossaryTermVo> queryPageList(GovGlossaryTermBo bo, PageQuery pageQuery) {
        Wrapper<GovGlossaryTerm> wrapper = buildQueryWrapper(bo);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public GovGlossaryTermVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public Long insertByBo(GovGlossaryTermBo bo) {
        GovGlossaryTerm entity = MapstructUtils.convert(bo, GovGlossaryTerm.class);
        if (entity.getStatus() == null) {
            entity.setStatus("DRAFT");
        }
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateByBo(GovGlossaryTermBo bo) {
        GovGlossaryTerm entity = MapstructUtils.convert(bo, GovGlossaryTerm.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public int publish(Long id) {
        GovGlossaryTerm term = baseMapper.selectById(id);
        if (term == null) {
            throw new ServiceException("术语不存在: " + id);
        }
        term.setStatus("PUBLISHED");
        return baseMapper.updateById(term);
    }

    @Override
    public List<GovGlossaryTermVo> listByKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return List.of();
        }
        return baseMapper.selectVoList(
            Wrappers.<GovGlossaryTerm>lambdaQuery()
                .like(GovGlossaryTerm::getTermName, keyword)
                .or()
                .like(GovGlossaryTerm::getTermAlias, keyword)
                .or()
                .like(GovGlossaryTerm::getTermDesc, keyword)
                .eq(GovGlossaryTerm::getStatus, "PUBLISHED")
                .orderByDesc(GovGlossaryTerm::getCreateTime)
        );
    }

    @Override
    public List<GovGlossaryTermVo> listTerm(GovGlossaryTermBo bo) {
        Wrapper<GovGlossaryTerm> wrapper = buildQueryWrapper(bo);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public TableDataInfo<GovGlossaryTermVo> pageTermList(GovGlossaryTermBo bo, PageQuery pageQuery) {
        return queryPageList(bo, pageQuery);
    }

    @Override
    public GovGlossaryTermVo getTermById(Long id) {
        return queryById(id);
    }

    @Override
    public Long insertTerm(GovGlossaryTermBo bo) {
        return insertByBo(bo);
    }

    @Override
    public int updateTerm(GovGlossaryTermBo bo) {
        return updateByBo(bo);
    }

    @Override
    public int deleteTerm(Long[] ids) {
        return deleteByIds(List.of(ids));
    }

    private Wrapper<GovGlossaryTerm> buildQueryWrapper(GovGlossaryTermBo bo) {
        LambdaQueryWrapper<GovGlossaryTerm> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(bo.getTermName()), GovGlossaryTerm::getTermName, bo.getTermName())
            .and(StringUtils.isNotBlank(bo.getKeyword()), w -> w
                .like(GovGlossaryTerm::getTermName, bo.getKeyword())
                .or()
                .like(GovGlossaryTerm::getTermAlias, bo.getKeyword())
                .or()
                .like(GovGlossaryTerm::getTermDesc, bo.getKeyword()))
            .eq(ObjectUtil.isNotNull(bo.getCategoryId()), GovGlossaryTerm::getCategoryId, bo.getCategoryId())
            .eq(StringUtils.isNotBlank(bo.getStatus()), GovGlossaryTerm::getStatus, bo.getStatus())
            .orderByDesc(GovGlossaryTerm::getCreateTime);
        return wrapper;
    }
}
