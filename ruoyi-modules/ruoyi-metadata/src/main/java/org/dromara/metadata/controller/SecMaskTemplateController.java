package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.vo.SecMaskTemplateVo;
import org.dromara.metadata.service.ISecMaskTemplateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 脱敏模板管理Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/security/mask-template")
public class SecMaskTemplateController extends BaseController {

    private final ISecMaskTemplateService maskTemplateService;

    /**
     * 查询脱敏模板列表
     */
    @SaCheckPermission("metadata:security:maskTemplate:list")
    @GetMapping("/list")
    public TableDataInfo<SecMaskTemplateVo> list(SecMaskTemplateVo vo, PageQuery pageQuery) {
        return maskTemplateService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取脱敏模板详细信息
     */
    @SaCheckPermission("metadata:security:maskTemplate:query")
    @GetMapping("/{id}")
    public R<SecMaskTemplateVo> getInfo(@PathVariable Long id) {
        return R.ok(maskTemplateService.queryById(id));
    }

    /**
     * 获取所有启用的模板
     */
    @SaCheckPermission("metadata:security:maskTemplate:list")
    @GetMapping("/all")
    public R<List<SecMaskTemplateVo>> listAll() {
        return R.ok(maskTemplateService.listAllEnabled());
    }

    /**
     * 获取模板编码->表达式映射
     */
    @SaCheckPermission("metadata:security:maskTemplate:list")
    @GetMapping("/expr-map")
    public R<Map<String, String>> getExprMap() {
        return R.ok(maskTemplateService.getTemplateExprMap());
    }

    /**
     * 新增脱敏模板
     */
    @SaCheckPermission("metadata:security:maskTemplate:add")
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SecMaskTemplateVo vo) {
        return R.ok(maskTemplateService.insert(vo));
    }

    /**
     * 修改脱敏模板
     */
    @SaCheckPermission("metadata:security:maskTemplate:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SecMaskTemplateVo vo) {
        return toAjax(maskTemplateService.update(vo));
    }

    /**
     * 删除脱敏模板
     */
    @SaCheckPermission("metadata:security:maskTemplate:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(maskTemplateService.deleteByIds(ids));
    }
}
