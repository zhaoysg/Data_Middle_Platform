package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.MetadataColumn;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.domain.bo.DqcRuleDefBo;
import org.dromara.metadata.domain.bo.DqcRuleDefStatusBo;
import org.dromara.metadata.domain.vo.DqcRuleDefVo;
import org.dromara.metadata.service.IDqcRuleDefService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据质量规则定义Controller
 * <p>
 * 支持元数据驱动的规则配置：
 * - 通过 tableId/columnId 选择目标表/字段
 * - 通过 compareTableId/compareColumnId 选择对比表/字段
 * <p>
 * 数据源信息通过元数据间接获取
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/dqc/rule")
public class DqcRuleDefController extends BaseController {

    private final IDqcRuleDefService ruleDefService;

    /**
     * 查询规则列表
     */
    @SaCheckPermission("metadata:dqc:rule:list")
    @GetMapping("/list")
    public TableDataInfo<DqcRuleDefVo> list(DqcRuleDefBo bo, PageQuery pageQuery) {
        return ruleDefService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取规则详细信息
     */
    @SaCheckPermission("metadata:dqc:rule:query")
    @GetMapping("/{id}")
    public R<DqcRuleDefVo> getInfo(@PathVariable Long id) {
        return R.ok(ruleDefService.queryById(id));
    }

    /**
     * 新增规则
     */
    @SaCheckPermission("metadata:dqc:rule:add")
    @Log(title = "数据质量规则")
    @PostMapping
    public R<Void> add(@Validated @RequestBody DqcRuleDefBo bo) {
        return toAjax(ruleDefService.insertByBo(bo) > 0);
    }

    /**
     * 修改规则
     */
    @SaCheckPermission("metadata:dqc:rule:edit")
    @Log(title = "数据质量规则")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody DqcRuleDefBo bo) {
        return toAjax(ruleDefService.updateByBo(bo));
    }

    /**
     * 仅更新启用状态（列表开关，避免完整 BO 校验导致「规则名称为空」等误报）
     */
    @SaCheckPermission("metadata:dqc:rule:edit")
    @Log(title = "数据质量规则-状态")
    @PutMapping("/status")
    public R<Void> changeStatus(@Validated @RequestBody DqcRuleDefStatusBo bo) {
        return toAjax(ruleDefService.updateEnabled(bo.getId(), bo.getEnabled()) > 0);
    }

    /**
     * 删除规则
     */
    @SaCheckPermission("metadata:dqc:rule:remove")
    @Log(title = "数据质量规则")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        return toAjax(ruleDefService.deleteByIds(ids));
    }

    /**
     * 根据方案ID查询规则
     */
    @SaCheckPermission("metadata:dqc:rule:list")
    @GetMapping("/plan/{planId}")
    public R<List<DqcRuleDefVo>> listByPlanId(@PathVariable Long planId) {
        return R.ok(ruleDefService.listByPlanId(planId));
    }

    /**
     * 绑定规则到方案
     */
    @SaCheckPermission("metadata:dqc:rule:edit")
    @Log(title = "数据质量规则-绑定方案")
    @PostMapping("/bind/{planId}")
    public R<Void> bindRules(@PathVariable Long planId, @RequestBody List<Long> ruleIds) {
        return toAjax(ruleDefService.bindRules(planId, ruleIds));
    }

    // ==================== 元数据查询接口 ====================

    /**
     * 获取元数据表信息（用于规则配置）
     */
    @SaCheckPermission("metadata:dqc:rule:list")
    @GetMapping("/metadata/table/{tableId}")
    public R<MetadataTable> getMetadataTable(@PathVariable Long tableId) {
        return R.ok(ruleDefService.getMetadataTable(tableId));
    }

    /**
     * 获取表的所有字段列表（用于规则配置时下拉选择）
     */
    @SaCheckPermission("metadata:dqc:rule:list")
    @GetMapping("/metadata/table/{tableId}/columns")
    public R<List<MetadataColumn>> getTableColumns(@PathVariable Long tableId) {
        return R.ok(ruleDefService.getTableColumns(tableId));
    }

    /**
     * 获取对比表信息（跨表/跨字段规则）
     */
    @SaCheckPermission("metadata:dqc:rule:list")
    @GetMapping("/metadata/compare/table/{compareTableId}")
    public R<MetadataTable> getCompareTable(@PathVariable Long compareTableId) {
        return R.ok(ruleDefService.getCompareTable(compareTableId));
    }

    /**
     * 获取对比表的所有字段列表（用于规则配置时下拉选择）
     */
    @SaCheckPermission("metadata:dqc:rule:list")
    @GetMapping("/metadata/compare/table/{compareTableId}/columns")
    public R<List<MetadataColumn>> getCompareTableColumns(@PathVariable Long compareTableId) {
        return R.ok(ruleDefService.getCompareTableColumns(compareTableId));
    }
}
