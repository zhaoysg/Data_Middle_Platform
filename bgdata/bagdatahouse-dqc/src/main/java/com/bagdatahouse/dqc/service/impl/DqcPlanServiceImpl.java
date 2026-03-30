package com.bagdatahouse.dqc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqcPlanDTO;
import com.bagdatahouse.core.entity.*;
import com.bagdatahouse.core.mapper.*;
import com.bagdatahouse.core.vo.PlanRuleBindVO;
import com.bagdatahouse.core.vo.PlanExecutionStatusVO;
import com.bagdatahouse.core.vo.CronValidationVO;
import com.bagdatahouse.dqc.config.DqcScheduledJob;
import com.bagdatahouse.dqc.enums.PlanStatusEnum;
import com.bagdatahouse.dqc.enums.TriggerTypeEnum;
import com.bagdatahouse.dqc.service.DqcPlanService;
import com.bagdatahouse.dqc.service.DqcExecutionService;
import com.bagdatahouse.core.mapper.GovMetadataMapper;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DQC质检方案服务实现
 */
@Service
public class DqcPlanServiceImpl extends ServiceImpl<DqcPlanMapper, DqcPlan>
        implements DqcPlanService {

    private static final Logger log = LoggerFactory.getLogger(DqcPlanServiceImpl.class);

    @Autowired
    private DqcPlanRuleMapper planRuleMapper;

    @Autowired
    private DqcRuleDefMapper ruleDefMapper;

    @Autowired
    private DqcExecutionMapper executionMapper;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private DqcExecutionService executionService;

    @Autowired
    private DqDataLayerMapper dataLayerMapper;

    @Autowired
    private DqcScheduledJob scheduledJob;

    @Autowired
    private DqcExecutionMapper dqcExecutionMapper;

    /** 用于查询元数据表数量（按层绑定场景） */
    @Autowired
    @Lazy
    private GovMetadataMapper govMetadataMapper;

    @Autowired
    @Lazy
    private DataSourceAdapterRegistry adapterRegistry;

    @Override
    public Result<Page<DqcPlan>> page(Integer pageNum, Integer pageSize, String planName,
                                        String layerCode, String status, String triggerType) {
        Page<DqcPlan> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DqcPlan> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(planName)) {
            wrapper.like(DqcPlan::getPlanName, planName);
        }
        if (StringUtils.hasText(layerCode)) {
            wrapper.eq(DqcPlan::getLayerCode, layerCode);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(DqcPlan::getStatus, status);
        }
        if (StringUtils.hasText(triggerType)) {
            wrapper.eq(DqcPlan::getTriggerType, triggerType);
        }

        wrapper.orderByDesc(DqcPlan::getCreateTime);
        Page<DqcPlan> result = this.page(page, wrapper);

        // Enrich with display names
        enrichDisplayNames(result.getRecords());

        return Result.success(result);
    }

    @Override
    public Result<DqcPlan> getById(Long id) {
        DqcPlan entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }
        enrichDisplayNames(List.of(entity));
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> create(DqcPlanDTO dto) {
        // Validate required fields
        if (!StringUtils.hasText(dto.getPlanCode())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "方案编码不能为空");
        }
        if (!StringUtils.hasText(dto.getPlanName())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "方案名称不能为空");
        }

        // Check code uniqueness
        LambdaQueryWrapper<DqcPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcPlan::getPlanCode, dto.getPlanCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "方案编码已存在");
        }

        DqcPlan entity = toEntity(dto);
        entity.setCreateUser(getCurrentUserId());
        entity.setCreateTime(LocalDateTime.now());
        entity.setStatus(PlanStatusEnum.DRAFT.getCode());
        entity.setRuleCount(0);
        entity.setTableCount(0);
        this.save(entity);

        log.info("Created plan: {} (id={}, code={})", entity.getPlanName(), entity.getId(), entity.getPlanCode());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, DqcPlanDTO dto) {
        DqcPlan existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }

        // Check code uniqueness if changed
        if (StringUtils.hasText(dto.getPlanCode()) && !dto.getPlanCode().equals(existing.getPlanCode())) {
            LambdaQueryWrapper<DqcPlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DqcPlan::getPlanCode, dto.getPlanCode());
            wrapper.ne(DqcPlan::getId, id);
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "方案编码已存在");
            }
        }

        DqcPlan entity = toEntity(dto);
        entity.setId(id);
        entity.setUpdateUser(getCurrentUserId());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setCreateUser(existing.getCreateUser());
        entity.setCreateTime(existing.getCreateTime());
        // Keep status unchanged on update
        entity.setStatus(existing.getStatus());
        this.updateById(entity);

        log.info("Updated plan: {} (id={})", entity.getPlanName(), id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        DqcPlan existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }

        // Delete associated plan-rule bindings
        LambdaQueryWrapper<DqcPlanRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(DqcPlanRule::getPlanId, id);
        planRuleMapper.delete(ruleWrapper);

        this.removeById(id);
        log.info("Deleted plan: {} (id={})", existing.getPlanName(), id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> publish(Long id) {
        DqcPlan entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }

        // Validate that plan has at least one bound rule before publishing
        LambdaQueryWrapper<DqcPlanRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcPlanRule::getPlanId, id);
        wrapper.eq(DqcPlanRule::getEnabled, true);
        long ruleCount = planRuleMapper.selectCount(wrapper);
        if (ruleCount == 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "方案未绑定任何规则，无法发布");
        }

        LambdaUpdateWrapper<DqcPlan> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DqcPlan::getId, id);
        updateWrapper.set(DqcPlan::getStatus, PlanStatusEnum.PUBLISHED.getCode());
        updateWrapper.set(DqcPlan::getUpdateTime, LocalDateTime.now());
        this.update(updateWrapper);

        log.info("Published plan: id={}, name={}", id, entity.getPlanName());
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disable(Long id) {
        DqcPlan entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }

        LambdaUpdateWrapper<DqcPlan> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DqcPlan::getId, id);
        wrapper.set(DqcPlan::getStatus, PlanStatusEnum.DISABLED.getCode());
        wrapper.set(DqcPlan::getUpdateTime, LocalDateTime.now());
        this.update(wrapper);

        log.info("Disabled plan: id={}, name={}", id, entity.getPlanName());
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> execute(Long id) {
        DqcPlan plan = baseMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }

        // Check if plan is published
        if (!PlanStatusEnum.isExecutable(plan.getStatus())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST,
                    "方案状态为【" + PlanStatusEnum.getNameByCode(plan.getStatus()) + "】，无法执行");
        }

        // Execute via execution service
        Result<com.bagdatahouse.core.vo.ExecutionTriggerVO> execResult =
                executionService.execute(id, TriggerTypeEnum.MANUAL.getCode(), getCurrentUserId());
        return Result.success(execResult.getData().getExecutionId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> bindRules(Long planId, List<PlanRuleBindVO> rules) {
        DqcPlan plan = baseMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }

        // Validate all ruleIds exist before any write (fail-fast)
        if (rules == null || rules.isEmpty()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "绑定规则列表不能为空");
        }
        for (PlanRuleBindVO rule : rules) {
            if (rule.getRuleId() == null) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "规则ID不能为空");
            }
            DqcRuleDef ruleDef = ruleDefMapper.selectById(rule.getRuleId());
            if (ruleDef == null) {
                throw new BusinessException(ResponseCode.RULE_NOT_FOUND,
                        "绑定的规则不存在: ruleId=" + rule.getRuleId());
            }
        }

        // Delete existing bindings
        LambdaQueryWrapper<DqcPlanRule> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(DqcPlanRule::getPlanId, planId);
        planRuleMapper.delete(deleteWrapper);

        // Insert new bindings
        for (int i = 0; i < rules.size(); i++) {
            PlanRuleBindVO rule = rules.get(i);
            DqcPlanRule planRule = DqcPlanRule.builder()
                    .planId(planId)
                    .ruleId(rule.getRuleId())
                    .ruleOrder(rule.getRuleOrder() != null ? rule.getRuleOrder() : i + 1)
                    .customThreshold(rule.getCustomThreshold())
                    .targetTable(StringUtils.hasText(rule.getTargetTable()) ? rule.getTargetTable() : null)
                    .targetColumn(StringUtils.hasText(rule.getTargetColumn()) ? rule.getTargetColumn() : null)
                    .skipOnFailure(rule.getSkipOnFailure())
                    .enabled(rule.getEnabled() != null ? rule.getEnabled() : true)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            planRuleMapper.insert(planRule);
        }

        // Update plan rule count and table count
        int tableCount = 0;
        if (plan.getBindValue() != null && "TABLE".equalsIgnoreCase(plan.getBindType())) {
            try {
                com.alibaba.fastjson2.JSONObject json =
                        com.alibaba.fastjson2.JSON.parseObject(plan.getBindValue());
                if (json != null && json.containsKey("tables")) {
                    com.alibaba.fastjson2.JSONArray tables = json.getJSONArray("tables");
                    if (tables != null) {
                        tableCount = tables.size();
                    }
                }
            } catch (Exception e) {
                log.warn("解析方案绑定配置计算表数量失败: planId={}", planId, e);
            }
        } else if (plan.getBindValue() != null && "LAYER".equalsIgnoreCase(plan.getBindType())) {
            try {
                com.alibaba.fastjson2.JSONObject json =
                        com.alibaba.fastjson2.JSON.parseObject(plan.getBindValue());
                if (json != null && json.containsKey("dsId") && json.containsKey("layer")) {
                    Long dsId = json.getLong("dsId");
                    String layer = json.getString("layer");
                    String pgSchema = json.containsKey("schema") ? json.getString("schema") : null;
                    if (dsId != null && layer != null && !layer.isBlank()) {
                        tableCount = countLayerMatchedTables(dsId, layer, pgSchema);
                    }
                }
            } catch (Exception e) {
                log.warn("按层绑定时查询元数据计算表数量失败: planId={}", planId, e);
            }
        }

        LambdaUpdateWrapper<DqcPlan> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DqcPlan::getId, planId);
        updateWrapper.set(DqcPlan::getRuleCount, rules.size());
        updateWrapper.set(DqcPlan::getTableCount, tableCount);
        updateWrapper.set(DqcPlan::getUpdateTime, LocalDateTime.now());
        this.update(updateWrapper);

        log.info("Bound {} rules to plan: id={}, tableCount={}", rules.size(), planId, tableCount);
        return Result.success();
    }

    @Override
    public Result<List<PlanRuleBindVO>> getBoundRules(Long planId) {
        LambdaQueryWrapper<DqcPlanRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcPlanRule::getPlanId, planId);
        wrapper.orderByAsc(DqcPlanRule::getRuleOrder);
        List<DqcPlanRule> planRules = planRuleMapper.selectList(wrapper);

        if (planRules.isEmpty()) {
            return Result.success(List.of());
        }

        List<Long> ruleIds = planRules.stream()
                .map(DqcPlanRule::getRuleId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<DqcRuleDef> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.in(DqcRuleDef::getId, ruleIds);
        List<DqcRuleDef> ruleDefs = ruleDefMapper.selectList(ruleWrapper);
        Map<Long, DqcRuleDef> ruleDefMap = ruleDefs.stream()
                .collect(Collectors.toMap(DqcRuleDef::getId, r -> r));

        List<PlanRuleBindVO> result = planRules.stream().map(planRule -> {
            DqcRuleDef ruleDef = ruleDefMap.get(planRule.getRuleId());
            return PlanRuleBindVO.builder()
                    .planRuleId(planRule.getId())
                    .ruleId(planRule.getRuleId())
                    .ruleName(ruleDef != null ? ruleDef.getRuleName() : null)
                    .ruleCode(ruleDef != null ? ruleDef.getRuleCode() : null)
                    .ruleType(ruleDef != null ? ruleDef.getRuleType() : null)
                    .ruleStrength(ruleDef != null ? ruleDef.getRuleStrength() : null)
                    .ruleOrder(planRule.getRuleOrder())
                    .customThreshold(planRule.getCustomThreshold())
                    .skipOnFailure(planRule.getSkipOnFailure())
                    .enabled(planRule.getEnabled())
                    .thresholdMin(ruleDef != null ? ruleDef.getThresholdMin() : null)
                    .thresholdMax(ruleDef != null ? ruleDef.getThresholdMax() : null)
                    .fluctuationThreshold(ruleDef != null ? ruleDef.getFluctuationThreshold() : null)
                    .targetTable(planRule.getTargetTable())
                    .targetColumn(planRule.getTargetColumn())
                    .build();
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unbindRule(Long planId, Long ruleId) {
        LambdaQueryWrapper<DqcPlanRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcPlanRule::getPlanId, planId);
        wrapper.eq(DqcPlanRule::getRuleId, ruleId);
        planRuleMapper.delete(wrapper);

        long remainingCount = planRuleMapper.selectCount(
                new LambdaQueryWrapper<DqcPlanRule>().eq(DqcPlanRule::getPlanId, planId));

        LambdaUpdateWrapper<DqcPlan> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DqcPlan::getId, planId);
        updateWrapper.set(DqcPlan::getRuleCount, (int) remainingCount);
        updateWrapper.set(DqcPlan::getUpdateTime, LocalDateTime.now());
        this.update(updateWrapper);

        log.info("Unbound rule {} from plan: id={}", ruleId, planId);
        return Result.success();
    }

    @Override
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new HashMap<>();

        long totalPlans = this.count();
        long publishedPlans = this.count(
                new LambdaQueryWrapper<DqcPlan>().eq(DqcPlan::getStatus, PlanStatusEnum.PUBLISHED.getCode()));
        long draftPlans = this.count(
                new LambdaQueryWrapper<DqcPlan>().eq(DqcPlan::getStatus, PlanStatusEnum.DRAFT.getCode()));
        long disabledPlans = this.count(
                new LambdaQueryWrapper<DqcPlan>().eq(DqcPlan::getStatus, PlanStatusEnum.DISABLED.getCode()));
        long totalRules = ruleDefMapper.selectCount(null);
        long totalExecutions = executionMapper.selectCount(null);

        LambdaQueryWrapper<DqcExecution> executionWrapper = new LambdaQueryWrapper<>();
        executionWrapper.apply("DATE(create_time) = CURDATE()");
        long todayExecutions = executionMapper.selectCount(executionWrapper);

        LambdaQueryWrapper<DqcExecution> avgScoreWrapper = new LambdaQueryWrapper<>();
        avgScoreWrapper.isNotNull(DqcExecution::getQualityScore);
        List<DqcExecution> executionsWithScore = executionMapper.selectList(avgScoreWrapper);
        double avgScore = executionsWithScore.stream()
                .filter(e -> e.getQualityScore() != null)
                .mapToInt(DqcExecution::getQualityScore)
                .average()
                .orElse(0.0);

        overview.put("totalPlans", totalPlans);
        overview.put("publishedPlans", publishedPlans);
        overview.put("draftPlans", draftPlans);
        overview.put("disabledPlans", disabledPlans);
        overview.put("totalRules", totalRules);
        overview.put("totalExecutions", totalExecutions);
        overview.put("avgScore", Math.round(avgScore * 100.0) / 100.0);
        overview.put("todayExecutions", todayExecutions);

        return Result.success(overview);
    }

    // ==================== 新增：支持三种触发方式的方法实现 ====================

    /**
     * API触发执行质检方案
     */
    @Override
    public Result<Long> triggerByApi(Long planId, String triggerParams) {
        DqcPlan plan = baseMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }

        // 检查方案是否可执行
        if (!PlanStatusEnum.isExecutable(plan.getStatus())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST,
                    "方案状态为【" + PlanStatusEnum.getNameByCode(plan.getStatus()) + "】，无法执行");
        }

        // 通过执行服务触发
        Result<com.bagdatahouse.core.vo.ExecutionTriggerVO> execResult =
                executionService.execute(planId, TriggerTypeEnum.API.getCode(), getCurrentUserId());

        Long executionId = execResult.getData().getExecutionId();

        // 如果传入了触发参数，更新执行记录的触发参数
        if (StringUtils.hasText(triggerParams) && executionId != null) {
            DqcExecution execution = dqcExecutionMapper.selectById(executionId);
            if (execution != null) {
                execution.setTriggerParams(triggerParams);
                dqcExecutionMapper.updateById(execution);
            }
        }

        log.info("[API触发] 执行方案: planId={}, planName={}, executionId={}",
                planId, plan.getPlanName(), executionId);

        return Result.success(executionId);
    }

    /**
     * 获取方案的下次执行时间
     */
    @Override
    public Result<LocalDateTime> getNextExecutionTime(Long planId) {
        DqcPlan plan = baseMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }

        if (!"SCHEDULE".equals(plan.getTriggerType())) {
            return Result.success(null);
        }

        LocalDateTime nextTime = scheduledJob.calculateNextExecutionTime(plan.getTriggerCron());
        return Result.success(nextTime);
    }

    /**
     * 获取方案的执行状态
     */
    @Override
    public Result<PlanExecutionStatusVO> getExecutionStatus(Long planId) {
        DqcPlan plan = baseMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }

        // 获取定时调度状态
        DqcScheduledJob.PlanExecutionStatus schedulerStatus = scheduledJob.getPlanExecutionStatus(planId);

        // 获取最近一次执行记录
        LambdaQueryWrapper<DqcExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcExecution::getPlanId, planId)
                .orderByDesc(DqcExecution::getCreateTime)
                .last("LIMIT 1");
        DqcExecution lastExecution = dqcExecutionMapper.selectOne(wrapper);

        // 计算下次执行时间
        LocalDateTime nextExecutionTime = null;
        Long secondsToNext = null;
        if ("SCHEDULE".equals(plan.getTriggerType()) && StringUtils.hasText(plan.getTriggerCron())) {
            nextExecutionTime = scheduledJob.calculateNextExecutionTime(plan.getTriggerCron());
            if (nextExecutionTime != null) {
                secondsToNext = Duration.between(LocalDateTime.now(), nextExecutionTime).getSeconds();
            }
        }

        // Cron有效性
        boolean cronValid = false;
        if (StringUtils.hasText(plan.getTriggerCron())) {
            cronValid = scheduledJob.validateCronExpression(plan.getTriggerCron());
        }

        PlanExecutionStatusVO statusVO = PlanExecutionStatusVO.builder()
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .planCode(plan.getPlanCode())
                .triggerType(plan.getTriggerType())
                .isRunning(schedulerStatus != null && schedulerStatus.isRunning())
                .lastExecutionId(lastExecution != null ? lastExecution.getId() : null)
                .lastExecutionNo(lastExecution != null ? lastExecution.getExecutionNo() : null)
                .lastExecutionStatus(lastExecution != null ? lastExecution.getStatus() : null)
                .lastExecutionStartTime(lastExecution != null ? lastExecution.getStartTime() : null)
                .lastExecutionEndTime(lastExecution != null ? lastExecution.getEndTime() : null)
                .lastExecutionElapsedMs(lastExecution != null ? lastExecution.getElapsedMs() : null)
                .lastQualityScore(lastExecution != null ? lastExecution.getQualityScore() : null)
                .lastTotalRules(lastExecution != null ? lastExecution.getTotalRules() : null)
                .lastPassedRules(lastExecution != null ? lastExecution.getPassedRules() : null)
                .lastFailedRules(lastExecution != null ? lastExecution.getFailedRules() : null)
                .nextExecutionTime(nextExecutionTime)
                .triggerCron(plan.getTriggerCron())
                .cronValid(cronValid)
                .secondsToNextExecution(secondsToNext)
                .status(plan.getStatus())
                .statusName(PlanStatusEnum.getNameByCode(plan.getStatus()))
                .build();

        return Result.success(statusVO);
    }

    /**
     * 验证Cron表达式
     */
    @Override
    public Result<CronValidationVO> validateCron(String cronExpression) {
        if (!StringUtils.hasText(cronExpression)) {
            return Result.success(CronValidationVO.builder()
                    .cronExpression(cronExpression)
                    .valid(false)
                    .errorMessage("Cron表达式不能为空")
                    .build());
        }

        boolean valid = scheduledJob.validateCronExpression(cronExpression);
        String errorMessage = null;
        LocalDateTime nextTime = null;
        List<LocalDateTime> next10Times = new ArrayList<>();

        if (valid) {
            nextTime = scheduledJob.calculateNextExecutionTime(cronExpression);

            // 计算未来10次执行时间
            try {
                com.cronutils.model.Cron cron = new com.cronutils.parser.CronParser(
                        com.cronutils.model.definition.CronDefinitionBuilder.instanceDefinitionFor(
                                com.cronutils.model.CronType.QUARTZ))
                        .parse(cronExpression);
                com.cronutils.model.time.ExecutionTime executionTime =
                        com.cronutils.model.time.ExecutionTime.forCron(cron);
                ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
                for (int i = 0; i < 10; i++) {
                    Optional<ZonedDateTime> next = executionTime.nextExecution(now);
                    if (next.isPresent()) {
                        next10Times.add(next.get().toLocalDateTime());
                        now = next.get();
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("计算未来执行时间失败: cron={}", cronExpression, e);
            }
        } else {
            errorMessage = "Cron表达式格式错误";
        }

        String description = describeCronExpression(cronExpression);
        String triggerTypeDesc = getTriggerTypeDescription(cronExpression);

        return Result.success(CronValidationVO.builder()
                .cronExpression(cronExpression)
                .valid(valid)
                .errorMessage(errorMessage)
                .nextExecutionTime(nextTime)
                .next10Executions(next10Times)
                .description(description)
                .triggerTypeDescription(triggerTypeDesc)
                .build());
    }

    /**
     * 取消正在执行的方案
     */
    @Override
    public Result<Boolean> cancelExecution(Long planId) {
        DqcPlan plan = baseMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ResponseCode.PLAN_NOT_FOUND);
        }

        // TODO: 实现取消执行逻辑
        // 目前通过并发锁机制防止重复执行
        log.info("[取消执行] 收到取消请求: planId={}", planId);

        return Result.success(true);
    }

    // ==================== 私有方法 ====================

    /**
     * 描述Cron表达式（人类可读）
     */
    private String describeCronExpression(String cron) {
        if (cron == null || cron.isBlank()) {
            return "未配置";
        }

        // 简单的描述生成
        String[] parts = cron.split(" ");
        if (parts.length >= 5) {
            StringBuilder desc = new StringBuilder();

            // 分钟
            if (parts[0].equals("0")) {
                desc.append("每小时的整点");
            } else if (parts[0].contains("*/")) {
                desc.append("每").append(parts[0].substring(2)).append("分钟");
            } else {
                desc.append("第").append(parts[0]).append("分钟");
            }

            // 小时
            if (!parts[1].equals("*")) {
                if (parts[1].contains("*/")) {
                    desc.append("，每").append(parts[1].substring(2)).append("小时");
                } else {
                    desc.append("，第").append(parts[1]).append("点");
                }
            }

            // 日期
            if (!parts[2].equals("*") && !parts[2].equals("?")) {
                desc.append("，每月").append(parts[2]).append("日");
            }

            // 月份
            if (!parts[3].equals("*")) {
                desc.append("，每月").append(parts[3]).append("月");
            }

            // 星期
            if (!parts[4].equals("*") && !parts[4].equals("?")) {
                desc.append("，每").append(getDayOfWeekName(parts[4]));
            }

            return desc.toString();
        }

        return cron;
    }

    private String getDayOfWeekName(String day) {
        return switch (day) {
            case "1" -> "周一";
            case "2" -> "周二";
            case "3" -> "周三";
            case "4" -> "周四";
            case "5" -> "周五";
            case "6" -> "周六";
            case "7", "0" -> "周日";
            default -> "第" + day + "天";
        };
    }

    private String getTriggerTypeDescription(String cron) {
        if (cron == null || cron.isBlank()) {
            return "未配置定时";
        }

        String[] parts = cron.split(" ");
        if (parts.length >= 1) {
            // 每5分钟
            if (parts[0].equals("0") && parts[1].equals("*/5")) {
                return "每5分钟执行";
            }
            // 每小时
            if (parts[0].equals("0") && parts[1].equals("*")) {
                return "每小时执行";
            }
            // 每天
            if (parts[0].equals("0") && parts[1].equals("0") && parts[2].equals("*")) {
                return "每天凌晨执行";
            }
        }

        return "按自定义Cron执行";
    }

    private void enrichDisplayNames(List<DqcPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }

        // Batch load data layers
        List<String> layerCodes = plans.stream()
                .map(DqcPlan::getLayerCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        Map<String, DqDataLayer> layerMap = new HashMap<>();
        if (!layerCodes.isEmpty()) {
            LambdaQueryWrapper<DqDataLayer> layerWrapper = new LambdaQueryWrapper<>();
            layerWrapper.in(DqDataLayer::getLayerCode, layerCodes);
            dataLayerMapper.selectList(layerWrapper).forEach(l -> layerMap.put(l.getLayerCode(), l));
        }

        // Batch load departments
        List<Long> deptIds = plans.stream()
                .map(DqcPlan::getDeptId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, SysDept> deptMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            deptMapper.selectBatchIds(deptIds).forEach(d -> deptMap.put(d.getId(), d));
        }

        // Batch load creators
        List<Long> userIds = plans.stream()
                .map(DqcPlan::getCreateUser)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }

        for (DqcPlan plan : plans) {
            // Status name
            plan.setStatusName(PlanStatusEnum.getNameByCode(plan.getStatus()));
            // Trigger type name
            plan.setTriggerTypeName(TriggerTypeEnum.getNameByCode(plan.getTriggerType()));
            // Data layer name
            if (plan.getLayerCode() != null && !plan.getLayerCode().isBlank()) {
                DqDataLayer layer = layerMap.get(plan.getLayerCode());
                if (layer != null) {
                    plan.setLayerName(layer.getLayerName());
                }
            }
            // Department name
            if (plan.getDeptId() != null) {
                SysDept dept = deptMap.get(plan.getDeptId());
                if (dept != null) {
                    plan.setDeptName(dept.getDeptName());
                }
            }
            // Creator name
            if (plan.getCreateUser() != null) {
                SysUser user = userMap.get(plan.getCreateUser());
                if (user != null) {
                    plan.setCreateUserName(user.getUsername());
                }
            }
            // Last execution status
            if (plan.getLastExecutionScore() != null) {
                plan.setLastExecutionStatus(getScoreStatus(plan.getLastExecutionScore()));
            }
        }
    }

    /**
     * 按数据层统计匹配表数。PostgreSQL 若 bindValue 含 schema，则与「该 schema 下物理表」求交集，
     * 避免把其他 schema 下同 dataLayer 标签的表算进来。
     */
    private int countLayerMatchedTables(Long dsId, String dataLayer, String pgSchema) {
        LambdaQueryWrapper<GovMetadata> metaWrapper = new LambdaQueryWrapper<>();
        metaWrapper.eq(GovMetadata::getDsId, dsId);
        metaWrapper.eq(GovMetadata::getDataLayer, dataLayer);
        List<GovMetadata> metas = govMetadataMapper.selectList(metaWrapper);
        if (metas.isEmpty()) {
            return 0;
        }
        DqDatasource ds = datasourceMapper.selectById(dsId);
        boolean isPg = ds != null && ds.getDsType() != null
                && "POSTGRESQL".equalsIgnoreCase(ds.getDsType().trim());
        if (isPg && StringUtils.hasText(pgSchema)) {
            DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
            if (adapter == null) {
                return 0;
            }
            try {
                Set<String> inSchema = adapter.getTables(pgSchema).stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
                return (int) metas.stream()
                        .map(GovMetadata::getTableName)
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .filter(inSchema::contains)
                        .distinct()
                        .count();
            } catch (Exception e) {
                log.warn("PostgreSQL 按层统计：读取 schema={} 表清单失败 dsId={}", pgSchema, dsId, e);
                return 0;
            }
        }
        return metas.size();
    }

    private String getScoreStatus(Integer score) {
        if (score == null) {
            return "UNKNOWN";
        }
        if (score >= 90) {
            return "EXCELLENT";
        }
        if (score >= 70) {
            return "GOOD";
        }
        if (score >= 60) {
            return "WARNING";
        }
        return "CRITICAL";
    }

    private DqcPlan toEntity(DqcPlanDTO dto) {
        return DqcPlan.builder()
                .id(dto.getId())
                .planName(dto.getPlanName())
                .planCode(dto.getPlanCode())
                .planDesc(dto.getPlanDesc())
                .bindType(dto.getBindType())
                .bindValue(dto.getBindValue())
                .layerCode(dto.getLayerCode())
                .deptId(dto.getDeptId())
                .triggerType(dto.getTriggerType())
                .triggerCron(dto.getTriggerCron())
                .alertOnSuccess(dto.getAlertOnSuccess() != null ? dto.getAlertOnSuccess() : false)
                .alertOnFailure(dto.getAlertOnFailure() != null ? dto.getAlertOnFailure() : true)
                .autoBlock(dto.getAutoBlock() != null ? dto.getAutoBlock() : true)
                .status(dto.getStatus())
                .ruleCount(dto.getRuleCount() != null ? dto.getRuleCount() : 0)
                .tableCount(dto.getTableCount() != null ? dto.getTableCount() : 0)
                .lastExecutionId(dto.getLastExecutionId())
                .lastExecutionTime(dto.getLastExecutionTime())
                .lastExecutionScore(dto.getLastExecutionScore())
                .build();
    }

    private Long getCurrentUserId() {
        return 1L;
    }
}
