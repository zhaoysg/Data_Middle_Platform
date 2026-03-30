package com.bagdatahouse.datasource.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据源类型枚举
 */
@Getter
@AllArgsConstructor
public enum DataSourceTypeEnum {

    MYSQL("MySQL", "MYSQL", 3306, "com.mysql.cj.jdbc.Driver", false, null),
    SQLSERVER("SQL Server", "MSSQL", 1433, "com.microsoft.sqlserver.jdbc.SQLServerDriver", false, null),
    ORACLE("Oracle", "ORA", 1521, "oracle.jdbc.OracleDriver", true, null),
    POSTGRESQL("PostgreSQL", "PG", 5432, "org.postgresql.Driver", false, "postgres"),
    TIDB("TiDB", "TIDB", 3306, "com.mysql.cj.jdbc.Driver", false, null);

    private final String displayName;
    private final String codePrefix;
    private final int defaultPort;
    private final String driverClass;
    private final boolean databaseNameRequired;
    private final String defaultDatabaseWhenBlank;

    public static DataSourceTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataSourceTypeEnum type : values()) {
            if (type.name().equalsIgnoreCase(code) || type.displayName.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isSupported(String code) {
        return fromCode(code) != null;
    }
}
