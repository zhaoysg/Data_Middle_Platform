package org.dromara.metadata.service;

import java.util.List;
import java.util.Map;

/**
 * 脱敏查询服务接口
 */
public interface IMaskQueryService {

    /**
     * 执行脱敏查询
     *
     * @param dsId      数据源ID
     * @param sql       用户输入的SQL（仅支持SELECT）
     * @param userId    当前用户ID
     * @param userName  当前用户名
     * @param ipAddress 客户端IP
     * @return 脱敏查询结果
     */
    MaskQueryResult query(Long dsId, String sql, Long userId, String userName, String ipAddress);

    /**
     * 验证SQL是否合法（双重验证：Controller + Service）
     *
     * @param sql SQL语句
     * @return 验证结果
     */
    ValidationResult validateSql(String sql);

    /**
     * 脱敏查询结果
     */
    record MaskQueryResult(
        List<Map<String, Object>> rows,
        int totalRows,
        boolean truncated,
        List<String> maskedColumns,
        long elapsedMs,
        String originalSql
    ) {
    }

    /**
     * SQL验证结果
     */
    record ValidationResult(
        boolean valid,
        String message,
        boolean syntaxOk,
        boolean isSelect,
        boolean hasLimit,
        int estimatedRows
    ) {
    }
}
