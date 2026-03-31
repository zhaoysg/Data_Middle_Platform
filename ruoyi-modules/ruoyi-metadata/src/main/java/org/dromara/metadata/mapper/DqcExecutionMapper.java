package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DqcExecution;
import org.dromara.metadata.domain.vo.DqcExecutionVo;

/**
 * 数据质量执行记录Mapper接口
 */
@DS("bigdata")
public interface DqcExecutionMapper extends BaseMapperPlus<DqcExecution, DqcExecutionVo> {
}
