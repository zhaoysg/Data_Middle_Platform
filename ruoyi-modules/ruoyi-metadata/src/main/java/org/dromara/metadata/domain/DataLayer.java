package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 数仓分层实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("data_layer")
public class DataLayer extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    /**
     * 分层编码：ODS / DWD / DWS / ADS
     */
    private String layerCode;

    /**
     * 分层名称
     */
    private String layerName;

    /**
     * 分层描述
     */
    private String layerDesc;

    /**
     * 展示颜色
     */
    private String layerColor;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态：0-正常 1-停用
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    public DataLayer(Long id) {
        this.id = id;
    }
}
