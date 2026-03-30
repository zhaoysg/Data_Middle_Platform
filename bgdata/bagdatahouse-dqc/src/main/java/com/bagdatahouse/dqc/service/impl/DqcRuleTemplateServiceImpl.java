package com.bagdatahouse.dqc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqcRuleTemplateDTO;
import com.bagdatahouse.core.entity.DqcRuleTemplate;
import com.bagdatahouse.core.mapper.DqcRuleTemplateMapper;
import com.bagdatahouse.dqc.service.DqcRuleTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DQC规则模板服务实现
 */
@Service
public class DqcRuleTemplateServiceImpl extends ServiceImpl<DqcRuleTemplateMapper, DqcRuleTemplate>
        implements DqcRuleTemplateService {

    private static final Logger log = LoggerFactory.getLogger(DqcRuleTemplateServiceImpl.class);

    @Override
    public Result<Page<DqcRuleTemplate>> page(Integer pageNum, Integer pageSize, String templateName,
                                               String ruleType, String applyLevel, Integer builtin) {
        Page<DqcRuleTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DqcRuleTemplate> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(templateName)) {
            wrapper.like(DqcRuleTemplate::getTemplateName, templateName);
        }
        if (StringUtils.hasText(ruleType)) {
            wrapper.eq(DqcRuleTemplate::getRuleType, ruleType);
        }
        if (StringUtils.hasText(applyLevel)) {
            wrapper.eq(DqcRuleTemplate::getApplyLevel, applyLevel);
        }
        if (builtin != null) {
            wrapper.eq(DqcRuleTemplate::getBuiltin, builtin);
        }

        wrapper.orderByDesc(DqcRuleTemplate::getCreateTime);
        Page<DqcRuleTemplate> result = this.page(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<DqcRuleTemplate> getById(Long id) {
        DqcRuleTemplate entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(3003, "规则模板不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<Long> create(DqcRuleTemplateDTO dto) {
        if (!StringUtils.hasText(dto.getTemplateCode())) {
            throw new BusinessException(400, "模板编码不能为空");
        }

        LambdaQueryWrapper<DqcRuleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcRuleTemplate::getTemplateCode, dto.getTemplateCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(400, "模板编码已存在");
        }

        DqcRuleTemplate entity = toEntity(dto);
        entity.setId(null);
        entity.setCreateUser(1L);
        entity.setCreateTime(LocalDateTime.now());
        entity.setEnabled(true);
        entity.setBuiltin(false);
        this.save(entity);

        return Result.success(entity.getId());
    }

    @Override
    public Result<Void> update(Long id, DqcRuleTemplateDTO dto) {
        DqcRuleTemplate existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(3003, "规则模板不存在");
        }

        if (Boolean.TRUE.equals(existing.getBuiltin()) && StringUtils.hasText(dto.getTemplateCode())
                && !dto.getTemplateCode().equals(existing.getTemplateCode())) {
            throw new BusinessException(3003, "内置模板禁止修改模板编码");
        }

        if (StringUtils.hasText(dto.getTemplateCode()) && !dto.getTemplateCode().equals(existing.getTemplateCode())) {
            LambdaQueryWrapper<DqcRuleTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DqcRuleTemplate::getTemplateCode, dto.getTemplateCode());
            wrapper.ne(DqcRuleTemplate::getId, id);
            if (this.count(wrapper) > 0) {
                throw new BusinessException(400, "模板编码已存在");
            }
        }

        DqcRuleTemplate entity = toEntity(dto);
        entity.setId(id);
        entity.setUpdateUser(1L);
        entity.setUpdateTime(LocalDateTime.now());
        entity.setCreateUser(existing.getCreateUser());
        entity.setCreateTime(existing.getCreateTime());
        this.updateById(entity);

        return Result.success();
    }

    @Override
    public Result<Void> delete(Long id) {
        DqcRuleTemplate existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(3003, "规则模板不存在");
        }
        if (Boolean.TRUE.equals(existing.getBuiltin())) {
            throw new BusinessException(3003, "内置模板禁止删除");
        }
        if (!this.removeById(id)) {
            throw new BusinessException(3003, "规则模板不存在");
        }
        return Result.success();
    }

    @Override
    public Result<List<DqcRuleTemplate>> listEnabled() {
        LambdaQueryWrapper<DqcRuleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcRuleTemplate::getEnabled, true);
        wrapper.orderByAsc(DqcRuleTemplate::getTemplateName);
        List<DqcRuleTemplate> list = this.list(wrapper);
        return Result.success(list);
    }

    @Override
    public Result<Map<String, List<DqcRuleTemplate>>> listGroupedByLevel() {
        LambdaQueryWrapper<DqcRuleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqcRuleTemplate::getEnabled, true);
        wrapper.orderByAsc(DqcRuleTemplate::getApplyLevel, DqcRuleTemplate::getTemplateName);
        List<DqcRuleTemplate> list = this.list(wrapper);

        Map<String, List<DqcRuleTemplate>> grouped = list.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getApplyLevel() != null ? t.getApplyLevel() : "UNKNOWN"
                ));

        return Result.success(grouped);
    }

    private DqcRuleTemplate toEntity(DqcRuleTemplateDTO dto) {
        return DqcRuleTemplate.builder()
                .id(dto.getId())
                .templateCode(dto.getTemplateCode())
                .templateName(dto.getTemplateName())
                .templateDesc(dto.getTemplateDesc())
                .ruleType(dto.getRuleType())
                .applyLevel(dto.getApplyLevel())
                .defaultExpr(dto.getDefaultExpr())
                .defaultThreshold(dto.getDefaultThreshold())
                .paramSpec(dto.getParamSpec())
                .dimension(dto.getDimension())
                .builtin(dto.getBuiltin())
                .enabled(dto.getEnabled())
                .build();
    }
}
