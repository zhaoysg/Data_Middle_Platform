package com.bagdatahouse.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.MonitorTaskExecution;
import com.bagdatahouse.core.mapper.MonitorTaskExecutionMapper;
import com.bagdatahouse.core.vo.MonitorDashboardVO;
import com.bagdatahouse.monitor.service.MonitorDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Monitor dashboard service implementation
 */
@Service
public class MonitorDashboardServiceImpl extends ServiceImpl<MonitorTaskExecutionMapper, MonitorTaskExecution>
        implements MonitorDashboardService {

    private static final Logger log = LoggerFactory.getLogger(MonitorDashboardServiceImpl.class);

    @Autowired
    private MonitorTaskExecutionMapper executionMapper;

    @Override
    public Result<MonitorDashboardVO> getDashboardOverview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);

        // 今日统计（使用自定义方法，避免 SELECT 不存在的列）
        List<MonitorTaskExecution> todayExecutions = executionMapper.selectForToday(todayStart, todayEnd);

        int todayTotal = todayExecutions.size();
        int todaySuccess = (int) todayExecutions.stream()
                .filter(e -> "SUCCESS".equals(e.getStatus())).count();
        int todayFailed = (int) todayExecutions.stream()
                .filter(e -> "FAILED".equals(e.getStatus())).count();
        int runningCount = (int) todayExecutions.stream()
                .filter(e -> "RUNNING".equals(e.getStatus())).count();

        BigDecimal successRateToday = todayTotal > 0
                ? BigDecimal.valueOf(100.0 * todaySuccess / todayTotal).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long avgElapsedMs = (long) todayExecutions.stream()
                .filter(e -> e.getElapsedMs() != null)
                .mapToLong(MonitorTaskExecution::getElapsedMs)
                .average()
                .orElse(0.0);

        // 昨日统计（使用自定义方法）
        List<MonitorTaskExecution> yesterdayExecutions = executionMapper.selectByTimeRange(yesterdayStart, todayStart);
        int yesterdayTotal = yesterdayExecutions.size();
        int yesterdaySuccess = (int) yesterdayExecutions.stream()
                .filter(e -> "SUCCESS".equals(e.getStatus())).count();
        BigDecimal yesterdayRate = yesterdayTotal > 0
                ? BigDecimal.valueOf(100.0 * yesterdaySuccess / yesterdayTotal).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal successRateChange = successRateToday.subtract(yesterdayRate)
                .setScale(2, RoundingMode.HALF_UP);

        // 近7天统计（使用自定义方法）
        LocalDateTime sevenDaysAgo = todayStart.minusDays(7);
        List<MonitorTaskExecution> weekExecutions = executionMapper.selectByTimeRange(sevenDaysAgo, todayEnd);
        int weekTotal = weekExecutions.size();
        int weekSuccess = (int) weekExecutions.stream()
                .filter(e -> "SUCCESS".equals(e.getStatus())).count();
        BigDecimal avgSuccessRate7d = weekTotal > 0
                ? BigDecimal.valueOf(100.0 * weekSuccess / weekTotal).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 近7天趋势（按天分组）
        List<MonitorDashboardVO.TrendPoint> successRateTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = now.toLocalDate().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            List<MonitorTaskExecution> dayExecs = weekExecutions.stream()
                    .filter(e -> e.getCreateTime() != null
                            && !e.getCreateTime().isBefore(dayStart)
                            && e.getCreateTime().isBefore(dayEnd))
                    .collect(Collectors.toList());

            int dayTotal = dayExecs.size();
            int daySuccess = (int) dayExecs.stream()
                    .filter(e -> "SUCCESS".equals(e.getStatus())).count();
            int dayFailed = (int) dayExecs.stream()
                    .filter(e -> "FAILED".equals(e.getStatus())).count();
            BigDecimal dayRate = dayTotal > 0
                    ? BigDecimal.valueOf(100.0 * daySuccess / dayTotal).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            long dayAvgMs = (long) dayExecs.stream()
                    .filter(e -> e.getElapsedMs() != null)
                    .mapToLong(MonitorTaskExecution::getElapsedMs)
                    .average()
                    .orElse(0.0);

            successRateTrend.add(MonitorDashboardVO.TrendPoint.builder()
                    .date(date.toString())
                    .total(dayTotal)
                    .success(daySuccess)
                    .failed(dayFailed)
                    .successRate(dayRate)
                    .avgElapsedMs(dayAvgMs)
                    .build());
        }

        // 今日每小时趋势
        Map<String, Integer> hourlyTrend = new LinkedHashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            final int currentHour = hour;
            String hourKey = String.format("%02d:00", hour);
            int hourCount = (int) todayExecutions.stream()
                    .filter(e -> e.getCreateTime() != null
                            && e.getCreateTime().getHour() == currentHour)
                    .count();
            hourlyTrend.put(hourKey, hourCount);
        }

        // 任务类型分布
        Map<String, Integer> taskTypeDistribution = new LinkedHashMap<>();
        todayExecutions.stream()
                .filter(e -> e.getTaskType() != null)
                .collect(Collectors.groupingBy(MonitorTaskExecution::getTaskType, Collectors.counting()))
                .forEach((type, count) -> taskTypeDistribution.put(type, count.intValue()));

        // 最近10条执行记录
        List<MonitorTaskExecution> recentList = executionMapper.selectRecent(10);
        List<MonitorDashboardVO.TaskExecutionSummary> recentExecutions = recentList.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        // 失败次数最多的Top5任务
        List<MonitorTaskExecution> failedList = executionMapper.selectRecentByStatus(sevenDaysAgo, "FAILED", 10);
        List<MonitorDashboardVO.TaskExecutionSummary> topFailedTasks = failedList.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        // 待处理告警数
        LambdaQueryWrapper<MonitorTaskExecution> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.ge(MonitorTaskExecution::getCreateTime, todayStart)
                .lt(MonitorTaskExecution::getCreateTime, todayEnd)
                .eq(MonitorTaskExecution::getStatus, "FAILED");
        long pendingAlerts = executionMapper.selectCount(pendingWrapper);

        MonitorDashboardVO vo = MonitorDashboardVO.builder()
                .todayExecutions(todayTotal)
                .todaySuccess(todaySuccess)
                .todayFailed(todayFailed)
                .runningCount(runningCount)
                .avgElapsedMs(avgElapsedMs)
                .avgSuccessRate7d(avgSuccessRate7d)
                .pendingAlerts((int) pendingAlerts)
                .successRateToday(successRateToday)
                .successRateChange(successRateChange)
                .hourlyTrend(hourlyTrend)
                .taskTypeDistribution(taskTypeDistribution)
                .successRateTrend(successRateTrend)
                .recentExecutions(recentExecutions)
                .topFailedTasks(topFailedTasks)
                .build();

        return Result.success(vo);
    }

    @Override
    public Result<IPage<MonitorTaskExecution>> pageExecutions(
            Integer pageNum, Integer pageSize,
            Long taskId, String taskType, String status,
            String triggerType, String startDate, String endDate) {

        Page<MonitorTaskExecution> page = new Page<>(pageNum, pageSize);
        IPage<MonitorTaskExecution> iResult = executionMapper.selectExecutionPage(
                page, taskId, taskType, status, triggerType, startDate, endDate);

        // 补充展示名称
        for (MonitorTaskExecution record : iResult.getRecords()) {
            enrichDisplayNames(record);
        }

        return Result.success(iResult);
    }

    @Override
    public Result<MonitorTaskExecution> getExecutionById(Long id) {
        MonitorTaskExecution record = executionMapper.selectById(id);
        if (record == null) {
            return Result.fail(404, "执行记录不存在");
        }
        enrichDisplayNames(record);
        return Result.success(record);
    }

    @Override
    public Result<List<Map<String, Object>>> getRunningExecutions() {
        List<MonitorTaskExecution> runningList = executionMapper.selectByStatus("RUNNING");

        List<Map<String, Object>> result = runningList.stream().map(exec -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", exec.getId());
            item.put("taskName", exec.getTaskName());
            item.put("taskType", exec.getTaskType());
            item.put("taskTypeName", getTaskTypeName(exec.getTaskType()));
            item.put("status", exec.getStatus());
            item.put("progress", exec.getProgress() != null ? exec.getProgress() : 0);
            item.put("startTime", exec.getStartTime() != null
                    ? exec.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            item.put("elapsedMs", exec.getElapsedMs() != null ? exec.getElapsedMs() : 0L);
            long runningTime = exec.getStartTime() != null
                    ? ChronoUnit.MINUTES.between(exec.getStartTime(), LocalDateTime.now())
                    : 0;
            item.put("runningMinutes", runningTime);
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<Boolean> cancelExecution(Long id) {
        MonitorTaskExecution record = executionMapper.selectById(id);
        if (record == null) {
            return Result.fail(404, "执行记录不存在");
        }
        if (!"RUNNING".equals(record.getStatus())) {
            return Result.fail(400, "只能取消运行中的任务");
        }

        record.setStatus("CANCELLED");
        record.setEndTime(LocalDateTime.now());
        record.setElapsedMs(record.getStartTime() != null
                ? ChronoUnit.MILLIS.between(record.getStartTime(), LocalDateTime.now())
                : 0L);
        record.setErrorMsg("用户手动取消");
        executionMapper.updateById(record);

        log.info("任务执行已取消: id={}, taskName={}", id, record.getTaskName());
        return Result.success(true);
    }

    @Override
    public Result<Long> retryExecution(Long id) {
        MonitorTaskExecution record = executionMapper.selectById(id);
        if (record == null) {
            return Result.fail(404, "执行记录不存在");
        }

        // 创建新的执行记录
        MonitorTaskExecution newExec = MonitorTaskExecution.builder()
                .taskId(record.getTaskId())
                .taskName(record.getTaskName())
                .taskType(record.getTaskType())
                .dsId(record.getDsId())
                .planId(record.getPlanId())
                .triggerType("MANUAL")
                .triggerUser(1L)
                .startTime(LocalDateTime.now())
                .status("RUNNING")
                .progress(0)
                .createTime(LocalDateTime.now())
                .build();
        executionMapper.insert(newExec);

        log.info("任务重试已触发: id={}, taskName={}, newExecId={}",
                id, record.getTaskName(), newExec.getId());
        return Result.success(newExec.getId());
    }

    @Override
    public Result<Map<String, Object>> getExecutionTrend(String daysStr) {
        int days = 7;
        if (StringUtils.hasText(daysStr)) {
            try {
                days = Integer.parseInt(daysStr);
                days = Math.max(1, Math.min(days, 30));
            } catch (NumberFormatException e) {
                days = 7;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusDays(days).toLocalDate().atStartOfDay();
        LocalDateTime endTime = now.toLocalDate().plusDays(1).atStartOfDay();

        List<MonitorTaskExecution> records = executionMapper.selectByTimeRange(startTime, endTime);

        // 按天分组统计
        Map<String, Map<String, Integer>> dailyStats = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            String dateKey = now.toLocalDate().minusDays(i).toString();
            dailyStats.put(dateKey, new LinkedHashMap<>() {{
                put("total", 0);
                put("success", 0);
                put("failed", 0);
                put("running", 0);
            }});
        }

        for (MonitorTaskExecution record : records) {
            if (record.getCreateTime() == null) continue;
            String dateKey = record.getCreateTime().toLocalDate().toString();
            if (!dailyStats.containsKey(dateKey)) continue;

            Map<String, Integer> stats = dailyStats.get(dateKey);
            stats.put("total", stats.get("total") + 1);
            switch (record.getStatus()) {
                case "SUCCESS" -> stats.put("success", stats.get("success") + 1);
                case "FAILED" -> stats.put("failed", stats.get("failed") + 1);
                case "RUNNING" -> stats.put("running", stats.get("running") + 1);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", days);
        result.put("dailyStats", dailyStats);
        result.put("totalRecords", records.size());

        return Result.success(result);
    }

    private MonitorDashboardVO.TaskExecutionSummary toSummary(MonitorTaskExecution exec) {
        return MonitorDashboardVO.TaskExecutionSummary.builder()
                .id(exec.getId())
                .taskName(exec.getTaskName())
                .taskType(exec.getTaskType())
                .taskTypeName(getTaskTypeName(exec.getTaskType()))
                .status(exec.getStatus())
                .elapsedMs(exec.getElapsedMs())
                .startTime(exec.getStartTime() != null
                        ? exec.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : null)
                .triggerType(exec.getTriggerType())
                .triggerUserName(exec.getTriggerUserName())
                .errorMsg(exec.getErrorMsg())
                .progress(exec.getProgress())
                .build();
    }

    private void enrichDisplayNames(MonitorTaskExecution record) {
        record.setTaskTypeName(getTaskTypeName(record.getTaskType()));
    }

    private String getTaskTypeName(String taskType) {
        if (taskType == null) return "未知";
        return switch (taskType) {
            case "DQC_PROFILE" -> "质量探查";
            case "DQC_EXECUTION" -> "质检执行";
            case "SCHEDULE" -> "定时任务";
            case "ALERT" -> "告警任务";
            default -> taskType;
        };
    }

    private String getTriggerTypeName(String triggerType) {
        if (triggerType == null) return "未知";
        return switch (triggerType) {
            case "MANUAL" -> "手动触发";
            case "SCHEDULE" -> "定时触发";
            case "API" -> "API触发";
            default -> triggerType;
        };
    }
}
