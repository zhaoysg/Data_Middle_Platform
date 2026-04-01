-- ============================================================
-- 数据中台工具 - V14__consolidated_security_dqc_migrations.sql
-- 版本: 1.0.0 | 日期: 2026-03-27
--
-- 说明:
--   本文件由 V2～V13 所有增量迁移脚本合并而来，
--   用于替换分散的增量脚本，确保迁移顺序正确且无重复。
--
--   V1__init.sql 为基础建表脚本（46张表 + 内置初始数据），
--   本文件仅包含 V2～V13 中的增量部分。
--
--   执行顺序:
--     1. V1__init.sql          → 创建基础表结构 + 内置数据
--     2. test_data.sql         → 导入测试数据（可独立运行）
--     3. V14__consolidated...   → 增量ALTER/新建表/种子数据（第14次Flyway）
--
-- 合并清单:
--   V2  敏感字段定时扫描任务表（sec_scan_schedule）
--   V3  内置敏感字段识别规则（R01～R08）
--   V4  dqc_plan_rule 新增 target_table/target_column 字段
--   V5  种子数据：内置分类/分级 + 内置规则扩展（R09～R15）
--   V5  新建表：sec_mask_task / sec_mask_execution_log / sec_mask_strategy / sec_mask_whitelist
--   V6  gov_lineage 新增 sensitivity_level 字段
--   V7  dqc_execution_detail / dqc_plan_rule 新增敏感等级字段
--   V8  monitor_alert_rule 新增 SENSITIVE 告警相关字段
--   V9  monitor_alert_record 新增 SENSITIVE 告警记录字段
--   V10 新建表：sec_class_level_binding
--   V11 sys_menu 插入数据安全 8 个子菜单 + 菜单路径修复
--   V12 新建表：sec_new_column_alert
--   V13 重复内容已移除（与 V5 的 sec_mask_* 表完全一致）
--
-- 已知问题修复:
--   - V5__sec_seed_data.sql 中 sec_classification 表的列名错误：
--     description → class_desc, parent_id → 无此字段（分类表无父子关系）
--     level 表的 description → level_desc
-- ============================================================

-- ============================================================
-- 【V2】敏感字段定时扫描任务表 + XXL-Job 集成
-- ============================================================

