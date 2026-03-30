package com.bagdatahouse.dqc.engine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Rule execution result
 */
public class ExecutionResult {

    private boolean success;
    private Long ruleId;
    private String ruleName;
    private String ruleCode;
    private String ruleType;
    private Integer ruleStrength;
    private String dimensions;
    private Long targetDsId;
    private String targetTable;
    private String targetColumn;
    private Status status;
    private Long totalCount;
    private Long errorCount;
    private Long passCount;
    private BigDecimal actualValue;
    private Integer qualityScore;
    private String errorDetail;
    private String sqlContent;
    private Long elapsedMs;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean blocked;
    private LocalDateTime createTime;
    private String executionNo;
    private Map<String, Object> details;

    public enum Status {
        SUCCESS,
        FAILED,
        SKIPPED,
        RUNNING,
        BLOCKED
    }

    public ExecutionResult() {
    }

    public ExecutionResult(boolean success, Long ruleId, String ruleName, String ruleCode, String ruleType,
                          Integer ruleStrength, String dimensions, Long targetDsId, String targetTable,
                          String targetColumn, Status status, Long totalCount, Long errorCount, Long passCount,
                          BigDecimal actualValue, Integer qualityScore, String errorDetail, String sqlContent,
                          Long elapsedMs, LocalDateTime startTime, LocalDateTime endTime, Boolean blocked,
                          LocalDateTime createTime, String executionNo, Map<String, Object> details) {
        this.success = success;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.ruleCode = ruleCode;
        this.ruleType = ruleType;
        this.ruleStrength = ruleStrength;
        this.dimensions = dimensions;
        this.targetDsId = targetDsId;
        this.targetTable = targetTable;
        this.targetColumn = targetColumn;
        this.status = status;
        this.totalCount = totalCount;
        this.errorCount = errorCount;
        this.passCount = passCount;
        this.actualValue = actualValue;
        this.qualityScore = qualityScore;
        this.errorDetail = errorDetail;
        this.sqlContent = sqlContent;
        this.elapsedMs = elapsedMs;
        this.startTime = startTime;
        this.endTime = endTime;
        this.blocked = blocked;
        this.createTime = createTime;
        this.executionNo = executionNo;
        this.details = details;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public Long getRuleId() { return ruleId; }
    public String getRuleName() { return ruleName; }
    public String getRuleCode() { return ruleCode; }
    public String getRuleType() { return ruleType; }
    public Integer getRuleStrength() { return ruleStrength; }
    public String getDimensions() { return dimensions; }
    public Long getTargetDsId() { return targetDsId; }
    public String getTargetTable() { return targetTable; }
    public String getTargetColumn() { return targetColumn; }
    public Status getStatus() { return status; }
    public Long getTotalCount() { return totalCount; }
    public Long getErrorCount() { return errorCount; }
    public Long getPassCount() { return passCount; }
    public BigDecimal getActualValue() { return actualValue; }
    public Integer getQualityScore() { return qualityScore; }
    public String getErrorDetail() { return errorDetail; }
    public String getSqlContent() { return sqlContent; }
    public Long getElapsedMs() { return elapsedMs; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Boolean getBlocked() { return blocked; }
    public LocalDateTime getCreateTime() { return createTime; }
    public String getExecutionNo() { return executionNo; }
    public Map<String, Object> getDetails() { return details; }

    // Setters
    public void setSuccess(boolean success) { this.success = success; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public void setRuleStrength(Integer ruleStrength) { this.ruleStrength = ruleStrength; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public void setTargetDsId(Long targetDsId) { this.targetDsId = targetDsId; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
    public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }
    public void setStatus(Status status) { this.status = status; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public void setErrorCount(Long errorCount) { this.errorCount = errorCount; }
    public void setPassCount(Long passCount) { this.passCount = passCount; }
    public void setActualValue(BigDecimal actualValue) { this.actualValue = actualValue; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }
    public void setSqlContent(String sqlContent) { this.sqlContent = sqlContent; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setExecutionNo(String executionNo) { this.executionNo = executionNo; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionResult that = (ExecutionResult) o;
        return success == that.success &&
               Objects.equals(ruleId, that.ruleId) &&
               Objects.equals(ruleName, that.ruleName) &&
               Objects.equals(ruleCode, that.ruleCode) &&
               Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, ruleId, ruleName, ruleCode, status);
    }

    @Override
    public String toString() {
        return "ExecutionResult{" +
               "success=" + success +
               ", ruleId=" + ruleId +
               ", ruleName='" + ruleName + '\'' +
               ", ruleCode='" + ruleCode + '\'' +
               ", status=" + status +
               ", totalCount=" + totalCount +
               ", passCount=" + passCount +
               ", errorCount=" + errorCount +
               ", qualityScore=" + qualityScore +
               '}';
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private Long ruleId;
        private String ruleName;
        private String ruleCode;
        private String ruleType;
        private Integer ruleStrength;
        private String dimensions;
        private Long targetDsId;
        private String targetTable;
        private String targetColumn;
        private Status status;
        private Long totalCount;
        private Long errorCount;
        private Long passCount;
        private BigDecimal actualValue;
        private Integer qualityScore;
        private String errorDetail;
        private String sqlContent;
        private Long elapsedMs;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Boolean blocked;
        private LocalDateTime createTime;
        private String executionNo;
        private Map<String, Object> details;

        public Builder success(boolean success) { this.success = success; return this; }
        public Builder ruleId(Long ruleId) { this.ruleId = ruleId; return this; }
        public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Builder ruleCode(String ruleCode) { this.ruleCode = ruleCode; return this; }
        public Builder ruleType(String ruleType) { this.ruleType = ruleType; return this; }
        public Builder ruleStrength(Integer ruleStrength) { this.ruleStrength = ruleStrength; return this; }
        public Builder dimensions(String dimensions) { this.dimensions = dimensions; return this; }
        public Builder targetDsId(Long targetDsId) { this.targetDsId = targetDsId; return this; }
        public Builder targetTable(String targetTable) { this.targetTable = targetTable; return this; }
        public Builder targetColumn(String targetColumn) { this.targetColumn = targetColumn; return this; }
        public Builder status(Status status) { this.status = status; return this; }
        public Builder totalCount(Long totalCount) { this.totalCount = totalCount; return this; }
        public Builder errorCount(Long errorCount) { this.errorCount = errorCount; return this; }
        public Builder passCount(Long passCount) { this.passCount = passCount; return this; }
        public Builder actualValue(BigDecimal actualValue) { this.actualValue = actualValue; return this; }
        public Builder qualityScore(Integer qualityScore) { this.qualityScore = qualityScore; return this; }
        public Builder errorDetail(String errorDetail) { this.errorDetail = errorDetail; return this; }
        public Builder sqlContent(String sqlContent) { this.sqlContent = sqlContent; return this; }
        public Builder elapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; return this; }
        public Builder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public Builder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public Builder blocked(Boolean blocked) { this.blocked = blocked; return this; }
        public Builder createTime(LocalDateTime createTime) { this.createTime = createTime; return this; }
        public Builder executionNo(String executionNo) { this.executionNo = executionNo; return this; }
        public Builder details(Map<String, Object> details) { this.details = details; return this; }

        public ExecutionResult build() {
            return new ExecutionResult(success, ruleId, ruleName, ruleCode, ruleType, ruleStrength,
                    dimensions, targetDsId, targetTable, targetColumn, status, totalCount, errorCount,
                    passCount, actualValue, qualityScore, errorDetail, sqlContent, elapsedMs, startTime,
                    endTime, blocked, createTime, executionNo, details);
        }
    }

    // Static factory methods
    public static ExecutionResult success(Long ruleId, String ruleName, String sqlContent,
                                         Long totalCount, Long passCount, Long errorCount,
                                         BigDecimal actualValue, Integer qualityScore,
                                         Long elapsedMs, LocalDateTime startTime) {
        return builder()
                .success(true)
                .ruleId(ruleId)
                .ruleName(ruleName)
                .status(Status.SUCCESS)
                .totalCount(totalCount)
                .passCount(passCount)
                .errorCount(errorCount)
                .actualValue(actualValue)
                .qualityScore(qualityScore)
                .sqlContent(sqlContent)
                .elapsedMs(elapsedMs)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .blocked(false)
                .build();
    }

    public static ExecutionResult failed(Long ruleId, String ruleName, String sqlContent,
                                         String errorDetail, Long elapsedMs,
                                         LocalDateTime startTime) {
        return builder()
                .success(false)
                .ruleId(ruleId)
                .ruleName(ruleName)
                .status(Status.FAILED)
                .errorDetail(errorDetail)
                .sqlContent(sqlContent)
                .elapsedMs(elapsedMs)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .qualityScore(0)
                .blocked(false)
                .build();
    }

    public static ExecutionResult skipped(Long ruleId, String ruleName, String reason,
                                         Long elapsedMs, LocalDateTime startTime) {
        return builder()
                .success(true)
                .ruleId(ruleId)
                .ruleName(ruleName)
                .status(Status.SKIPPED)
                .errorDetail(reason)
                .elapsedMs(elapsedMs)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .qualityScore(100)
                .blocked(false)
                .build();
    }

    public static ExecutionResult blocked(Long ruleId, String ruleName, String errorDetail,
                                         Long elapsedMs, LocalDateTime startTime) {
        return builder()
                .success(false)
                .ruleId(ruleId)
                .ruleName(ruleName)
                .status(Status.BLOCKED)
                .errorDetail(errorDetail)
                .elapsedMs(elapsedMs)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .qualityScore(0)
                .blocked(true)
                .build();
    }
}
