package com.bagdatahouse.governance.metadata.scanner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bagdatahouse.core.entity.GovLineage;
import com.bagdatahouse.core.entity.GovMetadata;
import com.bagdatahouse.core.entity.GovMetadataColumn;
import com.bagdatahouse.core.entity.SecColumnSensitivity;
import com.bagdatahouse.core.entity.SecLevel;
import com.bagdatahouse.core.entity.SecNewColumnAlert;
import com.bagdatahouse.core.mapper.GovLineageMapper;
import com.bagdatahouse.core.mapper.GovMetadataColumnMapper;
import com.bagdatahouse.core.mapper.GovMetadataMapper;
import com.bagdatahouse.core.mapper.SecColumnSensitivityMapper;
import com.bagdatahouse.core.mapper.SecLevelMapper;
import com.bagdatahouse.core.mapper.SecNewColumnAlertMapper;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter.ColumnInfo;
import com.bagdatahouse.governance.metadata.context.MetadataScanContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用元数据扫描器基类
 * 实现通用的表扫描、存储、更新逻辑
 */
@Slf4j
@Component
public abstract class AbstractMetadataScanner implements MetadataScanner {

    @Autowired
    private GovMetadataMapper metadataMapper;

    @Autowired
    private GovMetadataColumnMapper columnMapper;

    @Autowired
    private SecColumnSensitivityMapper secColumnSensitivityMapper;

    @Autowired
    private GovLineageMapper lineageMapper;

    @Autowired
    private SecLevelMapper secLevelMapper;

    @Autowired
    private SecNewColumnAlertMapper secNewColumnAlertMapper;

    @Override
    public void scan(MetadataScanContext ctx) {
        String dsType = ctx.getAdapter().getDataSourceType();
        if (!dsType.equalsIgnoreCase(getDataSourceType())) {
            log.warn("数据源类型不匹配：期望 {}，实际 {}", getDataSourceType(), dsType);
            return;
        }

        List<String> tables = resolveTableList(ctx);
        if (tables == null || tables.isEmpty()) {
            log.info("扫描范围内没有表: dsId={}, scope={}", ctx.getDatasource().getId(), ctx.getScanScope());
            ctx.setTotalPlannedTables(0);
            if ("RUNNING".equals(ctx.getStatus())) {
                ctx.setStatus("SUCCESS");
            }
            return;
        }

        ctx.setTotalPlannedTables(tables.size());

        log.info("开始扫描元数据: dsId={}, dsName={}, 表数量={}",
                ctx.getDatasource().getId(), ctx.getDatasource().getDsName(), tables.size());

        for (String tableName : tables) {
            if ("RUNNING".equals(ctx.getStatus())) {
                scanTable(ctx, tableName);
            }
        }

        if ("RUNNING".equals(ctx.getStatus())) {
            ctx.setStatus("SUCCESS");
        }
        log.info("元数据扫描完成: dsId={}, 成功={}, 失败={}, 耗时={}ms",
                ctx.getDatasource().getId(), ctx.getSuccessCount().get(),
                ctx.getFailedCount().get(), ctx.getElapsedMs());

        // 阶段三-T1: 元数据扫描完成后回填字段级血缘的 sensitivity_level
        try {
            backfillLineageSensitivityLevel(ctx);
        } catch (Exception e) {
            log.warn("回填字段级血缘敏感等级失败（不影响扫描主流程）: dsId={}, 错误: {}",
                    ctx.getDatasource().getId(), e.getMessage());
        }
    }

    /**
     * 阶段三-T1: 元数据扫描完成后回填字段级血缘节点的 sensitivity_level
     * <p>
     * 逻辑说明：
     * 1. 查询本次扫描涉及的所有字段级血缘记录（COLUMN 类型）
     * 2. 对每条记录，通过 (dsId, sourceTable, sourceColumn) JOIN sec_column_sensitivity
     *    获取字段的敏感等级
     * 3. 回填 gov_lineage.sensitivity_level（已 APPROVED 的记录优先，其次取最新扫描结果）
     */
    private void backfillLineageSensitivityLevel(MetadataScanContext ctx) {
        Long dsId = ctx.getDatasource().getId();

        // 查询该数据源下所有字段级血缘记录（分批处理，避免大数据量卡死）
        LambdaQueryWrapper<GovLineage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovLineage::getSourceDsId, dsId)
                .eq(GovLineage::getLineageType, "COLUMN")
                .isNotNull(GovLineage::getSourceTable)
                .isNotNull(GovLineage::getSourceColumn)
                .last("LIMIT 5000");

