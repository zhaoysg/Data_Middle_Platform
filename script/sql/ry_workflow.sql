-- ----------------------------
-- 0、warm-flow-all.sql，地址：https://gitee.com/dromara/warm-flow/blob/master/sql/mysql/warm-flow-all.sql
-- ----------------------------
CREATE TABLE `flow_definition`
(
    `id`              bigint          NOT NULL COMMENT '主键id',
    `flow_code`       varchar(40)     NOT NULL COMMENT '流程编码',
    `flow_name`       varchar(100)    NOT NULL COMMENT '流程名称',
    `model_value`     varchar(40)     NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
    `category`        varchar(100)             DEFAULT NULL COMMENT '流程类别',
    `version`         varchar(20)     NOT NULL COMMENT '流程版本',
    `is_publish`      tinyint(1)      NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_custom`     char(1)                  DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)             DEFAULT NULL COMMENT '审批表单路径',
    `activity_status` tinyint(1)      NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `listener_type`   varchar(100)             DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)             DEFAULT NULL COMMENT '监听器路径',
    `ext`             varchar(500)             DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`     datetime                 DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime                 DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`        char(1)                  DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)              DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程定义表';

CREATE TABLE `flow_node`
(
    `id`              bigint        NOT NULL COMMENT '主键id',
    `node_type`       tinyint(1)      NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `definition_id`   bigint          NOT NULL COMMENT '流程定义id',
    `node_code`       varchar(100)    NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)  DEFAULT NULL COMMENT '流程节点名称',
    `permission_flag` varchar(200)  DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
    `node_ratio`      varchar(200)  DEFAULT NULL COMMENT '流程签署比例值',
    `coordinate`      varchar(100)  DEFAULT NULL COMMENT '坐标',
    `any_node_skip`   varchar(100)  DEFAULT NULL COMMENT '任意结点跳转',
    `listener_type`   varchar(100)  DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)  DEFAULT NULL COMMENT '监听器路径',
    `form_custom`     char(1)       DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)  DEFAULT NULL COMMENT '审批表单路径',
    `version`         varchar(20)     NOT NULL COMMENT '版本',
    `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             text          COMMENT '节点扩展属性',
    `del_flag`        char(1)       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程节点表';

CREATE TABLE `flow_skip`
(
    `id`             bigint       NOT NULL COMMENT '主键id',
    `definition_id`  bigint          NOT NULL COMMENT '流程定义id',
    `now_node_code`  varchar(100)    NOT NULL COMMENT '当前流程节点的编码',
    `now_node_type`  tinyint(1)   DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `next_node_code` varchar(100)    NOT NULL COMMENT '下一个流程节点的编码',
    `next_node_type` tinyint(1)   DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `skip_name`      varchar(100) DEFAULT NULL COMMENT '跳转名称',
    `skip_type`      varchar(40)  DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
    `skip_condition` varchar(200) DEFAULT NULL COMMENT '跳转条件',
    `coordinate`     varchar(100) DEFAULT NULL COMMENT '坐标',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`       char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`      varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='节点跳转关联表';

CREATE TABLE `flow_instance`
(
    `id`              bigint      NOT NULL COMMENT '主键id',
    `definition_id`   bigint      NOT NULL COMMENT '对应flow_definition表的id',
    `business_id`     varchar(40) NOT NULL COMMENT '业务id',
    `node_type`       tinyint(1)  NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `node_code`       varchar(40) NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)         DEFAULT NULL COMMENT '流程节点名称',
    `variable`        text COMMENT '任务变量',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `activity_status` tinyint(1)  NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `def_json`        text COMMENT '流程定义json',
    `create_time`     datetime             DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime             DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             varchar(500)         DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
    `del_flag`        char(1)              DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)          DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程实例表';

CREATE TABLE `flow_task`
(
    `id`            bigint       NOT NULL COMMENT '主键id',
    `definition_id` bigint       NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`   bigint       NOT NULL COMMENT '对应flow_instance表的id',
    `node_code`     varchar(100) NOT NULL COMMENT '节点编码',
    `node_name`     varchar(100) DEFAULT NULL COMMENT '节点名称',
    `node_type`     tinyint(1)   NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `flow_status`   varchar(20)  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`   char(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`     varchar(100) DEFAULT NULL COMMENT '审批表单路径',
    `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='待办任务表';

CREATE TABLE `flow_his_task`
(
    `id`               bigint(20)                   NOT NULL COMMENT '主键id',
    `definition_id`    bigint(20)                   NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`      bigint(20)                   NOT NULL COMMENT '对应flow_instance表的id',
    `task_id`          bigint(20)                   NOT NULL COMMENT '对应flow_task表的id',
    `node_code`        varchar(100)                 DEFAULT NULL COMMENT '开始节点编码',
    `node_name`        varchar(100)                 DEFAULT NULL COMMENT '开始节点名称',
    `node_type`        tinyint(1)                   DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `target_node_code` varchar(200)                 DEFAULT NULL COMMENT '目标节点编码',
    `target_node_name` varchar(200)                 DEFAULT NULL COMMENT '结束节点名称',
    `approver`         varchar(40)                  DEFAULT NULL COMMENT '审批人',
    `cooperate_type`   tinyint(1)                   NOT NULL DEFAULT '0' COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
    `collaborator`     varchar(500)                 DEFAULT NULL COMMENT '协作人',
    `skip_type`        varchar(10)                  NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
    `flow_status`      varchar(20)                  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`      char(1)                      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`        varchar(100)                 DEFAULT NULL COMMENT '审批表单路径',
    `message`          varchar(500)                 DEFAULT NULL COMMENT '审批意见',
    `variable`         TEXT                         DEFAULT NULL COMMENT '任务变量',
    `ext`              TEXT                         DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`      datetime                     DEFAULT NULL COMMENT '任务开始时间',
    `update_time`      datetime                     DEFAULT NULL COMMENT '审批完成时间',
    `del_flag`         char(1)                      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`        varchar(40)                  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='历史任务记录表';


CREATE TABLE `flow_user`
(
    `id`           bigint      NOT NULL COMMENT '主键id',
    `type`         char(1)         NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
    `processed_by` varchar(80) DEFAULT NULL COMMENT '权限人',
    `associated`   bigint          NOT NULL COMMENT '任务表id',
    `create_time`  datetime    DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(80) DEFAULT NULL COMMENT '创建人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `del_flag`     char(1)     DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40) DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `user_processed_type` (`processed_by`, `type`),
    KEY `user_associated` (`associated`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程用户表';

-- ----------------------------
-- 流程分类表
-- ----------------------------
create table flow_category
(
    category_id   bigint(20)  not null comment '流程分类ID',
    tenant_id     varchar(20)  default '000000' comment '租户编号',
    parent_id     bigint(20)   default 0 comment '父流程分类id',
    ancestors     varchar(500) default '' comment '祖级列表',
    category_name varchar(30) not null comment '流程分类名称',
    order_num     int(4)       default 0 comment '显示顺序',
    del_flag      char(1)      default '0' comment '删除标志（0代表存在 1代表删除）',
    create_dept   bigint(20)  null comment '创建部门',
    create_by     bigint(20)  null comment '创建者',
    create_time   datetime    null comment '创建时间',
    update_by     bigint(20)  null comment '更新者',
    update_time   datetime    null comment '更新时间',
    primary key (category_id)
) engine = innodb comment = '流程分类';

INSERT INTO flow_category values (100, '000000', 0, '0', 'OA审批', 0, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (101, '000000', 100, '0,100', '假勤管理', 0, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (102, '000000', 100, '0,100', '人事管理', 1, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (103, '000000', 101, '0,100,101', '请假', 0, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (104, '000000', 101, '0,100,101', '出差', 1, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (105, '000000', 101, '0,100,101', '加班', 2, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (106, '000000', 101, '0,100,101', '换班', 3, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (107, '000000', 101, '0,100,101', '外出', 4, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (108, '000000', 102, '0,100,102', '转正', 1, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (109, '000000', 102, '0,100,102', '离职', 2, '0', 103, 1, sysdate(), null, null);

-- ----------------------------
-- 流程spel表达式定义表
-- ----------------------------

CREATE TABLE flow_spel (
    id bigint(20) NOT NULL COMMENT '主键id',
    component_name varchar(255) DEFAULT NULL COMMENT '组件名称',
    method_name varchar(255) DEFAULT NULL COMMENT '方法名',
    method_params varchar(255) DEFAULT NULL COMMENT '参数',
    view_spel varchar(255) DEFAULT NULL COMMENT '预览spel表达式',
    remark varchar(255) DEFAULT NULL COMMENT '备注',
    status char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    del_flag char(1) DEFAULT '0' COMMENT '删除标志',
    create_dept bigint(20) DEFAULT NULL COMMENT '创建部门',
    create_by bigint(20) DEFAULT NULL COMMENT '创建者',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    update_by bigint(20) DEFAULT NULL COMMENT '更新者',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT='流程spel表达式定义表';

INSERT INTO flow_spel VALUES (1, 'spelRuleComponent', 'selectDeptLeaderById', 'initiatorDeptId', '#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}', '根据部门id获取部门负责人', '0', '0', 103, 1, sysdate(), 1, sysdate());
INSERT INTO flow_spel VALUES (2, NULL, NULL, 'initiator', '${initiator}', '流程发起人', '0', '0', 103, 1, sysdate(), 1, sysdate());

-- ----------------------------
-- 流程实例业务扩展表
-- ----------------------------

create table flow_instance_biz_ext (
    id             bigint                       not null comment '主键id',
    tenant_id      varchar(20) default '000000' null comment '租户编号',
    create_dept    bigint                       null comment '创建部门',
    create_by      bigint                       null comment '创建者',
    create_time    datetime                     null comment '创建时间',
    update_by      bigint                       null comment '更新者',
    update_time    datetime                     null comment '更新时间',
    business_code  varchar(255)                 null comment '业务编码',
    business_title varchar(1000)                null comment '业务标题',
    del_flag       char        default '0'      null comment '删除标志（0代表存在 1代表删除）',
    instance_id    bigint                       null comment '流程实例Id',
    business_id    varchar(255)                 null comment '业务Id',
    PRIMARY KEY (id)
)  ENGINE = InnoDB COMMENT '流程实例业务扩展表';

-- ----------------------------
-- 请假单信息
-- ----------------------------

create table test_leave
(
    id          bigint(20)   not null comment 'id',
    tenant_id   varchar(20)  default '000000' comment '租户编号',
    apply_code  varchar(50)  not null comment '申请编号',
    leave_type  varchar(255) not null comment '请假类型',
    start_date  datetime     not null comment '开始时间',
    end_date    datetime     not null comment '结束时间',
    leave_days  int(10)      not null comment '请假天数',
    remark      varchar(255) null comment '请假原因',
    status      varchar(255) null comment '状态',
    create_dept bigint       null comment '创建部门',
    create_by   bigint       null comment '创建者',
    create_time datetime     null comment '创建时间',
    update_by   bigint       null comment '更新者',
    update_time datetime     null comment '更新时间',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB COMMENT = '请假申请表';

insert into sys_menu values ('11616', '工作流', '0', '6', 'workflow', '', '', '1', '0', 'M', '0', '0', '', 'workflow', 103, 1, sysdate(),NULL, NULL, '');
insert into sys_menu values ('11618', '我的任务', '0', '7', 'task', '', '', '1', '0', 'M', '0', '0', '', 'my-task', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11619', '我的待办', '11618', '2', 'taskWaiting', 'workflow/task/taskWaiting', '', '1', '1', 'C', '0', '0', '', 'waiting', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11632', '我的已办', '11618', '3', 'taskFinish', 'workflow/task/taskFinish', '', '1', '1', 'C', '0', '0', '', 'finish', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11633', '我的抄送', '11618', '4', 'taskCopyList', 'workflow/task/taskCopyList', '', '1', '1', 'C', '0', '0', '', 'my-copy', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11620', '流程定义', '11616', '3', 'processDefinition', 'workflow/processDefinition/index', '', '1', '1', 'C', '0', '0', 'workflow:definition:list', 'process-definition', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11621', '流程实例', '11630', '1', 'processInstance', 'workflow/processInstance/index', '', '1', '1', 'C', '0', '0', 'workflow:instance:list', 'tree-table', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11622', '流程分类', '11616', '1', 'category', 'workflow/category/index', '', '1', '0', 'C', '0', '0', 'workflow:category:list', 'category', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11801', '流程表达式', '11616', '2', 'spel',    'workflow/spel/index', '', 1, 0, 'C', '0', '0', 'workflow:spel:list', 'input', 103, 1, sysdate(), 1, sysdate(), '流程达式定义菜单');
insert into sys_menu values ('11629', '我发起的', '11618', '1', 'myDocument', 'workflow/task/myDocument', '', '1', '1', 'C', '0', '0', 'workflow:instance:currentList', 'guide', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11630', '流程监控', '11616', '4', 'processMonitor', '', '', '1', '0', 'M', '0', '0', '', 'monitor', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11631', '待办任务', '11630', '2', 'allTaskWaiting', 'workflow/task/allTaskWaiting', '', '1', '1', 'C', '0', '0', '', 'waiting', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11700', '流程设计', '11616', '5', 'design/index',   'workflow/processDefinition/design', '', 1, 1, 'C', '1', '0', 'workflow:leave:edit', '#', 103, 1, sysdate(), null, null, '/workflow/processDefinition');
insert into sys_menu values ('11701', '请假申请', '11616', '6', 'leaveEdit/index', 'workflow/leave/leaveEdit', '', 1, 1, 'C', '1', '0', 'workflow:leave:edit', '#', 103, 1, sysdate(), null, null, '');
-- 流程分类管理相关按钮
insert into sys_menu values ('11623', '流程分类查询', '11622', '1', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:query', '#', 103, 1,sysdate(), null, null, '');
insert into sys_menu values ('11624', '流程分类新增', '11622', '2', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:add', '#', 103, 1,sysdate(), null, null, '');
insert into sys_menu values ('11625', '流程分类修改', '11622', '3', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:edit', '#', 103, 1,sysdate(), null, null, '');
insert into sys_menu values ('11626', '流程分类删除', '11622', '4', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:remove', '#', 103,1, sysdate(), null, null, '');
insert into sys_menu values ('11627', '流程分类导出', '11622', '5', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:export', '#', 103,1, sysdate(), null, null, '');

-- 流程实例管理相关按钮
insert into sys_menu values ('11653', '流程实例查询', '11621', '1', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:instance:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11654', '流程变量查询', '11621', '2', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:instance:variableQuery', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11655', '流程变量修改', '11621', '3', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:instance:variable', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11656', '流程实例激活/挂起', '11621', '4', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:instance:active', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11657', '流程实例删除', '11621', '5', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:instance:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11658', '流程实例作废', '11621', '6', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:instance:invalid', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11659', '流程实例撤销', '11621', '7', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:instance:cancel', '#', 103, 1, sysdate(), null, null, '');

-- 流程定义管理相关按钮
insert into sys_menu values ('11644', '流程定义查询', '11620', '1', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:definition:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11645', '流程定义新增', '11620', '2', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:definition:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11646', '流程定义修改', '11620', '3', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:definition:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11647', '流程定义删除', '11620', '4', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:definition:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11648', '流程定义导出', '11620', '5', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:definition:export', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11649', '流程定义导入', '11620', '6', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:definition:import', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11650', '流程定义发布/取消发布', '11620', '7', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:definition:publish', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11651', '流程定义复制', '11620', '8', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:definition:copy', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values ('11652', '流程定义激活/挂起', '11620', '9', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:definition:active', '#', 103, 1, sysdate(), null, null, '');
-- 流程表达式管理相关按钮
INSERT INTO sys_menu VALUES ('11802', '流程达式定义查询', '11801', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11803', '流程达式定义新增', '11801', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11804', '流程达式定义修改', '11801', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11805', '流程达式定义删除', '11801', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11806', '流程达式定义导出', '11801', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:export', '#', 103, 1, sysdate(), NULL, NULL, '');
-- 请假测试相关按钮
insert into sys_menu VALUES ('11638', '请假申请',     5,    1, 'leave', 'workflow/leave/index', '', 1, 0, 'C', '0', '0', 'workflow:leave:list', '#', 103, 1, sysdate(), NULL, NULL, '请假申请菜单');
insert into sys_menu VALUES ('11639', '请假申请查询', '11638', 1, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:leave:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES ('11640', '请假申请新增', '11638', 2, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:leave:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES ('11641', '请假申请修改', '11638', 3, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:leave:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES ('11642', '请假申请删除', '11638', 4, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:leave:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES ('11643', '请假申请导出', '11638', 5, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:leave:export', '#', 103, 1, sysdate(), NULL, NULL, '');

-- ----------------------------
-- 数据中台菜单
-- ----------------------------
delete from sys_menu where menu_id between 12000 and 12699;

insert into sys_menu values ('12000', '数据中台', '0', '8', 'metadata', '', '', '1', '0', 'M', '0', '0', '', 'dashboard', 103, 1, sysdate(), NULL, NULL, '数据中台一级菜单');

-- 数据资产
insert into sys_menu values ('12100', '数据资产', '12000', '1', 'metadataAsset', '', '', '1', '0', 'M', '0', '0', '', 'table', 103, 1, sysdate(), NULL, NULL, '数据资产分组');
insert into sys_menu values ('12101', '资产目录', '12100', '1', 'metadataCatalog', 'metadata/catalog/index', '', '1', '1', 'C', '0', '0', 'metadata:catalog:list', 'tree', 103, 1, sysdate(), NULL, NULL, '资产目录管理');
insert into sys_menu values ('12102', '元数据表', '12100', '2', 'metadataTable', 'metadata/table/index', '', '1', '1', 'C', '0', '0', 'metadata:table:list', 'form', 103, 1, sysdate(), NULL, NULL, '元数据表管理');
insert into sys_menu values ('12103', '数仓分层', '12100', '3', 'metadataLayer', 'metadata/layer/index', '', '1', '1', 'C', '0', '0', 'metadata:layer:list', 'tree-table', 103, 1, sysdate(), NULL, NULL, '数仓分层管理');
insert into sys_menu values ('12104', '数据域管理', '12100', '4', 'metadataDomain', 'metadata/domain/index', '', '1', '1', 'C', '0', '0', 'metadata:domain:list', 'category', 103, 1, sysdate(), NULL, NULL, '数据域管理');
insert into sys_menu values ('12111', '资产目录查询', '12101', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:catalog:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12112', '资产目录新增', '12101', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:catalog:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12113', '资产目录修改', '12101', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:catalog:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12114', '资产目录删除', '12101', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:catalog:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12115', '资产目录导出', '12101', '5', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:catalog:export', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12121', '元数据表查询', '12102', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:table:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12122', '元数据表修改', '12102', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:table:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12123', '元数据表删除', '12102', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:table:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12124', '元数据表导出', '12102', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:table:export', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12125', '元数据表批量治理', '12102', '5', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:table:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12131', '数仓分层查询', '12103', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:layer:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12132', '数仓分层新增', '12103', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:layer:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12133', '数仓分层修改', '12103', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:layer:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12134', '数仓分层删除', '12103', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:layer:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12135', '数仓分层导出', '12103', '5', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:layer:export', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12141', '数据域查询', '12104', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:domain:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12142', '数据域新增', '12104', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:domain:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12143', '数据域修改', '12104', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:domain:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12144', '数据域删除', '12104', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:domain:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12145', '数据域导出', '12104', '5', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:domain:export', '#', 103, 1, sysdate(), NULL, NULL, '');

-- 数据探查
insert into sys_menu values ('12200', '数据探查', '12000', '2', 'metadataProfile', '', '', '1', '0', 'M', '0', '0', '', 'search', 103, 1, sysdate(), NULL, NULL, '数据探查分组');
insert into sys_menu values ('12201', '元数据采集', '12200', '1', 'metadataScan', 'metadata/scan/index', '', '1', '1', 'C', '0', '0', 'metadata:scan:list', 'job', 103, 1, sysdate(), NULL, NULL, '元数据采集');
insert into sys_menu values ('12202', '采集任务', '12200', '2', 'metadataSchedule', 'metadata/scan-schedule/index', '', '1', '1', 'C', '0', '0', 'metadata:schedule:list', 'date', 103, 1, sysdate(), NULL, NULL, '元数据采集调度');
insert into sys_menu values ('12203', '画像任务', '12200', '3', 'metadataProfileTask', 'metadata/dprofile/task/index', '', '1', '1', 'C', '0', '0', '', 'clipboard', 103, 1, sysdate(), NULL, NULL, '数据画像任务');
insert into sys_menu values ('12204', '探查报告', '12200', '4', 'metadataProfileReport', 'metadata/dprofile/report/index', '', '1', '1', 'C', '0', '0', '', 'education', 103, 1, sysdate(), NULL, NULL, '数据探查报告');
insert into sys_menu values ('12211', '元数据采集查询', '12201', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:scan:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12212', '元数据采集执行', '12201', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:scan:exec', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12221', '采集任务查询', '12202', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:schedule:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12222', '采集任务新增', '12202', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:schedule:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12223', '采集任务修改', '12202', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:schedule:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12224', '采集任务删除', '12202', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:schedule:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12225', '采集任务执行', '12202', '5', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:schedule:exec', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12231', '画像任务查询', '12203', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dprofile:task:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12232', '画像任务新增', '12203', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dprofile:task:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12233', '画像任务修改', '12203', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dprofile:task:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12234', '画像任务删除', '12203', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dprofile:task:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12235', '画像任务启动', '12203', '5', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dprofile:task:start', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12236', '画像任务停止', '12203', '6', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dprofile:task:stop', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12237', '画像任务执行', '12203', '7', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dprofile:task:run', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12241', '探查报告查询', '12204', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dprofile:report:query', '#', 103, 1, sysdate(), NULL, NULL, '');

-- 数据质量
insert into sys_menu values ('12300', '数据质量', '12000', '3', 'metadataQuality', '', '', '1', '0', 'M', '0', '0', '', 'chart', 103, 1, sysdate(), NULL, NULL, '数据质量分组');
insert into sys_menu values ('12301', '质量总览', '12300', '1', 'metadataDqcDashboard', 'metadata/dqc/dashboard/index', '', '1', '1', 'C', '0', '0', 'metadata:dqc:score:list', 'dashboard', 103, 1, sysdate(), NULL, NULL, '数据质量总览');
insert into sys_menu values ('12302', '规则模板', '12300', '2', 'metadataDqcTemplate', 'metadata/dqc/template/index', '', '1', '1', 'C', '0', '0', 'metadata:dqc:template:list', 'build', 103, 1, sysdate(), NULL, NULL, '质量规则模板');
insert into sys_menu values ('12303', '质量规则', '12300', '3', 'metadataDqcRule', 'metadata/dqc/rule/index', '', '1', '1', 'C', '0', '0', 'metadata:dqc:rule:list', 'edit', 103, 1, sysdate(), NULL, NULL, '质量规则定义');
insert into sys_menu values ('12304', '质量计划', '12300', '4', 'metadataDqcPlan', 'metadata/dqc/plan/index', '', '1', '1', 'C', '0', '0', 'metadata:dqc:plan:list', 'clipboard', 103, 1, sysdate(), NULL, NULL, '质量检测计划');
insert into sys_menu values ('12305', '执行记录', '12300', '5', 'metadataDqcExecution', 'metadata/dqc/execution/index', '', '1', '1', 'C', '0', '0', 'metadata:dqc:execution:list', 'history', 103, 1, sysdate(), NULL, NULL, '质量执行记录');
insert into sys_menu values ('12311', '质量总览查询', '12301', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:score:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12312', '质量总览导出', '12301', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:score:export', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12321', '规则模板查询', '12302', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:template:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12322', '规则模板新增', '12302', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:template:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12323', '规则模板修改', '12302', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:template:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12324', '规则模板删除', '12302', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:template:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12331', '质量规则查询', '12303', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:rule:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12332', '质量规则新增', '12303', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:rule:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12333', '质量规则修改', '12303', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:rule:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12334', '质量规则删除', '12303', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:rule:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12341', '质量计划查询', '12304', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:plan:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12342', '质量计划新增', '12304', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:plan:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12343', '质量计划修改', '12304', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:plan:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12344', '质量计划删除', '12304', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:plan:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12345', '质量计划执行', '12304', '5', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:plan:exec', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12346', '质量计划发布', '12304', '6', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:plan:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12347', '质量计划停用', '12304', '7', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:plan:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12351', '执行记录查询', '12305', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:execution:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12352', '执行明细查看', '12305', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:execution:detail', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12353', '执行记录停止', '12305', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:execution:stop', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12354', '执行记录重跑', '12305', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:dqc:execution:rerun', '#', 103, 1, sysdate(), NULL, NULL, '');

-- 数据安全
insert into sys_menu values ('12400', '数据安全', '12000', '4', 'metadataSecurity', '', '', '1', '0', 'M', '0', '0', '', 'lock', 103, 1, sysdate(), NULL, NULL, '数据安全分组');
insert into sys_menu values ('12401', '分类管理', '12400', '1', 'metadataSecurityClassification', 'metadata/security/classification/index', '', '1', '1', 'C', '0', '0', 'metadata:security:classification:list', 'nested', 103, 1, sysdate(), NULL, NULL, '数据分类管理');
insert into sys_menu values ('12402', '分级管理', '12400', '2', 'metadataSecurityLevel', 'metadata/security/level/index', '', '1', '1', 'C', '0', '0', 'metadata:security:level:list', 'number', 103, 1, sysdate(), NULL, NULL, '敏感等级管理');
insert into sys_menu values ('12403', '敏感识别规则', '12400', '3', 'metadataSecurityRules', 'metadata/security/rules/index', '', '1', '1', 'C', '0', '0', 'metadata:security:rules:list', 'skill', 103, 1, sysdate(), NULL, NULL, '敏感识别规则');
insert into sys_menu values ('12404', '敏感字段台账', '12400', '4', 'metadataSecuritySensitivity', 'metadata/security/sensitivity/index', '', '1', '1', 'C', '0', '0', 'metadata:security:sensitivity:list', 'validCode', 103, 1, sysdate(), NULL, NULL, '敏感字段台账');
insert into sys_menu values ('12405', '脱敏模板', '12400', '5', 'metadataSecurityMaskTemplate', 'metadata/security/mask-template/index', '', '1', '1', 'C', '0', '0', 'metadata:security:maskTemplate:list', 'example', 103, 1, sysdate(), NULL, NULL, '脱敏模板');
insert into sys_menu values ('12406', '脱敏策略', '12400', '6', 'metadataSecurityStrategy', 'metadata/security/strategy/index', '', '1', '1', 'C', '0', '0', 'metadata:security:strategy:list', 'guide', 103, 1, sysdate(), NULL, NULL, '脱敏策略');
insert into sys_menu values ('12407', '脱敏查询', '12400', '7', 'metadataSecurityMaskQuery', 'metadata/security/mask-query/index', '', '1', '1', 'C', '0', '0', '', 'search', 103, 1, sysdate(), NULL, NULL, '脱敏查询');
insert into sys_menu values ('12408', '访问日志', '12400', '8', 'metadataSecurityAccessLog', 'metadata/security/access-log/index', '', '1', '1', 'C', '0', '0', 'metadata:security:accessLog:list', 'form', 103, 1, sysdate(), NULL, NULL, '访问日志');
insert into sys_menu values ('12409', '安全审计', '12400', '9', 'metadataSecurityAudit', 'metadata/security/audit/index', '', '1', '1', 'C', '0', '0', '', 'monitor', 103, 1, sysdate(), NULL, NULL, '安全审计');
insert into sys_menu values ('12411', '分类管理查询', '12401', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:classification:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12412', '分类管理新增', '12401', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:classification:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12413', '分类管理修改', '12401', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:classification:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12414', '分类管理删除', '12401', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:classification:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12421', '分级管理查询', '12402', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:level:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12422', '分级管理新增', '12402', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:level:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12423', '分级管理修改', '12402', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:level:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12424', '分级管理删除', '12402', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:level:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12431', '敏感规则查询', '12403', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:rules:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12432', '敏感规则新增', '12403', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:rules:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12433', '敏感规则修改', '12403', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:rules:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12434', '敏感规则删除', '12403', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:rules:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12441', '敏感台账查询', '12404', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:sensitivity:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12442', '敏感台账新增', '12404', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:sensitivity:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12443', '敏感台账修改', '12404', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:sensitivity:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12444', '敏感台账删除', '12404', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:sensitivity:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12445', '敏感字段扫描', '12404', '5', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:sensitivity:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12446', '敏感字段确认', '12404', '6', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:sensitivity:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12451', '脱敏模板查询', '12405', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:maskTemplate:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12452', '脱敏模板新增', '12405', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:maskTemplate:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12453', '脱敏模板修改', '12405', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:maskTemplate:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12454', '脱敏模板删除', '12405', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:maskTemplate:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12461', '脱敏策略查询', '12406', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:strategy:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12462', '脱敏策略新增', '12406', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:strategy:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12463', '脱敏策略修改', '12406', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:strategy:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12464', '脱敏策略删除', '12406', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:strategy:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12471', '脱敏查询查看', '12407', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:maskQuery:list', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12481', '访问日志查询', '12408', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:accessLog:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12491', '安全审计查看', '12409', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:security:audit:list', '#', 103, 1, sysdate(), NULL, NULL, '');

-- 血缘探查
insert into sys_menu values ('12500', '血缘探查', '12000', '5', 'metadataLineage', '', '', '1', '0', 'M', '0', '0', '', 'share', 103, 1, sysdate(), NULL, NULL, '血缘探查分组');
insert into sys_menu values ('12501', '血缘总览', '12500', '1', 'metadataLineageList', 'metadata/lineage/index', '', '1', '1', 'C', '0', '0', 'metadata:lineage:list', 'link', 103, 1, sysdate(), NULL, NULL, '血缘关系管理');
insert into sys_menu values ('12502', '血缘图谱', '12500', '2', 'metadataLineageGraph', 'metadata/lineage/graph/index', '', '1', '1', 'C', '0', '0', 'metadata:lineage:view', 'tree', 103, 1, sysdate(), NULL, NULL, '血缘图谱查看');
insert into sys_menu values ('12511', '血缘关系查询', '12501', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:lineage:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12512', '血缘关系新增', '12501', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:lineage:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12513', '血缘关系修改', '12501', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:lineage:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12514', '血缘关系删除', '12501', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:lineage:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12515', '血缘图谱查看', '12501', '5', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:lineage:view', '#', 103, 1, sysdate(), NULL, NULL, '');

-- 标准治理
insert into sys_menu values ('12600', '标准治理', '12000', '6', 'metadataGovernance', '', '', '1', '0', 'M', '0', '0', '', 'dict', 103, 1, sysdate(), NULL, NULL, '标准治理分组');
insert into sys_menu values ('12601', '术语分类', '12600', '1', 'metadataGlossaryCategory', 'metadata/glossary/category/index', '', '1', '1', 'C', '0', '0', 'metadata:glossary:category:list', 'nested', 103, 1, sysdate(), NULL, NULL, '术语分类管理');
insert into sys_menu values ('12602', '业务术语', '12600', '2', 'metadataGlossaryTerm', 'metadata/glossary/term/index', '', '1', '1', 'C', '0', '0', 'metadata:glossary:term:list', 'documentation', 103, 1, sysdate(), NULL, NULL, '业务术语管理');
insert into sys_menu values ('12603', '术语映射', '12600', '3', 'metadataGlossaryMapping', 'metadata/glossary/mapping/index', '', '1', '1', 'C', '0', '0', 'metadata:glossary:mapping:list', 'mapping', 103, 1, sysdate(), NULL, NULL, '术语映射管理');
insert into sys_menu values ('12611', '术语分类查询', '12601', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:category:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12612', '术语分类新增', '12601', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:category:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12613', '术语分类修改', '12601', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:category:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12614', '术语分类删除', '12601', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:category:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12621', '业务术语查询', '12602', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:term:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12622', '业务术语新增', '12602', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:term:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12623', '业务术语修改', '12602', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:term:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12624', '业务术语删除', '12602', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:term:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12625', '业务术语发布', '12602', '5', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:term:publish', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12631', '术语映射查询', '12603', '1', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:mapping:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12632', '术语映射新增', '12603', '2', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:mapping:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12633', '术语映射修改', '12603', '3', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:mapping:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12634', '术语映射删除', '12603', '4', '#', '', '', '1', '0', 'F', '0', '0', 'metadata:glossary:mapping:remove', '#', 103, 1, sysdate(), NULL, NULL, '');

INSERT INTO sys_dict_type VALUES (13, '000000', '业务状态', 'wf_business_status', 103, 1, sysdate(), NULL, NULL, '业务状态列表');
INSERT INTO sys_dict_type VALUES (14, '000000', '表单类型', 'wf_form_type', 103, 1, sysdate(), NULL, NULL, '表单类型列表');
INSERT INTO sys_dict_type VALUES (15, '000000', '任务状态', 'wf_task_status', 103, 1, sysdate(), NULL, NULL, '任务状态');
INSERT INTO sys_dict_data VALUES (39, '000000', 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL,'已撤销');
INSERT INTO sys_dict_data VALUES (40, '000000', 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '草稿');
INSERT INTO sys_dict_data VALUES (41, '000000', 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL,'待审核');
INSERT INTO sys_dict_data VALUES (42, '000000', 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL,'已完成');
INSERT INTO sys_dict_data VALUES (43, '000000', 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL,'已作废');
INSERT INTO sys_dict_data VALUES (44, '000000', 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL,'已退回');
INSERT INTO sys_dict_data VALUES (45, '000000', 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate(), NULL,NULL, '已终止');
INSERT INTO sys_dict_data VALUES (46, '000000', 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL,'自定义表单');
INSERT INTO sys_dict_data VALUES (47, '000000', 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL,'动态表单');
INSERT INTO sys_dict_data VALUES (48, '000000', 1, '撤销', 'cancel', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '撤销');
INSERT INTO sys_dict_data VALUES (49, '000000', 2, '通过', 'pass', 'wf_task_status', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '通过');
INSERT INTO sys_dict_data VALUES (50, '000000', 3, '待审核', 'waiting', 'wf_task_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '待审核');
INSERT INTO sys_dict_data VALUES (51, '000000', 4, '作废', 'invalid', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '作废');
INSERT INTO sys_dict_data VALUES (52, '000000', 5, '退回', 'back', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '退回');
INSERT INTO sys_dict_data VALUES (53, '000000', 6, '终止', 'termination', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '终止');
INSERT INTO sys_dict_data VALUES (54, '000000', 7, '转办', 'transfer', 'wf_task_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '转办');
INSERT INTO sys_dict_data VALUES (55, '000000', 8, '委托', 'depute', 'wf_task_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '委托');
INSERT INTO sys_dict_data VALUES (56, '000000', 9, '抄送', 'copy', 'wf_task_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '抄送');
INSERT INTO sys_dict_data VALUES (57, '000000', 10, '加签', 'sign', 'wf_task_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '加签');
INSERT INTO sys_dict_data VALUES (58, '000000', 11, '减签', 'sign_off', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '减签');
INSERT INTO sys_dict_data VALUES (59, '000000', 11, '超时', 'timeout', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '超时');

