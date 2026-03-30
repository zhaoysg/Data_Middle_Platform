package com.bagdatahouse.governance.glossary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovGlossaryMapping;
import com.bagdatahouse.core.entity.GovGlossaryTerm;
import com.bagdatahouse.core.entity.SysDept;
import com.bagdatahouse.core.entity.SysUser;
import com.bagdatahouse.core.mapper.GovGlossaryMappingMapper;
import com.bagdatahouse.core.mapper.GovGlossaryTermMapper;
import com.bagdatahouse.core.mapper.SysDeptMapper;
import com.bagdatahouse.core.mapper.SysUserMapper;
import com.bagdatahouse.governance.glossary.dto.GlossaryTermQueryDTO;
import com.bagdatahouse.governance.glossary.dto.GlossaryTermSaveDTO;
import com.bagdatahouse.governance.glossary.enums.GlossaryTermStatusEnum;
import com.bagdatahouse.governance.glossary.service.GlossaryMappingService;
import com.bagdatahouse.governance.glossary.service.GlossaryTermService;
import com.bagdatahouse.governance.glossary.vo.GlossaryMappingVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryStatsVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryTermDetailVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryTermVO;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 术语管理服务实现
 */
@Slf4j
@Service
public class GlossaryTermServiceImpl extends ServiceImpl<GovGlossaryTermMapper, GovGlossaryTerm>
        implements GlossaryTermService {

    @Autowired
    private GovGlossaryTermMapper termMapper;

    @Autowired
    private GovGlossaryMappingMapper mappingMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private GlossaryMappingService mappingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GovGlossaryTerm> save(GlossaryTermSaveDTO dto) {
        validateDto(dto, null);

        GovGlossaryTerm term = new GovGlossaryTerm();
        BeanUtils.copyProperties(dto, term);
        term.setStatus(dto.getStatus() != null ? dto.getStatus() : GlossaryTermStatusEnum.DRAFT.getCode());
        term.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
        term.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        term.setCreateTime(LocalDateTime.now());
        termMapper.insert(term);

        if (dto.getMappings() != null && !dto.getMappings().isEmpty()) {
            mappingService.batchSave(term.getId(), dto.getMappings(), dto.getCreateUser());
        }

        return Result.success(term);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, GlossaryTermSaveDTO dto) {
        GovGlossaryTerm existing = termMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "术语不存在");
        }

        String originalStatus = existing.getStatus();
        if (GlossaryTermStatusEnum.PUBLISHED.getCode().equals(originalStatus)) {
            if (GlossaryTermStatusEnum.DRAFT.getCode().equals(dto.getStatus())) {
                throw new BusinessException(2001, "已发布的术语不能改为草稿状态");
            }
        }

        validateDto(dto, id);

        GovGlossaryTerm term = new GovGlossaryTerm();
        BeanUtils.copyProperties(dto, term);
        term.setId(id);
        term.setUpdateTime(LocalDateTime.now());
        termMapper.updateById(term);

        if (dto.getMappings() != null) {
            LambdaQueryWrapper<GovGlossaryMapping> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(GovGlossaryMapping::getTermId, id);
            mappingMapper.delete(delWrapper);
            if (!dto.getMappings().isEmpty()) {
                mappingService.batchSave(id, dto.getMappings(), existing.getCreateUser());
            }
        }

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        GovGlossaryTerm existing = termMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "术语不存在");
        }

        LambdaQueryWrapper<GovGlossaryMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovGlossaryMapping::getTermId, id);
        mappingMapper.delete(wrapper);

        termMapper.deleteById(id);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        for (Long id : ids) {
            delete(id);
        }
        return Result.success();
    }

    @Override
    public Result<GovGlossaryTerm> getById(Long id) {
        GovGlossaryTerm entity = termMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "术语不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<Page<GlossaryTermVO>> page(Integer pageNum, Integer pageSize, GlossaryTermQueryDTO queryDTO) {
        Page<GovGlossaryTerm> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GovGlossaryTerm> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(GovGlossaryTerm::getSortOrder)
               .orderByDesc(GovGlossaryTerm::getCreateTime);
        Page<GovGlossaryTerm> result = termMapper.selectPage(page, wrapper);

        List<Long> ownerIds = result.getRecords().stream()
                .map(GovGlossaryTerm::getOwnerId)
                .filter(o -> o != null)
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, SysUser> ownerMap;
        if (ownerIds.isEmpty()) {
            ownerMap = new HashMap<>();
        } else {
            List<SysUser> users = userMapper.selectBatchIds(ownerIds);
            ownerMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        }

        List<Long> deptIds = result.getRecords().stream()
                .map(GovGlossaryTerm::getDeptId)
                .filter(d -> d != null)
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, SysDept> deptMap;
        if (deptIds.isEmpty()) {
            deptMap = new HashMap<>();
        } else {
            List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
            deptMap = depts.stream().collect(Collectors.toMap(SysDept::getId, d -> d));
        }

        final Map<Long, Long> mappingCountMap = getMappingCountMap();

        Page<GlossaryTermVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<GlossaryTermVO> vos = result.getRecords().stream()
                .map(t -> buildVO(t, ownerMap, deptMap, mappingCountMap))
                .collect(Collectors.toList());
        voPage.setRecords(vos);

        return Result.success(voPage);
    }

    @Override
    public Result<GlossaryTermDetailVO> getDetail(Long id) {
        GovGlossaryTerm term = termMapper.selectById(id);
        if (term == null) {
            throw new BusinessException(2001, "术语不存在");
        }

        GlossaryTermDetailVO.GlossaryTermDetailVOBuilder builder = GlossaryTermDetailVO.builder()
                .id(term.getId())
                .termCode(term.getTermCode())
                .termName(term.getTermName())
                .termNameEn(term.getTermNameEn())
                .termAlias(term.getTermAlias())
                .categoryId(term.getCategoryId())
                .bizDomain(term.getBizDomain())
                .definition(term.getDefinition())
                .formula(term.getFormula())
                .dataType(term.getDataType())
                .unit(term.getUnit())
                .exampleValue(term.getExampleValue())
                .sensitivityLevel(term.getSensitivityLevel())
                .sensitivityLevelLabel(getSensitivityLabel(term.getSensitivityLevel()))
                .status(term.getStatus())
                .statusLabel(GlossaryTermStatusEnum.getLabelByCode(term.getStatus()))
                .enabled(term.getEnabled())
                .enabledLabel(term.getEnabled() != null && term.getEnabled() == 1 ? "启用" : "禁用")
                .ownerId(term.getOwnerId())
                .deptId(term.getDeptId())
                .sortOrder(term.getSortOrder())
                .publishedTime(term.getPublishedTime())
                .createUser(term.getCreateUser())
                .createTime(term.getCreateTime())
                .updateUser(term.getUpdateUser())
                .updateTime(term.getUpdateTime());

        if (term.getOwnerId() != null) {
            SysUser owner = userMapper.selectById(term.getOwnerId());
            if (owner != null) {
                builder.ownerName(owner.getNickname() != null ? owner.getNickname() : owner.getUsername());
            }
        }
        if (term.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(term.getDeptId());
            if (dept != null) {
                builder.deptName(dept.getDeptName());
            }
        }
        if (term.getCreateUser() != null) {
            SysUser creator = userMapper.selectById(term.getCreateUser());
            if (creator != null) {
                builder.createUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
            }
        }

        // 加载映射列表
        List<GlossaryMappingVO> mappingVOs = getMappingVOs(id);
        builder.mappings(mappingVOs);

        return Result.success(builder.build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enable(Long id) {
        GovGlossaryTerm term = termMapper.selectById(id);
        if (term == null) {
            throw new BusinessException(2001, "术语不存在");
        }

        GovGlossaryTerm update = new GovGlossaryTerm();
        update.setId(id);
        update.setEnabled(1);
        update.setUpdateTime(LocalDateTime.now());
        termMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disable(Long id) {
        GovGlossaryTerm term = termMapper.selectById(id);
        if (term == null) {
            throw new BusinessException(2001, "术语不存在");
        }

        GovGlossaryTerm update = new GovGlossaryTerm();
        update.setId(id);
        update.setEnabled(0);
        update.setUpdateTime(LocalDateTime.now());
        termMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> publish(Long id) {
        GovGlossaryTerm term = termMapper.selectById(id);
        if (term == null) {
            throw new BusinessException(2001, "术语不存在");
        }

        GovGlossaryTerm update = new GovGlossaryTerm();
        update.setId(id);
        update.setStatus(GlossaryTermStatusEnum.PUBLISHED.getCode());
        update.setPublishedTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        termMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deprecate(Long id) {
        GovGlossaryTerm term = termMapper.selectById(id);
        if (term == null) {
            throw new BusinessException(2001, "术语不存在");
        }

        GovGlossaryTerm update = new GovGlossaryTerm();
        update.setId(id);
        update.setStatus(GlossaryTermStatusEnum.DEPRECATED.getCode());
        update.setUpdateTime(LocalDateTime.now());
        termMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GovGlossaryTerm> copy(Long id) {
        GovGlossaryTerm original = termMapper.selectById(id);
        if (original == null) {
            throw new BusinessException(2001, "术语不存在");
        }

        GovGlossaryTerm copy = new GovGlossaryTerm();
        BeanUtils.copyProperties(original, copy);
        copy.setId(null);
        copy.setTermCode(generateCopyCode(original.getTermCode()));
        copy.setTermName(original.getTermName() + "（副本）");
        copy.setStatus(GlossaryTermStatusEnum.DRAFT.getCode());
        copy.setPublishedTime(null);
        copy.setCreateTime(LocalDateTime.now());
        copy.setUpdateTime(null);
        termMapper.insert(copy);

        return Result.success(copy);
    }

    @Override
    public Result<List<GlossaryTermVO>> list(GlossaryTermQueryDTO queryDTO) {
        LambdaQueryWrapper<GovGlossaryTerm> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByAsc(GovGlossaryTerm::getSortOrder)
               .orderByDesc(GovGlossaryTerm::getCreateTime);
        List<GovGlossaryTerm> list = termMapper.selectList(wrapper);

        List<Long> ownerIds = list.stream()
                .map(GovGlossaryTerm::getOwnerId)
                .filter(o -> o != null)
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, SysUser> ownerMap;
        if (ownerIds.isEmpty()) {
            ownerMap = new HashMap<>();
        } else {
            List<SysUser> users = userMapper.selectBatchIds(ownerIds);
            ownerMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        }

        List<Long> deptIds = list.stream()
                .map(GovGlossaryTerm::getDeptId)
                .filter(d -> d != null)
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, SysDept> deptMap;
        if (deptIds.isEmpty()) {
            deptMap = new HashMap<>();
        } else {
            List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
            deptMap = depts.stream().collect(Collectors.toMap(SysDept::getId, d -> d));
        }

        final Map<Long, Long> mappingCountMap = getMappingCountMap();

        List<GlossaryTermVO> vos = list.stream()
                .map(t -> buildVO(t, ownerMap, deptMap, mappingCountMap))
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    @Override
    public Result<GlossaryStatsVO> getStats() {
        long totalTerm = termMapper.selectCount(null);

        LambdaQueryWrapper<GovGlossaryTerm> pubWrapper = new LambdaQueryWrapper<>();
        pubWrapper.eq(GovGlossaryTerm::getStatus, GlossaryTermStatusEnum.PUBLISHED.getCode());
        long published = termMapper.selectCount(pubWrapper);

        LambdaQueryWrapper<GovGlossaryTerm> draftWrapper = new LambdaQueryWrapper<>();
        draftWrapper.eq(GovGlossaryTerm::getStatus, GlossaryTermStatusEnum.DRAFT.getCode());
        long draft = termMapper.selectCount(draftWrapper);

        LambdaQueryWrapper<GovGlossaryTerm> depWrapper = new LambdaQueryWrapper<>();
        depWrapper.eq(GovGlossaryTerm::getStatus, GlossaryTermStatusEnum.DEPRECATED.getCode());
        long deprecated = termMapper.selectCount(depWrapper);

        long totalMapping = mappingMapper.selectCount(null);

        LambdaQueryWrapper<GovGlossaryMapping> pendWrapper = new LambdaQueryWrapper<>();
        pendWrapper.eq(GovGlossaryMapping::getStatus, "PENDING");
        long pendingMapping = mappingMapper.selectCount(pendWrapper);

        LambdaQueryWrapper<GovGlossaryMapping> apprWrapper = new LambdaQueryWrapper<>();
        apprWrapper.eq(GovGlossaryMapping::getStatus, "APPROVED");
        long approvedMapping = mappingMapper.selectCount(apprWrapper);

        // 按业务域统计
        List<GovGlossaryTerm> allTerms = termMapper.selectList(null);
        Map<String, Long> bizDomainMap = allTerms.stream()
                .filter(t -> StringUtils.hasText(t.getBizDomain()))
                .collect(Collectors.groupingBy(GovGlossaryTerm::getBizDomain, Collectors.counting()));

        // 按数据类型统计
        Map<String, Long> dataTypeMap = allTerms.stream()
                .filter(t -> StringUtils.hasText(t.getDataType()))
                .collect(Collectors.groupingBy(GovGlossaryTerm::getDataType, Collectors.counting()));

        // 按敏感等级统计
        Map<String, Long> sensitivityMap = allTerms.stream()
                .filter(t -> StringUtils.hasText(t.getSensitivityLevel()))
                .collect(Collectors.groupingBy(GovGlossaryTerm::getSensitivityLevel, Collectors.counting()));

        GlossaryStatsVO stats = GlossaryStatsVO.builder()
                .totalTermCount(totalTerm)
                .publishedTermCount(published)
                .draftTermCount(draft)
                .deprecatedTermCount(deprecated)
                .totalMappingCount(totalMapping)
                .pendingMappingCount(pendingMapping)
                .approvedMappingCount(approvedMapping)
                .termCountByBizDomain(bizDomainMap)
                .termCountByDataType(dataTypeMap)
                .termCountBySensitivityLevel(sensitivityMap)
                .build();

        return Result.success(stats);
    }

    @Override
    public Result<List<GlossaryTermVO>> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Result.success(new ArrayList<>());
        }

        LambdaQueryWrapper<GovGlossaryTerm> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .like(GovGlossaryTerm::getTermName, keyword)
                .or()
                .like(GovGlossaryTerm::getTermCode, keyword)
                .or()
                .like(GovGlossaryTerm::getTermNameEn, keyword)
                .or()
                .like(GovGlossaryTerm::getTermAlias, keyword)
        );
        wrapper.orderByDesc(GovGlossaryTerm::getSortOrder)
               .orderByDesc(GovGlossaryTerm::getCreateTime);
        List<GovGlossaryTerm> list = termMapper.selectList(wrapper);

        List<Long> ownerIds = list.stream()
                .map(GovGlossaryTerm::getOwnerId)
                .filter(o -> o != null)
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, SysUser> ownerMap;
        if (ownerIds.isEmpty()) {
            ownerMap = new HashMap<>();
        } else {
            List<SysUser> users = userMapper.selectBatchIds(ownerIds);
            ownerMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        }

        List<Long> deptIds = list.stream()
                .map(GovGlossaryTerm::getDeptId)
                .filter(d -> d != null)
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, SysDept> deptMap;
        if (deptIds.isEmpty()) {
            deptMap = new HashMap<>();
        } else {
            List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
            deptMap = depts.stream().collect(Collectors.toMap(SysDept::getId, d -> d));
        }

        final Map<Long, Long> mappingCountMap = getMappingCountMap();

        List<GlossaryTermVO> vos = list.stream()
                .map(t -> buildVO(t, ownerMap, deptMap, mappingCountMap))
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    @Override
    public Result<List<GlossaryMappingVO>> getMappings(Long termId) {
        return Result.success(getMappingVOs(termId));
    }

    // ====== private helper methods ======

    private LambdaQueryWrapper<GovGlossaryTerm> buildQueryWrapper(GlossaryTermQueryDTO queryDTO) {
        LambdaQueryWrapper<GovGlossaryTerm> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO == null) {
            return wrapper;
        }

        if (StringUtils.hasText(queryDTO.getTermName())) {
            wrapper.like(GovGlossaryTerm::getTermName, queryDTO.getTermName());
        }
        if (StringUtils.hasText(queryDTO.getTermCode())) {
            wrapper.eq(GovGlossaryTerm::getTermCode, queryDTO.getTermCode());
        }
        if (StringUtils.hasText(queryDTO.getTermNameEn())) {
            wrapper.like(GovGlossaryTerm::getTermNameEn, queryDTO.getTermNameEn());
        }
        if (StringUtils.hasText(queryDTO.getTermAlias())) {
            wrapper.like(GovGlossaryTerm::getTermAlias, queryDTO.getTermAlias());
        }
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(GovGlossaryTerm::getCategoryId, queryDTO.getCategoryId());
        }
        if (StringUtils.hasText(queryDTO.getBizDomain())) {
            wrapper.eq(GovGlossaryTerm::getBizDomain, queryDTO.getBizDomain());
        }
        if (StringUtils.hasText(queryDTO.getStatus())) {
            wrapper.eq(GovGlossaryTerm::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getEnabled() != null) {
            wrapper.eq(GovGlossaryTerm::getEnabled, queryDTO.getEnabled());
        }
        if (StringUtils.hasText(queryDTO.getDataType())) {
            wrapper.eq(GovGlossaryTerm::getDataType, queryDTO.getDataType());
        }
        if (StringUtils.hasText(queryDTO.getSensitivityLevel())) {
            wrapper.eq(GovGlossaryTerm::getSensitivityLevel, queryDTO.getSensitivityLevel());
        }
        if (queryDTO.getOwnerId() != null) {
            wrapper.eq(GovGlossaryTerm::getOwnerId, queryDTO.getOwnerId());
        }
        if (queryDTO.getDeptId() != null) {
            wrapper.eq(GovGlossaryTerm::getDeptId, queryDTO.getDeptId());
        }

        return wrapper;
    }

    private void validateDto(GlossaryTermSaveDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getTermName())) {
            throw new BusinessException(2001, "术语名称不能为空");
        }
        if (!StringUtils.hasText(dto.getTermCode())) {
            throw new BusinessException(2001, "术语编码不能为空");
        }

        LambdaQueryWrapper<GovGlossaryTerm> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(GovGlossaryTerm::getTermCode, dto.getTermCode());
        if (excludeId != null) {
            codeWrapper.ne(GovGlossaryTerm::getId, excludeId);
        }
        if (termMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException(2001, "术语编码已存在");
        }
    }

    private Map<Long, Long> getMappingCountMap() {
        List<GovGlossaryMapping> allMappings = mappingMapper.selectList(null);
        return allMappings.stream()
                .collect(Collectors.groupingBy(
                        GovGlossaryMapping::getTermId,
                        Collectors.counting()
                ));
    }

    private List<GlossaryMappingVO> getMappingVOs(Long termId) {
        LambdaQueryWrapper<GovGlossaryMapping> mappingWrapper = new LambdaQueryWrapper<>();
        mappingWrapper.eq(GovGlossaryMapping::getTermId, termId)
                     .orderByDesc(GovGlossaryMapping::getCreateTime);
        List<GovGlossaryMapping> mappings = mappingMapper.selectList(mappingWrapper);

        return mappings.stream()
                .map(this::buildMappingVO)
                .collect(Collectors.toList());
    }

    private GlossaryMappingVO buildMappingVO(GovGlossaryMapping mapping) {
        GlossaryMappingVO.GlossaryMappingVOBuilder builder = GlossaryMappingVO.builder()
                .id(mapping.getId())
                .termId(mapping.getTermId())
                .dsId(mapping.getDsId())
                .tableName(mapping.getTableName())
                .columnName(mapping.getColumnName())
                .mappingType(mapping.getMappingType())
                .mappingTypeLabel(getMappingTypeLabel(mapping.getMappingType()))
                .mappingDesc(mapping.getMappingDesc())
                .confidence(mapping.getConfidence())
                .status(mapping.getStatus())
                .statusLabel(getMappingStatusLabel(mapping.getStatus()))
                .approvedBy(mapping.getApprovedBy())
                .approvedTime(mapping.getApprovedTime())
                .rejectReason(mapping.getRejectReason())
                .createUser(mapping.getCreateUser())
                .createTime(mapping.getCreateTime());

        if (mapping.getApprovedBy() != null) {
            SysUser approver = userMapper.selectById(mapping.getApprovedBy());
            if (approver != null) {
                builder.approvedByName(approver.getNickname() != null ? approver.getNickname() : approver.getUsername());
            }
        }

        if (mapping.getCreateUser() != null) {
            SysUser creator = userMapper.selectById(mapping.getCreateUser());
            if (creator != null) {
                builder.createUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
            }
        }

        return builder.build();
    }

    private GlossaryTermVO buildVO(GovGlossaryTerm term,
                                    Map<Long, SysUser> ownerMap,
                                    Map<Long, SysDept> deptMap,
                                    Map<Long, Long> mappingCountMap) {
        GlossaryTermVO.GlossaryTermVOBuilder builder = GlossaryTermVO.builder()
                .id(term.getId())
                .termCode(term.getTermCode())
                .termName(term.getTermName())
                .termNameEn(term.getTermNameEn())
                .termAlias(term.getTermAlias())
                .categoryId(term.getCategoryId())
                .bizDomain(term.getBizDomain())
                .definition(term.getDefinition())
                .dataType(term.getDataType())
                .unit(term.getUnit())
                .exampleValue(term.getExampleValue())
                .sensitivityLevel(term.getSensitivityLevel())
                .sensitivityLevelLabel(getSensitivityLabel(term.getSensitivityLevel()))
                .status(term.getStatus())
                .statusLabel(GlossaryTermStatusEnum.getLabelByCode(term.getStatus()))
                .enabled(term.getEnabled())
                .enabledLabel(term.getEnabled() != null && term.getEnabled() == 1 ? "启用" : "禁用")
                .ownerId(term.getOwnerId())
                .deptId(term.getDeptId())
                .sortOrder(term.getSortOrder())
                .publishedTime(term.getPublishedTime())
                .mappingCount(mappingCountMap.getOrDefault(term.getId(), 0L).intValue())
                .createUser(term.getCreateUser())
                .createTime(term.getCreateTime())
                .updateUser(term.getUpdateUser())
                .updateTime(term.getUpdateTime());

        if (term.getOwnerId() != null && ownerMap.containsKey(term.getOwnerId())) {
            SysUser owner = ownerMap.get(term.getOwnerId());
            builder.ownerName(owner.getNickname() != null ? owner.getNickname() : owner.getUsername());
        }

        if (term.getDeptId() != null && deptMap.containsKey(term.getDeptId())) {
            builder.deptName(deptMap.get(term.getDeptId()).getDeptName());
        }

        if (term.getCreateUser() != null && ownerMap.containsKey(term.getCreateUser())) {
            SysUser creator = ownerMap.get(term.getCreateUser());
            builder.createUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }

        return builder.build();
    }

    private String generateCopyCode(String originalCode) {
        return originalCode + "_COPY_" + System.currentTimeMillis();
    }

    private String getSensitivityLabel(String level) {
        if (level == null) return null;
        return switch (level) {
            case "PUBLIC" -> "公开";
            case "INTERNAL" -> "内部";
            case "CONFIDENTIAL" -> "保密";
            case "SECRET" -> "机密";
            default -> level;
        };
    }

    private String getMappingTypeLabel(String type) {
        if (type == null) return null;
        return switch (type) {
            case "DIRECT" -> "直接映射";
            case "TRANSFORM" -> "转换映射";
            case "AGGREGATE" -> "聚合映射";
            default -> type;
        };
    }

    private String getMappingStatusLabel(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已审批";
            case "REJECTED" -> "已驳回";
            default -> status;
        };
    }

    @Override
    public Result<Map<String, Object>> importTerms(java.io.InputStream inputStream) {
        if (inputStream == null) {
            return Result.fail(1, "文件不能为空");
        }
        int successCount = 0;
        int failCount = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();
        try {
            java.util.List<GlossaryTermExcelDTO> rows = com.alibaba.excel.EasyExcel.read(inputStream)
                    .head(GlossaryTermExcelDTO.class)
                    .sheet()
                    .headRowNumber(1)
                    .doReadSync();

            for (int i = 0; i < rows.size(); i++) {
                GlossaryTermExcelDTO dto = rows.get(i);
                if (dto.getTermCode() == null || dto.getTermCode().trim().isEmpty()) {
                    failCount++;
                    errors.add("第" + (i + 2) + "行: 术语编码不能为空");
                    continue;
                }
                if (dto.getTermName() == null || dto.getTermName().trim().isEmpty()) {
                    failCount++;
                    errors.add("第" + (i + 2) + "行: 术语名称不能为空");
                    continue;
                }
                try {
                    GlossaryTermSaveDTO saveDto = GlossaryTermSaveDTO.builder()
                            .termCode(dto.getTermCode().trim())
                            .termName(dto.getTermName().trim())
                            .termNameEn(dto.getTermNameEn() != null ? dto.getTermNameEn().trim() : null)
                            .termAlias(dto.getTermAlias() != null ? dto.getTermAlias().trim() : null)
                            .bizDomain(dto.getBizDomain() != null ? dto.getBizDomain().trim() : null)
                            .definition(dto.getDefinition())
                            .formula(dto.getFormula())
                            .dataType(dto.getDataType())
                            .unit(dto.getUnit())
                            .exampleValue(dto.getExampleValue())
                            .sensitivityLevel(dto.getSensitivityLevel())
                            .status(dto.getStatus() != null ? dto.getStatus().trim() : "DRAFT")
                            .enabled(1)
                            .sortOrder(0)
                            .build();
                    save(saveDto);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("第" + (i + 2) + "行: " + (e.getMessage() != null ? e.getMessage() : "导入失败"));
                }
            }
        } catch (Exception e) {
            log.error("批量导入术语失败", e);
            return Result.fail(1, "文件解析失败: " + e.getMessage());
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        return Result.success(result);
    }

    @Override
    public byte[] exportTerms(GlossaryTermQueryDTO queryDTO) {
        LambdaQueryWrapper<GovGlossaryTerm> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(GovGlossaryTerm::getSortOrder)
               .orderByDesc(GovGlossaryTerm::getCreateTime);
        List<GovGlossaryTerm> list = termMapper.selectList(wrapper);

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        ExcelWriter writer = com.alibaba.excel.EasyExcel.write(out).build();
        WriteSheet sheet = com.alibaba.excel.EasyExcel.writerSheet(0, "术语列表")
                .head(GlossaryTermExcelExportVO.class)
                .build();

        List<GlossaryTermExcelExportVO> exportData = list.stream().map(t -> {
            GlossaryTermExcelExportVO vo = new GlossaryTermExcelExportVO();
            vo.setTermCode(t.getTermCode());
            vo.setTermName(t.getTermName());
            vo.setTermNameEn(t.getTermNameEn());
            vo.setTermAlias(t.getTermAlias());
            vo.setBizDomain(t.getBizDomain());
            vo.setDefinition(t.getDefinition());
            vo.setFormula(t.getFormula());
            vo.setDataType(t.getDataType());
            vo.setUnit(t.getUnit());
            vo.setExampleValue(t.getExampleValue());
            vo.setSensitivityLevel(t.getSensitivityLevel());
            vo.setStatus(t.getStatus());
            return vo;
        }).collect(java.util.stream.Collectors.toList());

        writer.write(exportData, sheet);
        writer.finish();
        return out.toByteArray();
    }

    @Override
    public byte[] getImportTemplate() {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        ExcelWriter writer = com.alibaba.excel.EasyExcel.write(out).build();
        WriteSheet sheet = com.alibaba.excel.EasyExcel.writerSheet(0, "术语导入模板")
                .head(GlossaryTermExcelDTO.class)
                .build();

        // 创建示例行
        GlossaryTermExcelDTO example = new GlossaryTermExcelDTO();
        example.setTermCode("EXAMPLE_001");
        example.setTermName("客户ID");
        example.setTermNameEn("Customer ID");
        example.setTermAlias("cust_id,customer_id");
        example.setBizDomain("客户域");
        example.setDefinition("在系统中唯一标识一个客户的编号");
        example.setFormula("雪花算法ID");
        example.setDataType("NUMBER");
        example.setUnit("个");
        example.setExampleValue("6871947673600000001");
        example.setSensitivityLevel("PUBLIC");
        example.setStatus("PUBLISHED");

        writer.write(java.util.Collections.singletonList(example), sheet);
        writer.finish();
        return out.toByteArray();
    }

    // Excel 导入 DTO
    public static class GlossaryTermExcelDTO {
        @com.alibaba.excel.annotation.ExcelProperty("术语编码")
        private String termCode;
        @com.alibaba.excel.annotation.ExcelProperty("术语名称")
        private String termName;
        @com.alibaba.excel.annotation.ExcelProperty("英文名")
        private String termNameEn;
        @com.alibaba.excel.annotation.ExcelProperty("别名")
        private String termAlias;
        @com.alibaba.excel.annotation.ExcelProperty("所属分类")
        private String categoryName;
        @com.alibaba.excel.annotation.ExcelProperty("业务域")
        private String bizDomain;
        @com.alibaba.excel.annotation.ExcelProperty("术语定义")
        private String definition;
        @com.alibaba.excel.annotation.ExcelProperty("计算公式")
        private String formula;
        @com.alibaba.excel.annotation.ExcelProperty("数据类型")
        private String dataType;
        @com.alibaba.excel.annotation.ExcelProperty("单位")
        private String unit;
        @com.alibaba.excel.annotation.ExcelProperty("示例值")
        private String exampleValue;
        @com.alibaba.excel.annotation.ExcelProperty("敏感等级")
        private String sensitivityLevel;
        @com.alibaba.excel.annotation.ExcelProperty("状态")
        private String status;

        public String getTermCode() { return termCode; }
        public void setTermCode(String termCode) { this.termCode = termCode; }
        public String getTermName() { return termName; }
        public void setTermName(String termName) { this.termName = termName; }
        public String getTermNameEn() { return termNameEn; }
        public void setTermNameEn(String termNameEn) { this.termNameEn = termNameEn; }
        public String getTermAlias() { return termAlias; }
        public void setTermAlias(String termAlias) { this.termAlias = termAlias; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getBizDomain() { return bizDomain; }
        public void setBizDomain(String bizDomain) { this.bizDomain = bizDomain; }
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
        public String getFormula() { return formula; }
        public void setFormula(String formula) { this.formula = formula; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getExampleValue() { return exampleValue; }
        public void setExampleValue(String exampleValue) { this.exampleValue = exampleValue; }
        public String getSensitivityLevel() { return sensitivityLevel; }
        public void setSensitivityLevel(String sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // Excel 导出 VO
    public static class GlossaryTermExcelExportVO {
        @com.alibaba.excel.annotation.ExcelProperty("术语编码")
        private String termCode;
        @com.alibaba.excel.annotation.ExcelProperty("术语名称")
        private String termName;
        @com.alibaba.excel.annotation.ExcelProperty("英文名")
        private String termNameEn;
        @com.alibaba.excel.annotation.ExcelProperty("别名")
        private String termAlias;
        @com.alibaba.excel.annotation.ExcelProperty("业务域")
        private String bizDomain;
        @com.alibaba.excel.annotation.ExcelProperty("术语定义")
        private String definition;
        @com.alibaba.excel.annotation.ExcelProperty("计算公式")
        private String formula;
        @com.alibaba.excel.annotation.ExcelProperty("数据类型")
        private String dataType;
        @com.alibaba.excel.annotation.ExcelProperty("单位")
        private String unit;
        @com.alibaba.excel.annotation.ExcelProperty("示例值")
        private String exampleValue;
        @com.alibaba.excel.annotation.ExcelProperty("敏感等级")
        private String sensitivityLevel;
        @com.alibaba.excel.annotation.ExcelProperty("状态")
        private String status;

        public String getTermCode() { return termCode; }
        public void setTermCode(String termCode) { this.termCode = termCode; }
        public String getTermName() { return termName; }
        public void setTermName(String termName) { this.termName = termName; }
        public String getTermNameEn() { return termNameEn; }
        public void setTermNameEn(String termNameEn) { this.termNameEn = termNameEn; }
        public String getTermAlias() { return termAlias; }
        public void setTermAlias(String termAlias) { this.termAlias = termAlias; }
        public String getBizDomain() { return bizDomain; }
        public void setBizDomain(String bizDomain) { this.bizDomain = bizDomain; }
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
        public String getFormula() { return formula; }
        public void setFormula(String formula) { this.formula = formula; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getExampleValue() { return exampleValue; }
        public void setExampleValue(String exampleValue) { this.exampleValue = exampleValue; }
        public String getSensitivityLevel() { return sensitivityLevel; }
        public void setSensitivityLevel(String sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
