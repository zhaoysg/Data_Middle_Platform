package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.metadata.domain.DataDomain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 数据域视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DataDomain.class)
public class DataDomainVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
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
     * 部门名称
     */
    @Translation(type = TransConstant.DEPT_ID_TO_NAME, mapper = "deptId")
    private String deptName;

    /**
     * 负责人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "ownerId")
    private String ownerName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态
     */
    private String status;

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
