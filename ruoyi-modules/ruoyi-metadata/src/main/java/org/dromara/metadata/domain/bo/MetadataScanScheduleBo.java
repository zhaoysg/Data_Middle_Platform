package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.MetadataScanSchedule;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 元数据扫描调度业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MetadataScanSchedule.class, reverseConvertGenerate = false)
public class MetadataScanScheduleBo extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 调度类型：MANUAL/CRON/EVENT
     */
    @NotBlank(message = "调度类型不能为空")
    private String scheduleType;

    /**
     * Cron表达式
     */
    private String cronExpr;

    /**
     * 是否启用：1=启用
     */
    private String enabled;

    /**
     * 是否同步字段：1=同步
     */
    private String syncColumn;

    /**
     * 上次扫描时间
     */
    private LocalDateTime lastScanTime;

    /**
     * 下次扫描时间
     */
    private LocalDateTime nextScanTime;

    /**
     * 是否变更告警：1=告警
     */
    private String changeAlert;

    /**
     * 告警通知目标
     */
    private String alertTargets;

    /**
     * 备注
     */
    private String remark;
}
