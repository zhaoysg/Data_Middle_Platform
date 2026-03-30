package com.bagdatahouse.dqc.engine;

import com.bagdatahouse.core.entity.DqcExecution;
import com.bagdatahouse.core.entity.DqcExecutionDetail;
import com.bagdatahouse.core.entity.DqcPlan;
import com.bagdatahouse.core.entity.DqcPlanRule;
import com.bagdatahouse.core.entity.DqcRuleDef;
import com.bagdatahouse.core.mapper.DqcExecutionDetailMapper;
import com.bagdatahouse.core.mapper.DqcExecutionMapper;
import com.bagdatahouse.core.mapper.DqcPlanMapper;
import com.bagdatahouse.core.mapper.DqcPlanRuleMapper;
import com.bagdatahouse.core.mapper.DqcRuleDefMapper;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import com.bagdatahouse.dqc.engine.scorer.QualityScorer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core rule execution engine
 * Orchestrates rule execution across different executor types
 */
@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    @Autowired
    private RuleExecutorFactory executorFactory;

    @Autowired
    private DataSourceAdapterRegistry adapterRegistry;

    @Autowired
    private DqcPlanMapper planMapper;

    @Autowired
    private DqcPlanRuleMapper planRuleMapper;

    @Autowired
    private DqcRuleDefMapper ruleDefMapper;

    @Autowired
    private DqcExecutionMapper executionMapper;

    @Autowired
    private DqcExecutionDetailMapper detailMapper;

    @Autowired
    private QualityScorer qualityScorer;

    private static final String RULE_STRENGTH_STRONG = "STRONG";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    /**
     * Execute all rules in a plan
     * 注意：该路径未被 DqcExecutionServiceImpl 使用，仅作备用入口。
     * 已发布的方案通过 DqcExecutionServiceImpl.execute() 异步执行。
     */
    @Async
    public void executePlanAsync(Long executionId, Long planId, Long triggerUser) {
        DqcExecution execution = executionMapper.selectById(executionId);
        DqcPlan plan = planMapper.selectById(planId);

        if (execution == null || plan == null) {
            log.error("Execution or Plan not found: executionId={}, planId={}", executionId, planId);
            return;
        }

        try {
            executeRulesInternal(execution, plan, triggerUser);
        } catch (Exception e) {
            log.error("Error executing plan: {}", planId, e);
            execution.setStatus(STATUS_FAILED);
            execution.setEndTime(LocalDateTime.now());
            execution.setErrorDetail(e.getMessage());
            executionMapper.updateById(execution);
        }
    }

    /**
     * Synchronous plan execution - for non-async scenarios
     */
    @Transactional(rollbackFor = Exception.class)
    public ExecutionResult executePlan(Long planId, Long triggerUser) {
        DqcPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            return ExecutionResult.failed(null, null, null, "Plan not found: " + planId, 0L, LocalDateTime.now());
        }

        String executionNo = "EXEC_" + System.currentTimeMillis();
        DqcExecution execution = DqcExecution.builder()
                .executionNo(executionNo)
                .planId(planId)
                .planName(plan.getPlanName())
                .planCode(plan.getPlanCode())
                .layerCode(plan.getLayerCode())
                .deptId(plan.getDeptId())
                .triggerType("MANUAL")
                .triggerUser(triggerUser)
                .startTime(LocalDateTime.now())
                .status(STATUS_RUNNING)
                .totalRules(0)
                .passedRules(0)
                .failedRules(0)
                .skippedRules(0)
                .createTime(LocalDateTime.now())
                .build();
        executionMapper.insert(execution);

        try {
            executeRulesInternal(execution, plan, triggerUser);
            return ExecutionResult.success(null, plan.getPlanName(), null,
                    (long) execution.getTotalRules(), (long) execution.getPassedRules(),
                    (long) execution.getFailedRules(), BigDecimal.valueOf(execution.getQualityScore()),
                    execution.getQualityScore(), execution.getElapsedMs(), execution.getStartTime());
        } catch (Exception e) {
            return ExecutionResult.failed(null, plan.getPlanName(), null, e.getMessage(),
                    System.currentTimeMillis() - execution.getStartTime().toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli(),
                    execution.getStartTime());
        }
    }

    /**
     * Execute a single rule with full context (public for direct service calls)
     */
    public ExecutionResult execute(RuleContext context, DataSourceAdapter adapter) {
        return executeSingleRuleInternal(context, adapter);
    }

    /**
     * Execute a single rule
     */
    public ExecutionResult executeRule(Long ruleId, Long targetDsId, String tableName, String columnName) {
        DqcRuleDef rule = ruleDefMapper.selectById(ruleId);
        if (rule == null) {
            return ExecutionResult.failed(ruleId, null, null, "Rule not found: " + ruleId, 0L, LocalDateTime.now());
        }

        RuleContext context = buildRuleContext(rule, null, null);
        context.setTargetDsId(targetDsId != null ? targetDsId : rule.getTargetDsId());
        context.setTargetTable(tableName != null ? tableName : rule.getTargetTable());
        context.setTargetColumn(columnName != null ? columnName : rule.getTargetColumn());

        DataSourceAdapter adapter = getAdapter(context.getTargetDsId());
        if (adapter == null) {
            return ExecutionResult.failed(ruleId, null, null,
                    "Data source adapter not found for dsId: " + context.getTargetDsId(),
                    0L, LocalDateTime.now());
        }

        return executeSingleRuleInternal(context, adapter);
    }

    /**
     * Execute a batch of rules
     */
    public List<ExecutionResult> executeRules(Long planId, List<RuleContext> contexts) {
        DqcPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            return contexts.stream()
                    .map(c -> ExecutionResult.failed(c.getRuleId(), c.getRuleName(), null,
                            "Plan not found", 0L, LocalDateTime.now()))
                    .toList();
        }

        List<ExecutionResult> results = new ArrayList<>();
        boolean blocked = false;

        for (RuleContext context : contexts) {
            if (blocked) {
                results.add(ExecutionResult.skipped(context.getRuleId(), context.getRuleName(),
                        "Blocked by previous strong rule failure", 0L, LocalDateTime.now()));
                continue;
            }

            DataSourceAdapter adapter = getAdapter(context.getTargetDsId());
            if (adapter == null) {
                results.add(ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                        "Data source adapter not found for dsId: " + context.getTargetDsId(),
                        0L, LocalDateTime.now()));
                continue;
            }

            ExecutionResult result = executeSingleRuleInternal(context, adapter);
            results.add(result);

            if (Boolean.TRUE.equals(context.getAutoBlock())
                    && RULE_STRENGTH_STRONG.equals(context.getRuleStrength())
                    && !result.isSuccess()) {
                blocked = true;
            }
        }

        return results;
    }

    /**
     * Internal method to execute rules
     */
    private void executeRulesInternal(DqcExecution execution, DqcPlan plan, Long triggerUser) {
        LambdaQueryWrapper<DqcPlanRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcPlanRule::getPlanId, plan.getId());
        wrapper.eq(DqcPlanRule::getEnabled, true);
        wrapper.orderByAsc(DqcPlanRule::getRuleOrder);
        List<DqcPlanRule> planRules = planRuleMapper.selectList(wrapper);

        if (planRules.isEmpty()) {
            execution.setStatus(STATUS_SUCCESS);
            execution.setEndTime(LocalDateTime.now());
            execution.setElapsedMs(0L);
            executionMapper.updateById(execution);
            return;
        }

        int totalRules = planRules.size();
        int passedRules = 0;
        int failedRules = 0;
        int skippedRules = 0;
        boolean blocked = false;
        List<DqcExecutionDetail> allDetails = new ArrayList<>();

        for (DqcPlanRule planRule : planRules) {
            if (blocked) {
                DqcExecutionDetail skippedDetail = createSkippedDetail(execution, planRule,
                        "Blocked by previous strong rule failure");
                detailMapper.insert(skippedDetail);
                allDetails.add(skippedDetail);
                skippedRules++;
                continue;
            }

            DqcRuleDef ruleDef = ruleDefMapper.selectById(planRule.getRuleId());
            if (ruleDef == null || !Boolean.TRUE.equals(ruleDef.getEnabled())) {
                DqcExecutionDetail skippedDetail = createSkippedDetail(execution, planRule,
                        "Rule not found or disabled");
                detailMapper.insert(skippedDetail);
                allDetails.add(skippedDetail);
                skippedRules++;
                continue;
            }

            RuleContext context = buildRuleContext(ruleDef, planRule, execution);
            context.setAutoBlock(plan.getAutoBlock());

            DataSourceAdapter adapter = getAdapter(context.getTargetDsId());
            if (adapter == null) {
                DqcExecutionDetail skippedDetail = createSkippedDetail(execution, planRule,
                        "Data source adapter not found for dsId: " + context.getTargetDsId());
                detailMapper.insert(skippedDetail);
                allDetails.add(skippedDetail);
                skippedRules++;
                continue;
            }

            ExecutionResult result = executeSingleRuleInternal(context, adapter);

            DqcExecutionDetail detail = convertToDetail(execution, context, result);
            detailMapper.insert(detail);
            allDetails.add(detail);

            if (result.getStatus() == ExecutionResult.Status.SUCCESS) {
                passedRules++;
            } else if (result.getStatus() == ExecutionResult.Status.SKIPPED) {
                skippedRules++;
            } else {
                failedRules++;
            }

            if (Boolean.TRUE.equals(plan.getAutoBlock())
                    && RULE_STRENGTH_STRONG.equals(ruleDef.getRuleStrength())
                    && !result.isSuccess()) {
                blocked = true;
                execution.setBlocked(true);
            }
        }

        int qualityScore = calculateOverallScore(allDetails);

        execution.setEndTime(LocalDateTime.now());
        execution.setElapsedMs(System.currentTimeMillis() -
                execution.getStartTime().toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli());
        execution.setStatus(blocked ? "BLOCKED" : STATUS_SUCCESS);
        execution.setTotalRules(totalRules);
        execution.setPassedRules(passedRules);
        execution.setFailedRules(failedRules);
        execution.setSkippedRules(skippedRules);
        execution.setQualityScore(qualityScore);
        executionMapper.updateById(execution);

        try {
            qualityScorer.calculateAndSaveScore(execution.getId(), allDetails);
        } catch (Exception e) {
            log.error("Failed to calculate and save quality score", e);
        }

        DqcPlan updatePlan = new DqcPlan();
        updatePlan.setId(plan.getId());
        updatePlan.setLastExecutionId(execution.getId());
        updatePlan.setLastExecutionTime(LocalDateTime.now());
        updatePlan.setLastExecutionScore(qualityScore);
        planMapper.updateById(updatePlan);
    }

    /**
     * Execute a single rule (internal, uses getAdapter)
     */
    private ExecutionResult executeSingleRule(RuleContext context) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            DataSourceAdapter adapter = getAdapter(context.getTargetDsId());
            if (adapter == null) {
                return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                        "Data source adapter not found for dsId: " + context.getTargetDsId(),
                        0L, startTime);
            }

            return executorFactory.getExecutor(context.getRuleType())
                    .execute(context, adapter);

        } catch (IllegalArgumentException e) {
            log.error("Unsupported rule type: {}", context.getRuleType());
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Unsupported rule type: " + context.getRuleType(),
                    System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli(),
                    startTime);
        } catch (Exception e) {
            log.error("Error executing rule: {}", context.getRuleId(), e);
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Execution error: " + e.getMessage(),
                    System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli(),
                    startTime);
        }
    }

    /**
     * Execute a single rule with provided adapter (internal, for direct calls)
     */
    private ExecutionResult executeSingleRuleInternal(RuleContext context, DataSourceAdapter adapter) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            if (adapter == null) {
                return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                        "Data source adapter is null",
                        0L, startTime);
            }

            return executorFactory.getExecutor(context.getRuleType())
                    .execute(context, adapter);

        } catch (IllegalArgumentException e) {
            log.error("Unsupported rule type: {}", context.getRuleType());
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Unsupported rule type: " + context.getRuleType(),
                    System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli(),
                    startTime);
        } catch (Exception e) {
            log.error("Error executing rule: {}", context.getRuleId(), e);
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Execution error: " + e.getMessage(),
                    System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli(),
                    startTime);
        }
    }

    /**
     * Build rule context from rule definition and plan-rule binding
     */
    private RuleContext buildRuleContext(DqcRuleDef rule, DqcPlanRule planRule, DqcExecution execution) {
        RuleContext.Builder builder = RuleContext.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getRuleName())
                .ruleCode(rule.getRuleCode())
                .ruleType(rule.getRuleType())
                .applyLevel(rule.getApplyLevel())
                .targetDsId(rule.getTargetDsId())
                .targetTable(rule.getTargetTable())
                .targetColumn(rule.getTargetColumn())
                .compareDsId(rule.getCompareDsId())
                .compareTable(rule.getCompareTable())
                .compareColumn(rule.getCompareColumn())
                .thresholdMin(rule.getThresholdMin())
                .thresholdMax(rule.getThresholdMax())
                .fluctuationThreshold(rule.getFluctuationThreshold())
                .regexPattern(rule.getRegexPattern())
                .ruleExpr(rule.getRuleExpr())
                .dimensions(rule.getDimensions())
                .ruleStrength(rule.getRuleStrength())
                .errorLevel(rule.getErrorLevel())
                .customFunctionClass(rule.getCustomFunctionClass())
                .customFunctionParams(rule.getCustomFunctionParams());

        if (planRule != null) {
            builder.customThreshold(planRule.getCustomThreshold());
        }

        if (execution != null) {
            builder.executionId(execution.getId());
            builder.executionNo(execution.getExecutionNo());
        }

        return builder.build();
    }

    /**
     * Create a skipped execution detail
     */
    private DqcExecutionDetail createSkippedDetail(DqcExecution execution, DqcPlanRule planRule, String reason) {
        DqcRuleDef rule = ruleDefMapper.selectById(planRule.getRuleId());
        return DqcExecutionDetail.builder()
                .executionId(execution.getId())
                .executionNo(execution.getExecutionNo())
                .ruleId(planRule.getRuleId())
                .ruleName(rule != null ? rule.getRuleName() : "Unknown")
                .ruleCode(rule != null ? rule.getRuleCode() : null)
                .ruleType(rule != null ? rule.getRuleType() : null)
                .ruleStrength(rule != null ? rule.getRuleStrength() : null)
                .dimensions(rule != null ? rule.getDimensions() : null)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .elapsedMs(0L)
                .status("SKIPPED")
                .errorDetail(reason)
                .qualityScore(100)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * Convert execution result to detail entity
     */
    private DqcExecutionDetail convertToDetail(DqcExecution execution, RuleContext context, ExecutionResult result) {
        return DqcExecutionDetail.builder()
                .executionId(execution.getId())
                .executionNo(execution.getExecutionNo())
                .ruleId(context.getRuleId())
                .ruleName(context.getRuleName())
                .ruleCode(context.getRuleCode())
                .ruleType(context.getRuleType())
                .ruleStrength(context.getRuleStrength())
                .dimensions(context.getDimensions())
                .targetDsId(context.getTargetDsId())
                .targetTable(context.getTargetTable())
                .targetColumn(context.getTargetColumn())
                .startTime(result.getStartTime())
                .endTime(result.getEndTime())
                .elapsedMs(result.getElapsedMs())
                .status(result.getStatus().name())
                .totalCount(result.getTotalCount())
                .errorCount(result.getErrorCount())
                .passCount(result.getPassCount())
                .actualValue(result.getActualValue())
                .qualityScore(result.getQualityScore())
                .errorDetail(result.getErrorDetail())
                .sqlContent(result.getSqlContent())
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * Calculate overall quality score from details
     */
    private int calculateOverallScore(List<DqcExecutionDetail> details) {
        if (details.isEmpty()) {
            return 100;
        }

        QualityScorer.ScoreBreakdown breakdown = qualityScorer.calculateBreakdown(details);
        return breakdown.overallScore().intValue();
    }

    /**
     * Get data source adapter
     */
    private DataSourceAdapter getAdapter(Long dsId) {
        if (dsId == null) {
            return null;
        }
        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            log.warn("No adapter registered for dsId: {}, will try to get by querying datasource", dsId);
        }
        return adapter;
    }
}
