package com.bagdatahouse.dqc.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqcExecution;
import com.bagdatahouse.core.entity.DqcExecutionDetail;
import com.bagdatahouse.core.entity.DqcPlan;
import com.bagdatahouse.core.entity.DqcQualityScore;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.mapper.DqcExecutionDetailMapper;
import com.bagdatahouse.core.mapper.DqcExecutionMapper;
import com.bagdatahouse.core.mapper.DqcPlanMapper;
import com.bagdatahouse.core.mapper.DqcQualityScoreMapper;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.vo.QualityReportExecutionSheetVO;
import com.bagdatahouse.core.vo.QualityReportRuleDetailSheetVO;
import com.bagdatahouse.core.vo.QualityReportVO;
import com.bagdatahouse.dqc.engine.scorer.QualityScorer;
import com.bagdatahouse.dqc.service.QualityReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 质量报告服务实现
 *
 * 功能说明：
 * 1. 从 dqc_execution + dqc_execution_detail 实时聚合真实数据
 * 2. 使用 QualityScorer 计算六维度真实评分
 * 3. 生成包含概览+明细+趋势+问题的综合报告
 * 4. 多Sheet Excel导出（概览 + 明细）
 */
@Service
public class QualityReportServiceImpl implements QualityReportService {

    private static final Logger log = LoggerFactory.getLogger(QualityReportServiceImpl.class);

    private static final int EXCELLENT_THRESHOLD = 90;
    private static final int GOOD_THRESHOLD = 70;
    private static final int WARNING_THRESHOLD = 60;
    private static final int TOP_FAILED_RULES = 10;
    private static final int TOP_ATTENTION_TABLES = 10;

    @Autowired
    private DqcExecutionMapper executionMapper;

    @Autowired
    private DqcExecutionDetailMapper detailMapper;

    @Autowired
    private DqcPlanMapper planMapper;

    @Autowired
    private DqcQualityScoreMapper qualityScoreMapper;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private QualityScorer qualityScorer;

    // ==================== 报告查询接口 ====================

    @Override
    public Result<QualityReportVO> getReport(Long planId, String startDate, String endDate, String layerCode) {
        DqcPlan plan = null;
        if (planId != null) {
            plan = planMapper.selectById(planId);
        }

        LambdaQueryWrapper<DqcExecution> wrapper = buildExecutionWrapper(planId, layerCode, startDate, endDate);
        wrapper.in(DqcExecution::getStatus, "SUCCESS", "BLOCKED");
        wrapper.orderByDesc(DqcExecution::getCreateTime);
        List<DqcExecution> executions = executionMapper.selectList(wrapper);

        QualityReportVO report = buildComprehensiveReport(executions, plan, startDate, endDate);
        return Result.success(report);
    }

    @Override
    public Result<QualityReportVO> getOverallReport(String startDate, String endDate) {
        LambdaQueryWrapper<DqcExecution> wrapper = buildExecutionWrapper(null, null, startDate, endDate);
        wrapper.in(DqcExecution::getStatus, "SUCCESS", "BLOCKED");
        wrapper.orderByDesc(DqcExecution::getCreateTime);
        List<DqcExecution> executions = executionMapper.selectList(wrapper);

        QualityReportVO report = buildComprehensiveReport(executions, null, startDate, endDate);
        return Result.success(report);
    }

    @Override
    public Result<QualityReportVO> getExecutionReport(Long executionId) {
        DqcExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new BusinessException(404, "执行记录不存在");
        }

        LambdaQueryWrapper<DqcExecutionDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(DqcExecutionDetail::getExecutionId, executionId);
        List<DqcExecutionDetail> details = detailMapper.selectList(detailWrapper);

        List<DqcExecution> executions = new ArrayList<>();
        executions.add(execution);

