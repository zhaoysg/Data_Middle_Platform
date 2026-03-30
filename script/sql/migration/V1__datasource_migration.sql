-- ============================================================
-- 数据源管理模块迁移 SQL（bigdata 库）
-- 目标数据库: ry_bigdata_v1
-- 说明: 本脚本仅负责 sys_datasource 表结构，菜单脚本请执行 V2__datasource_menu_master.sql
-- ============================================================

-- ----------------------------
-- 1. 创建 sys_datasource 表（参考 ruoyi-system/sys_role 表结构）
-- 字段命名遵循 RuoYi 规范：下划线命名 + TenantEntity 字段
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_datasource` (
    `ds_id`           BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
    `tenant_id`       VARCHAR(20)     DEFAULT '000000'          COMMENT '租户编号',
    `ds_name`         VARCHAR(100)   NOT NULL                  COMMENT '数据源名称',
    `ds_code`         VARCHAR(50)    NOT NULL                  COMMENT '数据源编码',
    `ds_type`         VARCHAR(30)    NOT NULL                  COMMENT '数据源类型：MYSQL/SQLSERVER/ORACLE/TIDB/POSTGRESQL',
    `host`            VARCHAR(255)                              COMMENT '主机地址',
    `port`           INT                                        COMMENT '端口',
    `database_name`   VARCHAR(100)                               COMMENT '数据库名',
    `schema_name`     VARCHAR(100)    DEFAULT NULL              COMMENT 'Schema名称(用于PostgreSQL)',
    `username`        VARCHAR(100)                              COMMENT '用户名',
    `password`        VARCHAR(255)                              COMMENT '密码',
    `connection_params` TEXT                                      COMMENT '额外连接参数JSON',
    `data_layer`      VARCHAR(20)                               COMMENT '数仓层标记：ODS/DWD/DWS/ADS',
    `dept_id`         BIGINT(20)                                COMMENT '部门ID',
    `status`          CHAR(1)         DEFAULT '0'               COMMENT '状态（0正常 1停用）',
    `del_flag`        CHAR(1)         DEFAULT '0'               COMMENT '删除标志（0代表存在 1代表删除）',
    `remark`          VARCHAR(500)    DEFAULT ''                COMMENT '备注',
    `create_dept`     BIGINT(20)                                COMMENT '创建部门',
    `create_by`       BIGINT(20)     DEFAULT NULL               COMMENT '创建者',
    `create_time`     DATETIME                                  COMMENT '创建时间',
    `update_by`       BIGINT(20)     DEFAULT NULL               COMMENT '更新者',
    `update_time`     DATETIME                                  COMMENT '更新时间',
    PRIMARY KEY (`ds_id`),
    UNIQUE KEY `uk_ds_code` (`ds_code`),
    KEY `idx_ds_type` (`ds_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='数据源配置表';
