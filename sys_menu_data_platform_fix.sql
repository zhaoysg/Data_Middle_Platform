-- ================================================================
-- 数据中台菜单修复 & 权限按钮补充 SQL
-- 基于桌面文件 sys_menu_202604061044.sql 合并修正
-- ================================================================
--
-- 修复内容：
--   1. 数据源管理 组件路径和权限修正 (2302)
--   2. 数据源管理 权限按钮修正 & 补全 (23101-23107)
--   3. 其他模块 权限按钮补全（每个菜单页面对应的 F 型按钮）
--
-- 执行前备份：
--   CREATE TABLE sys_menu_bak_20260406_final AS SELECT * FROM sys_menu;
-- ================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- ================================================================
-- 一、数据源管理 核心修正 (menu_id=2302)
-- ================================================================

-- 1-1. 修正组件路径： datasource/list/index → system/datasource/index
UPDATE sys_menu
SET component = 'system/datasource/index'
WHERE menu_id = 2302 AND component = 'datasource/list/index';

-- 1-2. 修正权限字符串： datasource:list:query → system:ds:query
UPDATE sys_menu
SET perms = 'system:ds:query'
WHERE menu_id = 2302 AND perms = 'datasource:list:query';

-- ================================================================
-- 二、数据源管理 权限按钮修正 & 补全 (parent_id=2302)
-- ================================================================

-- 2-1. 删除旧的错误权限按钮（datasource:* 前缀）
DELETE FROM sys_menu WHERE menu_id IN (23101, 23102, 23103, 23104, 23105, 23106) AND perms LIKE 'datasource:%';

