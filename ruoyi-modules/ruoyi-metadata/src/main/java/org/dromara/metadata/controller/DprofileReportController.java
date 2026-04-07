package org.dromara.metadata.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.vo.DprofileColumnReportVo;
import org.dromara.metadata.domain.vo.DprofileReportVo;
import org.dromara.metadata.service.IDprofileReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据探查报告控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/dprofile/report")
public class DprofileReportController extends BaseController {

    private final IDprofileReportService reportService;

    /**
     * 分页查询探查报告列表
     */
    @GetMapping("/list")
    public TableDataInfo<DprofileReportVo> list(DprofileReportVo vo, PageQuery pageQuery) {
        return reportService.queryPageList(vo, pageQuery);
    }

    /**
     * 查询探查报告详情
     */
    @GetMapping("/{id}")
    public R<DprofileReportVo> getInfo(@PathVariable Long id) {
        return R.ok(reportService.queryById(id));
    }

    /**
     * 查询列级探查报告
     */
    @GetMapping("/{id}/columns")
    public R<List<DprofileColumnReportVo>> getColumns(@PathVariable Long id) {
        return R.ok(reportService.queryColumnsByReportId(id));
    }
}
