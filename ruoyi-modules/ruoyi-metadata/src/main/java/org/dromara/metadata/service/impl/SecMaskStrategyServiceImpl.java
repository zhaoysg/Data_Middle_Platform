package org.dromara.metadata.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.SecMaskStrategy;
import org.dromara.metadata.domain.SecMaskStrategyDetail;
import org.dromara.metadata.domain.bo.SecMaskStrategyBo;
import org.dromara.metadata.domain.bo.SecMaskStrategyDetailBo;
import org.dromara.metadata.domain.vo.SecMaskStrategyDetailVo;
import org.dromara.metadata.domain.vo.SecMaskStrategyVo;
import org.dromara.metadata.mapper.SecMaskStrategyDetailMapper;
import org.dromara.metadata.mapper.SecMaskStrategyMapper;
import org.dromara.metadata.service.ISecMaskStrategyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 脱敏策略服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SecMaskStrategyServiceImpl implements ISecMaskStrategyService {

    private final SecMaskStrategyMapper strategyMapper;
    private final SecMaskStrategyDetailMapper detailMapper;

    @Override
    public TableDataInfo<SecMaskStrategyVo> queryPageList(SecMaskStrategyVo vo, PageQuery pageQuery) {
        var wrapper = Wrappers.<SecMaskStrategy>lambdaQuery()
            .like(StringUtils.isNotBlank(vo.getStrategyName()), SecMaskStrategy::getStrategyName, vo.getStrategyName())
            .like(StringUtils.isNotBlank(vo.getStrategyCode()), SecMaskStrategy::getStrategyCode, vo.getStrategyCode())
            .eq(vo.getDsId() != null, SecMaskStrategy::getDsId, vo.getDsId())
            .eq(StringUtils.isNotBlank(vo.getEnabled()), SecMaskStrategy::getEnabled, vo.getEnabled())
            .orderByDesc(SecMaskStrategy::getCreateTime);
        var page = strategyMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SecMaskStrategyVo queryById(Long id) {
        return strategyMapper.selectVoById(id);
    }

    @Override
    public List<SecMaskStrategyDetailVo> queryDetailsByStrategyId(Long strategyId) {
        List<SecMaskStrategyDetail> details = detailMapper.selectByStrategyId(strategyId);
        List<SecMaskStrategyDetailVo> voList = new ArrayList<>();
        for (SecMaskStrategyDetail d : details) {
            SecMaskStrategyDetailVo vo = new SecMaskStrategyDetailVo();
            vo.setId(d.getId());
            vo.setStrategyId(d.getStrategyId());
            vo.setDsId(d.getDsId());
            vo.setTableName(d.getTableName());
            vo.setColumnName(d.getColumnName());
            vo.setTemplateCode(d.getTemplateCode());
            vo.setOutputAlias(d.getOutputAlias());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public List<SecMaskStrategyVo> listAllEnabled() {
        return strategyMapper.selectVoList(
            Wrappers.<SecMaskStrategy>lambdaQuery()
                .eq(SecMaskStrategy::getEnabled, "1")
                .orderByAsc(SecMaskStrategy::getId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertWithDetails(SecMaskStrategyBo bo) {
        if (StringUtils.isNotBlank(bo.getStrategyCode())) {
            boolean exist = strategyMapper.exists(Wrappers.<SecMaskStrategy>lambdaQuery()
                .eq(SecMaskStrategy::getStrategyCode, bo.getStrategyCode()));
            if (exist) {
                throw new ServiceException("策略编码已存在");
            }
        }

        SecMaskStrategy entity = new SecMaskStrategy();
        entity.setStrategyCode(bo.getStrategyCode());
        entity.setStrategyName(bo.getStrategyName());
        entity.setStrategyDesc(bo.getStrategyDesc());
        entity.setDsId(bo.getDsId());
        entity.setEnabled(bo.getEnabled() != null ? bo.getEnabled() : "1");
        strategyMapper.insert(entity);
        Long strategyId = entity.getId();

        if (CollUtil.isNotEmpty(bo.getDetails())) {
            for (SecMaskStrategyDetailBo detailBo : bo.getDetails()) {
                SecMaskStrategyDetail detail = new SecMaskStrategyDetail();
                detail.setStrategyId(strategyId);
                detail.setDsId(bo.getDsId());
                detail.setTableName(detailBo.getTableName());
                detail.setColumnName(detailBo.getColumnName());
                detail.setTemplateCode(detailBo.getTemplateCode());
                detail.setOutputAlias(detailBo.getOutputAlias());
                detail.setCreateTime(new Date());
                detailMapper.insert(detail);
            }
        }

        return strategyId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWithDetails(SecMaskStrategyBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("策略ID不能为空");
        }

        if (StringUtils.isNotBlank(bo.getStrategyCode())) {
            boolean exist = strategyMapper.exists(Wrappers.<SecMaskStrategy>lambdaQuery()
                .eq(SecMaskStrategy::getStrategyCode, bo.getStrategyCode())
                .ne(SecMaskStrategy::getId, bo.getId()));
            if (exist) {
                throw new ServiceException("策略编码已存在");
            }
        }

        SecMaskStrategy entity = new SecMaskStrategy();
        entity.setId(bo.getId());
        entity.setStrategyCode(bo.getStrategyCode());
        entity.setStrategyName(bo.getStrategyName());
        entity.setStrategyDesc(bo.getStrategyDesc());
        entity.setDsId(bo.getDsId());
        entity.setEnabled(bo.getEnabled());
        int updated = strategyMapper.updateById(entity);

        detailMapper.delete(Wrappers.<SecMaskStrategyDetail>lambdaUpdate()
            .eq(SecMaskStrategyDetail::getStrategyId, bo.getId()));

        if (CollUtil.isNotEmpty(bo.getDetails())) {
            for (SecMaskStrategyDetailBo detailBo : bo.getDetails()) {
                SecMaskStrategyDetail detail = new SecMaskStrategyDetail();
                detail.setStrategyId(bo.getId());
                detail.setDsId(bo.getDsId());
                detail.setTableName(detailBo.getTableName());
                detail.setColumnName(detailBo.getColumnName());
                detail.setTemplateCode(detailBo.getTemplateCode());
                detail.setOutputAlias(detailBo.getOutputAlias());
                detail.setCreateTime(new Date());
                detailMapper.insert(detail);
            }
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids) {
        for (Long id : ids) {
            detailMapper.delete(Wrappers.<SecMaskStrategyDetail>lambdaUpdate()
                .eq(SecMaskStrategyDetail::getStrategyId, id));
        }
        return strategyMapper.deleteBatchIds(List.of(ids));
    }
}
