package org.dromara.datasource.controller;

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
import org.dromara.common.core.utils.StringUtils;
import org.dromara.datasource.connection.ConnectionTestResultVO;
import org.dromara.datasource.domain.bo.SysDatasourceBo;
import org.dromara.datasource.domain.vo.SysDatasourceVo;
import org.dromara.datasource.service.ISysDatasourceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源管理
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/datasource")
public class SysDatasourceController extends BaseController {

    private final ISysDatasourceService datasourceService;

    /**
     * 查询数据源列表
     */
    @SaCheckPermission("system:ds:list")
    @GetMapping("/list")
    public TableDataInfo<SysDatasourceVo> list(SysDatasourceBo bo, PageQuery pageQuery) {
        return datasourceService.pageDatasourceList(bo, pageQuery);
    }

    /**
     * 导出数据源列表
     */
    @SaCheckPermission("system:ds:export")
    @Log(title = "数据源", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysDatasourceBo bo, HttpServletResponse response) {
        List<SysDatasourceVo> list = datasourceService.listDatasource(bo);
        ExcelUtil.exportExcel(list, "数据源数据", SysDatasourceVo.class, response);
    }

    /**
     * 获取数据源详细信息
     */
    @SaCheckPermission("system:ds:query")
    @GetMapping("/{dsId}")
    public R<SysDatasourceVo> getInfo(@PathVariable Long dsId) {
        return R.ok(datasourceService.getDatasourceById(dsId));
    }

    /**
     * 新增数据源
     */
    @SaCheckPermission("system:ds:add")
    @Log(title = "数据源", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated @RequestBody SysDatasourceBo bo) {
        if (StringUtils.isNotBlank(bo.getDsCode())
            && datasourceService.getDatasourceByCode(bo.getDsCode()) != null) {
            return R.fail("数据源编码已存在");
        }
        return R.ok(datasourceService.insertDatasource(bo));
    }

    /**
     * 修改数据源
     */
    @SaCheckPermission("system:ds:edit")
    @Log(title = "数据源", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody SysDatasourceBo bo) {
        return toAjax(datasourceService.updateDatasource(bo));
    }

    /**
     * 删除数据源
     */
    @SaCheckPermission("system:ds:remove")
    @Log(title = "数据源", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dsIds}")
    public R<Void> remove(@PathVariable @NotEmpty(message = "主键不能为空") Long[] dsIds) {
        return toAjax(datasourceService.deleteDatasource(dsIds));
    }

    /**
     * 测试数据源连接
     */
    @SaCheckPermission("system:ds:test")
    @Log(title = "数据源", businessType = BusinessType.OTHER)
    @PostMapping("/test")
    public R<ConnectionTestResultVO> test(@RequestBody SysDatasourceBo bo) {
        return R.ok(datasourceService.testConnection(bo));
    }

    /**
     * 修改数据源状态
     */
    @SaCheckPermission("system:ds:edit")
    @Log(title = "数据源", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysDatasourceBo bo) {
        return toAjax(datasourceService.updateStatus(bo.getDsId(), bo.getStatus()));
    }

    /**
     * 获取已启用数据源列表（下拉框用）
     */
    @SaCheckPermission("system:ds:query")
    @GetMapping("/enabled")
    public R<List<SysDatasourceVo>> enabled() {
        return R.ok(datasourceService.listEnabledDatasource());
    }

    /**
     * 获取数据源下的表列表
     */
    @SaCheckPermission("system:ds:query")
    @GetMapping("/{dsId}/tables")
    public R<List<String>> tables(
            @PathVariable Long dsId,
            @RequestParam(required = false) String schema) {
        return R.ok(datasourceService.getTables(dsId, schema));
    }

    /**
     * 获取数据源指定表的字段
     */
    @SaCheckPermission("system:ds:query")
    @GetMapping("/{dsId}/columns")
    public R<List<?>> columns(
            @PathVariable Long dsId,
            @RequestParam String tableName,
            @RequestParam(required = false) String schema) {
        return R.ok(datasourceService.getTableColumns(dsId, tableName, schema));
    }
}
