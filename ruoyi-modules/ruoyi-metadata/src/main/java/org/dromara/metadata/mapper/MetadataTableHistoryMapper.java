package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.MetadataTableHistory;
import org.dromara.metadata.domain.vo.MetadataTableHistoryVo;

/**
 * 元数据表历史快照Mapper接口
 */
@DS("bigdata")
public interface MetadataTableHistoryMapper extends BaseMapperPlus<MetadataTableHistory, MetadataTableHistoryVo> {
}
