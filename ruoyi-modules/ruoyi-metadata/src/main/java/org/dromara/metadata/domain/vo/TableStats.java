package org.dromara.metadata.domain.vo;

import java.util.List;
import java.util.Map;

/**
 * 表级统计信息记录
 *
 * @param tableName      表名
 * @param rowCount       行数
 * @param columnCount    列数
 * @param dataSizeBytes  数据大小（字节）
 * @param tableComment   表注释
 * @param lastModified   最后更新时间
 */
public record TableStats(
    String tableName,
    long rowCount,
    int columnCount,
    Long dataSizeBytes,
    String tableComment,
    String lastModified
) {}
