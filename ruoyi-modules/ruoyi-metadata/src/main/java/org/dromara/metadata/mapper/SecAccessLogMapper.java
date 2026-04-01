package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.SecAccessLog;
import org.dromara.metadata.domain.vo.SecAccessLogVo;

/**
 * 脱敏访问日志Mapper接口
 */
@DS("bigdata")
public interface SecAccessLogMapper extends BaseMapperPlus<SecAccessLog, SecAccessLogVo> {
}
