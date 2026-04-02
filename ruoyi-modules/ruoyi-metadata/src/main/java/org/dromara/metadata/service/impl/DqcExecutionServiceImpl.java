package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DqcExecution;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcPlan;
import org.dromara.metadata.domain.DqcPlanRule;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.vo.DqcExecutionDetailVo;
import org.dromara.metadata.domain.vo.DqcExecutionVo;
import org.dromara.metadata.engine.executor.RuleExecutor;
import org.dromara.metadata.engine.executor.RuleExecutorFactory;
import org.dromara.metadata.mapper.DqcExecutionDetailMapper;
import org.dromara.metadata.mapper.DqcExecutionMapper;
import org.dromara.metadata.mapper.DqcPlanMapper;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.mapper.DqcRuleDefMapper;
import org.dromara.metadata.service.IDqcExecutionService;
import org.dromara.metadata.service.IDqcQualityScoreService;
import org.dromara.metadata.support.DatasourceHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 数据质量执行服务实现 - 核心DQC执行引擎
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class DqcExecutionServiceImpl implements IDqcExecutionService {

    private final DqcExecutionMapper executionMapper;
    private final DqcExecutionDetailMapper detailMapper;
    private final DqcPlanMapper planMapper;
    private final DqcRuleDefMapper ruleDefMapper;
    private final DqcPlanRuleMapper planRuleMapper;
    private final DatasourceHelper datasourceHelper;
    private final RuleExecutorFactory executorFactory;
    private final IDqcQualityScoreService qualityScoreService;

    /** 方案级别执行锁，防止同一方案重复执行 */
    private final Map<Long, ReentrantLock> planLocks = new ConcurrentHashMap<>();

    @Override
    public TableDataInfo<DqcExecutionVo> queryPageList(DqcExecutionVo vo, PageQuery pageQuery) {
        Wrapper<DqcExecution> wrapper = buildQueryWrapper(vo);
        var page = executionMapper.selectVoPage(pageQuery.build(), wrapper);
        for (DqcExecutionVo item : page.getRecords()) {
            formatVo(item);
        }
        return TableDataInfo.build(page);
    }

    @Override
    public DqcExecutionVo queryById(Long id) {
        DqcExecutionVo vo = executionMapper.selectVoById(id);
        if (vo != null) {
            formatVo(vo);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DqcExecution executePlan(Long planId, String triggerType, Long triggerUser) {
        // 方案级别加锁，防止重复执行
        if (!tryLock(planId)) {
            throw new ServiceException("该方案正在执行中，请勿重复触发");
        }

        DqcExecution execution = null;
        try {
            return doExecutePlan(planId, triggerType, triggerUser);
        } finally {
            unlock(planId);
        }
    }

    /**
     * 实际执行方案（加锁后调用）
     */
    private DqcExecution doExecutePlan(Long planId, String triggerType, Long triggerUser) {
        DqcPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("方案不存在: " + planId);
        }

        // 创建执行记录
        DqcExecution execution = new DqcExecution();
        execution.setExecutionNo("DQC-" + System.currentTimeMillis());
        execution.setPlanId(planId);
        execution.setPlanName(plan.getPlanName());
        execution.setLayerCode(plan.getLayerCode());
        execution.setTriggerType(triggerType);
        execution.setTriggerUser(triggerUser);
        execution.setStartTime(LocalDateTime.now());
        execution.setStatus("RUNNING");
        executionMapper.insert(execution);

        // 查询绑定规则
        var planRules = planRuleMapper.selectList(
            Wrappers.<DqcPlanRule>lambdaQuery()
                .eq(DqcPlanRule::getPlanId, planId)
                .orderByAsc(DqcPlanRule::getSortOrder)
        );

        List<Long> ruleIds = planRules.stream().map(DqcPlanRule::getRuleId).toList();

        if (ruleIds.isEmpty()) {
            execution.setStatus("SUCCESS");
            execution.setTotalRules(0);
            execution.setPassedCount(0);
            execution.setFailedCount(0);
            execution.setBlockedCount(0);
            execution.setOverallScore(BigDecimal.valueOf(100));
            execution.setEndTime(LocalDateTime.now());
            execution.setElapsedMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
            executionMapper.updateById(execution);
            return execution;
        }

        List<DqcRuleDef> rules = ruleDefMapper.selectBatchIds(ruleIds);
        execution.setTotalRules(rules.size());

        int passed = 0, failed = 0, blocked = 0;

        for (DqcRuleDef rule : rules) {
            DqcExecutionDetail detail = createExecutionDetail(execution, rule);

            try {
                // 获取数据源适配器
                DataSourceAdapter adapter = datasourceHelper.getAdapter(rule.getTargetDsId());

                // 使用执行器工厂获取执行器
                RuleExecutor executor = executorFactory.getExecutor(rule.getRuleType());
                if (executor == null) {
                    log.warn("未找到规则类型的执行器: ruleType={}, ruleId={}", rule.getRuleType(), rule.getId());
                    detail.setPassFlag("0");
                    detail.setErrorLevel(rule.getErrorLevel());
                    detail.setErrorMsg("未找到规则类型的执行器: " + rule.getRuleType());
                    failed++;
                } else {
                    // 执行规则检查
                    executor.execute(rule, detail, adapter);

                    if ("1".equals(detail.getPassFlag())) {
                        passed++;
                    } else {
                        failed++;
                        if ("HIGH".equals(detail.getErrorLevel()) || "CRITICAL".equals(detail.getErrorLevel())) {
                            blocked++;
                        }
                    }
                }

            } catch (Exception e) {
                log.error("规则 {} 执行失败: {}", rule.getRuleName(), e.getMessage());
                detail.setPassFlag("0");
                detail.setErrorMsg(e.getMessage());
                detail.setErrorLevel(rule.getErrorLevel());
                if ("HIGH".equals(rule.getErrorLevel()) || "CRITICAL".equals(rule.getErrorLevel())) {
                    blocked++;
                }
                failed++;
            }

            detailMapper.insert(detail);
        }

        // 计算总分
        BigDecimal score = rules.isEmpty() ? BigDecimal.valueOf(100)
            : BigDecimal.valueOf(passed * 100.0 / rules.size());

        // 确定最终状态
        String finalStatus;
        if (failed == 0) {
            finalStatus = "SUCCESS";
        } else if (passed == 0) {
            finalStatus = "FAILED";
        } else {
            finalStatus = "PARTIAL";
        }

        // 更新执行记录
        execution.setPassedCount(passed);
        execution.setFailedCount(failed);
        execution.setBlockedCount(blocked);
        execution.setOverallScore(score);
        execution.setEndTime(LocalDateTime.now());
        execution.setElapsedMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
        execution.setStatus(finalStatus);
        executionMapper.updateById(execution);

        // 更新方案信息
        plan.setLastExecutionId(execution.getId());
        plan.setLastScore(score);
        plan.setLastExecutionTime(execution.getEndTime());
        planMapper.updateById(plan);

        // 计算并保存质量评分
        try {
            qualityScoreService.calculateAndSaveScore(execution.getId());
        } catch (Exception e) {
            log.error("计算质量评分失败: executionId={}, error={}", execution.getId(), e.getMessage());
            // 评分计算失败不影响执行结果
        }

        return execution;
    }

    /**
     * 创建执行明细
     */
    private DqcExecutionDetail createExecutionDetail(DqcExecution execution, DqcRuleDef rule) {
        DqcExecutionDetail detail = new DqcExecutionDetail();
        detail.setExecutionId(execution.getId());
        detail.setRuleId(rule.getId());
        detail.setRuleName(rule.getRuleName());
        detail.setRuleCode(rule.getRuleCode());
        detail.setRuleType(rule.getRuleType());
        detail.setDimension(rule.getDimensions());
        detail.setTargetDsId(rule.getTargetDsId());
        detail.setTargetTable(rule.getTargetTable());
        detail.setTargetColumn(rule.getTargetColumn());
        detail.setExecuteTime(LocalDateTime.now());
        return detail;
    }

    @Override
    public List<DqcExecutionDetailVo> listDetailsByExecutionId(Long executionId) {
        return detailMapper.selectVoList(
            Wrappers.<DqcExecutionDetail>lambdaQuery()
                .eq(DqcExecutionDetail::getExecutionId, executionId)
                .orderByAsc(DqcExecutionDetail::getId)
        );
    }

    @Override
    public List<DqcExecutionVo> listByPlanId(Long planId) {
        return executionMapper.selectVoList(
            Wrappers.<DqcExecution>lambdaQuery()
                .eq(DqcExecution::getPlanId, planId)
                .orderByDesc(DqcExecution::getCreateTime)
        );
    }

    /**
     * 尝试获取方案执行锁
     */
    private boolean tryLock(Long planId) {
        return planLocks.computeIfAbsent(planId, k -> new ReentrantLock()).tryLock();
    }

    /**
     * 释放方案执行锁
     */
    private void unlock(Long planId) {
        ReentrantLock lock = planLocks.get(planId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public void stopExecution(Long executionId) {
        DqcExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new ServiceException("执行记录不存在");
        }

        if (!"RUNNING".equals(execution.getStatus())) {
            throw new ServiceException("只有运行中的执行才能停止");
        }

        // 标记执行为停止状态
        execution.setStatus("STOPPED");
        execution.setEndTime(LocalDateTime.now());
        execution.setElapsedMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
        executionMapper.updateById(execution);

        log.info("执行记录已停止: executionId={}", executionId);
    }

    @Override
    public DqcExecution rerunExecution(Long executionId, Long triggerUser) {
        DqcExecution oldExecution = executionMapper.selectById(executionId);
        if (oldExecution == null) {
            throw new ServiceException("执行记录不存在");
        }

        if (oldExecution.getPlanId() == null) {
            throw new ServiceException("无法重新执行：原执行记录没有关联方案");
        }

        // 方案级别加锁
        if (!tryLock(oldExecution.getPlanId())) {
            throw new ServiceException("该方案正在执行中，请勿重复触发");
        }

        try {
            return doExecutePlan(oldExecution.getPlanId(), "MANUAL", triggerUser);
        } finally {
            unlock(oldExecution.getPlanId());
        }
    }

    /**
     * 格式化执行记录VO
     */
    private void formatVo(DqcExecutionVo vo) {
        if (vo == null) return;
        if ("RUNNING".equals(vo.getStatus())) {
            vo.setStatusText("运行中");
        } else if ("SUCCESS".equals(vo.getStatus())) {
            vo.setStatusText("成功");
        } else if ("FAILED".equals(vo.getStatus())) {
            vo.setStatusText("失败");
        } else if ("PARTIAL".equals(vo.getStatus())) {
            vo.setStatusText("部分成功");
        }
    }

    /**
     * 构建查询条件
     */
    private Wrapper<DqcExecution> buildQueryWrapper(DqcExecutionVo vo) {
        LambdaQueryWrapper<DqcExecution> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ObjectUtil.isNotNull(vo.getPlanId()), DqcExecution::getPlanId, vo.getPlanId())
            .like(StringUtils.isNotBlank(vo.getPlanName()), DqcExecution::getPlanName, vo.getPlanName())
            .eq(StringUtils.isNotBlank(vo.getStatus()), DqcExecution::getStatus, vo.getStatus())
            .eq(StringUtils.isNotBlank(vo.getTriggerType()), DqcExecution::getTriggerType, vo.getTriggerType())
            .orderByDesc(DqcExecution::getCreateTime);
        return wrapper;
    }
}
