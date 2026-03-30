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
 * PostgreSQL 数据源适配器
 */
@Slf4j
@Component
public class PostgresAdapter implements DataSourceAdapter {

    private Long dsId;
    private final DynamicDataSourceManager dataSourceManager;

    public PostgresAdapter(DynamicDataSourceManager dataSourceManager) {
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
        return "POSTGRESQL";
    }

    @Override
    public boolean testConnection() {
        try {
            getJdbcTemplate().execute("SELECT 1");
            return true;
        } catch (Exception e) {
            log.error("PostgreSQL连接测试失败", e);
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

    /**
     * PostgreSQL 的 information_schema 列使用字符串字面量比较（不用双引号标识符），
     * 因为 table_schema / table_name 等列是 sql_identifier 类型，
     * 对双引号形式存在版本兼容性问题（如 PG 16+）
     */
    private String schemaLiteral(String schema) {
        // 单引号转义
        return "'" + schema.replace("'", "''") + "'";
    }

    /**
     * 将表名转义为 PostgreSQL 字符串字面量（用于 col_description 等函数参数）。
     * col_description(regclass, attnum) 的第一个参数必须是 regclass 类型，
     * 接受 'schema.table' 形式的带引号字符串（必须包含 schema 前缀，否则
     * ::regclass 会按 search_path 解析，找不到非 public schema 的表）。
     */
    private String tableLiteral(String schema, String tableName) {
        return "'" + schema.replace("'", "''") + "." + tableName.trim().replace("'", "''") + "'";
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) {
        String schema = resolveSchema(null);
        return doGetColumns(schema, tableName);
    }

    @Override
    public List<ColumnInfo> getColumns(String schemaName, String tableName) {
        return doGetColumns(resolveSchema(schemaName), tableName);
    }

    private List<ColumnInfo> doGetColumns(String schema, String tableName) {
        String safeSchema = schemaLiteral(schema);
        String trimmedTable = tableName.trim();
        String safeTableRaw = trimmedTable.replace("'", "''");   // bare identifier (no outer quotes), used in WHERE clause
        String safeTableLit = tableLiteral(schema, trimmedTable); // string literal 'schema.table', used in col_description
        String sql = """
            SELECT
                c.column_name as columnName,
                c.data_type as dataType,
                col_description(%s::regclass, c.ordinal_position) as columnComment,
                c.character_maximum_length as columnSize,
                c.numeric_precision as numericPrecision,
                c.numeric_scale as numericScale,
                c.is_nullable as nullable,
                c.column_default as defaultValue,
                CASE WHEN pk.column_name IS NOT NULL THEN 1 ELSE 0 END as isPrimaryKey
            FROM information_schema.columns c
            LEFT JOIN (
                SELECT kcu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
                WHERE tc.table_schema = %s AND tc.table_name = '%s' AND tc.constraint_type = 'PRIMARY KEY'
            ) pk ON pk.column_name = c.column_name
            WHERE c.table_schema = %s AND c.table_name = '%s'
            ORDER BY c.ordinal_position
            """.formatted(safeTableLit, safeSchema, safeTableRaw, safeSchema, safeTableRaw);
        return getJdbcTemplate().query(sql, new ColumnInfoRowMapper());
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) {
        String schema = resolveSchema(null);
        String safeSchema = schemaLiteral(schema);
        String safeTable = tableName.trim().replace("'", "''");
        String sql = """
            SELECT kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
            WHERE tc.table_schema = %s AND tc.table_name = '%s' AND tc.constraint_type = 'PRIMARY KEY'
            ORDER BY kcu.ordinal_position
            """.formatted(safeSchema, safeTable);
        return getJdbcTemplate().queryForList(sql, String.class);
    }

    @Override
    public String getTableComment(String schemaName, String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return "";
        }
        String schema = resolveSchema(schemaName);
        String safeSchema = schema.replace("'", "''");
        String safeTable = tableName.trim().replace("'", "''");
        String sql = """
            SELECT obj_description(c.oid, 'pg_class')
            FROM pg_catalog.pg_class c
            JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = '%s' AND c.relname = '%s' AND c.relkind IN ('r', 'p', 'v', 'm')
            """.formatted(safeSchema, safeTable);
        try {
            List<String> rows = getJdbcTemplate().query(sql, (rs, i) -> rs.getString(1));
            if (rows.isEmpty() || rows.get(0) == null) {
                return "";
            }
            return rows.get(0).trim();
        } catch (Exception e) {
            log.debug("读取 PostgreSQL 表注释失败: {}.{}", schema, tableName, e);
            return "";
        }
    }

    @Override
    public List<String> getTables() {
        return listTablesExcludingPartitions(resolveSchema(null));
    }

    @Override
    public List<String> getTables(String schemaName) {
        return listTablesExcludingPartitions(resolveSchema(schemaName));
    }

    /**
     * 列出 BASE TABLE，并排除 PostgreSQL 分区子表（仅保留分区主表与普通表）。
     */
    private List<String> listTablesExcludingPartitions(String schema) {
        String safeSchema = schemaLiteral(schema);
        String sql = """
            SELECT t.table_name
            FROM information_schema.tables t
            WHERE t.table_schema = %s AND t.table_type = 'BASE TABLE'
              AND NOT EXISTS (
                  SELECT 1 FROM pg_catalog.pg_class c
                  JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                  WHERE n.nspname = t.table_schema
                    AND c.relname = t.table_name
                    AND c.relispartition
              )
            ORDER BY t.table_name
            """.formatted(safeSchema);
        return getJdbcTemplate().queryForList(sql).stream()
            .map(row -> row.values().iterator().next().toString())
            .collect(java.util.stream.Collectors.toList());
    }

    /** 查询当前数据库下所有可用的 schema 名称 */
    public List<String> getSchemas() {
        String sql = """
            SELECT schema_name
            FROM information_schema.schemata
            WHERE schema_name NOT IN ('pg_catalog', 'information_schema')
            ORDER BY schema_name
            """;
        return getJdbcTemplate().queryForList(sql).stream()
            .map(row -> row.values().iterator().next().toString())
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 解析最终使用的 schema 名称
     * @param explicitSchema  显式指定的 schema（优先）
     * @return  若 explicitSchema 非空则使用，否则取实体配置的 schema，再否则默认 public
     */
    private String resolveSchema(String explicitSchema) {
        if (explicitSchema != null && !explicitSchema.isBlank()) {
            return explicitSchema.trim();
        }
        String configured = dataSourceManager.getSchemaName(dsId);
        return (configured != null && !configured.isBlank()) ? configured.trim() : "public";
    }

    @Override
    public long getRowCount(String tableName) {
        String schema = resolveSchema(null);
        String safeSchema = quoteIdentifier(schema);
        String safeName = quoteIdentifier(tableName);
        String sql = "SELECT COUNT(*) FROM " + safeSchema + "." + safeName;
        Long count = getJdbcTemplate().queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long getRowCount(String schemaName, String tableName) {
        String schema = resolveSchema(schemaName);
        String safeSchema = quoteIdentifier(schema);
        String safeName = quoteIdentifier(tableName);
        String sql = "SELECT COUNT(*) FROM " + safeSchema + "." + safeName;
        Long count = getJdbcTemplate().queryForObject(sql, Long.class);
        return count != null ? count : 0L;
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
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String quoteTableName(String schema, String table) {
        String resolvedSchema = (schema != null && !schema.isBlank()) ? schema : resolveSchema(null);
        return quoteIdentifier(resolvedSchema) + "." + quoteIdentifier(table);
    }

    @Override
    public List<Map<String, Object>> sampleColumnValues(String tableName, String columnName, int limit) {
        return sampleColumnValues(null, tableName, columnName, limit);
    }

    @Override
    public List<Map<String, Object>> sampleColumnValues(String schemaName, String tableName, String columnName, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        String resolvedSchema = resolveSchema(schemaName);
        String qCol = quoteIdentifier(columnName);
        String qSchema = quoteIdentifier(resolvedSchema);
        String qTbl = quoteIdentifier(tableName);
        String sql = "SELECT " + qCol + " FROM " + qSchema + "." + qTbl
                + " WHERE " + qCol + " IS NOT NULL LIMIT " + safeLimit;
        return getJdbcTemplate().queryForList(sql);
    }

    @Override
    public List<ColumnInfo> getColumnsFromInformationSchema(String schema, String tableName) {
        String resolvedSchema = (schema != null && !schema.isBlank()) ? schema.trim() : resolveSchema(null);
        String safeSchema = schemaLiteral(resolvedSchema);
        String trimmedTable = tableName.trim();
        String safeTableRaw = trimmedTable.replace("'", "''");
        String safeTableLit = tableLiteral(resolvedSchema, trimmedTable);
        String sql = """
            SELECT
                c.column_name as columnName,
                c.data_type as dataType,
                col_description(%s::regclass, c.ordinal_position) as columnComment,
                c.character_maximum_length as columnSize,
                c.numeric_precision as numericPrecision,
                c.numeric_scale as numericScale,
                c.is_nullable as nullable,
                c.column_default as defaultValue,
                CASE WHEN pk.column_name IS NOT NULL THEN 1 ELSE 0 END as isPrimaryKey
            FROM information_schema.columns c
            LEFT JOIN (
                SELECT kcu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
                WHERE tc.table_schema = %s AND tc.table_name = '%s' AND tc.constraint_type = 'PRIMARY KEY'
            ) pk ON pk.column_name = c.column_name
            WHERE c.table_schema = %s AND c.table_name = '%s'
            ORDER BY c.ordinal_position
            """.formatted(safeTableLit, safeSchema, safeTableRaw, safeSchema, safeTableRaw);
        return getJdbcTemplate().query(sql, new ColumnInfoRowMapper());
    }

    @Override
    public String getNullFunction(String expression, String defaultValue) {
        return "COALESCE(" + expression + ", " + defaultValue + ")";
    }

    @Override
    public String getConcatFunction(String... args) {
        return "CONCAT(" + String.join(", ", args) + ")";
    }

    @Override
    public String getCurrentTimestampExpr() {
        return "CURRENT_TIMESTAMP";
    }

    @Override
    public String getDateFormatExpr(String dateExpr, String format) {
        return "TO_CHAR(" + dateExpr + ", '" + format + "')";
    }

    @Override
    public String getDatabaseName() {
        List<Map<String, Object>> result = executeQuery("SELECT current_database() as db");
        if (!result.isEmpty()) {
            Object db = result.get(0).get("db");
            return db != null ? db.toString() : null;
        }
        return null;
    }

    private static class ColumnInfoRowMapper implements RowMapper<ColumnInfo> {
        @Override
        public ColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            boolean nullable = "YES".equals(rs.getString("nullable"));
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
