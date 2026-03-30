package com.bagdatahouse.governance.glossary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovGlossaryCategory;
import com.bagdatahouse.core.entity.GovGlossaryMapping;
import com.bagdatahouse.core.entity.GovGlossaryTerm;
import com.bagdatahouse.core.entity.SysUser;
import com.bagdatahouse.core.mapper.GovGlossaryCategoryMapper;
import com.bagdatahouse.core.mapper.GovGlossaryMappingMapper;
import com.bagdatahouse.core.mapper.GovGlossaryTermMapper;
import com.bagdatahouse.core.mapper.SysUserMapper;
import com.bagdatahouse.governance.glossary.dto.GlossaryCategoryQueryDTO;
import com.bagdatahouse.governance.glossary.dto.GlossaryCategorySaveDTO;
import com.bagdatahouse.governance.glossary.service.GlossaryCategoryService;
import com.bagdatahouse.governance.glossary.vo.GlossaryCategoryDetailVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryCategoryTreeVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryCategoryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 术语分类管理服务实现
 */
@Slf4j
@Service
public class GlossaryCategoryServiceImpl extends ServiceImpl<GovGlossaryCategoryMapper, GovGlossaryCategory>
        implements GlossaryCategoryService {

    @Autowired
    private GovGlossaryCategoryMapper categoryMapper;

    @Autowired
    private GovGlossaryTermMapper termMapper;

    @Autowired
    private GovGlossaryMappingMapper mappingMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GovGlossaryCategory> save(GlossaryCategorySaveDTO dto) {
        validateDto(dto, null);

        GovGlossaryCategory category = new GovGlossaryCategory();
        BeanUtils.copyProperties(dto, category);
        category.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);

        return Result.success(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, GlossaryCategorySaveDTO dto) {
        GovGlossaryCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "分类不存在");
        }

        validateDto(dto, id);

        GovGlossaryCategory category = new GovGlossaryCategory();
        BeanUtils.copyProperties(dto, category);
        category.setId(id);
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        GovGlossaryCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "分类不存在");
        }

        // 收集所有后代分类ID
        List<Long> allDescendantIds = getAllDescendantIds(id);
        allDescendantIds.add(id);

        // 删除所有术语与映射
        for (Long catId : allDescendantIds) {
            LambdaQueryWrapper<GovGlossaryTerm> termWrapper = new LambdaQueryWrapper<>();
            termWrapper.eq(GovGlossaryTerm::getCategoryId, catId);
            List<GovGlossaryTerm> terms = termMapper.selectList(termWrapper);
            for (GovGlossaryTerm term : terms) {
                LambdaQueryWrapper<GovGlossaryMapping> mapWrapper = new LambdaQueryWrapper<>();
                mapWrapper.eq(GovGlossaryMapping::getTermId, term.getId());
                mappingMapper.delete(mapWrapper);
            }
            termMapper.delete(termWrapper);
        }

        // 删除所有分类
        LambdaQueryWrapper<GovGlossaryCategory> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.in(GovGlossaryCategory::getId, allDescendantIds);
        categoryMapper.delete(delWrapper);

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
    public Result<GovGlossaryCategory> getById(Long id) {
        GovGlossaryCategory entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(2001, "分类不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<Page<GlossaryCategoryVO>> page(Integer pageNum, Integer pageSize, GlossaryCategoryQueryDTO queryDTO) {
        Page<GovGlossaryCategory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GovGlossaryCategory> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(GovGlossaryCategory::getSortOrder)
               .orderByDesc(GovGlossaryCategory::getCreateTime);
        Page<GovGlossaryCategory> result = categoryMapper.selectPage(page, wrapper);

        // 批量加载术语数量
        List<Long> termCategoryIds = result.getRecords().stream()
                .map(GovGlossaryCategory::getId)
                .collect(Collectors.toList());
        final Map<Long, Long> termCountMap;
        if (termCategoryIds.isEmpty()) {
            termCountMap = new HashMap<>();
        } else {
            LambdaQueryWrapper<GovGlossaryTerm> termWrapper = new LambdaQueryWrapper<>();
            termWrapper.in(GovGlossaryTerm::getCategoryId, termCategoryIds);
            List<GovGlossaryTerm> terms = termMapper.selectList(termWrapper);
            termCountMap = terms.stream()
                    .collect(Collectors.groupingBy(GovGlossaryTerm::getCategoryId, Collectors.counting()));
        }

        // 批量加载子分类数量
        final Map<Long, Long> childCountMap;
        if (termCategoryIds.isEmpty()) {
            childCountMap = new HashMap<>();
        } else {
            LambdaQueryWrapper<GovGlossaryCategory> childWrapper = new LambdaQueryWrapper<>();
            childWrapper.in(GovGlossaryCategory::getParentId, termCategoryIds);
            List<GovGlossaryCategory> children = categoryMapper.selectList(childWrapper);
            childCountMap = children.stream()
                    .collect(Collectors.groupingBy(GovGlossaryCategory::getParentId, Collectors.counting()));
        }

        // 批量加载创建人
        List<Long> createUserIds = result.getRecords().stream()
                .map(GovGlossaryCategory::getCreateUser)
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

        // 构建父分类名称映射
        Set<Long> parentIds = result.getRecords().stream()
                .map(GovGlossaryCategory::getParentId)
                .filter(p -> p != null && p != 0)
                .collect(Collectors.toSet());
        final Map<Long, String> parentNameMap;
        if (parentIds.isEmpty()) {
            parentNameMap = new HashMap<>();
        } else {
            List<GovGlossaryCategory> parents = categoryMapper.selectBatchIds(parentIds);
            parentNameMap = parents.stream()
                    .collect(Collectors.toMap(GovGlossaryCategory::getId, GovGlossaryCategory::getCategoryName));
        }

        Page<GlossaryCategoryVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<GlossaryCategoryVO> vos = result.getRecords().stream()
                .map(c -> buildVO(c, termCountMap, childCountMap, userMap, parentNameMap))
                .collect(Collectors.toList());
        voPage.setRecords(vos);

        return Result.success(voPage);
    }

    @Override
    public Result<GlossaryCategoryDetailVO> getDetail(Long id) {
        GovGlossaryCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(2001, "分类不存在");
        }

        GlossaryCategoryDetailVO.GlossaryCategoryDetailVOBuilder builder = GlossaryCategoryDetailVO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .categoryName(category.getCategoryName())
                .categoryCode(category.getCategoryCode())
                .categoryDesc(category.getCategoryDesc())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .statusLabel(category.getStatus() != null && category.getStatus() == 1 ? "启用" : "禁用")
                .createUser(category.getCreateUser())
                .createTime(category.getCreateTime())
                .updateUser(category.getUpdateUser())
                .updateTime(category.getUpdateTime());

        if (category.getParentId() != null && category.getParentId() != 0) {
            GovGlossaryCategory parent = categoryMapper.selectById(category.getParentId());
            if (parent != null) {
                builder.parentName(parent.getCategoryName());
            }
        }

        if (category.getCreateUser() != null) {
            SysUser creator = userMapper.selectById(category.getCreateUser());
            if (creator != null) {
                builder.createUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
            }
        }

        // 统计术语数量
        LambdaQueryWrapper<GovGlossaryTerm> termWrapper = new LambdaQueryWrapper<>();
        termWrapper.eq(GovGlossaryTerm::getCategoryId, id);
        long termCount = termMapper.selectCount(termWrapper);
        builder.termCount((int) termCount);

        // 子分类树
        LambdaQueryWrapper<GovGlossaryCategory> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(GovGlossaryCategory::getParentId, id)
                    .orderByAsc(GovGlossaryCategory::getSortOrder);
        List<GovGlossaryCategory> children = categoryMapper.selectList(childWrapper);
        List<GlossaryCategoryTreeVO> childNodes = children.stream()
                .map(this::buildTreeNodeSimple)
                .collect(Collectors.toList());
        builder.children(childNodes);

        return Result.success(builder.build());
    }

    @Override
    public Result<List<GlossaryCategoryTreeVO>> getTree(GlossaryCategoryQueryDTO queryDTO) {
        List<GovGlossaryCategory> all = categoryMapper.selectList(buildQueryWrapper(queryDTO));

        if (all.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<Long, List<GovGlossaryCategory>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(GovGlossaryCategory::getParentId));

        List<GovGlossaryCategory> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .sorted((a, b) -> {
                    if (a.getSortOrder() == null) a.setSortOrder(0);
                    if (b.getSortOrder() == null) b.setSortOrder(0);
                    return a.getSortOrder().compareTo(b.getSortOrder());
                })
                .collect(Collectors.toList());

        List<GlossaryCategoryTreeVO> tree = new ArrayList<>();
        for (GovGlossaryCategory root : roots) {
            tree.add(buildTreeNode(root, childrenMap, all));
        }

        return Result.success(tree);
    }

    @Override
    public Result<List<GlossaryCategoryTreeVO>> getFullTree() {
        List<GovGlossaryCategory> all = categoryMapper.selectList(null);

        if (all.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<Long, List<GovGlossaryCategory>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(GovGlossaryCategory::getParentId));

        List<GovGlossaryCategory> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .sorted((a, b) -> {
                    if (a.getSortOrder() == null) a.setSortOrder(0);
                    if (b.getSortOrder() == null) b.setSortOrder(0);
                    return a.getSortOrder().compareTo(b.getSortOrder());
                })
                .collect(Collectors.toList());

        List<GlossaryCategoryTreeVO> tree = new ArrayList<>();
        for (GovGlossaryCategory root : roots) {
            tree.add(buildTreeNode(root, childrenMap, all));
        }

        return Result.success(tree);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enable(Long id) {
        GovGlossaryCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(2001, "分类不存在");
        }

        GovGlossaryCategory update = new GovGlossaryCategory();
        update.setId(id);
        update.setStatus(1);
        update.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disable(Long id) {
        GovGlossaryCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(2001, "分类不存在");
        }

        GovGlossaryCategory update = new GovGlossaryCategory();
        update.setId(id);
        update.setStatus(0);
        update.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(update);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> move(Long id, Long newParentId) {
        GovGlossaryCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(2001, "分类不存在");
        }

        if (newParentId != null && newParentId > 0) {
            if (newParentId.equals(id)) {
                throw new BusinessException(2001, "不能将分类移动到自身下");
            }
            if (isDescendant(id, newParentId)) {
                throw new BusinessException(2001, "不能将分类移动到其子分类下");
            }
            GovGlossaryCategory parent = categoryMapper.selectById(newParentId);
            if (parent == null) {
                throw new BusinessException(2001, "目标父分类不存在");
            }
        }

        GovGlossaryCategory update = new GovGlossaryCategory();
        update.setId(id);
        update.setParentId(newParentId == null ? 0L : newParentId);
        update.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(update);

        return Result.success();
    }

    @Override
    public Result<List<GlossaryCategoryVO>> list(GlossaryCategoryQueryDTO queryDTO) {
        LambdaQueryWrapper<GovGlossaryCategory> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByAsc(GovGlossaryCategory::getSortOrder)
               .orderByDesc(GovGlossaryCategory::getCreateTime);
        List<GovGlossaryCategory> list = categoryMapper.selectList(wrapper);

        final Map<Long, Long> termCountMap;
        final Map<Long, Long> childCountMap;
        final Map<Long, SysUser> userMap;
        final Map<Long, String> parentNameMap;

        if (list.isEmpty()) {
            termCountMap = new HashMap<>();
            childCountMap = new HashMap<>();
            userMap = new HashMap<>();
            parentNameMap = new HashMap<>();
        } else {
            List<Long> ids = list.stream().map(GovGlossaryCategory::getId).collect(Collectors.toList());

            LambdaQueryWrapper<GovGlossaryTerm> termWrapper = new LambdaQueryWrapper<>();
            termWrapper.in(GovGlossaryTerm::getCategoryId, ids);
            List<GovGlossaryTerm> terms = termMapper.selectList(termWrapper);
            termCountMap = terms.stream()
                    .collect(Collectors.groupingBy(GovGlossaryTerm::getCategoryId, Collectors.counting()));

            LambdaQueryWrapper<GovGlossaryCategory> childWrapper = new LambdaQueryWrapper<>();
            childWrapper.in(GovGlossaryCategory::getParentId, ids);
            List<GovGlossaryCategory> children = categoryMapper.selectList(childWrapper);
            childCountMap = children.stream()
                    .collect(Collectors.groupingBy(GovGlossaryCategory::getParentId, Collectors.counting()));

            List<Long> createUserIds = list.stream()
                    .map(GovGlossaryCategory::getCreateUser)
                    .filter(u -> u != null)
                    .distinct()
                    .collect(Collectors.toList());
            if (createUserIds.isEmpty()) {
                userMap = new HashMap<>();
            } else {
                List<SysUser> users = userMapper.selectBatchIds(createUserIds);
                userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
            }

            Set<Long> parentIds = list.stream()
                    .map(GovGlossaryCategory::getParentId)
                    .filter(p -> p != null && p != 0)
                    .collect(Collectors.toSet());
            if (parentIds.isEmpty()) {
                parentNameMap = new HashMap<>();
            } else {
                List<GovGlossaryCategory> parents = categoryMapper.selectBatchIds(parentIds);
                parentNameMap = parents.stream()
                        .collect(Collectors.toMap(GovGlossaryCategory::getId, GovGlossaryCategory::getCategoryName));
            }
        }

        List<GlossaryCategoryVO> vos = list.stream()
                .map(c -> buildVO(c, termCountMap, childCountMap, userMap, parentNameMap))
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    // ====== private helper methods ======

    private LambdaQueryWrapper<GovGlossaryCategory> buildQueryWrapper(GlossaryCategoryQueryDTO queryDTO) {
        LambdaQueryWrapper<GovGlossaryCategory> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO == null) {
            return wrapper;
        }

        if (StringUtils.hasText(queryDTO.getCategoryName())) {
            wrapper.like(GovGlossaryCategory::getCategoryName, queryDTO.getCategoryName());
        }
        if (StringUtils.hasText(queryDTO.getCategoryCode())) {
            wrapper.eq(GovGlossaryCategory::getCategoryCode, queryDTO.getCategoryCode());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(GovGlossaryCategory::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getParentId() != null) {
            wrapper.eq(GovGlossaryCategory::getParentId, queryDTO.getParentId());
        }
        if (Boolean.TRUE.equals(queryDTO.getTopLevelOnly())) {
            wrapper.and(w -> w.eq(GovGlossaryCategory::getParentId, 0L).or().isNull(GovGlossaryCategory::getParentId));
        }

        return wrapper;
    }

    private void validateDto(GlossaryCategorySaveDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getCategoryName())) {
            throw new BusinessException(2001, "分类名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCategoryCode())) {
            throw new BusinessException(2001, "分类编码不能为空");
        }

        LambdaQueryWrapper<GovGlossaryCategory> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(GovGlossaryCategory::getCategoryCode, dto.getCategoryCode());
        if (excludeId != null) {
            codeWrapper.ne(GovGlossaryCategory::getId, excludeId);
        }
        if (categoryMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException(2001, "分类编码已存在");
        }
    }

    private List<Long> getAllDescendantIds(Long parentId) {
        List<Long> descendants = new ArrayList<>();
        collectDescendants(parentId, descendants);
        return descendants;
    }

    private void collectDescendants(Long parentId, List<Long> descendants) {
        LambdaQueryWrapper<GovGlossaryCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovGlossaryCategory::getParentId, parentId);
        List<GovGlossaryCategory> children = categoryMapper.selectList(wrapper);
        for (GovGlossaryCategory child : children) {
            descendants.add(child.getId());
            collectDescendants(child.getId(), descendants);
        }
    }

    private boolean isDescendant(Long ancestorId, Long potentialDescendantId) {
        Long current = potentialDescendantId;
        while (current != null && current != 0) {
            GovGlossaryCategory parent = categoryMapper.selectById(current);
            if (parent == null) break;
            if (parent.getId().equals(ancestorId)) {
                return true;
            }
            current = parent.getParentId();
        }
        return false;
    }

    private GlossaryCategoryVO buildVO(GovGlossaryCategory category,
                                       Map<Long, Long> termCountMap,
                                       Map<Long, Long> childCountMap,
                                       Map<Long, SysUser> userMap,
                                       Map<Long, String> parentNameMap) {
        GlossaryCategoryVO.GlossaryCategoryVOBuilder builder = GlossaryCategoryVO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .categoryName(category.getCategoryName())
                .categoryCode(category.getCategoryCode())
                .categoryDesc(category.getCategoryDesc())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .statusLabel(category.getStatus() != null && category.getStatus() == 1 ? "启用" : "禁用")
                .termCount(termCountMap.getOrDefault(category.getId(), 0L).intValue())
                .childCount(childCountMap.getOrDefault(category.getId(), 0L).intValue())
                .createUser(category.getCreateUser())
                .createTime(category.getCreateTime())
                .updateUser(category.getUpdateUser())
                .updateTime(category.getUpdateTime());

        if (category.getParentId() != null && category.getParentId() != 0) {
            builder.parentName(parentNameMap.get(category.getParentId()));
        }

        if (category.getCreateUser() != null && userMap.containsKey(category.getCreateUser())) {
            SysUser creator = userMap.get(category.getCreateUser());
            builder.createUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }

        return builder.build();
    }

    private GlossaryCategoryTreeVO buildTreeNode(GovGlossaryCategory category,
                                                  Map<Long, List<GovGlossaryCategory>> childrenMap,
                                                  List<GovGlossaryCategory> all) {
        // 统计术语数量
        LambdaQueryWrapper<GovGlossaryTerm> termWrapper = new LambdaQueryWrapper<>();
        termWrapper.eq(GovGlossaryTerm::getCategoryId, category.getId());
        int termCount = termMapper.selectCount(termWrapper).intValue();

        GlossaryCategoryTreeVO.GlossaryCategoryTreeVOBuilder builder = GlossaryCategoryTreeVO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .categoryName(category.getCategoryName())
                .categoryCode(category.getCategoryCode())
                .categoryDesc(category.getCategoryDesc())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .statusLabel(category.getStatus() != null && category.getStatus() == 1 ? "启用" : "禁用")
                .termCount(termCount)
                .createTime(category.getCreateTime());

        List<GovGlossaryCategory> children = childrenMap.get(category.getId());
        if (children != null && !children.isEmpty()) {
            List<GovGlossaryCategory> sortedChildren = children.stream()
                    .sorted((a, b) -> {
                        if (a.getSortOrder() == null) a.setSortOrder(0);
                        if (b.getSortOrder() == null) b.setSortOrder(0);
                        return a.getSortOrder().compareTo(b.getSortOrder());
                    })
                    .collect(Collectors.toList());

            List<GlossaryCategoryTreeVO> childNodes = sortedChildren.stream()
                    .map(child -> buildTreeNode(child, childrenMap, all))
                    .collect(Collectors.toList());
            builder.children(childNodes);
        } else {
            builder.children(new ArrayList<>());
        }

        return builder.build();
    }

    private GlossaryCategoryTreeVO buildTreeNodeSimple(GovGlossaryCategory category) {
        return GlossaryCategoryTreeVO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .categoryName(category.getCategoryName())
                .categoryCode(category.getCategoryCode())
                .categoryDesc(category.getCategoryDesc())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .statusLabel(category.getStatus() != null && category.getStatus() == 1 ? "启用" : "禁用")
                .termCount(0)
                .children(new ArrayList<>())
                .createTime(category.getCreateTime())
                .build();
    }
}
