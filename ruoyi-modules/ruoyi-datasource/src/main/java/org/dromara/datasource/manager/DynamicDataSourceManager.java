package org.dromara.datasource.manager;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.enums.DataSourceTypeEnum;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源管理器
 * 负责运行时注册/注销外部数据源连接池，并提供 JdbcTemplate 访问
 *
 * @author Lion Li
 */
@Slf4j
@Component
public class DynamicDataSourceManager {

    /**
     * 数据源连接池缓存
     * key: dsId
     */
    private final Map<Long, HikariDataSource> configuredDataSources = new ConcurrentHashMap<>();

    /**
     * JdbcTemplate 缓存（避免重复创建）
     * key: dsId
     */
    private final Map<Long, JdbcTemplate> jdbcTemplateCache = new ConcurrentHashMap<>();

    /**
     * 获取指定数据源ID的JdbcTemplate（带缓存）
     *
     * @param dsId 数据源ID
     * @return JdbcTemplate，不存在返回 null
     */
    public JdbcTemplate getJdbcTemplateById(Long dsId) {
        JdbcTemplate cached = jdbcTemplateCache.get(dsId);
        if (cached != null) {
            return cached;
        }
        HikariDataSource dataSource = configuredDataSources.get(dsId);
        if (dataSource != null) {
            JdbcTemplate template = new JdbcTemplate(dataSource);
            jdbcTemplateCache.put(dsId, template);
            return template;
        }
        return null;
    }

    /**
     * 注册一个数据源
     *
     * @param dsId 数据源ID
     * @param dsType 数据源类型
     * @param host 主机
     * @param port 端口
     * @param databaseName 数据库名
     * @param schemaName Schema名（PostgreSQL专用，可为null）
     * @param username 用户名
     * @param password 密码
     * @param jdbcUrlTemplate JDBC URL模板
     * @param driverClass 驱动类名
     */
    public void registerDataSource(Long dsId, String dsType, String host, Integer port,
                                   String databaseName, String schemaName,
                                   String username, String password,
                                   String jdbcUrlTemplate, String driverClass) {
        try {
            if (configuredDataSources.containsKey(dsId)) {
                log.info("数据源已注册，先注销再重新注册: dsId={}", dsId);
                unregisterDataSource(dsId);
            }

            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setPoolName("dynamic-ds-" + dsId);
            dataSource.setDriverClassName(driverClass);
            dataSource.setJdbcUrl(buildJdbcUrl(jdbcUrlTemplate, host, port, databaseName, dsType, schemaName));
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setMinimumIdle(5);
            dataSource.setMaximumPoolSize(20);
            dataSource.setConnectionTimeout(60000L);
            dataSource.setValidationTimeout(5000L);
            dataSource.setIdleTimeout(300000L);
            dataSource.setMaxLifetime(1800000L);
            dataSource.setKeepaliveTime(30000L);
            dataSource.setConnectionTestQuery(getValidationQuery(dsType));
            dataSource.setInitializationFailTimeout(5000L);

            // 提前初始化一条连接，避免配置问题等到首次访问才暴露。
            dataSource.getConnection().close();
            configuredDataSources.put(dsId, dataSource);
            log.info("数据源注册成功: dsId={}, type={}, host={}", dsId, dsType, host);
        } catch (Exception e) {
            log.error("数据源注册失败: dsId={}", dsId, e);
            throw new RuntimeException("数据源注册失败: " + e.getMessage(), e);
        }
    }

    /**
     * 注销一个数据源
     *
     * @param dsId 数据源ID
     */
    public void unregisterDataSource(Long dsId) {
        HikariDataSource dataSource = configuredDataSources.remove(dsId);
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception e) {
                log.warn("关闭数据源连接池失败: dsId={}", dsId, e);
            }
        }
        jdbcTemplateCache.remove(dsId);
        log.info("数据源已注销: dsId={}", dsId);
    }

    /**
     * 检查数据源是否已注册
     *
     * @param dsId 数据源ID
     * @return 是否已注册
     */
    public boolean isRegistered(Long dsId) {
        return configuredDataSources.containsKey(dsId);
    }

    /**
     * 获取连接池统计信息
     *
     * @param dsId 数据源ID
     * @return 统计信息Map
     */
    public Map<String, Object> getDataSourceStats(Long dsId) {
        HikariDataSource dataSource = configuredDataSources.get(dsId);
        if (dataSource == null) {
            return null;
        }
        Map<String, Object> stats = new HashMap<>();
        HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();
        if (poolBean != null) {
            stats.put("activeCount", poolBean.getActiveConnections());
            stats.put("idleCount", poolBean.getIdleConnections());
            stats.put("totalCount", poolBean.getTotalConnections());
            stats.put("threadsAwaitingConnection", poolBean.getThreadsAwaitingConnection());
        }
        stats.put("maxPoolSize", dataSource.getMaximumPoolSize());
        stats.put("minIdle", dataSource.getMinimumIdle());
        return stats;
    }

    /**
     * 构建 JDBC URL
     */
    private String buildJdbcUrl(String urlTemplate, String host, Integer port,
                                 String databaseName, String dsType, String schemaName) {
        DataSourceTypeEnum typeEnum = DataSourceTypeEnum.fromCode(dsType);
        int targetPort = port != null && port > 0
            ? port
            : typeEnum != null ? typeEnum.getDefaultPort() : 3306;
        String baseUrl = String.format(urlTemplate, host, targetPort, databaseName);

        // PostgreSQL 支持 currentSchema 参数
        if ("POSTGRESQL".equalsIgnoreCase(dsType) && schemaName != null && !schemaName.isBlank()) {
            baseUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schemaName.trim();
        }
        return baseUrl;
    }

    /**
     * 获取数据源的验证查询SQL
     */
    private String getValidationQuery(String dsType) {
        return switch (dsType.toUpperCase()) {
            case "ORACLE" -> "SELECT 1 FROM DUAL";
            case "SQLSERVER" -> "SELECT 1";
            default -> "SELECT 1";
        };
    }
}
