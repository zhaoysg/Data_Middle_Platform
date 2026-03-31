package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.DataDomain;
import org.dromara.metadata.domain.vo.DataDomainVo;

/**
 * 数据域Mapper接口
 */
@DS("bigdata")
public interface DataDomainMapper extends BaseMapperPlus<DataDomain, DataDomainVo> {

    /**
     * 根据数据域编码查询
     */
    default DataDomain selectByDomainCode(String domainCode) {
        return this.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DataDomain>()
                .eq(DataDomain::getDomainCode, domainCode)
        );
    }
}