        DqcPlan plan = planMapper.selectById(execution.getPlanId());
        QualityReportVO report = buildSingleExecutionReport(execution, details, plan);
        return Result.success(report);
    }

    // ==================== 导出接口 ====================

    @Override
    public Result<byte[]> exportReport(Long planId, String startDate, String endDate) {
        DqcPlan plan = planId != null ? planMapper.selectById(planId) : null;

        LambdaQueryWrapper<DqcExecution> wrapper = buildExecutionWrapper(planId, null, startDate, endDate);
        wrapper.in(DqcExecution::getStatus, "SUCCESS", "BLOCKED");
        wrapper.orderByDesc(DqcExecution::getCreateTime);
        List<DqcExecution> executions = executionMapper.selectList(wrapper);

        if (executions.isEmpty()) {
            throw new BusinessException(4001, "没有可导出的执行记录");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ExcelWriter writer = EasyExcel.write(out).build();

            // Sheet1: 执行概览
            WriteSheet sheet1 = EasyExcel.writerSheet(0, "执行概览")
                    .head(QualityReportExecutionSheetVO.class)
                    .build();
            List<QualityReportExecutionSheetVO> execSheetData = buildExecutionSheetData(executions);
            writer.write(execSheetData, sheet1);

            // Sheet2: 规则明细
            WriteSheet sheet2 = EasyExcel.writerSheet(1, "规则明细")
                    .head(QualityReportRuleDetailSheetVO.class)
                    .build();
            List<QualityReportRuleDetailSheetVO> ruleSheetData = buildRuleDetailSheetDataByExecutions(executions);
            writer.write(ruleSheetData, sheet2);

            // Sheet3: 维度评分汇总
            WriteSheet sheet3 = EasyExcel.writerSheet(2, "维度评分汇总")
                    .head(DimensionScoreExportVO.class)
                    .build();
            List<DimensionScoreExportVO> dimData = buildDimensionScoreSheetData(executions);
            writer.write(dimData, sheet3);

            // Sheet4: 问题分析
            WriteSheet sheet4 = EasyExcel.writerSheet(3, "问题分析")
                    .head(ProblemAnalysisExportVO.class)
                    .build();
            List<ProblemAnalysisExportVO> problemData = buildProblemAnalysisSheetData(executions);
            writer.write(problemData, sheet4);

            writer.finish();

            log.info("质量报告导出成功: planId={}, executions={}, sheets=4", planId, executions.size());
            return Result.success(out.toByteArray());
        } catch (Exception e) {
            log.error("导出质量报告失败", e);
            throw new BusinessException(500, "导出失败: " + e.getMessage());
        }
    }

    @Override
    public Result<byte[]> exportExecutionReport(Long executionId) {
        DqcExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new BusinessException(404, "执行记录不存在");
        }

        LambdaQueryWrapper<DqcExecutionDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(DqcExecutionDetail::getExecutionId, executionId);
        List<DqcExecutionDetail> details = detailMapper.selectList(detailWrapper);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ExcelWriter writer = EasyExcel.write(out).build();

            // Sheet1: 执行概览
            WriteSheet sheet1 = EasyExcel.writerSheet(0, "执行概览")
                    .head(QualityReportExecutionSheetVO.class)
                    .build();
            DqcPlan plan = planMapper.selectById(execution.getPlanId());
            List<QualityReportExecutionSheetVO> execData = buildSingleExecutionSheetData(execution, details, plan);
            writer.write(execData, sheet1);

            // Sheet2: 规则明细
            WriteSheet sheet2 = EasyExcel.writerSheet(1, "规则明细")
                    .head(QualityReportRuleDetailSheetVO.class)
                    .build();
            List<QualityReportRuleDetailSheetVO> ruleData = details.stream()
                    .map(this::convertToRuleDetailSheet)
                    .collect(Collectors.toList());
            writer.write(ruleData, sheet2);

            // Sheet3: 维度评分
            WriteSheet sheet3 = EasyExcel.writerSheet(2, "维度评分")
                    .head(DimensionScoreExportVO.class)
                    .build();
            QualityScorer.ScoreBreakdown breakdown = qualityScorer.calculateBreakdown(details);
            List<DimensionScoreExportVO> dimData = buildDimensionScoreSheetDataSingle(breakdown);
            writer.write(dimData, sheet3);

            writer.finish();

            log.info("执行记录导出成功: executionId={}, detailCount={}", executionId, details.size());
            return Result.success(out.toByteArray());
        } catch (Exception e) {
            log.error("导出执行记录失败", e);
            throw new BusinessException(500, "导出失败: " + e.getMessage());
        }
    }

    @Override
    public List<QualityReportExecutionSheetVO> getExecutionSheetData(Long planId, String startDate, String endDate) {
        LambdaQueryWrapper<DqcExecution> wrapper = buildExecutionWrapper(planId, null, startDate, endDate);
        wrapper.in(DqcExecution::getStatus, "SUCCESS", "BLOCKED");
        wrapper.orderByDesc(DqcExecution::getCreateTime);
        List<DqcExecution> executions = executionMapper.selectList(wrapper);
        return buildExecutionSheetData(executions);
    }

    @Override
    public List<QualityReportRuleDetailSheetVO> getRuleDetailSheetData(Long planId, String startDate, String endDate) {
        LambdaQueryWrapper<DqcExecution> wrapper = buildExecutionWrapper(planId, null, startDate, endDate);
        wrapper.in(DqcExecution::getStatus, "SUCCESS", "BLOCKED");
        List<DqcExecution> executions = executionMapper.selectList(wrapper);
        return buildRuleDetailSheetDataByExecutions(executions);
    }

    @Override
    public List<QualityReportRuleDetailSheetVO> getRuleDetailSheetDataByExecution(Long executionId) {
        LambdaQueryWrapper<DqcExecutionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcExecutionDetail::getExecutionId, executionId);
        wrapper.orderByAsc(DqcExecutionDetail::getId);
        return detailMapper.selectList(wrapper).stream()
                .map(this::convertToRuleDetailSheet)
                .collect(Collectors.toList());
    }

    // ==================== 报告构建核心逻辑 ====================

    private QualityReportVO buildComprehensiveReport(List<DqcExecution> executions, DqcPlan plan,
                                                     String startDate, String endDate) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        if (executions.isEmpty()) {
            return buildEmptyReport(plan, startDate, endDate, now);
        }

        // 1. 收集所有执行ID
        List<Long> executionIds = executions.stream().map(DqcExecution::getId).collect(Collectors.toList());

        // 2. 批量查询执行明细
        LambdaQueryWrapper<DqcExecutionDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(DqcExecutionDetail::getExecutionId, executionIds);
        List<DqcExecutionDetail> allDetails = detailMapper.selectList(detailWrapper);

        // 3. 按执行ID分组
        Map<Long, List<DqcExecutionDetail>> detailsByExecution = allDetails.stream()
                .collect(Collectors.groupingBy(DqcExecutionDetail::getExecutionId));

        // 4. 统计概览
        int totalRules = executions.stream().mapToInt(e -> e.getTotalRules() != null ? e.getTotalRules() : 0).sum();
        int passedRules = executions.stream().mapToInt(e -> e.getPassedRules() != null ? e.getPassedRules() : 0).sum();
        int failedRules = executions.stream().mapToInt(e -> e.getFailedRules() != null ? e.getFailedRules() : 0).sum();
        int skippedRules = executions.stream().mapToInt(e -> e.getSkippedRules() != null ? e.getSkippedRules() : 0).sum();
        int blockedCount = (int) executions.stream().filter(e -> Boolean.TRUE.equals(e.getBlocked())).count();
        long totalElapsed = executions.stream().mapToLong(e -> e.getElapsedMs() != null ? e.getElapsedMs() : 0).sum();
        long avgElapsed = executions.size() > 0 ? totalElapsed / executions.size() : 0;

        double avgScore = executions.stream()
                .filter(e -> e.getQualityScore() != null)
                .mapToInt(DqcExecution::getQualityScore)
                .average().orElse(0);

        double passRate = totalRules > 0 ? (double) passedRules / totalRules * 100 : 0;

        String scoreGrade = getScoreGrade((int) avgScore);
        String scoreColor = getScoreColor((int) avgScore);

        // 5. 计算真实六维度评分（从所有明细聚合）
        Map<String, BigDecimal> dimensionScores = calculateRealDimensionScores(allDetails);

        // 6. 计算评分趋势
        List<QualityReportVO.ScoreTrendPoint> trendPoints = buildScoreTrend(executions, detailsByExecution);
        QualityReportVO.TrendAnalysis trendAnalysis = buildTrendAnalysis(trendPoints);

        // 7. 失败规则TopN
        List<QualityReportVO.FailedRuleInfo> topFailed = buildTopFailedRules(executions, detailsByExecution);

        // 8. 需要关注的表
        List<QualityReportVO.AttentionTable> attentionTables = buildAttentionTables(executions, detailsByExecution);

        // 9. 评分分布
        QualityReportVO.ScoreDistribution distribution = buildScoreDistribution(executions);

        // 10. 各数据层评分
        Map<String, BigDecimal> layerScores = buildLayerScores(executions);

        // 11. 最近执行记录
        List<QualityReportVO.ExecutionSummary> recentExecutions = executions.stream()
                .limit(10)
                .map(this::convertToExecutionSummary)
                .collect(Collectors.toList());

        // 12. 生成评估和建议
        String assessment = generateAssessment((int) avgScore, failedRules, attentionTables);
        List<String> suggestions = generateSuggestions(dimensionScores, topFailed, attentionTables);
        String issueSummary = generateIssueSummary(failedRules, attentionTables);

        // 13. 时间范围
        String dateRange = buildDateRange(startDate, endDate);

        return QualityReportVO.builder()
                .reportTitle(plan != null ? plan.getPlanName() + " 质量报告" : "全量质量报告")
                .generateTime(now)
                .dateRange(dateRange)
                .planId(plan != null ? plan.getId() : null)
                .planName(plan != null ? plan.getPlanName() : "全量数据")
                .overallScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP))
                .scoreGrade(scoreGrade)
                .scoreColor(scoreColor)
                .rulePassRate(BigDecimal.valueOf(passRate).setScale(2, RoundingMode.HALF_UP))
                .totalRules(totalRules)
                .passedRules(passedRules)
                .failedRules(failedRules)
                .skippedRules(skippedRules)
                .blockedCount(blockedCount)
                .executionCount(executions.size())
                .avgElapsedMs(avgElapsed)
                .dimensionScores(QualityReportVO.DimensionScoresReport.builder()
                        .completeness(dimensionScores.get("COMPLETENESS"))
                        .uniqueness(dimensionScores.get("UNIQUENESS"))
                        .accuracy(dimensionScores.get("ACCURACY"))
                        .consistency(dimensionScores.get("CONSISTENCY"))
                        .timeliness(dimensionScores.get("TIMELINESS"))
                        .validity(dimensionScores.get("VALIDITY"))
                        .build())
                .scoreTrend(trendPoints)
                .trendAnalysis(trendAnalysis)
                .topFailedRules(topFailed)
                .attentionTables(attentionTables)
                .issueSummary(issueSummary)
                .recentExecutions(recentExecutions)
                .layerScores(layerScores)
                .distribution(distribution)
                .overallAssessment(assessment)
                .improvementSuggestions(suggestions)
                .build();
    }

    private QualityReportVO buildSingleExecutionReport(DqcExecution execution, List<DqcExecutionDetail> details, DqcPlan plan) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        int totalRules = details.size();
        int passedRules = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int failedRules = (int) details.stream().filter(d -> "FAILED".equals(d.getStatus())).count();
        int skippedRules = (int) details.stream().filter(d -> "SKIPPED".equals(d.getStatus())).count();
        int blockedCount = Boolean.TRUE.equals(execution.getBlocked()) ? 1 : 0;
        double avgScore = execution.getQualityScore() != null ? execution.getQualityScore() : 0;
        double passRate = totalRules > 0 ? (double) passedRules / totalRules * 100 : 0;

        Map<String, BigDecimal> dimensionScores = calculateRealDimensionScores(details);

        // 评分趋势只有1个点
        List<QualityReportVO.ScoreTrendPoint> trendPoints = Collections.singletonList(
                QualityReportVO.ScoreTrendPoint.builder()
                        .date(execution.getCreateTime().toLocalDate().toString())
                        .score(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP))
                        .passRate(BigDecimal.valueOf(passRate).setScale(2, RoundingMode.HALF_UP))
                        .passedCount(passedRules)
                        .failedCount(failedRules)
                        .totalCount(totalRules)
                        .overallScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP))
                        .build()
        );

        // 失败规则分析
        Map<String, Integer> ruleErrorCounts = new HashMap<>();
        Map<String, String> ruleLastFail = new HashMap<>();
        Map<String, Set<String>> ruleTables = new HashMap<>();
        Map<String, String> ruleDimensions = new HashMap<>();
        for (DqcExecutionDetail d : details) {
            if ("FAILED".equals(d.getStatus())) {
                ruleErrorCounts.merge(d.getRuleName(), 1, Integer::sum);
                if (d.getEndTime() != null) {
                    ruleLastFail.put(d.getRuleName(), d.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                if (d.getTargetTable() != null) {
                    ruleTables.computeIfAbsent(d.getRuleName(), k -> new HashSet<>()).add(d.getTargetTable());
                }
                if (d.getDimensions() != null) {
                    ruleDimensions.put(d.getRuleName(), d.getDimensions());
                }
            }
        }

        List<QualityReportVO.FailedRuleInfo> topFailed = ruleErrorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_FAILED_RULES)
                .map(e -> QualityReportVO.FailedRuleInfo.builder()
                        .ruleName(e.getKey())
                        .ruleCode(details.stream()
                                .filter(d -> d.getRuleName().equals(e.getKey()))
                                .findFirst().map(DqcExecutionDetail::getRuleCode)
                                .orElse(null))
                        .errorCount(e.getValue())
                        .lastFailTime(ruleLastFail.get(e.getKey()))
                        .tableCount(ruleTables.getOrDefault(e.getKey(), Collections.emptySet()).size())
                        .dimension(ruleDimensions.get(e.getKey()))
                        .build())
                .collect(Collectors.toList());

        List<QualityReportVO.AttentionTable> attentionTables = Collections.emptyList();
        String issueSummary = generateIssueSummary(failedRules, attentionTables);

        return QualityReportVO.builder()
                .reportTitle((plan != null ? plan.getPlanName() + " - " : "") + "执行报告 " + execution.getExecutionNo())
                .generateTime(now)
                .planId(plan != null ? plan.getId() : null)
                .planName(plan != null ? plan.getPlanName() : execution.getPlanName())
                .overallScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP))
                .scoreGrade(getScoreGrade((int) avgScore))
                .scoreColor(getScoreColor((int) avgScore))
                .rulePassRate(BigDecimal.valueOf(passRate).setScale(2, RoundingMode.HALF_UP))
                .totalRules(totalRules)
                .passedRules(passedRules)
                .failedRules(failedRules)
                .skippedRules(skippedRules)
                .blockedCount(blockedCount)
                .executionCount(1)
                .avgElapsedMs(execution.getElapsedMs())
                .dimensionScores(QualityReportVO.DimensionScoresReport.builder()
                        .completeness(dimensionScores.get("COMPLETENESS"))
                        .uniqueness(dimensionScores.get("UNIQUENESS"))
                        .accuracy(dimensionScores.get("ACCURACY"))
                        .consistency(dimensionScores.get("CONSISTENCY"))
                        .timeliness(dimensionScores.get("TIMELINESS"))
                        .validity(dimensionScores.get("VALIDITY"))
                        .build())
                .scoreTrend(trendPoints)
                .trendAnalysis(null)
                .topFailedRules(topFailed)
                .attentionTables(Collections.emptyList())
                .issueSummary(issueSummary)
                .recentExecutions(Collections.singletonList(convertToExecutionSummary(execution)))
                .distribution(QualityReportVO.ScoreDistribution.builder()
                        .excellentCount(avgScore >= EXCELLENT_THRESHOLD ? 1 : 0)
                        .goodCount(avgScore >= GOOD_THRESHOLD && avgScore < EXCELLENT_THRESHOLD ? 1 : 0)
                        .warningCount(avgScore >= WARNING_THRESHOLD && avgScore < GOOD_THRESHOLD ? 1 : 0)
                        .criticalCount(avgScore < WARNING_THRESHOLD ? 1 : 0)
                        .excellentPercent(BigDecimal.valueOf(avgScore >= EXCELLENT_THRESHOLD ? 100 : 0))
                        .goodPercent(BigDecimal.valueOf(avgScore >= GOOD_THRESHOLD && avgScore < EXCELLENT_THRESHOLD ? 100 : 0))
                        .build())
                .overallAssessment(generateAssessment((int) avgScore, failedRules, Collections.emptyList()))
                .improvementSuggestions(generateSuggestions(dimensionScores, topFailed, Collections.emptyList()))
                .build();
    }

    private QualityReportVO buildEmptyReport(DqcPlan plan, String startDate, String endDate, String now) {
        String dateRange = buildDateRange(startDate, endDate);
        return QualityReportVO.builder()
                .reportTitle(plan != null ? plan.getPlanName() + " 质量报告" : "全量质量报告")
                .generateTime(now)
                .dateRange(dateRange)
                .planId(plan != null ? plan.getId() : null)
                .planName(plan != null ? plan.getPlanName() : "全量数据")
                .overallScore(BigDecimal.ZERO)
                .scoreGrade("UNKNOWN")
                .scoreColor("#8C8C8C")
                .rulePassRate(BigDecimal.ZERO)
                .totalRules(0)
                .passedRules(0)
                .failedRules(0)
                .skippedRules(0)
                .blockedCount(0)
                .executionCount(0)
                .avgElapsedMs(0L)
                .dimensionScores(QualityReportVO.DimensionScoresReport.builder()
                        .completeness(BigDecimal.valueOf(100))
                        .uniqueness(BigDecimal.valueOf(100))
                        .accuracy(BigDecimal.valueOf(100))
                        .consistency(BigDecimal.valueOf(100))
                        .timeliness(BigDecimal.valueOf(100))
                        .validity(BigDecimal.valueOf(100))
                        .build())
                .scoreTrend(Collections.emptyList())
                .topFailedRules(Collections.emptyList())
                .attentionTables(Collections.emptyList())
                .issueSummary("暂无执行数据")
                .recentExecutions(Collections.emptyList())
                .layerScores(Collections.emptyMap())
                .distribution(QualityReportVO.ScoreDistribution.builder()
                        .excellentCount(0).goodCount(0).warningCount(0).criticalCount(0)
                        .excellentPercent(BigDecimal.ZERO).goodPercent(BigDecimal.ZERO)
                        .warningPercent(BigDecimal.ZERO).criticalPercent(BigDecimal.ZERO)
                        .build())
                .overallAssessment("暂无执行数据，请先执行质检方案")
                .improvementSuggestions(Collections.emptyList())
                .build();
    }

    // ==================== 核心计算方法 ====================

    /**
     * 从执行明细实时计算六维度真实评分
     */
    private Map<String, BigDecimal> calculateRealDimensionScores(List<DqcExecutionDetail> allDetails) {
        if (allDetails == null || allDetails.isEmpty()) {
            return getDefaultDimensionScores();
        }

        Map<String, List<DqcExecutionDetail>> byDimension = new HashMap<>();
        for (DqcExecutionDetail detail : allDetails) {
            String dim = mapRuleTypeToDimension(detail.getRuleType());
            byDimension.computeIfAbsent(dim, k -> new ArrayList<>()).add(detail);
        }

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String dim : List.of("COMPLETENESS", "UNIQUENESS", "ACCURACY", "CONSISTENCY", "TIMELINESS", "VALIDITY")) {
            List<DqcExecutionDetail> dimDetails = byDimension.get(dim);
            if (dimDetails != null && !dimDetails.isEmpty()) {
                result.put(dim, calculateDimensionAvgScore(dimDetails));
            } else {
                result.put(dim, BigDecimal.valueOf(100));
            }
        }

        return result;
    }

    private BigDecimal calculateDimensionAvgScore(List<DqcExecutionDetail> details) {
        if (details == null || details.isEmpty()) {
            return BigDecimal.valueOf(100);
        }

        // 使用 QualityScorer 计算该维度下每条规则的评分，然后取平均
        List<BigDecimal> scores = new ArrayList<>();
        for (DqcExecutionDetail d : details) {
            int ruleScore = calculateRuleScore(d);
            scores.add(BigDecimal.valueOf(ruleScore));
        }

        double avg = scores.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(100);

        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    private int calculateRuleScore(DqcExecutionDetail detail) {
        String status = detail.getStatus();
        if ("SUCCESS".equals(status) || "SKIPPED".equals(status)) {
            return 100;
        }

        Long totalCount = detail.getTotalCount();
        Long errorCount = detail.getErrorCount();
        if (totalCount == null || totalCount == 0) {
            return "FAILED".equals(status) ? 0 : 100;
        }

        double errorRate = (double) errorCount / totalCount;
        double score = 100 * (1 - errorRate);

        // 强规则失败扣分更多
        if ("STRONG".equals(detail.getRuleStrength())) {
            score = score / 1.5;
        }

        return (int) Math.max(0, Math.min(100, score));
    }

    private String mapRuleTypeToDimension(String ruleType) {
        if (ruleType == null) return "COMPLETENESS";
        String upper = ruleType.toUpperCase();
        if (upper.contains("NULL") || upper.contains("ROW_COUNT") || upper.contains("FLUCTUATION")) {
            return "COMPLETENESS";
        }
        if (upper.contains("UNIQUE") || upper.contains("DUPLICATE") || upper.contains("CARDINALITY")) {
            return "UNIQUENESS";
        }
        if (upper.contains("THRESHOLD") || upper.contains("ACCURACY")) {
            return "ACCURACY";
        }
        if (upper.contains("CONSISTENCY") || upper.contains("CROSS") || upper.contains("ENUM")) {
            return "CONSISTENCY";
        }
        if (upper.contains("TIMELINESS") || upper.contains("TIMESTAMP")) {
            return "TIMELINESS";
        }
        if (upper.contains("REGEX") || upper.contains("VALID") || upper.contains("FORMAT") || upper.contains("PHONE") || upper.contains("EMAIL")) {
            return "VALIDITY";
        }
        return "VALIDITY";
    }

    private Map<String, BigDecimal> getDefaultDimensionScores() {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        map.put("COMPLETENESS", BigDecimal.valueOf(100));
        map.put("UNIQUENESS", BigDecimal.valueOf(100));
        map.put("ACCURACY", BigDecimal.valueOf(100));
        map.put("CONSISTENCY", BigDecimal.valueOf(100));
        map.put("TIMELINESS", BigDecimal.valueOf(100));
        map.put("VALIDITY", BigDecimal.valueOf(100));
        return map;
    }

    private List<QualityReportVO.ScoreTrendPoint> buildScoreTrend(
            List<DqcExecution> executions,
            Map<Long, List<DqcExecutionDetail>> detailsByExecution) {

        // 按日期分组
        Map<String, List<DqcExecution>> byDate = executions.stream()
                .collect(Collectors.groupingBy(e -> e.getCreateTime().toLocalDate().toString()));

        List<QualityReportVO.ScoreTrendPoint> trend = new ArrayList<>();
        for (Map.Entry<String, List<DqcExecution>> entry : byDate.entrySet()) {
            List<DqcExecution> dayExecs = entry.getValue();

            int totalRules = dayExecs.stream().mapToInt(e -> e.getTotalRules() != null ? e.getTotalRules() : 0).sum();
            int passedRules = dayExecs.stream().mapToInt(e -> e.getPassedRules() != null ? e.getPassedRules() : 0).sum();
            int failedRules = dayExecs.stream().mapToInt(e -> e.getFailedRules() != null ? e.getFailedRules() : 0).sum();

            double avgScore = dayExecs.stream()
                    .filter(e -> e.getQualityScore() != null)
                    .mapToInt(DqcExecution::getQualityScore)
                    .average().orElse(0);

            double passRate = totalRules > 0 ? (double) passedRules / totalRules * 100 : 0;

            trend.add(QualityReportVO.ScoreTrendPoint.builder()
                    .date(entry.getKey())
                    .score(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP))
                    .passRate(BigDecimal.valueOf(passRate).setScale(2, RoundingMode.HALF_UP))
                    .passedCount(passedRules)
                    .failedCount(failedRules)
                    .totalCount(totalRules)
                    .overallScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        // 按日期排序
        trend.sort(Comparator.comparing(QualityReportVO.ScoreTrendPoint::getDate));
        return trend;
    }

    private QualityReportVO.TrendAnalysis buildTrendAnalysis(List<QualityReportVO.ScoreTrendPoint> trend) {
        if (trend == null || trend.isEmpty()) {
            return null;
        }

        List<BigDecimal> scores = trend.stream()
                .map(QualityReportVO.ScoreTrendPoint::getScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (scores.isEmpty()) {
            return null;
        }

        double avg = scores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double max = scores.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0);
        double min = scores.stream().mapToDouble(BigDecimal::doubleValue).min().orElse(0);

        double variance = scores.stream()
                .mapToDouble(s -> Math.pow(s.doubleValue() - avg, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        String direction = "FLAT";
        double change = 0;
        double changePercent = 0;
        if (trend.size() >= 2) {
            int mid = trend.size() / 2;
            double firstHalf = trend.subList(0, mid).stream()
                    .map(QualityReportVO.ScoreTrendPoint::getScore)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average().orElse(0);
            double secondHalf = trend.subList(mid, trend.size()).stream()
                    .map(QualityReportVO.ScoreTrendPoint::getScore)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average().orElse(0);
            change = secondHalf - firstHalf;
            if (firstHalf != 0) {
                changePercent = change / firstHalf * 100;
            }
            direction = change > 2 ? "UP" : (change < -2 ? "DOWN" : "FLAT");
        }

        return QualityReportVO.TrendAnalysis.builder()
                .avgScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP))
                .maxScore(BigDecimal.valueOf(max).setScale(2, RoundingMode.HALF_UP))
                .minScore(BigDecimal.valueOf(min).setScale(2, RoundingMode.HALF_UP))
                .volatility(BigDecimal.valueOf(stdDev).setScale(2, RoundingMode.HALF_UP))
                .trendDirection(direction)
                .trendChange(BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP))
                .trendChangePercent(BigDecimal.valueOf(changePercent).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private List<QualityReportVO.FailedRuleInfo> buildTopFailedRules(
            List<DqcExecution> executions,
            Map<Long, List<DqcExecutionDetail>> detailsByExecution) {

        Map<String, Integer> ruleErrorCounts = new HashMap<>();
        Map<String, String> ruleLastFail = new HashMap<>();
        Map<String, Set<String>> ruleTables = new HashMap<>();
        Map<String, String> ruleDimensions = new HashMap<>();
        Map<String, String> ruleCodes = new HashMap<>();

        for (DqcExecution exec : executions) {
            List<DqcExecutionDetail> details = detailsByExecution.get(exec.getId());
            if (details == null) continue;

            for (DqcExecutionDetail d : details) {
                if ("FAILED".equals(d.getStatus())) {
                    ruleErrorCounts.merge(d.getRuleName(), 1, Integer::sum);
                    if (d.getEndTime() != null) {
                        String prev = ruleLastFail.get(d.getRuleName());
                        String curr = d.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        if (prev == null || curr.compareTo(prev) > 0) {
                            ruleLastFail.put(d.getRuleName(), curr);
                        }
                    }
                    if (d.getTargetTable() != null) {
                        ruleTables.computeIfAbsent(d.getRuleName(), k -> new HashSet<>()).add(d.getTargetTable());
                    }
                    if (d.getDimensions() != null) {
                        ruleDimensions.put(d.getRuleName(), d.getDimensions());
                    }
                    if (d.getRuleCode() != null) {
                        ruleCodes.put(d.getRuleName(), d.getRuleCode());
                    }
                }
            }
        }

        return ruleErrorCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(TOP_FAILED_RULES)
                .map(e -> QualityReportVO.FailedRuleInfo.builder()
                        .ruleName(e.getKey())
                        .ruleCode(ruleCodes.get(e.getKey()))
                        .errorCount(e.getValue())
                        .lastFailTime(ruleLastFail.get(e.getKey()))
                        .tableCount(ruleTables.getOrDefault(e.getKey(), Collections.emptySet()).size())
                        .dimension(ruleDimensions.get(e.getKey()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<QualityReportVO.AttentionTable> buildAttentionTables(
            List<DqcExecution> executions,
            Map<Long, List<DqcExecutionDetail>> detailsByExecution) {

        Map<String, Integer> tableFailCounts = new HashMap<>();
        Map<String, Long> tableDsIds = new HashMap<>();
        Map<String, String> tableLayerCodes = new HashMap<>();

        for (DqcExecution exec : executions) {
            List<DqcExecutionDetail> details = detailsByExecution.get(exec.getId());
            if (details == null) continue;

            for (DqcExecutionDetail d : details) {
                if ("FAILED".equals(d.getStatus()) && d.getTargetTable() != null) {
                    String table = d.getTargetTable();
                    tableFailCounts.merge(table, 1, Integer::sum);
                    if (d.getTargetDsId() != null) {
                        tableDsIds.put(table, d.getTargetDsId());
                    }
                }
            }

            // 获取数据层信息
            if (exec.getLayerCode() != null) {
                DqcPlan plan = planMapper.selectById(exec.getPlanId());
                if (plan != null && plan.getLayerCode() != null) {
                    // 通过执行记录获取层信息
                }
            }
        }

        return tableFailCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(TOP_ATTENTION_TABLES)
                .map(e -> {
                    String table = e.getKey();
                    Long dsId = tableDsIds.get(table);
                    // 从数据源获取层信息
                    String layerCode = null;
                    if (dsId != null) {
                        DqDatasource ds = datasourceMapper.selectById(dsId);
                        if (ds != null) {
                            layerCode = ds.getDataLayer();
                        }
                    }
                    return QualityReportVO.AttentionTable.builder()
                            .tableName(table)
                            .dsId(dsId)
                            .layerCode(layerCode)
                            .score(BigDecimal.ZERO)
                            .issueDescription("失败 " + e.getValue() + " 次")
                            .dimension(null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private QualityReportVO.ScoreDistribution buildScoreDistribution(List<DqcExecution> executions) {
        int excellent = 0, good = 0, warning = 0, critical = 0;
        for (DqcExecution e : executions) {
            if (e.getQualityScore() == null) continue;
            int score = e.getQualityScore();
            if (score >= EXCELLENT_THRESHOLD) excellent++;
            else if (score >= GOOD_THRESHOLD) good++;
            else if (score >= WARNING_THRESHOLD) warning++;
            else critical++;
        }
        int total = executions.size();
        return QualityReportVO.ScoreDistribution.builder()
                .excellentCount(excellent)
                .goodCount(good)
                .warningCount(warning)
                .criticalCount(critical)
                .excellentPercent(total > 0 ? BigDecimal.valueOf(100.0 * excellent / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .goodPercent(total > 0 ? BigDecimal.valueOf(100.0 * good / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .warningPercent(total > 0 ? BigDecimal.valueOf(100.0 * warning / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .criticalPercent(total > 0 ? BigDecimal.valueOf(100.0 * critical / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .build();
    }

    private Map<String, BigDecimal> buildLayerScores(List<DqcExecution> executions) {
        Map<String, List<DqcExecution>> byLayer = executions.stream()
                .filter(e -> e.getLayerCode() != null)
                .collect(Collectors.groupingBy(DqcExecution::getLayerCode));

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<DqcExecution>> entry : byLayer.entrySet()) {
            double avg = entry.getValue().stream()
                    .filter(e -> e.getQualityScore() != null)
                    .mapToInt(DqcExecution::getQualityScore)
                    .average().orElse(0);
            result.put(entry.getKey(), BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        }
        return result;
    }

    // ==================== Excel导出辅助方法 ====================

    private List<QualityReportExecutionSheetVO> buildExecutionSheetData(List<DqcExecution> executions) {
        List<QualityReportExecutionSheetVO> result = new ArrayList<>();
        for (DqcExecution exec : executions) {
            LambdaQueryWrapper<DqcExecutionDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DqcExecutionDetail::getExecutionId, exec.getId());
            List<DqcExecutionDetail> details = detailMapper.selectList(wrapper);

            DqcPlan plan = planMapper.selectById(exec.getPlanId());
            result.add(buildExecutionSheetRow(exec, details, plan));
        }
        return result;
    }

    private List<QualityReportExecutionSheetVO> buildSingleExecutionSheetData(
            DqcExecution execution, List<DqcExecutionDetail> details, DqcPlan plan) {
        return Collections.singletonList(buildExecutionSheetRow(execution, details, plan));
    }

    private QualityReportExecutionSheetVO buildExecutionSheetRow(DqcExecution exec,
                                                                   List<DqcExecutionDetail> details,
                                                                   DqcPlan plan) {
        Map<String, BigDecimal> dimScores = calculateRealDimensionScores(details);

        int total = details.size();
        int passed = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        double passRate = total > 0 ? (double) passed / total * 100 : 0;

        return QualityReportExecutionSheetVO.builder()
                .executionNo(exec.getExecutionNo())
                .planName(exec.getPlanName())
                .layerCode(exec.getLayerCode())
                .triggerType(exec.getTriggerType())
                .status(exec.getStatus())
                .startTime(exec.getStartTime())
                .endTime(exec.getEndTime())
                .elapsedMs(exec.getElapsedMs())
                .totalRules(exec.getTotalRules())
                .passedRules(exec.getPassedRules())
                .failedRules(exec.getFailedRules())
                .skippedRules(exec.getSkippedRules())
                .blocked(Boolean.TRUE.equals(exec.getBlocked()) ? "是" : "否")
                .qualityScore(exec.getQualityScore())
                .overallScore(exec.getQualityScore() != null
                        ? BigDecimal.valueOf(exec.getQualityScore()).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .completenessScore(dimScores.get("COMPLETENESS"))
                .uniquenessScore(dimScores.get("UNIQUENESS"))
                .accuracyScore(dimScores.get("ACCURACY"))
                .consistencyScore(dimScores.get("CONSISTENCY"))
                .timelinessScore(dimScores.get("TIMELINESS"))
                .validityScore(dimScores.get("VALIDITY"))
                .rulePassRate(BigDecimal.valueOf(passRate).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private List<QualityReportRuleDetailSheetVO> buildRuleDetailSheetDataByExecutions(List<DqcExecution> executions) {
        if (executions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> execIds = executions.stream().map(DqcExecution::getId).collect(Collectors.toList());
        LambdaQueryWrapper<DqcExecutionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DqcExecutionDetail::getExecutionId, execIds);
        wrapper.orderByAsc(DqcExecutionDetail::getExecutionId, DqcExecutionDetail::getId);

        return detailMapper.selectList(wrapper).stream()
                .map(this::convertToRuleDetailSheet)
                .collect(Collectors.toList());
    }

    private QualityReportRuleDetailSheetVO convertToRuleDetailSheet(DqcExecutionDetail detail) {
        return QualityReportRuleDetailSheetVO.builder()
                .executionNo(detail.getExecutionNo())
                .ruleName(detail.getRuleName())
                .ruleCode(detail.getRuleCode())
                .ruleType(detail.getRuleType())
                .ruleStrength(detail.getRuleStrength())
                .dimensions(detail.getDimensions())
                .targetDsId(detail.getTargetDsId())
                .targetTable(detail.getTargetTable())
                .targetColumn(detail.getTargetColumn())
                .startTime(detail.getStartTime())
                .endTime(detail.getEndTime())
                .elapsedMs(detail.getElapsedMs())
                .status(detail.getStatus())
                .totalCount(detail.getTotalCount())
                .errorCount(detail.getErrorCount())
                .passCount(detail.getPassCount())
                .actualValue(detail.getActualValue())
                .thresholdMin(detail.getThresholdMin())
                .thresholdMax(detail.getThresholdMax())
                .qualityScore(detail.getQualityScore())
                .errorDetail(detail.getErrorDetail())
                .build();
    }

    private List<DimensionScoreExportVO> buildDimensionScoreSheetData(List<DqcExecution> executions) {
        if (executions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> execIds = executions.stream().map(DqcExecution::getId).collect(Collectors.toList());
        LambdaQueryWrapper<DqcExecutionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DqcExecutionDetail::getExecutionId, execIds);
        List<DqcExecutionDetail> allDetails = detailMapper.selectList(wrapper);

        Map<String, List<DqcExecutionDetail>> byDim = new HashMap<>();
        for (DqcExecutionDetail d : allDetails) {
            String dim = mapRuleTypeToDimension(d.getRuleType());
            byDim.computeIfAbsent(dim, k -> new ArrayList<>()).add(d);
        }

        return byDim.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    BigDecimal avgScore = calculateDimensionAvgScore(e.getValue());
                    int passed = (int) e.getValue().stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
                    int total = e.getValue().size();
                    return DimensionScoreExportVO.builder()
                            .dimensionCode(e.getKey())
                            .dimensionName(getDimensionChineseName(e.getKey()))
                            .ruleCount(total)
                            .passedCount(passed)
                            .failedCount(total - passed)
                            .passRate(BigDecimal.valueOf(total > 0 ? 100.0 * passed / total : 0).setScale(2, RoundingMode.HALF_UP))
                            .avgScore(avgScore)
                            .scoreGrade(getScoreGrade(avgScore.intValue()))
                            .scoreColor(getScoreColor(avgScore.intValue()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<DimensionScoreExportVO> buildDimensionScoreSheetDataSingle(QualityScorer.ScoreBreakdown breakdown) {
        List<DimensionScoreExportVO> result = new ArrayList<>();
        Map<String, BigDecimal> scores = breakdown.dimensionScores();

        for (String dim : List.of("COMPLETENESS", "UNIQUENESS", "ACCURACY", "CONSISTENCY", "TIMELINESS", "VALIDITY")) {
            BigDecimal score = scores.getOrDefault(dim, BigDecimal.valueOf(100));
            result.add(DimensionScoreExportVO.builder()
                    .dimensionCode(dim)
                    .dimensionName(getDimensionChineseName(dim))
                    .avgScore(score)
                    .scoreGrade(getScoreGrade(score.intValue()))
                    .scoreColor(getScoreColor(score.intValue()))
                    .build());
        }
        return result;
    }

    private List<ProblemAnalysisExportVO> buildProblemAnalysisSheetData(List<DqcExecution> executions) {
        if (executions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> execIds = executions.stream().map(DqcExecution::getId).collect(Collectors.toList());
        LambdaQueryWrapper<DqcExecutionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DqcExecutionDetail::getExecutionId, execIds);
        wrapper.eq(DqcExecutionDetail::getStatus, "FAILED");
        List<DqcExecutionDetail> failedDetails = detailMapper.selectList(wrapper);

        Map<String, Integer> ruleFailCounts = new HashMap<>();
        Map<String, Set<String>> ruleTables = new HashMap<>();
        Map<String, String> ruleDimensions = new HashMap<>();
        Map<String, String> ruleLastFail = new HashMap<>();

        for (DqcExecutionDetail d : failedDetails) {
            ruleFailCounts.merge(d.getRuleName(), 1, Integer::sum);
            if (d.getTargetTable() != null) {
                ruleTables.computeIfAbsent(d.getRuleName(), k -> new HashSet<>()).add(d.getTargetTable());
            }
            if (d.getDimensions() != null) {
                ruleDimensions.put(d.getRuleName(), d.getDimensions());
            }
            if (d.getEndTime() != null) {
                ruleLastFail.put(d.getRuleName(),
                        d.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        }

        List<ProblemAnalysisExportVO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : ruleFailCounts.entrySet()) {
            result.add(ProblemAnalysisExportVO.builder()
                    .ruleName(e.getKey())
                    .dimension(ruleDimensions.get(e.getKey()))
                    .failCount(e.getValue())
                    .involvedTables(String.join(", ", ruleTables.getOrDefault(e.getKey(), Collections.emptySet())))
                    .lastFailTime(ruleLastFail.get(e.getKey()))
                    .priority(e.getValue() >= 5 ? "高" : (e.getValue() >= 3 ? "中" : "低"))
                    .build());
        }

        result.sort((a, b) -> Integer.compare(b.getFailCount(), a.getFailCount()));
        return result;
    }

    // ==================== 通用辅助方法 ====================

    private LambdaQueryWrapper<DqcExecution> buildExecutionWrapper(Long planId, String layerCode,
                                                                     String startDate, String endDate) {
        LambdaQueryWrapper<DqcExecution> wrapper = new LambdaQueryWrapper<>();
        if (planId != null) {
            wrapper.eq(DqcExecution::getPlanId, planId);
        }
        if (StringUtils.hasText(layerCode)) {
            wrapper.eq(DqcExecution::getLayerCode, layerCode);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(DqcExecution::getCreateTime,
                    LocalDateTime.parse(startDate + " 00:00:00",
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(DqcExecution::getCreateTime,
                    LocalDateTime.parse(endDate + " 23:59:59",
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return wrapper;
    }

    private QualityReportVO.ExecutionSummary convertToExecutionSummary(DqcExecution e) {
        return QualityReportVO.ExecutionSummary.builder()
                .executionId(e.getId())
                .executionNo(e.getExecutionNo())
                .status(e.getStatus())
                .startTime(e.getStartTime() != null ? e.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .elapsedMs(e.getElapsedMs())
                .totalRules(e.getTotalRules())
                .passedRules(e.getPassedRules())
                .failedRules(e.getFailedRules())
                .qualityScore(e.getQualityScore())
                .blocked(e.getBlocked())
                .build();
    }

    private String getScoreGrade(int score) {
        if (score >= EXCELLENT_THRESHOLD) return "EXCELLENT";
        if (score >= GOOD_THRESHOLD) return "GOOD";
        if (score >= WARNING_THRESHOLD) return "WARNING";
        return "CRITICAL";
    }

    private String getScoreColor(int score) {
        if (score >= EXCELLENT_THRESHOLD) return "#52C41A";
        if (score >= GOOD_THRESHOLD) return "#1677FF";
        if (score >= WARNING_THRESHOLD) return "#FAAD14";
        return "#FF4D4F";
    }

    private String getDimensionChineseName(String code) {
        return switch (code) {
            case "COMPLETENESS" -> "完整性";
            case "UNIQUENESS" -> "唯一性";
            case "ACCURACY" -> "准确性";
            case "CONSISTENCY" -> "一致性";
            case "TIMELINESS" -> "及时性";
            case "VALIDITY" -> "有效性";
            default -> code;
        };
    }

    private String buildDateRange(String startDate, String endDate) {
        if (startDate == null && endDate == null) {
            return "全部时间";
        }
        if (startDate != null && endDate != null) {
            return startDate + " 至 " + endDate;
        }
        if (startDate != null) {
            return startDate + " 至今";
        }
        return "至 " + endDate;
    }

    private String generateAssessment(int avgScore, int failedRules,
                                       List<QualityReportVO.AttentionTable> attentionTables) {
        if (avgScore >= EXCELLENT_THRESHOLD) {
            return "数据质量整体优秀，各项指标表现良好，继续保持当前的数据质量管理水平。";
        } else if (avgScore >= GOOD_THRESHOLD) {
            return "数据质量良好，存在少量问题需要关注。建议对失败规则进行分析和优化。";
        } else if (avgScore >= WARNING_THRESHOLD) {
            return "数据质量一般，存在较多质量问题。建议优先处理高频失败规则和重点关注表。";
        } else {
            return "数据质量较差，需要立即处理。建议全面梳理问题规则，逐一优化改进。";
        }
    }

    private List<String> generateSuggestions(Map<String, BigDecimal> dimScores,
                                               List<QualityReportVO.FailedRuleInfo> failedRules,
                                               List<QualityReportVO.AttentionTable> attentionTables) {
        List<String> suggestions = new ArrayList<>();

        // 维度分析
        for (Map.Entry<String, BigDecimal> e : dimScores.entrySet()) {
            if (e.getValue() != null && e.getValue().intValue() < GOOD_THRESHOLD) {
                String dimName = getDimensionChineseName(e.getKey());
                suggestions.add("【" + dimName + "】评分偏低(" + e.getValue() + "分)，建议加强" + dimName + "维度的质量管控");
            }
        }

        // 失败规则建议
        if (!failedRules.isEmpty()) {
            suggestions.add("失败规则 Top" + Math.min(3, failedRules.size()) + "：" +
                    failedRules.stream().limit(3)
                            .map(QualityReportVO.FailedRuleInfo::getRuleName)
                            .collect(Collectors.joining("、")) + "，建议优先分析失败原因");
        }

        // 关注表建议
        if (!attentionTables.isEmpty()) {
            suggestions.add("有 " + attentionTables.size() + " 张表存在质量问题，建议重点关注并分析根因");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("各项指标均处于正常范围，建议持续监控，保持当前管理状态");
        }

        return suggestions;
    }

    private String generateIssueSummary(int failedRules,
                                          List<QualityReportVO.AttentionTable> attentionTables) {
        if (failedRules == 0 && attentionTables.isEmpty()) {
            return "本次报告期间无质量问题，数据质量表现良好";
        }
        StringBuilder sb = new StringBuilder();
        if (failedRules > 0) {
            sb.append("共发现 ").append(failedRules).append(" 条失败规则");
        }
        if (!attentionTables.isEmpty()) {
            if (sb.length() > 0) sb.append("，涉及 ");
            else sb.append("涉及 ");
            sb.append(attentionTables.size()).append(" 张表存在质量问题");
        }
        return sb.toString();
    }

    // ==================== 内部导出VO（用于Excel Sheet） ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class DimensionScoreExportVO {
        @com.alibaba.excel.annotation.ExcelProperty(value = "维度编码", index = 0)
        private String dimensionCode;
        @com.alibaba.excel.annotation.ExcelProperty(value = "维度名称", index = 1)
        private String dimensionName;
        @com.alibaba.excel.annotation.ExcelProperty(value = "规则数", index = 2)
        private Integer ruleCount;
        @com.alibaba.excel.annotation.ExcelProperty(value = "通过数", index = 3)
        private Integer passedCount;
        @com.alibaba.excel.annotation.ExcelProperty(value = "失败数", index = 4)
        private Integer failedCount;
        @com.alibaba.excel.annotation.ExcelProperty(value = "通过率(%)", index = 5)
        private BigDecimal passRate;
        @com.alibaba.excel.annotation.ExcelProperty(value = "平均评分", index = 6)
        private BigDecimal avgScore;
        @com.alibaba.excel.annotation.ExcelProperty(value = "评分等级", index = 7)
        private String scoreGrade;
        @com.alibaba.excel.annotation.ExcelProperty(value = "颜色", index = 8)
        private String scoreColor;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ProblemAnalysisExportVO {
        @com.alibaba.excel.annotation.ExcelProperty(value = "规则名称", index = 0)
        private String ruleName;
        @com.alibaba.excel.annotation.ExcelProperty(value = "所属维度", index = 1)
        private String dimension;
        @com.alibaba.excel.annotation.ExcelProperty(value = "失败次数", index = 2)
        private Integer failCount;
        @com.alibaba.excel.annotation.ExcelProperty(value = "涉及表", index = 3)
        private String involvedTables;
        @com.alibaba.excel.annotation.ExcelProperty(value = "最近失败时间", index = 4)
        private String lastFailTime;
        @com.alibaba.excel.annotation.ExcelProperty(value = "优先级", index = 5)
        private String priority;
    }
}
