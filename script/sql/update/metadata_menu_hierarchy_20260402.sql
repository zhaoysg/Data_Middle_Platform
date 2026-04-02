-- 元数据菜单层级重构
-- 目标：
-- 1. 将“数据脱敏(2110)”和“数据探查(2120)”从“元数据平台(2100)”下提升为一级菜单
-- 2. 对齐当前前端代码中的顶级页面包装路由：views/security/*、views/dprofile/*
-- 3. 统一数据治理相关一级菜单排序，避免 order_num 冲突
-- 4. 保留既有 menu_id/perms，避免影响 sys_role_menu 授权关系
--
-- 说明：
-- 1. 本脚本只调整 sys_menu，不删除菜单，不改 sys_role_menu
-- 2. 执行后，菜单访问路径会从 /metadata/security/*、/metadata/dprofile/*
--    调整为 /security/*、/dprofile/*
-- 3. 2123/2124 为历史残留的“数据域”按钮权限，当前并无独立菜单页，
--    先从“数据探查”节点下移出，挂回“元数据平台”以避免错误归属

START TRANSACTION;

SET @menu_update_time = NOW();
SET @menu_update_by = 1;

-- 一级菜单顺序重排，保持数据治理菜单并列展示
UPDATE sys_menu
SET order_num = 10,
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2100;

UPDATE sys_menu
SET parent_id = 0,
    order_num = 11,
    path = 'security',
    component = NULL,
    menu_type = 'M',
    icon = 'safety-certificate',
    remark = '数据脱敏目录',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2110;

UPDATE sys_menu
SET parent_id = 0,
    order_num = 12,
    path = 'dprofile',
    component = NULL,
    menu_type = 'M',
    icon = 'database-search',
    remark = '数据探查目录',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2120;

UPDATE sys_menu
SET order_num = 13,
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2230;

UPDATE sys_menu
SET order_num = 14,
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2250;

-- 数据脱敏子菜单归一化
UPDATE sys_menu
SET parent_id = 2110,
    order_num = 1,
    path = 'classification',
    component = 'security/classification/index',
    remark = '分类分级页面',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2112;

UPDATE sys_menu
SET parent_id = 2110,
    order_num = 2,
    path = 'rules',
    component = 'security/rules/index',
    remark = '敏感识别规则页面',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2113;

UPDATE sys_menu
SET parent_id = 2110,
    order_num = 3,
    path = 'mask-template',
    component = 'security/mask-template/index',
    remark = '脱敏模板页面',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2114;

UPDATE sys_menu
SET parent_id = 2110,
    order_num = 4,
    path = 'sensitivity',
    component = 'security/sensitivity/index',
    remark = '敏感字段页面',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2115;

UPDATE sys_menu
SET parent_id = 2110,
    order_num = 5,
    path = 'strategy',
    component = 'security/strategy/index',
    remark = '脱敏策略页面',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2116;

UPDATE sys_menu
SET parent_id = 2110,
    order_num = 6,
    path = 'mask-query',
    component = 'security/mask-query/index',
    remark = '脱敏查询页面',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2111;

UPDATE sys_menu
SET parent_id = 2110,
    order_num = 7,
    path = 'audit',
    component = 'security/audit/index',
    remark = '脱敏访问日志页面',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2117;

-- 数据探查子菜单归一化
UPDATE sys_menu
SET parent_id = 2120,
    order_num = 1,
    path = 'task',
    component = 'dprofile/task/index',
    remark = '数据探查任务页面',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2121;

UPDATE sys_menu
SET parent_id = 2120,
    order_num = 2,
    path = 'report',
    component = 'dprofile/report/index',
    remark = '数据探查报告页面',
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2122;

-- 历史孤儿权限先挂回元数据平台，避免错误归属到“数据探查”
UPDATE sys_menu
SET parent_id = 2100,
    order_num = 90,
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2123;

UPDATE sys_menu
SET parent_id = 2100,
    order_num = 91,
    update_by = @menu_update_by,
    update_time = @menu_update_time
WHERE menu_id = 2124;

COMMIT;

-- 建议执行后核对
-- SELECT menu_id, menu_name, parent_id, order_num, path, component
-- FROM sys_menu
-- WHERE menu_id IN (2100, 2110, 2111, 2112, 2113, 2114, 2115, 2116, 2117, 2120, 2121, 2122, 2123, 2124, 2230, 2250)
-- ORDER BY parent_id, order_num, menu_id;
