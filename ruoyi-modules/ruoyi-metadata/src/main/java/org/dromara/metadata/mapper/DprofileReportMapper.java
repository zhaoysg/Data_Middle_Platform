package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DprofileReport;
import org.dromara.metadata.domain.vo.DprofileReportVo;

/**
 * 数据探查报告Mapper接口
 */
@DS("bigdata")
public interface DprofileReportMapper extends BaseMapperPlus<DprofileReport, DprofileReportVo> {
}
