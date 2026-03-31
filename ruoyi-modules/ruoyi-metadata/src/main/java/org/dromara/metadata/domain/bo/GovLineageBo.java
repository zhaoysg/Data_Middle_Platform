package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.GovLineage;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治理血缘关系业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = GovLineage.class, reverseConvertGenerate = false)
public class GovLineageBo extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
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
    @Size(max = 500, message = "业务描述长度不能超过{max}个字符")
    private String bizDescription;

    /**
     * 负责人ID
     */
    private Long ownerId;

    /**
     * 核验状态：UNVERIFIED/VERIFIED/INVALID
     */
    private String verifyStatus;
}
