package org.dromara.metadata.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.metadata.domain.DprofileColumnReport;

import java.math.BigDecimal;

/**
 * 列级探查报告视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DprofileColumnReportVo extends DprofileColumnReport {

    /**
     * 报告名称（表名.列名）
     */
    private String fullColumnName;

    /**
     * 空值率文本
     */
    private String nullRateText;

    /**
     * 唯一率文本
     */
    private String uniqueRateText;
}
