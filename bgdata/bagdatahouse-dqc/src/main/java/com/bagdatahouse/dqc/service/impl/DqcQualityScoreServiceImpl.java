package com.bagdatahouse.dqc.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.core.entity.DqcExecution;
import com.bagdatahouse.core.entity.DqcExecutionDetail;
import com.bagdatahouse.core.entity.DqcQualityScore;
import com.bagdatahouse.core.mapper.DqcExecutionDetailMapper;
import com.bagdatahouse.core.mapper.DqcExecutionMapper;
import com.bagdatahouse.core.mapper.DqcQualityScoreMapper;
import com.bagdatahouse.core.vo.QualityScoreTrendVO;
import com.bagdatahouse.core.vo.QualityScoreOverviewVO;
import com.bagdatahouse.core.vo.DimensionScoreVO;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.dqc.service.DqcQualityScoreService;
import com.bagdatahouse.dqc.engine.scorer.QualityScorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DQC质量评分服务实现
 */
@Service
public class DqcQualityScoreServiceImpl extends ServiceImpl<DqcQualityScoreMapper, DqcQualityScore>
        implements DqcQualityScoreService {

    private static final Logger log = LoggerFactory.getLogger(DqcQualityScoreServiceImpl.class);

    @Autowired
    private DqcExecutionMapper executionMapper;

    @Autowired
    private DqcExecutionDetailMapper detailMapper;

    @Autowired
    private QualityScorer qualityScorer;

    // 评分等级阈值
    private static final int EXCELLENT_THRESHOLD = 90;
    private static final int GOOD_THRESHOLD = 70;
    private static final int WARNING_THRESHOLD = 60;

    @Override
    public Result<List<DqcQualityScore>> getDailyScores(Long dsId, String tableName, String startDate, String endDate) {
        LambdaQueryWrapper<DqcQualityScore> wrapper = new LambdaQueryWrapper<>();

        if (dsId != null) {
            wrapper.eq(DqcQualityScore::getTargetDsId, dsId);
        }
        if (tableName != null && !tableName.isEmpty()) {
            wrapper.eq(DqcQualityScore::getTargetTable, tableName);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(DqcQualityScore::getCheckDate,
                    LocalDateTime.parse(startDate + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(DqcQualityScore::getCheckDate,
                    LocalDateTime.parse(endDate + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        wrapper.orderByDesc(DqcQualityScore::getCheckDate);
        List<DqcQualityScore> list = this.list(wrapper);
        return Result.success(list);
    }

    @Override
    public Result<List<DqcQualityScore>> getTableScores(String layerCode, String startDate, String endDate) {
        LambdaQueryWrapper<DqcQualityScore> wrapper = new LambdaQueryWrapper<>();

        if (layerCode != null && !layerCode.isEmpty()) {
            wrapper.eq(DqcQualityScore::getLayerCode, layerCode);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(DqcQualityScore::getCheckDate,
                    LocalDateTime.parse(startDate + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(DqcQualityScore::getCheckDate,
                    LocalDateTime.parse(endDate + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        wrapper.orderByDesc(DqcQualityScore::getCheckDate);
        List<DqcQualityScore> list = this.list(wrapper);

        // 同一数据层 + 表保留最新一条（列表已按检查时间倒序，首次出现即为最新）
        Map<String, DqcQualityScore> latestByTable = new LinkedHashMap<>();
        for (DqcQualityScore score : list) {
            String layer = score.getLayerCode() != null ? score.getLayerCode() : "";
            String table = score.getTargetTable();
            String key = table != null && !table.isEmpty()
                    ? layer + "|" + table
                    : layer + "|_id_" + score.getId();
            if (!latestByTable.containsKey(key)) {
                latestByTable.put(key, score);
            }
        }

        return Result.success(new ArrayList<>(latestByTable.values()));
    }

    @Override
    public Result<Map<String, BigDecimal>> getLayerScoreSummary(String startDate, String endDate) {
        LambdaQueryWrapper<DqcQualityScore> wrapper = new LambdaQueryWrapper<>();

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(DqcQualityScore::getCheckDate,
                    LocalDateTime.parse(startDate + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(DqcQualityScore::getCheckDate,
                    LocalDateTime.parse(endDate + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        List<DqcQualityScore> list = this.list(wrapper);

        Map<String, List<DqcQualityScore>> byLayer = list.stream()
                .filter(s -> s.getLayerCode() != null)
                .collect(Collectors.groupingBy(DqcQualityScore::getLayerCode));

        Map<String, BigDecimal> summary = new HashMap<>();
        for (Map.Entry<String, List<DqcQualityScore>> entry : byLayer.entrySet()) {
            double avgScore = entry.getValue().stream()
                    .filter(s -> s.getOverallScore() != null)
                    .mapToDouble(s -> s.getOverallScore().doubleValue())
                    .average()
                    .orElse(0.0);
            summary.put(entry.getKey(), BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));
        }

        return Result.success(summary);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateAndSaveScore(Long executionId) {
        DqcExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.warn("执行记录不存在: {}", executionId);
            return;
        }

        LambdaQueryWrapper<DqcExecutionDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(DqcExecutionDetail::getExecutionId, executionId);
        List<DqcExecutionDetail> details = detailMapper.selectList(detailWrapper);

        if (details.isEmpty()) {
            log.warn("执行明细为空: {}", executionId);
            return;
        }

        Map<String, List<DqcExecutionDetail>> byDimension = new HashMap<>();
        for (DqcExecutionDetail detail : details) {
            for (String dim : resolveDetailDimensions(detail)) {
                byDimension.computeIfAbsent(dim, k -> new ArrayList<>()).add(detail);
            }
        }

        Map<String, BigDecimal> dimensionScores = new HashMap<>();
        dimensionScores.put("COMPLETENESS", calculateDimensionScore(byDimension.get("COMPLETENESS")));
        dimensionScores.put("UNIQUENESS", calculateDimensionScore(byDimension.get("UNIQUENESS")));
        dimensionScores.put("ACCURACY", calculateDimensionScore(byDimension.get("ACCURACY")));
        dimensionScores.put("CONSISTENCY", calculateDimensionScore(byDimension.get("CONSISTENCY")));
        dimensionScores.put("TIMELINESS", calculateDimensionScore(byDimension.get("TIMELINESS")));
        dimensionScores.put("VALIDITY", calculateDimensionScore(byDimension.get("VALIDITY")));

        int totalRules = details.size();
        int passedRules = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int failedRules = (int) details.stream().filter(d -> "FAILED".equals(d.getStatus())).count();

        BigDecimal completeness = dimensionScores.get("COMPLETENESS");
        BigDecimal uniqueness = dimensionScores.get("UNIQUENESS");
        BigDecimal accuracy = dimensionScores.get("ACCURACY");
        BigDecimal consistency = dimensionScores.get("CONSISTENCY");
        BigDecimal timeliness = dimensionScores.get("TIMELINESS");
        BigDecimal validity = dimensionScores.get("VALIDITY");

        List<BigDecimal> evaluatedDims = new ArrayList<>();
        for (BigDecimal b : Arrays.asList(completeness, uniqueness, accuracy, consistency, timeliness, validity)) {
            if (b != null) {
                evaluatedDims.add(b);
            }
        }

        BigDecimal overallScoreBd;
        if (evaluatedDims.isEmpty()) {
            if (execution.getQualityScore() != null) {
                overallScoreBd = BigDecimal.valueOf(execution.getQualityScore()).setScale(2, RoundingMode.HALF_UP);
            } else {
                overallScoreBd = totalRules > 0
                        ? BigDecimal.valueOf((double) passedRules / totalRules * 100).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
            }
        } else {
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal b : evaluatedDims) {
                sum = sum.add(b);
            }
            overallScoreBd = sum.divide(BigDecimal.valueOf(evaluatedDims.size()), 2, RoundingMode.HALF_UP);
        }

        BigDecimal rulePassRate = totalRules > 0
                ? BigDecimal.valueOf((double) passedRules / totalRules * 100).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Set<String> tables = details.stream()
                .map(DqcExecutionDetail::getTargetTable)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String table : tables) {
            DqcQualityScore score = DqcQualityScore.builder()
                    .checkDate(LocalDateTime.now())
                    .targetDsId(details.get(0).getTargetDsId())
                    .targetTable(table)
                    .layerCode(execution.getLayerCode())
                    .deptId(execution.getDeptId())
                    .completenessScore(dimensionScores.get("COMPLETENESS"))
                    .uniquenessScore(dimensionScores.get("UNIQUENESS"))
                    .accuracyScore(dimensionScores.get("ACCURACY"))
                    .consistencyScore(dimensionScores.get("CONSISTENCY"))
                    .timelinessScore(dimensionScores.get("TIMELINESS"))
                    .validityScore(dimensionScores.get("VALIDITY"))
                    .overallScore(overallScoreBd)
                    .rulePassRate(rulePassRate)
                    .ruleTotalCount(totalRules)
                    .rulePassCount(passedRules)
                    .ruleFailCount(failedRules)
                    .executionId(executionId)
                    .createTime(LocalDateTime.now())
                    .build();

            LambdaQueryWrapper<DqcQualityScore> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(DqcQualityScore::getTargetTable, table);
            existWrapper.apply("DATE(check_date) = CURDATE()");
            DqcQualityScore existing = this.getOne(existWrapper);

            if (existing != null) {
                score.setId(existing.getId());
                this.updateById(score);
            } else {
                this.save(score);
            }
        }
    }

    /**
     * 规则上已配置维度 JSON 时按配置；否则按规则类型映射到六维之一（与 {@link QualityScorer#getDimension} 一致）。
     */
    private Set<String> resolveDetailDimensions(DqcExecutionDetail detail) {
        String raw = detail.getDimensions();
        if (raw != null && !raw.isBlank()) {
            Set<String> parsed = parseDimensionsString(raw);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        String rt = detail.getRuleType();
        return Collections.singleton(qualityScorer.getDimension(rt != null ? rt : ""));
    }

    private Set<String> parseDimensionsString(String dimensions) {
        Set<String> result = new HashSet<>();
        try {
            JSONArray array = JSON.parseArray(dimensions);
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    String s = array.getString(i);
                    if (s != null) {
                        result.add(s.toUpperCase());
                    }
                }
            }
        } catch (Exception e) {
            result.add(dimensions.trim().toUpperCase());
        }
        return result;
    }

    /**
     * 某维度下无任何执行明细时返回 null（前端展示「未评估」），不再默认 100。
     */
    private BigDecimal calculateDimensionScore(List<DqcExecutionDetail> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        int passed = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int total = details.size();
        return BigDecimal.valueOf((double) passed / total * 100).setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 新增：评分趋势分析实现 ====================

    /**
     * 获取质量评分趋势
     */
    @Override
    public Result<QualityScoreTrendVO> getScoreTrend(String dimension, String layerCode,
                                                     String startDate, String endDate) {
        LambdaQueryWrapper<DqcQualityScore> wrapper = buildScoreQueryWrapper(layerCode, startDate, endDate);
        if (StringUtils.hasText(dimension)) {
            wrapper.apply("JSON_EXTRACT(JSON_OBJECT(" +
                    "'completeness', completeness_score, " +
                    "'uniqueness', uniqueness_score, " +
                    "'accuracy', accuracy_score, " +
                    "'consistency', consistency_score, " +
                    "'timeliness', timeliness_score, " +
                    "'validity', validity_score), '$.{0}') IS NOT NULL", dimension.toUpperCase());
        }

        wrapper.orderByAsc(DqcQualityScore::getCheckDate);
        List<DqcQualityScore> scores = this.list(wrapper);

        if (scores.isEmpty()) {
            return Result.success(QualityScoreTrendVO.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .layerCode(layerCode)
                    .dimension(dimension)
                    .overallTrend(Collections.emptyList())
                    .dimensionTrends(Collections.emptyMap())
                    .analysis(null)
                    .build());
        }

        // 按日期分组计算每天的平均评分
        Map<String, List<DqcQualityScore>> byDate = scores.stream()
                .collect(Collectors.groupingBy(s -> s.getCheckDate().toLocalDate().toString()));

        List<QualityScoreTrendVO.TrendDataPoint> overallTrend = new ArrayList<>();
        for (Map.Entry<String, List<DqcQualityScore>> entry : byDate.entrySet()) {
            List<DqcQualityScore> dayScores = entry.getValue();

            double avgScore = dayScores.stream()
                    .filter(s -> s.getOverallScore() != null)
                    .mapToDouble(s -> s.getOverallScore().doubleValue())
                    .average().orElse(0);

            double avgPassRate = dayScores.stream()
                    .filter(s -> s.getRulePassRate() != null)
                    .mapToDouble(s -> s.getRulePassRate().doubleValue())
                    .average().orElse(0);

            int totalRules = dayScores.stream().mapToInt(s -> s.getRuleTotalCount() != null ? s.getRuleTotalCount() : 0).sum();
            int passedRules = dayScores.stream().mapToInt(s -> s.getRulePassCount() != null ? s.getRulePassCount() : 0).sum();
            int failedRules = dayScores.stream().mapToInt(s -> s.getRuleFailCount() != null ? s.getRuleFailCount() : 0).sum();

            BigDecimal dayPassRate = totalRules > 0
                    ? BigDecimal.valueOf(100.0 * passedRules / totalRules).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(avgPassRate).setScale(2, RoundingMode.HALF_UP);

            overallTrend.add(QualityScoreTrendVO.TrendDataPoint.builder()
                    .date(entry.getKey())
                    .score(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP))
                    .passRate(dayPassRate)
                    .ruleCount(totalRules)
                    .passedCount(passedRules)
                    .failedCount(failedRules)
                    .build());
        }

        overallTrend.sort(Comparator.comparing(QualityScoreTrendVO.TrendDataPoint::getDate));

        // 计算趋势分析
        QualityScoreTrendVO.TrendAnalysis analysis = calculateTrendAnalysis(overallTrend);

        return Result.success(QualityScoreTrendVO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .layerCode(layerCode)
                .dimension(dimension)
                .overallTrend(overallTrend)
                .dimensionTrends(Collections.emptyMap())
                .analysis(analysis)
                .build());
    }

    /**
     * 获取评分趋势对比
     */
    @Override
    public Result<Map<String, Object>> getTrendComparison(String layerCode,
                                                        String currentPeriodStart, String currentPeriodEnd,
                                                        String previousPeriodStart, String previousPeriodEnd) {
        // 当前周期评分
        LambdaQueryWrapper<DqcQualityScore> currentWrapper = buildScoreQueryWrapper(layerCode, currentPeriodStart, currentPeriodEnd);
        List<DqcQualityScore> currentScores = this.list(currentWrapper);

        // 对比周期评分
        LambdaQueryWrapper<DqcQualityScore> previousWrapper = buildScoreQueryWrapper(layerCode, previousPeriodStart, previousPeriodEnd);
        List<DqcQualityScore> previousScores = this.list(previousWrapper);

        double currentAvg = currentScores.stream()
                .filter(s -> s.getOverallScore() != null)
                .mapToDouble(s -> s.getOverallScore().doubleValue())
                .average().orElse(0);

        double previousAvg = previousScores.stream()
                .filter(s -> s.getOverallScore() != null)
                .mapToDouble(s -> s.getOverallScore().doubleValue())
                .average().orElse(0);

        double change = currentAvg - previousAvg;
        double changePercent = previousAvg != 0 ? (change / previousAvg * 100) : 0;

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("currentPeriodStart", currentPeriodStart);
        comparison.put("currentPeriodEnd", currentPeriodEnd);
        comparison.put("previousPeriodStart", previousPeriodStart);
        comparison.put("previousPeriodEnd", previousPeriodEnd);
        comparison.put("currentAvgScore", BigDecimal.valueOf(currentAvg).setScale(2, RoundingMode.HALF_UP));
        comparison.put("previousAvgScore", BigDecimal.valueOf(previousAvg).setScale(2, RoundingMode.HALF_UP));
        comparison.put("scoreChange", BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP));
        comparison.put("scoreChangePercent", BigDecimal.valueOf(changePercent).setScale(2, RoundingMode.HALF_UP));
        comparison.put("trend", change > 0 ? "UP" : (change < 0 ? "DOWN" : "FLAT"));
        comparison.put("currentRecordCount", currentScores.size());
        comparison.put("previousRecordCount", previousScores.size());

        return Result.success(comparison);
    }

    // ==================== 新增：评分概览实现 ====================

    /**
     * 获取质量评分概览
     */
    @Override
    public Result<QualityScoreOverviewVO> getScoreOverview(String startDate, String endDate) {
        LambdaQueryWrapper<DqcQualityScore> wrapper = buildScoreQueryWrapper(null, startDate, endDate);
        List<DqcQualityScore> scores = this.list(wrapper);

        if (scores.isEmpty()) {
            return Result.success(QualityScoreOverviewVO.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .overallScore(BigDecimal.ZERO)
                    .scoreGrade("UNKNOWN")
                    .rulePassRate(BigDecimal.ZERO)
                    .dimensionScores(QualityScoreOverviewVO.DimensionScores.builder().build())
                    .distribution(QualityScoreOverviewVO.ScoreDistribution.builder().build())
                    .issues(Collections.emptyList())
                    .build());
        }

        // 计算综合评分
        double avgScore = scores.stream()
                .filter(s -> s.getOverallScore() != null)
                .mapToDouble(s -> s.getOverallScore().doubleValue())
                .average().orElse(0);

        String scoreGrade = getScoreGrade((int) avgScore);
        String scoreColor = getScoreColor((int) avgScore);

        // 计算各维度平均分
        BigDecimal completeness = calculateDimAvg(scores, DqcQualityScore::getCompletenessScore);
        BigDecimal uniqueness = calculateDimAvg(scores, DqcQualityScore::getUniquenessScore);
        BigDecimal accuracy = calculateDimAvg(scores, DqcQualityScore::getAccuracyScore);
        BigDecimal consistency = calculateDimAvg(scores, DqcQualityScore::getConsistencyScore);
        BigDecimal timeliness = calculateDimAvg(scores, DqcQualityScore::getTimelinessScore);
        BigDecimal validity = calculateDimAvg(scores, DqcQualityScore::getValidityScore);

        int totalRules = scores.stream().mapToInt(s -> s.getRuleTotalCount() != null ? s.getRuleTotalCount() : 0).sum();
        int passedRules = scores.stream().mapToInt(s -> s.getRulePassCount() != null ? s.getRulePassCount() : 0).sum();
        int failedRules = scores.stream().mapToInt(s -> s.getRuleFailCount() != null ? s.getRuleFailCount() : 0).sum();

        // 规则通过率按「总通过 / 总规则」聚合，与各表行内计数一致，避免简单平均各行百分比失真
        BigDecimal aggregatePassRate = totalRules > 0
                ? BigDecimal.valueOf(100.0 * passedRules / totalRules).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 评分分布
        QualityScoreOverviewVO.ScoreDistribution distribution = calculateScoreDistribution(scores);

        // 各数据层评分
        Map<String, BigDecimal> layerScores = scores.stream()
                .filter(s -> s.getLayerCode() != null && s.getOverallScore() != null)
                .collect(Collectors.groupingBy(
                        DqcQualityScore::getLayerCode,
                        Collectors.averagingDouble(s -> s.getOverallScore().doubleValue())))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> BigDecimal.valueOf(e.getValue()).setScale(2, RoundingMode.HALF_UP)));

        // 质量异常列表
        List<QualityScoreOverviewVO.QualityIssue> issues = findQualityIssues(scores);

        String overallAssessment = generateOverallAssessment((int) avgScore, issues);

        return Result.success(QualityScoreOverviewVO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .overallScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP))
                .scoreGrade(scoreGrade)
                .scoreColor(scoreColor)
                .rulePassRate(aggregatePassRate)
                .totalRules(totalRules)
                .passedRules(passedRules)
                .failedRules(failedRules)
                .dimensionScores(QualityScoreOverviewVO.DimensionScores.builder()
                        .completeness(completeness)
                        .uniqueness(uniqueness)
                        .accuracy(accuracy)
                        .consistency(consistency)
                        .timeliness(timeliness)
                        .validity(validity)
                        .build())
                .layerScores(layerScores)
                .distribution(distribution)
                .issues(issues)
                .overallAssessment(overallAssessment)
                .build());
    }

    /**
     * 获取各维度评分详情
     */
    @Override
    public Result<List<DimensionScoreVO>> getDimensionScores(String dimension, String layerCode,
                                                            String startDate, String endDate) {
        LambdaQueryWrapper<DqcQualityScore> wrapper = buildScoreQueryWrapper(layerCode, startDate, endDate);
        List<DqcQualityScore> scores = this.list(wrapper);

        if (scores.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 定义所有维度
        List<String> allDimensions = List.of(
                "COMPLETENESS", "UNIQUENESS", "ACCURACY",
                "CONSISTENCY", "TIMELINESS", "VALIDITY"
        );

        List<DimensionScoreVO> result = new ArrayList<>();
        Map<String, BigDecimal> weights = qualityScorer.getDimensionWeights();

        for (String dim : allDimensions) {
            if (StringUtils.hasText(dimension) && !dimension.equalsIgnoreCase(dim)) {
                continue;
            }

            BigDecimal dimAvg = calculateDimAvgByCode(scores, dim);
            String dimName = qualityScorer.getDimensionName(dim);

            // 获取该维度涉及的所有记录
            List<DqcQualityScore> dimScores = scores.stream()
                    .filter(s -> hasDimension(s, dim))
                    .collect(Collectors.toList());

            int tableCount = (int) dimScores.stream()
                    .map(DqcQualityScore::getTargetTable)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            // 获取该维度评分最低的表
            List<QualityScoreOverviewVO.QualityIssue> issues = dimScores.stream()
                    .filter(s -> s.getOverallScore() != null && s.getOverallScore().intValue() < GOOD_THRESHOLD)
                    .sorted(Comparator.comparing(DqcQualityScore::getOverallScore))
                    .limit(5)
                    .map(s -> QualityScoreOverviewVO.QualityIssue.builder()
                            .tableName(s.getTargetTable())
                            .dsId(s.getTargetDsId())
                            .score(s.getOverallScore())
                            .dimension(dim)
                            .issueDescription("评分偏低")
                            .build())
                    .collect(Collectors.toList());

            result.add(DimensionScoreVO.builder()
                    .dimensionCode(dim)
                    .dimensionName(dimName)
                    .dimensionDesc("")
                    .weight(weights.getOrDefault(dim, BigDecimal.valueOf(0.1)))
                    .score(dimAvg)
                    .scoreGrade(dimAvg != null ? getScoreGrade(dimAvg.intValue()) : "N/A")
                    .scoreColor(dimAvg != null ? getScoreColor(dimAvg.intValue()) : "#999999")
                    .ruleCount(dimScores.size())
                    .passedCount((int) dimScores.stream()
                            .filter(s -> s.getRulePassRate() != null && s.getRulePassRate().intValue() >= GOOD_THRESHOLD)
                            .count())
                    .failedCount((int) dimScores.stream()
                            .filter(s -> s.getRulePassRate() != null && s.getRulePassRate().intValue() < GOOD_THRESHOLD)
                            .count())
                    .passRate(dimAvg)
                    .tableCount(tableCount)
                    .attentionTables(issues.stream()
                            .map(i -> DimensionScoreVO.TableScore.builder()
                                    .tableName(i.getTableName())
                                    .dsId(i.getDsId())
                                    .score(i.getScore())
                                    .issueDescription(i.getIssueDescription())
                                    .build())
                            .collect(Collectors.toList()))
                    .build());
        }

        return Result.success(result);
    }

    /**
     * 获取评分分布统计
     */
    @Override
    public Result<Map<String, Object>> getScoreDistribution(String layerCode, String startDate, String endDate) {
        LambdaQueryWrapper<DqcQualityScore> wrapper = buildScoreQueryWrapper(layerCode, startDate, endDate);
        List<DqcQualityScore> scores = this.list(wrapper);

        int excellent = 0, good = 0, warning = 0, critical = 0;
        for (DqcQualityScore score : scores) {
            if (score.getOverallScore() == null) continue;
            int val = score.getOverallScore().intValue();
            if (val >= EXCELLENT_THRESHOLD) excellent++;
            else if (val >= GOOD_THRESHOLD) good++;
            else if (val >= WARNING_THRESHOLD) warning++;
            else critical++;
        }

        int total = scores.size();
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("excellentCount", excellent);
        result.put("goodCount", good);
        result.put("warningCount", warning);
        result.put("criticalCount", critical);
        result.put("excellentPercent", total > 0 ? BigDecimal.valueOf(100.0 * excellent / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        result.put("goodPercent", total > 0 ? BigDecimal.valueOf(100.0 * good / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        result.put("warningPercent", total > 0 ? BigDecimal.valueOf(100.0 * warning / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        result.put("criticalPercent", total > 0 ? BigDecimal.valueOf(100.0 * critical / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return Result.success(result);
    }

    /**
     * 获取质量异常告警列表
     */
    @Override
    public Result<List<Map<String, Object>>> getQualityAlerts(Integer scoreThreshold, String layerCode,
                                                            String startDate, String endDate) {
        int threshold = scoreThreshold != null ? scoreThreshold : WARNING_THRESHOLD;
        LambdaQueryWrapper<DqcQualityScore> wrapper = buildScoreQueryWrapper(layerCode, startDate, endDate);
        List<DqcQualityScore> scores = this.list(wrapper);

        List<Map<String, Object>> alerts = scores.stream()
                .filter(s -> s.getOverallScore() != null && s.getOverallScore().intValue() < threshold)
                .sorted(Comparator.comparing(DqcQualityScore::getOverallScore))
                .map(s -> {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("tableName", s.getTargetTable());
                    alert.put("dsId", s.getTargetDsId());
                    alert.put("layerCode", s.getLayerCode());
                    alert.put("score", s.getOverallScore());
                    alert.put("passRate", s.getRulePassRate());
                    alert.put("failedRules", s.getRuleFailCount());
                    alert.put("checkDate", s.getCheckDate());
                    alert.put("alertLevel", getAlertLevel(s.getOverallScore().intValue()));
                    return alert;
                })
                .collect(Collectors.toList());

        return Result.success(alerts);
    }

    // ==================== 私有辅助方法 ====================

    private LambdaQueryWrapper<DqcQualityScore> buildScoreQueryWrapper(String layerCode, String startDate, String endDate) {
        LambdaQueryWrapper<DqcQualityScore> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(layerCode)) {
            wrapper.eq(DqcQualityScore::getLayerCode, layerCode);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(DqcQualityScore::getCheckDate,
                    LocalDateTime.parse(startDate + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(DqcQualityScore::getCheckDate,
                    LocalDateTime.parse(endDate + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return wrapper;
    }

    private QualityScoreTrendVO.TrendAnalysis calculateTrendAnalysis(List<QualityScoreTrendVO.TrendDataPoint> trend) {
        if (trend == null || trend.isEmpty()) {
            return null;
        }

        List<BigDecimal> scores = trend.stream()
                .map(QualityScoreTrendVO.TrendDataPoint::getScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (scores.isEmpty()) {
            return null;
        }

        double avg = scores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double max = scores.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0);
        double min = scores.stream().mapToDouble(BigDecimal::doubleValue).min().orElse(0);

        // 计算标准差
        double variance = scores.stream()
                .mapToDouble(s -> Math.pow(s.doubleValue() - avg, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        // 判断趋势方向（比较前半段和后半段的平均分）
        String direction = "FLAT";
        double change = 0;
        double changePercent = 0;
        if (trend.size() >= 4) {
            int mid = trend.size() / 2;
            double firstHalfAvg = trend.subList(0, mid).stream()
                    .map(QualityScoreTrendVO.TrendDataPoint::getScore)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average().orElse(0);
            double secondHalfAvg = trend.subList(mid, trend.size()).stream()
                    .map(QualityScoreTrendVO.TrendDataPoint::getScore)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average().orElse(0);
            change = secondHalfAvg - firstHalfAvg;
            if (firstHalfAvg != 0) {
                changePercent = change / firstHalfAvg * 100;
            }
            direction = change > 2 ? "UP" : (change < -2 ? "DOWN" : "FLAT");
        }

        String assessment = generateTrendAssessment(avg, direction);

        return QualityScoreTrendVO.TrendAnalysis.builder()
                .avgScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP))
                .maxScore(BigDecimal.valueOf(max).setScale(2, RoundingMode.HALF_UP))
                .minScore(BigDecimal.valueOf(min).setScale(2, RoundingMode.HALF_UP))
                .volatility(BigDecimal.valueOf(stdDev).setScale(2, RoundingMode.HALF_UP))
                .trendDirection(direction)
                .trendChange(BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP))
                .trendChangePercent(BigDecimal.valueOf(changePercent).setScale(2, RoundingMode.HALF_UP))
                .overallAssessment(assessment)
                .attentionDimensions(Collections.emptyList())
                .build();
    }

    private BigDecimal calculateDimAvg(List<DqcQualityScore> scores,
                                       java.util.function.Function<DqcQualityScore, BigDecimal> extractor) {
        List<BigDecimal> vals = scores.stream().map(extractor).filter(Objects::nonNull).toList();
        if (vals.isEmpty()) {
            return null;
        }
        double avg = vals.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDimAvgByCode(List<DqcQualityScore> scores, String dimCode) {
        java.util.function.Function<DqcQualityScore, BigDecimal> extractor = switch (dimCode.toUpperCase()) {
            case "COMPLETENESS" -> DqcQualityScore::getCompletenessScore;
            case "UNIQUENESS" -> DqcQualityScore::getUniquenessScore;
            case "ACCURACY" -> DqcQualityScore::getAccuracyScore;
            case "CONSISTENCY" -> DqcQualityScore::getConsistencyScore;
            case "TIMELINESS" -> DqcQualityScore::getTimelinessScore;
            case "VALIDITY" -> DqcQualityScore::getValidityScore;
            default -> s -> s.getOverallScore();
        };
        return calculateDimAvg(scores, extractor);
    }

    private boolean hasDimension(DqcQualityScore score, String dimCode) {
        return switch (dimCode.toUpperCase()) {
            case "COMPLETENESS" -> score.getCompletenessScore() != null;
            case "UNIQUENESS" -> score.getUniquenessScore() != null;
            case "ACCURACY" -> score.getAccuracyScore() != null;
            case "CONSISTENCY" -> score.getConsistencyScore() != null;
            case "TIMELINESS" -> score.getTimelinessScore() != null;
            case "VALIDITY" -> score.getValidityScore() != null;
            default -> true;
        };
    }

    private QualityScoreOverviewVO.ScoreDistribution calculateScoreDistribution(List<DqcQualityScore> scores) {
        int excellent = 0, good = 0, warning = 0, critical = 0;
        for (DqcQualityScore s : scores) {
            if (s.getOverallScore() == null) continue;
            int val = s.getOverallScore().intValue();
            if (val >= EXCELLENT_THRESHOLD) excellent++;
            else if (val >= GOOD_THRESHOLD) good++;
            else if (val >= WARNING_THRESHOLD) warning++;
            else critical++;
        }
        int total = scores.size();
        return QualityScoreOverviewVO.ScoreDistribution.builder()
                .excellentCount(excellent)
                .goodCount(good)
                .warningCount(warning)
                .criticalCount(critical)
                .excellentPercent(total > 0 ? BigDecimal.valueOf(100.0 * excellent / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .goodPercent(total > 0 ? BigDecimal.valueOf(100.0 * good / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .build();
    }

    private List<QualityScoreOverviewVO.QualityIssue> findQualityIssues(List<DqcQualityScore> scores) {
        return scores.stream()
                .filter(s -> s.getOverallScore() != null && s.getOverallScore().intValue() < WARNING_THRESHOLD)
                .sorted(Comparator.comparing(DqcQualityScore::getOverallScore))
                .limit(10)
                .map(s -> {
                    String dim = findLowestDimension(s);
                    return QualityScoreOverviewVO.QualityIssue.builder()
                            .tableName(s.getTargetTable())
                            .dsId(s.getTargetDsId())
                            .score(s.getOverallScore())
                            .dimension(dim)
                            .issueType("LOW_SCORE")
                            .issueDescription("综合评分偏低，需要关注")
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String findLowestDimension(DqcQualityScore score) {
        Map<String, BigDecimal> dims = new HashMap<>();
        if (score.getCompletenessScore() != null) dims.put("完整性", score.getCompletenessScore());
        if (score.getUniquenessScore() != null) dims.put("唯一性", score.getUniquenessScore());
        if (score.getAccuracyScore() != null) dims.put("准确性", score.getAccuracyScore());
        if (score.getConsistencyScore() != null) dims.put("一致性", score.getConsistencyScore());
        if (score.getTimelinessScore() != null) dims.put("及时性", score.getTimelinessScore());
        if (score.getValidityScore() != null) dims.put("有效性", score.getValidityScore());
        return dims.entrySet().stream()
                .min(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("综合");
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

    private String getAlertLevel(int score) {
        if (score >= EXCELLENT_THRESHOLD) return "INFO";
        if (score >= GOOD_THRESHOLD) return "NORMAL";
        if (score >= WARNING_THRESHOLD) return "WARN";
        return "CRITICAL";
    }

    private String generateOverallAssessment(int score, List<QualityScoreOverviewVO.QualityIssue> issues) {
        if (score >= EXCELLENT_THRESHOLD) {
            return "数据质量整体优秀，继续保持";
        } else if (score >= GOOD_THRESHOLD) {
            return "数据质量良好，存在少量问题需要关注";
        } else if (score >= WARNING_THRESHOLD) {
            return "数据质量一般，建议尽快处理质量问题";
        } else {
            return "数据质量较差，需要立即处理";
        }
    }

    private String generateTrendAssessment(double score, String direction) {
        String scoreDesc;
        if (score >= EXCELLENT_THRESHOLD) scoreDesc = "优秀";
        else if (score >= GOOD_THRESHOLD) scoreDesc = "良好";
        else if (score >= WARNING_THRESHOLD) scoreDesc = "一般";
        else scoreDesc = "较差";

        String trendDesc = switch (direction) {
            case "UP" -> "呈上升趋势";
            case "DOWN" -> "呈下降趋势";
            default -> "保持稳定";
        };

        return String.format("整体评分%s，%s", scoreDesc, trendDesc);
    }
}
