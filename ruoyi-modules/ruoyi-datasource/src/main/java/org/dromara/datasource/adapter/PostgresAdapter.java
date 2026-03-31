package org.dromara.datasource.adapter;

import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.manager.DynamicDataSourceManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL 数据源适配器
 *
 * @author Lion Li
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

    @Override
    public List<ColumnInfo> getColumns(String tableName) {
        String sql = """
            SELECT
                a.attname as columnName,
                pg_type.typname as dataType,
                col_description(a.attrelid, a.attnum) as columnComment,
                not a.attnotnull as nullable,
                exists(SELECT 1 FROM pg_constraint WHERE conrelid = a.attrelid AND conkey[1] = a.attnum AND contype = 'p') as primaryKey
            FROM pg_attribute a
            JOIN pg_type ON a.atttypid = pg_type.oid
            JOIN pg_class c ON a.attrelid = c.oid
            WHERE c.relname = ? and a.attnum > 0
            ORDER BY a.attnum
            """;
        return getJdbcTemplate().query(sql, new PostgresColumnInfoRowMapper(), tableName);
    }

    @Override
    public List<ColumnInfo> getColumns(String schemaName, String tableName) {
        String schema = (schemaName != null && !schemaName.isBlank()) ? schemaName.trim() : "public";
        String sql = """
            SELECT
                a.attname as columnName,
                pg_type.typname as dataType,
                col_description(a.attrelid, a.attnum) as columnComment,
                not a.attnotnull as nullable,
                exists(SELECT 1 FROM pg_constraint WHERE conrelid = a.attrelid AND conkey[1] = a.attnum AND contype = 'p') as primaryKey
            FROM pg_attribute a
            JOIN pg_type ON a.atttypid = pg_type.oid
            JOIN pg_class c ON a.attrelid = c.oid
            JOIN pg_namespace n ON c.relnamespace = n.oid
            WHERE n.nspname = ? AND c.relname = ? AND a.attnum > 0
            ORDER BY a.attnum
            """;
        return getJdbcTemplate().query(sql, new PostgresColumnInfoRowMapper(), schema, tableName);
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) {
        String sql = """
            SELECT a.attname as columnName
            FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
            JOIN pg_class t ON t.oid = c.conrelid
            WHERE t.relname = ? AND c.contype = 'p'
            ORDER BY a.attnum
            """;
        return getJdbcTemplate().queryForList(sql, String.class, tableName);
    }

    @Override
    public String getTableComment(String schemaName, String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return "";
        }
        String schema = (schemaName != null && !schemaName.isBlank()) ? schemaName.trim() : "public";
        String sql = """
            SELECT obj_description(c.oid, 'pg_class')
            FROM pg_class c
            JOIN pg_namespace n ON c.relnamespace = n.oid
            WHERE n.nspname = ? AND c.relname = ?
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
        String sql = """
            SELECT tablename FROM pg_tables
            WHERE schemaname = 'public' AND tablename NOT LIKE 'pg_%' AND tablename NOT LIKE 'sql_%'
            ORDER BY tablename
            """;
        return getJdbcTemplate().queryForList(sql, String.class);
    }

    @Override
    public List<String> getTables(String schemaName) {
        String schema = (schemaName != null && !schemaName.isBlank()) ? schemaName.trim() : "public";
        String sql = """
            SELECT tablename FROM pg_tables
            WHERE schemaname = ? AND tablename NOT LIKE 'pg_%' AND tablename NOT LIKE 'sql_%'
            ORDER BY tablename
            """;
        return getJdbcTemplate().queryForList(sql, String.class, schema);
    }

    @Override
    public List<String> getSchemas() {
        String sql = "SELECT schema_name FROM information_schema.schemata ORDER BY schema_name";
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
    public long getRowCount(String schemaName, String tableName) {
        String schema = (schemaName != null && !schemaName.isBlank()) ? schemaName.trim() : "public";
        String qSchema = quoteIdentifier(schema);
        String qTable = quoteIdentifier(tableName);
        String sql = "SELECT COUNT(*) FROM " + qSchema + "." + qTable;
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
    public List<Map<String, Object>> sampleColumnValues(String tableName, String columnName, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        String qCol = quoteIdentifier(columnName);
        String qTbl = quoteIdentifier(tableName);
        String sql = "SELECT " + qCol + " FROM " + qTbl + " WHERE " + qCol + " IS NOT NULL LIMIT " + safeLimit;
        return getJdbcTemplate().queryForList(sql);
    }

    @Override
    public java.util.Optional<String> getTableLastUpdateTime(String schemaName, String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return java.util.Optional.empty();
        }
        String schema = (schemaName != null && !schemaName.isBlank()) ? schemaName.trim() : "public";
        String sql = """
            SELECT GREATEST(
                COALESCE(pg_stat_get_last_analyze_time(c.oid), '1970-01-01'::timestamptz),
                COALESCE(pg_stat_get_last_autoanalyze_time(c.oid), '1970-01-01'::timestamptz)
            ) AS ut
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = ? AND c.relname = ?
            """;
        try {
            List<Map<String, Object>> rows = executeQuery(sql, schema, tableName.trim());
            if (!rows.isEmpty()) {
                Object val = rows.get(0).get("ut");
                if (val != null) {
                    if (val instanceof Timestamp ts) {
                        return java.util.Optional.of(ts.toLocalDateTime().toString());
                    }
                    if (val instanceof OffsetDateTime odt) {
                        return java.util.Optional.of(odt.toLocalDateTime().toString());
                    }
                    if (val instanceof LocalDateTime ldt) {
                        return java.util.Optional.of(ldt.toString());
                    }
                    return java.util.Optional.of(val.toString());
                }
            }
        } catch (Exception e) {
            log.debug("获取表最后更新时间失败: {}", tableName, e);
        }
        return java.util.Optional.empty();
    }

    private static class PostgresColumnInfoRowMapper implements RowMapper<ColumnInfo> {
        @Override
        public ColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ColumnInfo(
                rs.getString("columnName"),
                rs.getString("dataType"),
                rs.getString("columnComment"),
                rs.getBoolean("nullable"),
                rs.getBoolean("primaryKey")
            );
        }
    }
}
