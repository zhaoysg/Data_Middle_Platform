package com.bagdatahouse.dqc.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.vo.QualityReportExecutionSheetVO;
import com.bagdatahouse.core.vo.QualityReportRuleDetailSheetVO;
import com.bagdatahouse.core.vo.QualityReportVO;

import java.util.List;

/**
 * 质量报告服务接口
 * 提供质量报告的生成、查询、导出等功能
 */
public interface QualityReportService {

    /**
     * 获取质检方案报告（支持时间范围筛选）
     *
     * @param planId 方案ID（可选，为空则返回全量报告）
     * @param startDate 开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式 yyyy-MM-dd）
     * @param layerCode 数据层筛选（可选 ODS/DWD/DWS/ADS）
     * @return 质量报告VO
     */
    Result<QualityReportVO> getReport(Long planId, String startDate, String endDate, String layerCode);

    /**
     * 获取整体质量报告（全部执行数据聚合）
     *
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 质量报告VO
     */
    Result<QualityReportVO> getOverallReport(String startDate, String endDate);

    /**
     * 获取指定执行记录的单次报告
     *
     * @param executionId 执行记录ID
     * @return 质量报告VO
     */
    Result<QualityReportVO> getExecutionReport(Long executionId);

    /**
     * 导出质量报告（多Sheet Excel）
     *
     * @param planId 方案ID（可选，为空则导出全部）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return Excel文件字节数组
     */
    Result<byte[]> exportReport(Long planId, String startDate, String endDate);

    /**
     * 导出指定执行记录的详细报告（多Sheet Excel）
     *
     * @param executionId 执行记录ID
     * @return Excel文件字节数组
     */
    Result<byte[]> exportExecutionReport(Long executionId);

    /**
     * 获取报告执行概览数据（用于Excel Sheet1）
     *
     * @param planId 方案ID（可选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 执行概览列表
     */
    List<QualityReportExecutionSheetVO> getExecutionSheetData(Long planId, String startDate, String endDate);

    /**
     * 获取报告规则明细数据（用于Excel Sheet2）
     *
     * @param planId 方案ID（可选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 规则明细列表
     */
    List<QualityReportRuleDetailSheetVO> getRuleDetailSheetData(Long planId, String startDate, String endDate);

    /**
     * 获取指定执行ID的规则明细数据（用于Excel Sheet2）
     *
     * @param executionId 执行ID
     * @return 规则明细列表
     */
    List<QualityReportRuleDetailSheetVO> getRuleDetailSheetDataByExecution(Long executionId);
}
