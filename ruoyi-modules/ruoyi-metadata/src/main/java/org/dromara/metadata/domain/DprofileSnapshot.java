package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据探查快照实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("dprofile_snapshot")
public class DprofileSnapshot extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 快照编码
     */
    private String snapshotCode;

    /**
     * 快照名称
     */
    private String snapshotName;

    /**
     * 快照描述
     */
    private String snapshotDesc;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 快照元数据JSON
     */
    private String snapshotData;

    /**
     * 包含表数量
     */
    private Integer tableCount;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
