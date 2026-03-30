package com.bagdatahouse.governance.metadata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.entity.GovMetadata;
import com.bagdatahouse.core.entity.GovMetadataColumn;
import com.bagdatahouse.core.entity.SecColumnSensitivity;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.mapper.GovMetadataColumnMapper;
import com.bagdatahouse.core.mapper.GovMetadataMapper;
import com.bagdatahouse.core.mapper.SecColumnSensitivityMapper;
import com.bagdatahouse.governance.metadata.dto.MetadataScanRequest;
import com.bagdatahouse.governance.metadata.dto.MetadataScanResult;
import com.bagdatahouse.governance.metadata.dto.MetadataScanResult.TableScanError;
import com.bagdatahouse.governance.metadata.dto.MetadataScanResult.TableScanSummary;
import com.bagdatahouse.governance.metadata.context.MetadataScanContext;
import com.bagdatahouse.governance.metadata.scanner.MetadataScanner;
import com.bagdatahouse.governance.metadata.scanner.MySQLMetadataScanner;
import com.bagdatahouse.governance.metadata.scanner.SqlServerMetadataScanner;
import com.bagdatahouse.governance.metadata.scanner.OracleMetadataScanner;
import com.bagdatahouse.governance.metadata.scanner.PostgresMetadataScanner;
import com.bagdatahouse.governance.metadata.service.MetadataScanAsyncRunner;
import com.bagdatahouse.governance.metadata.service.MetadataService;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import com.bagdatahouse.governance.metadata.service.MetadataService.MetadataStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 元数据管理服务实现
 */
