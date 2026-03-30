package com.bagdatahouse.server.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.SysDept;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 系统部门服务接口
 */
public interface SysDeptService {

    /**
     * 获取部门树
     */
    Result<List<SysDept>> getTree();

    /**
     * 获取部门选项列表（用于下拉选择）
     */
    Result<List<Object>> listOptions();

    /**
     * 分页查询部门
     */
    Result<Page<SysDept>> page(Integer pageNum, Integer pageSize, String deptName, String deptCode, Integer status);

    /**
     * 根据ID查询部门
     */
    Result<SysDept> getById(Long id);

    /**
     * 新增部门
     */
    Result<Long> create(SysDept dept);

    /**
     * 更新部门
     */
    Result<Void> update(Long id, SysDept dept);

    /**
     * 删除部门
     */
    Result<Void> delete(Long id);

    /**
     * 修改部门状态
     */
    Result<Void> updateStatus(Long id, Integer status);
}
