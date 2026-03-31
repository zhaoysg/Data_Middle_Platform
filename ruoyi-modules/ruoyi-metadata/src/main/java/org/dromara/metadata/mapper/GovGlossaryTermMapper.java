package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.GovGlossaryTerm;
import org.dromara.metadata.domain.vo.GovGlossaryTermVo;

/**
 * 治理 Glossary 术语Mapper接口
 */
@DS("bigdata")
public interface GovGlossaryTermMapper extends BaseMapperPlus<GovGlossaryTerm, GovGlossaryTermVo> {
}
