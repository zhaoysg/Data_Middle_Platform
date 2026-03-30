package com.bagdatahouse.server.controller.dqc;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqcRuleDefDTO;
import com.bagdatahouse.core.entity.DqcRuleDef;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.bagdatahouse.dqc.service.DqcRuleDefService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DQC规则定义控制器
 */
@Api(tags = "数据质量-规则定义管理")
@RestController
@RequestMapping("/dqc/rule")
public class DqcRuleDefController {

    private static final Logger log = LoggerFactory.getLogger(DqcRuleDefController.class);

    @Autowired
    private DqcRuleDefService ruleDefService;

    @GetMapping("/page")
    @ApiOperation("分页查询规则")
    public Result<Page<DqcRuleDef>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String applyLevel,
            @RequestParam(required = false) Long targetDsId,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) String ruleStrength,
            @RequestParam(required = false) String errorLevel
    ) {
        return ruleDefService.page(pageNum, pageSize, ruleName, ruleType, applyLevel, targetDsId, enabled, ruleStrength, errorLevel);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取规则详情")
    public Result<DqcRuleDef> getById(
            @ApiParam("规则ID") @PathVariable Long id
    ) {
        return ruleDefService.getById(id);
    }

    @PostMapping
    @ApiOperation("新增规则")
    public Result<Long> create(@RequestBody DqcRuleDefDTO dto) {
        return ruleDefService.create(dto);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新规则")
    public Result<Void> update(
            @ApiParam("规则ID") @PathVariable Long id,
            @RequestBody DqcRuleDefDTO dto
    ) {
        return ruleDefService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除规则")
    public Result<Void> delete(
            @ApiParam("规则ID") @PathVariable Long id
    ) {
        return ruleDefService.delete(id);
    }

    @PostMapping("/{id}/toggle")
    @ApiOperation("启用/禁用规则")
    public Result<Void> toggleEnabled(
            @ApiParam("规则ID") @PathVariable Long id
    ) {
        return ruleDefService.toggleEnabled(id);
    }

    @PostMapping("/{id}/copy")
    @ApiOperation("复制规则")
    public Result<Void> copy(
            @ApiParam("规则ID") @PathVariable Long id
    ) {
        return ruleDefService.copy(id);
    }

    @GetMapping("/ds/{dsId}")
    @ApiOperation("根据数据源查询规则")
    public Result<List<DqcRuleDef>> listByDsId(
            @ApiParam("数据源ID") @PathVariable Long dsId
    ) {
        return ruleDefService.listByDsId(dsId);
    }

    @GetMapping("/template/{templateId}")
    @ApiOperation("根据模板查询规则")
    public Result<List<DqcRuleDef>> listByTemplateId(
            @ApiParam("模板ID") @PathVariable Long templateId
    ) {
        return ruleDefService.listByTemplateId(templateId);
    }

    @PostMapping("/{id}/execute")
    @ApiOperation("执行规则")
    public Result<ExecutionResult> execute(
            @ApiParam("规则ID") @PathVariable Long id
    ) {
        return ruleDefService.execute(id);
    }

    @GetMapping("/options")
    @ApiOperation("获取规则类型选项（枚举值）")
    public Result<Map<String, Object>> getRuleTypeOptions() {
        return ruleDefService.getRuleTypeOptions();
    }

    @PostMapping("/validate-expression")
    @ApiOperation("校验规则表达式")
    public Result<String> validateExpression(
            @RequestParam String ruleType,
            @RequestParam(required = false) String ruleExpr,
            @RequestParam(required = false) String targetTable,
            @RequestParam(required = false) String targetColumn,
            @RequestParam(required = false) String regexPattern,
            @RequestParam(required = false) BigDecimal thresholdMin,
            @RequestParam(required = false) BigDecimal thresholdMax
    ) {
        return ruleDefService.validateExpression(ruleType, ruleExpr, targetTable, targetColumn, regexPattern, thresholdMin, thresholdMax);
    }

    @GetMapping("/enabled-with-template")
    @ApiOperation("获取启用的规则列表（带模板信息）")
    public Result<List<Map<String, Object>>> listEnabledWithTemplateInfo() {
        return ruleDefService.listEnabledWithTemplateInfo();
    }

    @GetMapping("/{id}/preview")
    @ApiOperation("预览规则表达式")
    public Result<String> preview(
            @ApiParam("规则ID") @PathVariable Long id,
            @RequestParam(required = false) String table,
            @RequestParam(required = false) String column,
            @RequestParam(required = false) Long dsId
    ) {
        Result<DqcRuleDef> result = ruleDefService.getById(id);
        DqcRuleDef rule = result.getData();
        if (rule == null || rule.getRuleExpr() == null) {
            return Result.success("");
        }

        String expr = rule.getRuleExpr();
        if (table != null) {
            expr = expr.replace("${table}", table);
        }
        if (column != null) {
            expr = expr.replace("${column}", column);
        }
        if (dsId != null) {
            expr = expr.replace("${dsId}", String.valueOf(dsId));
        }

        return Result.success(expr);
    }

    @PostMapping("/batch/delete")
    @ApiOperation("批量删除规则")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            ruleDefService.delete(id);
        }
        return Result.success();
    }

    @PostMapping("/batch/toggle")
    @ApiOperation("批量启用/禁用规则")
    public Result<Void> batchToggle(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            ruleDefService.toggleEnabled(id);
        }
        return Result.success();
    }
}
