package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.DqcRuleTemplate;
import org.dromara.metadata.domain.bo.DqcRuleTemplateBo;
import org.dromara.metadata.domain.vo.DqcRuleTemplateVo;
import org.dromara.metadata.mapper.DqcRuleTemplateMapper;
import org.dromara.metadata.service.IDqcRuleTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据质量规则模板服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class DqcRuleTemplateServiceImpl implements IDqcRuleTemplateService {

    private final DqcRuleTemplateMapper baseMapper;

    @Override
    public TableDataInfo<DqcRuleTemplateVo> queryPageList(DqcRuleTemplateVo vo, PageQuery pageQuery) {
        var wrapper = Wrappers.<DqcRuleTemplate>lambdaQuery()
            .like(vo != null && vo.getTemplateName() != null && !vo.getTemplateName().isBlank(),
                DqcRuleTemplate::getTemplateName, vo.getTemplateName())
            .eq(vo != null && vo.getDimension() != null && !vo.getDimension().isBlank(),
                DqcRuleTemplate::getDimension, vo.getDimension())
            .eq(vo != null && vo.getRuleType() != null && !vo.getRuleType().isBlank(),
                DqcRuleTemplate::getRuleType, vo.getRuleType())
            .eq(vo != null && vo.getApplyLevel() != null && !vo.getApplyLevel().isBlank(),
                DqcRuleTemplate::getApplyLevel, vo.getApplyLevel())
            .eq(vo != null && vo.getBuiltin() != null && !vo.getBuiltin().isBlank(),
                DqcRuleTemplate::getBuiltin, vo.getBuiltin())
            .eq(vo != null && vo.getEnabled() != null && !vo.getEnabled().isBlank(),
                DqcRuleTemplate::getEnabled, vo.getEnabled())
            .orderByDesc(DqcRuleTemplate::getCreateTime);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public DqcRuleTemplateVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(DqcRuleTemplateBo bo) {
        return insertTemplate(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateByBo(DqcRuleTemplateBo bo) {
        return updateTemplate(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(List<Long> ids) {
        return deleteTemplate(ids);
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
    public TableDataInfo<DqcRuleTemplateVo> pageTemplateList(DqcRuleTemplateVo vo, PageQuery pageQuery) {
        return queryPageList(vo, pageQuery);
    }

    @Override
    public Long insertTemplate(DqcRuleTemplateBo bo) {
        boolean exists = baseMapper.exists(
            Wrappers.<DqcRuleTemplate>lambdaQuery()
                .eq(DqcRuleTemplate::getTemplateCode, bo.getTemplateCode())
        );
        if (exists) {
            throw new ServiceException("模板编码已存在");
        }

        DqcRuleTemplate entity = new DqcRuleTemplate();
        entity.setTemplateCode(bo.getTemplateCode());
        entity.setTemplateName(bo.getTemplateName());
        entity.setTemplateDesc(bo.getTemplateDesc());
        entity.setRuleType(bo.getRuleType());
        entity.setApplyLevel(bo.getApplyLevel());
        entity.setDefaultExpr(bo.getDefaultExpr());
        entity.setThresholdJson(bo.getThresholdJson());
        entity.setParamSpec(bo.getParamSpec());
        entity.setDimension(bo.getDimension());
        entity.setBuiltin(ObjectUtil.defaultIfNull(bo.getBuiltin(), "0"));
        entity.setEnabled(ObjectUtil.defaultIfNull(bo.getEnabled(), "1"));
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateTemplate(DqcRuleTemplateBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("模板ID不能为空");
        }

        DqcRuleTemplate existing = baseMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("模板不存在");
        }

        boolean exists = baseMapper.exists(
            Wrappers.<DqcRuleTemplate>lambdaQuery()
                .eq(DqcRuleTemplate::getTemplateCode, bo.getTemplateCode())
                .ne(DqcRuleTemplate::getId, bo.getId())
        );
        if (exists) {
            throw new ServiceException("模板编码已存在");
        }
        if ("1".equals(existing.getBuiltin()) && !"1".equals(ObjectUtil.defaultIfNull(bo.getBuiltin(), existing.getBuiltin()))) {
            throw new ServiceException("内置模板不允许修改内置状态");
        }

        DqcRuleTemplate entity = new DqcRuleTemplate();
        entity.setId(bo.getId());
        entity.setTemplateCode(bo.getTemplateCode());
        entity.setTemplateName(bo.getTemplateName());
        entity.setTemplateDesc(bo.getTemplateDesc());
        entity.setRuleType(bo.getRuleType());
        entity.setApplyLevel(bo.getApplyLevel());
        entity.setDefaultExpr(bo.getDefaultExpr());
        entity.setThresholdJson(bo.getThresholdJson());
        entity.setParamSpec(bo.getParamSpec());
        entity.setDimension(bo.getDimension());
        entity.setBuiltin(ObjectUtil.defaultIfNull(bo.getBuiltin(), existing.getBuiltin()));
        entity.setEnabled(ObjectUtil.defaultIfNull(bo.getEnabled(), existing.getEnabled()));
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteTemplate(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("删除的ID列表不能为空");
        }
        List<DqcRuleTemplate> templates = baseMapper.selectBatchIds(ids);
        boolean hasBuiltin = templates.stream().anyMatch(item -> "1".equals(item.getBuiltin()));
        if (hasBuiltin) {
            throw new ServiceException("内置模板不允许删除");
        }
        return baseMapper.deleteBatchIds(ids);
    }
}
