package com.bagdatahouse.datasource.adapter;

import java.util.List;
import java.util.Map;

/**
 * 数据源适配器接口
 * 统一抽象不同数据源的SQL执行能力，屏蔽语法差异
 */
public interface DataSourceAdapter {

    /**
     * 获取数据源类型
     */
    String getDataSourceType();

    /**
     * 测试连接
     */
    boolean testConnection();

    /**
     * 执行查询SQL
     */
    List<Map<String, Object>> executeQuery(String sql);

    /**
     * 执行查询SQL（带参数）
     */
    List<Map<String, Object>> executeQuery(String sql, Object... params);

    /**
     * 执行更新SQL（INSERT/UPDATE/DELETE）
     */
    int executeUpdate(String sql);

    /**
     * 执行更新SQL（带参数）
     */
    int executeUpdate(String sql, Object... params);

    /**
     * 获取表的所有列信息
     */
    List<ColumnInfo> getColumns(String tableName);

    /**
     * 获取表的主键信息
     */
    List<String> getPrimaryKeys(String tableName);

    /**
     * 获取表注释/说明（用于元数据「表注释」「中文名」）
     */
    default String getTableComment(String tableName) {
        return getTableComment(null, tableName);
    }

    /**
     * 获取表注释（带 schema/owner，PostgreSQL、SQL Server、Oracle 等需要）
     *
     * @param schemaName schema 或 owner；MySQL/TiDB 实现可忽略此参数
     * @param tableName  表名
     */
    default String getTableComment(String schemaName, String tableName) {
        return "";
    }

    /**
     * 获取数据库的所有表
     */
    List<String> getTables();

    /**
     * 获取数据库的所有 schema（PostgreSQL 专用；其他数据库默认返回空列表）
     */
    default List<String> getSchemas() {
        return List.of();
    }

    /**
     * 获取表行数
     */
    long getRowCount(String tableName);

    /**
     * 获取表行数（带 schema 过滤，PostgreSQL 等多 schema 数据库使用）
     */
    default long getRowCount(String schemaName, String tableName) {
        return getRowCount(tableName);
    }

    /**
     * 获取表列数
     */
    default int getColumnCount(String tableName) {
        return getColumns(tableName).size();
    }

    /**
     * 获取表列数（带 schema 过滤，PostgreSQL 等多 schema 数据库使用）
     */
    default int getColumnCount(String schemaName, String tableName) {
        return getColumns(schemaName, tableName).size();
    }

    /**
     * 获取表存储大小（字节）
     */
    default java.math.BigDecimal getStorageBytes(String tableName) {
        return java.math.BigDecimal.ZERO;
    }

    /**
     * 获取列级统计信息（空值数、唯一数、最小/最大值等）
     * @param tableName  表名
     * @param columnName 列名，null 表示全部列
     * @return Map：key=列名，value=统计信息 Map（包含 totalCount/nullCount/uniqueCount/minValue/maxValue/dataType 等）
     */
    default Map<String, Object> getColumnStats(String tableName, String columnName) {
        return java.util.Collections.emptyMap();
    }

    /**
     * 构建分页SQL
     */
    String buildPaginationSql(String sql, long offset, long limit);

    /**
     * 设置当前数据源ID（用于获取对应的JdbcTemplate）
     */
    void setDsId(Long dsId);

    /**
     * 获取当前数据源ID
     */
    Long getDsId();

    /**
     * 构建COUNT查询SQL
     */
    String buildCountSql(String sql);

    /**
     * 获取当前数据库名称
     */
    default String getDatabaseName() {
        return null;
    }

    /**
     * 获取列信息（带 schema 过滤）
     * @param schemaName  schema/database 名称
     * @param tableName   表名
     */
    default List<ColumnInfo> getColumns(String schemaName, String tableName) {
        return getColumns(tableName);
    }

    /**
     * 获取表列表（带 schema 过滤）
     * @param schemaName  schema/database 名称
     */
    default List<String> getTables(String schemaName) {
        return getTables();
    }

    /**
     * 标识符引号（单个标识符，如列名）
     * MySQL: `` / SQL Server: [] / Oracle: "" / PostgreSQL: ""
     */
    String quoteIdentifier(String identifier);

    /**
     * 表名引号（自动包含 schema 前缀）
     * @param schema  schema/database 名称，为 null 时仅返回带引号的表名
     * @param table   表名
     * @return 带 schema 前缀的引号表名，如 PostgreSQL: "schema"."table"
     */
    default String quoteTableName(String schema, String table) {
        if (schema == null || schema.isBlank()) {
            return quoteIdentifier(table);
        }
        return quoteIdentifier(schema) + "." + quoteIdentifier(table);
    }

    /**
     * 对指定列进行内容采样（用于敏感字段识别、内容级规则匹配）
     * 使用各数据源方言实现随机采样
     * @param tableName  表名
     * @param columnName 列名
     * @param limit      采样数量上限（最大不超过 200）
     * @return 采样结果列表，每行 Map 的 key 为列名
     */
    List<Map<String, Object>> sampleColumnValues(String tableName, String columnName, int limit);

    /**
     * 带 schema 的列采样（PostgreSQL 等多 schema 库必须传入扫描目标 schema，否则会落到连接默认 schema 如 public）
     * @param schemaName schema/database，可为 null 表示使用数据源连接配置的默认 schema
     */
    default List<Map<String, Object>> sampleColumnValues(String schemaName, String tableName, String columnName, int limit) {
        return sampleColumnValues(tableName, columnName, limit);
    }

    /**
     * 通过 INFORMATION_SCHEMA 查询表结构（用于直连数据源扫描模式）
     * @param schema    schema/database 名称
     * @param tableName 表名
     * @return 列信息列表
     */
    List<ColumnInfo> getColumnsFromInformationSchema(String schema, String tableName);

    /**
     * NULL 函数（如 NVL/IFNULL/COALESCE）
     */
    default String getNullFunction(String expression, String defaultValue) {
        return "IFNULL(" + expression + ", " + defaultValue + ")";
    }

    /**
     * 字符串拼接函数
     */
    default String getConcatFunction(String... args) {
        return "CONCAT(" + String.join(", ", args) + ")";
    }

    /**
     * 获取当前时间的 SQL 表达式
     */
    default String getCurrentTimestampExpr() {
        return "CURRENT_TIMESTAMP";
    }

    /**
     * 获取日期格式化函数
     * @param dateExpr  日期表达式
     * @param format    格式字符串
     */
    default String getDateFormatExpr(String dateExpr, String format) {
        return "DATE_FORMAT(" + dateExpr + ", '" + format + "')";
    }

    /**
     * 字段信息
     */
    record ColumnInfo(
        String columnName,
        String dataType,
        String columnComment,
        int columnSize,
        boolean nullable,
        String defaultValue,
        boolean primaryKey
    ) {}
}
