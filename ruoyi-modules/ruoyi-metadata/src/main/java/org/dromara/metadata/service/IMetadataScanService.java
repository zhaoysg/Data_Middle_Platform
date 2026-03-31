package org.dromara.metadata.service;

import org.dromara.metadata.domain.bo.MetadataScanBo;
import org.dromara.metadata.domain.vo.MetadataScanLogVo;
import org.dromara.metadata.domain.vo.MetadataScanResultVo;

import java.util.List;

/**
 * 元数据扫描服务接口
 */
public interface IMetadataScanService {

    /**
     * 根据数据源扫描元数据表信息
     *
     * @param bo       扫描请求
     * @param tenantId 租户ID（后台线程中 TenantHelper 可能为空，需显式传入）
     * @return 扫描结果
     */
    MetadataScanResultVo scanByDatasource(MetadataScanBo bo, String tenantId);

    /**
     * 根据数据源ID查询扫描历史
     *
     * @param dsId 数据源ID
     * @return 扫描历史列表
     */
    List<MetadataScanLogVo> listScanLogs(Long dsId);

    /**
     * 根据扫描记录ID查询详情
     *
     * @param scanLogId 扫描记录ID
     * @return 扫描记录
     */
    MetadataScanLogVo getScanLog(Long scanLogId);
}
