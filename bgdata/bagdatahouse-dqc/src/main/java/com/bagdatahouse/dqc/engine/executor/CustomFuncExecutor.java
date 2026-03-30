package com.bagdatahouse.dqc.engine.executor;

import com.alibaba.fastjson2.JSON;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.bagdatahouse.dqc.engine.RuleContext;
import com.bagdatahouse.dqc.engine.function.FunctionInvoker;
import com.bagdatahouse.dqc.engine.function.RuleFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义函数规则执行器
 * 支持用户注册的自定义 Java 函数进行数据质量检查
 */
@Component
public class CustomFuncExecutor implements RuleExecutor {

    private static final Logger log = LoggerFactory.getLogger(CustomFuncExecutor.class);

    @Autowired
    private FunctionInvoker functionInvoker;

    @Override
    public String getRuleType() {
        return "CUSTOM_FUNC";
    }

    @Override
    public ExecutionResult execute(RuleContext context, DataSourceAdapter adapter) {
        LocalDateTime startTime = LocalDateTime.now();
        String className = context.getCustomFunctionClass();
        String paramsJson = context.getCustomFunctionParams();

        if (className == null || className.isBlank()) {
            return ExecutionResult.failed(
                    context.getRuleId(),
                    context.getRuleName(),
                    null,
                    "自定义函数类名未配置",
                    System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli(),
                    startTime
            );
        }

        if (!functionInvoker.isFunctionAvailable(className)) {
            return ExecutionResult.failed(
                    context.getRuleId(),
                    context.getRuleName(),
                    null,
                    "自定义函数未注册或不可用: " + className,
                    System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli(),
                    startTime
            );
        }

        // Build params map
        Map<String, Object> params = buildParams(context, paramsJson);

        // Invoke the function
        RuleFunction.RuleFunctionResult result = functionInvoker.invoke(className, params, adapter);

        long elapsedMs = System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli();

        if (result == null) {
            return ExecutionResult.failed(
                    context.getRuleId(),
                    context.getRuleName(),
                    null,
                    "函数执行返回空结果",
                    elapsedMs,
                    startTime
            );
        }

        // Build details map
        Map<String, Object> details = new HashMap<>();
        details.put("customFunctionClass", className);
        details.put("customFunctionParams", paramsJson);
        if (result.details() != null) {
            details.putAll(result.details());
        }

        if (result.passed()) {
            int qualityScore = calculateScore(result.actualValue());
            return ExecutionResult.builder()
                    .ruleId(context.getRuleId())
                    .ruleName(context.getRuleName())
                    .ruleType(context.getRuleType())
                    .status(ExecutionResult.Status.SUCCESS)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .elapsedMs(elapsedMs)
                    .totalCount(1L)
                    .passCount(1L)
                    .errorCount(0L)
                    .actualValue(result.actualValue() != null ? new BigDecimal(result.actualValue()) : null)
                    .qualityScore(qualityScore)
                    .details(details)
                    .build();
        } else {
            return ExecutionResult.builder()
                    .ruleId(context.getRuleId())
                    .ruleName(context.getRuleName())
                    .ruleType(context.getRuleType())
                    .status(ExecutionResult.Status.FAILED)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .elapsedMs(elapsedMs)
                    .totalCount(1L)
                    .passCount(0L)
                    .errorCount(1L)
                    .actualValue(result.actualValue() != null ? new BigDecimal(result.actualValue()) : null)
                    .qualityScore(0)
                    .errorDetail(result.errorMessage())
                    .details(details)
                    .build();
        }
    }

    private Map<String, Object> buildParams(RuleContext context, String paramsJson) {
        Map<String, Object> params = new HashMap<>();

        // Always inject context info
        params.put("ruleId", context.getRuleId());
        params.put("ruleName", context.getRuleName());
        params.put("ruleCode", context.getRuleCode());

        // Inject target info
        if (context.getTargetTable() != null) {
            params.put("tableName", context.getTargetTable());
        }
        if (context.getTargetColumn() != null) {
            params.put("columnName", context.getTargetColumn());
        }
        if (context.getTargetDsId() != null) {
            params.put("dsId", context.getTargetDsId());
        }

        // Inject threshold info
        if (context.getThresholdMin() != null) {
            params.put("thresholdMin", context.getThresholdMin());
        }
        if (context.getThresholdMax() != null) {
            params.put("thresholdMax", context.getThresholdMax());
        }
        if (context.getFluctuationThreshold() != null) {
            params.put("fluctuationThreshold", context.getFluctuationThreshold());
        }

        // Parse and merge custom params JSON
        if (paramsJson != null && !paramsJson.isBlank()) {
            try {
                Map<String, Object> customParams = JSON.parseObject(paramsJson);
                if (customParams != null) {
                    params.putAll(customParams);
                }
            } catch (Exception e) {
                log.warn("Failed to parse custom function params JSON: {}", paramsJson, e);
            }
        }

        return params;
    }

    private int calculateScore(String actualValue) {
        if (actualValue == null || actualValue.isBlank()) {
            return 100;
        }
        try {
            double value = Double.parseDouble(actualValue);
            if (value < 0 || value > 100) {
                return 100;
            }
            return (int) Math.round(value);
        } catch (NumberFormatException e) {
            return 100;
        }
    }
}
