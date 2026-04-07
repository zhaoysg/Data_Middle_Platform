package org.dromara.datasource.adapter;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 数据源适配器接口
 * 统一抽象不同数据源的 SQL 执行能力，屏蔽语法差异
 *
 * @author Lion Li
 */
public interface DataSourceAdapter {

    /**
     * 获取数据源类型标识
     */
    String getDataSourceType();

    /**
     * 测试连接
     */
    boolean testConnection();

    /**
     * 执行查询（只读）
     *
     * ⚠️ 安全警告: 此方法直接执行传入的SQL字符串，存在SQL注入风险。
     * 仅限内部使用（已知可信的SQL，如系统表查询）。外部调用者请使用
     * {@link #executeQuery(String, Object...)} 参数化版本。
     *
     * @param sql 原始SQL（必须是可信的、不含用户输入的SQL）
     */
    List<Map<String, Object>> executeQuery(String sql);

    /**
     * 执行查询（带参数）
     */
    List<Map<String, Object>> executeQuery(String sql, Object... params);

    /**
     * 执行可取消的查询（用于DQC规则执行，支持中途停止）
     * @param sql SQL
     * @param cancelChecker 执行过程中周期性调用的取消检查器，返回true表示应取消
     */
    List<Map<String, Object>> executeQueryCancellable(String sql, Supplier<Boolean> cancelChecker) throws java.sql.SQLException;

    /**
     * 执行可取消的查询（带参数，用于DQC规则执行，支持中途停止）
     * @param sql SQL
     * @param cancelChecker 执行过程中周期性调用的取消检查器，返回true表示应取消
     * @param params 参数
     */
    default List<Map<String, Object>> executeQueryCancellable(String sql, Supplier<Boolean> cancelChecker, Object... params) {
        return executeQuery(sql, params);
    }

    /**
     * 执行更新
     *
     * ⚠️ 安全警告: 此方法直接执行传入的SQL字符串，存在SQL注入风险。
     * 仅限内部使用（已知可信的SQL）。外部调用者请使用
     * {@link #executeUpdate(String, Object...)} 参数化版本。
     *
     * @param sql 原始SQL（必须是可信的、不含用户输入的SQL）
     */
    int executeUpdate(String sql);

    /**
     * 执行更新（带参数）
     */
    int executeUpdate(String sql, Object... params);

    /**
     * 获取指定表的字段信息
     */
    List<ColumnInfo> getColumns(String tableName);

    /**
     * 获取指定 schema 下指定表的字段信息（PostgreSQL 专用）
     */
    default List<ColumnInfo> getColumns(String schemaName, String tableName) {
        return getColumns(tableName);
    }

    /**
     * 获取主键列名列表
     */
    List<String> getPrimaryKeys(String tableName);

    /**
     * 获取表注释
     */
    default String getTableComment(String tableName) {
        return getTableComment(null, tableName);
    }

    /**
     * 获取表注释（带 schema）
     */
    default String getTableComment(String schemaName, String tableName) {
        return "";
    }

    /**
     * 获取所有表名
     */
    List<String> getTables();

    /**
     * 获取指定 schema 下的所有表名（PostgreSQL 专用）
     */
    default List<String> getTables(String schemaName) {
        return getTables();
    }

    /**
     * 获取所有 schema 名称（PostgreSQL 专用，其他返回空列表）
     */
    default List<String> getSchemas() {
        return List.of();
    }

    /**
     * 获取表行数
     */
    long getRowCount(String tableName);

    /**
     * 获取行数（带 schema）
     */
    default long getRowCount(String schemaName, String tableName) {
        return getRowCount(tableName);
    }

    /**
     * 构建分页 SQL
     */
    String buildPaginationSql(String sql, long offset, long limit);

    /**
     * 构建 count SQL
     */
    String buildCountSql(String sql);

    /**
     * 获取数据库名
     */
    default String getDatabaseName() {
        return null;
    }

    /**
     * 设置数据源ID（由注册表调用）
     */
    void setDsId(Long dsId);

    /**
     * 获取数据源ID
     */
    Long getDsId();

    /**
     * 引用标识符（字段名/表名）
     */
    default String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /**
     * 采样列值（用于预览）
     */
    List<Map<String, Object>> sampleColumnValues(String tableName, String columnName, int limit);

    /**
     * 采样列值（带 schema）
     */
    default List<Map<String, Object>> sampleColumnValues(String schemaName, String tableName, String columnName, int limit) {
        return sampleColumnValues(tableName, columnName, limit);
    }

    /**
     * 获取表最后更新时间（不支持时返回 Optional.empty()）
     */
    default java.util.Optional<String> getTableLastUpdateTime(String tableName) {
        return getTableLastUpdateTime(null, tableName);
    }

    default java.util.Optional<String> getTableLastUpdateTime(String schemaName, String tableName) {
        return java.util.Optional.empty();
    }

    /**
     * 字段信息 record
     *
     * @param columnName    列名
     * @param dataType      数据类型
     * @param columnComment 列注释
     * @param nullable     是否可空
     * @param primaryKey   是否主键
     */
    record ColumnInfo(
        String columnName,
        String dataType,
        String columnComment,
        boolean nullable,
        boolean primaryKey
    ) {}
}
