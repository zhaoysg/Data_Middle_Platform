# 数据中台工具 - 前端项目

基于 Vue 3 + Vite + Ant Design Vue 的数据中台工具前端应用。

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 | 3.4+ |
| 构建工具 | Vite | 5.x |
| UI 组件库 | Ant Design Vue | 4.x |
| 状态管理 | Pinia | 2.x |
| 路由 | Vue Router | 4.x |
| HTTP 客户端 | Axios | 1.6.x |
| 图表 | ECharts | 5.x |

## 项目结构

```
bagdatahouse-ui/
├── public/                  # 静态资源
├── src/
│   ├── api/                 # API 接口封装
│   │   └── dqc.ts          # 数据质量模块接口
│   ├── components/          # 公共组件
│   ├── i18n/               # 国际化
│   ├── layouts/            # 布局组件
│   │   └── BasicLayout.vue # 基础布局（侧边栏+顶部导航）
│   ├── router/             # 路由配置
│   │   └── index.ts
│   ├── stores/             # Pinia 状态管理
│   │   └── user.ts
│   ├── styles/             # 全局样式
│   │   ├── index.less
│   │   └── variables.less  # CSS 变量
│   ├── types/              # TypeScript 类型定义
│   │   └── index.ts
│   ├── utils/              # 工具函数
│   │   └── request.ts      # Axios 封装
│   ├── views/              # 页面组件
│   │   ├── dashboard/      # 首页
│   │   ├── dqc/            # 数据质量中心
│   │   │   ├── rule-template/  # 规则模板管理
│   │   │   ├── rule-def/       # 规则定义
│   │   │   ├── plan/           # 质检方案
│   │   │   ├── execution/      # 执行记录
│   │   │   └── score/          # 质量评分
│   │   ├── dprofile/       # 数据探查中心
│   │   ├── governance/     # 数据治理中心
│   │   ├── monitor/        # 数据监控中心
│   │   ├── system/         # 系统管理
│   │   ├── error/         # 错误页面
│   │   └── login/          # 登录页
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.node.json
└── vite.config.ts
```

## 功能模块

### 数据质量中心
- **规则模板管理** — 21 种内置模板 + 自定义模板，含五步骤创建流程
- **规则定义** — 基于模板创建规则，绑定目标表/字段
- **质检方案** — 将规则绑定到表范围，定时/手动触发
- **执行记录** — 质量检查执行历史与结果
- **质量评分** — 六维度评分（完整性/唯一性/准确性/一致性/及时性/有效性）

### 其他模块（开发中）
- 数据探查中心、数据治理中心、数据监控中心、系统管理

## 开发

### 环境要求
- Node.js >= 18
- npm >= 9

### 安装依赖
```bash
cd bagdatahouse-ui
npm install
```

### 启动开发服务器
```bash
npm run dev
# 访问 http://localhost:3000
```

### 构建生产版本
```bash
npm run build
```

### 接口代理
开发环境下，Vite 代理 `/api/*` 请求到 `http://localhost:8080`（后端服务）。

## 后端 API

后端运行在 `http://localhost:8080/api`，主要接口：

| 模块 | 接口路径 | 说明 |
|------|----------|------|
| 规则模板 | `/api/dqc/rule-template/*` | 模板 CRUD |
| 规则定义 | `/api/dqc/rule-def/*` | 规则 CRUD |
| 质检方案 | `/api/dqc/plan/*` | 方案 CRUD |
| 执行记录 | `/api/dqc/execution/*` | 执行记录查询 |
| 数据源 | `/api/dqc/datasource/*` | 数据源配置 |

## 设计规范

参考第十二章 UI 设计规范：
- 主色：`#1677FF`（科技蓝）
- 渐变主色：`linear-gradient(135deg, #1677FF, #00B4DB)`
- 辅助色：翠绿 `#52C41A`、警示橙 `#FAAD14`、危险红 `#FF4D4F`
- 字体：Inter（正文）+ Noto Sans SC（中文）
- 等宽字体：JetBrains Mono（SQL 展示）
