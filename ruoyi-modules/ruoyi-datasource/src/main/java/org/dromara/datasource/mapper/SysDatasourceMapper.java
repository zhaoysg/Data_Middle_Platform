package org.dromara.datasource.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.datasource.domain.SysDatasource;
import org.dromara.datasource.domain.vo.SysDatasourceVo;

/**
 * 数据源表 Mapper接口
 *
 * @author Lion Li
 */
@DS("bigdata")
public interface SysDatasourceMapper extends BaseMapperPlus<SysDatasource, SysDatasourceVo> {

    /**
     * 分页查询数据源列表
     *
     * @param queryWrapper 查询条件
     * @return 数据源分页列表
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    SysDatasourceVo selectDatasourceDetail(@Param(Constants.WRAPPER) Wrapper<SysDatasource> queryWrapper);

    /**
     * 根据编码查询数据源
     *
     * @param dsCode 数据源编码
     * @return 数据源信息
     */
    default SysDatasource selectByDsCode(String dsCode) {
        return this.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDatasource>()
                .eq(SysDatasource::getDsCode, dsCode)
        );
    }
}
