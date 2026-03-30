package com.bagdatahouse.governance.asset.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovAssetCatalog;
import com.bagdatahouse.governance.asset.dto.AssetCatalogQueryDTO;
import com.bagdatahouse.governance.asset.dto.AssetCatalogSaveDTO;
import com.bagdatahouse.governance.asset.vo.AssetCatalogDetailVO;
import com.bagdatahouse.governance.asset.vo.AssetCatalogStatsVO;
import com.bagdatahouse.governance.asset.vo.AssetCatalogTreeVO;
import com.bagdatahouse.governance.asset.vo.CatalogAssetVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 资产目录管理服务接口
 */
public interface AssetCatalogService {

    /**
     * 新增或更新目录
     */
    Result<GovAssetCatalog> save(AssetCatalogSaveDTO dto);

    /**
     * 批量新增子目录（用于一次性创建完整的目录结构）
     */
    Result<Void> batchSave(List<AssetCatalogSaveDTO> dtoList);

    /**
     * 更新目录
     */
    Result<Void> update(Long id, AssetCatalogSaveDTO dto);

    /**
     * 删除目录（级联删除子目录和资产关联）
     */
    Result<Void> delete(Long id);

    /**
     * 批量删除目录
     */
    Result<Void> batchDelete(List<Long> ids);

    /**
     * 根据ID查询目录
     */
    Result<GovAssetCatalog> getById(Long id);

    /**
     * 分页查询目录
     */
    Result<Page<GovAssetCatalog>> page(Integer pageNum, Integer pageSize, AssetCatalogQueryDTO queryDTO);

    /**
     * 获取目录详情（含资产列表、子目录）
     */
    Result<AssetCatalogDetailVO> getDetail(Long id);

    /**
     * 获取目录树（左侧树形结构）
     * 按 catalogType 分组，支持展开/收起
     */
    Result<List<AssetCatalogTreeVO>> getTree(AssetCatalogQueryDTO queryDTO);

    /**
     * 获取完整的目录树（全部展开）
     */
    Result<List<AssetCatalogTreeVO>> getFullTree();

    /**
     * 向目录收录资产
     */
    Result<Void> addAssets(Long catalogId, List<Long> metadataIds);

    /**
     * 从目录移除资产
     */
    Result<Void> removeAssets(Long catalogId, List<Long> metadataIds);

    /**
     * 获取目录下收录的资产列表
     */
    Result<Page<CatalogAssetVO>> getAssets(Long catalogId, Integer pageNum, Integer pageSize);

    /**
     * 获取资产所属的所有目录
     */
    Result<List<AssetCatalogTreeVO>> getCatalogsByMetadata(Long metadataId);

    /**
     * 获取统计信息
     */
    Result<AssetCatalogStatsVO> getStats();

    /**
     * 刷新目录资产数量（定时任务或手动触发）
     */
    Result<Void> refreshItemCount(Long catalogId);

    /**
     * 增加访问次数
     */
    Result<Void> incrementAccessCount(Long catalogId);

    /**
     * 移动目录（变更父目录）
     */
    Result<Void> move(Long id, Long newParentId);
}
