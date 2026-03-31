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
import org.dromara.metadata.domain.DqcPlanRule;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.bo.DqcRuleDefBo;
import org.dromara.metadata.domain.vo.DqcRuleDefVo;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.mapper.DqcRuleDefMapper;
import org.dromara.metadata.service.IDqcRuleDefService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据质量规则定义服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DqcRuleDefServiceImpl implements IDqcRuleDefService {

    private final DqcRuleDefMapper baseMapper;
    private final DqcPlanRuleMapper planRuleMapper;

    @Override
    public TableDataInfo<DqcRuleDefVo> pageRuleList(DqcRuleDefBo bo, PageQuery pageQuery) {
        Wrapper<DqcRuleDef> wrapper = buildQueryWrapper(bo);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public DqcRuleDefVo getRuleById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public Long insertRule(DqcRuleDefBo bo) {
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
        DqcRuleDef entity = MapstructUtils.convert(bo, DqcRuleDef.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteRule(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }

    @Override
    public List<DqcRuleDefVo> listByPlanId(Long planId) {
        // 查询方案-规则关联
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

        return baseMapper.selectVoList(
            Wrappers.<DqcRuleDef>lambdaQuery()
                .in(DqcRuleDef::getId, ruleIds)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindRules(Long planId, List<Long> ruleIds) {
        // 先删除旧关联
        planRuleMapper.delete(
            Wrappers.<DqcPlanRule>lambdaQuery()
                .eq(DqcPlanRule::getPlanId, planId)
        );

        // 新增新关联
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
        return baseMapper.selectVoList(wrapper);
    }

    private Wrapper<DqcRuleDef> buildQueryWrapper(DqcRuleDefBo bo) {
        LambdaQueryWrapper<DqcRuleDef> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(bo.getRuleName()), DqcRuleDef::getRuleName, bo.getRuleName())
            .like(StringUtils.isNotBlank(bo.getRuleCode()), DqcRuleDef::getRuleCode, bo.getRuleCode())
            .eq(ObjectUtil.isNotNull(bo.getTemplateId()), DqcRuleDef::getTemplateId, bo.getTemplateId())
            .eq(StringUtils.isNotBlank(bo.getRuleType()), DqcRuleDef::getRuleType, bo.getRuleType())
            .eq(StringUtils.isNotBlank(bo.getDimensions()), DqcRuleDef::getDimensions, bo.getDimensions())
            .eq(StringUtils.isNotBlank(bo.getEnabled()), DqcRuleDef::getEnabled, bo.getEnabled())
            .orderByDesc(DqcRuleDef::getCreateTime);
        return wrapper;
    }

    // ==================== RuoYi 标准方法实现 ====================

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
}
