package com.bagdatahouse.datasource.adapter;

import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.datasource.manager.DynamicDataSourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源适配器注册表
 * 统一管理所有数据源适配器，提供按类型和数据源ID查找功能
 */
@Slf4j
@Component
public class DataSourceAdapterRegistry {

    private final DynamicDataSourceManager dataSourceManager;
    private final DqDatasourceMapper datasourceMapper;

    /**
     * 类型到适配器的映射（原型实例）
     */
    private final Map<String, DataSourceAdapter> adapterMap = new ConcurrentHashMap<>();

    /**
     * 数据源ID到适配器的动态注册映射
     */
    private final Map<Long, DataSourceAdapter> dsIdAdapterMap = new ConcurrentHashMap<>();

    public DataSourceAdapterRegistry(
            @Lazy DynamicDataSourceManager dataSourceManager,
            DqDatasourceMapper datasourceMapper,
            MySQLAdapter mysqlAdapter,
            SqlServerAdapter sqlServerAdapter,
            OracleAdapter oracleAdapter,
            PostgresAdapter postgresAdapter) {
        this.dataSourceManager = dataSourceManager;
        this.datasourceMapper = datasourceMapper;

        adapterMap.put("MYSQL", mysqlAdapter);
        adapterMap.put("SQLSERVER", sqlServerAdapter);
        adapterMap.put("ORACLE", oracleAdapter);
        adapterMap.put("POSTGRESQL", postgresAdapter);
        // TiDB 继承 MySQL，无需独立 Bean
        adapterMap.put("TIDB", new TiDBAdapter(dataSourceManager));

        log.info("数据源适配器注册表初始化完成，已注册类型: {}", adapterMap.keySet());
    }

    /**
     * 获取指定类型的适配器
     * @param dsType 数据源类型 (MYSQL, SQLSERVER, ORACLE, POSTGRESQL, TIDB)
     * @return 对应的适配器实例
     */
    public DataSourceAdapter getAdapter(String dsType) {
        DataSourceAdapter adapter = adapterMap.get(dsType.toUpperCase());
        if (adapter == null) {
            log.warn("未找到对应类型的数据源适配器: {}", dsType);
            throw new IllegalArgumentException("不支持的数据源类型: " + dsType);
        }
        return adapter;
    }

    /**
     * 获取指定类型的适配器（返回Optional）
     */
    public Optional<DataSourceAdapter> getAdapterByType(String dsType) {
        return Optional.ofNullable(adapterMap.get(dsType.toUpperCase()));
    }

    /**
     * 根据数据源ID获取适配器
     * 先查缓存；缓存未命中则：
     *   1. 从 DB 查出数据源配置
     *   2. 在连接池注册（如果尚未注册）
     *   3. 为该 dsId 创建独立的适配器实例（不复用 singleton，避免并发覆盖 dsId）
     *   4. 缓存并返回
     */
    public DataSourceAdapter getAdapterById(Long dsId) {
        DataSourceAdapter cached = dsIdAdapterMap.get(dsId);
        if (cached != null) {
            return cached;
        }

        DqDatasource datasource = datasourceMapper.selectById(dsId);
        if (datasource == null) {
            log.warn("数据源不存在: dsId={}", dsId);
            return null;
        }

        String dsType = datasource.getDsType().toUpperCase();

        // 关键修复：确保 DynamicDataSourceManager 中已注册该数据源的连接池，
        // 否则 getTables/getColumns 时 JdbcTemplate 为 null
        if (!dataSourceManager.isRegistered(dsId)) {
            try {
                dataSourceManager.registerDataSource(dsId, datasource);
            } catch (Exception e) {
                log.error("数据源连接池注册失败: dsId={}, error={}", dsId, e.getMessage());
                throw new RuntimeException("数据源连接池注册失败: " + e.getMessage(), e);
            }
        }

        // 为当前 dsId 创建独立的适配器实例（不再复用 singleton，避免并发覆盖 dsId）
        DataSourceAdapter baseAdapter = adapterMap.get(dsType);
        if (baseAdapter == null) {
            log.warn("未找到适配器类型: dsType={}", dsType);
            return null;
        }

        DataSourceAdapter newAdapter = createAdapterInstance(dsType, baseAdapter);
        newAdapter.setDsId(dsId);
        dsIdAdapterMap.put(dsId, newAdapter);
        log.debug("数据源适配器已创建（独立实例）: dsId={}, dsType={}", dsId, dsType);
        return newAdapter;
    }

    /** 为给定 dsType 创建独立适配器实例 */
    private DataSourceAdapter createAdapterInstance(String dsType, DataSourceAdapter base) {
        return switch (dsType) {
            case "MYSQL" -> new MySQLAdapter(dataSourceManager);
            case "SQLSERVER" -> new SqlServerAdapter(dataSourceManager);
            case "ORACLE" -> new OracleAdapter(dataSourceManager);
            case "POSTGRESQL" -> new PostgresAdapter(dataSourceManager);
            case "TIDB" -> new TiDBAdapter(dataSourceManager);
            default -> base; // 兜底（理论上不会走到这里）
        };
    }

    /**
     * 动态注册一个数据源ID的适配器（覆盖缓存）
     */
    public void registerAdapter(Long dsId, String dsType, DataSourceAdapter adapter) {
        adapter.setDsId(dsId);
        dsIdAdapterMap.put(dsId, adapter);
        log.info("数据源适配器动态注册成功: dsId={}, dsType={}", dsId, dsType);
    }

    /**
     * 注销指定数据源ID的适配器
     */
    public void unregisterAdapter(Long dsId) {
        DataSourceAdapter removed = dsIdAdapterMap.remove(dsId);
        if (removed != null) {
            log.info("数据源适配器已注销: dsId={}", dsId);
        }
    }

    /**
     * 获取指定数据源ID的JdbcTemplate
     */
    public JdbcTemplate getJdbcTemplateById(Long dsId) {
        return dataSourceManager.getJdbcTemplateById(dsId);
    }

    /**
     * 检查是否支持指定类型
     */
    public boolean isSupported(String dsType) {
        return adapterMap.containsKey(dsType.toUpperCase());
    }

    /**
     * 获取所有已注册的数据源类型
     */
    public List<String> getSupportedTypes() {
        return List.copyOf(adapterMap.keySet());
    }

    /**
     * 清除缓存（当数据源配置变更时调用）
     */
    public void clearCache(Long dsId) {
        dsIdAdapterMap.remove(dsId);
        log.info("数据源适配器缓存已清除: dsId={}", dsId);
    }
}
