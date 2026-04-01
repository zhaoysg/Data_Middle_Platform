package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.SecAccessLog;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 脱敏访问日志视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SecAccessLog.class)
public class SecAccessLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long dsId;

    private Long userId;

    private String userName;

    private String querySql;

    private String maskedSql;

    private Integer resultRows;

    private String maskedColumns;

    private Long elapsedMs;

    private String status;

    private String errorMsg;

    private String ipAddress;

    private LocalDateTime createTime;

    /**
     * 扩展参数（用于时间范围查询等）
     */
    private Map<String, Object> params;
}
