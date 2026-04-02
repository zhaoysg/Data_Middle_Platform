package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.domain.vo.SysDatasourceVo;
import org.dromara.datasource.service.ISysDatasourceService;
import org.dromara.metadata.domain.DqcPlanRule;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.DqcRuleTemplate;
import org.dromara.metadata.domain.bo.DqcRuleDefBo;
import org.dromara.metadata.domain.vo.DqcRuleDefVo;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.mapper.DqcRuleDefMapper;
import org.dromara.metadata.mapper.DqcRuleTemplateMapper;
import org.dromara.metadata.service.IDqcRuleDefService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据质量规则定义服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class DqcRuleDefServiceImpl implements IDqcRuleDefService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DqcRuleDefMapper baseMapper;
    private final DqcPlanRuleMapper planRuleMapper;
    private final DqcRuleTemplateMapper templateMapper;
    private final ISysDatasourceService datasourceService;

    @Override
    public TableDataInfo<DqcRuleDefVo> pageRuleList(DqcRuleDefBo bo, PageQuery pageQuery) {
        Wrapper<DqcRuleDef> wrapper = buildQueryWrapper(bo);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        enrichRuleVos(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public DqcRuleDefVo getRuleById(Long id) {
        DqcRuleDefVo vo = baseMapper.selectVoById(id);
        if (vo != null) {
            enrichRuleVos(List.of(vo));
        }
        return vo;
    }

    @Override
    public Long insertRule(DqcRuleDefBo bo) {
        normalizeRuleBo(bo, null);
        DqcRuleDef entity = MapstructUtils.convert(bo, DqcRuleDef.class);
        if (entity.getEnabled() == null) {
            entity.setEnabled("1");
        }
        if (entity.getErrorLevel() == null) {
            entity.setErrorLevel("MEDIUM");
        }
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateRule(DqcRuleDefBo bo) {
        DqcRuleDef existing = bo.getId() == null ? null : baseMapper.selectById(bo.getId());
        normalizeRuleBo(bo, existing);
        DqcRuleDef entity = MapstructUtils.convert(bo, DqcRuleDef.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteRule(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }

    @Override
    public List<DqcRuleDefVo> listByPlanId(Long planId) {
        List<DqcPlanRule> planRules = planRuleMapper.selectList(
            Wrappers.<DqcPlanRule>lambdaQuery()
                .eq(DqcPlanRule::getPlanId, planId)
                .orderByAsc(DqcPlanRule::getSortOrder)
        );

        if (planRules.isEmpty()) {
            return List.of();
        }

        List<Long> ruleIds = planRules.stream()
            .map(DqcPlanRule::getRuleId)
            .toList();

        List<DqcRuleDefVo> list = baseMapper.selectVoList(
            Wrappers.<DqcRuleDef>lambdaQuery()
                .in(DqcRuleDef::getId, ruleIds)
        );
        enrichRuleVos(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindRules(Long planId, List<Long> ruleIds) {
        planRuleMapper.delete(
            Wrappers.<DqcPlanRule>lambdaQuery()
                .eq(DqcPlanRule::getPlanId, planId)
        );

        int sortOrder = 1;
        for (Long ruleId : ruleIds) {
            DqcPlanRule planRule = new DqcPlanRule();
            planRule.setPlanId(planId);
            planRule.setRuleId(ruleId);
            planRule.setSortOrder(sortOrder++);
            planRuleMapper.insert(planRule);
        }

        return ruleIds.size();
    }

    @Override
    public List<DqcRuleDefVo> listRule(DqcRuleDefBo bo) {
        Wrapper<DqcRuleDef> wrapper = buildQueryWrapper(bo);
        List<DqcRuleDefVo> list = baseMapper.selectVoList(wrapper);
        enrichRuleVos(list);
        return list;
    }

    private Wrapper<DqcRuleDef> buildQueryWrapper(DqcRuleDefBo bo) {
        LambdaQueryWrapper<DqcRuleDef> wrapper = Wrappers.lambdaQuery();
        String dimension = StringUtils.isNotBlank(bo.getDimensions()) ? bo.getDimensions() : bo.getDimension();
        wrapper.like(StringUtils.isNotBlank(bo.getRuleName()), DqcRuleDef::getRuleName, bo.getRuleName())
            .like(StringUtils.isNotBlank(bo.getRuleCode()), DqcRuleDef::getRuleCode, bo.getRuleCode())
            .eq(ObjectUtil.isNotNull(bo.getTemplateId()), DqcRuleDef::getTemplateId, bo.getTemplateId())
            .eq(StringUtils.isNotBlank(bo.getRuleType()), DqcRuleDef::getRuleType, bo.getRuleType())
            .eq(StringUtils.isNotBlank(dimension), DqcRuleDef::getDimensions, dimension)
            .eq(StringUtils.isNotBlank(bo.getEnabled()), DqcRuleDef::getEnabled, bo.getEnabled())
            .orderByDesc(DqcRuleDef::getCreateTime);
        return wrapper;
    }

    @Override
    public TableDataInfo<DqcRuleDefVo> queryPageList(DqcRuleDefBo bo, PageQuery pageQuery) {
        return pageRuleList(bo, pageQuery);
    }

    @Override
    public DqcRuleDefVo queryById(Long id) {
        return getRuleById(id);
    }

    @Override
    public Long insertByBo(DqcRuleDefBo bo) {
        return insertRule(bo);
    }

    @Override
    public int updateByBo(DqcRuleDefBo bo) {
        return updateRule(bo);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return deleteRule(ids.toArray(new Long[0]));
    }

    private void normalizeRuleBo(DqcRuleDefBo bo, DqcRuleDef existing) {
        if (bo == null) {
            return;
        }
        if (StringUtils.isBlank(bo.getDimensions()) && StringUtils.isNotBlank(bo.getDimension())) {
            bo.setDimensions(bo.getDimension());
        }
        applyTemplateDefaults(bo, existing);
        applyRuleParams(bo);
        if (StringUtils.isBlank(bo.getEnabled()) && existing == null) {
            bo.setEnabled("1");
        }
        if (StringUtils.isBlank(bo.getErrorLevel()) && existing == null) {
            bo.setErrorLevel("MEDIUM");
        }
    }

    private void applyTemplateDefaults(DqcRuleDefBo bo, DqcRuleDef existing) {
        Long templateId = bo.getTemplateId();
        if (templateId == null && existing != null) {
            templateId = existing.getTemplateId();
        }
        if (templateId == null) {
            return;
        }

        DqcRuleTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            return;
        }

        if (StringUtils.isBlank(bo.getRuleType())) {
            bo.setRuleType(existing != null && StringUtils.isNotBlank(existing.getRuleType())
                ? existing.getRuleType()
                : template.getRuleType());
        }
        if (StringUtils.isBlank(bo.getApplyLevel())) {
            bo.setApplyLevel(existing != null && StringUtils.isNotBlank(existing.getApplyLevel())
                ? existing.getApplyLevel()
                : template.getApplyLevel());
        }
        if (StringUtils.isBlank(bo.getDimensions())) {
            bo.setDimensions(existing != null && StringUtils.isNotBlank(existing.getDimensions())
                ? existing.getDimensions()
                : template.getDimension());
        }
        if (StringUtils.isBlank(bo.getRuleExpr()) && existing == null) {
            bo.setRuleExpr(template.getDefaultExpr());
        }
        if (existing == null && StringUtils.isNotBlank(template.getThresholdJson())) {
            mergeThresholdJson(bo, template.getThresholdJson());
        }
    }

    private void applyRuleParams(DqcRuleDefBo bo) {
        if (StringUtils.isBlank(bo.getRuleParams())) {
            return;
        }
        try {
            Map<String, Object> ruleParams = OBJECT_MAPPER.readValue(
                bo.getRuleParams(),
                new TypeReference<Map<String, Object>>() {}
            );
            mergeThresholdMap(bo, ruleParams);
            if (StringUtils.isBlank(bo.getCompareTable()) && ruleParams.get("compareTable") != null) {
                bo.setCompareTable(String.valueOf(ruleParams.get("compareTable")));
            }
            if (StringUtils.isBlank(bo.getCompareColumn()) && ruleParams.get("compareColumn") != null) {
                bo.setCompareColumn(String.valueOf(ruleParams.get("compareColumn")));
            }
            if (StringUtils.isBlank(bo.getRuleExpr()) && ruleParams.get("ruleExpr") != null) {
                bo.setRuleExpr(String.valueOf(ruleParams.get("ruleExpr")));
            }
        } catch (IOException e) {
            log.warn("解析规则参数失败，忽略旧版 ruleParams: {}", e.getMessage());
        }
    }

    private void mergeThresholdJson(DqcRuleDefBo bo, String rawJson) {
        try {
            Map<String, Object> thresholdMap = OBJECT_MAPPER.readValue(
                rawJson,
                new TypeReference<Map<String, Object>>() {}
            );
            mergeThresholdMap(bo, thresholdMap);
        } catch (IOException e) {
            log.warn("解析模板阈值JSON失败，thresholdJson={}", rawJson);
        }
    }

    private void mergeThresholdMap(DqcRuleDefBo bo, Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        if (bo.getThresholdMin() == null) {
            bo.setThresholdMin(readDecimal(values, "thresholdMin", "threshold_min", "minValue", "min_value", "minThreshold", "min_threshold"));
        }
        if (bo.getThresholdMax() == null) {
            bo.setThresholdMax(readDecimal(values, "thresholdMax", "threshold_max", "maxValue", "max_value", "maxThreshold", "max_threshold"));
        }
        if (bo.getFluctuationThreshold() == null) {
            bo.setFluctuationThreshold(readDecimal(values, "fluctuationThreshold", "fluctuation_threshold", "thresholdPct", "threshold_pct", "maxFluctuation", "max_fluctuation"));
        }
        if (StringUtils.isBlank(bo.getRegexPattern())) {
            Object pattern = values.get("pattern");
            if (pattern == null) {
                pattern = values.get("regex");
            }
            if (pattern != null) {
                bo.setRegexPattern(String.valueOf(pattern));
            }
        }
    }

    private BigDecimal readDecimal(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                try {
                    return new BigDecimal(String.valueOf(value));
                } catch (NumberFormatException ignored) {
                    log.warn("忽略无法解析的数字参数 {}={}", key, value);
                }
            }
        }
        return null;
    }

    private void enrichRuleVos(List<DqcRuleDefVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        Set<Long> templateIds = records.stream()
            .map(DqcRuleDefVo::getTemplateId)
            .filter(ObjectUtil::isNotNull)
            .collect(Collectors.toSet());
        Map<Long, DqcRuleTemplate> templateMap = templateIds.isEmpty()
            ? Map.of()
            : templateMapper.selectBatchIds(templateIds).stream()
                .collect(Collectors.toMap(DqcRuleTemplate::getId, item -> item));

        Set<Long> dsIds = records.stream()
            .map(DqcRuleDefVo::getTargetDsId)
            .filter(ObjectUtil::isNotNull)
            .collect(Collectors.toSet());
        Map<Long, SysDatasourceVo> datasourceMap = dsIds.isEmpty()
            ? Map.of()
            : datasourceService.listDatasourceByIds(List.copyOf(dsIds)).stream()
                .filter(item -> ObjectUtil.isNotNull(item.getDsId()))
                .collect(Collectors.toMap(SysDatasourceVo::getDsId, item -> item, (left, right) -> left));

        for (DqcRuleDefVo record : records) {
            if (StringUtils.isBlank(record.getDimension())) {
                record.setDimension(record.getDimensions());
            }
            DqcRuleTemplate template = templateMap.get(record.getTemplateId());
            if (template != null) {
                record.setTemplateName(template.getTemplateName());
            }
            SysDatasourceVo datasource = datasourceMap.get(record.getTargetDsId());
            if (datasource != null) {
                record.setTargetDsName(datasource.getDsName());
                record.setDsName(datasource.getDsName());
            }
        }
    }
}
