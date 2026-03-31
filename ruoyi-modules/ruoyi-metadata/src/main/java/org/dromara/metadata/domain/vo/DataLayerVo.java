package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.DataLayer;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 数仓分层视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DataLayer.class)
public class DataLayerVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 分层编码
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
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;
}
