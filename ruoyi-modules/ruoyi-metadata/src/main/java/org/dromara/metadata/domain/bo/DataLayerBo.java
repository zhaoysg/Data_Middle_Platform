package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.DataLayer;

/**
 * 数仓分层业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DataLayer.class, reverseConvertGenerate = false)
public class DataLayerBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 分层编码：ODS / DWD / DWS / ADS
     */
    @NotBlank(message = "分层编码不能为空")
    @Size(max = 20, message = "分层编码长度不能超过{max}个字符")
    private String layerCode;

    /**
     * 分层名称
     */
    @NotBlank(message = "分层名称不能为空")
    @Size(max = 50, message = "分层名称长度不能超过{max}个字符")
    private String layerName;

    /**
     * 分层描述
     */
    @Size(max = 200, message = "分层描述长度不能超过{max}个字符")
    private String layerDesc;

    /**
     * 展示颜色
     */
    @Size(max = 20, message = "颜色长度不能超过{max}个字符")
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
    @Size(max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;
}
