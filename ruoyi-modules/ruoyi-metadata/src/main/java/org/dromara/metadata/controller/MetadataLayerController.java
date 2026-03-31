package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.DataLayerBo;
import org.dromara.metadata.domain.vo.DataLayerVo;
import org.dromara.metadata.service.IDataLayerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数仓分层管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/layer")
public class MetadataLayerController extends BaseController {

    private final IDataLayerService layerService;

    /** 查询数仓分层列表 */
    @SaCheckPermission("metadata:layer:list")
    @GetMapping("/list")
    public TableDataInfo<DataLayerVo> list(DataLayerBo bo, PageQuery pageQuery) {
        return layerService.pageLayerList(bo, pageQuery);
    }

    /** 获取全部分层（下拉框） */
    @SaCheckPermission("metadata:layer:list")
    @GetMapping("/options")
    public R<List<DataLayerVo>> options() {
        return R.ok(layerService.listEnabled());
    }

    /** 导数仓分层列表 */
    @SaCheckPermission("metadata:layer:export")
    @Log(title = "数仓分层", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(DataLayerBo bo, HttpServletResponse response) {
        List<DataLayerVo> list = layerService.listLayer(bo);
        ExcelUtil.exportExcel(list, "数仓分层数据", DataLayerVo.class, response);
    }

    /** 获取数仓分层详细信息 */
    @SaCheckPermission("metadata:layer:query")
    @GetMapping("/{id}")
    public R<DataLayerVo> getInfo(@PathVariable Long id) {
        return R.ok(layerService.getLayerById(id));
    }

    /** 新增数仓分层 */
    @SaCheckPermission("metadata:layer:add")
    @Log(title = "数仓分层", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated @RequestBody DataLayerBo bo) {
        return R.ok(layerService.insertLayer(bo));
    }

    /** 修改数仓分层 */
    @SaCheckPermission("metadata:layer:edit")
    @Log(title = "数仓分层", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody DataLayerBo bo) {
        return toAjax(layerService.updateLayer(bo));
    }

    /** 删除数仓分层 */
    @SaCheckPermission("metadata:layer:remove")
    @Log(title = "数仓分层", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable @NotEmpty(message = "主键不能为空") Long[] ids) {
        return toAjax(layerService.deleteLayer(ids));
    }
}
