package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.dto.SecSensitivityScanDTO;
import org.dromara.metadata.domain.vo.SecColumnSensitivityVo;
import org.dromara.metadata.domain.vo.SecScanResultVO;
import org.dromara.metadata.service.ISecColumnSensitivityService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字段敏感记录管理Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/security/sensitivity")
public class SecColumnSensitivityController extends BaseController {

    private final ISecColumnSensitivityService columnSensitivityService;

    /**
     * 查询敏感字段列表
     */
    @SaCheckPermission("metadata:security:sensitivity:list")
    @GetMapping("/list")
    public TableDataInfo<SecColumnSensitivityVo> list(SecColumnSensitivityVo vo, PageQuery pageQuery) {
        return columnSensitivityService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取敏感字段详细信息
     */
    @SaCheckPermission("metadata:security:sensitivity:query")
    @GetMapping("/{id}")
    public R<SecColumnSensitivityVo> getInfo(@PathVariable Long id) {
        return R.ok(columnSensitivityService.queryById(id));
    }

    /**
     * 执行敏感字段扫描（完整参数版，对标 governance 能力）
     */
    @SaCheckPermission("metadata:security:sensitivity:add")
    @PostMapping("/scan")
    public R<SecScanResultVO> scanSensitiveFields(@Validated @RequestBody SecSensitivityScanDTO dto) {
        return R.ok(columnSensitivityService.scanSensitiveFields(dto));
    }

    /**
     * 确认敏感字段
     */
    @SaCheckPermission("metadata:security:sensitivity:edit")
    @PostMapping("/confirm")
    public R<Void> confirmColumns(@RequestBody List<Long> ids) {
        return toAjax(columnSensitivityService.confirmColumns(ids));
    }

    /**
     * 新增敏感字段记录
     */
    @SaCheckPermission("metadata:security:sensitivity:add")
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SecColumnSensitivityVo vo) {
        return R.ok(columnSensitivityService.insert(vo));
    }

    /**
     * 修改敏感字段记录
     */
    @SaCheckPermission("metadata:security:sensitivity:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SecColumnSensitivityVo vo) {
        return toAjax(columnSensitivityService.update(vo));
    }

    /**
     * 删除敏感字段记录
     */
    @SaCheckPermission("metadata:security:sensitivity:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(columnSensitivityService.deleteByIds(ids));
    }
}
