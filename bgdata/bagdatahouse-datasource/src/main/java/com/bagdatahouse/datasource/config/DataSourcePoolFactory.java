package com.bagdatahouse.datasource.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.datasource.enums.DataSourceTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

/**
 * 数据源连接池工厂（性能优化版）
 * 提供标准化的连接池配置
 */
@Slf4j
public class DataSourcePoolFactory {

    /**
     * 创建优化后的DruidDataSource
     *
     * @param ds 数据源配置
     * @return 配置好的DruidDataSource
     */
    public static DruidDataSource createOptimizedDataSource(DqDatasource ds) {
        DataSourceTypeEnum typeEnum = DataSourceTypeEnum.fromCode(ds.getDsType());
        if (typeEnum == null) {
            throw new IllegalArgumentException("不支持的数据源类型: " + ds.getDsType());
        }

        DruidDataSource dataSource = new DruidDataSource();
        
        // 基础连接配置
        dataSource.setDriverClassName(typeEnum.getDriverClass());
        dataSource.setUrl(buildJdbcUrl(typeEnum, ds));
        dataSource.setUsername(ds.getUsername());
        dataSource.setPassword(ds.getPassword());

        // ==================== 性能优化配置 ====================
        
        // 连接池大小配置
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);

        // 连接获取配置
        dataSource.setMaxWait(60000L);
        dataSource.setConnectionErrorRetryAttempts(3);
        dataSource.setBreakAfterAcquireFailure(true);
        dataSource.setFailFast(true);

        // 连接检测配置
        dataSource.setTimeBetweenEvictionRunsMillis(60000L);
        dataSource.setMinEvictableIdleTimeMillis(300000L);
        dataSource.setValidationQuery(getValidationQuery(ds.getDsType()));
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);

        // PreparedStatement缓存（关键性能优化）
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(50);

        // 连接泄漏检测
        dataSource.setRemoveAbandoned(true);
        dataSource.setRemoveAbandonedTimeout(1800);
        dataSource.setLogAbandoned(true);

        // 统计与日志（不使用 wall：动态业务 SQL 由平台规则生成，Wall 易误判合法查询）
        try {
            dataSource.setFilters("stat,slf4j");
        } catch (SQLException e) {
            log.warn("设置Druid过滤器失败: {}", e.getMessage());
        }

        // 数据源特定优化
        if ("MYSQL".equalsIgnoreCase(ds.getDsType()) || "TIDB".equalsIgnoreCase(ds.getDsType())) {
            // MySQL特定优化
            dataSource.setValidationQueryTimeout(3);
            // 连接属性优化
            dataSource.setConnectionProperties("druid.stat.mergeSql=true;druid.stat.slowSqlMillis=3000");
        }

        // 初始化连接池
        try {
            dataSource.init();
        } catch (SQLException e) {
            log.error("初始化Druid连接池失败: {}", e.getMessage());
            throw new RuntimeException("初始化数据源连接池失败", e);
        }

        log.info("创建优化数据源: type={}, url={}", ds.getDsType(), dataSource.getUrl());
        return dataSource;
    }

    /**
     * 创建JdbcTemplate（带性能优化配置）
     */
    public static JdbcTemplate createOptimizedJdbcTemplate(DruidDataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        
        // 查询超时配置
        jdbcTemplate.setQueryTimeout(60);
        
        // 批量操作优化
        jdbcTemplate.setFetchSize(100);
        jdbcTemplate.setMaxRows(500);
        
        return jdbcTemplate;
    }

    /**
     * 构建JDBC URL
     */
    private static String buildJdbcUrl(DataSourceTypeEnum typeEnum, DqDatasource ds) {
        String host = ds.getHost();
        int port = ds.getPort() != null && ds.getPort() > 0 ? ds.getPort() : typeEnum.getDefaultPort();
        String dbName = ds.getDatabaseName();
        
        String baseUrl = String.format(typeEnum.getUrlTemplate(), host, port, dbName);
        
        // 根据数据源类型添加性能优化参数
        if ("MYSQL".equalsIgnoreCase(typeEnum.name()) || "TIDB".equalsIgnoreCase(typeEnum.name())) {
            // MySQL连接性能优化参数
            if (!baseUrl.contains("?")) {
                baseUrl += "?useUnicode=true&characterEncoding=utf8" +
                        "&zeroDateTimeBehavior=convertToNull" +
                        "&useSSL=false" +
                        "&serverTimezone=Asia/Shanghai" +
                        "&allowPublicKeyRetrieval=true" +
                        "&rewriteBatchedStatements=true" +
                        "&cachePrepStmts=true" +
                        "&prepStmtCacheSize=250" +
                        "&prepStmtCacheSqlLimit=2048";
            }
        } else if ("POSTGRESQL".equalsIgnoreCase(typeEnum.name())) {
            // PostgreSQL连接性能优化参数
            if (!baseUrl.contains("?")) {
                baseUrl += "?prepareThreshold=1" +
                        "&preparedStatementCacheSize=50" +
                        "&cachePrepStmts=true";
            }
        }
        
        return baseUrl;
    }

    /**
     * 获取数据源的验证查询SQL
     */
    private static String getValidationQuery(String dsType) {
        return switch (dsType.toUpperCase()) {
            case "ORACLE" -> "SELECT 1 FROM DUAL";
            case "SQLSERVER" -> "SELECT 1";
            default -> "SELECT 1";
        };
    }
}