-- 2-2. 插入正确的数据源权限按钮（system:ds:* 前缀）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
-- 查询
(23101, '数据源查询', 2302, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:query', '#', 103, 1, NOW()),
-- 新增
(23102, '数据源新增', 2302, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:add', '#', 103, 1, NOW()),
-- 修改
(23103, '数据源修改', 2302, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:edit', '#', 103, 1, NOW()),
-- 删除
(23104, '数据源删除', 2302, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:remove', '#', 103, 1, NOW()),
-- 导出
(23105, '数据源导出', 2302, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:export', '#', 103, 1, NOW()),
-- 状态切换
(23106, '数据源状态', 2302, 6, '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:edit', '#', 103, 1, NOW()),
-- 测试连接（新增，之前缺失）
(23107, '数据源测试', 2302, 7, '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:test', '#', 103, 1, NOW());

-- ================================================================
-- 三、其他模块 权限按钮补全
-- ================================================================

-- ---------------------------------------------------------------
-- 3-1. 目录管理 (2304) 权限按钮 - metadata:catalog:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2304 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(23111, '目录查询', 2304, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:catalog:query', '#', 103, 1, NOW()),
(23112, '目录新增', 2304, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:catalog:add',   '#', 103, 1, NOW()),
(23113, '目录修改', 2304, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:catalog:edit',   '#', 103, 1, NOW()),
(23114, '目录删除', 2304, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:catalog:remove', '#', 103, 1, NOW()),
(23115, '目录导出', 2304, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:catalog:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 3-2. 数仓分层 (2305) 权限按钮 - metadata:layer:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2305 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(23121, '分层查询', 2305, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:layer:query', '#', 103, 1, NOW()),
(23122, '分层新增', 2305, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:layer:add',   '#', 103, 1, NOW()),
(23123, '分层修改', 2305, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:layer:edit',   '#', 103, 1, NOW()),
(23124, '分层删除', 2305, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:layer:remove', '#', 103, 1, NOW()),
(23125, '分层导出', 2305, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:layer:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 3-3. 数据域管理 (2306) 权限按钮 - metadata:domain:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2306 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(23131, '数据域查询', 2306, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:query', '#', 103, 1, NOW()),
(23132, '数据域新增', 2306, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:add',   '#', 103, 1, NOW()),
(23133, '数据域修改', 2306, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:edit',   '#', 103, 1, NOW()),
(23134, '数据域删除', 2306, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:remove', '#', 103, 1, NOW()),
(23135, '数据域导出', 2306, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 3-4. 元数据表 (2308) 权限按钮 - metadata:table:* + metadata:scan:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2308 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(23141, '表查询', 2308, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:query', '#', 103, 1, NOW()),
(23142, '表新增', 2308, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:add',   '#', 103, 1, NOW()),
(23143, '表修改', 2308, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:edit',   '#', 103, 1, NOW()),
(23144, '表删除', 2308, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:remove', '#', 103, 1, NOW()),
(23145, '表导出', 2308, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:export', '#', 103, 1, NOW()),
(23146, '表扫描', 2308, 6, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:exec',   '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 3-5. 字段管理 (2309) 权限按钮 - metadata:column:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2309 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(23151, '字段查询', 2309, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:column:query',  '#', 103, 1, NOW()),
(23152, '字段修改', 2309, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:column:edit',   '#', 103, 1, NOW()),
(23153, '字段导出', 2309, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:column:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 3-6. 扫描记录 (2311) 权限按钮 - metadata:scan:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2311 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(23161, '扫描查询', 2311, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:query',  '#', 103, 1, NOW()),
(23162, '扫描执行', 2311, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:exec',    '#', 103, 1, NOW()),
(23163, '扫描删除', 2311, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:remove', '#', 103, 1, NOW()),
(23164, '扫描导出', 2311, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:export',  '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 3-7. 扫描调度 (2312) 权限按钮 - metadata:schedule:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2312 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(23171, '调度查询', 2312, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:schedule:query',  '#', 103, 1, NOW()),
(23172, '调度新增', 2312, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:schedule:add',    '#', 103, 1, NOW()),
(23173, '调度修改', 2312, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:schedule:edit',   '#', 103, 1, NOW()),
(23174, '调度删除', 2312, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:schedule:remove', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 3-8. 探查任务 (2314) 权限按钮 - metadata:dprofile:task:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2314 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(23181, '任务查询', 2314, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:query',  '#', 103, 1, NOW()),
(23182, '任务新增', 2314, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:add',    '#', 103, 1, NOW()),
(23183, '任务修改', 2314, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:edit',   '#', 103, 1, NOW()),
(23184, '任务删除', 2314, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:remove', '#', 103, 1, NOW()),
(23185, '任务启动', 2314, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:start', '#', 103, 1, NOW()),
(23186, '任务停止', 2314, 6, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:stop',  '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 3-9. 探查报告 (2315) 权限按钮 - metadata:dprofile:report:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2315 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(23191, '报告查询', 2315, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:report:query',  '#', 103, 1, NOW()),
(23192, '报告导出', 2315, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:report:export', '#', 103, 1, NOW());

-- ================================================================
-- 四、数据治理模块 权限按钮 (parent_id: 2401~2423)
-- ================================================================

-- ---------------------------------------------------------------
-- 4-1. 规则模板 (2402) - metadata:template:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2402 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24101, '模板查询', 2402, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:template:query',  '#', 103, 1, NOW()),
(24102, '模板新增', 2402, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:template:add',    '#', 103, 1, NOW()),
(24103, '模板修改', 2402, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:template:edit',   '#', 103, 1, NOW()),
(24104, '模板删除', 2402, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:template:remove', '#', 103, 1, NOW()),
(24105, '模板导出', 2402, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:template:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-2. 规则定义 (2403) - metadata:rule:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2403 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24111, '规则查询', 2403, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rule:query',  '#', 103, 1, NOW()),
(24112, '规则新增', 2403, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rule:add',    '#', 103, 1, NOW()),
(24113, '规则修改', 2403, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rule:edit',   '#', 103, 1, NOW()),
(24114, '规则删除', 2403, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rule:remove', '#', 103, 1, NOW()),
(24115, '规则导出', 2403, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rule:export', '#', 103, 1, NOW()),
(24116, '规则绑定', 2403, 6, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rule:bind',  '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-3. 质检方案 (2404) - metadata:plan:* + metadata:plan:execute
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2404 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24121, '方案查询', 2404, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:plan:query',   '#', 103, 1, NOW()),
(24122, '方案新增', 2404, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:plan:add',     '#', 103, 1, NOW()),
(24123, '方案修改', 2404, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:plan:edit',    '#', 103, 1, NOW()),
(24124, '方案删除', 2404, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:plan:remove',  '#', 103, 1, NOW()),
(24125, '方案导出', 2404, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:plan:export',  '#', 103, 1, NOW()),
(24126, '方案执行', 2404, 6, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:plan:execute', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-4. 执行记录 (2405) - metadata:execution:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2405 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24131, '执行查询', 2405, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:execution:query',  '#', 103, 1, NOW()),
(24132, '执行停止', 2405, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:execution:stop',   '#', 103, 1, NOW()),
(24133, '执行重跑', 2405, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:execution:rerun',  '#', 103, 1, NOW()),
(24134, '执行详情', 2405, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:execution:detail', '#', 103, 1, NOW()),
(24135, '执行导出', 2405, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:execution:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-5. 质量评分 (2406) - metadata:score:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2406 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24141, '评分查询', 2406, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:score:query',  '#', 103, 1, NOW()),
(24142, '评分趋势', 2406, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:score:trend',  '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-6. 数据分类 (2409) - metadata:classification:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2409 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24151, '分类查询', 2409, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:classification:query',  '#', 103, 1, NOW()),
(24152, '分类新增', 2409, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:classification:add',    '#', 103, 1, NOW()),
(24153, '分类修改', 2409, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:classification:edit',   '#', 103, 1, NOW()),
(24154, '分类删除', 2409, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:classification:remove', '#', 103, 1, NOW()),
(24155, '分类导出', 2409, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:classification:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-7. 敏感等级 (2410) - metadata:level:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2410 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24161, '等级查询', 2410, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:level:query',  '#', 103, 1, NOW()),
(24162, '等级新增', 2410, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:level:add',    '#', 103, 1, NOW()),
(24163, '等级修改', 2410, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:level:edit',   '#', 103, 1, NOW()),
(24164, '等级删除', 2410, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:level:remove', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-8. 识别规则 (2411) - metadata:rules:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2411 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24171, '规则查询', 2411, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rules:query',  '#', 103, 1, NOW()),
(24172, '规则新增', 2411, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rules:add',    '#', 103, 1, NOW()),
(24173, '规则修改', 2411, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rules:edit',   '#', 103, 1, NOW()),
(24174, '规则删除', 2411, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rules:remove', '#', 103, 1, NOW()),
(24175, '规则导出', 2411, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:rules:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-9. 敏感字段 (2412) - metadata:sensitivity:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2412 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24181, '敏感查询', 2412, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:sensitivity:query',   '#', 103, 1, NOW()),
(24182, '敏感扫描', 2412, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:sensitivity:scan',     '#', 103, 1, NOW()),
(24183, '敏感修改', 2412, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:sensitivity:edit',     '#', 103, 1, NOW()),
(24184, '敏感删除', 2412, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:sensitivity:remove',   '#', 103, 1, NOW()),
(24185, '敏感确认', 2412, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:sensitivity:confirm', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-10. 脱敏模板 (2413) - metadata:maskTemplate:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2413 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24191, '模板查询', 2413, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:maskTemplate:query',  '#', 103, 1, NOW()),
(24192, '模板新增', 2413, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:maskTemplate:add',    '#', 103, 1, NOW()),
(24193, '模板修改', 2413, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:maskTemplate:edit',   '#', 103, 1, NOW()),
(24194, '模板删除', 2413, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:maskTemplate:remove', '#', 103, 1, NOW()),
(24195, '模板导出', 2413, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:maskTemplate:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-11. 脱敏策略 (2414) - metadata:strategy:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2414 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24201, '策略查询', 2414, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:strategy:query',  '#', 103, 1, NOW()),
(24202, '策略新增', 2414, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:strategy:add',    '#', 103, 1, NOW()),
(24203, '策略修改', 2414, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:strategy:edit',   '#', 103, 1, NOW()),
(24204, '策略删除', 2414, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:strategy:remove', '#', 103, 1, NOW()),
(24205, '策略导出', 2414, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:strategy:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-12. 脱敏查询 (2415) - metadata:maskQuery:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2415 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24211, '查询执行', 2415, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:maskQuery:exec',     '#', 103, 1, NOW()),
(24212, 'SQL验证', 2415, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:maskQuery:validate', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-13. 访问日志 (2416) - metadata:accessLog:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2416 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24221, '日志查询', 2416, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:accessLog:query',  '#', 103, 1, NOW()),
(24222, '日志导出', 2416, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:accessLog:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-14. 血缘管理 (2418) - metadata:lineage:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2418 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24231, '血缘查询', 2418, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:query',   '#', 103, 1, NOW()),
(24232, '血缘新增', 2418, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:add',     '#', 103, 1, NOW()),
(24233, '血缘修改', 2418, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:edit',    '#', 103, 1, NOW()),
(24234, '血缘删除', 2418, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:remove',  '#', 103, 1, NOW()),
(24235, '血缘导出', 2418, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:export', '#', 103, 1, NOW()),
(24236, '血缘图谱', 2418, 6, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:graph',  '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-15. 血缘图谱 (2419) - metadata:lineageGraph:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2419 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24241, '图谱查询', 2419, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineageGraph:query',     '#', 103, 1, NOW()),
(24242, '图谱上游', 2419, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineageGraph:upstream',   '#', 103, 1, NOW()),
(24243, '图谱下游', 2419, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineageGraph:downstream', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-16. 术语分类 (2421) - metadata:category:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2421 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24251, '分类查询', 2421, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:category:query',  '#', 103, 1, NOW()),
(24252, '分类新增', 2421, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:category:add',    '#', 103, 1, NOW()),
(24253, '分类修改', 2421, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:category:edit',   '#', 103, 1, NOW()),
(24254, '分类删除', 2421, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:category:remove', '#', 103, 1, NOW()),
(24255, '分类导出', 2421, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:category:export', '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-17. 术语管理 (2422) - metadata:term:* + metadata:term:publish
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2422 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24261, '术语查询', 2422, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:term:query',   '#', 103, 1, NOW()),
(24262, '术语新增', 2422, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:term:add',     '#', 103, 1, NOW()),
(24263, '术语修改', 2422, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:term:edit',    '#', 103, 1, NOW()),
(24264, '术语删除', 2422, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:term:remove',  '#', 103, 1, NOW()),
(24265, '术语发布', 2422, 5, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:term:publish', '#', 103, 1, NOW()),
(24266, '术语导出', 2422, 6, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:term:export',  '#', 103, 1, NOW());

-- ---------------------------------------------------------------
-- 4-18. 术语映射 (2423) - metadata:mapping:*
-- ---------------------------------------------------------------
DELETE FROM sys_menu WHERE parent_id = 2423 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24271, '映射查询', 2423, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:mapping:query',   '#', 103, 1, NOW()),
(24272, '映射新增', 2423, 2, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:mapping:add',     '#', 103, 1, NOW()),
(24273, '映射修改', 2423, 3, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:mapping:edit',    '#', 103, 1, NOW()),
(24274, '映射删除', 2423, 4, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:mapping:remove',  '#', 103, 1, NOW());

-- ================================================================
-- 五、数据中台 数据质量驾驶舱按钮 (parent_id=2407)
--    质量驾驶舱目前没有 F 型按钮，补入查看权限
-- ================================================================
DELETE FROM sys_menu WHERE parent_id = 2407 AND menu_type = 'F';
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, `path`, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time) VALUES
(24140, '驾驶舱查看', 2407, 1, '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dashboard:query', '#', 103, 1, NOW());

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;

-- ================================================================
-- 验证查询
-- ================================================================
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
-- WHERE menu_id BETWEEN 2300 AND 2500
-- ORDER BY menu_id;
