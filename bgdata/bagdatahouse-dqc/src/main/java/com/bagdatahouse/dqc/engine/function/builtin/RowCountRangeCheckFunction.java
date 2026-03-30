package com.bagdatahouse.dqc.engine.function.builtin;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.dqc.engine.function.RuleFunction;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记录数范围检查函数
 * 检查表的记录数是否在指定范围内
 */
public class RowCountRangeCheckFunction implements RuleFunction {

    @Override
    public String getName() {
        return "ROW_COUNT_RANGE_CHECK";
    }

    @Override
    public String getDescription() {
        return "检查表的记录数是否在指定范围内（minCount <= rowCount <= maxCount）";
    }

    @Override
    public Map<String, String> getParameterSpecs() {
        Map<String, String> specs = new HashMap<>();
        specs.put("tableName", "目标表名");
        specs.put("minCount", "最小记录数（可为空）");
        specs.put("maxCount", "最大记录数（可为空）");
        return specs;
    }

    @Override
    public RuleFunctionResult execute(Map<String, Object> params, DataSourceAdapter adapter) {
        String tableName = (String) params.get("tableName");
        if (tableName == null || tableName.isBlank()) {
            return RuleFunctionResult.failed("表名不能为空", null);
        }

        BigDecimal minCount = parseDecimal(params.get("minCount"));
        BigDecimal maxCount = parseDecimal(params.get("maxCount"));

        if (minCount == null && maxCount == null) {
            return RuleFunctionResult.failed("请指定最小记录数或最大记录数", null);
        }

        try {
            long rowCount = adapter.getRowCount(tableName);
            Map<String, Object> details = new HashMap<>();
            details.put("tableName", tableName);
            details.put("actualRowCount", rowCount);
            details.put("minCount", minCount);
            details.put("maxCount", maxCount);

            boolean passed = true;
            StringBuilder reason = new StringBuilder();
            if (minCount != null && rowCount < minCount.longValue()) {
                passed = false;
                reason.append("记录数").append(rowCount).append("小于最小值").append(minCount);
            }
            if (maxCount != null && rowCount > maxCount.longValue()) {
                passed = false;
                if (reason.length() > 0) reason.append("; ");
                reason.append("记录数").append(rowCount).append("大于最大值").append(maxCount);
            }

            if (passed) {
                return RuleFunctionResult.success(String.valueOf(rowCount), details);
            } else {
                return RuleFunctionResult.failed(String.valueOf(rowCount), reason.toString(), details);
            }
        } catch (Exception e) {
            return RuleFunctionResult.failed("执行失败: " + e.getMessage(), null);
        }
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
