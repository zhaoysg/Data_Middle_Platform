package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.vo.SecSensitivityRuleVo;
import org.dromara.metadata.service.ISecSensitivityRuleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 敏感识别规则管理Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/security/rules")
public class SecSensitivityRuleController extends BaseController {

    private final ISecSensitivityRuleService ruleService;

    /**
     * 查询敏感识别规则列表
     */
    @SaCheckPermission("metadata:security:rules:list")
    @GetMapping("/list")
    public TableDataInfo<SecSensitivityRuleVo> list(SecSensitivityRuleVo vo, PageQuery pageQuery) {
        return ruleService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取敏感识别规则详细信息
     */
    @SaCheckPermission("metadata:security:rules:query")
    @GetMapping("/{id}")
    public R<SecSensitivityRuleVo> getInfo(@PathVariable Long id) {
        return R.ok(ruleService.queryById(id));
    }

    /**
     * 获取所有启用的规则
     */
    @SaCheckPermission("metadata:security:rules:list")
    @GetMapping("/all")
    public R<List<SecSensitivityRuleVo>> listAll() {
        return R.ok(ruleService.listAll());
    }

    /**
     * 新增敏感识别规则
     */
    @SaCheckPermission("metadata:security:rules:add")
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SecSensitivityRuleVo vo) {
        return R.ok(ruleService.insert(vo));
    }

    /**
     * 修改敏感识别规则
     */
    @SaCheckPermission("metadata:security:rules:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SecSensitivityRuleVo vo) {
        return toAjax(ruleService.update(vo));
    }

    /**
     * 删除敏感识别规则
     */
    @SaCheckPermission("metadata:security:rules:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(ruleService.deleteByIds(ids));
    }
}
