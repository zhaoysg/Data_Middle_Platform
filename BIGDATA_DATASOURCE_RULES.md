# Bigdata 数据源规则

> 本文专门约束 `bigdata` 数据源及 `ruoyi-datasource` 体系的后续迭代。  
> 默认阅读对象：后端开发、前端联调、数据库运维、后续 AI 编码助手。

## 1. 当前实现事实

### 1.1 配置层事实

- 开发环境在 [application-dev.yml](D:\zhaoysg\Projects\java\aaa\ruoyi-admin\src\main\resources\application-dev.yml) 中定义了两个动态数据源：
  - `master`
  - `bigdata`
- 当前默认主数据源仍然是 `master`
- `dynamic.strict=true`，意味着代码里如果切换到一个不存在的数据源会直接报错
- `bigdata` 当前默认库名是 `ry_bigdata_v1`

### 1.2 代码层事实

- 数据源管理模块位于 [ruoyi-datasource](D:\zhaoysg\Projects\java\aaa\ruoyi-modules\ruoyi-datasource)
- 数据源元数据表实体是 [SysDatasource.java](D:\zhaoysg\Projects\java\aaa\ruoyi-modules\ruoyi-datasource\src\main\java\org\dromara\datasource\domain\SysDatasource.java)
- 数据源管理 Mapper [SysDatasourceMapper.java](D:\zhaoysg\Projects\java\aaa\ruoyi-modules\ruoyi-datasource\src\main\java\org\dromara\datasource\mapper\SysDatasourceMapper.java) 明确使用了 `@DS("bigdata")`
- 这说明 `sys_datasource` 元数据表当前被设计为落在 `bigdata` 数据源，而不是 `master`
- 运行时外部库连接池由 [DynamicDataSourceManager.java](D:\zhaoysg\Projects\java\aaa\ruoyi-modules\ruoyi-datasource\src\main\java\org\dromara\datasource\manager\DynamicDataSourceManager.java) 管理
- 数据库类型差异由 [DataSourceAdapter.java](D:\zhaoysg\Projects\java\aaa\ruoyi-modules\ruoyi-datasource\src\main\java\org\dromara\datasource\adapter\DataSourceAdapter.java) 及其各类 Adapter 屏蔽

### 1.3 前端层事实

- 数据源管理前端页面位于 [index.vue](D:\zhaoysg\Projects\java\aaa\ruoyi-vben-ui\apps\web-antd\src\views\system\datasource\index.vue)
- 前端接口封装位于 [index.ts](D:\zhaoysg\Projects\java\aaa\ruoyi-vben-ui\apps\web-antd\src\api\system\datasource\index.ts)
- 页面入口走系统菜单 `system/datasource/index`

### 1.4 菜单权限事实

- 系统菜单仍由 [SysMenuMapper.java](D:\zhaoysg\Projects\java\aaa\ruoyi-modules\ruoyi-system\src\main\java\org\dromara\system\mapper\SysMenuMapper.java) 所在的系统主链路管理
- 该 Mapper 没有切到 `bigdata`
- 因此当前设计下：
  - `sys_datasource` 元数据在 `bigdata`
  - 菜单、角色、权限仍在系统主库链路

## 2. bigdata 体系的角色定位

`bigdata` 在当前工程里不是“随便放点大数据表”的泛化概念，而是两层含义：

1. 固定动态数据源名称：`bigdata`
2. 数据源管理中心的元数据承载库：`sys_datasource`

因此后续迭代必须先区分清楚下面三类数据：

- `master` 里的系统业务数据
- `bigdata` 里的数据源元数据与数仓相关配置数据
- `sys_datasource` 中登记的外部数据源所连接到的真实库表

这三层不能混着理解，更不能混着落表。

## 3. 访问模式规则

### 3.1 固定 bigdata 库访问

满足下面任一条件时，优先使用固定 `@DS("bigdata")` 模式：

- 访问 `sys_datasource`
- 访问未来新增的 `bigdata` 自身平台元数据表
- 访问明确属于 `ry_bigdata_v1` 这套内部管理库的表

推荐做法：

- 新建独立 Mapper
- 在 Mapper 或 Service 上显式标注 `@DS("bigdata")`
- 仍走 MyBatis-Plus / `Mapper -> Service -> Controller` 主分层

### 3.2 外部数据源访问

当需求是“访问某一条已登记数据源所对应的真实库表”时，不要直接写死 `@DS("bigdata")`。

