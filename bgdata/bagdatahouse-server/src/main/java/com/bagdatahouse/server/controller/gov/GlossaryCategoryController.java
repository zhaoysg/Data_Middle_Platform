package com.bagdatahouse.server.controller.gov;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovGlossaryCategory;
import com.bagdatahouse.governance.glossary.dto.GlossaryCategoryQueryDTO;
import com.bagdatahouse.governance.glossary.dto.GlossaryCategorySaveDTO;
import com.bagdatahouse.governance.glossary.service.GlossaryCategoryService;
import com.bagdatahouse.governance.glossary.vo.GlossaryCategoryDetailVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryCategoryTreeVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryCategoryVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 术语分类管理接口
 */
@Api(tags = "术语分类管理")
@RestController
@RequestMapping("/gov/glossary-category")
public class GlossaryCategoryController {

    @Autowired
    private GlossaryCategoryService categoryService;

    @ApiOperation("新增分类")
    @PostMapping
    public Result<GovGlossaryCategory> save(@Validated @RequestBody GlossaryCategorySaveDTO dto) {
        return categoryService.save(dto);
    }

    @ApiOperation("更新分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody GlossaryCategorySaveDTO dto) {
        return categoryService.update(id, dto);
    }

    @ApiOperation("删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return categoryService.delete(id);
    }

    @ApiOperation("批量删除分类")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return categoryService.batchDelete(ids);
    }

    @ApiOperation("根据ID查询分类")
    @GetMapping("/{id}")
    public Result<GovGlossaryCategory> getById(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @ApiOperation("分页查询分类")
    @GetMapping("/page")
    public Result<Page<GlossaryCategoryVO>> page(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("分类名称（模糊匹配）") @RequestParam(required = false) String categoryName,
            @ApiParam("分类编码（精确匹配）") @RequestParam(required = false) String categoryCode,
            @ApiParam("状态：0-禁用，1-启用") @RequestParam(required = false) Integer status,
            @ApiParam("父分类ID") @RequestParam(required = false) Long parentId) {

        GlossaryCategoryQueryDTO queryDTO = GlossaryCategoryQueryDTO.builder()
                .categoryName(categoryName)
                .categoryCode(categoryCode)
                .status(status)
                .parentId(parentId)
                .build();

        return categoryService.page(pageNum, pageSize, queryDTO);
    }

    @ApiOperation("获取分类详情（含子分类）")
    @GetMapping("/detail/{id}")
    public Result<GlossaryCategoryDetailVO> getDetail(@PathVariable Long id) {
        return categoryService.getDetail(id);
    }

    @ApiOperation("获取分类树（用于左侧树形展示）")
    @GetMapping("/tree")
    public Result<List<GlossaryCategoryTreeVO>> getTree(
            @ApiParam("分类名称（模糊匹配）") @RequestParam(required = false) String categoryName,
            @ApiParam("状态") @RequestParam(required = false) Integer status) {

        GlossaryCategoryQueryDTO queryDTO = GlossaryCategoryQueryDTO.builder()
                .categoryName(categoryName)
                .status(status)
                .build();

        return categoryService.getTree(queryDTO);
    }

    @ApiOperation("获取全量分类树")
    @GetMapping("/tree/full")
    public Result<List<GlossaryCategoryTreeVO>> getFullTree() {
        return categoryService.getFullTree();
    }

    @ApiOperation("启用分类")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        return categoryService.enable(id);
    }

    @ApiOperation("禁用分类")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        return categoryService.disable(id);
    }

    @ApiOperation("移动分类（调整父节点）")
    @PostMapping("/{id}/move")
    public Result<Void> move(
            @PathVariable Long id,
            @ApiParam("新父分类ID，0表示移到顶级") @RequestParam(required = false, defaultValue = "0") Long newParentId) {
        return categoryService.move(id, newParentId);
    }

    @ApiOperation("获取分类列表（不分页，用于下拉选择）")
    @GetMapping("/list")
    public Result<List<GlossaryCategoryVO>> list(
            @ApiParam("分类名称（模糊匹配）") @RequestParam(required = false) String categoryName,
            @ApiParam("状态") @RequestParam(required = false) Integer status,
            @ApiParam("父分类ID") @RequestParam(required = false) Long parentId,
            @ApiParam("是否仅查询顶级") @RequestParam(required = false) Boolean topLevelOnly) {

        GlossaryCategoryQueryDTO queryDTO = GlossaryCategoryQueryDTO.builder()
                .categoryName(categoryName)
                .status(status)
                .parentId(parentId)
                .topLevelOnly(topLevelOnly)
                .build();

        return categoryService.list(queryDTO);
    }
}
