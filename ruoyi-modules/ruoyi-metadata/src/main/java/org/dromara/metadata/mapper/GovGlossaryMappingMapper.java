package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.GovGlossaryMapping;
import org.dromara.metadata.domain.vo.GovGlossaryMappingVo;

/**
 * 治理 Glossary 映射Mapper接口
 */
@DS("bigdata")
public interface GovGlossaryMappingMapper extends BaseMapperPlus<GovGlossaryMapping, GovGlossaryMappingVo> {
}
