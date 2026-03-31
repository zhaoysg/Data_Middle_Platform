package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.MetadataColumnBo;
import org.dromara.metadata.domain.vo.MetadataColumnVo;
import org.dromara.metadata.service.IMetadataColumnService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 元数据字段管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/column")
public class MetadataColumnController extends BaseController {

    private final IMetadataColumnService columnService;

    /** 查询元数据字段列表 */
    @SaCheckPermission("metadata:column:list")
    @GetMapping("/list")
    public TableDataInfo<MetadataColumnVo> list(MetadataColumnBo bo, PageQuery pageQuery) {
        return columnService.pageColumnList(bo, pageQuery);
    }

    /** 导出元数据字段列表 */
    @SaCheckPermission("metadata:column:export")
    @Log(title = "元数据字段", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(MetadataColumnBo bo, HttpServletResponse response) {
        List<MetadataColumnVo> list = columnService.listColumn(bo);
        ExcelUtil.exportExcel(list, "元数据字段数据", MetadataColumnVo.class, response);
    }

    /** 获取元数据字段详细信息 */
    @SaCheckPermission("metadata:column:query")
    @GetMapping("/{id}")
    public R<MetadataColumnVo> getInfo(@PathVariable Long id) {
        return R.ok(columnService.getColumnById(id));
    }

    /** 修改字段别名 */
    @SaCheckPermission("metadata:column:edit")
    @Log(title = "元数据字段", businessType = BusinessType.UPDATE)
    @PutMapping("/alias")
    public R<Void> updateAlias(@RequestParam Long id, @RequestParam String alias) {
        return toAjax(columnService.updateAlias(id, alias));
    }

    /** 删除字段 */
    @SaCheckPermission("metadata:column:remove")
    @Log(title = "元数据字段", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(columnService.deleteColumn(ids));
    }
}
