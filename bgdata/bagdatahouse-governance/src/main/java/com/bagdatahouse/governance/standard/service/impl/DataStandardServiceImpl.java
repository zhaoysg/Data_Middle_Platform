package com.bagdatahouse.governance.standard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.entity.GovDataStandard;
import com.bagdatahouse.core.entity.GovMetadata;
import com.bagdatahouse.core.entity.GovStandardBinding;
import com.bagdatahouse.core.entity.SysDept;
import com.bagdatahouse.core.entity.SysUser;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.mapper.GovDataStandardMapper;
import com.bagdatahouse.core.mapper.GovMetadataMapper;
import com.bagdatahouse.core.mapper.GovStandardBindingMapper;
import com.bagdatahouse.core.mapper.SysDeptMapper;
import com.bagdatahouse.core.mapper.SysUserMapper;
import com.bagdatahouse.governance.standard.dto.DataStandardBindingDTO;
import com.bagdatahouse.governance.standard.dto.DataStandardQueryDTO;
import com.bagdatahouse.governance.standard.dto.DataStandardSaveDTO;
import com.bagdatahouse.governance.standard.enums.ComplianceStatusEnum;
import com.bagdatahouse.governance.standard.enums.StandardStatusEnum;
import com.bagdatahouse.governance.standard.enums.StandardTypeEnum;
import com.bagdatahouse.governance.standard.service.DataStandardService;
import com.bagdatahouse.governance.standard.vo.DataStandardBindingVO;
import com.bagdatahouse.governance.standard.vo.DataStandardDetailVO;
import com.bagdatahouse.governance.standard.vo.DataStandardStatsVO;
import com.bagdatahouse.governance.standard.vo.DataStandardVO;
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
 * 数据标准管理服务实现
 */
