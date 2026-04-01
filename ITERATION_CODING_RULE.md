# 迭代编码规则

> 本文件按当前仓库真实结构、真实运行方式、真实踩坑记录重写。  
> 目标不是写抽象规范，而是给后续所有二开、联调、修 Bug、加页面、改数据源、改元数据平台时提供一套可直接执行的规则。

---

## 1. 适用范围与默认口径

### 1.1 默认主线

- 后端主线：`ruoyi-admin`、`ruoyi-common/*`、`ruoyi-modules/*`、`ruoyi-extend/*`
- 前端主线：`ruoyi-vben-ui/apps/web-antd`
- 当前正式业务模块已包含：
  - `ruoyi-system`
  - `ruoyi-job`
  - `ruoyi-generator`
  - `ruoyi-demo`
  - `ruoyi-workflow`
  - `ruoyi-datasource`
  - `ruoyi-metadata`

### 1.2 非默认范围

- `bgdata/*` 是仓库内并存的另一条代码线
- 没有明确需求时，禁止把 `bgdata/*` 与 `ruoyi-*` 主线混改

### 1.3 默认交付口径

后续任何迭代任务，如无特别说明，一律按下面的口径执行：

1. 先判断需求属于哪条代码线、哪个模块、哪条数据链
2. 先补齐业务闭环，再做实现
3. 后端、前端、菜单权限、数据库、租户、验证链路一起看
4. 尽量复用当前工程习惯，不额外发明新框架层
5. 改完必须给出可验证证据，不能只说“理论上可以”

---

## 2. 当前项目事实基线

### 2.1 后端事实

- 基础架构：`Java 17 + Spring Boot 3.5.12 + Maven 多模块`
- 启动入口：`ruoyi-admin`
- 默认 profile：`dev`
- 默认 HTTP 端口：`8080`
- `Authorization` 是当前默认 token header
- 默认返回体系：
  - 普通接口：`R<T>`
  - 分页接口：`TableDataInfo<T>`
- 权限体系：`Sa-Token`
- 多租户：开启
- 动态数据源：开启，且 `dynamic.strict=true`
- 全局 API 加解密：开启

### 2.2 前端事实

- 基础架构：`Vue 3 + Vben Admin Monorepo + pnpm workspace + Turbo`
- 主应用：`ruoyi-vben-ui/apps/web-antd`
- 默认开发端口：`5666`
- 默认接口前缀：`/api`
- 当前联调模式：前端 `5666` 代理后端 `8080`
- 如本机后端端口变化，可用 `VITE_PROXY_TARGET` 覆盖 Vite 代理目标
- 默认请求入口：`apps/web-antd/src/api/request.ts`
- 默认页面入口：`apps/web-antd/src/views/**`
- 当前开发环境默认：
  - `VITE_GLOB_ENABLE_ENCRYPT=true`
  - `VITE_GLOB_SSE_ENABLE=true`
  - `VITE_GLOB_WEBSOCKET_ENABLE=false`
- 当前主 UI 组合是：
  - Vben 公共组件
  - Ant Design Vue 表单控件
  - VXE Grid 表格
  - Vben Drawer / Modal 封装

### 2.3 数据源事实

开发环境当前有两个动态数据源：

- `master`
- `bigdata`

默认规则：

- 系统主业务链路优先落 `master`
- 数据源管理与元数据平台强相关能力优先落 `bigdata`
- 涉及外部库接入时，要区分：
  - “访问 bigdata 平台自己的元数据表”
  - “访问 `sys_datasource` 里登记的外部数据源实际库表”

### 2.4 元数据平台事实

当前仓库里，元数据平台已经不是临时代码，而是正式模块：

- 后端模块：`ruoyi-modules/ruoyi-metadata`
- 前端 API：`ruoyi-vben-ui/apps/web-antd/src/api/metadata/*`
- 前端页面：`ruoyi-vben-ui/apps/web-antd/src/views/metadata/*`
- 后端接口前缀：`/system/metadata/*`

元数据模块当前职责至少包括：

- 元数据表
- 元数据字段
- 资产目录
- 数据域
- 数仓分层
- 扫描管理

### 2.5 当前运行与 QA 事实

- 后端日志目录：`logs/`
- 常看日志：
  - `logs/sys-info.log`
  - `logs/sys-error.log`
  - `logs/aaa-frontend.out.log`
  - `logs/aaa-frontend.err.log`
