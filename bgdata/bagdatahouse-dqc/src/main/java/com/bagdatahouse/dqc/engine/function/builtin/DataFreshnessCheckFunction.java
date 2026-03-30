package com.bagdatahouse.dqc.engine.function.builtin;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.dqc.engine.function.RuleFunction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataFreshnessCheckFunction implements RuleFunction {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
    };

    @Override
    public String getName() {
        return "DATA_FRESHNESS_CHECK";
    }

    @Override
    public String getDescription() {
        return "Check if the latest record timestamp in the table is within the specified threshold";
    }

    @Override
    public Map<String, String> getParameterSpecs() {
        Map<String, String> specs = new HashMap<>();
        specs.put("tableName", "Target table name");
        specs.put("timestampColumn", "Timestamp column name (used to find the latest record)");
        specs.put("maxAgeMinutes", "Max allowed age in minutes, e.g., 30");
        return specs;
    }

    @Override
    public RuleFunctionResult execute(Map<String, Object> params, DataSourceAdapter adapter) {
        String tableName = getString(params, "tableName");
        String timestampColumn = getString(params, "timestampColumn");
        Object maxAgeMinutesObj = params.get("maxAgeMinutes");

        if (tableName == null || tableName.isBlank()) {
            return RuleFunctionResult.failed("Table name cannot be empty", null);
        }
        if (timestampColumn == null || timestampColumn.isBlank()) {
            return RuleFunctionResult.failed("Timestamp column name cannot be empty", null);
        }
        if (maxAgeMinutesObj == null) {
            return RuleFunctionResult.failed("Max age cannot be empty", null);
        }

        long maxAgeMinutes;
        try {
            maxAgeMinutes = new BigDecimal(maxAgeMinutesObj.toString()).longValue();
        } catch (NumberFormatException e) {
            return RuleFunctionResult.failed("Invalid max age format: " + maxAgeMinutesObj, null);
        }

        String quotedCol = adapter.quoteIdentifier(timestampColumn);
        String quotedTable = adapter.quoteIdentifier(tableName);

        try {
            String sql = String.format("SELECT MAX(%s) as latest_ts FROM %s", quotedCol, quotedTable);
            List<Map<String, Object>> results = adapter.executeQuery(sql);
            if (results == null || results.isEmpty()) {
                return RuleFunctionResult.failed("Query returned no results", null);
            }

            Map<String, Object> row = results.get(0);
            Object latestObj = row.get("latest_ts");

            Map<String, Object> details = new HashMap<>();
            details.put("tableName", tableName);
            details.put("timestampColumn", timestampColumn);
            details.put("maxAgeMinutes", maxAgeMinutes);

            if (latestObj == null) {
                return RuleFunctionResult.failed("0", "No timestamp data in table", details);
            }

            LocalDateTime latest = parseTimestamp(latestObj.toString());
            if (latest == null) {
                return RuleFunctionResult.failed("Cannot parse timestamp: " + latestObj, null);
            }

            LocalDateTime now = LocalDateTime.now();
            long actualMinutes = ChronoUnit.MINUTES.between(latest, now);

            details.put("latestTimestamp", latest.toString());
            details.put("checkTime", now.toString());
            details.put("actualAgeMinutes", actualMinutes);

            if (actualMinutes <= maxAgeMinutes) {
                return RuleFunctionResult.success(String.valueOf(actualMinutes), details);
            } else {
                return RuleFunctionResult.failed(
                        String.valueOf(actualMinutes),
                        String.format("Data freshness threshold exceeded (expected %d min, actual %d min)", maxAgeMinutes, actualMinutes),
                        details
                );
            }
        } catch (Exception e) {
            return RuleFunctionResult.failed("Execution failed: " + e.getMessage(), null);
        }
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null) return null;
        String v = value.trim();
        try {
            String toParse = v.length() > 19 ? v.substring(0, 19) : v;
            return LocalDateTime.parse(toParse);
        } catch (Exception e1) {
            try {
                String datePart = v.length() > 10 ? v.substring(0, 10) : v;
                return LocalDate.parse(datePart).atStartOfDay();
            } catch (Exception e2) {
                for (DateTimeFormatter fmt : DATE_FORMATS) {
                    try {
                        return LocalDateTime.parse(v, fmt);
                    } catch (Exception e3) {
                    }
                }
                try {
                    long millis = Long.parseLong(v);
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
                } catch (Exception e4) {
                    return null;
                }
            }
        }
    }

    private String getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString().trim() : null;
    }
}
