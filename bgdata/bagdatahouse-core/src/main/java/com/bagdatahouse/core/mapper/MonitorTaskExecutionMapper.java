package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.core.entity.MonitorTaskExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MonitorTaskExecution Mapper
 * <p>
 * 注意：实体有 5 个 @TableField(exist=false) 字段（planId, planName, dsName,
 * triggerUserName, taskTypeName），MyBatis-Plus 默认的 selectList/selectPage
 * 会包含所有实体字段导致 SQL 错误。因此本 Mapper 提供自定义方法，仅 SELECT
 * 数据库实际存在的列。
 */
@Mapper
public interface MonitorTaskExecutionMapper extends BaseMapper<MonitorTaskExecution> {

    /**
     * 查询今日执行记录（仅数据库实际列）
     */
    List<MonitorTaskExecution> selectForToday(@Param("todayStart") LocalDateTime todayStart,
                                              @Param("todayEnd") LocalDateTime todayEnd);

    /**
     * 查询指定时间范围的执行记录（仅数据库实际列）
     */
    List<MonitorTaskExecution> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最近的执行记录（仅数据库实际列）
     */
    List<MonitorTaskExecution> selectRecent(@Param("limit") int limit);

    /**
     * 按状态查询最近记录（仅数据库实际列）
     */
    List<MonitorTaskExecution> selectRecentByStatus(@Param("startTime") LocalDateTime startTime,
                                                     @Param("status") String status,
                                                     @Param("limit") int limit);

    /**
     * 按状态查询所有记录（仅数据库实际列）
     */
    List<MonitorTaskExecution> selectByStatus(@Param("status") String status);

    /**
     * 分页查询执行记录（仅数据库实际列，避免 selectPage 的 @TableField(exist=false) 问题）
     *
     * @param page     分页对象
     * @param taskId   任务ID（可选）
     * @param taskType 任务类型（可选）
     * @param status   状态（可选）
     * @param triggerType 触发类型（可选）
     * @param startDate 开始日期（可选，yyyy-MM-dd）
     * @param endDate   结束日期（可选，yyyy-MM-dd）
     * @return 分页结果
     */
    IPage<MonitorTaskExecution> selectExecutionPage(Page<MonitorTaskExecution> page,
                                                   @Param("taskId") Long taskId,
                                                   @Param("taskType") String taskType,
                                                   @Param("status") String status,
                                                   @Param("triggerType") String triggerType,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);
}
