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
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.datasource.adapter.DataSourceAdapterRegistry;
import org.dromara.datasource.domain.SysDatasource;
import org.dromara.datasource.mapper.SysDatasourceMapper;
import org.dromara.datasource.support.DatasourceCryptoSupport;
import org.dromara.metadata.domain.DqcExecution;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcPlan;
import org.dromara.metadata.domain.DqcPlanRule;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.vo.DqcExecutionDetailVo;
import org.dromara.metadata.domain.vo.DqcExecutionVo;
import org.dromara.metadata.mapper.DqcExecutionDetailMapper;
import org.dromara.metadata.mapper.DqcExecutionMapper;
import org.dromara.metadata.mapper.DqcPlanMapper;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.mapper.DqcRuleDefMapper;
import org.dromara.metadata.service.IDqcExecutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据质量执行服务实现 - 核心DQC执行引擎
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DqcExecutionServiceImpl implements IDqcExecutionService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");

    private final DqcExecutionMapper executionMapper;
    private final DqcExecutionDetailMapper detailMapper;
    private final DqcPlanMapper planMapper;
    private final DqcRuleDefMapper ruleDefMapper;
    private final DqcPlanRuleMapper planRuleMapper;
    private final SysDatasourceMapper sysDatasourceMapper;
    private final DataSourceAdapterRegistry adapterRegistry;
    private final DatasourceCryptoSupport cryptoSupport;

    @Override
    public TableDataInfo<DqcExecutionVo> queryPageList(DqcExecutionVo vo, PageQuery pageQuery) {
        Wrapper<DqcExecution> wrapper = buildQueryWrapper(vo);
        var page = executionMapper.selectVoPage(pageQuery.build(), wrapper);
        // 格式化状态文本
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

            try {
                long start = System.currentTimeMillis();

                // 获取数据源连接
                SysDatasource ds = sysDatasourceMapper.selectById(rule.getTargetDsId());
                if (ds == null) {
                    throw new RuntimeException("数据源不存在");
                }

                String password = cryptoSupport.decryptPassword(ds.getPassword());
                DataSourceAdapter adapter = adapterRegistry.getOrCreateAdapter(
                    ds.getDsId(), ds.getDsType(), ds.getHost(), ds.getPort(),
                    ds.getDatabaseName(), ds.getSchemaName(), ds.getUsername(), password,
                    ds.getConnectionParams()
                );

                // 渲染 SQL
                String sql = renderSql(rule, adapter);
                detail.setExecuteSql(sql);

                // 执行查询
                List<java.util.Map<String, Object>> resultSet = adapter.executeQuery(sql);
                Object result = extractResultValue(resultSet);

                detail.setActualValue(result != null ? result.toString() : null);
                detail.setElapsedMs(System.currentTimeMillis() - start);

                // 比对阈值
                EvaluationResult evaluation = evaluateResult(result, rule);
                boolean pass = evaluation.pass();
                detail.setPassFlag(pass ? "1" : "0");
                detail.setResultValue(evaluation.resultValue());
                detail.setThresholdValue(evaluation.thresholdValue());

                if (!pass) {
                    detail.setErrorLevel(rule.getErrorLevel());
                    detail.setErrorMsg(evaluation.message());
                    if ("HIGH".equals(rule.getErrorLevel()) || "CRITICAL".equals(rule.getErrorLevel())) {
                        blocked++;
                    }
                }

                if (pass) {
                    passed++;
                } else {
                    failed++;
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

        return execution;
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
     * 渲染SQL模板
     */
    private String renderSql(DqcRuleDef rule, DataSourceAdapter adapter) {
        String sqlTemplate = rule.getRuleExpr();
        if (sqlTemplate == null || sqlTemplate.isBlank()) {
            return "";
        }
        String result = sqlTemplate;

        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("table", quoteQualifiedIdentifier(adapter, rule.getTargetTable()));
        replacements.put("table_name", quoteQualifiedIdentifier(adapter, rule.getTargetTable()));
        replacements.put("column", quoteIdentifier(adapter, rule.getTargetColumn()));
        replacements.put("column_name", quoteIdentifier(adapter, rule.getTargetColumn()));
        replacements.put("update_column", quoteIdentifier(adapter, rule.getTargetColumn()));
        replacements.put("compare_table", quoteQualifiedIdentifier(adapter, rule.getCompareTable()));
        replacements.put("compare_column", quoteIdentifier(adapter, rule.getCompareColumn()));
        replacements.put("column_a", quoteIdentifier(adapter, rule.getTargetColumn()));
        replacements.put("column_b", quoteIdentifier(adapter, rule.getCompareColumn()));
        replacements.put("column_c", quoteIdentifier(adapter, resolveThirdColumn(rule)));
        replacements.put("pk_column", quoteIdentifier(adapter, rule.getTargetColumn()));
        replacements.put("pattern", quoteStringLiteral(rule.getRegexPattern()));
        replacements.put("enum_values", buildEnumLiteralList(rule.getRegexPattern()));
        replacements.put("min_value", toSqlNumber(rule.getThresholdMin()));
        replacements.put("max_value", toSqlNumber(rule.getThresholdMax()));
        replacements.put("min_len", toSqlNumber(rule.getThresholdMin()));
        replacements.put("max_len", toSqlNumber(rule.getThresholdMax()));
        replacements.put("z_threshold", toSqlNumber(firstNonNull(rule.getThresholdMax(), rule.getThresholdMin())));
        replacements.put("threshold_min", toSqlNumber(rule.getThresholdMin()));
        replacements.put("threshold_max", toSqlNumber(rule.getThresholdMax()));
        replacements.put("threshold_pct", toSqlNumber(firstNonNull(rule.getThresholdMax(), rule.getThresholdMin())));
        replacements.put("threshold_hours", toSqlNumber(firstNonNull(rule.getThresholdMax(), rule.getThresholdMin())));
        replacements.put("fluctuation_threshold", toSqlNumber(rule.getFluctuationThreshold()));

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        List<String> unresolved = new java.util.ArrayList<>();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!unresolved.contains(placeholder)) {
                unresolved.add(placeholder);
            }
        }
        if (!unresolved.isEmpty()) {
            throw new IllegalArgumentException("规则SQL存在未绑定占位符: " + String.join(", ", unresolved));
        }
        return result;
    }

    /**
     * 评估结果是否通过阈值
     */
    private EvaluationResult evaluateResult(Object result, DqcRuleDef rule) {
        if (result == null) {
            return new EvaluationResult(false, null, buildThresholdText(rule), "执行结果为空");
        }

        if ("FLUCTUATION".equals(rule.getRuleType()) && rule.getFluctuationThreshold() != null) {
            BigDecimal currentValue = toDecimal(result);
            if (currentValue == null) {
                return new EvaluationResult(false, null, "波动阈值<=" + rule.getFluctuationThreshold() + "%", "波动检测结果不是数值");
            }
            DqcExecutionDetail previousDetail = findPreviousDetail(rule.getId());
            if (previousDetail == null || previousDetail.getResultValue() == null) {
                return new EvaluationResult(true, currentValue, "首轮执行，无历史基线", "首轮执行，无历史基线");
            }
            BigDecimal baseValue = previousDetail.getResultValue();
            if (BigDecimal.ZERO.compareTo(baseValue) == 0) {
                return new EvaluationResult(true, currentValue, "历史基线为0，跳过波动计算", "历史基线为0，跳过波动计算");
            }
            BigDecimal diffPct = currentValue.subtract(baseValue)
                .abs()
                .multiply(BigDecimal.valueOf(100))
                .divide(baseValue.abs(), 4, RoundingMode.HALF_UP);
            boolean pass = diffPct.compareTo(rule.getFluctuationThreshold()) <= 0;
            String thresholdText = String.format("历史基线=%s, 波动=%s%%, 阈值<=%s%%",
                baseValue.stripTrailingZeros().toPlainString(),
                diffPct.stripTrailingZeros().toPlainString(),
                rule.getFluctuationThreshold().stripTrailingZeros().toPlainString());
            return new EvaluationResult(pass, currentValue, thresholdText, thresholdText);
        }

        LocalDateTime resultTime = toLocalDateTime(result);
        if (resultTime != null && (rule.getThresholdMin() != null || rule.getThresholdMax() != null)) {
            BigDecimal ageHours = BigDecimal.valueOf(Duration.between(resultTime, LocalDateTime.now()).toMinutes())
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            boolean pass = isWithinThreshold(ageHours, rule.getThresholdMin(), rule.getThresholdMax());
            String thresholdText = String.format("最近更新时间=%s, 延迟=%s小时, 阈值=%s",
                resultTime,
                ageHours.stripTrailingZeros().toPlainString(),
                buildThresholdText(rule));
            return new EvaluationResult(pass, ageHours, thresholdText, thresholdText);
        }

        BigDecimal actual = toDecimal(result);
        if (actual == null) {
            return new EvaluationResult(false, null, buildThresholdText(rule), "执行结果不是可比较数值");
        }
        boolean pass = isWithinThreshold(actual, rule.getThresholdMin(), rule.getThresholdMax());
        String thresholdText = String.format("实际值=%s, 阈值=%s",
            actual.stripTrailingZeros().toPlainString(),
            buildThresholdText(rule));
        return new EvaluationResult(pass, actual, thresholdText, thresholdText);
    }

    /**
     * 转换为double
     */
    private double toDouble(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 转换为BigDecimal
     */
    private BigDecimal toDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal decimal) return decimal;
        if (obj instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Object extractResultValue(List<Map<String, Object>> resultSet) {
        if (resultSet == null || resultSet.isEmpty()) {
            return null;
        }
        Map<String, Object> row = resultSet.get(0);
        if (row == null || row.isEmpty()) {
            return null;
        }

        List<String> preferredKeys = List.of(
            "result", "result_value", "value", "cnt", "count", "total",
            "dup_count", "invalid_count", "enum_violation", "pattern_violation",
            "min_val", "max_val", "avg_val", "anomaly_pct", "violation_count",
            "null_inconsistency", "row_diff", "key_diff", "last_update", "last_update_time",
            "update_delay_hours", "timeliness_hours"
        );
        for (String key : preferredKeys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        for (Object value : row.values()) {
            if (value instanceof Number) {
                return value;
            }
        }
        for (Object value : row.values()) {
            if (toLocalDateTime(value) != null) {
                return value;
            }
        }
        return row.values().iterator().next();
    }

    private LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (obj instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (obj instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        String text = obj.toString();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Timestamp.valueOf(text.trim()).toLocalDateTime();
        } catch (Exception ignored) {
            // ignore
        }
        try {
            return LocalDateTime.parse(text.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isWithinThreshold(BigDecimal actual, BigDecimal min, BigDecimal max) {
        if (actual == null) {
            return false;
        }
        if (min != null && actual.compareTo(min) < 0) {
            return false;
        }
        if (max != null && actual.compareTo(max) > 0) {
            return false;
        }
        return true;
    }

    private String buildThresholdText(DqcRuleDef rule) {
        if (rule.getThresholdMin() == null && rule.getThresholdMax() == null) {
            return "未配置数值阈值";
        }
        return String.format("[%s, %s]",
            rule.getThresholdMin() != null ? rule.getThresholdMin().stripTrailingZeros().toPlainString() : "-∞",
            rule.getThresholdMax() != null ? rule.getThresholdMax().stripTrailingZeros().toPlainString() : "+∞");
    }

    private DqcExecutionDetail findPreviousDetail(Long ruleId) {
        List<DqcExecutionDetail> details = detailMapper.selectList(
            Wrappers.<DqcExecutionDetail>lambdaQuery()
                .eq(DqcExecutionDetail::getRuleId, ruleId)
                .isNotNull(DqcExecutionDetail::getResultValue)
                .orderByDesc(DqcExecutionDetail::getId)
                .last("limit 1")
        );
        return details.isEmpty() ? null : details.get(0);
    }

    private String quoteIdentifier(DataSourceAdapter adapter, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        return adapter.quoteIdentifier(identifier.trim());
    }

    private String quoteQualifiedIdentifier(DataSourceAdapter adapter, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String[] parts = identifier.trim().split("\\.");
        return java.util.Arrays.stream(parts)
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .map(adapter::quoteIdentifier)
            .reduce((left, right) -> left + "." + right)
            .orElse(null);
    }

    private String quoteStringLiteral(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "'" + value.trim().replace("'", "''") + "'";
    }

    private String buildEnumLiteralList(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.contains("'")) {
            return trimmed;
        }
        String[] parts = trimmed.split(",");
        List<String> literals = new java.util.ArrayList<>();
        for (String part : parts) {
            String item = part.trim();
            if (!item.isEmpty()) {
                literals.add("'" + item.replace("'", "''") + "'");
            }
        }
        return literals.isEmpty() ? null : String.join(", ", literals);
    }

    private String toSqlNumber(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private String resolveThirdColumn(DqcRuleDef rule) {
        if ("CROSS_FIELD".equals(rule.getApplyLevel())) {
            return rule.getCompareTable();
        }
        return null;
    }

    private record EvaluationResult(boolean pass, BigDecimal resultValue, String thresholdValue, String message) {
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
