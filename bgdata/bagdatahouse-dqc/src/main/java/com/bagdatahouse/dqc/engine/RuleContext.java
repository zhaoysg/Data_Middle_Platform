package com.bagdatahouse.dqc.engine;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Rule execution context
 * Contains all information needed to execute a single rule
 */
public class RuleContext {

    private Long ruleId;
    private String ruleName;
    private String ruleCode;
    private String ruleType;
    private String applyLevel;
    private Long targetDsId;
    /** 数据源配置中的库名（MySQL/TiDB 等用于限定表） */
    private String targetDatabaseName;
    /** Schema（PostgreSQL/SQL Server/Oracle 等；方案 bindValue 可覆盖） */
    private String targetSchemaName;
    private String targetTable;
    private String targetColumn;
    private Long compareDsId;
    private String compareTable;
    private String compareColumn;
    private BigDecimal thresholdMin;
    private BigDecimal thresholdMax;
    private BigDecimal fluctuationThreshold;
    private String regexPattern;
    private String ruleExpr;
    private String dimensions;
    private String ruleStrength;
    private String errorLevel;
    private String customThreshold;
    private String customFunctionClass;
    private String customFunctionParams;
    private Long executionId;
    private String executionNo;
    private Long planId;
    private Boolean autoBlock;

    public RuleContext() {
    }

    // Getters
    public Long getRuleId() { return ruleId; }
    public String getRuleName() { return ruleName; }
    public String getRuleCode() { return ruleCode; }
    public String getRuleType() { return ruleType; }
    public String getApplyLevel() { return applyLevel; }
    public Long getTargetDsId() { return targetDsId; }
    public String getTargetDatabaseName() { return targetDatabaseName; }
    public String getTargetSchemaName() { return targetSchemaName; }
    public String getTargetTable() { return targetTable; }
    public String getTargetColumn() { return targetColumn; }
    public Long getCompareDsId() { return compareDsId; }
    public String getCompareTable() { return compareTable; }
    public String getCompareColumn() { return compareColumn; }
    public BigDecimal getThresholdMin() { return thresholdMin; }
    public BigDecimal getThresholdMax() { return thresholdMax; }
    public BigDecimal getFluctuationThreshold() { return fluctuationThreshold; }
    public String getRegexPattern() { return regexPattern; }
    public String getRuleExpr() { return ruleExpr; }
    public String getDimensions() { return dimensions; }
    public String getRuleStrength() { return ruleStrength; }
    public String getErrorLevel() { return errorLevel; }
    public String getCustomThreshold() { return customThreshold; }
    public String getCustomFunctionClass() { return customFunctionClass; }
    public String getCustomFunctionParams() { return customFunctionParams; }
    public Long getExecutionId() { return executionId; }
    public String getExecutionNo() { return executionNo; }
    public Long getPlanId() { return planId; }
    public Boolean getAutoBlock() { return autoBlock; }

