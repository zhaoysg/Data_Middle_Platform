package org.dromara.metadata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.DqcExecution;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcQualityScore;
import org.dromara.metadata.domain.vo.DqcQualityScoreVo;
import org.dromara.metadata.mapper.DqcExecutionDetailMapper;
import org.dromara.metadata.mapper.DqcExecutionMapper;
import org.dromara.metadata.mapper.DqcQualityScoreMapper;
import org.dromara.metadata.service.IDqcQualityScoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据质量评分服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DqcQualityScoreServiceImpl implements IDqcQualityScoreService {

    private final DqcQualityScoreMapper baseMapper;
    private final DqcExecutionMapper executionMapper;
    private final DqcExecutionDetailMapper detailMapper;

    private static final String DIM_COMPLETENESS = "COMPLETENESS";
    private static final String DIM_UNIQUENESS = "UNIQUENESS";
    private static final String DIM_ACCURACY = "ACCURACY";
    private static final String DIM_CONSISTENCY = "CONSISTENCY";
    private static final String DIM_TIMELINESS = "TIMELINESS";
    private static final String DIM_VALIDITY = "VALIDITY";

    private static final List<String> ALL_DIMS = List.of(
        DIM_COMPLETENESS, DIM_UNIQUENESS, DIM_ACCURACY,
        DIM_CONSISTENCY, DIM_TIMELINESS, DIM_VALIDITY
    );

    /**
     * 每张表的评分聚合器
     */
    private static class TableScoreAccumulator {
        Long dsId;
        String tableName;
        BigDecimal completeness;
        BigDecimal uniqueness;
        BigDecimal accuracy;
        BigDecimal consistency;
        BigDecimal timeliness;
        BigDecimal validity;
        BigDecimal overallScore;
        BigDecimal rulePassRate;
        int totalRules = 0;
        int totalPass = 0;
        final Map<String, Integer> dimPass = new HashMap<>();
        final Map<String, Integer> dimTotal = new HashMap<>();

        TableScoreAccumulator() {
            for (String d : ALL_DIMS) {
                dimPass.put(d, 0);
                dimTotal.put(d, 0);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateAndSaveScore(Long executionId) {
        DqcExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.warn("执行记录不存在，跳过评分计算: executionId={}", executionId);
            return;
        }

        List<DqcExecutionDetail> details = detailMapper.selectList(
            Wrappers.<DqcExecutionDetail>lambdaQuery()
                .eq(DqcExecutionDetail::getExecutionId, executionId)
        );

        if (details.isEmpty()) {
            log.info("执行明细为空，跳过评分: executionId={}", executionId);
            return;
        }

        // 全局维度统计（用于参考日志）
        Map<String, Integer> globalDimPass = new HashMap<>();
        Map<String, Integer> globalDimTotal = new HashMap<>();
        for (String d : ALL_DIMS) {
            globalDimPass.put(d, 0);
            globalDimTotal.put(d, 0);
        }

        // 按 (targetDsId, targetTable) 分组，精确计算每张表的维度得分和综合得分
        Map<String, TableScoreAccumulator> tableAccumulators = new HashMap<>();
        for (DqcExecutionDetail detail : details) {
            if (detail.getTargetDsId() == null || StringUtils.isBlank(detail.getTargetTable())) continue;

            String key = detail.getTargetDsId() + "|" + detail.getTargetTable();
            TableScoreAccumulator acc = tableAccumulators.computeIfAbsent(key, k -> new TableScoreAccumulator());
            acc.dsId = detail.getTargetDsId();
            acc.tableName = detail.getTargetTable();

            String dim = detail.getDimension();
            if (dim == null) continue;
            dim = dim.toUpperCase();
            if (!ALL_DIMS.contains(dim)) continue;

            globalDimTotal.merge(dim, 1, Integer::sum);
            acc.dimTotal.merge(dim, 1, Integer::sum);
            acc.totalRules++;
            globalDimPass.merge(dim, "1".equals(detail.getPassFlag()) ? 1 : 0, Integer::sum);
            if ("1".equals(detail.getPassFlag())) {
                acc.dimPass.merge(dim, 1, Integer::sum);
                acc.totalPass++;
            }
        }

        // 计算每张表的各项得分
        for (TableScoreAccumulator acc : tableAccumulators.values()) {
            acc.completeness = calcDimScore(acc.dimPass.get(DIM_COMPLETENESS), acc.dimTotal.get(DIM_COMPLETENESS));
            acc.uniqueness = calcDimScore(acc.dimPass.get(DIM_UNIQUENESS), acc.dimTotal.get(DIM_UNIQUENESS));
            acc.accuracy = calcDimScore(acc.dimPass.get(DIM_ACCURACY), acc.dimTotal.get(DIM_ACCURACY));
            acc.consistency = calcDimScore(acc.dimPass.get(DIM_CONSISTENCY), acc.dimTotal.get(DIM_CONSISTENCY));
            acc.timeliness = calcDimScore(acc.dimPass.get(DIM_TIMELINESS), acc.dimTotal.get(DIM_TIMELINESS));
            acc.validity = calcDimScore(acc.dimPass.get(DIM_VALIDITY), acc.dimTotal.get(DIM_VALIDITY));

            // 每张表的综合得分 = 该表各维度的简单平均（零维度不计入）
            acc.overallScore = calcOverallScore(acc.dimPass, acc.dimTotal);
            acc.rulePassRate = acc.totalRules > 0
                ? BigDecimal.valueOf(acc.totalPass * 100.0 / acc.totalRules).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        }

        // 写入评分记录（upsert：存在则更新，不存在则插入）
        for (TableScoreAccumulator acc : tableAccumulators.values()) {
            DqcQualityScore score = buildScoreRecord(acc, execution);
            upsertScore(score);
        }

        log.info("评分计算完成: executionId={}, 写入{}条评分记录",
            executionId, tableAccumulators.size());
    }

    /**
     * 构建评分记录
     */
    private DqcQualityScore buildScoreRecord(TableScoreAccumulator acc, DqcExecution execution) {
        DqcQualityScore score = new DqcQualityScore();
        score.setCheckDate(LocalDate.now());
        score.setTargetDsId(acc.dsId);
        score.setTargetTable(acc.tableName);
        score.setLayerCode(execution.getLayerCode());
        score.setExecutionId(execution.getId());
        score.setPlanId(execution.getPlanId());
        score.setPlanName(execution.getPlanName());
        score.setCompletenessScore(acc.completeness);
        score.setUniquenessScore(acc.uniqueness);
        score.setAccuracyScore(acc.accuracy);
        score.setConsistencyScore(acc.consistency);
        score.setTimelinessScore(acc.timeliness);
        score.setValidityScore(acc.validity);
        score.setOverallScore(acc.overallScore);
        score.setRulePassRate(acc.rulePassRate);
        score.setRuleTotalCount(acc.totalRules);
        score.setRulePassCount(acc.totalPass);
        score.setRuleFailCount(acc.totalRules - acc.totalPass);
        return score;
    }

    /**
     * Upsert：按 (checkDate, targetDsId, targetTable) 唯一键，存在则更新，不存在则插入
     */
    private void upsertScore(DqcQualityScore score) {
        LambdaQueryWrapper<DqcQualityScore> wrapper = Wrappers.<DqcQualityScore>lambdaQuery()
            .eq(DqcQualityScore::getCheckDate, score.getCheckDate())
            .eq(DqcQualityScore::getTargetDsId, score.getTargetDsId())
            .eq(DqcQualityScore::getTargetTable, score.getTargetTable());
        DqcQualityScore existing = baseMapper.selectOne(wrapper);
        if (existing != null) {
            score.setId(existing.getId());
            baseMapper.updateById(score);
        } else {
            baseMapper.insert(score);
        }
    }

    /**
     * 计算单个维度得分：passCount / totalCount * 100
     */
    private BigDecimal calcDimScore(Integer passCount, Integer totalCount) {
        if (totalCount == null || totalCount == 0) return null;
        return BigDecimal.valueOf((passCount != null ? passCount : 0) * 100.0 / totalCount)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算综合得分：所有维度得分的简单平均（零维度不计入）
     */
    private BigDecimal calcOverallScore(Map<String, Integer> dimPass, Map<String, Integer> dimTotal) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (String dim : ALL_DIMS) {
            Integer total = dimTotal.get(dim);
            if (total != null && total > 0) {
                BigDecimal dimScore = calcDimScore(dimPass.get(dim), total);
                if (dimScore != null) {
                    sum = sum.add(dimScore);
                    count++;
                }
            }
        }
        if (count == 0) return BigDecimal.ZERO;
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    @Override
    public TableDataInfo<DqcQualityScoreVo> queryPageList(DqcQualityScoreVo vo, PageQuery pageQuery) {
        var wrapper = Wrappers.<DqcQualityScore>lambdaQuery()
            .eq(vo != null && vo.getTargetDsId() != null,
                DqcQualityScore::getTargetDsId, vo.getTargetDsId())
            .like(StringUtils.isNotBlank(vo != null ? vo.getTargetTable() : null),
                DqcQualityScore::getTargetTable, vo.getTargetTable())
            .ge(vo != null && vo.getParams() != null && vo.getParams().get("beginTime") != null,
                DqcQualityScore::getCheckDate,
                vo.getParams().get("beginTime"))
            .le(vo != null && vo.getParams() != null && vo.getParams().get("endTime") != null,
                DqcQualityScore::getCheckDate,
                vo.getParams().get("endTime"))
            .orderByDesc(DqcQualityScore::getCheckDate);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        MapstructUtils.convert(page.getRecords(), DqcQualityScoreVo.class);
        return TableDataInfo.build(page);
    }

    @Override
    public DqcQualityScoreVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<DqcQualityScoreVo> queryTrend(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return baseMapper.selectVoList(
            Wrappers.<DqcQualityScore>lambdaQuery()
                .ge(DqcQualityScore::getCheckDate, startDate)
                .orderByAsc(DqcQualityScore::getCheckDate)
        );
    }

    @Override
    public DqcQualityScoreVo queryLatestByTable(Long dsId, String tableName) {
        return baseMapper.selectVoList(
            Wrappers.<DqcQualityScore>lambdaQuery()
                .eq(DqcQualityScore::getTargetDsId, dsId)
                .eq(DqcQualityScore::getTargetTable, tableName)
                .orderByDesc(DqcQualityScore::getCheckDate)
                .last("LIMIT 1")
        ).stream().findFirst().orElse(null);
    }
}
