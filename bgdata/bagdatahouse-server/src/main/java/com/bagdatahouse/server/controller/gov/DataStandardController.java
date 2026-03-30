package com.bagdatahouse.server.controller.gov;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovDataStandard;
import com.bagdatahouse.governance.standard.dto.DataStandardQueryDTO;
import com.bagdatahouse.governance.standard.dto.DataStandardSaveDTO;
import com.bagdatahouse.governance.standard.service.DataStandardService;
import com.bagdatahouse.governance.standard.vo.DataStandardBindingVO;
import com.bagdatahouse.governance.standard.vo.DataStandardDetailVO;
import com.bagdatahouse.governance.standard.vo.DataStandardStatsVO;
import com.bagdatahouse.governance.standard.vo.DataStandardVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据标准管理接口
 */
@Api(tags = "数据标准管理")
@RestController
@RequestMapping("/gov/data-standard")
public class DataStandardController {

    @Autowired
    private DataStandardService dataStandardService;

    @ApiOperation("新增数据标准")
    @PostMapping
    public Result<GovDataStandard> save(@Validated @RequestBody DataStandardSaveDTO dto) {
        return dataStandardService.save(dto);
    }

    @ApiOperation("更新数据标准")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody DataStandardSaveDTO dto) {
        return dataStandardService.update(id, dto);
    }

    @ApiOperation("删除数据标准")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return dataStandardService.delete(id);
    }

    @ApiOperation("批量删除数据标准")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return dataStandardService.batchDelete(ids);
    }

    @ApiOperation("根据ID查询数据标准")
    @GetMapping("/{id}")
    public Result<GovDataStandard> getById(@PathVariable Long id) {
        return dataStandardService.getById(id);
    }

    @ApiOperation("分页查询数据标准")
    @GetMapping("/page")
    public Result<Page<DataStandardVO>> page(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("标准类型：CODE_STANDARD/NAMING_STANDARD/PRIMARY_DATA") @RequestParam(required = false) String standardType,
            @ApiParam("标准分类") @RequestParam(required = false) String standardCategory,
            @ApiParam("标准名称（模糊匹配）") @RequestParam(required = false) String standardName,
            @ApiParam("标准编码（精确匹配）") @RequestParam(required = false) String standardCode,
            @ApiParam("状态：DRAFT/PUBLISHED/DEPRECATED") @RequestParam(required = false) String status,
            @ApiParam("是否启用：0-禁用，1-启用") @RequestParam(required = false) Integer enabled,
            @ApiParam("业务域") @RequestParam(required = false) String bizDomain,
            @ApiParam("负责人用户ID") @RequestParam(required = false) Long ownerId,
            @ApiParam("部门ID") @RequestParam(required = false) Long deptId) {

        DataStandardQueryDTO queryDTO = DataStandardQueryDTO.builder()
                .standardType(standardType)
                .standardCategory(standardCategory)
                .standardName(standardName)
                .standardCode(standardCode)
                .status(status)
                .enabled(enabled)
                .bizDomain(bizDomain)
                .ownerId(ownerId)
                .deptId(deptId)
                .build();

        return dataStandardService.page(pageNum, pageSize, queryDTO);
    }

    @ApiOperation("获取数据标准详情（含绑定列表）")
    @GetMapping("/detail/{id}")
    public Result<DataStandardDetailVO> getDetail(@PathVariable Long id) {
        return dataStandardService.getDetail(id);
    }

    @ApiOperation("启用数据标准")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        return dataStandardService.enable(id);
    }

    @ApiOperation("禁用数据标准")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        return dataStandardService.disable(id);
    }

    @ApiOperation("发布数据标准")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        return dataStandardService.publish(id);
    }

    @ApiOperation("废弃数据标准")
    @PostMapping("/{id}/deprecate")
    public Result<Void> deprecate(@PathVariable Long id) {
        return dataStandardService.deprecate(id);
    }

    @ApiOperation("复制数据标准")
    @PostMapping("/{id}/copy")
    public Result<GovDataStandard> copy(@PathVariable Long id) {
        return dataStandardService.copy(id);
    }

    @ApiOperation("获取数据标准统计信息")
    @GetMapping("/stats")
    public Result<DataStandardStatsVO> getStats() {
        return dataStandardService.getStats();
    }

    @ApiOperation("获取数据标准列表（不分页，用于下拉选择）")
    @GetMapping("/list")
    public Result<List<DataStandardVO>> list(
            @ApiParam("标准类型") @RequestParam(required = false) String standardType,
            @ApiParam("标准分类") @RequestParam(required = false) String standardCategory,
            @ApiParam("状态") @RequestParam(required = false) String status,
            @ApiParam("是否启用") @RequestParam(required = false) Integer enabled) {

        DataStandardQueryDTO queryDTO = DataStandardQueryDTO.builder()
                .standardType(standardType)
                .standardCategory(standardCategory)
                .status(status)
                .enabled(enabled)
                .build();

        return dataStandardService.list(queryDTO);
    }

    @ApiOperation("绑定元数据到标准")
    @PostMapping("/{standardId}/bind")
    public Result<Void> bindMetadata(
            @PathVariable Long standardId,
            @ApiParam("元数据ID") @RequestParam Long metadataId,
            @ApiParam("目标列名（可选）") @RequestParam(required = false) String targetColumn,
            @ApiParam("不合规处理方式") @RequestParam(required = false) String enforceAction,
            @ApiParam("创建人用户ID") @RequestParam(required = false) Long createUser) {
        return dataStandardService.bindMetadata(standardId, metadataId, targetColumn, enforceAction, createUser);
    }

    @ApiOperation("批量绑定元数据到标准")
    @PostMapping("/{standardId}/bind/batch")
    public Result<Void> batchBindMetadata(
            @PathVariable Long standardId,
            @ApiParam("元数据ID列表") @RequestBody List<Long> metadataIds,
            @ApiParam("不合规处理方式") @RequestParam(required = false) String enforceAction,
            @ApiParam("创建人用户ID") @RequestParam(required = false) Long createUser) {
        return dataStandardService.batchBindMetadata(standardId, metadataIds, enforceAction, createUser);
    }

    @ApiOperation("解绑元数据")
    @DeleteMapping("/binding/{bindingId}")
    public Result<Void> unbindMetadata(@PathVariable Long bindingId) {
        return dataStandardService.unbindMetadata(bindingId);
    }

    @ApiOperation("批量解绑")
    @DeleteMapping("/binding/batch")
    public Result<Void> batchUnbindMetadata(@RequestBody List<Long> bindingIds) {
        return dataStandardService.batchUnbindMetadata(bindingIds);
    }

    @ApiOperation("获取标准的绑定列表")
    @GetMapping("/{standardId}/bindings")
    public Result<List<DataStandardBindingVO>> getBindings(@PathVariable Long standardId) {
        return dataStandardService.getBindings(standardId);
    }

    @ApiOperation("获取元数据关联的标准列表")
    @GetMapping("/metadata/{metadataId}/standards")
    public Result<List<DataStandardVO>> getStandardsByMetadata(@PathVariable Long metadataId) {
        return dataStandardService.getStandardsByMetadata(metadataId);
    }
}
