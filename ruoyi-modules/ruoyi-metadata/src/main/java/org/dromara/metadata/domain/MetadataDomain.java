package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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
public class MetadataDomain extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String domainName;
    private String domainCode;
    private String domainDesc;
    private Long ownerId;
    private Long deptId;
    private String status;
    private String remark;
    @TableLogic
    private String delFlag;

    public MetadataDomain(Long id) {
        this.id = id;
    }
}