        List<GovLineage> lineageList = lineageMapper.selectList(wrapper);
        if (lineageList.isEmpty()) {
            log.debug("没有字段级血缘记录需要回填敏感等级: dsId={}", dsId);
            return;
        }

        int updated = 0;
        for (GovLineage lineage : lineageList) {
            String level = resolveSensitivityLevel(dsId,
                    lineage.getSourceTable(), lineage.getSourceColumn());
            if (level != null && !level.equals(lineage.getSensitivityLevel())) {
                LambdaUpdateWrapper<GovLineage> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(GovLineage::getId, lineage.getId())
                        .set(GovLineage::getSensitivityLevel, level);
                lineageMapper.update(null, updateWrapper);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("字段级血缘敏感等级回填完成: dsId={}, 总记录={}, 更新={}", dsId, lineageList.size(), updated);
        }
    }

    /**
     * 解析字段的敏感等级
     * <p>
     * 查询优先级：
     * 1. 优先取已 APPROVED 的记录（经过审核确认的等级）
     * 2. 其次取最新扫描结果（PENDING 状态）
     * 3. 若 sec_column_sensitivity 只有 levelId，则 JOIN sec_level 获取 levelCode
     */
    private String resolveSensitivityLevel(Long dsId, String tableName, String columnName) {
        if (dsId == null || tableName == null || columnName == null) {
            return null;
        }

        // 优先查询已审核通过的记录
        LambdaQueryWrapper<SecColumnSensitivity> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.eq(SecColumnSensitivity::getDsId, dsId)
                .eq(SecColumnSensitivity::getTableName, tableName)
                .eq(SecColumnSensitivity::getColumnName, columnName)
                .eq(SecColumnSensitivity::getReviewStatus, "APPROVED")
                .last("LIMIT 1");
        SecColumnSensitivity approved = secColumnSensitivityMapper.selectOne(approvedWrapper);
        if (approved != null) {
            return mapToLevelCode(approved);
        }

        // 其次查询最新扫描结果（PENDING 状态）
        LambdaQueryWrapper<SecColumnSensitivity> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(SecColumnSensitivity::getDsId, dsId)
                .eq(SecColumnSensitivity::getTableName, tableName)
                .eq(SecColumnSensitivity::getColumnName, columnName)
                .orderByDesc(SecColumnSensitivity::getScanTime)
                .last("LIMIT 1");
        SecColumnSensitivity pending = secColumnSensitivityMapper.selectOne(pendingWrapper);
        if (pending != null) {
            return mapToLevelCode(pending);
        }

        return null;
    }

    /**
     * 将 SecColumnSensitivity 记录映射为等级码（L1/L2/L3/L4）
     * 优先使用 levelCode（直辖市），其次通过 levelId JOIN sec_level 获取
     */
    private String mapToLevelCode(SecColumnSensitivity record) {
        // 场景一：sec_column_sensitivity 已有 level_code 字段（阶段六扩展）
        // 目前阶段三 entity 中暂无此字段，通过 levelId 查询 sec_level
        if (record.getLevelId() == null) {
            return null;
        }

        SecLevel level = secLevelMapper.selectById(record.getLevelId());
        if (level != null && level.getLevelCode() != null) {
            return level.getLevelCode();
        }

        return null;
    }

    /**
     * 由子类实现，获取该数据源类型下的所有表名列表
     */
    protected abstract List<String> getAllTables(DataSourceAdapter adapter);

    /**
     * 由子类实现，过滤出符合通配符模式的表
     */
    protected abstract List<String> filterTablesByPattern(List<String> allTables, String pattern);

    protected List<String> resolveTableList(MetadataScanContext ctx) {
        String scope = ctx.getScanScope();
        if (scope == null) {
            scope = "ALL";
        }

        switch (scope.toUpperCase()) {
            case "SPECIFIED":
                return ctx.getTableNames();
            case "PATTERN":
                List<String> all = getAllTables(ctx.getAdapter());
                return filterTablesByPattern(all, ctx.getTablePattern());
            case "ALL":
            default:
                return getAllTables(ctx.getAdapter());
        }
    }

