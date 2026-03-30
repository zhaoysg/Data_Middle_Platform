package com.bagdatahouse.governance.lineage.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovLineage;
import com.bagdatahouse.governance.lineage.dto.LineageQueryDTO;
import com.bagdatahouse.governance.lineage.dto.LineageSaveDTO;
import com.bagdatahouse.governance.lineage.vo.LineageGraphVO;
import com.bagdatahouse.governance.lineage.vo.LineageNodeVO;
import com.bagdatahouse.governance.lineage.vo.LineageStatsVO;
import com.bagdatahouse.governance.lineage.vo.LineageVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 数据血缘管理服务接口
 */
public interface LineageService {

    /**
     * 新增血缘记录
     */
    Result<GovLineage> save(LineageSaveDTO dto);

    /**
     * 批量新增血缘记录
     */
    Result<Void> batchSave(List<LineageSaveDTO> dtoList);

    /**
     * 更新血缘记录
     */
    Result<Void> update(Long id, LineageSaveDTO dto);

    /**
     * 删除血缘记录
     */
    Result<Void> delete(Long id);

    /**
     * 批量删除血缘记录
     */
    Result<Void> batchDelete(List<Long> ids);

    /**
     * 根据ID查询血缘记录
     */
    Result<GovLineage> getById(Long id);

    /**
     * 分页查询血缘记录
     */
    Result<Page<GovLineage>> page(Integer pageNum, Integer pageSize, LineageQueryDTO queryDTO);

    /**
     * 查询血缘详情（包含关联的数据源名称等）
     */
    Result<LineageVO> getDetail(Long id);

    /**
     * 根据源表和目标表查询血缘
     */
    Result<List<GovLineage>> getBySourceAndTarget(Long sourceDsId, String sourceTable,
                                                   Long targetDsId, String targetTable);

    /**
     * 获取血缘图谱数据（节点+边）
     */
    Result<LineageGraphVO> getGraph(LineageQueryDTO queryDTO);

    /**
     * 获取下游血缘（DAG向下追溯）
     */
    Result<List<LineageNodeVO>> getDownstream(Long dsId, String tableName, Integer maxDepth);

    /**
     * 获取上游血缘（DAG向上回溯）
     */
    Result<List<LineageNodeVO>> getUpstream(Long dsId, String tableName, Integer maxDepth);

    /**
     * 获取血缘统计信息
     */
    Result<LineageStatsVO> getStats();
}
