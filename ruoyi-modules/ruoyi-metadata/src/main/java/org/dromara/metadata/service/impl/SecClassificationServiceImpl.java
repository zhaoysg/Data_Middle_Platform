package org.dromara.metadata.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.SecClassification;
import org.dromara.metadata.domain.vo.SecClassificationVo;
import org.dromara.metadata.mapper.SecClassificationMapper;
import org.dromara.metadata.service.ISecClassificationService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据分类服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SecClassificationServiceImpl implements ISecClassificationService {

    private final SecClassificationMapper baseMapper;

    @Override
    public TableDataInfo<SecClassificationVo> queryPageList(SecClassificationVo vo, PageQuery pageQuery) {
        var wrapper = Wrappers.<SecClassification>lambdaQuery()
            .like(StringUtils.isNotBlank(vo.getClassName()), SecClassification::getClassName, vo.getClassName())
            .like(StringUtils.isNotBlank(vo.getClassCode()), SecClassification::getClassCode, vo.getClassCode())
            .eq(StringUtils.isNotBlank(vo.getEnabled()), SecClassification::getEnabled, vo.getEnabled())
            .orderByAsc(SecClassification::getSortOrder);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SecClassificationVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<SecClassificationVo> listAll() {
        return baseMapper.selectVoList(
            Wrappers.<SecClassification>lambdaQuery()
                .eq(SecClassification::getEnabled, "1")
                .orderByAsc(SecClassification::getSortOrder)
        );
    }

    @Override
    public Long insert(SecClassificationVo vo) {
        if (StringUtils.isNotBlank(vo.getClassCode())) {
            boolean exist = baseMapper.exists(Wrappers.<SecClassification>lambdaQuery()
                .eq(SecClassification::getClassCode, vo.getClassCode()));
            if (exist) {
                throw new ServiceException("分类编码已存在");
            }
        }
        SecClassification entity = new SecClassification();
        entity.setClassCode(vo.getClassCode());
        entity.setClassName(vo.getClassName());
        entity.setClassDesc(vo.getClassDesc());
        entity.setSortOrder(vo.getSortOrder() != null ? vo.getSortOrder() : 0);
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : "1");
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int update(SecClassificationVo vo) {
        if (vo.getId() == null) {
            throw new ServiceException("分类ID不能为空");
        }
        if (StringUtils.isNotBlank(vo.getClassCode())) {
            boolean exist = baseMapper.exists(Wrappers.<SecClassification>lambdaQuery()
                .eq(SecClassification::getClassCode, vo.getClassCode())
                .ne(SecClassification::getId, vo.getId()));
            if (exist) {
                throw new ServiceException("分类编码已存在");
            }
        }
        SecClassification entity = new SecClassification();
        entity.setId(vo.getId());
        entity.setClassCode(vo.getClassCode());
        entity.setClassName(vo.getClassName());
        entity.setClassDesc(vo.getClassDesc());
        entity.setSortOrder(vo.getSortOrder());
        entity.setEnabled(vo.getEnabled());
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }
}
