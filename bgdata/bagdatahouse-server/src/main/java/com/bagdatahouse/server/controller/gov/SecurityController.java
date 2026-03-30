package com.bagdatahouse.server.controller.gov;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.SecAccessApplication;
import com.bagdatahouse.core.entity.SecClassification;
import com.bagdatahouse.core.entity.SecColumnSensitivity;
import com.bagdatahouse.core.entity.SecLevel;
import com.bagdatahouse.core.entity.SecMaskTemplate;
import com.bagdatahouse.core.entity.SecSensitivityRule;
import com.bagdatahouse.governance.security.dto.*;
import com.bagdatahouse.governance.security.service.SecurityService;
import com.bagdatahouse.governance.security.vo.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据安全-分类分级标准管理接口
 */
@Api(tags = "数据安全-分类分级标准管理")
@RestController
@RequestMapping("/gov/security")
public class SecurityController {

    @Autowired
    private SecurityService securityService;

    // ==================== 统计概览 ====================

    @ApiOperation("获取安全分类统计概览")
    @GetMapping("/stats")
    public Result<SecStatsVO> getStats() {
        return securityService.getStats();
    }

    // ==================== 数据分类管理 ====================

    @ApiOperation("分页查询数据分类")
    @GetMapping("/classification/page")
    public Result<Page<SecClassificationVO>> pageClassification(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("分类编码") @RequestParam(required = false) String classCode,
            @ApiParam("分类名称（模糊）") @RequestParam(required = false) String className,
            @ApiParam("是否启用：0-禁用，1-启用") @RequestParam(required = false) Integer enabled) {

        SecClassificationQueryDTO queryDTO = SecClassificationQueryDTO.builder()
                .classCode(classCode)
                .className(className)
                .enabled(enabled)
                .build();
        return securityService.pageClassification(pageNum, pageSize, queryDTO);
    }

    @ApiOperation("获取数据分类列表（不分页）")
    @GetMapping("/classification/list")
    public Result<List<SecClassificationVO>> listClassification(
            @ApiParam("是否启用") @RequestParam(required = false) Integer enabled) {
        SecClassificationQueryDTO queryDTO = SecClassificationQueryDTO.builder().enabled(enabled).build();
        return securityService.listClassification(queryDTO);
    }

    @ApiOperation("新增数据分类")
    @PostMapping("/classification")
    public Result<SecClassification> saveClassification(@Validated @RequestBody SecClassificationSaveDTO dto) {
        return securityService.saveClassification(dto);
    }

    @ApiOperation("更新数据分类")
    @PutMapping("/classification/{id}")
    public Result<Void> updateClassification(@PathVariable Long id, @Validated @RequestBody SecClassificationSaveDTO dto) {
        return securityService.updateClassification(id, dto);
    }

    @ApiOperation("删除数据分类")
    @DeleteMapping("/classification/{id}")
    public Result<Void> deleteClassification(@PathVariable Long id) {
        return securityService.deleteClassification(id);
    }

    @ApiOperation("获取数据分类详情")
    @GetMapping("/classification/{id}")
    public Result<SecClassificationDetailVO> getClassificationDetail(@PathVariable Long id) {
        return securityService.getClassificationDetail(id);
    }

    @ApiOperation("启用数据分类")
    @PostMapping("/classification/{id}/enable")
    public Result<Void> enableClassification(@PathVariable Long id) {
        return securityService.enableClassification(id);
    }

    @ApiOperation("禁用数据分类")
    @PostMapping("/classification/{id}/disable")
    public Result<Void> disableClassification(@PathVariable Long id) {
        return securityService.disableClassification(id);
    }

    // ==================== 敏感等级管理 ====================

    @ApiOperation("分页查询敏感等级")
    @GetMapping("/level/page")
    public Result<Page<SecLevelVO>> pageLevel(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        return securityService.pageLevel(pageNum, pageSize);
    }

    @ApiOperation("获取敏感等级列表（不分页）")
    @GetMapping("/level/list")
    public Result<List<SecLevelVO>> listLevel() {
        return securityService.listLevel();
    }

    @ApiOperation("新增敏感等级")
    @PostMapping("/level")
    public Result<SecLevel> saveLevel(@Validated @RequestBody SecLevelSaveDTO dto) {
        return securityService.saveLevel(dto);
    }

