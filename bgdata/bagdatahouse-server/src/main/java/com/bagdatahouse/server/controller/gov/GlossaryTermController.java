package com.bagdatahouse.server.controller.gov;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovGlossaryTerm;
import com.bagdatahouse.governance.glossary.dto.GlossaryTermQueryDTO;
import com.bagdatahouse.governance.glossary.dto.GlossaryTermSaveDTO;
import com.bagdatahouse.governance.glossary.service.GlossaryTermService;
import com.bagdatahouse.governance.glossary.vo.GlossaryMappingVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryStatsVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryTermDetailVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryTermVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 术语管理接口
 */
@Slf4j
@Api(tags = "术语管理")
@RestController
@RequestMapping("/gov/glossary-term")
public class GlossaryTermController {

    @Autowired
    private GlossaryTermService termService;

    @ApiOperation("新增术语")
    @PostMapping
    public Result<GovGlossaryTerm> save(@Validated @RequestBody GlossaryTermSaveDTO dto) {
        return termService.save(dto);
    }

    @ApiOperation("更新术语")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody GlossaryTermSaveDTO dto) {
        return termService.update(id, dto);
    }

    @ApiOperation("删除术语")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return termService.delete(id);
    }

    @ApiOperation("批量删除术语")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return termService.batchDelete(ids);
    }

    @ApiOperation("根据ID查询术语")
    @GetMapping("/{id}")
    public Result<GovGlossaryTerm> getById(@PathVariable Long id) {
        return termService.getById(id);
    }

    @ApiOperation("分页查询术语")
    @GetMapping("/page")
    public Result<Page<GlossaryTermVO>> page(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("术语名称（模糊匹配）") @RequestParam(required = false) String termName,
            @ApiParam("术语编码（精确匹配）") @RequestParam(required = false) String termCode,
            @ApiParam("英文名（模糊匹配）") @RequestParam(required = false) String termNameEn,
            @ApiParam("所属分类ID") @RequestParam(required = false) Long categoryId,
            @ApiParam("业务域") @RequestParam(required = false) String bizDomain,
            @ApiParam("状态：DRAFT-草稿/PUBLISHED-已发布/DEPRECATED-已废弃") @RequestParam(required = false) String status,
            @ApiParam("是否启用：0-禁用，1-启用") @RequestParam(required = false) Integer enabled,
            @ApiParam("数据类型") @RequestParam(required = false) String dataType,
            @ApiParam("敏感等级") @RequestParam(required = false) String sensitivityLevel,
            @ApiParam("负责人用户ID") @RequestParam(required = false) Long ownerId,
            @ApiParam("部门ID") @RequestParam(required = false) Long deptId) {

        GlossaryTermQueryDTO queryDTO = GlossaryTermQueryDTO.builder()
                .termName(termName)
                .termCode(termCode)
                .termNameEn(termNameEn)
                .categoryId(categoryId)
                .bizDomain(bizDomain)
                .status(status)
                .enabled(enabled)
                .dataType(dataType)
                .sensitivityLevel(sensitivityLevel)
                .ownerId(ownerId)
                .deptId(deptId)
                .build();

        return termService.page(pageNum, pageSize, queryDTO);
    }

    @ApiOperation("获取术语详情（含映射列表）")
    @GetMapping("/detail/{id}")
    public Result<GlossaryTermDetailVO> getDetail(@PathVariable Long id) {
        return termService.getDetail(id);
    }

    @ApiOperation("启用术语")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        return termService.enable(id);
    }

    @ApiOperation("禁用术语")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        return termService.disable(id);
    }

    @ApiOperation("发布术语")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        return termService.publish(id);
    }

    @ApiOperation("废弃术语")
    @PostMapping("/{id}/deprecate")
    public Result<Void> deprecate(@PathVariable Long id) {
        return termService.deprecate(id);
    }

    @ApiOperation("复制术语")
    @PostMapping("/{id}/copy")
    public Result<GovGlossaryTerm> copy(@PathVariable Long id) {
        return termService.copy(id);
    }

    @ApiOperation("获取术语列表（不分页，用于下拉选择）")
    @GetMapping("/list")
    public Result<List<GlossaryTermVO>> list(
            @ApiParam("术语名称（模糊匹配）") @RequestParam(required = false) String termName,
            @ApiParam("所属分类ID") @RequestParam(required = false) Long categoryId,
            @ApiParam("状态") @RequestParam(required = false) String status,
            @ApiParam("是否启用") @RequestParam(required = false) Integer enabled,
            @ApiParam("业务域") @RequestParam(required = false) String bizDomain) {

        GlossaryTermQueryDTO queryDTO = GlossaryTermQueryDTO.builder()
                .termName(termName)
                .categoryId(categoryId)
                .status(status)
                .enabled(enabled)
                .bizDomain(bizDomain)
                .build();

        return termService.list(queryDTO);
    }

    @ApiOperation("获取术语库统计信息")
    @GetMapping("/stats")
    public Result<GlossaryStatsVO> getStats() {
        return termService.getStats();
    }

    @ApiOperation("全局搜索术语（按名称/编码/英文名/别名模糊匹配）")
    @GetMapping("/search")
    public Result<List<GlossaryTermVO>> search(
            @ApiParam("搜索关键词") @RequestParam String keyword) {
        return termService.search(keyword);
    }

    @ApiOperation("获取术语的映射列表")
    @GetMapping("/{id}/mappings")
    public Result<List<GlossaryMappingVO>> getMappings(@PathVariable Long id) {
        return termService.getMappings(id);
    }

    @ApiOperation("批量导入术语（Excel）")
    @PostMapping("/import")
    public Result<Map<String, Object>> importTerms(
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(1, "请选择要导入的文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return Result.fail(1, "仅支持 .xlsx 或 .xls 格式的 Excel 文件");
        }
        try {
            return termService.importTerms(file.getInputStream());
        } catch (IOException e) {
            log.error("导入术语失败", e);
            return Result.fail(1, "文件读取失败: " + e.getMessage());
        }
    }

    @ApiOperation("导出术语（Excel）")
    @GetMapping("/export")
    public void exportTerms(
            @ApiParam("分类ID") @RequestParam(required = false) Long categoryId,
            @ApiParam("业务域") @RequestParam(required = false) String bizDomain,
            @ApiParam("状态") @RequestParam(required = false) String status,
            @ApiParam("敏感等级") @RequestParam(required = false) String sensitivityLevel,
            HttpServletResponse response) {
        GlossaryTermQueryDTO queryDTO = GlossaryTermQueryDTO.builder()
                .categoryId(categoryId)
                .bizDomain(bizDomain)
                .status(status)
                .sensitivityLevel(sensitivityLevel)
                .build();
        try {
            byte[] excelData = termService.exportTerms(queryDTO);
            String filename = "术语库导出_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            response.setHeader("Content-Length", String.valueOf(excelData.length));
            response.getOutputStream().write(excelData);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("导出术语失败", e);
        }
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        try {
            byte[] templateData = termService.getImportTemplate();
            String filename = "术语导入模板.xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            response.setHeader("Content-Length", String.valueOf(templateData.length));
            response.getOutputStream().write(templateData);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("下载导入模板失败", e);
        }
    }
}
