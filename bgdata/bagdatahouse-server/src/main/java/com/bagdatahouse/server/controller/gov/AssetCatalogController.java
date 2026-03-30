package com.bagdatahouse.server.controller.gov;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovAssetCatalog;
import com.bagdatahouse.governance.asset.dto.AssetCatalogQueryDTO;
import com.bagdatahouse.governance.asset.dto.AssetCatalogSaveDTO;
import com.bagdatahouse.governance.asset.service.AssetCatalogService;
import com.bagdatahouse.governance.asset.vo.AssetCatalogDetailVO;
import com.bagdatahouse.governance.asset.vo.AssetCatalogStatsVO;
import com.bagdatahouse.governance.asset.vo.AssetCatalogTreeVO;
import com.bagdatahouse.governance.asset.vo.CatalogAssetVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据资产目录管理接口
 */
@Api(tags = "数据资产目录管理")
@RestController
@RequestMapping("/gov/asset-catalog")
public class AssetCatalogController {

    @Autowired
    private AssetCatalogService assetCatalogService;

    @ApiOperation("新增资产目录")
    @PostMapping
    public Result<GovAssetCatalog> save(@Validated @RequestBody AssetCatalogSaveDTO dto) {
        return assetCatalogService.save(dto);
    }

    @ApiOperation("批量新增资产目录")
    @PostMapping("/batch")
    public Result<Void> batchSave(@RequestBody List<AssetCatalogSaveDTO> dtoList) {
        return assetCatalogService.batchSave(dtoList);
    }

    @ApiOperation("更新资产目录")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody AssetCatalogSaveDTO dto) {
        return assetCatalogService.update(id, dto);
    }

    @ApiOperation("删除资产目录（级联删除子目录）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return assetCatalogService.delete(id);
    }

    @ApiOperation("批量删除资产目录")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return assetCatalogService.batchDelete(ids);
    }

    @ApiOperation("根据ID查询资产目录")
    @GetMapping("/{id}")
    public Result<GovAssetCatalog> getById(@PathVariable Long id) {
        return assetCatalogService.getById(id);
    }

    @ApiOperation("分页查询资产目录")
    @GetMapping("/page")
    public Result<Page<GovAssetCatalog>> page(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("目录类型：BUSINESS_DOMAIN/DATA_DOMAIN/ALBUM") @RequestParam(required = false) String catalogType,
            @ApiParam("目录名称（模糊匹配）") @RequestParam(required = false) String catalogName,
            @ApiParam("目录编码（精确匹配）") @RequestParam(required = false) String catalogCode,
            @ApiParam("可见性：0-私有，1-公开") @RequestParam(required = false) Integer visible,
            @ApiParam("状态：0-禁用，1-启用") @RequestParam(required = false) Integer status,
            @ApiParam("负责人用户ID") @RequestParam(required = false) Long ownerId,
            @ApiParam("部门ID") @RequestParam(required = false) Long deptId,
            @ApiParam("是否仅查询顶级目录") @RequestParam(required = false) Boolean topLevelOnly) {

        AssetCatalogQueryDTO queryDTO = AssetCatalogQueryDTO.builder()
                .catalogType(catalogType)
                .catalogName(catalogName)
                .catalogCode(catalogCode)
                .visible(visible)
                .status(status)
                .ownerId(ownerId)
                .deptId(deptId)
                .topLevelOnly(topLevelOnly)
                .build();

        return assetCatalogService.page(pageNum, pageSize, queryDTO);
    }

    @ApiOperation("获取资产目录详情（含资产列表、子目录）")
    @GetMapping("/detail/{id}")
    public Result<AssetCatalogDetailVO> getDetail(@PathVariable Long id) {
        return assetCatalogService.getDetail(id);
    }

    @ApiOperation("获取资产目录树（左侧树形结构）")
    @GetMapping("/tree")
    public Result<List<AssetCatalogTreeVO>> getTree(
            @ApiParam("目录类型：BUSINESS_DOMAIN/DATA_DOMAIN/ALBUM") @RequestParam(required = false) String catalogType,
            @ApiParam("目录名称（模糊匹配）") @RequestParam(required = false) String catalogName,
            @ApiParam("可见性：0-私有，1-公开") @RequestParam(required = false) Integer visible,
            @ApiParam("状态：0-禁用，1-启用") @RequestParam(required = false) Integer status,
            @ApiParam("部门ID") @RequestParam(required = false) Long deptId) {

        AssetCatalogQueryDTO queryDTO = AssetCatalogQueryDTO.builder()
                .catalogType(catalogType)
                .catalogName(catalogName)
                .visible(visible)
                .status(status)
                .deptId(deptId)
                .build();

        return assetCatalogService.getTree(queryDTO);
    }

    @ApiOperation("获取完整的资产目录树")
    @GetMapping("/tree/full")
    public Result<List<AssetCatalogTreeVO>> getFullTree() {
        return assetCatalogService.getFullTree();
    }

    @ApiOperation("向目录收录资产")
    @PostMapping("/{catalogId}/assets")
    public Result<Void> addAssets(
            @PathVariable Long catalogId,
            @ApiParam("元数据ID列表") @RequestBody List<Long> metadataIds) {
        return assetCatalogService.addAssets(catalogId, metadataIds);
    }

    @ApiOperation("从目录移除资产")
    @DeleteMapping("/{catalogId}/assets")
    public Result<Void> removeAssets(
            @PathVariable Long catalogId,
            @ApiParam("元数据ID列表") @RequestBody List<Long> metadataIds) {
        return assetCatalogService.removeAssets(catalogId, metadataIds);
    }

    @ApiOperation("获取目录下收录的资产列表")
    @GetMapping("/{catalogId}/assets")
    public Result<Page<CatalogAssetVO>> getAssets(
            @PathVariable Long catalogId,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        return assetCatalogService.getAssets(catalogId, pageNum, pageSize);
    }

    @ApiOperation("获取资产所属的所有目录")
    @GetMapping("/metadata/{metadataId}/catalogs")
    public Result<List<AssetCatalogTreeVO>> getCatalogsByMetadata(@PathVariable Long metadataId) {
        return assetCatalogService.getCatalogsByMetadata(metadataId);
    }

    @ApiOperation("获取资产目录统计信息")
    @GetMapping("/stats")
    public Result<AssetCatalogStatsVO> getStats() {
        return assetCatalogService.getStats();
    }

    @ApiOperation("刷新目录资产数量")
    @PostMapping("/{id}/refresh-count")
    public Result<Void> refreshItemCount(@PathVariable Long id) {
        return assetCatalogService.refreshItemCount(id);
    }

    @ApiOperation("移动目录（变更父目录）")
    @PutMapping("/{id}/move")
    public Result<Void> move(
            @PathVariable Long id,
            @ApiParam("新的父目录ID，传0表示移到根目录") @RequestParam(required = false, defaultValue = "0") Long parentId) {
        return assetCatalogService.move(id, parentId);
    }
}
