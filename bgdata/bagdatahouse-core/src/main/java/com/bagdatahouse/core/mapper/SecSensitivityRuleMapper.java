package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SecSensitivityRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感字段识别规则 Mapper 接口
 */
@Mapper
public interface SecSensitivityRuleMapper extends BaseMapper<SecSensitivityRule> {
}
