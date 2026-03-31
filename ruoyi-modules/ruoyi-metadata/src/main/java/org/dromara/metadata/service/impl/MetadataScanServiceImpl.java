package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.metadata.domain.MetadataColumn;
import org.dromara.metadata.domain.bo.MetadataScanBo;
import org.dromara.metadata.domain.MetadataScanLog;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.domain.vo.MetadataScanLogVo;
import org.dromara.metadata.domain.vo.MetadataScanResultVo;
import org.dromara.metadata.mapper.MetadataScanLogMapper;
import org.dromara.metadata.service.IMetadataColumnService;
import org.dromara.metadata.service.IMetadataScanService;
import org.dromara.metadata.service.IMetadataTableService;
import org.dromara.metadata.support.MetadataScanRuleMatcher;
import org.dromara.datasource.support.DatasourceCryptoSupport;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.datasource.adapter.DataSourceAdapterRegistry;
import org.dromara.datasource.domain.SysDatasource;
import org.dromara.datasource.mapper.SysDatasourceMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 元数据扫描服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class MetadataScanServiceImpl implements IMetadataScanService {

    static final String DEFAULT_TENANT_ID = "000000";

    private final SysDatasourceMapper sysDatasourceMapper;
    private final MetadataScanLogMapper scanLogMapper;
    private final DataSourceAdapterRegistry adapterRegistry;
    private final DatasourceCryptoSupport cryptoSupport;
    private final IMetadataTableService tableService;
    private final IMetadataColumnService columnService;

    @Override
    public MetadataScanResultVo scanByDatasource(MetadataScanBo bo, String tenantId) {
        Long dsId = bo.getDsId();
        // 1. 验证数据源
        SysDatasource ds = sysDatasourceMapper.selectById(dsId);
        if (ds == null) {
            throw new ServiceException("数据源不存在: " + dsId);
        }

        // 2. 解密密码
        String password = cryptoSupport.decryptPassword(ds.getPassword());

        // 3. 获取适配器（不存在则创建）
        DataSourceAdapter adapter = adapterRegistry.getOrCreateAdapter(
            ds.getDsId(), ds.getDsType(), ds.getHost(), ds.getPort(),
            ds.getDatabaseName(), ds.getSchemaName(), ds.getUsername(), password,
            ds.getConnectionParams()
        );

        // 4. 创建扫描记录
        MetadataScanLog scanLog = new MetadataScanLog();
        scanLog.setDsId(ds.getDsId());
        scanLog.setDsName(ds.getDsName());
        scanLog.setDsCode(ds.getDsCode());
        scanLog.setScanType(MetadataScanRuleMatcher.resolveScanMode(bo));
        scanLog.setStatus("RUNNING");
        scanLog.setStartTime(LocalDateTime.now());
        scanLog.setScanUserId(LoginHelper.getUserId());
        scanLog.setTenantId(resolveTenantId(scanLog.getTenantId(), tenantId));
        scanLog.setRemark(buildScanRemark(bo));
        scanLogMapper.insert(scanLog);
        Long logId = scanLog.getId();

        int successCount = 0;
        int partialCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            String schemaName = StringUtils.isNotBlank(bo.getSchemaName())
                ? bo.getSchemaName().trim()
                : StringUtils.isNotBlank(ds.getSchemaName()) ? ds.getSchemaName().trim() : null;
            // 5. 获取表列表
            List<String> allTables = adapter.getTables(schemaName);
            List<String> tablesToScan = MetadataScanRuleMatcher.resolveTables(allTables, bo);
            if (tablesToScan.isEmpty()) {
                throw new ServiceException(buildEmptyScanMessage(bo));
            }

            scanLog.setTotalTables(tablesToScan.size());
            scanLogMapper.updateById(scanLog);

            // 6. 逐表扫描
            for (String tableName : tablesToScan) {
                try {
                    // 6.1 获取表基本信息
                    String tableComment = Optional.ofNullable(adapter.getTableComment(schemaName, tableName)).orElse("");
                    long rowCount = adapter.getRowCount(schemaName, tableName);

                    // 6.2 获取源表最后更新时间
                    Optional<String> updateTimeOpt = adapter.getTableLastUpdateTime(schemaName, tableName);

                    // 6.3 构建并 Upsert 表记录
                    MetadataTable tableRecord = new MetadataTable();
                    tableRecord.setDsId(ds.getDsId());
                    tableRecord.setDsName(ds.getDsName());
                    tableRecord.setDsCode(ds.getDsCode());
                    tableRecord.setTableName(tableName);
                    tableRecord.setTableComment(tableComment);
                    tableRecord.setTableType("TABLE");
                    tableRecord.setDataLayer(ds.getDataLayer());
                    tableRecord.setRowCount(rowCount);
                    tableRecord.setStatus("ACTIVE");
                    tableRecord.setLastScanTime(LocalDateTime.now());
                    tableRecord.setTenantId(resolveTenantId(tableRecord.getTenantId(), tenantId));
                    updateTimeOpt.ifPresent(ut -> {
                        LocalDateTime sourceUpdateTime = parseSourceUpdateTime(ut);
                        if (sourceUpdateTime != null) {
                            tableRecord.setSourceUpdateTime(sourceUpdateTime);
                        } else {
                            log.debug("无法解析源表更新时间: dsId={}, tableName={}, value={}", dsId, tableName, ut);
                        }
                    });

                    tableService.upsert(tableRecord);

                    // 6.4 获取入库后的 tableId
                    MetadataTable saved = tableService.getByDsIdAndTableName(ds.getDsId(), tableName);
                    if (saved == null) {
                        errors.add(tableName + ": 入库失败");
                        failCount++;
                        continue;
                    }
                    Long tableId = saved.getId();

                    // 6.5 同步字段
                    if (!Boolean.FALSE.equals(bo.getSyncColumn())) {
                        try {
                            List<DataSourceAdapter.ColumnInfo> colInfos = adapter.getColumns(schemaName, tableName);
                            List<MetadataColumn> columns = buildColumnRecords(
                                tableId, ds.getDsId(), tableName, colInfos, tenantId);
                            columnService.upsertBatch(tableId, ds.getDsId(), tableName, columns);
                        } catch (Exception e) {
                            log.warn("扫描表 {} 字段部分失败: {}", tableName, e.getMessage());
                            partialCount++;
                            errors.add(tableName + ": " + e.getMessage());
                            continue;
                        }
                    }

                    successCount++;
                } catch (Exception e) {
                    log.error("扫描表 {} 失败: {}", tableName, e.getMessage());
                    failCount++;
                    errors.add(tableName + ": " + e.getMessage());
                }
            }

            // 7. 更新扫描记录
            LocalDateTime endTime = LocalDateTime.now();
            long elapsedMs = java.time.Duration.between(scanLog.getStartTime(), endTime).toMillis();

            String finalStatus = resolveFinalStatus(tablesToScan.size(), successCount, partialCount);

            scanLog.setStatus(finalStatus);
            scanLog.setSuccessCount(successCount);
            scanLog.setPartialCount(partialCount);
            scanLog.setFailedCount(failCount);
            scanLog.setErrorDetail(errors.isEmpty() ? null : String.join("; ", errors));
            scanLog.setEndTime(endTime);
            scanLog.setElapsedMs(elapsedMs);
            scanLogMapper.updateById(scanLog);

            // 8. 构建返回结果
            MetadataScanResultVo result = new MetadataScanResultVo();
            result.setScanLogId(logId);
            result.setDsId(ds.getDsId());
            result.setDsName(ds.getDsName());
            result.setStatus(finalStatus);
            result.setTotalTables(tablesToScan.size());
            result.setSuccessCount(successCount);
            result.setPartialCount(partialCount);
            result.setFailedCount(failCount);
            result.setElapsedMs(elapsedMs);
            result.setStartTime(scanLog.getStartTime());
            result.setEndTime(endTime);
            result.setErrors(errors);

            return result;

        } catch (Exception e) {
            log.error("扫描数据源 {} 失败: {}", dsId, e.getMessage());
            scanLog.setStatus("FAILED");
            scanLog.setErrorDetail(e.getMessage());
            scanLog.setEndTime(LocalDateTime.now());
            scanLogMapper.updateById(scanLog);
            throw new RuntimeException("扫描失败: " + e.getMessage(), e);
        }
    }

    private List<MetadataColumn> buildColumnRecords(
            Long tableId, Long dsId, String tableName,
            List<DataSourceAdapter.ColumnInfo> colInfos, String tenantId) {

        List<MetadataColumn> columns = new ArrayList<>();
        int order = 1;
        for (DataSourceAdapter.ColumnInfo ci : colInfos) {
            MetadataColumn col = new MetadataColumn();
            col.setTableId(tableId);
            col.setDsId(dsId);
            col.setTableName(tableName);
            col.setColumnName(ci.columnName());
            col.setColumnComment(ci.columnComment() != null ? ci.columnComment() : "");
            col.setDataType(ci.dataType() != null ? ci.dataType() : "");
            col.setIsNullable(ci.nullable() ? "YES" : "NO");
            col.setIsPrimaryKey(ci.primaryKey());
            col.setSortOrder(order++);
            col.setDelFlag("0");
            col.setTenantId(resolveTenantId(col.getTenantId(), tenantId));
            columns.add(col);
        }
        return columns;
    }

    @Override
    public List<MetadataScanLogVo> listScanLogs(Long dsId) {
        var wrapper = Wrappers.<MetadataScanLog>lambdaQuery()
            .eq(ObjectUtil.isNotNull(dsId), MetadataScanLog::getDsId, dsId)
            .orderByDesc(MetadataScanLog::getStartTime);
        return scanLogMapper.selectVoList(wrapper);
    }

    @Override
    public MetadataScanLogVo getScanLog(Long id) {
        return scanLogMapper.selectVoById(id);
    }

    private String buildScanRemark(MetadataScanBo bo) {
        if (bo == null) {
            return null;
        }
        String scanMode = MetadataScanRuleMatcher.resolveScanMode(bo);
        if (!MetadataScanRuleMatcher.MODE_RULE.equals(scanMode)) {
            return null;
        }
        String includePattern = StringUtils.trim(bo.getIncludePattern());
        String excludePattern = StringUtils.trim(bo.getExcludePattern());
        return "规则扫描[" + bo.getRuleType() + "] include=" + StringUtils.blankToDefault(includePattern, "-")
            + ", exclude=" + StringUtils.blankToDefault(excludePattern, "-");
    }

    private String buildEmptyScanMessage(MetadataScanBo bo) {
        String scanMode = MetadataScanRuleMatcher.resolveScanMode(bo);
        if (MetadataScanRuleMatcher.MODE_TABLES.equals(scanMode)) {
            return "未匹配到选中的表，请重新选择";
        }
        if (MetadataScanRuleMatcher.MODE_RULE.equals(scanMode)) {
            return "规则未匹配到任何表，请调整扫描规则";
        }
        return "当前数据源下没有可扫描的表";
    }

    static String resolveTenantId(String entityTenantId, String tenantId, String currentTenantId) {
        if (StringUtils.isNotBlank(entityTenantId)) {
            return entityTenantId.trim();
        }
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId.trim();
        }
        if (StringUtils.isNotBlank(currentTenantId)) {
            return currentTenantId.trim();
        }
        return DEFAULT_TENANT_ID;
    }

    static String resolveFinalStatus(int totalTables, int successCount, int partialCount) {
        if (totalTables <= 0) {
            return "FAILED";
        }
        if (successCount == totalTables && partialCount == 0) {
            return "SUCCESS";
        }
        if (successCount == 0 && partialCount == 0) {
            return "FAILED";
        }
        return "PARTIAL";
    }

    private String resolveTenantId(String entityTenantId, String tenantId) {
        return resolveTenantId(entityTenantId, tenantId, TenantHelper.getTenantId());
    }

    private LocalDateTime parseSourceUpdateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        try {
            return Timestamp.valueOf(text).toLocalDateTime();
        } catch (Exception ignored) {
            // ignore
        }
        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {
            // ignore
        }
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }
}
