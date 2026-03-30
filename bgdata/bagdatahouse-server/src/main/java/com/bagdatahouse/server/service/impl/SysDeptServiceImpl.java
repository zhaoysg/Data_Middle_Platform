package com.bagdatahouse.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.SysDept;
import com.bagdatahouse.core.mapper.SysDeptMapper;
import com.bagdatahouse.server.context.LoginContext;
import com.bagdatahouse.server.service.SysDeptService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统部门服务实现
 */
@Service
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper deptMapper;

    public SysDeptServiceImpl(SysDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    @Override
    public Result<List<SysDept>> getTree() {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getStatus, 1);
        wrapper.orderByAsc(SysDept::getSortOrder);
        List<SysDept> allDepts = deptMapper.selectList(wrapper);
        List<SysDept> tree = buildTree(allDepts, 0L);
        return Result.success(tree);
    }

    @Override
    public Result<List<Object>> listOptions() {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getStatus, 1);
        wrapper.orderByAsc(SysDept::getSortOrder);
        List<SysDept> depts = deptMapper.selectList(wrapper);
        List<Object> options = depts.stream().map(dept -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("value", dept.getId());
            item.put("label", dept.getDeptName());
            item.put("parentId", dept.getParentId());
            return item;
        }).collect(Collectors.toList());
        return Result.success(options);
    }

    @Override
    public Result<Page<SysDept>> page(Integer pageNum, Integer pageSize, String deptName, String deptCode, Integer status) {
        Page<SysDept> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(deptName)) {
            wrapper.like(SysDept::getDeptName, deptName);
        }
        if (StringUtils.isNotBlank(deptCode)) {
            wrapper.eq(SysDept::getDeptCode, deptCode);
        }
        if (status != null) {
            wrapper.eq(SysDept::getStatus, status);
        }
        wrapper.orderByAsc(SysDept::getSortOrder, SysDept::getCreateTime);
        Page<SysDept> result = deptMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<SysDept> getById(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "部门ID不能为空");
        }
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "部门不存在");
        }
        return Result.success(dept);
    }

    @Override
    @Transactional
    public Result<Long> create(SysDept dept) {
        String deptName = dept.getDeptName();
        if (StringUtils.isBlank(deptName)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "部门名称不能为空");
        }
        String deptCode = dept.getDeptCode();
        if (StringUtils.isNotBlank(deptCode)) {
            LambdaQueryWrapper<SysDept> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(SysDept::getDeptCode, deptCode);
            if (deptMapper.selectCount(codeWrapper) > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "部门编码已存在");
            }
        }
        LambdaQueryWrapper<SysDept> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(SysDept::getDeptName, deptName);
        if (dept.getParentId() != null && dept.getParentId() > 0) {
            nameWrapper.eq(SysDept::getParentId, dept.getParentId());
        } else {
            nameWrapper.eq(SysDept::getParentId, 0L);
        }
        if (deptMapper.selectCount(nameWrapper) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "同级别下部门名称已存在");
        }

        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        dept.setCreateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        dept.setCreateUser(currentUserId != null ? currentUserId : 1L);
        deptMapper.insert(dept);
        return Result.success(dept.getId());
    }

    @Override
    @Transactional
    public Result<Void> update(Long id, SysDept dept) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "部门ID不能为空");
        }
        SysDept existDept = deptMapper.selectById(id);
        if (existDept == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "部门不存在");
        }
        if (StringUtils.isNotBlank(dept.getDeptName()) && !dept.getDeptName().equals(existDept.getDeptName())) {
            LambdaQueryWrapper<SysDept> nameWrapper = new LambdaQueryWrapper<>();
            nameWrapper.eq(SysDept::getDeptName, dept.getDeptName());
            Long parentId = dept.getParentId() != null ? dept.getParentId() : existDept.getParentId();
            nameWrapper.eq(SysDept::getParentId, parentId);
            nameWrapper.ne(SysDept::getId, id);
            if (deptMapper.selectCount(nameWrapper) > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "同级别下部门名称已存在");
            }
            existDept.setDeptName(dept.getDeptName());
        }
        if (dept.getDeptCode() != null && !dept.getDeptCode().equals(existDept.getDeptCode())) {
            LambdaQueryWrapper<SysDept> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(SysDept::getDeptCode, dept.getDeptCode());
            codeWrapper.ne(SysDept::getId, id);
            if (deptMapper.selectCount(codeWrapper) > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "部门编码已存在");
            }
            existDept.setDeptCode(dept.getDeptCode());
        }
        if (dept.getParentId() != null) {
            if (dept.getParentId().equals(id)) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "不能将自己设为父部门");
            }
            existDept.setParentId(dept.getParentId());
        }
        if (dept.getDeptType() != null) {
            existDept.setDeptType(dept.getDeptType());
        }
        if (dept.getLeaderId() != null) {
            existDept.setLeaderId(dept.getLeaderId());
        }
        if (dept.getPhone() != null) {
            existDept.setPhone(dept.getPhone());
        }
        if (dept.getEmail() != null) {
            existDept.setEmail(dept.getEmail());
        }
        if (dept.getStatus() != null) {
            existDept.setStatus(dept.getStatus());
        }
        if (dept.getSortOrder() != null) {
            existDept.setSortOrder(dept.getSortOrder());
        }
        existDept.setUpdateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        existDept.setUpdateUser(currentUserId != null ? currentUserId : 1L);
        deptMapper.updateById(existDept);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> delete(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "部门ID不能为空");
        }
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "部门不存在");
        }
        LambdaQueryWrapper<SysDept> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(SysDept::getParentId, id);
        if (deptMapper.selectCount(childWrapper) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "该部门存在子部门，无法删除");
        }
        deptMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> updateStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "部门ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "状态值无效");
        }
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "部门不存在");
        }
        dept.setStatus(status);
        dept.setUpdateTime(LocalDateTime.now());
        Long currentUserId = LoginContext.getUserId();
        dept.setUpdateUser(currentUserId != null ? currentUserId : 1L);
        deptMapper.updateById(dept);
        return Result.success();
    }

    private List<SysDept> buildTree(List<SysDept> all, Long parentId) {
        List<SysDept> children = new ArrayList<>();
        for (SysDept dept : all) {
            if (dept.getParentId() != null && dept.getParentId().equals(parentId)) {
                children.add(dept);
            }
        }
        return children;
    }
}
