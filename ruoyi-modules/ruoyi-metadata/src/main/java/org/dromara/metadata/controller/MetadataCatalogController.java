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
import org.dromara.metadata.domain.bo.MetadataCatalogBo;
import org.dromara.metadata.domain.vo.MetadataCatalogVo;
import org.dromara.metadata.service.IMetadataCatalogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产目录管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/catalog")
public class MetadataCatalogController extends BaseController {

    private final IMetadataCatalogService catalogService;

    /** 查询资产目录列表 */
    @SaCheckPermission("metadata:catalog:list")
    @GetMapping("/list")
    public TableDataInfo<MetadataCatalogVo> list(MetadataCatalogBo bo, PageQuery pageQuery) {
        return catalogService.pageCatalogList(bo, pageQuery);
    }

    /** 查询资产目录树形 */
    @SaCheckPermission("metadata:catalog:list")
    @GetMapping("/tree")
    public R<List<MetadataCatalogVo>> tree() {
        return R.ok(catalogService.listTree());
    }

    /** 获取资产目录下拉框 */
    @SaCheckPermission("metadata:catalog:list")
    @GetMapping("/options")
    public R<List<MetadataCatalogVo>> options() {
        return R.ok(catalogService.buildTree(catalogService.listEnabled()));
    }

    /** 导出资产目录列表 */
    @SaCheckPermission("metadata:catalog:export")
    @Log(title = "资产目录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(MetadataCatalogBo bo, HttpServletResponse response) {
        List<MetadataCatalogVo> list = catalogService.listEnabled();
        ExcelUtil.exportExcel(list, "资产目录数据", MetadataCatalogVo.class, response);
    }

    /** 获取资产目录详细信息 */
    @SaCheckPermission("metadata:catalog:query")
    @GetMapping("/{id}")
    public R<MetadataCatalogVo> getInfo(@PathVariable Long id) {
        return R.ok(catalogService.getCatalogById(id));
    }

    /** 新增资产目录 */
    @SaCheckPermission("metadata:catalog:add")
    @Log(title = "资产目录", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated @RequestBody MetadataCatalogBo bo) {
        return R.ok(catalogService.insertCatalog(bo));
    }

    /** 修改资产目录 */
    @SaCheckPermission("metadata:catalog:edit")
    @Log(title = "资产目录", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody MetadataCatalogBo bo) {
        return toAjax(catalogService.updateCatalog(bo));
    }

    /** 删除资产目录 */
    @SaCheckPermission("metadata:catalog:remove")
    @Log(title = "资产目录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable @NotEmpty(message = "主键不能为空") Long[] ids) {
        return toAjax(catalogService.deleteCatalog(ids));
    }
}
