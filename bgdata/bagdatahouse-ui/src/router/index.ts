import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'

NProgress.configure({ showSpinner: false })

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    name: 'BasicLayout',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'DashboardOutlined' }
      },
      {
        path: 'security',
        name: 'Security',
        redirect: '/security/overview',
        meta: { title: '数据安全', icon: 'SafetyCertificateOutlined' },
        children: [
          {
            path: 'overview',
            name: 'SecurityOverview',
            component: () => import('@/views/security/overview/index.vue'),
            meta: { title: '安全总览大盘' }
          },
          {
            path: 'classification',
            name: 'SecurityClassification',
            component: () => import('@/views/security/classification/index.vue'),
            meta: { title: '分类分级标准' }
          },
          {
            path: 'sensitivity-scan',
            name: 'SecuritySensitivityScan',
            component: () => import('@/views/security/sensitivity-scan/index.vue'),
            meta: { title: '敏感字段识别' }
          },
          {
            path: 'sensitivity-manage',
            name: 'SecuritySensitivityManage',
            component: () => import('@/views/security/sensitivity-manage/index.vue'),
            meta: { title: '敏感字段管理' }
          },
          {
            path: 'sensitivity-rules',
            name: 'SecuritySensitivityRules',
            component: () => import('@/views/security/sensitivity-rules/index.vue'),
            meta: { title: '识别规则管理' }
          },
          {
            path: 'mask-rules',
            name: 'SecurityMaskRules',
            component: () => import('@/views/security/mask-rules/index.vue'),
            meta: { title: '脱敏规则管理' }
          },
          {
            path: 'mask-strategy',
            name: 'SecurityMaskStrategy',
            component: () => import('@/views/security/mask-strategy/index.vue'),
            meta: { title: '脱敏策略管理' }
          },
          {
            path: 'access-approval',
            name: 'SecurityAccessApproval',
            component: () => import('@/views/security/access-approval/index.vue'),
            meta: { title: '访问审批管理' }
          }
        ]
      },
      {
        path: 'governance',
        name: 'Governance',
        redirect: '/governance/metadata',
        meta: { title: '数据治理', icon: 'AccountBookOutlined' },
        children: [
          {
            path: 'metadata',
            name: 'Metadata',
            component: () => import('@/views/governance/Metadata/index.vue'),
            meta: { title: '元数据管理' }
          },
          {
            path: 'lineage',
            name: 'Lineage',
            component: () => import('@/views/governance/Lineage/index.vue'),
            meta: { title: '数据血缘' }
          },
          {
            path: 'asset',
            name: 'Asset',
            component: () => import('@/views/governance/Asset/index.vue'),
            meta: { title: '资产目录' }
          },
          {
            path: 'standard',
            name: 'Standard',
            component: () => import('@/views/governance/Standard/index.vue'),
            meta: { title: '数据标准' }
          },
          {
            path: 'glossary',
            name: 'Glossary',
            component: () => import('@/views/governance/Glossary/index.vue'),
            meta: { title: '业务术语库' }
          }
        ]
      },
      {
        path: 'dqc',
        name: 'Dqc',
        redirect: '/dqc/rule-template',
        meta: { title: '数据质量', icon: 'SafetyCertificateOutlined' },
        children: [
          {
            path: 'rule-template',
            name: 'DqcRuleTemplate',
            component: () => import('@/views/dqc/rule-template/index.vue'),
            meta: { title: '规则模板管理' }
          },
          {
            path: 'rule-def',
            name: 'DqcRuleDef',
            component: () => import('@/views/dqc/rule-def/index.vue'),
            meta: { title: '规则定义' }
          },
          {
            path: 'plan',
            name: 'DqcPlan',
            component: () => import('@/views/dqc/plan/index.vue'),
            meta: { title: '质检方案' }
          },
          {
            path: 'execution',
            name: 'DqcExecution',
            component: () => import('@/views/dqc/execution/index.vue'),
            meta: { title: '执行记录' }
          },
          {
            path: 'score',
            name: 'DqcScore',
            component: () => import('@/views/dqc/score/index.vue'),
            meta: { title: '质量评分' }
          }
        ]
      },
      {
        path: 'asset-management',
        name: 'AssetManagement',
        redirect: '/asset-management/datasource',
        meta: { title: '数据资产管理', icon: 'AppstoreOutlined' },
        children: [
          {
            path: 'datasource',
            name: 'Datasource',
            component: () => import('@/views/dqc/datasource/index.vue'),
            meta: { title: '数据源管理' }
          },
          {
            path: 'data-domain',
            name: 'DataDomain',
            component: () => import('@/views/dqc/data-domain/index.vue'),
            meta: { title: '数据域管理' }
          }
        ]
      },
      {
        path: 'dprofile',
        name: 'Dprofile',
        redirect: '/dprofile/task',
        meta: { title: '数据探查', icon: 'FundOutlined' },
        children: [
          {
            path: 'task',
            name: 'DprofileTask',
            component: () => import('@/views/dprofile/task/index.vue'),
            meta: { title: '探查任务' }
          },
          {
            path: 'report',
            name: 'DprofileReport',
            component: () => import('@/views/dprofile/report/index.vue'),
            meta: { title: '探查报告' }
          }
        ]
      },
      {
        path: 'monitor',
        name: 'Monitor',
        redirect: '/monitor/index',
        meta: { title: '数据监控', icon: 'AlertOutlined' },
        children: [
          {
            path: 'index',
            name: 'MonitorIndex',
            component: () => import('@/views/monitor/index.vue'),
            meta: { title: '监控概览' }
          },
          {
            path: 'task-execution',
            name: 'MonitorTaskExecution',
            component: () => import('@/views/monitor/task-execution/index.vue'),
            meta: { title: '任务执行监控' }
          },
          {
            path: 'alert-rule',
            name: 'AlertRule',
            component: () => import('@/views/monitor/alert-rule/index.vue'),
            meta: { title: '告警规则' }
          },
          {
            path: 'alert-record',
            name: 'AlertRecord',
            component: () => import('@/views/monitor/alert-record/index.vue'),
            meta: { title: '告警记录' }
          }
        ]
      },
      {
        path: 'system',
        name: 'System',
        redirect: '/system/user',
        meta: { title: '系统管理', icon: 'SettingOutlined' },
        children: [
          {
            path: 'user',
            name: 'SystemUser',
            component: () => import('@/views/system/user/index.vue'),
            meta: { title: '用户管理' }
          },
          {
            path: 'role',
            name: 'SystemRole',
            component: () => import('@/views/system/role/index.vue'),
            meta: { title: '角色管理' }
          },
          {
            path: 'dept',
            name: 'SystemDept',
            component: () => import('@/views/system/dept/index.vue'),
            meta: { title: '部门管理' }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, _from, next) => {
  NProgress.start()
  document.title = `${to.meta.title || ''} - 数据中台工具`

  // 检查路由是否需要认证
  if (to.meta.requiresAuth !== false) {
    const token = localStorage.getItem('token')
    if (!token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
