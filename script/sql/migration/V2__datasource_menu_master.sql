-- ============================================================
-- 数据源管理模块菜单初始化 SQL（master 库）
-- 目标数据库: master / ry_moban_v1
-- 说明: 本脚本仅负责菜单与角色菜单关系初始化
-- ============================================================

-- ----------------------------
-- 1. 菜单定义（参考 ry_vue_5.X.sql 菜单模式）
-- sys_menu 表字段: menu_id, menu_name, parent_id, order_num, path,
--                  component, query_param, is_frame, is_cache,
--                  menu_type, visible, status, perms, icon,
--                  create_dept, create_by, create_time, update_by, update_time, remark
-- ----------------------------

-- 顶级菜单：数据源资产（menu_id=2000，在一级菜单末尾）
insert into sys_menu values('2000', '数据源资产', '0', '6', 'dsasset', null, '', 1, 0, 'M', '0', '0', '', 'database', 103, 1, sysdate(), null, null, '数据源资产目录');

-- 二级菜单：数据源管理（menu_id=2001）
insert into sys_menu values('2001', '数据源管理', '2000', '1', 'datasource', 'system/datasource/index', '', 1, 0, 'C', '0', '0', 'system:ds:list', 'database', 103, 1, sysdate(), null, null, '数据源管理菜单');

-- 三级/按钮菜单（menu_id 2002-2008）
insert into sys_menu values('2002', '数据源查询', '2001', '1', '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2003', '数据源新增', '2001', '2', '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2004', '数据源修改', '2001', '3', '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2005', '数据源删除', '2001', '4', '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2006', '数据源导出', '2001', '5', '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2007', '数据源导入', '2001', '6', '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:import', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2008', '连接测试', '2001', '7', '#', '', '', 1, 0, 'F', '0', '0', 'system:ds:test', '#', 103, 1, sysdate(), null, null, '');

-- ----------------------------
-- 2. 角色菜单关联
-- 将菜单分配给超级管理员角色(role_id=1)和测试角色(role_id=3)
-- ----------------------------
insert into sys_role_menu values ('1', '2000');
insert into sys_role_menu values ('1', '2001');
insert into sys_role_menu values ('1', '2002');
insert into sys_role_menu values ('1', '2003');
insert into sys_role_menu values ('1', '2004');
insert into sys_role_menu values ('1', '2005');
insert into sys_role_menu values ('1', '2006');
insert into sys_role_menu values ('1', '2007');
insert into sys_role_menu values ('1', '2008');

insert into sys_role_menu values ('3', '2000');
insert into sys_role_menu values ('3', '2001');
insert into sys_role_menu values ('3', '2002');
insert into sys_role_menu values ('3', '2003');
insert into sys_role_menu values ('3', '2004');
insert into sys_role_menu values ('3', '2005');
insert into sys_role_menu values ('3', '2006');
insert into sys_role_menu values ('3', '2007');
insert into sys_role_menu values ('3', '2008');
