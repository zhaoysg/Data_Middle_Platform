package com.bagdatahouse.dqc.engine.executor;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.bagdatahouse.dqc.engine.RuleContext;
import com.bagdatahouse.dqc.util.JdbcErrorMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 表更新时效：执行规则表达式（通常为 SELECT MAX(时间列) FROM 表），将结果与当前时间比较。
 * <p>
 * 若配置了 {@code thresholdMax}，解释为「最大允许延迟」的小时数；超出则质检失败。
 * {@code actualValue} 为相对当前时间的延迟分钟数，供质量评分等下游使用。
 */
@Component
public class TableUpdateTimelinessExecutor implements RuleExecutor {

    private static final Logger log = LoggerFactory.getLogger(TableUpdateTimelinessExecutor.class);

    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
            ".*(DROP|DELETE|TRUNCATE|ALTER|CREATE|INSERT|UPDATE|GRANT|REVOKE)\\s+",
            Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter[] TS_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    };

    @Override
    public String getRuleType() {
        return "TABLE_UPDATE_TIMELINESS";
    }

    @Override
    public ExecutionResult execute(RuleContext context, DataSourceAdapter adapter) {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            String ruleExpr = context.getRuleExpr();
            if (ruleExpr == null || ruleExpr.isBlank()) {
                return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                        "表更新时效规则需要配置规则表达式（如 SELECT MAX(时间列) FROM ${table}）",
                        elapsedMs(startTime), startTime);
            }

            String sql = interpolateRuleExpr(ruleExpr, context, adapter);
            validateSql(sql);

            log.debug("Executing TABLE_UPDATE_TIMELINESS: {}", sql);
            List<Map<String, Object>> results = adapter.executeQuery(sql);
            if (results == null || results.isEmpty()) {
                return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                        "查询无结果", elapsedMs(startTime), startTime);
            }

            Object raw = results.get(0).values().iterator().next();
            Optional<Instant> lastUpdateOpt = parseToInstant(raw);
            if (lastUpdateOpt.isEmpty()) {
                String hint = (raw == null)
                        ? "查询结果为 NULL（请确认库/Schema 是否正确、表是否有数据，以及 SQL 是否限定到正确库）"
                        : "无法解析最后更新时间: " + raw;
                return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                        hint,
                        elapsedMs(startTime), startTime);
            }

            Instant lastUpdate = lastUpdateOpt.get();
            Instant now = Instant.now();
            if (lastUpdate.isAfter(now)) {
                lastUpdate = now;
            }

            long delayMinutes = ChronoUnit.MINUTES.between(lastUpdate, now);
            BigDecimal actualValue = BigDecimal.valueOf(delayMinutes);

            BigDecimal thresholdMax = context.getThresholdMax();
            boolean passed = true;
            String errorDetail = null;
            if (thresholdMax != null) {
                double maxHours = thresholdMax.doubleValue();
                double delayHours = delayMinutes / 60.0;
                if (delayHours > maxHours) {
                    passed = false;
                    errorDetail = String.format("数据更新延迟超过阈值：当前约 %.2f 小时，阈值 %.2f 小时",
                            delayHours, maxHours);
                }
            }

            int qualityScore = passed ? 100 : 0;
            long totalCount = 1L;
            long errorCount = passed ? 0L : 1L;
            long passCount = passed ? 1L : 0L;

            if (passed) {
                return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                        totalCount, passCount, errorCount, actualValue, qualityScore,
                        elapsedMs(startTime), startTime);
            }
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                    errorDetail, elapsedMs(startTime), startTime);

        } catch (IllegalArgumentException e) {
            log.error("TABLE_UPDATE_TIMELINESS 参数非法: {}", e.getMessage());
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    e.getMessage(), elapsedMs(startTime), startTime);
        } catch (Exception e) {
            log.error("TABLE_UPDATE_TIMELINESS 执行异常: ruleId={}", context.getRuleId(), e);
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "执行异常: " + JdbcErrorMessageUtil.humanize(e), elapsedMs(startTime), startTime);
        }
    }

    private String interpolateRuleExpr(String ruleExpr, RuleContext context, DataSourceAdapter adapter) {
        String result = ruleExpr;
        if (context.getTargetDsId() != null) {
            result = result.replace("${dsId}", context.getTargetDsId().toString());
        }
        if (StringUtils.hasText(context.getTargetDatabaseName())) {
            result = result.replace("${database}", context.getTargetDatabaseName());
        }
        if (StringUtils.hasText(context.getTargetSchemaName())) {
            result = result.replace("${schema}", context.getTargetSchemaName());
        }
        String qualifiedTable = buildQualifiedTable(context, adapter);
        if (qualifiedTable != null) {
            result = result.replace("${qualified_table}", qualifiedTable);
        }
        if (context.getTargetTable() != null) {
            String tableToken = qualifiedTable != null ? qualifiedTable : context.getTargetTable();
            result = result.replace("${table}", tableToken);
        }
        if (context.getTargetColumn() != null) {
            String qCol = adapter != null ? adapter.quoteIdentifier(context.getTargetColumn())
                    : context.getTargetColumn();
            result = result.replace("${column}", qCol);
            result = result.replace("${partition_column}", qCol);
        }
        if (context.getThresholdMin() != null) {
            result = result.replace("${thresholdMin}", context.getThresholdMin().toPlainString());
        }
        if (context.getThresholdMax() != null) {
            result = result.replace("${thresholdMax}", context.getThresholdMax().toPlainString());
        }
        return result;
    }

    /**
     * 按数据源类型生成带库/Schema 的限定表名，避免多库环境下只写表名查错库。
     */
    private String buildQualifiedTable(RuleContext ctx, DataSourceAdapter adapter) {
        if (adapter == null || !StringUtils.hasText(ctx.getTargetTable())) {
            return null;
        }
        String table = ctx.getTargetTable().trim();
        if (table.contains(".") && !table.contains("`") && !table.contains("\"") && !table.contains("[")) {
            String[] parts = table.split("\\.", 2);
            return adapter.quoteTableName(parts[0].trim(), parts[1].trim());
        }
        String type = adapter.getDataSourceType();
        if (type == null) {
            return null;
        }
        String upper = type.toUpperCase();
        if ("MYSQL".equals(upper) || "TIDB".equals(upper)) {
            String db = StringUtils.hasText(ctx.getTargetDatabaseName())
                    ? ctx.getTargetDatabaseName().trim() : null;
            if (!StringUtils.hasText(db)) {
                try {
                    db = adapter.getDatabaseName();
                } catch (Exception e) {
                    log.debug("读取当前库名失败: {}", e.getMessage());
                }
            }
            if (StringUtils.hasText(db)) {
                return adapter.quoteTableName(db, table);
            }
        } else if ("POSTGRESQL".equals(upper)) {
            String schema = StringUtils.hasText(ctx.getTargetSchemaName())
                    ? ctx.getTargetSchemaName().trim() : "public";
            return adapter.quoteTableName(schema, table);
        } else if ("SQLSERVER".equals(upper)) {
            String schema = StringUtils.hasText(ctx.getTargetSchemaName())
                    ? ctx.getTargetSchemaName().trim() : "dbo";
            return adapter.quoteTableName(schema, table);
        } else if ("ORACLE".equals(upper)) {
            if (StringUtils.hasText(ctx.getTargetSchemaName())) {
                return adapter.quoteTableName(ctx.getTargetSchemaName().trim(), table);
            }
        }
        return adapter.quoteIdentifier(table);
    }

    private void validateSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        if (DANGEROUS_SQL_PATTERN.matcher(sql).matches()) {
            throw new IllegalArgumentException(
                    "SQL 包含危险操作（DROP/DELETE/TRUNCATE/ALTER/CREATE/INSERT/UPDATE/GRANT/REVOKE）");
        }
        String upper = sql.toUpperCase();
        if (upper.contains("DROP TABLE") || upper.contains("DELETE FROM") || upper.contains("TRUNCATE TABLE")) {
            throw new IllegalArgumentException("SQL 包含禁止的数据变更语句");
        }
    }

    private Optional<Instant> parseToInstant(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Timestamp ts) {
            return Optional.of(ts.toInstant());
        }
        if (value instanceof Date d) {
            return Optional.of(d.toInstant());
        }
        if (value instanceof LocalDateTime ldt) {
            return Optional.of(ldt.atZone(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof Instant i) {
            return Optional.of(i);
        }
        if (value instanceof Number n) {
            long lv = n.longValue();
            if (lv > 1_000_000_000_000L) {
                return Optional.of(Instant.ofEpochMilli(lv));
            }
            if (lv > 1_000_000_000L) {
                return Optional.of(Instant.ofEpochSecond(lv));
            }
            return Optional.empty();
        }
        String s = value.toString().trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Timestamp.valueOf(s).toInstant());
        } catch (IllegalArgumentException ignored) {
            // try formatters below
        }
        for (DateTimeFormatter f : TS_FORMATTERS) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(s, f);
                return Optional.of(ldt.atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
                // next
            }
        }
        try {
            long lv = Long.parseLong(s);
            if (lv > 1_000_000_000_000L) {
                return Optional.of(Instant.ofEpochMilli(lv));
            }
            return Optional.of(Instant.ofEpochSecond(lv));
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return Optional.empty();
    }

    private long elapsedMs(LocalDateTime startTime) {
        return System.currentTimeMillis() - startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