    @ApiOperation("更新敏感等级")
    @PutMapping("/level/{id}")
    public Result<Void> updateLevel(@PathVariable Long id, @Validated @RequestBody SecLevelSaveDTO dto) {
        return securityService.updateLevel(id, dto);
    }

    @ApiOperation("删除敏感等级")
    @DeleteMapping("/level/{id}")
    public Result<Void> deleteLevel(@PathVariable Long id) {
        return securityService.deleteLevel(id);
    }

    // ==================== 敏感字段识别规则 ====================

    @ApiOperation("分页查询敏感字段识别规则")
    @GetMapping("/rule/page")
    public Result<Page<SecSensitivityRuleVO>> pageRule(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("匹配类型") @RequestParam(required = false) String matchType,
            @ApiParam("是否启用") @RequestParam(required = false) Integer enabled) {

        SecSensitivityRuleQueryDTO queryDTO = SecSensitivityRuleQueryDTO.builder()
                .matchType(matchType)
                .enabled(enabled)
                .build();
        return securityService.pageSensitivityRule(pageNum, pageSize, queryDTO);
    }

    @ApiOperation("获取敏感字段识别规则列表（不分页）")
    @GetMapping("/rule/list")
    public Result<List<SecSensitivityRuleVO>> listRule(
            @ApiParam("是否启用") @RequestParam(required = false) Integer enabled) {
        SecSensitivityRuleQueryDTO queryDTO = SecSensitivityRuleQueryDTO.builder().enabled(enabled).build();
        return securityService.listSensitivityRule(queryDTO);
    }

    @ApiOperation("新增敏感字段识别规则")
    @PostMapping("/rule")
    public Result<SecSensitivityRule> saveRule(@Validated @RequestBody SecSensitivityRuleSaveDTO dto) {
        return securityService.saveSensitivityRule(dto);
    }

    @ApiOperation("更新敏感字段识别规则")
    @PutMapping("/rule/{id}")
    public Result<Void> updateRule(@PathVariable Long id, @Validated @RequestBody SecSensitivityRuleSaveDTO dto) {
        return securityService.updateSensitivityRule(id, dto);
    }

