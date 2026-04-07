package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.vo.DqcQualityScoreVo;
import org.dromara.metadata.service.IDqcQualityScoreService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 数据质量评分控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@Tag(name = "数据质量评分管理")
@RequestMapping("/system/metadata/dqc/score")
public class DqcQualityScoreController {

    private final IDqcQualityScoreService qualityScoreService;

    @Operation(summary = "查询评分列表")
    @SaCheckPermission("metadata:dqc:score:list")
    @GetMapping("/list")
    public TableDataInfo<DqcQualityScoreVo> list(DqcQualityScoreVo vo, PageQuery pageQuery) {
        return qualityScoreService.queryPageList(vo, pageQuery);
    }

    @Operation(summary = "导出评分列表")
    @SaCheckPermission("metadata:dqc:score:export")
    @PostMapping("/export")
    public void export(DqcQualityScoreVo vo, HttpServletResponse response) {
        List<DqcQualityScoreVo> list = qualityScoreService.queryPageList(vo, new PageQuery(Integer.MAX_VALUE, 1)).getRows();
        ExcelUtil.exportExcel(list, "质量评分", DqcQualityScoreVo.class, response);
    }

    @Operation(summary = "查询评分详情")
    @SaCheckPermission("metadata:dqc:score:query")
    @GetMapping("/{id}")
    public R<DqcQualityScoreVo> getInfo(@PathVariable Long id) {
        return R.ok(qualityScoreService.queryById(id));
    }

    @Operation(summary = "查询评分趋势（仪表盘用）")
    @SaCheckPermission("metadata:dqc:score:list")
    @GetMapping("/trend")
    public R<List<DqcQualityScoreVo>> trend(@RequestParam(defaultValue = "30") int days) {
        return R.ok(qualityScoreService.queryTrend(days));
    }

    @Operation(summary = "查询指定表的最新评分")
    @SaCheckPermission("metadata:dqc:score:query")
    @GetMapping("/latest")
    public R<DqcQualityScoreVo> latest(
            @RequestParam Long dsId,
            @RequestParam String tableName
    ) {
        return R.ok(qualityScoreService.queryLatestByTable(dsId, tableName));
    }
}
