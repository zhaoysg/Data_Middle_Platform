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
import org.dromara.metadata.domain.bo.DataDomainBo;
import org.dromara.metadata.domain.vo.DataDomainVo;
import org.dromara.metadata.service.IMetadataDomainService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据域管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/domain")
public class MetadataDomainController extends BaseController {

    private final IMetadataDomainService domainService;

    /** 查询数据域列表 */
    @SaCheckPermission("metadata:domain:list")
    @GetMapping("/list")
    public TableDataInfo<DataDomainVo> list(DataDomainBo bo, PageQuery pageQuery) {
        return domainService.pageDomainList(bo, pageQuery);
    }

    /** 获取数据域下拉框 */
    @SaCheckPermission("metadata:domain:list")
    @GetMapping("/options")
    public R<List<DataDomainVo>> options() {
        return R.ok(domainService.listEnabled());
    }

    /** 导出数据域列表 */
    @SaCheckPermission("metadata:domain:export")
    @Log(title = "数据域", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(DataDomainBo bo, HttpServletResponse response) {
        List<DataDomainVo> list = domainService.listDomain(bo);
        ExcelUtil.exportExcel(list, "数据域数据", DataDomainVo.class, response);
    }

    /** 获取数据域详细信息 */
    @SaCheckPermission("metadata:domain:query")
    @GetMapping("/{id}")
    public R<DataDomainVo> getInfo(@PathVariable Long id) {
        return R.ok(domainService.getDomainById(id));
    }

    /** 新增数据域 */
    @SaCheckPermission("metadata:domain:add")
    @Log(title = "数据域", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated @RequestBody DataDomainBo bo) {
        return R.ok(domainService.insertDomain(bo));
    }

    /** 修改数据域 */
    @SaCheckPermission("metadata:domain:edit")
    @Log(title = "数据域", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody DataDomainBo bo) {
        return toAjax(domainService.updateDomain(bo));
    }

    /** 删除数据域 */
    @SaCheckPermission("metadata:domain:remove")
    @Log(title = "数据域", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable @NotEmpty(message = "主键不能为空") Long[] ids) {
        return toAjax(domainService.deleteDomain(ids));
    }
}