@Slf4j
@Service
public class MetadataServiceImpl extends ServiceImpl<GovMetadataMapper, GovMetadata>
        implements MetadataService {

    private static final String TASK_PREFIX = "META_SCAN_";

    private final Map<String, MetadataScanContext> taskContextMap = new ConcurrentHashMap<>();

    @Autowired
    private GovMetadataMapper metadataMapper;

    @Autowired
    private GovMetadataColumnMapper columnMapper;

    @Autowired
    private SecColumnSensitivityMapper secColumnSensitivityMapper;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private DataSourceAdapterRegistry adapterRegistry;

    @Autowired
    private MySQLMetadataScanner mysqlScanner;

    @Autowired
    private SqlServerMetadataScanner sqlServerScanner;

    @Autowired
    private OracleMetadataScanner oracleScanner;

    @Autowired
    private PostgresMetadataScanner postgresScanner;

    @Autowired
    private MetadataScanAsyncRunner metadataScanAsyncRunner;

    @Override
    public Result<MetadataScanResult> scan(MetadataScanRequest request) {
        MetadataScanContext ctx = buildContext(request);
        MetadataScanner scanner = resolveScanner(ctx.getDatasource().getDsType());
        if (scanner == null) {
            throw new BusinessException(400, "不支持的数据源类型: " + ctx.getDatasource().getDsType());
        }
        scanner.scan(ctx);
        return Result.success(buildResult(ctx));
    }

    @Override
    public Result<String> scanAsync(MetadataScanRequest request) {
        MetadataScanContext ctx = buildContext(request);
        String taskId = TASK_PREFIX + System.currentTimeMillis();
        ctx.setTaskId(taskId);
        taskContextMap.put(taskId, ctx);
        MetadataScanner scanner = resolveScanner(ctx.getDatasource().getDsType());
        if (scanner == null) {
            ctx.setStatus("FAILED");
            log.error("不支持的数据源类型: {}", ctx.getDatasource().getDsType());
            return Result.success(taskId);
        }
        metadataScanAsyncRunner.runScan(ctx, scanner);
        return Result.success(taskId);
    }

    @Override
    public Result<MetadataScanResult> getScanProgress(String taskId) {
        MetadataScanContext ctx = taskContextMap.get(taskId);
        if (ctx == null) {
            throw new BusinessException(2001, "扫描任务不存在: " + taskId);
        }
        MetadataScanResult result = buildResult(ctx);
        return Result.success(result);
    }

    @Override
    public Result<Void> cancelScan(String taskId) {
        MetadataScanContext ctx = taskContextMap.get(taskId);
        if (ctx == null) {
            throw new BusinessException(2001, "扫描任务不存在: " + taskId);
        }
        if (!"RUNNING".equals(ctx.getStatus())) {
            throw new BusinessException(2001, "任务已结束，无法取消");
        }
        ctx.setStatus("CANCELLED");
        return Result.success();
    }

    @Override
    public Result<Page<GovMetadata>> page(Integer pageNum, Integer pageSize, Long dsId,
                                           String dataLayer, String dataDomain,
                                           String tableName, String status) {
        Page<GovMetadata> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GovMetadata> wrapper = new LambdaQueryWrapper<>();

        if (dsId != null) {
            wrapper.eq(GovMetadata::getDsId, dsId);
        }
        if (StringUtils.hasText(dataLayer)) {
            wrapper.eq(GovMetadata::getDataLayer, dataLayer);
        }
        if (StringUtils.hasText(dataDomain)) {
            wrapper.eq(GovMetadata::getDataDomain, dataDomain);
        }
        if (StringUtils.hasText(tableName)) {
            wrapper.like(GovMetadata::getTableName, tableName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(GovMetadata::getStatus, status);
        }

        wrapper.orderByDesc(GovMetadata::getCreateTime);
        Page<GovMetadata> result = this.page(page, wrapper);
        enrichMetadataListFlags(result.getRecords());
        return Result.success(result);
    }

    @Override
    public Result<GovMetadata> getById(Long id) {
        GovMetadata entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "元数据不存在");
        }
        enrichMetadataListFlags(Collections.singletonList(entity));
        return Result.success(entity);
    }

    @Override
    public Result<GovMetadata> getByDsIdAndTable(Long dsId, String tableName) {
        LambdaQueryWrapper<GovMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovMetadata::getDsId, dsId)
                .eq(GovMetadata::getTableName, tableName);
        GovMetadata entity = this.getOne(wrapper);
        if (entity == null) {
            throw new BusinessException(2001, "元数据不存在");
        }
        enrichMetadataListFlags(Collections.singletonList(entity));
        return Result.success(entity);
    }

    @Override
    public Result<List<GovMetadataColumn>> getColumns(Long metadataId) {
        LambdaQueryWrapper<GovMetadataColumn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovMetadataColumn::getMetadataId, metadataId)
                .orderByAsc(GovMetadataColumn::getSortOrder);
        List<GovMetadataColumn> columns = columnMapper.selectList(wrapper);
        return Result.success(columns);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateMetadata(Long id, GovMetadata metadata) {
        GovMetadata existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "元数据不存在");
        }
        metadata.setId(id);
        metadata.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(metadata);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateColumn(Long id, GovMetadataColumn column) {
        GovMetadataColumn existing = columnMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "字段不存在");
        }
        column.setId(id);
        column.setUpdateTime(LocalDateTime.now());
        columnMapper.updateById(column);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteMetadata(Long id) {
        GovMetadata existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "元数据不存在");
        }
        LambdaQueryWrapper<GovMetadataColumn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovMetadataColumn::getMetadataId, id);
        columnMapper.delete(wrapper);
        baseMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        for (Long id : ids) {
            LambdaQueryWrapper<GovMetadataColumn> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GovMetadataColumn::getMetadataId, id);
            columnMapper.delete(wrapper);
        }
        baseMapper.deleteBatchIds(ids);
        return Result.success();
    }

    @Override
    public Result<Void> syncStats(Long dsId, List<String> tableNames) {
        DqDatasource datasource = datasourceMapper.selectById(dsId);
        if (datasource == null || datasource.getStatus() != 1) {
            throw new BusinessException(2001, "数据源不存在或未启用");
        }

        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            throw new BusinessException(2001, "数据源适配器未找到");
        }

        for (String tableName : tableNames) {
            try {
                long rowCount = adapter.getRowCount(tableName);
                LambdaQueryWrapper<GovMetadata> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(GovMetadata::getDsId, dsId)
                        .eq(GovMetadata::getTableName, tableName);
                GovMetadata metadata = this.getOne(wrapper);
                if (metadata != null) {
                    metadata.setRowCount(rowCount);
                    metadata.setLastAccessedAt(LocalDateTime.now());
                    baseMapper.updateById(metadata);
                }
            } catch (Exception e) {
                log.warn("同步表统计失败: {}.{}, 错误: {}", dsId, tableName, e.getMessage());
            }
        }
        return Result.success();
    }

    @Override
    public Result<MetadataStats> getStats() {
        long totalTables = this.count();
        long totalColumns = columnMapper.selectCount(null);

        LambdaQueryWrapper<GovMetadata> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(GovMetadata::getStatus, "ACTIVE");
        long activeTables = this.count(activeWrapper);

        List<GovMetadata> allMetadata = this.list();
        Map<String, Long> byDataLayer = allMetadata.stream()
                .filter(m -> m.getDataLayer() != null)
                .collect(Collectors.groupingBy(GovMetadata::getDataLayer, Collectors.counting()));

        Map<String, Long> byDsType = new HashMap<>();
        Set<Long> dsIdSet = allMetadata.stream()
                .map(GovMetadata::getDsId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        long dsCount = dsIdSet.size();

        for (Long dsId : dsIdSet) {
            DqDatasource ds = datasourceMapper.selectById(dsId);
            if (ds != null) {
                String type = ds.getDsType() != null ? ds.getDsType() : "UNKNOWN";
                byDsType.merge(type, 1L, Long::sum);
            }
        }

        return Result.success(new MetadataStats(
                totalTables, totalColumns, dsCount, activeTables, byDataLayer, byDsType
        ));
    }

    /**
     * 补充列表展示字段：是否存在敏感列（元数据字段表 或 敏感识别结果，非已驳回）
     */
    private void enrichMetadataListFlags(List<GovMetadata> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream()
                .map(GovMetadata::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Set<Long> sensitiveByColumn = new HashSet<>();
        if (!ids.isEmpty()) {
            LambdaQueryWrapper<GovMetadataColumn> cw = new LambdaQueryWrapper<>();
            cw.in(GovMetadataColumn::getMetadataId, ids).eq(GovMetadataColumn::getIsSensitive, true);
            List<GovMetadataColumn> cols = columnMapper.selectList(cw);
            sensitiveByColumn = cols.stream()
                    .map(GovMetadataColumn::getMetadataId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        Set<String> sensitiveTableKeys = new HashSet<>();
        Set<String> uniquePairs = records.stream()
                .filter(m -> m.getDsId() != null && StringUtils.hasText(m.getTableName()))
                .map(m -> m.getDsId() + "\u0001" + m.getTableName())
                .collect(Collectors.toCollection(HashSet::new));
        for (String key : uniquePairs) {
            int sep = key.indexOf('\u0001');
            if (sep <= 0 || sep == key.length() - 1) {
                continue;
            }
            try {
                Long dsId = Long.parseLong(key.substring(0, sep));
                String tableName = key.substring(sep + 1);
                LambdaQueryWrapper<SecColumnSensitivity> sw = new LambdaQueryWrapper<>();
                sw.eq(SecColumnSensitivity::getDsId, dsId)
                        .eq(SecColumnSensitivity::getTableName, tableName)
                        .and(o -> o.isNull(SecColumnSensitivity::getReviewStatus)
                                .or()
                                .ne(SecColumnSensitivity::getReviewStatus, "REJECTED"));
                if (secColumnSensitivityMapper.selectCount(sw) > 0) {
                    sensitiveTableKeys.add(key);
                }
            } catch (NumberFormatException ignored) {
                // skip malformed key
            }
        }

        for (GovMetadata m : records) {
            boolean byCol = m.getId() != null && sensitiveByColumn.contains(m.getId());
            String pairKey = m.getDsId() != null && StringUtils.hasText(m.getTableName())
                    ? m.getDsId() + "\u0001" + m.getTableName()
                    : null;
            boolean bySec = pairKey != null && sensitiveTableKeys.contains(pairKey);
            m.setIsSensitive(byCol || bySec);
        }
    }

    private MetadataScanContext buildContext(MetadataScanRequest request) {
        DqDatasource datasource = datasourceMapper.selectById(request.getDsId());
        if (datasource == null || datasource.getStatus() != 1) {
            throw new BusinessException(2001, "数据源不存在或未启用");
        }

        DataSourceAdapter adapter = adapterRegistry.getAdapterById(request.getDsId());
        if (adapter == null) {
            throw new BusinessException(2001, "数据源适配器未找到，请检查数据源配置");
        }

        String scope = request.getScanScope() != null ? request.getScanScope().trim() : "ALL";
        if ("SPECIFIED".equalsIgnoreCase(scope)) {
            if (request.getTableNames() == null || request.getTableNames().isEmpty()) {
                throw new BusinessException(400, "选择「指定表」时请至少选择一个表");
            }
            List<String> trimmed = request.getTableNames().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
            if (trimmed.isEmpty()) {
                throw new BusinessException(400, "选择「指定表」时请至少选择一个表");
            }
            request.setTableNames(trimmed);
        }
        if ("PATTERN".equalsIgnoreCase(scope)) {
            if (request.getTablePattern() == null || request.getTablePattern().isBlank()) {
                throw new BusinessException(400, "选择「按模式匹配」时请填写匹配模式");
            }
        }

        return MetadataScanContext.builder()
                .taskId(TASK_PREFIX + System.currentTimeMillis())
                .datasource(datasource)
                .adapter(adapter)
                .scanScope(scope)
                .tableNames(request.getTableNames())
                .tablePattern(request.getTablePattern())
                .dataLayer(request.getDataLayer())
                .dataDomain(request.getDataDomain())
                .bizDomain(request.getBizDomain())
                .collectStats(Boolean.TRUE.equals(request.getCollectStats()))
                .collectColumnStats(Boolean.TRUE.equals(request.getCollectColumnStats()))
                .overwriteExisting(Boolean.TRUE.equals(request.getOverwriteExisting()))
                .createUser(request.getCreateUser())
                .build();
    }

    private MetadataScanner resolveScanner(String dsType) {
        if (dsType == null) {
            return null;
        }
        return switch (dsType.toUpperCase()) {
            case "MYSQL", "TIDB" -> mysqlScanner;
            case "SQLSERVER" -> sqlServerScanner;
            case "ORACLE" -> oracleScanner;
            case "POSTGRESQL" -> postgresScanner;
            default -> null;
        };
    }

    private MetadataScanResult buildResult(MetadataScanContext ctx) {
        List<TableScanError> errors = ctx.getErrors().stream()
                .map(e -> TableScanError.builder()
                        .tableName(e.tableName())
                        .errorMessage(e.message())
                        .exceptionType(e.exceptionType())
                        .build())
                .collect(Collectors.toList());

        List<TableScanSummary> tables = ctx.getMetadataList().stream()
                .map(m -> TableScanSummary.builder()
                        .metadataId(m.getId())
                        .tableName(m.getTableName())
                        .tableType(m.getTableType())
                        .tableComment(m.getTableComment())
                        .rowCount(m.getRowCount())
                        .inserted(ctx.getInsertedCount().get() > 0)
                        .build())
                .collect(Collectors.toList());

        int processed = ctx.getSuccessCount().get() + ctx.getFailedCount().get() + ctx.getSkippedCount().get();
        int planned = ctx.getTotalPlannedTables();

        String ctxStatus = ctx.getStatus();
        String status;
        if ("RUNNING".equals(ctxStatus)) {
            status = "RUNNING";
        } else if ("CANCELLED".equalsIgnoreCase(ctxStatus)) {
            status = "CANCELLED";
        } else if ("FAILED".equals(ctxStatus)) {
            status = "FAILED";
        } else {
            status = "SUCCESS";
            if (ctx.getFailedCount().get() > 0 && ctx.getSuccessCount().get() > 0) {
                status = "PARTIAL_SUCCESS";
            } else if (ctx.getFailedCount().get() > 0) {
                status = "FAILED";
            }
        }

        return MetadataScanResult.builder()
                .taskId(ctx.getTaskId())
                .status(status)
                .dsId(ctx.getDatasource().getId())
                .dsName(ctx.getDatasource().getDsName())
                .totalTables(ctx.getSuccessCount().get() + ctx.getFailedCount().get())
                .plannedTableCount(planned)
                .processedTableCount(processed)
                .successTables(ctx.getSuccessCount().get())
                .failedTables(ctx.getFailedCount().get())
                .insertedTables(ctx.getInsertedCount().get())
                .updatedTables(ctx.getUpdatedCount().get())
                .skippedTables(ctx.getSkippedCount().get())
                .totalColumns(ctx.getTotalColumns().get())
                .elapsedMs(ctx.getElapsedMs())
                .errors(errors)
                .tables(tables)
                .build();
    }
}
