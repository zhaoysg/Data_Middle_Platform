package com.bagdatahouse.server.controller.dqc;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqcRuleTemplateDTO;
import com.bagdatahouse.core.entity.DqcRuleTemplate;
import com.bagdatahouse.dqc.service.DqcRuleTemplateService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * DQC规则模板控制器
 */
@Api(tags = "数据质量-规则模板管理")
@RestController
@RequestMapping("/dqc/rule-template")
public class DqcRuleTemplateController {

    private static final Logger log = LoggerFactory.getLogger(DqcRuleTemplateController.class);

    @Autowired
    private DqcRuleTemplateService ruleTemplateService;

    @GetMapping("/page")
    @ApiOperation("分页查询规则模板")
    public Result<Page<DqcRuleTemplate>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String applyLevel,
            @RequestParam(required = false) Integer builtin
    ) {
        return ruleTemplateService.page(pageNum, pageSize, templateName, ruleType, applyLevel, builtin);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取规则模板详情")
    public Result<DqcRuleTemplate> getById(
            @ApiParam("模板ID") @PathVariable Long id
    ) {
        return ruleTemplateService.getById(id);
    }

    @PostMapping
    @ApiOperation("新增规则模板")
    public Result<Long> create(@RequestBody DqcRuleTemplateDTO dto) {
        return ruleTemplateService.create(dto);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新规则模板")
    public Result<Void> update(
            @ApiParam("模板ID") @PathVariable Long id,
            @RequestBody DqcRuleTemplateDTO dto
    ) {
        return ruleTemplateService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除规则模板")
    public Result<Void> delete(
            @ApiParam("模板ID") @PathVariable Long id
    ) {
        return ruleTemplateService.delete(id);
    }

    @GetMapping("/enabled")
    @ApiOperation("获取启用的规则模板列表")
    public Result<List<DqcRuleTemplate>> listEnabled() {
        return ruleTemplateService.listEnabled();
    }

    @GetMapping("/grouped")
    @ApiOperation("按适用级别分组获取规则模板")
    public Result<Map<String, List<DqcRuleTemplate>>> listGroupedByLevel() {
        return ruleTemplateService.listGroupedByLevel();
    }
}
