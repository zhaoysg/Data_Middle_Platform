package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
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
import org.dromara.metadata.engine.executor.AbstractRuleExecutor;
import org.dromara.metadata.engine.executor.CustomSqlExecutor;
import org.dromara.metadata.engine.executor.CustomSqlSecuritySupport;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 数据质量执行服务实现 - 核心DQC执行引擎
 *
 * <p>安全修复:
 * <ul>
 *   <li>S4: 内存锁替换为 Redis 分布式锁（进程重启不丢失，支持集群）</li>
 *   <li>S3: 真正的查询中断 — stopExecution 设置取消标志位，规则执行循环周期性检查</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DqcExecutionServiceImpl implements IDqcExecutionService {

    private final DqcExecutionMapper executionMapper;
    private final DqcExecutionDetailMapper detailMapper;
    private final DqcPlanMapper planMapper;
    private final DqcRuleDefMapper ruleDefMapper;
    private final DqcPlanRuleMapper planRuleMapper;
    private final DatasourceHelper datasourceHelper;
    private final RuleExecutorFactory executorFactory;
    private final IDqcQualityScoreService qualityScoreService;

    /** Redis 锁前缀：dqcexec:lock:{planId} */
    private static final String LOCK_KEY_PREFIX = "dqcexec:lock:";
    /** Redis 取消标志前缀：dqcexec:cancel:{executionId} */
    private static final String CANCEL_KEY_PREFIX = "dqcexec:cancel:";
    /** 锁等待超时（秒） */
    private static final long LOCK_WAIT_SECONDS = 0;
    /** 锁持有超时（秒），防止死锁。设置为10分钟，足够覆盖大多数DQC规则执行时间。 */
    private static final long LOCK_LEASE_SECONDS = 600;

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
        RLock lock = getLock(planId);
        if (!tryLock(lock)) {
            throw new ServiceException("该方案正在执行中，请勿重复触发");
        }

        DqcExecution execution = null;
        try {
            return doExecutePlan(planId, triggerType, triggerUser);
        } finally {
            unlock(lock);
        }
    }

    /**
     * 实际执行方案（加锁后调用）
     */
    private DqcExecution doExecutePlan(Long planId, String triggerType, Long triggerUser) {
        DqcPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new ServiceException("方案不存在: " + planId);
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

        // 设置取消标志位（用于 stopExecution 停止）
        String cancelKey = CANCEL_KEY_PREFIX + execution.getId();
        RedisUtils.setCacheObject(cancelKey, "0", Duration.ofHours(1));

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
        Long execId = execution.getId();

        for (DqcRuleDef rule : rules) {
            // 每次规则执行前检查是否被取消
            if (isCancelled(execId)) {
                log.info("执行已被取消: executionId={}, ruleId={}", execId, rule.getId());
                break;
            }

            DqcExecutionDetail detail = createExecutionDetail(execution, rule);

            try {
                DataSourceAdapter adapter = datasourceHelper.getAdapter(rule.getTargetDsId());
                RuleExecutor executor = executorFactory.getExecutor(rule.getRuleType());
                if (executor == null) {
                    log.warn("未找到规则类型的执行器: ruleType={}, ruleId={}", rule.getRuleType(), rule.getId());
                    detail.setPassFlag("0");
                    detail.setErrorLevel(rule.getErrorLevel());
                    detail.setErrorMsg("未找到规则类型的执行器: " + rule.getRuleType());
                    failed++;
                } else {
                    // 重置取消计数器并执行（传入取消检查器）
                    if (executor instanceof AbstractRuleExecutor abstractExecutor) {
                        abstractExecutor.resetCancelCounter();
                    }
                    executor.execute(rule, detail, adapter, () -> isCancelled(execId));
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

        // 清理取消标志位
        RedisUtils.deleteObject(cancelKey);

        BigDecimal score = rules.isEmpty() ? BigDecimal.valueOf(100)
            : BigDecimal.valueOf(passed * 100.0 / rules.size());

        String finalStatus;
        if (isCancelled(execution.getId())) {
            finalStatus = "STOPPED";
        } else if (failed == 0) {
            finalStatus = "SUCCESS";
        } else if (passed == 0) {
            finalStatus = "FAILED";
        } else {
            finalStatus = "PARTIAL";
        }

        execution.setPassedCount(passed);
        execution.setFailedCount(failed);
        execution.setBlockedCount(blocked);
        execution.setOverallScore(score);
        execution.setEndTime(LocalDateTime.now());
        execution.setElapsedMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
        execution.setStatus(finalStatus);
        executionMapper.updateById(execution);

        plan.setLastExecutionId(execution.getId());
        plan.setLastScore(score);
        plan.setLastExecutionTime(execution.getEndTime());
        planMapper.updateById(plan);

        try {
            qualityScoreService.calculateAndSaveScore(execution.getId());
        } catch (Exception e) {
            log.error("计算质量评分失败: executionId={}, error={}", execution.getId(), e.getMessage());
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
        List<DqcExecutionDetailVo> details = detailMapper.selectVoList(
            Wrappers.<DqcExecutionDetail>lambdaQuery()
                .eq(DqcExecutionDetail::getExecutionId, executionId)
                .orderByAsc(DqcExecutionDetail::getId)
        );
        sanitizeCustomSqlDetails(details);
        return details;
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
     * 获取 Redis 分布式锁
     */
    private RLock getLock(Long planId) {
        return RedisUtils.getClient().getLock(LOCK_KEY_PREFIX + planId);
    }

    /**
     * 尝试获取分布式锁
     */
    private boolean tryLock(RLock lock) {
        try {
            return lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("获取执行锁被中断");
        }
    }

    /**
     * 释放分布式锁
     */
    private void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 检查执行是否已被取消
     */
    private boolean isCancelled(Long executionId) {
        String cancelKey = CANCEL_KEY_PREFIX + executionId;
        String value = RedisUtils.getCacheObject(cancelKey);
        return "1".equals(value);
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

        // 设置取消标志位，doExecutePlan 的规则循环会周期性检查此标志
        String cancelKey = CANCEL_KEY_PREFIX + executionId;
        RedisUtils.setCacheObject(cancelKey, "1", Duration.ofHours(1));

        // 标记执行为停止状态
        execution.setStatus("STOPPED");
        execution.setEndTime(LocalDateTime.now());
        execution.setElapsedMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
        executionMapper.updateById(execution);

        log.info("执行已标记停止，等待规则循环退出: executionId={}", executionId);
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

        RLock lock = getLock(oldExecution.getPlanId());
        if (!tryLock(lock)) {
            throw new ServiceException("该方案正在执行中，请勿重复触发");
        }

        try {
            return doExecutePlan(oldExecution.getPlanId(), "MANUAL", triggerUser);
        } finally {
            unlock(lock);
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
        } else if ("STOPPED".equals(vo.getStatus())) {
            vo.setStatusText("已停止");
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

    private void sanitizeCustomSqlDetails(List<DqcExecutionDetailVo> details) {
        for (DqcExecutionDetailVo detail : details) {
            if (!CustomSqlExecutor.TYPE.equalsIgnoreCase(detail.getRuleType())) {
                continue;
            }
            detail.setExecuteSql(CustomSqlSecuritySupport.REDACTED_SQL);
            detail.setActualValue(null);
            if (StringUtils.isNotBlank(detail.getErrorMsg())) {
                detail.setErrorMsg(CustomSqlSecuritySupport.EXECUTION_ERROR);
            }
        }
    }
}
