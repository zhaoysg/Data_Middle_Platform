package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.vo.SecLevelVo;
import org.dromara.metadata.service.ISecLevelService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 敏感等级管理Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/security/level")
public class SecLevelController extends BaseController {

    private final ISecLevelService levelService;

    /**
     * 查询敏感等级列表
     */
    @SaCheckPermission("metadata:security:level:list")
    @GetMapping("/list")
    public TableDataInfo<SecLevelVo> list(SecLevelVo vo, PageQuery pageQuery) {
        return levelService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取敏感等级详细信息
     */
    @SaCheckPermission("metadata:security:level:query")
    @GetMapping("/{id}")
    public R<SecLevelVo> getInfo(@PathVariable Long id) {
        return R.ok(levelService.queryById(id));
    }

    /**
     * 获取所有启用的等级
     */
    @SaCheckPermission("metadata:security:level:list")
    @GetMapping("/all")
    public R<List<SecLevelVo>> listAll() {
        return R.ok(levelService.listAll());
    }

    /**
     * 新增敏感等级
     */
    @SaCheckPermission("metadata:security:level:add")
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SecLevelVo vo) {
        return R.ok(levelService.insert(vo));
    }

    /**
     * 修改敏感等级
     */
    @SaCheckPermission("metadata:security:level:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SecLevelVo vo) {
        return toAjax(levelService.update(vo));
    }

    /**
     * 删除敏感等级
     */
    @SaCheckPermission("metadata:security:level:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(levelService.deleteByIds(ids));
    }
}
