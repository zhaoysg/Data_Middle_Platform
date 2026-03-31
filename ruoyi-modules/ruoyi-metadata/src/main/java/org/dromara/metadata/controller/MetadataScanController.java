package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.MetadataScanBo;
import org.dromara.metadata.domain.vo.MetadataScanLogVo;
import org.dromara.metadata.domain.vo.MetadataScanResultVo;
import org.dromara.metadata.service.IMetadataScanService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 元数据扫描管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/scan")
public class MetadataScanController extends BaseController {

    private final IMetadataScanService scanService;

    /**
     * 触发数据源扫描
     * @param dsId 数据源ID（路径变量）
     * @param bo 扫描请求参数（请求体）
     */
    @SaCheckPermission("metadata:scan:exec")
    @Log(title = "元数据扫描", businessType = BusinessType.OTHER)
    @PostMapping("/exec/{dsId}")
    public R<MetadataScanResultVo> exec(
            @PathVariable @NotNull(message = "数据源ID不能为空") Long dsId,
            @RequestBody(required = false) MetadataScanBo bo) {
        MetadataScanBo scanBo = bo == null ? new MetadataScanBo() : bo;
        scanBo.setDsId(dsId);
        return R.ok(scanService.scanByDatasource(scanBo, TenantHelper.getTenantId()));
    }

    /**
     * 查询数据源扫描历史（按 dsId 查询）
     */
    @SaCheckPermission("metadata:scan:list")
    @GetMapping("/logs/{dsId}")
    public R<List<MetadataScanLogVo>> logs(@PathVariable Long dsId) {
        return R.ok(scanService.listScanLogs(dsId));
    }

    /**
     * 查询扫描记录列表（分页 / 全量）
     * 前端调用：GET /system/metadata/scan/logs?dsId=123
     */
    @SaCheckPermission("metadata:scan:list")
    @GetMapping("/list")
    public R<List<MetadataScanLogVo>> listLogs(
            @RequestParam(required = false) Long dsId,
            @RequestParam(required = false) String status) {
        if (dsId != null) {
            return R.ok(scanService.listScanLogs(dsId));
        }
        return R.ok(scanService.listScanLogs(null));
    }

    /**
     * 获取扫描记录详情
     */
    @SaCheckPermission("metadata:scan:query")
    @GetMapping("/logs/detail/{scanLogId}")
    public R<MetadataScanLogVo> detail(@PathVariable Long scanLogId) {
        return R.ok(scanService.getScanLog(scanLogId));
    }
}
