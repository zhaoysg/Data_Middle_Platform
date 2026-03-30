package com.bagdatahouse.datasource.manager;

import com.alibaba.druid.pool.DruidDataSource;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import com.bagdatahouse.datasource.enums.DataSourceTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源管理器（性能优化版）
 * 负责管理多个数据源的连接池，并同步注册到适配器注册表
 */
@Slf4j
@Component
public class DynamicDataSourceManager {

    private DataSourceAdapterRegistry adapterRegistry;

    @Autowired(required = false)
    @Lazy
    public void setAdapterRegistry(DataSourceAdapterRegistry adapterRegistry) {
        this.adapterRegistry = adapterRegistry;
    }

    /**
     * 数据源连接池缓存
     * key: dsId
     */
    private final Map<Long, DruidDataSource> configuredDataSources = new ConcurrentHashMap<>();

    /**
     * 数据源配置缓存（用于适配器获取连接信息）
     * key: dsId
     */
    private final Map<Long, DqDatasource> datasourceCache = new ConcurrentHashMap<>();

    /**
     * JdbcTemplate 缓存（避免重复创建）
     * key: dsId
     */
    private final Map<Long, JdbcTemplate> jdbcTemplateCache = new ConcurrentHashMap<>();

    /**
     * 获取指定数据源ID的JdbcTemplate（带缓存）
     */
    public JdbcTemplate getJdbcTemplateById(Long dsId) {
        // 先从缓存获取
        JdbcTemplate cached = jdbcTemplateCache.get(dsId);
        if (cached != null) {
            return cached;
        }
        
        DruidDataSource dataSource = configuredDataSources.get(dsId);
        if (dataSource != null) {
            JdbcTemplate template = new JdbcTemplate(dataSource);
            jdbcTemplateCache.put(dsId, template);
            return template;
        }
        
        DqDatasource ds = datasourceCache.get(dsId);
        if (ds != null) {
            registerDataSource(dsId, ds);
            dataSource = configuredDataSources.get(dsId);
            if (dataSource != null) {
                JdbcTemplate template = new JdbcTemplate(dataSource);
                jdbcTemplateCache.put(dsId, template);
                return template;
            }
        }
        return null;
    }

    /**
     * 注册一个数据源（性能优化版）
     */
    public void registerDataSource(Long dsId, DqDatasource ds) {
        try {
            DataSourceTypeEnum typeEnum = DataSourceTypeEnum.fromCode(ds.getDsType());
            if (typeEnum == null) {
                throw new IllegalArgumentException("不支持的数据源类型: " + ds.getDsType());
            }

            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setDriverClassName(typeEnum.getDriverClass());
            dataSource.setUrl(buildJdbcUrl(typeEnum, ds));
            dataSource.setUsername(ds.getUsername());
            dataSource.setPassword(ds.getPassword());
            
            // ==================== 连接池性能优化配置 ====================
            // 初始连接数
            dataSource.setInitialSize(5);
            // 最小空闲连接数
            dataSource.setMinIdle(5);
            // 最大活跃连接数（根据实际需求调整）
            dataSource.setMaxActive(20);
            // 获取连接最大等待时间
            dataSource.setMaxWait(60000L);
            // 连接检测间隔
            dataSource.setTimeBetweenEvictionRunsMillis(60000L);
            // 最小生存时间
            dataSource.setMinEvictableIdleTimeMillis(300000L);
            // 验证查询SQL
            dataSource.setValidationQuery(getValidationQuery(ds.getDsType()));
            // 空闲时检测连接有效性
            dataSource.setTestWhileIdle(true);
            // 获取时检测（生产环境建议关闭）
            dataSource.setTestOnBorrow(false);
            // 归还时检测
            dataSource.setTestOnReturn(false);
            
            // ==================== 性能优化关键配置 ====================
            // 启用PreparedStatement缓存
            dataSource.setPoolPreparedStatements(true);
            dataSource.setMaxPoolPreparedStatementPerConnectionSize(50);
            
            // 连接泄漏检测
            dataSource.setRemoveAbandoned(true);
            dataSource.setRemoveAbandonedTimeout(1800);
            dataSource.setLogAbandoned(true);
            
            // 统计与日志（不使用 wall：DQC/元数据等会下发动态只读 SQL，Druid Wall 常误判合法语句如 SELECT MAX(col)）
            dataSource.setFilters("stat,slf4j");

            // MySQL / TiDB 特定优化
            if ("MYSQL".equalsIgnoreCase(ds.getDsType()) || "TIDB".equalsIgnoreCase(ds.getDsType())) {
                dataSource.setValidationQueryTimeout(3);
                dataSource.setConnectionProperties("druid.stat.mergeSql=true;druid.stat.slowSqlMillis=3000");
            }

            // 初始化连接池
            dataSource.init();

            configuredDataSources.put(dsId, dataSource);
            datasourceCache.put(dsId, ds);

            if (adapterRegistry != null) {
                DataSourceAdapter adapter = adapterRegistry.getAdapter(ds.getDsType());
                adapterRegistry.registerAdapter(dsId, ds.getDsType(), adapter);
            }

            log.info("数据源注册成功（性能优化模式）: id={}, name={}, type={}", dsId, ds.getDsName(), ds.getDsType());
        } catch (Exception e) {
            log.error("数据源注册失败: id={}", dsId, e);
            throw new RuntimeException("数据源注册失败: " + e.getMessage(), e);
        }
    }

