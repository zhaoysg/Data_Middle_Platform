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
import org.dromara.metadata.domain.DqcPlan;
import org.dromara.metadata.domain.DqcPlanRule;
import org.dromara.metadata.domain.bo.DqcPlanBo;
import org.dromara.metadata.domain.vo.DqcPlanVo;
import org.dromara.metadata.mapper.DqcPlanMapper;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.service.IDqcPlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据质量检查方案服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DqcPlanServiceImpl implements IDqcPlanService {

    private final DqcPlanMapper baseMapper;
    private final DqcPlanRuleMapper planRuleMapper;

    @Override
    public TableDataInfo<DqcPlanVo> pagePlanList(DqcPlanBo bo, PageQuery pageQuery) {
        Wrapper<DqcPlan> wrapper = buildQueryWrapper(bo);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        // 格式化分数和时间
        for (DqcPlanVo vo : page.getRecords()) {
            formatVo(vo);
        }
        return TableDataInfo.build(page);
    }

    @Override
    public DqcPlanVo getPlanById(Long id) {
        DqcPlanVo vo = baseMapper.selectVoById(id);
        if (vo != null) {
            formatVo(vo);
        }
        return vo;
    }

    @Override
    public Long insertPlan(DqcPlanBo bo) {
        DqcPlan entity = MapstructUtils.convert(bo, DqcPlan.class);
        if (entity.getStatus() == null) {
            entity.setStatus("DRAFT");
        }
        if (entity.getTriggerType() == null) {
            entity.setTriggerType("MANUAL");
        }
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updatePlan(DqcPlanBo bo) {
        DqcPlan entity = MapstructUtils.convert(bo, DqcPlan.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deletePlan(Long[] ids) {
        // 先删除关联的规则
        for (Long id : ids) {
            planRuleMapper.delete(
                Wrappers.<DqcPlanRule>lambdaQuery()
                    .eq(DqcPlanRule::getPlanId, id)
            );
        }
        return baseMapper.deleteBatchIds(List.of(ids));
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

        // 更新方案规则数量
        DqcPlan plan = baseMapper.selectById(planId);
        if (plan != null) {
            plan.setRuleCount(ruleIds.size());
            baseMapper.updateById(plan);
        }

        return ruleIds.size();
    }

    @Override
    public List<DqcPlanVo> listPlan(DqcPlanBo bo) {
        Wrapper<DqcPlan> wrapper = buildQueryWrapper(bo);
        List<DqcPlanVo> list = baseMapper.selectVoList(wrapper);
        for (DqcPlanVo vo : list) {
            formatVo(vo);
        }
        return list;
    }

    @Override
    public List<DqcPlanVo> options() {
        List<DqcPlanVo> list = baseMapper.selectVoList(
            Wrappers.<DqcPlan>lambdaQuery()
                .eq(DqcPlan::getStatus, "PUBLISHED")
                .orderByDesc(DqcPlan::getCreateTime)
        );
        for (DqcPlanVo vo : list) {
            formatVo(vo);
        }
        return list;
    }

    private void formatVo(DqcPlanVo vo) {
        if (vo.getLastScore() != null) {
            vo.setLastScoreStr(vo.getLastScore().setScale(2, java.math.RoundingMode.HALF_UP) + "%");
        }
        if (vo.getLastExecutionTime() != null) {
            vo.setLastExecutionTimeStr(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .format(vo.getLastExecutionTime())
            );
        }
    }

    private Wrapper<DqcPlan> buildQueryWrapper(DqcPlanBo bo) {
        LambdaQueryWrapper<DqcPlan> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(bo.getPlanName()), DqcPlan::getPlanName, bo.getPlanName())
            .like(StringUtils.isNotBlank(bo.getKeyword()), DqcPlan::getPlanName, bo.getKeyword())
            .eq(StringUtils.isNotBlank(bo.getPlanCode()), DqcPlan::getPlanCode, bo.getPlanCode())
            .eq(StringUtils.isNotBlank(bo.getBindType()), DqcPlan::getBindType, bo.getBindType())
            .eq(StringUtils.isNotBlank(bo.getStatus()), DqcPlan::getStatus, bo.getStatus())
            .eq(StringUtils.isNotBlank(bo.getTriggerType()), DqcPlan::getTriggerType, bo.getTriggerType())
            .orderByDesc(DqcPlan::getCreateTime);
        return wrapper;
    }

    // ==================== RuoYi 标准方法实现 ====================

    @Override
    public TableDataInfo<DqcPlanVo> queryPageList(DqcPlanBo bo, PageQuery pageQuery) {
        return pagePlanList(bo, pageQuery);
    }

    @Override
    public DqcPlanVo queryById(Long id) {
        return getPlanById(id);
    }

    @Override
    public Long insertByBo(DqcPlanBo bo) {
        return insertPlan(bo);
    }

    @Override
    public int updateByBo(DqcPlanBo bo) {
        return updatePlan(bo);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return deletePlan(ids.toArray(new Long[0]));
    }

    @Override
    public int publish(Long id) {
        DqcPlan plan = baseMapper.selectById(id);
        if (plan == null) {
            throw new IllegalArgumentException("方案不存在: " + id);
        }
        if (!"DRAFT".equals(plan.getStatus()) && !"UNPUBLISHED".equals(plan.getStatus())) {
            throw new IllegalArgumentException("只能发布草稿状态的方案");
        }
        plan.setStatus("PUBLISHED");
        return baseMapper.updateById(plan);
    }

    @Override
    public List<DqcPlan> queryPublishedPlans() {
        return baseMapper.selectList(
            Wrappers.<DqcPlan>lambdaQuery()
                .eq(DqcPlan::getStatus, "PUBLISHED")
                .orderByDesc(DqcPlan::getCreateTime)
        );
    }
}
