package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SecMaskStrategy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 脱敏策略 Mapper 接口
 */
@Mapper
public interface SecMaskStrategyMapper extends BaseMapper<SecMaskStrategy> {
}
