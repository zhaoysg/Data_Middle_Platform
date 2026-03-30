package com.bagdatahouse.dqc.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqcExecutionDTO;
import com.bagdatahouse.core.entity.DqcExecution;
import com.bagdatahouse.core.entity.DqcExecutionDetail;
import com.bagdatahouse.core.entity.DqcPlanRule;
import com.bagdatahouse.core.vo.ExecutionTriggerVO;
import com.bagdatahouse.core.vo.QualityReportVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * DQC质检执行服务接口
 */
public interface DqcExecutionService {

    /**
     * 触发质检执行
     */
    Result<ExecutionTriggerVO> execute(Long planId, String triggerType, Long triggerUser);

    /**
     * 分页查询执行记录
     */
    Result<Page<DqcExecution>> page(Integer pageNum, Integer pageSize, Long planId, String status, String triggerType, String startDate, String endDate);

    /**
     * 根据ID查询执行记录
     */
    Result<DqcExecution> getById(Long id);

    /**
     * 根据执行编号查询
     */
    Result<DqcExecution> getByExecutionNo(String executionNo);

    /**
     * 获取执行明细列表
     */
    Result<List<DqcExecutionDetail>> getExecutionDetails(Long executionId);

    /**
     * 获取执行明细详情
     */
    Result<DqcExecutionDetail> getDetailById(Long id);

    /**
     * 获取质检方案报告
     */
    Result<QualityReportVO> getReport(Long planId, String startDate, String endDate);

    /**
     * 获取整体质量报告
     */
    Result<QualityReportVO> getOverallReport();

    /**
     * 导出质检报告
     */
    Result<byte[]> exportReport(Long executionId);

    /**
     * 异步执行所有规则（供内部通过代理调用，触发 @Async）
     * @param executionId 执行记录ID
     * @param planRules    方案关联的规则列表
     * @param bindType     方案绑定类型：TABLE / LAYER / PATTERN
     * @param bindValue    方案绑定值 JSON（含 dsId / tables / layer）
     */
    void executeRulesAsync(Long executionId, List<DqcPlanRule> planRules,
                          String bindType, String bindValue);
}
