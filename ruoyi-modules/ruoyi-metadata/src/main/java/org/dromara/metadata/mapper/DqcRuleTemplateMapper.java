package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DqcRuleTemplate;
import org.dromara.metadata.domain.vo.DqcRuleTemplateVo;

/**
 * 数据质量规则模板Mapper接口
 */
@DS("bigdata")
public interface DqcRuleTemplateMapper extends BaseMapperPlus<DqcRuleTemplate, DqcRuleTemplateVo> {
}
