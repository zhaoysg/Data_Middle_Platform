<script setup lang="ts">
import { Page } from '@vben/common-ui';
import { Empty, Result } from 'ant-design-vue';
</script>

<template>
  <Page :auto-content-height="true">
    <div class="flex-center h-full">
      <Result title="功能开发中" sub-title="数据质量方案管理功能正在开发中，敬请期待！">
        <template #icon>
          <Empty />
        </template>
      </Result>
    </div>
  </Page>
</template>
