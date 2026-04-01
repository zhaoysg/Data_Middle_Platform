package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 脱敏策略明细实体
 */
@Data
@TableName("sec_mask_strategy_detail")
public class SecMaskStrategyDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 策略ID
     */
    private Long strategyId;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 字段名
     */
    private String columnName;

    /**
     * 脱敏模板编码
     */
    private String templateCode;

    /**
     * 输出别名
     */
    private String outputAlias;

    /**
     * 创建时间
     */
    private Date createTime;
}
