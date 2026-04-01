package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.vo.SecClassificationVo;
import org.dromara.metadata.service.ISecClassificationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据分类管理Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/security/classification")
public class SecClassificationController extends BaseController {

    private final ISecClassificationService classificationService;

    /**
     * 查询数据分类列表
     */
    @SaCheckPermission("metadata:security:classification:list")
    @GetMapping("/list")
    public TableDataInfo<SecClassificationVo> list(SecClassificationVo vo, PageQuery pageQuery) {
        return classificationService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取数据分类详细信息
     */
    @SaCheckPermission("metadata:security:classification:query")
    @GetMapping("/{id}")
    public R<SecClassificationVo> getInfo(@PathVariable Long id) {
        return R.ok(classificationService.queryById(id));
    }

    /**
     * 获取所有启用的分类
     */
    @SaCheckPermission("metadata:security:classification:list")
    @GetMapping("/all")
    public R<List<SecClassificationVo>> listAll() {
        return R.ok(classificationService.listAll());
    }

    /**
     * 新增数据分类
     */
    @SaCheckPermission("metadata:security:classification:add")
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SecClassificationVo vo) {
        return R.ok(classificationService.insert(vo));
    }

    /**
     * 修改数据分类
     */
    @SaCheckPermission("metadata:security:classification:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SecClassificationVo vo) {
        return toAjax(classificationService.update(vo));
    }

    /**
     * 删除数据分类
     */
    @SaCheckPermission("metadata:security:classification:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(classificationService.deleteByIds(ids));
    }
}
