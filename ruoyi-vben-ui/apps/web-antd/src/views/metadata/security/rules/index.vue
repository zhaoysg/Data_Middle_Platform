<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecClassification, SecLevel, SecSensitivityRule } from '#/api/metadata/model';

import { ThunderboltOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import {
  Alert,
  Button,
  Descriptions,
  DescriptionsItem,
  Input,
  Modal,
  Popconfirm,
  Space,
  Tag,
  message,
} from 'ant-design-vue';
import { computed, onMounted, ref } from 'vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  secSensitivityRuleList,
  secSensitivityRuleRemove,
  secSensitivityRuleUpdate,
} from '#/api/metadata/security/rules';
import { secClassificationList } from '#/api/metadata/security/classification';
import { secLevelList } from '#/api/metadata/security/level';
import { TableSwitch } from '#/components/table';

import rulesDrawer from './rules-drawer.vue';
import {
  RULE_TYPE_LABEL,
  displayMatchExpr,
  getPriorityFromExpr,
  matchRuleLocal,
} from './rule-utils';

const levelList = ref<SecLevel[]>([]);
const classList = ref<SecClassification[]>([]);

const levelMap = computed(() => {
  const m = new Map<string, SecLevel>();
  for (const l of levelList.value) {
    if (l.levelCode) {
      m.set(l.levelCode, l);
    }
  }
  return m;
});

const classMap = computed(() => {
  const m = new Map<string, SecClassification>();
  for (const c of classList.value) {
    if (c.classCode) {
      m.set(c.classCode, c);
    }
  }
  return m;
});

async function loadDict() {
  const [lr, cr] = await Promise.all([
    secLevelList({ pageNum: 1, pageSize: 500 }),
    secClassificationList({ pageNum: 1, pageSize: 500 }),
  ]);
  levelList.value = lr.rows ?? [];
  classList.value = cr.rows ?? [];
}

onMounted(() => {
  loadDict();
});

