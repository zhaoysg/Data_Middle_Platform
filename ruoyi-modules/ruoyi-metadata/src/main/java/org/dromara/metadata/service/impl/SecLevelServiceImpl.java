package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.SecLevel;
import org.dromara.metadata.domain.vo.SecLevelVo;
import org.dromara.metadata.mapper.SecLevelMapper;
import org.dromara.metadata.service.ISecLevelService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 敏感等级服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class SecLevelServiceImpl implements ISecLevelService {

    private final SecLevelMapper baseMapper;

    @Override
    public TableDataInfo<SecLevelVo> queryPageList(SecLevelVo vo, PageQuery pageQuery) {
        var wrapper = Wrappers.<SecLevel>lambdaQuery()
            .like(StringUtils.isNotBlank(vo.getLevelName()), SecLevel::getLevelName, vo.getLevelName())
            .like(StringUtils.isNotBlank(vo.getLevelCode()), SecLevel::getLevelCode, vo.getLevelCode())
            .eq(vo.getLevelValue() != null, SecLevel::getLevelValue, vo.getLevelValue())
            .eq(StringUtils.isNotBlank(vo.getEnabled()), SecLevel::getEnabled, vo.getEnabled())
            .orderByAsc(SecLevel::getSortOrder);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SecLevelVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<SecLevelVo> listAll() {
        return baseMapper.selectVoList(
            Wrappers.<SecLevel>lambdaQuery()
                .eq(SecLevel::getEnabled, "1")
                .orderByAsc(SecLevel::getSortOrder)
        );
    }

    @Override
    public Long insert(SecLevelVo vo) {
        if (StringUtils.isNotBlank(vo.getLevelCode())) {
            boolean exist = baseMapper.exists(Wrappers.<SecLevel>lambdaQuery()
                .eq(SecLevel::getLevelCode, vo.getLevelCode()));
            if (exist) {
                throw new ServiceException("等级编码已存在");
            }
        }
        SecLevel entity = new SecLevel();
        entity.setLevelCode(vo.getLevelCode());
        entity.setLevelName(vo.getLevelName());
        entity.setLevelValue(vo.getLevelValue());
        entity.setLevelDesc(vo.getLevelDesc());
        entity.setColor(vo.getColor());
        entity.setSortOrder(vo.getSortOrder() != null ? vo.getSortOrder() : 0);
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : "1");
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int update(SecLevelVo vo) {
        if (vo.getId() == null) {
            throw new ServiceException("等级ID不能为空");
        }
        if (StringUtils.isNotBlank(vo.getLevelCode())) {
            boolean exist = baseMapper.exists(Wrappers.<SecLevel>lambdaQuery()
                .eq(SecLevel::getLevelCode, vo.getLevelCode())
                .ne(SecLevel::getId, vo.getId()));
            if (exist) {
                throw new ServiceException("等级编码已存在");
            }
        }
        SecLevel entity = new SecLevel();
        entity.setId(vo.getId());
        entity.setLevelCode(vo.getLevelCode());
        entity.setLevelName(vo.getLevelName());
        entity.setLevelValue(vo.getLevelValue());
        entity.setLevelDesc(vo.getLevelDesc());
        entity.setColor(vo.getColor());
        entity.setSortOrder(vo.getSortOrder());
        entity.setEnabled(vo.getEnabled());
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }
}
