package com.bagdatahouse.monitor.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SensityAlertDTO;
import com.bagdatahouse.core.entity.MonitorAlertRecord;

import java.util.List;
import java.util.Map;

/**
 * 告警记录服务接口
 */
public interface AlertRecordService extends IService<MonitorAlertRecord> {

    /**
     * 分页查询告警记录
     */
    Result<Page<MonitorAlertRecord>> page(
            Integer pageNum,
            Integer pageSize,
            String ruleName,
            String alertLevel,
            String status,
            String targetType,
            String startDate,
            String endDate,
            String ruleType,
            String sensitivityLevel
    );

    /**
     * 分页查询告警记录（兼容旧签名）
     */
    Result<Page<MonitorAlertRecord>> page(
            Integer pageNum,
            Integer pageSize,
            String ruleName,
            String alertLevel,
            String status,
            String targetType,
            String startDate,
            String endDate
    );

    /**
     * 根据ID查询
     */
    Result<MonitorAlertRecord> getById(Long id);

    /**
     * 标记为已读
     * @param id 记录ID
     * @param readUser 操作用户ID
     */
    Result<Void> markAsRead(Long id, Long readUser);

    /**
     * 批量标记为已读
     * @param ids 记录ID数组
     * @param readUser 操作用户ID
     */
    Result<Void> batchMarkAsRead(Long[] ids, Long readUser);

    /**
     * 标记为已解决
     * @param id 记录ID
     * @param resolveUser 操作用户ID
     * @param resolveComment 解决说明
     */
    Result<Void> resolve(Long id, Long resolveUser, String resolveComment);

    /**
     * 批量解决
     * @param ids 记录ID数组
     * @param resolveUser 操作用户ID
     * @param resolveComment 解决说明
     */
    Result<Void> batchResolve(Long[] ids, Long resolveUser, String resolveComment);

    /**
     * 获取告警统计概览
     */
    Result<Map<String, Object>> getAlertOverview();

    /**
     * 获取告警级别分布
     */
    Result<Map<String, Object>> getAlertLevelDistribution(String startDate, String endDate);

    /**
     * 获取告警趋势
     */
    Result<Map<String, Object>> getAlertTrend(String days);

    // ==================== SENSITIVE 类型告警记录 ====================

    /**
     * 创建 SENSITIVE 类型告警记录（由 SecurityServiceImpl 扫描/审核触发）
     * @param dto SENSITIVE 告警 DTO
     * @return 告警记录ID
     */
    Result<Long> createSensityAlert(SensityAlertDTO dto);

    /**
     * 批量创建 SENSITIVE 类型告警记录
     * @param alerts SENSITIVE 告警 DTO 列表
     * @return 成功创建数量
     */
    Result<Integer> createSensityAlertBatch(List<SensityAlertDTO> alerts);
}
