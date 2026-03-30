package com.bagdatahouse.dqc.config;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqcPlan;
import com.bagdatahouse.core.entity.DqcExecution;
import com.bagdatahouse.core.entity.DqcPlanRule;
import com.bagdatahouse.core.mapper.DqcPlanMapper;
import com.bagdatahouse.core.mapper.DqcExecutionMapper;
import com.bagdatahouse.core.mapper.DqcPlanRuleMapper;
import com.bagdatahouse.dqc.service.DqcExecutionService;
import com.bagdatahouse.core.vo.ExecutionTriggerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DQC定时任务调度器
 * 负责扫描并执行已发布且配置了定时触发的质检方案
 *
 * 支持三种触发方式：
 * - MANUAL（手动触发）：通过前端手动触发
 * - SCHEDULE（定时触发）：通过定时器自动触发
 * - API（接口触发）：通过REST API触发
 */
@Component
public class DqcScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(DqcScheduledJob.class);

    @Autowired
    private DqcPlanMapper planMapper;

    @Autowired
    private DqcExecutionMapper executionMapper;

    @Autowired
    private DqcPlanRuleMapper planRuleMapper;

    @Autowired
    private DqcExecutionService executionService;

    /** 防止并发重复执行同一个方案 */
    private final ConcurrentHashMap<Long, AtomicBoolean> executionLocks = new ConcurrentHashMap<>();

    /** Cron表达式解析器（支持Quartz格式） */
    private final CronParser quartzCronParser = new CronParser(
            CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    /** 默认保留天数 */
    private static final int DEFAULT_RETENTION_DAYS = 90;

    /**
     * 定时扫描待执行的方案
     * 每5分钟扫描一次，检查是否有方案的定时触发时间到达
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void scanScheduledPlans() {
        log.debug("[定时调度] 开始扫描待执行的定时方案...");
        try {
            // 查询所有已发布且触发类型为定时的方案
            List<DqcPlan> scheduledPlans = getScheduledPlans();
            log.debug("[定时调度] 发现{}个定时方案待检查", scheduledPlans.size());

            int executedCount = 0;
            for (DqcPlan plan : scheduledPlans) {
                try {
                    if (shouldExecute(plan)) {
                        triggerScheduledExecution(plan);
                        executedCount++;
                    }
                } catch (Exception e) {
                    log.error("[定时调度] 执行方案失败: planId={}, planName={}",
                            plan.getId(), plan.getPlanName(), e);
                }
            }

            if (executedCount > 0) {
                log.info("[定时调度] 本次扫描共触发{}个方案执行", executedCount);
            }
        } catch (Exception e) {
            log.error("[定时调度] 扫描定时方案时发生异常", e);
        }
    }

    /**
     * 每日凌晨3点清理历史执行记录
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldRecords() {
        log.info("[定时调度] 开始清理历史执行记录...");
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(DEFAULT_RETENTION_DAYS);

            LambdaQueryWrapper<DqcExecution> wrapper = new LambdaQueryWrapper<>();
            wrapper.le(DqcExecution::getCreateTime, cutoffDate);
            List<DqcExecution> oldExecutions = executionMapper.selectList(wrapper);

            if (!oldExecutions.isEmpty()) {
                int deletedCount = 0;
                for (DqcExecution execution : oldExecutions) {
                    executionMapper.deleteById(execution.getId());
                    deletedCount++;
                }
                log.info("[定时调度] 已清理{}条超过{}天的历史执行记录", deletedCount, DEFAULT_RETENTION_DAYS);
            } else {
                log.debug("[定时调度] 无需清理的历史记录");
            }
        } catch (Exception e) {
            log.error("[定时调度] 清理历史记录时发生异常", e);
        }
    }

    /**
     * 查询所有已发布且配置了定时触发的方案
     */
    private List<DqcPlan> getScheduledPlans() {
        LambdaQueryWrapper<DqcPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcPlan::getTriggerType, "SCHEDULE")
                .eq(DqcPlan::getStatus, "PUBLISHED"); // 已发布状态
        return planMapper.selectList(wrapper);
    }

    /**
     * 判断方案是否应该执行
     * 基于Cron表达式和上次执行时间判断
     */
    private boolean shouldExecute(DqcPlan plan) {
        String cronExpression = plan.getTriggerCron();
        if (cronExpression == null || cronExpression.isBlank()) {
            log.warn("[定时调度] 方案{}未配置Cron表达式", plan.getId());
            return false;
        }

        try {
            // 解析Cron表达式
            Cron cron = quartzCronParser.parse(cronExpression);
            cron.validate();
            ExecutionTime executionTime = ExecutionTime.forCron(cron);

            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());

            // 获取最近一次应该执行的时间点
            Optional<ZonedDateTime> lastExecutionOpt = executionTime.lastExecution(now);
            if (lastExecutionOpt.isEmpty()) {
                return false;
            }

            ZonedDateTime lastScheduledTime = lastExecutionOpt.get();

            // 检查距上次调度时间是否在5分钟窗口内
            long minutesSinceScheduled = ChronoUnit.MINUTES.between(lastScheduledTime, now);
            if (minutesSinceScheduled > 10) {
                // 如果超过10分钟，说明已经错过了调度窗口，跳过
                log.debug("[定时调度] 方案{}已错过调度窗口(lastScheduledTime={}, now={})",
                        plan.getId(), lastScheduledTime, now);
                return false;
            }

            // 检查数据库中最近是否有实际执行记录
            DqcExecution lastExecution = getLastExecutionForPlan(plan.getId());
            if (lastExecution != null && lastExecution.getCreateTime() != null) {
                ZonedDateTime lastDbTime = lastExecution.getCreateTime().atZone(ZoneId.systemDefault());

                // 如果上次实际执行时间在Cron调度时间之后，说明已经执行过，跳过
                if (!lastDbTime.isBefore(lastScheduledTime)) {
                    log.debug("[定时调度] 方案{}在{}时已执行过，跳过", plan.getId(), lastScheduledTime);
                    return false;
                }
            }

            // 检查并发锁，防止重复执行
            if (isPlanLocked(plan.getId())) {
                log.debug("[定时调度] 方案{}正在执行中，跳过", plan.getId());
                return false;
            }

            return true;

        } catch (IllegalArgumentException e) {
            log.error("[定时调度] Cron表达式格式错误: planId={}, cron={}", plan.getId(), cronExpression, e);
            return false;
        } catch (Exception e) {
            log.warn("[定时调度] 解析Cron表达式失败: planId={}, cron={}", plan.getId(), cronExpression, e);
            return false;
        }
    }

    /**
     * 触发定时方案执行
     */
    private void triggerScheduledExecution(DqcPlan plan) {
        Long planId = plan.getId();

        // 检查方案是否有绑定的规则
        long ruleCount = planRuleMapper.selectCount(
                new LambdaQueryWrapper<DqcPlanRule>()
                        .eq(DqcPlanRule::getPlanId, planId)
                        .eq(DqcPlanRule::getEnabled, true));

        if (ruleCount == 0) {
            log.info("[定时调度] 方案{}未绑定任何启用的规则，跳过执行", planId);
            return;
        }

        // 获取执行锁
        if (!acquireLock(planId)) {
            log.warn("[定时调度] 无法获取方案执行锁: planId={}", planId);
            return;
        }

        try {
            log.info("[定时调度] 触发方案执行: planId={}, planName={}, triggerType=SCHEDULE",
                    planId, plan.getPlanName());

            // 调用执行服务触发执行
            Result<ExecutionTriggerVO> result = executionService.execute(planId, "SCHEDULE", 1L);

            if (result.isSuccess() && result.getData() != null) {
                ExecutionTriggerVO triggerVO = result.getData();
                log.info("[定时调度] 方案执行已提交: executionId={}, executionNo={}",
                        triggerVO.getExecutionId(), triggerVO.getExecutionNo());
            } else {
                log.warn("[定时调度] 方案执行提交失败: planId={}, result={}", planId, result);
            }
        } finally {
            // 执行完成后释放锁（考虑到是异步执行，这里延迟释放）
            // 实际释放逻辑在执行开始时设置，执行完成后释放
            scheduleLockRelease(planId);
        }
    }

    /**
     * 获取方案最近一次执行记录
     */
    private DqcExecution getLastExecutionForPlan(Long planId) {
        LambdaQueryWrapper<DqcExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcExecution::getPlanId, planId)
                .orderByDesc(DqcExecution::getCreateTime)
                .last("LIMIT 1");
        List<DqcExecution> executions = executionMapper.selectList(wrapper);
        return executions.isEmpty() ? null : executions.get(0);
    }

    /**
     * 检查方案是否被锁住（防止并发重复执行）
     */
    private boolean isPlanLocked(Long planId) {
        AtomicBoolean lock = executionLocks.get(planId);
        return lock != null && lock.get();
    }

    /**
     * 获取执行锁
     */
    private boolean acquireLock(Long planId) {
        AtomicBoolean lock = executionLocks.computeIfAbsent(planId, k -> new AtomicBoolean(false));
        return lock.compareAndSet(false, true);
    }

    /**
     * 释放执行锁
     */
    private void releaseLock(Long planId) {
        AtomicBoolean lock = executionLocks.get(planId);
        if (lock != null) {
            lock.set(false);
        }
    }

    /**
     * 延迟释放锁（考虑到异步执行需要一定时间）
     */
    private void scheduleLockRelease(Long planId) {
        // 异步延迟释放锁，给执行任务足够的启动时间
        new Thread(() -> {
            try {
                // 等待30秒后释放锁（假设最大执行时间为30秒）
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                releaseLock(planId);
            }
        }).start();
    }

    // ==================== 公开方法：供外部调用 ====================

    /**
     * 计算下次执行时间
     * 根据Cron表达式计算距当前时间最近的下一次执行时间点
     *
     * @param cronExpression Cron表达式（Quartz格式）
     * @return 下次执行时间，如果解析失败返回null
     */
    public LocalDateTime calculateNextExecutionTime(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return null;
        }

        try {
            Cron cron = quartzCronParser.parse(cronExpression);
            cron.validate();
            ExecutionTime executionTime = ExecutionTime.forCron(cron);

            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(now);

            if (nextExecution.isPresent()) {
                return nextExecution.get().toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("计算下次执行时间失败: cron={}", cronExpression, e);
        }

        return null;
    }

    /**
     * 验证Cron表达式是否有效
     *
     * @param cronExpression Cron表达式
     * @return 是否有效
     */
    public boolean validateCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return false;
        }

        try {
            Cron cron = quartzCronParser.parse(cronExpression);
            cron.validate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取方案的执行状态
     *
     * @param planId 方案ID
     * @return 执行状态信息
     */
    public PlanExecutionStatus getPlanExecutionStatus(Long planId) {
        boolean isLocked = isPlanLocked(planId);
        DqcExecution lastExecution = getLastExecutionForPlan(planId);
        LocalDateTime nextExecution = null;

        // 获取方案的Cron表达式
        DqcPlan plan = planMapper.selectById(planId);
        if (plan != null && StringUtils.hasText(plan.getTriggerCron())) {
            nextExecution = calculateNextExecutionTime(plan.getTriggerCron());
        }

        return new PlanExecutionStatus(
                planId,
                isLocked,
                lastExecution != null ? lastExecution.getId() : null,
                lastExecution != null ? lastExecution.getExecutionNo() : null,
                lastExecution != null ? lastExecution.getStatus() : null,
                lastExecution != null ? lastExecution.getCreateTime() : null,
                nextExecution
        );
    }

    /**
     * 方案执行状态
     */
    public record PlanExecutionStatus(
            Long planId,
            boolean isRunning,
            Long lastExecutionId,
            String lastExecutionNo,
            String lastExecutionStatus,
            LocalDateTime lastExecutionTime,
            LocalDateTime nextExecutionTime
    ) {}

    // JDK 8 兼容的字符串检查
    private static class StringUtils {
        static boolean hasText(String str) {
            return str != null && !str.isBlank();
        }
    }
}
