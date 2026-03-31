package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.vo.DqcRuleDefVo;

/**
 * 数据质量规则定义Mapper接口
 */
@DS("bigdata")
public interface DqcRuleDefMapper extends BaseMapperPlus<DqcRuleDef, DqcRuleDefVo> {
}
