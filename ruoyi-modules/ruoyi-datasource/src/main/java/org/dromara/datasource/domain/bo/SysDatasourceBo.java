package org.dromara.datasource.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.datasource.domain.SysDatasource;

/**
 * 数据源信息业务对象 sys_datasource
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysDatasource.class, reverseConvertGenerate = false)
public class SysDatasourceBo extends BaseEntity {

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 数据源名称
     */
    @NotBlank(message = "数据源名称不能为空")
    @Size(max = 100, message = "数据源名称长度不能超过{max}个字符")
    private String dsName;

    /**
     * 数据源编码
     */
    @Size(max = 50, message = "数据源编码长度不能超过{max}个字符")
    private String dsCode;

    /**
     * 数据源类型：MYSQL / SQLSERVER / ORACLE / TIDB / POSTGRESQL
     */
    @NotBlank(message = "数据源类型不能为空")
    private String dsType;

    /**
     * 主机地址
     */
    @NotBlank(message = "主机地址不能为空")
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 数据库名
     */
    private String databaseName;

    /**
     * Schema名称（用于PostgreSQL等）
     */
    private String schemaName;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 额外连接参数JSON
     */
    private String connectionParams;

    /**
     * 数仓层标记：ODS / DWD / DWS / ADS
     */
    private String dataLayer;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否测试连接（不持久化）
     */
    private Boolean testConnection;

    public SysDatasourceBo(Long dsId) {
        this.dsId = dsId;
    }
}
