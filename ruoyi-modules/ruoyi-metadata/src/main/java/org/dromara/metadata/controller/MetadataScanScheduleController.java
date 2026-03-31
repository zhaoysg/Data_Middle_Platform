package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.MetadataScanSchedule;
import org.dromara.metadata.domain.bo.MetadataScanScheduleBo;
import org.dromara.metadata.domain.vo.MetadataScanScheduleVo;
import org.dromara.metadata.service.IMetadataScanScheduleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 元数据扫描调度计划Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/schedule")
public class MetadataScanScheduleController extends BaseController {

    private final IMetadataScanScheduleService scheduleService;

    /**
     * 查询调度计划列表
     */
    @SaCheckPermission("metadata:schedule:list")
    @GetMapping("/list")
    public TableDataInfo<MetadataScanScheduleVo> list(MetadataScanScheduleBo bo, PageQuery pageQuery) {
        return scheduleService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取调度计划详细信息
     */
    @SaCheckPermission("metadata:schedule:query")
    @GetMapping("/{id}")
    public R<MetadataScanScheduleVo> getInfo(@PathVariable Long id) {
        return R.ok(scheduleService.queryById(id));
    }

    /**
     * 新增调度计划
     */
    @SaCheckPermission("metadata:schedule:add")
    @Log(title = "调度计划")
    @PostMapping
    public R<Void> add(@Validated @RequestBody MetadataScanScheduleBo bo) {
        return toAjax(scheduleService.insertByBo(bo) != null ? 1 : 0);
    }

    /**
     * 修改调度计划
     */
    @SaCheckPermission("metadata:schedule:edit")
    @Log(title = "调度计划")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody MetadataScanScheduleBo bo) {
        return toAjax(scheduleService.updateByBo(bo));
    }

    /**
     * 修改调度计划状态
     */
    @SaCheckPermission("metadata:schedule:edit")
    @Log(title = "调度计划")
    @PutMapping("/enabled")
    public R<Void> updateEnabled(@Validated @RequestBody MetadataScanScheduleBo bo) {
        return toAjax(scheduleService.updateEnabled(bo));
    }

    /**
     * 删除调度计划
     */
    @SaCheckPermission("metadata:schedule:remove")
    @Log(title = "调度计划")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        return toAjax(scheduleService.deleteByIds(ids));
    }

    /**
     * 触发立即扫描
     */
    @SaCheckPermission("metadata:schedule:exec")
    @Log(title = "调度计划-触发扫描")
    @PostMapping("/exec/{id}")
    public R<Void> executeScan(@PathVariable Long id) {
        scheduleService.executeScan(id);
        return R.ok();
    }

    /**
     * 获取启用的调度计划
     */
    @SaCheckPermission("metadata:schedule:list")
    @GetMapping("/enabled")
    public R<List<MetadataScanSchedule>> getEnabledSchedules() {
        return R.ok(scheduleService.queryEnabledSchedules());
    }
}
