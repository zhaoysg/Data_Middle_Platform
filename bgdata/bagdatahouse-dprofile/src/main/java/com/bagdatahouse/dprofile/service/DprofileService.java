package com.bagdatahouse.dprofile.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DprofileColumnStats;
import com.bagdatahouse.core.entity.DprofileProfileTask;
import com.bagdatahouse.core.entity.DprofileSnapshot;
import com.bagdatahouse.core.entity.DprofileTableStats;
import com.bagdatahouse.dprofile.dto.SnapshotCompareRequestDTO;
import com.bagdatahouse.dprofile.dto.TableProfileRequestDTO;
import com.bagdatahouse.dprofile.vo.ColumnProfileResultVO;
import com.bagdatahouse.dprofile.vo.ProfileExecutionRecordVO;
import com.bagdatahouse.dprofile.vo.SnapshotCompareResultVO;
import com.bagdatahouse.dprofile.vo.TableProfileResultVO;

import java.util.List;
import java.util.Map;

/**
 * 数据探查服务接口
 */
public interface DprofileService {

    // ========== 探查任务 ==========

    /**
     * 创建探查任务
     */
    Result<Long> createTask(DprofileProfileTask task);

    /**
     * 更新探查任务
     */
    Result<Void> updateTask(Long id, DprofileProfileTask task);

    /**
     * 删除探查任务
     */
    Result<Void> deleteTask(Long id);

    /**
     * 分页查询任务
     */
    Result<Page<DprofileProfileTask>> pageTasks(Integer pageNum, Integer pageSize,
                                                  String taskName, String triggerType,
                                                  String profileLevel, Long targetDsId,
                                                  String status);

    /**
     * 根据ID查询任务
     */
    Result<DprofileProfileTask> getTaskById(Long id);

    /**
     * 立即执行探查
     */
    Result<Long> executeTask(Long id);

    /**
     * 取消执行中的任务
     */
    Result<Void> cancelTask(Long id);

    /**
     * 启用/禁用任务
     */
    Result<Void> toggleTask(Long id, Boolean enabled);

    /**
     * 获取任务统计
     */
    Result<TaskStats> getTaskStats();

    // ========== 探查统计 ==========

    /**
     * 查询表级统计记录
     */
    Result<List<DprofileTableStats>> listTableStats(Long dsId, String tableName, Long executionId, int limit);

    /**
     * 查询列级统计记录
     */
    Result<List<DprofileColumnStats>> listColumnStats(Long tableStatsId);

    /**
     * 获取最新探查结果（表+列）
     */
    Result<ProfileResult> getLatestProfile(Long metadataId);

    /**
     * 手动探查指定表
     */
    Result<Void> profileTable(Long dsId, String tableName, String columns, boolean collectColumnStats);

    // ========== 表级探查（新）==========

    /**
     * 执行表级探查（核心方法）
     *
     * @param request 探查请求参数
     * @return 表级探查结果 VO
     */
    Result<TableProfileResultVO> profileTableAdvanced(TableProfileRequestDTO request);

    /**
     * 获取探查执行记录
     */
    Result<ProfileExecutionRecordVO> getExecutionRecord(Long executionId);

    /**
     * 查询表历史探查记录
     */
    Result<List<DprofileTableStats>> getTableProfileHistory(Long dsId, String tableName, int limit);

    /**
     * 查询表最新探查结果（不执行新探查）
     */
    Result<TableProfileResultVO> getLastProfile(Long dsId, String tableName);

    /**
     * 获取活跃的执行任务列表
     */
    Result<List<ProfileExecutionRecordVO>> getActiveExecutions();

    // ========== 列级探查 ==========

    /**
     * 执行列级探查并获取完整结果（包含分布可视化和异常检测数据）
     *
     * @param tableStatsId 表统计记录ID
     * @return 列级探查结果列表
     */
    Result<List<ColumnProfileResultVO>> profileColumns(Long tableStatsId);

    /**
     * 获取单列的分布可视化数据
     *
     * @param dsId        数据源ID
     * @param tableName   表名
     * @param columnName  列名
     * @param dataType    数据类型
     * @param topN        Top-N 值数量（默认10）
     * @return 分布数据（频率分布、直方图、异常检测）
     */
    Result<Map<String, Object>> getColumnDistribution(Long dsId, String tableName,
                                                       String columnName, String dataType, int topN);

    /**
     * 批量获取列的探查结果（带异常警告）
     *
     * @param tableStatsId 表统计记录ID
     * @return 带警告的列级探查结果
     */
    Result<List<ColumnProfileResultVO>> listColumnProfilesWithWarnings(Long tableStatsId);

    // ========== 快照管理 ==========

    /**
     * 创建快照
     */
    Result<Long> createSnapshot(DprofileSnapshot snapshot);

    /**
     * 查询快照列表
     */
    Result<List<DprofileSnapshot>> listSnapshots(Long targetDsId, String targetTable);

    /**
     * 删除快照
     */
    Result<Void> deleteSnapshot(Long id);

    /**
     * 获取快照详情（包含表统计和列统计）
     */
    Result<SnapshotDetail> getSnapshotDetail(Long snapshotId);

    // ========== 快照比对 ==========

    /**
     * 执行快照比对
     *
     * @param request 比对请求参数
     * @return 比对结果
     */
    Result<SnapshotCompareResultVO> compareSnapshots(SnapshotCompareRequestDTO request);

    /**
     * 查询比对记录列表
     */
    Result<Page<com.bagdatahouse.core.entity.DprofileCompareResult>> pageCompareResults(
            Integer pageNum, Integer pageSize, Long snapshotAId, Long snapshotBId, String compareType);

    /**
     * 根据ID查询比对结果详情
     */
    Result<SnapshotCompareResultVO> getCompareResultById(Long compareId);

    // ========== 快照详情（聚合结果）==========
    record SnapshotDetail(
            DprofileSnapshot snapshot,
            DprofileTableStats tableStats,
            List<DprofileColumnStats> columnStats
    ) {}

    // ========== 任务统计 ==========
    record TaskStats(
            long totalTasks,
            long runningTasks,
            long enabledTasks,
            long totalExecutions
    ) {}

    // ========== 探查结果 ==========
    record ProfileResult(
            DprofileTableStats tableStats,
            List<DprofileColumnStats> columnStats
    ) {}
}
