package com.bagdatahouse.governance.metadata.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovMetadata;
import com.bagdatahouse.core.entity.GovMetadataColumn;
import com.bagdatahouse.governance.metadata.dto.MetadataScanRequest;
import com.bagdatahouse.governance.metadata.dto.MetadataScanResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 元数据管理服务接口
 */
public interface MetadataService {

    /**
     * 扫描元数据（同步）
     * @param request 扫描请求
     * @return 扫描结果
     */
    Result<MetadataScanResult> scan(MetadataScanRequest request);

    /**
     * 扫描元数据（异步）
     * @param request 扫描请求
     * @return 任务ID
     */
    Result<String> scanAsync(MetadataScanRequest request);

    /**
     * 获取异步扫描进度
     * @param taskId 任务ID
     * @return 扫描结果（如果完成）或进度信息
     */
    Result<MetadataScanResult> getScanProgress(String taskId);

    /**
     * 取消异步扫描任务
     * @param taskId 任务ID
     */
    Result<Void> cancelScan(String taskId);

    /**
     * 分页查询元数据
     */
    Result<Page<GovMetadata>> page(Integer pageNum, Integer pageSize, Long dsId, String dataLayer,
                                    String dataDomain, String tableName, String status);

    /**
     * 根据ID查询
     */
    Result<GovMetadata> getById(Long id);

    /**
     * 根据数据源ID和表名查询
     */
    Result<GovMetadata> getByDsIdAndTable(Long dsId, String tableName);

    /**
     * 获取表的字段列表
     */
    Result<List<GovMetadataColumn>> getColumns(Long metadataId);

    /**
     * 更新元数据
     */
    Result<Void> updateMetadata(Long id, GovMetadata metadata);

    /**
     * 更新字段信息
     */
    Result<Void> updateColumn(Long id, GovMetadataColumn column);

    /**
     * 删除元数据（包含字段）
     */
    Result<Void> deleteMetadata(Long id);

    /**
     * 批量删除元数据
     */
    Result<Void> batchDelete(List<Long> ids);

    /**
     * 同步元数据统计信息（行数等）
     */
    Result<Void> syncStats(Long dsId, List<String> tableNames);

    /**
     * 获取元数据统计
     */
    Result<MetadataStats> getStats();

    /**
     * 元数据统计
     */
    record MetadataStats(
            long totalTables,
            long totalColumns,
            long dsCount,
            long activeTables,
            java.util.Map<String, Long> byDataLayer,
            java.util.Map<String, Long> byDsType
    ) {}
}