- 当前项目已存在 Playwright / QA 运行痕迹：
  - 运行脚本：`.codex-runlogs/*.mjs`
  - 结果产物：`ruoyi-vben-ui/.gstack/qa-reports/*.json`

这些产物可以复用，但不要把一次性的验证脚本误当成正式业务代码。

---

## 3. 开工前必须先回答的 12 个问题

任何需求开工前，先把下面 12 个问题答清楚：

1. 这次需求属于 `ruoyi-*` 主线还是 `bgdata/*`？
2. 它属于哪个后端模块？
3. 它对应哪个前端页面目录和 API 目录？
4. 它是普通 CRUD、流程业务、任务调度、数据源治理，还是元数据平台能力？
5. 是否涉及数据库表、字段、索引、默认值、历史数据修复？
6. 是否涉及 `sys_menu`、按钮权限码、角色授权、动态路由？
7. 是否涉及租户隔离、数据权限、超管保护？
8. 是否涉及接口加密、登录态、`Authorization`、`ClientID`、SSE、WebSocket？
9. 是否涉及 `master` 和 `bigdata` 的表落点区分？
10. 是否涉及旧数据兼容或旧枚举兼容？
11. 变更后是由后端主动返回闭环，还是前端还要做结构适配？
12. 最终验证证据是什么：编译、接口、页面、日志、脚本、截图，还是全部都要？

只要其中任意 3 项回答为“是”，就不允许只盯着单文件小修。

---

## 4. 模块落点规则

### 4.1 后端模块落点

- `ruoyi-admin`
  - 只放启动入口、认证入口、全局接入、聚合配置
  - 登录、注册、统一接入逻辑优先放这里
- `ruoyi-common/*`
  - 只放跨模块公共能力
  - 单模块专用逻辑不要为了“看起来通用”提前下沉
- `ruoyi-modules/ruoyi-system`
  - 系统内管默认主战场
  - 用户、角色、菜单、部门、字典、参数、租户、OSS 等优先放这里
- `ruoyi-modules/ruoyi-workflow`
  - 流程定义、任务、实例、审批流业务优先放这里
- `ruoyi-modules/ruoyi-job`
  - 调度执行器、补偿任务、汇总任务、离线任务逻辑放这里
- `ruoyi-modules/ruoyi-datasource`
  - 数据源台账、连接测试、外部库适配、动态数据源管理放这里
- `ruoyi-modules/ruoyi-metadata`
  - 元数据表、字段、目录、数据域、数仓分层、扫描治理放这里
- `ruoyi-extend/*`
  - 监控后台、SnailJob Server 这类独立扩展服务放这里

### 4.2 前端目录落点

- 业务页面：`apps/web-antd/src/views/**`
- 业务 API：`apps/web-antd/src/api/**`
- VXE 适配：`apps/web-antd/src/adapter/vxe-table.ts`
- 请求统一入口：`apps/web-antd/src/api/request.ts`
- 动态路由转换：`apps/web-antd/src/router/access.ts`

细分落点：

- 系统管理页：`src/views/system/**`
- 数据源页：`src/views/system/datasource/**`
- 元数据平台页：`src/views/metadata/**`
- 元数据 API：`src/api/metadata/**`
- 动态菜单 API：`src/api/core/menu.ts`

---

## 5. 后端编码规则

### 5.1 分层必须与当前工程对齐

- `Controller`：只做入参接收、权限注解、返回值组装、少量编排
- `Service`：承担业务编排、事务、校验、跨表处理
- `Mapper`：承担数据访问
- `Entity / Bo / Vo`：分开维护，不混用

### 5.2 返回体规则

- 普通接口优先 `R<T>`
- 分页接口优先 `TableDataInfo<T>`
- 如果前端页面直接走 `useVbenVxeGrid` 的 `proxyConfig`，后端分页返回必须能被前端直接识别，或者前端必须手动包装 `rows/total`

### 5.3 注解与常用模式

- 权限：`@SaCheckPermission`
- 操作日志：`@Log`
- 参数校验：`@Validated`
- 写操作事务：`@Transactional(rollbackFor = Exception.class)` 视场景使用
- Mapper/VO 映射：优先复用 `@AutoMapper`、`MapstructUtils`
- 查询条件：优先复用 `Wrappers`、`LambdaQueryWrapper`、`LambdaUpdateWrapper`

### 5.4 多租户与数据权限硬规则

- 只要表继承 `TenantEntity` 或服务处在租户链路中，就必须核对：
  - `tenant_id`
  - `create_dept`
  - `create_by`
  - `create_time`
  - `update_by`
  - `update_time`
