package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DqcPlanRule;

/**
 * 数据质量方案-规则关联Mapper接口
 */
@DS("bigdata")
public interface DqcPlanRuleMapper extends BaseMapperPlus<DqcPlanRule, DqcPlanRule> {
}
