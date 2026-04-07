<script setup lang="ts">
import { Page } from '@vben/common-ui';
import {
  Card,
  Select,
  Input,
  Button,
  Spin,
  Empty,
  Drawer,
  Descriptions,
  Tag,
  Space,
  Segmented,
  message,
} from 'ant-design-vue';
import { computed, onMounted, ref, watch, nextTick, onUnmounted } from 'vue';
import * as d3 from 'd3';
import { useRoute } from 'vue-router';
import {
  lineageUpstream,
  lineageDownstream,
  datasourceList,
  type LineageNode,
  type LineageEdge,
} from '#/api/metadata/lineage';

const route = useRoute();

type Direction = 'UPSTREAM' | 'DOWNSTREAM' | 'BOTH';
type LayoutMode = 'vertical' | 'horizontal';

// ============ State ============
const loading = ref(false);
const selectedDsId = ref<number | undefined>();
const searchTable = ref('');
const searchDepth = ref(3);
const direction = ref<Direction>('BOTH');
const layoutMode = ref<LayoutMode>('vertical');
const graphContainer = ref<HTMLDivElement>();
const selectedNode = ref<LineageNodeDetail | null>(null);
const drawerVisible = ref(false);
const nodeData = ref<Map<string, LineageNode>>(new Map());
const edgeData = ref<LineageEdge[]>([]);
const dsOptions = ref<Array<{ label: string; value: number }>>([]);
const errorMsg = ref('');
const tooltipVisible = ref(false);
const tooltipContent = ref({ x: 0, y: 0, node: null as LineageNode | null });
// 敏感血缘联动开关
const sensitivityOverlay = ref(false);
const highlightSensitive = ref(false);

// ============ Types ============
interface LineageNodeDetail {
  dsId: number;
  dsName: string;
  tableName: string;
  columnName?: string;
  layerCode?: string;
  layerName?: string;
  rowCount?: number;
  lastScanTime?: string;
  sensitivityLevel?: string;
  sensitiveCount?: number;
  upstreamCount: number;
  downstreamCount: number;
}

// ============ Layer Colors ============
const LAYER_COLORS: Record<string, string> = {
  ODS: '#E8E8E8',
  DWD: '#5BC2E7',
  DWS: '#4A90D9',
  ADS: '#2D5F8B',
  DIM: '#9B59B6',
  DEFAULT: '#666666',
};

function getLayerColor(layer?: string): string {
  return LAYER_COLORS[layer || ''] || LAYER_COLORS.DEFAULT;
}

function getSensitivityColor(level?: string): string {
  const colors: Record<string, string> = {
    HIGH: '#ff4d4f',
    MEDIUM: '#faad14',
    LOW: '#52c41a',
  };
  return colors[level || ''] || '#999';
}

// ============ Load Datasource Options ============
async function loadDsOptions() {
  try {
    const data = await datasourceList();
    dsOptions.value = (data || []).map((item: any) => ({
      label: item.dsName || item.label || '',
      value: item.dsId || item.value || 0,
    }));
  } catch {
    // fallback to mock data
    dsOptions.value = [
      { label: 'MySQL 数据源', value: 1 },
      { label: 'PostgreSQL 数据源', value: 2 },
      { label: 'Oracle 数据源', value: 3 },
    ];
  }
}

// ============ Assign Layers (Topological Sort) ============
function assignLayers(
  nodes: LineageNode[],
  edges: LineageEdge[],
  reverse: boolean,
): Map<string, number> {
  const layers = new Map<string, number>();
  const inDegree = new Map<string, number>();
  const adj = new Map<string, string[]>();

  for (const node of nodes) inDegree.set(node.id, 0);
  for (const edge of edges) {
    const [src, tgt] = reverse ? [edge.target, edge.source] : [edge.source, edge.target];
    if (!adj.has(src)) adj.set(src, []);
    adj.get(src)!.push(tgt);
    inDegree.set(tgt, (inDegree.get(tgt) || 0) + 1);
  }

  const queue: string[] = [];
  for (const [id, deg] of inDegree) if (deg === 0) queue.push(id);

  let layer = 0;
  while (queue.length > 0) {
    const size = queue.length;
    for (let i = 0; i < size; i++) {
      const nodeId = queue.shift()!;
      layers.set(nodeId, layer);
      const neighbors = adj.get(nodeId) || [];
      for (const nb of neighbors) {
        const newDeg = (inDegree.get(nb) || 1) - 1;
        inDegree.set(nb, newDeg);
        if (newDeg === 0) queue.push(nb);
      }
    }
    layer++;
  }

  return layers;
}

