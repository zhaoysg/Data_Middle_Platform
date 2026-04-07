-- ============================================================
-- 数据中台菜单修复 SQL（基于 sys_menu_202604051745.sql）
--
-- 崩溃根因修复：
--   menu_id=2300 (资产目录) path=NULL → 导致后端 StringUtils.capitalize(NULL)="null"
--   生成无效路由名 "null2300" → Vue Router addRoute 崩溃
--   修复：给 2300 设置 path='catalog'（作为父目录不需要组件）
--
-- 其他修复：
--   2433/2434 血缘图谱 perms 修正
--   清理孤立按钮权限 2123/2124
--
-- 执行前备份：
--   CREATE TABLE sys_menu_bak_20260406 AS SELECT * FROM sys_menu;
--   CREATE TABLE sys_role_menu_bak_20260406 AS SELECT * FROM sys_role_menu;
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- ============================================================
-- Step 1: 修复核心问题 - menu_id=2300 path 不能为 NULL
--         资产目录是父级菜单(M类型)，path 不能为空
--         设置 path='catalog'（逻辑与资产目录对应）
-- ============================================================
UPDATE sys_menu
SET `path` = 'catalog', update_by = '1', update_time = NOW()
WHERE menu_id = 2300 AND (`path` IS NULL OR `path` = '');

-- ============================================================
-- Step 2: 血缘图谱按钮权限修正（复用 lineage 权限体系）
-- ============================================================
UPDATE sys_menu
SET perms = 'metadata:lineage:view', update_by = '1', update_time = NOW()
WHERE menu_id = 2433 AND perms = 'metadata:lineage:graph:view';

UPDATE sys_menu
SET perms = 'metadata:lineage:export', update_by = '1', update_time = NOW()
WHERE menu_id = 2434 AND perms = 'metadata:lineage:graph:export';

-- ============================================================
-- Step 3: 清理孤立按钮权限（parent_id 指向错误父菜单）
--         2123/2124 原属于 2100（元数据管理），但新架构下
--         数据域权限由 2310 的按钮提供（2311-2315）
-- ============================================================
DELETE FROM sys_role_menu WHERE menu_id IN (2123, 2124);
DELETE FROM sys_menu WHERE menu_id IN (2123, 2124);

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 验证查询：确认 menu_id=2300 的 path 已被修复
-- ============================================================
-- SELECT menu_id, menu_name, `path`, component, menu_type FROM sys_menu WHERE menu_id = 2300;
-- 预期：menu_id=2300, path='catalog'
