package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.MetadataScanLog;
import org.dromara.metadata.domain.vo.MetadataScanLogVo;

/**
 * 扫描记录Mapper接口
 */
@DS("bigdata")
public interface MetadataScanLogMapper extends BaseMapperPlus<MetadataScanLog, MetadataScanLogVo> {
}
