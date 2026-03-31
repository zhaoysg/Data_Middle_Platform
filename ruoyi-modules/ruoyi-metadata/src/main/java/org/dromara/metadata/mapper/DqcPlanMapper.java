package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DqcPlan;
import org.dromara.metadata.domain.vo.DqcPlanVo;

/**
 * 数据质量检查方案Mapper接口
 */
@DS("bigdata")
public interface DqcPlanMapper extends BaseMapperPlus<DqcPlan, DqcPlanVo> {
}