这类场景应走：

1. 从 `sys_datasource` 读取数据源记录
2. 通过 [DataSourceAdapterRegistry.java](D:\zhaoysg\Projects\java\aaa\ruoyi-modules\ruoyi-datasource\src\main\java\org\dromara\datasource\adapter\DataSourceAdapterRegistry.java) 获取适配器
3. 由 [DynamicDataSourceManager.java](D:\zhaoysg\Projects\java\aaa\ruoyi-modules\ruoyi-datasource\src\main\java\org\dromara\datasource\manager\DynamicDataSourceManager.java) 动态注册连接池
4. 通过 Adapter 执行查询、取表、取字段、做采样

一句话区分：

- `@DS("bigdata")` 是访问 bigdata 元数据库
- `adapterRegistry + dsId` 是访问 bigdata 体系登记的外部库

### 3.3 禁止混用

- 不要为了访问外部库，直接把业务 Mapper 标成 `@DS("bigdata")`
- 不要为了读 `sys_datasource`，反过来绕到 Adapter 动态查
- 不要把“元数据管理”和“外部数据探查”写进同一个 Service 里耦在一起

## 4. 表落点规则

### 4.1 必须落在 bigdata 的表

- `sys_datasource`
- 未来所有“描述外部数据源本身”的平台元数据表
- 未来所有“数仓治理配置”且明确服务于 bigdata 平台的表

### 4.2 默认仍落在 master 的表

- 用户、角色、菜单、部门、租户、字典、参数、日志等系统基础表
- 普通后台业务表
- 与数据源无直接关系的流程、任务、通知等业务表

### 4.3 新增表前必须先判断

创建新表前，先回答：

1. 这是平台元数据，还是普通业务数据？
2. 这张表是不是描述“外部数据源本身”？
3. 这张表是否必须跟 `sys_datasource` 强关联？
4. 这张表是否必须由 `@DS("bigdata")` 链路直接访问？

只有答案明显偏向“平台元数据”时，才应落到 `bigdata`。

## 5. 迭代编码规则

### 5.1 新增 bigdata 平台能力

如果需求属于以下范围：

- 数据源台账
- 数据库探查
- 表/字段元数据采集
- 数仓分层配置
- 数据源健康检查
- 数据源连接测试

优先落点：

- 后端：`ruoyi-modules/ruoyi-datasource`
- 前端：`src/views/system/datasource` 或相邻的 datasource 业务目录

### 5.2 新增 bigdata 平台表

必须同步检查：

1. 是否在 `bigdata` 库建表
2. 是否需要单独 migration SQL
3. 是否需要 `@DS("bigdata")`
4. 是否需要新增菜单权限
5. 是否需要增加字典项，例如数据源类型、数仓层级

### 5.3 新增外部数据库类型

必须同步修改：

1. [DataSourceTypeEnum.java](D:\zhaoysg\Projects\java\aaa\ruoyi-modules\ruoyi-datasource\src\main\java\org\dromara\datasource\enums\DataSourceTypeEnum.java)
2. 对应 Adapter 实现
3. [DataSourceAdapterRegistry.java](D:\zhaoysg\Projects\java\aaa\ruoyi-modules\ruoyi-datasource\src\main\java\org\dromara\datasource\adapter\DataSourceAdapterRegistry.java) 的创建逻辑
4. 前端类型选项 [data.tsx](D:\zhaoysg\Projects\java\aaa\ruoyi-vben-ui\apps\web-antd\src\views\system\datasource\data.tsx)
5. 如有需要，补充连接测试与字段读取差异处理

不能只改后端枚举，不改前端选项和适配器。

### 5.4 新增数仓层级

当前前后端都默认使用：

- `ODS`
- `DWD`
- `DWS`
- `ADS`

如果后续要增加新的层级：

- 前端 `dataLayerOptions` 要同步改
- 导出字典和展示逻辑要同步改
- 数据库已有数据的兼容性要先确认

### 5.5 数据源编码规则

- `ds_code` 必须全局唯一
- 编码一旦进入业务使用，不要轻易改名
- 面向长期使用的数据源，编码建议稳定、语义化、全小写或统一大写风格
- `bigdata` 是固定动态数据源名，不建议把外部登记数据源再起名成会混淆系统级数据源的编码

推荐：

