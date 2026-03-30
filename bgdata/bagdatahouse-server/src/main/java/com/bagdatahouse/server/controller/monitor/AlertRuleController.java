package com.bagdatahouse.server.controller.monitor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.MonitorAlertRuleDTO;
import com.bagdatahouse.core.entity.MonitorAlertRule;
import com.bagdatahouse.monitor.service.AlertRuleService;
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
 * 告警规则管理控制器
 */
@Api(tags = "数据监控-告警规则管理")
@RestController
@RequestMapping("/monitor/alert/rule")
public class AlertRuleController {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleController.class);

    @Autowired
    private AlertRuleService alertRuleService;

    @GetMapping("/page")
    @ApiOperation("分页查询告警规则")
    public Result<Page<MonitorAlertRule>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("规则名称") @RequestParam(required = false) String ruleName,
            @ApiParam("规则类型") @RequestParam(required = false) String ruleType,
            @ApiParam("目标类型") @RequestParam(required = false) String targetType,
            @ApiParam("告警级别") @RequestParam(required = false) String alertLevel,
            @ApiParam("启用状态") @RequestParam(required = false) Integer enabled
    ) {
        return alertRuleService.page(pageNum, pageSize, ruleName, ruleType, targetType, alertLevel, enabled);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取告警规则详情")
    public Result<MonitorAlertRule> getById(
            @ApiParam("规则ID") @PathVariable Long id
    ) {
        return alertRuleService.getById(id);
    }

    @PostMapping
    @ApiOperation("新增告警规则")
    public Result<Long> create(@RequestBody MonitorAlertRuleDTO dto) {
        return alertRuleService.create(dto);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新告警规则")
    public Result<Void> update(
            @ApiParam("规则ID") @PathVariable Long id,
            @RequestBody MonitorAlertRuleDTO dto
    ) {
        return alertRuleService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除告警规则")
    public Result<Void> delete(
            @ApiParam("规则ID") @PathVariable Long id
    ) {
        return alertRuleService.delete(id);
    }

    @PostMapping("/{id}/toggle")
    @ApiOperation("启用/禁用告警规则")
    public Result<Void> toggleEnabled(
            @ApiParam("规则ID") @PathVariable Long id
    ) {
        return alertRuleService.toggleEnabled(id);
    }

    @PostMapping("/batch/delete")
    @ApiOperation("批量删除告警规则")
    public Result<Void> batchDelete(@RequestBody Long[] ids) {
        for (Long id : ids) {
            alertRuleService.delete(id);
        }
        return Result.success();
    }

    @PostMapping("/batch/toggle")
    @ApiOperation("批量启用/禁用告警规则")
    public Result<Void> batchToggle(@RequestBody Long[] ids) {
        for (Long id : ids) {
            alertRuleService.toggleEnabled(id);
        }
        return Result.success();
    }

    @GetMapping("/options/rule-types")
    @ApiOperation("获取规则类型选项")
    public Result<Map<String, Object>> getRuleTypeOptions() {
        return alertRuleService.getRuleTypeOptions();
    }

    @GetMapping("/options/target-types")
    @ApiOperation("获取目标类型选项")
    public Result<Map<String, Object>> getTargetTypeOptions() {
        return alertRuleService.getTargetTypeOptions();
    }

    @GetMapping("/targets")
    @ApiOperation("根据目标类型获取目标列表")
    public Result<Map<String, Object>> getTargetListByType(
            @ApiParam("目标类型") @RequestParam String targetType
    ) {
        return alertRuleService.getTargetListByType(targetType);
    }

    // ==================== SENSITIVE 类型告警规则 ====================

    @PostMapping("/sensity")
    @ApiOperation("新建 SENSITIVE 类型告警规则")
    public Result<Long> createSensityAlertRule(@RequestBody MonitorAlertRuleDTO dto) {
        return alertRuleService.createSensityAlertRule(dto);
    }

    @PutMapping("/{id}/sensity")
    @ApiOperation("更新 SENSITIVE 类型告警规则")
    public Result<Void> updateSensityAlertRule(
            @ApiParam("规则ID") @PathVariable Long id,
            @RequestBody MonitorAlertRuleDTO dto
    ) {
        return alertRuleService.updateSensityAlertRule(id, dto);
    }

    @GetMapping("/sensity/by-level")
    @ApiOperation("按敏感等级查询 SENSITIVE 告警规则")
    public Result<List<MonitorAlertRule>> getSensityAlertRulesByLevel(
            @ApiParam("敏感等级") @RequestParam(required = false) String sensitivityLevel
    ) {
        return alertRuleService.getSensityAlertRulesByLevel(sensitivityLevel);
    }

    @GetMapping("/sensity/by-ds")
    @ApiOperation("按数据源查询 SENSITIVE 告警规则")
    public Result<List<MonitorAlertRule>> getSensityAlertRulesByDsId(
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId
    ) {
        return alertRuleService.getSensityAlertRulesByDsId(dsId);
    }
}
