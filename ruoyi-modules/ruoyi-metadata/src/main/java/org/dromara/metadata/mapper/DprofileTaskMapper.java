package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DprofileTask;
import org.dromara.metadata.domain.vo.DprofileTaskVo;

/**
 * 数据探查任务Mapper接口
 */
@DS("bigdata")
public interface DprofileTaskMapper extends BaseMapperPlus<DprofileTask, DprofileTaskVo> {
}
