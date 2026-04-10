package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.MetadataTableBo;
import org.dromara.metadata.domain.vo.MetadataColumnVo;
import org.dromara.metadata.domain.vo.MetadataTableVo;
import org.dromara.metadata.service.IMetadataColumnService;
import org.dromara.metadata.service.IMetadataTableService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 元数据表管理
 */
@Validated
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/system/metadata/table")
public class MetadataTableController extends BaseController {

    private final IMetadataTableService tableService;
    private final IMetadataColumnService columnService;

    /** 查询元数据表列表 */
    @SaCheckPermission("metadata:table:list")
    @GetMapping("/list")
    public TableDataInfo<MetadataTableVo> list(MetadataTableBo bo, PageQuery pageQuery) {
        return tableService.pageTableList(bo, pageQuery);
    }

    /** 导出元数据表列表 */
    @SaCheckPermission("metadata:table:export")
    @Log(title = "元数据表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(MetadataTableBo bo, HttpServletResponse response) {
        List<MetadataTableVo> list = tableService.listTable(bo);
        // 治理提示：检查导出数据中是否有未配置域/分层的表
        long noLayer = list.stream().filter(t -> StringUtils.isBlank(t.getDataLayer())).count();
        long noDomain = list.stream().filter(t -> StringUtils.isBlank(t.getDataDomain())).count();
        if (noLayer > 0 || noDomain > 0) {
            log.warn("导出元数据表治理提醒：{}张表未配置分层，{}张表未配置数据域", noLayer, noDomain);
        }
        ExcelUtil.exportExcel(list, "元数据表数据", MetadataTableVo.class, response);
    }

    /**
     * 查询治理统计（未配置分层/域/敏感等级的表数量）
     */
    @SaCheckPermission("metadata:table:query")
    @GetMapping("/governance-stat")
    public R<long[]> governanceStat() {
        return R.ok(tableService.countIncompleteTables());
    }

    /** 获取元数据表详细信息 */
    @SaCheckPermission("metadata:table:query")
    @GetMapping("/{id}")
    public R<MetadataTableVo> getInfo(@PathVariable Long id) {
        return R.ok(tableService.getTableById(id));
    }

    /** 获取元数据标签选项 */
    @SaCheckPermission("metadata:table:query")
    @GetMapping("/tag-options")
    public R<List<String>> tagOptions() {
        return R.ok(tableService.listTagOptions());
    }

    /** 修改元数据表（别名等） */
    @SaCheckPermission("metadata:table:edit")
    @Log(title = "元数据表", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody MetadataTableBo bo) {
        if (bo.getId() == null) {
            return R.fail("表ID不能为空");
        }
        return toAjax(tableService.updateTable(bo));
    }

    /** 删除元数据表 */
    @SaCheckPermission("metadata:table:remove")
    @Log(title = "元数据表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable @NotEmpty(message = "主键不能为空") Long[] ids) {
        return toAjax(tableService.deleteTable(ids));
    }

    /** 获取表字段列表 */
    @SaCheckPermission("metadata:table:query")
    @GetMapping("/{id}/columns")
    public R<List<MetadataColumnVo>> columns(@PathVariable Long id) {
        return R.ok(columnService.listByTableId(id));
    }

    /** 更新字段别名 */
    @SaCheckPermission("metadata:table:edit")
    @Log(title = "元数据字段", businessType = BusinessType.UPDATE)
    @PutMapping("/column/alias")
    public R<Void> updateColumnAlias(@RequestParam Long id, @RequestParam String alias) {
        return toAjax(columnService.updateAlias(id, alias));
    }

    /** 导出字段列表 */
    @SaCheckPermission("metadata:table:export")
    @Log(title = "元数据字段", businessType = BusinessType.EXPORT)
    @PostMapping("/column/export")
    public void exportColumns(@RequestParam Long tableId, HttpServletResponse response) {
        List<MetadataColumnVo> list = columnService.listByTableId(tableId);
        ExcelUtil.exportExcel(list, "字段列表", MetadataColumnVo.class, response);
    }

    /**
     * 批量更新元数据表（分层/域/敏感等级/标签）
     * 用于扫描完成后的引导配置
     */
    @SaCheckPermission("metadata:table:edit")
    @Log(title = "元数据表", businessType = BusinessType.UPDATE)
    @PutMapping("/batch-update")
    public R<Integer> batchUpdate(@RequestBody MetadataTableBo bo) {
        if (bo.getIds() == null || bo.getIds().isEmpty()) {
            return R.fail("请选择要更新的表");
        }
        return R.ok(tableService.batchUpdate(bo));
    }
}
