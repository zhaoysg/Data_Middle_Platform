package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SecAccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感字段访问审计日志 Mapper 接口
 */
@Mapper
public interface SecAccessLogMapper extends BaseMapper<SecAccessLog> {
}
