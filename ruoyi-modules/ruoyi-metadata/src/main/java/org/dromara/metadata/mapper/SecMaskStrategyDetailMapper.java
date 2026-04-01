package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.SecMaskStrategyDetail;
import org.dromara.metadata.domain.vo.SecMaskStrategyDetailVo;

import java.util.List;

/**
 * 脱敏策略明细Mapper接口
 */
@DS("bigdata")
public interface SecMaskStrategyDetailMapper extends BaseMapperPlus<SecMaskStrategyDetail, SecMaskStrategyDetailVo> {

    /**
     * 根据策略ID查询明细列表
     */
    @Select("SELECT * FROM sec_mask_strategy_detail WHERE strategy_id = #{strategyId}")
    List<SecMaskStrategyDetail> selectByStrategyId(@Param("strategyId") Long strategyId);
}
