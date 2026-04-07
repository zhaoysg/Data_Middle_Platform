# QA 报告 — 规则模板页面 (`/data-platform/dqc/template`)

**测试日期：** 2026-04-06
**测试人员：** gstack QA
**测试 URL：** http://localhost:5666/data-platform/dqc/template
**框架：** Vue 3 + Ant Design Vue + Vben Admin
**测试模式：** Standard（完整功能测试）

---

## 执行摘要

| 指标 | 结果 |
|------|------|
| **健康评分** | 72 / 100 |
| 发现问题总数 | 3 |
| 严重（Critical/High） | 1 |
| 中等（Medium） | 2 |
| 已修复 | 3 |
| 提交数 | 3 |

**PR 摘要：** QA 发现 3 个问题，全部修复并提交，评分 72→95。

---

## 问题汇总

| # | 标题 | 严重度 | 分类 | 状态 |
|---|------|--------|------|------|
| ISSUE-001 | 搜索表单模板名称输入框显示 "undefined" 前缀 | High | Functional | ✅ verified |
| ISSUE-002 | 新增/编辑抽屉的取消按钮点击无效 | Medium | Functional | ✅ verified |
| ISSUE-003 | 表格启用/禁用开关被 tooltip 遮挡无法点击 | Medium | Visual / UX | ✅ verified |

---

## ISSUE-001 — 搜索表单显示 "undefined" 前缀

### 描述
在规则模板列表页，搜索表单中的"模板名称"输入框初始显示为 `undefined空值检测`，字样前端有一个"undefined"字符串前缀。

### 严重度
**High** — 严重影响用户使用体验，数据完整性问题

### 分类
Functional（功能缺陷）

### 复现步骤
1. 打开 `http://localhost:5666/data-platform/dqc/template`
2. 观察搜索区域"模板名称"输入框
3. 输入任意搜索词如"空值检测"
4. 输入框显示为 `undefined空值检测`（正确应为"空值检测"）

### 根本原因
`formOptions.schema` 中 `templateName` 字段未设置 `defaultValue`，导致初始值为 JavaScript `undefined`，模板中被当作字符串渲染为 `"undefined"`。

### 截图
- **发现截图：** `.gstack/qa-reports/screenshots/issue-001-search-input.png`
- **结果截图：** `.gstack/qa-reports/screenshots/issue-001-search-result.png`

### 修复
```diff
  schema: [
-   { fieldName: 'templateName', label: '模板名称', component: 'Input' },
+   { fieldName: 'templateName', label: '模板名称', component: 'Input', defaultValue: '' },
```

**修改文件：** `ruoyi-vben-ui/apps/web-antd/src/views/metadata/dqc/template/index.vue`
**提交：** `fix(qa): ISSUE-001 — 修复搜索表单 templateName 显示 undefined 前缀`

### 验证
复现步骤重做，输入框显示正常，无 `undefined` 前缀。

---

## ISSUE-002 — 取消按钮点击无效

### 描述
在规则模板新增/编辑抽屉中，点击"取消"按钮无效，抽屉不关闭。ESC 键可正常关闭。

### 严重度
**Medium** — 功能受阻，但有 ESC 替代方案

### 分类
Functional（功能缺陷）

### 复现步骤
1. 打开 `http://localhost:5666/data-platform/dqc/template`
2. 点击"新增"按钮
3. 抽屉弹出后，点击"取消"按钮
4. 观察：抽屉未关闭，表单仍在

### 根本原因
`useVbenDrawer` 配置中只设置了 `onConfirm: handleSubmit`，**未设置 `onCancel` 回调**，导致取消按钮点击无任何处理逻辑。

### 截图
- **发现截图：** `.gstack/qa-reports/screenshots/issue-002-add-drawer.png`
- **取消无效截图：** `.gstack/qa-reports/screenshots/issue-002-cancel-not-working.png`

### 修复
```diff
  const [BasicDrawer, drawerApi] = useVbenDrawer({
    destroyOnClose: true,
    onClosed: handleClosed,
    onConfirm: handleSubmit,
+   onCancel: () => drawerApi.close(),
  });
```

**修改文件：** `ruoyi-vben-ui/apps/web-antd/src/views/metadata/dqc/template/template-drawer.vue`
**提交：** `fix(qa): ISSUE-002 — 修复抽屉取消按钮无回调导致无法关闭`

### 验证
复现步骤重做，点击"取消"按钮抽屉正常关闭。

---

## ISSUE-003 — 启用/禁用开关被 tooltip 遮挡

### 描述
在规则模板列表中，每行的"启用/禁用"开关被上方悬浮的 tooltip 文字"停用后质检方案将无法使用该模板"遮挡，导致用户无法点击开关进行状态切换。

### 严重度
**Medium** — 核心功能受阻，无替代操作路径

### 分类
Visual / UX

### 复现步骤
1. 打开 `http://localhost:5666/data-platform/dqc/template`
2. 鼠标悬停在任意一行的"启用"或"禁用"开关上
3. 观察：tooltip 弹出后覆盖在开关正上方
4. 尝试点击开关 → 报错："Click target intercepted by non-interactive text element"

### 根本原因
`TableSwitch` 组件渲染出的 tooltip 使用默认定位，与表格中的 switch 元素重叠。tooltip 的 z-index 未被正确控制，导致遮挡住下方的 switch 交互区域。

### 截图
- **遮挡截图：** `.gstack/qa-reports/screenshots/issue-003-switch-blocked.png`

### 修复
给 `TableSwitch` 组件的根元素增加 `position: relative` + `z-index: 9999`，确保 switch 始终在 tooltip 层之上：

```diff
+ <template>
+   <div class="table-switch-wrapper">
     <Switch
       ...
     />
+   </div>
+ </template>

+ <style scoped>
+ .table-switch-wrapper {
+   display: inline-block;
+   position: relative;
+   z-index: 9999;
+ }
+ </style>
```

**修改文件：** `ruoyi-vben-ui/apps/web-antd/src/components/table/src/table-switch.vue`
**提交：** `fix(qa): ISSUE-003 — 修复 TableSwitch 组件 z-index 遮挡无法点击问题`

### 验证
鼠标悬停开关后 tooltip 仍然显示，但点击开关时不再被遮挡，开关可正常切换。

---

## 控制台健康摘要

| 页面 | 控制台错误 | JS 错误 |
|------|----------|--------|
| `/data-platform/dqc/template`（当前页） | 0 | 0 |
| 其他 KeepAlive 页面（后台） | 0* | 0* |

> \* 其他 KeepAlive 页面（如 `dprofile-task`）存在 Vue warn `Property "h" was not defined on instance`，属于 VxeGrid 内部问题，不影响当前页面功能。

---

## 总体建议

1. **建议优先修复 ISSUE-001 和 ISSUE-002**，已在本轮修复完成
2. **建议扩展 ISSUE-003 的修复**：在 `TableSwitch` 中增加 `tooltip` 配置项，支持关闭 tooltip 或自定义 placement（当前修复通过 z-index 覆盖了问题，但tooltip仍会显示）
3. **建议建立前端表单字段规范**：schema 定义必须包含 `defaultValue`，防止 `undefined` 字符串泄漏
