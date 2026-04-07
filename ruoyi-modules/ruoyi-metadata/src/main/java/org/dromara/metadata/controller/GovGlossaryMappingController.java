package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.GovGlossaryMappingBo;
import org.dromara.metadata.domain.vo.GovGlossaryMappingVo;
import org.dromara.metadata.service.IGovGlossaryMappingService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 治理 Glossary 术语映射控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@Tag(name = "术语映射管理")
@RequestMapping("/system/metadata/glossary/mapping")
public class GovGlossaryMappingController {

    private final IGovGlossaryMappingService mappingService;

    @Operation(summary = "查询映射列表")
    @SaCheckPermission("metadata:glossary:mapping:list")
    @GetMapping("/list")
    public TableDataInfo<GovGlossaryMappingVo> list(GovGlossaryMappingBo bo, PageQuery pageQuery) {
        return TableDataInfo.build(mappingService.queryPageList(bo, pageQuery));
    }

    @Operation(summary = "查询映射详情")
    @SaCheckPermission("metadata:glossary:mapping:query")
    @GetMapping("/{id}")
    public R<GovGlossaryMappingVo> getInfo(@PathVariable Long id) {
        return R.ok(mappingService.queryById(id));
    }

    @Operation(summary = "新增映射")
    @SaCheckPermission("metadata:glossary:mapping:add")
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody GovGlossaryMappingBo bo) {
        return R.ok(mappingService.insertByBo(bo));
    }

    @Operation(summary = "修改映射")
    @SaCheckPermission("metadata:glossary:mapping:edit")
    @PutMapping
    public R<Integer> edit(@Validated(EditGroup.class) @RequestBody GovGlossaryMappingBo bo) {
        return R.ok(mappingService.updateByBo(bo));
    }

    @Operation(summary = "删除映射")
    @SaCheckPermission("metadata:glossary:mapping:remove")
    @DeleteMapping("/{ids}")
    public R<Integer> remove(@PathVariable List<Long> ids) {
        return R.ok(mappingService.deleteByIds(ids));
    }

    @Operation(summary = "根据术语ID查询映射")
    @SaCheckPermission("metadata:glossary:mapping:list")
    @GetMapping("/term/{termId}")
    public R<List<GovGlossaryMappingVo>> listByTerm(@PathVariable Long termId) {
        return R.ok(mappingService.listByTermId(termId));
    }

    @Operation(summary = "根据表名查询映射")
    @SaCheckPermission("metadata:glossary:mapping:list")
    @GetMapping("/table")
    public R<List<GovGlossaryMappingVo>> listByTable(
            @RequestParam Long dsId,
            @RequestParam String tableName
    ) {
        return R.ok(mappingService.listByTable(dsId, tableName));
    }
}
