package org.dromara.metadata.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.SecMaskTemplate;
import org.dromara.metadata.domain.vo.SecMaskTemplateVo;
import org.dromara.metadata.mapper.SecMaskTemplateMapper;
import org.dromara.metadata.service.ISecMaskTemplateService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 脱敏模板服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SecMaskTemplateServiceImpl implements ISecMaskTemplateService {

    private final SecMaskTemplateMapper baseMapper;

    @Override
    public TableDataInfo<SecMaskTemplateVo> queryPageList(SecMaskTemplateVo vo, PageQuery pageQuery) {
        var wrapper = Wrappers.<SecMaskTemplate>lambdaQuery()
            .like(StringUtils.isNotBlank(vo.getTemplateName()), SecMaskTemplate::getTemplateName, vo.getTemplateName())
            .like(StringUtils.isNotBlank(vo.getTemplateCode()), SecMaskTemplate::getTemplateCode, vo.getTemplateCode())
            .eq(StringUtils.isNotBlank(vo.getTemplateType()), SecMaskTemplate::getTemplateType, vo.getTemplateType())
            .eq(StringUtils.isNotBlank(vo.getEnabled()), SecMaskTemplate::getEnabled, vo.getEnabled())
            .orderByDesc(SecMaskTemplate::getCreateTime);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SecMaskTemplateVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<SecMaskTemplateVo> listAllEnabled() {
        return baseMapper.selectVoList(
            Wrappers.<SecMaskTemplate>lambdaQuery()
                .eq(SecMaskTemplate::getEnabled, "1")
                .orderByAsc(SecMaskTemplate::getId)
        );
    }

    @Override
    public List<SecMaskTemplateVo> listAll() {
        return baseMapper.selectVoList(
            Wrappers.<SecMaskTemplate>lambdaQuery()
                .orderByAsc(SecMaskTemplate::getId)
        );
    }

    @Override
    public Map<String, String> getTemplateExprMap() {
        List<SecMaskTemplate> templates = baseMapper.selectList(
            Wrappers.<SecMaskTemplate>lambdaQuery()
                .eq(SecMaskTemplate::getEnabled, "1")
        );
        Map<String, String> exprMap = new HashMap<>();
        for (SecMaskTemplate t : templates) {
            exprMap.put(t.getTemplateCode(), t.getMaskExpr());
        }
        return exprMap;
    }

    @Override
    public Long insert(SecMaskTemplateVo vo) {
        if (StringUtils.isNotBlank(vo.getTemplateCode())) {
            boolean exist = baseMapper.exists(Wrappers.<SecMaskTemplate>lambdaQuery()
                .eq(SecMaskTemplate::getTemplateCode, vo.getTemplateCode()));
            if (exist) {
                throw new ServiceException("模板编码已存在");
            }
        }
        SecMaskTemplate entity = new SecMaskTemplate();
        entity.setTemplateCode(vo.getTemplateCode());
        entity.setTemplateName(vo.getTemplateName());
        entity.setTemplateType(vo.getTemplateType());
        entity.setMaskExpr(vo.getMaskExpr());
        entity.setMaskChar(vo.getMaskChar());
        entity.setMaskPosition(vo.getMaskPosition());
        entity.setMaskHeadKeep(vo.getMaskHeadKeep());
        entity.setMaskTailKeep(vo.getMaskTailKeep());
        entity.setMaskPattern(vo.getMaskPattern());
        entity.setTemplateDesc(vo.getTemplateDesc());
        entity.setBuiltin(vo.getBuiltin() != null ? vo.getBuiltin() : "0");
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : "1");
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int update(SecMaskTemplateVo vo) {
        if (vo.getId() == null) {
            throw new ServiceException("模板ID不能为空");
        }
        if (StringUtils.isNotBlank(vo.getTemplateCode())) {
            boolean exist = baseMapper.exists(Wrappers.<SecMaskTemplate>lambdaQuery()
                .eq(SecMaskTemplate::getTemplateCode, vo.getTemplateCode())
                .ne(SecMaskTemplate::getId, vo.getId()));
            if (exist) {
                throw new ServiceException("模板编码已存在");
            }
        }
        SecMaskTemplate entity = new SecMaskTemplate();
        entity.setId(vo.getId());
        entity.setTemplateCode(vo.getTemplateCode());
        entity.setTemplateName(vo.getTemplateName());
        entity.setTemplateType(vo.getTemplateType());
        entity.setMaskExpr(vo.getMaskExpr());
        entity.setMaskChar(vo.getMaskChar());
        entity.setMaskPosition(vo.getMaskPosition());
        entity.setMaskHeadKeep(vo.getMaskHeadKeep());
        entity.setMaskTailKeep(vo.getMaskTailKeep());
        entity.setMaskPattern(vo.getMaskPattern());
        entity.setTemplateDesc(vo.getTemplateDesc());
        entity.setBuiltin(vo.getBuiltin());
        entity.setEnabled(vo.getEnabled());
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }
}
