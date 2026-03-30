package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SecMaskWhitelist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 脱敏白名单 Mapper 接口
 */
@Mapper
public interface SecMaskWhitelistMapper extends BaseMapper<SecMaskWhitelist> {
}
