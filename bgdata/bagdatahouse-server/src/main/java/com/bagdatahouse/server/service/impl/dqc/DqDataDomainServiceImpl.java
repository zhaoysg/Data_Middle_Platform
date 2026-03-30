package com.bagdatahouse.server.service.impl.dqc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqDataDomain;
import com.bagdatahouse.core.mapper.DqDataDomainMapper;
import com.bagdatahouse.server.context.LoginContext;
import com.bagdatahouse.server.service.dqc.DqDataDomainService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据域服务实现
 */
@Service
public class DqDataDomainServiceImpl implements DqDataDomainService {

    private final DqDataDomainMapper dataDomainMapper;

    public DqDataDomainServiceImpl(DqDataDomainMapper dataDomainMapper) {
        this.dataDomainMapper = dataDomainMapper;
    }

    @Override
    public Result<Page<DqDataDomain>> page(Integer pageNum, Integer pageSize, String domainName, String domainCode, Integer status) {
        Page<DqDataDomain> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DqDataDomain> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(domainName)) {
            wrapper.like(DqDataDomain::getDomainName, domainName);
        }
        if (StringUtils.isNotBlank(domainCode)) {
            wrapper.eq(DqDataDomain::getDomainCode, domainCode);
        }
        if (status != null) {
            wrapper.eq(DqDataDomain::getStatus, status);
        }
        wrapper.orderByDesc(DqDataDomain::getSortOrder, DqDataDomain::getCreateTime);
        Page<DqDataDomain> result = dataDomainMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<List<DqDataDomain>> listEnabled() {
        LambdaQueryWrapper<DqDataDomain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqDataDomain::getStatus, 1);
        wrapper.orderByAsc(DqDataDomain::getSortOrder);
        List<DqDataDomain> domains = dataDomainMapper.selectList(wrapper);
        return Result.success(domains);
    }

    @Override
    public Result<List<DqDataDomain>> getTree() {
        LambdaQueryWrapper<DqDataDomain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqDataDomain::getStatus, 1);
        wrapper.orderByAsc(DqDataDomain::getSortOrder);
        List<DqDataDomain> domains = dataDomainMapper.selectList(wrapper);
        return Result.success(domains);
    }

    @Override
    public Result<DqDataDomain> getById(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "数据域ID不能为空");
        }
        DqDataDomain domain = dataDomainMapper.selectById(id);
        if (domain == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "数据域不存在");
        }
        return Result.success(domain);
    }

    @Override
    @Transactional
    public Result<Long> create(DqDataDomain dataDomain) {
        String domainName = dataDomain.getDomainName();
        if (StringUtils.isBlank(domainName)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "数据域名称不能为空");
        }
        String domainCode = dataDomain.getDomainCode();
        if (StringUtils.isNotBlank(domainCode)) {
            LambdaQueryWrapper<DqDataDomain> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(DqDataDomain::getDomainCode, domainCode);
            if (dataDomainMapper.selectCount(codeWrapper) > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "数据域编码已存在");
            }
        }
        if (dataDomain.getSortOrder() == null) {
            dataDomain.setSortOrder(0);
        }
        if (dataDomain.getStatus() == null) {
            dataDomain.setStatus(1);
        }
        dataDomain.setCreateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        dataDomain.setCreateUser(currentUserId != null ? currentUserId : 1L);
        dataDomainMapper.insert(dataDomain);
        return Result.success(dataDomain.getId());
    }

    @Override
    @Transactional
    public Result<Void> update(Long id, DqDataDomain dataDomain) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "数据域ID不能为空");
        }
        DqDataDomain existDomain = dataDomainMapper.selectById(id);
        if (existDomain == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "数据域不存在");
        }
        if (StringUtils.isNotBlank(dataDomain.getDomainName()) && !dataDomain.getDomainName().equals(existDomain.getDomainName())) {
            existDomain.setDomainName(dataDomain.getDomainName());
        }
        if (dataDomain.getDomainCode() != null && !dataDomain.getDomainCode().equals(existDomain.getDomainCode())) {
            LambdaQueryWrapper<DqDataDomain> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(DqDataDomain::getDomainCode, dataDomain.getDomainCode());
            codeWrapper.ne(DqDataDomain::getId, id);
            if (dataDomainMapper.selectCount(codeWrapper) > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "数据域编码已存在");
            }
            existDomain.setDomainCode(dataDomain.getDomainCode());
        }
        if (dataDomain.getDomainDesc() != null) {
            existDomain.setDomainDesc(dataDomain.getDomainDesc());
        }
        if (dataDomain.getDeptId() != null) {
            existDomain.setDeptId(dataDomain.getDeptId());
        }
        if (dataDomain.getStatus() != null) {
            existDomain.setStatus(dataDomain.getStatus());
        }
        if (dataDomain.getSortOrder() != null) {
            existDomain.setSortOrder(dataDomain.getSortOrder());
        }
        existDomain.setUpdateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        existDomain.setUpdateUser(currentUserId != null ? currentUserId : 1L);
        dataDomainMapper.updateById(existDomain);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> delete(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "数据域ID不能为空");
        }
        DqDataDomain domain = dataDomainMapper.selectById(id);
        if (domain == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "数据域不存在");
        }
        dataDomainMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> updateStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "数据域ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "状态值无效");
        }
        DqDataDomain domain = dataDomainMapper.selectById(id);
        if (domain == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "数据域不存在");
        }
        domain.setStatus(status);
        domain.setUpdateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        domain.setUpdateUser(currentUserId != null ? currentUserId : 1L);
        dataDomainMapper.updateById(domain);
        return Result.success();
    }
}
