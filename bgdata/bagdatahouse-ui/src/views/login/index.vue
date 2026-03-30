<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <div class="logo">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <rect width="40" height="40" rx="10" fill="#1677FF"/>
            <path d="M10 20L17 13L24 20L31 13" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
            <path d="M10 27L17 20L24 27L31 20" stroke="white" stroke-width="2.5" stroke-linecap="round" opacity="0.6"/>
          </svg>
        </div>
        <h1 class="title">数据中台工具</h1>
        <p class="subtitle">整合阿里 DataWorks 和华为 DataArts Studio 最佳实践</p>
      </div>

      <a-form
        :model="formState"
        :rules="rules"
        @finish="handleLogin"
        class="login-form"
      >
        <a-form-item name="username">
          <a-input v-model:value="formState.username" size="large" placeholder="请输入用户名">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item name="password">
          <a-input-password v-model:value="formState.password" size="large" placeholder="请输入密码">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-checkbox v-model:checked="formState.remember">记住密码</a-checkbox>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading">
            登 录
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login-tips">
        <span>默认账号：admin / admin123</span>
      </div>

      <div v-if="mockMode" class="mock-login">
        <a-divider>Mock 模式</a-divider>
        <a-space direction="vertical" style="width: 100%">
          <a-button type="dashed" block @click="handleMockLogin">
            快速登录 (Mock)
          </a-button>
          <a-tag color="orange" style="width: 100%; text-align: center">
            当前为 Mock 模式，所有 API 调用将使用模拟数据
          </a-tag>
        </a-space>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { login as loginApi } from '@/api/auth'
import { MOCK_MODE } from '@/mock/config'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const formState = reactive({
  username: 'admin',
  password: 'admin123',
  remember: false
})

const mockMode = computed(() => MOCK_MODE)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  loading.value = true
  try {
    const res = await loginApi({
      username: formState.username,
      password: formState.password,
      rememberMe: formState.remember
    })
    const token = res.data.accessToken
    userStore.setToken(token)
    userStore.setUserInfo({
      id: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      avatar: res.data.avatar,
      roles: res.data.roles
    })
    message.success('登录成功')
    router.push('/dashboard')
  } catch {
    // 错误已在 request.ts 拦截器中处理
  } finally {
    loading.value = false
  }
}

// Mock 模式快速登录
function handleMockLogin() {
  // 设置模拟 token
  const mockToken = 'mock-token-' + Date.now()
  userStore.setToken(mockToken)
  userStore.setUserInfo({
    id: 1,
    username: 'admin',
    nickname: '管理员',
    roles: ['admin']
  })
  message.success('Mock 登录成功')
  router.push('/dashboard')
}
</script>

<style lang="less" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0D1117 0%, #161B22 50%, #1C1F2E 100%);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle at 30% 50%, rgba(22, 119, 255, 0.08) 0%, transparent 50%),
                radial-gradient(circle at 70% 80%, rgba(0, 180, 219, 0.06) 0%, transparent 50%);
  }
}

.login-container {
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  .logo {
    display: flex;
    justify-content: center;
    margin-bottom: 16px;
  }

  .title {
    font-size: 24px;
    font-weight: 700;
    color: #1F1F1F;
    margin: 0 0 8px;
  }

  .subtitle {
    font-size: 13px;
    color: #8C8C8C;
    margin: 0;
  }
}

.login-form {
  :deep(.ant-input-affix-wrapper),
  :deep(.ant-input) {
    border-radius: 8px;
  }
  :deep(.ant-btn) {
    border-radius: 8px;
    height: 44px;
    font-size: 16px;
    font-weight: 600;
    background: linear-gradient(135deg, #1677FF 0%, #00B4DB 100%);
    border: none;
    box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
    transition: all 0.3s;
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(22, 119, 255, 0.4);
    }
  }
}

.login-tips {
  text-align: center;
  margin-top: 16px;
  font-size: 12px;
  color: #999;
}

.mock-login {
  margin-top: 16px;
  :deep(.ant-divider) {
    margin: 12px 0;
  }
}
</style>
