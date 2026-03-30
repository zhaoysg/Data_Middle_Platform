package com.bagdatahouse.dqc.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 规则表达式预览查询结果（只读 SELECT，最多返回若干行）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlPreviewResultVO {

    /** 实际执行的 SQL（规范化后） */
    private String sqlExecuted;

    /** 结果行（列名 -> 值） */
    private List<Map<String, Object>> rows;

    /** 本次返回行数 */
    private Integer rowCount;

    /** 是否可能因行数上限被截断 */
    private Boolean truncated;

    /** 规范化说明，如去掉首尾单引号 */
    private String normalizeNote;
}
