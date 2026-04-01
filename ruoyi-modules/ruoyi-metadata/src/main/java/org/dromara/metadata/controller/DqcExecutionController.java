package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.DqcExecution;
import org.dromara.metadata.domain.vo.DqcExecutionDetailVo;
import org.dromara.metadata.domain.vo.DqcExecutionVo;
import org.dromara.metadata.service.IDqcExecutionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据质量执行记录Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/dqc/execution")
public class DqcExecutionController extends BaseController {

    private final IDqcExecutionService executionService;

    /**
     * 查询执行记录列表
     */
    @SaCheckPermission("metadata:dqc:execution:list")
    @GetMapping("/list")
    public TableDataInfo<DqcExecutionVo> list(DqcExecutionVo vo, PageQuery pageQuery) {
        return executionService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取执行记录详细信息
     */
    @SaCheckPermission("metadata:dqc:execution:query")
    @GetMapping("/{id}")
    public R<DqcExecutionVo> getInfo(@PathVariable Long id) {
        return R.ok(executionService.queryById(id));
    }

    /**
     * 执行方案
     */
    @SaCheckPermission("metadata:dqc:plan:exec")
    @Log(title = "数据质量执行-执行方案")
    @PostMapping("/exec/{planId}")
    public R<DqcExecution> executePlan(@PathVariable Long planId) {
        Long userId = LoginHelper.getUserId();
        DqcExecution execution = executionService.executePlan(planId, "MANUAL", userId);
        return R.ok(execution);
    }

    /**
     * 查询执行明细列表
     */
    @SaCheckPermission("metadata:dqc:execution:detail")
    @GetMapping("/detail/{executionId}")
    public R<List<DqcExecutionDetailVo>> listDetails(@PathVariable Long executionId) {
        return R.ok(executionService.listDetailsByExecutionId(executionId));
    }

    /**
     * 根据方案ID查询执行记录
     */
    @SaCheckPermission("metadata:dqc:execution:list")
    @GetMapping("/plan/{planId}")
    public R<List<DqcExecutionVo>> listByPlanId(@PathVariable Long planId) {
        return R.ok(executionService.listByPlanId(planId));
    }

    /**
     * 停止正在运行的执行
     */
    @SaCheckPermission("metadata:dqc:execution:stop")
    @Log(title = "数据质量执行-停止执行")
    @PutMapping("/{id}/stop")
    public R<Void> stopExecution(@PathVariable Long id) {
        executionService.stopExecution(id);
        return R.ok();
    }

    /**
     * 重新执行指定执行记录
     */
    @SaCheckPermission("metadata:dqc:execution:rerun")
    @Log(title = "数据质量执行-重新执行")
    @PostMapping("/{id}/rerun")
    public R<DqcExecution> rerunExecution(@PathVariable Long id) {
        Long userId = LoginHelper.getUserId();
        DqcExecution execution = executionService.rerunExecution(id, userId);
        return R.ok(execution);
    }
}
