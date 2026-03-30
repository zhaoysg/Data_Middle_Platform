# 项目规则记录

## 1. 项目定位

本仓库是基于 **RuoYi-Vue-Plus 5.6.0** 改造的前后端分离项目：

- 后端是 **Java 17 + Spring Boot 3.5.12 + Maven 多模块**。
- 前端是 **Vue 3 + Vben Admin Monorepo + pnpm workspace + Turbo**。
- 当前默认联调形态是：
  - 后端本地启动在 `8080`
  - 前端开发服务启动在 `5666`
  - 前端通过 `/api` 代理到后端 `http://localhost:8080`

---

## 2. 后端结构规则

### 2.1 启动入口

- 统一启动入口：`ruoyi-admin`
- 该模块是最终打包与运行的 Web 服务入口
- 后端默认激活 `dev` profile

### 2.2 模块职责

- `ruoyi-admin`
  - 最终启动模块
  - 聚合系统、任务、代码生成、演示、工作流等模块
- `ruoyi-common`
  - 通用基础能力
  - 包含 core、mybatis、redis、security、web、excel、log、mail、oss、tenant 等公共组件
- `ruoyi-modules`
  - 业务模块集合
  - 当前包含 `ruoyi-system`、`ruoyi-job`、`ruoyi-generator`、`ruoyi-demo`、`ruoyi-workflow`
- `ruoyi-extend`
  - 扩展服务
  - 当前包含 `ruoyi-monitor-admin`、`ruoyi-snailjob-server`

### 2.3 后续迭代约束

- 新增通用能力，优先放入 `ruoyi-common`
- 新增业务能力，优先按领域落入 `ruoyi-modules`
- 非主链路的扩展服务，放入 `ruoyi-extend`
- 默认不要改变 `ruoyi-admin` 作为总启动入口的架构
- 若只是普通业务迭代，尽量不要同时改动多个基础公共模块

---

## 3. 前端结构规则

### 3.1 前端主工作区

- 前端根目录：`ruoyi-vben-ui`
- 包管理器：`pnpm`
- 工作区管理：`pnpm workspace`
- 构建编排：`turbo`

### 3.2 当前主要前端应用

- 主应用：`ruoyi-vben-ui/apps/web-antd`
- 该应用是当前最应该优先迭代和联调的前端入口
- `apps/web-antdv-next` 存在，但本轮不作为默认主站入口

### 3.3 前端运行规则

- 开发端口固定为 `5666`
- 开发环境接口前缀是 `/api`
- Vite 代理目标默认是 `http://localhost:8080`
- 如本机 `8080` 被其他项目占用，可通过环境变量 `VITE_PROXY_TARGET` 临时切换代理目标
- 生产环境接口前缀是 `/prod-api`
- 前后端都启用了接口加解密相关配置，联调时不要随意关闭某一端而保留另一端

### 3.4 后续迭代约束

- 没有明确理由时，前端修改优先落在 `apps/web-antd`
- 若改接口协议、加密、SSE、WebSocket，必须同步核对后端配置
- 若新增页面，先确认是否属于已有系统模块，避免页面结构与后端模块边界错位

---

## 4. 数据库与缓存规则

### 4.1 已确认的远端资源

已完成基础连通性与鉴权确认：

- MySQL 主机：`49.232.153.150`
- MySQL 端口：`3366`
- Redis 主机：当前按同一主机 `49.232.153.150` 使用
- Redis 端口：`6679`
- Redis 鉴权：已确认可用

### 4.2 已确认的数据情况

MySQL 中已存在以下与本项目高度相关的库：

- `ry-vue`
- `ry_vue`

本轮为当前项目单独初始化的新库：

- `ruoyi_moban_v1`

当前项目源码中的默认开发库名是：

- `ry-vue`

### 4.3 数据层约束

- 默认以源码当前配置对应的 `ry-vue` 作为优先目标库
- 若启动时报表结构不匹配，再进一步核对是否应切换到 `ry_vue`
- **不要把明文密码直接提交进仓库文档或业务代码**
- 数据连接信息建议仅保留主机、端口、库名、用途；密码只用于本地运行配置

