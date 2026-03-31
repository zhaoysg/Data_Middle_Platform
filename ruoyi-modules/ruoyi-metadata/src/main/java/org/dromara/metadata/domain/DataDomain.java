package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 数据域实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("data_domain")
public class DataDomain extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    /**
     * 数据域编码
     */
    private String domainCode;

    /**
     * 数据域名称
     */
    private String domainName;

    /**
     * 数据域描述
     */
    private String domainDesc;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 负责人ID
     */
    private Long ownerId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态：0-正常 1-停用
     */
    private String status;

    @TableLogic
    private String delFlag;
}
