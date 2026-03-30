package com.bagdatahouse.governance.glossary.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovGlossaryMapping;
import com.bagdatahouse.governance.glossary.dto.GlossaryMappingDTO;
import com.bagdatahouse.governance.glossary.vo.GlossaryMappingVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 术语-字段映射服务接口
 */
public interface GlossaryMappingService {

    /**
     * 新增映射
     */
    Result<GovGlossaryMapping> save(GlossaryMappingDTO dto);

    /**
     * 批量新增映射
     */
    Result<Void> batchSave(Long termId, List<GlossaryMappingDTO> mappings, Long createUser);

    /**
     * 更新映射
     */
    Result<Void> update(Long id, GlossaryMappingDTO dto);

    /**
     * 删除映射
     */
    Result<Void> delete(Long id);

    /**
     * 批量删除映射
     */
    Result<Void> batchDelete(List<Long> ids);

    /**
     * 审批映射
     */
    Result<Void> approve(Long id, Long approvedBy);

    /**
     * 驳回映射
     */
    Result<Void> reject(Long id, Long approvedBy, String rejectReason);

    /**
     * 根据术语ID查询所有映射
     */
    Result<List<GlossaryMappingVO>> getByTermId(Long termId);

    /**
     * 根据元数据字段查询关联的术语
     */
    Result<List<GlossaryMappingVO>> getByMetadataField(Long dsId, String tableName, String columnName);

    /**
     * 分页查询映射（支持按术语名、数据源、状态筛选）
     */
    Result<Page<GlossaryMappingVO>> pagePending(Integer pageNum, Integer pageSize, String termName, Long dsId, String status);

    /**
     * 查询所有映射列表（支持按术语名、数据源筛选，不分页）
     */
    Result<List<GlossaryMappingVO>> listPending(String termName, Long dsId);

    /**
     * 根据数据源查询已审批映射
     */
    Result<List<GlossaryMappingVO>> getApprovedByDatasource(Long dsId);

    /**
     * 根据数据源和表查询已审批映射
     */
    Result<List<GlossaryMappingVO>> getApprovedByTable(Long dsId, String tableName);
}
