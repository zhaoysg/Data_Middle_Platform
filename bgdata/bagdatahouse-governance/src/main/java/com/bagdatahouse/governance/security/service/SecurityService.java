package com.bagdatahouse.governance.security.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.*;
import com.bagdatahouse.governance.security.dto.*;
import com.bagdatahouse.governance.security.vo.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 数据安全分类分级服务接口
 */
public interface SecurityService {

    // ==================== 数据分类管理 ====================

    /**
     * 新增数据分类
     */
    Result<SecClassification> saveClassification(SecClassificationSaveDTO dto);

    /**
     * 更新数据分类
     */
    Result<Void> updateClassification(Long id, SecClassificationSaveDTO dto);

    /**
     * 删除数据分类
     */
    Result<Void> deleteClassification(Long id);

    /**
     * 批量删除数据分类
     */
    Result<Void> batchDeleteClassification(List<Long> ids);

    /**
     * 根据ID查询数据分类
     */
    Result<SecClassification> getClassificationById(Long id);

    /**
     * 分页查询数据分类
     */
    Result<Page<SecClassificationVO>> pageClassification(Integer pageNum, Integer pageSize, SecClassificationQueryDTO queryDTO);

    /**
     * 获取数据分类详情
     */
    Result<SecClassificationDetailVO> getClassificationDetail(Long id);

    /**
     * 启用数据分类
     */
    Result<Void> enableClassification(Long id);

    /**
     * 禁用数据分类
     */
    Result<Void> disableClassification(Long id);

    /**
     * 获取数据分类列表（不分页）
     */
    Result<List<SecClassificationVO>> listClassification(SecClassificationQueryDTO queryDTO);

    // ==================== 数据分级管理 ====================

    /**
     * 新增数据分级
     */
    Result<SecLevel> saveLevel(SecLevelSaveDTO dto);

    /**
     * 更新数据分级
     */
    Result<Void> updateLevel(Long id, SecLevelSaveDTO dto);

    /**
     * 删除数据分级
     */
    Result<Void> deleteLevel(Long id);

    /**
     * 批量删除数据分级
     */
    Result<Void> batchDeleteLevel(List<Long> ids);

    /**
     * 根据ID查询数据分级
     */
    Result<SecLevel> getLevelById(Long id);

    /**
     * 分页查询数据分级
     */
    Result<Page<SecLevelVO>> pageLevel(Integer pageNum, Integer pageSize);

    /**
     * 启用数据分级
     */
    Result<Void> enableLevel(Long id);

    /**
     * 禁用数据分级
     */
    Result<Void> disableLevel(Long id);

    /**
     * 获取数据分级列表（不分页）
     */
    Result<List<SecLevelVO>> listLevel();

    // ==================== 敏感字段识别规则管理 ====================

    /**
     * 新增敏感字段识别规则
     */
    Result<SecSensitivityRule> saveSensitivityRule(SecSensitivityRuleSaveDTO dto);

    /**
     * 更新敏感字段识别规则
     */
    Result<Void> updateSensitivityRule(Long id, SecSensitivityRuleSaveDTO dto);

    /**
     * 删除敏感字段识别规则
     */
    Result<Void> deleteSensitivityRule(Long id);

    /**
     * 批量删除敏感字段识别规则
     */
    Result<Void> batchDeleteSensitivityRule(List<Long> ids);

    /**
     * 根据ID查询敏感字段识别规则
     */
    Result<SecSensitivityRule> getSensitivityRuleById(Long id);

    /**
     * 分页查询敏感字段识别规则
     */
    Result<Page<SecSensitivityRuleVO>> pageSensitivityRule(Integer pageNum, Integer pageSize, SecSensitivityRuleQueryDTO queryDTO);

    /**
     * 启用敏感字段识别规则
     */
    Result<Void> enableSensitivityRule(Long id);

    /**
     * 禁用敏感字段识别规则
     */
    Result<Void> disableSensitivityRule(Long id);

    /**
     * 获取敏感字段识别规则列表（不分页）
     */
    Result<List<SecSensitivityRuleVO>> listSensitivityRule(SecSensitivityRuleQueryDTO queryDTO);

    // ==================== 字段敏感等级管理 ====================

    /**
     * 保存/更新字段敏感等级
     */
    Result<Void> saveColumnSensitivity(SecColumnSensitivitySaveDTO dto);

    /**
     * 批量保存字段敏感等级
     */
    Result<Void> batchSaveColumnSensitivity(List<SecColumnSensitivitySaveDTO> dtos);

    /**
     * 删除字段敏感等级记录
     */
    Result<Void> deleteColumnSensitivity(Long id);

