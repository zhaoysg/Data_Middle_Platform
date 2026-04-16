-- ============================================================
-- 数据中台菜单初始化脚本
-- 基于 ruoyi-vue-plus 框架的 sys_menu 表结构
-- ============================================================

-- ----------------------------
-- 数据中台一级目录
-- ----------------------------
insert into sys_menu values ('2000', '数据中台', '0', '6', 'metadata', null, '', 1, 0, 'M', '0', '0', '', 'database', 103, 1, sysdate(), null, null, '数据中台根目录');

-- ============================================================
-- 数据质量模块
-- ============================================================
-- 数据质量目录
insert into sys_menu values ('2001', '数据质量', '2000', '1', 'dqc', '', '', 1, 0, 'M', '0', '0', '', 'check-circle', 103, 1, sysdate(), null, null, '数据质量模块目录');

-- 质量看板
insert into sys_menu values ('2002', '质量看板', '2001', '1', 'dashboard', 'metadata/dqc/dashboard/index', '', 1, 1, 'C', '0', '0', 'metadata:dqc:dashboard:list', 'dashboard', 103, 1, sysdate(), null, null, '数据质量看板菜单');
-- 质量计划
insert into sys_menu values ('2003', '质量计划', '2001', '2', 'plan', 'metadata/dqc/plan/index', '', 1, 1, 'C', '0', '0', 'metadata:dqc:plan:list', 'schedule', 103, 1, sysdate(), null, null, '数据质量计划菜单');
-- 规则定义
insert into sys_menu values ('2004', '规则定义', '2001', '3', 'rule', 'metadata/dqc/rule/index', '', 1, 1, 'C', '0', '0', 'metadata:dqc:rule:list', 'rule', 103, 1, sysdate(), null, null, '数据质量规则定义菜单');
-- 规则模板
insert into sys_menu values ('2005', '规则模板', '2001', '4', 'template', 'metadata/dqc/template/index', '', 1, 1, 'C', '0', '0', 'metadata:dqc:template:list', 'template', 103, 1, sysdate(), null, null, '数据质量规则模板菜单');
-- 执行记录
insert into sys_menu values ('2006', '执行记录', '2001', '5', 'execution', 'metadata/dqc/execution/index', '', 1, 1, 'C', '0', '0', 'metadata:dqc:execution:list', 'history', 103, 1, sysdate(), null, null, '数据质量执行记录菜单');

