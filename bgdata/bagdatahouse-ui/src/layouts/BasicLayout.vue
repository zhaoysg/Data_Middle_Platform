<template>
  <a-layout class="basic-layout">
    <!-- 侧边栏 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      :width="220"
      :collapsed-width="64"
      class="layout-sider"
    >
      <div class="logo-area">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="6" fill="#1677FF"/>
            <path d="M7 14L12 9L17 14L22 9" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <path d="M7 19L12 14L17 19L22 14" stroke="white" stroke-width="2" stroke-linecap="round" opacity="0.6"/>
          </svg>
        </div>
        <span v-show="!collapsed" class="logo-text">数据中台工具</span>
      </div>

      <!-- 静态菜单 -->
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        theme="dark"
        mode="inline"
        :inline-collapsed="collapsed"
        :triggerSubMenuAction="'hover'"
        @click="handleMenuClick"
      >
        <a-menu-item key="/dashboard">
          <template #icon><DashboardOutlined /></template>
          <span>首页</span>
        </a-menu-item>

        <!-- 数据安全 -->
        <a-sub-menu key="/security">
          <template #icon><SafetyCertificateOutlined /></template>
          <template #title>数据安全</template>
          <a-menu-item key="/security/overview">安全总览大盘</a-menu-item>
          <a-menu-item key="/security/classification">分类分级标准</a-menu-item>
          <a-menu-item key="/security/sensitivity-scan">敏感字段识别</a-menu-item>
          <a-menu-item key="/security/sensitivity-manage">敏感字段管理</a-menu-item>
          <a-menu-item key="/security/sensitivity-rules">识别规则管理</a-menu-item>
          <a-menu-item key="/security/mask-rules">脱敏规则管理</a-menu-item>
          <a-menu-item key="/security/mask-strategy">脱敏策略管理</a-menu-item>
          <a-menu-item key="/security/access-approval">访问审批管理</a-menu-item>
        </a-sub-menu>

        <!-- 数据治理 -->
        <a-sub-menu key="/governance">
          <template #icon><AccountBookOutlined /></template>
          <template #title>数据治理</template>
          <a-menu-item key="/governance/metadata">元数据管理</a-menu-item>
          <a-menu-item key="/governance/lineage">数据血缘</a-menu-item>
          <a-menu-item key="/governance/asset">资产目录</a-menu-item>
          <a-menu-item key="/governance/standard">数据标准</a-menu-item>
          <a-menu-item key="/governance/glossary">业务术语库</a-menu-item>
        </a-sub-menu>

        <!-- 数据质量 -->
        <a-sub-menu key="/dqc">
          <template #icon><SafetyCertificateOutlined /></template>
          <template #title>数据质量</template>
          <a-menu-item key="/dqc/rule-template">规则模板管理</a-menu-item>
          <a-menu-item key="/dqc/rule-def">规则定义</a-menu-item>
          <a-menu-item key="/dqc/plan">质检方案</a-menu-item>
          <a-menu-item key="/dqc/execution">执行记录</a-menu-item>
          <a-menu-item key="/dqc/score">质量评分</a-menu-item>
        </a-sub-menu>

        <!-- 数据资产管理（数据源 + 数据域） -->
        <a-sub-menu key="/asset-management">
          <template #icon><AppstoreOutlined /></template>
          <template #title>数据资产管理</template>
          <a-menu-item key="/asset-management/datasource">数据源管理</a-menu-item>
          <a-menu-item key="/asset-management/data-domain">数据域管理</a-menu-item>
        </a-sub-menu>

        <!-- 数据探查 -->
        <a-sub-menu key="/dprofile">
          <template #icon><FundOutlined /></template>
          <template #title>数据探查</template>
          <a-menu-item key="/dprofile/task">探查任务</a-menu-item>
          <a-menu-item key="/dprofile/report">探查报告</a-menu-item>
        </a-sub-menu>

        <!-- 数据监控 -->
        <a-sub-menu key="/monitor">
          <template #icon><AlertOutlined /></template>
          <template #title>数据监控</template>
          <a-menu-item key="/monitor/index">监控概览</a-menu-item>
          <a-menu-item key="/monitor/task-execution">任务执行监控</a-menu-item>
          <a-menu-item key="/monitor/alert-rule">告警规则</a-menu-item>
          <a-menu-item key="/monitor/alert-record">告警记录</a-menu-item>
        </a-sub-menu>

        <!-- 系统管理 -->
        <a-sub-menu key="/system">
          <template #icon><SettingOutlined /></template>
          <template #title>系统管理</template>
          <a-menu-item key="/system/user">用户管理</a-menu-item>
          <a-menu-item key="/system/role">角色管理</a-menu-item>
          <a-menu-item key="/system/dept">部门管理</a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <!-- 顶部导航 -->
      <a-layout-header class="layout-header">
        <div class="header-left">
          <MenuFoldOutlined class="trigger-icon" @click="toggleCollapsed" />
          <a-breadcrumb class="header-breadcrumb">
            <a-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="header-right">
          <a-tooltip title="帮助文档">
            <QuestionCircleOutlined class="header-action-icon" />
          </a-tooltip>

          <a-dropdown>
            <div class="user-avatar">
              <a-avatar :size="32" style="background-color: #1677FF">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <span class="username">{{ username }}</span>
              <DownOutlined />
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile">
                  <UserOutlined /> 个人中心
                </a-menu-item>
                <a-menu-item key="settings">
                  <SettingOutlined /> 系统设置
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 主内容区 -->
      <a-layout-content class="layout-content">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Layout,
  LayoutSider,
  LayoutHeader,
  LayoutContent,
  Menu,
  Breadcrumb,
  Tooltip,
  Dropdown,
  Avatar
} from 'ant-design-vue'
import {
  MenuFoldOutlined,
  DashboardOutlined,
  SettingOutlined,
  UserOutlined,
  QuestionCircleOutlined,
  DownOutlined,
  LogoutOutlined,
  SafetyCertificateOutlined,
  FundOutlined,
  AccountBookOutlined,
  AlertOutlined,
  AppstoreOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)
