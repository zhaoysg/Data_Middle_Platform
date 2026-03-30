/**
 * 组件映射注册表
 * key = 后端 sys_menu.component 字段值
 * value = 对应的 Vue 组件（通过动态 import 懒加载）
 *
 * 命名约定：
 * - 前端 views 目录使用 PascalCase（大写首字母），如 governance/Metadata/index.vue
 * - 后端 component 字段使用 kebab-case（小写），如 governance/metadata/index
 * - componentMap 同时注册两种格式以兼容
 */
const modules = import.meta.glob('@/views/**/index.vue')

export const componentMap: Record<string, () => Promise<any>> = {
  // ===== 工作台 =====
  'dashboard/index': () => import('@/views/dashboard/index.vue'),
  'Workbench': () => import('@/views/dashboard/index.vue'),

  // ===== DQC 数据质量中心 =====
  'dqc/rule-template/index': () => import('@/views/dqc/rule-template/index.vue'),
  'dqc/rule-def/index': () => import('@/views/dqc/rule-def/index.vue'),
  'dqc/plan/index': () => import('@/views/dqc/plan/index.vue'),
  'dqc/execution/index': () => import('@/views/dqc/execution/index.vue'),
  // 合并后的质检报告（质量概览 Tab）直接用执行记录页面
  'quality/report/index': () => import('@/views/dqc/execution/index.vue'),
  'dqc/data-domain/index': () => import('@/views/dqc/data-domain/index.vue'),
  'dqc/score/index': () => import('@/views/dqc/score/index.vue'),
  'dqc/datasource/index': () => import('@/views/dqc/datasource/index.vue'),

  // DQC - 兼容旧 component 名（quality 前缀）
  'quality/index': () => import('@/views/dqc/plan/index.vue'),
  'quality/plan/index': () => import('@/views/dqc/plan/index.vue'),
  'quality/rule/index': () => import('@/views/dqc/rule-def/index.vue'),
  'quality/rule-template/index': () => import('@/views/dqc/rule-template/index.vue'),
  'quality/template/index': () => import('@/views/dqc/rule-template/index.vue'),
  'quality/execution/index': () => import('@/views/dqc/execution/index.vue'),

  // ===== DProfile 数据探查中心 =====
  'dprofile/task/index': () => import('@/views/dprofile/task/index.vue'),
  'dprofile/report/index': () => import('@/views/dprofile/report/index.vue'),
  // 兼容旧 component 名（profile 前缀）
  'profile/index': () => import('@/views/dprofile/task/index.vue'),
  'profile/task/index': () => import('@/views/dprofile/task/index.vue'),
  'profile/report/index': () => import('@/views/dprofile/report/index.vue'),

  // ===== Governance 数据治理中心 =====
  'governance/Metadata/index': () => import('@/views/governance/Metadata/index.vue'),
  'governance/Lineage/index': () => import('@/views/governance/Lineage/index.vue'),
  'governance/Asset/index': () => import('@/views/governance/Asset/index.vue'),
  'governance/Standard/index': () => import('@/views/governance/Standard/index.vue'),
  'governance/Glossary/index': () => import('@/views/governance/Glossary/index.vue'),
  'governance/Security/index': () => import('@/views/governance/Security/index.vue'),
  // 兼容 kebab-case（小写）
  'governance/metadata/index': () => import('@/views/governance/Metadata/index.vue'),
  'governance/lineage/index': () => import('@/views/governance/Lineage/index.vue'),
  'governance/asset/index': () => import('@/views/governance/Asset/index.vue'),
  'governance/standard/index': () => import('@/views/governance/Standard/index.vue'),
  'governance/glossary/index': () => import('@/views/governance/Glossary/index.vue'),
  'governance/security/index': () => import('@/views/governance/Security/index.vue'),
  'governance/catalog/index': () => import('@/views/governance/Asset/index.vue'),
  // 兼容 gov 前缀
  'gov/index': () => import('@/views/governance/Metadata/index.vue'),
  'gov/metadata/index': () => import('@/views/governance/Metadata/index.vue'),
  'gov/lineage/index': () => import('@/views/governance/Lineage/index.vue'),
  'gov/standard/index': () => import('@/views/governance/Standard/index.vue'),
  'gov/glossary/index': () => import('@/views/governance/Glossary/index.vue'),
  'gov/catalog/index': () => import('@/views/governance/Asset/index.vue'),
  'security/index': () => import('@/views/governance/Security/index.vue'),

  // ===== Monitor 数据监控中心 =====
  'monitor/index': () => import('@/views/monitor/index.vue'),
  'monitor/task-execution/index': () => import('@/views/monitor/task-execution/index.vue'),
  'monitor/alert-rule/index': () => import('@/views/monitor/alert-rule/index.vue'),
  'monitor/alert-record/index': () => import('@/views/monitor/alert-record/index.vue'),
  // 兼容旧 component 名
  'monitor/metric/index': () => import('@/views/monitor/index.vue'),
  'monitor/alert/index': () => import('@/views/monitor/alert-rule/index.vue'),
  'monitor/execution/index': () => import('@/views/monitor/task-execution/index.vue'),

  // ===== System 系统管理 =====
  'system/user/index': () => import('@/views/system/user/index.vue'),
  'system/role/index': () => import('@/views/system/role/index.vue'),
  'system/menu/index': () => import('@/views/system/menu/index.vue'),
  'system/dept/index': () => import('@/views/system/dept/index.vue'),
  'system/index': () => import('@/views/system/user/index.vue'),

  // ===== Security 数据安全模块 =====
  'security/overview/index': () => import('@/views/security/overview/index.vue'),
  'security/classification/index': () => import('@/views/security/classification/index.vue'),
  'security/sensitivity-scan/index': () => import('@/views/security/sensitivity-scan/index.vue'),
  'security/sensitivity-manage/index': () => import('@/views/security/sensitivity-manage/index.vue'),
  'security/sensitivity-rules/index': () => import('@/views/security/sensitivity-rules/index.vue'),
  'security/mask-rules/index': () => import('@/views/security/mask-rules/index.vue'),
  'security/mask-strategy/index': () => import('@/views/security/mask-strategy/index.vue'),
  'security/access-approval/index': () => import('@/views/security/access-approval/index.vue'),
  'security/audit/index': () => import('@/views/security/audit/index.vue'),

  // ===== 兼容旧 datasource =====
  'datasource/index': () => import('@/views/dqc/datasource/index.vue'),
}

/**
 * 根据 component 路径获取组件加载器
 * 优先精确匹配，其次尝试标准化路径匹配
 */
export function getComponentLoader(component: string): (() => Promise<any>) | null {
  if (!component) return null

  // 1. 精确匹配
  if (componentMap[component]) {
    return componentMap[component]
  }

  // 2. 标准化路径后匹配（去除末尾 /index）
  const normalized = component.replace(/\/index$/, '')
  const normalizedWithIndex = normalized + '/index'
  if (componentMap[normalizedWithIndex]) {
    return componentMap[normalizedWithIndex]
  }

  // 3. 尝试 glob 动态匹配（兜底）
  const normalizedPath = normalized.replace(/^\//, '')
  const key = '@/views/' + normalizedPath + '/index.vue'
  if (modules[key]) {
    return modules[key] as () => Promise<any>
  }

  // 4. 最后尝试 glob 直接匹配原始 component
  const rawKey = '@/views/' + component.replace(/^\//, '') + '.vue'
  if (modules[rawKey]) {
    return modules[rawKey] as () => Promise<any>
  }

  console.warn(`[componentMap] 未找到组件映射: ${component}`)
  return null
}
