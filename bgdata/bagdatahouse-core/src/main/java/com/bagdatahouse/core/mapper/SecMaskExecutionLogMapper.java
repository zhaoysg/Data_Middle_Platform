package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SecMaskExecutionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 脱敏执行日志 Mapper 接口
 */
@Mapper
public interface SecMaskExecutionLogMapper extends BaseMapper<SecMaskExecutionLog> {
}