- 不要只改 Java 实体，不看物理表结构
- 任何继承 `BaseEntity` / `TenantEntity` 的表，数据库实际字段必须和实体要求兼容，否则查询或 `selectVoList` 很容易直接报错
- 涉及唯一索引的租户表，索引设计必须先判断是否应包含 `tenant_id`

### 5.5 动态数据源规则

- 访问系统主链路业务表：默认走 `master`
- 访问数据源平台元数据表：明确使用 `@DS("bigdata")`
- 访问外部登记数据源的真实库表：不要直接写死 `@DS("bigdata")`，而应走 `ruoyi-datasource` 的适配器与动态连接池能力

### 5.6 元数据模块专属规则

只要改的是 `ruoyi-metadata`，必须同时检查：

1. 后端是否明确处在 `bigdata` 链路
2. 表结构脚本是否放对位置
3. 唯一索引是否考虑 `tenant_id`
4. 历史数据是否需要补默认值或兼容修复
5. 前端页面、API、菜单、权限是否同步

### 5.7 元数据表结构脚本规则

当前元数据模块启动时自动执行的是：

- `ruoyi-modules/ruoyi-metadata/src/main/resources/sql/metadata/*.sql`

自动初始化由 `MetadataSchemaInitializer` 负责，特点是：

- 只扫 `classpath*:sql/metadata/*.sql`
- 只在 `bigdata` 数据源执行
- 只支持 MySQL / MariaDB 风格脚本

硬规则：

- 元数据模块的自动修复 SQL，必须放到 `ruoyi-metadata/src/main/resources/sql/metadata/`
- 不要只把元数据修复 SQL 丢到根目录 `script/sql/migration/`，那不会被应用启动自动执行
- 根目录 `script/sql/**` 默认视为人工执行脚本，不视为启动自动初始化脚本

### 5.8 字段类型与审计字段规则

- `create_by` / `update_by` 如果实体定义是数值 ID，就不要把用户名字符串直接塞进去
- 日期字段要和全局序列化格式兼容：`yyyy-MM-dd HH:mm:ss`
- 枚举值变更必须考虑旧数据兼容，前后端都要兼容旧值

---

## 6. 数据库与 SQL 规则

### 6.1 表落点规则

- `master`
  - 系统主业务表
  - 菜单、角色、用户、租户、字典、参数、日志等
- `bigdata`
  - 数据源平台元数据
  - 元数据平台表
  - 数仓治理类平台配置表

### 6.2 菜单与权限表规则

无论新增的是系统页、数据源页还是元数据页，只要页面最终要在左侧菜单出现，至少要检查：

1. `sys_menu`
2. 角色授权
3. 按钮权限码
4. 前端页面路径与 `sys_menu.component` 的映射关系

### 6.3 动态路由硬规则

本项目不是“只靠前端本地路由文件”的项目。

当前主链路是：

1. 后端 `sys_menu`
2. `/system/menu/getRouters`
3. 前端 `src/router/access.ts` 的 `backMenuToVbenMenu`
4. `import.meta.glob('../views/**/*.vue')`

结论：

- 新增页面时，不补菜单和权限，通常不算真正完成
- `sys_menu.component` 必须能映射到 `src/views/**` 实际文件

### 6.4 SQL 执行规则

- 主业务库初始化脚本：`script/sql/ry_vue_5.X.sql`
- SnailJob 相关：`script/sql/ry_job.sql`
- 工作流相关：`script/sql/ry_workflow.sql`
- 手工迁移目录：`script/sql/migration/`

规则：

- 先判断 SQL 落在哪个库，再执行
- 不要把 `master` 和 `bigdata` 的脚本混着导
- 不要把菜单数据脚本和 bigdata 平台表脚本混成“无脑整包执行”

---

## 7. 前端编码规则

### 7.1 请求层硬规则

- 所有业务请求统一走 `src/api/request.ts` 里的 `requestClient`
- 页面里不要裸写 `fetch` / `axios`
- 接口加密场景必须显式传 `{ encrypt: true }`
- 原始响应场景如果确实需要 `code/msg/data` 原样结构，必须显式调整 `isTransformResponse`
- GET / DELETE 带数组参数时，默认会按 repeat 形式序列化，页面不要再手动拼接一套重复逻辑

### 7.2 `requestClient` 当前真实行为

当前 `requestClient` 成功响应后的默认行为不是返回原始 `R<T>`，而是：

