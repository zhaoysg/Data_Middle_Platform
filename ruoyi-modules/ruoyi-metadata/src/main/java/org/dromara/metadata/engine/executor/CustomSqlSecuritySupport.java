package org.dromara.metadata.engine.executor;

import org.dromara.common.core.utils.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 自定义SQL安全辅助方法。
 */
public final class CustomSqlSecuritySupport {

    public static final String REDACTED_SQL = "[REDACTED_CUSTOM_SQL]";
    public static final String RESULT_TYPE_ERROR = "自定义SQL必须返回单个数值或时间结果";
    public static final String EXECUTION_ERROR = "自定义SQL执行失败，请联系管理员查看日志";

    private static final Pattern WRITE_KEYWORDS = Pattern.compile(
        "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|MERGE|EXEC|EXECUTE|CALL|GRANT|REVOKE)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HIGH_RISK_PATTERNS = Pattern.compile(
        "(;|--|/\\*|\\*/|\\bINTO\\s+OUTFILE\\b|\\bINTO\\s+DUMPFILE\\b|\\bLOAD_FILE\\b|\\bSLEEP\\s*\\(|\\bBENCHMARK\\s*\\(|\\bPG_SLEEP\\s*\\(|\\bPG_READ_FILE\\b|\\bPG_LO_READ\\b)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ALLOWED_RESULT_FUNCTIONS = Pattern.compile(
        "\\b(COUNT|SUM|AVG|MIN|MAX|TIMESTAMPDIFF|DATEDIFF|DATE_PART|EXTRACT)\\s*\\(",
        Pattern.CASE_INSENSITIVE
    );

    private CustomSqlSecuritySupport() {
    }

    public static void validateRuleExpr(String sql) {
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("自定义SQL不能为空");
        }
        String normalized = sql.trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!(upper.startsWith("SELECT") || upper.startsWith("WITH"))) {
            throw new IllegalArgumentException("自定义SQL仅支持只读 SELECT/WITH 查询");
        }
        if (WRITE_KEYWORDS.matcher(normalized).find() || HIGH_RISK_PATTERNS.matcher(normalized).find()) {
            throw new IllegalArgumentException("自定义SQL包含禁止使用的语句或高风险片段");
        }
        if (!ALLOWED_RESULT_FUNCTIONS.matcher(normalized).find()) {
            throw new IllegalArgumentException("自定义SQL必须返回聚合值或时间函数结果");
        }
    }

    public static boolean isAllowedResultType(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Number || value instanceof LocalDateTime || value instanceof LocalDate
            || value instanceof OffsetDateTime || value instanceof Timestamp) {
            return true;
        }
        String text = StringUtils.trim(value.toString());
        if (StringUtils.isBlank(text)) {
            return false;
        }
        return isNumeric(text) || isTemporal(text);
    }

    private static boolean isNumeric(String value) {
        try {
            new BigDecimal(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isTemporal(String value) {
        try {
            Timestamp.valueOf(value);
            return true;
        } catch (Exception ignored) {
            // ignore
        }
        try {
            LocalDateTime.parse(value);
            return true;
        } catch (Exception ignored) {
            // ignore
        }
        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (Exception ignored) {
            // ignore
        }
        try {
            LocalDate.parse(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