const selectedKeys = ref<string[]>([route.path])
const openKeys = ref<string[]>([route.path.replace(/\/[^/]+$/, '')])

const username = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || 'Admin')

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.map(item => ({
    title: item.meta.title as string,
    path: item.path
  }))
})

watch(() => route.path, (path) => {
  selectedKeys.value = [path]
  openKeys.value = [path.replace(/\/[^/]+$/, '')]
})

function toggleCollapsed() {
  collapsed.value = !collapsed.value
}

function handleMenuClick({ key }: { key: string }) {
  router.push(key)
}
</script>

<style lang="less" scoped>
.basic-layout {
  min-height: 100vh;
}

.layout-sider {
  background: linear-gradient(180deg, #0D1117 0%, #161B22 100%);
  .logo-area {
    height: 64px;
    display: flex;
    align-items: center;
    padding: 0 16px;
    gap: 12px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    .logo-icon {
      flex-shrink: 0;
    }
    .logo-text {
      font-size: 16px;
      font-weight: 600;
      color: #fff;
      white-space: nowrap;
      overflow: hidden;
    }
  }

  :deep(.ant-menu) {
    background: transparent;
    border-right: none;
    margin-top: 8px;
  }

  :deep(.ant-menu-dark .ant-menu-inline.ant-menu-sub) {
    background: rgba(255, 255, 255, 0.04);
  }
}

.layout-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .trigger-icon {
      font-size: 18px;
      cursor: pointer;
      color: #666;
      transition: color 0.3s;
      &:hover {
        color: #1677FF;
      }
    }

    .header-breadcrumb {
      font-size: 14px;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .header-action-icon {
      font-size: 18px;
      color: #666;
      cursor: pointer;
      transition: color 0.3s;
      &:hover {
        color: #1677FF;
      }
    }

    .user-avatar {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 6px;
      transition: background 0.3s;
      &:hover {
        background: #f5f5f5;
      }
      .username {
        font-size: 14px;
        color: #333;
      }
    }
  }
}

.layout-content {
  margin: 0;
  min-height: calc(100vh - 64px);
  background: #F5F7FA;
}
</style>
