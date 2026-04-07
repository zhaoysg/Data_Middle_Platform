package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.GovLineage;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治理血缘关系视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = GovLineage.class)
public class GovLineageVo implements Serializable {

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
    private String bizDescription;

    /**
     * 负责人ID
     */
    private Long ownerId;

    /**
     * 负责人名称
     */
    private String ownerName;

    /**
     * 核验状态：UNVERIFIED/VERIFIED/INVALID
     */
    private String verifyStatus;

    /**
     * 血缘深度（用于血缘追溯时标记层级）
     */
    private Integer level;

    /**
     * 数据分层编码（ODS/DWD/DWS/ADS/DIM）
     * 从 metadata_table 表关联获取
     */
    private String layerCode;

    /**
     * 数据分层名称
     */
    private String layerName;

    /**
     * 敏感字段数量（从 sec_column_sensitivity 表统计）
     */
    private Integer sensitiveCount;

    /**
     * 最高敏感等级（HIGH/MEDIUM/LOW）
     * 取该表所有敏感字段中的最高等级
     */
    private String sensitivityLevel;

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

    /**
     * 更新时间
     */
    private java.time.LocalDateTime updateTime;
}
