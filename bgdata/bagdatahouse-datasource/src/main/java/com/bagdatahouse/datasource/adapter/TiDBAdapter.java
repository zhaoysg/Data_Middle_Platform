package com.bagdatahouse.datasource.adapter;

import com.bagdatahouse.datasource.manager.DynamicDataSourceManager;
import lombok.extern.slf4j.Slf4j;

/**
 * TiDB 数据源适配器
 * TiDB 是 MySQL 兼容的分布式 NewSQL 数据库，直接复用 MySQL 适配器
 * 注意：不使用 @Component，由 DataSourceAdapterRegistry 手动实例化以避免 Bean 类型冲突
 */
@Slf4j
public class TiDBAdapter extends MySQLAdapter {

    public TiDBAdapter(DynamicDataSourceManager dataSourceManager) {
        super(dataSourceManager);
    }

    @Override
    public String getDataSourceType() {
        return "TIDB";
    }
}
