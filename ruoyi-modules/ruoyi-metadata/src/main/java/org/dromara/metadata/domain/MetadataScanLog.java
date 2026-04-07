package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 元数据扫描记录实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("metadata_scan_log")
public class MetadataScanLog extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long dsId;
    private String dsName;
    private String dsCode;
    private String scanType;
    private String status;
    private Integer totalTables;
    private Integer successCount;
    private Integer partialCount;
    private Integer failedCount;
    private String errorDetail;
    private java.time.LocalDateTime startTime;
    private java.time.LocalDateTime endTime;
    private Long elapsedMs;
    private Long scanUserId;
    private String remark;

    /**
     * 下游处理：敏感识别匹配数
     */
    private Integer sensitiveMatched;

    /**
     * 下游处理：血缘关系发现数
     */
    private Integer lineageDiscovered;

    /**
     * 下游处理：DQC绑定数
     */
    private Integer dqcBinded;

    /**
     * 下游处理：脱敏应用数
     */
    private Integer maskApplied;
}