-- 质量看板按钮
insert into sys_menu values ('2100', '质量看板查询', '2002', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:dashboard:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2101', '质量看板导出', '2002', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:dashboard:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2102', '质量看板刷新', '2002', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:dashboard:refresh', '#', 103, 1, sysdate(), null, null, '');

-- 质量计划按钮
insert into sys_menu values ('2103', '质量计划查询', '2003', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:plan:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2104', '质量计划新增', '2003', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:plan:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2105', '质量计划修改', '2003', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:plan:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2106', '质量计划删除', '2003', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:plan:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2107', '质量计划导出', '2003', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:plan:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2108', '质量计划执行', '2003', '6', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:plan:execute', '#', 103, 1, sysdate(), null, null, '');

-- 规则定义按钮
insert into sys_menu values ('2109', '规则定义查询', '2004', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:rule:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2110', '规则定义新增', '2004', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:rule:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2111', '规则定义修改', '2004', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:rule:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2112', '规则定义删除', '2004', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:rule:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2113', '规则定义导出', '2004', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:rule:export', '#', 103, 1, sysdate(), null, null, '');

-- 规则模板按钮
insert into sys_menu values ('2114', '规则模板查询', '2005', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:template:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2115', '规则模板新增', '2005', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:template:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2116', '规则模板修改', '2005', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:template:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2117', '规则模板删除', '2005', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:template:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2118', '规则模板导出', '2005', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:template:export', '#', 103, 1, sysdate(), null, null, '');

-- 执行记录按钮
insert into sys_menu values ('2119', '执行记录查询', '2006', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:execution:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2120', '执行记录详情', '2006', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:execution:detail', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2121', '执行记录导出', '2006', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:execution:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2122', '执行记录重试', '2006', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dqc:execution:retry', '#', 103, 1, sysdate(), null, null, '');

-- ============================================================
-- 数据安全模块
-- ============================================================
-- 数据安全目录
insert into sys_menu values ('2010', '数据安全', '2000', '2', 'security', '', '', 1, 0, 'M', '0', '0', '', 'shield', 103, 1, sysdate(), null, null, '数据安全模块目录');

-- 安全分级
insert into sys_menu values ('2011', '安全分级', '2010', '1', 'level', 'metadata/security/level/index', '', 1, 1, 'C', '0', '0', 'metadata:security:level:list', 'security', 103, 1, sysdate(), null, null, '安全分级菜单');
-- 敏感字段
insert into sys_menu values ('2012', '敏感字段', '2010', '2', 'sensitivity', 'metadata/security/sensitivity/index', '', 1, 1, 'C', '0', '0', 'metadata:security:sensitivity:list', 'field', 103, 1, sysdate(), null, null, '敏感字段菜单');
-- 分类分级
insert into sys_menu values ('2013', '分类分级', '2010', '3', 'classification', 'metadata/security/classification/index', '', 1, 1, 'C', '0', '0', 'metadata:security:classification:list', 'classify', 103, 1, sysdate(), null, null, '分类分级菜单');
-- 脱敏策略
insert into sys_menu values ('2014', '脱敏策略', '2010', '4', 'strategy', 'metadata/security/strategy/index', '', 1, 1, 'C', '0', '0', 'metadata:security:strategy:list', 'strategy', 103, 1, sysdate(), null, null, '脱敏策略菜单');
-- 脱敏模板
insert into sys_menu values ('2015', '脱敏模板', '2010', '5', 'maskTemplate', 'metadata/security/mask-template/index', '', 1, 1, 'C', '0', '0', 'metadata:security:maskTemplate:list', 'mask', 103, 1, sysdate(), null, null, '脱敏模板菜单');
-- 脱敏规则
insert into sys_menu values ('2016', '脱敏规则', '2010', '6', 'maskRule', 'metadata/security/rules/index', '', 1, 1, 'C', '0', '0', 'metadata:security:maskRule:list', 'rule', 103, 1, sysdate(), null, null, '脱敏规则菜单');
-- 脱敏查询
insert into sys_menu values ('2017', '脱敏查询', '2010', '7', 'maskQuery', 'metadata/security/mask-query/index', '', 1, 1, 'C', '0', '0', 'metadata:security:maskQuery:list', 'search', 103, 1, sysdate(), null, null, '脱敏查询菜单');
-- 访问日志
insert into sys_menu values ('2018', '访问日志', '2010', '8', 'accessLog', 'metadata/security/access-log/index', '', 1, 1, 'C', '0', '0', 'metadata:security:accessLog:list', 'log', 103, 1, sysdate(), null, null, '访问日志菜单');
-- 安全审计
insert into sys_menu values ('2019', '安全审计', '2010', '9', 'audit', 'metadata/security/audit/index', '', 1, 1, 'C', '0', '0', 'metadata:security:audit:list', 'audit', 103, 1, sysdate(), null, null, '安全审计菜单');

-- 安全分级按钮
insert into sys_menu values ('2200', '安全分级查询', '2011', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:level:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2201', '安全分级新增', '2011', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:level:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2202', '安全分级修改', '2011', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:level:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2203', '安全分级删除', '2011', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:level:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2204', '安全分级导出', '2011', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:level:export', '#', 103, 1, sysdate(), null, null, '');

-- 敏感字段按钮
insert into sys_menu values ('2205', '敏感字段查询', '2012', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:sensitivity:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2206', '敏感字段新增', '2012', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:sensitivity:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2207', '敏感字段修改', '2012', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:sensitivity:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2208', '敏感字段删除', '2012', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:sensitivity:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2209', '敏感字段导出', '2012', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:sensitivity:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2210', '敏感字段识别', '2012', '6', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:sensitivity:recognize', '#', 103, 1, sysdate(), null, null, '');

-- 分类分级按钮
insert into sys_menu values ('2211', '分类分级查询', '2013', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:classification:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2212', '分类分级新增', '2013', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:classification:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2213', '分类分级修改', '2013', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:classification:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2214', '分类分级删除', '2013', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:classification:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2215', '分类分级导出', '2013', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:classification:export', '#', 103, 1, sysdate(), null, null, '');

-- 脱敏策略按钮
insert into sys_menu values ('2216', '脱敏策略查询', '2014', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:strategy:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2217', '脱敏策略新增', '2014', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:strategy:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2218', '脱敏策略修改', '2014', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:strategy:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2219', '脱敏策略删除', '2014', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:strategy:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2220', '脱敏策略导出', '2014', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:strategy:export', '#', 103, 1, sysdate(), null, null, '');

-- 脱敏模板按钮
insert into sys_menu values ('2221', '脱敏模板查询', '2015', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskTemplate:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2222', '脱敏模板新增', '2015', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskTemplate:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2223', '脱敏模板修改', '2015', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskTemplate:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2224', '脱敏模板删除', '2015', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskTemplate:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2225', '脱敏模板导出', '2015', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskTemplate:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2226', '脱敏模板预览', '2015', '6', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskTemplate:preview', '#', 103, 1, sysdate(), null, null, '');

-- 脱敏规则按钮
insert into sys_menu values ('2227', '脱敏规则查询', '2016', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskRule:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2228', '脱敏规则新增', '2016', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskRule:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2229', '脱敏规则修改', '2016', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskRule:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2230', '脱敏规则删除', '2016', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskRule:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2231', '脱敏规则导出', '2016', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskRule:export', '#', 103, 1, sysdate(), null, null, '');

-- 脱敏查询按钮
insert into sys_menu values ('2232', '脱敏查询', '2017', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskQuery:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2233', '脱敏导出', '2017', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:maskQuery:export', '#', 103, 1, sysdate(), null, null, '');

-- 访问日志按钮
insert into sys_menu values ('2234', '访问日志查询', '2018', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:accessLog:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2235', '访问日志导出', '2018', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:accessLog:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2236', '访问日志详情', '2018', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:accessLog:detail', '#', 103, 1, sysdate(), null, null, '');

-- 安全审计按钮
insert into sys_menu values ('2237', '安全审计查询', '2019', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:audit:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2238', '安全审计导出', '2019', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:security:audit:export', '#', 103, 1, sysdate(), null, null, '');

-- ============================================================
-- 数据探查模块
-- ============================================================
-- 数据探查目录
insert into sys_menu values ('2020', '数据探查', '2000', '3', 'dprofile', '', '', 1, 0, 'M', '0', '0', '', 'search', 103, 1, sysdate(), null, null, '数据探查模块目录');

-- 探查任务
insert into sys_menu values ('2021', '探查任务', '2020', '1', 'task', 'metadata/dprofile/task/index', '', 1, 1, 'C', '0', '0', 'metadata:dprofile:task:list', 'task', 103, 1, sysdate(), null, null, '数据探查任务菜单');
-- 探查报告
insert into sys_menu values ('2022', '探查报告', '2020', '2', 'report', 'metadata/dprofile/report/index', '', 1, 1, 'C', '0', '0', 'metadata:dprofile:report:list', 'report', 103, 1, sysdate(), null, null, '数据探查报告菜单');

-- 探查任务按钮
insert into sys_menu values ('2300', '探查任务查询', '2021', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2301', '探查任务新增', '2021', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2302', '探查任务修改', '2021', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2303', '探查任务删除', '2021', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2304', '探查任务导出', '2021', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2305', '探查任务执行', '2021', '6', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:task:execute', '#', 103, 1, sysdate(), null, null, '');

-- 探查报告按钮
insert into sys_menu values ('2306', '探查报告查询', '2022', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:report:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2307', '探查报告详情', '2022', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:report:detail', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2308', '探查报告导出', '2022', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:report:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2309', '探查报告删除', '2022', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:dprofile:report:remove', '#', 103, 1, sysdate(), null, null, '');

-- ============================================================
-- 血缘探查模块
-- ============================================================
-- 血缘探查目录
insert into sys_menu values ('2025', '血缘探查', '2000', '4', 'lineage', '', '', 1, 0, 'M', '0', '0', '', 'node', 103, 1, sysdate(), null, null, '血缘探查模块目录');

-- 血缘关系
insert into sys_menu values ('2026', '血缘关系', '2025', '1', 'lineage', 'metadata/lineage/index', '', 1, 1, 'C', '0', '0', 'metadata:lineage:list', 'tree', 103, 1, sysdate(), null, null, '血缘关系菜单');
-- 血缘图谱
insert into sys_menu values ('2027', '血缘图谱', '2025', '2', 'graph', 'metadata/lineage/graph/index', '', 1, 1, 'C', '0', '0', 'metadata:lineage:graph:list', 'graph', 103, 1, sysdate(), null, null, '血缘图谱菜单');

-- 血缘关系按钮
insert into sys_menu values ('2400', '血缘关系查询', '2026', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2401', '血缘关系新增', '2026', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2402', '血缘关系修改', '2026', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2403', '血缘关系删除', '2026', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2404', '血缘关系导出', '2026', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2405', '血缘关系刷新', '2026', '6', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:refresh', '#', 103, 1, sysdate(), null, null, '');

-- 血缘图谱按钮
insert into sys_menu values ('2406', '血缘图谱查看', '2027', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:graph:view', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2407', '血缘图谱导出', '2027', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:graph:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2408', '血缘图谱放大', '2027', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:graph:zoomIn', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2409', '血缘图谱缩小', '2027', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:lineage:graph:zoomOut', '#', 103, 1, sysdate(), null, null, '');

-- ============================================================
-- 元数据管理模块
-- ============================================================
-- 元数据管理目录
insert into sys_menu values ('2030', '元数据管理', '2000', '5', 'metadata', '', '', 1, 0, 'M', '0', '0', '', 'table', 103, 1, sysdate(), null, null, '元数据管理模块目录');

-- 表管理
insert into sys_menu values ('2031', '表管理', '2030', '1', 'table', 'metadata/table/index', '', 1, 1, 'C', '0', '0', 'metadata:table:list', 'table', 103, 1, sysdate(), null, null, '元数据表管理菜单');
-- 分层管理
insert into sys_menu values ('2032', '分层管理', '2030', '2', 'layer', 'metadata/layer/index', '', 1, 1, 'C', '0', '0', 'metadata:layer:list', 'layers', 103, 1, sysdate(), null, null, '元数据分层管理菜单');
-- 目录管理
insert into sys_menu values ('2033', '目录管理', '2030', '3', 'catalog', 'metadata/catalog/index', '', 1, 1, 'C', '0', '0', 'metadata:catalog:list', 'folder', 103, 1, sysdate(), null, null, '元数据目录管理菜单');
-- 域管理
insert into sys_menu values ('2034', '域管理', '2030', '4', 'domain', 'metadata/domain/index', '', 1, 1, 'C', '0', '0', 'metadata:domain:list', 'domain', 103, 1, sysdate(), null, null, '元数据域管理菜单');

-- 表管理按钮
insert into sys_menu values ('2500', '表管理查询', '2031', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2501', '表管理新增', '2031', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2502', '表管理修改', '2031', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2503', '表管理删除', '2031', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2504', '表管理导出', '2031', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2505', '表管理详情', '2031', '6', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:detail', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2506', '表管理同步', '2031', '7', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:table:sync', '#', 103, 1, sysdate(), null, null, '');

-- 分层管理按钮
insert into sys_menu values ('2507', '分层管理查询', '2032', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:layer:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2508', '分层管理新增', '2032', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:layer:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2509', '分层管理修改', '2032', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:layer:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2510', '分层管理删除', '2032', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:layer:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2511', '分层管理导出', '2032', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:layer:export', '#', 103, 1, sysdate(), null, null, '');

-- 目录管理按钮
insert into sys_menu values ('2512', '目录管理查询', '2033', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:catalog:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2513', '目录管理新增', '2033', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:catalog:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2514', '目录管理修改', '2033', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:catalog:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2515', '目录管理删除', '2033', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:catalog:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2516', '目录管理导出', '2033', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:catalog:export', '#', 103, 1, sysdate(), null, null, '');

-- 域管理按钮
insert into sys_menu values ('2517', '域管理查询', '2034', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2518', '域管理新增', '2034', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2519', '域管理修改', '2034', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2520', '域管理删除', '2034', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2521', '域管理导出', '2034', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:domain:export', '#', 103, 1, sysdate(), null, null, '');

-- ============================================================
-- 数据标准模块
-- ============================================================
-- 数据标准目录
insert into sys_menu values ('2040', '数据标准', '2000', '6', 'glossary', '', '', 1, 0, 'M', '0', '0', '', 'book', 103, 1, sysdate(), null, null, '数据标准模块目录');

-- 术语管理
insert into sys_menu values ('2041', '术语管理', '2040', '1', 'term', 'metadata/glossary/term/index', '', 1, 1, 'C', '0', '0', 'metadata:glossary:term:list', 'term', 103, 1, sysdate(), null, null, '数据标准术语管理菜单');
-- 术语映射
insert into sys_menu values ('2042', '术语映射', '2040', '2', 'mapping', 'metadata/glossary/mapping/index', '', 1, 1, 'C', '0', '0', 'metadata:glossary:mapping:list', 'mapping', 103, 1, sysdate(), null, null, '数据标准术语映射菜单');
-- 分类管理
insert into sys_menu values ('2043', '分类管理', '2040', '3', 'category', 'metadata/glossary/category/index', '', 1, 1, 'C', '0', '0', 'metadata:glossary:category:list', 'category', 103, 1, sysdate(), null, null, '数据标准分类管理菜单');

-- 术语管理按钮
insert into sys_menu values ('2600', '术语管理查询', '2041', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:term:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2601', '术语管理新增', '2041', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:term:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2602', '术语管理修改', '2041', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:term:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2603', '术语管理删除', '2041', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:term:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2604', '术语管理导出', '2041', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:term:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2605', '术语管理审核', '2041', '6', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:term:approve', '#', 103, 1, sysdate(), null, null, '');

-- 术语映射按钮
insert into sys_menu values ('2606', '术语映射查询', '2042', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:mapping:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2607', '术语映射新增', '2042', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:mapping:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2608', '术语映射修改', '2042', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:mapping:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2609', '术语映射删除', '2042', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:mapping:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2610', '术语映射导出', '2042', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:mapping:export', '#', 103, 1, sysdate(), null, null, '');

-- 分类管理按钮
insert into sys_menu values ('2611', '分类管理查询', '2043', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:category:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2612', '分类管理新增', '2043', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:category:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2613', '分类管理修改', '2043', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:category:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2614', '分类管理删除', '2043', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:category:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2615', '分类管理导出', '2043', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:glossary:category:export', '#', 103, 1, sysdate(), null, null, '');

-- ============================================================
-- 数据扫描模块
-- ============================================================
-- 数据扫描目录
insert into sys_menu values ('2050', '数据扫描', '2000', '7', 'scan', '', '', 1, 0, 'M', '0', '0', '', 'scan', 103, 1, sysdate(), null, null, '数据扫描模块目录');

-- 扫描任务
insert into sys_menu values ('2051', '扫描任务', '2050', '1', 'scan', 'metadata/scan/index', '', 1, 1, 'C', '0', '0', 'metadata:scan:list', 'scan', 103, 1, sysdate(), null, null, '数据扫描任务菜单');
-- 扫描调度
insert into sys_menu values ('2052', '扫描调度', '2050', '2', 'scanSchedule', 'metadata/scan-schedule/index', '', 1, 1, 'C', '0', '0', 'metadata:scan:schedule:list', 'schedule', 103, 1, sysdate(), null, null, '数据扫描调度菜单');

-- 扫描任务按钮
insert into sys_menu values ('2700', '扫描任务查询', '2051', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2701', '扫描任务新增', '2051', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2702', '扫描任务修改', '2051', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2703', '扫描任务删除', '2051', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2704', '扫描任务导出', '2051', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2705', '扫描任务执行', '2051', '6', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:execute', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2706', '扫描任务停止', '2051', '7', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:stop', '#', 103, 1, sysdate(), null, null, '');

-- 扫描调度按钮
insert into sys_menu values ('2707', '扫描调度查询', '2052', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:schedule:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2708', '扫描调度新增', '2052', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:schedule:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2709', '扫描调度修改', '2052', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:schedule:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2710', '扫描调度删除', '2052', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:schedule:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2711', '扫描调度导出', '2052', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:schedule:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2712', '扫描调度启用', '2052', '6', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:schedule:enable', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2713', '扫描调度禁用', '2052', '7', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:scan:schedule:disable', '#', 103, 1, sysdate(), null, null, '');

-- ============================================================
-- 数据源管理模块
-- ============================================================
-- 数据源管理目录
insert into sys_menu values ('2060', '数据源管理', '2000', '6', 'datasource', '', '', 1, 0, 'M', '0', '0', '', 'database', 103, 1, sysdate(), null, null, '数据源管理模块目录');

-- 数据源列表
insert into sys_menu values ('2061', '数据源列表', '2060', '1', 'datasource', 'system/datasource/index', '', 1, 1, 'C', '0', '0', 'metadata:datasource:list', 'database', 103, 1, sysdate(), null, null, '数据源列表菜单');

-- 数据源列表按钮
insert into sys_menu values ('2800', '数据源查询', '2061', '1', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:datasource:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2801', '数据源新增', '2061', '2', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:datasource:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2802', '数据源修改', '2061', '3', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:datasource:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2803', '数据源删除', '2061', '4', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:datasource:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2804', '数据源导出', '2061', '5', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:datasource:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2805', '数据源测试', '2061', '6', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:datasource:test', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('2806', '数据源详情', '2061', '7', '#', '', '', 1, 0, 'F', '0', '0', 'metadata:datasource:detail', '#', 103, 1, sysdate(), null, null, '');

-- ============================================================
-- 将数据中台菜单授权给超级管理员角色
-- ============================================================
-- 超级管理员角色(1)获得数据中台所有菜单权限
insert into sys_role_menu (select 1, menu_id from sys_menu where menu_id >= 2000);

-- ============================================================
-- 将数据中台菜单授权给本部门及以下角色(3)
-- ============================================================
insert into sys_role_menu (select 3, menu_id from sys_menu where menu_id >= 2000);