    @ApiOperation("删除敏感字段识别规则")
    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        return securityService.deleteSensitivityRule(id);
    }

    @ApiOperation("启用敏感字段识别规则")
    @PostMapping("/rule/{id}/enable")
    public Result<Void> enableRule(@PathVariable Long id) {
        return securityService.enableSensitivityRule(id);
    }

    @ApiOperation("禁用敏感字段识别规则")
    @PostMapping("/rule/{id}/disable")
    public Result<Void> disableRule(@PathVariable Long id) {
        return securityService.disableSensitivityRule(id);
    }

    // ==================== 敏感字段扫描与审核 ====================

    @ApiOperation("执行敏感字段扫描")
    @PostMapping("/scan")
    public Result<SecScanResultVO> scanSensitiveFields(@Validated @RequestBody SecSensitivityScanDTO dto) {
        return securityService.scanSensitiveFields(dto);
    }

    @ApiOperation("分页查询敏感字段记录")
    @GetMapping("/sensitivity/page")
    public Result<Page<SecColumnSensitivityVO>> pageSensitivity(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId,
            @ApiParam("表名（模糊搜索）") @RequestParam(required = false) String tableName,
            @ApiParam("分类ID") @RequestParam(required = false) Long classId,
            @ApiParam("等级ID") @RequestParam(required = false) Long levelId,
            @ApiParam("审核状态") @RequestParam(required = false) String reviewStatus,
            @ApiParam("敏感级别") @RequestParam(required = false) String sensitivityLevel) {

        SecColumnSensitivityQueryDTO queryDTO = SecColumnSensitivityQueryDTO.builder()
                .dsId(dsId)
                .tableName(tableName)
                .classId(classId)
                .levelId(levelId)
                .reviewStatus(reviewStatus)
                .sensitivityLevel(sensitivityLevel)
                .build();
        return securityService.pageColumnSensitivity(pageNum, pageSize, queryDTO);
    }

    @ApiOperation("获取敏感字段记录详情")
    @GetMapping("/sensitivity/{id}")
    public Result<SecColumnSensitivityVO> getSensitivityById(@PathVariable Long id) {
        // Delegate to page query with ID filter
        SecColumnSensitivityQueryDTO queryDTO = SecColumnSensitivityQueryDTO.builder().id(id).build();
        Result<Page<SecColumnSensitivityVO>> result = securityService.pageColumnSensitivity(1, 1, queryDTO);
        if (result.isSuccess() && result.getData() != null && !result.getData().getRecords().isEmpty()) {
            return Result.success(result.getData().getRecords().get(0));
        }
        return Result.success(null);
    }

    @ApiOperation("审核敏感字段记录")
    @PostMapping("/sensitivity/{id}/review")
    public Result<Void> reviewColumnSensitivity(
            @PathVariable Long id,
            @ApiParam("审核状态：APPROVED/REJECTED") @RequestParam String reviewStatus,
            @ApiParam("审核意见") @RequestParam(required = false) String reviewComment) {
        return securityService.reviewColumnSensitivity(id, reviewStatus, reviewComment, null);
    }

    @ApiOperation("批量审核敏感字段记录")
    @PostMapping("/sensitivity/batch-review")
    public Result<Void> batchReviewColumnSensitivity(
            @ApiParam("记录ID列表") @RequestBody List<Long> ids,
            @ApiParam("审核状态：APPROVED/REJECTED") @RequestParam String reviewStatus,
            @ApiParam("审核意见") @RequestParam(required = false) String reviewComment) {
        return securityService.batchReviewColumnSensitivity(ids, reviewStatus, reviewComment, null);
    }

    @ApiOperation("删除敏感字段记录")
    @DeleteMapping("/sensitivity/{id}")
    public Result<Void> deleteSensitivity(@PathVariable Long id) {
        return securityService.deleteColumnSensitivity(id);
    }

    @ApiOperation("根据扫描批次号获取敏感字段列表")
    @GetMapping("/sensitivity/batch/{batchNo}")
    public Result<List<SecColumnSensitivityVO>> getByBatchNo(@PathVariable String batchNo) {
        SecColumnSensitivityQueryDTO queryDTO = SecColumnSensitivityQueryDTO.builder().scanBatchNo(batchNo).build();
        Result<Page<SecColumnSensitivityVO>> result = securityService.pageColumnSensitivity(1, 10000, queryDTO);
        return Result.success(result.getData() != null ? result.getData().getRecords() : List.of());
    }

    @ApiOperation("获取数据源及其敏感字段统计")
    @GetMapping("/sensitivity/ds-stats")
    public Result<List<Map<String, Object>>> getSensitivityStatsByDs() {
        return securityService.getSensitivityStatsByDs();
    }

    @ApiOperation("敏感字段审核状态汇总（单条 SQL，供管理页统计卡片）")
    @GetMapping("/sensitivity/review-counts")
    public Result<Map<String, Long>> getSensitivityReviewCounts() {
        return securityService.getSensitivityReviewCounts();
    }

    @ApiOperation("手动新增/更新敏感字段记录")
    @PostMapping("/sensitivity/save")
    public Result<Void> saveColumnSensitivity(@Validated @RequestBody SecColumnSensitivitySaveDTO dto) {
        return securityService.saveColumnSensitivity(dto);
    }

    @ApiOperation("批量新增/更新敏感字段记录")
    @PostMapping("/sensitivity/batch-save")
    public Result<Void> batchSaveColumnSensitivity(@RequestBody List<SecColumnSensitivitySaveDTO> dtos) {
        return securityService.batchSaveColumnSensitivity(dtos);
    }

    @ApiOperation("根据数据源和表名获取字段敏感等级列表")
    @GetMapping("/sensitivity/table")
    public Result<List<SecColumnSensitivityVO>> getColumnSensitivityByTable(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName) {
        return securityService.getColumnSensitivityByTable(dsId, tableName);
    }

    // ==================== 数据脱敏模板 ====================

    @ApiOperation("分页查询数据脱敏模板")
    @GetMapping("/mask-template/page")
    public Result<Page<SecMaskTemplateVO>> pageMaskTemplate(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("是否启用") @RequestParam(required = false) Integer enabled) {
        return securityService.pageMaskTemplate(pageNum, pageSize, enabled);
    }

    @ApiOperation("获取脱敏模板列表（不分页）")
    @GetMapping("/mask-template/list")
    public Result<List<SecMaskTemplateVO>> listMaskTemplate(
            @ApiParam("是否启用") @RequestParam(required = false) Integer enabled) {
        return securityService.listMaskTemplate(enabled);
    }

    @ApiOperation("新增脱敏模板")
    @PostMapping("/mask-template")
    public Result<SecMaskTemplate> saveMaskTemplate(@Validated @RequestBody SecMaskTemplateSaveDTO dto) {
        return securityService.saveMaskTemplate(dto);
    }

    @ApiOperation("更新脱敏模板")
    @PutMapping("/mask-template/{id}")
    public Result<Void> updateMaskTemplate(@PathVariable Long id, @Validated @RequestBody SecMaskTemplateSaveDTO dto) {
        return securityService.updateMaskTemplate(id, dto);
    }

    @ApiOperation("删除脱敏模板")
    @DeleteMapping("/mask-template/{id}")
    public Result<Void> deleteMaskTemplate(@PathVariable Long id) {
        return securityService.deleteMaskTemplate(id);
    }

    @ApiOperation("启用脱敏模板")
    @PostMapping("/mask-template/{id}/enable")
    public Result<Void> enableMaskTemplate(@PathVariable Long id) {
        return securityService.enableMaskTemplate(id);
    }

    @ApiOperation("禁用脱敏模板")
    @PostMapping("/mask-template/{id}/disable")
    public Result<Void> disableMaskTemplate(@PathVariable Long id) {
        return securityService.disableMaskTemplate(id);
    }

    // ==================== 获取枚举选项 ====================

    @ApiOperation("获取所有枚举选项")
    @GetMapping("/enums")
    public Result<Map<String, Object>> getEnums() {
        return securityService.getEnums();
    }

    // ==================== 敏感字段访问审批 ====================

    @ApiOperation("提交敏感字段访问申请")
    @PostMapping("/access-apply")
    public Result<SecAccessApplication> submitAccessApplication(@Validated @RequestBody SecAccessApplicationDTO dto) {
        return securityService.submitAccessApplication(dto);
    }

    @ApiOperation("取消访问申请")
    @PostMapping("/access-apply/{id}/cancel")
    public Result<Void> cancelAccessApplication(
            @PathVariable Long id,
            @ApiParam("操作人ID") @RequestParam Long operatorId,
            @ApiParam("操作人名称") @RequestParam String operatorName) {
        return securityService.cancelAccessApplication(id, operatorId, operatorName);
    }

    @ApiOperation("审批访问申请")
    @PostMapping("/access-apply/approve")
    public Result<Void> approveAccessApplication(@Validated @RequestBody SecAccessApprovalDTO dto) {
        return securityService.approveAccessApplication(dto);
    }

    @ApiOperation("分页查询访问申请")
    @GetMapping("/access-apply/page")
    public Result<Page<SecAccessApplicationVO>> pageAccessApply(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("申请编号") @RequestParam(required = false) String applicationNo,
            @ApiParam("申请人ID") @RequestParam(required = false) Long applicantId,
            @ApiParam("申请人名称") @RequestParam(required = false) String applicantName,
            @ApiParam("目标数据源ID") @RequestParam(required = false) Long targetDsId,
            @ApiParam("目标表名") @RequestParam(required = false) String targetTable,
            @ApiParam("申请状态") @RequestParam(required = false) String status) {

        SecAccessApplicationQueryDTO queryDTO = SecAccessApplicationQueryDTO.builder()
                .applicationNo(applicationNo)
                .applicantId(applicantId)
                .applicantName(applicantName)
                .targetDsId(targetDsId)
                .targetTable(targetTable)
                .status(status)
                .build();
        return securityService.pageAccessApplication(pageNum, pageSize, queryDTO);
    }

    @ApiOperation("获取待审批列表")
    @GetMapping("/access-apply/pending")
    public Result<Page<SecAccessApplicationVO>> pagePendingApproval(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("审批人ID") @RequestParam(required = false) Long approverId) {
        return securityService.pagePendingApproval(pageNum, pageSize, approverId);
    }

    @ApiOperation("获取访问申请详情")
    @GetMapping("/access-apply/{id}")
    public Result<SecAccessApplicationVO> getAccessApplyById(@PathVariable Long id) {
        return securityService.getAccessApplicationById(id);
    }

    @ApiOperation("查询访问申请审批历史")
    @GetMapping("/access-apply/{id}/audit-history")
    public Result<List<SecAccessAuditVO>> listAuditHistory(@PathVariable Long id) {
        return securityService.listAccessAuditHistory(id);
    }

    @ApiOperation("查询访问审计日志")
    @GetMapping("/access-log/page")
    public Result<Page<SecAccessLogVO>> pageAccessLog(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("申请ID") @RequestParam(required = false) Long applicationId,
            @ApiParam("操作类型") @RequestParam(required = false) String operationType,
            @ApiParam("操作人ID") @RequestParam(required = false) Long operatorId) {
        return securityService.pageAccessLog(pageNum, pageSize, applicationId, operationType, operatorId);
    }

    @ApiOperation("获取访问申请统计")
    @GetMapping("/access-stats")
    public Result<Map<String, Object>> getAccessStats() {
        return securityService.getAccessStats();
    }

    // ==================== 脱敏任务管理 ====================

    @Autowired
    private com.bagdatahouse.governance.security.service.MaskService maskService;

    @ApiOperation("分页查询脱敏任务")
    @GetMapping("/mask-task/page")
    public Result<Page<com.bagdatahouse.governance.security.vo.SecMaskTaskVO>> pageMaskTask(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId,
            @ApiParam("任务状态") @RequestParam(required = false) String status,
            @ApiParam("任务类型") @RequestParam(required = false) String taskType) {
        return maskService.pageMaskTask(pageNum, pageSize, dsId, status, taskType);
    }

    @ApiOperation("新增脱敏任务")
    @PostMapping("/mask-task")
    public Result<com.bagdatahouse.core.entity.SecMaskTask> saveMaskTask(
            @Validated @RequestBody com.bagdatahouse.governance.security.dto.SecMaskTaskSaveDTO dto) {
        return maskService.saveMaskTask(dto);
    }

    @ApiOperation("更新脱敏任务")
    @PutMapping("/mask-task/{id}")
    public Result<Void> updateMaskTask(@PathVariable Long id,
            @Validated @RequestBody com.bagdatahouse.governance.security.dto.SecMaskTaskSaveDTO dto) {
        return maskService.updateMaskTask(id, dto);
    }

    @ApiOperation("删除脱敏任务")
    @DeleteMapping("/mask-task/{id}")
    public Result<Void> deleteMaskTask(@PathVariable Long id) {
        return maskService.deleteMaskTask(id);
    }

    @ApiOperation("批量删除脱敏任务")
    @DeleteMapping("/mask-task/batch")
    public Result<Void> batchDeleteMaskTask(@RequestBody java.util.List<Long> ids) {
        return maskService.batchDeleteMaskTask(ids);
    }

    @ApiOperation("获取脱敏任务详情")
    @GetMapping("/mask-task/{id}")
    public Result<com.bagdatahouse.core.entity.SecMaskTask> getMaskTaskById(@PathVariable Long id) {
        return maskService.getMaskTaskById(id);
    }

    @ApiOperation("启用脱敏任务")
    @PostMapping("/mask-task/{id}/enable")
    public Result<Void> enableMaskTask(@PathVariable Long id) {
        return maskService.enableMaskTask(id);
    }

    @ApiOperation("禁用脱敏任务")
    @PostMapping("/mask-task/{id}/disable")
    public Result<Void> disableMaskTask(@PathVariable Long id) {
        return maskService.disableMaskTask(id);
    }

    @ApiOperation("执行脱敏任务（静态脱敏）")
    @PostMapping("/mask-task/{id}/execute")
    public Result<com.bagdatahouse.governance.security.vo.SecMaskExecutionLogVO> executeMaskTask(@PathVariable Long id) {
        return maskService.executeMaskTask(id);
    }

    @ApiOperation("分页查询脱敏执行日志")
    @GetMapping("/mask-task/{taskId}/log/page")
    public Result<Page<com.bagdatahouse.governance.security.vo.SecMaskExecutionLogVO>> pageMaskExecutionLog(
            @PathVariable Long taskId,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        return maskService.pageMaskExecutionLog(pageNum, pageSize, taskId);
    }

    // ==================== 脱敏策略管理 ====================

    @ApiOperation("分页查询脱敏策略")
    @GetMapping("/mask-strategy/page")
    public Result<Page<com.bagdatahouse.governance.security.vo.SecMaskStrategyVO>> pageMaskStrategy(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("场景类型") @RequestParam(required = false) String sceneType,
            @ApiParam("状态") @RequestParam(required = false) String status) {
        return maskService.pageMaskStrategy(pageNum, pageSize, sceneType, status);
    }

    @ApiOperation("新增脱敏策略")
    @PostMapping("/mask-strategy")
    public Result<com.bagdatahouse.core.entity.SecMaskStrategy> saveMaskStrategy(
            @Validated @RequestBody com.bagdatahouse.governance.security.dto.SecMaskStrategySaveDTO dto) {
        return maskService.saveMaskStrategy(dto);
    }

    @ApiOperation("更新脱敏策略")
    @PutMapping("/mask-strategy/{id}")
    public Result<Void> updateMaskStrategy(@PathVariable Long id,
            @Validated @RequestBody com.bagdatahouse.governance.security.dto.SecMaskStrategySaveDTO dto) {
        return maskService.updateMaskStrategy(id, dto);
    }

    @ApiOperation("删除脱敏策略")
    @DeleteMapping("/mask-strategy/{id}")
    public Result<Void> deleteMaskStrategy(@PathVariable Long id) {
        return maskService.deleteMaskStrategy(id);
    }

    @ApiOperation("获取脱敏策略详情")
    @GetMapping("/mask-strategy/{id}")
    public Result<com.bagdatahouse.core.entity.SecMaskStrategy> getMaskStrategyById(@PathVariable Long id) {
        return maskService.getMaskStrategyById(id);
    }

    @ApiOperation("获取脱敏策略列表（不分页，按场景分组）")
    @GetMapping("/mask-strategy/list")
    public Result<java.util.List<com.bagdatahouse.governance.security.vo.SecMaskStrategyVO>> listMaskStrategyByScene(
            @ApiParam("场景类型") @RequestParam(required = false) String sceneType) {
        return maskService.listMaskStrategyByScene(sceneType);
    }

    @ApiOperation("启用脱敏策略")
    @PostMapping("/mask-strategy/{id}/enable")
    public Result<Void> enableMaskStrategy(@PathVariable Long id) {
        return maskService.enableMaskStrategy(id);
    }

    @ApiOperation("禁用脱敏策略")
    @PostMapping("/mask-strategy/{id}/disable")
    public Result<Void> disableMaskStrategy(@PathVariable Long id) {
        return maskService.disableMaskStrategy(id);
    }

    @ApiOperation("检测脱敏策略冲突")
    @GetMapping("/mask-strategy/conflicts")
    public Result<java.util.List<String>> detectStrategyConflicts() {
        return maskService.detectStrategyConflicts();
    }

    // ==================== 脱敏白名单管理 ====================

    @ApiOperation("分页查询脱敏白名单")
    @GetMapping("/mask-whitelist/page")
    public Result<Page<com.bagdatahouse.governance.security.vo.SecMaskWhitelistVO>> pageMaskWhitelist(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("策略ID") @RequestParam(required = false) Long strategyId,
            @ApiParam("实体类型") @RequestParam(required = false) String entityType,
            @ApiParam("状态") @RequestParam(required = false) String status) {
        return maskService.pageMaskWhitelist(pageNum, pageSize, strategyId, entityType, status);
    }

    @ApiOperation("新增脱敏白名单")
    @PostMapping("/mask-whitelist")
    public Result<com.bagdatahouse.core.entity.SecMaskWhitelist> saveMaskWhitelist(
            @Validated @RequestBody com.bagdatahouse.governance.security.dto.SecMaskWhitelistSaveDTO dto) {
        return maskService.saveMaskWhitelist(dto);
    }

    @ApiOperation("更新脱敏白名单")
    @PutMapping("/mask-whitelist/{id}")
    public Result<Void> updateMaskWhitelist(@PathVariable Long id,
            @Validated @RequestBody com.bagdatahouse.governance.security.dto.SecMaskWhitelistSaveDTO dto) {
        return maskService.updateMaskWhitelist(id, dto);
    }

    @ApiOperation("删除脱敏白名单")
    @DeleteMapping("/mask-whitelist/{id}")
    public Result<Void> deleteMaskWhitelist(@PathVariable Long id) {
        return maskService.deleteMaskWhitelist(id);
    }

    @ApiOperation("获取脱敏白名单详情")
    @GetMapping("/mask-whitelist/{id}")
    public Result<com.bagdatahouse.core.entity.SecMaskWhitelist> getMaskWhitelistById(@PathVariable Long id) {
        return maskService.getMaskWhitelistById(id);
    }

    @ApiOperation("审批脱敏白名单")
    @PostMapping("/mask-whitelist/approve")
    public Result<Void> approveMaskWhitelist(
            @Validated @RequestBody com.bagdatahouse.governance.security.dto.SecMaskWhitelistApproveDTO dto) {
        return maskService.approveMaskWhitelist(dto);
    }

    @ApiOperation("撤销脱敏白名单")
    @PostMapping("/mask-whitelist/{id}/revoke")
    public Result<Void> revokeMaskWhitelist(@PathVariable Long id) {
        return maskService.revokeMaskWhitelist(id);
    }

    @ApiOperation("检查用户/角色是否在白名单中")
    @GetMapping("/mask-whitelist/check")
    public Result<Boolean> isInWhitelist(
            @ApiParam("策略ID") @RequestParam Long strategyId,
            @ApiParam("实体类型") @RequestParam String entityType,
            @ApiParam("用户ID/角色ID") @RequestParam Long entityId) {
        return maskService.isInWhitelist(strategyId, entityType, entityId);
    }

    // ==================== 动态脱敏 ====================

    @ApiOperation("动态脱敏（根据策略对数据进行脱敏）")
    @PostMapping("/mask/dynamic")
    public Result<String> dynamicMask(
            @ApiParam("原始值") @RequestParam String value,
            @ApiParam("场景类型") @RequestParam String sceneType,
            @ApiParam("敏感等级") @RequestParam(required = false) String level,
            @ApiParam("当前用户类型") @RequestParam(required = false) String entityType,
            @ApiParam("当前用户ID") @RequestParam(required = false) Long entityId) {
        return maskService.dynamicMask(value, sceneType, level, entityType, entityId);
    }

    // ==================== 数据安全健康分 ====================

    @ApiOperation("获取数据安全健康分")
    @GetMapping("/health-score")
    public Result<Map<String, Object>> getHealthScore() {
        return securityService.getHealthScore();
    }

    // ==================== 扫描历史管理 ====================

    @ApiOperation("分页查询扫描历史")
    @GetMapping("/scan/history")
    public Result<Page<Map<String, Object>>> pageScanHistory(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("批次号") @RequestParam(required = false) String batchNo,
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId,
            @ApiParam("扫描状态") @RequestParam(required = false) String status) {
        return securityService.pageScanHistory(pageNum, pageSize, batchNo, dsId, status);
    }

    @ApiOperation("获取安全总览统计数据")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getSecurityOverview() {
        return securityService.getSecurityOverview();
    }

    @ApiOperation("获取安全总览趋势数据")
    @GetMapping("/overview/trend")
    public Result<Map<String, Object>> getSecurityOverviewTrend(
            @ApiParam(value = "统计天数", defaultValue = "30") @RequestParam(required = false) Integer days) {
        return securityService.getSecurityOverviewTrend(days);
    }

    // ==================== 定时扫描任务管理 ====================

    @ApiOperation("分页查询定时扫描任务")
    @GetMapping("/scan-schedule/page")
    public Result<Page<com.bagdatahouse.core.entity.SecScanSchedule>> pageScanSchedule(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId,
            @ApiParam("是否启用") @RequestParam(required = false) Integer enabled) {
        return securityService.pageScanSchedule(pageNum, pageSize, dsId, enabled);
    }

    @ApiOperation("获取定时扫描任务详情")
    @GetMapping("/scan-schedule/{id}")
    public Result<com.bagdatahouse.core.entity.SecScanSchedule> getScanScheduleById(@PathVariable Long id) {
        return securityService.getScanScheduleById(id);
    }

    @ApiOperation("新增定时扫描任务")
    @PostMapping("/scan-schedule")
    public Result<com.bagdatahouse.core.entity.SecScanSchedule> saveScanSchedule(
            @Validated @RequestBody com.bagdatahouse.governance.security.dto.SecScanScheduleSaveDTO dto) {
        return securityService.saveScanSchedule(dto);
    }

    @ApiOperation("更新定时扫描任务")
    @PutMapping("/scan-schedule/{id}")
    public Result<Void> updateScanSchedule(@PathVariable Long id,
            @Validated @RequestBody com.bagdatahouse.governance.security.dto.SecScanScheduleSaveDTO dto) {
        return securityService.updateScanSchedule(id, dto);
    }

    @ApiOperation("删除定时扫描任务")
    @DeleteMapping("/scan-schedule/{id}")
    public Result<Void> deleteScanSchedule(@PathVariable Long id) {
        return securityService.deleteScanSchedule(id);
    }

    @ApiOperation("手动触发定时扫描任务")
    @PostMapping("/scan-schedule/{id}/trigger")
    public Result<Void> triggerScanSchedule(@PathVariable Long id) {
        return securityService.triggerScanSchedule(id);
    }

    // ==================== 分类等级绑定配置 ====================

    @ApiOperation("查询分类的等级绑定列表")
    @GetMapping("/class-level-binding/{classId}")
    public Result<java.util.List<com.bagdatahouse.governance.security.vo.SecClassLevelBindingVO>> listClassLevelBindings(
            @PathVariable Long classId) {
        return securityService.listClassLevelBindings(classId);
    }

    @ApiOperation("新增/更新分类等级绑定")
    @PostMapping("/class-level-binding")
    public Result<Void> saveClassLevelBinding(
            @Validated @RequestBody com.bagdatahouse.governance.security.dto.SecClassLevelBindingSaveDTO dto) {
        return securityService.saveClassLevelBinding(dto);
    }

    @ApiOperation("删除分类等级绑定")
    @DeleteMapping("/class-level-binding/{id}")
    public Result<Void> deleteClassLevelBinding(@PathVariable Long id) {
        return securityService.deleteClassLevelBinding(id);
    }

    @ApiOperation("获取分类推荐等级")
    @GetMapping("/class-level-binding/{classId}/recommended")
    public Result<java.util.List<com.bagdatahouse.governance.security.vo.SecLevelVO>> listRecommendedLevels(
            @PathVariable Long classId) {
        return securityService.listRecommendedLevels(classId);
    }

    // ==================== 新字段发现（T6） ====================

    @ApiOperation("分页查询新字段告警列表")
    @GetMapping("/new-column-alerts")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.bagdatahouse.core.entity.SecNewColumnAlert>> pageNewColumnAlerts(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("状态：PENDING/SCANNED/DISMISSED") @RequestParam(required = false) String status,
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId,
            @ApiParam("表名") @RequestParam(required = false) String tableName) {
        return securityService.pageNewColumnAlerts(pageNum, pageSize, status, dsId, tableName);
    }

    @ApiOperation("获取新字段告警待处理数量")
    @GetMapping("/new-column-alerts/pending-count")
    public Result<Long> countPendingNewColumnAlerts() {
        return securityService.countPendingNewColumnAlerts();
    }

    @ApiOperation("一键扫描新字段告警")
    @PostMapping("/new-column-alerts/{alertId}/scan")
    public Result<com.bagdatahouse.governance.security.vo.SecScanResultVO> scanNewColumnAlert(
            @ApiParam("告警ID") @PathVariable Long alertId,
            @ApiParam("操作人ID") @RequestParam(required = false, defaultValue = "1") Long operatorId) {
        return securityService.scanNewColumnAlert(alertId, operatorId);
    }

    @ApiOperation("忽略新字段告警")
    @DeleteMapping("/new-column-alerts/{alertId}")
    public Result<Void> dismissNewColumnAlert(
            @ApiParam("告警ID") @PathVariable Long alertId,
            @ApiParam("操作人ID") @RequestParam(required = false, defaultValue = "1") Long operatorId,
            @ApiParam("处理意见") @RequestParam(required = false) String comment) {
        return securityService.dismissNewColumnAlert(alertId, operatorId, comment);
    }
}