    /**
     * 批量删除字段敏感等级记录
     */
    Result<Void> batchDeleteColumnSensitivity(List<Long> ids);

    /**
     * 分页查询字段敏感等级
     */
    Result<Page<SecColumnSensitivityVO>> pageColumnSensitivity(Integer pageNum, Integer pageSize, SecColumnSensitivityQueryDTO queryDTO);

    /**
     * 审核字段敏感等级
     */
    Result<Void> reviewColumnSensitivity(Long id, String reviewStatus, String reviewComment, Long approvedBy);

    /**
     * 批量审核字段敏感等级
     */
    Result<Void> batchReviewColumnSensitivity(List<Long> ids, String reviewStatus, String reviewComment, Long approvedBy);

    /**
     * 根据数据源和表名获取字段敏感等级
     */
    Result<List<SecColumnSensitivityVO>> getColumnSensitivityByTable(Long dsId, String tableName);

    // ==================== 敏感字段扫描 ====================

    /**
     * 执行敏感字段扫描
     */
    Result<SecScanResultVO> scanSensitiveFields(SecSensitivityScanDTO dto);

    // ==================== 脱敏模板管理 ====================

    /**
     * 新增脱敏模板
     */
    Result<SecMaskTemplate> saveMaskTemplate(SecMaskTemplateSaveDTO dto);

    /**
     * 更新脱敏模板
     */
    Result<Void> updateMaskTemplate(Long id, SecMaskTemplateSaveDTO dto);

    /**
     * 删除脱敏模板
     */
    Result<Void> deleteMaskTemplate(Long id);

    /**
     * 批量删除脱敏模板
     */
    Result<Void> batchDeleteMaskTemplate(List<Long> ids);

    /**
     * 根据ID查询脱敏模板
     */
    Result<SecMaskTemplate> getMaskTemplateById(Long id);

    /**
     * 分页查询脱敏模板
     */
    Result<Page<SecMaskTemplateVO>> pageMaskTemplate(Integer pageNum, Integer pageSize, Integer enabled);

    /**
     * 获取脱敏模板列表（不分页）
     */
    Result<List<SecMaskTemplateVO>> listMaskTemplate(Integer enabled);

    /**
     * 启用脱敏模板
     */
    Result<Void> enableMaskTemplate(Long id);

    /**
     * 禁用脱敏模板
     */
    Result<Void> disableMaskTemplate(Long id);

    /**
     * 根据ID查询敏感字段记录
     */
    Result<SecColumnSensitivityVO> getSensitivityById(Long id);

    /**
     * 根据批次号获取敏感字段记录列表
     */
    Result<List<SecColumnSensitivityVO>> getByBatchNo(String batchNo);

    // ==================== 统计 ====================

    /**
     * 获取数据安全分级统计信息
     */
    Result<SecStatsVO> getStats();

    /**
     * 获取数据源敏感字段统计
     */
    Result<List<Map<String, Object>>> getSensitivityStatsByDs();

    /**
     * 敏感字段审核状态汇总（总数/待审/通过/驳回），单条聚合 SQL
     */
    Result<Map<String, Long>> getSensitivityReviewCounts();

    /**
     * 获取枚举选项
     */
    Result<Map<String, Object>> getEnums();

    // ==================== 敏感字段访问审批 ====================

    /**
     * 提交敏感字段访问申请
     */
    Result<SecAccessApplication> submitAccessApplication(SecAccessApplicationDTO dto);

    /**
     * 取消敏感字段访问申请
     */
    Result<Void> cancelAccessApplication(Long id, Long operatorId, String operatorName);

    /**
     * 审批敏感字段访问申请
     */
    Result<Void> approveAccessApplication(SecAccessApprovalDTO dto);

    /**
     * 分页查询访问申请
     */
    Result<Page<SecAccessApplicationVO>> pageAccessApplication(Integer pageNum, Integer pageSize, SecAccessApplicationQueryDTO queryDTO);

    /**
     * 获取访问申请详情
     */
    Result<SecAccessApplicationVO> getAccessApplicationById(Long id);

    /**
     * 获取待审批列表
     */
    Result<Page<SecAccessApplicationVO>> pagePendingApproval(Integer pageNum, Integer pageSize, Long approverId);

    /**
     * 查询访问申请审批历史
     */
    Result<List<SecAccessAuditVO>> listAccessAuditHistory(Long applicationId);

    /**
     * 查询访问审计日志
     */
    Result<Page<SecAccessLogVO>> pageAccessLog(Integer pageNum, Integer pageSize, Long applicationId, String operationType, Long operatorId);

    /**
     * 获取访问申请统计
     */
    Result<Map<String, Object>> getAccessStats();

