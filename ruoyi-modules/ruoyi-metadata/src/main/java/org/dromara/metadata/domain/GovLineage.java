package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治理血缘关系实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("gov_lineage")
public class GovLineage extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 源数据源ID
     */
    private Long srcDsId;

    /**
     * 源数据源名称
     */
    private String srcDsName;

    /**
     * 源表名
     */
    private String srcTableName;

    /**
     * 源列名
     */
    private String srcColumnName;

    /**
     * 目标数据源ID
     */
    private Long tgtDsId;

    /**
     * 目标数据源名称
     */
    private String tgtDsName;

    /**
     * 目标表名
     */
    private String tgtTableName;

    /**
     * 目标列名
     */
    private String tgtColumnName;

    /**
     * 血缘类型：DIRECT/DERIVED
     */
    private String lineageType;

    /**
     * 转换类型：ETL/SQL/STREAMING/COPY
     */
    private String transformType;

    /**
     * 转换SQL
     */
    private String transformSql;

    /**
     * 业务描述
     */
    private String bizDescription;

    /**
     * 负责人ID
     */
    private Long ownerId;

    /**
     * 核验状态：UNVERIFIED/VERIFIED/INVALID
     */
    private String verifyStatus;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