// ============ Build Node Detail ============
function buildNodeDetail(node: LineageNode): LineageNodeDetail {
  let upstreamCount = 0;
  let downstreamCount = 0;
  for (const edge of edgeData.value) {
    if (edge.source === node.id) downstreamCount++;
    if (edge.target === node.id) upstreamCount++;
  }
  return {
    dsId: node.dsId,
    dsName: node.dsName,
    tableName: node.tableName,
    columnName: node.columnName,
    layerCode: node.layerCode,
    layerName: node.layerName,
    rowCount: node.rowCount,
    lastScanTime: node.lastScanTime,
    sensitivityLevel: node.sensitivityLevel,
    sensitiveCount: node.sensitiveCount,
    upstreamCount,
    downstreamCount,
  };
}

// ============ Load Graph ============
async function loadGraph() {
  if (!selectedDsId.value || !searchTable.value) {
    message.warning('请选择数据源并输入表名');
    return;
  }
  loading.value = true;
  errorMsg.value = '';
  nodeData.value = new Map();
  edgeData.value = [];

  try {
    const rootId = `${selectedDsId.value}:${searchTable.value}`;
    const selectedDsLabel = dsOptions.value.find((d) => d.value === selectedDsId.value)?.label || '';

    // Build root node
    nodeData.value.set(rootId, {
      id: rootId,
      dsId: selectedDsId.value,
      dsName: selectedDsLabel,
      tableName: searchTable.value,
    });

    const promises: Promise<any>[] = [];

    if (direction.value === 'UPSTREAM' || direction.value === 'BOTH') {
      promises.push(
        lineageUpstream(selectedDsId.value, searchTable.value, searchDepth.value)
          .then((upstream) => {
            for (const item of upstream || []) {
              const nodeId = `${item.srcDsId}:${item.srcTableName}`;
              if (!nodeData.value.has(nodeId)) {
                nodeData.value.set(nodeId, {
                  id: nodeId,
                  dsId: item.srcDsId || 0,
                  dsName: item.srcDsName || '',
                  tableName: item.srcTableName || '',
                  layerCode: item.layerCode,
                  sensitivityLevel: item.sensitivityLevel,
                });
              }
              edgeData.value.push({
                id: `e_${nodeId}_${rootId}`,
                source: nodeId,
                target: rootId,
                lineageType: item.lineageType || 'TABLE',
                columnName: item.srcColumnName,
                verifyStatus: item.verifyStatus,
              });
            }
          })
          .catch(() => {}),
      );
    }

    if (direction.value === 'DOWNSTREAM' || direction.value === 'BOTH') {
      promises.push(
        lineageDownstream(selectedDsId.value, searchTable.value, searchDepth.value)
          .then((downstream) => {
            for (const item of downstream || []) {
              const nodeId = `${item.tgtDsId}:${item.tgtTableName}`;
              if (!nodeData.value.has(nodeId)) {
                nodeData.value.set(nodeId, {
                  id: nodeId,
                  dsId: item.tgtDsId || 0,
                  dsName: item.tgtDsName || '',
                  tableName: item.tgtTableName || '',
                  layerCode: item.layerCode,
                  sensitivityLevel: item.sensitivityLevel,
                });
              }
              edgeData.value.push({
                id: `e_${rootId}_${nodeId}`,
                source: rootId,
                target: nodeId,
                lineageType: item.lineageType || 'TABLE',
                columnName: item.tgtColumnName,
                verifyStatus: item.verifyStatus,
              });
            }
          })
          .catch(() => {}),
      );
    }

    await Promise.all(promises);

    if (nodeData.value.size <= 1) {
      message.info('未找到血缘关系');
    }

    await nextTick();
    renderGraph();
  } catch (e: any) {
    errorMsg.value = e?.message || '加载失败';
  } finally {
    loading.value = false;
  }
}

