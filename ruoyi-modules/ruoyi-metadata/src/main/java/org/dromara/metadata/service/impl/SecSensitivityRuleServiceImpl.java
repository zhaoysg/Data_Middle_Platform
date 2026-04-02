package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.SecSensitivityRule;
import org.dromara.metadata.domain.vo.SecSensitivityRuleVo;
import org.dromara.metadata.mapper.SecSensitivityRuleMapper;
import org.dromara.metadata.service.ISecSensitivityRuleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 敏感识别规则服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class SecSensitivityRuleServiceImpl implements ISecSensitivityRuleService {

    private final SecSensitivityRuleMapper baseMapper;

    @Override
    public TableDataInfo<SecSensitivityRuleVo> queryPageList(SecSensitivityRuleVo vo, PageQuery pageQuery) {
        var wrapper = Wrappers.<SecSensitivityRule>lambdaQuery()
            .like(StringUtils.isNotBlank(vo.getRuleName()), SecSensitivityRule::getRuleName, vo.getRuleName())
            .like(StringUtils.isNotBlank(vo.getRuleCode()), SecSensitivityRule::getRuleCode, vo.getRuleCode())
            .eq(StringUtils.isNotBlank(vo.getRuleType()), SecSensitivityRule::getRuleType, vo.getRuleType())
            .eq(StringUtils.isNotBlank(vo.getTargetLevelCode()), SecSensitivityRule::getTargetLevelCode, vo.getTargetLevelCode())
            .eq(StringUtils.isNotBlank(vo.getTargetClassCode()), SecSensitivityRule::getTargetClassCode, vo.getTargetClassCode())
            .eq(StringUtils.isNotBlank(vo.getEnabled()), SecSensitivityRule::getEnabled, vo.getEnabled())
            .orderByDesc(SecSensitivityRule::getCreateTime);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SecSensitivityRuleVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<SecSensitivityRuleVo> listAllEnabled() {
        return baseMapper.selectVoList(
            Wrappers.<SecSensitivityRule>lambdaQuery()
                .eq(SecSensitivityRule::getEnabled, "1")
                .orderByAsc(SecSensitivityRule::getId)
        );
    }

    @Override
    public List<SecSensitivityRuleVo> listAll() {
        return baseMapper.selectVoList(
            Wrappers.<SecSensitivityRule>lambdaQuery()
                .orderByAsc(SecSensitivityRule::getId)
        );
    }

    @Override
    public Long insert(SecSensitivityRuleVo vo) {
        if (StringUtils.isNotBlank(vo.getRuleCode())) {
            boolean exist = baseMapper.exists(Wrappers.<SecSensitivityRule>lambdaQuery()
                .eq(SecSensitivityRule::getRuleCode, vo.getRuleCode()));
            if (exist) {
                throw new ServiceException("规则编码已存在");
            }
        }
        SecSensitivityRule entity = new SecSensitivityRule();
        entity.setRuleCode(vo.getRuleCode());
        entity.setRuleName(vo.getRuleName());
        entity.setRuleType(vo.getRuleType());
        entity.setRuleExpr(vo.getRuleExpr());
        entity.setTargetLevelCode(vo.getTargetLevelCode());
        entity.setTargetClassCode(vo.getTargetClassCode());
        entity.setBuiltin(vo.getBuiltin() != null ? vo.getBuiltin() : "0");
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : "1");
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int update(SecSensitivityRuleVo vo) {
        if (vo.getId() == null) {
            throw new ServiceException("规则ID不能为空");
        }
        if (StringUtils.isNotBlank(vo.getRuleCode())) {
            boolean exist = baseMapper.exists(Wrappers.<SecSensitivityRule>lambdaQuery()
                .eq(SecSensitivityRule::getRuleCode, vo.getRuleCode())
                .ne(SecSensitivityRule::getId, vo.getId()));
            if (exist) {
                throw new ServiceException("规则编码已存在");
            }
        }
        SecSensitivityRule entity = new SecSensitivityRule();
        entity.setId(vo.getId());
        entity.setRuleCode(vo.getRuleCode());
        entity.setRuleName(vo.getRuleName());
        entity.setRuleType(vo.getRuleType());
        entity.setRuleExpr(vo.getRuleExpr());
        entity.setTargetLevelCode(vo.getTargetLevelCode());
        entity.setTargetClassCode(vo.getTargetClassCode());
        entity.setBuiltin(vo.getBuiltin());
        entity.setEnabled(vo.getEnabled());
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }
}
