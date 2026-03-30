package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SecMaskTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 静态脱敏任务 Mapper 接口
 */
@Mapper
public interface SecMaskTaskMapper extends BaseMapper<SecMaskTask> {
}
