package com.bagdatahouse.dprofile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DprofileColumnStats;
import com.bagdatahouse.core.entity.DprofileCompareResult;
import com.bagdatahouse.core.entity.DprofileProfileTask;
import com.bagdatahouse.core.entity.DprofileSnapshot;
import com.bagdatahouse.core.entity.DprofileTableStats;
import com.bagdatahouse.core.entity.GovMetadata;
import com.bagdatahouse.core.entity.GovMetadataColumn;
import com.bagdatahouse.core.mapper.DprofileColumnStatsMapper;
import com.bagdatahouse.core.mapper.DprofileCompareResultMapper;
import com.bagdatahouse.core.mapper.DprofileProfileTaskMapper;
import com.bagdatahouse.core.mapper.DprofileSnapshotMapper;
import com.bagdatahouse.core.mapper.DprofileTableStatsMapper;
import com.bagdatahouse.core.mapper.GovMetadataColumnMapper;
import com.bagdatahouse.core.mapper.GovMetadataMapper;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import com.bagdatahouse.dprofile.analyzer.ColumnAnalyzer;
import com.bagdatahouse.dprofile.analyzer.DistributionAnalyzer;
import com.bagdatahouse.dprofile.analyzer.TableAnalyzer;
import com.bagdatahouse.dprofile.dto.SnapshotCompareRequestDTO;
import com.bagdatahouse.dprofile.dto.TableProfileRequestDTO;
import com.bagdatahouse.dprofile.job.ProfileJob;
import com.bagdatahouse.dprofile.service.DprofileService;
import com.bagdatahouse.dprofile.vo.ColumnCompareVO;
import com.bagdatahouse.dprofile.vo.ColumnProfileResultVO;
import com.bagdatahouse.dprofile.vo.ProfileExecutionRecordVO;
import com.bagdatahouse.dprofile.vo.SnapshotCompareResultVO;
import com.bagdatahouse.dprofile.vo.TableProfileResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 数据探查服务实现
 */
