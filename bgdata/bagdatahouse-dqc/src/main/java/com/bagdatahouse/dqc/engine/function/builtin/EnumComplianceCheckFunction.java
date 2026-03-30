package com.bagdatahouse.dqc.engine.function.builtin;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.dqc.engine.function.RuleFunction;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 字段值域合规性检查函数
 * 检查指定字段的所有值是否都属于预定义的枚举值集合
 */
public class EnumComplianceCheckFunction implements RuleFunction {

    private static final Pattern COMMA_PATTERN = Pattern.compile("\\s*,\\s*");

    @Override
    public String getName() {
        return "ENUM_COMPLIANCE_CHECK";
    }

    @Override
    public String getDescription() {
        return "检查字段所有值是否均属于预定义的枚举值集合";
    }

    @Override
    public Map<String, String> getParameterSpecs() {
        Map<String, String> specs = new HashMap<>();
        specs.put("tableName", "目标表名");
        specs.put("columnName", "目标列名");
        specs.put("validValues", "允许的枚举值（逗号分隔，如: 1,2,3）");
        return specs;
    }

    @Override
    public RuleFunctionResult execute(Map<String, Object> params, DataSourceAdapter adapter) {
        String tableName = getString(params, "tableName");
        String columnName = getString(params, "columnName");
        String validValuesStr = getString(params, "validValues");

        if (tableName == null || tableName.isBlank()) {
            return RuleFunctionResult.failed("表名不能为空", null);
        }
        if (columnName == null || columnName.isBlank()) {
            return RuleFunctionResult.failed("列名不能为空", null);
        }
        if (validValuesStr == null || validValuesStr.isBlank()) {
            return RuleFunctionResult.failed("枚举值列表不能为空", null);
        }

        String[] validValues = COMMA_PATTERN.split(validValuesStr.trim());
        String quotedCol = adapter.quoteIdentifier(columnName);
        String quotedTable = adapter.quoteIdentifier(tableName);

        try {
            String countSql = String.format(
                    "SELECT COUNT(*) as total, " +
                            "SUM(CASE WHEN %s IN (%s) THEN 1 ELSE 0 END) as valid_count " +
                            "FROM %s",
                    quotedCol,
                    formatInClause(validValues),
                    quotedTable
            );

            List<Map<String, Object>> results = adapter.executeQuery(countSql);
            if (results == null || results.isEmpty()) {
                return RuleFunctionResult.failed("查询结果为空", null);
            }

            Map<String, Object> row = results.get(0);
            Object totalObj = row.get("total");
            Object validObj = row.get("valid_count");

            long total = totalObj != null ? ((Number) totalObj).longValue() : 0;
            long validCount = validObj != null ? ((Number) validObj).longValue() : 0;
            long invalidCount = total - validCount;
            BigDecimal complianceRate = total > 0
                    ? BigDecimal.valueOf(validCount).divide(BigDecimal.valueOf(total), 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> details = new HashMap<>();
            details.put("tableName", tableName);
            details.put("columnName", columnName);
            details.put("totalCount", total);
            details.put("validCount", validCount);
            details.put("invalidCount", invalidCount);
            details.put("complianceRate", complianceRate);
            details.put("validValues", List.of(validValues));

            if (total == 0) {
                return RuleFunctionResult.failed("0", "表为空", details);
            }
            if (invalidCount == 0) {
                return RuleFunctionResult.success(complianceRate.toString(), details);
            } else {
                return RuleFunctionResult.failed(
                        complianceRate.toString(),
                        String.format("发现%d条不合规记录（枚举值外），合规率%.2f%%", invalidCount, complianceRate),
                        details
                );
            }
        } catch (Exception e) {
            return RuleFunctionResult.failed("执行失败: " + e.getMessage(), null);
        }
    }

    private String formatInClause(String[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("'").append(values[i].trim().replace("'", "''")).append("'");
        }
        return sb.toString();
    }

    private String getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString().trim() : null;
    }
}
