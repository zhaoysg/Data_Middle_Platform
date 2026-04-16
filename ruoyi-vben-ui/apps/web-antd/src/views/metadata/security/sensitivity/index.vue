<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecColumnSensitivity, SecScanResultVO } from '#/api/metadata/model';

import { CloudServerOutlined, PlayCircleOutlined } from '@ant-design/icons-vue';
import { Page } from '@vben/common-ui';
import { Button, Card, Checkbox, Col, Descriptions, DescriptionsItem, Divider, Form, InputNumber, message, Modal, Progress, Row, Select, Space, Statistic, Tag, Tooltip } from 'ant-design-vue';
import { computed, h, ref } from 'vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { datasourceEnabled } from '#/api/system/datasource';
import { secColumnSensitivityList, secColumnSensitivityScan } from '#/api/metadata/security/sensitivity';

const identifiedByLabelMap: Record<string, string> = {
  AUTO: '自动识别',
  MANUAL: '手动标记',
};

const confirmedColorMap: Record<string, string> = {
  '0': 'warning',
  '1': 'success',
};

const confirmedLabelMap: Record<string, string> = {
  '0': '待确认',
  '1': '已确认',
};

const levelColorMap: Record<string, string> = {
  RED: 'red',
  ORANGE: 'orange',
  YELLOW: 'gold',
  GREEN: 'green',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'dsName', label: '数据源', component: 'Input' },
    { fieldName: 'tableName', label: '表名', component: 'Input' },
    { fieldName: 'columnName', label: '字段名', component: 'Input' },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  columns: [
    { title: '数据源', field: 'dsName', width: 150 },
    { title: '表名', field: 'tableName', width: 150 },
    { title: '字段名', field: 'columnName', width: 150 },
    { title: '数据类型', field: 'dataType', width: 120 },
    { title: '敏感等级', field: 'levelName', width: 100, slots: { default: 'levelName' } },
    { title: '识别方式', field: 'identifiedBy', width: 100 },
    { title: '确认状态', field: 'confirmed', width: 100, slots: { default: 'confirmed' } },
    { title: '创建时间', field: 'createTime', width: 180 },
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
        const data = await secColumnSensitivityList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'sec-sensitivity-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });

// ==================== 扫描向导 ====================

const scanVisible = ref(false);
const scanLoading = ref(false);
const scanResult = ref<SecScanResultVO | null>(null);
const scanProgress = ref(0);
const scanPhase = ref<'idle' | 'scanning' | 'done'>('idle');

const scanForm = ref({
  dsId: undefined as number | undefined,
  scanColumnName: true,
  scanColumnComment: true,
  scanDataType: false,
  excludeSystemTables: true,
  sampleSize: 50,
  enableContentScan: true,
  scanDimensions: ['COLUMN_NAME', 'COLUMN_COMMENT'],
});

const dataSourceList = ref<{ id: number; dsName: string; dsType: string }[]>([]);
const dsLoading = ref(false);

async function loadDataSources() {
  dsLoading.value = true;
  try {
    const res = await datasourceEnabled();
    dataSourceList.value = (res || []).map((ds: any) => ({
      id: ds.dsId || ds.id,
      dsName: ds.dsName,
      dsType: ds.dsType,
    }));
  } catch {
    message.error('加载数据源失败');
  } finally {
    dsLoading.value = false;
  }
}

function openScanDialog() {
  scanVisible.value = true;
  scanResult.value = null;
  scanProgress.value = 0;
  scanPhase.value = 'idle';
  scanForm.value = {
    dsId: undefined,
    scanColumnName: true,
    scanColumnComment: true,
    scanDataType: false,
    excludeSystemTables: true,
    sampleSize: 50,
    enableContentScan: true,
    scanDimensions: ['COLUMN_NAME', 'COLUMN_COMMENT'],
  };
  loadDataSources();
}

function handleScanDimensionsChange(checkedValues: string[]) {
  scanForm.value.scanDimensions = checkedValues;
  scanForm.value.scanColumnName = checkedValues.includes('COLUMN_NAME');
  scanForm.value.scanColumnComment = checkedValues.includes('COLUMN_COMMENT');
  scanForm.value.scanDataType = checkedValues.includes('DATA_TYPE');
}

async function handleScan() {
  if (!scanForm.value.dsId) {
    message.warning('请选择数据源');
    return;
  }
  scanLoading.value = true;
  scanPhase.value = 'scanning';
  scanProgress.value = 5;

  try {
    // 模拟进度
    const timer = setInterval(() => {
      if (scanProgress.value < 90) {
        scanProgress.value = Math.min(90, scanProgress.value + Math.floor(Math.random() * 8 + 3));
      }
    }, 500);

    const dto = {
      dsId: scanForm.value.dsId,
      scanScope: 'ALL',
      excludeSystemTables: scanForm.value.excludeSystemTables,
      scanColumnName: scanForm.value.scanColumnName,
      scanColumnComment: scanForm.value.scanColumnComment,
      scanDataType: scanForm.value.scanDataType,
      sampleSize: scanForm.value.sampleSize,
      enableContentScan: scanForm.value.enableContentScan,
      incremental: false,
      directScan: false,
      scanCycle: 'ONCE',
      enableColumnNameMatch: scanForm.value.scanColumnName,
      enableColumnCommentMatch: scanForm.value.scanColumnComment,
    };

    const res = await secColumnSensitivityScan(dto);
    clearInterval(timer);
    scanProgress.value = 100;

    if (res) {
      scanResult.value = res;
      scanPhase.value = 'done';
      message.success(`扫描完成！发现 ${res.foundSensitiveCount ?? 0} 个敏感字段`);
      tableApi.query();
    } else {
      message.error('扫描失败，请重试');
    }
  } catch (err: any) {
    scanPhase.value = 'idle';
    message.error(err?.message || '扫描请求失败');
  } finally {
    scanLoading.value = false;
  }
}

function closeScanDialog() {
  scanVisible.value = false;
  scanResult.value = null;
  scanPhase.value = 'idle';
  scanProgress.value = 0;
}

const statusColorMap: Record<string, string> = {
  SUCCESS: 'success',
  PARTIAL: 'warning',
  FAILED: 'error',
};

const statusLabelMap: Record<string, string> = {
  SUCCESS: '成功',
  PARTIAL: '部分成功',
  FAILED: '失败',
};
</script>

<template>
  <div>
    <Page :auto-content-height="true">
    <BasicTable table-title="字段敏感记录">
      <template #toolbar-tools>
        <Space>
          <Button type="primary" :icon="h(PlayCircleOutlined)" @click="openScanDialog">
            扫描敏感字段
          </Button>
        </Space>
      </template>
      <template #levelName="{ row }">
        <Tag v-if="row.levelColor" :color="levelColorMap[row.levelColor] || 'default'">
          {{ row.levelName || row.levelCode || '-' }}
        </Tag>
        <span v-else>{{ row.levelName || row.levelCode || '-' }}</span>
      </template>
      <template #identifiedBy="{ row }">
        {{ identifiedByLabelMap[row.identifiedBy || ''] || row.identifiedBy || '-' }}
      </template>
      <template #confirmed="{ row }">
        <Tag :color="confirmedColorMap[row.confirmed || '0']">
          {{ confirmedLabelMap[row.confirmed || '0'] || row.confirmed || '-' }}
        </Tag>
      </template>
    </BasicTable>
  </Page>

  <!-- 扫描向导弹窗 -->
  <Modal
    v-model:open="scanVisible"
    title="敏感字段扫描"
    :width="680"
    :footer="scanPhase === 'done' ? null : undefined"
    @cancel="closeScanDialog"
  >
    <!-- 扫描结果展示 -->
    <div v-if="scanPhase === 'done' && scanResult" class="scan-result-container">
      <Card size="small" :bordered="false" class="result-summary-card">
        <Descriptions :column="2" bordered size="small">
          <DescriptionsItem label="数据源">{{ scanResult.dsName }}</DescriptionsItem>
          <DescriptionsItem label="批次号">
            <code>{{ scanResult.batchNo }}</code>
          </DescriptionsItem>
          <DescriptionsItem label="扫描表数">{{ scanResult.totalTableCount ?? 0 }}</DescriptionsItem>
          <DescriptionsItem label="扫描字段数">{{ scanResult.totalColumnCount ?? 0 }}</DescriptionsItem>
          <DescriptionsItem label="发现敏感字段">
            <Statistic :value="scanResult.foundSensitiveCount ?? 0" :value-style="{ color: (scanResult.foundSensitiveCount ?? 0) > 0 ? '#52c41a' : '#1890ff' }" />
          </DescriptionsItem>
          <DescriptionsItem label="扫描状态">
            <Tag :color="statusColorMap[scanResult.status || 'PARTIAL']">
              {{ statusLabelMap[scanResult.status || 'PARTIAL'] }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="新增">{{ scanResult.newSensitiveCount ?? 0 }}</DescriptionsItem>
          <DescriptionsItem label="更新">{{ scanResult.updatedSensitiveCount ?? 0 }}</DescriptionsItem>
          <DescriptionsItem label="耗时">{{ scanResult.costMs ? `${scanResult.costMs}ms` : '-' }}</DescriptionsItem>
          <DescriptionsItem label="扫描时间">{{ new Date().toLocaleString() }}</DescriptionsItem>
        </Descriptions>
      </Card>

      <div v-if="scanResult.sensitiveFields?.length" style="margin-top: 16px">
        <Divider>发现的敏感字段</Divider>
        <div class="sensitive-fields-list">
          <Tag v-for="field in scanResult.sensitiveFields" :key="field.id" color="purple" style="margin: 4px">
            {{ field.tableName }}.{{ field.columnName }}
            <span style="opacity: 0.7">({{ field.levelName || field.levelCode }})</span>
          </Tag>
        </div>
      </div>
    </div>

    <!-- 扫描配置表单 -->
    <template v-else>
      <Form layout="vertical" size="large">
        <Divider orientation="left">数据源选择</Divider>
        <Form.Item label="数据源" required>
          <Select
            v-model:value="scanForm.dsId"
            placeholder="请选择要扫描的数据源"
            :loading="dsLoading"
            :options="dataSourceList.map(ds => ({ value: ds.id, label: `${ds.dsName} (${ds.dsType})` }))"
            style="width: 100%"
          />
        </Form.Item>

        <Divider orientation="left">扫描维度</Divider>
        <Form.Item>
          <Checkbox.Group :value="scanForm.scanDimensions" @change="handleScanDimensionsChange">
            <Space direction="vertical" style="width: 100%">
              <Row :gutter="16">
                <Col :span="8">
                  <Checkbox value="COLUMN_NAME">
                    <Tooltip title="匹配字段名中的敏感关键词，如 id_card、phone、email">
                      <span>字段名匹配</span>
                    </Tooltip>
                  </Checkbox>
                </Col>
                <Col :span="8">
                  <Checkbox value="COLUMN_COMMENT">
                    <Tooltip title="匹配字段注释中的敏感关键词">
                      <span>注释匹配</span>
                    </Tooltip>
                  </Checkbox>
                </Col>
                <Col :span="8">
                  <Checkbox value="DATA_TYPE">
                    <Tooltip title="根据数据类型（VARCHAR/TEXT 等）识别敏感字段">
                      <span>数据类型匹配</span>
                    </Tooltip>
                  </Checkbox>
                </Col>
              </Row>
            </Space>
          </Checkbox.Group>
        </Form.Item>

        <Divider orientation="left">高级选项</Divider>

        <Row :gutter="16">
          <Col :span="12">
            <Form.Item label="采样数量">
              <InputNumber
                v-model:value="scanForm.sampleSize"
                :min="0"
                :max="200"
                :step="10"
                style="width: 100%"
              />
              <div style="font-size: 12px; color: #999; margin-top: 4px">每个字段采样行数（0=不采样，最大200）</div>
            </Form.Item>
          </Col>
          <Col :span="12">
            <Form.Item label="采样增强">
              <Checkbox v-model:checked="scanForm.enableContentScan">
                启用内容级扫描
                <Tooltip title="通过数据采样增强敏感字段识别的置信度">
                  <CloudServerOutlined style="margin-left: 4px" />
                </Tooltip>
              </Checkbox>
            </Form.Item>
          </Col>
        </Row>

        <Form.Item>
          <Checkbox v-model:checked="scanForm.excludeSystemTables">
            排除系统表（sys_、mysql_、information_ 开头）
          </Checkbox>
        </Form.Item>

        <Divider />

        <div v-if="scanPhase === 'scanning'" class="scan-progress-section">
          <div class="progress-label">
            <span>正在扫描…</span>
            <span>{{ scanProgress }}%</span>
          </div>
          <Progress :percent="scanProgress" :status="scanProgress >= 100 ? 'success' : 'active'" size="small" />
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            正在匹配敏感识别规则，请稍候…
          </div>
        </div>
      </Form>
    </template>

    <template #footer v-if="scanPhase !== 'done'">
      <Space>
        <Button @click="closeScanDialog" :disabled="scanLoading">取消</Button>
        <Button type="primary" :loading="scanLoading" :disabled="!scanForm.dsId" @click="handleScan">
          <PlayCircleOutlined />
          开始扫描
        </Button>
      </Space>
    </template>
    <template #footer v-else>
      <Space>
        <Button @click="closeScanDialog">关闭</Button>
        <Button type="primary" @click="openScanDialog">
          <PlayCircleOutlined />
          再次扫描
        </Button>
      </Space>
    </template>
    </Modal>
  </div>
</template>

<style scoped>
.scan-progress-section {
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 16px;
}
.progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}
.sensitive-fields-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px;
  background: #f5f5f5;
  border-radius: 6px;
}
</style>
