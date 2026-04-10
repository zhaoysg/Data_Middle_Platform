package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
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
import org.dromara.metadata.domain.MetadataColumn;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.domain.bo.MetadataColumnBo;
import org.dromara.metadata.domain.vo.MetadataColumnVo;
import org.dromara.metadata.mapper.MetadataColumnMapper;
import org.dromara.metadata.mapper.MetadataTableMapper;
import org.dromara.metadata.service.IMetadataColumnService;
import org.dromara.metadata.support.DatasourceHelper;
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
    private final MetadataTableMapper tableMapper;
    private final DatasourceHelper datasourceHelper;

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
        applyAccessibleDatasourceFilter(wrapper, bo.getDsId(), bo.getTableId());

        wrapper.eq(ObjectUtil.isNotNull(bo.getTableId()), MetadataColumn::getTableId, bo.getTableId())
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
        MetadataColumn column = requireAccessibleColumn(id);
        return MapstructUtils.convert(column, MetadataColumnVo.class);
    }

    @Override
    public List<MetadataColumnVo> listByTableId(Long tableId) {
        MetadataTable table = requireAccessibleTable(tableId);
        return baseMapper.selectVoList(
            Wrappers.<MetadataColumn>lambdaQuery()
                .eq(MetadataColumn::getTableId, table.getId())
                .orderByAsc(MetadataColumn::getSortOrder)
        );
    }

    @Override
    public int deleteColumn(Long[] ids) {
        for (Long id : ids) {
            requireAccessibleColumn(id);
        }
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
        requireAccessibleColumn(id);
        MetadataColumn column = new MetadataColumn(id);
        column.setColumnAlias(alias);
        return baseMapper.updateById(column);
    }

    @Override
    public int updateColumn(MetadataColumnBo bo) {
        requireAccessibleColumn(bo.getId());
        // 业务编辑入口：显式写入别名/注释/敏感等级（含置空），避免 isNotBlank 导致无法保存空串
        return baseMapper.update(null,
            Wrappers.<MetadataColumn>lambdaUpdate()
                .eq(MetadataColumn::getId, bo.getId())
                .set(MetadataColumn::getColumnAlias, bo.getColumnAlias())
                .set(MetadataColumn::getColumnComment, bo.getColumnComment())
                .set(MetadataColumn::getSensitivityLevel, bo.getSensitivityLevel())
                .set(bo.getSortOrder() != null, MetadataColumn::getSortOrder, bo.getSortOrder()));
    }

    private void applyAccessibleDatasourceFilter(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetadataColumn> wrapper,
                                                 Long dsId, Long tableId) {
        if (tableId != null) {
            MetadataTable table = requireAccessibleTable(tableId);
            wrapper.eq(MetadataColumn::getDsId, table.getDsId());
            return;
        }
        List<Long> accessibleDsIds = datasourceHelper.resolveAccessibleDatasourceIds(dsId);
        if (accessibleDsIds.isEmpty()) {
            wrapper.eq(MetadataColumn::getId, -1L);
            return;
        }
        wrapper.in(MetadataColumn::getDsId, accessibleDsIds);
    }

    private MetadataTable requireAccessibleTable(Long tableId) {
        if (tableId == null) {
            throw new ServiceException("表ID不能为空");
        }
        MetadataTable table = tableMapper.selectById(tableId);
        if (table == null) {
            throw new ServiceException("元数据表不存在: " + tableId);
        }
        datasourceHelper.getSysDatasource(table.getDsId());
        return table;
    }

    private MetadataColumn requireAccessibleColumn(Long id) {
        if (id == null) {
            throw new ServiceException("字段ID不能为空");
        }
        MetadataColumn column = baseMapper.selectById(id);
        if (column == null) {
            throw new ServiceException("元数据字段不存在: " + id);
        }
        datasourceHelper.getSysDatasource(column.getDsId());
        return column;
    }

    private String normalizeTenantId(String tenantId) {
        return StringUtils.isNotBlank(tenantId) ? tenantId.trim() : MetadataScanServiceImpl.DEFAULT_TENANT_ID;
    }
}