- 如果响应里有 `data`，直接返回 `data`
- 如果没有 `data`，则返回剩余字段，例如 `rows`、`total`

这意味着：

- 前端 API 类型定义要按“解包后结构”写
- 页面层不要默认以为还能拿到完整 `code/msg/data`
- VXE 页面如果接口最终拿到的是裸数组，需要自己包装，不要直接丢给 `proxyConfig`

### 7.3 VXE Grid 规则

当前全局 VXE 适配在 `src/adapter/vxe-table.ts` 中固定了：

- `result: 'rows'`
- `total: 'total'`
- `list: 'rows'`

硬规则：

- 只要页面使用 `useVbenVxeGrid + proxyConfig`
- 就必须保证最终返回值能提供 `rows` 和 `total`
- 如果接口只返回数组，页面必须自己包装成：
  - `{ rows, total }`
- 不要把裸数组直接交给 VXE proxy

### 7.4 Drawer / Modal 规则

当前 `useVbenDrawer` 的底层不是 Ant Drawer，而是 Vben 自己封装的 `Sheet/SheetContent`。

规则：

- 写页面时可以继续用 `useVbenDrawer`
- 但写自动化测试、DOM 选择器、样式覆盖时，不要假定它是 `.ant-drawer-*`
- QA / 自动化定位优先按：
  - `role="dialog"`
  - 抽屉标题
  - 表单项 label

### 7.5 页面组件规则

- 页面优先复用：
  - `Page`
  - `useVbenVxeGrid`
  - `useVbenDrawer`
  - `useVbenModal`
- 表单控件来自 Ant Design Vue 时，要显式 import
- 不要假设 `a-input`、`a-select` 等能自动全局注册成功

### 7.6 权限与按钮规则

- 按钮权限必须挂 `v-access:code`
- 只写页面按钮，不配权限码，不算完成
- 涉及状态切换、批量操作、删除、导出时，必须核对按钮权限和后端权限一致

### 7.7 路由与隐藏页规则

- 普通菜单页走 `sys_menu.component -> src/views/**`
- 隐藏详情页、回填高亮页，要同步看 `src/router/access.ts` 里的 `routeMetaMapping`
- 不要只建 Vue 文件，不补动态路由映射就期待菜单高亮正确

---

## 8. 前后端联动规则

### 8.1 新增一个普通后台页面

至少一起检查：

1. 后端模块落点
2. 物理表与索引
3. `Entity / Bo / Vo / Mapper / Service / Controller`
4. 前端 `src/api/**`
5. 前端 `src/views/**`
6. `sys_menu`
7. 权限码
8. 角色授权

### 8.2 修改一个已有页面

先判断是哪一类问题：

- 页面展示问题
- 接口协议问题
- 菜单权限问题
- 租户过滤问题
- 数据库字段缺失 / 默认值缺失问题

规则：

- 字段名、枚举值、状态值、字典值一旦改动，前后端必须同步
- 旧数据已入库时，页面展示必须考虑旧值兼容

### 8.3 修改登录、认证、加密、密码、用户、角色、租户

默认按高风险改动处理，必须同时检查：

- `Authorization`
- `ClientID`
- token 失效处理
- 接口加密开关
- 超管特判
- 多租户
- 数据权限
- 页面按钮权限

### 8.4 修改数据源与元数据平台

必须同时检查：

1. `ruoyi-datasource`
2. `ruoyi-metadata`
3. `bigdata` 表结构
4. 元数据初始化脚本
5. 前端 `src/api/metadata/*`
6. 前端 `src/views/metadata/*`

### 8.5 修改扫描管理、列表页、表格页

只要用到了 VXE Grid，就要额外检查：

- 后端返回是否是分页结构
- 前端 API 解包后是否还是你以为的结构
- 页面 `query` 返回给 VXE 的对象是否有 `rows` / `total`

---

## 9. 项目特有高风险坑位

### 9.1 不要误判“接口没数据”

本项目里经常出现这类情况：

- 接口有数据
- `requestClient` 已经把它解包
- 页面又把解包结果错当原始响应
- 最终表格显示为空

因此任何“页面没数据”问题，都先同时抓：

1. 浏览器实际请求返回体
2. API 封装返回值
3. 页面拿到的数据结构
4. 表格组件实际要求的结构

### 9.2 不要误判“SQL 已加但应用没生效”

只要是元数据平台表结构修复：