    // ==================== 定时扫描任务管理 ====================

    /**
     * 新建定时扫描任务
     */
    Result<com.bagdatahouse.core.entity.SecScanSchedule> saveScanSchedule(SecScanScheduleSaveDTO dto);

    /**
     * 更新定时扫描任务
     */
    Result<Void> updateScanSchedule(Long id, SecScanScheduleSaveDTO dto);

    /**
     * 删除定时扫描任务
     */
    Result<Void> deleteScanSchedule(Long id);

    /**
     * 分页查询定时扫描任务
     */
    Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.bagdatahouse.core.entity.SecScanSchedule>> pageScanSchedule(
            Integer pageNum, Integer pageSize, Long dsId, Integer enabled);

    /**
     * 根据ID获取定时扫描任务详情
     */
    Result<com.bagdatahouse.core.entity.SecScanSchedule> getScanScheduleById(Long id);

    /**
     * 手动触发定时扫描任务（立即执行）
     */
    Result<Void> triggerScanSchedule(Long id);

    /**
     * 根据批次号获取扫描历史详情
     */
    Result<List<com.bagdatahouse.governance.security.vo.SecColumnSensitivityVO>> getScanHistoryByBatchNo(String batchNo);

    // ==================== 多级审批人联动 ====================

    /**
     * 根据敏感等级和字段信息，解析推荐审批人
     * <p>
     * 联动规则：
     * - L4 机密：强制指定审批人为 DAYU_ADMIN（系统管理员）
     * - L3 敏感：自动取 gov_metadata.owner_id（数据 Owner）作为默认审批人
     * - L2/L1：可自行确认或由组长审批
     *
     * @param dsId      数据源ID
     * @param tableName 表名
     * @param columnName 字段名（可选）
     * @param levelId   分级ID
     * @return 推荐审批人用户ID，null 表示无推荐
     */
    Result<Long> resolveRecommendedApprover(Long dsId, String tableName, String columnName, Long levelId);

    // ==================== 分类等级推荐绑定（T5） ====================

    /**
     * 新增/更新分类等级绑定
     * 先删除该分类所有旧绑定，再批量插入新绑定
     */
    Result<Void> saveClassLevelBinding(SecClassLevelBindingSaveDTO dto);

    /**
     * 查询某个分类的所有等级绑定
     */
    Result<List<SecClassLevelBindingVO>> listClassLevelBindings(Long classId);

    /**
     * 查询某个分类的推荐等级（isRecommended=1）
     */
    Result<List<SecLevelVO>> listRecommendedLevels(Long classId);

    /**
     * 删除指定绑定记录
     */
    Result<Void> deleteClassLevelBinding(Long id);

    // ==================== 安全总览与健康分 ====================

    /**
     * 获取数据安全总览统计数据
     */
    Result<Map<String, Object>> getSecurityOverview();

    /**
     * 获取数据安全总览趋势数据
     * @param days 统计天数，默认30天
     */
    Result<Map<String, Object>> getSecurityOverviewTrend(Integer days);

    /**
     * 获取数据安全健康分
     */
    Result<Map<String, Object>> getHealthScore();

    /**
     * 分页查询扫描历史
     */
    Result<Page<Map<String, Object>>> pageScanHistory(Integer pageNum, Integer pageSize, String batchNo, Long dsId, String status);

    // ==================== 新字段发现（T6） ====================

    /**
     * 分页查询新字段告警列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param status 状态筛选：PENDING/SCANNED/DISMISSED
     * @param dsId 数据源ID筛选
     * @param tableName 表名筛选
     * @return 分页告警列表
     */
    Result<Page<SecNewColumnAlert>> pageNewColumnAlerts(Integer pageNum, Integer pageSize, String status, Long dsId, String tableName);

    /**
     * 获取新字段告警统计数量（待处理数）
     * @return 待处理新字段告警数量
     */
    Result<Long> countPendingNewColumnAlerts();

    /**
     * 一键扫描指定新字段告警（触发敏感字段扫描）
     * <p>
     * 根据告警记录中的 dsId/tableName/columnName 触发敏感字段扫描，
     * 扫描完成后将告警状态更新为 SCANNED
     *
     * @param alertId 告警ID
     * @param operatorId 操作人ID
     * @return 扫描结果
     */
    Result<SecScanResultVO> scanNewColumnAlert(Long alertId, Long operatorId);

    /**
     * 忽略指定新字段告警
     * @param alertId 告警ID
     * @param operatorId 操作人ID
     * @param comment 处理意见
     */
    Result<Void> dismissNewColumnAlert(Long alertId, Long operatorId, String comment);
}
