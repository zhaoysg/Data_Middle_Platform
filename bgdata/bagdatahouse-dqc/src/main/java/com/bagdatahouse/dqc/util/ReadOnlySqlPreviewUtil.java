package com.bagdatahouse.dqc.util;

import com.bagdatahouse.common.exception.BusinessException;

import java.util.regex.Pattern;

/**
 * 规则表达式「预览查询」：仅允许单条只读 SELECT，并做基础危险语句拦截。
 */
public final class ReadOnlySqlPreviewUtil {

    private static final Pattern SELECT_START = Pattern.compile("(?is)^\\s*select\\b.*");

    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
            ".*(DROP|DELETE|TRUNCATE|ALTER|CREATE|INSERT|UPDATE|GRANT|REVOKE)\\s+",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private ReadOnlySqlPreviewUtil() {
    }

    /**
     * 去掉首尾单层单引号（常见误把整条 SQL 写在引号里）。
     *
     * @return 去掉后的字符串；若无外层引号则返回 trim 后的原串
     */
    public static String stripOuterSingleQuotes(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    public static boolean wasWrappedInOuterSingleQuotes(String raw) {
        if (raw == null) {
            return false;
        }
        String s = raw.trim();
        return s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'';
    }

    /**
     * 校验并返回可执行的 SQL（去掉末尾分号）。
     */
    public static String validateAndNormalizeSelect(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new BusinessException(400, "SQL 不能为空");
        }
        String s = sql.trim();
        int semi = s.indexOf(';');
        if (semi >= 0) {
            String after = s.substring(semi + 1).trim();
            if (!after.isEmpty()) {
                throw new BusinessException(400, "不允许执行多条 SQL 语句");
            }
            s = s.substring(0, semi).trim();
        }
        if (!SELECT_START.matcher(s).matches()) {
            throw new BusinessException(400, "预览仅允许单条 SELECT 查询");
        }
        String upper = s.toUpperCase();
        if (upper.contains(" INTO OUTFILE") || upper.contains(" INTO DUMPFILE")) {
            throw new BusinessException(400, "不允许在预览中使用文件导出类语句");
        }
        if (DANGEROUS_SQL_PATTERN.matcher(s).matches()) {
            throw new BusinessException(400, "SQL 包含禁止的数据变更类关键字");
        }
        if (upper.contains("DROP TABLE") || upper.contains("DELETE FROM") || upper.contains("TRUNCATE TABLE")) {
            throw new BusinessException(400, "SQL 包含禁止的数据变更语句");
        }
        return s;
    }
}
