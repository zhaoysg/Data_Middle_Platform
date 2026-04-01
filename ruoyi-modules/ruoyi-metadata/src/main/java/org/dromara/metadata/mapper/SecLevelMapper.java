package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.SecLevel;
import org.dromara.metadata.domain.vo.SecLevelVo;

/**
 * 敏感等级Mapper接口
 */
@DS("bigdata")
public interface SecLevelMapper extends BaseMapperPlus<SecLevel, SecLevelVo> {
}
