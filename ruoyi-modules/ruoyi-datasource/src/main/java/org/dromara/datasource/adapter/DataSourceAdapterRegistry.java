package org.dromara.datasource.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源适配器注册表
 * 统一管理所有数据源适配器，提供按类型和 dsId 查找功能
 *
 * @author Lion Li
 */
@Slf4j
@Component
public class DataSourceAdapterRegistry {

    private final org.dromara.datasource.manager.DynamicDataSourceManager dataSourceManager;

    /**
     * 数据源ID到适配器的动态注册映射
     */
    private final ConcurrentHashMap<Long, DataSourceAdapter> dsIdAdapterMap = new ConcurrentHashMap<>();

    public DataSourceAdapterRegistry(
            org.dromara.datasource.manager.DynamicDataSourceManager dataSourceManager,
            MySQLAdapter mysqlAdapter,
            PostgresAdapter postgresAdapter,
            SqlServerAdapter sqlServerAdapter,
            OracleAdapter oracleAdapter) {
        this.dataSourceManager = dataSourceManager;
        // TiDB 继承 MySQL，运行时通过 MySQLAdapter 代理
        log.info("数据源适配器注册表初始化完成");
    }

    /**
     * 获取指定 dsId 的适配器（不存在则创建）
     *
     * @param dsId       数据源ID
     * @param dsType     数据源类型
     * @param host       主机
     * @param port       端口
     * @param database  数据库名
     * @param schema    Schema名
     * @param username  用户名
     * @param password  密码
     * @param connectionParams 连接参数
     * @return 适配器实例
     */
    public DataSourceAdapter getOrCreateAdapter(Long dsId, String dsType,
                                               String host, Integer port,
                                               String database, String schema,
                                               String username, String password,
                                               String connectionParams) {
        DataSourceAdapter cached = dsIdAdapterMap.get(dsId);
        if (cached != null) {
            return cached;
        }
        synchronized (dsIdAdapterMap) {
            cached = dsIdAdapterMap.get(dsId);
            if (cached != null) {
                return cached;
            }

            if (!dataSourceManager.isRegistered(dsId)) {
                org.dromara.datasource.enums.DataSourceTypeEnum typeEnum =
                    org.dromara.datasource.enums.DataSourceTypeEnum.fromCode(dsType);
                if (typeEnum == null) {
                    throw new IllegalArgumentException("不支持的数据源类型: " + dsType);
                }

                dataSourceManager.registerDataSource(dsId, dsType, host, port, database, schema,
                    username, password, connectionParams,
                    typeEnum.getUrlTemplate(), typeEnum.getDriverClass());
            }

            DataSourceAdapter adapter = createAdapterInstance(dsType);
            adapter.setDsId(dsId);
            dsIdAdapterMap.put(dsId, adapter);
            log.debug("数据源适配器已创建: dsId={}, dsType={}", dsId, dsType);
            return adapter;
        }
    }

    /**
     * 获取已缓存的适配器
     *
     * @param dsId 数据源ID
     * @return 适配器，不存在返回 null
     */
    public DataSourceAdapter getAdapter(Long dsId) {
        return dsIdAdapterMap.get(dsId);
    }

    /**
     * 注销指定数据源ID的适配器
     *
     * @param dsId 数据源ID
     */
    public void unregisterAdapter(Long dsId) {
        dsIdAdapterMap.remove(dsId);
        dataSourceManager.unregisterDataSource(dsId);
        log.info("数据源适配器已注销: dsId={}", dsId);
    }

    /**
     * 为给定 dsType 创建独立适配器实例
     */
    private DataSourceAdapter createAdapterInstance(String dsType) {
        return switch (dsType.toUpperCase()) {
            case "MYSQL", "TIDB" -> new MySQLAdapter(dataSourceManager);
            case "POSTGRESQL" -> new PostgresAdapter(dataSourceManager);
            case "SQLSERVER" -> new SqlServerAdapter(dataSourceManager);
            case "ORACLE" -> new OracleAdapter(dataSourceManager);
            default -> throw new IllegalArgumentException("不支持的数据源类型: " + dsType);
        };
    }
}
