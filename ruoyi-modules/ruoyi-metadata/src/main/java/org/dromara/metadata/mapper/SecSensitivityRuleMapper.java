package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.SecSensitivityRule;
import org.dromara.metadata.domain.vo.SecSensitivityRuleVo;

/**
 * 敏感识别规则Mapper接口
 */
@DS("bigdata")
public interface SecSensitivityRuleMapper extends BaseMapperPlus<SecSensitivityRule, SecSensitivityRuleVo> {
}
