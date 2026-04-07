package org.dromara.datasource.adapter;

import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.manager.DynamicDataSourceManager;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * MySQL / TiDB 数据源适配器
 * 兼容 MySQL 5.7+ 和 TiDB（MySQL 兼容协议）
 *
 * @author Lion Li
 */
@Slf4j
@Component
public class MySQLAdapter implements DataSourceAdapter {

    private Long dsId;
    private final DynamicDataSourceManager dataSourceManager;

    public MySQLAdapter(DynamicDataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    private JdbcTemplate getJdbcTemplate() {
        JdbcTemplate template = dataSourceManager.getJdbcTemplateById(dsId);
        if (template == null) {
            throw new IllegalStateException("数据源未注册: dsId=" + dsId);
        }
        return template;
    }

    @Override
    public void setDsId(Long dsId) {
        this.dsId = dsId;
    }

    @Override
    public Long getDsId() {
        return this.dsId;
    }

    @Override
    public String getDataSourceType() {
        return "MYSQL";
    }

    @Override
    public boolean testConnection() {
        try {
            getJdbcTemplate().execute("SELECT 1");
            return true;
        } catch (Exception e) {
            log.error("MySQL连接测试失败", e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql) {
        return getJdbcTemplate().queryForList(sql);
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        return getJdbcTemplate().queryForList(sql, params);
    }

    @Override
    public List<Map<String, Object>> executeQueryCancellable(String sql, Supplier<Boolean> cancelChecker) {
        return getJdbcTemplate().execute((ConnectionCallback<List<Map<String, Object>>>) conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> result = new ArrayList<>();
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                while (rs.next()) {
                    if (cancelChecker.get()) {
                        throw new SQLException("查询已被取消");
                    }
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(md.getColumnLabel(i), rs.getObject(i));
                    }
                    result.add(row);
                }
                return result;
            }
        });
    }

    @Override
    public List<Map<String, Object>> executeQueryCancellable(String sql, Supplier<Boolean> cancelChecker, Object... params) {
        return getJdbcTemplate().execute((ConnectionCallback<List<Map<String, Object>>>) conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    while (rs.next()) {
                        if (cancelChecker.get()) {
                            throw new SQLException("查询已被取消");
                        }
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= cols; i++) {
                            row.put(md.getColumnLabel(i), rs.getObject(i));
                        }
                        result.add(row);
                    }
                    return result;
                }
            }
        });
    }

    @Override
    public int executeUpdate(String sql) {
        return getJdbcTemplate().update(sql);
    }

    @Override
    public int executeUpdate(String sql, Object... params) {
        return getJdbcTemplate().update(sql, params);
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) {
        return getColumns(null, tableName);
    }

    @Override
    public List<ColumnInfo> getColumns(String schemaName, String tableName) {
        String schema = resolveSchemaName(schemaName);
        String sql = """
            SELECT
                COLUMN_NAME as columnName,
                DATA_TYPE as dataType,
                COLUMN_COMMENT as columnComment,
                IS_NULLABLE as isNullable,
                COLUMN_KEY as columnKey
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            ORDER BY ORDINAL_POSITION
            """;
        return getJdbcTemplate().query(sql, new ColumnInfoRowMapper(), schema, tableName);
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) {
        String schema = resolveSchemaName(null);
        String sql = """
            SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND CONSTRAINT_NAME = 'PRIMARY'
            ORDER BY ORDINAL_POSITION
            """;
        return getJdbcTemplate().queryForList(sql, String.class, schema, tableName);
    }

    @Override
    public String getTableComment(String schemaName, String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return "";
        }
        String schema = resolveSchemaName(schemaName);
        String sql = """
            SELECT TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            """;
        try {
            List<String> rows = getJdbcTemplate().query(sql, (rs, i) -> rs.getString(1), schema, tableName.trim());
            if (rows.isEmpty() || rows.get(0) == null) {
                return "";
            }
            return rows.get(0).trim();
        } catch (Exception e) {
            log.debug("读取表注释失败: {}", tableName, e);
            return "";
        }
    }

    @Override
    public List<String> getTables() {
        return getTables(null);
    }

    @Override
    public List<String> getTables(String schemaName) {
        String schema = resolveSchemaName(schemaName);
        String sql = """
            SELECT TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'
            ORDER BY TABLE_NAME
            """;
        return getJdbcTemplate().queryForList(sql, String.class, schema);
    }

    @Override
    public long getRowCount(String tableName) {
        return getRowCount(null, tableName);
    }

    @Override
    public long getRowCount(String schemaName, String tableName) {
        String schema = resolveSchemaName(schemaName);
        String qualifiedTable = quoteIdentifier(schema) + "." + quoteIdentifier(tableName);
        String sql = "SELECT COUNT(*) FROM " + qualifiedTable;
        Long count = getJdbcTemplate().queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    private String resolveSchemaName(String schemaName) {
        if (schemaName != null && !schemaName.isBlank()) {
            return schemaName.trim();
        }
        String databaseName = getDatabaseName();
        if (databaseName == null || databaseName.isBlank()) {
            throw new IllegalStateException("无法解析当前数据库名");
        }
        return databaseName.trim();
    }

    @Override
    public String buildPaginationSql(String sql, long offset, long limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String buildCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") AS _cnt";
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    @Override
    public List<Map<String, Object>> sampleColumnValues(String tableName, String columnName, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        String qCol = quoteIdentifier(columnName);
        String qTbl = quoteIdentifier(tableName);
        String sql = "SELECT " + qCol + " FROM " + qTbl + " WHERE " + qCol + " IS NOT NULL LIMIT " + safeLimit;
        return getJdbcTemplate().queryForList(sql);
    }

    @Override
    public String getDatabaseName() {
        List<Map<String, Object>> result = executeQuery("SELECT DATABASE() as db");
        if (!result.isEmpty()) {
            Object db = result.get(0).get("db");
            return db != null ? db.toString() : null;
        }
        return null;
    }

    @Override
    public java.util.Optional<String> getTableLastUpdateTime(String schemaName, String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return java.util.Optional.empty();
        }
        String schema = (schemaName != null && !schemaName.isBlank()) ? schemaName.trim() : getDatabaseName();
        if (schema == null || schema.isBlank()) {
            return java.util.Optional.empty();
        }
        String sql = """
            SELECT COALESCE(UPDATE_TIME, CREATE_TIME) AS ut
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            """;
        try {
            List<Map<String, Object>> rows = executeQuery(sql, schema, tableName.trim());
            if (!rows.isEmpty()) {
                Object val = rows.get(0).get("ut");
                if (val != null) {
                    if (val instanceof Timestamp ts) {
                        return java.util.Optional.of(ts.toLocalDateTime().toString());
                    }
                    return java.util.Optional.of(val.toString());
                }
            }
        } catch (Exception e) {
            log.debug("获取表最后更新时间失败: {}", tableName, e);
        }
        return java.util.Optional.empty();
    }

    private static class ColumnInfoRowMapper implements RowMapper<ColumnInfo> {
        @Override
        public ColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ColumnInfo(
                rs.getString("columnName"),
                rs.getString("dataType"),
                rs.getString("columnComment"),
                "YES".equals(rs.getString("isNullable")),
                "PRI".equals(rs.getString("columnKey"))
            );
        }
    }
}
