package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.datasource.domain.SysDatasource;
import org.dromara.datasource.mapper.SysDatasourceMapper;
import org.dromara.metadata.domain.MetadataScanLog;
import org.dromara.metadata.domain.bo.MetadataScanBo;
import org.dromara.metadata.domain.vo.MetadataScanLogVo;
import org.dromara.metadata.domain.vo.MetadataScanResultVo;
import org.dromara.metadata.mapper.MetadataScanLogMapper;
import org.dromara.metadata.service.IMetadataScanLogService;
import org.dromara.metadata.service.IMetadataScanService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 元数据扫描日志Service实现
 *
 * @author shaozhengchao
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class MetadataScanLogServiceImpl implements IMetadataScanLogService {

    private final MetadataScanLogMapper scanLogMapper;
    private final IMetadataScanService scanService;
    private final SysDatasourceMapper sysDatasourceMapper;

    @Override
    public Long createScanLog(Long dsId, String scanType) {
        SysDatasource ds = sysDatasourceMapper.selectById(dsId);
        if (ds == null) {
            throw new IllegalArgumentException("数据源不存在: " + dsId);
        }

        MetadataScanLog scanLog = new MetadataScanLog();
        scanLog.setDsId(ds.getDsId());
        scanLog.setDsName(ds.getDsName());
        scanLog.setDsCode(ds.getDsCode());
        scanLog.setScanType(scanType);
        scanLog.setStatus("RUNNING");
        scanLog.setStartTime(LocalDateTime.now());
        scanLogMapper.insert(scanLog);
        return scanLog.getId();
    }

    @Override
    @Async
    public void executeScanAsync(Long logId, Long dsId, boolean syncColumn) {
        try {
            MetadataScanResultVo result = scanService.scanByDatasource(buildScanBo(dsId, syncColumn), null);

            // 更新日志状态
            MetadataScanLog scanLog = scanLogMapper.selectById(logId);
            if (scanLog != null) {
                scanLog.setStatus(result.getStatus());
                scanLog.setTotalTables(result.getTotalTables());
                scanLog.setSuccessCount(result.getSuccessCount());
                scanLog.setPartialCount(result.getPartialCount());
                scanLog.setFailedCount(result.getFailedCount());
                scanLog.setErrorDetail(result.getErrors() != null && !result.getErrors().isEmpty()
                    ? String.join("; ", result.getErrors()) : null);
                scanLog.setEndTime(result.getEndTime());
                scanLog.setElapsedMs(result.getElapsedMs());
                scanLogMapper.updateById(scanLog);
            }
        } catch (Exception e) {
            log.error("异步扫描失败: {}", e.getMessage(), e);
            MetadataScanLog scanLog = scanLogMapper.selectById(logId);
            if (scanLog != null) {
                scanLog.setStatus("FAILED");
                scanLog.setErrorDetail(e.getMessage());
                scanLog.setEndTime(LocalDateTime.now());
                scanLogMapper.updateById(scanLog);
            }
        }
    }

    @Override
    public MetadataScanLogVo getScanLog(Long id) {
        return scanLogMapper.selectVoById(id);
    }

    @Override
    public List<MetadataScanLogVo> listByDsId(Long dsId) {
        LambdaQueryWrapper<MetadataScanLog> lqw = Wrappers.lambdaQuery();
        lqw.eq(MetadataScanLog::getDsId, dsId);
        lqw.orderByDesc(MetadataScanLog::getStartTime);
        return scanLogMapper.selectVoList(lqw);
    }

    @Override
    public MetadataScanResultVo executeScan(Long dsId, boolean syncColumn) {
        return scanService.scanByDatasource(buildScanBo(dsId, syncColumn), null);
    }

    private MetadataScanBo buildScanBo(Long dsId, boolean syncColumn) {
        MetadataScanBo bo = new MetadataScanBo();
        bo.setDsId(dsId);
        bo.setSyncColumn(syncColumn);
        return bo;
    }
}
