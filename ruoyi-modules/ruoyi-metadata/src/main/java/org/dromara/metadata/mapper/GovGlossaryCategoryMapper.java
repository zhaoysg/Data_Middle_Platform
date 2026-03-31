package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.GovGlossaryCategory;
import org.dromara.metadata.domain.vo.GovGlossaryCategoryVo;

/**
 * 治理 Glossary 分类Mapper接口
 */
@DS("bigdata")
public interface GovGlossaryCategoryMapper extends BaseMapperPlus<GovGlossaryCategory, GovGlossaryCategoryVo> {
}
