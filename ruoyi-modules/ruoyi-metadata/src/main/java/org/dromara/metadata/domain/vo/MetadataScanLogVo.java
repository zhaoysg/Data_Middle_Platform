package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.metadata.domain.MetadataScanLog;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 元数据扫描记录视图对象
 */
@Data
@ExcelIgnoreUnannotated
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MetadataScanLog.class)
public class MetadataScanLogVo extends org.dromara.common.mybatis.core.domain.BaseEntity implements Serializable {

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

    /**
     * 转换方法：将 entity 字段映射到 VO 字段
     */
    public static MetadataScanLogVo fromEntity(MetadataScanLog entity) {
        if (entity == null) return null;
        MetadataScanLogVo vo = new MetadataScanLogVo();
        vo.setId(entity.getId());
        vo.setDsId(entity.getDsId());
        vo.setDsName(entity.getDsName());
        vo.setDsCode(entity.getDsCode());
        vo.setScanType(entity.getScanType());
        vo.setStatus(entity.getStatus());
        vo.setTotalTables(entity.getTotalTables());
        vo.setSuccessCount(entity.getSuccessCount());
        vo.setPartialCount(entity.getPartialCount());
        vo.setFailedCount(entity.getFailedCount());
        vo.setErrorDetail(entity.getErrorDetail());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setElapsedMs(entity.getElapsedMs());
        vo.setScanUserId(entity.getScanUserId());
        vo.setRemark(entity.getRemark());
        // BaseEntity fields
        vo.setCreateBy(entity.getCreateBy());
        vo.setUpdateBy(entity.getUpdateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
