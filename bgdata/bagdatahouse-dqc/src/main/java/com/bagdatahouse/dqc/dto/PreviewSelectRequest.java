package com.bagdatahouse.dqc.dto;

import lombok.Data;

/**
 * 数据源上执行只读 SQL 预览请求体。
 */
@Data
public class PreviewSelectRequest {

    /**
     * 待执行的 SQL（建议前端已完成占位符替换）
     */
    private String sql;
}
