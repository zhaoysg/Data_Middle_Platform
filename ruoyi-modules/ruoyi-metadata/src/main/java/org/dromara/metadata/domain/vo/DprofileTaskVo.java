package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.metadata.domain.DprofileTask;

/**
 * 数据探查任务视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DprofileTask.class)
@ExcelIgnoreUnannotated
public class DprofileTaskVo extends DprofileTask {

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 数据源编码
     */
    private String dsCode;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 探查级别文本
     */
    private String profileLevelText;

    /**
     * 触发方式文本
     */
    private String triggerTypeText;

    /**
     * 关键词搜索
     */
    private String keyword;

    /**
     * 获取状态文本
     */
    public String getStatusText() {
        String statusVal = super.getStatus();
        if (statusVal == null) return "";
        return switch (statusVal) {
            case "DRAFT" -> "草稿";
            case "RUNNING" -> "运行中";
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            case "STOPPED" -> "已停止";
            default -> statusVal;
        };
    }

    /**
     * 获取探查级别文本
     */
    public String getProfileLevelText() {
        String level = super.getProfileLevel();
        if (level == null) return "";
        return switch (level) {
            case "BASIC" -> "基础探查";
            case "DETAILED" -> "详细探查";
            case "FULL" -> "全量探查";
            default -> level;
        };
    }

    /**
     * 获取触发方式文本
     */
    public String getTriggerTypeText() {
        String type = super.getTriggerType();
        if (type == null) return "";
        return switch (type) {
            case "MANUAL" -> "手动触发";
            case "SCHEDULE" -> "定时调度";
            case "API" -> "API调用";
            default -> type;
        };
    }
}
