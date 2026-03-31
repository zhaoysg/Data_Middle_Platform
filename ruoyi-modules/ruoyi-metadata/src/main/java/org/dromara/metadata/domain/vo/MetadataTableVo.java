package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.metadata.domain.MetadataTable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 元数据表视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = MetadataTable.class)
public class MetadataTableVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long dsId;
    private String dsName;
    private String dsCode;
    private String tableName;
    private String tableAlias;
    private String tableComment;
    private String tableType;
    private String dataLayer;
    private String dataDomain;
    private Long rowCount;
    private Long storageBytes;
    private LocalDateTime sourceUpdateTime;
    private String sensitivityLevel;
    private Long ownerId;
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "ownerId")
    private String ownerName;
    private Long deptId;
    @Translation(type = TransConstant.DEPT_ID_TO_NAME, mapper = "deptId")
    private String deptName;
    private Long catalogId;
    private String catalogName;
    private String tags;
    private LocalDateTime lastScanTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
}
