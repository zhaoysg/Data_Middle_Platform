package com.bagdatahouse.governance.metadata.scanner;

import com.bagdatahouse.governance.metadata.context.MetadataScanContext;

/**
 * 元数据扫描器接口
 * 负责从外部数据源采集表结构信息（字段、主键、注释等）
 */
public interface MetadataScanner {

    /**
     * 执行扫描入口
     * @param ctx 扫描上下文
     */
    void scan(MetadataScanContext ctx);

    /**
     * 扫描单个表
     * @param ctx     扫描上下文
     * @param tableName 表名
     */
    void scanTable(MetadataScanContext ctx, String tableName);

    /**
     * 获取扫描器对应的数据源类型
     */
    String getDataSourceType();
}
