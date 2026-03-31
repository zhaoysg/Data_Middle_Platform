package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import com.baomidou.mybatisplus.annotation.TableField;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.MetadataCatalog;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资产目录视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = MetadataCatalog.class)
public class MetadataCatalogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String catalogName;
    private String catalogCode;
    private String catalogType;
    private Long parentId;
    private String parentName;
    @TableField("sort_order")
    private Integer sortOrder;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;

    /** 子目录（树形构建用） */
    private List<MetadataCatalogVo> children;
}
