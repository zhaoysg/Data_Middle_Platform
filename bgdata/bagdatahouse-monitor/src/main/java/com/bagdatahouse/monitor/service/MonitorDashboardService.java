package com.bagdatahouse.monitor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.MonitorTaskExecution;
import com.bagdatahouse.core.vo.MonitorDashboardVO;

import java.util.List;
import java.util.Map;

/**
 * Monitor dashboard service interface
 */
public interface MonitorDashboardService extends IService<MonitorTaskExecution> {

    /**
     * Get dashboard overview statistics
     */
    Result<MonitorDashboardVO> getDashboardOverview();

    /**
     * Query task execution records with pagination
     */
    Result<IPage<MonitorTaskExecution>> pageExecutions(
            Integer pageNum,
            Integer pageSize,
            Long taskId,
            String taskType,
            String status,
            String triggerType,
            String startDate,
            String endDate
    );

    /**
     * Get execution record by ID
     */
    Result<MonitorTaskExecution> getExecutionById(Long id);

    /**
     * Get real-time status of running executions
     */
    Result<List<Map<String, Object>>> getRunningExecutions();

    /**
     * Cancel a running execution
     */
    Result<Boolean> cancelExecution(Long id);

    /**
     * Retry a failed execution
     */
    Result<Long> retryExecution(Long id);

    /**
     * Get execution trend data
     */
    Result<Map<String, Object>> getExecutionTrend(String days);
}
