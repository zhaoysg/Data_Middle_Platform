package com.bagdatahouse.datasource.service.impl;

import com.alibaba.druid.pool.DruidDataSource;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.core.dto.DqDatasourceDTO;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.datasource.enums.DataSourceTypeEnum;
import com.bagdatahouse.datasource.service.DataSourceConnectionService;
import com.bagdatahouse.datasource.vo.ConnectionTestResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源连接测试服务实现
 */
@Slf4j
@Service
public class DataSourceConnectionServiceImpl implements DataSourceConnectionService {

    private final DqDatasourceMapper datasourceMapper;

    private static final ConcurrentHashMap<Long, DruidDataSource> TEMP_DATASOURCES = new ConcurrentHashMap<>();

    public DataSourceConnectionServiceImpl(DqDatasourceMapper datasourceMapper) {
        this.datasourceMapper = datasourceMapper;
    }

    @Override
    public ConnectionTestResultVO testConnection(DqDatasourceDTO dto) {
        if (dto.getId() != null) {
            return testConnectionById(dto.getId());
        }
        return doTestConnection(dto.getHost(), dto.getPort(), dto.getDatabaseName(),
                dto.getUsername(), dto.getPassword(), dto.getDsType(), dto.getSchemaName());
    }

    @Override
    public ConnectionTestResultVO testConnectionById(Long dsId) {
        DqDatasource datasource = datasourceMapper.selectById(dsId);
        if (datasource == null) {
            throw new BusinessException(2001, "数据源不存在");
        }
        return doTestConnection(datasource.getHost(), datasource.getPort(),
                datasource.getDatabaseName(), datasource.getUsername(),
                datasource.getPassword(), datasource.getDsType(), datasource.getSchemaName());
    }

    @Override
    public ConnectionTestResultVO testConnection(String host, Integer port,
            String databaseName, String username, String password, String dsType, String schemaName) {
        return doTestConnection(host, port, databaseName, username, password, dsType, schemaName);
    }

    private ConnectionTestResultVO doTestConnection(String host, Integer port,
            String databaseName, String username, String password, String dsType, String schemaName) {
        DataSourceTypeEnum typeEnum = DataSourceTypeEnum.fromCode(dsType);
        if (typeEnum == null) {
            throw new BusinessException(400, "不支持的数据源类型: " + dsType);
        }

        int targetPort = port != null ? port : typeEnum.getDefaultPort();
        String jdbcUrl = buildJdbcUrl(typeEnum, host, targetPort, databaseName, dsType, schemaName);

        long tempKey = System.nanoTime();
        DruidDataSource ds = null;
        long startMs = System.currentTimeMillis();

        try {
            ds = new DruidDataSource();
            ds.setDriverClassName(typeEnum.getDriverClass());
            ds.setUrl(jdbcUrl);
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setInitialSize(1);
            ds.setMinIdle(1);
            ds.setMaxActive(3);
            ds.setMaxWait(10000L);
            ds.setTimeBetweenEvictionRunsMillis(60000L);
            ds.setMinEvictableIdleTimeMillis(300000L);
            ds.setValidationQuery(getValidationQuery(dsType));
            ds.setTestWhileIdle(true);
            ds.setTestOnBorrow(false);
            ds.setTestOnReturn(false);
            ds.init();

            TEMP_DATASOURCES.put(tempKey, ds);

            ds.getConnection().createStatement().execute(getValidationQuery(dsType));

            long elapsedMs = System.currentTimeMillis() - startMs;
            String version = getDatabaseVersion(ds, dsType);

            return ConnectionTestResultVO.builder()
                    .success(true)
                    .message("连接成功")
                    .databaseVersion(version)
                    .elapsedMs(elapsedMs)
                    .testTime(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startMs;
            log.error("连接测试失败: host={}, type={}", host, dsType, e);
            return ConnectionTestResultVO.builder()
                    .success(false)
                    .message("连接失败: " + e.getMessage())
                    .elapsedMs(elapsedMs)
                    .testTime(LocalDateTime.now())
                    .build();
        } finally {
            TEMP_DATASOURCES.remove(tempKey);
            if (ds != null) {
                ds.close();
            }
        }
    }

    private String buildJdbcUrl(DataSourceTypeEnum typeEnum, String host, int port,
            String dbName, String dsType, String schemaName) {
        String baseUrl = String.format(typeEnum.getUrlTemplate(), host, port, dbName);
        if ("POSTGRESQL".equalsIgnoreCase(dsType)
                && schemaName != null && !schemaName.isBlank()) {
            baseUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?")
                    + "currentSchema=" + schemaName.trim();
        }
        return baseUrl;
    }

    private String getValidationQuery(String dsType) {
        return switch (dsType.toUpperCase()) {
            case "ORACLE" -> "SELECT 1 FROM DUAL";
            default -> "SELECT 1";
        };
    }

    private String getDatabaseVersion(DruidDataSource ds, String dsType) {
        try {
            java.sql.Connection conn = ds.getConnection();
            try {
                return conn.getMetaData().getDatabaseProductVersion();
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.debug("获取数据库版本失败: {}", e.getMessage());
            return null;
        }
    }

    public void closeTempDataSource(Long key) {
        DruidDataSource ds = TEMP_DATASOURCES.remove(key);
        if (ds != null) {
            ds.close();
        }
    }
}
