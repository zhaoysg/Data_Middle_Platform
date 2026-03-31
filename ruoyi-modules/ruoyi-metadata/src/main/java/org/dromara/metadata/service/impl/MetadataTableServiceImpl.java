package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.domain.bo.MetadataTableBo;
import org.dromara.metadata.domain.vo.MetadataTableVo;
import org.dromara.metadata.mapper.MetadataTableMapper;
import org.dromara.metadata.service.IMetadataTableService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 元数据表服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class MetadataTableServiceImpl implements IMetadataTableService {

    private final MetadataTableMapper baseMapper;

    @Override
    public TableDataInfo<MetadataTableVo> pageTableList(MetadataTableBo bo, PageQuery pageQuery) {
        // pageSize 上限 100
        int pageSize = Math.min(pageQuery.getPageSize(), 100);
        pageQuery.setPageSize(pageSize);

        long startMs = System.currentTimeMillis();
        Wrapper<MetadataTable> wrapper = buildQueryWrapper(bo);
        Page<MetadataTableVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);

        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed > 5000) {
            log.warn("元数据表分页查询耗时超过5秒: {}ms, keyword={}", elapsed, bo.getKeyword());
        }

        return TableDataInfo.build(page);
    }

    private Wrapper<MetadataTable> buildQueryWrapper(MetadataTableBo bo) {
        var wrapper = Wrappers.<MetadataTable>lambdaQuery();

        wrapper.eq(ObjectUtil.isNotNull(bo.getDsId()), MetadataTable::getDsId, bo.getDsId())
            .eq(StringUtils.isNotBlank(bo.getDataLayer()), MetadataTable::getDataLayer, bo.getDataLayer())
            .eq(StringUtils.isNotBlank(bo.getDataDomain()), MetadataTable::getDataDomain, bo.getDataDomain())
            .eq(StringUtils.isNotBlank(bo.getStatus()), MetadataTable::getStatus, bo.getStatus())
            .orderByDesc(MetadataTable::getLastScanTime);

        // 关键字搜索：转义特殊字符
        if (StringUtils.isNotBlank(bo.getKeyword())) {
            String keyword = bo.getKeyword()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
            wrapper.and(w -> w
                .like(MetadataTable::getTableName, keyword)
                .or()
                .like(MetadataTable::getTableAlias, keyword)
                .or()
                .like(MetadataTable::getTableComment, keyword)
            );
        }

        return wrapper;
    }

    @Override
    public MetadataTableVo getTableById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public Long insertTable(MetadataTableBo bo) {
        MetadataTable entity = MapstructUtils.convert(bo, MetadataTable.class);
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateTable(MetadataTableBo bo) {
        MetadataTable entity = MapstructUtils.convert(bo, MetadataTable.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteTable(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }

    @Override
    public List<MetadataTableVo> listByDsId(Long dsId) {
        return baseMapper.selectVoList(
            Wrappers.<MetadataTable>lambdaQuery()
                .eq(MetadataTable::getDsId, dsId)
                .orderByAsc(MetadataTable::getTableName)
        );
    }

    @Override
    public List<MetadataTableVo> listTable(MetadataTableBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public List<String> listTagOptions() {
        return extractTagOptions(baseMapper.selectList(
                Wrappers.<MetadataTable>lambdaQuery()
                    .select(MetadataTable::getTags)
                    .isNotNull(MetadataTable::getTags)
                    .ne(MetadataTable::getTags, "")
            ).stream()
            .map(MetadataTable::getTags)
            .toList());
    }

    static List<String> extractTagOptions(List<String> rawTags) {
        return rawTags.stream()
            .filter(StringUtils::isNotBlank)
            .flatMap(tags -> Arrays.stream(tags.split("[,，]")))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .sorted()
            .toList();
    }

    @Override
    public void upsert(MetadataTable table) {
        table.setTenantId(normalizeTenantId(table.getTenantId()));
        if (table.getDelFlag() == null || table.getDelFlag().isBlank()) {
            table.setDelFlag("0");
        }
        baseMapper.upsert(table);
    }

    @Override
    public void upsertBatch(List<MetadataTable> tables) {
        if (tables == null || tables.isEmpty()) return;
        for (MetadataTable table : tables) {
            upsert(table);
        }
    }

    @Override
    public MetadataTable getByDsIdAndTableName(Long dsId, String tableName) {
        return baseMapper.selectByTenantDsIdAndTableName(normalizeTenantId(TenantHelper.getTenantId()), dsId, tableName);
    }

    @Override
    public int updateAlias(Long id, String alias) {
        MetadataTable table = new MetadataTable(id);
        table.setTableAlias(alias);
        return baseMapper.updateById(table);
    }

    @Override
    public int updateStatus(Long id, String status) {
        MetadataTable table = new MetadataTable(id);
        table.setStatus(status);
        return baseMapper.updateById(table);
    }

    private String normalizeTenantId(String tenantId) {
        return StringUtils.isNotBlank(tenantId) ? tenantId.trim() : MetadataScanServiceImpl.DEFAULT_TENANT_ID;
    }
}
