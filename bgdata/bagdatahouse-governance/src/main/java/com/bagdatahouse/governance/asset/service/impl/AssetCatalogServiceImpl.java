package com.bagdatahouse.governance.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.entity.GovAssetCatalog;
import com.bagdatahouse.core.entity.GovCatalogMetadata;
import com.bagdatahouse.core.entity.GovMetadata;
import com.bagdatahouse.core.entity.SysDept;
import com.bagdatahouse.core.entity.SysUser;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.mapper.GovAssetCatalogMapper;
import com.bagdatahouse.core.mapper.GovCatalogMetadataMapper;
import com.bagdatahouse.core.mapper.GovMetadataMapper;
import com.bagdatahouse.core.mapper.SysDeptMapper;
import com.bagdatahouse.core.mapper.SysUserMapper;
import com.bagdatahouse.governance.asset.dto.AssetCatalogQueryDTO;
import com.bagdatahouse.governance.asset.dto.AssetCatalogSaveDTO;
import com.bagdatahouse.governance.asset.enums.CatalogTypeEnum;
import com.bagdatahouse.governance.asset.service.AssetCatalogService;
import com.bagdatahouse.governance.asset.vo.AssetCatalogDetailVO;
import com.bagdatahouse.governance.asset.vo.AssetCatalogStatsVO;
import com.bagdatahouse.governance.asset.vo.AssetCatalogTreeVO;
import com.bagdatahouse.governance.asset.vo.CatalogAssetVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资产目录管理服务实现
 */
