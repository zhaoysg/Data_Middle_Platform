package com.bagdatahouse.dqc.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqcExecution;
import com.bagdatahouse.core.entity.DqcExecutionDetail;
import com.bagdatahouse.core.entity.DqcPlan;
import com.bagdatahouse.core.entity.DqcPlanRule;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.entity.DqcRuleDef;
import com.bagdatahouse.core.entity.DqcQualityScore;
import com.bagdatahouse.core.entity.SecColumnSensitivity;
import com.bagdatahouse.core.entity.SecLevel;
import com.bagdatahouse.core.mapper.DqcExecutionDetailMapper;
import com.bagdatahouse.core.mapper.DqcExecutionMapper;
import com.bagdatahouse.core.mapper.DqcPlanMapper;
import com.bagdatahouse.core.mapper.DqcPlanRuleMapper;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.mapper.DqcRuleDefMapper;
import com.bagdatahouse.core.mapper.DqcQualityScoreMapper;
import com.bagdatahouse.core.mapper.SecColumnSensitivityMapper;
import com.bagdatahouse.core.mapper.SecLevelMapper;
import com.bagdatahouse.core.vo.ExecutionTriggerVO;
import com.bagdatahouse.core.vo.QualityReportVO;
import com.bagdatahouse.dqc.service.DqcExecutionService;
import com.bagdatahouse.dqc.service.DqcQualityScoreService;
import com.bagdatahouse.dqc.engine.RuleEngine;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.bagdatahouse.dqc.engine.RuleContext;
import com.bagdatahouse.dqc.engine.scorer.QualityScorer;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import com.bagdatahouse.dqc.engine.executor.RuleExecutor;
import com.bagdatahouse.dqc.engine.RuleExecutorFactory;
import com.bagdatahouse.dqc.util.JdbcErrorMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.Map.Entry;

/**
 * DQC质检执行服务实现
 * 支持三种触发方式：手动触发、定时触发、API触发
 */
