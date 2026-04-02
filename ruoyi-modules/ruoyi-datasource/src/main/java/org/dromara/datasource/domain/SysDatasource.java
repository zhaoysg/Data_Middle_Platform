package org.dromara.datasource.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 数据源配置表 sys_datasource
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_datasource")
public class SysDatasource extends TenantEntity {

    /**
     * 数据源ID
     */
    @TableId(value = "ds_id")
    private Long dsId;

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 数据源编码
     */
    private String dsCode;

    /**
     * 数据源类型：MYSQL / SQLSERVER / ORACLE / TIDB / POSTGRESQL
     */
    private String dsType;

    /**
     * 主机地址
     */
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
     * 数据来源：K3DC / K3HW / K1 / K2 / OTHER
     */
    private String dataSource;

    /**
     * 数据源标识：0-内部 1-外部
     */
    private String dsFlag;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

    public SysDatasource(Long dsId) {
        this.dsId = dsId;
    }
}
