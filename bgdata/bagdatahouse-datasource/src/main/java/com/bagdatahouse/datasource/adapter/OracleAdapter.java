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
 * Oracle 数据源适配器
 * 支持 Oracle 11g+（使用 ROWNUM 分页）
 */
@Slf4j
@Component
public class OracleAdapter implements DataSourceAdapter {

    private final DynamicDataSourceManager dataSourceManager;
    private Long dsId;

    public OracleAdapter(DynamicDataSourceManager dataSourceManager) {
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
        return "ORACLE";
    }

    @Override
    public boolean testConnection() {
        try {
            getJdbcTemplate().execute("SELECT 1 FROM DUAL");
            return true;
        } catch (Exception e) {
            log.error("Oracle连接测试失败", e);
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
                col.COLUMN_NAME as columnName,
                col.DATA_TYPE as dataType,
                com.COMMENTS as columnComment,
                col.DATA_LENGTH as columnSize,
                col.NULLABLE as nullable,
                col.DATA_DEFAULT as defaultValue,
                CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 1 ELSE 0 END as isPrimaryKey
            FROM USER_TAB_COLUMNS col
            LEFT JOIN USER_COL_COMMENTS com ON com.TABLE_NAME = col.TABLE_NAME AND com.COLUMN_NAME = col.COLUMN_NAME
            LEFT JOIN (
                SELECT cu.COLUMN_NAME
                FROM USER_CONS_COLUMNS cu
                JOIN USER_CONSTRAINTS tc ON cu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                WHERE tc.TABLE_NAME = ? AND tc.CONSTRAINT_TYPE = 'P'
            ) pk ON pk.COLUMN_NAME = col.COLUMN_NAME
            WHERE col.TABLE_NAME = ?
            ORDER BY col.COLUMN_ID
            """;
        return getJdbcTemplate().query(sql, new ColumnInfoRowMapper(), tableName, tableName);
    }

    @Override
    public List<ColumnInfo> getColumns(String schemaName, String tableName) {
        String sql = """
            SELECT
                col.COLUMN_NAME as columnName,
                col.DATA_TYPE as dataType,
                com.COMMENTS as columnComment,
                col.DATA_LENGTH as columnSize,
                col.NULLABLE as nullable,
                col.DATA_DEFAULT as defaultValue,
                CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 1 ELSE 0 END as isPrimaryKey
            FROM ALL_TAB_COLUMNS col
            LEFT JOIN ALL_COL_COMMENTS com ON com.OWNER = col.OWNER AND com.TABLE_NAME = col.TABLE_NAME AND com.COLUMN_NAME = col.COLUMN_NAME
            LEFT JOIN (
                SELECT cu.COLUMN_NAME
                FROM ALL_CONS_COLUMNS cu
                JOIN ALL_CONSTRAINTS tc ON cu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                WHERE tc.OWNER = ? AND tc.TABLE_NAME = ? AND tc.CONSTRAINT_TYPE = 'P'
            ) pk ON pk.COLUMN_NAME = col.COLUMN_NAME
            WHERE col.OWNER = ? AND col.TABLE_NAME = ?
            ORDER BY col.COLUMN_ID
            """;
        return getJdbcTemplate().query(sql, new ColumnInfoRowMapper(), schemaName, tableName, schemaName, tableName);
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) {
        String sql = """
            SELECT cu.COLUMN_NAME
            FROM USER_CONS_COLUMNS cu
            JOIN USER_CONSTRAINTS tc ON cu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
            WHERE tc.TABLE_NAME = ? AND tc.CONSTRAINT_TYPE = 'P'
            ORDER BY cu.POSITION
            """;
        return getJdbcTemplate().queryForList(sql, String.class, tableName);
    }

    @Override
    public String getTableComment(String schemaName, String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return "";
        }
        String t = tableName.trim().toUpperCase();
        try {
            if (schemaName != null && !schemaName.isBlank()) {
                String owner = schemaName.trim().toUpperCase();
                String sql = "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE OWNER = ? AND TABLE_NAME = ?";
                List<String> rows = getJdbcTemplate().query(sql, (rs, i) -> rs.getString(1), owner, t);
                if (!rows.isEmpty() && rows.get(0) != null) {
                    return rows.get(0).trim();
                }
                return "";
            }
            String sql = "SELECT COMMENTS FROM USER_TAB_COMMENTS WHERE TABLE_NAME = ?";
            List<String> rows = getJdbcTemplate().query(sql, (rs, i) -> rs.getString(1), t);
            if (!rows.isEmpty() && rows.get(0) != null) {
                return rows.get(0).trim();
            }
            return "";
        } catch (Exception e) {
            log.debug("读取 Oracle 表注释失败: {}", tableName, e);
            return "";
        }
    }

    @Override
    public List<String> getTables() {
        String sql = "SELECT TABLE_NAME FROM USER_TABLES ORDER BY TABLE_NAME";
        return getJdbcTemplate().queryForList(sql).stream()
            .map(row -> row.values().iterator().next().toString())
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<String> getTables(String schemaName) {
        String sql = "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = ? AND TEMPORARY = 'N' ORDER BY TABLE_NAME";
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
        long startRow = offset + 1;
        long endRow = offset + limit;
        return "SELECT * FROM (SELECT t.*, ROWNUM rn FROM (" + sql + ") t WHERE ROWNUM <= " + endRow + ") WHERE rn >= " + startRow;
    }

    @Override
    public String buildCountSql(String sql) {
        if (sql.toUpperCase().contains("ROWNUM")) {
            return "SELECT COUNT(*) FROM (" + sql + ")";
        }
        return "SELECT COUNT(*) FROM (" + sql + ")";
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public List<Map<String, Object>> sampleColumnValues(String tableName, String columnName, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        String qCol = quoteIdentifier(columnName);
        String qTbl = quoteIdentifier(tableName);
        String sql = "SELECT " + qCol + " FROM (SELECT " + qCol + " FROM " + qTbl + " WHERE " + qCol + " IS NOT NULL AND ROWNUM <= " + safeLimit + ")";
        return getJdbcTemplate().queryForList(sql);
    }

    @Override
    public List<ColumnInfo> getColumnsFromInformationSchema(String schema, String tableName) {
        boolean useAllColumns = schema != null && !schema.isBlank();
        String sql;
        if (useAllColumns) {
            sql = """
                SELECT
                    col.COLUMN_NAME as columnName,
                    col.DATA_TYPE as dataType,
                    com.COMMENTS as columnComment,
                    col.DATA_LENGTH as columnSize,
                    col.NULLABLE as nullable,
                    col.DATA_DEFAULT as defaultValue,
                    CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 1 ELSE 0 END as isPrimaryKey
                FROM ALL_TAB_COLUMNS col
                LEFT JOIN ALL_COL_COMMENTS com ON com.OWNER = col.OWNER AND com.TABLE_NAME = col.TABLE_NAME AND com.COLUMN_NAME = col.COLUMN_NAME
                LEFT JOIN (
                    SELECT cu.COLUMN_NAME
                    FROM ALL_CONS_COLUMNS cu
                    JOIN ALL_CONSTRAINTS tc ON cu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                    WHERE tc.OWNER = ? AND tc.TABLE_NAME = ? AND tc.CONSTRAINT_TYPE = 'P'
                ) pk ON pk.COLUMN_NAME = col.COLUMN_NAME
                WHERE col.OWNER = ? AND col.TABLE_NAME = ?
                ORDER BY col.COLUMN_ID
                """;
            return getJdbcTemplate().query(sql, new ColumnInfoRowMapper(),
                    schema, tableName, schema, tableName);
        } else {
            sql = """
                SELECT
                    col.COLUMN_NAME as columnName,
                    col.DATA_TYPE as dataType,
                    com.COMMENTS as columnComment,
                    col.DATA_LENGTH as columnSize,
                    col.NULLABLE as nullable,
                    col.DATA_DEFAULT as defaultValue,
                    CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 1 ELSE 0 END as isPrimaryKey
                FROM USER_TAB_COLUMNS col
                LEFT JOIN USER_COL_COMMENTS com ON com.TABLE_NAME = col.TABLE_NAME AND com.COLUMN_NAME = col.COLUMN_NAME
                LEFT JOIN (
                    SELECT cu.COLUMN_NAME
                    FROM USER_CONS_COLUMNS cu
                    JOIN USER_CONSTRAINTS tc ON cu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                    WHERE tc.TABLE_NAME = ? AND tc.CONSTRAINT_TYPE = 'P'
                ) pk ON pk.COLUMN_NAME = col.COLUMN_NAME
                WHERE col.TABLE_NAME = ?
                ORDER BY col.COLUMN_ID
                """;
            return getJdbcTemplate().query(sql, new ColumnInfoRowMapper(), tableName, tableName);
        }
    }

    @Override
    public String getNullFunction(String expression, String defaultValue) {
        return "NVL(" + expression + ", " + defaultValue + ")";
    }

    @Override
    public String getConcatFunction(String... args) {
        return "CONCAT(" + args[0] + ", " + args[1] + ")";
    }

    @Override
    public String getCurrentTimestampExpr() {
        return "SYSDATE";
    }

    @Override
    public String getDateFormatExpr(String dateExpr, String format) {
        return "TO_CHAR(" + dateExpr + ", '" + format + "')";
    }

    @Override
    public String getDatabaseName() {
        List<Map<String, Object>> result = executeQuery("SELECT USER FROM DUAL");
        if (!result.isEmpty()) {
            Object db = result.get(0).get("USER");
            return db != null ? db.toString() : null;
        }
        return null;
    }

    private static class ColumnInfoRowMapper implements RowMapper<ColumnInfo> {
        @Override
        public ColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            String nullableStr = rs.getString("nullable");
            boolean nullable = "Y".equals(nullableStr);
            boolean isPk = rs.getInt("isPrimaryKey") == 1;

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
}