    /**
     * 注销一个数据源
     */
    public void unregisterDataSource(Long dsId) {
        DruidDataSource dataSource = configuredDataSources.remove(dsId);
        if (dataSource != null) {
            dataSource.close();
        }
        datasourceCache.remove(dsId);
        // 清理JdbcTemplate缓存
        jdbcTemplateCache.remove(dsId);
        
        if (adapterRegistry != null) {
            adapterRegistry.unregisterAdapter(dsId);
        }
        log.info("数据源已注销: id={}", dsId);
    }

    /**
     * 刷新指定数据源的连接池
     */
    public void refreshDataSource(Long dsId) {
        DqDatasource ds = datasourceCache.get(dsId);
        if (ds != null) {
            unregisterDataSource(dsId);
            registerDataSource(dsId, ds);
            log.info("数据源连接池已刷新: id={}", dsId);
        }
    }

    /**
     * 获取缓存的数据源配置
     */
    public DqDatasource getDatasource(Long dsId) {
        return datasourceCache.get(dsId);
    }

    /**
     * 获取指定数据源的 schema 名称（用于 PostgreSQL 等）
     * @return schema 名称，若未设置则返回 null
     */
    public String getSchemaName(Long dsId) {
        DqDatasource ds = datasourceCache.get(dsId);
        return ds != null ? ds.getSchemaName() : null;
    }

    /**
     * 检查数据源是否已注册
     */
    public boolean isRegistered(Long dsId) {
        return configuredDataSources.containsKey(dsId);
    }

    /**
     * 获取所有已注册的数据源ID
     */
    public Long[] getRegisteredIds() {
        return configuredDataSources.keySet().toArray(new Long[0]);
    }
    
    /**
     * 获取连接池统计信息
     */
    public Map<String, Object> getDataSourceStats(Long dsId) {
        DruidDataSource dataSource = configuredDataSources.get(dsId);
        if (dataSource == null) {
            return null;
        }
        
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("activeCount", dataSource.getActiveCount());
        stats.put("poolingCount", dataSource.getPoolingCount());
        stats.put("createCount", dataSource.getCreateCount());
        stats.put("destroyCount", dataSource.getDestroyCount());
        stats.put("activePeak", dataSource.getActivePeak());
        stats.put("activePeakTime", dataSource.getActivePeakTime());
        stats.put("connectCount", dataSource.getConnectCount());
        stats.put("errorCount", dataSource.getErrorCount());
        stats.put("executeCount", dataSource.getExecuteCount());
        
        return stats;
    }

    /**
     * 根据数据源类型构建JDBC URL
     */
    private String buildJdbcUrl(DataSourceTypeEnum typeEnum, DqDatasource ds) {
        String host = ds.getHost();
        int port = ds.getPort() != null && ds.getPort() > 0 ? ds.getPort() : typeEnum.getDefaultPort();
        String dbName = ds.getDatabaseName();
        String baseUrl = String.format(typeEnum.getUrlTemplate(), host, port, dbName);

        // PostgreSQL 支持 currentSchema 参数
        if ("POSTGRESQL".equalsIgnoreCase(ds.getDsType())
                && ds.getSchemaName() != null && !ds.getSchemaName().isBlank()) {
            baseUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?")
                    + "currentSchema=" + ds.getSchemaName().trim();
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
