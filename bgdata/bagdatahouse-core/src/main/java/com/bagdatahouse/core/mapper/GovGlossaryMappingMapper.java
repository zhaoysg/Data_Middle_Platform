package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.GovGlossaryMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 术语-字段映射 Mapper 接口
 */
@Mapper
public interface GovGlossaryMappingMapper extends BaseMapper<GovGlossaryMapping> {
}
