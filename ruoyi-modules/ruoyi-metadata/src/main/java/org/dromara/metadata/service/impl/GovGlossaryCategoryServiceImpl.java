package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.GovGlossaryCategory;
import org.dromara.metadata.domain.bo.GovGlossaryCategoryBo;
import org.dromara.metadata.domain.vo.GovGlossaryCategoryVo;
import org.dromara.metadata.mapper.GovGlossaryCategoryMapper;
import org.dromara.metadata.service.IGovGlossaryCategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 治理 Glossary 分类服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GovGlossaryCategoryServiceImpl implements IGovGlossaryCategoryService {

    private final GovGlossaryCategoryMapper baseMapper;

    @Override
    public TableDataInfo<GovGlossaryCategoryVo> queryPageList(GovGlossaryCategoryBo bo, PageQuery pageQuery) {
        Wrapper<GovGlossaryCategory> wrapper = buildQueryWrapper(bo);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<GovGlossaryCategoryVo> queryOptions() {
        return baseMapper.selectVoList(
            Wrappers.<GovGlossaryCategory>lambdaQuery()
                .eq(GovGlossaryCategory::getStatus, "0")
                .orderByAsc(GovGlossaryCategory::getSortOrder)
        );
    }

    @Override
    public GovGlossaryCategoryVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public Long insertByBo(GovGlossaryCategoryBo bo) {
        GovGlossaryCategory entity = MapstructUtils.convert(bo, GovGlossaryCategory.class);
        if (entity.getParentId() == null) {
            entity.setParentId(0L);
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus("0");
        }
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateByBo(GovGlossaryCategoryBo bo) {
        GovGlossaryCategory entity = MapstructUtils.convert(bo, GovGlossaryCategory.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public TableDataInfo<GovGlossaryCategoryVo> pageCategoryList(GovGlossaryCategoryBo bo, PageQuery pageQuery) {
        return queryPageList(bo, pageQuery);
    }

    @Override
    public GovGlossaryCategoryVo getCategoryById(Long id) {
        return queryById(id);
    }

    @Override
    public Long insertCategory(GovGlossaryCategoryBo bo) {
        return insertByBo(bo);
    }

    @Override
    public int updateCategory(GovGlossaryCategoryBo bo) {
        return updateByBo(bo);
    }

    @Override
    public int deleteCategory(Long[] ids) {
        return deleteByIds(List.of(ids));
    }

    @Override
    public List<GovGlossaryCategoryVo> listTree() {
        List<GovGlossaryCategoryVo> allList = baseMapper.selectVoList(
            Wrappers.<GovGlossaryCategory>lambdaQuery()
                .orderByAsc(GovGlossaryCategory::getSortOrder)
        );
        return buildTree(allList);
    }

    @Override
    public List<GovGlossaryCategoryVo> options() {
        return queryOptions();
    }

    @Override
    public List<GovGlossaryCategoryVo> listCategory(GovGlossaryCategoryBo bo) {
        Wrapper<GovGlossaryCategory> wrapper = buildQueryWrapper(bo);
        return baseMapper.selectVoList(wrapper);
    }

    /**
     * 构建树形结构
     */
    public List<GovGlossaryCategoryVo> buildTree(List<GovGlossaryCategoryVo> list) {
        List<GovGlossaryCategoryVo> result = new ArrayList<>();
        var groupByParent = list.stream()
            .collect(Collectors.groupingBy(GovGlossaryCategoryVo::getParentId));
        result.addAll(buildChildren(list, 0L, groupByParent));
        return result;
    }

    private List<GovGlossaryCategoryVo> buildChildren(
            List<GovGlossaryCategoryVo> allList,
            Long parentId,
            java.util.Map<Long, List<GovGlossaryCategoryVo>> groupByParent) {
        List<GovGlossaryCategoryVo> children = groupByParent.getOrDefault(parentId, List.of());
        for (GovGlossaryCategoryVo category : children) {
            List<GovGlossaryCategoryVo> childrens = buildChildren(allList, category.getId(), groupByParent);
            category.setChildren(childrens);
        }
        return children;
    }

    private Wrapper<GovGlossaryCategory> buildQueryWrapper(GovGlossaryCategoryBo bo) {
        LambdaQueryWrapper<GovGlossaryCategory> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(bo.getCategoryName()), GovGlossaryCategory::getCategoryName, bo.getCategoryName())
            .like(StringUtils.isNotBlank(bo.getCategoryCode()), GovGlossaryCategory::getCategoryCode, bo.getCategoryCode())
            .eq(ObjectUtil.isNotNull(bo.getParentId()), GovGlossaryCategory::getParentId, bo.getParentId())
            .eq(StringUtils.isNotBlank(bo.getStatus()), GovGlossaryCategory::getStatus, bo.getStatus())
            .orderByAsc(GovGlossaryCategory::getSortOrder);
        return wrapper;
    }
}
