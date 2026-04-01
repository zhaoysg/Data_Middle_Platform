package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.vo.SecSensitivityRuleVo;

import java.util.List;

/**
 * 敏感识别规则服务接口
 */
public interface ISecSensitivityRuleService {

    /**
     * 分页查询规则列表
     */
    TableDataInfo<SecSensitivityRuleVo> queryPageList(SecSensitivityRuleVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询规则
     */
    SecSensitivityRuleVo queryById(Long id);

    /**
     * 查询所有启用的规则列表
     */
    List<SecSensitivityRuleVo> listAllEnabled();

    /**
     * 查询所有规则列表
     */
    List<SecSensitivityRuleVo> listAll();

    /**
     * 新增规则
     */
    Long insert(SecSensitivityRuleVo vo);

    /**
     * 修改规则
     */
    int update(SecSensitivityRuleVo vo);

    /**
     * 删除规则
     */
    int deleteByIds(Long[] ids);
}