### 4.4 SQL 脚本位置

- 主业务库脚本：`script/sql/ry_vue_5.X.sql`
- 定时任务相关：`script/sql/ry_job.sql`
- 工作流相关：`script/sql/ry_workflow.sql`
- 历史升级脚本：`script/sql/update/`

### 4.5 新库初始化规则

- 当前已通过 `script/init_remote_mysql.py` 成功将标准脚本导入到 `ruoyi_moban_v1`
- 后续如果需要重新初始化独立库，优先复用该脚本
- 本轮新库用途：专门承载 `moban-v1` 项目，避免污染已有 `ry-vue` / `ry_vue`

---

## 5. 当前运行事实

### 5.1 后端

- Java 版本：`17.0.2`
- Maven 版本：`3.9.6`
- Spring Boot 版本：`3.5.12`
- 默认 HTTP 端口：`8080`
- 默认 profile：`dev`
- 如本机 `8080` 冲突，可通过环境变量 `SERVER_PORT` 临时改端口
- 本地启动建议同时支持以下环境变量覆盖：
  - `DB_HOST` `DB_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD`
  - `REDIS_HOST` `REDIS_PORT` `REDIS_PASSWORD`
  - `BOOT_ADMIN_ENABLED`
  - `SNAIL_JOB_ENABLED`

### 5.2 前端

- Node 版本：`v24.14.1`
- `package.json` 要求：
  - Node `>=20.19.0`
  - pnpm `>=10.0.0`
- 当前系统未安装全局 `pnpm`，后续优先尝试使用 `corepack` 提供 pnpm

---

## 6. 推荐启动顺序

1. 先确认 MySQL、Redis 可访问
2. 配置后端 `application-dev.yml`
3. 编译并启动后端 `ruoyi-admin`
4. 安装前端依赖并启动 `ruoyi-vben-ui/apps/web-antd`
5. 浏览器访问前端页面，确认接口代理到 `8080`

---

## 7. 后续迭代工作规则

- 每次开始改动前，先确认修改影响的是：
  - 后端公共层
  - 后端业务层
  - 前端页面层
  - 前后端协议层
- 若涉及数据库字段、接口结构、枚举值、权限标识，必须同步记录
- 若新增模块，先明确应归属到 `ruoyi-common`、`ruoyi-modules` 还是 `ruoyi-extend`
- 若只是页面问题，不要顺手改后端公共底层
- 若只是业务逻辑问题，不要同时重构前端基础框架
- 每次完成一轮梳理或实现，都要更新这份文档或新增对应专题文档，不能只看不记
- 本项目的专题梳理文档已新增：`SECONDARY_DEV_MAP.md`
- 本项目的 bigdata 数据源专项规则文档已新增：`BIGDATA_DATASOURCE_RULES.md`
- 后续涉及后端模块、核心表、前端页面路由的二开，优先同步更新 `SECONDARY_DEV_MAP.md`
- 后续涉及 `bigdata` 数据源、`sys_datasource`、外部库接入、数仓层级的二开，优先同步更新 `BIGDATA_DATASOURCE_RULES.md`

---

## 8. 本轮已完成确认项

- 已确认本仓库为后端 Maven 多模块 + 前端 pnpm monorepo 架构
- 已确认后端启动核心为 `ruoyi-admin`
- 已确认前端主应用为 `ruoyi-vben-ui/apps/web-antd`
- 已确认前端开发代理指向 `http://localhost:8080`
- 已确认远端 MySQL `49.232.153.150:3366` 可连接
- 已确认远端 Redis `49.232.153.150:6679` 可连接并鉴权成功
- 已确认 MySQL 中存在 `ry-vue` / `ry_vue` 两个相关库
- 已新建并导入独立数据库 `ruoyi_moban_v1`
- 已新增“二次开发地图”专题文档：`SECONDARY_DEV_MAP.md`
