package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.MetadataScanSchedule;
import org.dromara.metadata.domain.vo.MetadataScanScheduleVo;

/**
 * 元数据扫描调度Mapper接口
 */
@DS("bigdata")
public interface MetadataScanScheduleMapper extends BaseMapperPlus<MetadataScanSchedule, MetadataScanScheduleVo> {
}
