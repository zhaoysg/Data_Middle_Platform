package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.DqcPlan;
import org.dromara.metadata.domain.bo.DqcPlanBo;
import org.dromara.metadata.domain.vo.DqcPlanVo;
import org.dromara.metadata.service.IDqcPlanService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据质量检测方案Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/dqc/plan")
public class DqcPlanController extends BaseController {

    private final IDqcPlanService planService;

    /**
     * 查询方案列表
     */
    @SaCheckPermission("metadata:dqc:plan:list")
    @GetMapping("/list")
    public TableDataInfo<DqcPlanVo> list(DqcPlanBo bo, PageQuery pageQuery) {
        return planService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取方案详细信息
     */
    @SaCheckPermission("metadata:dqc:plan:query")
    @GetMapping("/{id}")
    public R<DqcPlanVo> getInfo(@PathVariable Long id) {
        return R.ok(planService.queryById(id));
    }

    /**
     * 新增方案
     */
    @SaCheckPermission("metadata:dqc:plan:add")
    @Log(title = "数据质量检测方案")
    @PostMapping
    public R<Long> add(@Validated @RequestBody DqcPlanBo bo) {
        return R.ok(planService.insertByBo(bo));
    }

    /**
     * 修改方案
     */
    @SaCheckPermission("metadata:dqc:plan:edit")
    @Log(title = "数据质量检测方案")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody DqcPlanBo bo) {
        return toAjax(planService.updateByBo(bo));
    }

    /**
     * 删除方案
     */
    @SaCheckPermission("metadata:dqc:plan:remove")
    @Log(title = "数据质量检测方案")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        return toAjax(planService.deleteByIds(ids));
    }

    /**
     * 发布方案
     */
    @SaCheckPermission("metadata:dqc:plan:edit")
    @Log(title = "数据质量检测方案-发布")
    @PostMapping("/publish/{id}")
    public R<Void> publish(@PathVariable Long id) {
        return toAjax(planService.publish(id));
    }

    /**
     * 绑定规则到方案
     */
    @SaCheckPermission("metadata:dqc:plan:edit")
    @Log(title = "数据质量检测方案-绑定规则")
    @PostMapping("/bindRules")
    public R<Void> bindRules(@RequestParam Long planId, @RequestBody List<Long> ruleIds) {
        return toAjax(planService.bindRules(planId, ruleIds));
    }

    /**
     * 获取已发布的方案列表
     */
    @SaCheckPermission("metadata:dqc:plan:list")
    @GetMapping("/published")
    public R<List<DqcPlan>> getPublishedPlans() {
        return R.ok(planService.queryPublishedPlans());
    }
}
