package com.bagdatahouse.governance.glossary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovGlossaryMapping;
import com.bagdatahouse.core.entity.GovGlossaryTerm;
import com.bagdatahouse.core.entity.SysUser;
import com.bagdatahouse.core.mapper.GovGlossaryMappingMapper;
import com.bagdatahouse.core.mapper.GovGlossaryTermMapper;
import com.bagdatahouse.core.mapper.SysUserMapper;
import com.bagdatahouse.governance.glossary.dto.GlossaryMappingDTO;
import com.bagdatahouse.governance.glossary.service.GlossaryMappingService;
import com.bagdatahouse.governance.glossary.vo.GlossaryMappingVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 术语-字段映射服务实现
 */
@Slf4j
@Service
public class GlossaryMappingServiceImpl extends ServiceImpl<GovGlossaryMappingMapper, GovGlossaryMapping>
        implements GlossaryMappingService {

    @Autowired
    private GovGlossaryMappingMapper mappingMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private GovGlossaryTermMapper termMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GovGlossaryMapping> save(GlossaryMappingDTO dto) {
        validateDto(dto, null);

        GovGlossaryMapping mapping = new GovGlossaryMapping();
        BeanUtils.copyProperties(dto, mapping);
        mapping.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");
        mapping.setCreateTime(LocalDateTime.now());
        mappingMapper.insert(mapping);

        return Result.success(mapping);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchSave(Long termId, List<GlossaryMappingDTO> mappings, Long createUser) {
        if (mappings == null || mappings.isEmpty()) {
            return Result.success();
        }

        for (GlossaryMappingDTO dto : mappings) {
            GovGlossaryMapping mapping = new GovGlossaryMapping();
            BeanUtils.copyProperties(dto, mapping);
            mapping.setTermId(termId);
            mapping.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");
            mapping.setCreateUser(createUser);
            mapping.setCreateTime(LocalDateTime.now());
            mappingMapper.insert(mapping);
        }

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, GlossaryMappingDTO dto) {
        GovGlossaryMapping existing = mappingMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "映射不存在");
        }

        validateDto(dto, id);

        GovGlossaryMapping mapping = new GovGlossaryMapping();
        BeanUtils.copyProperties(dto, mapping);
        mapping.setId(id);
        mapping.setUpdateTime(LocalDateTime.now());
        mappingMapper.updateById(mapping);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        GovGlossaryMapping existing = mappingMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "映射不存在");
        }

        mappingMapper.deleteById(id);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }

        mappingMapper.deleteBatchIds(ids);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> approve(Long id, Long approvedBy) {
        GovGlossaryMapping existing = mappingMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "映射不存在");
        }

        GovGlossaryMapping mapping = new GovGlossaryMapping();
        mapping.setId(id);
        mapping.setStatus("APPROVED");
        mapping.setApprovedBy(approvedBy);
        mapping.setApprovedTime(LocalDateTime.now());
        mapping.setUpdateTime(LocalDateTime.now());
        mappingMapper.updateById(mapping);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> reject(Long id, Long approvedBy, String rejectReason) {
        GovGlossaryMapping existing = mappingMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "映射不存在");
        }

        GovGlossaryMapping mapping = new GovGlossaryMapping();
        mapping.setId(id);
        mapping.setStatus("REJECTED");
        mapping.setApprovedBy(approvedBy);
        mapping.setApprovedTime(LocalDateTime.now());
        mapping.setRejectReason(rejectReason);
        mapping.setUpdateTime(LocalDateTime.now());
        mappingMapper.updateById(mapping);

        return Result.success();
    }

    @Override
    public Result<List<GlossaryMappingVO>> getByTermId(Long termId) {
        LambdaQueryWrapper<GovGlossaryMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovGlossaryMapping::getTermId, termId)
              .orderByDesc(GovGlossaryMapping::getCreateTime);
        List<GovGlossaryMapping> list = mappingMapper.selectList(wrapper);

        // Build term map
        List<Long> termIds = list.stream()
                .map(GovGlossaryMapping::getTermId)
                .filter(t -> t != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        Map<Long, GovGlossaryTerm> termMap = termIds.isEmpty() ? new java.util.HashMap<>() :
                termMapper.selectBatchIds(termIds).stream().collect(java.util.stream.Collectors.toMap(GovGlossaryTerm::getId, t -> t));

        List<GlossaryMappingVO> vos = list.stream()
                .map(m -> buildFullVO(m, termMap, new java.util.HashMap<>(), new java.util.HashMap<>()))
                .collect(java.util.stream.Collectors.toList());
        return Result.success(vos);
    }

    @Override
    public Result<List<GlossaryMappingVO>> getByMetadataField(Long dsId, String tableName, String columnName) {
        LambdaQueryWrapper<GovGlossaryMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovGlossaryMapping::getDsId, dsId)
              .eq(GovGlossaryMapping::getTableName, tableName)
              .eq(GovGlossaryMapping::getColumnName, columnName)
              .orderByDesc(GovGlossaryMapping::getCreateTime);
        List<GovGlossaryMapping> list = mappingMapper.selectList(wrapper);

        // Build term map
        List<Long> termIds = list.stream()
                .map(GovGlossaryMapping::getTermId)
                .filter(t -> t != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        Map<Long, GovGlossaryTerm> termMap = termIds.isEmpty() ? new java.util.HashMap<>() :
                termMapper.selectBatchIds(termIds).stream().collect(java.util.stream.Collectors.toMap(GovGlossaryTerm::getId, t -> t));

        List<GlossaryMappingVO> vos = list.stream()
                .map(m -> buildFullVO(m, termMap, new java.util.HashMap<>(), new java.util.HashMap<>()))
                .collect(java.util.stream.Collectors.toList());
        return Result.success(vos);
    }

    private void validateDto(GlossaryMappingDTO dto, Long excludeId) {
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

    @Override
    public Result<Page<GlossaryMappingVO>> pagePending(Integer pageNum, Integer pageSize, String termName, Long dsId, String status) {
        // 先按 status 和 dsId 查询所有记录
        LambdaQueryWrapper<GovGlossaryMapping> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(GovGlossaryMapping::getStatus, status);
        }
        if (dsId != null) {
            wrapper.eq(GovGlossaryMapping::getDsId, dsId);
        }
        wrapper.orderByDesc(GovGlossaryMapping::getCreateTime);

        List<GovGlossaryMapping> allRecords = mappingMapper.selectList(wrapper);

        // 按术语名过滤（内存过滤，因为 join 较复杂）
        List<GovGlossaryMapping> filtered = allRecords;
        if (StringUtils.hasText(termName)) {
            final String kw = termName.toLowerCase();
            List<Long> termIds = allRecords.stream()
                    .map(GovGlossaryMapping::getTermId)
                    .filter(t -> t != null)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            Map<Long, GovGlossaryTerm> termMap;
            if (termIds.isEmpty()) {
                termMap = new java.util.HashMap<>();
            } else {
                List<GovGlossaryTerm> terms = termMapper.selectBatchIds(termIds);
                termMap = terms.stream().collect(java.util.stream.Collectors.toMap(GovGlossaryTerm::getId, t -> t));
            }

            filtered = allRecords.stream()
                    .filter(m -> {
                        GovGlossaryTerm t = termMap.get(m.getTermId());
                        if (t == null) return false;
                        return (t.getTermName() != null && t.getTermName().toLowerCase().contains(kw))
                                || (t.getTermCode() != null && t.getTermCode().toLowerCase().contains(kw))
                                || (t.getTermNameEn() != null && t.getTermNameEn().toLowerCase().contains(kw));
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        // 构建 termMap 和 userMap
        List<Long> termIds = filtered.stream()
                .map(GovGlossaryMapping::getTermId)
                .filter(t -> t != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        final Map<Long, GovGlossaryTerm> termMap;
        if (termIds.isEmpty()) {
            termMap = new java.util.HashMap<>();
        } else {
            List<GovGlossaryTerm> terms = termMapper.selectBatchIds(termIds);
            termMap = terms.stream().collect(java.util.stream.Collectors.toMap(GovGlossaryTerm::getId, t -> t));
        }

        List<Long> approverIds = filtered.stream()
                .map(GovGlossaryMapping::getApprovedBy)
                .filter(u -> u != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        final Map<Long, SysUser> approverMap;
        if (approverIds.isEmpty()) {
            approverMap = new java.util.HashMap<>();
        } else {
            List<SysUser> users = userMapper.selectBatchIds(approverIds);
            approverMap = users.stream().collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u));
        }

        List<Long> creatorIds = filtered.stream()
                .map(GovGlossaryMapping::getCreateUser)
                .filter(u -> u != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        final Map<Long, SysUser> creatorMap;
        if (creatorIds.isEmpty()) {
            creatorMap = new java.util.HashMap<>();
        } else {
            List<SysUser> users = userMapper.selectBatchIds(creatorIds);
            creatorMap = users.stream().collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u));
        }

        // 分页
        int total = filtered.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<GovGlossaryMapping> paged = fromIndex < total ? filtered.subList(fromIndex, toIndex) : java.util.Collections.emptyList();

        Page<GlossaryMappingVO> voPage = new Page<>((long) pageNum, pageSize, total);
        List<GlossaryMappingVO> vos = paged.stream()
                .map(m -> buildFullVO(m, termMap, approverMap, creatorMap))
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(vos);

        return Result.success(voPage);
    }

    @Override
    public Result<List<GlossaryMappingVO>> listPending(String termName, Long dsId) {
        LambdaQueryWrapper<GovGlossaryMapping> wrapper = new LambdaQueryWrapper<>();

        if (dsId != null) {
            wrapper.eq(GovGlossaryMapping::getDsId, dsId);
        }
        wrapper.orderByDesc(GovGlossaryMapping::getCreateTime);

        List<GovGlossaryMapping> list = mappingMapper.selectList(wrapper);

        List<Long> termIds = list.stream()
                .map(GovGlossaryMapping::getTermId)
                .filter(t -> t != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        final Map<Long, GovGlossaryTerm> termMap;
        if (termIds.isEmpty()) {
            termMap = new java.util.HashMap<>();
        } else {
            List<GovGlossaryTerm> terms = termMapper.selectBatchIds(termIds);
            termMap = terms.stream().collect(java.util.stream.Collectors.toMap(GovGlossaryTerm::getId, t -> t));
        }

        List<Long> approverIds = list.stream()
                .map(GovGlossaryMapping::getApprovedBy)
                .filter(u -> u != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        final Map<Long, SysUser> approverMap;
        if (approverIds.isEmpty()) {
            approverMap = new java.util.HashMap<>();
        } else {
            List<SysUser> users = userMapper.selectBatchIds(approverIds);
            approverMap = users.stream().collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u));
        }

        List<Long> creatorIds = list.stream()
                .map(GovGlossaryMapping::getCreateUser)
                .filter(u -> u != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        final Map<Long, SysUser> creatorMap;
        if (creatorIds.isEmpty()) {
            creatorMap = new java.util.HashMap<>();
        } else {
            List<SysUser> users = userMapper.selectBatchIds(creatorIds);
            creatorMap = users.stream().collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u));
        }

        // 按术语名过滤
        List<GovGlossaryMapping> filtered = list;
        if (StringUtils.hasText(termName)) {
            final String kw = termName.toLowerCase();
            filtered = filtered.stream()
                    .filter(m -> {
                        GovGlossaryTerm t = termMap.get(m.getTermId());
                        if (t == null) return false;
                        return (t.getTermName() != null && t.getTermName().toLowerCase().contains(kw))
                                || (t.getTermCode() != null && t.getTermCode().toLowerCase().contains(kw));
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        List<GlossaryMappingVO> vos = filtered.stream()
                .map(m -> buildFullVO(m, termMap, approverMap, creatorMap))
                .collect(java.util.stream.Collectors.toList());

        return Result.success(vos);
    }

    @Override
    public Result<List<GlossaryMappingVO>> getApprovedByDatasource(Long dsId) {
        LambdaQueryWrapper<GovGlossaryMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovGlossaryMapping::getDsId, dsId)
               .eq(GovGlossaryMapping::getStatus, "APPROVED")
               .orderByDesc(GovGlossaryMapping::getCreateTime);
        List<GovGlossaryMapping> list = mappingMapper.selectList(wrapper);

        List<Long> termIds = list.stream()
                .map(GovGlossaryMapping::getTermId)
                .filter(t -> t != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        final Map<Long, GovGlossaryTerm> termMap;
        if (termIds.isEmpty()) {
            termMap = new java.util.HashMap<>();
        } else {
            List<GovGlossaryTerm> terms = termMapper.selectBatchIds(termIds);
            termMap = terms.stream().collect(java.util.stream.Collectors.toMap(GovGlossaryTerm::getId, t -> t));
        }

        List<GlossaryMappingVO> vos = list.stream()
                .map(m -> buildFullVO(m, termMap, new java.util.HashMap<>(), new java.util.HashMap<>()))
                .collect(java.util.stream.Collectors.toList());

        return Result.success(vos);
    }

    @Override
    public Result<List<GlossaryMappingVO>> getApprovedByTable(Long dsId, String tableName) {
        LambdaQueryWrapper<GovGlossaryMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovGlossaryMapping::getDsId, dsId)
               .eq(GovGlossaryMapping::getTableName, tableName)
               .eq(GovGlossaryMapping::getStatus, "APPROVED")
               .orderByDesc(GovGlossaryMapping::getCreateTime);
        List<GovGlossaryMapping> list = mappingMapper.selectList(wrapper);

        List<Long> termIds = list.stream()
                .map(GovGlossaryMapping::getTermId)
                .filter(t -> t != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        final Map<Long, GovGlossaryTerm> termMap;
        if (termIds.isEmpty()) {
            termMap = new java.util.HashMap<>();
        } else {
            List<GovGlossaryTerm> terms = termMapper.selectBatchIds(termIds);
            termMap = terms.stream().collect(java.util.stream.Collectors.toMap(GovGlossaryTerm::getId, t -> t));
        }

        List<GlossaryMappingVO> vos = list.stream()
                .map(m -> buildFullVO(m, termMap, new java.util.HashMap<>(), new java.util.HashMap<>()))
                .collect(java.util.stream.Collectors.toList());

        return Result.success(vos);
    }

    private GlossaryMappingVO buildFullVO(GovGlossaryMapping mapping,
                                           Map<Long, GovGlossaryTerm> termMap,
                                           Map<Long, SysUser> approverMap,
                                           Map<Long, SysUser> creatorMap) {
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

        // 填充术语信息
        if (mapping.getTermId() != null && termMap.containsKey(mapping.getTermId())) {
            GovGlossaryTerm term = termMap.get(mapping.getTermId());
            builder.termName(term.getTermName());
            builder.termCode(term.getTermCode());
        }

        if (mapping.getApprovedBy() != null && approverMap.containsKey(mapping.getApprovedBy())) {
            SysUser approver = approverMap.get(mapping.getApprovedBy());
            builder.approvedByName(approver.getNickname() != null ? approver.getNickname() : approver.getUsername());
        }

        if (mapping.getCreateUser() != null && creatorMap.containsKey(mapping.getCreateUser())) {
            SysUser creator = creatorMap.get(mapping.getCreateUser());
            builder.createUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }

        return builder.build();
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
}
