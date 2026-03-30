package com.bagdatahouse.server.controller.dqc;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqcExecutionDTO;
import com.bagdatahouse.core.entity.DqcExecution;
import com.bagdatahouse.core.entity.DqcExecutionDetail;
import com.bagdatahouse.core.vo.ExecutionTriggerVO;
import com.bagdatahouse.core.vo.QualityReportVO;
import com.bagdatahouse.core.vo.QualityScoreTrendVO;
import com.bagdatahouse.core.vo.QualityScoreOverviewVO;
import com.bagdatahouse.core.vo.DimensionScoreVO;
import com.bagdatahouse.core.vo.QualityReportExecutionSheetVO;
import com.bagdatahouse.core.vo.QualityReportRuleDetailSheetVO;
import com.bagdatahouse.dqc.service.DqcExecutionService;
import com.bagdatahouse.dqc.service.DqcQualityScoreService;
import com.bagdatahouse.dqc.service.QualityReportService;
import com.bagdatahouse.core.entity.DqcQualityScore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * DQC质检执行控制器
 */
@Api(tags = "数据质量-质检执行与报告")
@RestController
@RequestMapping("/dqc/execution")
public class DqcExecutionController {

    private static final Logger log = LoggerFactory.getLogger(DqcExecutionController.class);

    @Autowired
    private DqcExecutionService executionService;

    @Autowired
    private DqcQualityScoreService qualityScoreService;

    @Autowired
    private QualityReportService qualityReportService;

