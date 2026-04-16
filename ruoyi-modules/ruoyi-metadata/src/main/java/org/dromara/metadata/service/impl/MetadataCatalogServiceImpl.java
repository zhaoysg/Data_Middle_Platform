package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.MetadataCatalog;
import org.dromara.metadata.domain.bo.MetadataCatalogBo;
import org.dromara.metadata.domain.vo.MetadataCatalogVo;
import org.dromara.metadata.mapper.MetadataCatalogMapper;
import org.dromara.metadata.service.IMetadataCatalogService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资产目录服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class MetadataCatalogServiceImpl implements IMetadataCatalogService {

    private final MetadataCatalogMapper baseMapper;

    @Override
    public TableDataInfo<MetadataCatalogVo> pageCatalogList(MetadataCatalogBo bo, PageQuery pageQuery) {
        Wrapper<MetadataCatalog> wrapper = Wrappers.<MetadataCatalog>lambdaQuery()
            .like(StringUtils.isNotBlank(bo.getCatalogName()), MetadataCatalog::getCatalogName, bo.getCatalogName())
            .like(StringUtils.isNotBlank(bo.getCatalogCode()), MetadataCatalog::getCatalogCode, bo.getCatalogCode())
            .eq(StringUtils.isNotBlank(bo.getCatalogType()), MetadataCatalog::getCatalogType, bo.getCatalogType())
            .eq(StringUtils.isNotBlank(bo.getStatus()), MetadataCatalog::getStatus, bo.getStatus())
            .orderByAsc(MetadataCatalog::getSortOrder);
        Page<MetadataCatalogVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        enrichParentNames(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public MetadataCatalogVo getCatalogById(Long id) {
        MetadataCatalogVo catalog = baseMapper.selectVoById(id);
        if (catalog != null) {
            enrichParentNames(CollUtil.newArrayList(catalog));
        }
        return catalog;
    }

    @Override
    @DSTransactional
    public Long insertCatalog(MetadataCatalogBo bo) {
        if (StringUtils.isNotBlank(bo.getCatalogCode())) {
            MetadataCatalog exist = baseMapper.selectByCatalogCode(bo.getCatalogCode());
            if (exist != null) {
                throw new ServiceException("目录编码已存在");
            }
        }
        bo.setParentId(normalizeParentId(bo.getParentId()));
        validateParent(null, bo.getParentId());
        MetadataCatalog entity = MapstructUtils.convert(bo, MetadataCatalog.class);
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @DSTransactional
    public int updateCatalog(MetadataCatalogBo bo) {
        if (ObjectUtil.isNull(bo.getId())) {
            throw new ServiceException("目录ID不能为空");
        }
        if (StringUtils.isNotBlank(bo.getCatalogCode())) {
            boolean exist = baseMapper.exists(Wrappers.<MetadataCatalog>lambdaQuery()
                .eq(MetadataCatalog::getCatalogCode, bo.getCatalogCode())
                .ne(MetadataCatalog::getId, bo.getId()));
            if (exist) {
                throw new ServiceException("目录编码已存在");
            }
        }
        bo.setParentId(normalizeParentId(bo.getParentId()));
        validateParent(bo.getId(), bo.getParentId());
        MetadataCatalog entity = MapstructUtils.convert(bo, MetadataCatalog.class);
        return baseMapper.updateById(entity);
    }

    @Override
    @DSTransactional
    public int deleteCatalog(Long[] ids) {
        for (Long id : ids) {
            boolean hasChildren = baseMapper.exists(Wrappers.<MetadataCatalog>lambdaQuery()
                .eq(MetadataCatalog::getParentId, id));
            if (hasChildren) {
                throw new ServiceException("请先删除子目录");
            }
        }
        return baseMapper.deleteBatchIds(List.of(ids));
    }

    @Override
    public List<MetadataCatalogVo> listTree() {
        List<MetadataCatalogVo> all = baseMapper.selectVoList(
            Wrappers.<MetadataCatalog>lambdaQuery().orderByAsc(MetadataCatalog::getSortOrder)
        );
        enrichParentNames(all);
        return buildTree(all);
    }

    @Override
    public List<MetadataCatalogVo> buildTree(List<MetadataCatalogVo> list) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        Map<Long, List<MetadataCatalogVo>> childrenMap = list.stream()
            .filter(e -> e.getParentId() != null && e.getParentId() > 0)
            .collect(Collectors.groupingBy(MetadataCatalogVo::getParentId));

        return list.stream()
            .filter(e -> e.getParentId() == null || e.getParentId() == 0)
            .peek(root -> buildChildren(root, childrenMap))
            .collect(Collectors.toList());
    }

    private void buildChildren(MetadataCatalogVo parent, Map<Long, List<MetadataCatalogVo>> childrenMap) {
        List<MetadataCatalogVo> children = childrenMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            for (MetadataCatalogVo child : children) {
                buildChildren(child, childrenMap);
            }
        } else {
            parent.setChildren(null);
        }
    }

    @Override
    public List<MetadataCatalogVo> listEnabled() {
        List<MetadataCatalogVo> list = baseMapper.selectVoList(
            Wrappers.<MetadataCatalog>lambdaQuery()
                .eq(MetadataCatalog::getStatus, "0")
                .orderByAsc(MetadataCatalog::getSortOrder)
        );
        enrichParentNames(list);
        return list;
    }

    private void validateParent(Long id, Long parentId) {
        if (parentId == null || parentId <= 0) {
            return;
        }
        if (ObjectUtil.isNotNull(id) && id.equals(parentId)) {
            throw new ServiceException("上级目录不能选择自己");
        }
        MetadataCatalog parent = baseMapper.selectById(parentId);
        if (parent == null) {
            throw new ServiceException("上级目录不存在");
        }
        if (ObjectUtil.isNull(id)) {
            return;
        }
        Long currentParentId = parent.getParentId();
        Set<Long> visited = new HashSet<>();
        while (currentParentId != null && currentParentId > 0) {
            if (!visited.add(currentParentId)) {
                throw new ServiceException("目录层级存在循环引用");
            }
            if (id.equals(currentParentId)) {
                throw new ServiceException("上级目录不能选择当前目录的子目录");
            }
            MetadataCatalog currentParent = baseMapper.selectById(currentParentId);
            if (currentParent == null) {
                break;
            }
            currentParentId = currentParent.getParentId();
        }
    }

    private Long normalizeParentId(Long parentId) {
        return ObjectUtil.defaultIfNull(parentId, 0L);
    }

    private void enrichParentNames(List<MetadataCatalogVo> catalogs) {
        if (CollUtil.isEmpty(catalogs)) {
            return;
        }
        List<Long> parentIds = catalogs.stream()
            .map(MetadataCatalogVo::getParentId)
            .filter(parentId -> parentId != null && parentId > 0)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(parentIds)) {
            catalogs.forEach(item -> item.setParentName("顶级目录"));
            return;
        }
        Map<Long, String> parentNameMap = baseMapper.selectList(
                Wrappers.<MetadataCatalog>lambdaQuery()
                    .select(MetadataCatalog::getId, MetadataCatalog::getCatalogName)
                    .in(MetadataCatalog::getId, parentIds)
            ).stream()
            .collect(Collectors.toMap(MetadataCatalog::getId, MetadataCatalog::getCatalogName));
        catalogs.forEach(item -> {
            Long parentId = item.getParentId();
            if (parentId == null || parentId <= 0) {
                item.setParentName("顶级目录");
                return;
            }
            item.setParentName(parentNameMap.getOrDefault(parentId, "未知目录"));
        });
    }
}
