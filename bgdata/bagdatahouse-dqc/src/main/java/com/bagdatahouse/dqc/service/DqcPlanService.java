package com.bagdatahouse.dqc.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqcPlanDTO;
import com.bagdatahouse.core.entity.DqcPlan;
import com.bagdatahouse.core.vo.PlanRuleBindVO;
import com.bagdatahouse.core.vo.PlanExecutionStatusVO;
import com.bagdatahouse.core.vo.CronValidationVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DQC质检方案服务接口
 *
 * 支持三种触发方式：
 * - MANUAL（手动触发）：通过前端手动触发
 * - SCHEDULE（定时触发）：通过定时器自动触发
 * - API（接口触发）：通过REST API触发
 */
public interface DqcPlanService {

    /**
     * 分页查询质检方案
     */
    Result<Page<DqcPlan>> page(Integer pageNum, Integer pageSize, String planName, String layerCode, String status, String triggerType);

    /**
     * 获取方案详情
     */
    Result<DqcPlan> getById(Long id);

    /**
     * 新增方案
     */
    Result<Long> create(DqcPlanDTO dto);

    /**
     * 更新方案
     */
    Result<Void> update(Long id, DqcPlanDTO dto);

    /**
     * 删除方案
     */
    Result<Void> delete(Long id);

    /**
     * 发布方案
     */
    Result<Void> publish(Long id);

    /**
     * 禁用方案
     */
    Result<Void> disable(Long id);

    /**
     * 绑定规则到方案
     */
    Result<Void> bindRules(Long planId, List<PlanRuleBindVO> rules);

    /**
     * 获取方案绑定的规则列表
     */
    Result<List<PlanRuleBindVO>> getBoundRules(Long planId);

    /**
     * 解除规则绑定
     */
    Result<Void> unbindRule(Long planId, Long ruleId);

    /**
     * 执行质检方案（手动触发）
     */
    Result<Long> execute(Long planId);

    /**
     * 获取质检概览统计
     */
    Result<Map<String, Object>> getOverview();

    // ==================== 新增：支持三种触发方式的方法 ====================

    /**
     * API触发执行质检方案
     * 支持通过接口调用触发，适用于自动化流水线集成
     *
     * @param planId 方案ID
     * @param triggerParams 触发参数（可选）
     * @return 执行记录ID
     */
    Result<Long> triggerByApi(Long planId, String triggerParams);

    /**
     * 获取方案的下次执行时间
     * 根据Cron表达式计算最近一次执行时间点
     *
     * @param planId 方案ID
     * @return 下次执行时间
     */
    Result<LocalDateTime> getNextExecutionTime(Long planId);

    /**
     * 获取方案的执行状态
     * 包括当前是否在执行、上次执行状态、下次执行时间等
     *
     * @param planId 方案ID
     * @return 执行状态信息
     */
    Result<PlanExecutionStatusVO> getExecutionStatus(Long planId);

    /**
     * 验证Cron表达式是否有效
     *
     * @param cronExpression Cron表达式
     * @return 验证结果
     */
    Result<CronValidationVO> validateCron(String cronExpression);

    /**
     * 取消正在执行的方案
     *
     * @param planId 方案ID
     * @return 是否成功取消
     */
    Result<Boolean> cancelExecution(Long planId);
}
