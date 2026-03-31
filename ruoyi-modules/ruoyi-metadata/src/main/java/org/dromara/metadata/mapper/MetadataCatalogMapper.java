package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.MetadataCatalog;
import org.dromara.metadata.domain.vo.MetadataCatalogVo;

/**
 * 资产目录Mapper接口
 */
@DS("bigdata")
public interface MetadataCatalogMapper extends BaseMapperPlus<MetadataCatalog, MetadataCatalogVo> {

    /**
     * 根据目录编码查询
     */
    default MetadataCatalog selectByCatalogCode(String catalogCode) {
        return this.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetadataCatalog>()
                .eq(MetadataCatalog::getCatalogCode, catalogCode)
        );
    }
}
