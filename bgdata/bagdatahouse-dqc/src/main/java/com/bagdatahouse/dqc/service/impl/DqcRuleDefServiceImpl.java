package com.bagdatahouse.dqc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.common.util.IdGenerator;
import com.bagdatahouse.core.dto.DqcRuleDefDTO;
import com.bagdatahouse.core.entity.DqcRuleDef;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.entity.DqcRuleTemplate;
import com.bagdatahouse.core.mapper.DqcRuleDefMapper;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.mapper.DqcRuleTemplateMapper;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.bagdatahouse.dqc.engine.RuleContext;
import com.bagdatahouse.dqc.engine.RuleEngine;
import com.bagdatahouse.dqc.engine.function.CustomFunctionRegistry;
import com.bagdatahouse.dqc.engine.function.FunctionInvoker;
import com.bagdatahouse.dqc.engine.function.RuleFunction;
import com.bagdatahouse.dqc.enums.ApplyLevelEnum;
import com.bagdatahouse.dqc.enums.ErrorLevelEnum;
import com.bagdatahouse.dqc.enums.QualityDimensionEnum;
import com.bagdatahouse.dqc.enums.RuleStrengthEnum;
import com.bagdatahouse.dqc.enums.RuleTypeEnum;
import com.bagdatahouse.dqc.service.DqcRuleDefService;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * DQC规则定义服务实现
 */
