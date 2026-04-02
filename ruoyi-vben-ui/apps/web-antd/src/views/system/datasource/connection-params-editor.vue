<script setup lang="ts">
import { ref, watch } from 'vue';

interface ParamRow {
  id: number;
  key: string;
  value: string;
}

const modelValue = defineModel<string>({ default: '' });

let rowId = 0;
let syncingFromModel = false;
let syncingToModel = false;

const rows = ref<ParamRow[]>([]);

function createRow(key = '', value = ''): ParamRow {
  rowId += 1;
  return {
    id: rowId,
    key,
    value,
  };
}

function ensureRows(items: ParamRow[]) {
  rows.value = items.length > 0 ? items : [createRow()];
}

function parseConnectionParams(raw?: string): ParamRow[] {
  if (!raw) {
    return [];
  }
  const normalized = raw.trim();
  if (!normalized) {
    return [];
  }

  if (normalized.startsWith('{') && normalized.endsWith('}')) {
    try {
      const json = JSON.parse(normalized) as Record<string, unknown>;
      return Object.entries(json).map(([key, value]) =>
        createRow(key, value == null ? '' : String(value)),
      );
    } catch {
      // ignore malformed JSON and fall back to line parsing
    }
  }

  const cleaned = normalized.replace(/^[?&;]+/, '');
  return cleaned
    .split(/\r?\n|[&;]/)
    .map((segment) => segment.trim())
    .filter(Boolean)
    .map((segment) => {
      const separatorIndex = segment.includes('=')
        ? segment.indexOf('=')
        : segment.indexOf(':');
      if (separatorIndex <= 0) {
        return createRow(segment, '');
      }
      return createRow(
        segment.slice(0, separatorIndex).trim(),
        segment.slice(separatorIndex + 1).trim(),
      );
    });
}

function serializeRows(items: ParamRow[]) {
  return items
    .map((item) => ({
      key: item.key.trim(),
      value: item.value.trim(),
    }))
    .filter((item) => item.key || item.value)
    .map((item) => `${item.key}=${item.value}`)
    .join('\n');
}

function addRow() {
  rows.value.push(createRow());
}

function removeRow(id: number) {
  const nextRows = rows.value.filter((item) => item.id !== id);
  ensureRows(nextRows);
}

watch(
  modelValue,
  (value) => {
    if (syncingToModel) {
      syncingToModel = false;
      return;
    }
    syncingFromModel = true;
    ensureRows(parseConnectionParams(value));
    syncingFromModel = false;
  },
  {
    immediate: true,
  },
);

watch(
  rows,
  (value) => {
    if (syncingFromModel) {
      return;
    }
    const serialized = serializeRows(value);
    if (serialized === modelValue.value) {
      return;
    }
    syncingToModel = true;
    modelValue.value = serialized;
  },
  {
    deep: true,
  },
);
</script>

<template>
  <div class="w-full rounded-md border border-dashed border-gray-300 p-3">
    <div class="mb-2 hidden grid-cols-[1fr_1fr_72px] gap-3 px-1 text-xs text-gray-500 md:grid">
      <span>Key</span>
      <span>Value</span>
      <span>操作</span>
    </div>
    <div class="space-y-2">
      <div
        v-for="item in rows"
        :key="item.id"
        class="grid gap-2 md:grid-cols-[1fr_1fr_72px]"
      >
        <a-input
          v-model:value="item.key"
          placeholder="例如 characterEncoding"
        />
        <a-input
          v-model:value="item.value"
          placeholder="例如 utf8"
        />
        <a-button danger type="link" @click="removeRow(item.id)">
          删除
        </a-button>
      </div>
    </div>
    <div class="mt-3">
      <a-button type="dashed" @click="addRow">新增一行</a-button>
    </div>
  </div>
</template>