const formOptions = computed<VbenFormProps>(() => ({
  commonConfig: {
    labelWidth: 96,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'ruleName', label: '规则名称', component: 'Input' },
    { fieldName: 'ruleCode', label: '规则编码', component: 'Input' },
    {
      fieldName: 'ruleType',
      label: '匹配类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: RULE_TYPE_LABEL.COLUMN_NAME, value: 'COLUMN_NAME' },
          { label: RULE_TYPE_LABEL.REGEX, value: 'REGEX' },
          { label: RULE_TYPE_LABEL.DATA_TYPE, value: 'DATA_TYPE' },
          { label: RULE_TYPE_LABEL.CUSTOM, value: 'CUSTOM' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'targetLevelCode',
      label: '推荐等级',
      component: 'Select',
      componentProps: {
        options: levelList.value.map((l) => ({
          label: `${l.levelCode || ''} - ${l.levelName || ''}`,
          value: l.levelCode,
        })),
        allowClear: true,
      },
    },
    {
      fieldName: 'targetClassCode',
      label: '所属分类',
      component: 'Select',
      componentProps: {
        options: classList.value.map((c) => ({
          label: `${c.className || ''}（${c.classCode || ''}）`,
          value: c.classCode,
        })),
        allowClear: true,
      },
    },
    {
      fieldName: 'builtin',
      label: '内置',
      component: 'Select',
      componentProps: {
        options: [
          { label: '内置', value: '1' },
          { label: '自定义', value: '0' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'enabled',
      label: '启用状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '启用', value: '1' },
          { label: '禁用', value: '0' },
        ],
        allowClear: true,
      },
    },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
}));

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    {
      title: '规则名称',
      field: 'ruleName',
      width: 220,
      fixed: 'left',
      slots: { default: 'ruleNameCol' },
    },
    {
      title: '匹配类型',
      field: 'ruleType',
      width: 140,
      align: 'center',
      slots: { default: 'matchTypeCol' },
    },
    {
      title: '匹配表达式',
      field: 'ruleExpr',
      minWidth: 260,
      slots: { default: 'matchExprCol' },
    },
    {
      title: '推荐等级',
      field: 'targetLevelCode',
      width: 120,
      align: 'center',
      slots: { default: 'levelCol' },
    },
    {
      title: '所属分类',
      field: 'targetClassCode',
      width: 130,
      slots: { default: 'classCol' },
    },
    {
      title: '优先级',
      field: 'rulePriority',
      width: 88,
      align: 'center',
      slots: { default: 'priorityCol' },
    },
    { title: '启用', field: 'enabled', width: 100, slots: { default: 'enabled' } },
    { title: '创建时间', field: 'createTime', width: 170 },
    { title: '操作', field: 'action', width: 140, fixed: 'right', slots: { default: 'action' } },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async ({ page, form }: any) => {
        const params = {
          ...form,
          pageNum: page?.currentPage,
          pageSize: page?.pageSize,
        };
        const data = await secSensitivityRuleList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'sec-rules-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [RulesDrawer, drawerApi] = useVbenDrawer({ connectedComponent: rulesDrawer });

const testOpen = ref(false);
const testRow = ref<SecSensitivityRule | null>(null);
const testInput = ref('');
const testDataType = ref('');
const testLoading = ref(false);
const testMatched = ref<null | boolean>(null);

function matchTypeColor(ruleType?: string) {
  const map: Record<string, string> = {
    COLUMN_NAME: 'blue',
    REGEX: 'orange',
    DATA_TYPE: 'cyan',
    CUSTOM: 'default',
  };
  return map[ruleType || ''] || 'default';
}

function openTest(row: SecSensitivityRule) {
  testRow.value = row;
  testInput.value = '';
  testDataType.value = '';
  testMatched.value = null;
  testOpen.value = true;
}

function levelTagColor(code?: string) {
  const lv = code ? levelMap.value.get(code) : undefined;
  return lv?.color || 'red';
}

function runListTest() {
  const row = testRow.value;
  if (!row) {
    return;
  }
  const col = testInput.value.trim();
  if (!col) {
    message.warning('请输入要测试的列名');
    return;
  }
  testLoading.value = true;
  testMatched.value = null;
  try {
    const dt = row.ruleType === 'DATA_TYPE' ? testDataType.value.trim() || 'VARCHAR' : '';
    testMatched.value = matchRuleLocal(row.ruleType, row.ruleExpr, col, dt);
  } finally {
    testLoading.value = false;
  }
}

function clearListTest() {
  testInput.value = '';
  testDataType.value = '';
  testMatched.value = null;
}

const testLevelLabel = computed(() => {
  const code = testRow.value?.targetLevelCode;
  if (!code) {
    return '';
  }
  return levelMap.value.get(code)?.levelName ?? code;
});

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: SecSensitivityRule) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: SecSensitivityRule) {
  await secSensitivityRuleRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: SecSensitivityRule) => row.id)
    .filter((id): id is number => id !== undefined);
  await secSensitivityRuleRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="敏感识别规则">
      <template #toolbar-tools>
        <Space>
          <a-button type="primary" @click="handleAdd">
            <template #icon>
              <PlusOutlined />
            </template>
            新增
          </a-button>
          <a-button
            :disabled="!vxeCheckboxChecked(tableApi)"
            danger
            type="primary"
            @click="handleMultiDelete"
          >
            批量删除
          </a-button>
        </Space>
      </template>

      <template #ruleNameCol="{ row }">
        <div>
          <div class="flex flex-wrap items-center gap-1">
            <Tag v-if="row.builtin === '1'" color="gold" class="m-0 text-xs">内置</Tag>
            <span class="font-medium">{{ row.ruleName }}</span>
          </div>
          <div class="text-xs text-gray-500">{{ row.ruleCode }}</div>
        </div>
      </template>

      <template #matchTypeCol="{ row }">
        <Tag :color="matchTypeColor(row.ruleType)">
          {{ RULE_TYPE_LABEL[row.ruleType || ''] || row.ruleType || '—' }}
        </Tag>
      </template>

      <template #matchExprCol="{ row }">
        <span class="font-mono text-xs text-gray-700">{{
          displayMatchExpr(row.ruleType, row.ruleExpr) || '—'
        }}</span>
      </template>

      <template #levelCol="{ row }">
        <Tag
          v-if="row.targetLevelCode"
          :color="levelTagColor(row.targetLevelCode)"
          class="font-medium"
        >
          {{ levelMap.get(row.targetLevelCode)?.levelName || row.targetLevelCode }}
        </Tag>
        <span v-else class="text-gray-300">—</span>
      </template>

      <template #classCol="{ row }">
        <span class="text-gray-700">{{
          classMap.get(row.targetClassCode || '')?.className || row.targetClassCode || '—'
        }}</span>
      </template>

      <template #priorityCol="{ row }">
        <span class="text-gray-500">P{{ getPriorityFromExpr(row.ruleExpr) }}</span>
      </template>

      <template #enabled="{ row }">
        <TableSwitch
          v-model:value="row.enabled"
          checked-value="1"
          un-checked-value="0"
          :api="() => secSensitivityRuleUpdate({ id: row.id, enabled: row.enabled === '1' ? '0' : '1' })"
          @reload="tableApi.query()"
        />
      </template>

      <template #action="{ row }">
        <Space>
          <Button type="link" size="small" @click="openTest(row)">
            <ThunderboltOutlined />
            测试
          </Button>
          <Button type="link" size="small" @click="handleEdit(row)">编辑</Button>
          <Popconfirm
            v-if="row.builtin !== '1'"
            title="确认删除？"
            @confirm="handleDelete(row)"
          >
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
    <RulesDrawer @reload="tableApi.query()" />

    <Modal
      v-model:open="testOpen"
      :title="`测试规则 — ${testRow?.ruleName || ''}`"
      width="560px"
      :footer="null"
      destroy-on-close
      @cancel="testOpen = false"
    >
      <template v-if="testRow">
        <Descriptions bordered size="small" :column="2" class="mb-4">
          <DescriptionsItem label="规则编码">{{ testRow.ruleCode }}</DescriptionsItem>
          <DescriptionsItem label="匹配类型">
            <Tag :color="matchTypeColor(testRow.ruleType)">
              {{ RULE_TYPE_LABEL[testRow.ruleType || ''] || testRow.ruleType }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="推荐等级" :span="2">
            <Tag v-if="testRow.targetLevelCode" :color="levelTagColor(testRow.targetLevelCode)">
              {{ levelMap.get(testRow.targetLevelCode)?.levelName || testRow.targetLevelCode }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="匹配表达式" :span="2">
            <code class="text-xs text-purple-700">{{
              displayMatchExpr(testRow.ruleType, testRow.ruleExpr) || '—'
            }}</code>
          </DescriptionsItem>
        </Descriptions>

        <div class="mb-2 text-gray-600">测试列名</div>
        <Input
          v-model:value="testInput"
          placeholder="请输入列名，如 phone_no、password"
          allow-clear
          class="mb-3"
          @press-enter="runListTest"
        />
        <template v-if="testRow.ruleType === 'DATA_TYPE'">
          <div class="mb-2 text-gray-600">数据类型</div>
          <Input
            v-model:value="testDataType"
            placeholder="如 VARCHAR、TEXT"
            allow-clear
            class="mb-3"
            @press-enter="runListTest"
          />
        </template>

        <div class="mb-2 text-gray-600">测试结果</div>
        <div class="min-h-[72px]">
          <div v-if="testLoading" class="text-sm text-gray-500">正在测试…</div>
          <Alert
            v-else-if="testMatched === true"
            type="success"
            show-icon
            message="匹配成功"
            :description="`推荐等级：${testLevelLabel || '—'}`"
          />
          <Alert
            v-else-if="testMatched === false"
            type="error"
            show-icon
            message="匹配失败"
            description="该列名/类型不符合当前规则。"
          />
          <div v-else class="text-sm text-gray-500">输入内容后点击「测试匹配」</div>
        </div>
        <Space class="mt-4">
          <Button type="primary" :loading="testLoading" @click="runListTest">
            <ThunderboltOutlined />
            测试匹配
          </Button>
          <Button @click="clearListTest">重置</Button>
        </Space>
      </template>
    </Modal>
  </Page>
</template>