@Service
public class DqcExecutionServiceImpl extends ServiceImpl<DqcExecutionMapper, DqcExecution>
        implements DqcExecutionService {

    private static final Logger log = LoggerFactory.getLogger(DqcExecutionServiceImpl.class);

    @Autowired
    private DqcPlanMapper planMapper;

    @Autowired
    private DqcPlanRuleMapper planRuleMapper;

    @Autowired
    private DqcRuleDefMapper ruleDefMapper;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private DqcExecutionDetailMapper detailMapper;

    @Autowired
    private DqcQualityScoreMapper qualityScoreMapper;

    @Autowired
    private DqcQualityScoreService qualityScoreService;

    @Autowired
    private DqcExecutionMapper executionMapper;

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private RuleExecutorFactory executorFactory;

    @Autowired
    private DataSourceAdapterRegistry adapterRegistry;

    @Autowired
    private QualityScorer qualityScorer;

    @Autowired
    private SecColumnSensitivityMapper secColumnSensitivityMapper;

    @Autowired
    private SecLevelMapper secLevelMapper;

    @Autowired
    @Lazy
    private DqcExecutionService self;

    /**
     * 触发质检执行
     * 支持三种触发类型：MANUAL（手动）、SCHEDULE（定时）、API（接口）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<ExecutionTriggerVO> execute(Long planId, String triggerType, Long triggerUser) {
        DqcPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(4001, "质检方案不存在");
        }

        // 检查方案是否已发布（只有已发布状态才能执行）
        if (!isPlanExecutable(plan)) {
            throw new BusinessException(4002, "方案未发布，不能执行");
        }

        // 生成唯一执行编号
        String executionNo = generateExecutionNo(triggerType);
        Long actualUser = triggerUser != null ? triggerUser : 1L;

        DqcExecution execution = DqcExecution.builder()
                .executionNo(executionNo)
                .planId(planId)
                .planName(plan.getPlanName())
                .planCode(plan.getPlanCode())
                .layerCode(plan.getLayerCode())
                .deptId(plan.getDeptId())
                .triggerType(triggerType)
                .triggerUser(actualUser)
                .startTime(LocalDateTime.now())
                .status("RUNNING")
                .totalRules(0)
                .passedRules(0)
                .failedRules(0)
                .skippedRules(0)
                .blocked(false)
                .createTime(LocalDateTime.now())
                .build();
        executionMapper.insert(execution);

        // 查询方案绑定的规则
        List<DqcPlanRule> planRules = getEnabledPlanRules(planId);
        if (planRules.isEmpty()) {
            execution.setStatus("SUCCESS");
            execution.setEndTime(LocalDateTime.now());
            execution.setElapsedMs(0L);
            execution.setQualityScore(100);
            executionMapper.updateById(execution);
            return Result.success(buildTriggerVO(execution, plan, 0));
        }

        // 异步执行规则：通过 self 代理触发（同类内部调用不走 AOP，@Async 必须走代理）
        // 必须在事务提交后再调度：否则异步线程可能读不到未提交的 execution 行（selectById 为空），
        // 任务直接 return，前端永远「运行中」且 totalRules=0。
        final Long committedExecutionId = execution.getId();
        final String bindType = plan.getBindType();
        final String bindValue = plan.getBindValue();
        Runnable scheduleAsync = () -> self.executeRulesAsync(committedExecutionId, planRules, bindType, bindValue);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    scheduleAsync.run();
                }
            });
        } else {
            scheduleAsync.run();
        }

        log.info("质检执行已触发: executionNo={}, planId={}, planName={}, triggerType={}, user={}",
                executionNo, planId, plan.getPlanName(), triggerType, actualUser);

        return Result.success(buildTriggerVO(execution, plan, planRules.size()));
    }

    /**
     * 异步执行所有规则（通过 self 代理调用才生效）
     * @param bindType    方案绑定类型：TABLE / LAYER / PATTERN
     * @param bindValue   方案绑定值 JSON，含 dsId / tables / layer 等
     */
    @Async
    public void executeRulesAsync(Long executionId, List<DqcPlanRule> planRules,
                                  String bindType, String bindValue) {
        DqcExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.error("执行记录不存在: executionId={}", executionId);
            return;
        }

        DqcPlan plan = planMapper.selectById(execution.getPlanId());
        // 解析方案的绑定配置，供规则上下文使用
        BindConfig bindConfig = parseBindConfig(bindType, bindValue);

        int totalRules = planRules.size();
        int passedRules = 0;
        int failedRules = 0;
        int skippedRules = 0;
        boolean blocked = false;
        List<DqcExecutionDetail> allDetails = new ArrayList<>();

        long startMs = System.currentTimeMillis();

        // 立即写入总规则数，便于前端轮询展示进度分母（否则首条规则执行期间一直为 0）
        updateExecutionProgress(execution, totalRules, 0, 0, 0);

        for (DqcPlanRule planRule : planRules) {
            if (blocked) {
                DqcExecutionDetail skippedDetail = createSkippedDetail(execution, planRule,
                        "被前序强规则失败阻断");
                detailMapper.insert(skippedDetail);
                allDetails.add(skippedDetail);
                skippedRules++;
                updateExecutionProgress(execution, totalRules, passedRules, failedRules, skippedRules);
                continue;
            }

            DqcRuleDef ruleDef = ruleDefMapper.selectById(planRule.getRuleId());
            if (ruleDef == null || !Boolean.TRUE.equals(ruleDef.getEnabled())) {
                DqcExecutionDetail skippedDetail = createSkippedDetail(execution, planRule,
                        "规则不存在或已禁用");
                detailMapper.insert(skippedDetail);
                allDetails.add(skippedDetail);
                skippedRules++;
                continue;
            }

            // 构建规则上下文，并应用方案的绑定覆盖（关键修复）
            RuleContext context = buildRuleContext(ruleDef, planRule, execution, bindConfig);
            context.setAutoBlock(plan.getAutoBlock());

            // 获取数据源适配器
            DataSourceAdapter adapter = getDataSourceAdapter(context.getTargetDsId());
            if (adapter == null) {
                DqcExecutionDetail skippedDetail = createSkippedDetail(execution, planRule,
                        "数据源适配器不存在: dsId=" + context.getTargetDsId());
                detailMapper.insert(skippedDetail);
                allDetails.add(skippedDetail);
                skippedRules++;
                continue;
            }

            // 执行规则
            DqcExecutionDetail detail = executeSingleRule(execution, context, planRule, adapter);
            allDetails.add(detail);

            // 更新计数
            switch (detail.getStatus()) {
                case "SUCCESS" -> passedRules++;
                case "FAILED" -> {
                    failedRules++;
                    // 检查强规则阻断
                    if (Boolean.TRUE.equals(plan.getAutoBlock())
                            && "STRONG".equals(ruleDef.getRuleStrength())) {
                        blocked = true;
                        execution.setBlocked(true);
                        log.warn("强规则[{}]执行失败且autoBlock=true，阻断后续规则执行", ruleDef.getRuleName());
                    }
                }
                case "SKIPPED" -> skippedRules++;
            }

            // 实时更新执行进度
            updateExecutionProgress(execution, totalRules, passedRules, failedRules, skippedRules);
        }

        // 计算质量评分
        QualityScorer.ScoreBreakdown breakdown = qualityScorer.calculateBreakdown(allDetails);
        int qualityScore = breakdown.overallScore().intValue();

        // 计算耗时
        long elapsedMs = System.currentTimeMillis() - startMs;

        // 更新执行记录最终状态
        execution.setEndTime(LocalDateTime.now());
        execution.setElapsedMs(elapsedMs);
        execution.setStatus(blocked ? "BLOCKED" : "SUCCESS");
        execution.setTotalRules(totalRules);
        execution.setPassedRules(passedRules);
        execution.setFailedRules(failedRules);
        execution.setSkippedRules(skippedRules);
        execution.setQualityScore(qualityScore);

        // 保存评分明细JSON
        execution.setDimensionScores(qualityScorer.serializeBreakdown(breakdown));
        execution.setScoreBreakdown(JSON.toJSONString(breakdown.dimensionScores()));
        executionMapper.updateById(execution);

        // 更新方案的最新执行信息
        updatePlanLastExecution(plan, executionId, qualityScore);

        // 计算并保存质量评分到历史表
        try {
            saveQualityScoreHistory(execution, allDetails, breakdown);
        } catch (Exception e) {
            log.error("保存质量评分历史失败: executionId={}", executionId, e);
        }

        // 发送告警通知（如果配置了）
        try {
            sendAlertIfNeeded(plan, execution, passedRules, failedRules);
        } catch (Exception e) {
            log.error("发送告警失败: executionId={}", executionId, e);
        }

        log.info("质检执行完成: executionNo={}, total={}, passed={}, failed={}, score={}, blocked={}, elapsed={}ms",
                execution.getExecutionNo(), totalRules, passedRules, failedRules, qualityScore, blocked, elapsedMs);
    }

    /**
     * 执行单条规则
     */
    private DqcExecutionDetail executeSingleRule(DqcExecution execution, RuleContext context,
                                                DqcPlanRule planRule, DataSourceAdapter adapter) {
        DqcExecutionDetail detail = DqcExecutionDetail.builder()
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
                .startTime(LocalDateTime.now())
                .status("RUNNING")
                .createTime(LocalDateTime.now())
                .build();
        detailMapper.insert(detail);

        long startMs = System.currentTimeMillis();

        try {
            // 使用 RuleEngine 执行规则
            ExecutionResult result = ruleEngine.execute(context, adapter);

            detail.setEndTime(LocalDateTime.now());
            detail.setElapsedMs(System.currentTimeMillis() - startMs);
            detail.setStatus(result.getStatus().name());
            detail.setTotalCount(result.getTotalCount());
            detail.setPassCount(result.getPassCount());
            detail.setErrorCount(result.getErrorCount());
            detail.setActualValue(result.getActualValue());
            detail.setQualityScore(result.getQualityScore());
            detail.setErrorDetail(result.getErrorDetail());
            detail.setSqlContent(result.getSqlContent());
        } catch (IllegalArgumentException e) {
            // 不支持的规则类型
            log.error("不支持的规则类型: ruleType={}", context.getRuleType(), e);
            detail.setEndTime(LocalDateTime.now());
            detail.setElapsedMs(System.currentTimeMillis() - startMs);
            detail.setStatus("FAILED");
            detail.setErrorDetail("不支持的规则类型: " + context.getRuleType());
            detail.setQualityScore(0);
        } catch (Exception e) {
            // 执行异常
            log.error("规则执行异常: ruleId={}", context.getRuleId(), e);
            detail.setEndTime(LocalDateTime.now());
            detail.setElapsedMs(System.currentTimeMillis() - startMs);
            detail.setStatus("FAILED");
            detail.setErrorDetail("执行异常: " + JdbcErrorMessageUtil.humanize(e));
            detail.setQualityScore(0);
        }

        // 阶段三-T3: JOIN sec_column_sensitivity 回填 sensitivityLevel + sensitivityClass
        enrichSensitivityInfo(detail);

        detailMapper.updateById(detail);
        return detail;
    }

    /**
     * 构建规则执行上下文
     * @param bindConfig 方案的绑定配置，用于覆盖规则默认目标表/数据源
     */
    private RuleContext buildRuleContext(DqcRuleDef rule, DqcPlanRule planRule,
                                       DqcExecution execution, BindConfig bindConfig) {
        // 优先级: planRule.getTargetTable() > bindConfig.table > rule.getTargetTable()
        String effectiveTable = (planRule != null && StringUtils.hasText(planRule.getTargetTable()))
                ? planRule.getTargetTable()
                : ((bindConfig != null && StringUtils.hasText(bindConfig.table))
                        ? bindConfig.table : rule.getTargetTable());
        // 优先级: planRule.getTargetColumn() > rule.getTargetColumn()
        String effectiveColumn = (planRule != null && StringUtils.hasText(planRule.getTargetColumn()))
                ? planRule.getTargetColumn() : rule.getTargetColumn();
        Long effectiveDsId = (bindConfig != null && bindConfig.dsId != null)
                ? bindConfig.dsId : rule.getTargetDsId();

        String targetDatabaseName = null;
        String targetSchemaName = null;
        if (effectiveDsId != null) {
            DqDatasource ds = datasourceMapper.selectById(effectiveDsId);
            if (ds != null) {
                targetDatabaseName = ds.getDatabaseName();
                if (bindConfig != null && StringUtils.hasText(bindConfig.schema)) {
                    targetSchemaName = bindConfig.schema.trim();
                } else if (StringUtils.hasText(ds.getSchemaName())) {
                    targetSchemaName = ds.getSchemaName().trim();
                }
            }
        }

        RuleContext.Builder builder = RuleContext.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getRuleName())
                .ruleCode(rule.getRuleCode())
                .ruleType(rule.getRuleType())
                .applyLevel(rule.getApplyLevel())
                .targetDsId(effectiveDsId)
                .targetDatabaseName(targetDatabaseName)
                .targetSchemaName(targetSchemaName)
                .targetTable(effectiveTable)
                .targetColumn(effectiveColumn)
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
                .customFunctionParams(rule.getCustomFunctionParams())
                .executionId(execution.getId())
                .executionNo(execution.getExecutionNo())
                .planId(execution.getPlanId());

        // 合并方案级别的自定义阈值覆盖
        if (planRule != null && StringUtils.hasText(planRule.getCustomThreshold())) {
            builder.customThreshold(planRule.getCustomThreshold());
            applyCustomThreshold(builder, planRule.getCustomThreshold());
        }

        RuleContext ctx = builder.build();
        // NOTE: 早期 ${table}/${dsId} 替换已移除。
        // 所有占位符统一由各执行器的 interpolateRuleExpr() 在 context.getTargetTable() /
        // context.getTargetDsId() 上展开，保证与 effectiveTable（planRule > bindConfig > rule）
        // 的优先级完全一致，避免双路径真相。
        return ctx;
    }

    /**
     * 应用方案级别的自定义阈值覆盖
     * 阈值格式: {"thresholdMin": 0.95, "thresholdMax": 1.0}
     */
    private void applyCustomThreshold(RuleContext.Builder builder, String customThreshold) {
        if (!StringUtils.hasText(customThreshold)) {
            return;
        }
        try {
            JSONObject json = JSON.parseObject(customThreshold);
            if (json.containsKey("thresholdMin")) {
                builder.thresholdMin(json.getBigDecimal("thresholdMin"));
            }
            if (json.containsKey("thresholdMax")) {
                builder.thresholdMax(json.getBigDecimal("thresholdMax"));
            }
            if (json.containsKey("fluctuationThreshold")) {
                builder.fluctuationThreshold(json.getBigDecimal("fluctuationThreshold"));
            }
        } catch (Exception e) {
            log.warn("解析自定义阈值失败: {}", customThreshold, e);
        }
    }

    /**
     * 解析方案的绑定配置，从 bindValue JSON 中提取 dsId 和目标表
     * bindValue 格式示例（按表绑定）:
     *   {"dsId": 2, "schema": "public", "tables": ["table_a", "table_b"]}
     * bindValue 格式示例（按层绑定 / LAYER）:
     *   {"dsId": 2, "layer": "DWD"}
     */
    private BindConfig parseBindConfig(String bindType, String bindValue) {
        if (!StringUtils.hasText(bindValue)) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(bindValue);
            BindConfig cfg = new BindConfig();
            if (json.containsKey("dsId")) {
                cfg.dsId = json.getLong("dsId");
            }
            if ("TABLE".equalsIgnoreCase(bindType)) {
                // 按表绑定：取第一张表作为执行目标（规则绑定粒度到单表）
                JSONArray tables = json.getJSONArray("tables");
                if (tables != null && !tables.isEmpty()) {
                    cfg.table = tables.getString(0);
                }
                if (json.containsKey("schema")) {
                    cfg.schema = json.getString("schema");
                }
            } else if ("LAYER".equalsIgnoreCase(bindType)) {
                cfg.layer = json.getString("layer");
            }
            return cfg;
        } catch (Exception e) {
            log.warn("解析方案绑定配置失败: bindValue={}", bindValue, e);
            return null;
        }
    }

    /** 方案绑定配置内部类 */
    private static class BindConfig {
        Long dsId;
        String table;
        String layer;
        /** PostgreSQL 等：方案绑定的 schema */
        String schema;
    }

    /**
     * 创建被跳过的执行明细
     */
    private DqcExecutionDetail createSkippedDetail(DqcExecution execution, DqcPlanRule planRule, String reason) {
        DqcRuleDef rule = ruleDefMapper.selectById(planRule.getRuleId());
        return DqcExecutionDetail.builder()
                .executionId(execution.getId())
                .executionNo(execution.getExecutionNo())
                .ruleId(planRule.getRuleId())
                .ruleName(rule != null ? rule.getRuleName() : "未知规则")
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
     * 获取启用的方案规则列表
     */
    private List<DqcPlanRule> getEnabledPlanRules(Long planId) {
        LambdaQueryWrapper<DqcPlanRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcPlanRule::getPlanId, planId)
                .eq(DqcPlanRule::getEnabled, true)
                .orderByAsc(DqcPlanRule::getRuleOrder);
        return planRuleMapper.selectList(wrapper);
    }

    /**
     * 获取数据源适配器
     */
    private DataSourceAdapter getDataSourceAdapter(Long dsId) {
        if (dsId == null) {
            return null;
        }
        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            log.warn("数据源适配器不存在: dsId={}", dsId);
        }
        return adapter;
    }

    /**
     * 检查方案是否可执行
     */
    private boolean isPlanExecutable(DqcPlan plan) {
        // 状态为 PUBLISHED 表示已发布，可执行
        return plan.getStatus() != null && "PUBLISHED".equals(plan.getStatus());
    }

    /**
     * 生成执行编号
     */
    private String generateExecutionNo(String triggerType) {
        String prefix = switch (triggerType != null ? triggerType.toUpperCase() : "MANUAL") {
            case "SCHEDULE" -> "SCH";
            case "API" -> "API";
            default -> "MAN";
        };
        return String.format("%s_%s_%04d", prefix, System.currentTimeMillis(),
                new Random().nextInt(10000));
    }

    /**
     * 构建触发响应VO
     */
    private ExecutionTriggerVO buildTriggerVO(DqcExecution execution, DqcPlan plan, int ruleCount) {
        return ExecutionTriggerVO.builder()
                .executionId(execution.getId())
                .executionNo(execution.getExecutionNo())
                .status(execution.getStatus())
                .startTime(execution.getStartTime())
                .totalRules(ruleCount)
                .passedRules(0)
                .failedRules(0)
                .skippedRules(0)
                .qualityScore(execution.getQualityScore() != null ? execution.getQualityScore() : 0)
                .blocked(false)
                .planName(plan.getPlanName())
                .planCode(plan.getPlanCode())
                .build();
    }

    /**
     * 更新执行进度
     */
    private void updateExecutionProgress(DqcExecution execution, int total, int passed, int failed, int skipped) {
        LambdaUpdateWrapper<DqcExecution> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DqcExecution::getId, execution.getId());
        wrapper.set(DqcExecution::getTotalRules, total);
        wrapper.set(DqcExecution::getPassedRules, passed);
        wrapper.set(DqcExecution::getFailedRules, failed);
        wrapper.set(DqcExecution::getSkippedRules, skipped);
        executionMapper.update(null, wrapper);
    }

    /**
     * 更新方案的最新执行信息
     */
    private void updatePlanLastExecution(DqcPlan plan, Long executionId, int qualityScore) {
        LambdaUpdateWrapper<DqcPlan> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DqcPlan::getId, plan.getId());
        wrapper.set(DqcPlan::getLastExecutionId, executionId);
        wrapper.set(DqcPlan::getLastExecutionTime, LocalDateTime.now());
        wrapper.set(DqcPlan::getLastExecutionScore, qualityScore);
        planMapper.update(null, wrapper);
    }

    /**
     * 保存质量评分历史
     */
    private void saveQualityScoreHistory(DqcExecution execution, List<DqcExecutionDetail> details,
                                        QualityScorer.ScoreBreakdown breakdown) {
        if (details.isEmpty()) {
            return;
        }

        // 按目标表分组计算评分
        Map<String, List<DqcExecutionDetail>> byTable = details.stream()
                .filter(d -> d.getTargetTable() != null)
                .collect(Collectors.groupingBy(DqcExecutionDetail::getTargetTable));

        for (Map.Entry<String, List<DqcExecutionDetail>> entry : byTable.entrySet()) {
            String tableName = entry.getKey();
            List<DqcExecutionDetail> tableDetails = entry.getValue();

            // 计算该表的评分
            QualityScorer.ScoreBreakdown tableBreakdown = qualityScorer.calculateBreakdown(tableDetails);

            // 统计规则执行情况
            int total = tableDetails.size();
            int passed = (int) tableDetails.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
            int failed = (int) tableDetails.stream().filter(d -> "FAILED".equals(d.getStatus())).count();

            BigDecimal rulePassRate = total > 0
                    ? BigDecimal.valueOf(100.0 * passed / total).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 综合评分存「健康度」：有失败时等于规则通过率，避免加权维度分与 通过/总数 严重背离
            BigDecimal displayOverall = tableBreakdown.healthScore() != null
                    ? tableBreakdown.healthScore()
                    : tableBreakdown.overallScore();

            DqcQualityScore score = DqcQualityScore.builder()
                    .checkDate(LocalDateTime.now())
                    .targetDsId(details.get(0).getTargetDsId())
                    .targetTable(tableName)
                    .layerCode(execution.getLayerCode())
                    .deptId(execution.getDeptId())
                    .completenessScore(tableBreakdown.dimensionScores().get("COMPLETENESS"))
                    .uniquenessScore(tableBreakdown.dimensionScores().get("UNIQUENESS"))
                    .accuracyScore(tableBreakdown.dimensionScores().get("ACCURACY"))
                    .consistencyScore(tableBreakdown.dimensionScores().get("CONSISTENCY"))
                    .timelinessScore(tableBreakdown.dimensionScores().get("TIMELINESS"))
                    .validityScore(tableBreakdown.dimensionScores().get("VALIDITY"))
                    .sensitivityComplianceScore(tableBreakdown.dimensionScores().get(QualityScorer.DIM_SENSITIVITY))
                    .overallScore(displayOverall)
                    .rulePassRate(rulePassRate)
                    .ruleTotalCount(total)
                    .rulePassCount(passed)
                    .ruleFailCount(failed)
                    .executionId(execution.getId())
                    .createTime(LocalDateTime.now())
                    .build();

            // 检查是否当天已有记录，有则更新
            LambdaQueryWrapper<DqcQualityScore> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(DqcQualityScore::getTargetTable, tableName);
            existWrapper.apply("DATE(check_date) = CURDATE()");
            DqcQualityScore existing = qualityScoreMapper.selectOne(existWrapper);

            if (existing != null) {
                score.setId(existing.getId());
                qualityScoreMapper.updateById(score);
            } else {
                qualityScoreMapper.insert(score);
            }
        }
    }

    /**
     * 根据配置发送告警通知
     */
    private void sendAlertIfNeeded(DqcPlan plan, DqcExecution execution, int passed, int failed) {
        boolean hasFailure = failed > 0;
        boolean allSuccess = failed == 0 && !Boolean.TRUE.equals(execution.getBlocked());

        // 执行失败且配置了失败告警
        if (hasFailure && Boolean.TRUE.equals(plan.getAlertOnFailure())) {
            log.info("触发失败告警: executionNo={}, failed={}", execution.getExecutionNo(), failed);
            // TODO: 集成告警服务发送通知
        }

        // 全部成功且配置了成功告警
        if (allSuccess && Boolean.TRUE.equals(plan.getAlertOnSuccess())) {
            log.info("触发成功通知: executionNo={}", execution.getExecutionNo());
            // TODO: 集成告警服务发送通知
        }
    }

    @Override
    public Result<Page<DqcExecution>> page(Integer pageNum, Integer pageSize, Long planId,
                                            String status, String triggerType, String startDate, String endDate) {
        Page<DqcExecution> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DqcExecution> wrapper = new LambdaQueryWrapper<>();

        if (planId != null) {
            wrapper.eq(DqcExecution::getPlanId, planId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(DqcExecution::getStatus, status);
        }
        if (StringUtils.hasText(triggerType)) {
            wrapper.eq(DqcExecution::getTriggerType, triggerType);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(DqcExecution::getCreateTime, LocalDateTime.parse(startDate + " 00:00:00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(DqcExecution::getCreateTime, LocalDateTime.parse(endDate + " 23:59:59",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        wrapper.orderByDesc(DqcExecution::getCreateTime);
        Page<DqcExecution> result = this.page(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<DqcExecution> getById(Long id) {
        DqcExecution entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "执行记录不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<DqcExecution> getByExecutionNo(String executionNo) {
        LambdaQueryWrapper<DqcExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcExecution::getExecutionNo, executionNo);
        DqcExecution entity = this.getOne(wrapper);
        if (entity == null) {
            throw new BusinessException(404, "执行记录不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<List<DqcExecutionDetail>> getExecutionDetails(Long executionId) {
        LambdaQueryWrapper<DqcExecutionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcExecutionDetail::getExecutionId, executionId);
        wrapper.orderByAsc(DqcExecutionDetail::getId);
        List<DqcExecutionDetail> details = detailMapper.selectList(wrapper);
        return Result.success(details);
    }

    @Override
    public Result<DqcExecutionDetail> getDetailById(Long id) {
        DqcExecutionDetail detail = detailMapper.selectById(id);
        if (detail == null) {
            throw new BusinessException(404, "执行明细不存在");
        }
        return Result.success(detail);
    }

    @Override
    public Result<QualityReportVO> getReport(Long planId, String startDate, String endDate) {
        LambdaQueryWrapper<DqcExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcExecution::getPlanId, planId);
        wrapper.eq(DqcExecution::getStatus, "SUCCESS");
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(DqcExecution::getCreateTime, LocalDateTime.parse(startDate + " 00:00:00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(DqcExecution::getCreateTime, LocalDateTime.parse(endDate + " 23:59:59",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        wrapper.orderByDesc(DqcExecution::getCreateTime);
        List<DqcExecution> executions = this.list(wrapper);

        return Result.success(buildReport(executions));
    }

    @Override
    public Result<QualityReportVO> getOverallReport() {
        LambdaQueryWrapper<DqcExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcExecution::getStatus, "SUCCESS");
        wrapper.orderByDesc(DqcExecution::getCreateTime);
        List<DqcExecution> executions = this.list(wrapper);

        return Result.success(buildReport(executions));
    }

    private QualityReportVO buildReport(List<DqcExecution> executions) {
        if (executions.isEmpty()) {
            return QualityReportVO.builder()
                    .overallScore(BigDecimal.ZERO)
                    .rulePassRate(BigDecimal.ZERO)
                    .totalRules(0)
                    .passedRules(0)
                    .failedRules(0)
                    .skippedRules(0)
                    .dimensionScores(QualityReportVO.DimensionScoresReport.builder()
                            .completeness(BigDecimal.valueOf(100))
                            .uniqueness(BigDecimal.valueOf(100))
                            .accuracy(BigDecimal.valueOf(100))
                            .consistency(BigDecimal.valueOf(100))
                            .timeliness(BigDecimal.valueOf(100))
                            .validity(BigDecimal.valueOf(100))
                            .build())
                    .scoreTrend(new ArrayList<>())
                    .topFailedRules(new ArrayList<>())
                    .recentExecutions(new ArrayList<>())
                    .build();
        }

        int totalRules = executions.stream().mapToInt(DqcExecution::getTotalRules).sum();
        int passedRules = executions.stream().mapToInt(DqcExecution::getPassedRules).sum();
        int failedRules = executions.stream().mapToInt(DqcExecution::getFailedRules).sum();
        int skippedRules = executions.stream().mapToInt(DqcExecution::getSkippedRules).sum();

        double avgScore = executions.stream()
                .filter(e -> e.getQualityScore() != null)
                .mapToInt(DqcExecution::getQualityScore)
                .average()
                .orElse(0.0);

        double passRate = totalRules > 0 ? (double) passedRules / totalRules * 100 : 0;

        // Real dimension scores aggregated from execution details
        Map<String, int[]> dimStat = new HashMap<>();
        dimStat.put("COMPLETENESS", new int[]{0, 0});
        dimStat.put("UNIQUENESS", new int[]{0, 0});
        dimStat.put("ACCURACY", new int[]{0, 0});
        dimStat.put("CONSISTENCY", new int[]{0, 0});
        dimStat.put("TIMELINESS", new int[]{0, 0});
        dimStat.put("VALIDITY", new int[]{0, 0});
        for (DqcExecution exec : executions) {
            LambdaQueryWrapper<DqcExecutionDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(DqcExecutionDetail::getExecutionId, exec.getId());
            List<DqcExecutionDetail> details = detailMapper.selectList(detailWrapper);
            for (DqcExecutionDetail detail : details) {
                Set<String> dims = parseExecutionDimensions(detail.getDimensions());
                for (String dim : dims) {
                    int[] stat = dimStat.get(dim.toUpperCase());
                    if (stat != null) {
                        stat[1]++; // total
                        if ("SUCCESS".equals(detail.getStatus())) stat[0]++; // passed
                    }
                }
            }
        }
        QualityReportVO.DimensionScoresReport dimensionScores = QualityReportVO.DimensionScoresReport.builder()
                .completeness(calcDimScore(dimStat.get("COMPLETENESS")))
                .uniqueness(calcDimScore(dimStat.get("UNIQUENESS")))
                .accuracy(calcDimScore(dimStat.get("ACCURACY")))
                .consistency(calcDimScore(dimStat.get("CONSISTENCY")))
                .timeliness(calcDimScore(dimStat.get("TIMELINESS")))
                .validity(calcDimScore(dimStat.get("VALIDITY")))
                .build();

        List<QualityReportVO.ScoreTrendPoint> scoreTrend = executions.stream()
                .limit(30)
                .map(e -> QualityReportVO.ScoreTrendPoint.builder()
                        .date(e.getCreateTime().toLocalDate().toString())
                        .score(BigDecimal.valueOf(e.getQualityScore() != null ? e.getQualityScore() : 0))
                        .totalCount(e.getTotalRules())
                        .passedCount(e.getPassedRules())
                        .failedCount(e.getFailedRules())
                        .overallScore(BigDecimal.valueOf(e.getQualityScore() != null ? e.getQualityScore() : 0))
                        .build())
                .collect(Collectors.toList());
        Collections.reverse(scoreTrend);

        List<QualityReportVO.FailedRuleInfo> topFailedRules = new ArrayList<>();
        Map<String, Integer> ruleErrorCounts = new HashMap<>();
        for (DqcExecution exec : executions) {
            LambdaQueryWrapper<DqcExecutionDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(DqcExecutionDetail::getExecutionId, exec.getId());
            detailWrapper.eq(DqcExecutionDetail::getStatus, "FAILED");
            List<DqcExecutionDetail> failedDetails = detailMapper.selectList(detailWrapper);
            for (DqcExecutionDetail detail : failedDetails) {
                ruleErrorCounts.merge(detail.getRuleName(), 1, Integer::sum);
            }
        }
        topFailedRules = ruleErrorCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(entry -> QualityReportVO.FailedRuleInfo.builder()
                        .ruleName(entry.getKey())
                        .errorCount(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        List<QualityReportVO.ExecutionSummary> recentExecutions = executions.stream()
                .limit(10)
                .map(e -> QualityReportVO.ExecutionSummary.builder()
                        .executionId(e.getId())
                        .executionNo(e.getExecutionNo())
                        .status(e.getStatus())
                        .startTime(e.getStartTime() != null ? e.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                        .elapsedMs(e.getElapsedMs())
                        .totalRules(e.getTotalRules())
                        .passedRules(e.getPassedRules())
                        .failedRules(e.getFailedRules())
                        .qualityScore(e.getQualityScore())
                        .blocked(e.getBlocked())
                        .build())
                .collect(Collectors.toList());

        return QualityReportVO.builder()
                .overallScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP))
                .rulePassRate(BigDecimal.valueOf(passRate).setScale(2, RoundingMode.HALF_UP))
                .totalRules(totalRules)
                .passedRules(passedRules)
                .failedRules(failedRules)
                .skippedRules(skippedRules)
                .dimensionScores(dimensionScores)
                .scoreTrend(scoreTrend)
                .topFailedRules(topFailedRules)
                .recentExecutions(recentExecutions)
                .build();
    }

    @Override
    public Result<byte[]> exportReport(Long executionId) {
        LambdaQueryWrapper<DqcExecutionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcExecutionDetail::getExecutionId, executionId);
        wrapper.orderByAsc(DqcExecutionDetail::getId);
        List<DqcExecutionDetail> details = detailMapper.selectList(wrapper);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            EasyExcel.write(out)
                    .sheet("质检明细")
                    .head(DqcExecutionDetail.class)
                    .doWrite(details);
            return Result.success(out.toByteArray());
        } catch (Exception e) {
            log.error("导出报告失败", e);
            throw new BusinessException(500, "导出报告失败: " + e.getMessage());
        }
    }

    private Set<String> parseExecutionDimensions(String dimensions) {
        Set<String> result = new HashSet<>();
        if (dimensions == null || dimensions.isEmpty()) {
            result.add("COMPLETENESS");
            return result;
        }
        try {
            JSONArray array = JSON.parseArray(dimensions);
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    result.add(array.getString(i).toUpperCase());
                }
            }
        } catch (Exception e) {
            result.add(dimensions.toUpperCase());
        }
        if (result.isEmpty()) {
            result.add("COMPLETENESS");
        }
        return result;
    }

    private BigDecimal calcDimScore(int[] stat) {
        if (stat == null || stat[1] == 0) return BigDecimal.valueOf(0);
        return BigDecimal.valueOf((double) stat[0] / stat[1] * 100)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 阶段三-T3: 质检执行明细关联敏感字段信息
     * <p>
     * 通过 (targetDsId, targetTable, targetColumn) JOIN sec_column_sensitivity
     * 回填 sensitivityLevel 和 sensitivityClass。
     * 优先取已 APPROVED 的记录，其次取最新扫描结果。
     */
    private void enrichSensitivityInfo(DqcExecutionDetail detail) {
        if (detail.getTargetDsId() == null || detail.getTargetTable() == null) {
            return;
        }

        try {
            // 优先查询已审核通过的敏感字段记录
            LambdaQueryWrapper<SecColumnSensitivity> approvedWrapper = new LambdaQueryWrapper<>();
            approvedWrapper.eq(SecColumnSensitivity::getDsId, detail.getTargetDsId())
                    .eq(SecColumnSensitivity::getTableName, detail.getTargetTable());
            if (StringUtils.hasText(detail.getTargetColumn())) {
                approvedWrapper.eq(SecColumnSensitivity::getColumnName, detail.getTargetColumn());
            }
            approvedWrapper.eq(SecColumnSensitivity::getReviewStatus, "APPROVED").last("LIMIT 1");
            SecColumnSensitivity approved = secColumnSensitivityMapper.selectOne(approvedWrapper);

            SecColumnSensitivity record = approved;
            if (record == null) {
                // 其次取最新扫描结果
                LambdaQueryWrapper<SecColumnSensitivity> pendingWrapper = new LambdaQueryWrapper<>();
                pendingWrapper.eq(SecColumnSensitivity::getDsId, detail.getTargetDsId())
                        .eq(SecColumnSensitivity::getTableName, detail.getTargetTable());
                if (StringUtils.hasText(detail.getTargetColumn())) {
                    pendingWrapper.eq(SecColumnSensitivity::getColumnName, detail.getTargetColumn());
                }
                pendingWrapper.orderByDesc(SecColumnSensitivity::getScanTime).last("LIMIT 1");
                record = secColumnSensitivityMapper.selectOne(pendingWrapper);
            }

            if (record != null && record.getLevelId() != null) {
                SecLevel level = secLevelMapper.selectById(record.getLevelId());
                if (level != null && level.getLevelCode() != null) {
                    detail.setSensitivityLevel(level.getLevelCode());
                    detail.setSensitivityClass(level.getLevelName());
                    log.debug("执行明细 {} 回填敏感等级: {}/{}", detail.getId(), level.getLevelCode(), level.getLevelName());
                }
            }
        } catch (Exception e) {
            // 敏感字段关联失败不影响执行主流程
            log.warn("质检明细敏感字段关联失败: detailId={}, 错误: {}", detail.getId(), e.getMessage());
        }
    }
}
