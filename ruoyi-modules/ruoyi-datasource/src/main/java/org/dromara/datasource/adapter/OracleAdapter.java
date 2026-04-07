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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Oracle 数据源适配器
 *
 * @author Lion Li
 */
@Slf4j
@Component
public class OracleAdapter implements DataSourceAdapter {

    private Long dsId;
    private final DynamicDataSourceManager dataSourceManager;

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
        String sql = """
            SELECT
                utc.column_name as columnName,
                utc.data_type as dataType,
                ucc.comments as columnComment,
                CASE WHEN utc.nullable = 'Y' THEN 1 ELSE 0 END as nullable,
                CASE WHEN pk.column_name IS NOT NULL THEN 1 ELSE 0 END as primaryKey
            FROM user_tab_columns utc
            LEFT JOIN user_col_comments ucc ON ucc.table_name = utc.table_name AND ucc.column_name = utc.column_name
            LEFT JOIN (
                SELECT cols.column_name
                FROM user_constraints cons
                JOIN user_cons_columns cols ON cons.constraint_name = cols.constraint_name AND cons.owner = cols.owner
                WHERE cons.constraint_type = 'P' AND cols.table_name = ?
            ) pk ON pk.column_name = utc.column_name
            WHERE utc.table_name = UPPER(?)
            ORDER BY utc.column_id
            """;
        return getJdbcTemplate().query(sql, new OracleColumnInfoRowMapper(), tableName, tableName);
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) {
        String sql = """
            SELECT cols.column_name
            FROM user_constraints cons
            JOIN user_cons_columns cols ON cons.constraint_name = cols.constraint_name AND cons.owner = cols.owner
            WHERE cons.constraint_type = 'P' AND cols.table_name = UPPER(?)
            ORDER BY cols.position
            """;
        return getJdbcTemplate().queryForList(sql, String.class, tableName);
    }

    @Override
    public List<String> getTables() {
        String sql = """
            SELECT table_name FROM user_tables
            WHERE table_name NOT LIKE 'BIN$%'
            ORDER BY table_name
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
        long end = offset + limit;
        return "SELECT * FROM (SELECT TMP.*, ROWNUM RN FROM (" + sql + ") TMP WHERE ROWNUM <= " + end + ") WHERE RN > " + offset;
    }

    @Override
    public String buildCountSql(String sql) {
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
        String sql = "SELECT " + qCol + " FROM " + qTbl + " WHERE " + qCol + " IS NOT NULL AND ROWNUM <= " + safeLimit;
        return getJdbcTemplate().queryForList(sql);
    }

    private static class OracleColumnInfoRowMapper implements RowMapper<ColumnInfo> {
        @Override
        public ColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ColumnInfo(
                rs.getString("columnName"),
                rs.getString("dataType"),
                rs.getString("columnComment"),
                rs.getInt("nullable") == 1,
                rs.getInt("primaryKey") == 1
            );
        }
    }
}
