package com.bagdatahouse.server.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SysUserDTO;
import com.bagdatahouse.core.entity.SysUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 系统用户服务接口
 */
public interface SysUserService {

    /**
     * 分页查询用户
     */
    Result<Page<SysUser>> page(Integer pageNum, Integer pageSize, String username, String nickname, Integer status, Long deptId);

    /**
     * 根据ID查询用户
     */
    Result<SysUser> getById(Long id);

    /**
     * 新增用户
     */
    Result<Long> create(SysUserDTO dto);

    /**
     * 更新用户
     */
    Result<Void> update(Long id, SysUserDTO dto);

    /**
     * 删除用户
     */
    Result<Void> delete(Long id);

    /**
     * 修改用户状态
     */
    Result<Void> updateStatus(Long id, Integer status);

    /**
     * 重置密码
     */
    Result<Void> resetPassword(Long id, String newPassword);

    /**
     * 获取用户选择器数据
     */
    Result<Object> listOptions();
}
