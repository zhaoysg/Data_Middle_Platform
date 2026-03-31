package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.GovGlossaryMapping;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治理 Glossary 映射视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = GovGlossaryMapping.class)
public class GovGlossaryMappingVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 术语ID
     */
    private Long termId;

    /**
     * 术语名称
     */
    private String termName;

    /**
     * 资产类型：TABLE/COLUMN
     */
    private String assetType;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 列名
     */
    private String columnName;

    /**
     * 映射类型：CONTAINS/DEFINES/REFERENCES
     */
    private String mappingType;

    /**
     * 置信度：0-100
     */
    private Integer confidence;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

    /**
     * 更新时间
     */
    private java.time.LocalDateTime updateTime;
}
