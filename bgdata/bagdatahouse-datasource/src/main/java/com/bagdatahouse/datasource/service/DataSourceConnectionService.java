package com.bagdatahouse.datasource.service;

import com.bagdatahouse.core.dto.DqDatasourceDTO;
import com.bagdatahouse.datasource.vo.ConnectionTestResultVO;

/**
 * 数据源连接测试服务接口
 */
public interface DataSourceConnectionService {

    /**
     * 测试新数据源连接（未持久化）
     */
    ConnectionTestResultVO testConnection(DqDatasourceDTO dto);

    /**
     * 测试已有数据源连接
     */
    ConnectionTestResultVO testConnectionById(Long dsId);

    /**
     * 测试连接（使用临时数据源配置）
     */
    ConnectionTestResultVO testConnection(String host, Integer port, String databaseName,
            String username, String password, String dsType, String schemaName);
}
