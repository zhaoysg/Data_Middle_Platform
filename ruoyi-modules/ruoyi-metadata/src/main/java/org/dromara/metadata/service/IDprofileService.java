package org.dromara.metadata.service;

import org.dromara.metadata.domain.vo.ColumnStats;
import org.dromara.metadata.domain.vo.TableStats;

import java.util.List;
import java.util.Set;

/**
 * 数据探查服务接口
 */
public interface IDprofileService {

    /**
     * 分析单张表（全部列）
     *
     * @param dsId       数据源ID
     * @param tableName  表名
     * @param level      探查级别：BASIC/DETAILED/FULL
     * @return 探查报告ID
     */
    Long analyzeTable(Long dsId, String tableName, String level);

    /**
     * 分析单张表（指定列）
     *
     * @param dsId         数据源ID
     * @param tableName    表名
     * @param level        探查级别
     * @param targetColumns 指定探查的列名集合（空表示全部列）
     * @return 探查报告ID
     */
    Long analyzeTable(Long dsId, String tableName, String level, Set<String> targetColumns);

    /**
     * 批量分析多张表（全部列）
     *
     * @param dsId        数据源ID
     * @param tableNames  表名列表
     * @param level       探查级别
     * @return 报告ID列表
     */
    List<Long> analyzeTables(Long dsId, List<String> tableNames, String level);

    /**
     * 批量分析多张表（全部列，指定列过滤统一传入用于报告记录）
     *
     * @param dsId         数据源ID
     * @param tableNames   表名列表
     * @param level        探查级别
     * @param targetColumns 指定探查的列名集合（空表示全部列）
     * @return 报告ID列表
     */
    List<Long> analyzeTables(Long dsId, List<String> tableNames, String level,
                             Set<String> targetColumns);

    /**
     * 获取表级统计
     *
     * @param dsId      数据源ID
     * @param tableName 表名
     * @return 表级统计信息
     */
    TableStats getTableStats(Long dsId, String tableName);

    /**
     * 获取列级统计（全部列）
     *
     * @param dsId      数据源ID
     * @param tableName 表名
     * @return 列级统计信息列表
     */
    List<ColumnStats> getColumnStats(Long dsId, String tableName);

    /**
     * 获取列级统计（指定列）
     *
     * @param dsId           数据源ID
     * @param tableName      表名
     * @param targetColumns  指定探查的列名集合
     * @return 列级统计信息列表
     */
    List<ColumnStats> getColumnStats(Long dsId, String tableName, Set<String> targetColumns);
}
