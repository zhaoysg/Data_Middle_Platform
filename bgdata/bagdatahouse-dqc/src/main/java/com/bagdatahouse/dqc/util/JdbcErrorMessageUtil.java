package com.bagdatahouse.dqc.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 JDBC / Spring 抛出的冗长异常信息整理为便于展示的文案。
 */
public final class JdbcErrorMessageUtil {

    private static final Pattern TRAILING_QUOTED_SQL = Pattern.compile(":\\s*'([^']*)'\\s*$");

    private JdbcErrorMessageUtil() {
    }

    public static String humanize(Throwable t) {
        if (t == null) {
            return "未知错误";
        }
        String msg = t.getMessage();
        if (msg == null || msg.isBlank()) {
            return t.getClass().getSimpleName();
        }
        String lower = msg.toLowerCase();

        // Druid Wall 误判
        if (lower.contains("sql injection violation") && lower.contains("druid")) {
            String sql = extractTrailingQuotedSql(msg);
            StringBuilder sb = new StringBuilder(
                    "数据源连接池的 Druid SQL 防火墙（Wall）拦截了本次语句，多为合法只读 SQL 被解析器误判。");
            if (sql != null && !sql.isBlank()) {
                sb.append(" 语句：").append(sql);
            }
            return sb.toString();
        }

        // 连接超时 / 无法连接
        if (lower.contains("connect") || lower.contains("connection")
                || lower.contains("timeout") || lower.contains("refused")
                || lower.contains("unreachable") || lower.contains("network")) {
            String dbType = inferDbType(msg, lower);
            return switch (dbType) {
                case "postgresql" -> buildConnectionError("PostgreSQL", msg);
                case "oracle"     -> buildConnectionError("Oracle", msg);
                case "sqlserver"  -> buildConnectionError("SQL Server", msg);
                default           -> buildConnectionError("数据库", msg);
            };
        }

        // 认证失败
        if (lower.contains("access denied") || lower.contains("authentication failed")
                || lower.contains("password authentication") || lower.contains("login failed")
                || lower.contains("ora-01017") || lower.contains("ora-28000")) {
            return "认证失败：请检查用户名/密码是否正确，或该账户是否已被锁定/过期。";
        }

        // 权限不足
        if (lower.contains("access denied") && lower.contains("to database")
                || lower.contains("insufficient privilege") || lower.contains("ora-00942")
                || lower.contains("permission denied") || lower.contains("error 1044")
                || lower.contains("error 1045")) {
            return "权限不足：当前用户无权访问目标库/表，请联系 DBA 授权。";
        }

        // 数据库不存在
        if (lower.contains("unknown database") || lower.contains("database does not exist")
                || lower.contains("ora-12154") || lower.contains("ora-12505")) {
            return "数据库不存在或无法解析服务名，请检查数据库名称/TNS 服务名配置是否正确。";
        }

        // 表/列不存在
        if (lower.contains("table") && (lower.contains("doesn't exist") || lower.contains("not found"))
                || lower.contains("ora-00942") || lower.contains("invalid object name")
                || lower.contains("error 1146")) {
            String table = extractTableName(msg);
            return table != null
                    ? "表「" + table + "」不存在，请确认质检规则配置的目标表是否正确。"
                    : "表不存在，请确认质检规则配置的目标表是否正确。";
        }

        // 语法错误
        if (lower.contains("syntax error") || lower.contains("sql syntax")
                || lower.contains("ora-00900") || lower.contains("ora-01756")
                || lower.contains("error 1064") || lower.contains("error 1065")) {
            return "SQL 语法错误，请检查规则表达式语法是否正确。"
                    + truncate("（" + extractSyntaxError(msg) + "）", 200);
        }

        // Spring StatementCallback 通用截取
        if (msg.startsWith("StatementCallback") || msg.contains(" uncategorized SQLException")) {
            int semi = msg.indexOf("]; ");
            if (semi >= 0 && semi + 3 < msg.length()) {
                String tail = msg.substring(semi + 3).trim();
                if (!tail.isEmpty()) {
                    return truncate(tail, 1200);
                }
            }
        }

        return truncate(msg, 1200);
    }

    private static String buildConnectionError(String dbName, String msg) {
        if (msg.contains("timeout") || msg.contains("Timeout")) {
            return dbName + " 连接超时，请检查：1. 主机/端口是否可达；2. 防火墙是否放行；3. 数据库是否达到最大连接数。";
        }
        if (msg.contains("refused") || msg.contains("Connection refused")) {
            return dbName + " 连接被拒绝，请检查：1. 主机/端口是否正确；2. " + dbName + " 服务是否已启动。";
        }
        return dbName + " 连接失败，请检查主机、端口、网络联通性及账号权限。" + truncate("（" + msg + "）", 300);
    }

    private static String inferDbType(String msg, String lower) {
        if (lower.contains("postgresql") || lower.contains("psql")
                || lower.contains("org.postgresql") || lower.contains("pg")
                || lower.contains("org.hibernate")) {
            return "postgresql";
        }
        if (lower.contains("oracle") || lower.contains("ojdbc")
                || lower.contains("ora-")) {
            return "oracle";
        }
        if (lower.contains("sql server") || lower.contains("microsoft")
                || lower.contains("jtds") || lower.contains("mssql")) {
            return "sqlserver";
        }
        return "unknown";
    }

    private static String extractTableName(String msg) {
        // Oracle: "table or view does not exist: SCHEMA.TAB"
        int lastDot = msg.lastIndexOf('.');
        if (lastDot > 0 && lastDot + 1 < msg.length()) {
            String tail = msg.substring(lastDot + 1).trim();
            int end = tail.indexOf(' ');
            return end > 0 ? tail.substring(0, end).replace("\"", "") : tail.replace("\"", "");
        }
        return null;
    }

    private static String extractSyntaxError(String msg) {
        int pos = msg.indexOf("position");
        if (pos > 0) {
            int start = Math.max(0, pos - 80);
            int end = Math.min(msg.length(), pos + 60);
            return msg.substring(start, end);
        }
        int line = msg.indexOf("line");
        if (line > 0) {
            int start = Math.max(0, line - 60);
            int end = Math.min(msg.length(), line + 80);
            return msg.substring(start, end);
        }
        return msg;
    }

    private static String extractTrailingQuotedSql(String msg) {
        Matcher m = TRAILING_QUOTED_SQL.matcher(msg.trim());
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }
}
