package org.dromara.datasource.connection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据源连接测试结果 VO
 *
 * @author Lion Li
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否连接成功
     */
    private Boolean success;

    /**
     * 连接消息
     */
    private String message;

    /**
     * 数据库版本
     */
    private String databaseVersion;

    /**
     * 耗时（毫秒）
     */
    private Long elapsedMs;

    /**
     * 测试时间
     */
    private LocalDateTime testTime;
}
