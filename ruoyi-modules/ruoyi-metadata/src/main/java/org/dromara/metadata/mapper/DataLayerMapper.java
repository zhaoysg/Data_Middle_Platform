package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DataLayer;
import org.dromara.metadata.domain.vo.DataLayerVo;

/**
 * 数仓分层Mapper接口
 */
@DS("bigdata")
public interface DataLayerMapper extends BaseMapperPlus<DataLayer, DataLayerVo> {

    /**
     * 根据分层编码查询
     */
    default DataLayer selectByLayerCode(String layerCode) {
        return this.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DataLayer>()
                .eq(DataLayer::getLayerCode, layerCode)
        );
    }
}