@Slf4j
@Service
public class AssetCatalogServiceImpl extends ServiceImpl<GovAssetCatalogMapper, GovAssetCatalog>
        implements AssetCatalogService {

    @Autowired
    private GovAssetCatalogMapper catalogMapper;

    @Autowired
    private GovCatalogMetadataMapper catalogMetadataMapper;

    @Autowired
    private GovMetadataMapper metadataMapper;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GovAssetCatalog> save(AssetCatalogSaveDTO dto) {
        validateDto(dto, null);

        GovAssetCatalog catalog = new GovAssetCatalog();
        BeanUtils.copyProperties(dto, catalog);
        catalog.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        catalog.setItemCount(0);
        catalog.setAccessCount(0L);
        catalog.setCreateTime(LocalDateTime.now());
        if (catalog.getSortOrder() == null) {
            catalog.setSortOrder(0);
        }
        if (catalog.getVisible() == null) {
            catalog.setVisible(1);
        }
        if (catalog.getStatus() == null) {
            catalog.setStatus("ACTIVE");
        }
        catalogMapper.insert(catalog);

        if (dto.getMetadataIds() != null && !dto.getMetadataIds().isEmpty()) {
            addAssetsInternal(catalog.getId(), dto.getMetadataIds(), dto.getCreateUser());
            refreshItemCountInternal(catalog.getId());
        }

        return Result.success(catalog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchSave(List<AssetCatalogSaveDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Result.success();
        }
        for (AssetCatalogSaveDTO dto : dtoList) {
            save(dto);
        }
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, AssetCatalogSaveDTO dto) {
        GovAssetCatalog existing = catalogMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "目录不存在");
        }

        validateDto(dto, id);

        GovAssetCatalog catalog = new GovAssetCatalog();
        BeanUtils.copyProperties(dto, catalog);
        catalog.setId(id);
        catalog.setUpdateTime(LocalDateTime.now());
        catalogMapper.updateById(catalog);

        if (dto.getMetadataIds() != null) {
            removeAllAssetsInternal(id);
            addAssetsInternal(id, dto.getMetadataIds(), existing.getCreateUser());
            refreshItemCountInternal(id);
        }

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        GovAssetCatalog existing = catalogMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "目录不存在");
        }

        List<Long> childIds = getAllDescendantIds(id);
        childIds.add(id);

        for (Long childId : childIds) {
            removeAllAssetsInternal(childId);
        }

        LambdaQueryWrapper<GovAssetCatalog> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GovAssetCatalog::getId, childIds);
        catalogMapper.delete(wrapper);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        for (Long id : ids) {
            delete(id);
        }
        return Result.success();
    }

    @Override
    public Result<GovAssetCatalog> getById(Long id) {
        GovAssetCatalog entity = catalogMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "目录不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<Page<GovAssetCatalog>> page(Integer pageNum, Integer pageSize, AssetCatalogQueryDTO queryDTO) {
        Page<GovAssetCatalog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GovAssetCatalog> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(GovAssetCatalog::getSortOrder)
               .orderByDesc(GovAssetCatalog::getCreateTime);
        Page<GovAssetCatalog> result = catalogMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<AssetCatalogDetailVO> getDetail(Long id) {
        GovAssetCatalog catalog = catalogMapper.selectById(id);
        if (catalog == null) {
            throw new BusinessException(2001, "目录不存在");
        }

        AssetCatalogDetailVO vo = buildDetailVO(catalog);
        incrementAccessCountInternal(id);

        return Result.success(vo);
    }

    @Override
    public Result<List<AssetCatalogTreeVO>> getTree(AssetCatalogQueryDTO queryDTO) {
        List<GovAssetCatalog> all = catalogMapper.selectList(buildQueryWrapper(queryDTO));

        if (all.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<Long, List<GovAssetCatalog>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(GovAssetCatalog::getParentId));

        Set<Long> rootIds = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .map(GovAssetCatalog::getId)
                .collect(Collectors.toSet());

        List<GovAssetCatalog> roots = all.stream()
                .filter(c -> (c.getParentId() == null || c.getParentId() == 0)
                        && (queryDTO == null || queryDTO.getCatalogType() == null
                            || queryDTO.getCatalogType().equals(c.getCatalogType())))
                .sorted((a, b) -> {
                    if (a.getSortOrder() == null) a.setSortOrder(0);
                    if (b.getSortOrder() == null) b.setSortOrder(0);
                    return a.getSortOrder().compareTo(b.getSortOrder());
                })
                .collect(Collectors.toList());

        List<AssetCatalogTreeVO> tree = new ArrayList<>();
        for (GovAssetCatalog root : roots) {
            tree.add(buildTreeNode(root, childrenMap, all));
        }

        return Result.success(tree);
    }

    @Override
    public Result<List<AssetCatalogTreeVO>> getFullTree() {
        List<GovAssetCatalog> all = catalogMapper.selectList(null);

        if (all.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<Long, List<GovAssetCatalog>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(GovAssetCatalog::getParentId));

        List<GovAssetCatalog> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .sorted((a, b) -> {
                    if (a.getSortOrder() == null) a.setSortOrder(0);
                    if (b.getSortOrder() == null) b.setSortOrder(0);
                    return a.getSortOrder().compareTo(b.getSortOrder());
                })
                .collect(Collectors.toList());

        List<AssetCatalogTreeVO> tree = new ArrayList<>();
        for (GovAssetCatalog root : roots) {
            tree.add(buildTreeNode(root, childrenMap, all));
        }

        return Result.success(tree);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addAssets(Long catalogId, List<Long> metadataIds) {
        GovAssetCatalog catalog = catalogMapper.selectById(catalogId);
        if (catalog == null) {
            throw new BusinessException(2001, "目录不存在");
        }
        addAssetsInternal(catalogId, metadataIds, catalog.getCreateUser());
        refreshItemCountInternal(catalogId);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> removeAssets(Long catalogId, List<Long> metadataIds) {
        if (metadataIds == null || metadataIds.isEmpty()) {
            return Result.success();
        }
        LambdaQueryWrapper<GovCatalogMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovCatalogMetadata::getCatalogId, catalogId)
               .in(GovCatalogMetadata::getMetadataId, metadataIds);
        catalogMetadataMapper.delete(wrapper);
        refreshItemCountInternal(catalogId);
        return Result.success();
    }

    @Override
    public Result<Page<CatalogAssetVO>> getAssets(Long catalogId, Integer pageNum, Integer pageSize) {
        GovAssetCatalog catalog = catalogMapper.selectById(catalogId);
        if (catalog == null) {
            throw new BusinessException(2001, "目录不存在");
        }

        Page<GovCatalogMetadata> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GovCatalogMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovCatalogMetadata::getCatalogId, catalogId)
               .orderByDesc(GovCatalogMetadata::getSortOrder)
               .orderByDesc(GovCatalogMetadata::getCreateTime);
        Page<GovCatalogMetadata> pageResult = catalogMetadataMapper.selectPage(page, wrapper);

        List<Long> metadataIds = pageResult.getRecords().stream()
                .map(GovCatalogMetadata::getMetadataId)
                .collect(Collectors.toList());

        Map<Long, DqDatasource> dsMapTemp = new HashMap<>();
        if (!metadataIds.isEmpty()) {
            List<GovMetadata> metadatas = metadataMapper.selectBatchIds(metadataIds);
            Set<Long> dsIds = metadatas.stream()
                    .map(GovMetadata::getDsId)
                    .filter(d -> d != null)
                    .collect(Collectors.toSet());
            if (!dsIds.isEmpty()) {
                List<DqDatasource> datasources = datasourceMapper.selectBatchIds(dsIds);
                dsMapTemp = datasources.stream().collect(Collectors.toMap(DqDatasource::getId, d -> d));
            }
        }
        final Map<Long, DqDatasource> dsMap = dsMapTemp;

        Page<CatalogAssetVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        List<CatalogAssetVO> vos = pageResult.getRecords().stream().map(cm -> {
            GovMetadata meta = metadataMapper.selectById(cm.getMetadataId());
            return buildCatalogAssetVO(meta, cm, dsMap);
        }).collect(Collectors.toList());

        voPage.setRecords(vos);
        return Result.success(voPage);
    }

    @Override
    public Result<List<AssetCatalogTreeVO>> getCatalogsByMetadata(Long metadataId) {
        LambdaQueryWrapper<GovCatalogMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovCatalogMetadata::getMetadataId, metadataId);
        List<GovCatalogMetadata> relations = catalogMetadataMapper.selectList(wrapper);

        if (relations.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<Long> catalogIds = relations.stream()
                .map(GovCatalogMetadata::getCatalogId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<GovAssetCatalog> catalogWrapper = new LambdaQueryWrapper<>();
        catalogWrapper.in(GovAssetCatalog::getId, catalogIds);
        List<GovAssetCatalog> catalogs = catalogMapper.selectList(catalogWrapper);

        return Result.success(catalogs.stream()
                .map(this::buildTreeNodeFromEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public Result<AssetCatalogStatsVO> getStats() {
        long total = catalogMapper.selectCount(null);

        LambdaQueryWrapper<GovAssetCatalog> bizWrapper = new LambdaQueryWrapper<>();
        bizWrapper.eq(GovAssetCatalog::getCatalogType, CatalogTypeEnum.BUSINESS_DOMAIN.getCode());
        long bizCount = catalogMapper.selectCount(bizWrapper);

        LambdaQueryWrapper<GovAssetCatalog> domainWrapper = new LambdaQueryWrapper<>();
        domainWrapper.eq(GovAssetCatalog::getCatalogType, CatalogTypeEnum.DATA_DOMAIN.getCode());
        long domainCount = catalogMapper.selectCount(domainWrapper);

        LambdaQueryWrapper<GovAssetCatalog> albumWrapper = new LambdaQueryWrapper<>();
        albumWrapper.eq(GovAssetCatalog::getCatalogType, CatalogTypeEnum.ALBUM.getCode());
        long albumCount = catalogMapper.selectCount(albumWrapper);

        long totalAssets = catalogMetadataMapper.selectCount(null);

        LambdaQueryWrapper<GovAssetCatalog> allWrapper = new LambdaQueryWrapper<>();
        List<GovAssetCatalog> allCatalogs = catalogMapper.selectList(allWrapper);

        Map<String, Long> assetByType = new HashMap<>();
        assetByType.put(CatalogTypeEnum.BUSINESS_DOMAIN.getLabel(), 0L);
        assetByType.put(CatalogTypeEnum.DATA_DOMAIN.getLabel(), 0L);
        assetByType.put(CatalogTypeEnum.ALBUM.getLabel(), 0L);
        for (GovAssetCatalog c : allCatalogs) {
            if (c.getItemCount() != null && c.getItemCount() > 0) {
                String label = CatalogTypeEnum.getLabel(c.getCatalogType());
                assetByType.merge(label, (long) c.getItemCount(), Long::sum);
            }
        }

        Set<Long> allMetadataIds = catalogMetadataMapper.selectList(null).stream()
                .map(GovCatalogMetadata::getMetadataId)
                .collect(Collectors.toSet());
        Map<String, Long> assetByLayer = new HashMap<>();
        if (!allMetadataIds.isEmpty()) {
            List<GovMetadata> metas = metadataMapper.selectBatchIds(allMetadataIds);
            for (GovMetadata m : metas) {
                if (StringUtils.hasText(m.getDataLayer())) {
                    assetByLayer.merge(m.getDataLayer(), 1L, Long::sum);
                }
            }
        }

        AssetCatalogStatsVO stats = AssetCatalogStatsVO.builder()
                .totalCount(total)
                .businessDomainCount(bizCount)
                .dataDomainCount(domainCount)
                .albumCount(albumCount)
                .totalAssetCount(totalAssets)
                .assetCountByType(assetByType)
                .assetCountByLayer(assetByLayer)
                .build();

        return Result.success(stats);
    }

    @Override
    public Result<Void> refreshItemCount(Long catalogId) {
        refreshItemCountInternal(catalogId);
        return Result.success();
    }

    @Override
    public Result<Void> incrementAccessCount(Long catalogId) {
        incrementAccessCountInternal(catalogId);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> move(Long id, Long newParentId) {
        GovAssetCatalog catalog = catalogMapper.selectById(id);
        if (catalog == null) {
            throw new BusinessException(2001, "目录不存在");
        }
        if (newParentId != null && newParentId > 0) {
            GovAssetCatalog parent = catalogMapper.selectById(newParentId);
            if (parent == null) {
                throw new BusinessException(2001, "目标父目录不存在");
            }
            if (newParentId.equals(id)) {
                throw new BusinessException(2001, "不能将目录移动到自身下");
            }
            if (isDescendant(id, newParentId)) {
                throw new BusinessException(2001, "不能将目录移动到其子目录下");
            }
        }

        GovAssetCatalog update = new GovAssetCatalog();
        update.setId(id);
        update.setParentId(newParentId == null ? 0L : newParentId);
        update.setUpdateTime(LocalDateTime.now());
        catalogMapper.updateById(update);

        return Result.success();
    }

    // ====== private helper methods ======

    private LambdaQueryWrapper<GovAssetCatalog> buildQueryWrapper(AssetCatalogQueryDTO queryDTO) {
        LambdaQueryWrapper<GovAssetCatalog> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO == null) {
            return wrapper;
        }

        if (StringUtils.hasText(queryDTO.getCatalogType())) {
            wrapper.eq(GovAssetCatalog::getCatalogType, queryDTO.getCatalogType());
        }
        if (StringUtils.hasText(queryDTO.getCatalogName())) {
            wrapper.like(GovAssetCatalog::getCatalogName, queryDTO.getCatalogName());
        }
        if (StringUtils.hasText(queryDTO.getCatalogCode())) {
            wrapper.eq(GovAssetCatalog::getCatalogCode, queryDTO.getCatalogCode());
        }
        if (queryDTO.getVisible() != null) {
            wrapper.eq(GovAssetCatalog::getVisible, queryDTO.getVisible());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(GovAssetCatalog::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getOwnerId() != null) {
            wrapper.eq(GovAssetCatalog::getOwnerId, queryDTO.getOwnerId());
        }
        if (queryDTO.getDeptId() != null) {
            wrapper.eq(GovAssetCatalog::getDeptId, queryDTO.getDeptId());
        }
        if (Boolean.TRUE.equals(queryDTO.getTopLevelOnly())) {
            wrapper.and(w -> w.eq(GovAssetCatalog::getParentId, 0L).or().isNull(GovAssetCatalog::getParentId));
        }

        return wrapper;
    }

    private void validateDto(AssetCatalogSaveDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getCatalogType())) {
            throw new BusinessException(2001, "目录类型不能为空");
        }
        if (!StringUtils.hasText(dto.getCatalogName())) {
            throw new BusinessException(2001, "目录名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCatalogCode())) {
            throw new BusinessException(2001, "目录编码不能为空");
        }

        LambdaQueryWrapper<GovAssetCatalog> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(GovAssetCatalog::getCatalogCode, dto.getCatalogCode());
        if (excludeId != null) {
            codeWrapper.ne(GovAssetCatalog::getId, excludeId);
        }
        if (catalogMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException(2001, "目录编码已存在");
        }

        boolean validType = CatalogTypeEnum.BUSINESS_DOMAIN.getCode().equals(dto.getCatalogType())
                || CatalogTypeEnum.DATA_DOMAIN.getCode().equals(dto.getCatalogType())
                || CatalogTypeEnum.ALBUM.getCode().equals(dto.getCatalogType());
        if (!validType) {
            throw new BusinessException(2001, "目录类型无效，仅支持 BUSINESS_DOMAIN / DATA_DOMAIN / ALBUM");
        }

        if (CatalogTypeEnum.DATA_DOMAIN.getCode().equals(dto.getCatalogType())) {
            if (dto.getParentId() == null || dto.getParentId() == 0) {
                throw new BusinessException(2001, "数据域必须挂靠在业务域下");
            }
            GovAssetCatalog parent = catalogMapper.selectById(dto.getParentId());
            if (parent == null || !CatalogTypeEnum.BUSINESS_DOMAIN.getCode().equals(parent.getCatalogType())) {
                throw new BusinessException(2001, "数据域的父目录必须为业务域");
            }
        }

        if (CatalogTypeEnum.ALBUM.getCode().equals(dto.getCatalogType())) {
            if (dto.getParentId() != null && dto.getParentId() > 0) {
                GovAssetCatalog parent = catalogMapper.selectById(dto.getParentId());
                if (parent != null && CatalogTypeEnum.DATA_DOMAIN.getCode().equals(parent.getCatalogType())) {
                    // 专辑可以在数据域下
                }
            }
        }
    }

    private void addAssetsInternal(Long catalogId, List<Long> metadataIds, Long createUser) {
        if (metadataIds == null || metadataIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<GovCatalogMetadata> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(GovCatalogMetadata::getCatalogId, catalogId)
                    .in(GovCatalogMetadata::getMetadataId, metadataIds);
        List<GovCatalogMetadata> existing = catalogMetadataMapper.selectList(existWrapper);
        Set<Long> existingIds = existing.stream()
                .map(GovCatalogMetadata::getMetadataId)
                .collect(Collectors.toSet());

        int sortOrder = 0;
        for (Long metadataId : metadataIds) {
            if (!existingIds.contains(metadataId)) {
                GovCatalogMetadata relation = GovCatalogMetadata.builder()
                        .catalogId(catalogId)
                        .metadataId(metadataId)
                        .sortOrder(sortOrder++)
                        .createUser(createUser)
                        .createTime(LocalDateTime.now())
                        .build();
                catalogMetadataMapper.insert(relation);
            }
        }
    }

    private void removeAllAssetsInternal(Long catalogId) {
        LambdaQueryWrapper<GovCatalogMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovCatalogMetadata::getCatalogId, catalogId);
        catalogMetadataMapper.delete(wrapper);
    }

    private void refreshItemCountInternal(Long catalogId) {
        LambdaQueryWrapper<GovCatalogMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovCatalogMetadata::getCatalogId, catalogId);
        long count = catalogMetadataMapper.selectCount(wrapper);

        GovAssetCatalog update = new GovAssetCatalog();
        update.setId(catalogId);
        update.setItemCount((int) count);
        update.setUpdateTime(LocalDateTime.now());
        catalogMapper.updateById(update);
    }

    private void incrementAccessCountInternal(Long catalogId) {
        GovAssetCatalog catalog = catalogMapper.selectById(catalogId);
        if (catalog == null) return;

        GovAssetCatalog update = new GovAssetCatalog();
        update.setId(catalogId);
        update.setAccessCount((catalog.getAccessCount() == null ? 0L : catalog.getAccessCount()) + 1);
        catalogMapper.updateById(update);
    }

    private List<Long> getAllDescendantIds(Long parentId) {
        List<Long> descendants = new ArrayList<>();
        collectDescendants(parentId, descendants);
        return descendants;
    }

    private void collectDescendants(Long parentId, List<Long> descendants) {
        LambdaQueryWrapper<GovAssetCatalog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovAssetCatalog::getParentId, parentId);
        List<GovAssetCatalog> children = catalogMapper.selectList(wrapper);
        for (GovAssetCatalog child : children) {
            descendants.add(child.getId());
            collectDescendants(child.getId(), descendants);
        }
    }

    private boolean isDescendant(Long ancestorId, Long potentialDescendantId) {
        Long current = potentialDescendantId;
        while (current != null && current != 0) {
            GovAssetCatalog parent = catalogMapper.selectById(current);
            if (parent == null) break;
            if (parent.getId().equals(ancestorId)) {
                return true;
            }
            current = parent.getParentId();
        }
        return false;
    }

    private AssetCatalogTreeVO buildTreeNode(GovAssetCatalog catalog,
                                              Map<Long, List<GovAssetCatalog>> childrenMap,
                                              List<GovAssetCatalog> all) {
        AssetCatalogTreeVO.AssetCatalogTreeVOBuilder builder = AssetCatalogTreeVO.builder()
                .id(catalog.getId())
                .parentId(catalog.getParentId())
                .catalogType(catalog.getCatalogType())
                .catalogTypeLabel(CatalogTypeEnum.getLabel(catalog.getCatalogType()))
                .catalogName(catalog.getCatalogName())
                .catalogCode(catalog.getCatalogCode())
                .catalogDesc(catalog.getCatalogDesc())
                .coverImage(catalog.getCoverImage())
                .itemCount(catalog.getItemCount())
                .accessCount(catalog.getAccessCount())
                .ownerId(catalog.getOwnerId())
                .sortOrder(catalog.getSortOrder())
                .visible(catalog.getVisible())
                .visibleLabel(catalog.getVisible() != null && catalog.getVisible() == 1 ? "公开" : "私有")
                .status(catalog.getStatus())
                .statusLabel(catalog.getStatus() != null && "ACTIVE".equals(catalog.getStatus()) ? "启用" : "禁用")
                .createUser(catalog.getCreateUser())
                .createTime(catalog.getCreateTime());

        if (catalog.getOwnerId() != null) {
            SysUser owner = userMapper.selectById(catalog.getOwnerId());
            if (owner != null) {
                builder.ownerName(owner.getNickname() != null ? owner.getNickname() : owner.getUsername());
            }
        }

        List<GovAssetCatalog> children = childrenMap.get(catalog.getId());
        if (children != null && !children.isEmpty()) {
            List<GovAssetCatalog> sortedChildren = children.stream()
                    .sorted((a, b) -> {
                        if (a.getSortOrder() == null) a.setSortOrder(0);
                        if (b.getSortOrder() == null) b.setSortOrder(0);
                        return a.getSortOrder().compareTo(b.getSortOrder());
                    })
                    .collect(Collectors.toList());

            List<AssetCatalogTreeVO> childNodes = sortedChildren.stream()
                    .map(child -> buildTreeNode(child, childrenMap, all))
                    .collect(Collectors.toList());
            builder.children(childNodes);
        } else {
            builder.children(new ArrayList<>());
        }

        return builder.build();
    }

    private AssetCatalogTreeVO buildTreeNodeFromEntity(GovAssetCatalog catalog) {
        return AssetCatalogTreeVO.builder()
                .id(catalog.getId())
                .parentId(catalog.getParentId())
                .catalogType(catalog.getCatalogType())
                .catalogTypeLabel(CatalogTypeEnum.getLabel(catalog.getCatalogType()))
                .catalogName(catalog.getCatalogName())
                .catalogCode(catalog.getCatalogCode())
                .catalogDesc(catalog.getCatalogDesc())
                .itemCount(catalog.getItemCount())
                .accessCount(catalog.getAccessCount())
                .ownerId(catalog.getOwnerId())
                .visible(catalog.getVisible())
                .visibleLabel(catalog.getVisible() != null && catalog.getVisible() == 1 ? "公开" : "私有")
                .status(catalog.getStatus())
                .statusLabel(catalog.getStatus() != null && "ACTIVE".equals(catalog.getStatus()) ? "启用" : "禁用")
                .createUser(catalog.getCreateUser())
                .createTime(catalog.getCreateTime())
                .children(new ArrayList<>())
                .build();
    }

    private AssetCatalogDetailVO buildDetailVO(GovAssetCatalog catalog) {
        AssetCatalogDetailVO.AssetCatalogDetailVOBuilder builder = AssetCatalogDetailVO.builder()
                .id(catalog.getId())
                .parentId(catalog.getParentId())
                .catalogType(catalog.getCatalogType())
                .catalogTypeLabel(CatalogTypeEnum.getLabel(catalog.getCatalogType()))
                .catalogName(catalog.getCatalogName())
                .catalogCode(catalog.getCatalogCode())
                .catalogDesc(catalog.getCatalogDesc())
                .coverImage(catalog.getCoverImage())
                .itemCount(catalog.getItemCount())
                .accessCount(catalog.getAccessCount())
                .ownerId(catalog.getOwnerId())
                .deptId(catalog.getDeptId())
                .sortOrder(catalog.getSortOrder())
                .visible(catalog.getVisible())
                .visibleLabel(catalog.getVisible() != null && catalog.getVisible() == 1 ? "公开" : "私有")
                .status(catalog.getStatus())
                .statusLabel(catalog.getStatus() != null && "ACTIVE".equals(catalog.getStatus()) ? "启用" : "禁用")
                .createUser(catalog.getCreateUser())
                .createTime(catalog.getCreateTime())
                .updateUser(catalog.getUpdateUser())
                .updateTime(catalog.getUpdateTime())
                .albumDesc(catalog.getCatalogDesc());

        if (catalog.getParentId() != null && catalog.getParentId() != 0) {
            GovAssetCatalog parent = catalogMapper.selectById(catalog.getParentId());
            if (parent != null) {
                builder.parentName(parent.getCatalogName());
            }
        }

        if (catalog.getOwnerId() != null) {
            SysUser owner = userMapper.selectById(catalog.getOwnerId());
            if (owner != null) {
                builder.ownerName(owner.getNickname() != null ? owner.getNickname() : owner.getUsername());
            }
        }

        if (catalog.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(catalog.getDeptId());
            if (dept != null) {
                builder.deptName(dept.getDeptName());
            }
        }

        if (catalog.getCreateUser() != null) {
            SysUser creator = userMapper.selectById(catalog.getCreateUser());
            if (creator != null) {
                builder.createUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
            }
        }

        LambdaQueryWrapper<GovCatalogMetadata> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(GovCatalogMetadata::getCatalogId, catalog.getId())
                  .orderByDesc(GovCatalogMetadata::getSortOrder);
        List<GovCatalogMetadata> relations = catalogMetadataMapper.selectList(relWrapper);

        Set<Long> metadataIds = relations.stream()
                .map(GovCatalogMetadata::getMetadataId)
                .collect(Collectors.toSet());

        Map<Long, DqDatasource> dsMapDetailTemp = new HashMap<>();
        if (!metadataIds.isEmpty()) {
            List<GovMetadata> metas = metadataMapper.selectBatchIds(metadataIds);
            Set<Long> dsIds = metas.stream()
                    .map(GovMetadata::getDsId)
                    .filter(d -> d != null)
                    .collect(Collectors.toSet());
            if (!dsIds.isEmpty()) {
                List<DqDatasource> datasources = datasourceMapper.selectBatchIds(dsIds);
                dsMapDetailTemp = datasources.stream().collect(Collectors.toMap(DqDatasource::getId, d -> d));
            }
        }
        final Map<Long, DqDatasource> dsMapDetail = dsMapDetailTemp;

        Map<Long, GovCatalogMetadata> relMap = relations.stream()
                .collect(Collectors.toMap(GovCatalogMetadata::getMetadataId, r -> r));

        List<CatalogAssetVO> assetVOs = relations.stream()
                .map(rel -> {
                    GovMetadata meta = metadataMapper.selectById(rel.getMetadataId());
                    return buildCatalogAssetVO(meta, rel, dsMapDetail);
                })
                .collect(Collectors.toList());
        builder.assets(assetVOs);

        LambdaQueryWrapper<GovAssetCatalog> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(GovAssetCatalog::getParentId, catalog.getId())
                    .orderByAsc(GovAssetCatalog::getSortOrder);
        List<GovAssetCatalog> children = catalogMapper.selectList(childWrapper);
        List<AssetCatalogTreeVO> childNodes = children.stream()
                .map(this::buildTreeNodeFromEntity)
                .collect(Collectors.toList());
        builder.children(childNodes);

        return builder.build();
    }

    private CatalogAssetVO buildCatalogAssetVO(GovMetadata meta, GovCatalogMetadata rel,
                                               Map<Long, DqDatasource> dsMap) {
        if (meta == null) {
            return CatalogAssetVO.builder()
                    .metadataId(rel.getMetadataId())
                    .createTime(rel.getCreateTime())
                    .build();
        }

        DqDatasource ds = dsMap.get(meta.getDsId());

        return CatalogAssetVO.builder()
                .metadataId(meta.getId())
                .dsId(meta.getDsId())
                .dsName(ds != null ? ds.getDsName() : null)
                .dsType(ds != null ? ds.getDsType() : null)
                .dataLayer(meta.getDataLayer())
                .dataDomain(meta.getDataDomain())
                .tableName(meta.getTableName())
                .tableAlias(meta.getTableAlias())
                .tableComment(meta.getTableComment())
                .tableType(meta.getTableType())
                .rowCount(meta.getRowCount())
                .storageBytes(meta.getStorageBytes())
                .sensitivityLevel(meta.getSensitivityLevel())
                .sensitivityLevelLabel(getSensitivityLabel(meta.getSensitivityLevel()))
                .lastModifiedAt(meta.getLastModifiedAt())
                .createTime(rel.getCreateTime())
                .build();
    }

    private String getSensitivityLabel(String level) {
        if (level == null) return "一般";
        return switch (level) {
            case "PUBLIC" -> "公开";
            case "INTERNAL" -> "内部";
            case "CONFIDENTIAL" -> "保密";
            case "SECRET" -> "机密";
            default -> level;
        };
    }
}
