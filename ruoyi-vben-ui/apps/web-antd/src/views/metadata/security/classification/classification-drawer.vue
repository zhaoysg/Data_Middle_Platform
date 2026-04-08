<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, InputNumber, Radio, Select, message } from 'ant-design-vue';

import type { SecLevel } from '#/api/metadata/model';
import {
  secClassificationAdd,
  secClassificationInfo,
  secClassificationUpdate,
} from '#/api/metadata/security/classification';
import { secLevelList } from '#/api/metadata/security/level';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const levelOptions = ref<{ label: string; value: string }[]>([]);
const levelLoading = ref(false);

// 状态选项：value 与数据库 `sec_classification.enabled` 一致
// '0' = 启用, '1' = 停用
const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const title = computed(() => {
  return recordId.value ? '编辑数据分类' : '新增数据分类';
});

const [BasicDrawer, drawerApi] = useVbenDrawer({
  destroyOnClose: true,
  onClosed: handleClosed,
  onConfirm: handleSubmit,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    drawerApi.drawerLoading(true);
    levelLoading.value = true;
    try {
      // 加载等级下拉选项
      await loadLevelOptions();
      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await secClassificationInfo(id);
        // 防御性清理：过滤掉 undefined/null 值，防止显示 "undefined" 字符串
        formValues.value = Object.fromEntries(
          Object.entries({ ...info })
            .filter(([, v]) => v !== undefined && v !== null)
            .map(([k, v]) => [k, v === 'undefined' ? '' : v]),
        );
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
          sortOrder: 0,
        };
      }
    } finally {
      drawerApi.drawerLoading(false);
      levelLoading.value = false;
    }
  },
});

async function loadLevelOptions() {
  const data = await secLevelList({ pageNum: 1, pageSize: 500 });
  const rows = (data?.rows ?? []) as SecLevel[];
  levelOptions.value = rows
    .filter((l) => l.levelCode)
    .map((l) => ({ label: l.levelName ?? l.levelCode!, value: l.levelCode! }));
}

async function handleSubmit() {
  drawerApi.lock(true);
  try {
    if (recordId.value) {
      await secClassificationUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await secClassificationAdd(formValues.value);
    }
    message.success('保存成功');
    emit('reload');
    drawerApi.close();
  } catch (error) {
    console.error(error);
  } finally {
    drawerApi.lock(false);
  }
}

function handleClosed() {
  recordId.value = undefined;
  formValues.value = {};
}
</script>

<template>
  <BasicDrawer :title="title" class="clsf-drawer">
    <div class="clsf-drawer-body">
      <!-- 左侧图标装饰区 -->
      <div class="clsf-drawer-hint">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
          <rect width="24" height="24" rx="7" fill="url(#drGrad)" />
          <path d="M12 4L4 8.5V13L12 17.5L20 13V8.5L12 4Z" stroke="white" stroke-width="1.5" stroke-linejoin="round" />
          <path d="M12 11V17.5" stroke="white" stroke-width="1.5" stroke-linecap="round" />
          <defs>
            <linearGradient id="drGrad" x1="0" y1="0" x2="24" y2="24" gradientUnits="userSpaceOnUse">
              <stop offset="0%" stop-color="#722ED1" />
              <stop offset="100%" stop-color="#9254DE" />
            </linearGradient>
          </defs>
        </svg>
        <p class="clsf-drawer-hint-text">
          数据分类用于对敏感字段进行分类标识，可关联敏感等级并被规则引用。
        </p>
      </div>

      <Form :model="formValues" layout="vertical" class="clsf-form">
        <!-- 基础信息 -->
        <div class="clsf-form-section">
          <div class="clsf-form-section-label">基本信息</div>

          <Form.Item label="分类名称" name="className" required>
            <Input
              v-model:value="formValues.className"
              placeholder="如：个人信息、财务数据"
              :maxlength="100"
              show-count
            />
          </Form.Item>

          <Form.Item label="分类编码" name="classCode" required>
            <Input
              v-model:value="formValues.classCode"
              placeholder="唯一编码，如 PERS_INFO"
              :maxlength="50"
              show-count
            />
          </Form.Item>

          <Form.Item label="分类描述" name="classDesc">
            <Input.TextArea
              v-model:value="formValues.classDesc"
              placeholder="简要描述该分类的适用范围和管理规范"
              :rows="3"
              :maxlength="500"
              show-count
            />
          </Form.Item>
        </div>

        <!-- 关联配置 -->
        <div class="clsf-form-section">
          <div class="clsf-form-section-label">关联配置</div>

          <Form.Item
            label="关联等级"
            name="defaultLevelCode"
            :tooltip="'选择后，该分类在列表中展示默认敏感等级'"
          >
            <Select
              v-model:value="formValues.defaultLevelCode"
              allow-clear
              :loading="levelLoading"
              placeholder="选择默认关联的敏感等级"
              :options="levelOptions"
              style="width: 100%"
            />
            <div class="clsf-form-tip">
              选择后，该分类在列表中展示默认敏感等级；规则与字段数由系统自动统计。
            </div>
          </Form.Item>
        </div>

        <!-- 其他设置 -->
        <div class="clsf-form-section">
          <div class="clsf-form-section-label">其他设置</div>

          <Form.Item label="排序值" name="sortOrder" :tooltip="'数值越小排列越靠前'">
            <InputNumber
              v-model:value="formValues.sortOrder"
              :min="0"
              :max="9999"
              style="width: 100%"
              placeholder="0"
            />
          </Form.Item>

          <Form.Item label="状态" name="enabled">
            <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
          </Form.Item>
        </div>
      </Form>
    </div>
  </BasicDrawer>
</template>

<style scoped>
/* ================================================================
   数据分类抽屉 — 设计系统
   ================================================================ */

.clsf-drawer :deep(.ant-drawer-body) {
  padding: 0;
}

.clsf-drawer-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* ---- 顶部提示区 ---- */
.clsf-drawer-hint {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f0ff 100%);
  border-bottom: 1px solid #f0e8ff;
  margin-bottom: 4px;
}
.clsf-drawer-hint svg {
  flex-shrink: 0;
  margin-top: 2px;
}
.clsf-drawer-hint-text {
  font-size: 13px;
  color: #8c8ca1;
  margin: 0;
  line-height: 1.6;
}

/* ---- 表单主体 ---- */
.clsf-form {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px 24px;
}

/* ---- 分组标题 ---- */
.clsf-form-section {
  padding-top: 20px;
}
.clsf-form-section-label {
  font-size: 12px;
  font-weight: 600;
  color: #8c8ca1;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

/* ---- 辅助提示文字 ---- */
.clsf-form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #8c8ca1;
  line-height: 1.5;
}

/* ---- Form.Item 间距 ---- */
.clsf-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}
.clsf-form :deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #1a1a2e;
  font-weight: 500;
}
.clsf-form :deep(.ant-input-textarea textarea) {
  resize: none;
}
</style>