@Slf4j
@Service
public class DprofileServiceImpl extends ServiceImpl<DprofileProfileTaskMapper, DprofileProfileTask>
        implements DprofileService {

    @Autowired
    private DprofileProfileTaskMapper taskMapper;

    @Autowired
    private DprofileTableStatsMapper tableStatsMapper;

    @Autowired
    private DprofileColumnStatsMapper columnStatsMapper;

    @Autowired
    private DprofileSnapshotMapper snapshotMapper;

    @Autowired
    private DprofileCompareResultMapper compareResultMapper;

    @Autowired
    private GovMetadataMapper metadataMapper;

    @Autowired
    private GovMetadataColumnMapper metadataColumnMapper;

    @Autowired
    private DataSourceAdapterRegistry adapterRegistry;

    @Autowired
    private TableAnalyzer tableAnalyzer;

    @Autowired
    private ColumnAnalyzer columnAnalyzer;

    @Autowired
    private DistributionAnalyzer distributionAnalyzer;

    @Autowired
    private ProfileJob profileJob;

    // ========== 任务管理 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createTask(DprofileProfileTask task) {
        if (!StringUtils.hasText(task.getTaskName())) {
            throw new BusinessException(400, "任务名称不能为空");
        }
        if (task.getTargetDsId() == null) {
            throw new BusinessException(400, "目标数据源不能为空");
        }
        if (!StringUtils.hasText(task.getTargetTable())) {
            throw new BusinessException(400, "目标表名不能为空");
        }
        if (!StringUtils.hasText(task.getTaskCode())) {
            task.setTaskCode("TASK_" + System.currentTimeMillis());
        }
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        if (!StringUtils.hasText(task.getStatus())) {
            task.setStatus("DRAFT");
        }
        taskMapper.insert(task);
        return Result.success(task.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateTask(Long id, DprofileProfileTask task) {
        DprofileProfileTask existing = taskMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "探查任务不存在");
        }
        task.setId(id);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteTask(Long id) {
        DprofileProfileTask existing = taskMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "探查任务不存在");
        }
        taskMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<Page<DprofileProfileTask>> pageTasks(Integer pageNum, Integer pageSize,
                                                         String taskName, String triggerType,
                                                         String profileLevel, Long targetDsId,
                                                         String status) {
        Page<DprofileProfileTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DprofileProfileTask> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(taskName)) {
            wrapper.like(DprofileProfileTask::getTaskName, taskName);
        }
        if (StringUtils.hasText(triggerType)) {
            wrapper.eq(DprofileProfileTask::getTriggerType, triggerType);
        }
        if (StringUtils.hasText(profileLevel)) {
            wrapper.eq(DprofileProfileTask::getProfileLevel, profileLevel);
        }
        if (targetDsId != null) {
            wrapper.eq(DprofileProfileTask::getTargetDsId, targetDsId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(DprofileProfileTask::getStatus, status);
        }

        wrapper.orderByDesc(DprofileProfileTask::getCreateTime);
        Page<DprofileProfileTask> result = this.page(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<DprofileProfileTask> getTaskById(Long id) {
        DprofileProfileTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "探查任务不存在");
        }
        return Result.success(task);
    }

    @Override
    public Result<Long> executeTask(Long id) {
        DprofileProfileTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "探查任务不存在");
        }

        // 先生成并写入 executionId，确保 TableAnalyzer 与前端追踪同一个执行批次
        Long executionId = System.currentTimeMillis();
        task.setLastExecutionId(executionId);
        task.setLastExecutionTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 使用 ProfileJob 异步执行（ProfileJob 会读取 task.lastExecutionId）
        profileJob.executeAsync(id);
        return Result.success(executionId);
    }

    @Override
    public Result<Void> cancelTask(Long id) {
        DprofileProfileTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "探查任务不存在");
        }
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> toggleTask(Long id, Boolean enabled) {
        DprofileProfileTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "探查任务不存在");
        }
        task.setStatus(enabled ? "PUBLISHED" : "DISABLED");
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        return Result.success();
    }

    @Override
    public Result<TaskStats> getTaskStats() {
        long totalTasks = this.count();
        LambdaQueryWrapper<DprofileProfileTask> enabledWrapper = new LambdaQueryWrapper<>();
        enabledWrapper.eq(DprofileProfileTask::getStatus, "PUBLISHED");
        long enabledTasks = this.count(enabledWrapper);

        LambdaQueryWrapper<DprofileProfileTask> runningWrapper = new LambdaQueryWrapper<>();
        runningWrapper.eq(DprofileProfileTask::getStatus, "RUNNING");
        long runningTasks = this.count(runningWrapper);

        LambdaQueryWrapper<DprofileTableStats> execWrapper = new LambdaQueryWrapper<>();
        execWrapper.select(DprofileTableStats::getExecutionId);
        long totalExecutions = tableStatsMapper.selectCount(execWrapper);

        return Result.success(new TaskStats(totalTasks, runningTasks, enabledTasks, totalExecutions));
    }

    // ========== 探查统计 ==========

    @Override
    public Result<List<DprofileTableStats>> listTableStats(Long dsId, String tableName, Long executionId, int limit) {
        LambdaQueryWrapper<DprofileTableStats> wrapper = new LambdaQueryWrapper<>();
        if (dsId != null) {
            wrapper.eq(DprofileTableStats::getDsId, dsId);
        }
        if (StringUtils.hasText(tableName)) {
            wrapper.eq(DprofileTableStats::getTableName, tableName);
        }
        if (executionId != null) {
            wrapper.eq(DprofileTableStats::getExecutionId, executionId);
        }
        wrapper.orderByDesc(DprofileTableStats::getProfileTime);
        wrapper.last("LIMIT " + limit);
        List<DprofileTableStats> stats = tableStatsMapper.selectList(wrapper);
        return Result.success(stats);
    }

    @Override
    public Result<List<DprofileColumnStats>> listColumnStats(Long tableStatsId) {
        LambdaQueryWrapper<DprofileColumnStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DprofileColumnStats::getTableStatsId, tableStatsId)
                .orderByAsc(DprofileColumnStats::getColumnName);
        List<DprofileColumnStats> stats = columnStatsMapper.selectList(wrapper);
        return Result.success(stats);
    }

    @Override
    public Result<ProfileResult> getLatestProfile(Long metadataId) {
        GovMetadata metadata = metadataMapper.selectById(metadataId);
        if (metadata == null) {
            throw new BusinessException(404, "元数据不存在");
        }

        // 查询该表最新的一条表级统计
        LambdaQueryWrapper<DprofileTableStats> tableWrapper = new LambdaQueryWrapper<>();
        tableWrapper.eq(DprofileTableStats::getDsId, metadata.getDsId())
                .eq(DprofileTableStats::getTableName, metadata.getTableName())
                .orderByDesc(DprofileTableStats::getProfileTime)
                .last("LIMIT 1");
        DprofileTableStats tableStats = tableStatsMapper.selectOne(tableWrapper);

        List<DprofileColumnStats> columnStats = null;
        if (tableStats != null) {
            LambdaQueryWrapper<DprofileColumnStats> colWrapper = new LambdaQueryWrapper<>();
            colWrapper.eq(DprofileColumnStats::getTableStatsId, tableStats.getId())
                    .orderByAsc(DprofileColumnStats::getColumnName);
            columnStats = columnStatsMapper.selectList(colWrapper);
        }

        return Result.success(new ProfileResult(tableStats, columnStats));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> profileTable(Long dsId, String tableName, String columns, boolean collectColumnStats) {
        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            throw new BusinessException(400, "数据源适配器未找到");
        }

        long executionId = System.currentTimeMillis();
        LocalDateTime profileTime = LocalDateTime.now();

        // 表级统计
        Long rowCount = adapter.getRowCount(tableName);
        Integer columnCount = adapter.getColumnCount(tableName);
        BigDecimal storageBytes = adapter.getStorageBytes(tableName);

        DprofileTableStats tableStats = DprofileTableStats.builder()
                .executionId(executionId)
                .dsId(dsId)
                .tableName(tableName)
                .profileTime(profileTime)
                .rowCount(rowCount)
                .columnCount(columnCount)
                .storageBytes(storageBytes)
                .createTime(LocalDateTime.now())
                .build();
        tableStatsMapper.insert(tableStats);

        // 列级统计（如果需要）
        if (collectColumnStats) {
            List<GovMetadataColumn> metadataColumns = null;
            if (columns != null && !columns.isEmpty()) {
                // 按 metadataId 找表，再过滤字段名
                LambdaQueryWrapper<GovMetadataColumn> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(GovMetadataColumn::getMetadataId, null); // 占位，按字段名查
                for (String col : columns.split(",")) {
                    wrapper.or().like(GovMetadataColumn::getColumnName, col.trim());
                }
                metadataColumns = metadataColumnMapper.selectList(wrapper);
            }
            collectColumnStatsData(adapter, tableStats, tableName, profileTime);
        }

        return Result.success();
    }

    // ========== 表级探查（新）==========

    @Override
    public Result<TableProfileResultVO> profileTableAdvanced(TableProfileRequestDTO request) {
        if (request.getDsId() == null) {
            throw new BusinessException(400, "数据源ID不能为空");
        }
        if (!StringUtils.hasText(request.getTableName())) {
            throw new BusinessException(400, "目标表名不能为空");
        }

        // 解析列名列表
        List<String> columnNames = null;
        if (StringUtils.hasText(request.getColumns())) {
            columnNames = Arrays.asList(request.getColumns().split(","));
        }

        // 执行表级探查（PostgreSQL 使用 schema 参数）
        TableProfileResultVO tableResult = tableAnalyzer.analyze(
                request.getTaskId(),
                request.getTaskName(),
                request.getDsId(),
                request.getTableName(),
                request.getSchema(),
                null
        );

        // 是否执行列级探查
        boolean doColumnProfile = Boolean.TRUE.equals(request.getCollectColumnStats());
        if (doColumnProfile && tableResult.getTableStatsId() != null) {
            columnAnalyzer.analyzeColumns(
                    request.getTaskId(),
                    request.getDsId(),
                    request.getTableName(),
                    request.getSchema(),
                    tableResult.getExecutionId(),
                    tableResult.getTableStatsId(),
                    columnNames,
                    tableResult.getProfileTime()
            );
        }

        return Result.success(tableResult);
    }

    @Override
    public Result<ProfileExecutionRecordVO> getExecutionRecord(Long executionId) {
        ProfileExecutionRecordVO record = tableAnalyzer.getExecutionRecord(executionId);
        if (record == null) {
            throw new BusinessException(404, "执行记录不存在");
        }
        return Result.success(record);
    }

    @Override
    public Result<List<DprofileTableStats>> getTableProfileHistory(Long dsId, String tableName, int limit) {
        List<DprofileTableStats> history = tableAnalyzer.listHistory(dsId, tableName, limit);
        return Result.success(history);
    }

    @Override
    public Result<TableProfileResultVO> getLastProfile(Long dsId, String tableName) {
        TableProfileResultVO lastProfile = tableAnalyzer.getLastProfile(dsId, tableName);
        if (lastProfile == null) {
            throw new BusinessException(404, "未找到探查记录");
        }
        return Result.success(lastProfile);
    }

    @Override
    public Result<List<ProfileExecutionRecordVO>> getActiveExecutions() {
        List<ProfileExecutionRecordVO> activeExecutions = tableAnalyzer.getActiveExecutions();
        return Result.success(activeExecutions);
    }

    private void collectColumnStatsData(DataSourceAdapter adapter, DprofileTableStats tableStats,
                                        String tableName, LocalDateTime profileTime) {
        try {
            Map<String, Object> stats = adapter.getColumnStats(tableName, null);
            if (stats == null || stats.isEmpty()) {
                return;
            }

            AtomicInteger sortOrder = new AtomicInteger(0);
            stats.forEach((colName, values) -> {
                if (values instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> colStats = (Map<String, Object>) values;
                    DprofileColumnStats col = DprofileColumnStats.builder()
                            .tableStatsId(tableStats.getId())
                            .executionId(tableStats.getExecutionId())
                            .dsId(tableStats.getDsId())
                            .tableName(tableName)
                            .columnName(colName)
                            .profileTime(profileTime)
                            .dataType(String.valueOf(colStats.getOrDefault("dataType", "UNKNOWN")))
                            .totalCount(toLong(colStats.get("totalCount")))
                            .nullCount(toLong(colStats.get("nullCount")))
                            .uniqueCount(toLong(colStats.get("uniqueCount")))
                            .minValue(String.valueOf(colStats.getOrDefault("minValue", "")))
                            .maxValue(String.valueOf(colStats.getOrDefault("maxValue", "")))
                            .avgValue(toDecimal(colStats.get("avgValue")))
                            .minLength(toInt(colStats.get("minLength")))
                            .maxLength(toInt(colStats.get("maxLength")))
                            .avgLength(toDecimal(colStats.get("avgLength")))
                            .createTime(LocalDateTime.now())
                            .build();

                    // 计算空值率
                    if (col.getTotalCount() != null && col.getTotalCount() > 0
                            && col.getNullCount() != null) {
                        BigDecimal nullRate = BigDecimal.valueOf(col.getNullCount())
                                .divide(BigDecimal.valueOf(col.getTotalCount()), 4, java.math.RoundingMode.HALF_UP);
                        col.setNullRate(nullRate);
                    }

                    // 计算唯一率
                    if (col.getTotalCount() != null && col.getTotalCount() > 0
                            && col.getUniqueCount() != null) {
                        BigDecimal uniqueRate = BigDecimal.valueOf(col.getUniqueCount())
                                .divide(BigDecimal.valueOf(col.getTotalCount()), 4, java.math.RoundingMode.HALF_UP);
                        col.setUniqueRate(uniqueRate);
                    }

                    columnStatsMapper.insert(col);
                    sortOrder.incrementAndGet();
                }
            });
        } catch (Exception e) {
            log.warn("采集列级统计失败: {}.{}, 错误: {}", tableStats.getDsId(), tableName, e.getMessage());
        }
    }

    // ========== 快照管理 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createSnapshot(DprofileSnapshot snapshot) {
        if (!StringUtils.hasText(snapshot.getSnapshotName())) {
            throw new BusinessException(400, "快照名称不能为空");
        }
        if (!StringUtils.hasText(snapshot.getSnapshotCode())) {
            snapshot.setSnapshotCode("SNAP_" + System.currentTimeMillis());
        }
        snapshot.setCreateTime(LocalDateTime.now());

        if (snapshot.getTargetDsId() != null && StringUtils.hasText(snapshot.getTargetTable())) {
            LambdaQueryWrapper<DprofileTableStats> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DprofileTableStats::getDsId, snapshot.getTargetDsId())
                    .eq(DprofileTableStats::getTableName, snapshot.getTargetTable())
                    .orderByDesc(DprofileTableStats::getProfileTime)
                    .last("LIMIT 1");
            DprofileTableStats latestStats = tableStatsMapper.selectOne(wrapper);
            if (latestStats != null) {
                snapshot.setTableStatsId(latestStats.getId());
                snapshot.setColumnCount(latestStats.getColumnCount());
            }
        }

        snapshotMapper.insert(snapshot);
        return Result.success(snapshot.getId());
    }

    @Override
    public Result<List<DprofileSnapshot>> listSnapshots(Long targetDsId, String targetTable) {
        LambdaQueryWrapper<DprofileSnapshot> wrapper = new LambdaQueryWrapper<>();
        if (targetDsId != null) {
            wrapper.eq(DprofileSnapshot::getTargetDsId, targetDsId);
        }
        if (StringUtils.hasText(targetTable)) {
            wrapper.eq(DprofileSnapshot::getTargetTable, targetTable);
        }
        wrapper.orderByDesc(DprofileSnapshot::getCreateTime);
        List<DprofileSnapshot> snapshots = snapshotMapper.selectList(wrapper);
        return Result.success(snapshots);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteSnapshot(Long id) {
        DprofileSnapshot existing = snapshotMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "快照不存在");
        }
        snapshotMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<SnapshotDetail> getSnapshotDetail(Long snapshotId) {
        DprofileSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            throw new BusinessException(404, "快照不存在");
        }

        DprofileTableStats tableStats = null;
        List<DprofileColumnStats> columnStats = null;

        if (snapshot.getTableStatsId() != null) {
            tableStats = tableStatsMapper.selectById(snapshot.getTableStatsId());

            LambdaQueryWrapper<DprofileColumnStats> colWrapper = new LambdaQueryWrapper<>();
            colWrapper.eq(DprofileColumnStats::getTableStatsId, snapshot.getTableStatsId())
                    .orderByAsc(DprofileColumnStats::getColumnName);
            columnStats = columnStatsMapper.selectList(colWrapper);
        } else {
            LambdaQueryWrapper<DprofileTableStats> tableWrapper = new LambdaQueryWrapper<>();
            tableWrapper.eq(DprofileTableStats::getDsId, snapshot.getTargetDsId())
                    .eq(DprofileTableStats::getTableName, snapshot.getTargetTable())
                    .orderByDesc(DprofileTableStats::getProfileTime)
                    .last("LIMIT 1");
            tableStats = tableStatsMapper.selectOne(tableWrapper);

            if (tableStats != null) {
                LambdaQueryWrapper<DprofileColumnStats> colWrapper = new LambdaQueryWrapper<>();
                colWrapper.eq(DprofileColumnStats::getTableStatsId, tableStats.getId())
                        .orderByAsc(DprofileColumnStats::getColumnName);
                columnStats = columnStatsMapper.selectList(colWrapper);
            }
        }

        return Result.success(new SnapshotDetail(snapshot, tableStats, columnStats));
    }

    // ========== 快照比对 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SnapshotCompareResultVO> compareSnapshots(SnapshotCompareRequestDTO request) {
        if (request.getSnapshotAId() == null || request.getSnapshotBId() == null) {
            throw new BusinessException(400, "快照A和快照B的ID不能为空");
        }
        if (request.getSnapshotAId().equals(request.getSnapshotBId())) {
            throw new BusinessException(400, "不能将快照与自身比对");
        }

        long startMs = System.currentTimeMillis();
        LocalDateTime compareTime = LocalDateTime.now();

        DprofileSnapshot snapA = snapshotMapper.selectById(request.getSnapshotAId());
        DprofileSnapshot snapB = snapshotMapper.selectById(request.getSnapshotBId());
        if (snapA == null || snapB == null) {
            throw new BusinessException(404, "快照不存在");
        }

        DprofileTableStats tableStatsA = fetchTableStats(snapA);
        DprofileTableStats tableStatsB = fetchTableStats(snapB);

        List<DprofileColumnStats> colStatsA = tableStatsA != null
                ? fetchColumnStats(tableStatsA.getId()) : List.of();
        List<DprofileColumnStats> colStatsB = tableStatsB != null
                ? fetchColumnStats(tableStatsB.getId()) : List.of();

        String compareType = request.getCompareType() != null ? request.getCompareType() : "COLUMN";
        boolean doColumnCompare = "COLUMN".equals(compareType) || "FULL".equals(compareType);

        List<ColumnCompareVO> columnCompares = new ArrayList<>();
        List<ColumnCompareVO> newColumns = new ArrayList<>();
        List<ColumnCompareVO> removedColumns = new ArrayList<>();
        List<String> significantChanges = new ArrayList<>();
        int tableDiffCount = 0;
        int columnDiffCount = 0;

        Map<String, DprofileColumnStats> colMapB = colStatsB.stream()
                .collect(Collectors.toMap(DprofileColumnStats::getColumnName, c -> c, (a, b) -> b));

        Map<String, String> colTypeMap = new HashMap<>();
        colStatsB.forEach(c -> colTypeMap.put(c.getColumnName(), c.getDataType()));
        colStatsA.forEach(c -> {
            if (!colTypeMap.containsKey(c.getColumnName())) {
                colTypeMap.put(c.getColumnName(), c.getDataType());
            }
        });

        for (DprofileColumnStats colA : colStatsA) {
            DprofileColumnStats colB = colMapB.get(colA.getColumnName());
            if (colB == null) {
                removedColumns.add(buildRemovedColumn(colA));
                significantChanges.add("列 [" + colA.getColumnName() + "] 被移除");
                columnDiffCount++;
            } else if (doColumnCompare) {
                ColumnCompareVO compare = compareColumn(colA, colB, request);
                columnCompares.add(compare);
                if ("SIGNIFICANT".equals(compare.getDiffLevel())) {
                    significantChanges.add(buildSignificantChangeDesc(compare));
                    columnDiffCount++;
                }
            }
        }

        for (DprofileColumnStats colB : colStatsB) {
            boolean existsInA = colStatsA.stream()
                    .anyMatch(c -> c.getColumnName().equals(colB.getColumnName()));
            if (!existsInA) {
                newColumns.add(buildNewColumn(colB));
                significantChanges.add("新增列 [" + colB.getColumnName() + "]");
                columnDiffCount++;
            }
        }

        SnapshotCompareResultVO.TableCompare tableCompare = buildTableCompare(
                tableStatsA, tableStatsB, request);
        if (Boolean.TRUE.equals(tableCompare.getRowCountSignificant())) {
            tableDiffCount++;
            significantChanges.add("行数从 " + tableCompare.getSnapshotARowCount() + " 变为 "
                    + tableCompare.getSnapshotBRowCount()
                    + " (变化率 " + tableCompare.getRowCountChangeRate() + "%)");
        }
        if (Boolean.TRUE.equals(tableCompare.getColumnCountSignificant())) {
            tableDiffCount++;
            significantChanges.add("列数从 " + tableCompare.getSnapshotAColumnCount() + " 变为 "
                    + tableCompare.getSnapshotBColumnCount());
        }
        if (Boolean.TRUE.equals(tableCompare.getStorageBytesSignificant())) {
            tableDiffCount++;
        }
        if (Boolean.TRUE.equals(tableCompare.getUpdateTimeChanged())) {
            significantChanges.add("数据最后更新时间发生变化");
        }

        int totalDiff = tableDiffCount + columnDiffCount;
        String diffLevel = determineDiffLevel(totalDiff, tableDiffCount, columnDiffCount);

        Integer daysBetween = null;
        if (tableStatsA != null && tableStatsA.getProfileTime() != null
                && tableStatsB != null && tableStatsB.getProfileTime() != null) {
            daysBetween = (int) Duration.between(
                    tableStatsA.getProfileTime(), tableStatsB.getProfileTime()).toDays();
        }

        String compareName = request.getCompareName();
        if (!StringUtils.hasText(compareName)) {
            compareName = "比对_" + snapA.getSnapshotName() + "_vs_" + snapB.getSnapshotName();
        }
        String compareCode = "CMP_" + System.currentTimeMillis();

        DprofileCompareResult compareRecord = DprofileCompareResult.builder()
                .compareName(compareName)
                .compareCode(compareCode)
                .snapshotAId(request.getSnapshotAId())
                .snapshotBId(request.getSnapshotBId())
                .compareType(compareType)
                .diffCount(totalDiff)
                .createUser(null)
                .createTime(compareTime)
                .build();
        compareResultMapper.insert(compareRecord);

        long elapsedMs = System.currentTimeMillis() - startMs;

        return Result.success(SnapshotCompareResultVO.builder()
                .compareId(compareRecord.getId())
                .compareName(compareName)
                .compareType(compareType)
                .status("SUCCESS")
                .compareTime(compareTime)
                .elapsedMs(elapsedMs)
                .snapshotAId(snapA.getId())
                .snapshotAName(snapA.getSnapshotName())
                .snapshotATime(tableStatsA != null ? tableStatsA.getProfileTime() : null)
                .snapshotADsId(snapA.getTargetDsId())
                .snapshotATable(snapA.getTargetTable())
                .snapshotBId(snapB.getId())
                .snapshotBName(snapB.getSnapshotName())
                .snapshotBTime(tableStatsB != null ? tableStatsB.getProfileTime() : null)
                .snapshotBDsId(snapB.getTargetDsId())
                .snapshotBTable(snapB.getTargetTable())
                .daysBetween(daysBetween)
                .diffLevel(diffLevel)
                .diffCount(totalDiff)
                .tableDiffCount(tableDiffCount)
                .columnDiffCount(columnDiffCount)
                .significantChanges(significantChanges)
                .tableCompare(tableCompare)
                .columnCompares(columnCompares)
                .newColumns(newColumns)
                .removedColumns(removedColumns)
                .build());
    }

    @Override
    public Result<Page<DprofileCompareResult>> pageCompareResults(
            Integer pageNum, Integer pageSize,
            Long snapshotAId, Long snapshotBId, String compareType) {
        Page<DprofileCompareResult> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DprofileCompareResult> wrapper = new LambdaQueryWrapper<>();

        if (snapshotAId != null) {
            wrapper.eq(DprofileCompareResult::getSnapshotAId, snapshotAId);
        }
        if (snapshotBId != null) {
            wrapper.eq(DprofileCompareResult::getSnapshotBId, snapshotBId);
        }
        if (StringUtils.hasText(compareType)) {
            wrapper.eq(DprofileCompareResult::getCompareType, compareType);
        }

        wrapper.orderByDesc(DprofileCompareResult::getCreateTime);
        Page<DprofileCompareResult> result = compareResultMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<SnapshotCompareResultVO> getCompareResultById(Long compareId) {
        DprofileCompareResult record = compareResultMapper.selectById(compareId);
        if (record == null) {
            throw new BusinessException(404, "比对记录不存在");
        }

        DprofileSnapshot snapA = snapshotMapper.selectById(record.getSnapshotAId());
        DprofileSnapshot snapB = snapshotMapper.selectById(record.getSnapshotBId());
        DprofileTableStats tableStatsA = snapA != null ? fetchTableStats(snapA) : null;
        DprofileTableStats tableStatsB = snapB != null ? fetchTableStats(snapB) : null;

        SnapshotCompareResultVO.TableCompare tableCompare = buildTableCompare(tableStatsA, tableStatsB, null);

        Integer daysBetween = null;
        if (tableStatsA != null && tableStatsA.getProfileTime() != null
                && tableStatsB != null && tableStatsB.getProfileTime() != null) {
            daysBetween = (int) Duration.between(
                    tableStatsA.getProfileTime(), tableStatsB.getProfileTime()).toDays();
        }

        return Result.success(SnapshotCompareResultVO.builder()
                .compareId(record.getId())
                .compareName(record.getCompareName())
                .compareType(record.getCompareType())
                .status("SUCCESS")
                .compareTime(record.getCreateTime())
                .snapshotAId(snapA != null ? snapA.getId() : null)
                .snapshotAName(snapA != null ? snapA.getSnapshotName() : null)
                .snapshotATime(tableStatsA != null ? tableStatsA.getProfileTime() : null)
                .snapshotADsId(snapA != null ? snapA.getTargetDsId() : null)
                .snapshotATable(snapA != null ? snapA.getTargetTable() : null)
                .snapshotBId(snapB != null ? snapB.getId() : null)
                .snapshotBName(snapB != null ? snapB.getSnapshotName() : null)
                .snapshotBTime(tableStatsB != null ? tableStatsB.getProfileTime() : null)
                .snapshotBDsId(snapB != null ? snapB.getTargetDsId() : null)
                .snapshotBTable(snapB != null ? snapB.getTargetTable() : null)
                .daysBetween(daysBetween)
                .diffLevel(determineDiffLevel(record.getDiffCount(), 0, 0))
                .diffCount(record.getDiffCount())
                .tableDiffCount(0)
                .columnDiffCount(0)
                .significantChanges(List.of())
                .tableCompare(tableCompare)
                .columnCompares(List.of())
                .newColumns(List.of())
                .removedColumns(List.of())
                .build());
    }

    // ========== 比对辅助方法 ==========

    private DprofileTableStats fetchTableStats(DprofileSnapshot snapshot) {
        if (snapshot == null) return null;

        if (snapshot.getTableStatsId() != null) {
            return tableStatsMapper.selectById(snapshot.getTableStatsId());
        }

        LambdaQueryWrapper<DprofileTableStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DprofileTableStats::getDsId, snapshot.getTargetDsId())
                .eq(DprofileTableStats::getTableName, snapshot.getTargetTable())
                .orderByDesc(DprofileTableStats::getProfileTime)
                .last("LIMIT 1");
        return tableStatsMapper.selectOne(wrapper);
    }

    private List<DprofileColumnStats> fetchColumnStats(Long tableStatsId) {
        if (tableStatsId == null) return List.of();
        LambdaQueryWrapper<DprofileColumnStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DprofileColumnStats::getTableStatsId, tableStatsId)
                .orderByAsc(DprofileColumnStats::getColumnName);
        return columnStatsMapper.selectList(wrapper);
    }

    private SnapshotCompareResultVO.TableCompare buildTableCompare(
            DprofileTableStats statsA, DprofileTableStats statsB,
            SnapshotCompareRequestDTO request) {

        Long rowA = statsA != null ? statsA.getRowCount() : null;
        Long rowB = statsB != null ? statsB.getRowCount() : null;
        Integer colA = statsA != null ? statsA.getColumnCount() : null;
        Integer colB = statsB != null ? statsB.getColumnCount() : null;
        BigDecimal storageA = statsA != null ? statsA.getStorageBytes() : null;
        BigDecimal storageB = statsB != null ? statsB.getStorageBytes() : null;

        Long rowChange = (rowA != null && rowB != null) ? (rowB - rowA) : null;
        BigDecimal rowChangeRate = calcRate(rowA, rowB);
        boolean rowSignificant = isSignificantRate(rowChangeRate,
                request != null ? request.getRowCountChangeThreshold() : new BigDecimal("5"));

        Integer colChange = (colA != null && colB != null) ? (colB - colA) : null;
        BigDecimal colChangeRate = calcRate(
                colA != null ? BigDecimal.valueOf(colA) : null,
                colB != null ? BigDecimal.valueOf(colB) : null);
        boolean colSignificant = isSignificantRate(colChangeRate,
                request != null ? request.getColumnCountChangeThreshold() : BigDecimal.ZERO);

        BigDecimal storageChange = (storageA != null && storageB != null)
                ? storageB.subtract(storageA) : null;
        BigDecimal storageChangeRate = calcRate(storageA, storageB);
        boolean storageSignificant = isSignificantRate(storageChangeRate, new BigDecimal("10"));

        boolean updateTimeChanged = false;
        if (statsA != null && statsB != null
                && statsA.getUpdateTime() != null && statsB.getUpdateTime() != null) {
            updateTimeChanged = !statsA.getUpdateTime().equals(statsB.getUpdateTime());
        }

        Long incrementChange = null;
        boolean incrementChanged = false;
        if (statsA != null && statsB != null
                && statsA.getIncrementRows() != null && statsB.getIncrementRows() != null) {
            incrementChange = statsB.getIncrementRows() - statsA.getIncrementRows();
            incrementChanged = !incrementChange.equals(0L);
        }

        return SnapshotCompareResultVO.TableCompare.builder()
                .snapshotARowCount(rowA)
                .snapshotBRowCount(rowB)
                .rowCountChange(rowChange)
                .rowCountChangeRate(rowChangeRate)
                .rowCountSignificant(rowChange != null && rowSignificant)
                .snapshotAColumnCount(colA)
                .snapshotBColumnCount(colB)
                .columnCountChange(colChange)
                .columnCountChangeRate(colChangeRate)
                .columnCountSignificant(colChange != null && colSignificant)
                .snapshotAStorageBytes(storageA)
                .snapshotBStorageBytes(storageB)
                .storageBytesChange(storageChange)
                .storageBytesChangeRate(storageChangeRate)
                .storageBytesSignificant(storageChange != null && storageSignificant)
                .snapshotAUpdateTime(statsA != null ? statsA.getUpdateTime() : null)
                .snapshotBUpdateTime(statsB != null ? statsB.getUpdateTime() : null)
                .updateTimeChanged(updateTimeChanged)
                .snapshotAIncrementRows(statsA != null ? statsA.getIncrementRows() : null)
                .snapshotBIncrementRows(statsB != null ? statsB.getIncrementRows() : null)
                .incrementRowsChange(incrementChange)
                .incrementRowsChanged(incrementChanged)
                .build();
    }

    private ColumnCompareVO compareColumn(DprofileColumnStats colA, DprofileColumnStats colB,
                                         SnapshotCompareRequestDTO request) {
        String dataTypeCat = classifyDataType(colA.getDataType());

        Long totalA = colA.getTotalCount();
        Long totalB = colB.getTotalCount();
        Long totalChange = (totalA != null && totalB != null) ? (totalB - totalA) : null;

        Long nullA = colA.getNullCount();
        Long nullB = colB.getNullCount();
        Long nullChange = (nullA != null && nullB != null) ? (nullB - nullA) : null;
        BigDecimal nullRateA = colA.getNullRate();
        BigDecimal nullRateB = colB.getNullRate();
        BigDecimal nullRateChange = subtractRate(nullRateA, nullRateB);
        BigDecimal nullThreshold = request != null
                ? request.getNullRateChangeThreshold() : new BigDecimal("10");
        boolean nullRateSignificant = isSignificantChange(nullRateChange, nullThreshold);

        Long uniqueA = colA.getUniqueCount();
        Long uniqueB = colB.getUniqueCount();
        Long uniqueChange = (uniqueA != null && uniqueB != null) ? (uniqueB - uniqueA) : null;
        BigDecimal uniqueRateA = colA.getUniqueRate();
        BigDecimal uniqueRateB = colB.getUniqueRate();
        BigDecimal uniqueRateChange = subtractRate(uniqueRateA, uniqueRateB);
        BigDecimal uniqueThreshold = request != null
                ? request.getUniqueRateChangeThreshold() : new BigDecimal("10");
        boolean uniqueRateSignificant = isSignificantChange(uniqueRateChange, uniqueThreshold);

        boolean minChanged = colA.getMinValue() == null && colB.getMinValue() == null ? false :
                (colA.getMinValue() == null || colB.getMinValue() == null ? true :
                        !colA.getMinValue().equals(colB.getMinValue()));
        boolean maxChanged = colA.getMaxValue() == null && colB.getMaxValue() == null ? false :
                (colA.getMaxValue() == null || colB.getMaxValue() == null ? true :
                        !colA.getMaxValue().equals(colB.getMaxValue()));

        boolean avgSignificant = false;
        if (colA.getAvgValue() != null && colB.getAvgValue() != null) {
            BigDecimal avgDiff = colB.getAvgValue().subtract(colA.getAvgValue()).abs();
            if (colA.getAvgValue().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal avgChangeRate = avgDiff
                        .divide(colA.getAvgValue().abs(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                avgSignificant = avgChangeRate.compareTo(new BigDecimal("5")) > 0;
            }
        }

        boolean minLenChanged = false;
        boolean maxLenChanged = false;
        if ("STRING".equals(dataTypeCat)) {
            minLenChanged = !Objects.equals(colA.getMinLength(), colB.getMinLength());
            maxLenChanged = !Objects.equals(colA.getMaxLength(), colB.getMaxLength());
        }

        boolean outlierChanged = !Objects.equals(colA.getOutlierCount(), colB.getOutlierCount());

        boolean minMaxSignificant = request == null || Boolean.TRUE.equals(request.getCheckMinMaxChange());
        boolean significant = nullRateSignificant || uniqueRateSignificant || avgSignificant
                || (minMaxSignificant && (minChanged || maxChanged));

        List<ColumnCompareVO.TopNValueCompare> topNChanges = buildTopNChanges(colA, colB, request);

        String status = significant ? "SIGNIFICANT" : "UNCHANGED";
        String diffLevel = significant ? "SIGNIFICANT" : "NONE";
        if (topNChanges != null && !topNChanges.isEmpty()) {
            boolean anySignificantTopN = topNChanges.stream()
                    .anyMatch(t -> Boolean.TRUE.equals(t.getSignificant()));
            if (anySignificantTopN) {
                diffLevel = "SIGNIFICANT";
                status = "SIGNIFICANT";
            }
        }

        String diffSummary = buildDiffSummary(colA.getColumnName(), nullRateSignificant,
                uniqueRateSignificant, minChanged, maxChanged, avgSignificant,
                !topNChanges.isEmpty(), outlierChanged);

        return ColumnCompareVO.builder()
                .columnName(colA.getColumnName())
                .dataType(colA.getDataType())
                .dataTypeCategory(dataTypeCat)
                .status(status)
                .diffLevel(diffLevel)
                .snapshotATotalCount(totalA)
                .snapshotBTotalCount(totalB)
                .totalCountChange(totalChange)
                .snapshotANullCount(nullA)
                .snapshotBNullCount(nullB)
                .nullCountChange(nullChange)
                .snapshotANullRate(nullRateA)
                .snapshotBNullRate(nullRateB)
                .nullRateChange(nullRateChange)
                .nullRateSignificant(nullRateSignificant)
                .snapshotAUniqueCount(uniqueA)
                .snapshotBUniqueCount(uniqueB)
                .uniqueCountChange(uniqueChange)
                .snapshotAUniqueRate(uniqueRateA)
                .snapshotBUniqueRate(uniqueRateB)
                .uniqueRateChange(uniqueRateChange)
                .uniqueRateSignificant(uniqueRateSignificant)
                .snapshotAMinValue(colA.getMinValue())
                .snapshotBMinValue(colB.getMinValue())
                .minValueChanged(minChanged)
                .snapshotAMaxValue(colA.getMaxValue())
                .snapshotBMaxValue(colB.getMaxValue())
                .maxValueChanged(maxChanged)
                .snapshotAAvgValue(colA.getAvgValue())
                .snapshotBAvgValue(colB.getAvgValue())
                .avgValueSignificant(avgSignificant)
                .snapshotAStdDev(colA.getStdDev())
                .snapshotBStdDev(colB.getStdDev())
                .snapshotAMinLength(colA.getMinLength())
                .snapshotBMinLength(colB.getMinLength())
                .minLengthChanged(minLenChanged)
                .snapshotAMaxLength(colA.getMaxLength())
                .snapshotBMaxLength(colB.getMaxLength())
                .maxLengthChanged(maxLenChanged)
                .snapshotAAvgLength(colA.getAvgLength())
                .snapshotBAvgLength(colB.getAvgLength())
                .snapshotAOutlierCount(colA.getOutlierCount())
                .snapshotBOutlierCount(colB.getOutlierCount())
                .outlierCountChanged(outlierChanged)
                .topNChanges(topNChanges)
                .diffSummary(diffSummary)
                .build();
    }

    private List<ColumnCompareVO.TopNValueCompare> buildTopNChanges(
            DprofileColumnStats colA, DprofileColumnStats colB,
            SnapshotCompareRequestDTO request) {
        List<ColumnCompareVO.TopNValueCompare> changes = new ArrayList<>();

        Map<String, BigDecimal> topMapA = parseTopValues(colA.getTopValues());
        Map<String, BigDecimal> topMapB = parseTopValues(colB.getTopValues());

        int topN = request != null ? request.getTopNCompareCount() : 5;
        BigDecimal changeThreshold = request != null
                ? request.getTopNChangeThreshold() : new BigDecimal("5");

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(topMapA.keySet());
        allKeys.addAll(topMapB.keySet());

        List<String> sortedKeys = allKeys.stream()
                .sorted((a, b) -> {
                    BigDecimal rateA = topMapB.getOrDefault(a, BigDecimal.ZERO);
                    BigDecimal rateB = topMapB.getOrDefault(b, BigDecimal.ZERO);
                    return rateB.compareTo(rateA);
                })
                .limit(topN)
                .toList();

        for (String key : sortedKeys) {
            BigDecimal rateA = topMapA.getOrDefault(key, null);
            BigDecimal rateB = topMapB.getOrDefault(key, null);

            if (rateA == null && rateB != null) {
                changes.add(ColumnCompareVO.TopNValueCompare.builder()
                        .value(key)
                        .status("NEW")
                        .snapshotARate(null)
                        .snapshotBRate(rateB)
                        .rateChange(rateB)
                        .significant(true)
                        .build());
            } else if (rateA != null && rateB == null) {
                changes.add(ColumnCompareVO.TopNValueCompare.builder()
                        .value(key)
                        .status("REMOVED")
                        .snapshotARate(rateA)
                        .snapshotBRate(null)
                        .rateChange(rateA.negate())
                        .significant(true)
                        .build());
            } else if (rateA != null && rateB != null) {
                BigDecimal rateChange = rateB.subtract(rateA);
                boolean significant = rateChange.abs().compareTo(changeThreshold) > 0;
                changes.add(ColumnCompareVO.TopNValueCompare.builder()
                        .value(key)
                        .status("RATE_CHANGED")
                        .snapshotARate(rateA)
                        .snapshotBRate(rateB)
                        .rateChange(rateChange)
                        .significant(significant)
                        .build());
            }
        }

        return changes;
    }

    private Map<String, BigDecimal> parseTopValues(String topValuesJson) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        if (topValuesJson == null || topValuesJson.isEmpty()) return result;

        try {
            List<Map<String, Object>> list = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(topValuesJson, List.class);
            for (Map<String, Object> item : list) {
                Object val = item.get("value");
                Object rate = item.get("rate");
                if (val != null && rate != null) {
                    result.put(String.valueOf(val), toDecimal(rate));
                }
            }
        } catch (Exception e) {
            log.debug("解析 topValues JSON 失败: {}", e.getMessage());
        }
        return result;
    }

    private ColumnCompareVO buildNewColumn(DprofileColumnStats col) {
        return ColumnCompareVO.builder()
                .columnName(col.getColumnName())
                .dataType(col.getDataType())
                .dataTypeCategory(classifyDataType(col.getDataType()))
                .status("NEW")
                .diffLevel("SIGNIFICANT")
                .snapshotBTotalCount(col.getTotalCount())
                .snapshotBNullCount(col.getNullCount())
                .snapshotBNullRate(col.getNullRate())
                .snapshotBUniqueCount(col.getUniqueCount())
                .snapshotBUniqueRate(col.getUniqueRate())
                .snapshotBMinValue(col.getMinValue())
                .snapshotBMaxValue(col.getMaxValue())
                .snapshotBAvgValue(col.getAvgValue())
                .snapshotBStdDev(col.getStdDev())
                .snapshotBMinLength(col.getMinLength())
                .snapshotBMaxLength(col.getMaxLength())
                .snapshotBAvgLength(col.getAvgLength())
                .snapshotBOutlierCount(col.getOutlierCount())
                .diffSummary("新增列 [" + col.getColumnName() + "]")
                .build();
    }

    private ColumnCompareVO buildRemovedColumn(DprofileColumnStats col) {
        return ColumnCompareVO.builder()
                .columnName(col.getColumnName())
                .dataType(col.getDataType())
                .dataTypeCategory(classifyDataType(col.getDataType()))
                .status("REMOVED")
                .diffLevel("SIGNIFICANT")
                .snapshotATotalCount(col.getTotalCount())
                .snapshotANullCount(col.getNullCount())
                .snapshotANullRate(col.getNullRate())
                .snapshotAUniqueCount(col.getUniqueCount())
                .snapshotAUniqueRate(col.getUniqueRate())
                .snapshotAMinValue(col.getMinValue())
                .snapshotAMaxValue(col.getMaxValue())
                .snapshotAAvgValue(col.getAvgValue())
                .snapshotAStdDev(col.getStdDev())
                .snapshotAMinLength(col.getMinLength())
                .snapshotAMaxLength(col.getMaxLength())
                .snapshotAAvgLength(col.getAvgLength())
                .snapshotAOutlierCount(col.getOutlierCount())
                .diffSummary("移除列 [" + col.getColumnName() + "]")
                .build();
    }

    private String buildSignificantChangeDesc(ColumnCompareVO compare) {
        StringBuilder sb = new StringBuilder();
        sb.append("列 [").append(compare.getColumnName()).append("]: ");
        List<String> items = new ArrayList<>();
        if (Boolean.TRUE.equals(compare.getNullRateSignificant())) {
            items.add("空值率变化 " + formatRateChange(compare.getNullRateChange()));
        }
        if (Boolean.TRUE.equals(compare.getUniqueRateSignificant())) {
            items.add("唯一率变化 " + formatRateChange(compare.getUniqueRateChange()));
        }
        if (Boolean.TRUE.equals(compare.getMinValueChanged())) {
            items.add("最小值从 [" + nullSafe(compare.getSnapshotAMinValue())
                    + "] 变为 [" + nullSafe(compare.getSnapshotBMinValue()) + "]");
        }
        if (Boolean.TRUE.equals(compare.getMaxValueChanged())) {
            items.add("最大值从 [" + nullSafe(compare.getSnapshotAMaxValue())
                    + "] 变为 [" + nullSafe(compare.getSnapshotBMaxValue()) + "]");
        }
        if (Boolean.TRUE.equals(compare.getAvgValueSignificant())) {
            items.add("平均值显著变化");
        }
        if (Boolean.TRUE.equals(compare.getOutlierCountChanged())) {
            items.add("异常值数量变化");
        }
        sb.append(String.join(", ", items));
        return sb.toString();
    }

    private String buildDiffSummary(String columnName, boolean nullRateSig, boolean uniqueRateSig,
                                    boolean minChanged, boolean maxChanged, boolean avgSig,
                                    boolean hasTopN, boolean outlierChanged) {
        List<String> parts = new ArrayList<>();
        if (nullRateSig) parts.add("空值率变化");
        if (uniqueRateSig) parts.add("唯一性变化");
        if (minChanged) parts.add("最小值变化");
        if (maxChanged) parts.add("最大值变化");
        if (avgSig) parts.add("平均值显著变化");
        if (hasTopN) parts.add("分布变化");
        if (outlierChanged) parts.add("异常值变化");
        return parts.isEmpty() ? "无显著变化" : columnName + ": " + String.join(", ", parts);
    }

    private String determineDiffLevel(int totalDiff, int tableDiffs, int columnDiffs) {
        if (totalDiff == 0) return "NONE";
        if (tableDiffs > 0 && columnDiffs == 0) {
            return tableDiffs >= 2 ? "MAJOR" : "MODERATE";
        }
        if (columnDiffs > 0) {
            if (columnDiffs >= 5) return "CRITICAL";
            if (columnDiffs >= 3) return "MAJOR";
            if (columnDiffs >= 1) return "MODERATE";
        }
        return totalDiff > 0 ? "MINOR" : "NONE";
    }

    private BigDecimal calcRate(Number oldVal, Number newVal) {
        if (oldVal == null || newVal == null) return null;
        double oldD = oldVal.doubleValue();
        if (oldD == 0) {
            return newVal.doubleValue() == 0 ? BigDecimal.ZERO : new BigDecimal("100");
        }
        return BigDecimal.valueOf(newVal.doubleValue() - oldD)
                .divide(BigDecimal.valueOf(Math.abs(oldD)), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal subtractRate(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a.negate();
        return b.subtract(a).setScale(4, RoundingMode.HALF_UP);
    }

    private boolean isSignificantRate(BigDecimal rate, BigDecimal threshold) {
        if (rate == null || threshold == null) return false;
        return rate.abs().compareTo(threshold) > 0;
    }

    private boolean isSignificantChange(BigDecimal change, BigDecimal threshold) {
        if (change == null) return false;
        return change.abs().compareTo(threshold) > 0;
    }

    private String formatRateChange(BigDecimal change) {
        if (change == null) return "N/A";
        return (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                + change.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String nullSafe(String val) {
        return val != null ? val : "N/A";
    }

    private String classifyDataType(String dataType) {
        if (dataType == null) return "OTHER";
        String lower = dataType.toLowerCase();
        if (lower.contains("int") || lower.contains("decimal") || lower.contains("numeric")
                || lower.contains("float") || lower.contains("double") || lower.contains("real")
                || lower.contains("number") || lower.contains("smallmoney")
                || lower.contains("money") || lower.contains("bit")) {
            return "NUMERIC";
        }
        if (lower.contains("char") || lower.contains("text") || lower.contains("varchar")
                || lower.contains("nchar") || lower.contains("nvarchar")
                || lower.contains("clob") || lower.contains("blob")
                || lower.contains("binary") || lower.contains("varbinary")) {
            return "STRING";
        }
        if (lower.contains("date") || lower.contains("time") || lower.contains("timestamp")
                || lower.contains("datetime") || lower.contains("interval")
                || lower.contains("year") || lower.contains("smalldatetime")) {
            return "DATETIME";
        }
        if (lower.contains("bool") || lower.equals("bit")) {
            return "BOOLEAN";
        }
        return "OTHER";
    }

    // ========== 辅助方法 ==========

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ========== 列级探查 ==========

    @Override
    public Result<List<ColumnProfileResultVO>> profileColumns(Long tableStatsId) {
        List<DprofileColumnStats> columnStats = columnAnalyzer.getColumnStatsByTableStatsId(tableStatsId);
        List<ColumnProfileResultVO> results = columnStats.stream()
                .map(this::convertToColumnProfileVO)
                .toList();
        return Result.success(results);
    }

    @Override
    public Result<Map<String, Object>> getColumnDistribution(Long dsId, String tableName,
                                                              String columnName, String dataType, int topN) {
        Map<String, Object> distribution = new java.util.LinkedHashMap<>();

        DataSourceAdapter adapter = adapterRegistry.getAdapterById(dsId);
        if (adapter == null) {
            throw new BusinessException(400, "数据源适配器未找到");
        }

        // 判断数据类型
        boolean isNumeric = isNumericType(dataType);
        boolean isString = isStringType(dataType);

        if (isNumeric) {
            // 数值类型：获取数值分布 + Top-N（按值分组）
            Map<String, Object> numericDist = distributionAnalyzer.analyzeNumericDistribution(
                    dsId, tableName, columnName, 10);
            distribution.put("type", "numeric");
            distribution.put("numericHistogram", numericDist.get("histogram"));
            distribution.put("min", numericDist.get("min"));
            distribution.put("max", numericDist.get("max"));
            distribution.put("totalRows", numericDist.get("totalRows"));

            // Top-N 值（频率最高的值）
            Map<String, Object> freqDist = distributionAnalyzer.analyzeFrequency(
                    dsId, tableName, columnName, topN);
            distribution.put("topValues", freqDist.get("distribution"));

            // 异常检测
            Map<String, Object> outlierResult = distributionAnalyzer.detectOutliersByIQR(
                    dsId, tableName, columnName);
            distribution.put("outlier", outlierResult);

        } else if (isString) {
            // 字符串类型：长度分布 + Top-N 值
            Map<String, Object> lengthDist = distributionAnalyzer.analyzeLengthDistribution(
                    dsId, tableName, columnName);
            distribution.put("type", "string");
            distribution.put("lengthHistogram", lengthDist.get("histogram"));
            distribution.put("totalRows", lengthDist.get("totalRows"));

            // Top-N 值
            Map<String, Object> freqDist = distributionAnalyzer.analyzeFrequency(
                    dsId, tableName, columnName, topN);
            distribution.put("topValues", freqDist.get("distribution"));

        } else {
            // 其他类型（日期等）：仅 Top-N
            Map<String, Object> freqDist = distributionAnalyzer.analyzeFrequency(
                    dsId, tableName, columnName, topN);
            distribution.put("type", "other");
            distribution.put("topValues", freqDist.get("distribution"));
            distribution.put("totalRows", freqDist.get("totalRows"));
        }

        return Result.success(distribution);
    }

    @Override
    public Result<List<ColumnProfileResultVO>> listColumnProfilesWithWarnings(Long tableStatsId) {
        List<DprofileColumnStats> columnStats = columnAnalyzer.getColumnStatsByTableStatsId(tableStatsId);
        List<ColumnProfileResultVO> results = columnStats.stream()
                .map(this::convertToColumnProfileVO)
                .map(this::enrichWithWarnings)
                .toList();
        return Result.success(results);
    }

    /**
     * 将实体转换为 VO
     */
    private ColumnProfileResultVO convertToColumnProfileVO(DprofileColumnStats entity) {
        ColumnProfileResultVO.ColumnProfileResultVOBuilder builder = ColumnProfileResultVO.builder()
                .id(entity.getId())
                .tableStatsId(entity.getTableStatsId())
                .dsId(entity.getDsId())
                .tableName(entity.getTableName())
                .columnName(entity.getColumnName())
                .dataType(entity.getDataType())
                .nullable(entity.getNullable())
                .profileTime(entity.getProfileTime())
                .totalCount(entity.getTotalCount())
                .nullCount(entity.getNullCount())
                .nullRate(entity.getNullRate())
                .uniqueCount(entity.getUniqueCount())
                .uniqueRate(entity.getUniqueRate())
                .minValue(entity.getMinValue())
                .maxValue(entity.getMaxValue())
                .avgValue(entity.getAvgValue())
                .medianValue(entity.getMedianValue())
                .stdDev(entity.getStdDev())
                .zeroCount(entity.getZeroCount())
                .zeroRate(entity.getZeroRate())
                .negativeCount(entity.getNegativeCount())
                .minLength(entity.getMinLength())
                .maxLength(entity.getMaxLength())
                .avgLength(entity.getAvgLength())
                .outlierCount(entity.getOutlierCount())
                .outlierRate(entity.getOutlierRate())
                .createTime(entity.getCreateTime());

        // 解析 topValues JSON
        if (entity.getTopValues() != null && !entity.getTopValues().isEmpty()) {
            try {
                List<Map<String, Object>> topList = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(entity.getTopValues(), List.class);
                List<ColumnProfileResultVO.TopValueItem> topItems = topList.stream()
                        .map(item -> {
                            Object val = item.get("value");
                            Object cnt = item.get("count");
                            Object rate = item.get("rate");
                            return ColumnProfileResultVO.TopValueItem.builder()
                                    .value(val != null ? String.valueOf(val) : null)
                                    .count(toLong(cnt))
                                    .rate(toDecimal(rate))
                                    .build();
                        })
                        .toList();
                builder.topValues(topItems);
            } catch (Exception e) {
                log.debug("解析 topValues 失败: {}", e.getMessage());
            }
        }

        // 解析 histogram JSON
        if (entity.getHistogram() != null && !entity.getHistogram().isEmpty()) {
            try {
                List<Map<String, Object>> histList = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(entity.getHistogram(), List.class);
                List<ColumnProfileResultVO.HistogramBucket> buckets = histList.stream()
                        .map(item -> {
                            Object range = item.get("range");
                            Object cnt = item.get("count");
                            Object rate = item.get("rate");
                            return ColumnProfileResultVO.HistogramBucket.builder()
                                    .range(range != null ? String.valueOf(range) : null)
                                    .count(toLong(cnt))
                                    .rate(toDecimal(rate))
                                    .build();
                        })
                        .toList();
                builder.histogram(buckets);
            } catch (Exception e) {
                log.debug("解析 histogram 失败: {}", e.getMessage());
            }
        }

        // 设置数据类型分类
        String dataType = entity.getDataType();
        String category = "OTHER";
        if (isNumericType(dataType)) {
            category = "NUMERIC";
        } else if (isStringType(dataType)) {
            category = "STRING";
        } else if (isDateTimeType(dataType)) {
            category = "DATETIME";
        } else if (isBooleanType(dataType)) {
            category = "BOOLEAN";
        }
        builder.dataTypeCategory(category);

        return builder.build();
    }

    /**
     * 补充异常警告信息
     */
    private ColumnProfileResultVO enrichWithWarnings(ColumnProfileResultVO vo) {
        List<String> warnings = new java.util.ArrayList<>();

        // 空值率警告
        if (vo.getNullRate() != null) {
            double nullPct = vo.getNullRate().doubleValue();
            if (nullPct > 50) {
                warnings.add("【高】空值率达 " + String.format("%.1f", nullPct) + "%，建议检查数据完整性");
            } else if (nullPct > 20) {
                warnings.add("【中】空值率偏高 " + String.format("%.1f", nullPct) + "%，关注数据覆盖情况");
            }
        }

        // 唯一率警告
        if (vo.getUniqueRate() != null) {
            double uniquePct = vo.getUniqueRate().doubleValue();
            if (uniquePct > 99) {
                warnings.add("【提示】唯一率 " + String.format("%.1f", uniquePct) + "%，字段接近主键");
            }
        }

        // 零值率警告（数值类型）
        if (vo.getZeroRate() != null && vo.getDataTypeCategory().equals("NUMERIC")) {
            double zeroPct = vo.getZeroRate().doubleValue();
            if (zeroPct > 30) {
                warnings.add("【高】零值率达 " + String.format("%.1f", zeroPct) + "%，存在大量零值");
            } else if (zeroPct > 10) {
                warnings.add("【中】零值率 " + String.format("%.1f", zeroPct) + "%，关注数据合理性");
            }
        }

        // 负值警告
        if (vo.getNegativeCount() != null && vo.getNegativeCount() > 0
                && vo.getDataTypeCategory().equals("NUMERIC")) {
            warnings.add("【中】存在 " + vo.getNegativeCount() + " 个负值，检查数据合法性");
        }

        // 异常值警告
        if (vo.getOutlierCount() != null && vo.getOutlierCount() > 0) {
            warnings.add("【中】检测到 " + vo.getOutlierCount() + " 个异常值（基于 "
                    + (vo.getOutlierMethod() != null ? vo.getOutlierMethod() : "IQR") + " 法则）");
        }

        // 数据类型判断
        if (vo.getDataTypeCategory().equals("NUMERIC") && vo.getAvgValue() == null) {
            warnings.add("【警告】数值类型但平均值未计算，可能存在非数值数据");
        }

        if (vo.getDataTypeCategory().equals("STRING")) {
            if (vo.getMinLength() != null && vo.getMaxLength() != null) {
                if (vo.getMinLength() > vo.getMaxLength()) {
                    warnings.add("【错误】字段长度数据异常");
                } else if (vo.getMaxLength() > 1000) {
                    warnings.add("【提示】字段最大长度达 " + vo.getMaxLength() + "，建议核实定义");
                }
            }
        }

        // 离散度分析（Cardinality）
        if (vo.getUniqueCount() != null && vo.getTotalCount() != null && vo.getTotalCount() > 0) {
            double cardinality = (double) vo.getUniqueCount() / vo.getTotalCount();
            if (cardinality >= 0.95 && vo.getTotalCount() > 100) {
                warnings.add("【提示】高离散度（" + String.format("%.1f", cardinality * 100) + "%），每个值几乎唯一");
            } else if (cardinality <= 0.01 && vo.getTotalCount() > 100) {
                warnings.add("【提示】低离散度（" + String.format("%.1f", cardinality * 100) + "%），枚举类型特征明显");
            }
        }

        vo.setWarnings(warnings);
        return vo;
    }

    // ==================== 工具方法 ====================

    private boolean isNumericType(String dataType) {
        if (dataType == null) return false;
        dataType = dataType.toLowerCase();
        return dataType.contains("int") || dataType.contains("decimal")
                || dataType.contains("numeric") || dataType.contains("float")
                || dataType.contains("double") || dataType.contains("real")
                || dataType.contains("number") || dataType.contains("smallmoney")
                || dataType.contains("money") || dataType.contains("bit");
    }

    private boolean isStringType(String dataType) {
        if (dataType == null) return false;
        dataType = dataType.toLowerCase();
        return dataType.contains("char") || dataType.contains("text")
                || dataType.contains("varchar") || dataType.contains("nchar")
                || dataType.contains("nvarchar") || dataType.contains("clob")
                || dataType.contains("blob") || dataType.contains("binary")
                || dataType.contains("varbinary");
    }

    private boolean isDateTimeType(String dataType) {
        if (dataType == null) return false;
        dataType = dataType.toLowerCase();
        return dataType.contains("date") || dataType.contains("time")
                || dataType.contains("timestamp") || dataType.contains("datetime")
                || dataType.contains("interval") || dataType.contains("year")
                || dataType.contains("smalldatetime");
    }

    private boolean isBooleanType(String dataType) {
        if (dataType == null) return false;
        dataType = dataType.toLowerCase();
        return dataType.contains("bool") || dataType.equals("bit");
    }
}
