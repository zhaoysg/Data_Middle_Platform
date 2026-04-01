package org.dromara.metadata.domain.vo;

import lombok.Data;
import org.dromara.metadata.domain.DqcQualityScore;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 数据质量评分VO
 */
@Data
public class DqcQualityScoreVo extends DqcQualityScore {

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 数据源编码
     */
    private String dsCode;
}