- 系统级固定库：保留 `master`、`bigdata`
- 业务登记数据源：使用明确业务前缀，如 `ods_trade_mysql`、`dwd_user_pg`

## 6. 配置规则

### 6.1 Profile 同步规则

只要新增或调整 `bigdata` 数据源，必须同步检查：

- [application-dev.yml](D:\zhaoysg\Projects\java\aaa\ruoyi-admin\src\main\resources\application-dev.yml)
- [application-prod.yml](D:\zhaoysg\Projects\java\aaa\ruoyi-admin\src\main\resources\application-prod.yml)
- 其他实际启用的 profile

不能只改 `dev`，不看 `prod`。

### 6.2 环境变量规则

当前 `dev` 配置中，`master` 和 `bigdata` 共用了同一组 `DB_HOST / DB_PORT / DB_USER / DB_PASSWORD`，库名只靠默认值区分。

这意味着：

- 如果外部只覆盖一个统一的 `DB_NAME`，有机会让两个数据源落到同一个库

因此后续规则是：

- 没有把配置改成独立变量前，不要在部署环境里随意只覆盖一个全局 `DB_NAME`
- 如果要正式独立部署 `bigdata`，优先改造成独立环境变量组，例如 `BIGDATA_DB_HOST`、`BIGDATA_DB_PORT`、`BIGDATA_DB_NAME`、`BIGDATA_DB_USER`、`BIGDATA_DB_PASSWORD`

## 7. SQL 与迁移规则

### 7.1 migration 文件规则

当前 migration 文件是 [V1__datasource_migration.sql](D:\zhaoysg\Projects\java\aaa\script\sql\migration\V1__datasource_migration.sql)。

后续新增 bigdata 相关脚本时：

- 表结构变更与初始化数据分开写
- bigdata 元数据表脚本与系统菜单脚本尽量拆开
- 文件名要能看出是 bigdata 平台脚本，不要混进普通业务库脚本里

### 7.2 菜单脚本执行规则

根据当前代码实现，可以明确一条约束：

- `sys_datasource` 应落在 `bigdata`
- 系统菜单和角色菜单仍由系统主链路读取

因此执行 migration 时不要机械整包导入到单一数据库。  
如果脚本里同时包含：

- `sys_datasource` DDL
- `sys_menu` / `sys_role_menu` 初始化

则应先确认每段 SQL 应该落在哪个库，再执行。

## 8. 前后端联动规则

新增 bigdata 平台页面时，至少同步检查：

1. 后端接口路径是否在 `/system/datasource` 或相邻清晰业务域下
2. 前端 API 是否放在 `src/api/system/datasource`
3. 前端页面是否放在 `src/views/system/datasource`
4. 是否补齐 `sys_menu` 与按钮权限码
5. 是否需要连接测试、表列表、字段列表等配套接口

## 9. 禁止事项

- 禁止把所有“和数据有关”的表都放进 `bigdata`
- 禁止把访问外部库误写成访问 `bigdata` 固定库
- 禁止把 `sys_datasource` 当成普通系统表直接迁回 `master`
- 禁止只改前端数据源页面，不核对后端 Adapter 与动态连接池逻辑
- 禁止新增数据源类型时只改一个枚举值，不补适配器
- 禁止在脚本里把 bigdata 表和系统菜单 SQL 混执行而不区分目标库

## 10. 后续默认执行口径

以后只要任务涉及：

- `bigdata` 数据源
- `sys_datasource`
- 外部数据源登记
- 表/字段探查
- 数仓层级
- 新数据库类型接入

默认按下面的顺序处理：

1. 先判断这是 bigdata 元数据需求，还是外部库访问需求
2. 再决定走 `@DS("bigdata")` 还是走 `adapterRegistry + dsId`
3. 确认表落点、脚本落点、菜单权限落点
4. 最后补齐前后端页面和验证链路

## 11. 本文与其他规则的关系

- 项目总体规则看 [PROJECT_RULES.md](D:\zhaoysg\Projects\java\aaa\PROJECT_RULES.md)
- 总体迭代编码基线看 [ITERATION_CODING_RULE.md](D:\zhaoysg\Projects\java\aaa\ITERATION_CODING_RULE.md)
- 模块与页面地图看 [SECONDARY_DEV_MAP.md](D:\zhaoysg\Projects\java\aaa\SECONDARY_DEV_MAP.md)

后续凡是涉及 bigdata 数据源体系的迭代，优先先看本文。