@Service
public class DqcRuleDefServiceImpl extends ServiceImpl<DqcRuleDefMapper, DqcRuleDef>
        implements DqcRuleDefService {

    private static final Logger log = LoggerFactory.getLogger(DqcRuleDefServiceImpl.class);

    private static final Pattern JAVA_CLASS_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$");

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private DqcRuleTemplateMapper ruleTemplateMapper;

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private CustomFunctionRegistry functionRegistry;

    @Autowired
    private FunctionInvoker functionInvoker;

    @Autowired
    private DataSourceAdapterRegistry adapterRegistry;

    @Override
    public Result<Page<DqcRuleDef>> page(Integer pageNum, Integer pageSize, String ruleName,
                                          String ruleType, String applyLevel, Long targetDsId, Integer enabled) {
        return page(pageNum, pageSize, ruleName, ruleType, applyLevel, targetDsId, enabled, null, null);
    }

    @Override
    public Result<Page<DqcRuleDef>> page(Integer pageNum, Integer pageSize, String ruleName,
                                          String ruleType, String applyLevel, Long targetDsId,
                                          Integer enabled, String ruleStrength, String errorLevel) {
        Page<DqcRuleDef> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DqcRuleDef> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(ruleName)) {
            wrapper.like(DqcRuleDef::getRuleName, ruleName);
        }
        if (StringUtils.isNotBlank(ruleType)) {
            wrapper.eq(DqcRuleDef::getRuleType, ruleType);
        }
        if (StringUtils.isNotBlank(applyLevel)) {
            wrapper.eq(DqcRuleDef::getApplyLevel, applyLevel);
        }
        if (targetDsId != null) {
            wrapper.eq(DqcRuleDef::getTargetDsId, targetDsId);
        }
        if (enabled != null) {
            wrapper.eq(DqcRuleDef::getEnabled, enabled == 1);
        }
        if (StringUtils.isNotBlank(ruleStrength)) {
            wrapper.eq(DqcRuleDef::getRuleStrength, ruleStrength);
        }
        if (StringUtils.isNotBlank(errorLevel)) {
            wrapper.eq(DqcRuleDef::getErrorLevel, errorLevel);
        }

        wrapper.orderByDesc(DqcRuleDef::getCreateTime);
        Page<DqcRuleDef> result = this.page(page, wrapper);

        // Enrich with display names
        enrichDisplayNames(result.getRecords());

        return Result.success(result);
    }

    @Override
    public Result<DqcRuleDef> getById(Long id) {
        DqcRuleDef entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResponseCode.RULE_NOT_FOUND);
        }
        enrichDisplayName(entity);
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> create(DqcRuleDefDTO dto) {
        // Validate basic fields
        validateBasicFields(dto, null);

        // Validate rule-type specific requirements
        validateRuleTypeSpecificFields(dto);

        // Validate custom function
        validateCustomFunction(dto.getRuleType(), dto.getCustomFunctionClass());

        // Generate rule code if not provided
        String ruleCode = dto.getRuleCode();
        if (!StringUtils.isNotBlank(ruleCode)) {
            ruleCode = IdGenerator.generateRuleCode();
        } else {
            // Check uniqueness
            LambdaQueryWrapper<DqcRuleDef> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DqcRuleDef::getRuleCode, ruleCode);
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "规则编码已存在");
            }
        }

        DqcRuleDef entity = toEntity(dto);
        entity.setRuleCode(ruleCode);
        entity.setCreateUser(getCurrentUserId());
        entity.setCreateTime(LocalDateTime.now());
        entity.setEnabled(true);
        this.save(entity);

        log.info("Created rule: {} (id={})", entity.getRuleName(), entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, DqcRuleDefDTO dto) {
        DqcRuleDef existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResponseCode.RULE_NOT_FOUND);
        }

        // Validate basic fields
        validateBasicFields(dto, id);

        // Validate rule-type specific requirements
        validateRuleTypeSpecificFields(dto);

        // Validate custom function
        validateCustomFunction(dto.getRuleType(), dto.getCustomFunctionClass());

        // Check rule code uniqueness if changed
        if (StringUtils.isNotBlank(dto.getRuleCode()) && !dto.getRuleCode().equals(existing.getRuleCode())) {
            LambdaQueryWrapper<DqcRuleDef> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DqcRuleDef::getRuleCode, dto.getRuleCode());
            wrapper.ne(DqcRuleDef::getId, id);
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "规则编码已存在");
            }
        }

        DqcRuleDef entity = toEntity(dto);
        entity.setId(id);
        entity.setUpdateUser(getCurrentUserId());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setCreateUser(existing.getCreateUser());
        entity.setCreateTime(existing.getCreateTime());
        this.updateById(entity);

        log.info("Updated rule: {} (id={})", entity.getRuleName(), id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        DqcRuleDef existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResponseCode.RULE_NOT_FOUND);
        }
        this.removeById(id);
        log.info("Deleted rule: {} (id={})", existing.getRuleName(), id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> toggleEnabled(Long id) {
        DqcRuleDef entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResponseCode.RULE_NOT_FOUND);
        }
        entity.setEnabled(!entity.getEnabled());
        entity.setUpdateTime(LocalDateTime.now());
        this.updateById(entity);
        log.info("Toggled rule enabled status: {} (id={}) -> {}", entity.getRuleName(), id, entity.getEnabled());
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> copy(Long id) {
        DqcRuleDef original = baseMapper.selectById(id);
        if (original == null) {
            throw new BusinessException(ResponseCode.RULE_NOT_FOUND);
        }

        String newRuleCode = IdGenerator.generateRuleCode();

        DqcRuleDef copy = DqcRuleDef.builder()
                .ruleName(original.getRuleName() + "_副本")
                .ruleCode(newRuleCode)
                .templateId(original.getTemplateId())
                .ruleType(original.getRuleType())
                .applyLevel(original.getApplyLevel())
                .layerCode(original.getLayerCode())
                .dimensions(original.getDimensions())
                .ruleExpr(original.getRuleExpr())
                .targetDsId(original.getTargetDsId())
                .targetTable(original.getTargetTable())
                .targetColumn(original.getTargetColumn())
                .compareDsId(original.getCompareDsId())
                .compareTable(original.getCompareTable())
                .compareColumn(original.getCompareColumn())
                .thresholdMin(original.getThresholdMin())
                .thresholdMax(original.getThresholdMax())
                .fluctuationThreshold(original.getFluctuationThreshold())
                .regexPattern(original.getRegexPattern())
                .errorLevel(original.getErrorLevel())
                .ruleStrength(original.getRuleStrength())
                .alertReceivers(original.getAlertReceivers())
                .sortOrder(original.getSortOrder())
                .enabled(false)
                .deptId(original.getDeptId())
                .customFunctionClass(original.getCustomFunctionClass())
                .customFunctionParams(original.getCustomFunctionParams())
                .createUser(getCurrentUserId())
                .createTime(LocalDateTime.now())
                .build();

        this.save(copy);
        log.info("Copied rule: {} -> {} (id={})", original.getRuleName(), copy.getRuleName(), copy.getId());
        return Result.success();
    }

    @Override
    public Result<List<DqcRuleDef>> listByDsId(Long dsId) {
        LambdaQueryWrapper<DqcRuleDef> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcRuleDef::getTargetDsId, dsId);
        wrapper.eq(DqcRuleDef::getEnabled, true);
        wrapper.orderByAsc(DqcRuleDef::getSortOrder);
        List<DqcRuleDef> list = this.list(wrapper);
        enrichDisplayNames(list);
        return Result.success(list);
    }

    @Override
    public Result<List<DqcRuleDef>> listByTemplateId(Long templateId) {
        LambdaQueryWrapper<DqcRuleDef> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcRuleDef::getTemplateId, templateId);
        wrapper.eq(DqcRuleDef::getEnabled, true);
        wrapper.orderByAsc(DqcRuleDef::getSortOrder);
        List<DqcRuleDef> list = this.list(wrapper);
        enrichDisplayNames(list);
        return Result.success(list);
    }

    @Override
    public Result<ExecutionResult> execute(Long id) {
        DqcRuleDef rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResponseCode.RULE_NOT_FOUND);
        }
        if (rule.getTargetDsId() == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则未配置目标数据源");
        }
        if (!StringUtils.isNotBlank(rule.getTargetTable())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则未配置目标表");
        }

        DqDatasource datasource = datasourceMapper.selectById(rule.getTargetDsId());
        if (datasource == null) {
            throw new BusinessException(ResponseCode.DATASOURCE_NOT_FOUND);
        }

        DataSourceAdapter adapter = adapterRegistry.getAdapterById(rule.getTargetDsId());
        if (adapter == null) {
            throw new BusinessException(ResponseCode.DATASOURCE_CONNECTION_FAILED, "数据源未连接");
        }

        RuleContext context = RuleContext.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .applyLevel(rule.getApplyLevel())
                .ruleExpr(rule.getRuleExpr())
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
                .customFunctionClass(rule.getCustomFunctionClass())
                .customFunctionParams(rule.getCustomFunctionParams())
                .build();

        ExecutionResult result = ruleEngine.execute(context, adapter);
        log.info("Executed rule: {} (id={}), result: success={}, score={}",
                rule.getRuleName(), id, result.isSuccess(), result.getQualityScore());
        return Result.success(result);
    }

    @Override
    public Result<String> validateExpression(String ruleType, String ruleExpr, String targetTable,
                                              String targetColumn, String regexPattern,
                                              BigDecimal thresholdMin, BigDecimal thresholdMax) {
        if (StringUtils.isBlank(ruleType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则类型不能为空");
        }
        RuleTypeEnum typeEnum = RuleTypeEnum.fromCode(ruleType);
        if (typeEnum == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "不支持的规则类型: " + ruleType);
        }

        switch (typeEnum) {
            case NULL_CHECK, ROW_COUNT_NOT_ZERO -> {
                if (StringUtils.isBlank(targetTable)) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "目标表名不能为空");
                }
            }
            case REGEX, REGEX_PHONE, REGEX_EMAIL, REGEX_IDCARD -> {
                if (StringUtils.isBlank(regexPattern) && typeEnum == RuleTypeEnum.REGEX) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "正则表达式不能为空");
                }
            }
            case THRESHOLD_RANGE, COUNT_THRESHOLD -> {
                if (thresholdMin == null && thresholdMax == null) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "阈值范围不能为空");
                }
                if (thresholdMin != null && thresholdMax != null && thresholdMin.compareTo(thresholdMax) > 0) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "最小阈值不能大于最大阈值");
                }
            }
            case THRESHOLD_MIN -> {
                if (thresholdMin == null) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "最小阈值不能为空");
                }
            }
            case THRESHOLD_MAX -> {
                if (thresholdMax == null) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "最大阈值不能为空");
                }
            }
            case CUSTOM_SQL -> {
                if (StringUtils.isBlank(ruleExpr)) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "自定义SQL不能为空");
                }
                validateSqlSafety(ruleExpr);
            }
            case TABLE_UPDATE_TIMELINESS -> {
                if (StringUtils.isBlank(targetTable)) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "目标表名不能为空");
                }
                if (StringUtils.isBlank(ruleExpr)) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "规则表达式不能为空");
                }
                validateSqlSafety(ruleExpr);
            }
            default -> {
                // Other types don't need special validation
            }
        }

        return Result.success("表达式校验通过");
    }

    @Override
    public Result<Map<String, Object>> getRuleTypeOptions() {
        Map<String, Object> result = new HashMap<>();
        result.put("types", RuleTypeEnum.values());
        result.put("applyLevels", ApplyLevelEnum.values());
        result.put("dimensions", QualityDimensionEnum.values());
        result.put("strengths", RuleStrengthEnum.values());
        result.put("errorLevels", ErrorLevelEnum.values());
        return Result.success(result);
    }

    @Override
    public Result<List<Map<String, Object>>> listEnabledWithTemplateInfo() {
        LambdaQueryWrapper<DqcRuleDef> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcRuleDef::getEnabled, true);
        wrapper.orderByAsc(DqcRuleDef::getSortOrder);
        List<DqcRuleDef> rules = this.list(wrapper);

        List<Map<String, Object>> result = rules.stream().map(rule -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", rule.getId());
            item.put("ruleName", rule.getRuleName());
            item.put("ruleCode", rule.getRuleCode());
            item.put("ruleType", rule.getRuleType());
            item.put("ruleTypeName", RuleTypeEnum.getNameByCode(rule.getRuleType()));
            item.put("applyLevel", rule.getApplyLevel());
            item.put("applyLevelName", ApplyLevelEnum.getNameByCode(rule.getApplyLevel()));
            item.put("targetDsId", rule.getTargetDsId());
            item.put("targetTable", rule.getTargetTable());
            item.put("targetColumn", rule.getTargetColumn());
            item.put("ruleStrength", rule.getRuleStrength());
            item.put("errorLevel", rule.getErrorLevel());
            item.put("templateId", rule.getTemplateId());

            // Get template name if exists
            if (rule.getTemplateId() != null) {
                DqcRuleTemplate template = ruleTemplateMapper.selectById(rule.getTemplateId());
                if (template != null) {
                    item.put("templateName", template.getTemplateName());
                }
            }
            return item;
        }).toList();

        return Result.success(result);
    }

    // ==================== Private Methods ====================

    private void validateBasicFields(DqcRuleDefDTO dto, Long excludeId) {
        if (!StringUtils.isNotBlank(dto.getRuleName())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则名称不能为空");
        }
        if (dto.getRuleName().length() > 100) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则名称不能超过100字符");
        }
        if (!StringUtils.isNotBlank(dto.getRuleType())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "规则类型不能为空");
        }
        // Validate rule type
        if (RuleTypeEnum.fromCode(dto.getRuleType()) == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "不支持的规则类型: " + dto.getRuleType());
        }
    }

    private void validateRuleTypeSpecificFields(DqcRuleDefDTO dto) {
        RuleTypeEnum typeEnum = RuleTypeEnum.fromCode(dto.getRuleType());
        if (typeEnum == null) return;

        switch (typeEnum) {
            case NULL_CHECK, ROW_COUNT_NOT_ZERO -> {
                if (!StringUtils.isNotBlank(dto.getTargetTable())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "目标表名不能为空");
                }
            }
            case UNIQUE_CHECK, DUPLICATE_CHECK, CARDINALITY,
                 THRESHOLD_MIN, THRESHOLD_MAX, THRESHOLD_RANGE,
                 REGEX_PHONE, REGEX_EMAIL, REGEX_IDCARD, LENGTH_CHECK -> {
                if (!StringUtils.isNotBlank(dto.getTargetTable())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "目标表名不能为空");
                }
                if (!StringUtils.isNotBlank(dto.getTargetColumn())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "目标列名不能为空");
                }
            }
            case CROSS_FIELD_COMPARE, CROSS_FIELD_SUM, CROSS_FIELD_NULL_CHECK -> {
                if (!StringUtils.isNotBlank(dto.getTargetTable())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "目标表名不能为空");
                }
                if (!StringUtils.isNotBlank(dto.getTargetColumn())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "源列名不能为空");
                }
                if (!StringUtils.isNotBlank(dto.getCompareColumn())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "比对列名不能为空");
                }
            }
            case CROSS_TABLE_COUNT, CROSS_TABLE_PRIMARY_KEY -> {
                if (!StringUtils.isNotBlank(dto.getTargetTable())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "目标表名不能为空");
                }
                if (!StringUtils.isNotBlank(dto.getCompareTable())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "比对表名不能为空");
                }
            }
            case REGEX -> {
                if (StringUtils.isNotBlank(dto.getRuleExpr()) && dto.getRuleExpr().contains("${table}")) {
                } else if (!StringUtils.isNotBlank(dto.getRegexPattern())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "正则表达式不能为空");
                }
            }
            case COUNT_THRESHOLD -> {
                if (dto.getThresholdMin() == null && dto.getThresholdMax() == null) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "请设置阈值范围");
                }
                if (dto.getThresholdMin() != null && dto.getThresholdMax() != null
                        && dto.getThresholdMin().compareTo(dto.getThresholdMax()) > 0) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "最小阈值不能大于最大阈值");
                }
            }
            case CUSTOM_SQL -> {
                if (!StringUtils.isNotBlank(dto.getRuleExpr())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "自定义SQL不能为空");
                }
                validateSqlSafety(dto.getRuleExpr());
            }
            case TABLE_UPDATE_TIMELINESS -> {
                if (!StringUtils.isNotBlank(dto.getTargetTable())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "目标表名不能为空");
                }
                if (!StringUtils.isNotBlank(dto.getRuleExpr())) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "规则表达式不能为空");
                }
                validateSqlSafety(dto.getRuleExpr());
            }
            default -> {
                // CUSTOM_FUNC and ROW_COUNT_FLUCTUATION don't require special validation here
            }
        }

        // Validate dimensions
        if (StringUtils.isNotBlank(dto.getDimensions())) {
            try {
                com.alibaba.fastjson2.JSON.parseArray(dto.getDimensions());
            } catch (Exception e) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "维度格式错误，应为JSON数组");
            }
        }
    }

    private void validateCustomFunction(String ruleType, String customFunctionClass) {
        if ("CUSTOM_FUNC".equalsIgnoreCase(ruleType)) {
            if (!StringUtils.isNotBlank(customFunctionClass)) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "自定义函数规则必须指定函数类全限定名");
            }
            if (!JAVA_CLASS_NAME_PATTERN.matcher(customFunctionClass).matches()) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "函数类全限定名格式不正确");
            }
            // Try to load the class
            if (!functionRegistry.isRegistered(customFunctionClass)) {
                // Try to register it dynamically
                try {
                    functionRegistry.registerFunction(customFunctionClass, "User registered function", new HashMap<>());
                } catch (Exception e) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST,
                            "无法加载自定义函数类: " + customFunctionClass + "，请检查类是否存在且实现 RuleFunction 接口");
                }
            }
        }
    }

    private void validateSqlSafety(String sql) {
        String upperSql = sql.toUpperCase();
        String[] dangerous = {"DROP ", "TRUNCATE ", "DELETE ", "INSERT ", "UPDATE ", "ALTER ", "CREATE ", "GRANT ", "REVOKE "};
        for (String keyword : dangerous) {
            if (upperSql.contains(keyword) && !upperSql.contains("--") && upperSql.indexOf(keyword) < upperSql.indexOf("WHERE")) {
                // Allow SELECT statements (which contain these keywords after WHERE)
                // Block if they appear before WHERE (meaning modifying statements)
                if (upperSql.contains("WHERE")) {
                    int keywordIdx = upperSql.indexOf(keyword);
                    int whereIdx = upperSql.indexOf("WHERE");
                    if (keywordIdx < whereIdx) {
                        throw new BusinessException(ResponseCode.BAD_REQUEST,
                                "SQL语句不允许包含数据修改操作: " + keyword.trim());
                    }
                } else {
                    throw new BusinessException(ResponseCode.BAD_REQUEST,
                            "SQL语句不允许包含数据修改操作: " + keyword.trim());
                }
            }
        }
    }

    private void enrichDisplayName(DqcRuleDef rule) {
        rule.setRuleTypeName(RuleTypeEnum.getNameByCode(rule.getRuleType()));
        rule.setApplyLevelName(ApplyLevelEnum.getNameByCode(rule.getApplyLevel()));
        rule.setRuleStrengthName(RuleStrengthEnum.getNameByCode(rule.getRuleStrength()));
        rule.setErrorLevelName(ErrorLevelEnum.getNameByCode(rule.getErrorLevel()));

        if (rule.getTargetDsId() != null) {
            DqDatasource ds = datasourceMapper.selectById(rule.getTargetDsId());
            if (ds != null) {
                rule.setTargetDsName(ds.getDsName());
            }
        }
        if (rule.getTemplateId() != null) {
            DqcRuleTemplate template = ruleTemplateMapper.selectById(rule.getTemplateId());
            if (template != null) {
                rule.setTemplateName(template.getTemplateName());
            }
        }
    }

    private void enrichDisplayNames(List<DqcRuleDef> rules) {
        if (rules == null || rules.isEmpty()) return;

        // Batch load datasources
        List<Long> dsIds = rules.stream()
                .map(DqcRuleDef::getTargetDsId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, DqDatasource> dsMap = new HashMap<>();
        if (!dsIds.isEmpty()) {
            datasourceMapper.selectBatchIds(dsIds).forEach(ds -> dsMap.put(ds.getId(), ds));
        }

        // Batch load templates
        List<Long> templateIds = rules.stream()
                .map(DqcRuleDef::getTemplateId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, DqcRuleTemplate> templateMap = new HashMap<>();
        if (!templateIds.isEmpty()) {
            ruleTemplateMapper.selectBatchIds(templateIds).forEach(t -> templateMap.put(t.getId(), t));
        }

        for (DqcRuleDef rule : rules) {
            rule.setRuleTypeName(RuleTypeEnum.getNameByCode(rule.getRuleType()));
            rule.setApplyLevelName(ApplyLevelEnum.getNameByCode(rule.getApplyLevel()));
            rule.setRuleStrengthName(RuleStrengthEnum.getNameByCode(rule.getRuleStrength()));
            rule.setErrorLevelName(ErrorLevelEnum.getNameByCode(rule.getErrorLevel()));

            DqDatasource ds = dsMap.get(rule.getTargetDsId());
            if (ds != null) {
                rule.setTargetDsName(ds.getDsName());
            }
            DqcRuleTemplate template = templateMap.get(rule.getTemplateId());
            if (template != null) {
                rule.setTemplateName(template.getTemplateName());
            }
        }
    }

    private DqcRuleDef toEntity(DqcRuleDefDTO dto) {
        return DqcRuleDef.builder()
                .id(dto.getId())
                .ruleName(dto.getRuleName())
                .ruleCode(dto.getRuleCode())
                .templateId(dto.getTemplateId())
                .ruleType(dto.getRuleType())
                .applyLevel(dto.getApplyLevel())
                .layerCode(dto.getLayerCode())
                .dimensions(dto.getDimensions())
                .ruleExpr(dto.getRuleExpr())
                .targetDsId(dto.getTargetDsId())
                .targetTable(dto.getTargetTable())
                .targetColumn(dto.getTargetColumn())
                .compareDsId(dto.getCompareDsId())
                .compareTable(dto.getCompareTable())
                .compareColumn(dto.getCompareColumn())
                .thresholdMin(dto.getThresholdMin())
                .thresholdMax(dto.getThresholdMax())
                .fluctuationThreshold(dto.getFluctuationThreshold())
                .regexPattern(dto.getRegexPattern())
                .errorLevel(dto.getErrorLevel())
                .ruleStrength(dto.getRuleStrength())
                .alertReceivers(dto.getAlertReceivers())
                .sortOrder(dto.getSortOrder())
                .enabled(dto.getEnabled())
                .deptId(dto.getDeptId())
                .customFunctionClass(dto.getCustomFunctionClass())
                .customFunctionParams(dto.getCustomFunctionParams())
                .build();
    }

    private Long getCurrentUserId() {
        // In a real application, get from SecurityContext
        return 1L;
    }
}
