package org.dromara.metadata.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.DprofileTaskBo;
import org.dromara.metadata.domain.vo.DprofileTaskVo;
import org.dromara.metadata.service.IDprofileTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据探查任务控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/dprofile/task")
public class DprofileTaskController extends BaseController {

    private final IDprofileTaskService taskService;

    /**
     * 分页查询任务列表
     */
    @GetMapping("/list")
    public TableDataInfo<DprofileTaskVo> list(DprofileTaskVo vo, PageQuery pageQuery) {
        return taskService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public R<DprofileTaskVo> getInfo(@PathVariable Long id) {
        return R.ok(taskService.queryById(id));
    }

    /**
     * 创建任务
     */
    @PostMapping
    public R<Long> add(@Validated @RequestBody DprofileTaskBo bo) {
        if (StringUtils.isBlank(bo.getTaskName())) {
            throw new ServiceException("任务名称不能为空");
        }
        if (bo.getDsId() == null) {
            throw new ServiceException("数据源ID不能为空");
        }
        return R.ok(taskService.insertByBo(bo));
    }

    /**
     * 更新任务
     */
    @PutMapping
    public R<Void> edit(@Validated @RequestBody DprofileTaskBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("任务ID不能为空");
        }
        taskService.updateByBo(bo);
        return R.ok();
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        taskService.deleteByIds(ids);
        return R.ok();
    }

    /**
     * 启动任务
     */
    @PostMapping("/{id}/start")
    public R<Void> start(@PathVariable Long id) {
        taskService.startTask(id);
        return R.ok();
    }

    /**
     * 停止任务
     */
    @PostMapping("/{id}/stop")
    public R<Void> stop(@PathVariable Long id) {
        taskService.stopTask(id);
        return R.ok();
    }

    /**
     * 同步执行任务（手动触发）
     */
    @PostMapping("/{id}/run")
    public R<Void> run(@PathVariable Long id) {
        taskService.runTaskSync(id);
        return R.ok();
    }
}
