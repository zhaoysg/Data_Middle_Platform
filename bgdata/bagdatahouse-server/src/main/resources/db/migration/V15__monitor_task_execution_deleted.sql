-- 与实体 MonitorTaskExecution @TableLogic 及 Mapper XML 对齐：补充逻辑删除字段
ALTER TABLE `monitor_task_execution`
    ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是' AFTER `create_time`;
