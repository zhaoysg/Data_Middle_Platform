package org.dromara.metadata.service;

import org.dromara.metadata.domain.vo.ColumnStats;
import org.dromara.metadata.domain.vo.TableStats;

import java.util.List;

/**
 * 数据探查服务接口
 */
public interface IDprofileService {

    /**
     * 分析单张表
     *
     * @param dsId       数据源ID
     * @param tableName  表名
     * @param level      探查级别：BASIC/DETAILED/FULL
     * @return 探查报告ID
     */
    Long analyzeTable(Long dsId, String tableName, String level);

    /**
     * 批量分析多张表
     *
     * @param dsId        数据源ID
     * @param tableNames  表名列表
     * @param level       探查级别
     * @return 报告ID列表
     */
    List<Long> analyzeTables(Long dsId, List<String> tableNames, String level);

    /**
     * 获取表级统计
     *
     * @param dsId      数据源ID
     * @param tableName 表名
     * @return 表级统计信息
     */
    TableStats getTableStats(Long dsId, String tableName);

    /**
     * 获取列级统计
     *
     * @param dsId      数据源ID
     * @param tableName 表名
     * @return 列级统计信息列表
     */
    List<ColumnStats> getColumnStats(Long dsId, String tableName);
}
