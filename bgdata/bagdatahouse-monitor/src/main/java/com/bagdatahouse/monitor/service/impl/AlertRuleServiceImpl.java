package com.bagdatahouse.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.common.util.IdGenerator;
import com.bagdatahouse.core.dto.MonitorAlertRuleDTO;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.entity.DqcRuleDef;
import com.bagdatahouse.core.entity.MonitorAlertRule;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.mapper.DqcRuleDefMapper;
import com.bagdatahouse.core.mapper.MonitorAlertRuleMapper;
import com.bagdatahouse.monitor.enums.AlertConditionTypeEnum;
import com.bagdatahouse.monitor.enums.AlertLevelEnum;
import com.bagdatahouse.monitor.enums.AlertRuleTypeEnum;
import com.bagdatahouse.monitor.service.AlertRuleService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警规则服务实现
 */
@Service
public class AlertRuleServiceImpl extends ServiceImpl<MonitorAlertRuleMapper, MonitorAlertRule>
        implements AlertRuleService {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleServiceImpl.class);

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private DqcRuleDefMapper ruleDefMapper;

    @Override
    public Result<Page<MonitorAlertRule>> page(
            Integer pageNum,
            Integer pageSize,
            String ruleName,
            String ruleType,
            String targetType,
            String alertLevel,
            Integer enabled
    ) {
        LambdaQueryWrapper<MonitorAlertRule> wrapper = new LambdaQueryWrapper<>();
        // 注意：第三个参数会在调用前求值，不能写 enabled == 1（enabled 为 null 时会拆箱 NPE）
        boolean enabledEqOne = enabled != null && enabled == 1;
        wrapper.like(StringUtils.isNotBlank(ruleName), MonitorAlertRule::getRuleName, ruleName)
               .eq(StringUtils.isNotBlank(ruleType), MonitorAlertRule::getRuleType, ruleType)
               .eq(StringUtils.isNotBlank(targetType), MonitorAlertRule::getTargetType, targetType)
               .eq(StringUtils.isNotBlank(alertLevel), MonitorAlertRule::getAlertLevel, alertLevel)
               .eq(enabled != null, MonitorAlertRule::getEnabled, enabledEqOne)
               .orderByDesc(MonitorAlertRule::getCreateTime);

        Page<MonitorAlertRule> page = new Page<>(pageNum, pageSize);
        Page<MonitorAlertRule> result = baseMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<MonitorAlertRule> getById(Long id) {
        MonitorAlertRule rule = baseMapper.selectById(id);
        if (rule == null) {
            return Result.fail(ResponseCode.NOT_FOUND, "告警规则不存在");
        }
        return Result.success(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> create(MonitorAlertRuleDTO dto) {
        validateRule(dto, null);

        MonitorAlertRule rule = MonitorAlertRule.builder()
                .ruleName(dto.getRuleName())
                .ruleCode(generateRuleCode())
                .ruleType(dto.getRuleType())
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId())
                .conditionType(dto.getConditionType())
                .thresholdValue(dto.getThresholdValue())
                .thresholdMaxValue(dto.getThresholdMaxValue())
                .fluctuationPct(dto.getFluctuationPct())
                .comparisonType(dto.getComparisonType())
                .consecutiveTriggers(dto.getConsecutiveTriggers())
                .alertLevel(dto.getAlertLevel())
                .alertReceivers(dto.getAlertReceivers())
                .alertChannels(dto.getAlertChannels())
                .alertTitleTemplate(dto.getAlertTitleTemplate())
                .alertContentTemplate(dto.getAlertContentTemplate())
                .muteUntil(dto.getMuteUntil())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .deptId(dto.getDeptId())
                .createUser(dto.getCreateUser())
                .build();

        baseMapper.insert(rule);
        return Result.success(rule.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, MonitorAlertRuleDTO dto) {
        MonitorAlertRule existing = baseMapper.selectById(id);
        if (existing == null) {
            return Result.fail(ResponseCode.NOT_FOUND, "告警规则不存在");
        }
        validateRule(dto, id);

        existing.setRuleName(dto.getRuleName());
        existing.setRuleType(dto.getRuleType());
        existing.setTargetType(dto.getTargetType());
        existing.setTargetId(dto.getTargetId());
        existing.setConditionType(dto.getConditionType());
        existing.setThresholdValue(dto.getThresholdValue());
        existing.setThresholdMaxValue(dto.getThresholdMaxValue());
        existing.setFluctuationPct(dto.getFluctuationPct());
        existing.setComparisonType(dto.getComparisonType());
        existing.setConsecutiveTriggers(dto.getConsecutiveTriggers());
        existing.setAlertLevel(dto.getAlertLevel());
        existing.setAlertReceivers(dto.getAlertReceivers());
        existing.setAlertChannels(dto.getAlertChannels());
        existing.setAlertTitleTemplate(dto.getAlertTitleTemplate());
        existing.setAlertContentTemplate(dto.getAlertContentTemplate());
        existing.setMuteUntil(dto.getMuteUntil());
        existing.setDeptId(dto.getDeptId());

        baseMapper.updateById(existing);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        MonitorAlertRule rule = baseMapper.selectById(id);
        if (rule == null) {
            return Result.fail(ResponseCode.NOT_FOUND, "告警规则不存在");
        }
        baseMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> toggleEnabled(Long id) {
        MonitorAlertRule rule = baseMapper.selectById(id);
        if (rule == null) {
            return Result.fail(ResponseCode.NOT_FOUND, "告警规则不存在");
        }
        rule.setEnabled(!Boolean.TRUE.equals(rule.getEnabled()));
        baseMapper.updateById(rule);
        return Result.success();
    }

    @Override
    public Result<Map<String, Object>> getRuleTypeOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("ruleTypes", AlertRuleTypeEnum.values());
        return Result.success(options);
    }

    @Override
    public Result<Map<String, Object>> getTargetTypeOptions() {
        Map<String, Object> options = new HashMap<>();
        List<Map<String, String>> targetTypes = List.of(
                Map.of("value", "TABLE", "label", "数据表"),
                Map.of("value", "COLUMN", "label", "数据字段"),
                Map.of("value", "DATASOURCE", "label", "数据源"),
                Map.of("value", "METRIC", "label", "监控指标"),
                Map.of("value", "RULE", "label", "质检规则")
        );
        options.put("targetTypes", targetTypes);
        return Result.success(options);
    }

    @Override
    public Result<Map<String, Object>> getTargetListByType(String targetType) {
        Map<String, Object> result = new HashMap<>();
        switch (targetType) {
            case "DATASOURCE" -> {
                List<DqDatasource> datasources = datasourceMapper.selectList(
                        new LambdaQueryWrapper<DqDatasource>().eq(DqDatasource::getStatus, 1));
                result.put("targets", datasources.stream().map(ds -> Map.of(
                        "id", ds.getId(),
                        "name", ds.getDsName(),
                        "code", ds.getDsCode()
                )));
            }
            case "RULE" -> {
                List<DqcRuleDef> rules = ruleDefMapper.selectList(
                        new LambdaQueryWrapper<DqcRuleDef>().eq(DqcRuleDef::getEnabled, 1));
                result.put("targets", rules.stream().map(r -> Map.of(
                        "id", r.getId(),
                        "name", r.getRuleName(),
                        "code", r.getRuleCode()
                )));
            }
            default -> result.put("targets", List.of());
        }
        return Result.success(result);
    }

    private void validateRule(MonitorAlertRuleDTO dto, Long excludeId) {
        if (StringUtils.isBlank(dto.getRuleName())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则名称不能为空");
        }
        if (StringUtils.isBlank(dto.getRuleType())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则类型不能为空");
        }
        if (StringUtils.isBlank(dto.getAlertLevel())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "告警级别不能为空");
        }
    }

    private String generateRuleCode() {
        return "ALERT_RULE_" + IdGenerator.simpleUUID().replace("-", "").substring(0, 8).toUpperCase();
    }

    // ==================== SENSITIVE 类型告警规则实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createSensityAlertRule(MonitorAlertRuleDTO dto) {
        if (StringUtils.isBlank(dto.getRuleName())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则名称不能为空");
        }
        if (StringUtils.isBlank(dto.getRuleType())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则类型不能为空");
        }
        if (StringUtils.isBlank(dto.getAlertLevel())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "告警级别不能为空");
        }

        MonitorAlertRule rule = MonitorAlertRule.builder()
                .ruleName(dto.getRuleName())
                .ruleCode(generateRuleCode())
                .ruleType(dto.getRuleType())
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId())
                .sensitivityLevel(dto.getSensitivityLevel())
                .sensitivityTable(dto.getSensitivityTable())
                .sensitivityColumn(dto.getSensitivityColumn())
                .sensitivityDsId(dto.getSensitivityDsId())
                .spikeThresholdPct(dto.getSpikeThresholdPct())
                .unreviewThresholdDays(dto.getUnreviewThresholdDays())
                .accessAnomalyOffHours(dto.getAccessAnomalyOffHours())
                .accessAnomalyThreshold(dto.getAccessAnomalyThreshold())
                .alertLevel(dto.getAlertLevel())
                .alertReceivers(dto.getAlertReceivers())
                .alertChannels(dto.getAlertChannels())
                .alertTitleTemplate(dto.getAlertTitleTemplate())
                .alertContentTemplate(dto.getAlertContentTemplate())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .deptId(dto.getDeptId())
                .createUser(dto.getCreateUser())
                .build();

        baseMapper.insert(rule);
        log.info("创建 SENSITIVE 类型告警规则: name={}, type={}", dto.getRuleName(), dto.getRuleType());
        return Result.success(rule.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateSensityAlertRule(Long id, MonitorAlertRuleDTO dto) {
        MonitorAlertRule existing = baseMapper.selectById(id);
        if (existing == null) {
            return Result.fail(ResponseCode.NOT_FOUND, "告警规则不存在");
        }
        if (StringUtils.isBlank(dto.getRuleName())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则名称不能为空");
        }
        if (StringUtils.isBlank(dto.getAlertLevel())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "告警级别不能为空");
        }

        existing.setRuleName(dto.getRuleName());
        existing.setRuleType(dto.getRuleType());
        existing.setTargetType(dto.getTargetType());
        existing.setTargetId(dto.getTargetId());
        existing.setSensitivityLevel(dto.getSensitivityLevel());
        existing.setSensitivityTable(dto.getSensitivityTable());
        existing.setSensitivityColumn(dto.getSensitivityColumn());
        existing.setSensitivityDsId(dto.getSensitivityDsId());
        existing.setSpikeThresholdPct(dto.getSpikeThresholdPct());
        existing.setUnreviewThresholdDays(dto.getUnreviewThresholdDays());
        existing.setAccessAnomalyOffHours(dto.getAccessAnomalyOffHours());
        existing.setAccessAnomalyThreshold(dto.getAccessAnomalyThreshold());
        existing.setAlertLevel(dto.getAlertLevel());
        existing.setAlertReceivers(dto.getAlertReceivers());
        existing.setAlertChannels(dto.getAlertChannels());
        existing.setAlertTitleTemplate(dto.getAlertTitleTemplate());
        existing.setAlertContentTemplate(dto.getAlertContentTemplate());
        existing.setDeptId(dto.getDeptId());

        baseMapper.updateById(existing);
        log.info("更新 SENSITIVE 类型告警规则: id={}, name={}", id, dto.getRuleName());
        return Result.success();
    }

    @Override
    public Result<List<MonitorAlertRule>> getSensityAlertRulesByLevel(String sensitivityLevel) {
        LambdaQueryWrapper<MonitorAlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MonitorAlertRule::getEnabled, true)
               .eq(StringUtils.isNotBlank(sensitivityLevel),
                       MonitorAlertRule::getSensitivityLevel, sensitivityLevel)
               .like(StringUtils.isNotBlank(sensitivityLevel),
                       MonitorAlertRule::getRuleType, "SENSITIVE")
               .orderByDesc(MonitorAlertRule::getCreateTime);
        List<MonitorAlertRule> rules = baseMapper.selectList(wrapper);
        return Result.success(rules);
    }

    @Override
    public Result<List<MonitorAlertRule>> getSensityAlertRulesByDsId(Long dsId) {
        LambdaQueryWrapper<MonitorAlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MonitorAlertRule::getEnabled, true)
               .eq(dsId != null, MonitorAlertRule::getSensitivityDsId, dsId)
               .like(MonitorAlertRule::getRuleType, "SENSITIVE")
               .orderByDesc(MonitorAlertRule::getCreateTime);
        List<MonitorAlertRule> rules = baseMapper.selectList(wrapper);
        return Result.success(rules);
    }
}
