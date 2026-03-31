package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.GovLineageBo;
import org.dromara.metadata.domain.vo.GovLineageVo;
import org.dromara.metadata.service.IGovLineageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据血缘关系Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/lineage")
public class GovLineageController extends BaseController {

    private final IGovLineageService lineageService;

    /**
     * 查询血缘关系列表
     */
    @SaCheckPermission("metadata:lineage:list")
    @GetMapping("/list")
    public TableDataInfo<GovLineageVo> list(GovLineageBo bo, PageQuery pageQuery) {
        return lineageService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取血缘关系详细信息
     */
    @SaCheckPermission("metadata:lineage:query")
    @GetMapping("/{id}")
    public R<GovLineageVo> getInfo(@PathVariable Long id) {
        return R.ok(lineageService.queryById(id));
    }

    /**
     * 新增血缘关系
     */
    @SaCheckPermission("metadata:lineage:add")
    @Log(title = "数据血缘关系")
    @PostMapping
    public R<Void> add(@Validated @RequestBody GovLineageBo bo) {
        return toAjax(lineageService.insertByBo(bo) > 0);
    }

    /**
     * 修改血缘关系
     */
    @SaCheckPermission("metadata:lineage:edit")
    @Log(title = "数据血缘关系")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody GovLineageBo bo) {
        return toAjax(lineageService.updateByBo(bo));
    }

    /**
     * 删除血缘关系
     */
    @SaCheckPermission("metadata:lineage:remove")
    @Log(title = "数据血缘关系")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        return toAjax(lineageService.deleteByIds(ids));
    }

    /**
     * 查询上游血缘
     */
    @SaCheckPermission("metadata:lineage:view")
    @GetMapping("/upstream/{dsId}/{tableName}")
    public R<List<GovLineageVo>> listUpstream(
            @PathVariable Long dsId,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "5") int depth) {
        return R.ok(lineageService.listUpstream(dsId, tableName, depth));
    }

    /**
     * 查询下游血缘
     */
    @SaCheckPermission("metadata:lineage:view")
    @GetMapping("/downstream/{dsId}/{tableName}")
    public R<List<GovLineageVo>> listDownstream(
            @PathVariable Long dsId,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "5") int depth) {
        return R.ok(lineageService.listDownstream(dsId, tableName, depth));
    }
}
