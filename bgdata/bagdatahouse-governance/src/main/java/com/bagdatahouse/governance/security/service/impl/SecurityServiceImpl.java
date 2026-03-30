package com.bagdatahouse.governance.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.*;
import com.bagdatahouse.core.mapper.*;
import com.bagdatahouse.core.entity.GovMetadata;
import com.bagdatahouse.core.entity.GovMetadataColumn;
import com.bagdatahouse.core.mapper.GovMetadataMapper;
import com.bagdatahouse.core.mapper.GovMetadataColumnMapper;
import com.bagdatahouse.datasource.manager.DynamicDataSourceManager;
import com.bagdatahouse.governance.security.dto.*;
import com.bagdatahouse.governance.security.enums.*;
import com.bagdatahouse.governance.security.service.SecurityService;
import com.bagdatahouse.governance.security.vo.*;
import com.bagdatahouse.common.util.DataMaskingEngine;
import com.bagdatahouse.core.entity.*;
import com.bagdatahouse.core.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据安全分类分级服务实现
 */
@Slf4j
@Service
public class SecurityServiceImpl implements SecurityService {

    @Autowired
    private SecClassificationMapper classificationMapper;

    @Autowired
    private SecLevelMapper levelMapper;

    @Autowired
    private SecSensitivityRuleMapper ruleMapper;

    @Autowired
    private SecColumnSensitivityMapper sensitivityMapper;

    @Autowired
    private SecMaskTemplateMapper maskTemplateMapper;

    @Autowired
    private GovMetadataMapper metadataMapper;

    @Autowired
    private GovMetadataColumnMapper columnMapper;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SecAccessApplicationMapper accessApplicationMapper;

    @Autowired
    private SecClassLevelBindingMapper classLevelBindingMapper;

    @Autowired
    private SecAccessAuditMapper accessAuditMapper;

    @Autowired
    private SecAccessLogMapper accessLogMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private com.bagdatahouse.core.mapper.SysRoleMapper roleMapper;

    @Autowired
    private com.bagdatahouse.core.mapper.SecScanScheduleMapper scanScheduleMapper;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Autowired(required = false)
    private com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry adapterRegistry;

    @Autowired(required = false)
    private DataMaskingEngine maskingEngine;

    // SENSITIVE 告警联动（依赖 monitor 模块）
    @Autowired(required = false)
    private com.bagdatahouse.monitor.service.AlertRecordService alertRecordService;

    @Autowired(required = false)
    private com.bagdatahouse.monitor.service.AlertRuleService alertRuleService;

    // 健康分计算依赖
    @Autowired
    private com.bagdatahouse.core.mapper.MonitorAlertRecordMapper alertRecordMapper;

    @Autowired
    private GovMetadataColumnMapper govMetadataColumnMapper;

    @Autowired
    private com.bagdatahouse.core.mapper.SecNewColumnAlertMapper newColumnAlertMapper;

    /**
     * 请求级缓存：同一线程内各 Service 方法共享维表数据（避免一次请求内多次 selectList）。
     * 值为 Map<String, Object>，key 见 initRequestMaps()。
     */
    private final ThreadLocal<Map<String, Object>> requestMaps = ThreadLocal.withInitial(this::initRequestMaps);