// ============ Render Graph ============
function renderGraph() {
  if (!graphContainer.value) return;

  // Clear previous
  d3.select(graphContainer.value).selectAll('*').remove();

  const nodes = Array.from(nodeData.value.values());
  const links = edgeData.value;

  if (nodes.length === 0) return;

  const container = graphContainer.value;
  const width = container.clientWidth || 800;
  const height = container.clientHeight || 600;

  const svg = d3
    .select(container)
    .append('svg')
    .attr('width', '100%')
    .attr('height', '100%')
    .attr('viewBox', `0 0 ${width} ${height}`)
    .attr('preserveAspectRatio', 'xMidYMid meet');

  // Define arrow marker
  const defs = svg.append('defs');
  defs
    .append('marker')
    .attr('id', 'arrowhead')
    .attr('viewBox', '-0 -5 10 10')
    .attr('refX', 22)
    .attr('refY', 0)
    .attr('orient', 'auto')
    .attr('markerWidth', 6)
    .attr('markerHeight', 6)
    .append('path')
    .attr('d', 'M 0,-5 L 10 ,0 L 0,5')
    .attr('fill', '#999');

  const isVertical = layoutMode.value === 'vertical';

  // Assign layers
  const layers = assignLayers(nodes, links, direction.value === 'UPSTREAM');

  // Group nodes by layer
  const layerNodes = new Map<number, LineageNode[]>();
  for (const node of nodes) {
    const layer = layers.get(node.id) || 0;
    if (!layerNodes.has(layer)) layerNodes.set(layer, []);
    layerNodes.get(layer)!.push(node);
  }

  const nodeWidth = 160;
  const nodeHeight = 60;
  const layerGap = isVertical ? 180 : 150;
  const nodeGap = isVertical ? 100 : 120;

  // Position nodes
  const nodePositions = new Map<string, { x: number; y: number }>();

  layerNodes.forEach((layerNodeList, layer) => {
    const l = Number(layer);
    const basePos = isVertical
      ? { x: 80 + l * (nodeWidth + layerGap), y: height / 2 }
      : { x: width / 2, y: 80 + l * (nodeHeight + layerGap) };

    const perNode = nodeGap;
    layerNodeList.forEach((node, idx) => {
      const offset = idx - (layerNodeList.length - 1) / 2;
      nodePositions.set(node.id, {
        x: isVertical ? basePos.x : basePos.x + offset * perNode,
        y: isVertical ? basePos.y + offset * perNode : basePos.y,
      });
    });
  });

  // Create zoom behavior
  const zoom = d3
    .zoom<SVGSVGElement, unknown>()
    .scaleExtent([0.1, 4])
    .on('zoom', (event) => {
      g.attr('transform', event.transform);
    });

  svg.call(zoom);

  const g = svg.append('g');

  // Draw edges
  const linkGen = isVertical
    ? d3.linkVertical<SVGPathElement, any>().x((d) => d.x).y((d) => d.y)
    : d3.linkHorizontal<SVGPathElement, any>().x((d) => d.x).y((d) => d.y);

  g.selectAll('.edge')
    .data(links)
    .join('path')
    .attr('class', 'edge')
    .attr('d', (d) => {
      const src = nodePositions.get(d.source);
      const tgt = nodePositions.get(d.target);
      if (!src || !tgt) return '';
      return linkGen({ source: src, target: tgt } as any) as string;
    })
    .attr('fill', 'none')
    .attr('stroke', (d) => {
      const srcNode = nodeData.value.get(d.source);
      return getLayerColor(srcNode?.layerCode);
    })
    .attr('stroke-width', 2)
    .attr('marker-end', 'url(#arrowhead)')
    .attr('opacity', 0.6);

  // Draw nodes
  const nodeGroup = g
    .selectAll('.node')
    .data(nodes)
    .join('g')
    .attr('class', 'node')
    .attr('transform', (d) => {
      const pos = nodePositions.get(d.id) || { x: 0, y: 0 };
      return `translate(${pos.x - nodeWidth / 2}, ${pos.y - nodeHeight / 2})`;
    })
    .style('cursor', 'pointer')
    .on('click', (_event, d) => {
      selectedNode.value = buildNodeDetail(d);
      drawerVisible.value = true;
    })
    .on('mouseenter', (event, d) => {
      tooltipContent.value = {
        x: event.pageX + 10,
        y: event.pageY + 10,
        node: d,
      };
      tooltipVisible.value = true;
    })
    .on('mousemove', (event) => {
      tooltipContent.value.x = event.pageX + 10;
      tooltipContent.value.y = event.pageY + 10;
    })
    .on('mouseleave', () => {
      tooltipVisible.value = false;
    });

  // Node rectangle background
  const overlayEnabled = sensitivityOverlay.value;
  nodeGroup
    .append('rect')
    .attr('class', 'node-bg')
    .attr('width', nodeWidth)
    .attr('height', nodeHeight)
    .attr('rx', 8)
    .attr('fill', (d) => {
      if (!overlayEnabled) return getLayerColor(d.layerCode);
      if (d.sensitivityLevel === 'HIGH') return '#ff4d4f';
      if (d.sensitivityLevel === 'MEDIUM') return '#fa8c16';
      if (d.sensitivityLevel === 'LOW') return '#fadb14';
      return getLayerColor(d.layerCode);
    })
    .attr('fill-opacity', (d) => {
      if (!overlayEnabled) return 0.12;
      if (d.sensitivityLevel) return 0.25;
      return 0.05;
    })
    .attr('stroke', (d) => {
      if (!overlayEnabled) return getLayerColor(d.layerCode);
      if (d.sensitivityLevel === 'HIGH') return '#ff4d4f';
      if (d.sensitivityLevel === 'MEDIUM') return '#fa8c16';
      if (d.sensitivityLevel === 'LOW') return '#fadb14';
      return '#ccc';
    })
    .attr('stroke-width', 2);

  // Layer badge background
  nodeGroup
    .append('rect')
    .attr('class', 'layer-badge-bg')
    .attr('x', 4)
    .attr('y', 4)
    .attr('width', 44)
    .attr('height', 18)
    .attr('rx', 4)
    .attr('fill', (d) => getLayerColor(d.layerCode));

  // Layer badge text
  nodeGroup
    .append('text')
    .attr('class', 'layer-badge-text')
    .attr('x', 26)
    .attr('y', 16)
    .attr('text-anchor', 'middle')
    .attr('fill', '#fff')
    .attr('font-size', '10px')
    .attr('font-weight', 'bold')
    .attr('pointer-events', 'none')
    .text((d) => d.layerCode || 'TABLE');

  // Table name
  nodeGroup
    .append('text')
    .attr('class', 'table-name')
    .attr('x', nodeWidth / 2)
    .attr('y', 36)
    .attr('text-anchor', 'middle')
    .attr('fill', '#333')
    .attr('font-size', '12px')
    .attr('font-weight', '600')
    .attr('pointer-events', 'none')
    .text((d) => {
      const name = d.tableName || '';
      return name.length > 18 ? name.slice(0, 16) + '…' : name;
    });

  // Datasource name
  nodeGroup
    .append('text')
    .attr('class', 'ds-name')
    .attr('x', nodeWidth / 2)
    .attr('y', 50)
    .attr('text-anchor', 'middle')
    .attr('fill', '#888')
    .attr('font-size', '10px')
    .attr('pointer-events', 'none')
    .text((d) => {
      const name = d.dsName || '';
      return name.length > 18 ? name.slice(0, 16) + '…' : name;
    });

  // Sensitive badge
  nodeGroup
    .filter((d) => d.sensitiveCount && d.sensitiveCount > 0)
    .append('rect')
    .attr('class', 'sensitive-badge-bg')
    .attr('x', nodeWidth - 26)
    .attr('y', 4)
    .attr('width', 22)
    .attr('height', 18)
    .attr('rx', 4)
    .attr('fill', '#ff4d4f');

  nodeGroup
    .filter((d) => d.sensitiveCount && d.sensitiveCount > 0)
    .append('text')
    .attr('class', 'sensitive-badge-text')
    .attr('x', nodeWidth - 15)
    .attr('y', 16)
    .attr('text-anchor', 'middle')
    .attr('fill', '#fff')
    .attr('font-size', '9px')
    .attr('font-weight', 'bold')
    .attr('pointer-events', 'none')
    .text((d) => String(d.sensitiveCount || 0));

  // Center the view
  svg.call(zoom.transform, d3.zoomIdentity);
}

