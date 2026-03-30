package org.dromara.datasource.adapter;

import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.manager.DynamicDataSourceManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * SQL Server 数据源适配器
 *
 * @author Lion Li
 */
@Slf4j
@Component
public class SqlServerAdapter implements DataSourceAdapter {

    private Long dsId;
    private final DynamicDataSourceManager dataSourceManager;

    public SqlServerAdapter(DynamicDataSourceManager dataSourceManager) {
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
        return "SQLSERVER";
    }

    @Override
    public boolean testConnection() {
        try {
            getJdbcTemplate().execute("SELECT 1");
            return true;
        } catch (Exception e) {
            log.error("SQL Server连接测试失败", e);
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
    public int executeUpdate(String sql) {
        return getJdbcTemplate().update(sql);
    }

    @Override
    public int executeUpdate(String sql, Object... params) {
        return getJdbcTemplate().update(sql, params);
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) {
        String sql = """
            SELECT
                c.name as columnName,
                t.name as dataType,
                ep.value as columnComment,
                c.is_nullable as nullable,
                CASE WHEN pk.column_id IS NOT NULL THEN 1 ELSE 0 END as primaryKey
            FROM sys.columns c
            JOIN sys.types t ON c.user_type_id = t.user_type_id
            LEFT JOIN sys.extended_properties ep ON ep.major_id = c.object_id AND ep.minor_id = c.column_id AND ep.name = 'MS_Description'
            LEFT JOIN (
                SELECT kc.column_id, kc.object_id
                FROM sys.key_constraints k
                JOIN sys.index_columns kc ON k.parent_object_id = kc.object_id AND k.name = kc.name
                WHERE k.type = 'PK'
            ) pk ON pk.column_id = c.column_id AND pk.object_id = c.object_id
            WHERE c.object_id = OBJECT_ID(?)
            ORDER BY c.column_id
            """;
        return getJdbcTemplate().query(sql, new SqlServerColumnInfoRowMapper(), tableName);
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) {
        String sql = """
            SELECT COLUMN_NAME
            FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
            WHERE TABLE_NAME = ? AND CONSTRAINT_NAME LIKE 'PK_%'
            ORDER BY ORDINAL_POSITION
            """;
        return getJdbcTemplate().queryForList(sql, String.class, tableName);
    }

    @Override
    public List<String> getTables() {
        String sql = """
            SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_NAME NOT LIKE 'sys%'
            ORDER BY TABLE_NAME
            """;
        return getJdbcTemplate().queryForList(sql, String.class);
    }

    @Override
    public long getRowCount(String tableName) {
        String safeName = quoteIdentifier(tableName);
        String sql = "SELECT COUNT(*) FROM " + safeName;
        Long count = getJdbcTemplate().queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public String buildPaginationSql(String sql, long offset, long limit) {
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public String buildCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") AS _cnt";
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "[" + identifier.replace("]", "]]") + "]";
    }

    @Override
    public List<Map<String, Object>> sampleColumnValues(String tableName, String columnName, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        String qCol = quoteIdentifier(columnName);
        String qTbl = quoteIdentifier(tableName);
        String sql = "SELECT TOP " + safeLimit + " " + qCol + " FROM " + qTbl + " WHERE " + qCol + " IS NOT NULL";
        return getJdbcTemplate().queryForList(sql);
    }

    private static class SqlServerColumnInfoRowMapper implements RowMapper<ColumnInfo> {
        @Override
        public ColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ColumnInfo(
                rs.getString("columnName"),
                rs.getString("dataType"),
                rs.getString("columnComment"),
                "YES".equalsIgnoreCase(rs.getString("nullable")),
                rs.getInt("primaryKey") == 1
            );
        }
    }
}
