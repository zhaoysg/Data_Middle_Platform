package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.vo.DqcQualityScoreVo;

import java.util.List;

/**
 * 数据质量评分服务接口
 */
public interface IDqcQualityScoreService {

    /**
     * 根据执行ID计算并保存评分
     * <p>
     * 由 DqcExecutionServiceImpl 在执行完成后调用。
     * 从 dqc_execution_detail 聚合各维度规则结果，计算 6 个维度分 + 综合分，写入 dqc_quality_score。
     *
     * @param executionId 执行记录ID
     */
    void calculateAndSaveScore(Long executionId);

    /**
     * 分页查询评分列表
     */
    TableDataInfo<DqcQualityScoreVo> queryPageList(DqcQualityScoreVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询评分
     */
    DqcQualityScoreVo queryById(Long id);

    /**
     * 查询最近N天的评分趋势（用于仪表盘折线图）
     *
     * @param days 天数，默认30天
     * @return 每日汇总评分列表
     */
    List<DqcQualityScoreVo> queryTrend(int days);

    /**
     * 查询指定数据源+表的最新评分
     *
     * @param dsId 数据源ID
     * @param tableName 表名
     * @return 最新评分
     */
    DqcQualityScoreVo queryLatestByTable(Long dsId, String tableName);
}
