package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SecMaskTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 脱敏规则模板 Mapper 接口
 */
@Mapper
public interface SecMaskTemplateMapper extends BaseMapper<SecMaskTemplate> {
}
