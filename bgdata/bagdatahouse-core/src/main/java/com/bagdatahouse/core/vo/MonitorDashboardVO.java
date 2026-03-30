package com.bagdatahouse.core.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Monitor dashboard overview VO
 * 监控大盘概览统计数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "Monitor dashboard overview statistics")
public class MonitorDashboardVO {

    @ApiModelProperty("Total task executions today")
    private Integer todayExecutions;

    @ApiModelProperty("Success executions today")
    private Integer todaySuccess;

    @ApiModelProperty("Failed executions today")
    private Integer todayFailed;

    @ApiModelProperty("Running executions currently")
    private Integer runningCount;

    @ApiModelProperty("Average execution time today (ms)")
    private Long avgElapsedMs;

    @ApiModelProperty("Average success rate last 7 days")
    private BigDecimal avgSuccessRate7d;

    @ApiModelProperty("Pending alerts count")
    private Integer pendingAlerts;

    @ApiModelProperty("Success rate today")
    private BigDecimal successRateToday;

    @ApiModelProperty("Success rate change vs yesterday")
    private BigDecimal successRateChange;

    @ApiModelProperty("Today executions trend (hourly counts)")
    private Map<String, Integer> hourlyTrend;

    @ApiModelProperty("Task type distribution")
    private Map<String, Integer> taskTypeDistribution;

    @ApiModelProperty("Last 7 days success rate trend")
    private List<TrendPoint> successRateTrend;

    @ApiModelProperty("Recent executions (last 10)")
    private List<TaskExecutionSummary> recentExecutions;

    @ApiModelProperty("Top failed tasks")
    private List<TaskExecutionSummary> topFailedTasks;

    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private Integer total;
        private Integer success;
        private Integer failed;
        private BigDecimal successRate;
        private Long avgElapsedMs;
    }

    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TaskExecutionSummary {
        private Long id;
        private String taskName;
        private String taskType;
        private String taskTypeName;
        private String status;
        private Long elapsedMs;
        private String startTime;
        private String triggerType;
        private String triggerUserName;
        private String errorMsg;
        private Integer progress;
    }
}
