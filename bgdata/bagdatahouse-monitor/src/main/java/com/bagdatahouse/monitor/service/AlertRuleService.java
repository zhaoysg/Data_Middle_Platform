package com.bagdatahouse.monitor.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.MonitorAlertRuleDTO;
import com.bagdatahouse.core.entity.MonitorAlertRule;

import java.util.List;
import java.util.Map;

/**
 * 告警规则服务接口
 */
public interface AlertRuleService extends IService<MonitorAlertRule> {

    /**
     * 分页查询告警规则
     */
    Result<Page<MonitorAlertRule>> page(
            Integer pageNum,
            Integer pageSize,
            String ruleName,
            String ruleType,
            String targetType,
            String alertLevel,
            Integer enabled
    );

    /**
     * 根据ID查询
     */
    Result<MonitorAlertRule> getById(Long id);

    /**
     * 新增规则
     */
    Result<Long> create(MonitorAlertRuleDTO dto);

    /**
     * 更新规则
     */
    Result<Void> update(Long id, MonitorAlertRuleDTO dto);

    /**
     * 删除规则
     */
    Result<Void> delete(Long id);

    /**
     * 启用/禁用规则
     */
    Result<Void> toggleEnabled(Long id);

    /**
     * 获取规则类型选项
     */
    Result<Map<String, Object>> getRuleTypeOptions();

    /**
     * 获取目标类型选项
     */
    Result<Map<String, Object>> getTargetTypeOptions();

    /**
     * 根据类型获取目标列表（如数据源列表、规则列表）
     */
    Result<Map<String, Object>> getTargetListByType(String targetType);

    // ==================== SENSITIVE 类型告警规则 ====================

    /**
     * 新建 SENSITIVE 类型告警规则
     */
    Result<Long> createSensityAlertRule(MonitorAlertRuleDTO dto);

    /**
     * 更新 SENSITIVE 类型告警规则
     */
    Result<Void> updateSensityAlertRule(Long id, MonitorAlertRuleDTO dto);

    /**
     * 按敏感等级查询 SENSITIVE 告警规则
     */
    Result<List<MonitorAlertRule>> getSensityAlertRulesByLevel(String sensitivityLevel);

    /**
     * 按数据源ID查询 SENSITIVE 告警规则
     */
    Result<List<MonitorAlertRule>> getSensityAlertRulesByDsId(Long dsId);
}
