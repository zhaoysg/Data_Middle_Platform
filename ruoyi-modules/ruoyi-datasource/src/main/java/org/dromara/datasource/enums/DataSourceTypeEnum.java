package org.dromara.datasource.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据源类型枚举
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum DataSourceTypeEnum {

    MYSQL("MySQL", 3306, "com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"),
    SQLSERVER("SQL Server", 1433, "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true"),
    ORACLE("Oracle", 1521, "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@%s:%d:%s"),
    POSTGRESQL("PostgreSQL", 5432, "org.postgresql.Driver", "jdbc:postgresql://%s:%d/%s"),
    TIDB("TiDB", 3306, "com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");

    /**
     * 显示名称
     */
    private final String displayName;

    /**
     * 默认端口
     */
    private final int defaultPort;

    /**
     * JDBC驱动类名
     */
    private final String driverClass;

    /**
     * JDBC URL 模板
     */
    private final String urlTemplate;

    /**
     * 根据编码查找枚举
     *
     * @param code 类型编码
     * @return 枚举实例，不存在返回 null
     */
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

    /**
     * 判断是否支持该类型
     *
     * @param code 类型编码
     * @return 是否支持
     */
    public static boolean isSupported(String code) {
        return fromCode(code) != null;
    }
}
