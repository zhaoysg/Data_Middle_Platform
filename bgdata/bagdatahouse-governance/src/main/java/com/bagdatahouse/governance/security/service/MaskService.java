package com.bagdatahouse.governance.security.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.*;
import com.bagdatahouse.governance.security.dto.*;
import com.bagdatahouse.governance.security.vo.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 数据安全 — 脱敏任务/策略/白名单服务接口
 */
public interface MaskService {

    // ==================== 脱敏任务管理 ====================

    /**
     * 新增脱敏任务
     */
    Result<SecMaskTask> saveMaskTask(SecMaskTaskSaveDTO dto);

    /**
     * 更新脱敏任务
     */
    Result<Void> updateMaskTask(Long id, SecMaskTaskSaveDTO dto);

    /**
     * 删除脱敏任务
     */
    Result<Void> deleteMaskTask(Long id);

    /**
     * 批量删除脱敏任务
     */
    Result<Void> batchDeleteMaskTask(List<Long> ids);

    /**
     * 根据ID查询脱敏任务
     */
    Result<SecMaskTask> getMaskTaskById(Long id);

    /**
     * 分页查询脱敏任务
     */
    Result<Page<SecMaskTaskVO>> pageMaskTask(Integer pageNum, Integer pageSize, Long dsId, String status, String taskType);

    /**
     * 启用脱敏任务
     */
    Result<Void> enableMaskTask(Long id);

    /**
     * 禁用脱敏任务
     */
    Result<Void> disableMaskTask(Long id);

    /**
     * 执行脱敏任务（静态脱敏核心逻辑）
     */
    Result<SecMaskExecutionLogVO> executeMaskTask(Long id);

    /**
     * 查询脱敏执行日志
     */
    Result<Page<SecMaskExecutionLogVO>> pageMaskExecutionLog(Integer pageNum, Integer pageSize, Long taskId);

    // ==================== 脱敏策略管理 ====================

    /**
     * 新增脱敏策略
     */
    Result<SecMaskStrategy> saveMaskStrategy(SecMaskStrategySaveDTO dto);

    /**
     * 更新脱敏策略
     */
    Result<Void> updateMaskStrategy(Long id, SecMaskStrategySaveDTO dto);

    /**
     * 删除脱敏策略
     */
    Result<Void> deleteMaskStrategy(Long id);

    /**
     * 根据ID查询脱敏策略
     */
    Result<SecMaskStrategy> getMaskStrategyById(Long id);

    /**
     * 分页查询脱敏策略
     */
    Result<Page<SecMaskStrategyVO>> pageMaskStrategy(Integer pageNum, Integer pageSize, String sceneType, String status);

    /**
     * 获取脱敏策略列表（不分页，按 sceneType 分组）
     */
    Result<List<SecMaskStrategyVO>> listMaskStrategyByScene(String sceneType);

    /**
     * 启用脱敏策略
     */
    Result<Void> enableMaskStrategy(Long id);

    /**
     * 禁用脱敏策略
     */
    Result<Void> disableMaskStrategy(Long id);

    /**
     * 检测脱敏策略冲突（同一字段在多个策略中有不同规则）
     */
    Result<List<String>> detectStrategyConflicts();

    // ==================== 脱敏白名单管理 ====================

    /**
     * 新增脱敏白名单
     */
    Result<SecMaskWhitelist> saveMaskWhitelist(SecMaskWhitelistSaveDTO dto);

    /**
     * 更新脱敏白名单
     */
    Result<Void> updateMaskWhitelist(Long id, SecMaskWhitelistSaveDTO dto);

    /**
     * 删除脱敏白名单
     */
    Result<Void> deleteMaskWhitelist(Long id);

    /**
     * 根据ID查询脱敏白名单
     */
    Result<SecMaskWhitelist> getMaskWhitelistById(Long id);

    /**
     * 分页查询脱敏白名单
     */
    Result<Page<SecMaskWhitelistVO>> pageMaskWhitelist(Integer pageNum, Integer pageSize, Long strategyId, String entityType, String status);

    /**
     * 审批脱敏白名单
     */
    Result<Void> approveMaskWhitelist(SecMaskWhitelistApproveDTO dto);

    /**
     * 撤销脱敏白名单
     */
    Result<Void> revokeMaskWhitelist(Long id);

    /**
     * 检查用户/角色是否在白名单中（在有效期内）
     *
     * @param strategyId 策略ID
     * @param entityType USER 或 ROLE
     * @param entityId   用户ID或角色ID
     * @return true 表示命中白名单，不脱敏
     */
    Result<Boolean> isInWhitelist(Long strategyId, String entityType, Long entityId);

    // ==================== 动态脱敏 ====================

    /**
     * 动态脱敏（根据策略对单条数据进行脱敏）
     *
     * @param value        原始值
     * @param sceneType    场景类型
     * @param level        敏感等级
     * @param entityType   当前用户类型（USER/ROLE）
     * @param entityId     当前用户ID或角色ID
     * @return 脱敏后的值
     */
    Result<String> dynamicMask(String value, String sceneType, String level, String entityType, Long entityId);
}
