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
import org.dromara.metadata.domain.MetadataColumn;
import org.dromara.metadata.domain.bo.MetadataColumnBo;
import org.dromara.metadata.domain.vo.MetadataColumnVo;
import org.dromara.metadata.mapper.MetadataColumnMapper;
import org.dromara.metadata.service.IMetadataColumnService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 元数据字段服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class MetadataColumnServiceImpl implements IMetadataColumnService {

    private final MetadataColumnMapper baseMapper;

    @Override
    public TableDataInfo<MetadataColumnVo> pageColumnList(MetadataColumnBo bo, PageQuery pageQuery) {
        int pageSize = Math.min(pageQuery.getPageSize(), 100);
        pageQuery.setPageSize(pageSize);

        long startMs = System.currentTimeMillis();
        Wrapper<MetadataColumn> wrapper = buildQueryWrapper(bo);
        Page<MetadataColumnVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);

        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed > 5000) {
            log.warn("元数据字段分页查询耗时超过5秒: {}ms, keyword={}", elapsed, bo.getKeyword());
        }

        return TableDataInfo.build(page);
    }

    private Wrapper<MetadataColumn> buildQueryWrapper(MetadataColumnBo bo) {
        var wrapper = Wrappers.<MetadataColumn>lambdaQuery();

        wrapper.eq(ObjectUtil.isNotNull(bo.getTableId()), MetadataColumn::getTableId, bo.getTableId())
            .eq(ObjectUtil.isNotNull(bo.getDsId()), MetadataColumn::getDsId, bo.getDsId())
            .orderByAsc(MetadataColumn::getSortOrder);

        if (StringUtils.isNotBlank(bo.getKeyword())) {
            String keyword = bo.getKeyword()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
            wrapper.and(w -> w
                .like(MetadataColumn::getColumnName, keyword)
                .or()
                .like(MetadataColumn::getColumnAlias, keyword)
                .or()
                .like(MetadataColumn::getColumnComment, keyword)
            );
        }

        return wrapper;
    }

    @Override
    public MetadataColumnVo getColumnById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<MetadataColumnVo> listByTableId(Long tableId) {
        return baseMapper.selectVoList(
            Wrappers.<MetadataColumn>lambdaQuery()
                .eq(MetadataColumn::getTableId, tableId)
                .orderByAsc(MetadataColumn::getSortOrder)
        );
    }

    @Override
    public int deleteColumn(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }

    @Override
    public List<MetadataColumnVo> listColumn(MetadataColumnBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public void upsert(MetadataColumn column) {
        column.setTenantId(normalizeTenantId(column.getTenantId()));
        if (column.getDelFlag() == null || column.getDelFlag().isBlank()) {
            column.setDelFlag("0");
        }
        baseMapper.upsert(column);
    }

    @Override
    public void upsertBatch(Long tableId, Long dsId, String tableName, List<MetadataColumn> columns) {
        if (columns == null || columns.isEmpty()) return;

        for (MetadataColumn col : columns) {
            col.setTableId(tableId);
            col.setDsId(dsId);
            col.setTableName(tableName);
            upsert(col);
        }
    }

    @Override
    public int updateAlias(Long id, String alias) {
        MetadataColumn column = new MetadataColumn(id);
        column.setColumnAlias(alias);
        return baseMapper.updateById(column);
    }

    private String normalizeTenantId(String tenantId) {
        return StringUtils.isNotBlank(tenantId) ? tenantId.trim() : MetadataScanServiceImpl.DEFAULT_TENANT_ID;
    }
}
