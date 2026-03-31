package org.dromara.metadata.service;

import org.dromara.metadata.domain.vo.MetadataScanLogVo;
import org.dromara.metadata.domain.vo.MetadataScanResultVo;

import java.util.List;

/**
 * 元数据扫描日志Service接口
 *
 * @author shaozhengchao
 */
public interface IMetadataScanLogService {

    /**
     * 创建扫描日志
     *
     * @param dsId 数据源ID
     * @param scanType 扫描类型
     * @return 日志ID
     */
    Long createScanLog(Long dsId, String scanType);

    /**
     * 异步执行扫描
     *
     * @param logId 日志ID
     * @param dsId 数据源ID
     * @param syncColumn 是否同步字段
     */
    void executeScanAsync(Long logId, Long dsId, boolean syncColumn);

    /**
     * 查询扫描日志
     *
     * @param id 日志ID
     * @return 扫描日志
     */
    MetadataScanLogVo getScanLog(Long id);

    /**
     * 根据数据源查询扫描日志列表
     *
     * @param dsId 数据源ID
     * @return 扫描日志列表
     */
    List<MetadataScanLogVo> listByDsId(Long dsId);

    /**
     * 执行扫描
     *
     * @param dsId 数据源ID
     * @param syncColumn 是否同步字段
     * @return 扫描结果
     */
    MetadataScanResultVo executeScan(Long dsId, boolean syncColumn);
}
