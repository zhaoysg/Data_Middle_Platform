package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 脱敏访问日志实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sec_access_log")
public class SecAccessLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 访问用户ID
     */
    private Long userId;

    /**
     * 访问用户名
     */
    private String userName;

    /**
     * 查询SQL
     */
    private String querySql;

    /**
     * 脱敏后SQL
     */
    private String maskedSql;

    /**
     * 返回行数
     */
    private Integer resultRows;

    /**
     * 脱敏字段列表 JSON
     */
    private String maskedColumns;

    /**
     * 耗时
     */
    private Long elapsedMs;

    /**
     * 状态 SUCCESS/FAILED
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 客户端IP
     */
    private String ipAddress;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