- 如果 SQL 不在 `ruoyi-metadata/src/main/resources/sql/metadata/*.sql`
- 启动时就不会自动执行

不要只看根目录 `script/sql/migration/` 就以为应用会自动跑到。

### 9.3 不要误判“抽屉打不开 / 自动化坏了”

`useVbenDrawer` 当前底层是 `SheetContent`，不是 Ant Drawer。

所以：

- DOM 选择器坏了，不代表页面坏了
- 自动化测试和样式覆盖不要写死 `.ant-drawer-*`

### 9.4 不要误判“当前改动引入了所有错误”

当前项目存在一些基础设施噪音可能混入日志，例如：

- SnailJob 未连通时的心跳错误
- 浏览器断开时的连接中断
- 历史失败扫描记录

规则：

- 报告里必须区分“本次新增问题”和“历史已存在问题”
- 不要把旧日志直接当本次回归

---

## 10. 验证与 QA 规则

### 10.1 后端验证

至少选最贴近改动范围的命令验证：

- 单模块测试：
  - `mvn -pl ruoyi-modules/ruoyi-metadata -am test -DskipTests=false`
- 打包主入口：
  - `mvn -pl ruoyi-admin -am package -DskipTests`

原则：

- 改哪个模块，先验证哪个模块
- 涉及启动链路时，再验证 `ruoyi-admin`

### 10.2 前端验证

常用验证命令：

- 主应用类型检查：
  - `pnpm --dir ruoyi-vben-ui/apps/web-antd typecheck`
- 主应用开发启动：
  - `pnpm --dir ruoyi-vben-ui dev:antd`
- 一键起前后端：
  - `script/start-all.bat`

原则：

- 页面改动至少要做页面级联调
- 如果仓库存在历史类型错误，必须在结果里区分“历史失败”和“本次新增失败”

### 10.3 联调与浏览器验证

涉及页面交互、抽屉、状态切换、表格加载、保存回显时，优先做真实浏览器验证。

当前建议：

- 复用 `.codex-runlogs/*.mjs`
- 结果输出到 `ruoyi-vben-ui/.gstack/qa-reports/*.json`

### 10.4 运行日志核对

后端出问题默认看：

- `logs/sys-info.log`
- `logs/sys-error.log`

前端出问题默认看：

- `logs/aaa-frontend.out.log`
- `logs/aaa-frontend.err.log`

### 10.5 Curl / Postman 调试规则

当前项目调试接口时，除了 `Authorization`，还要注意 `ClientID`。

规则：

- 只带 token 不带 `ClientID`，不一定能通过
- 登录态问题不能只看一个 header

---

## 11. 禁止事项

- 禁止默认混改 `ruoyi-*` 与 `bgdata/*`
- 禁止在普通需求里顺手重构 `ruoyi-common`
- 禁止只改前端页面、不补菜单权限就说完成
- 禁止只改数据库字段、不补 Java 实体和前端表单
- 禁止绕开 `requestClient` 另起一套请求封装
- 禁止把元数据模块的自动初始化 SQL 只丢到根目录手工脚本目录
- 禁止把外部数据源访问逻辑误写成固定 `@DS("bigdata")`
- 禁止忽略租户、权限、超管保护、按钮权限
- 禁止把旧日志、旧失败记录直接当成当前变更回归
- 禁止把密码、密钥、生产连接信息写进业务代码或规范文档

---

## 12. 文档同步规则

发生下面情况时，必须同步文档：

- 改项目整体结构、启动方式、端口、环境变量：更新 `PROJECT_RULES.md`
- 改模块职责、菜单地图、核心页面路径：更新 `SECONDARY_DEV_MAP.md`
- 改 `bigdata`、`sys_datasource`、外部库适配、数仓规则：更新 `BIGDATA_DATASOURCE_RULES.md`
- 改全局开发约束、交付方式、踩坑口径：更新本文件

---

## 13. 默认执行流程

后续迭代默认按下面顺序执行：

1. 明确代码线、模块落点、数据落点
2. 梳理前后端、菜单权限、租户、数据源联动面
3. 再开始编码
4. 编码后先跑最小必要验证
5. 再做页面联调与日志核对
6. 最后输出“改了什么、怎么验证、还有什么风险”

---

## 14. 一句话总纲

> 本项目不是“改一个页面或一个接口就算完成”的项目。  
> 任何真正可交付的迭代，都必须把 **后端模块、前端页面、接口协议、菜单权限、数据库结构、租户/数据源约束、验证证据** 作为一条完整链路一起处理。
