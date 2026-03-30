package com.bagdatahouse.server.controller.gov;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovGlossaryMapping;
import com.bagdatahouse.core.entity.SysUser;
import com.bagdatahouse.core.mapper.SysUserMapper;
import com.bagdatahouse.governance.glossary.dto.GlossaryMappingDTO;
import com.bagdatahouse.governance.glossary.service.GlossaryMappingService;
import com.bagdatahouse.governance.glossary.vo.GlossaryMappingVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 术语-字段映射管理接口
 */
@Api(tags = "术语-字段映射管理")
@RestController
@RequestMapping("/gov/glossary-mapping")
public class GlossaryMappingController {

    @Autowired
    private GlossaryMappingService mappingService;

    @Autowired
    private SysUserMapper userMapper;

    @ApiOperation("新增映射")
    @PostMapping
    public Result<GovGlossaryMapping> save(@Validated @RequestBody GlossaryMappingDTO dto) {
        Long userId = getCurrentUserId();
        dto.setCreateUser(userId);
        return mappingService.save(dto);
    }

    @ApiOperation("批量新增映射")
    @PostMapping("/batch/{termId}")
    public Result<Void> batchSave(
            @ApiParam("术语ID") @PathVariable Long termId,
            @RequestBody List<GlossaryMappingDTO> mappings) {
        Long userId = getCurrentUserId();
        return mappingService.batchSave(termId, mappings, userId);
    }

    @ApiOperation("更新映射")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody GlossaryMappingDTO dto) {
        return mappingService.update(id, dto);
    }

    @ApiOperation("删除映射")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return mappingService.delete(id);
    }

    @ApiOperation("批量删除映射")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return mappingService.batchDelete(ids);
    }

    @ApiOperation("审批映射（通过）")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return mappingService.approve(id, userId);
    }

    @ApiOperation("驳回映射")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        String reason = body.get("rejectReason");
        return mappingService.reject(id, userId, reason);
    }

    @ApiOperation("批量审批（通过）")
    @PostMapping("/batch/approve")
    public Result<Map<String, Integer>> batchApprove(@RequestBody List<Long> ids) {
        Long userId = getCurrentUserId();
        int count = 0;
        for (Long id : ids) {
            try {
                mappingService.approve(id, userId);
                count++;
            } catch (Exception ignored) {
            }
        }
        Map<String, Integer> result = new HashMap<>();
        result.put("successCount", count);
        result.put("failCount", ids.size() - count);
        return Result.success(result);
    }

    @ApiOperation("批量驳回")
    @PostMapping("/batch/reject")
    public Result<Map<String, Integer>> batchReject(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        String reason = (String) body.get("rejectReason");
        Long userId = getCurrentUserId();
        int count = 0;
        for (Long id : ids) {
            try {
                mappingService.reject(id, userId, reason);
                count++;
            } catch (Exception ignored) {
            }
        }
        Map<String, Integer> result = new HashMap<>();
        result.put("successCount", count);
        result.put("failCount", ids.size() - count);
        return Result.success(result);
    }

    @ApiOperation("根据术语ID查询所有映射")
    @GetMapping("/term/{termId}")
    public Result<List<GlossaryMappingVO>> getByTermId(@PathVariable Long termId) {
        return Result.success(getMappingVOList(termId));
    }

    @ApiOperation("分页查询待审批映射")
    @GetMapping("/pending/page")
    public Result<Page<GlossaryMappingVO>> pagePending(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("术语名称（模糊匹配）") @RequestParam(required = false) String termName,
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId,
            @ApiParam("状态：PENDING/APPROVED/REJECTED") @RequestParam(required = false) String status) {

        return mappingService.pagePending(pageNum, pageSize, termName, dsId, status);
    }

    @ApiOperation("查询所有待审批映射")
    @GetMapping("/pending/list")
    public Result<List<GlossaryMappingVO>> listPending(
            @ApiParam("术语名称（模糊匹配）") @RequestParam(required = false) String termName,
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId) {
        return mappingService.listPending(termName, dsId);
    }

    @ApiOperation("根据元数据字段查询关联的术语映射")
    @GetMapping("/metadata")
    public Result<List<GlossaryMappingVO>> getByMetadata(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName,
            @ApiParam("字段名") @RequestParam(required = false) String columnName) {
        return mappingService.getByMetadataField(dsId, tableName, columnName);
    }

    @ApiOperation("根据数据源查询已审批映射")
    @GetMapping("/approved/by-datasource/{dsId}")
    public Result<List<GlossaryMappingVO>> getApprovedByDatasource(@PathVariable Long dsId) {
        return mappingService.getApprovedByDatasource(dsId);
    }

    @ApiOperation("根据数据源和表查询已审批映射")
    @GetMapping("/approved/by-table")
    public Result<List<GlossaryMappingVO>> getApprovedByTable(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName) {
        return mappingService.getApprovedByTable(dsId, tableName);
    }

    // ===== private helpers =====

    private List<GlossaryMappingVO> getMappingVOList(Long termId) {
        return mappingService.getByTermId(termId).getData();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SysUser user) {
            return user.getId();
        }
        return null;
    }
}
