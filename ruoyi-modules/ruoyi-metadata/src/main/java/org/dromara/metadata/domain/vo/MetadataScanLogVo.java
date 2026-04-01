package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.MetadataScanLog;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 元数据扫描记录视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = MetadataScanLog.class)
public class MetadataScanLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long dsId;
    private String dsName;
    private String dsCode;

    /** 扫描类型：TABLE_ONLY / FULL */
    private String scanType;

    /** 扫描状态：RUNNING / SUCCESS / FAILED / PARTIAL / CANCELLED */
    private String status;

    private Integer totalTables;
    private Integer successCount;
    private Integer partialCount;
    private Integer failedCount;
    private String errorDetail;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long elapsedMs;
    private Long scanUserId;
    private String remark;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
