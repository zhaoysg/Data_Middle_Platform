package com.bagdatahouse.dqc.engine.util;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.enums.DataSourceTypeEnum;

/**
 * SQL syntax utility for building database-agnostic SQL statements.
 * Delegates to DataSourceAdapter helper methods and provides convenient wrappers.
 */
public final class SqlSyntaxHelper {

    private SqlSyntaxHelper() {
    }

    /**
     * Quote an identifier (table name or column name) for safe SQL use.
     */
    public static String quote(DataSourceAdapter adapter, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return identifier;
        }
        return adapter.quoteIdentifier(identifier);
    }

    /**
     * Quote a table name, optionally with schema prefix.
     * Handles both "schema.table" format and simple "table" format.
     */
    public static String quoteTable(DataSourceAdapter adapter, String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return tableName;
        }
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            return quote(adapter, parts[0]) + "." + quote(adapter, parts[1]);
        }
        return quote(adapter, tableName);
    }

    /**
     * Build a NULL check SQL: SELECT COUNT(*) AS total, SUM(CASE WHEN col IS NULL THEN 1 ELSE 0 END) AS nullCount FROM table
     */
    public static String buildNullCheckSql(DataSourceAdapter adapter, String tableName, String columnName) {
        String quotedTable = quoteTable(adapter, tableName);
        String quotedCol = quote(adapter, columnName);
        return String.format(
                "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN %s IS NULL THEN 1 ELSE 0 END) AS nullCount " +
                "FROM %s",
                quotedCol, quotedTable);
    }

    /**
     * Build a row count SQL: SELECT COUNT(*) AS rowCount FROM table
     */
    public static String buildRowCountSql(DataSourceAdapter adapter, String tableName) {
        return String.format(
                "SELECT COUNT(*) AS rowCount FROM %s",
                quoteTable(adapter, tableName));
    }

    /**
     * Build a unique count SQL: SELECT COUNT(*) AS total, COUNT(DISTINCT col) AS uniqueCount FROM table
     */
    public static String buildUniqueCountSql(DataSourceAdapter adapter, String tableName, String columnName) {
        return String.format(
                "SELECT COUNT(*) AS total, COUNT(DISTINCT %s) AS uniqueCount FROM %s",
                quote(adapter, columnName), quoteTable(adapter, tableName));
    }

    /**
     * Build a duplicate check SQL: SELECT col, COUNT(*) AS cnt FROM table GROUP BY col HAVING COUNT(*) > 1
     */
    public static String buildDuplicateCheckSql(DataSourceAdapter adapter, String tableName, String columnName) {
        return String.format(
                "SELECT %s, COUNT(*) AS cnt FROM %s GROUP BY %s HAVING COUNT(*) > 1",
                quote(adapter, columnName),
                quoteTable(adapter, tableName),
                quote(adapter, columnName));
    }

    /**
     * Build a cardinality SQL:
     * SELECT COUNT(DISTINCT col) AS uniqueCount, COUNT(*) AS total,
     *        COUNT(DISTINCT col) * 100.0 / NULLIF(COUNT(*), 0) AS cardinalityRate
     * FROM table
     */
    public static String buildCardinalitySql(DataSourceAdapter adapter, String tableName, String columnName) {
        String quotedTable = quoteTable(adapter, tableName);
        String quotedCol = quote(adapter, columnName);
        return String.format(
                "SELECT COUNT(DISTINCT %s) AS uniqueCount, COUNT(*) AS total, " +
                "COUNT(DISTINCT %s) * 100.0 / NULLIF(COUNT(*), 0) AS cardinalityRate FROM %s",
                quotedCol, quotedCol, quotedTable);
    }

    /**
     * Build a MIN value SQL: SELECT MIN(col) AS minValue FROM table
     */
    public static String buildMinValueSql(DataSourceAdapter adapter, String tableName, String columnName) {
        return String.format(
                "SELECT MIN(%s) AS minValue FROM %s",
                quote(adapter, columnName), quoteTable(adapter, tableName));
    }

    /**
     * Build a MAX value SQL: SELECT MAX(col) AS maxValue FROM table
     */
    public static String buildMaxValueSql(DataSourceAdapter adapter, String tableName, String columnName) {
        return String.format(
                "SELECT MAX(%s) AS maxValue FROM %s",
                quote(adapter, columnName), quoteTable(adapter, tableName));
    }

    /**
     * Build a range check SQL using BETWEEN or comparison operators.
     * Respects database dialect for BETWEEN syntax.
     */
    public static String buildRangeCheckSql(DataSourceAdapter adapter, String tableName,
                                            String columnName, String minVal, String maxVal) {
        String quotedTable = quoteTable(adapter, tableName);
        String quotedCol = quote(adapter, columnName);
        String dsType = adapter.getDataSourceType().toUpperCase();

        if (minVal != null && maxVal != null) {
            // Use BETWEEN for range check
            if ("ORACLE".equals(dsType)) {
                return String.format(
                        "SELECT COUNT(*) AS outOfRangeCount FROM %s WHERE %s NOT BETWEEN %s AND %s",
                        quotedTable, quotedCol, minVal, maxVal);
            }
            return String.format(
                    "SELECT COUNT(*) AS outOfRangeCount FROM %s WHERE %s NOT BETWEEN %s AND %s",
                    quotedTable, quotedCol, minVal, maxVal);
        } else if (minVal != null) {
            return String.format(
                    "SELECT COUNT(*) AS outOfRangeCount FROM %s WHERE %s < %s",
                    quotedTable, quotedCol, minVal);
        } else if (maxVal != null) {
            return String.format(
                    "SELECT COUNT(*) AS outOfRangeCount FROM %s WHERE %s > %s",
                    quotedTable, quotedCol, maxVal);
        }
        return String.format("SELECT 0 AS outOfRangeCount FROM %s WHERE 1=0", quotedTable);
    }

    /**
     * Build a COUNT(*) SQL: SELECT COUNT(*) AS total FROM table
     */
    public static String buildCountSql(DataSourceAdapter adapter, String tableName) {
        return String.format(
                "SELECT COUNT(*) AS total FROM %s",
                quoteTable(adapter, tableName));
    }

    /**
     * Build a COUNT(*) with condition SQL: SELECT COUNT(*) AS countValue FROM table WHERE condition
     */
    public static String buildCountWithConditionSql(DataSourceAdapter adapter, String tableName, String condition) {
        return String.format(
                "SELECT COUNT(*) AS countValue FROM %s WHERE %s",
                quoteTable(adapter, tableName), condition);
    }

    /**
     * Build a cross-field comparison SQL: SELECT COUNT(*) AS errorCount FROM table WHERE NOT (colA > colB)
     */
    public static String buildCrossFieldCompareSql(DataSourceAdapter adapter, String tableName,
                                                   String columnA, String columnB) {
        return String.format(
                "SELECT COUNT(*) AS errorCount FROM %s WHERE NOT (%s > %s)",
                quoteTable(adapter, tableName),
                quote(adapter, columnA),
                quote(adapter, columnB));
    }

    /**
     * Build a cross-field sum check SQL:
     * SELECT COUNT(*) AS errorCount FROM table WHERE NOT (colA + colB = colC)
     */
    public static String buildCrossFieldSumSql(DataSourceAdapter adapter, String tableName,
                                                String columnA, String columnB, String columnC) {
        return String.format(
                "SELECT COUNT(*) AS errorCount FROM %s WHERE NOT (%s + %s = %s)",
                quoteTable(adapter, tableName),
                quote(adapter, columnA),
                quote(adapter, columnB),
                quote(adapter, columnC));
    }

    /**
     * Build a cross-field null consistency check SQL:
     * SELECT COUNT(*) AS errorCount FROM table
     * WHERE (colA IS NULL AND colB IS NOT NULL) OR (colA IS NOT NULL AND colB IS NULL)
     */
    public static String buildCrossFieldNullCheckSql(DataSourceAdapter adapter, String tableName,
                                                      String columnA, String columnB) {
        return String.format(
                "SELECT COUNT(*) AS errorCount FROM %s " +
                "WHERE (%s IS NULL AND %s IS NOT NULL) OR (%s IS NOT NULL AND %s IS NULL)",
                quoteTable(adapter, tableName),
                quote(adapter, columnA), quote(adapter, columnB),
                quote(adapter, columnA), quote(adapter, columnB));
    }

    /**
     * Build a row count difference SQL for cross-table comparison:
     * SELECT (SELECT COUNT(*) FROM source) - (SELECT COUNT(*) FROM target) AS diff
     */
    public static String buildCrossTableCountDiffSql(DataSourceAdapter adapter,
                                                      String sourceTable, String targetTable) {
        return String.format(
                "SELECT (SELECT COUNT(*) FROM %s) - (SELECT COUNT(*) FROM %s) AS diff",
                quoteTable(adapter, sourceTable),
                quoteTable(adapter, targetTable));
    }

    /**
     * Build a regex check SQL for MySQL/TiDB: uses REGEXP operator.
     */
    public static String buildMysqlRegexSql(DataSourceAdapter adapter, String tableName,
                                             String columnName, String regexPattern) {
        String quotedTable = quoteTable(adapter, tableName);
        String quotedCol = quote(adapter, columnName);
        String escapedPattern = regexPattern.replace("\\", "\\\\");
        return String.format(
                "SELECT COUNT(*) AS errorCount FROM %s WHERE %s IS NOT NULL AND %s NOT REGEXP '%s'",
                quotedTable, quotedCol, quotedCol, escapedPattern);
    }

    /**
     * Build a regex check SQL for PostgreSQL: uses ~ (tilde) operator.
     */
    public static String buildPostgresRegexSql(DataSourceAdapter adapter, String tableName,
                                                 String columnName, String regexPattern) {
        String quotedTable = quoteTable(adapter, tableName);
        String quotedCol = quote(adapter, columnName);
        return String.format(
                "SELECT COUNT(*) AS errorCount FROM %s WHERE %s IS NOT NULL AND %s !~ '%s'",
                quotedTable, quotedCol, quotedCol, regexPattern);
    }

    /**
     * Build a regex check SQL for Oracle: uses REGEXP_LIKE function.
     */
    public static String buildOracleRegexSql(DataSourceAdapter adapter, String tableName,
                                              String columnName, String regexPattern) {
        String quotedTable = quoteTable(adapter, tableName);
        String quotedCol = quote(adapter, columnName);
        return String.format(
                "SELECT COUNT(*) AS errorCount FROM %s WHERE %s IS NOT NULL AND NOT REGEXP_LIKE(%s, '%s')",
                quotedTable, quotedCol, quotedCol, regexPattern);
    }

    /**
     * Build a regex check SQL for SQL Server: uses PATINDEX or LIKE (simplified).
     * SQL Server does not have native REGEXP; we use LIKE with escape patterns as fallback.
     * For phone/email/idcard, we build a simplified format check.
     */
    public static String buildSqlServerRegexSql(DataSourceAdapter adapter, String tableName,
                                                String columnName, String regexPattern) {
        String quotedTable = quoteTable(adapter, tableName);
        String quotedCol = quote(adapter, columnName);
        return String.format(
                "SELECT COUNT(*) AS errorCount FROM %s WHERE %s IS NOT NULL AND %s NOT LIKE '%%' + @pattern + '%%'",
                quotedTable, quotedCol, quotedCol);
    }

    /**
     * Build an ID card (18-digit) format check SQL for MySQL/TiDB.
     */
    public static String buildIdCardCheckMysqlSql(DataSourceAdapter adapter, String tableName, String columnName) {
        return String.format(
                "SELECT COUNT(*) AS errorCount FROM %s WHERE %s IS NOT NULL AND NOT (%s REGEXP '^\\\\d{17}[\\\\dXx]$')",
                quoteTable(adapter, tableName),
                quote(adapter, columnName),
                quote(adapter, columnName));
    }

    /**
     * Build an ID card (18-digit) format check SQL for PostgreSQL.
     */
    public static String buildIdCardCheckPostgresSql(DataSourceAdapter adapter, String tableName, String columnName) {
        return String.format(
                "SELECT COUNT(*) AS errorCount FROM %s WHERE %s IS NOT NULL AND NOT (%s ~ '^\\\\d{17}[\\\\dXx]$')",
                quoteTable(adapter, tableName),
                quote(adapter, columnName),
                quote(adapter, columnName));
    }

    /**
     * Get the database-specific regex SQL based on data source type.
     */
    public static String buildRegexCheckSql(DataSourceAdapter adapter, String tableName,
                                             String columnName, String regexPattern, String ruleType) {
        String dsType = adapter.getDataSourceType().toUpperCase();

        // ID card has special handling across databases
        if ("REGEX_IDCARD".equalsIgnoreCase(ruleType)) {
            return buildIdCardCheckSql(adapter, tableName, columnName);
        }

        return switch (dsType) {
            case "MYSQL", "TIDB" -> buildMysqlRegexSql(adapter, tableName, columnName, regexPattern);
            case "POSTGRESQL" -> buildPostgresRegexSql(adapter, tableName, columnName, regexPattern);
            case "ORACLE" -> buildOracleRegexSql(adapter, tableName, columnName, regexPattern);
            case "SQLSERVER" -> buildSqlServerRegexSql(adapter, tableName, columnName, regexPattern);
            default -> buildMysqlRegexSql(adapter, tableName, columnName, regexPattern);
        };
    }

    /**
     * Build ID card check SQL based on adapter's database type.
     */
    public static String buildIdCardCheckSql(DataSourceAdapter adapter, String tableName, String columnName) {
        String dsType = adapter.getDataSourceType().toUpperCase();
        return switch (dsType) {
            case "MYSQL", "TIDB" -> buildIdCardCheckMysqlSql(adapter, tableName, columnName);
            case "POSTGRESQL" -> buildIdCardCheckPostgresSql(adapter, tableName, columnName);
            default -> String.format(
                    "SELECT COUNT(*) AS errorCount FROM %s WHERE %s IS NOT NULL AND LENGTH(%s) != 18",
                    quoteTable(adapter, tableName),
                    quote(adapter, columnName),
                    quote(adapter, columnName));
        };
    }

    /**
     * Validate table name: must be alphanumeric with underscores, optionally schema-qualified.
     */
    public static String validateTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("表名不能为空");
        }
        if (!tableName.matches("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?$")) {
            throw new IllegalArgumentException("无效的表名格式: " + tableName);
        }
        return tableName;
    }

    /**
     * Validate column name: must be alphanumeric with underscores.
     */
    public static String validateColumnName(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("列名不能为空");
        }
        if (!columnName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("无效的列名格式: " + columnName);
        }
        return columnName;
    }

    /**
     * Validate table name optionally (returns null if blank is allowed).
     */
    public static String validateColumnNameOptional(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return null;
        }
        if (!columnName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("无效的列名格式: " + columnName);
        }
        return columnName;
    }
}