    @Override
    public void scanTable(MetadataScanContext ctx, String tableName) {
        try {
            log.debug("扫描表: {}.{}", ctx.getDatasource().getDsName(), tableName);

            DataSourceAdapter adapter = ctx.getAdapter();
            GovMetadata existing = findExistingMetadata(ctx.getDatasource().getId(), tableName);

            List<ColumnInfo> columnInfos = adapter.getColumns(tableName);
            List<String> primaryKeys = adapter.getPrimaryKeys(tableName);
            Set<String> pkSet = primaryKeys != null
                    ? primaryKeys.stream().collect(Collectors.toSet())
                    : Set.of();

            GovMetadata metadata = buildMetadata(ctx, tableName, columnInfos);
            GovMetadata saved = saveMetadata(metadata, existing, ctx);

            List<GovMetadataColumn> columns = buildColumns(ctx, saved.getId(), tableName, columnInfos, pkSet);
            saveColumns(saved.getId(), columns, ctx);

            ctx.addSuccess(tableName, existing == null);
            ctx.addColumns(columns.size());

            log.debug("表扫描完成: {}.{}, 字段数={}, 行数={}",
                    ctx.getDatasource().getDsName(), tableName,
                    columnInfos.size(), metadata.getRowCount());
        } catch (Exception e) {
            log.error("扫描表失败: {}.{}, 错误: {}", ctx.getDatasource().getDsName(), tableName, e.getMessage(), e);
            ctx.addError(tableName, e.getMessage(), e);
        }
    }

