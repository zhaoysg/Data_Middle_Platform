package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.GovLineage;
import org.dromara.metadata.domain.vo.GovLineageVo;

/**
 * 治理血缘关系Mapper接口
 */
@DS("bigdata")
public interface GovLineageMapper extends BaseMapperPlus<GovLineage, GovLineageVo> {
}
