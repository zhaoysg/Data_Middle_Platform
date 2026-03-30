package com.bagdatahouse.governance.lineage.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.governance.lineage.dto.LineageBatchImportDTO;
import com.bagdatahouse.governance.lineage.vo.BatchImportResultVO;
import com.bagdatahouse.governance.lineage.vo.ImpactAnalysisResultVO;
import com.bagdatahouse.governance.lineage.vo.TableColumnSuggestVO;

/**
 * 手动录入血缘增强服务接口
 * 提供影响分析、表/字段建议、批量导入等增强功能
 */
public interface LineageManualService {

    /**
     * 影响分析：从指定表/字段出发追踪下游影响
     */
    Result<ImpactAnalysisResultVO> analyzeDownstream(Long dsId, String tableName,
                                                     String column, Integer maxDepth);

    /**
     * 回溯分析：从指定表/字段出发追溯上游来源
     */
    Result<ImpactAnalysisResultVO> analyzeUpstream(Long dsId, String tableName,
                                                   String column, Integer maxDepth);

    /**
     * 从节点详情面板分析（自动判断方向）
     */
    Result<ImpactAnalysisResultVO> analyzeFromNode(String direction, Long dsId,
                                                   String tableName, String column,
                                                   Integer maxDepth);

    /**
     * 搜索表/字段建议（关联元数据，自动补全）
     */
    Result<TableColumnSuggestVO> suggestTablesAndColumns(Long dsId, String keyword);

    /**
     * 获取指定数据源的表名列表（用于下拉选择）
     */
    Result<java.util.List<TableColumnSuggestVO.TableSuggest>> listTables(Long dsId);

    /**
     * 获取指定表的字段列表（用于下拉选择）
     */
    Result<java.util.List<TableColumnSuggestVO.ColumnSuggest>> listColumns(Long dsId, String tableName);

    /**
     * 批量导入血缘（支持 Excel 解析后的结构化数据）
     */
    Result<BatchImportResultVO> batchImport(LineageBatchImportDTO dto);

    /**
     * 下载血缘导入模板（Excel）
     */
    Result<String> getImportTemplate();

    /**
     * 阶段三-T2：敏感等级传播校验
     * <p>
     * 字段级血缘中，目标字段的敏感等级应 >= 源字段敏感等级。
     * 若目标等级 < 源等级，发出升级建议告警。
     *
     * @param sourceDsId    源数据源ID
     * @param sourceTable   源表名
     * @param sourceColumn  源字段名
     * @param targetDsId    目标数据源ID
     * @param targetTable   目标表名
     * @param targetColumn  目标字段名
     * @return 升级建议（当目标等级 < 源等级时返回警告）
     */
    Result<String> checkSensitivityPropagation(Long sourceDsId, String sourceTable, String sourceColumn,
                                              Long targetDsId, String targetTable, String targetColumn);
}