    private Map<String, Object> initRequestMaps() {
        Map<String, Object> m = new HashMap<>();
        m.put("classification", classificationMapper.selectList(null)
                .stream().collect(Collectors.toMap(SecClassification::getId, c -> c)));
        m.put("level", levelMapper.selectList(null)
                .stream().collect(Collectors.toMap(SecLevel::getId, l -> l)));
        m.put("rule", ruleMapper.selectList(null)
                .stream().collect(Collectors.toMap(SecSensitivityRule::getId, r -> r)));
        m.put("datasource", datasourceMapper.selectList(null)
                .stream().collect(Collectors.toMap(DqDatasource::getId, d -> d)));
        m.put("user", userMapper.selectList(null)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u)));
        m.put("sensitivityAll", sensitivityMapper.selectList(null));
        return m;
    }

    private Map<String, Object> getAllMaps() {
        return requestMaps.get();
    }

    // ==================== 数据分类管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecClassification> saveClassification(SecClassificationSaveDTO dto) {
        validateClassificationDto(dto, null);
        SecClassification entity = new SecClassification();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setClassOrder(dto.getClassOrder() != null ? dto.getClassOrder() : 0);
        entity.setCreateTime(LocalDateTime.now());
        classificationMapper.insert(entity);
        syncClassificationLevelBinding(entity.getId(), dto.getSensitivityLevel());
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateClassification(Long id, SecClassificationSaveDTO dto) {
        SecClassification existing = classificationMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据分类不存在");
        }
        validateClassificationDto(dto, id);
        SecClassification entity = new SecClassification();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        if (entity.getClassOrder() == null) {
            entity.setClassOrder(existing.getClassOrder());
        }
        entity.setUpdateTime(LocalDateTime.now());
        classificationMapper.updateById(entity);
        syncClassificationLevelBinding(id, dto.getSensitivityLevel());
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteClassification(Long id) {
        SecClassification existing = classificationMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据分类不存在");
        }
        classificationMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDeleteClassification(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        classificationMapper.deleteBatchIds(ids);
        return Result.success();
    }

    @Override
    public Result<SecClassification> getClassificationById(Long id) {
        SecClassification entity = classificationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "数据分类不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<Page<SecClassificationVO>> pageClassification(Integer pageNum, Integer pageSize, SecClassificationQueryDTO queryDTO) {
        Page<SecClassification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecClassification> wrapper = buildClassificationQueryWrapper(queryDTO);
        wrapper.orderByAsc(SecClassification::getClassOrder)
               .orderByDesc(SecClassification::getCreateTime);
        Page<SecClassification> result = classificationMapper.selectPage(page, wrapper);

        List<SecSensitivityRule> allRules = ruleMapper.selectList(null);
        Map<Long, Long> ruleCountMap = allRules.stream()
                .filter(r -> r.getClassId() != null)
                .collect(Collectors.groupingBy(SecSensitivityRule::getClassId, Collectors.counting()));
        Map<Long, List<SecSensitivityRule>> rulesByClassId = allRules.stream()
                .filter(r -> r.getClassId() != null)
                .collect(Collectors.groupingBy(SecSensitivityRule::getClassId));
        Map<Long, List<SecClassLevelBinding>> bindingsByClass = loadRecommendedBindingsByClassId();
        Map<Long, SecLevel> levelMap = getLevelMap();
        Map<Long, Long> fieldCountMap = getSensitiveFieldCountByClassMap();

        Page<SecClassificationVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SecClassificationVO> vos = result.getRecords().stream()
                .map(c -> buildClassificationVO(c, ruleCountMap, fieldCountMap,
                        rulesByClassId.getOrDefault(c.getId(), Collections.emptyList()),
                        bindingsByClass.getOrDefault(c.getId(), Collections.emptyList()),
                        levelMap))
                .collect(Collectors.toList());
        voPage.setRecords(vos);
        return Result.success(voPage);
    }

    @Override
    public Result<SecClassificationDetailVO> getClassificationDetail(Long id) {
        SecClassification c = classificationMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(2001, "数据分类不存在");
        }

        LambdaQueryWrapper<SecSensitivityRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(SecSensitivityRule::getClassId, c.getId());
        List<SecSensitivityRule> classRules = ruleMapper.selectList(ruleWrapper);
        LambdaQueryWrapper<SecClassLevelBinding> bindWrapper = new LambdaQueryWrapper<>();
        bindWrapper.eq(SecClassLevelBinding::getClassId, c.getId())
                .eq(SecClassLevelBinding::getIsRecommended, 1);
        List<SecClassLevelBinding> classBindings = classLevelBindingMapper.selectList(bindWrapper);
        LevelDisplay strictest = resolveStrictestClassificationLevel(classRules, classBindings, getLevelMap());
        String sensitivityLevel = strictest.code();
        String sensitivityLevelLabel = strictest.label();

        SecClassificationDetailVO vo = SecClassificationDetailVO.builder()
                .id(c.getId())
                .classCode(c.getClassCode())
                .className(c.getClassName())
                .classDesc(c.getClassDesc())
                .classOrder(c.getClassOrder())
                .status(c.getStatus())
                .statusLabel(c.getStatus() != null && c.getStatus() == 1 ? "启用" : "禁用")
                .sensitivityLevel(sensitivityLevel)
                .sensitivityLevelLabel(sensitivityLevelLabel)
                .ruleCount(getRuleCountByClassMap().getOrDefault(id, 0L).intValue())
                .sensitiveFieldCount(getSensitiveFieldCountByClassMap().getOrDefault(id, 0L).intValue())
                .createUser(c.getCreateUser())
                .createTime(c.getCreateTime())
                .updateUser(c.getUpdateUser())
                .updateTime(c.getUpdateTime())
                .build();
        return Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enableClassification(Long id) {
        SecClassification existing = classificationMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据分类不存在");
        }
        SecClassification update = new SecClassification();
        update.setId(id);
        update.setStatus(1);
        update.setUpdateTime(LocalDateTime.now());
        classificationMapper.updateById(update);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disableClassification(Long id) {
        SecClassification existing = classificationMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据分类不存在");
        }
        SecClassification update = new SecClassification();
        update.setId(id);
        update.setStatus(0);
        update.setUpdateTime(LocalDateTime.now());
        classificationMapper.updateById(update);
        return Result.success();
    }

    @Override
    public Result<List<SecClassificationVO>> listClassification(SecClassificationQueryDTO queryDTO) {
        LambdaQueryWrapper<SecClassification> wrapper = buildClassificationQueryWrapper(queryDTO);
        wrapper.orderByAsc(SecClassification::getClassOrder)
               .orderByDesc(SecClassification::getCreateTime);
        List<SecClassification> list = classificationMapper.selectList(wrapper);
        List<SecSensitivityRule> allRules = ruleMapper.selectList(null);
        Map<Long, Long> ruleCountMap = allRules.stream()
                .filter(r -> r.getClassId() != null)
                .collect(Collectors.groupingBy(SecSensitivityRule::getClassId, Collectors.counting()));
        Map<Long, List<SecSensitivityRule>> rulesByClassId = allRules.stream()
                .filter(r -> r.getClassId() != null)
                .collect(Collectors.groupingBy(SecSensitivityRule::getClassId));
        Map<Long, List<SecClassLevelBinding>> bindingsByClass = loadRecommendedBindingsByClassId();
        Map<Long, SecLevel> levelMap = getLevelMap();
        Map<Long, Long> fieldCountMap = getSensitiveFieldCountByClassMap();
        List<SecClassificationVO> vos = list.stream()
                .map(c -> buildClassificationVO(c, ruleCountMap, fieldCountMap,
                        rulesByClassId.getOrDefault(c.getId(), Collections.emptyList()),
                        bindingsByClass.getOrDefault(c.getId(), Collections.emptyList()),
                        levelMap))
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    // ==================== 数据分级管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecLevel> saveLevel(SecLevelSaveDTO dto) {
        validateLevelDto(dto, null);
        SecLevel entity = new SecLevel();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setCreateTime(LocalDateTime.now());
        levelMapper.insert(entity);
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateLevel(Long id, SecLevelSaveDTO dto) {
        SecLevel existing = levelMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据分级不存在");
        }
        validateLevelDto(dto, id);
        SecLevel entity = new SecLevel();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        entity.setUpdateTime(LocalDateTime.now());
        levelMapper.updateById(entity);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteLevel(Long id) {
        SecLevel existing = levelMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据分级不存在");
        }
        levelMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDeleteLevel(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        levelMapper.deleteBatchIds(ids);
        return Result.success();
    }

    @Override
    public Result<SecLevel> getLevelById(Long id) {
        SecLevel entity = levelMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "数据分级不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<Page<SecLevelVO>> pageLevel(Integer pageNum, Integer pageSize) {
        Page<SecLevel> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SecLevel::getLevelValue);
        Page<SecLevel> result = levelMapper.selectPage(page, wrapper);

        Map<Long, Long> fieldCountMap = getSensitiveFieldCountByLevelMap();

        Page<SecLevelVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SecLevelVO> vos = result.getRecords().stream()
                .map(l -> buildLevelVO(l, fieldCountMap))
                .collect(Collectors.toList());
        voPage.setRecords(vos);
        return Result.success(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enableLevel(Long id) {
        SecLevel existing = levelMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据分级不存在");
        }
        SecLevel update = new SecLevel();
        update.setId(id);
        update.setStatus(1);
        update.setUpdateTime(LocalDateTime.now());
        levelMapper.updateById(update);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disableLevel(Long id) {
        SecLevel existing = levelMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据分级不存在");
        }
        SecLevel update = new SecLevel();
        update.setId(id);
        update.setStatus(0);
        update.setUpdateTime(LocalDateTime.now());
        levelMapper.updateById(update);
        return Result.success();
    }

    @Override
    public Result<List<SecLevelVO>> listLevel() {
        LambdaQueryWrapper<SecLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SecLevel::getLevelValue);
        List<SecLevel> list = levelMapper.selectList(wrapper);
        Map<Long, Long> fieldCountMap = getSensitiveFieldCountByLevelMap();
        List<SecLevelVO> vos = list.stream()
                .map(l -> buildLevelVO(l, fieldCountMap))
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    // ==================== 敏感字段识别规则管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecSensitivityRule> saveSensitivityRule(SecSensitivityRuleSaveDTO dto) {
        validateRuleDto(dto, null);
        SecSensitivityRule entity = new SecSensitivityRule();
        BeanUtils.copyProperties(dto, entity);
        entity.setBuiltin(0);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        entity.setCreateTime(LocalDateTime.now());
        ruleMapper.insert(entity);
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateSensitivityRule(Long id, SecSensitivityRuleSaveDTO dto) {
        SecSensitivityRule existing = ruleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "敏感字段识别规则不存在");
        }
        if (existing.getBuiltin() != null && existing.getBuiltin() == 1) {
            throw new BusinessException(2001, "内置规则不允许编辑");
        }
        validateRuleDto(dto, id);
        SecSensitivityRule entity = new SecSensitivityRule();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        entity.setBuiltin(0);
        entity.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(entity);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteSensitivityRule(Long id) {
        SecSensitivityRule existing = ruleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "敏感字段识别规则不存在");
        }
        if (existing.getBuiltin() != null && existing.getBuiltin() == 1) {
            throw new BusinessException(2001, "内置规则不允许删除");
        }
        ruleMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDeleteSensitivityRule(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        for (Long id : ids) {
            SecSensitivityRule existing = ruleMapper.selectById(id);
            if (existing != null && existing.getBuiltin() != null && existing.getBuiltin() == 1) {
                continue;
            }
            ruleMapper.deleteById(id);
        }
        return Result.success();
    }

    @Override
    public Result<SecSensitivityRule> getSensitivityRuleById(Long id) {
        SecSensitivityRule entity = ruleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "敏感字段识别规则不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<Page<SecSensitivityRuleVO>> pageSensitivityRule(Integer pageNum, Integer pageSize, SecSensitivityRuleQueryDTO queryDTO) {
        Page<SecSensitivityRule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecSensitivityRule> wrapper = buildRuleQueryWrapper(queryDTO);
        wrapper.orderByDesc(SecSensitivityRule::getPriority)
               .orderByDesc(SecSensitivityRule::getCreateTime);
        Page<SecSensitivityRule> result = ruleMapper.selectPage(page, wrapper);

        Map<Long, SecClassification> classMap = getClassificationMap();
        Map<Long, SecLevel> levelMap = getLevelMap();

        Page<SecSensitivityRuleVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SecSensitivityRuleVO> vos = result.getRecords().stream()
                .map(r -> buildRuleVO(r, classMap, levelMap))
                .collect(Collectors.toList());
        voPage.setRecords(vos);
        return Result.success(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enableSensitivityRule(Long id) {
        SecSensitivityRule existing = ruleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "敏感字段识别规则不存在");
        }
        SecSensitivityRule update = new SecSensitivityRule();
        update.setId(id);
        update.setStatus(1);
        update.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(update);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disableSensitivityRule(Long id) {
        SecSensitivityRule existing = ruleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "敏感字段识别规则不存在");
        }
        SecSensitivityRule update = new SecSensitivityRule();
        update.setId(id);
        update.setStatus(0);
        update.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(update);
        return Result.success();
    }

    @Override
    public Result<List<SecSensitivityRuleVO>> listSensitivityRule(SecSensitivityRuleQueryDTO queryDTO) {
        LambdaQueryWrapper<SecSensitivityRule> wrapper = buildRuleQueryWrapper(queryDTO);
        wrapper.orderByDesc(SecSensitivityRule::getPriority)
               .orderByDesc(SecSensitivityRule::getCreateTime);
        List<SecSensitivityRule> list = ruleMapper.selectList(wrapper);
        Map<Long, SecClassification> classMap = getClassificationMap();
        Map<Long, SecLevel> levelMap = getLevelMap();
        List<SecSensitivityRuleVO> vos = list.stream()
                .map(r -> buildRuleVO(r, classMap, levelMap))
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    // ==================== 字段敏感等级管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveColumnSensitivity(SecColumnSensitivitySaveDTO dto) {
        validateColumnSensitivityDto(dto);
        SecColumnSensitivity existing = findExistingSensitivity(dto.getDsId(), dto.getTableName(), dto.getColumnName());
        if (existing != null) {
            SecColumnSensitivity entity = new SecColumnSensitivity();
            BeanUtils.copyProperties(dto, entity);
            entity.setId(existing.getId());
            entity.setUpdateTime(LocalDateTime.now());
            sensitivityMapper.updateById(entity);
        } else {
            SecColumnSensitivity entity = new SecColumnSensitivity();
            BeanUtils.copyProperties(dto, entity);
            entity.setReviewStatus(ReviewStatusEnum.PENDING.getCode());
            entity.setCreateTime(LocalDateTime.now());
            sensitivityMapper.insert(entity);
        }
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchSaveColumnSensitivity(List<SecColumnSensitivitySaveDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Result.success();
        }
        for (SecColumnSensitivitySaveDTO dto : dtos) {
            saveColumnSensitivity(dto);
        }
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteColumnSensitivity(Long id) {
        sensitivityMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDeleteColumnSensitivity(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        sensitivityMapper.deleteBatchIds(ids);
        return Result.success();
    }

    @Override
    public Result<Page<SecColumnSensitivityVO>> pageColumnSensitivity(Integer pageNum, Integer pageSize, SecColumnSensitivityQueryDTO queryDTO) {
        Page<SecColumnSensitivity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecColumnSensitivity> wrapper = buildSensitivityQueryWrapper(queryDTO);
        wrapper.orderByDesc(SecColumnSensitivity::getCreateTime);
        Page<SecColumnSensitivity> result = sensitivityMapper.selectPage(page, wrapper);

        Map<Long, DqDatasource> dsMap = getDatasourceMap();
        Map<Long, SecClassification> classMap = getClassificationMap();
        Map<Long, SecLevel> levelMap = getLevelMap();
        Map<Long, SecSensitivityRule> ruleMap = getRuleMap();
        Map<Long, SysUser> userMap = getUserMap(result.getRecords());

        Page<SecColumnSensitivityVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SecColumnSensitivityVO> vos = result.getRecords().stream()
                .map(s -> buildSensitivityVO(s, dsMap, classMap, levelMap, ruleMap, userMap))
                .collect(Collectors.toList());
        voPage.setRecords(vos);
        return Result.success(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> reviewColumnSensitivity(Long id, String reviewStatus, String reviewComment, Long approvedBy) {
        SecColumnSensitivity existing = sensitivityMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "字段敏感等级记录不存在");
        }

        // 审核通过时：同步回写 gov_metadata_column.is_sensitive + sensitivity_level
        if (ReviewStatusEnum.APPROVED.getCode().equals(reviewStatus)) {
            writebackMetadataColumn(existing);
        }

        SecColumnSensitivity update = new SecColumnSensitivity();
        update.setId(id);
        update.setReviewStatus(reviewStatus);
        update.setReviewComment(reviewComment);
        update.setApprovedBy(approvedBy);
        update.setApprovedTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        sensitivityMapper.updateById(update);

        // 审核后：记录访问日志
        logSecurityAccess("REVIEW", existing.getDsId(), existing.getTableName(),
                existing.getColumnName(), "审核" + reviewStatus + "：" + reviewComment);

        // 审核通过后：触发 SENSITIVE_LEVEL_CHANGE 告警（等级降级时）
        if (ReviewStatusEnum.APPROVED.getCode().equals(reviewStatus)) {
            triggerSensityLevelChangeAlert(existing);
        }

        return Result.success();
    }

    /**
     * 审核通过后同步回写 gov_metadata_column
     * 将 is_sensitive 和 sensitivity_level 回填到元数据字段记录
     */
    private void writebackMetadataColumn(SecColumnSensitivity sensitivity) {
        if (sensitivity.getMetadataId() == null && !StringUtils.hasText(sensitivity.getTableName())) {
            return;
        }

        try {
            GovMetadataColumn columnRecord = null;

            // 优先通过 metadataId 查找
            if (sensitivity.getMetadataId() != null) {
                columnRecord = columnMapper.selectById(sensitivity.getMetadataId());
            }

            // 通过表名+字段名查找（兜底）
            if (columnRecord == null && StringUtils.hasText(sensitivity.getTableName())
                    && StringUtils.hasText(sensitivity.getColumnName())) {
                LambdaQueryWrapper<GovMetadataColumn> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(GovMetadataColumn::getColumnName, sensitivity.getColumnName());
                if (sensitivity.getMetadataId() != null) {
                    wrapper.eq(GovMetadataColumn::getMetadataId, sensitivity.getMetadataId());
                }
                columnRecord = columnMapper.selectOne(wrapper);
            }

            if (columnRecord == null) {
                log.debug("未找到对应的元数据字段记录，跳过回写: metadataId={}, table={}, column={}",
                        sensitivity.getMetadataId(), sensitivity.getTableName(), sensitivity.getColumnName());
                return;
            }

            // 回写 is_sensitive = 1
            GovMetadataColumn update = new GovMetadataColumn();
            update.setId(columnRecord.getId());
            update.setIsSensitive(true);

            // 回写 sensitivity_level（从 levelId 获取 levelCode）
            if (sensitivity.getLevelId() != null) {
                SecLevel level = levelMapper.selectById(sensitivity.getLevelId());
                if (level != null) {
                    update.setSensitivityLevel(level.getLevelCode());
                }
            }

            update.setUpdateTime(LocalDateTime.now());
            columnMapper.updateById(update);

            log.info("敏感字段审核通过，已回写 gov_metadata_column: id={}, isSensitive=1, level={}",
                    columnRecord.getId(), update.getSensitivityLevel());
        } catch (Exception e) {
            log.warn("回写 gov_metadata_column 失败（不影响审核流程）: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchReviewColumnSensitivity(List<Long> ids, String reviewStatus, String reviewComment, Long approvedBy) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        for (Long id : ids) {
            reviewColumnSensitivity(id, reviewStatus, reviewComment, approvedBy);
        }
        // 批量审核：记录汇总日志
        logSecurityAccess("REVIEW", null, null, null,
                String.format("批量审核 %d 条记录，状态=%s", ids.size(), reviewStatus));
        return Result.success();
    }

    @Override
    public Result<Long> resolveRecommendedApprover(Long dsId, String tableName, String columnName, Long levelId) {
        Long recommendedApprover = null;

        if (levelId == null) {
            return Result.success(null);
        }

        SecLevel level = levelMapper.selectById(levelId);
        if (level == null) {
            return Result.success(null);
        }

        String levelCode = level.getLevelCode();

        if ("L4".equals(levelCode)) {
            // L4 机密：强制指定审批人为 DAYU_ADMIN（系统管理员）
            recommendedApprover = findApproverByRoleCode(com.bagdatahouse.common.constant.GlobalConstants.ROLE_CODE_DAYU_ADMIN);
            if (recommendedApprover == null) {
                recommendedApprover = findApproverByRoleCode(com.bagdatahouse.common.constant.GlobalConstants.ROLE_CODE_SUPER_ADMIN);
            }
        } else if ("L3".equals(levelCode)) {
            // L3 敏感：自动取 gov_metadata.owner_id（数据 Owner）作为默认审批人
            recommendedApprover = findOwnerFromMetadata(dsId, tableName);
        }
        // L2/L1：可自行确认或组长审批，返回 null

        return Result.success(recommendedApprover);
    }

    /**
     * 根据角色代码查找审批人用户ID
     */
    private Long findApproverByRoleCode(String roleCode) {
        try {
            LambdaQueryWrapper<com.bagdatahouse.core.entity.SysRole> wrapper =
                    new LambdaQueryWrapper<>();
            wrapper.eq(com.bagdatahouse.core.entity.SysRole::getRoleCode, roleCode)
                   .eq(com.bagdatahouse.core.entity.SysRole::getStatus, 1);
            com.bagdatahouse.core.entity.SysRole role = roleMapper.selectOne(wrapper);
            if (role == null) {
                return null;
            }
            List<Long> userIds = userMapper.selectUserIdsByRoleId(role.getId());
            return userIds.isEmpty() ? null : userIds.get(0);
        } catch (Exception e) {
            log.debug("根据角色代码查找审批人失败: roleCode={}, error={}", roleCode, e.getMessage());
            return null;
        }
    }

    /**
     * 从元数据表查找数据 Owner（owner_id）
     */
    private Long findOwnerFromMetadata(Long dsId, String tableName) {
        try {
            if (!StringUtils.hasText(tableName)) {
                return null;
            }
            LambdaQueryWrapper<GovMetadata> wrapper = new LambdaQueryWrapper<>();
            if (dsId != null) {
                wrapper.eq(GovMetadata::getDsId, dsId);
            }
            wrapper.eq(GovMetadata::getTableName, tableName);
            GovMetadata metadata = metadataMapper.selectOne(wrapper);
            if (metadata != null && metadata.getOwnerId() != null) {
                return metadata.getOwnerId();
            }
            return null;
        } catch (Exception e) {
            log.debug("查找数据 Owner 失败: dsId={}, table={}, error={}", dsId, tableName, e.getMessage());
            return null;
        }
    }

    @Override
    public Result<List<SecColumnSensitivityVO>> getColumnSensitivityByTable(Long dsId, String tableName) {
        LambdaQueryWrapper<SecColumnSensitivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecColumnSensitivity::getDsId, dsId)
               .eq(SecColumnSensitivity::getTableName, tableName)
               .orderByDesc(SecColumnSensitivity::getCreateTime);
        List<SecColumnSensitivity> list = sensitivityMapper.selectList(wrapper);

        Map<Long, DqDatasource> dsMap = getDatasourceMap();
        Map<Long, SecClassification> classMap = getClassificationMap();
        Map<Long, SecLevel> levelMap = getLevelMap();
        Map<Long, SecSensitivityRule> ruleMap = getRuleMap();

        List<SecColumnSensitivityVO> vos = list.stream()
                .map(s -> buildSensitivityVO(s, dsMap, classMap, levelMap, ruleMap, null))
                .collect(Collectors.toList());

        // 敏感字段查询：记录访问日志
        logSecurityAccess("ACCESS", dsId, tableName, null,
                "查询敏感字段列表，表=" + tableName + "，命中" + vos.size() + "条");

        return Result.success(vos);
    }

    /**
     * 记录敏感字段访问/操作日志（sec_access_log）
     * 操作类型：SCAN-扫描 / REVIEW-审核 / MASK-脱敏 / ACCESS-访问
     */
    private void logSecurityAccess(String operationType, Long dsId, String tableName,
            String columnName, String detail) {
        try {
            SecAccessLog accessLog = new SecAccessLog();
            accessLog.setOperationType(operationType);
            accessLog.setTargetDsId(dsId);
            accessLog.setTargetTable(tableName);
            accessLog.setTargetColumn(columnName);
            accessLog.setOperationContent(detail);
            accessLog.setCreateTime(LocalDateTime.now());
            accessLogMapper.insert(accessLog);
        } catch (Exception e) {
            log.debug("记录安全访问日志失败: {}", e.getMessage());
        }
    }

    // ==================== 敏感字段扫描 ====================

    /**
     * 通过数据采样增强置信度评分（默认100条）
     */
    private BigDecimal buildConfidenceBySampling(Long dsId, String tableName, String columnName, SecSensitivityRule rule) {
        return buildConfidenceBySamplingWithLimit(dsId, null, tableName, columnName, rule, 100);
    }

    /**
     * PostgreSQL 等内容采样使用的 schema。
     * 直连扫描时列上带有 schemaName；否则仅当 directScan 时用 DTO 中的 schema（避免元数据扫描误用 UI 所选 schema）。
     */
    private String resolveSamplingSchema(SecSensitivityScanDTO dto, GovMetadataColumn column) {
        if (column != null && StringUtils.hasText(column.getSchemaName())) {
            return column.getSchemaName().trim();
        }
        if (dto != null && Boolean.TRUE.equals(dto.getDirectScan()) && StringUtils.hasText(dto.getSchema())) {
            return dto.getSchema().trim();
        }
        if (dynamicDataSourceManager != null && dto != null) {
            String s = dynamicDataSourceManager.getSchemaName(dto.getDsId());
            if (StringUtils.hasText(s)) {
                return s.trim();
            }
        }
        return null;
    }

    /**
     * 通过数据采样增强置信度评分（指定采样数量）
     * 使用 DataSourceAdapterRegistry 统一接口，支持所有数据源方言（MySQL/SQLServer/Oracle/PostgreSQL/TiDB）
     */
    private BigDecimal buildConfidenceBySamplingWithLimit(Long dsId, String schemaName, String tableName,
            String columnName, SecSensitivityRule rule, int limit) {
        if (adapterRegistry == null || dynamicDataSourceManager == null) {
            return null;
        }
        try {
            // 使用 DataSourceAdapterRegistry 获取对应数据源的适配器（自动处理方言差异）
            com.bagdatahouse.datasource.adapter.DataSourceAdapter adapter =
                    adapterRegistry.getAdapterById(dsId);
            if (adapter == null) {
                return null;
            }

            // 使用适配器的 sampleColumnValues（PostgreSQL 等必须传入 schema，否则会落到默认 public）
            int effectiveLimit = Math.min(Math.max(limit, 1), 200);
            List<Map<String, Object>> rows = adapter.sampleColumnValues(schemaName, tableName, columnName, effectiveLimit);
            if (rows == null || rows.isEmpty()) {
                return null;
            }

            int matchCount = 0;
            String matchType = rule.getMatchType();
            String expr = rule.getMatchExpr();
            String exprType = rule.getMatchExprType();

            for (Map<String, Object> row : rows) {
                Object val = row.values().iterator().next();
                if (val == null) continue;
                String value = val.toString().trim();
                if (value.isEmpty()) continue;

                boolean matched = false;
                if ("COLUMN_NAME".equals(matchType)) {
                    matched = valueMatchesPattern(value, rule.getMatchExpr(), rule.getMatchExprType());
                } else if ("COLUMN_COMMENT".equals(matchType)) {
                    matched = false;
                } else if ("REGEX".equals(matchType)) {
                    matched = valueMatchesPattern(value, expr, "REGEX");
                } else if ("DATA_TYPE".equals(matchType)) {
                    matched = valueMatchesPattern(value, expr, "CONTAINS");
                } else if ("BUILTIN_ALGORITHM".equals(matchType)) {
                    matched = valueMatchesPattern(value, expr, "BUILTIN_ALGORITHM");
                }

                if (matched) matchCount++;
            }

            double matchRate = (double) matchCount / rows.size();
            if (matchRate >= 0.8) {
                return BigDecimal.valueOf(95);
            } else if (matchRate >= 0.5) {
                return BigDecimal.valueOf(75);
            } else if (matchRate >= 0.2) {
                return BigDecimal.valueOf(50);
            }
            return null;
        } catch (Exception e) {
            log.debug("Data sampling failed for {}.{}: {}", tableName, columnName, e.getMessage());
            return null;
        }
    }

    /**
     * Check if a value matches a pattern expression (支持内置算法校验)
     * 新增对手机号、身份证、银行卡的算法级校验
     */
    private boolean valueMatchesPattern(String value, String expr, String exprType) {
        if (!StringUtils.hasText(expr)) return false;
        String lowerValue = value.toLowerCase();
        String lowerExpr = expr.toLowerCase();
        String[] keywords = lowerExpr.split("[,\\s]+");

        if ("CONTAINS".equals(exprType)) {
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (StringUtils.hasText(keyword) && lowerValue.contains(keyword)) {
                    return true;
                }
            }
            return false;
        } else if ("EQUALS".equals(exprType)) {
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (StringUtils.hasText(keyword) && lowerValue.equals(keyword)) {
                    return true;
                }
            }
            return false;
        } else if ("STARTS_WITH".equals(exprType)) {
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (StringUtils.hasText(keyword) && lowerValue.startsWith(keyword)) {
                    return true;
                }
            }
            return false;
        } else if ("REGEX".equals(exprType)) {
            try {
                return Pattern.matches(lowerExpr, lowerValue);
            } catch (Exception e) {
                log.warn("Invalid regex in sampling: {}", lowerExpr, e);
                return false;
            }
        } else if ("BUILTIN_ALGORITHM".equals(exprType)) {
            return applyBuiltinAlgorithmValidation(value, expr);
        }
        return false;
    }

    /**
     * 内置算法校验（对齐阿里内置识别算法）
     * @param value  待校验值
     * @param algorithm 算法类型：PHONE_VALIDATE / IDCARD_VALIDATE / BANKCARD_VALIDATE / EMAIL_FORMAT
     * @return true 表示校验通过（值符合该类型特征）
     */
    private boolean applyBuiltinAlgorithmValidation(String value, String algorithm) {
        if (maskingEngine == null || !StringUtils.hasText(value)) {
            return false;
        }
        return switch (algorithm.toUpperCase().trim()) {
            case "PHONE_VALIDATE" -> maskingEngine.isValidPhone(value);
            case "IDCARD_VALIDATE" -> maskingEngine.isValidIdCard(value);
            case "BANKCARD_VALIDATE" -> maskingEngine.isValidBankCard(value);
            case "EMAIL_FORMAT" -> maskingEngine.isValidEmail(value);
            case "IP_VALIDATE" -> maskingEngine.isValidIpv4(value);
            default -> false;
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecScanResultVO> scanSensitiveFields(SecSensitivityScanDTO dto) {
        long startTime = System.currentTimeMillis();
        String batchNo = "SCAN_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + System.currentTimeMillis() % 1000;

        DqDatasource datasource = datasourceMapper.selectById(dto.getDsId());
        if (datasource == null) {
            throw new BusinessException(2001, "数据源不存在");
        }

        SecScanResultVO scanResult = executeSensitiveFieldScan(dto, batchNo, datasource, startTime);
        return Result.success(scanResult);
    }

    /**
     * 执行敏感字段扫描核心逻辑（无 @Transactional，由调用方控制事务）
     */
    private SecScanResultVO executeSensitiveFieldScan(SecSensitivityScanDTO dto, String batchNo,
            DqDatasource datasource, long startTime) {

        List<GovMetadataColumn> columnsToScan = buildScanColumnList(dto);

        LambdaQueryWrapper<SecSensitivityRule> enabledRuleWrapper = new LambdaQueryWrapper<>();
        enabledRuleWrapper.eq(SecSensitivityRule::getStatus, 1);
        List<SecSensitivityRule> enabledRules = ruleMapper.selectList(enabledRuleWrapper);
        enabledRules.sort((a, b) -> {
            int pA = a.getPriority() != null ? a.getPriority() : 0;
            int pB = b.getPriority() != null ? b.getPriority() : 0;
            return Integer.compare(pB, pA);
        });

        Map<Long, SecClassification> classMap = getClassificationMap();
        Map<Long, SecLevel> levelMap = getLevelMap();

        Map<String, SecSensitivityRule> matchedRules = new HashMap<>();
        List<SecColumnSensitivityVO> foundSensitiveFields = new ArrayList<>();

        Set<String> scannedTables = columnsToScan.stream()
                .map(GovMetadataColumn::getTableName)
                .collect(Collectors.toSet());
        Map<String, SecColumnSensitivity> existingByKey = loadExistingSensitivityMap(dto.getDsId(), scannedTables);

        for (GovMetadataColumn column : columnsToScan) {
            SecSensitivityRule bestMatch = null;
            for (SecSensitivityRule rule : enabledRules) {
                if (matchColumn(rule, column, dto)) {
                    bestMatch = rule;
                    break;
                }
            }
            if (bestMatch != null) {
                SecClassification classification = classMap.get(bestMatch.getClassId());
                SecLevel level = levelMap.get(bestMatch.getLevelId());

                // 内容级扫描（默认开启，enableContentScan=true 时使用数据采样增强置信度）
                BigDecimal enhancedConfidence = null;
                if (Boolean.TRUE.equals(dto.getEnableContentScan())) {
                    int effectiveSampleSize = dto.getSampleSize() != null && dto.getSampleSize() > 0
                            ? Math.min(dto.getSampleSize(), 200) : 50;
                    String samplingSchema = resolveSamplingSchema(dto, column);
                    enhancedConfidence = buildConfidenceBySamplingWithLimit(
                            dto.getDsId(), samplingSchema, column.getTableName(), column.getColumnName(),
                            bestMatch, effectiveSampleSize);
                }
                BigDecimal finalConfidence = enhancedConfidence != null
                        ? enhancedConfidence
                        : defaultConfidenceFromRule(bestMatch);

                        SecColumnSensitivitySaveDTO saveDto = SecColumnSensitivitySaveDTO.builder()
                                .dsId(dto.getDsId())
                                .metadataId(column.getMetadataId())
                                .tableName(column.getTableName())
                                .columnName(column.getColumnName())
                                .columnComment(column.getColumnComment())
                                .dataType(column.getDataType())
                                .classId(bestMatch.getClassId())
                                .levelId(bestMatch.getLevelId())
                                .matchRuleId(bestMatch.getId())
                                .maskType(bestMatch.getSuggestionAction())
                                .maskPattern(bestMatch.getSuggestionMaskPattern())
                                .maskPosition(bestMatch.getSuggestionMaskType())
                                .confidence(finalConfidence)
                                .scanBatchNo(batchNo)
                                .createUser(dto.getCreateUser())
                                .build();

                String colKey = column.getTableName() + "\0" + column.getColumnName();
                SecColumnSensitivity existing = existingByKey.get(colKey);
                if (existing != null) {
                    saveDto.setId(existing.getId());
                    SecColumnSensitivity entity = new SecColumnSensitivity();
                    BeanUtils.copyProperties(saveDto, entity);
                    entity.setUpdateTime(LocalDateTime.now());
                    sensitivityMapper.updateById(entity);
                } else {
                    SecColumnSensitivity entity = new SecColumnSensitivity();
                    BeanUtils.copyProperties(saveDto, entity);
                    entity.setReviewStatus(ReviewStatusEnum.PENDING.getCode());
                    entity.setScanTime(LocalDateTime.now());
                    entity.setCreateTime(LocalDateTime.now());
                    sensitivityMapper.insert(entity);
                }

                SecColumnSensitivityVO vo = SecColumnSensitivityVO.builder()
                        .dsId(dto.getDsId())
                        .dsName(datasource.getDsName())
                        .tableName(column.getTableName())
                        .columnName(column.getColumnName())
                        .columnComment(column.getColumnComment())
                        .dataType(column.getDataType())
                        .classId(bestMatch.getClassId())
                        .className(classification != null ? classification.getClassName() : null)
                        .levelId(bestMatch.getLevelId())
                        .levelName(level != null ? level.getLevelName() : null)
                        .levelColor(level != null ? level.getColor() : null)
                        .matchRuleId(bestMatch.getId())
                        .matchRuleName(bestMatch.getRuleName())
                        .maskType(bestMatch.getSuggestionAction())
                        .maskTypeLabel(MaskTypeEnum.getLabelByCode(bestMatch.getSuggestionAction()))
                        .maskPattern(bestMatch.getSuggestionMaskPattern())
                        .maskPosition(bestMatch.getSuggestionMaskType())
                        .confidence(finalConfidence)
                        .confidenceLabel(getConfidenceLabel(finalConfidence))
                        .scanBatchNo(batchNo)
                        .scanTime(LocalDateTime.now())
                        .reviewStatus(ReviewStatusEnum.PENDING.getCode())
                        .reviewStatusLabel(ReviewStatusEnum.PENDING.getLabel())
                        .build();
                foundSensitiveFields.add(vo);
            }
        }

        Map<String, Integer> countByLevel = foundSensitiveFields.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getLevelName() != null ? v.getLevelName() : "未知",
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> countByClass = foundSensitiveFields.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getClassName() != null ? v.getClassName() : "未知",
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        long endTime = System.currentTimeMillis();

        SecScanResultVO scanResult = SecScanResultVO.builder()
                .scanBatchNo(batchNo)
                .startTime(LocalDateTime.now().minusNanos((endTime - startTime) * 1_000_000).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .endTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .elapsedMs(endTime - startTime)
                .dsId(dto.getDsId())
                .dsName(datasource.getDsName())
                .totalTableCount(scannedTables.size())
                .totalColumnCount(columnsToScan.size())
                .foundSensitiveCount(foundSensitiveFields.size())
                .results(foundSensitiveFields)
                .countByLevel(countByLevel)
                .countByClassification(countByClass)
                .build();

        // 扫描完成后：记录扫描日志 + 触发 SENSITIVE_FIELD_SPIKE 突增告警
        logSecurityAccess("SCAN", dto.getDsId(), null, null,
                String.format("扫描完成，批次号=%s，发现敏感字段 %d 个", batchNo, foundSensitiveFields.size()));
        triggerSensityFieldSpikeAlert(dto.getDsId(), foundSensitiveFields.size(), batchNo);

        return scanResult;
    }

    // ==================== 脱敏模板管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecMaskTemplate> saveMaskTemplate(SecMaskTemplateSaveDTO dto) {
        validateMaskTemplateDto(dto, null);
        SecMaskTemplate entity = new SecMaskTemplate();
        BeanUtils.copyProperties(dto, entity);
        entity.setBuiltin(0);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setCreateTime(LocalDateTime.now());
        maskTemplateMapper.insert(entity);
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateMaskTemplate(Long id, SecMaskTemplateSaveDTO dto) {
        SecMaskTemplate existing = maskTemplateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "脱敏模板不存在");
        }
        if (existing.getBuiltin() != null && existing.getBuiltin() == 1) {
            throw new BusinessException(2001, "内置模板不允许编辑");
        }
        validateMaskTemplateDto(dto, id);
        SecMaskTemplate entity = new SecMaskTemplate();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        entity.setBuiltin(0);
        entity.setUpdateTime(LocalDateTime.now());
        maskTemplateMapper.updateById(entity);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteMaskTemplate(Long id) {
        SecMaskTemplate existing = maskTemplateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "脱敏模板不存在");
        }
        if (existing.getBuiltin() != null && existing.getBuiltin() == 1) {
            throw new BusinessException(2001, "内置模板不允许删除");
        }
        maskTemplateMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDeleteMaskTemplate(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        for (Long id : ids) {
            SecMaskTemplate existing = maskTemplateMapper.selectById(id);
            if (existing != null && existing.getBuiltin() != null && existing.getBuiltin() == 1) {
                continue;
            }
            maskTemplateMapper.deleteById(id);
        }
        return Result.success();
    }

    @Override
    public Result<SecMaskTemplate> getMaskTemplateById(Long id) {
        SecMaskTemplate entity = maskTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "脱敏模板不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<Page<SecMaskTemplateVO>> pageMaskTemplate(Integer pageNum, Integer pageSize, Integer enabled) {
        Page<SecMaskTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecMaskTemplate> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(SecMaskTemplate::getStatus, enabled);
        }
        wrapper.orderByAsc(SecMaskTemplate::getBuiltin)
               .orderByDesc(SecMaskTemplate::getCreateTime);
        Page<SecMaskTemplate> result = maskTemplateMapper.selectPage(page, wrapper);

        Page<SecMaskTemplateVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SecMaskTemplateVO> vos = result.getRecords().stream()
                .map(this::buildMaskTemplateVO)
                .collect(Collectors.toList());
        voPage.setRecords(vos);
        return Result.success(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enableMaskTemplate(Long id) {
        SecMaskTemplate existing = maskTemplateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "脱敏模板不存在");
        }
        SecMaskTemplate update = new SecMaskTemplate();
        update.setId(id);
        update.setStatus(1);
        update.setUpdateTime(LocalDateTime.now());
        maskTemplateMapper.updateById(update);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disableMaskTemplate(Long id) {
        SecMaskTemplate existing = maskTemplateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "脱敏模板不存在");
        }
        SecMaskTemplate update = new SecMaskTemplate();
        update.setId(id);
        update.setStatus(0);
        update.setUpdateTime(LocalDateTime.now());
        maskTemplateMapper.updateById(update);
        return Result.success();
    }

    @Override
    public Result<List<SecMaskTemplateVO>> listMaskTemplate(Integer enabled) {
        LambdaQueryWrapper<SecMaskTemplate> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(SecMaskTemplate::getStatus, enabled);
        }
        wrapper.orderByAsc(SecMaskTemplate::getBuiltin)
               .orderByAsc(SecMaskTemplate::getSortOrder);
        List<SecMaskTemplate> list = maskTemplateMapper.selectList(wrapper);
        List<SecMaskTemplateVO> vos = list.stream()
                .map(this::buildMaskTemplateVO)
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    // ==================== 统计 ====================

    @Override
    public Result<SecStatsVO> getStats() {
        long totalClassification = classificationMapper.selectCount(null);
        long totalLevel = levelMapper.selectCount(null);

        long totalRule = ruleMapper.selectCount(null);
        LambdaQueryWrapper<SecSensitivityRule> builtinWrapper = new LambdaQueryWrapper<>();
        builtinWrapper.eq(SecSensitivityRule::getBuiltin, 1);
        long builtinRule = ruleMapper.selectCount(builtinWrapper);
        long customRule = totalRule - builtinRule;

        long totalSensitiveField = sensitivityMapper.selectCount(null);

        LambdaQueryWrapper<SecColumnSensitivity> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(SecColumnSensitivity::getReviewStatus, ReviewStatusEnum.PENDING.getCode());
        long pendingReview = sensitivityMapper.selectCount(pendingWrapper);

        LambdaQueryWrapper<SecColumnSensitivity> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.eq(SecColumnSensitivity::getReviewStatus, ReviewStatusEnum.APPROVED.getCode());
        long approved = sensitivityMapper.selectCount(approvedWrapper);

        LambdaQueryWrapper<SecColumnSensitivity> rejectedWrapper = new LambdaQueryWrapper<>();
        rejectedWrapper.eq(SecColumnSensitivity::getReviewStatus, ReviewStatusEnum.REJECTED.getCode());
        long rejected = sensitivityMapper.selectCount(rejectedWrapper);

        long totalMaskTemplate = maskTemplateMapper.selectCount(null);

        Map<String, Long> countByClassification = buildClassificationCountMap();
        Map<String, Long> countByLevel = buildLevelCountMap();
        Map<String, Long> countByReviewStatus = new HashMap<>();
        countByReviewStatus.put(ReviewStatusEnum.PENDING.getLabel(), pendingReview);
        countByReviewStatus.put(ReviewStatusEnum.APPROVED.getLabel(), approved);
        countByReviewStatus.put(ReviewStatusEnum.REJECTED.getLabel(), rejected);

        Map<String, Long> countByMaskType = buildMaskTypeCountMap();
        Map<String, Long> countByMatchType = buildMatchTypeCountMap();

        SecStatsVO stats = SecStatsVO.builder()
                .totalClassificationCount(totalClassification)
                .totalLevelCount(totalLevel)
                .totalRuleCount(totalRule)
                .builtinRuleCount(builtinRule)
                .customRuleCount(customRule)
                .totalSensitiveFieldCount(totalSensitiveField)
                .pendingReviewCount(pendingReview)
                .approvedCount(approved)
                .rejectedCount(rejected)
                .totalMaskTemplateCount(totalMaskTemplate)
                .countByClassification(countByClassification)
                .countByLevel(countByLevel)
                .countByReviewStatus(countByReviewStatus)
                .countByMaskType(countByMaskType)
                .countByMatchType(countByMatchType)
                .build();

        return Result.success(stats);
    }

    @Override
    public Result<List<Map<String, Object>>> getSensitivityStatsByDs() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<DqDatasource> datasources = datasourceMapper.selectList(null);
        for (DqDatasource ds : datasources) {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("dsId", ds.getId());
            stat.put("dsName", ds.getDsName());
            stat.put("dsType", ds.getDsType());
            LambdaQueryWrapper<SecColumnSensitivity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SecColumnSensitivity::getDsId, ds.getId());
            long total = sensitivityMapper.selectCount(wrapper);
            stat.put("totalCount", total);

            LambdaQueryWrapper<SecColumnSensitivity> pendingWrapper = new LambdaQueryWrapper<>();
            pendingWrapper.eq(SecColumnSensitivity::getDsId, ds.getId())
                    .eq(SecColumnSensitivity::getReviewStatus, ReviewStatusEnum.PENDING.getCode());
            stat.put("pendingCount", sensitivityMapper.selectCount(pendingWrapper));

            LambdaQueryWrapper<GovMetadata> metaWrapper = new LambdaQueryWrapper<>();
            metaWrapper.eq(GovMetadata::getDsId, ds.getId());
            stat.put("tableCount", metadataMapper.selectCount(metaWrapper));
            result.add(stat);
        }
        return Result.success(result);
    }

    @Override
    public Result<Map<String, Long>> getSensitivityReviewCounts() {
        Map<String, Object> raw = sensitivityMapper.selectReviewStatusAggregate();
        Map<String, Long> out = new LinkedHashMap<>();
        out.put("total", toAggregateLong(raw, "total"));
        out.put("pending", toAggregateLong(raw, "pending"));
        out.put("approved", toAggregateLong(raw, "approved"));
        out.put("rejected", toAggregateLong(raw, "rejected"));
        return Result.success(out);
    }

    @Override
    public Result<Map<String, Object>> getEnums() {
        Map<String, Object> enums = new LinkedHashMap<>();

        // 敏感级别
        List<Map<String, String>> levels = new ArrayList<>();
        for (SensitivityLevelEnum e : SensitivityLevelEnum.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", e.getCode());
            item.put("label", e.getLabel());
            item.put("value", String.valueOf(e.getValue()));
            item.put("color", e.getColor());
            levels.add(item);
        }
        enums.put("sensitivityLevel", levels);

        // 脱敏类型
        List<Map<String, String>> maskTypes = new ArrayList<>();
        for (MaskTypeEnum e : MaskTypeEnum.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", e.getCode());
            item.put("label", e.getLabel());
            maskTypes.add(item);
        }
        enums.put("maskType", maskTypes);

        // 匹配类型
        List<Map<String, String>> matchTypes = new ArrayList<>();
        for (MatchTypeEnum e : MatchTypeEnum.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", e.getCode());
            item.put("label", e.getLabel());
            matchTypes.add(item);
        }
        enums.put("matchType", matchTypes);

        // 审核状态
        List<Map<String, String>> reviewStatuses = new ArrayList<>();
        for (ReviewStatusEnum e : ReviewStatusEnum.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", e.getCode());
            item.put("label", e.getLabel());
            reviewStatuses.add(item);
        }
        enums.put("reviewStatus", reviewStatuses);

        // 扫描范围
        List<Map<String, String>> scanScopes = new ArrayList<>();
        for (String s : new String[]{"ALL", "LAYER", "SPECIFIC"}) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", s);
            item.put("label", s.equals("ALL") ? "全部" : s.equals("LAYER") ? "按数据层" : "指定表");
            scanScopes.add(item);
        }
        enums.put("scanScope", scanScopes);

        // 数据层（数仓分层）
        List<Map<String, String>> dataLayers = new ArrayList<>();
        for (String s : new String[]{"ODS", "DWD", "DWS", "DIM", "DM", "ADS"}) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", s);
            String label = switch (s) {
                case "ODS" -> "ODS - 操作数据层";
                case "DWD" -> "DWD - 明细事实层";
                case "DWS" -> "DWS - 汇总事实层";
                case "DIM" -> "DIM - 公共维度层";
                case "DM" -> "DM - 数据集市层";
                case "ADS" -> "ADS - 应用层";
                default -> s;
            };
            item.put("label", label);
            dataLayers.add(item);
        }
        enums.put("dataLayer", dataLayers);

        // 表达式类型
        List<Map<String, String>> exprTypes = new ArrayList<>();
        for (String s : new String[]{"CONTAINS", "EQUALS", "STARTS_WITH", "REGEX"}) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", s);
            item.put("label", s.equals("CONTAINS") ? "包含" : s.equals("EQUALS") ? "等于" : s.equals("STARTS_WITH") ? "开头是" : "正则匹配");
            exprTypes.add(item);
        }
        enums.put("exprType", exprTypes);

        // 适用数据类型
        List<Map<String, String>> dataTypes = new ArrayList<>();
        for (String s : new String[]{"STRING", "NUMBER", "DATE", "ALL"}) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", s);
            String label = switch (s) {
                case "STRING" -> "字符串";
                case "NUMBER" -> "数值";
                case "DATE" -> "日期";
                case "ALL" -> "全部";
                default -> s;
            };
            item.put("label", label);
            dataTypes.add(item);
        }
        enums.put("dataType", dataTypes);

        // 脱敏位置
        List<Map<String, String>> maskPositions = new ArrayList<>();
        for (String s : new String[]{"CENTER", "HEAD", "TAIL", "FULL"}) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", s);
            String label = switch (s) {
                case "CENTER" -> "中间遮蔽";
                case "HEAD" -> "头部遮蔽";
                case "TAIL" -> "尾部遮蔽";
                case "FULL" -> "完全隐藏";
                default -> s;
            };
            item.put("label", label);
            maskPositions.add(item);
        }
        enums.put("maskPosition", maskPositions);

        return Result.success(enums);
    }

    @Override
    public Result<SecColumnSensitivityVO> getSensitivityById(Long id) {
        SecColumnSensitivity s = sensitivityMapper.selectById(id);
        if (s == null) {
            return Result.success(null);
        }
        Map<Long, DqDatasource> dsMap = getDatasourceMap();
        Map<Long, SecClassification> classMap = getClassificationMap();
        Map<Long, SecLevel> levelMap = getLevelMap();
        Map<Long, SecSensitivityRule> ruleMap = getRuleMap();
        Map<Long, SysUser> userMap = new HashMap<>();
        SecColumnSensitivityVO vo = buildSensitivityVO(s, dsMap, classMap, levelMap, ruleMap, userMap);
        return Result.success(vo);
    }

    @Override
    public Result<List<SecColumnSensitivityVO>> getByBatchNo(String batchNo) {
        LambdaQueryWrapper<SecColumnSensitivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecColumnSensitivity::getScanBatchNo, batchNo);
        List<SecColumnSensitivity> list = sensitivityMapper.selectList(wrapper);
        Map<Long, DqDatasource> dsMap = getDatasourceMap();
        Map<Long, SecClassification> classMap = getClassificationMap();
        Map<Long, SecLevel> levelMap = getLevelMap();
        Map<Long, SecSensitivityRule> ruleMap = getRuleMap();
        Map<Long, SysUser> userMap = getUserMap(list);
        List<SecColumnSensitivityVO> vos = list.stream()
                .map(s -> buildSensitivityVO(s, dsMap, classMap, levelMap, ruleMap, userMap))
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    // ==================== Private Helper Methods ====================

    private LambdaQueryWrapper<SecClassification> buildClassificationQueryWrapper(SecClassificationQueryDTO queryDTO) {
        LambdaQueryWrapper<SecClassification> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO == null) {
            return wrapper;
        }
        if (StringUtils.hasText(queryDTO.getClassCode())) {
            wrapper.eq(SecClassification::getClassCode, queryDTO.getClassCode());
        }
        if (StringUtils.hasText(queryDTO.getClassName())) {
            wrapper.like(SecClassification::getClassName, queryDTO.getClassName());
        }
        if (queryDTO.getEnabled() != null) {
            wrapper.eq(SecClassification::getStatus, queryDTO.getEnabled());
        } else if (queryDTO.getStatus() != null) {
            wrapper.eq(SecClassification::getStatus, queryDTO.getStatus());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<SecSensitivityRule> buildRuleQueryWrapper(SecSensitivityRuleQueryDTO queryDTO) {
        LambdaQueryWrapper<SecSensitivityRule> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO == null) {
            return wrapper;
        }
        if (StringUtils.hasText(queryDTO.getRuleCode())) {
            wrapper.eq(SecSensitivityRule::getRuleCode, queryDTO.getRuleCode());
        }
        if (StringUtils.hasText(queryDTO.getRuleName())) {
            wrapper.like(SecSensitivityRule::getRuleName, queryDTO.getRuleName());
        }
        if (queryDTO.getClassId() != null) {
            wrapper.eq(SecSensitivityRule::getClassId, queryDTO.getClassId());
        }
        if (queryDTO.getLevelId() != null) {
            wrapper.eq(SecSensitivityRule::getLevelId, queryDTO.getLevelId());
        }
        if (StringUtils.hasText(queryDTO.getMatchType())) {
            wrapper.eq(SecSensitivityRule::getMatchType, queryDTO.getMatchType());
        }
        if (queryDTO.getBuiltin() != null) {
            wrapper.eq(SecSensitivityRule::getBuiltin, queryDTO.getBuiltin());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(SecSensitivityRule::getStatus, queryDTO.getStatus());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<SecColumnSensitivity> buildSensitivityQueryWrapper(SecColumnSensitivityQueryDTO queryDTO) {
        LambdaQueryWrapper<SecColumnSensitivity> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO == null) {
            return wrapper;
        }
        if (queryDTO.getId() != null) {
            wrapper.eq(SecColumnSensitivity::getId, queryDTO.getId());
        }
        if (queryDTO.getDsId() != null) {
            wrapper.eq(SecColumnSensitivity::getDsId, queryDTO.getDsId());
        }
        if (StringUtils.hasText(queryDTO.getTableName())) {
            wrapper.like(SecColumnSensitivity::getTableName, queryDTO.getTableName());
        }
        if (StringUtils.hasText(queryDTO.getColumnName())) {
            wrapper.like(SecColumnSensitivity::getColumnName, queryDTO.getColumnName());
        }
        if (queryDTO.getClassId() != null) {
            wrapper.eq(SecColumnSensitivity::getClassId, queryDTO.getClassId());
        }
        if (queryDTO.getLevelId() != null) {
            wrapper.eq(SecColumnSensitivity::getLevelId, queryDTO.getLevelId());
        }
        if (StringUtils.hasText(queryDTO.getReviewStatus())) {
            wrapper.eq(SecColumnSensitivity::getReviewStatus, queryDTO.getReviewStatus());
        }
        if (StringUtils.hasText(queryDTO.getScanBatchNo())) {
            wrapper.eq(SecColumnSensitivity::getScanBatchNo, queryDTO.getScanBatchNo());
        }
        return wrapper;
    }

    private void validateClassificationDto(SecClassificationSaveDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getClassCode())) {
            throw new BusinessException(2001, "分类编码不能为空");
        }
        if (!StringUtils.hasText(dto.getClassName())) {
            throw new BusinessException(2001, "分类名称不能为空");
        }
        LambdaQueryWrapper<SecClassification> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(SecClassification::getClassCode, dto.getClassCode());
        if (excludeId != null) {
            codeWrapper.ne(SecClassification::getId, excludeId);
        }
        if (classificationMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException(2001, "分类编码已存在");
        }
    }

    private void validateLevelDto(SecLevelSaveDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getLevelCode())) {
            throw new BusinessException(2001, "分级编码不能为空");
        }
        if (!StringUtils.hasText(dto.getLevelName())) {
            throw new BusinessException(2001, "分级名称不能为空");
        }
        if (dto.getLevelValue() == null || dto.getLevelValue() < 1 || dto.getLevelValue() > 4) {
            throw new BusinessException(2001, "等级值必须在 1-4 之间");
        }
        LambdaQueryWrapper<SecLevel> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(SecLevel::getLevelCode, dto.getLevelCode());
        if (excludeId != null) {
            codeWrapper.ne(SecLevel::getId, excludeId);
        }
        if (levelMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException(2001, "分级编码已存在");
        }
        LambdaQueryWrapper<SecLevel> valueWrapper = new LambdaQueryWrapper<>();
        valueWrapper.eq(SecLevel::getLevelValue, dto.getLevelValue());
        if (excludeId != null) {
            valueWrapper.ne(SecLevel::getId, excludeId);
        }
        if (levelMapper.selectCount(valueWrapper) > 0) {
            throw new BusinessException(2001, "等级值已存在");
        }
    }

    private void validateRuleDto(SecSensitivityRuleSaveDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getRuleCode())) {
            throw new BusinessException(2001, "规则编码不能为空");
        }
        if (!StringUtils.hasText(dto.getRuleName())) {
            throw new BusinessException(2001, "规则名称不能为空");
        }
        if (!StringUtils.hasText(dto.getMatchExpr())) {
            throw new BusinessException(2001, "匹配表达式不能为空");
        }
        LambdaQueryWrapper<SecSensitivityRule> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(SecSensitivityRule::getRuleCode, dto.getRuleCode());
        if (excludeId != null) {
            codeWrapper.ne(SecSensitivityRule::getId, excludeId);
        }
        if (ruleMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException(2001, "规则编码已存在");
        }
    }

    private void validateColumnSensitivityDto(SecColumnSensitivitySaveDTO dto) {
        if (dto.getDsId() == null) {
            throw new BusinessException(2001, "数据源ID不能为空");
        }
        if (!StringUtils.hasText(dto.getTableName())) {
            throw new BusinessException(2001, "表名不能为空");
        }
        if (!StringUtils.hasText(dto.getColumnName())) {
            throw new BusinessException(2001, "字段名不能为空");
        }
    }

    private void validateMaskTemplateDto(SecMaskTemplateSaveDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getTemplateCode())) {
            throw new BusinessException(2001, "模板编码不能为空");
        }
        if (!StringUtils.hasText(dto.getTemplateName())) {
            throw new BusinessException(2001, "模板名称不能为空");
        }
        if (!StringUtils.hasText(dto.getMaskType())) {
            throw new BusinessException(2001, "脱敏类型不能为空");
        }
        LambdaQueryWrapper<SecMaskTemplate> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(SecMaskTemplate::getTemplateCode, dto.getTemplateCode());
        if (excludeId != null) {
            codeWrapper.ne(SecMaskTemplate::getId, excludeId);
        }
        if (maskTemplateMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException(2001, "模板编码已存在");
        }
    }

    private boolean matchColumn(SecSensitivityRule rule, GovMetadataColumn column, SecSensitivityScanDTO dto) {
        if (rule == null || column == null) {
            return false;
        }
        String matchType = rule.getMatchType();
        String expr = rule.getMatchExpr();
        String exprType = rule.getMatchExprType();

        if (!StringUtils.hasText(expr)) {
            return false;
        }

        // Respect scan dimension flags from DTO
        boolean scanColumnName = dto.getScanColumnName() == null || dto.getScanColumnName();
        boolean scanColumnComment = dto.getScanColumnComment() == null || dto.getScanColumnComment();
        boolean scanDataType = dto.getScanDataType() != null && dto.getScanDataType();
        boolean useRegex = dto.getUseRegex() == null || dto.getUseRegex();

        if ("COLUMN_NAME".equals(matchType)) {
            if (!scanColumnName) return false;
            String columnName = column.getColumnName() != null ? column.getColumnName().toLowerCase() : "";
            String[] keywords = expr.toLowerCase().split("[,\\s]+");
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (!StringUtils.hasText(keyword)) continue;
                if ("CONTAINS".equals(exprType)) {
                    if (columnName.contains(keyword)) return true;
                } else if ("EQUALS".equals(exprType)) {
                    if (columnName.equals(keyword)) return true;
                } else if ("STARTS_WITH".equals(exprType)) {
                    if (columnName.startsWith(keyword)) return true;
                } else if ("REGEX".equals(exprType) && useRegex) {
                    try {
                        if (Pattern.matches(expr, columnName)) return true;
                    } catch (Exception e) {
                        log.warn("Invalid regex pattern in COLUMN_NAME: {}", expr, e);
                    }
                }
            }
            return false;
        } else if ("COLUMN_COMMENT".equals(matchType)) {
            if (!scanColumnComment) return false;
            String comment = column.getColumnComment() != null ? column.getColumnComment().toLowerCase() : "";
            String[] keywords = expr.toLowerCase().split("[,\\s]+");
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (!StringUtils.hasText(keyword)) continue;
                if ("CONTAINS".equals(exprType)) {
                    if (comment.contains(keyword)) return true;
                } else if ("EQUALS".equals(exprType)) {
                    if (comment.equals(keyword)) return true;
                } else if ("REGEX".equals(exprType) && useRegex) {
                    try {
                        if (Pattern.matches(expr, comment)) return true;
                    } catch (Exception e) {
                        log.warn("Invalid regex pattern in COLUMN_COMMENT: {}", expr, e);
                    }
                }
            }
            return false;
        } else if ("REGEX".equals(matchType)) {
            if (!useRegex) return false;
            try {
                Pattern pattern = Pattern.compile(expr, Pattern.CASE_INSENSITIVE);
                String columnName = column.getColumnName() != null ? column.getColumnName() : "";
                String comment = column.getColumnComment() != null ? column.getColumnComment() : "";
                boolean matchedColumn = scanColumnName && pattern.matcher(columnName).find();
                boolean matchedComment = scanColumnComment && pattern.matcher(comment).find();
                return matchedColumn || matchedComment;
            } catch (Exception e) {
                log.warn("Invalid regex pattern: {}", expr, e);
                return false;
            }
        } else if ("DATA_TYPE".equals(matchType)) {
            if (!scanDataType) return false;
            String dataType = column.getDataType() != null ? column.getDataType().toLowerCase() : "";
            String[] keywords = expr.toLowerCase().split("[,\\s]+");
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (!StringUtils.hasText(keyword)) continue;
                if (dataType.contains(keyword)) return true;
            }
            return false;
        } else if ("CONTENT_REGEX".equals(matchType)) {
            // 内容级正则匹配：同时检查字段名和注释（内容级扫描会在采样阶段补充验证）
            if (!useRegex) return false;
            try {
                Pattern pattern = Pattern.compile(expr, Pattern.CASE_INSENSITIVE);
                String columnName = column.getColumnName() != null ? column.getColumnName() : "";
                String comment = column.getColumnComment() != null ? column.getColumnComment() : "";
                boolean matchedColumn = scanColumnName && pattern.matcher(columnName).find();
                boolean matchedComment = scanColumnComment && pattern.matcher(comment).find();
                return matchedColumn || matchedComment;
            } catch (Exception e) {
                log.warn("CONTENT_REGEX 无效正则: {}", expr, e);
                return false;
            }
        }
        // BUILTIN_ALGORITHM（手机号/身份证/银行卡算法）仅在采样阶段验证，不在元数据匹配阶段处理
        return false;
    }

    private List<GovMetadataColumn> buildScanColumnList(SecSensitivityScanDTO dto) {
        // 模式0：单字段扫描（用于新字段一键扫描）
        if ("SINGLE_COLUMN".equals(dto.getScanMode()) && dto.getColumnNames() != null && !dto.getColumnNames().isEmpty()) {
            return buildColumnListForSingleColumns(dto);
        }

        // 模式1：直连数据源扫描（无需先采集元数据）
        if (Boolean.TRUE.equals(dto.getDirectScan())) {
            return buildColumnListByDirectScan(dto);
        }

        // 模式2：基于已采集元数据扫描
        List<GovMetadataColumn> columns = new ArrayList<>();

        if ("SPECIFIC".equals(dto.getScanScope()) && StringUtils.hasText(dto.getTableNames())) {
            String[] tableNames = dto.getTableNames().split("[,\\s]+");
            for (String tableName : tableNames) {
                tableName = tableName.trim();
                if (!StringUtils.hasText(tableName)) continue;
                LambdaQueryWrapper<GovMetadataColumn> wrapper = new LambdaQueryWrapper<>();
                wrapper.apply("EXISTS (SELECT 1 FROM gov_metadata WHERE id = gov_metadata_column.metadata_id AND ds_id = {0} AND table_name = {1})",
                        dto.getDsId(), tableName);
                if (Boolean.TRUE.equals(dto.getExcludeSystemTables())) {
                    wrapper.apply("EXISTS (SELECT 1 FROM gov_metadata WHERE id = gov_metadata_column.metadata_id AND ds_id = {0} AND table_name = {1} AND table_name NOT LIKE 'sys_%%')",
                            dto.getDsId(), tableName);
                }
                columns.addAll(columnMapper.selectList(wrapper));
            }
        } else if ("LAYER".equals(dto.getScanScope()) && StringUtils.hasText(dto.getLayerCode())) {
            LambdaQueryWrapper<GovMetadataColumn> wrapper = new LambdaQueryWrapper<>();
            wrapper.apply("EXISTS (SELECT 1 FROM gov_metadata WHERE id = gov_metadata_column.metadata_id AND ds_id = {0} AND data_layer = {1})",
                    dto.getDsId(), dto.getLayerCode());
            if (Boolean.TRUE.equals(dto.getExcludeSystemTables())) {
                wrapper.apply("EXISTS (SELECT 1 FROM gov_metadata WHERE id = gov_metadata_column.metadata_id AND ds_id = {0} AND data_layer = {1} AND table_name NOT LIKE 'sys_%%')",
                        dto.getDsId(), dto.getLayerCode());
            }
            columns.addAll(columnMapper.selectList(wrapper));
        } else {
            LambdaQueryWrapper<GovMetadataColumn> wrapper = new LambdaQueryWrapper<>();
            wrapper.apply("EXISTS (SELECT 1 FROM gov_metadata WHERE id = gov_metadata_column.metadata_id AND ds_id = {0})",
                    dto.getDsId());
            if (Boolean.TRUE.equals(dto.getExcludeSystemTables())) {
                wrapper.apply("EXISTS (SELECT 1 FROM gov_metadata WHERE id = gov_metadata_column.metadata_id AND ds_id = {0} AND table_name NOT LIKE 'sys_%%')",
                        dto.getDsId());
            }
            columns.addAll(columnMapper.selectList(wrapper));
        }
        return columns;
    }

    /**
     * 模式0：单字段扫描（用于新字段一键扫描）
     * 根据指定的 (dsId, tableName, columnNames) 查询 gov_metadata_column 中对应记录
     */
    private List<GovMetadataColumn> buildColumnListForSingleColumns(SecSensitivityScanDTO dto) {
        List<GovMetadataColumn> columns = new ArrayList<>();
        if (dto.getColumnNames() == null || dto.getColumnNames().isEmpty()) {
            return columns;
        }
        // 收集有效列名
        List<String> validNames = dto.getColumnNames().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
        if (validNames.isEmpty()) {
            return columns;
        }
        // 一次查询，用 IN 替代逐条 SELECT
        LambdaQueryWrapper<GovMetadataColumn> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("EXISTS (SELECT 1 FROM gov_metadata WHERE id = gov_metadata_column.metadata_id AND ds_id = {0} AND table_name = {1})",
                dto.getDsId(), dto.getTableNames());
        wrapper.in(GovMetadataColumn::getColumnName, validNames);
        columns.addAll(columnMapper.selectList(wrapper));
        return columns;
    }

    /**
     * 直连数据源扫描（无需先采集元数据）
     * 使用 DataSourceAdapter.getColumnsFromInformationSchema 获取表结构
     */
    private List<GovMetadataColumn> buildColumnListByDirectScan(SecSensitivityScanDTO dto) {
        if (adapterRegistry == null || dynamicDataSourceManager == null) {
            log.warn("直连扫描模式不可用：adapterRegistry 或 dynamicDataSourceManager 未初始化");
            return new ArrayList<>();
        }

        List<GovMetadataColumn> columns = new ArrayList<>();
        com.bagdatahouse.datasource.adapter.DataSourceAdapter adapter =
                adapterRegistry.getAdapterById(dto.getDsId());
        if (adapter == null) {
            throw new BusinessException(2001, "数据源连接失败，请检查数据源配置");
        }

        // PostgreSQL 等多 schema：优先使用请求中的 schema，否则用数据源连接配置的默认 schema
        String schema = StringUtils.hasText(dto.getSchema())
                ? dto.getSchema().trim()
                : dynamicDataSourceManager.getSchemaName(dto.getDsId());

        // 确定要扫描的表名列表
        List<String> tableNamesToScan = new ArrayList<>();

        if ("SPECIFIC".equals(dto.getScanScope()) && StringUtils.hasText(dto.getTableNames())) {
            for (String t : dto.getTableNames().split("[,\\s]+")) {
                String trimmed = t.trim();
                if (StringUtils.hasText(trimmed) && !isSystemTable(trimmed, dto.getExcludeSystemTables())) {
                    tableNamesToScan.add(trimmed);
                }
            }
        } else if ("LAYER".equals(dto.getScanScope()) && StringUtils.hasText(dto.getLayerCode())) {
            // LAYER 模式下需要从元数据表获取属于该层的表
            LambdaQueryWrapper<GovMetadata> metaWrapper = new LambdaQueryWrapper<>();
            metaWrapper.eq(GovMetadata::getDsId, dto.getDsId())
                       .eq(GovMetadata::getDataLayer, dto.getLayerCode());
            if (Boolean.TRUE.equals(dto.getExcludeSystemTables())) {
                metaWrapper.apply("table_name NOT LIKE 'sys_%%'");
            }
            List<GovMetadata> metadataList = metadataMapper.selectList(metaWrapper);
            for (GovMetadata meta : metadataList) {
                if (StringUtils.hasText(meta.getTableName())) {
                    tableNamesToScan.add(meta.getTableName());
                }
            }
        } else {
            // ALL 模式：获取数据源下所有表
            try {
                List<String> allTables = schema != null
                        ? adapter.getTables(schema)
                        : adapter.getTables();
                for (String t : allTables) {
                    if (!isSystemTable(t, dto.getExcludeSystemTables())) {
                        tableNamesToScan.add(t);
                    }
                }
            } catch (Exception e) {
                log.warn("直连扫描获取表列表失败: {}", e.getMessage());
            }
        }

        // 对每个表获取列信息
        for (String tableName : tableNamesToScan) {
            try {
                List<com.bagdatahouse.datasource.adapter.DataSourceAdapter.ColumnInfo> colInfos =
                        adapter.getColumnsFromInformationSchema(schema, tableName);

                for (int i = 0; i < colInfos.size(); i++) {
                    com.bagdatahouse.datasource.adapter.DataSourceAdapter.ColumnInfo colInfo = colInfos.get(i);
                    GovMetadataColumn col = GovMetadataColumn.builder()
                            .tableName(tableName.trim())
                            .schemaName(schema)
                            .columnName(colInfo.columnName())
                            .columnComment(colInfo.columnComment())
                            .dataType(colInfo.dataType())
                            .isNullable(colInfo.nullable())
                            .defaultValue(colInfo.defaultValue())
                            .isPrimaryKey(colInfo.primaryKey())
                            .sortOrder(i)
                            .build();
                    columns.add(col);
                }
            } catch (Exception e) {
                log.debug("直连扫描获取列信息失败: table={}, error={}", tableName, e.getMessage());
            }
        }

        return columns;
    }

    private boolean isSystemTable(String tableName, Boolean excludeSystemTables) {
        return Boolean.TRUE.equals(excludeSystemTables) && tableName.toLowerCase().startsWith("sys_");
    }

    private SecColumnSensitivity findExistingSensitivity(Long dsId, String tableName, String columnName) {
        LambdaQueryWrapper<SecColumnSensitivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecColumnSensitivity::getDsId, dsId)
               .eq(SecColumnSensitivity::getTableName, tableName)
               .eq(SecColumnSensitivity::getColumnName, columnName)
               .last("LIMIT 1");
        List<SecColumnSensitivity> list = sensitivityMapper.selectList(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 扫描前一次性加载本数据源、涉及表下的已有敏感字段，避免对每一列再 SELECT。
     */
    private Map<String, SecColumnSensitivity> loadExistingSensitivityMap(Long dsId, Set<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SecColumnSensitivity> w = new LambdaQueryWrapper<>();
        w.eq(SecColumnSensitivity::getDsId, dsId);
        w.in(SecColumnSensitivity::getTableName, tableNames);
        List<SecColumnSensitivity> list = sensitivityMapper.selectList(w);
        return list.stream().collect(Collectors.toMap(
                e -> e.getTableName() + "\0" + e.getColumnName(),
                e -> e,
                (a, b) -> a));
    }

    /**
     * 未开启内容采样时，按规则优先级给出差异化置信度（约 65~98），避免全部为 90。
     */
    private BigDecimal defaultConfidenceFromRule(SecSensitivityRule rule) {
        int p = rule.getPriority() != null ? rule.getPriority() : 50;
        double conf = 60.0 + (p - 30) * 0.55;
        conf = Math.max(65.0, Math.min(98.0, conf));
        return BigDecimal.valueOf(Math.round(conf * 10) / 10.0);
    }

    private static long toAggregateLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static long toAggregateLong(Map<String, Object> raw, String key) {
        if (raw == null) {
            return 0L;
        }
        return toAggregateLong(raw.get(key));
    }

    // ==================== VO Build Helpers ====================

    /**
     * 分类在列表中展示的「关联等级」：取关联规则与「分类-等级推荐绑定」中等级值最高者（最严）。
     */
    private record LevelDisplay(String code, String label) {}

    private Map<Long, List<SecClassLevelBinding>> loadRecommendedBindingsByClassId() {
        LambdaQueryWrapper<SecClassLevelBinding> w = new LambdaQueryWrapper<>();
        w.eq(SecClassLevelBinding::getIsRecommended, 1);
        List<SecClassLevelBinding> all = classLevelBindingMapper.selectList(w);
        return all.stream().collect(Collectors.groupingBy(SecClassLevelBinding::getClassId));
    }

    private LevelDisplay resolveStrictestClassificationLevel(List<SecSensitivityRule> classRules,
                                                             List<SecClassLevelBinding> classBindings,
                                                             Map<Long, SecLevel> levelMap) {
        Integer bestVal = null;
        String code = null;
        String label = null;
        for (SecSensitivityRule r : classRules) {
            if (r.getLevelId() == null) {
                continue;
            }
            SecLevel lv = levelMap.get(r.getLevelId());
            if (lv != null && lv.getLevelValue() != null) {
                if (bestVal == null || lv.getLevelValue() > bestVal) {
                    bestVal = lv.getLevelValue();
                    code = lv.getLevelCode();
                    label = lv.getLevelName();
                }
            }
        }
        for (SecClassLevelBinding b : classBindings) {
            if (b.getLevelId() == null) {
                continue;
            }
            SecLevel lv = levelMap.get(b.getLevelId());
            if (lv != null && lv.getLevelValue() != null) {
                if (bestVal == null || lv.getLevelValue() > bestVal) {
                    bestVal = lv.getLevelValue();
                    code = lv.getLevelCode();
                    label = lv.getLevelName();
                }
            }
        }
        return new LevelDisplay(code, label);
    }

    /**
     * 同步分类表单中的「关联等级」到 sec_class_level_binding（推荐绑定），供列表展示与推荐逻辑使用。
     */
    private void syncClassificationLevelBinding(Long classId, String sensitivityLevelCode) {
        LambdaQueryWrapper<SecClassLevelBinding> del = new LambdaQueryWrapper<>();
        del.eq(SecClassLevelBinding::getClassId, classId);
        classLevelBindingMapper.delete(del);
        if (!StringUtils.hasText(sensitivityLevelCode)) {
            return;
        }
        SecLevel level = getLevelByCode(sensitivityLevelCode.trim());
        if (level == null) {
            log.warn("分类关联等级未找到对应分级: classId={}, levelCode={}", classId, sensitivityLevelCode);
            return;
        }
        SecClassLevelBinding binding = SecClassLevelBinding.builder()
                .classId(classId)
                .levelId(level.getId())
                .isRecommended(1)
                .bindingDesc("分类默认关联等级")
                .createUser(getCurrentUserId())
                .createTime(LocalDateTime.now())
                .build();
        classLevelBindingMapper.insert(binding);
    }

    private SecClassificationVO buildClassificationVO(SecClassification c,
                                                        Map<Long, Long> ruleCountMap,
                                                        Map<Long, Long> fieldCountMap,
                                                        List<SecSensitivityRule> classRules,
                                                        List<SecClassLevelBinding> classBindings,
                                                        Map<Long, SecLevel> levelMap) {
        LevelDisplay strictest = resolveStrictestClassificationLevel(classRules, classBindings, levelMap);
        String sensitivityLevel = strictest.code();
        String sensitivityLevelLabel = strictest.label();
        return SecClassificationVO.builder()
                .id(c.getId())
                .classCode(c.getClassCode())
                .className(c.getClassName())
                .classDesc(c.getClassDesc())
                .classOrder(c.getClassOrder())
                .status(c.getStatus())
                .statusLabel(c.getStatus() != null && c.getStatus() == 1 ? "启用" : "禁用")
                .sensitivityLevel(sensitivityLevel)
                .sensitivityLevelLabel(sensitivityLevelLabel)
                .ruleCount(ruleCountMap.getOrDefault(c.getId(), 0L).intValue())
                .sensitiveFieldCount(fieldCountMap.getOrDefault(c.getId(), 0L).intValue())
                .createUser(c.getCreateUser())
                .createTime(c.getCreateTime())
                .updateUser(c.getUpdateUser())
                .updateTime(c.getUpdateTime())
                .build();
    }

    private SecLevelVO buildLevelVO(SecLevel l, Map<Long, Long> fieldCountMap) {
        return SecLevelVO.builder()
                .id(l.getId())
                .levelCode(l.getLevelCode())
                .levelName(l.getLevelName())
                .levelDesc(l.getLevelDesc())
                .levelValue(l.getLevelValue())
                .color(l.getColor())
                .icon(l.getIcon())
                .status(l.getStatus())
                .statusLabel(l.getStatus() != null && l.getStatus() == 1 ? "启用" : "禁用")
                .sensitiveFieldCount(fieldCountMap.getOrDefault(l.getId(), 0L).intValue())
                .createUser(l.getCreateUser())
                .createTime(l.getCreateTime())
                .updateUser(l.getUpdateUser())
                .updateTime(l.getUpdateTime())
                .build();
    }

    private SecSensitivityRuleVO buildRuleVO(SecSensitivityRule r,
                                             Map<Long, SecClassification> classMap,
                                             Map<Long, SecLevel> levelMap) {
        SecClassification classification = classMap.get(r.getClassId());
        SecLevel level = levelMap.get(r.getLevelId());

        SecSensitivityRuleVO.SecSensitivityRuleVOBuilder builder = SecSensitivityRuleVO.builder()
                .id(r.getId())
                .ruleName(r.getRuleName())
                .ruleCode(r.getRuleCode())
                .classId(r.getClassId())
                .levelId(r.getLevelId())
                .matchType(r.getMatchType())
                .matchTypeLabel(MatchTypeEnum.getLabelByCode(r.getMatchType()))
                .matchExpr(r.getMatchExpr())
                .matchExprType(r.getMatchExprType())
                .suggestionAction(r.getSuggestionAction())
                .suggestionActionLabel(MaskTypeEnum.getLabelByCode(r.getSuggestionAction()))
                .suggestionMaskPattern(r.getSuggestionMaskPattern())
                .suggestionMaskType(r.getSuggestionMaskType())
                .priority(r.getPriority())
                .builtin(r.getBuiltin())
                .builtinLabel(r.getBuiltin() != null && r.getBuiltin() == 1 ? "内置" : "自定义")
                .status(r.getStatus())
                .statusLabel(r.getStatus() != null && r.getStatus() == 1 ? "启用" : "禁用")
                .createUser(r.getCreateUser())
                .createTime(r.getCreateTime())
                .updateUser(r.getUpdateUser())
                .updateTime(r.getUpdateTime());

        if (classification != null) {
            builder.className(classification.getClassName());
        }
        if (level != null) {
            builder.levelName(level.getLevelName());
            builder.levelColor(level.getColor());
        }

        return builder.build();
    }

    private SecColumnSensitivityVO buildSensitivityVO(SecColumnSensitivity s,
                                                      Map<Long, DqDatasource> dsMap,
                                                      Map<Long, SecClassification> classMap,
                                                      Map<Long, SecLevel> levelMap,
                                                      Map<Long, SecSensitivityRule> ruleMap,
                                                      Map<Long, SysUser> userMap) {
        DqDatasource ds = dsMap.get(s.getDsId());
        SecClassification classification = classMap.get(s.getClassId());
        SecLevel level = levelMap.get(s.getLevelId());
        SecSensitivityRule rule = ruleMap.get(s.getMatchRuleId());

        SecColumnSensitivityVO.SecColumnSensitivityVOBuilder builder = SecColumnSensitivityVO.builder()
                .id(s.getId())
                .dsId(s.getDsId())
                .dsName(ds != null ? ds.getDsName() : null)
                .tableName(s.getTableName())
                .columnName(s.getColumnName())
                .columnComment(s.getColumnComment())
                .dataType(s.getDataType())
                .classId(s.getClassId())
                .levelId(s.getLevelId())
                .maskType(s.getMaskType())
                .maskTypeLabel(MaskTypeEnum.getLabelByCode(s.getMaskType()))
                .maskPattern(s.getMaskPattern())
                .maskPosition(s.getMaskPosition())
                .maskChar(s.getMaskChar())
                .confidence(s.getConfidence())
                .scanBatchNo(s.getScanBatchNo())
                .scanTime(s.getScanTime() != null ? s.getScanTime() : s.getCreateTime())
                .reviewStatus(s.getReviewStatus())
                .reviewStatusLabel(ReviewStatusEnum.getLabelByCode(s.getReviewStatus()))
                .reviewComment(s.getReviewComment())
                .approvedBy(s.getApprovedBy())
                .approvedTime(s.getApprovedTime())
                .createUser(s.getCreateUser())
                .createTime(s.getCreateTime())
                .updateUser(s.getUpdateUser())
                .updateTime(s.getUpdateTime());

        if (classification != null) {
            builder.className(classification.getClassName());
        }
        if (level != null) {
            builder.levelName(level.getLevelName());
            builder.levelColor(level.getColor());
        }
        if (rule != null) {
            builder.matchRuleName(rule.getRuleName());
        }
        if (s.getConfidence() != null) {
            double conf = s.getConfidence().doubleValue();
            if (conf >= 90) {
                builder.confidenceLabel("高");
            } else if (conf >= 70) {
                builder.confidenceLabel("中");
            } else {
                builder.confidenceLabel("低");
            }
        }
        if (s.getApprovedBy() != null && userMap != null && userMap.containsKey(s.getApprovedBy())) {
            SysUser approver = userMap.get(s.getApprovedBy());
            builder.approvedByName(approver.getNickname() != null ? approver.getNickname() : approver.getUsername());
        }
        if (s.getCreateUser() != null && userMap != null && userMap.containsKey(s.getCreateUser())) {
            SysUser creator = userMap.get(s.getCreateUser());
            builder.createUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }

        return builder.build();
    }

    private SecMaskTemplateVO buildMaskTemplateVO(SecMaskTemplate m) {
        return SecMaskTemplateVO.builder()
                .id(m.getId())
                .templateName(m.getTemplateName())
                .templateCode(m.getTemplateCode())
                .templateDesc(m.getTemplateDesc())
                .dataType(m.getDataType())
                .dataTypeLabel(getDataTypeLabel(m.getDataType()))
                .maskType(m.getMaskType())
                .maskTypeLabel(MaskTypeEnum.getLabelByCode(m.getMaskType()))
                .maskPosition(m.getMaskPosition())
                .maskChar(m.getMaskChar())
                .maskHeadKeep(m.getMaskHeadKeep())
                .maskTailKeep(m.getMaskTailKeep())
                .maskPattern(m.getMaskPattern())
                .builtin(m.getBuiltin())
                .builtinLabel(m.getBuiltin() != null && m.getBuiltin() == 1 ? "内置" : "自定义")
                .status(m.getStatus())
                .statusLabel(m.getStatus() != null && m.getStatus() == 1 ? "启用" : "禁用")
                .createUser(m.getCreateUser())
                .createTime(m.getCreateTime())
                .updateUser(m.getUpdateUser())
                .updateTime(m.getUpdateTime())
                .build();
    }

    private String getDataTypeLabel(String dataType) {
        if (dataType == null) return null;
        return switch (dataType) {
            case "STRING" -> "字符串";
            case "NUMBER" -> "数值";
            case "DATE" -> "日期";
            case "ALL" -> "全部";
            default -> dataType;
        };
    }

    private String getConfidenceLabel(BigDecimal confidence) {
        if (confidence == null) return "低";
        double conf = confidence.doubleValue();
        if (conf >= 80) return "高";
        if (conf >= 50) return "中";
        return "低";
    }

    // ==================== Map Cache Helpers ====================

    @SuppressWarnings("unchecked")
    private Map<Long, SecClassification> getClassificationMap() {
        return (Map<Long, SecClassification>) getAllMaps().get("classification");
    }

    @SuppressWarnings("unchecked")
    private Map<Long, SecLevel> getLevelMap() {
        return (Map<Long, SecLevel>) getAllMaps().get("level");
    }

    @SuppressWarnings("unchecked")
    private Map<Long, SecSensitivityRule> getRuleMap() {
        return (Map<Long, SecSensitivityRule>) getAllMaps().get("rule");
    }

    @SuppressWarnings("unchecked")
    private Map<Long, DqDatasource> getDatasourceMap() {
        return (Map<Long, DqDatasource>) getAllMaps().get("datasource");
    }

    private Map<Long, Long> getRuleCountByClassMap() {
        return getRuleMap().values().stream()
                .filter(r -> r.getClassId() != null)
                .collect(Collectors.groupingBy(
                        SecSensitivityRule::getClassId,
                        Collectors.counting()
                ));
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> getSensitiveFieldCountByClassMap() {
        List<SecColumnSensitivity> all = getAllMaps().get("sensitivityAll") != null
                ? (List<SecColumnSensitivity>) getAllMaps().get("sensitivityAll")
                : sensitivityMapper.selectList(null);
        return all.stream()
                .filter(f -> f.getClassId() != null)
                .collect(Collectors.groupingBy(
                        SecColumnSensitivity::getClassId,
                        Collectors.counting()
                ));
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> getSensitiveFieldCountByLevelMap() {
        List<SecColumnSensitivity> all = getAllMaps().get("sensitivityAll") != null
                ? (List<SecColumnSensitivity>) getAllMaps().get("sensitivityAll")
                : sensitivityMapper.selectList(null);
        // 按「数据源 + 表 + 列」去重，避免同一字段多次扫描产生多行导致字段数虚高
        return all.stream()
                .filter(f -> f.getLevelId() != null)
                .collect(Collectors.groupingBy(
                        SecColumnSensitivity::getLevelId,
                        Collectors.mapping(
                                SecurityServiceImpl::sensitivityFieldIdentityKey,
                                Collectors.toSet()
                        )
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (long) e.getValue().size()));
    }

    /**
     * 敏感字段在业务上的唯一键（用于统计「多少个字段」而非多少条扫描记录）。
     */
    private static String sensitivityFieldIdentityKey(SecColumnSensitivity f) {
        long ds = f.getDsId() != null ? f.getDsId() : 0L;
        String table = f.getTableName() != null ? f.getTableName() : "";
        String col = f.getColumnName() != null ? f.getColumnName() : "";
        return ds + "\0" + table + "\0" + col;
    }

    private Map<Long, SysUser> getUserMap(List<SecColumnSensitivity> records) {
        Set<Long> userIds = new HashSet<>();
        for (SecColumnSensitivity r : records) {
            if (r.getApprovedBy() != null) userIds.add(r.getApprovedBy());
            if (r.getCreateUser() != null) userIds.add(r.getCreateUser());
        }
        if (userIds.isEmpty()) return new HashMap<>();
        List<SysUser> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
    }

    // ==================== Stats Helpers ====================

    @SuppressWarnings("unchecked")
    private Map<String, Long> buildClassificationCountMap() {
        List<SecColumnSensitivity> all = getAllMaps().get("sensitivityAll") != null
                ? (List<SecColumnSensitivity>) getAllMaps().get("sensitivityAll")
                : sensitivityMapper.selectList(null);
        Map<Long, SecClassification> classMap = getClassificationMap();
        Map<String, Long> result = new LinkedHashMap<>();
        Map<Long, Long> group = all.stream()
                .filter(s -> s.getClassId() != null)
                .collect(Collectors.groupingBy(
                        SecColumnSensitivity::getClassId,
                        Collectors.counting()
                ));
        for (Map.Entry<Long, Long> entry : group.entrySet()) {
            SecClassification c = classMap.get(entry.getKey());
            String name = c != null ? c.getClassName() : "未知";
            result.put(name, entry.getValue());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> buildLevelCountMap() {
        List<SecColumnSensitivity> all = getAllMaps().get("sensitivityAll") != null
                ? (List<SecColumnSensitivity>) getAllMaps().get("sensitivityAll")
                : sensitivityMapper.selectList(null);
        Map<Long, SecLevel> levelMap = getLevelMap();
        Map<String, Long> result = new LinkedHashMap<>();
        Map<Long, Long> group = all.stream()
                .filter(s -> s.getLevelId() != null)
                .collect(Collectors.groupingBy(
                        SecColumnSensitivity::getLevelId,
                        Collectors.counting()
                ));
        for (Map.Entry<Long, Long> entry : group.entrySet()) {
            SecLevel l = levelMap.get(entry.getKey());
            String name = l != null ? l.getLevelName() : "未知";
            result.put(name, entry.getValue());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> buildMaskTypeCountMap() {
        List<SecColumnSensitivity> all = getAllMaps().get("sensitivityAll") != null
                ? (List<SecColumnSensitivity>) getAllMaps().get("sensitivityAll")
                : sensitivityMapper.selectList(null);
        Map<String, Long> result = new LinkedHashMap<>();
        Map<String, Long> group = all.stream()
                .filter(s -> StringUtils.hasText(s.getMaskType()))
                .collect(Collectors.groupingBy(
                        s -> MaskTypeEnum.getLabelByCode(s.getMaskType()),
                        Collectors.counting()
                ));
        result.putAll(group);
        return result;
    }

    private Map<String, Long> buildMatchTypeCountMap() {
        Map<String, Long> result = new LinkedHashMap<>();
        Map<String, Long> group = getRuleMap().values().stream()
                .filter(r -> StringUtils.hasText(r.getMatchType()))
                .collect(Collectors.groupingBy(
                        r -> MatchTypeEnum.getLabelByCode(r.getMatchType()),
                        Collectors.counting()
                ));
        result.putAll(group);
        return result;
    }

    // ==================== 敏感字段访问审批实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecAccessApplication> submitAccessApplication(SecAccessApplicationDTO dto) {
        // 生成申请编号
        String applicationNo = "SAA" + System.currentTimeMillis();

        // 计算访问截止时间
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime;
        if ("HOUR".equals(dto.getDurationType())) {
            endTime = startTime.plusHours(dto.getDurationHours() != null ? dto.getDurationHours() : 1);
        } else if ("DAY".equals(dto.getDurationType())) {
            endTime = startTime.plusDays(dto.getDurationHours() != null ? dto.getDurationHours() : 1);
        } else if ("WEEK".equals(dto.getDurationType())) {
            endTime = startTime.plusWeeks(dto.getDurationHours() != null ? dto.getDurationHours() : 1);
        } else {
            endTime = startTime.plusDays(7);
        }

        // 获取数据源信息
        DqDatasource ds = datasourceMapper.selectById(dto.getTargetDsId());
        if (ds == null) {
            throw new BusinessException(2001, "数据源不存在");
        }

        SecAccessApplication entity = SecAccessApplication.builder()
                .applicationNo(applicationNo)
                .applicantId(dto.getApplicantId())
                .applicantName(dto.getApplicantName())
                .deptId(dto.getDeptId())
                .deptName(dto.getDeptName())
                .targetDsId(dto.getTargetDsId())
                .targetDsName(ds.getDsName())
                .targetTable(dto.getTargetTable())
                .targetColumns(dto.getTargetColumns())
                .targetColumnNames(dto.getTargetColumnNames())
                .durationType(dto.getDurationType())
                .durationHours(dto.getDurationHours())
                .startTime(startTime)
                .endTime(endTime)
                .applyReason(dto.getApplyReason())
                .status("PENDING")
                .createTime(LocalDateTime.now())
                .createUser(dto.getCreateUser())
                .build();

        accessApplicationMapper.insert(entity);

        // 记录审计日志
        logAccess(entity, dto.getApplicantId(), dto.getApplicantName(), dto.getDeptName(),
                "APPLY", "提交访问申请", null);

        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelAccessApplication(Long id, Long operatorId, String operatorName) {
        SecAccessApplication entity = accessApplicationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "申请记录不存在");
        }

        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(2002, "只有待审批状态的申请可以取消");
        }

        entity.setStatus("CANCELLED");
        entity.setUpdateTime(LocalDateTime.now());
        accessApplicationMapper.updateById(entity);

        // 获取部门信息
        String deptName = null;
        if (entity.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(entity.getDeptId());
            if (dept != null) {
                deptName = dept.getDeptName();
            }
        }

        // 记录审计日志
        logAccess(entity, operatorId, operatorName, deptName,
                "CANCEL", "取消访问申请", "CANCELLED");

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> approveAccessApplication(SecAccessApprovalDTO dto) {
        SecAccessApplication entity = accessApplicationMapper.selectById(dto.getApplicationId());
        if (entity == null) {
            throw new BusinessException(2001, "申请记录不存在");
        }

        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(2002, "只有待审批状态的申请可以审批");
        }

        // 更新申请状态
        String newStatus = "APPROVE".equals(dto.getAction()) ? "APPROVED" : "REJECTED";
        entity.setStatus(newStatus);
        entity.setApproverId(dto.getApproverId());
        entity.setApproverName(dto.getApproverName());
        entity.setApprovalComment(dto.getComment());
        entity.setApprovalTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        accessApplicationMapper.updateById(entity);

        // 记录审批记录
        SecAccessAudit audit = SecAccessAudit.builder()
                .applicationId(entity.getId())
                .applicationNo(entity.getApplicationNo())
                .approverId(dto.getApproverId())
                .approverName(dto.getApproverName())
                .approverDept(dto.getApproverDept())
                .action(dto.getAction())
                .comment(dto.getComment())
                .result("APPROVE".equals(dto.getAction()) ? "AGREE" : "REJECT")
                .auditNode("FIRST")
                .auditTime(LocalDateTime.now())
                .auditSource("MANUAL")
                .createTime(LocalDateTime.now())
                .build();
        accessAuditMapper.insert(audit);

        // 记录审计日志
        logAccess(entity, dto.getApproverId(), dto.getApproverName(), dto.getApproverDept(),
                dto.getAction(), dto.getComment(), newStatus);

        return Result.success();
    }

    @Override
    public Result<Page<SecAccessApplicationVO>> pageAccessApplication(Integer pageNum, Integer pageSize, SecAccessApplicationQueryDTO queryDTO) {
        Page<SecAccessApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecAccessApplication> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getId() != null) {
            wrapper.eq(SecAccessApplication::getId, queryDTO.getId());
        }
        if (StringUtils.hasText(queryDTO.getApplicationNo())) {
            wrapper.like(SecAccessApplication::getApplicationNo, queryDTO.getApplicationNo());
        }
        if (queryDTO.getApplicantId() != null) {
            wrapper.eq(SecAccessApplication::getApplicantId, queryDTO.getApplicantId());
        }
        if (StringUtils.hasText(queryDTO.getApplicantName())) {
            wrapper.like(SecAccessApplication::getApplicantName, queryDTO.getApplicantName());
        }
        if (queryDTO.getDeptId() != null) {
            wrapper.eq(SecAccessApplication::getDeptId, queryDTO.getDeptId());
        }
        if (queryDTO.getTargetDsId() != null) {
            wrapper.eq(SecAccessApplication::getTargetDsId, queryDTO.getTargetDsId());
        }
        if (StringUtils.hasText(queryDTO.getTargetTable())) {
            wrapper.like(SecAccessApplication::getTargetTable, queryDTO.getTargetTable());
        }
        if (StringUtils.hasText(queryDTO.getStatus())) {
            wrapper.eq(SecAccessApplication::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getApproverId() != null) {
            wrapper.eq(SecAccessApplication::getApproverId, queryDTO.getApproverId());
        }

        wrapper.orderByDesc(SecAccessApplication::getCreateTime);
        Page<SecAccessApplication> resultPage = accessApplicationMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<SecAccessApplicationVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<SecAccessApplicationVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    @Override
    public Result<SecAccessApplicationVO> getAccessApplicationById(Long id) {
        SecAccessApplication entity = accessApplicationMapper.selectById(id);
        if (entity == null) {
            return Result.success(null);
        }
        return Result.success(convertToVO(entity));
    }

    @Override
    public Result<Page<SecAccessApplicationVO>> pagePendingApproval(Integer pageNum, Integer pageSize, Long approverId) {
        Page<SecAccessApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecAccessApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecAccessApplication::getStatus, "PENDING");
        wrapper.orderByDesc(SecAccessApplication::getCreateTime);
        Page<SecAccessApplication> resultPage = accessApplicationMapper.selectPage(page, wrapper);

        Page<SecAccessApplicationVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<SecAccessApplicationVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    @Override
    public Result<List<SecAccessAuditVO>> listAccessAuditHistory(Long applicationId) {
        LambdaQueryWrapper<SecAccessAudit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecAccessAudit::getApplicationId, applicationId);
        wrapper.orderByAsc(SecAccessAudit::getAuditTime);
        List<SecAccessAudit> audits = accessAuditMapper.selectList(wrapper);

        List<SecAccessAuditVO> voList = audits.stream()
                .map(this::convertToAuditVO)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    @Override
    public Result<Page<SecAccessLogVO>> pageAccessLog(Integer pageNum, Integer pageSize, Long applicationId, String operationType, Long operatorId) {
        Page<SecAccessLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecAccessLog> wrapper = new LambdaQueryWrapper<>();

        if (applicationId != null) {
            wrapper.eq(SecAccessLog::getApplicationId, applicationId);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(SecAccessLog::getOperationType, operationType);
        }
        if (operatorId != null) {
            wrapper.eq(SecAccessLog::getOperatorId, operatorId);
        }

        wrapper.orderByDesc(SecAccessLog::getCreateTime);
        Page<SecAccessLog> resultPage = accessLogMapper.selectPage(page, wrapper);

        Page<SecAccessLogVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<SecAccessLogVO> voList = resultPage.getRecords().stream()
                .map(this::convertToLogVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    @Override
    public Result<Map<String, Object>> getAccessStats() {
        Map<String, Object> stats = new HashMap<>();

        // 统计申请数量
        Long totalCount = accessApplicationMapper.selectCount(null);
        Long pendingCount = accessApplicationMapper.selectCount(
                new LambdaQueryWrapper<SecAccessApplication>().eq(SecAccessApplication::getStatus, "PENDING"));
        Long approvedCount = accessApplicationMapper.selectCount(
                new LambdaQueryWrapper<SecAccessApplication>().eq(SecAccessApplication::getStatus, "APPROVED"));
        Long rejectedCount = accessApplicationMapper.selectCount(
                new LambdaQueryWrapper<SecAccessApplication>().eq(SecAccessApplication::getStatus, "REJECTED"));

        stats.put("totalCount", totalCount);
        stats.put("pendingCount", pendingCount);
        stats.put("approvedCount", approvedCount);
        stats.put("rejectedCount", rejectedCount);

        return Result.success(stats);
    }

    // ==================== 定时扫描任务管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecScanSchedule> saveScanSchedule(SecScanScheduleSaveDTO dto) {
        if (!StringUtils.hasText(dto.getTaskCode())) {
            throw new BusinessException(2001, "任务编码不能为空");
        }
        if (datasourceMapper.selectById(dto.getDsId()) == null) {
            throw new BusinessException(2001, "数据源不存在");
        }

        // 校验 taskCode 唯一
        LambdaQueryWrapper<SecScanSchedule> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(SecScanSchedule::getTaskCode, dto.getTaskCode());
        if (scanScheduleMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException(2001, "任务编码已存在");
        }

        SecScanSchedule entity = new SecScanSchedule();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "ENABLED");
        entity.setScanCycle(dto.getScanCycle() != null ? dto.getScanCycle() : "ONCE");
        entity.setEnableContentScan(dto.getEnableContentScan() != null ? dto.getEnableContentScan() : true);
        entity.setSampleSize(dto.getSampleSize() != null ? Math.min(dto.getSampleSize(), 200) : 50);
        entity.setCreateTime(LocalDateTime.now());
        scanScheduleMapper.insert(entity);

        log.info("定时扫描任务创建成功: id={}, taskCode={}", entity.getId(), entity.getTaskCode());
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateScanSchedule(Long id, SecScanScheduleSaveDTO dto) {
        SecScanSchedule existing = scanScheduleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "定时扫描任务不存在");
        }
        if (dto.getDsId() != null && datasourceMapper.selectById(dto.getDsId()) == null) {
            throw new BusinessException(2001, "数据源不存在");
        }

        SecScanSchedule update = new SecScanSchedule();
        BeanUtils.copyProperties(dto, update);
        update.setId(id);
        update.setUpdateTime(LocalDateTime.now());
        scanScheduleMapper.updateById(update);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteScanSchedule(Long id) {
        SecScanSchedule existing = scanScheduleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "定时扫描任务不存在");
        }
        scanScheduleMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<Page<SecScanSchedule>> pageScanSchedule(Integer pageNum, Integer pageSize, Long dsId, Integer enabled) {
        Page<SecScanSchedule> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<SecScanSchedule> wrapper = new LambdaQueryWrapper<>();
        if (dsId != null) {
            wrapper.eq(SecScanSchedule::getDsId, dsId);
        }
        if (enabled != null) {
            wrapper.eq(SecScanSchedule::getStatus, enabled == 1 ? "ENABLED" : "DISABLED");
        }
        wrapper.orderByDesc(SecScanSchedule::getCreateTime);
        Page<SecScanSchedule> result = scanScheduleMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<SecScanSchedule> getScanScheduleById(Long id) {
        SecScanSchedule schedule = scanScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException(2001, "定时扫描任务不存在");
        }
        return Result.success(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> triggerScanSchedule(Long id) {
        SecScanSchedule schedule = scanScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException(2001, "定时扫描任务不存在");
        }

        // 构建扫描参数
        SecSensitivityScanDTO scanDto = SecSensitivityScanDTO.builder()
                .dsId(schedule.getDsId())
                .scanScope(schedule.getScanScope())
                .tableNames(schedule.getTableNames())
                .layerCode(schedule.getLayerCode())
                .enableContentScan(schedule.getEnableContentScan())
                .sampleSize(schedule.getSampleSize())
                .scanCycle("ONCE")
                .createUser(schedule.getCreateUser())
                .build();

        // 更新状态为 RUNNING
        SecScanSchedule updateStatus = new SecScanSchedule();
        updateStatus.setId(id);
        updateStatus.setLastRunStatus("RUNNING");
        scanScheduleMapper.updateById(updateStatus);

        // 异步执行扫描（避免阻塞）
        final Long scheduleId = id;
        new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                scanSensitiveFields(scanDto);
                long duration = (System.currentTimeMillis() - start) / 1000;
                SecScanSchedule done = new SecScanSchedule();
                done.setId(scheduleId);
                done.setLastRunTime(LocalDateTime.now());
                done.setLastRunStatus("SUCCESS");
                done.setLastBatchNo(scanDto.getCreateUser() != null
                        ? "SCHEDULED_" + scheduleId + "_" + System.currentTimeMillis()
                        : null);
                done.setLastDurationSeconds((int) duration);
                scanScheduleMapper.updateById(done);
            } catch (Exception e) {
                long duration = (System.currentTimeMillis() - start) / 1000;
                SecScanSchedule done = new SecScanSchedule();
                done.setId(scheduleId);
                done.setLastRunTime(LocalDateTime.now());
                done.setLastRunStatus("FAILED");
                done.setLastDurationSeconds((int) duration);
                scanScheduleMapper.updateById(done);
            }
        }).start();

        return Result.success();
    }

    @Override
    public Result<List<SecColumnSensitivityVO>> getScanHistoryByBatchNo(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            throw new BusinessException(2001, "批次号不能为空");
        }
        LambdaQueryWrapper<SecColumnSensitivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecColumnSensitivity::getScanBatchNo, batchNo);
        List<SecColumnSensitivity> list = sensitivityMapper.selectList(wrapper);

        Map<Long, DqDatasource> dsMap = getDatasourceMap();
        Map<Long, SecClassification> classMap = getClassificationMap();
        Map<Long, SecLevel> levelMap = getLevelMap();
        Map<Long, SecSensitivityRule> ruleMap = getRuleMap();

        Map<Long, GovMetadata> metaCache = new HashMap<>();
        List<SecColumnSensitivityVO> vos = new ArrayList<>(list.size());
        for (SecColumnSensitivity s : list) {
            SecColumnSensitivityVO vo = buildSensitivityVO(s, dsMap, classMap, levelMap, ruleMap, null);
            String tn = resolveTableNameForSensitivity(s, metaCache);
            if (StringUtils.hasText(tn)) {
                vo.setTableName(tn);
            }
            vos.add(vo);
        }
        return Result.success(vos);
    }

    /**
     * 解析敏感记录对应表名：优先行内 table_name；否则按 metadata_id 尝试 gov_metadata.id；
     * 再否则尝试 gov_metadata_column.id 并回退到其父元数据表名。
     */
    private String resolveTableNameForSensitivity(SecColumnSensitivity r, Map<Long, GovMetadata> metaCache) {
        if (StringUtils.hasText(r.getTableName())) {
            return r.getTableName().trim();
        }
        if (r.getMetadataId() == null) {
            return null;
        }
        Long mid = r.getMetadataId();

        GovMetadata meta = metaCache.get(mid);
        if (meta == null) {
            meta = metadataMapper.selectById(mid);
            if (meta != null && meta.getId() != null) {
                metaCache.put(meta.getId(), meta);
            }
        }
        if (meta != null && StringUtils.hasText(meta.getTableName())) {
            return meta.getTableName();
        }

        GovMetadataColumn col = columnMapper.selectById(mid);
        if (col == null) {
            return null;
        }
        if (StringUtils.hasText(col.getTableName())) {
            return col.getTableName();
        }
        if (col.getMetadataId() != null) {
            GovMetadata parent = metaCache.get(col.getMetadataId());
            if (parent == null) {
                parent = metadataMapper.selectById(col.getMetadataId());
                if (parent != null && parent.getId() != null) {
                    metaCache.put(parent.getId(), parent);
                }
            }
            return parent != null ? parent.getTableName() : null;
        }
        return null;
    }

    // ==================== 私有方法 ====================

    private void logAccess(SecAccessApplication application, Long operatorId, String operatorName,
                          String operatorDept, String operationType, String content, String status) {
        SecAccessLog log = SecAccessLog.builder()
                .applicationId(application.getId())
                .applicationNo(application.getApplicationNo())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .operatorDept(operatorDept)
                .operationType(operationType)
                .operationContent(content)
                .targetDsId(application.getTargetDsId())
                .targetTable(application.getTargetTable())
                .status(status != null ? status : application.getStatus())
                .createTime(LocalDateTime.now())
                .build();
        accessLogMapper.insert(log);
    }

    private SecAccessApplicationVO convertToVO(SecAccessApplication entity) {
        SecAccessApplicationVO vo = SecAccessApplicationVO.builder()
                .id(entity.getId())
                .applicationNo(entity.getApplicationNo())
                .applicantId(entity.getApplicantId())
                .applicantName(entity.getApplicantName())
                .deptId(entity.getDeptId())
                .deptName(entity.getDeptName())
                .targetDsId(entity.getTargetDsId())
                .targetDsName(entity.getTargetDsName())
                .targetTable(entity.getTargetTable())
                .targetColumns(entity.getTargetColumns())
                .targetColumnNames(entity.getTargetColumnNames())
                .durationType(entity.getDurationType())
                .durationTypeLabel(getDurationTypeLabel(entity.getDurationType()))
                .durationHours(entity.getDurationHours())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .applyReason(entity.getApplyReason())
                .status(entity.getStatus())
                .statusLabel(getStatusLabel(entity.getStatus()))
                .approverId(entity.getApproverId())
                .approverName(entity.getApproverName())
                .approvalComment(entity.getApprovalComment())
                .approvalTime(entity.getApprovalTime())
                .createTime(entity.getCreateTime())
                .build();

        // 计算是否可审批
        vo.setCanApprove("PENDING".equals(entity.getStatus()));
        vo.setCanCancel("PENDING".equals(entity.getStatus()));

        // 检查是否已过期
        if (entity.getEndTime() != null && entity.getEndTime().isBefore(LocalDateTime.now())) {
            vo.setIsExpired(true);
            if ("APPROVED".equals(entity.getStatus())) {
                // 更新为过期状态
                entity.setStatus("EXPIRED");
                entity.setUpdateTime(LocalDateTime.now());
                accessApplicationMapper.updateById(entity);
                vo.setStatus("EXPIRED");
                vo.setStatusLabel("已过期");
            }
        } else {
            vo.setIsExpired(false);
        }

        return vo;
    }

    private SecAccessAuditVO convertToAuditVO(SecAccessAudit entity) {
        return SecAccessAuditVO.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .applicationNo(entity.getApplicationNo())
                .approverId(entity.getApproverId())
                .approverName(entity.getApproverName())
                .approverDept(entity.getApproverDept())
                .action(entity.getAction())
                .actionLabel(getActionLabel(entity.getAction()))
                .comment(entity.getComment())
                .result(entity.getResult())
                .resultLabel(getResultLabel(entity.getResult()))
                .auditNode(entity.getAuditNode())
                .auditNodeLabel(getAuditNodeLabel(entity.getAuditNode()))
                .auditTime(entity.getAuditTime())
                .auditSource(entity.getAuditSource())
                .createTime(entity.getCreateTime())
                .build();
    }

    private SecAccessLogVO convertToLogVO(SecAccessLog entity) {
        return SecAccessLogVO.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .applicationNo(entity.getApplicationNo())
                .operatorId(entity.getOperatorId())
                .operatorName(entity.getOperatorName())
                .operatorDept(entity.getOperatorDept())
                .operationType(entity.getOperationType())
                .operationTypeLabel(getOperationTypeLabel(entity.getOperationType()))
                .operationContent(entity.getOperationContent())
                .targetDsId(entity.getTargetDsId())
                .targetTable(entity.getTargetTable())
                .targetColumn(entity.getTargetColumn())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .status(entity.getStatus())
                .remark(entity.getRemark())
                .createTime(entity.getCreateTime())
                .build();
    }

    private String getDurationTypeLabel(String type) {
        if (type == null) return "";
        switch (type) {
            case "HOUR": return "小时";
            case "DAY": return "天";
            case "WEEK": return "周";
            case "CUSTOM": return "自定义";
            default: return type;
        }
    }

    private String getStatusLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case "PENDING": return "待审批";
            case "APPROVED": return "已批准";
            case "REJECTED": return "已拒绝";
            case "EXPIRED": return "已过期";
            case "CANCELLED": return "已取消";
            default: return status;
        }
    }

    private String getActionLabel(String action) {
        if (action == null) return "";
        switch (action) {
            case "APPROVE": return "批准";
            case "REJECT": return "拒绝";
            case "APPLY": return "申请";
            case "CANCEL": return "取消";
            default: return action;
        }
    }

    private String getResultLabel(String result) {
        if (result == null) return "";
        switch (result) {
            case "AGREE": return "同意";
            case "REJECT": return "拒绝";
            default: return result;
        }
    }

    private String getAuditNodeLabel(String node) {
        if (node == null) return "";
        switch (node) {
            case "FIRST": return "第一级";
            case "SECOND": return "第二级";
            case "FINAL": return "最终级";
            default: return node;
        }
    }

    private String getOperationTypeLabel(String type) {
        if (type == null) return "";
        switch (type) {
            case "APPLY": return "申请";
            case "APPROVE": return "审批";
            case "REJECT": return "拒绝";
            case "ACCESS": return "访问";
            case "CANCEL": return "取消";
            case "EXPIRE": return "过期";
            case "SCAN": return "扫描";
            case "MASK": return "脱敏";
            default: return type;
        }
    }

    // ==================== SENSITIVE 类型告警联动（T3-T4） ====================

    /**
     * SENSITIVE_FIELD_SPIKE：敏感字段突增告警
     * 扫描完成后触发，计算本次与上次扫描的敏感字段数量差
     */
    private void triggerSensityFieldSpikeAlert(Long dsId, int currentCount, String batchNo) {
        if (alertRecordService == null || alertRuleService == null) {
            log.debug("AlertRecordService 或 AlertRuleService 未注入，跳过 SENSITIVE_FIELD_SPIKE 告警");
            return;
        }
        try {
            List<com.bagdatahouse.core.entity.MonitorAlertRule> rules =
                    alertRuleService.getSensityAlertRulesByDsId(dsId).getData();
            if (rules == null || rules.isEmpty()) {
                return;
            }

            // 查询上次扫描的敏感字段数量
            int previousCount = getPreviousSensitiveFieldCount(dsId);

            for (com.bagdatahouse.core.entity.MonitorAlertRule rule : rules) {
                if (!"SENSITIVE_FIELD_SPIKE".equals(rule.getRuleType())) {
                    continue;
                }

                java.math.BigDecimal spikePct = rule.getSpikeThresholdPct() != null
                        ? rule.getSpikeThresholdPct()
                        : new java.math.BigDecimal("20.00");

                java.math.BigDecimal actualPct;
                if (previousCount > 0) {
                    actualPct = java.math.BigDecimal.valueOf(currentCount - previousCount)
                            .divide(java.math.BigDecimal.valueOf(previousCount), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(new java.math.BigDecimal("100"));
                } else if (currentCount > 0) {
                    actualPct = new java.math.BigDecimal("100");
                } else {
                    continue;
                }

                if (actualPct.compareTo(spikePct) >= 0) {
                    com.bagdatahouse.core.dto.SensityAlertDTO dto = com.bagdatahouse.core.dto.SensityAlertDTO.builder()
                            .alertType("SENSITIVE_FIELD_SPIKE")
                            .alertLevel(rule.getAlertLevel() != null ? rule.getAlertLevel() : "WARN")
                            .sensitivityDsId(dsId)
                            .scanBatchNo(batchNo)
                            .triggerValue(actualPct)
                            .thresholdValue(spikePct)
                            .alertTitle("敏感字段数突增告警")
                            .alertContent(String.format("敏感字段数较上次扫描增长 %.2f%%（当前%d条，上次%d条），超过阈值 %.2f%%",
                                    actualPct, currentCount, previousCount, spikePct))
                            .build();
                    alertRecordService.createSensityAlert(dto);
                    log.info("触发 SENSITIVE_FIELD_SPIKE 告警: dsId={}, 当前={}, 上次={}, 增长=%.2f%%",
                            dsId, currentCount, previousCount, actualPct);
                }
            }
        } catch (Exception e) {
            log.warn("触发 SENSITIVE_FIELD_SPIKE 告警失败: {}", e.getMessage());
        }
    }

    /**
     * SENSITIVE_LEVEL_CHANGE：敏感等级变更告警（降级时触发）
     * 审核通过后触发，当字段等级被降级时通知管理员
     */
    private void triggerSensityLevelChangeAlert(SecColumnSensitivity sensitivity) {
        if (alertRecordService == null || alertRuleService == null) {
            log.debug("AlertRecordService 或 AlertRuleService 未注入，跳过 SENSITIVE_LEVEL_CHANGE 告警");
            return;
        }
        try {
            if (sensitivity.getLevelId() == null) {
                return;
            }

            Map<Long, SecLevel> levelMap = getLevelMap();

            // 查询该数据源关联的 SENSITIVE_LEVEL_CHANGE 规则
            List<com.bagdatahouse.core.entity.MonitorAlertRule> rules =
                    alertRuleService.getSensityAlertRulesByDsId(sensitivity.getDsId()).getData();
            if (rules == null || rules.isEmpty()) {
                return;
            }

            // 获取当前审核的等级值（使用 ThreadLocal 维表缓存）
            SecLevel newLevel = getLevelMap().get(sensitivity.getLevelId());
            int newLevelValue = newLevel != null && newLevel.getLevelValue() != null
                    ? newLevel.getLevelValue() : 0;

            for (com.bagdatahouse.core.entity.MonitorAlertRule rule : rules) {
                if (!"SENSITIVE_LEVEL_CHANGE".equals(rule.getRuleType())) {
                    continue;
                }

                // 获取规则配置的等级阈值
                String ruleLevelCode = rule.getSensitivityLevel();
                SecLevel ruleLevel = ruleLevelCode != null ? getLevelByCode(ruleLevelCode) : null;
                int ruleLevelValue = ruleLevel != null && ruleLevel.getLevelValue() != null
                        ? ruleLevel.getLevelValue() : 0;

                // 仅在等级低于规则阈值时触发降级告警
                if (newLevelValue < ruleLevelValue) {
                    String newLevelName = newLevel != null ? newLevel.getLevelName() : "未知";
                    String ruleLevelName = ruleLevel != null ? ruleLevel.getLevelName() : "未知";

                    com.bagdatahouse.core.dto.SensityAlertDTO dto = com.bagdatahouse.core.dto.SensityAlertDTO.builder()
                            .alertType("SENSITIVE_LEVEL_CHANGE")
                            .alertLevel("ERROR")
                            .sensitivityDsId(sensitivity.getDsId())
                            .sensitivityLevel(sensitivity.getLevelId() != null
                                    ? getLevelCodeById(sensitivity.getLevelId()) : null)
                            .sensitivityTable(sensitivity.getTableName())
                            .sensitivityColumn(sensitivity.getColumnName())
                            .alertTitle("敏感等级降级告警")
                            .alertContent(String.format("字段 %s.%s 敏感等级被降级为 %s（低于阈值 %s），请关注",
                                    sensitivity.getTableName(), sensitivity.getColumnName(),
                                    newLevelName, ruleLevelName))
                            .build();
                    alertRecordService.createSensityAlert(dto);
                    log.info("触发 SENSITIVE_LEVEL_CHANGE 告警: table={}.{}, level={}",
                            sensitivity.getTableName(), sensitivity.getColumnName(), newLevelName);
                }
            }
        } catch (Exception e) {
            log.warn("触发 SENSITIVE_LEVEL_CHANGE 告警失败: {}", e.getMessage());
        }
    }

    /**
     * 查询上次扫描的敏感字段数量（SENSITIVE_FIELD_SPIKE 告警用）
     */
    private int getPreviousSensitiveFieldCount(Long dsId) {
        try {
            LambdaQueryWrapper<SecColumnSensitivity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SecColumnSensitivity::getDsId, dsId)
                   .orderByDesc(SecColumnSensitivity::getScanTime)
                   .last("LIMIT 1");
            SecColumnSensitivity last = sensitivityMapper.selectOne(wrapper);
            if (last != null && StringUtils.hasText(last.getScanBatchNo())) {
                return sensitivityMapper.selectCount(
                        new LambdaQueryWrapper<SecColumnSensitivity>()
                                .eq(SecColumnSensitivity::getDsId, dsId)
                                .eq(SecColumnSensitivity::getScanBatchNo, last.getScanBatchNo())
                ).intValue();
            }
        } catch (Exception e) {
            log.debug("查询上次敏感字段数量失败: {}", e.getMessage());
        }
        return 0;
    }

    private SecLevel getLevelByCode(String levelCode) {
        if (!StringUtils.hasText(levelCode)) return null;
        return getLevelMap().values().stream()
                .filter(l -> levelCode.equals(l.getLevelCode()))
                .findFirst().orElse(null);
    }

    private String getLevelCodeById(Long levelId) {
        if (levelId == null) return null;
        SecLevel level = getLevelMap().get(levelId);
        return level != null ? level.getLevelCode() : null;
    }

    private Long getCurrentUserId() {
        try {
            org.springframework.security.core.context.SecurityContext ctx =
                org.springframework.security.core.context.SecurityContextHolder.getContext();
            if (ctx != null && ctx.getAuthentication() != null) {
                Object principal = ctx.getAuthentication().getPrincipal();
                if (principal instanceof com.bagdatahouse.core.dto.CurrentUser) {
                    return ((com.bagdatahouse.core.dto.CurrentUser) principal).getUserId();
                }
                if (principal instanceof Long) {
                    return (Long) principal;
                }
                if (principal instanceof Integer) {
                    return ((Integer) principal).longValue();
                }
            }
        } catch (Exception e) {
            log.debug("获取当前用户ID失败: {}", e.getMessage());
        }
        return 1L;
    }

    // ==================== 分类等级推荐绑定实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveClassLevelBinding(SecClassLevelBindingSaveDTO dto) {
        if (dto == null || dto.getClassId() == null) {
            throw new BusinessException(400, "分类ID不能为空");
        }
        if (dto.getLevelIds() == null || dto.getLevelIds().isEmpty()) {
            throw new BusinessException(400, "请至少选择一个等级");
        }
        // 先删除该分类所有旧绑定（软删除）
        LambdaQueryWrapper<SecClassLevelBinding> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(SecClassLevelBinding::getClassId, dto.getClassId());
        SecClassLevelBinding one = classLevelBindingMapper.selectOne(delWrapper);
        if (one != null) {
            classLevelBindingMapper.delete(delWrapper);
        }
        // 批量插入新绑定
        Long userId = getCurrentUserId();
        for (Long levelId : dto.getLevelIds()) {
            SecClassLevelBinding binding = SecClassLevelBinding.builder()
                    .classId(dto.getClassId())
                    .levelId(levelId)
                    .isRecommended(1)
                    .bindingDesc("用户配置绑定")
                    .createUser(userId)
                    .createTime(LocalDateTime.now())
                    .build();
            classLevelBindingMapper.insert(binding);
        }
        return Result.success();
    }

    @Override
    public Result<List<SecClassLevelBindingVO>> listClassLevelBindings(Long classId) {
        if (classId == null) {
            return Result.success(Collections.emptyList());
        }
        LambdaQueryWrapper<SecClassLevelBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecClassLevelBinding::getClassId, classId)
                .orderByAsc(SecClassLevelBinding::getLevelId);
        List<SecClassLevelBinding> bindings = classLevelBindingMapper.selectList(wrapper);

        List<SecClassLevelBindingVO> voList = bindings.stream().map(b -> {
            // 用 ThreadLocal 中的维表缓存，避免每条 binding 再查一次 DB
            SecClassification classification = getClassificationMap().get(b.getClassId());
            SecLevel level = getLevelMap().get(b.getLevelId());
            return SecClassLevelBindingVO.builder()
                    .id(b.getId())
                    .classId(b.getClassId())
                    .className(classification != null ? classification.getClassName() : null)
                    .levelId(b.getLevelId())
                    .levelCode(level != null ? level.getLevelCode() : null)
                    .levelName(level != null ? level.getLevelName() : null)
                    .levelValue(level != null ? level.getLevelValue() : null)
                    .isRecommended(b.getIsRecommended())
                    .bindingDesc(b.getBindingDesc())
                    .createTime(b.getCreateTime())
                    .updateTime(b.getUpdateTime())
                    .build();
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @Override
    public Result<List<SecLevelVO>> listRecommendedLevels(Long classId) {
        if (classId == null) {
            return Result.success(Collections.emptyList());
        }
        LambdaQueryWrapper<SecClassLevelBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecClassLevelBinding::getClassId, classId)
                .eq(SecClassLevelBinding::getIsRecommended, 1);
        List<SecClassLevelBinding> bindings = classLevelBindingMapper.selectList(wrapper);

        List<SecLevelVO> voList = bindings.stream().map(b -> {
            SecLevel level = getLevelMap().get(b.getLevelId());
            if (level == null) return null;
            return SecLevelVO.builder()
                    .id(level.getId())
                    .levelCode(level.getLevelCode())
                    .levelName(level.getLevelName())
                    .levelDesc(level.getLevelDesc())
                    .levelValue(level.getLevelValue())
                    .color(level.getColor())
                    .icon(level.getIcon())
                    .status(level.getStatus())
                    .createTime(level.getCreateTime())
                    .build();
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return Result.success(voList);
    }

    @Override
    public Result<Void> deleteClassLevelBinding(Long id) {
        if (id == null) {
            throw new BusinessException(400, "绑定ID不能为空");
        }
        classLevelBindingMapper.deleteById(id);
        return Result.success();
    }

    // ==================== 安全总览与健康分 ====================

    @Override
    public Result<Map<String, Object>> getSecurityOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 敏感资产总数
        Long totalSensitiveCount = sensitivityMapper.selectCount(null);
        overview.put("totalSensitiveFieldCount", totalSensitiveCount);

        // 按等级统计（L4/L3/L2/L1）
        Map<Long, SecLevel> levelMap = getLevelMap();
        Map<String, Long> countByLevel = new HashMap<>();
        // 初始化各等级数量
        for (SensitivityLevelEnum level : SensitivityLevelEnum.values()) {
            countByLevel.put(level.getCode(), 0L);
        }
        // 从 levelMap 中获取等级名称并统计
        Map<Long, Long> groupByLevelId = buildLevelIdCountMap();
        for (Map.Entry<Long, Long> entry : groupByLevelId.entrySet()) {
            SecLevel level = levelMap.get(entry.getKey());
            if (level != null && level.getLevelCode() != null) {
                countByLevel.put(level.getLevelCode(), entry.getValue());
            }
        }
        overview.put("countByLevel", countByLevel);
        overview.put("l4Count", countByLevel.getOrDefault("L4", 0L));
        overview.put("l3Count", countByLevel.getOrDefault("L3", 0L));
        overview.put("l2Count", countByLevel.getOrDefault("L2", 0L));
        overview.put("l1Count", countByLevel.getOrDefault("L1", 0L));

        // 待审核数
        LambdaQueryWrapper<SecColumnSensitivity> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(SecColumnSensitivity::getReviewStatus, ReviewStatusEnum.PENDING.getCode());
        overview.put("pendingReviewCount", sensitivityMapper.selectCount(pendingWrapper));

        // 本周扫描数（本周内的扫描批次数量）
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        LambdaQueryWrapper<SecColumnSensitivity> weeklyWrapper = new LambdaQueryWrapper<>();
        weeklyWrapper.ge(SecColumnSensitivity::getCreateTime, weekStart);
        weeklyWrapper.select(SecColumnSensitivity::getScanBatchNo);
        List<SecColumnSensitivity> weeklyScans = sensitivityMapper.selectList(weeklyWrapper);
        long weeklyScanCount = weeklyScans.stream()
            .map(SecColumnSensitivity::getScanBatchNo)
            .filter(batchNo -> batchNo != null && !batchNo.isEmpty())
            .distinct()
            .count();
        overview.put("weeklyScanCount", weeklyScanCount);

        // 健康分（简化：覆盖率 = 已审核数 / 总数）
        LambdaQueryWrapper<SecColumnSensitivity> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.eq(SecColumnSensitivity::getReviewStatus, ReviewStatusEnum.APPROVED.getCode());
        Long approvedCount = sensitivityMapper.selectCount(approvedWrapper);
        double coverage = totalSensitiveCount > 0
            ? (double) approvedCount / totalSensitiveCount * 100
            : 0;
        overview.put("sensitiveCoverage", Math.round(coverage * 100.0) / 100.0);

        // 敏感资产分布（按等级）
        Map<String, Long> sensitiveFieldDistribution = new HashMap<>();
        sensitiveFieldDistribution.put("L4_机密", countByLevel.getOrDefault("L4", 0L));
        sensitiveFieldDistribution.put("L3_敏感", countByLevel.getOrDefault("L3", 0L));
        sensitiveFieldDistribution.put("L2_内部", countByLevel.getOrDefault("L2", 0L));
        sensitiveFieldDistribution.put("L1_公开", countByLevel.getOrDefault("L1", 0L));
        overview.put("sensitiveFieldDistribution", sensitiveFieldDistribution);

        // 敏感字段趋势（简化处理，返回空列表）
        overview.put("sensitiveFieldTrend", List.of());

        // 待审核字段列表（最多取5条）
        Map<Long, DqDatasource> dsMap = getDatasourceMap();
        Map<Long, SecClassification> classMap = getClassificationMap();
        Map<Long, SecSensitivityRule> ruleMap = getRuleMap();
        Page<SecColumnSensitivity> pendingPage = sensitivityMapper.selectPage(
            new Page<>(1, 5), pendingWrapper.orderByAsc(SecColumnSensitivity::getCreateTime)
        );
        List<SecColumnSensitivityVO> pendingFields = pendingPage.getRecords().stream()
            .map(s -> buildSensitivityVO(s, dsMap, classMap, levelMap, ruleMap, null))
            .collect(Collectors.toList());
        overview.put("pendingReviewFields", pendingFields);

        return Result.success(overview);
    }

    @Override
    public Result<Map<String, Object>> getHealthScore() {
        Map<String, Object> health = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        Long totalCount = sensitivityMapper.selectCount(null);
        if (totalCount == 0) {
            health.put("score", 0);
            health.put("grade", "UNKNOWN");
            health.put("gradeLabel", "暂无数据");
            health.put("sensitivityCoverageScore", 0);
            health.put("complianceRateScore", 0);
            health.put("alertResponseRateScore", 100);
            health.put("governanceTimelinessScore", 0);
            health.put("totalSensitivityCount", 0);
            health.put("approvedSensitivityCount", 0);
            health.put("totalMetadataColumnCount", 0);
            health.put("totalAlertCount", 0);
            health.put("resolvedAlertCount", 0);
            health.put("newPendingCount7d", 0);
            health.put("reviewedWithin7dCount", 0);
            health.put("lastUpdated", now.toString());
            return Result.success(health);
        }

        // ============ 1. 敏感覆盖率得分 ============
        // 覆盖率 = (已识别敏感字段数 / 应识别预估数) × 100
        // 应识别预估数 = gov_metadata_column 表中所有字段数
        LambdaQueryWrapper<GovMetadataColumn> allColWrapper = new LambdaQueryWrapper<>();
        long totalMetadataColumnCount = govMetadataColumnMapper.selectCount(allColWrapper);
        double sensitivityCoverageScore;
        if (totalMetadataColumnCount == 0) {
            sensitivityCoverageScore = 0.0;
        } else {
            sensitivityCoverageScore = (double) totalCount / totalMetadataColumnCount * 100;
            // 上限 100%
            sensitivityCoverageScore = Math.min(sensitivityCoverageScore, 100.0);
        }

        // ============ 2. 合规率得分 ============
        // 合规率 = (已审核通过 / 已识别总数) × 100
        LambdaQueryWrapper<SecColumnSensitivity> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.eq(SecColumnSensitivity::getReviewStatus, ReviewStatusEnum.APPROVED.getCode());
        Long approvedCount = sensitivityMapper.selectCount(approvedWrapper);
        double complianceRateScore = totalCount > 0
            ? (double) approvedCount / totalCount * 100
            : 0.0;

        // ============ 3. 告警响应率得分 ============
        // 响应率 = (已解决告警数 / 触发告警总数) × 100
        LambdaQueryWrapper<MonitorAlertRecord> alertWrapper = new LambdaQueryWrapper<>();
        long totalAlertCount = alertRecordMapper.selectCount(alertWrapper);

        LambdaQueryWrapper<MonitorAlertRecord> resolvedWrapper = new LambdaQueryWrapper<>();
        resolvedWrapper.eq(MonitorAlertRecord::getStatus, "resolved");
        long resolvedAlertCount = alertRecordMapper.selectCount(resolvedWrapper);

        double alertResponseRateScore;
        if (totalAlertCount == 0) {
            alertResponseRateScore = 100.0; // 无告警记录时，按 100% 约定处理
        } else {
            alertResponseRateScore = (double) resolvedAlertCount / totalAlertCount * 100;
        }

        // ============ 4. 治理及时性 ============
        // 及时性 = (7天内完成审核的字段数 / 7天内新增待审核字段数) × 100
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime nowDt = now;

        // 7天内新增的待审核字段数
        LambdaQueryWrapper<SecColumnSensitivity> newPendingWrapper = new LambdaQueryWrapper<>();
        newPendingWrapper.eq(SecColumnSensitivity::getReviewStatus, ReviewStatusEnum.PENDING.getCode());
        newPendingWrapper.ge(SecColumnSensitivity::getCreateTime, sevenDaysAgo);
        long newPendingCount7d = sensitivityMapper.selectCount(newPendingWrapper);

        // 7天内完成审核（APPROVED）的字段数
        LambdaQueryWrapper<SecColumnSensitivity> reviewedWrapper = new LambdaQueryWrapper<>();
        reviewedWrapper.eq(SecColumnSensitivity::getReviewStatus, ReviewStatusEnum.APPROVED.getCode());
        reviewedWrapper.ge(SecColumnSensitivity::getApprovedTime, sevenDaysAgo);
        reviewedWrapper.le(SecColumnSensitivity::getApprovedTime, nowDt);
        long reviewedWithin7dCount = sensitivityMapper.selectCount(reviewedWrapper);

        double governanceTimelinessScore;
        if (newPendingCount7d == 0) {
            governanceTimelinessScore = 100.0; // 无新增待审核时，按 100% 约定处理
        } else {
            governanceTimelinessScore = (double) reviewedWithin7dCount / newPendingCount7d * 100;
            governanceTimelinessScore = Math.min(governanceTimelinessScore, 100.0);
        }

        // ============ 5. 综合分 ============
        // 综合分 = 覆盖率×0.3 + 合规率×0.3 + 告警响应×0.25 + 治理及时性×0.15
        double totalScore = sensitivityCoverageScore * 0.3
                + complianceRateScore * 0.3
                + alertResponseRateScore * 0.25
                + governanceTimelinessScore * 0.15;
        totalScore = Math.round(totalScore * 100.0) / 100.0;

        health.put("score", totalScore);
        health.put("sensitivityCoverageScore", Math.round(sensitivityCoverageScore * 100.0) / 100.0);
        health.put("complianceRateScore", Math.round(complianceRateScore * 100.0) / 100.0);
        health.put("alertResponseRateScore", Math.round(alertResponseRateScore * 100.0) / 100.0);
        health.put("governanceTimelinessScore", Math.round(governanceTimelinessScore * 100.0) / 100.0);

        // 辅助数据（供前端展示详情）
        health.put("totalSensitivityCount", totalCount);
        health.put("approvedSensitivityCount", approvedCount);
        health.put("totalMetadataColumnCount", totalMetadataColumnCount);
        health.put("totalAlertCount", totalAlertCount);
        health.put("resolvedAlertCount", resolvedAlertCount);
        health.put("newPendingCount7d", newPendingCount7d);
        health.put("reviewedWithin7dCount", reviewedWithin7dCount);
        health.put("lastUpdated", now.toString());

        // ============ 6. 等级判定 ============
        String grade, gradeLabel;
        if (totalScore >= 90) {
            grade = "EXCELLENT";
            gradeLabel = "优秀";
        } else if (totalScore >= 70) {
            grade = "GOOD";
            gradeLabel = "良好";
        } else if (totalScore >= 50) {
            grade = "FAIR";
            gradeLabel = "一般";
        } else {
            grade = "POOR";
            gradeLabel = "较差";
        }
        health.put("grade", grade);
        health.put("gradeLabel", gradeLabel);

        return Result.success(health);
    }

    private Map<Long, Long> buildLevelIdCountMap() {
        List<Map<String, Object>> rows = sensitivityMapper.countGroupByLevelId();
        Map<Long, Long> out = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object lid = row.get("levelId");
            Object cnt = row.get("cnt");
            if (lid != null && cnt != null) {
                out.put(((Number) lid).longValue(), toAggregateLong(cnt));
            }
        }
        return out;
    }

    @Override
    public Result<Map<String, Object>> getSecurityOverviewTrend(Integer days) {
        int daysValue = days != null && days > 0 ? days : 30;
        Map<String, Object> trend = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate rangeStartDate = today.minusDays(daysValue - 1);
        LocalDateTime rangeStart = rangeStartDate.atStartOfDay();
        LocalDateTime rangeEnd = today.atTime(23, 59, 59);

        Map<Long, SecLevel> levelMap = getLevelMap();
        Map<String, Long> codeToLevelId = new HashMap<>();
        for (Map.Entry<Long, SecLevel> entry : levelMap.entrySet()) {
            SecLevel lv = entry.getValue();
            if (lv != null && lv.getLevelCode() != null) {
                codeToLevelId.put(lv.getLevelCode(), entry.getKey());
            }
        }

        List<Map<String, Object>> byDayLevelRows = sensitivityMapper.countCreateByDayAndLevel(rangeStart, rangeEnd);
        List<Map<String, Object>> byDayTotalRows = sensitivityMapper.countCreateByDay(rangeStart, rangeEnd);
        List<Map<String, Object>> byDayApprovedRows = sensitivityMapper.countApprovedByDay(rangeStart, rangeEnd);

        Map<String, Map<Long, Long>> levelCountByDate = new HashMap<>();
        for (Map<String, Object> row : byDayLevelRows) {
            String statDate = row.get("statDate") != null ? row.get("statDate").toString() : null;
            Object lid = row.get("levelId");
            Object cnt = row.get("cnt");
            if (statDate == null || lid == null || cnt == null) {
                continue;
            }
            long levelId = ((Number) lid).longValue();
            levelCountByDate
                    .computeIfAbsent(statDate, k -> new HashMap<>())
                    .put(levelId, toAggregateLong(cnt));
        }

        Map<String, Long> totalByDate = new HashMap<>();
        for (Map<String, Object> row : byDayTotalRows) {
            String statDate = row.get("statDate") != null ? row.get("statDate").toString() : null;
            Object cnt = row.get("cnt");
            if (statDate != null && cnt != null) {
                totalByDate.put(statDate, toAggregateLong(cnt));
            }
        }

        Map<String, Long> approvedByDate = new HashMap<>();
        for (Map<String, Object> row : byDayApprovedRows) {
            String statDate = row.get("statDate") != null ? row.get("statDate").toString() : null;
            Object cnt = row.get("cnt");
            if (statDate != null && cnt != null) {
                approvedByDate.put(statDate, toAggregateLong(cnt));
            }
        }

        List<Map<String, Object>> trendData = new ArrayList<>();
        for (int i = daysValue - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString();

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);

            Map<Long, Long> countsForDay = levelCountByDate.getOrDefault(dateStr, Collections.emptyMap());
            for (SensitivityLevelEnum level : SensitivityLevelEnum.values()) {
                Long targetLevelId = codeToLevelId.get(level.getCode());
                long count = 0L;
                if (targetLevelId != null) {
                    count = countsForDay.getOrDefault(targetLevelId, 0L);
                }
                dayData.put(level.getCode().toLowerCase() + "Count", count);
            }

            dayData.put("totalCount", totalByDate.getOrDefault(dateStr, 0L));
            dayData.put("approvedCount", approvedByDate.getOrDefault(dateStr, 0L));

            trendData.add(dayData);
        }

        trend.put("days", daysValue);
        trend.put("trendData", trendData);

        return Result.success(trend);
    }

    @Override
    public Result<Page<Map<String, Object>>> pageScanHistory(Integer pageNum, Integer pageSize,
            String batchNo, Long dsId, String status) {
        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 10;

        // 当前扫描批次无独立状态表：仅「成功写入敏感记录」的批次；非 SUCCESS 筛选时返回空
        if (StringUtils.hasText(status) && !"SUCCESS".equalsIgnoreCase(status)) {
            Page<Map<String, Object>> empty = new Page<>(pn, ps);
            empty.setRecords(Collections.emptyList());
            empty.setTotal(0);
            return Result.success(empty);
        }

        LambdaQueryWrapper<SecColumnSensitivity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(batchNo)) {
            wrapper.like(SecColumnSensitivity::getScanBatchNo, batchNo);
        }
        if (dsId != null) {
            wrapper.eq(SecColumnSensitivity::getDsId, dsId);
        }
        List<SecColumnSensitivity> allRecords = sensitivityMapper.selectList(wrapper);

        Map<String, List<SecColumnSensitivity>> groupByBatch = allRecords.stream()
                .filter(s -> StringUtils.hasText(s.getScanBatchNo()))
                .collect(Collectors.groupingBy(SecColumnSensitivity::getScanBatchNo));

        List<String> batchNos = new ArrayList<>(groupByBatch.keySet());
        batchNos.sort((a, b) -> {
            LocalDateTime maxA = groupByBatch.get(a).stream()
                    .map(SecColumnSensitivity::getCreateTime)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(LocalDateTime.MIN);
            LocalDateTime maxB = groupByBatch.get(b).stream()
                    .map(SecColumnSensitivity::getCreateTime)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(LocalDateTime.MIN);
            return maxB.compareTo(maxA);
        });

        Map<Long, DqDatasource> dsMap = getDatasourceMap();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        int start = (pn - 1) * ps;
        int end = Math.min(start + ps, batchNos.size());
        List<Map<String, Object>> records = new ArrayList<>();

        if (start < batchNos.size()) {
            for (int i = start; i < end; i++) {
                String bn = batchNos.get(i);
                List<SecColumnSensitivity> batchRecords = groupByBatch.get(bn);
                if (batchRecords == null || batchRecords.isEmpty()) {
                    continue;
                }

                SecColumnSensitivity first = batchRecords.get(0);
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("batchNo", bn);
                record.put("dsId", first.getDsId());
                DqDatasource ds = dsMap.get(first.getDsId());
                record.put("dsName", ds != null ? ds.getDsName() : null);

                LocalDateTime startTs = batchRecords.stream()
                        .map(r -> r.getScanTime() != null ? r.getScanTime() : r.getCreateTime())
                        .filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(null);
                LocalDateTime endTs = batchRecords.stream()
                        .map(SecColumnSensitivity::getCreateTime)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null);

                record.put("startTime", startTs != null ? startTs.format(dtf) : null);

                long elapsedMs = 0L;
                if (startTs != null && endTs != null) {
                    elapsedMs = Duration.between(startTs, endTs).toMillis();
                    if (elapsedMs < 0) {
                        elapsedMs = 0;
                    }
                }
                record.put("elapsedMs", elapsedMs > 0 ? elapsedMs : null);

                Map<Long, GovMetadata> metaCache = new HashMap<>();
                Set<String> distinctTables = new HashSet<>();
                for (SecColumnSensitivity r : batchRecords) {
                    String tn = resolveTableNameForSensitivity(r, metaCache);
                    if (StringUtils.hasText(tn)) {
                        distinctTables.add(tn.trim());
                    }
                }
                record.put("totalTableCount", distinctTables.size());
                record.put("totalColumnCount", batchRecords.size());
                record.put("foundSensitiveCount", batchRecords.size());

                record.put("status", "SUCCESS");
                record.put("statusLabel", "成功");

                record.put("scanTime", first.getCreateTime() != null ? first.getCreateTime().format(dtf) : null);
                record.put("totalCount", batchRecords.size());
                long approved = batchRecords.stream()
                        .filter(s -> ReviewStatusEnum.APPROVED.getCode().equals(s.getReviewStatus()))
                        .count();
                record.put("approvedCount", approved);

                records.add(record);
            }
        }

        Page<Map<String, Object>> result = new Page<>(pn, ps);
        result.setRecords(records);
        result.setTotal(batchNos.size());
        return Result.success(result);
    }

    // ==================== 新字段发现机制（T6） ====================

    @Override
    public Result<Page<SecNewColumnAlert>> pageNewColumnAlerts(Integer pageNum, Integer pageSize,
            String status, Long dsId, String tableName) {
        Page<SecNewColumnAlert> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecNewColumnAlert> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(SecNewColumnAlert::getStatus, status);
        }
        if (dsId != null) {
            wrapper.eq(SecNewColumnAlert::getDsId, dsId);
        }
        if (StringUtils.hasText(tableName)) {
            wrapper.like(SecNewColumnAlert::getTableName, tableName);
        }
        wrapper.orderByDesc(SecNewColumnAlert::getCreatedAt);

        IPage<SecNewColumnAlert> result = newColumnAlertMapper.selectPage(page, wrapper);
        return Result.success((Page<SecNewColumnAlert>) result);
    }

    @Override
    public Result<Long> countPendingNewColumnAlerts() {
        LambdaQueryWrapper<SecNewColumnAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecNewColumnAlert::getStatus, "PENDING");
        long count = newColumnAlertMapper.selectCount(wrapper);
        return Result.success(count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecScanResultVO> scanNewColumnAlert(Long alertId, Long operatorId) {
        SecNewColumnAlert alert = newColumnAlertMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException(2001, "新字段告警记录不存在");
        }
        if (!"PENDING".equals(alert.getStatus())) {
            throw new BusinessException(2002, "该告警已处理，无法重复扫描");
        }

        // 构建扫描 DTO：仅扫描该字段
        SecSensitivityScanDTO dto = SecSensitivityScanDTO.builder()
                .dsId(alert.getDsId())
                .tableNames(alert.getTableName())
                .scanScope("SPECIFIC")
                .scanColumnName(true)
                .scanColumnComment(true)
                .scanDataType(false)
                .enableContentScan(false)
                .sampleSize(0)
                .incremental(false)
                .scanMode("SINGLE_COLUMN")
                .columnNames(Collections.singletonList(alert.getColumnName()))
                .createUser(operatorId)
                .build();

        DqDatasource datasource = datasourceMapper.selectById(alert.getDsId());
        if (datasource == null) {
            throw new BusinessException(2001, "数据源不存在");
        }
        String batchNo = "SCAN_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + System.currentTimeMillis() % 1000;

        // 执行扫描（复用内部逻辑，避免嵌套事务）
        SecScanResultVO scanResult = executeSensitiveFieldScan(dto, batchNo, datasource, System.currentTimeMillis());

        // 更新告警状态为已扫描
        SecNewColumnAlert updateAlert = new SecNewColumnAlert();
        updateAlert.setId(alertId);
        updateAlert.setStatus("SCANNED");
        updateAlert.setScanBatchNo(scanResult.getScanBatchNo());
        updateAlert.setHandleUser(operatorId);
        updateAlert.setHandleTime(LocalDateTime.now());
        updateAlert.setHandleComment("一键扫描完成，扫描批次：" + scanResult.getScanBatchNo());
        updateAlert.setUpdateTime(LocalDateTime.now());
        newColumnAlertMapper.updateById(updateAlert);

        return Result.success(scanResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> dismissNewColumnAlert(Long alertId, Long operatorId, String comment) {
        SecNewColumnAlert alert = newColumnAlertMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException(2001, "新字段告警记录不存在");
        }
        if (!"PENDING".equals(alert.getStatus())) {
            throw new BusinessException(2002, "该告警已处理，无法重复操作");
        }

        SecNewColumnAlert updateAlert = new SecNewColumnAlert();
        updateAlert.setId(alertId);
        updateAlert.setStatus("DISMISSED");
        updateAlert.setHandleUser(operatorId);
        updateAlert.setHandleTime(LocalDateTime.now());
        updateAlert.setHandleComment(comment != null ? comment : "管理员忽略");
        updateAlert.setUpdateTime(LocalDateTime.now());
        newColumnAlertMapper.updateById(updateAlert);

        return Result.success();
    }

    /**
     * 将元数据扫描中发现的新字段写入告警表（内部调用）
     * <p>
     * 由 AbstractMetadataScanner 在扫描表结构时调用，当发现
     * gov_metadata_column 中不存在的字段时，写入 sec_new_column_alert
     *
     * @param dsId 数据源ID
     * @param tableName 表名
     * @param columnName 字段名
     * @param dataType 数据类型
     * @param columnComment 字段注释
     * @param scanBatchNo 扫描批次号
     * @return 写入的告警记录数（0表示已存在）
     */
    @Transactional(rollbackFor = Exception.class)
    public int recordNewColumnAlert(Long dsId, String tableName, String columnName,
            String dataType, String columnComment, String scanBatchNo) {
        // 检查是否已存在
        LambdaQueryWrapper<SecNewColumnAlert> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(SecNewColumnAlert::getDsId, dsId);
        existWrapper.eq(SecNewColumnAlert::getTableName, tableName);
        existWrapper.eq(SecNewColumnAlert::getColumnName, columnName);
        existWrapper.ne(SecNewColumnAlert::getStatus, "DISMISSED");
        long existingCount = newColumnAlertMapper.selectCount(existWrapper);
        if (existingCount > 0) {
            return 0;
        }

        SecNewColumnAlert alert = SecNewColumnAlert.builder()
                .dsId(dsId)
                .tableName(tableName)
                .columnName(columnName)
                .dataType(dataType)
                .columnComment(columnComment)
                .alertType("NEW_COLUMN")
                .status("PENDING")
                .scanBatchNo(scanBatchNo)
                .createdAt(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        newColumnAlertMapper.insert(alert);
        log.info("发现新字段[dsId={}, table={}, column={}]，已写入告警表", dsId, tableName, columnName);
        return 1;
    }
}
