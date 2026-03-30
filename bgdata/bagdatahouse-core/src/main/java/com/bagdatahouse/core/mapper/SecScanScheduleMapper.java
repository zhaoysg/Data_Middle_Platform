package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SecScanSchedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感字段定时扫描任务 Mapper
 */
@Mapper
public interface SecScanScheduleMapper extends BaseMapper<SecScanSchedule> {
}
