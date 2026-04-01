package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DprofileColumnReport;
import org.dromara.metadata.domain.vo.DprofileColumnReportVo;

/**
 * 列级探查报告Mapper接口
 */
@DS("bigdata")
public interface DprofileColumnReportMapper extends BaseMapperPlus<DprofileColumnReport, DprofileColumnReportVo> {
}
