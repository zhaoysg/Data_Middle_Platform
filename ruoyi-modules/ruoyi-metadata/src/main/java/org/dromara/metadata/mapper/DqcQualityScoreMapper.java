package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DqcQualityScore;
import org.dromara.metadata.domain.vo.DqcQualityScoreVo;

/**
 * 数据质量评分Mapper接口
 */
@DS("bigdata")
public interface DqcQualityScoreMapper extends BaseMapperPlus<DqcQualityScore, DqcQualityScoreVo> {
}
