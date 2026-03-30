package com.bagdatahouse.dqc.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqDatasourceDTO;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.datasource.vo.ConnectionTestResultVO;
import com.bagdatahouse.dqc.dto.PreviewSelectRequest;
import com.bagdatahouse.dqc.vo.DatasourceTableColumnVO;
import com.bagdatahouse.dqc.vo.SqlPreviewResultVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * DQ数据源服务接口
 */
public interface DqDatasourceService {

    /**
     * 分页查询
     */
    Result<Page<DqDatasource>> page(Integer pageNum, Integer pageSize, String dsName, String dsType, String dataLayer, Integer status);

    /**
     * 根据ID查询
     */
    Result<DqDatasource> getById(Long id);

    /**
     * 根据编码查询
     */
    Result<DqDatasource> getByCode(String dsCode);

    /**
     * 新增
     */
    Result<Long> create(DqDatasourceDTO dto);

    /**
     * 更新
     */
    Result<Void> update(Long id, DqDatasourceDTO dto);

    /**
     * 删除
     */
    Result<Void> delete(Long id);

    /**
     * 测试数据源连接（兼容旧接口）
     */
    Result<Boolean> testConnection(DqDatasourceDTO dto);

    /**
     * 测试数据源连接（返回详细信息）
     */
    Result<ConnectionTestResultVO> testConnectionDetail(DqDatasourceDTO dto);

    /**
     * 测试已有数据源连接
     */
    Result<ConnectionTestResultVO> testConnectionById(Long id);

    /**
     * 启用数据源
     */
    Result<Void> enable(Long id);

    /**
     * 禁用数据源
     */
    Result<Void> disable(Long id);

    /**
     * 按类型查询数据源
     */
    Result<List<DqDatasource>> listByType(String dsType);

    /**
     * 按数据层查询数据源
     */
    Result<List<DqDatasource>> listByLayer(String layerCode);

    /**
     * 获取已启用的数据源
     */
    Result<List<DqDatasource>> listEnabled();

    /**
     * 获取数据源统计
     */
    Result<Map<String, Object>> getStatistics();

    /**
     * 获取数据源下的表列表
     * @param id     数据源ID
     * @param schema schema 名称（PostgreSQL 专用，可为空）
     */
    Result<List<String>> getTables(Long id, String schema);

    /**
     * 获取数据源下指定表的列信息（直连数据库，不依赖元数据扫描）
     * @param id         数据源ID
     * @param tableName  表名
     * @param schema     schema 名称（PostgreSQL 专用，可为空）
     */
    Result<List<DatasourceTableColumnVO>> getTableColumns(Long id, String tableName, String schema);

    /**
     * 获取数据源下的 schema 列表（PostgreSQL 专用；其他类型返回空列表）
     */
    Result<List<String>> getSchemas(Long id);

    /**
     * 在指定数据源上执行只读 SELECT 预览（行数与超时受配置限制，供规则表达式调试）
     */
    Result<SqlPreviewResultVO> previewSelect(Long dsId, PreviewSelectRequest request);
}