    // Setters
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public void setApplyLevel(String applyLevel) { this.applyLevel = applyLevel; }
    public void setTargetDsId(Long targetDsId) { this.targetDsId = targetDsId; }
    public void setTargetDatabaseName(String targetDatabaseName) { this.targetDatabaseName = targetDatabaseName; }
    public void setTargetSchemaName(String targetSchemaName) { this.targetSchemaName = targetSchemaName; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
    public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }
    public void setCompareDsId(Long compareDsId) { this.compareDsId = compareDsId; }
    public void setCompareTable(String compareTable) { this.compareTable = compareTable; }
    public void setCompareColumn(String compareColumn) { this.compareColumn = compareColumn; }
    public void setThresholdMin(BigDecimal thresholdMin) { this.thresholdMin = thresholdMin; }
    public void setThresholdMax(BigDecimal thresholdMax) { this.thresholdMax = thresholdMax; }
    public void setFluctuationThreshold(BigDecimal fluctuationThreshold) { this.fluctuationThreshold = fluctuationThreshold; }
    public void setRegexPattern(String regexPattern) { this.regexPattern = regexPattern; }
    public void setRuleExpr(String ruleExpr) { this.ruleExpr = ruleExpr; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public void setRuleStrength(String ruleStrength) { this.ruleStrength = ruleStrength; }
    public void setErrorLevel(String errorLevel) { this.errorLevel = errorLevel; }
    public void setCustomThreshold(String customThreshold) { this.customThreshold = customThreshold; }
    public void setCustomFunctionClass(String customFunctionClass) { this.customFunctionClass = customFunctionClass; }
    public void setCustomFunctionParams(String customFunctionParams) { this.customFunctionParams = customFunctionParams; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }
    public void setExecutionNo(String executionNo) { this.executionNo = executionNo; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public void setAutoBlock(Boolean autoBlock) { this.autoBlock = autoBlock; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleContext that = (RuleContext) o;
        return Objects.equals(ruleId, that.ruleId) &&
               Objects.equals(ruleCode, that.ruleCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, ruleCode);
    }

    @Override
    public String toString() {
        return "RuleContext{" +
               "ruleId=" + ruleId +
               ", ruleName='" + ruleName + '\'' +
               ", ruleCode='" + ruleCode + '\'' +
               ", ruleType='" + ruleType + '\'' +
               ", targetTable='" + targetTable + '\'' +
               ", targetColumn='" + targetColumn + '\'' +
               '}';
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long ruleId;
        private String ruleName;
        private String ruleCode;
        private String ruleType;
        private String applyLevel;
        private Long targetDsId;
        private String targetDatabaseName;
        private String targetSchemaName;
        private String targetTable;
        private String targetColumn;
        private Long compareDsId;
        private String compareTable;
        private String compareColumn;
        private BigDecimal thresholdMin;
        private BigDecimal thresholdMax;
        private BigDecimal fluctuationThreshold;
        private String regexPattern;
        private String ruleExpr;
        private String dimensions;
        private String ruleStrength;
        private String errorLevel;
        private String customThreshold;
        private String customFunctionClass;
        private String customFunctionParams;
        private Long executionId;
        private String executionNo;
        private Long planId;
        private Boolean autoBlock;

        public Builder ruleId(Long ruleId) { this.ruleId = ruleId; return this; }
        public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Builder ruleCode(String ruleCode) { this.ruleCode = ruleCode; return this; }
        public Builder ruleType(String ruleType) { this.ruleType = ruleType; return this; }
        public Builder applyLevel(String applyLevel) { this.applyLevel = applyLevel; return this; }
        public Builder targetDsId(Long targetDsId) { this.targetDsId = targetDsId; return this; }
        public Builder targetDatabaseName(String targetDatabaseName) { this.targetDatabaseName = targetDatabaseName; return this; }
        public Builder targetSchemaName(String targetSchemaName) { this.targetSchemaName = targetSchemaName; return this; }
        public Builder targetTable(String targetTable) { this.targetTable = targetTable; return this; }
        public Builder targetColumn(String targetColumn) { this.targetColumn = targetColumn; return this; }
        public Builder compareDsId(Long compareDsId) { this.compareDsId = compareDsId; return this; }
        public Builder compareTable(String compareTable) { this.compareTable = compareTable; return this; }
        public Builder compareColumn(String compareColumn) { this.compareColumn = compareColumn; return this; }
        public Builder thresholdMin(BigDecimal thresholdMin) { this.thresholdMin = thresholdMin; return this; }
        public Builder thresholdMax(BigDecimal thresholdMax) { this.thresholdMax = thresholdMax; return this; }
        public Builder fluctuationThreshold(BigDecimal fluctuationThreshold) { this.fluctuationThreshold = fluctuationThreshold; return this; }
        public Builder regexPattern(String regexPattern) { this.regexPattern = regexPattern; return this; }
        public Builder ruleExpr(String ruleExpr) { this.ruleExpr = ruleExpr; return this; }
        public Builder dimensions(String dimensions) { this.dimensions = dimensions; return this; }
        public Builder ruleStrength(String ruleStrength) { this.ruleStrength = ruleStrength; return this; }
        public Builder errorLevel(String errorLevel) { this.errorLevel = errorLevel; return this; }
        public Builder customThreshold(String customThreshold) { this.customThreshold = customThreshold; return this; }
        public Builder customFunctionClass(String customFunctionClass) { this.customFunctionClass = customFunctionClass; return this; }
        public Builder customFunctionParams(String customFunctionParams) { this.customFunctionParams = customFunctionParams; return this; }
        public Builder executionId(Long executionId) { this.executionId = executionId; return this; }
        public Builder executionNo(String executionNo) { this.executionNo = executionNo; return this; }
        public Builder planId(Long planId) { this.planId = planId; return this; }
        public Builder autoBlock(Boolean autoBlock) { this.autoBlock = autoBlock; return this; }

        public RuleContext build() {
            RuleContext ctx = new RuleContext();
            ctx.ruleId = this.ruleId;
            ctx.ruleName = this.ruleName;
            ctx.ruleCode = this.ruleCode;
            ctx.ruleType = this.ruleType;
            ctx.applyLevel = this.applyLevel;
            ctx.targetDsId = this.targetDsId;
            ctx.targetDatabaseName = this.targetDatabaseName;
            ctx.targetSchemaName = this.targetSchemaName;
            ctx.targetTable = this.targetTable;
            ctx.targetColumn = this.targetColumn;
            ctx.compareDsId = this.compareDsId;
            ctx.compareTable = this.compareTable;
            ctx.compareColumn = this.compareColumn;
            ctx.thresholdMin = this.thresholdMin;
            ctx.thresholdMax = this.thresholdMax;
            ctx.fluctuationThreshold = this.fluctuationThreshold;
            ctx.regexPattern = this.regexPattern;
            ctx.ruleExpr = this.ruleExpr;
            ctx.dimensions = this.dimensions;
            ctx.ruleStrength = this.ruleStrength;
            ctx.errorLevel = this.errorLevel;
            ctx.customThreshold = this.customThreshold;
            ctx.customFunctionClass = this.customFunctionClass;
            ctx.customFunctionParams = this.customFunctionParams;
            ctx.executionId = this.executionId;
            ctx.executionNo = this.executionNo;
            ctx.planId = this.planId;
            ctx.autoBlock = this.autoBlock;
            return ctx;
        }
    }

    /**
     * Create a copy of this context with overrides
     */
    public RuleContext copy() {
        return RuleContext.builder()
                .ruleId(this.ruleId)
                .ruleName(this.ruleName)
                .ruleCode(this.ruleCode)
                .ruleType(this.ruleType)
                .applyLevel(this.applyLevel)
                .targetDsId(this.targetDsId)
                .targetDatabaseName(this.targetDatabaseName)
                .targetSchemaName(this.targetSchemaName)
                .targetTable(this.targetTable)
                .targetColumn(this.targetColumn)
                .compareDsId(this.compareDsId)
                .compareTable(this.compareTable)
                .compareColumn(this.compareColumn)
                .thresholdMin(this.thresholdMin)
                .thresholdMax(this.thresholdMax)
                .fluctuationThreshold(this.fluctuationThreshold)
                .regexPattern(this.regexPattern)
                .ruleExpr(this.ruleExpr)
                .dimensions(this.dimensions)
                .ruleStrength(this.ruleStrength)
                .errorLevel(this.errorLevel)
                .customThreshold(this.customThreshold)
                .customFunctionClass(this.customFunctionClass)
                .customFunctionParams(this.customFunctionParams)
                .executionId(this.executionId)
                .executionNo(this.executionNo)
                .planId(this.planId)
                .autoBlock(this.autoBlock)
                .build();
    }
}
