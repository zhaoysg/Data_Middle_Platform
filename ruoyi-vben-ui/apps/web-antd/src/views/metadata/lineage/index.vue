<script setup lang="ts">
import { Page } from '@vben/common-ui';
import { Empty, Result } from 'ant-design-vue';
</script>

<template>
  <Page :auto-content-height="true">
    <div class="flex-center h-full">
      <Result title="鍔熻兘寮€鍙戜腑" sub-title="鏁版嵁琛€缂樼鐞嗗姛鑳芥鍦ㄥ紑鍙戜腑锛屾暚璇锋湡寰咃紒">
        <template #icon>
          <Empty />
        </template>
      </Result>
    </div>
  </Page>
</template>
