package com.bagdatahouse.server.controller.monitor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.MonitorAlertRecord;
import com.bagdatahouse.monitor.service.AlertRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 告警记录控制器
 */
@Api(tags = "数据监控-告警记录")
@RestController
@RequestMapping("/monitor/alert/record")
public class AlertRecordController {

    private static final Logger log = LoggerFactory.getLogger(AlertRecordController.class);

    @Autowired
    private AlertRecordService alertRecordService;

    @GetMapping("/page")
    @ApiOperation("分页查询告警记录")
    public Result<Page<MonitorAlertRecord>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("规则名称") @RequestParam(required = false) String ruleName,
            @ApiParam("告警级别") @RequestParam(required = false) String alertLevel,
            @ApiParam("处理状态") @RequestParam(required = false) String status,
            @ApiParam("目标类型") @RequestParam(required = false) String targetType,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate,
            @ApiParam("规则类型（SENSITIVE_FIELD_SPIKE 等）") @RequestParam(required = false) String ruleType,
            @ApiParam("敏感等级（L1/L2/L3/L4）") @RequestParam(required = false) String sensitivityLevel
    ) {
        return alertRecordService.page(pageNum, pageSize, ruleName, alertLevel, status, targetType, startDate, endDate, ruleType, sensitivityLevel);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取告警记录详情")
    public Result<MonitorAlertRecord> getById(
            @ApiParam("记录ID") @PathVariable Long id
    ) {
        return alertRecordService.getById(id);
    }

    @PostMapping("/{id}/read")
    @ApiOperation("标记告警为已读")
    public Result<Void> markAsRead(
            @ApiParam("记录ID") @PathVariable Long id
    ) {
        Long currentUserId = getCurrentUserId();
        return alertRecordService.markAsRead(id, currentUserId);
    }

    @PostMapping("/batch/read")
    @ApiOperation("批量标记告警为已读")
    public Result<Void> batchMarkAsRead(@RequestBody Long[] ids) {
        Long currentUserId = getCurrentUserId();
        return alertRecordService.batchMarkAsRead(ids, currentUserId);
    }

    @PostMapping("/{id}/resolve")
    @ApiOperation("标记告警为已解决")
    public Result<Void> resolve(
            @ApiParam("记录ID") @PathVariable Long id,
            @ApiParam("解决说明") @RequestParam(required = false) String resolveComment
    ) {
        Long currentUserId = getCurrentUserId();
        return alertRecordService.resolve(id, currentUserId, resolveComment);
    }

    @PostMapping("/batch/resolve")
    @ApiOperation("批量标记告警为已解决")
    public Result<Void> batchResolve(
            @RequestBody Long[] ids,
            @ApiParam("解决说明") @RequestParam(required = false) String resolveComment
    ) {
        Long currentUserId = getCurrentUserId();
        return alertRecordService.batchResolve(ids, currentUserId, resolveComment);
    }

    private Long getCurrentUserId() {
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                return null;
            }
        } catch (Exception ignored) {}
        return 1L;
    }

    @GetMapping("/overview")
    @ApiOperation("获取告警统计概览")
    public Result<Map<String, Object>> getOverview() {
        return alertRecordService.getAlertOverview();
    }

    @GetMapping("/distribution")
    @ApiOperation("获取告警级别分布")
    public Result<Map<String, Object>> getDistribution(
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return alertRecordService.getAlertLevelDistribution(startDate, endDate);
    }

    @GetMapping("/trend")
    @ApiOperation("获取告警趋势")
    public Result<Map<String, Object>> getTrend(
            @ApiParam("统计天数") @RequestParam(required = false, defaultValue = "7") String days
    ) {
        return alertRecordService.getAlertTrend(days);
    }
}