@Slf4j
@Service
public class DataStandardServiceImpl extends ServiceImpl<GovDataStandardMapper, GovDataStandard>
        implements DataStandardService {

    @Autowired
    private GovDataStandardMapper standardMapper;

    @Autowired
    private GovStandardBindingMapper bindingMapper;

    @Autowired
    private GovMetadataMapper metadataMapper;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GovDataStandard> save(DataStandardSaveDTO dto) {
        validateDto(dto, null);

        GovDataStandard standard = new GovDataStandard();
        BeanUtils.copyProperties(dto, standard);
        standard.setStatus(dto.getStatus() != null ? dto.getStatus() : StandardStatusEnum.DRAFT.getCode());
        standard.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
        standard.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        standard.setCreateTime(LocalDateTime.now());
        standardMapper.insert(standard);

        if (dto.getBindings() != null && !dto.getBindings().isEmpty()) {
            saveBindings(standard.getId(), dto.getBindings(), dto.getCreateUser());
        }

        return Result.success(standard);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, DataStandardSaveDTO dto) {
        GovDataStandard existing = standardMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据标准不存在");
        }

        validateDto(dto, id);

        GovDataStandard standard = new GovDataStandard();
        BeanUtils.copyProperties(dto, standard);
        standard.setId(id);
        standard.setUpdateTime(LocalDateTime.now());
        standardMapper.updateById(standard);

        LambdaQueryWrapper<GovStandardBinding> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(GovStandardBinding::getStandardId, id);
        bindingMapper.delete(delWrapper);

        if (dto.getBindings() != null && !dto.getBindings().isEmpty()) {
            saveBindings(id, dto.getBindings(), existing.getCreateUser());
        }

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        GovDataStandard existing = standardMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据标准不存在");
        }

        LambdaQueryWrapper<GovStandardBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovStandardBinding::getStandardId, id);
        bindingMapper.delete(wrapper);

        standardMapper.deleteById(id);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }

        LambdaQueryWrapper<GovStandardBinding> bindingWrapper = new LambdaQueryWrapper<>();
        bindingWrapper.in(GovStandardBinding::getStandardId, ids);
        bindingMapper.delete(bindingWrapper);

        standardMapper.deleteBatchIds(ids);

        return Result.success();
    }

    @Override
    public Result<GovDataStandard> getById(Long id) {
        GovDataStandard entity = standardMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "数据标准不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<Page<DataStandardVO>> page(Integer pageNum, Integer pageSize, DataStandardQueryDTO queryDTO) {
        Page<GovDataStandard> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GovDataStandard> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(GovDataStandard::getSortOrder)
               .orderByDesc(GovDataStandard::getCreateTime);
        Page<GovDataStandard> result = standardMapper.selectPage(page, wrapper);

        List<Long> ownerIds = result.getRecords().stream()
                .map(GovDataStandard::getOwnerId)
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
                .map(GovDataStandard::getDeptId)
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

        final Map<Long, Long> bindingCountMap = getBindingCountMap();

        Page<DataStandardVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<DataStandardVO> vos = result.getRecords().stream()
                .map(s -> buildVO(s, ownerMap, deptMap, bindingCountMap))
                .collect(Collectors.toList());
        voPage.setRecords(vos);

        return Result.success(voPage);
    }

    @Override
    public Result<DataStandardDetailVO> getDetail(Long id) {
        GovDataStandard standard = standardMapper.selectById(id);
        if (standard == null) {
            throw new BusinessException(2001, "数据标准不存在");
        }

        DataStandardDetailVO vo = buildDetailVO(standard);
        return Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enable(Long id) {
        GovDataStandard standard = standardMapper.selectById(id);
        if (standard == null) {
            throw new BusinessException(2001, "数据标准不存在");
        }

        GovDataStandard update = new GovDataStandard();
        update.setId(id);
        update.setEnabled(1);
        update.setUpdateTime(LocalDateTime.now());
        standardMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disable(Long id) {
        GovDataStandard standard = standardMapper.selectById(id);
        if (standard == null) {
            throw new BusinessException(2001, "数据标准不存在");
        }

        GovDataStandard update = new GovDataStandard();
        update.setId(id);
        update.setEnabled(0);
        update.setUpdateTime(LocalDateTime.now());
        standardMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> publish(Long id) {
        GovDataStandard standard = standardMapper.selectById(id);
        if (standard == null) {
            throw new BusinessException(2001, "数据标准不存在");
        }

        GovDataStandard update = new GovDataStandard();
        update.setId(id);
        update.setStatus(StandardStatusEnum.PUBLISHED.getCode());
        update.setUpdateTime(LocalDateTime.now());
        standardMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deprecate(Long id) {
        GovDataStandard standard = standardMapper.selectById(id);
        if (standard == null) {
            throw new BusinessException(2001, "数据标准不存在");
        }

        GovDataStandard update = new GovDataStandard();
        update.setId(id);
        update.setStatus(StandardStatusEnum.DEPRECATED.getCode());
        update.setUpdateTime(LocalDateTime.now());
        standardMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GovDataStandard> copy(Long id) {
        GovDataStandard original = standardMapper.selectById(id);
        if (original == null) {
            throw new BusinessException(2001, "数据标准不存在");
        }

        GovDataStandard copy = new GovDataStandard();
        BeanUtils.copyProperties(original, copy);
        copy.setId(null);
        copy.setStandardCode(generateCopyCode(original.getStandardCode()));
        copy.setStandardName(original.getStandardName() + "（副本）");
        copy.setStatus(StandardStatusEnum.DRAFT.getCode());
        copy.setCreateTime(LocalDateTime.now());
        copy.setUpdateTime(null);
        standardMapper.insert(copy);

        LambdaQueryWrapper<GovStandardBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovStandardBinding::getStandardId, id);
        List<GovStandardBinding> bindings = bindingMapper.selectList(wrapper);
        for (GovStandardBinding binding : bindings) {
            GovStandardBinding newBinding = new GovStandardBinding();
            BeanUtils.copyProperties(binding, newBinding);
            newBinding.setId(null);
            newBinding.setStandardId(copy.getId());
            newBinding.setCreateTime(LocalDateTime.now());
            newBinding.setUpdateTime(null);
            bindingMapper.insert(newBinding);
        }

        return Result.success(copy);
    }

    @Override
    public Result<DataStandardStatsVO> getStats() {
        long total = standardMapper.selectCount(null);

        LambdaQueryWrapper<GovDataStandard> pubWrapper = new LambdaQueryWrapper<>();
        pubWrapper.eq(GovDataStandard::getStatus, StandardStatusEnum.PUBLISHED.getCode());
        long published = standardMapper.selectCount(pubWrapper);

        LambdaQueryWrapper<GovDataStandard> draftWrapper = new LambdaQueryWrapper<>();
        draftWrapper.eq(GovDataStandard::getStatus, StandardStatusEnum.DRAFT.getCode());
        long draft = standardMapper.selectCount(draftWrapper);

        LambdaQueryWrapper<GovDataStandard> depWrapper = new LambdaQueryWrapper<>();
        depWrapper.eq(GovDataStandard::getStatus, StandardStatusEnum.DEPRECATED.getCode());
        long deprecated = standardMapper.selectCount(depWrapper);

        LambdaQueryWrapper<GovDataStandard> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(GovDataStandard::getStandardType, StandardTypeEnum.CODE_STANDARD.getCode());
        long codeCount = standardMapper.selectCount(codeWrapper);

        LambdaQueryWrapper<GovDataStandard> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(GovDataStandard::getStandardType, StandardTypeEnum.NAMING_STANDARD.getCode());
        long namingCount = standardMapper.selectCount(nameWrapper);

        LambdaQueryWrapper<GovDataStandard> primaryWrapper = new LambdaQueryWrapper<>();
        primaryWrapper.eq(GovDataStandard::getStandardType, StandardTypeEnum.PRIMARY_DATA.getCode());
        long primaryCount = standardMapper.selectCount(primaryWrapper);

        LambdaQueryWrapper<GovDataStandard> enabledWrapper = new LambdaQueryWrapper<>();
        enabledWrapper.eq(GovDataStandard::getEnabled, 1);
        long enabledCount = standardMapper.selectCount(enabledWrapper);

        long bindingTotal = bindingMapper.selectCount(null);

        LambdaQueryWrapper<GovStandardBinding> compWrapper = new LambdaQueryWrapper<>();
        compWrapper.eq(GovStandardBinding::getComplianceStatus, ComplianceStatusEnum.COMPLIANT.getCode());
        long compliantCount = bindingMapper.selectCount(compWrapper);

        LambdaQueryWrapper<GovStandardBinding> nonCompWrapper = new LambdaQueryWrapper<>();
        nonCompWrapper.eq(GovStandardBinding::getComplianceStatus, ComplianceStatusEnum.NON_COMPLIANT.getCode());
        long nonCompliantCount = bindingMapper.selectCount(nonCompWrapper);

        LambdaQueryWrapper<GovStandardBinding> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(GovStandardBinding::getComplianceStatus, ComplianceStatusEnum.PENDING.getCode());
        long pendingCount = bindingMapper.selectCount(pendingWrapper);

        Map<String, Long> countByType = new HashMap<>();
        countByType.put(StandardTypeEnum.CODE_STANDARD.getLabel(), codeCount);
        countByType.put(StandardTypeEnum.NAMING_STANDARD.getLabel(), namingCount);
        countByType.put(StandardTypeEnum.PRIMARY_DATA.getLabel(), primaryCount);

        DataStandardStatsVO stats = DataStandardStatsVO.builder()
                .totalCount(total)
                .publishedCount(published)
                .draftCount(draft)
                .deprecatedCount(deprecated)
                .codeStandardCount(codeCount)
                .namingStandardCount(namingCount)
                .primaryDataCount(primaryCount)
                .enabledCount(enabledCount)
                .disabledCount(total - enabledCount)
                .bindingCount(bindingTotal)
                .compliantBindingCount(compliantCount)
                .nonCompliantBindingCount(nonCompliantCount)
                .pendingBindingCount(pendingCount)
                .countByType(countByType)
                .build();

        return Result.success(stats);
    }

    @Override
    public Result<List<DataStandardVO>> list(DataStandardQueryDTO queryDTO) {
        LambdaQueryWrapper<GovDataStandard> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByAsc(GovDataStandard::getSortOrder)
               .orderByDesc(GovDataStandard::getCreateTime);
        List<GovDataStandard> list = standardMapper.selectList(wrapper);

        List<Long> ownerIds = list.stream()
                .map(GovDataStandard::getOwnerId)
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
                .map(GovDataStandard::getDeptId)
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

        final Map<Long, Long> bindingCountMap = getBindingCountMap();

        List<DataStandardVO> vos = list.stream()
                .map(s -> buildVO(s, ownerMap, deptMap, bindingCountMap))
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> bindMetadata(Long standardId, Long metadataId, String targetColumn,
                                     String enforceAction, Long createUser) {
        GovDataStandard standard = standardMapper.selectById(standardId);
        if (standard == null) {
            throw new BusinessException(2001, "数据标准不存在");
        }

        GovMetadata metadata = metadataMapper.selectById(metadataId);
        if (metadata == null) {
            throw new BusinessException(2001, "元数据不存在");
        }

        GovStandardBinding binding = GovStandardBinding.builder()
                .standardId(standardId)
                .metadataId(metadataId)
                .dsId(metadata.getDsId())
                .targetTable(metadata.getTableName())
                .targetColumn(targetColumn)
                .complianceStatus(ComplianceStatusEnum.PENDING.getCode())
                .violationCount(0)
                .enforceAction(enforceAction != null ? enforceAction : standard.getEnforceAction())
                .createUser(createUser)
                .createTime(LocalDateTime.now())
                .build();
        bindingMapper.insert(binding);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchBindMetadata(Long standardId, List<Long> metadataIds,
                                          String enforceAction, Long createUser) {
        if (metadataIds == null || metadataIds.isEmpty()) {
            return Result.success();
        }

        for (Long metadataId : metadataIds) {
            bindMetadata(standardId, metadataId, null, enforceAction, createUser);
        }

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unbindMetadata(Long bindingId) {
        bindingMapper.deleteById(bindingId);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchUnbindMetadata(List<Long> bindingIds) {
        if (bindingIds == null || bindingIds.isEmpty()) {
            return Result.success();
        }
        bindingMapper.deleteBatchIds(bindingIds);
        return Result.success();
    }

    @Override
    public Result<List<DataStandardBindingVO>> getBindings(Long standardId) {
        LambdaQueryWrapper<GovStandardBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovStandardBinding::getStandardId, standardId)
               .orderByDesc(GovStandardBinding::getCreateTime);
        List<GovStandardBinding> bindings = bindingMapper.selectList(wrapper);

        Set<Long> metadataIds = bindings.stream()
                .map(GovStandardBinding::getMetadataId)
                .filter(m -> m != null)
                .collect(Collectors.toSet());
        final Map<Long, GovMetadata> metadataMap;
        if (metadataIds.isEmpty()) {
            metadataMap = new HashMap<>();
        } else {
            List<GovMetadata> metadataList = metadataMapper.selectBatchIds(metadataIds);
            metadataMap = metadataList.stream().collect(Collectors.toMap(GovMetadata::getId, m -> m));
        }

        Set<Long> dsIds = bindings.stream()
                .map(GovStandardBinding::getDsId)
                .filter(d -> d != null)
                .collect(Collectors.toSet());
        final Map<Long, DqDatasource> dsMap;
        if (dsIds.isEmpty()) {
            dsMap = new HashMap<>();
        } else {
            List<DqDatasource> dsList = datasourceMapper.selectBatchIds(dsIds);
            dsMap = dsList.stream().collect(Collectors.toMap(DqDatasource::getId, d -> d));
        }

        List<Long> createUserIds = bindings.stream()
                .map(GovStandardBinding::getCreateUser)
                .filter(u -> u != null)
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, SysUser> userMap;
        if (createUserIds.isEmpty()) {
            userMap = new HashMap<>();
        } else {
            List<SysUser> users = userMapper.selectBatchIds(createUserIds);
            userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        }

        List<DataStandardBindingVO> vos = bindings.stream()
                .map(b -> buildBindingVO(b, metadataMap, dsMap, userMap))
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    @Override
    public Result<List<DataStandardVO>> getStandardsByMetadata(Long metadataId) {
        GovMetadata metadata = metadataMapper.selectById(metadataId);
        if (metadata == null) {
            throw new BusinessException(2001, "元数据不存在");
        }

        LambdaQueryWrapper<GovStandardBinding> bindingWrapper = new LambdaQueryWrapper<>();
        bindingWrapper.eq(GovStandardBinding::getMetadataId, metadataId)
                     .or()
                     .eq(GovStandardBinding::getTargetTable, metadata.getTableName())
                     .eq(GovStandardBinding::getDsId, metadata.getDsId());
        List<GovStandardBinding> bindings = bindingMapper.selectList(bindingWrapper);

        List<Long> standardIds = bindings.stream()
                .map(GovStandardBinding::getStandardId)
                .distinct()
                .collect(Collectors.toList());

        if (standardIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        LambdaQueryWrapper<GovDataStandard> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GovDataStandard::getId, standardIds);
        List<GovDataStandard> standards = standardMapper.selectList(wrapper);

        List<Long> ownerIds = standards.stream()
                .map(GovDataStandard::getOwnerId)
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

        final Map<Long, Long> bindingCountMap = getBindingCountMap();

        List<DataStandardVO> vos = standards.stream()
                .map(s -> buildVO(s, ownerMap, new HashMap<>(), bindingCountMap))
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    // ==================== private helper methods ====================

    private LambdaQueryWrapper<GovDataStandard> buildQueryWrapper(DataStandardQueryDTO queryDTO) {
        LambdaQueryWrapper<GovDataStandard> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO == null) {
            return wrapper;
        }

        if (StringUtils.hasText(queryDTO.getStandardType())) {
            wrapper.eq(GovDataStandard::getStandardType, queryDTO.getStandardType());
        }
        if (StringUtils.hasText(queryDTO.getStandardCategory())) {
            wrapper.eq(GovDataStandard::getStandardCategory, queryDTO.getStandardCategory());
        }
        if (StringUtils.hasText(queryDTO.getStandardName())) {
            wrapper.like(GovDataStandard::getStandardName, queryDTO.getStandardName());
        }
        if (StringUtils.hasText(queryDTO.getStandardCode())) {
            wrapper.eq(GovDataStandard::getStandardCode, queryDTO.getStandardCode());
        }
        if (StringUtils.hasText(queryDTO.getStatus())) {
            wrapper.eq(GovDataStandard::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getEnabled() != null) {
            wrapper.eq(GovDataStandard::getEnabled, queryDTO.getEnabled());
        }
        if (StringUtils.hasText(queryDTO.getBizDomain())) {
            wrapper.eq(GovDataStandard::getBizDomain, queryDTO.getBizDomain());
        }
        if (queryDTO.getOwnerId() != null) {
            wrapper.eq(GovDataStandard::getOwnerId, queryDTO.getOwnerId());
        }
        if (queryDTO.getDeptId() != null) {
            wrapper.eq(GovDataStandard::getDeptId, queryDTO.getDeptId());
        }

        return wrapper;
    }

    private void validateDto(DataStandardSaveDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getStandardType())) {
            throw new BusinessException(2001, "标准类型不能为空");
        }

        boolean validType = StandardTypeEnum.CODE_STANDARD.getCode().equals(dto.getStandardType())
                || StandardTypeEnum.NAMING_STANDARD.getCode().equals(dto.getStandardType())
                || StandardTypeEnum.PRIMARY_DATA.getCode().equals(dto.getStandardType());
        if (!validType) {
            throw new BusinessException(2001, "标准类型无效，仅支持 CODE_STANDARD / NAMING_STANDARD / PRIMARY_DATA");
        }

        if (!StringUtils.hasText(dto.getStandardName())) {
            throw new BusinessException(2001, "标准名称不能为空");
        }

        if (!StringUtils.hasText(dto.getStandardCode())) {
            throw new BusinessException(2001, "标准编码不能为空");
        }

        LambdaQueryWrapper<GovDataStandard> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(GovDataStandard::getStandardCode, dto.getStandardCode());
        if (excludeId != null) {
            codeWrapper.ne(GovDataStandard::getId, excludeId);
        }
        if (standardMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException(2001, "标准编码已存在");
        }
    }

    private void saveBindings(Long standardId, List<DataStandardBindingDTO> bindings, Long createUser) {
        for (DataStandardBindingDTO dto : bindings) {
            GovMetadata metadata = null;
            if (dto.getMetadataId() != null) {
                metadata = metadataMapper.selectById(dto.getMetadataId());
            }

            GovStandardBinding binding = GovStandardBinding.builder()
                    .standardId(standardId)
                    .metadataId(dto.getMetadataId())
                    .dsId(dto.getDsId() != null ? dto.getDsId() : (metadata != null ? metadata.getDsId() : null))
                    .targetTable(dto.getTargetTable() != null ? dto.getTargetTable() : (metadata != null ? metadata.getTableName() : null))
                    .targetColumn(dto.getTargetColumn())
                    .complianceStatus(ComplianceStatusEnum.PENDING.getCode())
                    .violationCount(0)
                    .enforceAction(dto.getEnforceAction())
                    .createUser(dto.getCreateUser() != null ? dto.getCreateUser() : createUser)
                    .createTime(LocalDateTime.now())
                    .build();
            bindingMapper.insert(binding);
        }
    }

    private Map<Long, Long> getBindingCountMap() {
        List<GovStandardBinding> allBindings = bindingMapper.selectList(null);
        return allBindings.stream()
                .collect(Collectors.groupingBy(
                        GovStandardBinding::getStandardId,
                        Collectors.counting()
                ));
    }

    private DataStandardVO buildVO(GovDataStandard standard, Map<Long, SysUser> ownerMap,
                                    Map<Long, SysDept> deptMap, Map<Long, Long> bindingCountMap) {
        DataStandardVO.DataStandardVOBuilder builder = DataStandardVO.builder()
                .id(standard.getId())
                .standardCode(standard.getStandardCode())
                .standardName(standard.getStandardName())
                .standardType(standard.getStandardType())
                .standardTypeLabel(StandardTypeEnum.getLabelByCode(standard.getStandardType()))
                .standardCategory(standard.getStandardCategory())
                .standardDesc(standard.getStandardDesc())
                .status(standard.getStatus())
                .statusLabel(StandardStatusEnum.getLabelByCode(standard.getStatus()))
                .enabled(standard.getEnabled())
                .enabledLabel(standard.getEnabled() != null && standard.getEnabled() == 1 ? "启用" : "禁用")
                .bizDomain(standard.getBizDomain())
                .ownerId(standard.getOwnerId())
                .deptId(standard.getDeptId())
                .sortOrder(standard.getSortOrder())
                .bindingCount(bindingCountMap.getOrDefault(standard.getId(), 0L).intValue())
                .createUser(standard.getCreateUser())
                .createTime(standard.getCreateTime())
                .updateUser(standard.getUpdateUser())
                .updateTime(standard.getUpdateTime());

        if (standard.getOwnerId() != null && ownerMap.containsKey(standard.getOwnerId())) {
            SysUser owner = ownerMap.get(standard.getOwnerId());
            builder.ownerName(owner.getNickname() != null ? owner.getNickname() : owner.getUsername());
        }

        if (standard.getDeptId() != null && deptMap.containsKey(standard.getDeptId())) {
            builder.deptName(deptMap.get(standard.getDeptId()).getDeptName());
        }

        if (standard.getCreateUser() != null && ownerMap.containsKey(standard.getCreateUser())) {
            SysUser creator = ownerMap.get(standard.getCreateUser());
            builder.createUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }

        return builder.build();
    }

    private DataStandardDetailVO buildDetailVO(GovDataStandard standard) {
        DataStandardDetailVO.DataStandardDetailVOBuilder builder = DataStandardDetailVO.builder()
                .id(standard.getId())
                .standardCode(standard.getStandardCode())
                .standardName(standard.getStandardName())
                .standardType(standard.getStandardType())
                .standardTypeLabel(StandardTypeEnum.getLabelByCode(standard.getStandardType()))
                .standardCategory(standard.getStandardCategory())
                .standardDesc(standard.getStandardDesc())
                .standardContent(standard.getStandardContent())
                .ruleExpr(standard.getRuleExpr())
                .exampleValue(standard.getExampleValue())
                .applicableObject(standard.getApplicableObject())
                .applicableObjectLabel(getApplicableObjectLabel(standard.getApplicableObject()))
                .enforceAction(standard.getEnforceAction())
                .enforceActionLabel(getEnforceActionLabel(standard.getEnforceAction()))
                .status(standard.getStatus())
                .statusLabel(StandardStatusEnum.getLabelByCode(standard.getStatus()))
                .enabled(standard.getEnabled())
                .bizDomain(standard.getBizDomain())
                .ownerId(standard.getOwnerId())
                .deptId(standard.getDeptId())
                .sortOrder(standard.getSortOrder())
                .createUser(standard.getCreateUser())
                .createTime(standard.getCreateTime())
                .updateUser(standard.getUpdateUser())
                .updateTime(standard.getUpdateTime());

        if (standard.getOwnerId() != null) {
            SysUser owner = userMapper.selectById(standard.getOwnerId());
            if (owner != null) {
                builder.ownerName(owner.getNickname() != null ? owner.getNickname() : owner.getUsername());
            }
        }

        if (standard.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(standard.getDeptId());
            if (dept != null) {
                builder.deptName(dept.getDeptName());
            }
        }

        if (standard.getCreateUser() != null) {
            SysUser creator = userMapper.selectById(standard.getCreateUser());
            if (creator != null) {
                builder.createUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
            }
        }

        LambdaQueryWrapper<GovStandardBinding> bindingWrapper = new LambdaQueryWrapper<>();
        bindingWrapper.eq(GovStandardBinding::getStandardId, standard.getId())
                     .orderByDesc(GovStandardBinding::getCreateTime);
        List<GovStandardBinding> bindings = bindingMapper.selectList(bindingWrapper);

        Set<Long> metadataIds = bindings.stream()
                .map(GovStandardBinding::getMetadataId)
                .filter(m -> m != null)
                .collect(Collectors.toSet());
        final Map<Long, GovMetadata> metadataMap;
        if (metadataIds.isEmpty()) {
            metadataMap = new HashMap<>();
        } else {
            List<GovMetadata> metadataList = metadataMapper.selectBatchIds(metadataIds);
            metadataMap = metadataList.stream().collect(Collectors.toMap(GovMetadata::getId, m -> m));
        }

        Set<Long> dsIds = bindings.stream()
                .map(GovStandardBinding::getDsId)
                .filter(d -> d != null)
                .collect(Collectors.toSet());
        final Map<Long, DqDatasource> dsMap;
        if (dsIds.isEmpty()) {
            dsMap = new HashMap<>();
        } else {
            List<DqDatasource> dsList = datasourceMapper.selectBatchIds(dsIds);
            dsMap = dsList.stream().collect(Collectors.toMap(DqDatasource::getId, d -> d));
        }

        List<Long> createUserIds = bindings.stream()
                .map(GovStandardBinding::getCreateUser)
                .filter(u -> u != null)
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, SysUser> userMap;
        if (createUserIds.isEmpty()) {
            userMap = new HashMap<>();
        } else {
            List<SysUser> users = userMapper.selectBatchIds(createUserIds);
            userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        }

        List<DataStandardBindingVO> bindingVOs = bindings.stream()
                .map(b -> buildBindingVO(b, metadataMap, dsMap, userMap))
                .collect(Collectors.toList());
        builder.bindings(bindingVOs);
        builder.bindingCount(bindingVOs.size());

        return builder.build();
    }

    private DataStandardBindingVO buildBindingVO(GovStandardBinding binding,
                                                   Map<Long, GovMetadata> metadataMap,
                                                   Map<Long, DqDatasource> dsMap,
                                                   Map<Long, SysUser> userMap) {
        GovMetadata metadata = binding.getMetadataId() != null ? metadataMap.get(binding.getMetadataId()) : null;
        DqDatasource ds = binding.getDsId() != null ? dsMap.get(binding.getDsId()) : null;

        return DataStandardBindingVO.builder()
                .id(binding.getId())
                .standardId(binding.getStandardId())
                .metadataId(binding.getMetadataId())
                .dsId(binding.getDsId())
                .dsName(ds != null ? ds.getDsName() : null)
                .targetTable(binding.getTargetTable())
                .targetTableAlias(metadata != null ? metadata.getTableAlias() : null)
                .targetColumn(binding.getTargetColumn())
                .dataLayer(metadata != null ? metadata.getDataLayer() : null)
                .complianceStatus(binding.getComplianceStatus())
                .complianceStatusLabel(ComplianceStatusEnum.getLabelByCode(binding.getComplianceStatus()))
                .violationCount(binding.getViolationCount())
                .lastCheckTime(binding.getLastCheckTime())
                .enforceAction(binding.getEnforceAction())
                .createUser(binding.getCreateUser())
                .createUserName(binding.getCreateUser() != null && userMap.containsKey(binding.getCreateUser())
                        ? (userMap.get(binding.getCreateUser()).getNickname() != null
                           ? userMap.get(binding.getCreateUser()).getNickname()
                           : userMap.get(binding.getCreateUser()).getUsername())
                        : null)
                .createTime(binding.getCreateTime())
                .build();
    }

    private String getApplicableObjectLabel(String applicableObject) {
        if (applicableObject == null) return null;
        return switch (applicableObject) {
            case "TABLE_NAME" -> "表名";
            case "COLUMN_NAME" -> "列名";
            case "DATA_VALUE" -> "数据值";
            default -> applicableObject;
        };
    }

    private String getEnforceActionLabel(String enforceAction) {
        if (enforceAction == null) return null;
        return switch (enforceAction) {
            case "ALERT" -> "仅告警";
            case "BLOCK" -> "阻断创建";
            default -> enforceAction;
        };
    }

    private String generateCopyCode(String originalCode) {
        return originalCode + "_COPY_" + System.currentTimeMillis();
    }
}