CREATE TABLE IF NOT EXISTS `sec_scan_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `task_code` VARCHAR(50) NOT NULL COMMENT '任务编码，唯一',
    `ds_id` BIGINT NOT NULL COMMENT '数据源ID',
    `scan_scope` VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '扫描范围：ALL-全部表/SPECIFIC-指定表/LAYER-按数据层',
    `table_names` VARCHAR(500) COMMENT '指定表名列表（逗号分隔），scanScope=SPECIFIC时使用',
    `layer_code` VARCHAR(20) COMMENT '数据层，scanScope=LAYER时使用：ODS/DWD/DWS/ADS',
    `enable_content_scan` TINYINT(1) DEFAULT 1 COMMENT '是否启用内容级扫描（1-是，0-否）',
    `sample_size` INT DEFAULT 50 COMMENT '采样数量上限',
    `scan_cycle` VARCHAR(20) NOT NULL DEFAULT 'ONCE' COMMENT '扫描周期：ONCE-单次/DAILY-每日/WEEKLY-每周/MONTHLY-每月',
    `cron_expression` VARCHAR(100) COMMENT 'Cron表达式（可选，手动指定）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用/DISABLED-停用',
    `last_run_time` DATETIME COMMENT '最近执行时间',
    `last_run_status` VARCHAR(20) COMMENT '最近执行状态：SUCCESS/FAILED/RUNNING',
    `last_batch_no` VARCHAR(64) COMMENT '最近执行批次号',
    `last_duration_seconds` INT COMMENT '最近执行耗时（秒）',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_code` (`task_code`),
    KEY `idx_ds_id` (`ds_id`),
    KEY `idx_status` (`status`),
    KEY `idx_scan_cycle` (`scan_cycle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感字段定时扫描任务表';


-- ============================================================
-- 【V3】内置敏感字段识别规则扩充（R01～R08）
-- 对齐阿里内置识别算法
-- ============================================================

-- R01: 手机号识别（强，推荐等级 L3）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '手机号识别(强)', 'R01_PHONE_VALIDATE', '识别中国大陆手机号（1[3-9]\\d{9}），启用内容级算法校验', 1, 3,
    'CONTENT_REGEX', '1[3-9]\\d{9}', 'REGEX', 'MASK', '{"keepHead":3,"keepTail":4,"maskChar":"*"}',
    'CENTER', 95, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R01_PHONE_VALIDATE');

-- R02: 身份证识别（强，推荐等级 L3）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '身份证识别(强)', 'R02_IDCARD_VALIDATE', '识别居民身份证（18位+加权求和校验位），启用内置身份证算法', 1, 3,
    'CONTENT_REGEX', 'IDCARD_VALIDATE', 'BUILTIN_ALGORITHM', 'MASK', '{"keepHead":6,"keepTail":4,"maskChar":"*"}',
    'CENTER', 95, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R02_IDCARD_VALIDATE');

-- R03: 邮箱识别（强，推荐等级 L3）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '邮箱识别(强)', 'R03_EMAIL_FORMAT', '识别电子邮箱地址，启用内置邮箱格式校验', 1, 3,
    'CONTENT_REGEX', '^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$', 'REGEX', 'MASK',
    '{"keepHead":1,"keepTail":0,"maskChar":"*"}', 'CENTER', 90, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R03_EMAIL_FORMAT');

-- R04: 银行卡识别（强，推荐等级 L4）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '银行卡识别(强)', 'R04_BANKCARD_VALIDATE', '识别银行卡号（13-19位+Luhn校验），推荐等级 L4 机密', 1, 4,
    'CONTENT_REGEX', 'BANKCARD_VALIDATE', 'BUILTIN_ALGORITHM', 'MASK', '{"keepHead":4,"keepTail":4,"maskChar":"*"}',
    'CENTER', 98, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R04_BANKCARD_VALIDATE');

-- R05: IP地址识别（中等，推荐等级 L2）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT 'IP地址识别', 'R05_IP_COLUMN_NAME', '识别IP相关字段（ip/ipv4/ipv6/client_ip/server_ip等）', 1, 2,
    'COLUMN_NAME', 'ip,ipv4,ipv6,client_ip,server_ip,remote_ip,forwarded_ip', 'CONTAINS', 'MASK',
    '{"keepHead":0,"keepTail":0,"maskChar":"*"}', 'FULL', 60, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R05_IP_COLUMN_NAME');

-- R06: 地址信息识别（中等，推荐等级 L3）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '地址信息识别', 'R06_ADDRESS_COLUMN_COMMENT', '识别地址相关字段（地址、address、location、province、city等）', 1, 3,
    'COLUMN_COMMENT', '地址,address,location,province,city,district,街道,收货地址', 'CONTAINS', 'MASK',
    '{"keepHead":6,"keepTail":0,"maskChar":"*"}', 'HEAD', 55, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R06_ADDRESS_COLUMN_COMMENT');

-- R07: 姓名识别（慎用，推荐等级 L3）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '姓名识别(慎)', 'R07_NAME_COLUMN_NAME', '识别姓名字段（name,real_name,username,nickname等），注意避免误判', 1, 3,
    'COLUMN_NAME', 'name,real_name,user_name,username,nickname,full_name,surname,contact_name', 'CONTAINS', 'MASK',
    '{"keepHead":1,"keepTail":0,"maskChar":"*"}', 'HEAD', 50, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R07_NAME_COLUMN_NAME');

-- R08: 密码/密钥识别（强，推荐等级 L4）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '密码识别', 'R08_PASSWORD_COLUMN_NAME', '识别密码/密钥字段（password,pwd,passwd,secret,api_key,token等）', 1, 4,
    'COLUMN_NAME', 'password,pwd,passwd,secret,api_key,access_key,private_key,token,credential', 'CONTAINS', 'HIDE',
    '{"keepHead":0,"keepTail":0,"maskChar":"*"}', 'FULL', 99, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R08_PASSWORD_COLUMN_NAME');


-- ============================================================
-- 【V4】dqc_plan_rule 新增 target_table / target_column 字段
-- 支持方案级别覆盖规则默认的目标表和目标列
-- 优先级: planRule.targetTable > rule.targetTable
--         planRule.targetColumn > rule.targetColumn
-- 向后兼容: 已有记录该字段为 NULL，fallback 到规则默认值
-- ============================================================

ALTER TABLE `dqc_plan_rule`
    ADD COLUMN `target_table` VARCHAR(128) DEFAULT NULL COMMENT '目标表名（覆盖规则默认值，空时 fallback 到规则定义）' AFTER `custom_threshold`,
    ADD COLUMN `target_column` VARCHAR(128) DEFAULT NULL COMMENT '目标列名（覆盖规则默认值，空时 fallback 到规则定义）' AFTER `target_table`;

ALTER TABLE `dqc_plan_rule` ADD INDEX `idx_target_table` (`target_table`);


-- ============================================================
-- 【V5 Part A】补充内置分类和分级种子数据
-- 修复：sec_classification 表的正确列名为 class_desc 而非 description
--       sec_classification 表没有 parent_id 字段（无层级结构）
--       sec_level 表的正确列名为 level_desc 而非 description
-- ============================================================

-- 内置分类（客户数据）
INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 1, 'CUSTOMER', '客户数据', '包含个人客户的身份、联系、财务等敏感信息', 1, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sec_classification WHERE id = 1);

-- 内置分类（财务数据）
INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 2, 'FINANCE', '财务数据', '包含企业财务、交易、账务等敏感信息', 2, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sec_classification WHERE id = 2);

-- 内置分类（运营数据）
INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 3, 'OPERATION', '运营数据', '包含业务运营、流程、内部管理等敏感信息', 3, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sec_classification WHERE id = 3);

-- 内置分类（员工数据）
INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 4, 'EMPLOYEE', '员工数据', '包含员工个人信息、薪资、绩效等敏感信息', 4, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sec_classification WHERE id = 4);

-- 内置分级（L4 机密）
INSERT INTO `sec_level` (`id`, `level_code`, `level_name`, `level_desc`, `level_value`, `color`, `icon`, `status`, `deleted`, `create_time`)
SELECT 4, 'L4', '机密', '极高敏感度，泄露会造成严重损害，需最严格保护', 4, '#FF4D4F', 'lock', 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sec_level WHERE id = 4);

-- 内置分级（L3 敏感）
INSERT INTO `sec_level` (`id`, `level_code`, `level_name`, `level_desc`, `level_value`, `color`, `icon`, `status`, `deleted`, `create_time`)
SELECT 3, 'L3', '敏感', '高敏感度，泄露会造成较大影响，需重点保护', 3, '#FAAD14', 'safety', 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sec_level WHERE id = 3);

-- 内置分级（L2 内部）
INSERT INTO `sec_level` (`id`, `level_code`, `level_name`, `level_desc`, `level_value`, `color`, `icon`, `status`, `deleted`, `create_time`)
SELECT 2, 'L2', '内部', '中等敏感度，仅限内部使用，泄露影响有限', 2, '#1677FF', 'team', 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sec_level WHERE id = 2);

-- 内置分级（L1 公开）
INSERT INTO `sec_level` (`id`, `level_code`, `level_name`, `level_desc`, `level_value`, `color`, `icon`, `status`, `deleted`, `create_time`)
SELECT 1, 'L1', '公开', '低敏感度，可对外公开使用', 1, '#52C41A', 'global', 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sec_level WHERE id = 1);


-- ============================================================
-- 【V5 Part B】补充内置识别规则扩展（R09～R15）
-- ============================================================

-- R09: 身份证/护照识别（扩展：除居民身份证外，兼容护照、港澳通行证等）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '身份证/护照识别(强)', 'R09_IDCARD_PASSPORT', '识别居民身份证（18位）、护照号码等证件号，推荐等级 L3', 1, 3,
    'COLUMN_NAME', 'id_card,idcard,identity,identity_no,certificate,cert_no,passport', 'CONTAINS', 'MASK',
    '{"keepHead":4,"keepTail":4,"maskChar":"*"}', 'CENTER', 88, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R09_IDCARD_PASSPORT');

-- R10: 银行卡号识别（扩展字段名）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '银行卡号识别(强)', 'R10_BANKCARD_COLUMN', '识别银行卡号相关字段名（bank_card,card_no,account_no等）', 2, 4,
    'COLUMN_NAME', 'bank_card,bankcard,card_no,cardnum,account_no,account_number,credit_card', 'CONTAINS', 'MASK',
    '{"keepHead":4,"keepTail":4,"maskChar":"*"}', 'CENTER', 87, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R10_BANKCARD_COLUMN');

-- R11: 薪资/工资识别（强，推荐等级 L4）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '薪资工资识别(强)', 'R11_SALARY_COLUMN', '识别薪资、工资、奖金、佣金等财务敏感字段', 4, 4,
    'COLUMN_NAME', 'salary,wage,wages,bonus,commission,compensation,income,payment_amount,pay,earn', 'CONTAINS', 'MASK',
    '{"keepHead":0,"keepTail":0,"maskChar":"*"}', 'FULL', 85, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R11_SALARY_COLUMN');

-- R12: 工号/员工ID识别（中等，推荐等级 L2）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '工号员工ID识别', 'R12_EMPID_COLUMN', '识别员工工号、员工ID、内部编号等字段', 4, 2,
    'COLUMN_NAME', 'emp_id,empid,employee_id,staff_id,staff_no,user_no,employee_no,work_no,job_no', 'CONTAINS', 'MASK',
    '{"keepHead":0,"keepTail":0,"maskChar":"*"}', 'FULL', 40, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R12_EMPID_COLUMN');

-- R13: 宗教/政治/健康信息识别（敏感，推荐等级 L3）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '特殊类别信息识别', 'R13_SPECIAL_CATEGORY', '识别宗教信仰、政治面貌、健康状况等特殊类别信息', 1, 3,
    'COLUMN_COMMENT', '宗教,信仰,政治面貌,健康状况,病史,疾病,信仰,民族,血型', 'CONTAINS', 'MASK',
    '{"keepHead":0,"keepTail":0,"maskChar":"*"}', 'FULL', 45, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R13_SPECIAL_CATEGORY');

-- R14: 车牌号识别（低，推荐等级 L2）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '车牌号识别', 'R14_LICENSE_PLATE', '识别机动车车牌号码字段', 1, 2,
    'COLUMN_NAME', 'license_plate,plate_no,car_no,vehicle_no,car_plate,plate_number', 'CONTAINS', 'MASK',
    '{"keepHead":2,"keepTail":0,"maskChar":"*"}', 'TAIL', 30, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R14_LICENSE_PLATE');

-- R15: 婚姻状况/家庭信息识别（敏感，推荐等级 L3）
INSERT INTO `sec_sensitivity_rule` (`rule_name`, `rule_code`, `description`, `class_id`, `level_id`,
    `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`,
    `suggestion_mask_type`, `priority`, `builtin`, `status`)
SELECT '家庭信息识别(慎)', 'R15_FAMILY_COLUMN', '识别婚姻状况、家庭成员、家庭地址等家庭相关敏感信息', 1, 3,
    'COLUMN_COMMENT', '婚姻,配偶,家庭成员,家属,子女,父亲,母亲,婚姻状况,家庭关系', 'CONTAINS', 'MASK',
    '{"keepHead":0,"keepTail":0,"maskChar":"*"}', 'FULL', 35, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sec_sensitivity_rule WHERE rule_code = 'R15_FAMILY_COLUMN');


-- ============================================================
-- 【V5 Part C】新建脱敏任务相关表（sec_mask_task / sec_mask_execution_log / sec_mask_strategy / sec_mask_whitelist）
-- 注意：V13 与 V5 内容完全一致，已去重，仅保留一份
-- ============================================================

-- 静态脱敏任务表
CREATE TABLE IF NOT EXISTS `sec_mask_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
    `task_code` VARCHAR(64) NOT NULL COMMENT '任务编码（唯一）',
    `task_type` VARCHAR(20) NOT NULL COMMENT '任务类型：STATIC-静态脱敏 / DYNAMIC-动态脱敏',
    `source_ds_id` BIGINT NOT NULL COMMENT '源端数据源ID',
    `source_sql` TEXT COMMENT '源端查询SQL（支持 ${table} 占位符）',
    `source_schema` VARCHAR(64) COMMENT '源端Schema（PG等支持Schema的数据源使用）',
    `target_ds_id` BIGINT COMMENT '目标端数据源ID（静态脱敏时必须）',
    `target_schema` VARCHAR(64) COMMENT '目标端Schema',
    `target_table` VARCHAR(200) COMMENT '目标端表名（静态脱敏时使用）',
    `target_mode` VARCHAR(20) DEFAULT 'APPEND' COMMENT '写入模式：APPEND-追加 / TRUNCATE-清空重写 / UPSERT-upsert',
    `mask_rules` TEXT COMMENT '应用的脱敏规则JSON，格式：[{"columnName":"xxx","maskType":"MASK","maskPattern":"{}"},...]',
    `trigger_type` VARCHAR(20) DEFAULT 'MANUAL' COMMENT '触发方式：MANUAL-手动 / SCHEDULED-定时 / EVENT-事件触发',
    `trigger_cron` VARCHAR(100) COMMENT 'Cron表达式（定时任务时使用）',
    `batch_size` INT DEFAULT 1000 COMMENT '每批处理行数',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '任务状态：DRAFT-草稿 / PUBLISHED-已发布 / RUNNING-执行中 / SUCCESS-成功 / FAILED-失败 / CANCELLED-已取消',
    `last_run_time` DATETIME COMMENT '上次执行时间',
    `last_run_status` VARCHAR(20) COMMENT '上次执行状态',
    `last_run_rows` INT DEFAULT 0 COMMENT '上次处理行数',
    `last_run_error` TEXT COMMENT '上次执行错误信息',
    `total_run_count` INT DEFAULT 0 COMMENT '累计执行次数',
    `remark` VARCHAR(500) COMMENT '备注说明',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_code` (`task_code`),
    KEY `idx_source_ds` (`source_ds_id`),
    KEY `idx_target_ds` (`target_ds_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_user` (`create_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='静态脱敏任务表';

-- 脱敏执行日志表
CREATE TABLE IF NOT EXISTS `sec_mask_execution_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '脱敏任务ID',
    `task_code` VARCHAR(64) NOT NULL COMMENT '任务编码',
    `run_time` DATETIME NOT NULL COMMENT '本次执行时间',
    `status` VARCHAR(20) NOT NULL COMMENT '执行状态：RUNNING-执行中 / SUCCESS-成功 / FAILED-失败 / CANCELLED-取消',
    `total_rows` INT DEFAULT 0 COMMENT '总处理行数',
    `masked_rows` INT DEFAULT 0 COMMENT '脱敏行数',
    `error_rows` INT DEFAULT 0 COMMENT '错误行数',
    `duration_ms` BIGINT DEFAULT 0 COMMENT '执行耗时（毫秒）',
    `error_message` TEXT COMMENT '错误信息',
    `source_row_count` BIGINT COMMENT '源端总行数',
    `execute_sql_log` TEXT COMMENT '执行的SQL摘要日志（脱敏后的SQL，防泄露）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_status` (`status`),
    KEY `idx_run_time` (`run_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='脱敏执行日志表';

-- 脱敏策略表（sceneType + sensitivityLevel → maskType 映射 + 白名单）
CREATE TABLE IF NOT EXISTS `sec_mask_strategy` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `strategy_name` VARCHAR(200) NOT NULL COMMENT '策略名称',
    `strategy_code` VARCHAR(64) NOT NULL COMMENT '策略编码（唯一）',
    `scene_type` VARCHAR(30) NOT NULL COMMENT '应用场景：DEVELOP_SHOW-数据开发 / DATA_MAP_SHOW-数据地图 / ANALYSIS_QUERY-数据分析 / EXPORT_RESULT-导出结果 / PRINT_REPORT-报表打印',
    `strategy_desc` VARCHAR(500) COMMENT '策略描述',
    `level_mask_mapping` TEXT NOT NULL COMMENT '敏感等级→脱敏类型映射JSON，格式：[{"sensitivityLevel":"L4","maskType":"HIDE"},{"sensitivityLevel":"L3","maskType":"MASK"}]',
    `whitelist_config` TEXT COMMENT '白名单配置JSON：[{"type":"USER","id":123,"name":"张三"},{"type":"ROLE","id":456,"name":"管理员"}]',
    `whitelist_expiry` DATETIME COMMENT '白名单全局有效期',
    `priority` INT DEFAULT 100 COMMENT '优先级（数字越小优先级越高）',
    `conflict_check` TINYINT DEFAULT 1 COMMENT '是否启用冲突检测：0-否，1-是',
    `status` VARCHAR(20) DEFAULT 'ENABLED' COMMENT '策略状态：ENABLED-启用 / DISABLED-禁用',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_strategy_code` (`strategy_code`),
    KEY `idx_scene_type` (`scene_type`),
    KEY `idx_status` (`status`),
    KEY `idx_create_user` (`create_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='脱敏策略表';

-- 脱敏白名单表（用户/角色 + 有效期，期内不脱敏）
CREATE TABLE IF NOT EXISTS `sec_mask_whitelist` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `strategy_id` BIGINT NOT NULL COMMENT '关联脱敏策略ID',
    `entity_type` VARCHAR(20) NOT NULL COMMENT '白名单类型：USER-用户 / ROLE-角色',
    `entity_id` BIGINT NOT NULL COMMENT '用户ID或角色ID',
    `entity_name` VARCHAR(100) COMMENT '用户姓名或角色名称',
    `whitelist_type` VARCHAR(20) DEFAULT 'FULL_EXEMPT' COMMENT '白名单类型：FULL_EXEMPT-完全豁免（不脱敏） / PARTIAL_EXEMPT-部分豁免（仅显示）',
    `start_time` DATETIME COMMENT '生效开始时间（为空表示立即生效）',
    `end_time` DATETIME COMMENT '生效结束时间（为空表示永久生效）',
    `reason` VARCHAR(200) COMMENT '申请原因',
    `approver_id` BIGINT COMMENT '审批人ID',
    `approver_name` VARCHAR(100) COMMENT '审批人姓名',
    `approve_time` DATETIME COMMENT '审批时间',
    `approve_comment` VARCHAR(200) COMMENT '审批意见',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-生效 / EXPIRED-已过期 / REVOKED-已撤销',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_strategy_id` (`strategy_id`),
    KEY `idx_entity` (`entity_type`, `entity_id`),
    KEY `idx_status` (`status`),
    KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='脱敏白名单表';


-- ============================================================
-- 【V6】gov_lineage 新增 sensitivity_level 字段
-- 阶段三-T1：血缘节点增加敏感等级字段
-- 元数据扫描完成后回填字段级血缘节点的 sensitivity_level
-- 支持血缘图谱敏感节点着色和敏感传播告警
-- ============================================================

ALTER TABLE `gov_lineage`
    ADD COLUMN `sensitivity_level` VARCHAR(20) DEFAULT NULL
    COMMENT '敏感等级：L1/L2/L3/L4/NORMAL（字段级血缘节点使用）' AFTER `status`;

-- 为已有字段级血缘节点设置默认值 NORMAL
UPDATE `gov_lineage` SET `sensitivity_level` = 'NORMAL' WHERE `lineage_type` = 'COLUMN' AND `sensitivity_level` IS NULL;

-- 添加索引以支持按敏感等级查询血缘
ALTER TABLE `gov_lineage` ADD INDEX `idx_sensitivity_level` (`sensitivity_level`);


-- ============================================================
-- 【V7】dqc_execution_detail / dqc_plan_rule 新增敏感等级字段
-- 阶段三-T3：DQC 质检增强
-- ============================================================

-- dqc_execution_detail 新增 sensitivity_level / sensitivity_class
ALTER TABLE `dqc_execution_detail`
    ADD COLUMN `sensitivity_level` VARCHAR(20) DEFAULT NULL
    COMMENT '关联的敏感字段等级：L1/L2/L3/L4（JOIN sec_column_sensitivity 获取）' AFTER `sql_content`,
    ADD COLUMN `sensitivity_class` VARCHAR(64) DEFAULT NULL
    COMMENT '关联的敏感字段数据分类（JOIN sec_column_sensitivity 获取）' AFTER `sensitivity_level`;

ALTER TABLE `dqc_execution_detail` ADD INDEX `idx_sensitivity_level` (`sensitivity_level`);

-- dqc_plan_rule 新增 sensitivity_level / sensitivity_class（支持按敏感等级维度筛选规则）
ALTER TABLE `dqc_plan_rule`
    ADD COLUMN `sensitivity_level` VARCHAR(20) DEFAULT NULL
    COMMENT '绑定规则的敏感等级维度：L1/L2/L3/L4（为空表示不限等级）' AFTER `enabled`,
    ADD COLUMN `sensitivity_class` VARCHAR(64) DEFAULT NULL
    COMMENT '绑定规则的敏感分类维度（为空表示不限分类）' AFTER `sensitivity_level`;

ALTER TABLE `dqc_plan_rule` ADD INDEX `idx_plan_sensitivity` (`plan_id`, `sensitivity_level`);


-- ============================================================
-- 【V8】monitor_alert_rule 新增 SENSITIVE 告警相关字段
-- 阶段三-T4：告警模块增强 — SENSITIVE 类型告警规则
-- ============================================================

ALTER TABLE `monitor_alert_rule`
    ADD COLUMN `sensitivity_level` VARCHAR(20) DEFAULT NULL
    COMMENT '关联敏感等级：L1/L2/L3/L4（用于 SENSITIVE 类型告警）' AFTER `fluctuation_pct`,
    ADD COLUMN `sensitivity_table` VARCHAR(200) DEFAULT NULL
    COMMENT '关联敏感表名（支持通配符，如 user_%）' AFTER `sensitivity_level`,
    ADD COLUMN `sensitivity_column` VARCHAR(100) DEFAULT NULL
    COMMENT '关联敏感字段名（支持通配符，如 phone%）' AFTER `sensitivity_table`,
    ADD COLUMN `sensitivity_ds_id` BIGINT DEFAULT NULL
    COMMENT '关联数据源ID' AFTER `sensitivity_column`,
    ADD COLUMN `spike_threshold_pct` DECIMAL(5,2) DEFAULT 20.00
    COMMENT '敏感字段突增告警阈值百分比（默认20%）' AFTER `sensitivity_ds_id`,
    ADD COLUMN `unreview_threshold_days` INT DEFAULT 7
    COMMENT '待审核超期天数阈值（默认7天）' AFTER `spike_threshold_pct`,
    ADD COLUMN `access_anomaly_off_hours` VARCHAR(100) DEFAULT NULL
    COMMENT '非工作时间定义（格式如 22:00-08:00，多个用逗号分隔）' AFTER `unreview_threshold_days`,
    ADD COLUMN `access_anomaly_threshold` INT DEFAULT 5
    COMMENT '非工作时间访问次数阈值（默认5次）' AFTER `access_anomaly_off_hours`;

-- 添加索引以支持 SENSITIVE 告警查询场景
ALTER TABLE `monitor_alert_rule` ADD INDEX `idx_sensitivity_ds` (`sensitivity_ds_id`);
ALTER TABLE `monitor_alert_rule` ADD INDEX `idx_sensitivity_level` (`sensitivity_level`);


-- ============================================================
-- 【V9】monitor_alert_record 新增 SENSITIVE 告警记录字段
-- 阶段三-T4：告警模块增强 — SENSITIVE 类型告警记录
-- ============================================================

ALTER TABLE `monitor_alert_record`
    ADD COLUMN `sensitivity_level` VARCHAR(20) DEFAULT NULL
    COMMENT '关联敏感等级：L1/L2/L3/L4' AFTER `target_name`,
    ADD COLUMN `sensitivity_table` VARCHAR(200) DEFAULT NULL
    COMMENT '关联敏感表名' AFTER `sensitivity_level`,
    ADD COLUMN `sensitivity_column` VARCHAR(100) DEFAULT NULL
    COMMENT '关联敏感字段名' AFTER `sensitivity_table`,
    ADD COLUMN `sensitivity_ds_id` BIGINT DEFAULT NULL
    COMMENT '关联数据源ID' AFTER `sensitivity_column`,
    ADD COLUMN `scan_batch_no` VARCHAR(64) DEFAULT NULL
    COMMENT '关联扫描批次号（SENSITIVE_FIELD_SPIKE 溯源用）' AFTER `sensitivity_ds_id`;

-- 添加索引以支持 SENSITIVE 告警记录查询
ALTER TABLE `monitor_alert_record` ADD INDEX `idx_sensitivity_level` (`sensitivity_level`);
ALTER TABLE `monitor_alert_record` ADD INDEX `idx_sensitivity_ds` (`sensitivity_ds_id`);
ALTER TABLE `monitor_alert_record` ADD INDEX `idx_scan_batch` (`scan_batch_no`);


-- ============================================================
-- 【V10】新建 sec_class_level_binding 表
-- 阶段三-T5：分类与等级推荐绑定配置
-- 存储分类→等级的推荐绑定关系
-- ============================================================

CREATE TABLE `sec_class_level_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
    `class_id` BIGINT NOT NULL COMMENT '分类ID（关联 sec_classification.id）',
    `level_id` BIGINT NOT NULL COMMENT '分级ID（关联 sec_level.id）',
    `is_recommended` TINYINT DEFAULT 1 COMMENT '是否为推荐绑定：0-可选，1-推荐',
    `binding_desc` VARCHAR(200) DEFAULT NULL COMMENT '绑定说明',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_class_id` (`class_id`),
    KEY `idx_level_id` (`level_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据分类与分级推荐绑定配置';

-- 插入内置推荐绑定数据
INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`) VALUES
-- 客户数据 → L3（敏感）、L4（机密）
(1, 3, 1, '客户数据通常为敏感等级'),
(1, 4, 0, '客户数据最高可到机密等级'),
-- 财务数据 → L3（敏感）、L4（机密）
(2, 3, 1, '财务数据通常为敏感等级'),
(2, 4, 0, '财务数据最高可到机密等级'),
-- 运营数据 → L2（内部）
(3, 2, 1, '运营数据通常为内部等级'),
(3, 3, 0, '运营数据特殊场景可为敏感等级'),
-- 员工数据 → L2（内部）
(4, 2, 1, '员工基本信息为内部等级'),
(4, 3, 0, '员工敏感信息（薪资等）可为敏感等级');


-- ============================================================
-- 【V11】sys_menu 插入数据安全 8 个子菜单 + 菜单路径修复
-- 阶段五：数据安全模块独立菜单
-- ============================================================

-- 1. 新增 8 个数据安全子菜单（避开已有 ID 区间）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `visible`) VALUES
-- 安全总览大盘
(78, 7, '安全总览', 'SEC_OVERVIEW', 'MENU', '/security/overview', 'security/overview/index', 'dashboard', 1, 1),
-- 分类分级标准（已存在，补全 component）
(73, 7, '分类分级标准', 'SEC_CLASSIFICATION', 'MENU', '/security/classification', 'security/classification/index', 'apartment', 2, 1),
-- 敏感字段识别（扫描）
(79, 7, '敏感字段识别', 'SEC_SENSITIVITY_SCAN', 'MENU', '/security/sensitivity-scan', 'security/sensitivity-scan/index', 'scan', 3, 1),
-- 敏感字段管理（审核）
(80, 7, '敏感字段管理', 'SEC_SENSITIVITY_MANAGE', 'MENU', '/security/sensitivity-manage', 'security/sensitivity-manage/index', 'safety', 4, 1),
-- 识别规则管理
(81, 7, '识别规则管理', 'SEC_SENSITIVITY_RULES', 'MENU', '/security/sensitivity-rules', 'security/sensitivity-rules/index', 'file-search', 5, 1),
-- 脱敏规则管理
(82, 7, '脱敏模板', 'SEC_MASK_RULES', 'MENU', '/security/mask-template', 'security/mask-template/index', 'shield', 6, 1),
-- 脱敏策略管理
(83, 7, '脱敏策略', 'SEC_MASK_STRATEGY', 'MENU', '/security/strategy', 'security/strategy/index', 'setting', 7, 1),
-- 访问审批管理
(84, 7, '脱敏查询', 'SEC_ACCESS_APPROVAL', 'MENU', '/security/mask-query', 'security/mask-query/index', 'search', 8, 1),
-- 敏感数据审计（日志）
(85, 7, '脱敏访问日志', 'SEC_AUDIT', 'MENU', '/security/audit', 'security/audit/index', 'audit', 9, 1)
ON DUPLICATE KEY UPDATE
    `menu_name` = VALUES(`menu_name`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `icon` = VALUES(`icon`),
    `sort_order` = VALUES(`sort_order`);

-- 2. 更新已有菜单的路径（向后兼容旧路径）
UPDATE `sys_menu` SET `path` = '/security/classification', `component` = 'security/classification/index' WHERE `menu_code` = 'SEC_CLASSIFICATION';
UPDATE `sys_menu` SET `path` = '/security/mask-rules', `component` = 'security/mask-rules/index', `menu_name` = '脱敏规则管理' WHERE `menu_code` = 'SEC_MASK';
UPDATE `sys_menu` SET `path` = '/security/rules', `component` = 'security/rules/index', `menu_name` = '识别规则管理' WHERE `menu_code` = 'SEC_SENSITIVITY_RULES';
UPDATE `sys_menu` SET `path` = '/security/mask-template', `component` = 'security/mask-template/index', `menu_name` = '脱敏模板' WHERE `menu_code` IN ('SEC_MASK', 'SEC_MASK_RULES');
UPDATE `sys_menu` SET `path` = '/security/strategy', `component` = 'security/strategy/index', `menu_name` = '脱敏策略' WHERE `menu_code` = 'SEC_MASK_STRATEGY';
UPDATE `sys_menu` SET `path` = '/security/mask-query', `component` = 'security/mask-query/index', `menu_name` = '脱敏查询' WHERE `menu_code` IN ('SEC_ACCESS', 'SEC_ACCESS_APPROVAL');
UPDATE `sys_menu` SET `path` = '/security/audit', `component` = 'security/audit/index', `menu_name` = '脱敏访问日志' WHERE `menu_code` = 'SEC_AUDIT';

-- 3. 为 admin 角色分配所有新菜单权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.id FROM `sys_menu` m WHERE m.`parent_id` = 7 AND m.id >= 78
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- 4. 标记旧 SEC_SENSITIVITY 菜单为隐藏（已被拆分）
UPDATE `sys_menu` SET `visible` = 0 WHERE `menu_code` = 'SEC_SENSITIVITY';


-- ============================================================
-- 【V12】新建 sec_new_column_alert 表
-- 阶段六：新字段发现机制
-- 元数据扫描时若发现新字段（之前未采集过），写入此表
-- ============================================================

CREATE TABLE `sec_new_column_alert` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '告警ID',
    `ds_id` BIGINT NOT NULL COMMENT '数据源ID',
    `table_name` VARCHAR(200) NOT NULL COMMENT '表名',
    `column_name` VARCHAR(100) NOT NULL COMMENT '新发现的字段名',
    `data_type` VARCHAR(50) COMMENT '字段数据类型',
    `column_comment` VARCHAR(500) COMMENT '字段注释',
    `alert_type` VARCHAR(20) DEFAULT 'NEW_COLUMN' COMMENT '告警类型：NEW_COLUMN-新字段',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理/SCANNED-已扫描/DISMISSED-已忽略',
    `scan_batch_no` VARCHAR(64) COMMENT '关联的扫描批次号',
    `handle_comment` VARCHAR(500) COMMENT '处理意见',
    `handle_user` BIGINT COMMENT '处理人',
    `handle_time` DATETIME COMMENT '处理时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发现时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_ds_table` (`ds_id`, `table_name`),
    KEY `idx_status` (`status`),
    KEY `idx_scan_batch` (`scan_batch_no`),
    UNIQUE KEY `uk_ds_table_column` (`ds_id`, `table_name`, `column_name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新字段发现告警表';


-- ============================================================
-- 【V13 已跳过】sec_mask_* 表重复
-- V5__sec_mask_task_and_strategy.sql 和 V13__sec_mask_task_and_strategy.sql
-- 内容完全一致，已在 V5 Part C 中创建，不再重复执行
-- ============================================================


-- ============================================================
-- 【V7 Part B】dqc_quality_score 新增 sensitivity_compliance_score 字段
-- 敏感合规评分：L3+ 敏感字段的合规通过率（必须脱敏/必须审核通过才算合规）
-- 与 entity DqcQualityScore.sensitivityComplianceScore 对应
-- ============================================================

ALTER TABLE `dqc_quality_score`
    ADD COLUMN `sensitivity_compliance_score` DECIMAL(5,2) DEFAULT NULL
    COMMENT '敏感合规评分：L3+敏感字段的合规通过率（0-100）' AFTER `validity_score`;


-- ============================================================
-- 【直连扫描修复】gov_metadata_column 新增 table_name / schema_name 反规范字段
-- 根因：GovMetadataColumn.tableName 原为 @TableField(exist=false)，直接扫描时无 parent metadata_id，
--       导致 sec_column_sensitivity 表的 table_name 始终为 NULL。
-- 影响：敏感字段扫描记录无法关联到具体表名。
-- 解决：增加 table_name 和 schema_name 列，直接扫描时回填这两个字段。
-- ============================================================

ALTER TABLE `gov_metadata_column`
    ADD COLUMN `table_name` VARCHAR(200) DEFAULT NULL COMMENT '表名（反规范字段，直接扫描时回填）' AFTER `metadata_id`,
    ADD COLUMN `schema_name` VARCHAR(100) DEFAULT NULL COMMENT 'Schema 名（PostgreSQL 等多 Schema 数据库使用）' AFTER `table_name`;


-- ============================================================
-- V14 完成
-- ============================================================
