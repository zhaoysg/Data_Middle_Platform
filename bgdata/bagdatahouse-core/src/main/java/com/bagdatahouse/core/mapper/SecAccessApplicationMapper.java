package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SecAccessApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感字段访问申请 Mapper 接口
 */
@Mapper
public interface SecAccessApplicationMapper extends BaseMapper<SecAccessApplication> {
}