    @PostMapping("/execute/{planId}")
    @ApiOperation("触发质检执行")
    public Result<ExecutionTriggerVO> execute(
            @ApiParam("方案ID") @PathVariable Long planId,
            @RequestParam(defaultValue = "MANUAL") String triggerType,
            @RequestParam(required = false) Long triggerUser
    ) {
        return executionService.execute(planId, triggerType, triggerUser);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询执行记录")
    public Result<Page<DqcExecution>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String triggerType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return executionService.page(pageNum, pageSize, planId, status, triggerType, startDate, endDate);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取执行记录详情")
    public Result<DqcExecution> getById(
            @ApiParam("执行ID") @PathVariable Long id
    ) {
        return executionService.getById(id);
    }

    @GetMapping("/no/{executionNo}")
    @ApiOperation("根据执行编号查询")
    public Result<DqcExecution> getByExecutionNo(
            @ApiParam("执行编号") @PathVariable String executionNo
    ) {
        return executionService.getByExecutionNo(executionNo);
    }

    @GetMapping("/{id}/details")
    @ApiOperation("获取执行明细列表")
    public Result<List<DqcExecutionDetail>> getExecutionDetails(
            @ApiParam("执行ID") @PathVariable Long id
    ) {
        return executionService.getExecutionDetails(id);
    }

    @GetMapping("/details/{detailId}")
    @ApiOperation("获取执行明细详情")
    public Result<DqcExecutionDetail> getDetailById(
            @ApiParam("明细ID") @PathVariable Long detailId
    ) {
        return executionService.getDetailById(detailId);
    }

    // ==================== 质量报告API（综合版）====================

    @GetMapping("/report/plan/{planId}")
    @ApiOperation("获取质检方案综合报告（含概览+明细+趋势+问题分析）")
    public Result<QualityReportVO> getPlanReport(
            @ApiParam("方案ID") @PathVariable Long planId,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate,
            @ApiParam("数据层筛选") @RequestParam(required = false) String layerCode
    ) {
        return qualityReportService.getReport(planId, startDate, endDate, layerCode);
    }

    @GetMapping("/report/overall")
    @ApiOperation("获取整体质量综合报告（全部执行数据聚合）")
    public Result<QualityReportVO> getOverallReport(
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return qualityReportService.getOverallReport(startDate, endDate);
    }

    @GetMapping("/report/execution/{executionId}")
    @ApiOperation("获取指定执行记录的综合报告")
    public Result<QualityReportVO> getExecutionReport(
            @ApiParam("执行ID") @PathVariable Long executionId
    ) {
        return qualityReportService.getExecutionReport(executionId);
    }

    @GetMapping("/report/export")
    @ApiOperation("导出质量报告（多Sheet Excel，包含概览+明细+维度评分+问题分析）")
    public void exportReport(
            @ApiParam("方案ID（可选，不填则导出全部）") @RequestParam(required = false) Long planId,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate,
            HttpServletResponse response
    ) {
        byte[] data = qualityReportService.exportReport(planId, startDate, endDate).getData();
        String filename = "质量报告_" + System.currentTimeMillis() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        try {
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("导出报告文件失败", e);
        }
    }

    @GetMapping("/report/export/execution/{executionId}")
    @ApiOperation("导出指定执行记录的详细报告（多Sheet Excel）")
    public void exportExecutionReport(
            @ApiParam("执行ID") @PathVariable Long executionId,
            HttpServletResponse response
    ) {
        byte[] data = qualityReportService.exportExecutionReport(executionId).getData();
        String filename = "执行报告_" + executionId + "_" + System.currentTimeMillis() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        try {
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("导出执行报告文件失败", e);
        }
    }

    // ==================== 评分相关API ====================

    @GetMapping("/score/trend")
    @ApiOperation("获取质量评分趋势")
    public Result<QualityScoreTrendVO> getScoreTrend(
            @ApiParam("维度（可选）") @RequestParam(required = false) String dimension,
            @ApiParam("数据层（可选）") @RequestParam(required = false) String layerCode,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return qualityScoreService.getScoreTrend(dimension, layerCode, startDate, endDate);
    }

    @GetMapping("/score/trend/comparison")
    @ApiOperation("获取评分趋势对比")
    public Result<Map<String, Object>> getTrendComparison(
            @ApiParam("数据层") @RequestParam(required = false) String layerCode,
            @ApiParam("当前周期开始") @RequestParam String currentPeriodStart,
            @ApiParam("当前周期结束") @RequestParam String currentPeriodEnd,
            @ApiParam("对比周期开始") @RequestParam String previousPeriodStart,
            @ApiParam("对比周期结束") @RequestParam String previousPeriodEnd
    ) {
        return qualityScoreService.getTrendComparison(layerCode,
                currentPeriodStart, currentPeriodEnd, previousPeriodStart, previousPeriodEnd);
    }

    @GetMapping("/score/overview")
    @ApiOperation("获取质量评分概览")
    public Result<QualityScoreOverviewVO> getScoreOverview(
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return qualityScoreService.getScoreOverview(startDate, endDate);
    }

    @GetMapping("/score/dimensions")
    @ApiOperation("获取各维度评分详情")
    public Result<List<DimensionScoreVO>> getDimensionScores(
            @ApiParam("维度") @RequestParam(required = false) String dimension,
            @ApiParam("数据层（可选）") @RequestParam(required = false) String layerCode,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return qualityScoreService.getDimensionScores(dimension, layerCode, startDate, endDate);
    }

    @GetMapping("/score/distribution")
    @ApiOperation("获取评分分布统计")
    public Result<Map<String, Object>> getScoreDistribution(
            @ApiParam("数据层（可选）") @RequestParam(required = false) String layerCode,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return qualityScoreService.getScoreDistribution(layerCode, startDate, endDate);
    }

    @GetMapping("/score/alerts")
    @ApiOperation("获取质量异常告警列表")
    public Result<List<Map<String, Object>>> getQualityAlerts(
            @ApiParam("评分阈值") @RequestParam(defaultValue = "60") Integer scoreThreshold,
            @ApiParam("数据层（可选）") @RequestParam(required = false) String layerCode,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return qualityScoreService.getQualityAlerts(scoreThreshold, layerCode, startDate, endDate);
    }

    @GetMapping("/score/daily")
    @ApiOperation("按日统计评分")
    public Result<List<DqcQualityScore>> getDailyScores(
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId,
            @ApiParam("表名") @RequestParam(required = false) String tableName,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return qualityScoreService.getDailyScores(dsId, tableName, startDate, endDate);
    }

    @GetMapping("/score/tables")
    @ApiOperation("按表统计评分")
    public Result<List<DqcQualityScore>> getTableScores(
            @ApiParam("数据层") @RequestParam(required = false) String layerCode,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return qualityScoreService.getTableScores(layerCode, startDate, endDate);
    }

    @GetMapping("/score/layers")
    @ApiOperation("按数据层统计评分")
    public Result<Map<String, BigDecimal>> getLayerScoreSummary(
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate
    ) {
        return qualityScoreService.getLayerScoreSummary(startDate, endDate);
    }
}
