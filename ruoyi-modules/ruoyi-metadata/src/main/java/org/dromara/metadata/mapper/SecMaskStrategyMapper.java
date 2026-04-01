package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.SecMaskStrategy;
import org.dromara.metadata.domain.vo.SecMaskStrategyVo;

/**
 * 脱敏策略Mapper接口
 */
@DS("bigdata")
public interface SecMaskStrategyMapper extends BaseMapperPlus<SecMaskStrategy, SecMaskStrategyVo> {
}
