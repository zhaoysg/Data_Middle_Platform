package com.bagdatahouse.dqc.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqcQualityScore;
import com.bagdatahouse.core.vo.QualityScoreTrendVO;
import com.bagdatahouse.core.vo.QualityScoreOverviewVO;
import com.bagdatahouse.core.vo.DimensionScoreVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DQC质量评分服务接口
 *
 * 提供六维质量评分查询和分析功能：
 * - 完整性（COMPLETENESS）
 * - 唯一性（UNIQUENESS）
 * - 准确性（ACCURACY）
 * - 一致性（CONSISTENCY）
 * - 及时性（TIMELINESS）
 * - 有效性（VALIDITY）
 */
public interface DqcQualityScoreService {

    /**
     * 按日统计评分
     */
    Result<List<DqcQualityScore>> getDailyScores(Long dsId, String tableName, String startDate, String endDate);

    /**
     * 按表统计评分
     */
    Result<List<DqcQualityScore>> getTableScores(String layerCode, String startDate, String endDate);

    /**
     * 按数据层统计评分
     */
    Result<Map<String, BigDecimal>> getLayerScoreSummary(String startDate, String endDate);

    /**
     * 计算并保存评分
     */
    void calculateAndSaveScore(Long executionId);

    // ==================== 新增：评分趋势分析 ====================

    /**
     * 获取质量评分趋势
     * 按时间维度分析评分变化趋势
     *
     * @param dimension 维度（可选，为空则返回所有维度）
     * @param layerCode 数据层（可选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 评分趋势数据
     */
    Result<QualityScoreTrendVO> getScoreTrend(String dimension, String layerCode, String startDate, String endDate);

    /**
     * 获取评分趋势对比
     * 对比两个时间段的评分变化
     *
     * @param layerCode 数据层
     * @param currentPeriodStart 当前周期开始日期
     * @param currentPeriodEnd 当前周期结束日期
     * @param previousPeriodStart 对比周期开始日期
     * @param previousPeriodEnd 对比周期结束日期
     * @return 评分对比数据
     */
    Result<Map<String, Object>> getTrendComparison(String layerCode,
                                                   String currentPeriodStart, String currentPeriodEnd,
                                                   String previousPeriodStart, String previousPeriodEnd);

    // ==================== 新增：评分概览 ====================

    /**
     * 获取质量评分概览
     * 提供当前整体质量状况的概览信息
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 评分概览数据
     */
    Result<QualityScoreOverviewVO> getScoreOverview(String startDate, String endDate);

    /**
     * 获取各维度评分详情
     *
     * @param dimension 维度
     * @param layerCode 数据层（可选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 维度评分列表
     */
    Result<List<DimensionScoreVO>> getDimensionScores(String dimension, String layerCode,
                                                     String startDate, String endDate);

    /**
     * 获取评分分布统计
     * 统计各评分区间的分布情况
     *
     * @param layerCode 数据层（可选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 评分分布
     */
    Result<Map<String, Object>> getScoreDistribution(String layerCode, String startDate, String endDate);

    /**
     * 获取质量异常告警列表
     * 评分低于阈值的表或维度
     *
     * @param scoreThreshold 评分阈值（默认60）
     * @param layerCode 数据层（可选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 异常列表
     */
    Result<List<Map<String, Object>>> getQualityAlerts(Integer scoreThreshold, String layerCode,
                                                       String startDate, String endDate);
}
