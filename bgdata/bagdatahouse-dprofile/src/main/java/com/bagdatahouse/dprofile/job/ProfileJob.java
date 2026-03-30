package com.bagdatahouse.dprofile.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bagdatahouse.core.entity.DprofileProfileTask;
import com.bagdatahouse.core.mapper.DprofileProfileTaskMapper;
import com.bagdatahouse.dprofile.analyzer.ColumnAnalyzer;
import com.bagdatahouse.dprofile.analyzer.TableAnalyzer;
import com.bagdatahouse.dprofile.vo.TableProfileResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 探查任务执行器
 * 负责异步执行探查任务（表级 + 列级）
 * 支持手动触发和定时触发
 */
@Slf4j
@Component
public class ProfileJob {

    @Autowired
    private TableAnalyzer tableAnalyzer;

    @Autowired
    private ColumnAnalyzer columnAnalyzer;

    @Autowired
    private DprofileProfileTaskMapper taskMapper;

    /**
     * 执行探查任务（异步）
     *
     * @param taskId 探查任务ID
     * @return CompletableFuture 包含表级探查结果
     */
    @Async("profileTaskExecutor")
    public CompletableFuture<TableProfileResultVO> executeAsync(Long taskId) {
        DprofileProfileTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("探查任务不存在: taskId={}", taskId);
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.info("开始执行探查任务: taskId={}, taskName={}, table={}.{}",
                    taskId, task.getTaskName(), task.getTargetDsId(), task.getTargetTable());

            // 统一使用任务表中已记录的 executionId（若前端/服务端已生成），避免前后端追踪“假 ID”
            Long executionId = task.getLastExecutionId();
            if (executionId == null) {
                executionId = System.currentTimeMillis();
            }

            // 更新任务状态为运行中
            task.setStatus("RUNNING"); // 运行中
            task.setLastExecutionTime(LocalDateTime.now());
            task.setLastExecutionId(executionId);
            taskMapper.updateById(task);

            // 执行表级探查（PostgreSQL 使用 targetSchema）
            TableProfileResultVO tableResult = tableAnalyzer.analyze(
                    taskId,
                    task.getTaskName(),
                    task.getTargetDsId(),
                    task.getTargetTable(),
                    task.getTargetSchema(),
                    executionId
            );

            // 根据探查级别决定是否执行列级探查
            String profileLevel = task.getProfileLevel();
            boolean doColumnProfile = "DETAILED".equalsIgnoreCase(profileLevel)
                    || "FULL".equalsIgnoreCase(profileLevel)
                    || "TABLE_AND_COLUMN".equalsIgnoreCase(profileLevel);

            if (doColumnProfile && tableResult.getTableStatsId() != null) {
                // 解析指定列
                List<String> columnNames = null;
                if (StringUtils.hasText(task.getTargetColumns())) {
                    columnNames = Arrays.asList(task.getTargetColumns().split(","));
                }

                // 执行列级探查
                columnAnalyzer.analyzeColumns(
                        taskId,
                        task.getTargetDsId(),
                        task.getTargetTable(),
                        task.getTargetSchema(),
                        tableResult.getExecutionId(),
                        tableResult.getTableStatsId(),
                        columnNames,
                        tableResult.getProfileTime()
                );
            }

            // 更新任务状态为成功
            task.setStatus("SUCCESS");
            taskMapper.updateById(task);

            log.info("探查任务执行完成: taskId={}, tableStatsId={}, rowCount={}",
                    taskId, tableResult.getTableStatsId(), tableResult.getRowCount());

            return CompletableFuture.completedFuture(tableResult);

        } catch (Exception e) {
            log.error("探查任务执行失败: taskId={}", taskId, e);

            // 更新任务状态为失败
            task.setStatus("FAILED"); // 失败
            taskMapper.updateById(task);

            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 执行单个表的探查（手动探查）
     */
    public TableProfileResultVO executeSingleTable(Long taskId, String taskName,
                                                   Long dsId, String tableName,
                                                   List<String> columnNames,
                                                   boolean collectColumnStats) {
        try {
            log.info("开始执行单表探查: dsId={}, table={}", dsId, tableName);

            // 表级探查
            TableProfileResultVO tableResult = tableAnalyzer.analyze(
                    taskId,
                    taskName,
                    dsId,
                    tableName
            );

            // 列级探查（如果需要）
            if (collectColumnStats && tableResult.getTableStatsId() != null) {
                columnAnalyzer.analyzeColumns(
                        taskId,
                        dsId,
                        tableName,
                        tableResult.getExecutionId(),
                        tableResult.getTableStatsId(),
                        columnNames,
                        tableResult.getProfileTime()
                );
            }

            log.info("单表探查完成: dsId={}, table={}, rowCount={}",
                    dsId, tableName, tableResult.getRowCount());

            return tableResult;

        } catch (Exception e) {
            log.error("单表探查失败: dsId={}, table={}", dsId, tableName, e);
            throw new RuntimeException("单表探查失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量执行定时探查任务
     * 由调度器调用，扫描所有待执行的定时任务
     */
    public void executeScheduledTasks() {
        LambdaQueryWrapper<DprofileProfileTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DprofileProfileTask::getTriggerType, "SCHEDULE")
                .eq(DprofileProfileTask::getStatus, "PUBLISHED"); // 已发布/启用状态
        List<DprofileProfileTask> scheduledTasks = taskMapper.selectList(wrapper);

        for (DprofileProfileTask task : scheduledTasks) {
            try {
                // 检查是否应该执行（根据 cron 表达式）
                if (shouldExecute(task)) {
                    executeAsync(task.getId());
                }
            } catch (Exception e) {
                log.error("定时探查任务触发失败: taskId={}", task.getId(), e);
            }
        }
    }

    /**
     * 判断任务是否应该执行（简化实现）
     * 完整实现需要解析 cron 表达式并与当前时间比对
     */
    private boolean shouldExecute(DprofileProfileTask task) {
        // TODO: 集成 cron-utils 解析 triggerCron，与当前时间比对
        // 目前返回 true，实际生产环境需要实现 cron 比对
        return true;
    }
}
