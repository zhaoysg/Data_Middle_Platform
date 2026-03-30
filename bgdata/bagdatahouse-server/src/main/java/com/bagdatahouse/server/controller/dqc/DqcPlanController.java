package com.bagdatahouse.server.controller.dqc;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqcPlanDTO;
import com.bagdatahouse.core.entity.DqcPlan;
import com.bagdatahouse.core.vo.PlanRuleBindVO;
import com.bagdatahouse.core.vo.PlanExecutionStatusVO;
import com.bagdatahouse.core.vo.CronValidationVO;
import com.bagdatahouse.dqc.service.DqcPlanService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DQC质检方案控制器
 *
 * 支持三种触发方式：
 * - MANUAL（手动触发）：通过前端手动触发
 * - SCHEDULE（定时触发）：通过定时器自动触发
 * - API（接口触发）：通过REST API触发
 */
@Api(tags = "数据质量-质检方案管理")
@RestController
@RequestMapping("/dqc/plan")
public class DqcPlanController {

    private static final Logger log = LoggerFactory.getLogger(DqcPlanController.class);

    @Autowired
    private DqcPlanService planService;

    @GetMapping("/page")
    @ApiOperation("分页查询质检方案")
    public Result<Page<DqcPlan>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String planName,
            @RequestParam(required = false) String layerCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String triggerType
    ) {
        return planService.page(pageNum, pageSize, planName, layerCode, status, triggerType);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取质检方案详情")
    public Result<DqcPlan> getById(
            @ApiParam("方案ID") @PathVariable Long id
    ) {
        return planService.getById(id);
    }

    @PostMapping
    @ApiOperation("新增质检方案")
    public Result<Long> create(@RequestBody DqcPlanDTO dto) {
        return planService.create(dto);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新质检方案")
    public Result<Void> update(
            @ApiParam("方案ID") @PathVariable Long id,
            @RequestBody DqcPlanDTO dto
    ) {
        return planService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除质检方案")
    public Result<Void> delete(
            @ApiParam("方案ID") @PathVariable Long id
    ) {
        return planService.delete(id);
    }

    @PostMapping("/{id}/publish")
    @ApiOperation("发布质检方案")
    public Result<Void> publish(
            @ApiParam("方案ID") @PathVariable Long id
    ) {
        return planService.publish(id);
    }

    @PostMapping("/{id}/disable")
    @ApiOperation("禁用质检方案")
    public Result<Void> disable(
            @ApiParam("方案ID") @PathVariable Long id
    ) {
        return planService.disable(id);
    }

    @GetMapping("/{id}/rules")
    @ApiOperation("获取方案绑定的规则")
    public Result<List<PlanRuleBindVO>> getBoundRules(
            @ApiParam("方案ID") @PathVariable Long id
    ) {
        return planService.getBoundRules(id);
    }

    @PostMapping("/{id}/rules")
    @ApiOperation("绑定规则到方案")
    public Result<Void> bindRules(
            @ApiParam("方案ID") @PathVariable Long id,
            @RequestBody List<PlanRuleBindVO> rules
    ) {
        return planService.bindRules(id, rules);
    }

    @DeleteMapping("/{planId}/rules/{ruleId}")
    @ApiOperation("解除规则绑定")
    public Result<Void> unbindRule(
            @ApiParam("方案ID") @PathVariable Long planId,
            @ApiParam("规则ID") @PathVariable Long ruleId
    ) {
        return planService.unbindRule(planId, ruleId);
    }

    @GetMapping("/overview")
    @ApiOperation("获取质检概览统计")
    public Result<Map<String, Object>> getOverview() {
        return planService.getOverview();
    }

    // ==================== 三种触发方式相关接口 ====================

    /**
     * 手动触发执行
     */
    @PostMapping("/{id}/execute")
    @ApiOperation("手动执行质检方案")
    public Result<Long> execute(
            @ApiParam("方案ID") @PathVariable Long id
    ) {
        return planService.execute(id);
    }

    /**
     * API触发执行
     * 适用于自动化流水线集成
     */
    @PostMapping("/{id}/trigger")
    @ApiOperation("API触发执行质检方案")
    public Result<Long> triggerByApi(
            @ApiParam("方案ID") @PathVariable Long id,
            @ApiParam("触发参数（可选）") @RequestParam(required = false) String triggerParams
    ) {
        return planService.triggerByApi(id, triggerParams);
    }

    /**
     * 获取方案的下次执行时间
     */
    @GetMapping("/{id}/next-execution")
    @ApiOperation("获取方案的下次执行时间")
    public Result<LocalDateTime> getNextExecutionTime(
            @ApiParam("方案ID") @PathVariable Long id
    ) {
        return planService.getNextExecutionTime(id);
    }

    /**
     * 获取方案的执行状态
     */
    @GetMapping("/{id}/execution-status")
    @ApiOperation("获取方案的执行状态")
    public Result<PlanExecutionStatusVO> getExecutionStatus(
            @ApiParam("方案ID") @PathVariable Long id
    ) {
        return planService.getExecutionStatus(id);
    }

    /**
     * 验证Cron表达式
     */
    @GetMapping("/validate-cron")
    @ApiOperation("验证Cron表达式是否有效")
    public Result<CronValidationVO> validateCron(
            @ApiParam("Cron表达式") @RequestParam String cronExpression
    ) {
        return planService.validateCron(cronExpression);
    }

    /**
     * 取消正在执行的方案
     */
    @PostMapping("/{id}/cancel")
    @ApiOperation("取消正在执行的方案")
    public Result<Boolean> cancelExecution(
            @ApiParam("方案ID") @PathVariable Long id
    ) {
        return planService.cancelExecution(id);
    }
}