    /**
     * 从元数据库查找已存在的记录
     */
    protected GovMetadata findExistingMetadata(Long dsId, String tableName) {
        LambdaQueryWrapper<GovMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovMetadata::getDsId, dsId)
                .eq(GovMetadata::getTableName, tableName);
        return metadataMapper.selectOne(wrapper);
    }

    /**
     * 构建元数据实体
     */
    protected GovMetadata buildMetadata(MetadataScanContext ctx, String tableName, List<ColumnInfo> columnInfos) {
        String schemaHint = ctx.getDatasource() != null && StringUtils.hasText(ctx.getDatasource().getSchemaName())
                ? ctx.getDatasource().getSchemaName().trim()
                : null;

        String tableComment = "";
        try {
            DataSourceAdapter ad = ctx.getAdapter();
            tableComment = schemaHint != null ? ad.getTableComment(schemaHint, tableName) : ad.getTableComment(tableName);
        } catch (Exception e) {
            log.debug("读取表级注释失败: {}", tableName, e);
        }

        String layer = ctx.getDataLayer();
        if (!StringUtils.hasText(layer) && ctx.getDatasource() != null) {
            layer = ctx.getDatasource().getDataLayer();
        }

        GovMetadata metadata = GovMetadata.builder()
                .dsId(ctx.getDatasource().getId())
                .tableName(tableName)
                .tableType(determineTableType(ctx.getAdapter(), tableName))
                .dataLayer(layer)
                .dataDomain(ctx.getDataDomain())
                .bizDomain(ctx.getBizDomain())
                .createUser(ctx.getCreateUser())
                .createTime(LocalDateTime.now())
                .status("ACTIVE")
                .accessFreq(0)
                .isPartitioned(false)
                .sensitivityLevel("NORMAL")
                .build();

        if (StringUtils.hasText(tableComment)) {
            metadata.setTableComment(tableComment.trim());
            metadata.setTableAlias(tableComment.trim());
        }

        // 收集表统计信息
        if (ctx.isCollectStats()) {
            try {
                metadata.setRowCount(ctx.getAdapter().getRowCount(tableName));
            } catch (Exception e) {
                log.debug("获取表行数失败: {}.{}", tableName, e.getMessage());
            }
        }

        return metadata;
    }

    /**
     * 保存元数据（新增或更新）
     */
    protected GovMetadata saveMetadata(GovMetadata metadata, GovMetadata existing, MetadataScanContext ctx) {
        if (existing == null) {
            metadataMapper.insert(metadata);
            return metadata;
        }

        if (ctx.isOverwriteExisting()) {
            metadata.setId(existing.getId());
            metadata.setUpdateUser(ctx.getCreateUser());
            metadata.setUpdateTime(LocalDateTime.now());
            metadata.setDataLayer(existing.getDataLayer());
            boolean aliasWasAuto = !StringUtils.hasText(existing.getTableAlias())
                    || (StringUtils.hasText(existing.getTableComment())
                    && existing.getTableAlias().equals(existing.getTableComment()));
            if (aliasWasAuto && StringUtils.hasText(metadata.getTableComment())) {
                metadata.setTableAlias(metadata.getTableComment());
            } else {
                metadata.setTableAlias(existing.getTableAlias());
            }
            metadata.setOwnerId(existing.getOwnerId());
            metadata.setDeptId(existing.getDeptId());
            metadata.setTags(existing.getTags());
            metadata.setLifecycleDays(existing.getLifecycleDays());
            metadata.setSensitivityLevel(existing.getSensitivityLevel());
            metadata.setLastModifiedAt(LocalDateTime.now());
            metadataMapper.updateById(metadata);
            return metadata;
        } else {
            ctx.addSkipped();
            return existing;
        }
    }

    /**
     * 构建字段列表
     */
    protected List<GovMetadataColumn> buildColumns(
            MetadataScanContext ctx, Long metadataId, String tableName,
            List<ColumnInfo> columnInfos, Set<String> pkSet) {

        int[] sortOrder = {0};
        return columnInfos.stream()
                .map(col -> GovMetadataColumn.builder()
                        .metadataId(metadataId)
                        .tableName(tableName.trim())
                        .columnName(col.columnName())
                        .columnComment(col.columnComment())
                        .dataType(col.dataType())
                        .isNullable(col.nullable())
                        .defaultValue(col.defaultValue())
                        .isPrimaryKey(pkSet.contains(col.columnName()))
                        .isForeignKey(false)
                        .isSensitive(false)
                        .sensitivityLevel("NORMAL")
                        .sortOrder(sortOrder[0]++)
                        .createUser(ctx.getCreateUser())
                        .createTime(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 保存字段列表
     */
    protected void saveColumns(Long metadataId, List<GovMetadataColumn> columns, MetadataScanContext ctx) {
        if (columns == null || columns.isEmpty()) {
            return;
        }

        // 获取表名（从 metadata 获取）
        GovMetadata meta = metadataMapper.selectById(metadataId);
        if (meta == null) {
            return;
        }
        String targetTableName = meta.getTableName();
        Long dsId = meta.getDsId();

        // 生成扫描批次号（用于写入告警表）
        String scanBatchNo = "META_" + LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + System.currentTimeMillis() % 1000;

        // 收集已存在的字段名集合
        Set<String> existingColumnNames = new java.util.HashSet<>();
        LambdaQueryWrapper<GovMetadataColumn> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(GovMetadataColumn::getMetadataId, metadataId);
        columnMapper.selectList(checkWrapper).forEach(c -> existingColumnNames.add(c.getColumnName()));

        if (ctx.isOverwriteExisting()) {
            // 全量覆盖模式：删除旧字段，重新插入全部字段，不触发新字段告警
            LambdaQueryWrapper<GovMetadataColumn> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(GovMetadataColumn::getMetadataId, metadataId);
            columnMapper.delete(delWrapper);
        } else {
            // 增量模式：检查是否已有字段
            if (columnMapper.selectCount(checkWrapper) > 0) {
                // 仅处理新增字段（不在 existingColumnNames 中的字段）
                for (GovMetadataColumn col : columns) {
                    if (!existingColumnNames.contains(col.getColumnName())) {
                        recordNewColumnAlert(dsId, targetTableName, col.getColumnName(),
                                col.getDataType(), col.getColumnComment(), scanBatchNo);
                    }
                }
                return;
            }
        }

        for (GovMetadataColumn col : columns) {
            columnMapper.insert(col);
        }
    }

    /**
     * 阶段六-T3: 发现新字段时写入 sec_new_column_alert 告警表
     */
    private void recordNewColumnAlert(Long dsId, String tableName, String columnName,
            String dataType, String columnComment, String scanBatchNo) {
        // 检查是否已存在（未忽略的记录）
        LambdaQueryWrapper<SecNewColumnAlert> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(SecNewColumnAlert::getDsId, dsId)
                .eq(SecNewColumnAlert::getTableName, tableName)
                .eq(SecNewColumnAlert::getColumnName, columnName)
                .ne(SecNewColumnAlert::getStatus, "DISMISSED");
        if (secNewColumnAlertMapper.selectCount(existWrapper) > 0) {
            return;
        }

        SecNewColumnAlert alert = SecNewColumnAlert.builder()
                .dsId(dsId)
                .tableName(tableName)
                .columnName(columnName)
                .dataType(dataType)
                .columnComment(columnComment)
                .alertType("NEW_COLUMN")
                .status("PENDING")
                .scanBatchNo(scanBatchNo)
                .createdAt(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        secNewColumnAlertMapper.insert(alert);
        log.info("发现新字段[dsId={}, table={}, column={}]，已写入 sec_new_column_alert", dsId, tableName, columnName);
    }

    /**
     * 判断表类型（TABLE 或 VIEW）
     */
    protected String determineTableType(DataSourceAdapter adapter, String tableName) {
        return "TABLE";
    }
}
