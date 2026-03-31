package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.DataDomain;

/**
 * 数据域业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DataDomain.class, reverseConvertGenerate = false)
public class DataDomainBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 数据域编码
     */
    @NotBlank(message = "数据域编码不能为空")
    @Size(max = 50, message = "数据域编码长度不能超过{max}个字符")
    private String domainCode;

    /**
     * 数据域名称
     */
    @NotBlank(message = "数据域名称不能为空")
    @Size(max = 100, message = "数据域名称长度不能超过{max}个字符")
    private String domainName;

    /**
     * 数据域描述
     */
    @Size(max = 500, message = "数据域描述长度不能超过{max}个字符")
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
    @Size(max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;

    /**
     * 状态：0-正常 1-停用
     */
    private String status;
}
