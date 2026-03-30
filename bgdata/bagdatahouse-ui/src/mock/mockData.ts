// Mock 数据文件
// 生成随机 ID
export const generateId = () => Math.floor(Math.random() * 10000) + 100

// 通用分页响应
const createPageResponse = (records: any[], total: number, pageNum: number, pageSize: number) => ({
  success: true,
  data: {
    records,
    total,
    size: pageSize,
    current: pageNum,
    pages: Math.ceil(total / pageSize)
  }
})

// 通用成功响应
const createSuccessResponse = (data: any) => ({
  success: true,
  data
})

// 根据 URL 和方法获取 Mock 数据
export function getMockData(url: string, method: string, params?: any, body?: any) {
  // 移除 /api 前缀
  const path = url.replace('/api', '')
  
  // 认证相关
  if (path === '/auth/login' && method === 'post') {
    return {
      success: true,
      data: {
        accessToken: 'mock-token-' + Date.now(),
        tokenType: 'Bearer',
        expiresIn: 7200,
        userId: 1,
        username: 'admin',
        nickname: '管理员',
        avatar: '',
        roles: ['admin'],
        permissions: ['*:*:*']
      }
    }
  }
  
  if (path === '/auth/logout' && method === 'post') {
    return createSuccessResponse(null)
  }
  
  if (path === '/auth/current-user' && method === 'get') {
    return createSuccessResponse({
      userId: 1,
      username: 'admin',
      nickname: '管理员',
      email: 'admin@example.com',
      phone: '13800138000',
      avatar: '',
      roles: ['admin'],
      permissions: ['*:*:*']
    })
  }
  
  // 系统管理 - 用户
  if (path === '/system/user/page' && method === 'get') {
    return createPageResponse([
      { id: 1, username: 'admin', nickname: '管理员', email: 'admin@example.com', phone: '13800138000', status: 1, deptId: 1, createTime: '2024-01-01 10:00:00' },
      { id: 2, username: 'user1', nickname: '用户一', email: 'user1@example.com', phone: '13800138001', status: 1, deptId: 1, createTime: '2024-01-02 10:00:00' },
      { id: 3, username: 'user2', nickname: '用户二', email: 'user2@example.com', phone: '13800138002', status: 0, deptId: 2, createTime: '2024-01-03 10:00:00' }
    ], 3, params?.pageNum || 1, params?.pageSize || 10)
  }
  
  // 系统管理 - 角色
  if (path === '/system/role/page' && method === 'get') {
    return createPageResponse([
      { id: 1, roleName: '超级管理员', roleCode: 'admin', status: 1, createTime: '2024-01-01 10:00:00' },
      { id: 2, roleName: '普通用户', roleCode: 'user', status: 1, createTime: '2024-01-02 10:00:00' }
    ], 2, 1, 10)
  }
  
  // 系统管理 - 菜单
  if (path === '/system/menu/page' && method === 'get') {
    return createPageResponse([
      { id: 1, menuName: '首页', menuType: 'MENU', path: '/dashboard', icon: 'DashboardOutlined', orderNum: 1, status: 1 },
      { id: 2, menuName: '数据质量中心', menuType: 'CATALOG', path: '/dqc', icon: 'SafetyCertificateOutlined', orderNum: 2, status: 1 },
      { id: 3, menuName: '规则模板管理', menuType: 'MENU', path: '/dqc/rule-template', icon: '', orderNum: 3, status: 1, parentId: 2 },
      { id: 4, menuName: '规则定义', menuType: 'MENU', path: '/dqc/rule-def', icon: '', orderNum: 4, status: 1, parentId: 2 },
      { id: 5, menuName: '质检方案', menuType: 'MENU', path: '/dqc/plan', icon: '', orderNum: 5, status: 1, parentId: 2 },
      { id: 6, menuName: '执行记录', menuType: 'MENU', path: '/dqc/execution', icon: '', orderNum: 6, status: 1, parentId: 2 },
      { id: 7, menuName: '数据域管理', menuType: 'MENU', path: '/dqc/data-domain', icon: '', orderNum: 7, status: 1, parentId: 2 },
      { id: 8, menuName: '质量评分', menuType: 'MENU', path: '/dqc/score', icon: '', orderNum: 8, status: 1, parentId: 2 },
      { id: 9, menuName: '数据探查中心', menuType: 'MENU', path: '/dprofile', icon: 'FundOutlined', orderNum: 9, status: 1 },
      { id: 10, menuName: '探查任务', menuType: 'MENU', path: '/dprofile/task', icon: '', orderNum: 10, status: 1, parentId: 9 },
      { id: 11, menuName: '探查报告', menuType: 'MENU', path: '/dprofile/report', icon: '', orderNum: 11, status: 1, parentId: 9 },
      { id: 12, menuName: '数据治理中心', menuType: 'MENU', path: '/governance', icon: 'AccountBookOutlined', orderNum: 12, status: 1 },
      { id: 13, menuName: '元数据管理', menuType: 'MENU', path: '/governance/metadata', icon: '', orderNum: 13, status: 1, parentId: 12 },
      { id: 14, menuName: '数据血缘', menuType: 'MENU', path: '/governance/lineage', icon: '', orderNum: 14, status: 1, parentId: 12 },
      { id: 15, menuName: '资产目录', menuType: 'MENU', path: '/governance/asset', icon: '', orderNum: 15, status: 1, parentId: 12 },
      { id: 16, menuName: '数据标准', menuType: 'MENU', path: '/governance/standard', icon: '', orderNum: 16, status: 1, parentId: 12 },
      { id: 17, menuName: '术语管理', menuType: 'MENU', path: '/governance/glossary', icon: '', orderNum: 17, status: 1, parentId: 12 },
      { id: 18, menuName: '安全管理', menuType: 'MENU', path: '/governance/security', icon: '', orderNum: 18, status: 1, parentId: 12 },
      { id: 19, menuName: '数据监控中心', menuType: 'MENU', path: '/monitor', icon: 'AlertOutlined', orderNum: 19, status: 1 },
      { id: 20, menuName: '监控概览', menuType: 'MENU', path: '/monitor/index', icon: '', orderNum: 20, status: 1, parentId: 19 },
      { id: 21, menuName: '任务执行监控', menuType: 'MENU', path: '/monitor/task-execution', icon: '', orderNum: 21, status: 1, parentId: 19 },
      { id: 22, menuName: '告警规则', menuType: 'MENU', path: '/monitor/alert-rule', icon: '', orderNum: 22, status: 1, parentId: 19 },
      { id: 23, menuName: '告警记录', menuType: 'MENU', path: '/monitor/alert-record', icon: '', orderNum: 23, status: 1, parentId: 19 },
      { id: 24, menuName: '系统管理', menuType: 'MENU', path: '/system', icon: 'SettingOutlined', orderNum: 24, status: 1 },
      { id: 25, menuName: '用户管理', menuType: 'MENU', path: '/system/user', icon: '', orderNum: 25, status: 1, parentId: 24 },
      { id: 26, menuName: '角色管理', menuType: 'MENU', path: '/system/role', icon: '', orderNum: 26, status: 1, parentId: 24 },
      { id: 27, menuName: '菜单管理', menuType: 'MENU', path: '/system/menu', icon: '', orderNum: 27, status: 1, parentId: 24 },
      { id: 28, menuName: '部门管理', menuType: 'MENU', path: '/system/dept', icon: '', orderNum: 28, status: 1, parentId: 24 }
    ], 28, 1, 100)
  }
  
  // 系统管理 - 部门
  if (path === '/system/dept/page' && method === 'get') {
    return createPageResponse([
      { id: 1, deptName: '技术部', deptCode: 'TECH', parentId: 0, leader: '张三', phone: '13800000001', email: 'tech@company.com', status: 1, orderNum: 1, createTime: '2024-01-01 10:00:00' },
      { id: 2, deptName: '运营部', deptCode: 'OPS', parentId: 0, leader: '李四', phone: '13800000002', email: 'ops@company.com', status: 1, orderNum: 2, createTime: '2024-01-01 10:00:00' },
      { id: 3, deptName: '数据组', deptCode: 'DATA', parentId: 1, leader: '王五', phone: '13800000003', email: 'data@company.com', status: 1, orderNum: 3, createTime: '2024-01-02 10:00:00' }
    ], 3, 1, 10)
  }
  
  // 系统管理 - 角色
  if (path === '/system/role/page' && method === 'get') {
    return createPageResponse([
      { id: 1, roleName: '超级管理员', roleCode: 'admin', roleKey: 'admin', status: 1, sort: 1, dataScope: 'ALL', createTime: '2024-01-01 10:00:00', remark: '拥有系统所有权限' },
      { id: 2, roleName: '普通用户', roleCode: 'user', roleKey: 'user', status: 1, sort: 2, dataScope: 'SELF', createTime: '2024-01-01 10:00:00', remark: '普通用户角色' },
      { id: 3, roleName: '数据管理员', roleCode: 'data_admin', roleKey: 'data-admin', status: 1, sort: 3, dataScope: 'DEPT', createTime: '2024-01-02 10:00:00', remark: '负责数据质量管理工作' }
    ], 3, 1, 10)
  }
  
  // 系统管理 - 用户
  if (path === '/system/user/page' && method === 'get') {
    return createPageResponse([
      { id: 1, username: 'admin', nickname: '管理员', email: 'admin@company.com', phone: '13800138000', sex: '1', avatar: '', status: 1, deptId: 1, deptName: '技术部', roles: ['admin'], createTime: '2024-01-01 10:00:00', loginIp: '127.0.0.1', loginDate: '2024-03-23 10:00:00' },
      { id: 2, username: 'user001', nickname: '张三', email: 'zhangsan@company.com', phone: '13800138001', sex: '1', avatar: '', status: 1, deptId: 1, deptName: '技术部', roles: ['user'], createTime: '2024-01-02 10:00:00', loginIp: '127.0.0.1', loginDate: '2024-03-22 14:30:00' },
      { id: 3, username: 'user002', nickname: '李四', email: 'lisi@company.com', phone: '13800138002', sex: '0', avatar: '', status: 1, deptId: 2, deptName: '运营部', roles: ['user', 'data_admin'], createTime: '2024-01-03 10:00:00', loginIp: '', loginDate: '' },
      { id: 4, username: 'user003', nickname: '王五', email: 'wangwu@company.com', phone: '13800138003', sex: '1', avatar: '', status: 0, deptId: 1, deptName: '技术部', roles: ['user'], createTime: '2024-01-04 10:00:00', loginIp: '', loginDate: '' }
    ], 4, params?.pageNum || 1, params?.pageSize || 10)
  }
  
  // 系统管理 - 角色选项
  if (path === '/system/role/options' && method === 'get') {
    return createSuccessResponse([
      { id: 1, roleName: '超级管理员', roleCode: 'admin' },
      { id: 2, roleName: '普通用户', roleCode: 'user' },
      { id: 3, roleName: '数据管理员', roleCode: 'data_admin' }
    ])
  }
  
  // 监控中心 - 概览
  if (path === '/monitor/dashboard/overview' && method === 'get') {
    return createSuccessResponse({
      todayExecutions: 15,
      todaySuccess: 12,
      todayFailed: 3,
      runningCount: 2,
      avgElapsedMs: 35000,
      avgSuccessRate7d: 92.5,
      pendingAlerts: 5,
      successRateToday: 80.0,
      successRateChange: 2.5
    })
  }
  
  // 监控中心 - 执行记录分页
  if (path === '/monitor/dashboard/execution/page' && method === 'get') {
    return createPageResponse([
      { id: 1, taskName: 'ODS层质检', taskType: 'DQC', status: 'SUCCESS', startTime: '2024-03-01 10:00:00', elapsedMs: 30000 },
      { id: 2, taskName: 'DW层质检', taskType: 'DQC', status: 'FAILED', startTime: '2024-03-01 11:00:00', elapsedMs: 5000, errorMsg: '连接超时' }
    ], 2, 1, 10)
  }
  
  // 监控中心 - 告警规则
  if (path === '/monitor/alert/rule/page' && method === 'get') {
    return createPageResponse([
      { id: 1, ruleName: '执行失败告警', ruleType: 'EXECUTION_FAIL', alertLevel: 'HIGH', enabled: true, alertReceivers: 'admin@example.com' },
      { id: 2, ruleName: '分数低于阈值', ruleType: 'SCORE_THRESHOLD', alertLevel: 'MEDIUM', enabled: true, alertReceivers: 'admin@example.com' }
    ], 2, 1, 10)
  }
  
  // 监控中心 - 告警规则类型
  if (path === '/monitor/alert/rule/options/rule-types' && method === 'get') {
    return createSuccessResponse({
      EXECUTION_FAIL: '执行失败',
      SCORE_THRESHOLD: '分数阈值',
      TASK_TIMEOUT: '任务超时'
    })
  }
  
  // 监控中心 - 告警概览
  if (path === '/monitor/alert/record/overview' && method === 'get') {
    return createSuccessResponse({
      pendingCount: 5,
      todayTotal: 10,
      todayResolved: 3,
      criticalCount: 2
    })
  }
  
  // 监控中心 - 告警分布
  if (path === '/monitor/alert/record/distribution' && method === 'get') {
    return createSuccessResponse({
      '信息': 5,
      '警告': 8,
      '错误': 3,
      '严重': 1
    })
  }
  
  // 监控中心 - 告警记录
  if (path === '/monitor/alert/record/page' && method === 'get') {
    return createPageResponse([
      { id: 1, alertNo: 'AL20240301001', ruleName: '执行失败告警', alertLevel: 'HIGH', targetName: 'ODS层质检', status: 'PENDING', createTime: '2024-03-01 10:00:00' },
      { id: 2, alertNo: 'AL20240301002', ruleName: '分数低于阈值', alertLevel: 'MEDIUM', targetName: 'DW层质检', status: 'RESOLVED', createTime: '2024-03-01 11:00:00' }
    ], 2, 1, 10)
  }
  
  // 数据探查 - 任务
  if (path === '/dprofile/task/page' && method === 'get') {
    return createPageResponse([
      { id: 1, taskName: '用户表探查', dsName: 'MySQL', tableName: 'users', status: 'COMPLETED', createTime: '2024-03-01 10:00:00' },
      { id: 2, taskName: '订单表探查', dsName: 'MySQL', tableName: 'orders', status: 'RUNNING', createTime: '2024-03-01 11:00:00' }
    ], 2, 1, 10)
  }
  
  // 数据探查 - 报告
  if (path === '/dprofile/report/page' && method === 'get') {
    return createPageResponse([
      { id: 1, tableName: 'users', rowCount: 10000, columnCount: 10, createTime: '2024-03-01 10:00:00' },
      { id: 2, tableName: 'orders', rowCount: 50000, columnCount: 15, createTime: '2024-03-01 11:00:00' }
    ], 2, 1, 10)
  }
  
  // 数据治理 - 元数据
  if (path === '/gov/metadata/page' && method === 'get') {
    return createPageResponse([
      { id: 1, tableName: 'users', tableAlias: '用户表', dataLayer: 'ODS', rowCount: 10000, status: 'ACTIVE', createTime: '2024-01-01 10:00:00' },
      { id: 2, tableName: 'orders', tableAlias: '订单表', dataLayer: 'ODS', rowCount: 50000, status: 'ACTIVE', createTime: '2024-01-02 10:00:00' }
    ], 2, 1, 10)
  }
  
  if (path === '/gov/metadata/stats' && method === 'get') {
    return createSuccessResponse({ totalTables: 100, totalColumns: 1000, dsCount: 5, activeTables: 95 })
  }
  
  // 数据血缘
  if (path === '/lineage/graph' && method === 'get') {
    return createSuccessResponse({
      nodes: [
        { id: 'ods_orders', name: 'ods_orders', type: 'table', layer: 'ODS' },
        { id: 'dwd_orders', name: 'dwd_orders', type: 'table', layer: 'DWD' },
        { id: 'dws_orders', name: 'dws_orders', type: 'table', layer: 'DWS' }
      ],
      edges: [
        { source: 'ods_orders', target: 'dwd_orders' },
        { source: 'dwd_orders', target: 'dws_orders' }
      ]
    })
  }
  
  // 资产目录
  if (path === '/gov/asset/page' && method === 'get') {
    return createPageResponse([
      { id: 1, assetName: '用户主数据', assetType: 'TABLE', owner: 'admin', createTime: '2024-01-01 10:00:00' },
      { id: 2, assetName: '订单交易数据', assetType: 'TABLE', owner: 'admin', createTime: '2024-01-02 10:00:00' }
    ], 2, 1, 10)
  }
  
  // 数据标准
  if (path === '/gov/standard/page' && method === 'get') {
    return createPageResponse([
      { id: 1, standardCode: 'STD001', standardName: '手机号格式', standardDesc: '必须为11位数字', createTime: '2024-01-01 10:00:00' },
      { id: 2, standardCode: 'STD002', standardName: '邮箱格式', standardDesc: '符合RFC标准', createTime: '2024-01-02 10:00:00' }
    ], 2, 1, 10)
  }
  
  // 业务术语
  if (path === '/gov/glossary/page' && method === 'get') {
    return createPageResponse([
      { id: 1, termCode: 'USER', termName: '用户', termDesc: '平台注册用户', bizDomain: '用户域' },
      { id: 2, termCode: 'ORDER', termName: '订单', termDesc: '用户下单记录', bizDomain: '交易域' }
    ], 2, 1, 10)
  }
  
  // 数据安全
  if (path === '/gov/security/page' && method === 'get') {
    return createPageResponse([
      { id: 1, policyName: '敏感字段脱敏', targetType: 'COLUMN', sensitivityLevel: 'HIGH', status: 'ENABLED' },
      { id: 2, policyName: '访问权限控制', targetType: 'TABLE', sensitivityLevel: 'MEDIUM', status: 'ENABLED' }
    ], 2, 1, 10)
  }
  
  // 仪表盘统计
  if (path === '/dashboard/stats' && method === 'get') {
    return createSuccessResponse({
      totalRules: 50,
      activeRules: 45,
      totalExecutions: 1000,
      successRate: 95.5,
      avgScore: 92.0
    })
  }
  
  // POST 请求处理
  if (method === 'post') {
    // 创建用户
    if (path === '/system/user') {
      return createSuccessResponse(generateId())
    }
    // 创建角色
    if (path === '/system/role') {
      return createSuccessResponse(generateId())
    }
    // 创建部门
    if (path === '/system/dept') {
      return createSuccessResponse(generateId())
    }
    // 告警规则
    if (path === '/monitor/alert/rule') {
      return createSuccessResponse(generateId())
    }
    // 告警记录已读
    if (path.includes('/monitor/alert/record/') && path.endsWith('/read')) {
      return createSuccessResponse(null)
    }
    // 告警记录解决
    if (path.includes('/monitor/alert/record/') && path.endsWith('/resolve')) {
      return createSuccessResponse(null)
    }
  }
  
  // PUT 请求处理
  if (method === 'put') {
    return createSuccessResponse(null)
  }
  
  // DELETE 请求处理
  if (method === 'delete') {
    return createSuccessResponse(null)
  }
  
  // 未找到匹配的 Mock 数据，返回 null 表示不使用 Mock
  return null
}
