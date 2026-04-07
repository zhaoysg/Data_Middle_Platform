package org.dromara.metadata.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.DqcRuleTemplate;
import org.dromara.metadata.domain.vo.DqcRuleTemplateVo;
import org.dromara.metadata.mapper.DqcRuleTemplateMapper;
import org.dromara.metadata.service.IDqcRuleTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据质量规则模板服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DqcRuleTemplateServiceImpl implements IDqcRuleTemplateService {

    private final DqcRuleTemplateMapper baseMapper;

    @Override
    public TableDataInfo<DqcRuleTemplateVo> queryPageList(DqcRuleTemplateVo vo, PageQuery pageQuery) {
        var wrapper = Wrappers.<DqcRuleTemplate>lambdaQuery()
            .like(vo != null && vo.getTemplateName() != null && !vo.getTemplateName().isBlank(),
                DqcRuleTemplate::getTemplateName, vo.getTemplateName())
            .eq(vo != null && vo.getDimension() != null && !vo.getDimension().isBlank(),
                DqcRuleTemplate::getDimension, vo.getDimension())
            .orderByDesc(DqcRuleTemplate::getCreateTime);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<DqcRuleTemplateVo> pageTemplateList(DqcRuleTemplateVo vo, PageQuery pageQuery) {
        return queryPageList(vo, pageQuery);
    }

    @Override
    public DqcRuleTemplateVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<DqcRuleTemplateVo> listAll() {
        return baseMapper.selectVoList(
            Wrappers.<DqcRuleTemplate>lambdaQuery()
                .eq(DqcRuleTemplate::getEnabled, "1")
                .orderByAsc(DqcRuleTemplate::getId)
        );
    }

    @Override
    public List<DqcRuleTemplateVo> listByDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return listAll();
        }
        return baseMapper.selectVoList(
            Wrappers.<DqcRuleTemplate>lambdaQuery()
                .eq(DqcRuleTemplate::getDimension, dimension)
                .eq(DqcRuleTemplate::getEnabled, "1")
                .orderByAsc(DqcRuleTemplate::getId)
        );
    }

    @Override
    public DqcRuleTemplateVo getTemplateById(Long id) {
        return queryById(id);
    }

    @Override
    public int deleteTemplate(List<Long> ids) {
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public Long insertTemplate(DqcRuleTemplateVo vo) {
        if (StringUtils.isNotBlank(vo.getTemplateCode())) {
            boolean exist = baseMapper.exists(Wrappers.<DqcRuleTemplate>lambdaQuery()
                .eq(DqcRuleTemplate::getTemplateCode, vo.getTemplateCode()));
            if (exist) {
                throw new ServiceException("模板编码已存在");
            }
        }
        DqcRuleTemplate entity = new DqcRuleTemplate();
        entity.setTemplateCode(vo.getTemplateCode());
        entity.setTemplateName(vo.getTemplateName());
        entity.setTemplateDesc(vo.getTemplateDesc());
        entity.setRuleType(vo.getRuleType());
        entity.setApplyLevel(vo.getApplyLevel());
        entity.setDefaultExpr(vo.getDefaultExpr());
        entity.setThresholdJson(vo.getThresholdJson());
        entity.setParamSpec(vo.getParamSpec());
        entity.setDimension(vo.getDimension());
        entity.setBuiltin(vo.getBuiltin() != null ? vo.getBuiltin() : "0");
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : "0");
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateTemplate(DqcRuleTemplateVo vo) {
        if (vo.getId() == null) {
            throw new ServiceException("模板ID不能为空");
        }
        if (StringUtils.isNotBlank(vo.getTemplateCode())) {
            boolean exist = baseMapper.exists(Wrappers.<DqcRuleTemplate>lambdaQuery()
                .eq(DqcRuleTemplate::getTemplateCode, vo.getTemplateCode())
                .ne(DqcRuleTemplate::getId, vo.getId()));
            if (exist) {
                throw new ServiceException("模板编码已存在");
            }
        }
        DqcRuleTemplate entity = new DqcRuleTemplate();
        entity.setId(vo.getId());
        entity.setTemplateCode(vo.getTemplateCode());
        entity.setTemplateName(vo.getTemplateName());
        entity.setTemplateDesc(vo.getTemplateDesc());
        entity.setRuleType(vo.getRuleType());
        entity.setApplyLevel(vo.getApplyLevel());
        entity.setDefaultExpr(vo.getDefaultExpr());
        entity.setThresholdJson(vo.getThresholdJson());
        entity.setParamSpec(vo.getParamSpec());
        entity.setDimension(vo.getDimension());
        if (vo.getBuiltin() != null) {
            entity.setBuiltin(vo.getBuiltin());
        }
        if (vo.getEnabled() != null) {
            entity.setEnabled(vo.getEnabled());
        }
        return baseMapper.updateById(entity);
    }
}
