package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.GovGlossaryCategoryBo;
import org.dromara.metadata.domain.vo.GovGlossaryCategoryVo;
import org.dromara.metadata.service.IGovGlossaryCategoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据治理术语分类Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/glossary/category")
public class GovGlossaryCategoryController extends BaseController {

    private final IGovGlossaryCategoryService categoryService;

    /**
     * 查询分类列表
     */
    @SaCheckPermission("metadata:glossary:category:list")
    @GetMapping("/list")
    public TableDataInfo<GovGlossaryCategoryVo> list(GovGlossaryCategoryBo bo, PageQuery pageQuery) {
        return categoryService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取分类树结构
     */
    @SaCheckPermission("metadata:glossary:category:list")
    @GetMapping("/tree")
    public R<List<GovGlossaryCategoryVo>> tree() {
        return R.ok(categoryService.listTree());
    }

    /**
     * 获取分类下拉选项
     */
    @SaCheckPermission("metadata:glossary:category:list")
    @GetMapping("/options")
    public R<List<GovGlossaryCategoryVo>> options() {
        return R.ok(categoryService.queryOptions());
    }

    /**
     * 获取分类详细信息
     */
    @SaCheckPermission("metadata:glossary:category:query")
    @GetMapping("/{id}")
    public R<GovGlossaryCategoryVo> getInfo(@PathVariable Long id) {
        return R.ok(categoryService.queryById(id));
    }

    /**
     * 新增分类
     */
    @SaCheckPermission("metadata:glossary:category:add")
    @Log(title = "数据治理术语分类")
    @PostMapping
    public R<Void> add(@Validated @RequestBody GovGlossaryCategoryBo bo) {
        return toAjax(categoryService.insertByBo(bo) > 0);
    }

    /**
     * 修改分类
     */
    @SaCheckPermission("metadata:glossary:category:edit")
    @Log(title = "数据治理术语分类")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody GovGlossaryCategoryBo bo) {
        return toAjax(categoryService.updateByBo(bo));
    }

    /**
     * 删除分类
     */
    @SaCheckPermission("metadata:glossary:category:remove")
    @Log(title = "数据治理术语分类")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        return toAjax(categoryService.deleteByIds(ids));
    }
}
