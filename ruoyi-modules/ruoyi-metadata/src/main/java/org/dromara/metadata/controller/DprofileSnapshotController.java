package org.dromara.metadata.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.DprofileSnapshotBo;
import org.dromara.metadata.domain.vo.DprofileSnapshotVo;
import org.dromara.metadata.domain.vo.SnapshotCompareVo;
import org.dromara.metadata.service.IDprofileSnapshotService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据探查快照控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/dprofile/snapshot")
public class DprofileSnapshotController extends BaseController {

    private final IDprofileSnapshotService snapshotService;

    /**
     * 分页查询快照列表
     */
    @GetMapping("/list")
    public TableDataInfo<DprofileSnapshotVo> list(DprofileSnapshotVo vo, PageQuery pageQuery) {
        return snapshotService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取快照详情
     */
    @GetMapping("/{id}")
    public R<DprofileSnapshotVo> getInfo(@PathVariable Long id) {
        return R.ok(snapshotService.queryById(id));
    }

    /**
     * 创建快照
     */
    @PostMapping
    public R<Long> add(@Validated @RequestBody DprofileSnapshotBo bo) {
        if (StringUtils.isBlank(bo.getSnapshotName())) {
            throw new ServiceException("快照名称不能为空");
        }
        if (bo.getDsId() == null) {
            throw new ServiceException("数据源ID不能为空");
        }
        return R.ok(snapshotService.createSnapshot(
            bo.getDsId(),
            bo.getSnapshotName(),
            bo.getSnapshotDesc()
        ));
    }

    /**
     * 对比两个快照
     */
    @GetMapping("/compare")
    public R<SnapshotCompareVo> compare(
            @RequestParam Long snapshotId1,
            @RequestParam Long snapshotId2) {
        return R.ok(snapshotService.compareSnapshot(snapshotId1, snapshotId2));
    }

    /**
     * 删除快照
     */
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        snapshotService.deleteByIds(ids);
        return R.ok();
    }
}