// ============ Lifecycle ============
onMounted(() => {
  loadDsOptions();
  // 从血缘管理页跳转时自动加载
  if (route.query.dsId && route.query.table) {
    selectedDsId.value = Number(route.query.dsId);
    searchTable.value = String(route.query.table || '');
    nextTick(() => loadGraph());
  }
});

onUnmounted(() => {
  if (graphContainer.value) {
    d3.select(graphContainer.value).selectAll('*').remove();
  }
});

// Re-render when layout mode or sensitivity overlay changes
watch([layoutMode, sensitivityOverlay], () => {
  if (nodeData.value.size > 0) {
    nextTick(() => renderGraph());
  }
});

// ============ Computed ============
const showEmpty = computed(() => !loading.value && !errorMsg.value && nodeData.value.size === 0);
const showPrompt = computed(
  () => !selectedDsId.value || !searchTable.value,
);
</script>

<template>
  <Page :auto-content-height="true" overflow-hidden>
    <!-- Search Bar -->
    <Card class="mb-3">
      <Space wrap>
        <Select
          v-model:value="selectedDsId"
          :options="dsOptions"
          placeholder="选择数据源"
          style="width: 200px"
          allow-clear
        />
        <Input
          v-model:value="searchTable"
          placeholder="输入表名"
          style="width: 200px"
          @pressEnter="loadGraph"
        />
        <Select v-model:value="searchDepth" style="width: 120px">
          <Select.Option :value="1">深度 1</Select.Option>
          <Select.Option :value="2">深度 2</Select.Option>
          <Select.Option :value="3">深度 3</Select.Option>
          <Select.Option :value="5">深度 5</Select.Option>
        </Select>
        <Segmented
          v-model:value="direction"
          :options="[
            { label: '全血缘', value: 'BOTH' },
            { label: '上游', value: 'UPSTREAM' },
            { label: '下游', value: 'DOWNSTREAM' },
          ]"
        />
        <Segmented
          v-model:value="layoutMode"
          :options="[
            { label: '纵向', value: 'vertical' },
            { label: '横向', value: 'horizontal' },
          ]"
        />
        <Button :type="sensitivityOverlay ? 'primary' : 'default'" @click="sensitivityOverlay = !sensitivityOverlay">
          {{ sensitivityOverlay ? '隐藏敏感' : '显示敏感' }}
        </Button>
        <Button type="primary" :loading="loading" @click="loadGraph">
          查询血缘
        </Button>
      </Space>
    </Card>

    <!-- Graph Container -->
    <Card
      class="flex-1"
      :body-style="{ padding: 0, height: 'calc(100vh - 280px)', overflow: 'hidden', position: 'relative' }"
    >
      <Spin :spinning="loading" tip="加载血缘图谱...">
        <template v-if="errorMsg">
          <div class="flex-center h-full flex-col">
            <Empty description="加载失败">
              <template #image>
                <span />
              </template>
              <p class="text-red-500">{{ errorMsg }}</p>
              <Button @click="loadGraph">重试</Button>
            </Empty>
          </div>
        </template>

        <template v-else-if="showPrompt">
          <div class="flex-center h-full">
            <Empty description="请选择数据源并输入表名" />
          </div>
        </template>

        <template v-else>
          <div
            ref="graphContainer"
            class="lineage-graph"
            style="position: relative; width: 100%; height: 100%"
          />
          <!-- Sensitivity Legend Overlay -->
          <div v-if="sensitivityOverlay" class="sensitivity-legend">
            <div class="legend-title">敏感等级</div>
            <div class="legend-item">
              <span class="legend-dot" style="background:#ff4d4f" />
              <span>高敏感（HIGH）</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot" style="background:#fa8c16" />
              <span>中敏感（MEDIUM）</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot" style="background:#fadb14" />
              <span>低敏感（LOW）</span>
            </div>
          </div>
          <!-- Layer Legend -->
          <div class="layer-legend">
            <div class="legend-title">数仓分层</div>
            <div v-for="(color, layer) in LAYER_COLORS" :key="layer" class="legend-item">
              <template v-if="layer !== 'DEFAULT'">
                <span class="legend-dot" :style="{ background: color }" />
                <span>{{ layer }}</span>
              </template>
            </div>
          </div>
        </template>
      </Spin>
    </Card>

    <!-- Tooltip -->
    <Teleport to="body">
      <div
        v-if="tooltipVisible && tooltipContent.node"
        class="lineage-tooltip"
        :style="{ left: tooltipContent.x + 'px', top: tooltipContent.y + 'px' }"
      >
        <div class="tooltip-row">
          <span class="tooltip-label">表名：</span>
          <span class="tooltip-value">{{ tooltipContent.node.tableName }}</span>
        </div>
        <div class="tooltip-row">
          <span class="tooltip-label">数据源：</span>
          <span class="tooltip-value">{{ tooltipContent.node.dsName }}</span>
        </div>
        <div class="tooltip-row">
          <span class="tooltip-label">分层：</span>
          <Tag :color="getLayerColor(tooltipContent.node.layerCode)">
            {{ tooltipContent.node.layerCode || 'TABLE' }}
          </Tag>
        </div>
        <div v-if="tooltipContent.node.rowCount" class="tooltip-row">
          <span class="tooltip-label">行数：</span>
          <span class="tooltip-value">{{ tooltipContent.node.rowCount?.toLocaleString() }}</span>
        </div>
        <div v-if="tooltipContent.node.sensitiveCount" class="tooltip-row">
          <span class="tooltip-label">敏感字段：</span>
          <span class="tooltip-value">
            <Tag :color="getSensitivityColor(tooltipContent.node.sensitivityLevel)" style="margin: 0">
              {{ tooltipContent.node.sensitiveCount }}个
            </Tag>
          </span>
        </div>
      </div>
    </Teleport>

    <!-- Node Detail Drawer -->
    <Drawer
      v-model:open="drawerVisible"
      title="血缘节点详情"
      width="400"
      placement="right"
    >
      <Descriptions v-if="selectedNode" :column="1" bordered size="small">
        <Descriptions.Item label="表名">{{ selectedNode.tableName }}</Descriptions.Item>
        <Descriptions.Item label="数据源">{{ selectedNode.dsName }}</Descriptions.Item>
        <Descriptions.Item label="分层">
          <Tag :color="getLayerColor(selectedNode.layerCode)">
            {{ selectedNode.layerCode || 'TABLE' }}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="行数">
          {{ selectedNode.rowCount ? selectedNode.rowCount.toLocaleString() : '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="最后扫描">
          {{ selectedNode.lastScanTime || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="上游节点">{{ selectedNode.upstreamCount }}</Descriptions.Item>
        <Descriptions.Item label="下游节点">{{ selectedNode.downstreamCount }}</Descriptions.Item>
        <Descriptions.Item label="敏感等级">
          <Tag v-if="selectedNode.sensitivityLevel" :color="getSensitivityColor(selectedNode.sensitivityLevel)">
            {{ selectedNode.sensitivityLevel }}
          </Tag>
          <span v-else class="text-gray-400">无</span>
        </Descriptions.Item>
      </Descriptions>
    </Drawer>
  </Page>
</template>

<style scoped>
.lineage-graph {
  background: #fafafa;
  background-image: radial-gradient(circle, #ddd 1px, transparent 1px);
  background-size: 20px 20px;
}

.layer-legend {
  position: absolute;
  top: 12px;
  right: 12px;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 8px 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  z-index: 10;
}

.sensitivity-legend {
  position: absolute;
  top: 12px;
  right: 160px;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 8px 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  z-index: 10;
}

.legend-title {
  font-size: 12px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 3px;
  font-size: 11px;
  color: #666;
}

.legend-item:last-child {
  margin-bottom: 0;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.lineage-tooltip {
  position: fixed;
  z-index: 9999;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 10px 14px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  pointer-events: none;
  max-width: 280px;
}

.tooltip-row {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
  gap: 6px;
}

.tooltip-row:last-child {
  margin-bottom: 0;
}

.tooltip-label {
  color: #888;
  font-size: 12px;
  flex-shrink: 0;
}

.tooltip-value {
  color: #333;
  font-size: 12px;
  font-weight: 500;
  word-break: break-all;
}

:deep(.node) {
  transition: transform 0.15s ease;
}

:deep(.node:hover .node-bg) {
  fill-opacity: 0.25;
}

:deep(.edge) {
  transition: opacity 0.15s ease;
}
</style>
