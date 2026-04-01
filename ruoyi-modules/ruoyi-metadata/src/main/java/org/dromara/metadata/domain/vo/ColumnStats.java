package org.dromara.metadata.domain.vo;

import java.util.List;
import java.util.Map;

/**
 * 列级统计信息记录
 *
 * @param columnName    列名
 * @param dataType      数据类型
 * @param columnComment 列注释
 * @param nullable      是否可空
 * @param primaryKey     是否主键
 * @param nullCount     空值数量
 * @param nullRate      空值率
 * @param uniqueCount   唯一值数量
 * @param uniqueRate    唯一值比例
 * @param sampleValues  样本值列表
 * @param topValues     高频值分布（值 -> 出现次数）
 */
public record ColumnStats(
    String columnName,
    String dataType,
    String columnComment,
    boolean nullable,
    boolean primaryKey,
    long nullCount,
    double nullRate,
    long uniqueCount,
    double uniqueRate,
    List<String> sampleValues,
    Map<String, Long> topValues
) {}
