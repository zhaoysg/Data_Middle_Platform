package com.bagdatahouse.server.controller.monitor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.MonitorTaskExecution;
import com.bagdatahouse.core.vo.MonitorDashboardVO;
import com.bagdatahouse.monitor.service.MonitorDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Monitor dashboard controller
 * 任务执行监控大盘
 */
@Api(tags = "数据监控-任务执行监控大盘")
@RestController
@RequestMapping("/monitor/dashboard")
public class MonitorDashboardController {

    private static final Logger log = LoggerFactory.getLogger(MonitorDashboardController.class);

    @Autowired
    private MonitorDashboardService dashboardService;

    @GetMapping("/overview")
    @ApiOperation("获取监控大盘概览统计")
    public Result<MonitorDashboardVO> getOverview() {
        return dashboardService.getDashboardOverview();
    }

    @GetMapping("/execution/page")
    @ApiOperation("分页查询任务执行记录")
    public Result<IPage<MonitorTaskExecution>> pageExecutions(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("任务ID") @RequestParam(required = false) Long taskId,
            @ApiParam("任务类型") @RequestParam(required = false) String taskType,
            @ApiParam("执行状态") @RequestParam(required = false) String status,
            @ApiParam("触发方式") @RequestParam(required = false) String triggerType,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return dashboardService.pageExecutions(
                pageNum, pageSize, taskId, taskType, status, triggerType, startDate, endDate);
    }

    @GetMapping("/execution/{id}")
    @ApiOperation("获取执行记录详情")
    public Result<MonitorTaskExecution> getExecutionById(
            @ApiParam("执行记录ID") @PathVariable Long id
    ) {
        return dashboardService.getExecutionById(id);
    }

    @GetMapping("/execution/running")
    @ApiOperation("获取正在运行中的任务")
    public Result<List<Map<String, Object>>> getRunningExecutions() {
        return dashboardService.getRunningExecutions();
    }

    @PostMapping("/execution/{id}/cancel")
    @ApiOperation("取消运行中的任务")
    public Result<Boolean> cancelExecution(
            @ApiParam("执行记录ID") @PathVariable Long id
    ) {
        return dashboardService.cancelExecution(id);
    }

    @PostMapping("/execution/{id}/retry")
    @ApiOperation("重试失败的任务")
    public Result<Long> retryExecution(
            @ApiParam("执行记录ID") @PathVariable Long id
    ) {
        return dashboardService.retryExecution(id);
    }

    @GetMapping("/execution/trend")
    @ApiOperation("获取执行趋势数据")
    public Result<Map<String, Object>> getExecutionTrend(
            @ApiParam("统计天数") @RequestParam(required = false, defaultValue = "7") String days
    ) {
        return dashboardService.getExecutionTrend(days);
    }
}
