<template>
  <div class="not-found">
    <div class="content">
      <div class="code">404</div>
      <div class="message">抱歉，您访问的页面不存在</div>
      <a-button type="primary" @click="goHome">返回首页</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
function goHome() {
  router.push('/dashboard')
}
</script>

<style lang="less" scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F5F7FA;

  .content {
    text-align: center;

    .code {
      font-size: 120px;
      font-weight: 800;
      color: #E8E8E8;
      line-height: 1;
      margin-bottom: 16px;
    }

    .message {
      font-size: 18px;
      color: #666;
      margin-bottom: 32px;
    }
  }
}
</style>
