package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.SecClassification;
import org.dromara.metadata.domain.vo.SecClassificationVo;

/**
 * 数据分类Mapper接口
 */
@DS("bigdata")
public interface SecClassificationMapper extends BaseMapperPlus<SecClassification, SecClassificationVo> {
}
