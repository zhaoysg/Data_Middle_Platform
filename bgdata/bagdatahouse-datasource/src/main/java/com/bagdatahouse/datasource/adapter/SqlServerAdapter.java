package com.bagdatahouse.datasource.adapter;

import com.bagdatahouse.datasource.manager.DynamicDataSourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * SQL Server 数据源适配器
 * 支持 SQL Server 2012+（使用 OFFSET FETCH 分页语法）
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
                c.COLUMN_NAME as columnName,
                c.DATA_TYPE as dataType,
                ep.value as columnComment,
                c.CHARACTER_MAXIMUM_LENGTH as columnSize,
                c.NUMERIC_PRECISION as numericPrecision,
                c.NUMERIC_SCALE as numericScale,
                c.IS_NULLABLE as nullable,
                c.COLUMN_DEFAULT as defaultValue,
                CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 1 ELSE 0 END as primaryKey
            FROM INFORMATION_SCHEMA.COLUMNS c
            LEFT JOIN sys.columns sa ON sa.name = c.COLUMN_NAME AND OBJECT_ID(c.TABLE_NAME) = sa.object_id
            LEFT JOIN sys.extended_properties ep ON ep.major_id = sa.object_id AND ep.minor_id = sa.column_id AND ep.name = 'MS_Description'
            LEFT JOIN (
                SELECT ku.COLUMN_NAME
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku ON tc.CONSTRAINT_NAME = ku.CONSTRAINT_NAME
                WHERE tc.TABLE_NAME = ? AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
            ) pk ON pk.COLUMN_NAME = c.COLUMN_NAME
            WHERE c.TABLE_NAME = ?
            ORDER BY c.ORDINAL_POSITION
            """;
        return getJdbcTemplate().query(sql, new ColumnInfoRowMapper(), tableName, tableName);
    }

    @Override
    public List<ColumnInfo> getColumns(String schemaName, String tableName) {
        String sql = """
            SELECT
                c.COLUMN_NAME as columnName,
                c.DATA_TYPE as dataType,
                c.CHARACTER_MAXIMUM_LENGTH as columnSize,
                c.NUMERIC_PRECISION as numericPrecision,
                c.NUMERIC_SCALE as numericScale,
                c.IS_NULLABLE as nullable,
                c.COLUMN_DEFAULT as defaultValue,
                CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 1 ELSE 0 END as primaryKey
            FROM INFORMATION_SCHEMA.COLUMNS c
            LEFT JOIN (
                SELECT ku.COLUMN_NAME
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku ON tc.CONSTRAINT_NAME = ku.CONSTRAINT_NAME
                WHERE tc.TABLE_SCHEMA = ? AND tc.TABLE_NAME = ? AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
            ) pk ON pk.COLUMN_NAME = c.COLUMN_NAME
            WHERE c.TABLE_SCHEMA = ? AND c.TABLE_NAME = ?
            ORDER BY c.ORDINAL_POSITION
            """;
        return getJdbcTemplate().query(sql, new ColumnInfoRowMapperNoComment(),
                schemaName, tableName, schemaName, tableName);
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) {
        String sql = """
            SELECT COLUMN_NAME
            FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
            WHERE TABLE_NAME = ? AND CONSTRAINT_NAME LIKE 'PK%'
            ORDER BY ORDINAL_POSITION
            """;
        return getJdbcTemplate().queryForList(sql, String.class, tableName);
    }

    @Override
    public String getTableComment(String schemaName, String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return "";
        }
        String schema = (schemaName != null && !schemaName.isBlank()) ? schemaName.trim() : "dbo";
        String sql = """
            SELECT CAST(ep.value AS NVARCHAR(2000))
            FROM sys.tables t
            INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
            LEFT JOIN sys.extended_properties ep ON ep.major_id = t.object_id AND ep.minor_id = 0
                AND ep.class = 1 AND ep.name = 'MS_Description'
            WHERE t.name = ? AND s.name = ?
            """;
        try {
            List<String> rows = getJdbcTemplate().query(sql, (rs, i) -> rs.getString(1), tableName.trim(), schema);
            if (rows.isEmpty() || rows.get(0) == null) {
                return "";
            }
            return rows.get(0).trim();
        } catch (Exception e) {
            log.debug("读取 SQL Server 表注释失败: {}.{}", schema, tableName, e);
            return "";
        }
    }

    @Override
    public List<String> getTables() {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'";
        return getJdbcTemplate().queryForList(sql).stream()
            .map(row -> row.values().iterator().next().toString())
            .toList();
    }

    @Override
    public List<String> getTables(String schemaName) {
        String sql = """
            SELECT TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'
            """;
        return getJdbcTemplate().queryForList(sql, String.class, schemaName);
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
    public String quoteTableName(String schema, String table) {
        if (schema == null || schema.isBlank()) {
            return quoteIdentifier(table);
        }
        return quoteIdentifier(schema) + "." + quoteIdentifier(table);
    }

    @Override
    public List<Map<String, Object>> sampleColumnValues(String tableName, String columnName, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        String qCol = quoteIdentifier(columnName);
        String qTbl = quoteIdentifier(tableName);
        String sql = "SELECT TOP " + safeLimit + " " + qCol + " FROM " + qTbl + " WHERE " + qCol + " IS NOT NULL";
        return getJdbcTemplate().queryForList(sql);
    }

    @Override
    public List<ColumnInfo> getColumnsFromInformationSchema(String schema, String tableName) {
        String sql = """
            SELECT
                c.COLUMN_NAME as columnName,
                c.DATA_TYPE as dataType,
                ep.value as columnComment,
                c.CHARACTER_MAXIMUM_LENGTH as columnSize,
                c.NUMERIC_PRECISION as numericPrecision,
                c.NUMERIC_SCALE as numericScale,
                c.IS_NULLABLE as nullable,
                c.COLUMN_DEFAULT as defaultValue,
                CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 1 ELSE 0 END as primaryKey
            FROM INFORMATION_SCHEMA.COLUMNS c
            LEFT JOIN sys.columns sa ON sa.name = c.COLUMN_NAME AND OBJECT_ID(c.TABLE_NAME) = sa.object_id
            LEFT JOIN sys.extended_properties ep ON ep.major_id = sa.object_id AND ep.minor_id = sa.column_id AND ep.name = 'MS_Description'
            LEFT JOIN (
                SELECT ku.COLUMN_NAME
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku ON tc.CONSTRAINT_NAME = ku.CONSTRAINT_NAME
                WHERE tc.TABLE_NAME = ? AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
            ) pk ON pk.COLUMN_NAME = c.COLUMN_NAME
            WHERE c.TABLE_NAME = ?
            ORDER BY c.ORDINAL_POSITION
            """;
        return getJdbcTemplate().query(sql, new ColumnInfoRowMapper(), tableName, tableName);
    }

    @Override
    public String getNullFunction(String expression, String defaultValue) {
        return "ISNULL(" + expression + ", " + defaultValue + ")";
    }

    @Override
    public String getConcatFunction(String... args) {
        return "CONCAT(" + String.join(", ", args) + ")";
    }

    @Override
    public String getCurrentTimestampExpr() {
        return "GETDATE()";
    }

    @Override
    public String getDateFormatExpr(String dateExpr, String format) {
        return "FORMAT(" + dateExpr + ", '" + format + "')";
    }

    @Override
    public String getDatabaseName() {
        List<Map<String, Object>> result = executeQuery("SELECT DB_NAME() as db");
        if (!result.isEmpty()) {
            Object db = result.get(0).get("db");
            return db != null ? db.toString() : null;
        }
        return null;
    }

    private static class ColumnInfoRowMapper implements RowMapper<ColumnInfo> {
        @Override
        public ColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            String nullableStr = rs.getString("nullable");
            boolean nullable = "YES".equals(nullableStr) || "".equals(nullableStr);
            boolean isPk = rs.getInt("primaryKey") == 1;

            return new ColumnInfo(
                rs.getString("columnName"),
                rs.getString("dataType"),
                rs.getString("columnComment"),
                rs.getInt("columnSize"),
                nullable,
                rs.getString("defaultValue"),
                isPk
            );
        }
    }

    private static class ColumnInfoRowMapperNoComment implements RowMapper<ColumnInfo> {
        @Override
        public ColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            String nullableStr = rs.getString("nullable");
            boolean nullable = "YES".equals(nullableStr) || "".equals(nullableStr);
            boolean isPk = rs.getInt("primaryKey") == 1;

            return new ColumnInfo(
                rs.getString("columnName"),
                rs.getString("dataType"),
                null,
                rs.getInt("columnSize"),
                nullable,
                rs.getString("defaultValue"),
                isPk
            );
        }
    }
}
