package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.SecColumnSensitivity;
import org.dromara.metadata.domain.vo.SecColumnSensitivityVo;

/**
 * 字段敏感记录Mapper接口
 */
@DS("bigdata")
public interface SecColumnSensitivityMapper extends BaseMapperPlus<SecColumnSensitivity, SecColumnSensitivityVo> {
}
