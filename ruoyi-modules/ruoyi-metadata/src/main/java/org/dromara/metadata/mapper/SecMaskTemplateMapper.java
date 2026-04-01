package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.SecMaskTemplate;
import org.dromara.metadata.domain.vo.SecMaskTemplateVo;

/**
 * 脱敏模板Mapper接口
 */
@DS("bigdata")
public interface SecMaskTemplateMapper extends BaseMapperPlus<SecMaskTemplate, SecMaskTemplateVo> {
}
