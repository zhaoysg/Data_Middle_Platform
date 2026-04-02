package org.dromara.datasource.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.datasource.domain.SysDatasource;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 数据源信息视图对象 sys_datasource
 *
 * @author Lion Li
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysDatasource.class)
public class SysDatasourceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据源ID
     */
    @ExcelProperty(value = "数据源序号")
    private Long dsId;

    /**
     * 数据源名称
     */
    @ExcelProperty(value = "数据源名称")
    private String dsName;

    /**
     * 数据源编码
     */
    @ExcelProperty(value = "数据源编码")
    private String dsCode;

    /**
     * 数据源类型
     */
    @ExcelProperty(value = "数据源类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_datasource_type")
    private String dsType;

    /**
     * 主机地址
     */
    @ExcelProperty(value = "主机地址")
    private String host;

    /**
     * 端口
     */
    @ExcelProperty(value = "端口")
    private Integer port;

    /**
     * 数据库名
     */
    @ExcelProperty(value = "数据库名")
    private String databaseName;

    /**
     * Schema名称
     */
    @ExcelProperty(value = "Schema名称")
    private String schemaName;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 额外连接参数
     */
    private String connectionParams;

    /**
     * 数仓层标记
     */
    @ExcelProperty(value = "数仓层", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_datasource_layer")
    private String dataLayer;

    /**
     * 数据来源
     */
    @ExcelProperty(value = "数据来源")
    private String dataSource;

    /**
     * 数据源标识：0-内部 1-外部
     */
    @ExcelProperty(value = "数据源标识")
    private String dsFlag;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    @ExcelProperty(value = "部门名称")
    private String deptName;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 更新者
     */
    private String updateBy;
}
