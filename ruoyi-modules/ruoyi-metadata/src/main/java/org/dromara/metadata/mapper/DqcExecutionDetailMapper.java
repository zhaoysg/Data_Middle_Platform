package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.vo.DqcExecutionDetailVo;

/**
 * 数据质量执行明细Mapper接口
 */
@DS("bigdata")
public interface DqcExecutionDetailMapper extends BaseMapperPlus<DqcExecutionDetail, DqcExecutionDetailVo> {
}
