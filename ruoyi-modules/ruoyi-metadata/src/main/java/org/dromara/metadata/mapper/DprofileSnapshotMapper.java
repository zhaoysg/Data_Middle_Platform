package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DprofileSnapshot;
import org.dromara.metadata.domain.vo.DprofileSnapshotVo;

/**
 * 数据探查快照Mapper接口
 */
@DS("bigdata")
public interface DprofileSnapshotMapper extends BaseMapperPlus<DprofileSnapshot, DprofileSnapshotVo> {
}
