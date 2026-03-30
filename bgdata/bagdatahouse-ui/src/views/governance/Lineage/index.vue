<template>
  <div class="page-container">
    <!-- ========== 页面标题区 ========== -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#lineageGrad)"/>
            <circle cx="7" cy="12" r="2.5" stroke="white" stroke-width="1.5"/>
            <circle cx="17" cy="7" r="2.5" stroke="white" stroke-width="1.5"/>
            <circle cx="17" cy="17" r="2.5" stroke="white" stroke-width="1.5"/>
            <path d="M9.5 11L14.5 8" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M9.5 13L14.5 16" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <defs>
              <linearGradient id="lineageGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#1677FF"/>
                <stop offset="100%" stop-color="#00B4DB"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">数据血缘</h1>
          <p class="page-subtitle">可视化血缘关系，支持表级/字段级完整血缘，支持影响分析与回溯分析</p>
        </div>
      </div>
      <div class="header-right">
        <a-space>
          <a-button @click="openImportModal">
            <template #icon><UploadOutlined /></template>
            批量导入
          </a-button>
          <a-button @click="openAddDrawer" type="primary">
            <template #icon><PlusOutlined /></template>
            新增血缘
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- ========== 统计卡片 ========== -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="8" :md="4" v-for="stat in statCards" :key="stat.label">
        <div class="stat-mini-card" :style="{ background: stat.bg }">
          <div class="mini-value">{{ stat.value }}</div>
          <div class="mini-label">{{ stat.label }}</div>
        </div>
      </a-col>
    </a-row>

    <!-- ========== 工具栏 ========== -->
    <div class="toolbar">
      <a-space wrap>
        <!-- 血缘类型 -->
        <a-segmented
          v-model:value="viewMode"
          :options="[
            { label: '全部血缘', value: 'ALL' },
            { label: '表级血缘', value: 'TABLE' },
            { label: '字段级血缘', value: 'COLUMN' },
          ]"
          @change="onViewModeChange"
        />
        <a-divider type="vertical" />

        <!-- 筛选 -->
        <a-select
          v-model:value="filterDsId"
          placeholder="选择数据源"
          style="width: 160px"
          allowClear
          :loading="dsLoading"
          @change="loadGraph"
        >
          <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
            {{ ds.dsName }}
          </a-select-option>
        </a-select>

        <!-- 搜索节点 -->
        <a-input
          v-model:value="searchKeyword"
          placeholder="搜索表名 / 字段名"
          style="width: 200px"
          allowClear
          @pressEnter="handleSearch"
          @change="handleSearchChange"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>

        <!-- 转换类型筛选 -->
        <a-select
          v-model:value="filterTransformType"
          placeholder="转换类型"
          style="width: 120px"
          allowClear
          @change="loadGraph"
        >
          <a-select-option v-for="t in transformTypeOptions" :key="t.value" :value="t.value">
            {{ t.label }}
          </a-select-option>
        </a-select>

        <a-divider type="vertical" />

        <!-- 视图控制 -->
        <a-tooltip title="适应画布">
          <a-button @click="fitToScreen" :icon="h(CompressOutlined)" />
        </a-tooltip>
        <a-tooltip title="放大">
          <a-button @click="zoomIn" :icon="h(ZoomInOutlined)" />
        </a-tooltip>
        <a-tooltip title="缩小">
          <a-button @click="zoomOut" :icon="h(ZoomOutOutlined)" />
        </a-tooltip>
        <a-tooltip title="居中">
          <a-button @click="centerGraph" :icon="h(AimOutlined)" />
        </a-tooltip>

        <a-divider type="vertical" />

        <a-tooltip title="切换分层布局">
          <a-button @click="toggleLayout" :icon="h(ColumnWidthOutlined)" />
        </a-tooltip>
        <a-tooltip title="切换力导向布局">
          <a-button @click="toggleLayout" :icon="h(AppstoreOutlined)" />
        </a-tooltip>

        <a-divider type="vertical" />

        <!-- 层级过滤 -->
        <a-select
          v-model:value="filterMaxDepth"
          placeholder="追溯深度"
          style="width: 120px"
          @change="onDepthChange"
        >
          <a-select-option :value="2">深度 2</a-select-option>
          <a-select-option :value="3">深度 3</a-select-option>
          <a-select-option :value="5">深度 5</a-select-option>
          <a-select-option :value="10">深度 10</a-select-option>
        </a-select>

        <a-divider type="vertical" />

        <!-- 敏感等级过滤 -->
        <a-select
          v-model:value="filterSensitivityLevel"
          placeholder="敏感等级"
          style="width: 120px"
          allowClear
          @change="onSensitivityLevelChange"
        >
          <a-select-option value="L4">L4 机密</a-select-option>
          <a-select-option value="L3">L3 敏感</a-select-option>
          <a-select-option value="L2">L2 内部</a-select-option>
          <a-select-option value="L1">L1 公开</a-select-option>
        </a-select>

        <!-- 仅显示敏感节点 -->
        <a-switch
          v-model:checked="showSensitiveOnly"
          size="small"
          @change="onShowSensitiveOnlyChange"
        />
        <span style="font-size: 12px; color: #8C8C8C">仅显示敏感节点</span>

        <!-- 影响分析入口 -->
        <a-dropdown>
          <a-button>
            <template #icon><NodeExpandOutlined /></template>
            影响分析
          </a-button>
          <template #overlay>
            <a-menu @click="handleImpactAnalyze">
              <a-menu-item key="downstream">查看下游影响</a-menu-item>
              <a-menu-item key="upstream">回溯上游来源</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-space>

      <!-- 图例 -->
      <div class="graph-legend">
        <span class="legend-title">数据层</span>
        <span v-for="layer in layerLegend" :key="layer.value" class="legend-item">
          <span class="legend-dot" :style="{ background: layer.color }"></span>
          {{ layer.label }}
        </span>
        <span class="legend-divider">|</span>
        <span class="legend-title">敏感</span>
        <span v-for="sens in sensitivityLegend" :key="sens.value" class="legend-item">
          <span class="legend-dot" :style="{ background: sens.color }"></span>
          {{ sens.label }}
        </span>
        <span class="legend-divider">|</span>
        <span class="legend-item">
          <span class="legend-node-table"></span>表节点
        </span>
        <span class="legend-item">
          <span class="legend-node-col"></span>字段节点
        </span>
      </div>
    </div>

    <!-- ========== 主内容区 ========== -->
    <div class="main-content">
      <!-- DAG 图画布 -->
      <div class="graph-container" ref="graphContainerRef">
        <div v-if="loading" class="graph-loading">
          <a-spin size="large" tip="加载血缘图谱..." />
        </div>
        <div v-else-if="graphData.nodes?.length === 0" class="graph-empty">
          <div class="empty-icon">
            <svg width="80" height="80" viewBox="0 0 80 80" fill="none">
              <circle cx="40" cy="30" r="10" stroke="#1677FF" stroke-width="2" stroke-dasharray="4 3" opacity="0.4"/>
              <circle cx="15" cy="60" r="8" stroke="#52C41A" stroke-width="2" stroke-dasharray="4 3" opacity="0.4"/>
              <circle cx="40" cy="65" r="8" stroke="#FAAD14" stroke-width="2" stroke-dasharray="4 3" opacity="0.4"/>
              <circle cx="65" cy="60" r="8" stroke="#722ED1" stroke-width="2" stroke-dasharray="4 3" opacity="0.4"/>
              <path d="M30 30L15 52M40 30L40 57M50 30L65 52" stroke="#1677FF" stroke-width="1.5" stroke-dasharray="4 3" opacity="0.3"/>
            </svg>
          </div>
          <div class="empty-text">暂无血缘数据</div>
          <div class="empty-desc">请先在左侧添加血缘关系，或扫描元数据自动解析</div>
          <a-button type="primary" @click="openAddDrawer">
            <template #icon><PlusOutlined /></template>
            新增第一条血缘
          </a-button>
        </div>
        <svg ref="svgRef" class="lineage-svg" @click="onSvgClick">
          <defs>
            <!-- 箭头标记（按数据层颜色） -->
            <marker
              v-for="layer in layerLegend"
              :key="`arrow-${layer.value}`"
              :id="`arrow-${layer.value}`"
              markerWidth="10"
              markerHeight="7"
              refX="10"
              refY="3.5"
              orient="auto"
            >
              <polygon points="0 0, 10 3.5, 0 7" :fill="layer.color" opacity="0.7"/>
            </marker>
            <!-- 默认箭头 -->
            <marker id="arrow-default" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#8C8C8C" opacity="0.7"/>
            </marker>
            <!-- 发光滤镜 -->
            <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
              <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
            <!-- 阴影 -->
            <filter id="nodeShadow" x="-10%" y="-10%" width="130%" height="140%">
              <feDropShadow dx="0" dy="2" stdDeviation="4" flood-color="rgba(0,0,0,0.15)"/>
            </filter>
          </defs>
        </svg>
        <!-- 节点详情面板 -->
        <transition name="slide-panel">
          <div v-if="selectedNode" class="node-detail-panel">
            <div class="panel-header">
              <div class="panel-title">
                <span class="node-type-badge" :class="selectedNode.nodeType.toLowerCase()">
                  {{ selectedNode.nodeType === 'TABLE' ? '表' : '字段' }}
                </span>
                <span class="node-name">{{ selectedNode.tableName }}</span>
              </div>
              <a-button type="text" size="small" @click="selectedNode = null">
                <template #icon><CloseOutlined /></template>
              </a-button>
            </div>
            <div class="panel-body">
              <a-descriptions :column="1" size="small">
                <a-descriptions-item label="数据源">{{ selectedNode.dsName || '-' }}</a-descriptions-item>
                <a-descriptions-item label="数据源类型">{{ selectedNode.dsType || '-' }}</a-descriptions-item>
                <a-descriptions-item label="数据层">
                  <span class="layer-tag" :style="{ background: getLayerColor(selectedNode.dataLayer) }">
                    {{ selectedNode.dataLayer || '-' }}
                  </span>
                </a-descriptions-item>
                <a-descriptions-item label="敏感等级">
                  <span
                    v-if="selectedNode.sensitivityLevel"
                    class="sensitivity-badge"
                    :style="{
                      borderColor: getSensitivityBorderColor(selectedNode.sensitivityLevel),
                      color: getSensitivityTextColor(selectedNode.sensitivityLevel),
                      background: getSensitivityBgColor(selectedNode.sensitivityLevel)
                    }"
                  >
                    <SafetyCertificateOutlined />
                    {{ selectedNode.sensitivityLevel }}
                  </span>
                  <span v-else style="color: #8C8C8C; font-size: 12px">未分级</span>
                </a-descriptions-item>
                <a-descriptions-item v-if="selectedNode.columnName" label="字段名">
                  <code class="col-name">{{ selectedNode.columnName }}</code>
                </a-descriptions-item>
                <a-descriptions-item v-if="selectedNode.columnAlias" label="字段中文名">
                  {{ selectedNode.columnAlias }}
                </a-descriptions-item>
                <a-descriptions-item v-if="selectedNode.tableAlias" label="表中文名">
                  {{ selectedNode.tableAlias }}
                </a-descriptions-item>
              </a-descriptions>

              <a-divider>血缘关系</a-divider>

              <div class="panel-actions">
                <a-button block size="small" @click="analyzeFromNode('downstream')">
                  <template #icon><ArrowRightOutlined /></template>
                  查看下游影响
                </a-button>
                <a-button block size="small" @click="analyzeFromNode('upstream')">
                  <template #icon><ArrowLeftOutlined /></template>
                  回溯上游来源
                </a-button>
              </div>
            </div>
          </div>
        </transition>
      </div>

      <!-- 右侧血缘记录列表 -->
      <div class="lineage-list-panel">
        <div class="list-panel-header">
          <span class="list-title">血缘记录</span>
          <a-badge :count="totalLineages" :overflow-count="999" />
        </div>
        <div class="list-filter">
          <a-input
            v-model:value="listSearchKeyword"
            placeholder="搜索源/目标表"
            size="small"
            allowClear
            @pressEnter="loadLineageList"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </div>
        <div class="list-body" ref="listBodyRef">
          <div
            v-for="item in lineageList"
            :key="item.id"
            class="lineage-list-item"
            :class="{ active: hoveredLineageId === item.id }"
            @click="onLineageItemClick(item)"
            @mouseenter="hoveredLineageId = item.id"
            @mouseleave="hoveredLineageId = null"
          >
            <div class="item-header">
              <a-tag :color="item.lineageType === 'TABLE' ? '#1677FF' : '#722ED1'" size="small">
                {{ item.lineageTypeLabel }}
              </a-tag>
              <a-tag :color="item.lineageSource === 'MANUAL' ? 'green' : 'blue'" size="small">
                {{ item.lineageSourceLabel }}
              </a-tag>
            </div>
            <div class="item-flow">
              <span class="flow-table" :title="item.sourceTable">
                <DatabaseOutlined /> {{ truncate(item.sourceTable, 14) }}
              </span>
              <ArrowRightOutlined class="flow-arrow" />
              <span class="flow-transform">{{ item.transformTypeLabel || '-' }}</span>
              <ArrowRightOutlined class="flow-arrow" />
              <span class="flow-table" :title="item.targetTable">
                <DatabaseOutlined /> {{ truncate(item.targetTable, 14) }}
              </span>
            </div>
            <div class="item-meta">
              <span>{{ item.sourceDsName }}</span>
              <span v-if="item.transformType">· {{ item.transformTypeLabel }}</span>
            </div>
            <div class="item-actions" @click.stop>
              <a-tooltip title="查看详情">
                <a-button type="text" size="small" @click="openDetailDrawer(item)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="编辑">
                <a-button type="text" size="small" @click="openEditDrawer(item)">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="删除">
                <a-popconfirm
                  title="确定删除该血缘记录？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="handleDelete(item.id)"
                >
                  <a-button type="text" size="small" danger>
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </a-tooltip>
            </div>
          </div>
          <div v-if="lineageListLoading" class="list-loading">
            <a-spin size="small" />
          </div>
          <div v-else-if="lineageList.length === 0" class="list-empty">
            暂无记录
          </div>
        </div>
        <div class="list-footer">
          <a-pagination
            v-model:current="listPage"
            v-model:pageSize="listPageSize"
            :total="totalLineages"
            size="small"
            :showSizeChanger="false"
            :showTotal="(total: number) => `共 ${total} 条`"
            @change="loadLineageList"
          />
        </div>
      </div>
    </div>

    <!-- ========== 新增/编辑血缘抽屉 ========== -->
    <a-drawer
      v-model:open="addDrawerVisible"
      :title="editMode ? '编辑血缘记录' : '新增血缘记录'"
      :width="600"
      @close="closeAddDrawer"
    >
      <a-steps :current="stepCurrent" size="small" style="margin-bottom: 24px">
        <a-step title="血缘类型" />
        <a-step title="源端信息" />
        <a-step title="目标端信息" />
        <a-step title="转换信息" />
      </a-steps>

      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <!-- 步骤1：血缘类型 -->
        <div v-show="stepCurrent === 0">
          <a-form-item label="血缘类型" name="lineageType" :rules="[{ required: true }]">
            <a-radio-group v-model:value="formData.lineageType" button-style="solid">
              <a-radio-button value="TABLE">表级血缘</a-radio-button>
              <a-radio-button value="COLUMN">字段级血缘</a-radio-button>
            </a-radio-group>
            <div class="form-tip">
              <span v-if="formData.lineageType === 'TABLE'">表级血缘：描述表与表之间的依赖关系</span>
              <span v-else>字段级血缘：描述字段与字段之间的转换关系（需要填写源字段和目标字段）</span>
            </div>
          </a-form-item>
          <a-form-item label="血缘来源" name="lineageSource">
            <a-select v-model:value="formData.lineageSource" placeholder="选择血缘来源">
              <a-select-option value="MANUAL">手动录入</a-select-option>
              <a-select-option value="AUTO_PARSER">自动解析</a-select-option>
            </a-select>
          </a-form-item>
        </div>

        <!-- 步骤2：源端信息 -->
        <div v-show="stepCurrent === 1">
          <a-form-item label="源数据源" name="sourceDsId" :rules="[{ required: true, message: '请选择源数据源' }]">
            <a-select
              v-model:value="formData.sourceDsId"
              placeholder="请选择源数据源"
              :loading="dsLoading"
              @change="onSourceDsChange"
            >
              <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                {{ ds.dsName }} ({{ ds.dsType }})
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="源表名" name="sourceTable" :rules="[{ required: true, message: '请输入或选择源表名' }]">
            <a-auto-complete
              v-model:value="formData.sourceTable"
              placeholder="输入或选择源表名"
              :options="sourceTableOptions.map(t => ({ value: t.tableName, label: `${t.tableName} ${t.tableAlias ? '(' + t.tableAlias + ')' : ''} ${t.dataLayer ? '[' + t.dataLayer + ']' : ''}` }))"
              @select="(val: string) => { formData.sourceTable = val; onSourceTableChange() }"
              @search="(val: string) => { if (!val) { sourceTableOptions = [] } else { handleSourceTableSearch(val) } }"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item
            v-if="formData.lineageType === 'COLUMN'"
            label="源字段"
            name="sourceColumn"
            :rules="[{ required: true, message: '字段级血缘必须选择源字段' }]"
          >
            <a-select
              v-model:value="formData.sourceColumn"
              placeholder="请选择源字段"
              :loading="sourceColLoading"
              allowClear
              show-search
            >
              <a-select-option v-for="col in sourceColOptions" :key="col.value" :value="col.value">
                {{ col.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="源字段中文名" name="sourceColumnAlias" v-if="formData.lineageType === 'COLUMN'">
            <a-input v-model:value="formData.sourceColumnAlias" placeholder="可选，填写源字段中文名" />
          </a-form-item>
        </div>

        <!-- 步骤3：目标端信息 -->
        <div v-show="stepCurrent === 2">
          <a-form-item label="目标数据源" name="targetDsId" :rules="[{ required: true, message: '请选择目标数据源' }]">
            <a-select
              v-model:value="formData.targetDsId"
              placeholder="请选择目标数据源"
              :loading="dsLoading"
              @change="onTargetDsChange"
            >
              <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                {{ ds.dsName }} ({{ ds.dsType }})
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="目标表名" name="targetTable" :rules="[{ required: true, message: '请输入或选择目标表名' }]">
            <a-auto-complete
              v-model:value="formData.targetTable"
              placeholder="输入或选择目标表名"
              :options="targetTableOptions.map(t => ({ value: t.tableName, label: `${t.tableName} ${t.tableAlias ? '(' + t.tableAlias + ')' : ''} ${t.dataLayer ? '[' + t.dataLayer + ']' : ''}` }))"
              @select="(val: string) => { formData.targetTable = val; onTargetTableChange() }"
              @search="(val: string) => { if (!val) { targetTableOptions = [] } else { handleTargetTableSearch(val) } }"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item
            v-if="formData.lineageType === 'COLUMN'"
            label="目标字段"
            name="targetColumn"
            :rules="[{ required: true, message: '字段级血缘必须选择目标字段' }]"
          >
            <a-select
              v-model:value="formData.targetColumn"
              placeholder="请选择目标字段"
              :loading="targetColLoading"
              allowClear
              show-search
            >
              <a-select-option v-for="col in targetColOptions" :key="col.value" :value="col.value">
                {{ col.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="目标字段中文名" name="targetColumnAlias" v-if="formData.lineageType === 'COLUMN'">
            <a-input v-model:value="formData.targetColumnAlias" placeholder="可选，填写目标字段中文名" />
          </a-form-item>
        </div>

        <!-- 步骤4：转换信息 -->
        <div v-show="stepCurrent === 3">
          <a-form-item label="转换类型" name="transformType">
            <a-select v-model:value="formData.transformType" placeholder="选择转换类型" allowClear>
              <a-select-option v-for="t in transformTypeOptions" :key="t.value" :value="t.value">
                {{ t.label }}
              </a-select-option>
            </a-select>
            <div class="form-tip">描述数据从源到目标的转换方式</div>
          </a-form-item>
          <a-form-item label="转换表达式" name="transformExpr">
            <a-textarea
              v-model:value="formData.transformExpr"
              placeholder="填写转换表达式描述，如 A + B，或 CASE WHEN ..."
              :rows="3"
            />
            <div class="form-tip">支持描述字段间的计算或转换逻辑</div>
          </a-form-item>
          <a-form-item label="来源作业" name="jobName">
            <a-input v-model:value="formData.jobName" placeholder="可选，填写来源作业名称，如 Kettle_job_order" />
          </a-form-item>

          <!-- 预览 -->
          <a-divider>预览</a-divider>
          <div class="preview-box">
            <div class="preview-flow">
              <span class="preview-node">
                <DatabaseOutlined /> {{ formData.sourceTable || '源表' }}
                <span v-if="formData.lineageType === 'COLUMN' && formData.sourceColumn">.{{ formData.sourceColumn }}</span>
              </span>
              <ArrowRightOutlined class="preview-arrow" />
              <span class="preview-transform">{{ getTransformLabel(formData.transformType) }}</span>
              <ArrowRightOutlined class="preview-arrow" />
              <span class="preview-node">
                <DatabaseOutlined /> {{ formData.targetTable || '目标表' }}
                <span v-if="formData.lineageType === 'COLUMN' && formData.targetColumn">.{{ formData.targetColumn }}</span>
              </span>
            </div>
          </div>
        </div>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button v-if="stepCurrent > 0" @click="stepCurrent--">上一步</a-button>
          <a-button v-if="stepCurrent < 3" type="primary" @click="handleNextStep">下一步</a-button>
          <a-button v-if="stepCurrent === 3" type="primary" :loading="saving" @click="handleSave">
            {{ editMode ? '保存修改' : '确认创建' }}
          </a-button>
        </a-space>
        <a-button @click="closeAddDrawer">取消</a-button>
      </div>
    </a-drawer>

    <!-- ========== 血缘详情抽屉 ========== -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      :title="`血缘详情 - ID: ${currentLineage?.id}`"
      :width="640"
      @close="detailDrawerVisible = false"
    >
      <div v-if="currentLineage" class="detail-content">
        <a-descriptions :column="2" size="small" bordered title="基本信息">
          <a-descriptions-item label="血缘类型" :span="2">
            <a-tag :color="currentLineage.lineageType === 'TABLE' ? '#1677FF' : '#722ED1'">
              {{ currentLineage.lineageTypeLabel }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="血缘来源">
            <a-tag :color="currentLineage.lineageSource === 'MANUAL' ? 'green' : 'blue'">
              {{ currentLineage.lineageSourceLabel }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge :status="currentLineage.status === 'ACTIVE' ? 'success' : 'default'" :text="currentLineage.status === 'ACTIVE' ? '活跃' : currentLineage.status" />
          </a-descriptions-item>
        </a-descriptions>

        <a-divider orientation="left">数据流向</a-divider>
        <div class="flow-card">
          <div class="flow-card-side source">
            <div class="flow-card-label">源端</div>
            <div class="flow-card-ds">{{ currentLineage.sourceDsName }}</div>
            <div class="flow-card-table">
              <DatabaseOutlined /> {{ currentLineage.sourceTable }}
            </div>
            <div v-if="currentLineage.sourceColumn" class="flow-card-col">
              {{ currentLineage.sourceColumn }}
              <span v-if="currentLineage.sourceColumnAlias" class="col-alias">({{ currentLineage.sourceColumnAlias }})</span>
            </div>
          </div>
          <div class="flow-card-arrow">
            <ArrowRightOutlined />
            <div class="flow-transform-tag">{{ currentLineage.transformTypeLabel || '直接传递' }}</div>
          </div>
          <div class="flow-card-side target">
            <div class="flow-card-label">目标端</div>
            <div class="flow-card-ds">{{ currentLineage.targetDsName }}</div>
            <div class="flow-card-table">
              <DatabaseOutlined /> {{ currentLineage.targetTable }}
            </div>
            <div v-if="currentLineage.targetColumn" class="flow-card-col">
              {{ currentLineage.targetColumn }}
              <span v-if="currentLineage.targetColumnAlias" class="col-alias">({{ currentLineage.targetColumnAlias }})</span>
            </div>
          </div>
        </div>

        <div v-if="currentLineage.transformExpr" style="margin: 12px 0">
          <div class="detail-label">转换表达式</div>
          <code class="transform-expr">{{ currentLineage.transformExpr }}</code>
        </div>

        <a-descriptions :column="2" size="small" style="margin-top: 16px">
          <a-descriptions-item label="创建时间">{{ currentLineage.createTime }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ currentLineage.updateTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="来源作业" :span="2">{{ currentLineage.jobName || '-' }}</a-descriptions-item>
        </a-descriptions>
      </div>
      <div class="drawer-footer">
        <a-space>
          <a-button @click="openEditDrawer(currentLineage)">
            <template #icon><EditOutlined /></template>
            编辑
          </a-button>
          <a-popconfirm
            title="确定删除该血缘记录？"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleDelete(currentLineage?.id)"
          >
            <a-button danger>
              <template #icon><DeleteOutlined /></template>
              删除
            </a-button>
          </a-popconfirm>
        </a-space>
      </div>
    </a-drawer>

    <!-- ========== 批量导入血缘弹窗 ========== -->
    <a-modal
      v-model:open="importModalVisible"
      :title="`批量导入血缘 (${importStep === 0 ? '上传文件' : importStep === 1 ? '配置映射' : '导入结果'})`"
      :width="700"
      :footer="importStep === 2 ? null : undefined"
      @cancel="importModalVisible = false"
    >
      <!-- 步骤 0: 上传文件 -->
      <div v-if="importStep === 0" class="import-step">
        <a-alert type="info" show-icon style="margin-bottom: 16px">
          <template #message>
            支持 CSV/TXT 格式，每行一条血缘记录，字段用逗号分隔。
            <a-button type="link" size="small" @click="lineageManualApi.getImportTemplate().then(r => r.data?.data && message.info(r.data.data))">
              查看模板格式说明
            </a-button>
          </template>
        </a-alert>
        <a-space direction="vertical" style="width: 100%">
          <div>
            <div class="form-label">血缘类型</div>
            <a-radio-group v-model:value="importConfigForm.lineageType" button-style="solid">
              <a-radio-button value="TABLE">表级血缘</a-radio-button>
              <a-radio-button value="COLUMN">字段级血缘</a-radio-button>
            </a-radio-group>
          </div>
          <a-upload
            :before-upload="(file: any) => { handleImportFileChange({ target: { files: [file] } }); return false }"
            :max-count="1"
            accept=".csv,.txt"
          >
            <a-button><UploadOutlined /> 选择 CSV/TXT 文件</a-button>
          </a-upload>
          <div v-if="importRecords.length" class="import-records-hint">
            已解析 <strong>{{ importRecords.length }}</strong> 条血缘记录，请点击下一步配置
          </div>
        </a-space>
      </div>

      <!-- 步骤 1: 配置映射 -->
      <div v-if="importStep === 1" class="import-step">
        <a-alert type="info" show-icon style="margin-bottom: 16px">
          已解析 {{ importRecords.length }} 条血缘记录，请配置默认数据源
        </a-alert>
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="血缘来源">
            <a-select v-model:value="importConfigForm.lineageSource" placeholder="选择血缘来源">
              <a-select-option value="MANUAL">手动录入</a-select-option>
              <a-select-option value="AUTO_PARSER">自动解析</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="默认源数据源" required>
            <a-select
              v-model:value="importConfigForm.defaultSourceDsId"
              placeholder="请选择默认源数据源"
              :loading="dsLoading"
            >
              <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                {{ ds.dsName }} ({{ ds.dsType }})
              </a-select-option>
            </a-select>
            <div class="form-tip">如果记录中已指定数据源ID，以记录中为准</div>
          </a-form-item>
          <a-form-item label="默认目标数据源" required>
            <a-select
              v-model:value="importConfigForm.defaultTargetDsId"
              placeholder="请选择默认目标数据源"
              :loading="dsLoading"
            >
              <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                {{ ds.dsName }} ({{ ds.dsType }})
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
        <a-divider>前 5 条预览</a-divider>
        <a-table
          :data-source="importRecords.slice(0, 5)"
          :pagination="false"
          size="small"
          :scroll="{ y: 200 }"
        >
          <a-table-column title="源表" dataIndex="sourceTable" />
          <a-table-column title="源字段" dataIndex="sourceColumn" />
          <a-table-column title="目标表" dataIndex="targetTable" />
          <a-table-column title="目标字段" dataIndex="targetColumn" />
        </a-table>
      </div>

      <!-- 步骤 2: 导入结果 -->
      <div v-if="importStep === 2 && importResult" class="import-step">
        <a-result
          :status="importResult.successCount > 0 ? 'success' : 'error'"
          :title="`导入完成：`"
          :sub-title="`成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条，跳过 ${importResult.skippedCount} 条`"
        >
          <template #extra>
            <a-space direction="vertical" style="width: 100%; max-height: 200px; overflow-y: auto">
              <div v-if="importResult.errors?.length" class="import-errors">
                <div class="import-errors-title">失败记录：</div>
                <div v-for="err in importResult.errors" :key="err.rowNum" class="import-error-item">
                  <span class="import-error-row">第 {{ err.rowNum }} 行:</span>
                  <span class="import-error-msg">{{ err.message }}</span>
                </div>
              </div>
            </a-space>
          </template>
        </a-result>
      </div>

      <template #footer>
        <a-space>
          <a-button v-if="importStep > 0 && importStep < 2" @click="importStep--">上一步</a-button>
          <a-button v-if="importStep === 0" type="primary" @click="importStep++" :disabled="!importRecords.length">
            下一步
          </a-button>
          <a-button v-if="importStep === 1" type="primary" :loading="importLoading" @click="handleImportConfirm">
            确认导入
          </a-button>
          <a-button v-if="importStep === 2" type="primary" @click="handleImportFinish">
            完成
          </a-button>
        </a-space>
        <a-button @click="importModalVisible = false">取消</a-button>
      </template>
    </a-modal>

    <!-- ========== 影响分析结果模态框 ========== -->
    <a-modal
      v-model:open="impactModalVisible"
      :title="`影响分析结果 — ${impactDirection === 'DOWNSTREAM' ? '下游影响' : '上游回溯'}`"
      :width="960"
      :footer="null"
      @cancel="impactModalVisible = false"
    >
      <div v-if="impactLoading" class="impact-loading">
        <a-spin size="large" tip="正在分析血缘关系..." />
      </div>
      <div v-else-if="impactData" class="impact-content">
        <!-- 统计概览 -->
        <a-row :gutter="12" style="margin-bottom: 16px">
          <a-col :span="3">
            <div class="impact-stat-card">
              <div class="impact-stat-value">{{ impactData.scope?.totalNodeCount }}</div>
              <div class="impact-stat-label">影响节点</div>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="impact-stat-card">
              <div class="impact-stat-value">{{ impactData.scope?.affectedTableCount }}</div>
              <div class="impact-stat-label">影响表数</div>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="impact-stat-card">
              <div class="impact-stat-value">{{ impactData.scope?.affectedColumnCount }}</div>
              <div class="impact-stat-label">影响字段</div>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="impact-stat-card">
              <div class="impact-stat-value">{{ impactData.scope?.depthLevel }}</div>
              <div class="impact-stat-label">影响层级</div>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="impact-stat-card">
              <div class="impact-stat-value">{{ impactData.scope?.totalEdgeCount }}</div>
              <div class="impact-stat-label">血缘关系</div>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="impact-stat-card">
              <div class="impact-stat-value" style="color: #FAAD14">{{ impactData.scope?.affectedPlanCount }}</div>
              <div class="impact-stat-label">影响方案</div>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="impact-stat-card" style="background: linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%)">
              <div class="impact-stat-value">{{ impactData.scope?.l4SensitiveCount || 0 }}</div>
              <div class="impact-stat-label">L4 机密</div>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="impact-stat-card" style="background: linear-gradient(135deg, #FA8C16 0%, #FFC53D 100%)">
              <div class="impact-stat-value">{{ impactData.scope?.l3SensitiveCount || 0 }}</div>
              <div class="impact-stat-label">L3 敏感</div>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="impact-stat-card" style="background: linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)">
              <div class="impact-stat-value">{{ getImpactLevelCount('L2') }}</div>
              <div class="impact-stat-label">L2 内部</div>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="impact-stat-card" style="background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%)">
              <div class="impact-stat-value">{{ getImpactLevelCount('L1') }}</div>
              <div class="impact-stat-label">L1 公开</div>
            </div>
          </a-col>
        </a-row>

        <!-- 敏感等级分布统计 -->
        <div v-if="impactData && getImpactLevelCount('total') > 0" class="impact-sensitivity-dist">
          <div class="dist-title">
            <SafetyCertificateOutlined /> 敏感等级分布
          </div>
          <div class="dist-bars">
            <div
              v-for="level in ['L4', 'L3', 'L2', 'L1']"
              :key="level"
              class="dist-bar-item"
            >
              <div class="dist-bar-label">
                <span class="dist-level-dot" :style="{ background: getSensitivityBorderColor(level) }"></span>
                <span class="dist-level-name">{{ getSensitivityLevelLabel(level) }}</span>
                <span class="dist-level-count">{{ getImpactLevelCount(level) }}</span>
              </div>
              <div class="dist-bar-track">
                <div
                  class="dist-bar-fill"
                  :style="{
                    width: getImpactLevelPct(level) + '%',
                    background: getSensitivityBorderColor(level)
                  }"
                ></div>
              </div>
              <span class="dist-bar-pct">{{ getImpactLevelPct(level) }}%</span>
            </div>
          </div>
        </div>

        <!-- DAG 子图 -->
        <div class="impact-graph-wrapper">
          <svg ref="impactSvgRef" class="impact-svg">
            <defs>
              <filter id="nodeShadow2" x="-10%" y="-10%" width="130%" height="140%">
                <feDropShadow dx="0" dy="2" stdDeviation="4" flood-color="rgba(0,0,0,0.15)"/>
              </filter>
            </defs>
          </svg>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, watch, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined, SearchOutlined, CloseOutlined,
  DatabaseOutlined, ArrowRightOutlined, ArrowLeftOutlined,
  EyeOutlined, EditOutlined, DeleteOutlined,
  CompressOutlined, ZoomInOutlined, ZoomOutOutlined,
  NodeExpandOutlined,
  AppstoreOutlined, ColumnWidthOutlined, UploadOutlined, AimOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons-vue'
import * as d3 from 'd3'
import { lineageApi, lineageManualApi, type LineageGraphVO, type LineageNodeVO, type LineageEdgeVO, type LineageVO, type LineageStatsVO, type LineageSaveDTO, type ImpactAnalysisResultVO, type BatchImportResultVO, type LineageBatchImportDTO, type TableSuggest } from '@/api/lineage'
import { dataSourceApi } from '@/api/dqc'

// ============================================================
// 常量
// ============================================================
const LAYER_COLORS: Record<string, string> = {
  ODS: '#1677FF',
  DWD: '#52C41A',
  DWS: '#FAAD14',
  ADS: '#722ED1',
}

const SENSITIVITY_LEVEL_COLORS: Record<string, { border: string; bg: string; text: string; label: string }> = {
  L4: { border: '#FF4D4F', bg: '#FFF1F0', text: '#FF4D4F', label: 'L4 机密' },
  L3: { border: '#FAAD14', bg: '#FFFBE6', text: '#FAAD14', label: 'L3 敏感' },
  L2: { border: '#1677FF', bg: '#E6F4FF', text: '#1677FF', label: 'L2 内部' },
  L1: { border: '#52C41A', bg: '#F6FFED', text: '#52C41A', label: 'L1 公开' },
}

const DS_TYPE_COLORS: Record<string, string> = {
  MYSQL: '#00759A',
  SQLSERVER: '#CC2927',
  ORACLE: '#F80000',
  POSTGRESQL: '#336791',
  TIDB: '#2DDAAF',
}

const TRANSFORM_COLORS: Record<string, string> = {
  DIRECT: '#8C8C8C',
  SUM: '#1677FF',
  AVG: '#52C41A',
  COUNT: '#FAAD14',
  MAX: '#FF4D4F',
  MIN: '#722ED1',
  CONCAT: '#00B4DB',
  CASE_WHEN: '#FA8C16',
  CUSTOM_EXPR: '#EB2F96',
}

const TABLE_NODE_WIDTH = 160
const TABLE_NODE_HEIGHT = 48
const COL_NODE_WIDTH = 120
const COL_NODE_HEIGHT = 36
const LAYER_HEIGHT_OFFSET = 80

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const dsLoading = ref(false)
const saving = ref(false)
const dataSourceList = ref<any[]>([])

// 图谱数据
const graphData = ref<LineageGraphVO>({ nodes: [], edges: [] })
const selectedNode = ref<LineageNodeVO | null>(null)
const hoveredLineageId = ref<number | null>(null)
const searchKeyword = ref('')
const highlightedNodeIds = ref<Set<string>>(new Set())

// 筛选
const filterDsId = ref<number | undefined>()
const filterTransformType = ref<string | undefined>()
const filterMaxDepth = ref<number>(10)
const filterSensitivityLevel = ref<string | undefined>()
const showSensitiveOnly = ref(false)
const viewMode = ref<'ALL' | 'TABLE' | 'COLUMN'>('ALL')

// 血缘列表
const lineageList = ref<LineageVO[]>([])
const lineageListLoading = ref(false)
const totalLineages = ref(0)
const listPage = ref(1)
const listPageSize = ref(20)
const listSearchKeyword = ref('')

// 统计
const statCards = ref([
  { label: '总记录', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)' },
  { label: '表级血缘', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '字段级血缘', value: 0, bg: 'linear-gradient(135deg, #722ED1 0%, #9254DE 100%)' },
  { label: 'L4 机密字段', value: 0, bg: 'linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%)' },
  { label: 'L3 敏感字段', value: 0, bg: 'linear-gradient(135deg, #FA8C16 0%, #FFC53D 100%)' },
  { label: 'L2/L1 字段', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #52C41A 100%)' },
])

// 新增/编辑
const addDrawerVisible = ref(false)
const editMode = ref(false)
const formRef = ref<FormInstance>()
const stepCurrent = ref(0)
const formData = reactive<LineageSaveDTO>({
  lineageType: 'TABLE',
  sourceDsId: undefined as unknown as number,
  sourceTable: '',
  sourceColumn: '',
  sourceColumnAlias: '',
  targetDsId: undefined as unknown as number,
  targetTable: '',
  targetColumn: '',
  targetColumnAlias: '',
  transformType: undefined as unknown as string,
  transformExpr: '',
  jobName: '',
  lineageSource: 'MANUAL',
})

// 详情
const detailDrawerVisible = ref(false)
const currentLineage = ref<LineageVO | null>(null)

// ============================================================
// 批量导入
// ============================================================
const importModalVisible = ref(false)
const importStep = ref(0) // 0: 上传 1: 配置 2: 结果
const importLoading = ref(false)
const importFile = ref<any>(null)
const importConfigForm = reactive({
  lineageType: 'TABLE',
  lineageSource: 'MANUAL',
  defaultSourceDsId: undefined as unknown as number,
  defaultTargetDsId: undefined as unknown as number,
})
const importRecords = ref<any[]>([])
const importResult = ref<BatchImportResultVO | null>(null)

// ============================================================
// 影响分析结果
// ============================================================
const impactModalVisible = ref(false)
const impactLoading = ref(false)
const impactData = ref<ImpactAnalysisResultVO | null>(null)
const impactDirection = ref<'DOWNSTREAM' | 'UPSTREAM'>('DOWNSTREAM')
const impactSvgRef = ref<SVGSVGElement | null>(null)
const impactSvgSel = ref<d3.Selection<SVGSVGElement, unknown, null, undefined> | null>(null)
const impactZoom = ref<d3.ZoomBehavior<SVGSVGElement, unknown> | null>(null)

// ============================================================
// 表/字段自动补全
// ============================================================
const sourceTableOptions = ref<TableSuggest[]>([])
const targetTableOptions = ref<TableSuggest[]>([])
const sourceColOptions = ref<any[]>([])
const targetColOptions = ref<any[]>([])
const sourceColLoading = ref(false)
const targetColLoading = ref(false)

// ============================================================
// 枚举选项
// ============================================================
const transformTypeOptions = [
  { value: 'DIRECT', label: '直接传递' },
  { value: 'SUM', label: '求和聚合' },
  { value: 'AVG', label: '求平均聚合' },
  { value: 'COUNT', label: '计数聚合' },
  { value: 'MAX', label: '取最大值' },
  { value: 'MIN', label: '取最小值' },
  { value: 'CONCAT', label: '字符串拼接' },
  { value: 'CASE_WHEN', label: '条件转换' },
  { value: 'CUSTOM_EXPR', label: '自定义表达式' },
]

const layerLegend = [
  { value: 'ODS', label: 'ODS', color: '#1677FF' },
  { value: 'DWD', label: 'DWD', color: '#52C41A' },
  { value: 'DWS', label: 'DWS', color: '#FAAD14' },
  { value: 'ADS', label: 'ADS', color: '#722ED1' },
]

const sensitivityLegend = [
  { value: 'L4', label: 'L4 机密', color: '#FF4D4F' },
  { value: 'L3', label: 'L3 敏感', color: '#FAAD14' },
  { value: 'L2', label: 'L2 内部', color: '#1677FF' },
  { value: 'L1', label: 'L1 公开', color: '#52C41A' },
]

// ============================================================
// DOM Refs
// ============================================================
const svgRef = ref<SVGSVGElement | null>(null)
const graphContainerRef = ref<HTMLDivElement | null>(null)
const listBodyRef = ref<HTMLDivElement | null>(null)

// ============================================================
// D3 状态
// ============================================================
let svgSelection: d3.Selection<SVGSVGElement, unknown, null, undefined> | null = null
let zoomBehavior: d3.ZoomBehavior<SVGSVGElement, unknown> | null = null
let currentTransform = d3.zoomIdentity
let currentLayout = 'layered' // 'layered' | 'force'

// ============================================================
// 生命周期
// ============================================================
onMounted(async () => {
  await loadDataSources()
  await loadStats()
  await loadGraph()
  await loadLineageList()
  await nextTick()
  initSvg()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (svgSelection) {
    svgSelection.on('.zoom', null)
    svgSelection.on('.drag', null)
  }
  svgSelection = null
  zoomBehavior = null
})

function handleResize() {
  if (svgRef.value) {
    initSvg()
  }
}

// ============================================================
// 数据加载
// ============================================================
async function loadDataSources() {
  dsLoading.value = true
  try {
    const res = await dataSourceApi.listEnabled()
    if (res.data?.success !== false) {
      dataSourceList.value = (res.data as any)?.data || []
    }
  } catch { /* ignore */ }
  finally { dsLoading.value = false }
}

async function loadStats() {
  try {
    const res = await lineageApi.getStats()
    if (res.data?.success !== false) {
    const s = res.data?.data as LineageStatsVO
    statCards.value[0].value = s?.totalCount || 0
    statCards.value[1].value = s?.tableLineageCount || 0
    statCards.value[2].value = s?.columnLineageCount || 0
    statCards.value[3].value = s?.l4SensitiveCount || 0
    statCards.value[4].value = s?.l3SensitiveCount || 0
    statCards.value[5].value = (s?.sensitivityDistribution?.['L2'] || 0) + (s?.sensitivityDistribution?.['L1'] || 0)
    }
  } catch { /* ignore */ }
}

async function loadGraph() {
  loading.value = true
  try {
    const params: any = {}
    if (filterDsId.value) params.sourceDsId = filterDsId.value
    if (filterTransformType.value) params.transformType = filterTransformType.value

    const res = await lineageApi.getGraph(params)
    if (res.data?.success !== false) {
      graphData.value = res.data?.data as LineageGraphVO
      renderGraph()
    }
  } catch (e: any) {
    message.error('加载图谱失败: ' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

async function loadLineageList() {
  lineageListLoading.value = true
  try {
    const res = await lineageApi.page({
      pageNum: listPage.value,
      pageSize: listPageSize.value,
      sourceTable: listSearchKeyword.value || undefined,
      targetTable: listSearchKeyword.value || undefined,
      lineageType: viewMode.value === 'ALL' ? undefined : viewMode.value,
    })
    if (res.data?.success !== false) {
      const page = res.data as any
      lineageList.value = page?.records || []
      totalLineages.value = page?.total || 0
    }
  } catch { /* ignore */ }
  finally { lineageListLoading.value = false }
}

// ============================================================
// D3 图渲染
// ============================================================
function initSvg() {
  if (!svgRef.value || !graphContainerRef.value) return

  const container = graphContainerRef.value
  const width = container.clientWidth - (selectedNode.value ? 340 : 0)
  const height = container.clientHeight

  svgSelection = d3.select(svgRef.value)
    .attr('width', width)
    .attr('height', height)

  zoomBehavior = d3.zoom<SVGSVGElement, unknown>()
    .scaleExtent([0.05, 4])
    .on('zoom', (event: d3.D3ZoomEvent<SVGSVGElement, unknown>) => {
      currentTransform = event.transform
      svgSelection?.select<SVGGElement>('g.graph-content').attr('transform', event.transform.toString())
    })

  svgSelection.call(zoomBehavior)

  svgSelection.select('g.graph-content').remove()
  svgSelection.append('g').attr('class', 'graph-content')

  renderGraph()
}

function renderGraph() {
  if (!svgSelection) return

  const container = graphContainerRef.value
  if (!container) return

  const width = container.clientWidth - (selectedNode.value ? 340 : 0)
  const height = container.clientHeight
  svgSelection.attr('width', width).attr('height', height)

  const g = svgSelection.select<SVGGElement>('g.graph-content')
  g.selectAll('*').remove()

  const nodes = graphData.value.nodes || []
  const edges = (graphData.value.edges || [])

  if (nodes.length === 0) return

  // --- 敏感等级过滤 ---
  const filteredNodes = filterSensitivityLevel.value
    ? nodes.filter(n => n.sensitivityLevel === filterSensitivityLevel.value)
    : nodes
  const filteredNodeIds = new Set(filteredNodes.map(n => n.nodeId))

  // 敏感节点 set（用于「仅显示敏感节点」和边框着色）
  const sensitiveNodes = new Set(nodes.filter(n => n.sensitivityLevel && n.sensitivityLevel !== 'L1').map(n => n.nodeId))

  // --- 节点分组 ---
  const nodeGroups = g.selectAll<SVGGElement, LineageNodeVO>('g.node')
    .data(filteredNodes, (d: LineageNodeVO) => d.nodeId)
    .enter()
    .append('g')
    .attr('class', 'node')
    .attr('cursor', 'pointer')
    .call(
      d3.drag<SVGGElement, LineageNodeVO>()
        .on('start', onDragStart)
        .on('drag', onDrag)
        .on('end', onDragEnd)
    )
    .on('click', onNodeClick)
    .on('mouseenter', onNodeMouseEnter)
    .on('mouseleave', onNodeMouseLeave)

  // 节点背景
  nodeGroups.each(function(this: SVGGElement, d: LineageNodeVO) {
    const el = d3.select<SVGGElement, LineageNodeVO>(this)
    const isTable = d.nodeType === 'TABLE'
    const layerColor = getLayerColor(d.dataLayer)
    const isHighlighted = highlightedNodeIds.value.size > 0 && highlightedNodeIds.value.has(d.nodeId)
    const sensLevel = d.sensitivityLevel
    const sensConfig = sensLevel ? SENSITIVITY_LEVEL_COLORS[sensLevel] : null
    const hasSensitivity = !!sensConfig
    const sensBorderColor = hasSensitivity ? sensConfig!.border : layerColor
    const isSensitiveOnlyHidden = showSensitiveOnly.value && !sensitiveNodes.has(d.nodeId)

    if (isTable) {
      // 表节点：圆角矩形
      el.append('rect')
        .attr('class', 'node-rect table-rect')
        .attr('x', -TABLE_NODE_WIDTH / 2)
        .attr('y', -TABLE_NODE_HEIGHT / 2)
        .attr('width', TABLE_NODE_WIDTH)
        .attr('height', TABLE_NODE_HEIGHT)
        .attr('rx', 8)
        .attr('fill', '#ffffff')
        .attr('stroke', sensBorderColor)
        .attr('stroke-width', isHighlighted ? 3 : (hasSensitivity ? 2.5 : 2))
        .attr('filter', 'url(#nodeShadow)')
        .attr('opacity', isSensitiveOnlyHidden ? 0.15 : 1)

      // 敏感等级角标（左上角红色小三角或边框加粗表示敏感）
      if (hasSensitivity) {
        // 左上角小三角
        el.append('polygon')
          .attr('class', 'sens-indicator')
          .attr('points', `${-TABLE_NODE_WIDTH / 2},${-TABLE_NODE_HEIGHT / 2} ${-TABLE_NODE_WIDTH / 2 + 16},${-TABLE_NODE_HEIGHT / 2} ${-TABLE_NODE_WIDTH / 2},${-TABLE_NODE_HEIGHT / 2 + 16}`)
          .attr('fill', sensBorderColor)
          .attr('opacity', isSensitiveOnlyHidden ? 0.15 : 0.85)

        // 敏感图标
        el.append('text')
          .attr('class', 'node-icon sens-icon')
          .attr('x', -TABLE_NODE_WIDTH / 2 + 4)
          .attr('y', -TABLE_NODE_HEIGHT / 2 + 14)
          .attr('text-anchor', 'middle')
          .attr('font-size', 9)
          .attr('fill', 'white')
          .attr('pointer-events', 'none')
          .text('!')
          .attr('opacity', isSensitiveOnlyHidden ? 0.15 : 0.9)
      }

      // 顶部色条（敏感时使用敏感色）
      el.append('rect')
        .attr('class', 'node-topbar')
        .attr('x', -TABLE_NODE_WIDTH / 2)
        .attr('y', -TABLE_NODE_HEIGHT / 2)
        .attr('width', TABLE_NODE_WIDTH)
        .attr('height', 4)
        .attr('rx', [8, 8, 0, 0])
        .attr('fill', sensBorderColor)

      // 数据层标签
      if (d.dataLayer) {
        el.append('rect')
          .attr('x', TABLE_NODE_WIDTH / 2 - 36)
          .attr('y', -TABLE_NODE_HEIGHT / 2 + 6)
          .attr('width', 30)
          .attr('height', 16)
          .attr('rx', 4)
          .attr('fill', layerColor)
          .attr('opacity', hasSensitivity ? 0.7 : 0.9)
        el.append('text')
          .attr('class', 'node-layer-label')
          .attr('x', TABLE_NODE_WIDTH / 2 - 21)
          .attr('y', -TABLE_NODE_HEIGHT / 2 + 17)
          .attr('text-anchor', 'middle')
          .attr('fill', 'white')
          .attr('font-size', 9)
          .attr('font-weight', 600)
          .text(d.dataLayer)
      }

      // 数据库图标
      el.append('text')
        .attr('class', 'node-icon')
        .attr('x', -TABLE_NODE_WIDTH / 2 + 16)
        .attr('y', 5)
        .attr('text-anchor', 'middle')
        .attr('font-size', 14)
        .attr('fill', sensBorderColor)
        .text('◆')

      // 表名
      el.append('text')
        .attr('class', 'node-label')
        .attr('x', hasSensitivity ? 20 : 6)
        .attr('y', 2)
        .attr('dominant-baseline', 'middle')
        .attr('fill', '#1F1F1F')
        .attr('font-size', 12)
        .attr('font-weight', 600)
        .text(truncate(d.tableName, 14))

      // 敏感等级标签（表名下）
      if (hasSensitivity) {
        el.append('text')
          .attr('class', 'sens-level-label')
          .attr('x', 20)
          .attr('y', 16)
          .attr('dominant-baseline', 'middle')
          .attr('fill', sensConfig!.text)
          .attr('font-size', 9)
          .attr('font-weight', 700)
          .text(sensLevel || '')
      }

      // 别名
      if (d.tableAlias) {
        el.append('text')
          .attr('class', 'node-alias')
          .attr('x', 6)
          .attr('y', 16)
          .attr('dominant-baseline', 'middle')
          .attr('fill', '#8C8C8C')
          .attr('font-size', 9)
          .text(truncate(d.tableAlias, 16))
      }

      // 搜索高亮
      if (isHighlighted) {
        el.select('.node-rect')
          .attr('filter', 'url(#glow)')
      }
    } else {
      // 字段节点：更小的圆角矩形
      el.append('rect')
        .attr('class', 'node-rect col-rect')
        .attr('x', -COL_NODE_WIDTH / 2)
        .attr('y', -COL_NODE_HEIGHT / 2)
        .attr('width', COL_NODE_WIDTH)
        .attr('height', COL_NODE_HEIGHT)
        .attr('rx', 6)
        .attr('fill', sensConfig ? sensConfig.bg : '#F5F7FA')
        .attr('stroke', sensBorderColor)
        .attr('stroke-width', isHighlighted ? 2.5 : (hasSensitivity ? 2 : 1.5))
        .attr('stroke-dasharray', hasSensitivity ? 'none' : '3,2')
        .attr('opacity', isSensitiveOnlyHidden ? 0.15 : 1)

      // 敏感三角标记（字段节点右上角）
      if (hasSensitivity) {
        el.append('rect')
          .attr('x', COL_NODE_WIDTH / 2 - 12)
          .attr('y', -COL_NODE_HEIGHT / 2)
          .attr('width', 12)
          .attr('height', 4)
          .attr('rx', [0, 6, 0, 0])
          .attr('fill', sensBorderColor)
          .attr('opacity', isSensitiveOnlyHidden ? 0.15 : 0.85)
      }

      // 字段图标
      el.append('text')
        .attr('class', 'node-icon')
        .attr('x', -COL_NODE_WIDTH / 2 + 14)
        .attr('y', 0)
        .attr('text-anchor', 'middle')
        .attr('dominant-baseline', 'middle')
        .attr('font-size', 10)
        .attr('fill', sensBorderColor)
        .text('⫶')

      // 字段名
      el.append('text')
        .attr('class', 'node-label')
        .attr('x', 2)
        .attr('y', 0)
        .attr('dominant-baseline', 'middle')
        .attr('fill', '#1F1F1F')
        .attr('font-size', 11)
        .attr('font-family', 'JetBrains Mono, monospace')
        .text(truncate(d.columnName || d.nodeId, 12))
    }
  })

  // --- 连线 ---
  // 先渲染连线（在节点之下），只渲染两端都在 filteredNodes 中的边
  edges.forEach(edge => {
    if (!filteredNodeIds.has(edge.sourceNodeId) || !filteredNodeIds.has(edge.targetNodeId)) return
    const sourceNode = filteredNodes.find(n => n.nodeId === edge.sourceNodeId)
    const targetNode = filteredNodes.find(n => n.nodeId === edge.targetNodeId)
    if (!sourceNode || !targetNode) return

    // 动态计算节点尺寸
    const sIsTable = sourceNode.nodeType === 'TABLE'
    const tIsTable = targetNode.nodeType === 'TABLE'
    const sW = sIsTable ? TABLE_NODE_WIDTH : COL_NODE_WIDTH
    const sH = sIsTable ? TABLE_NODE_HEIGHT : COL_NODE_HEIGHT
    const tW = tIsTable ? TABLE_NODE_WIDTH : COL_NODE_WIDTH
    const tH = tIsTable ? TABLE_NODE_HEIGHT : COL_NODE_HEIGHT

    const sx = sourceNode.x || 0
    const sy = sourceNode.y || 0
    const tx = targetNode.x || 0
    const ty = targetNode.y || 0

    // 计算连线端点（从节点边缘出发）
    const dx = tx - sx
    const dy = ty - sy
    const dist = Math.sqrt(dx * dx + dy * dy)
    if (dist === 0) return

    const nx = dx / dist
    const ny = dy / dist
    const ex = tx - nx * (tW / 2 + 2)
    const ey = ty - ny * (tH / 2 + 2)
    const sx2 = sx + nx * (sW / 2 + 2)
    const sy2 = sy + ny * (sH / 2 + 2)

    const edgeColor = TRANSFORM_COLORS[edge.transformType] || '#8C8C8C'
    const layerColor = getLayerColor(sourceNode.dataLayer)
    const isHovered = hoveredLineageId.value === edge.lineageId

    // 曲线路径
    const midX = (sx2 + ex) / 2
    const midY = (sy2 + ey) / 2
    const perpX = -ny * 15
    const perpY = nx * 15

    const path = g.append('path')
      .attr('class', 'edge')
      .attr('data-edge-id', String(edge.lineageId))
      .attr('d', `M${sx2},${sy2} Q${midX + perpX},${midY + perpY} ${ex},${ey}`)
      .attr('fill', 'none')
      .attr('stroke', isHovered ? layerColor : edgeColor)
      .attr('stroke-width', isHovered ? 2.5 : 1.5)
      .attr('stroke-opacity', isHovered ? 0.9 : 0.6)
      .attr('marker-end', `url(#arrow-${sourceNode.dataLayer || 'default'})`)
      .attr('cursor', 'pointer')
      .style('transition', 'stroke-width 0.15s, stroke-opacity 0.15s')

    // 转换类型标签
    if (edge.transformType && edge.transformType !== 'DIRECT') {
      const labelX = midX
      const labelY = midY
      const label = g.append('g')
        .attr('class', 'edge-label')
        .attr('cursor', 'pointer')
        .on('mouseenter', () => { hoveredLineageId.value = edge.lineageId })
        .on('mouseleave', () => { hoveredLineageId.value = null })

      label.append('rect')
        .attr('x', labelX - 22)
        .attr('y', labelY - 10)
        .attr('width', 44)
        .attr('height', 20)
        .attr('rx', 4)
        .attr('fill', 'white')
        .attr('stroke', edgeColor)
        .attr('stroke-width', 1)
        .attr('opacity', 0.9)

      label.append('text')
        .attr('x', labelX)
        .attr('y', labelY + 1)
        .attr('text-anchor', 'middle')
        .attr('dominant-baseline', 'middle')
        .attr('font-size', 9)
        .attr('fill', edgeColor)
        .attr('font-weight', 600)
        .text(edge.transformType.substring(0, 6))
    }
  })

  // --- 应用布局 ---
  if (currentLayout === 'layered') {
    applyDagreLayout(filteredNodes, edges)
  } else {
    applyForceLayout(filteredNodes, edges)
  }

  // 居中到视口
  centerGraph()
}

// ============================================================
// D3 布局算法
// ============================================================

/**
 * 分层布局（Dagre 风格）：按 level 分层，同层节点水平排列
 */
function applyDagreLayout(nodes: LineageNodeVO[], edges: LineageEdgeVO[]) {
  if (!svgSelection || nodes.length === 0) return

  const container = graphContainerRef.value
  if (!container) return
  const width = container.clientWidth - (selectedNode.value ? 340 : 0)
  const height = container.clientHeight

  // 按 level 分组
  const levelMap = new Map<number, LineageNodeVO[]>()
  nodes.forEach(n => {
    const lvl = n.level ?? 0
    if (!levelMap.has(lvl)) levelMap.set(lvl, [])
    levelMap.get(lvl)!.push(n)
  })

  const levels = Array.from(levelMap.keys()).sort((a, b) => a - b)
  const nodeSpacingX = Math.max(220, width / (levels.length + 1))
  const layerSpacingY = 90

  levels.forEach((lvl, levelIdx) => {
    const nodesAtLevel = levelMap.get(lvl) || []
    const layerWidth = nodesAtLevel.length * 180
    const startX = (width - layerWidth) / 2 + 90

    nodesAtLevel.forEach((node, nodeIdx) => {
      const x = startX + nodeIdx * 180
      const y = 80 + levelIdx * layerSpacingY
      node.x = x
      node.y = y
    })
  })

  // 定位节点
  svgSelection.selectAll<SVGGElement, LineageNodeVO>('g.node')
    .transition()
    .duration(600)
    .ease(d3.easeCubicOut)
    .attr('transform', (d: LineageNodeVO) => `translate(${d.x || 0}, ${d.y || 0})`)

  // 更新所有连线位置
  svgSelection.selectAll<SVGPathElement, LineageEdgeVO>('path.edge')
    .transition()
    .duration(600)
    .ease(d3.easeCubicOut)
    .attr('d', (edge: LineageEdgeVO) => {
      const src = nodes.find(n => n.nodeId === edge.sourceNodeId)
      const tgt = nodes.find(n => n.nodeId === edge.targetNodeId)
      if (!src || !tgt) return ''
      const sx = src.x || 0, sy = src.y || 0
      const tx = tgt.x || 0, ty = tgt.y || 0
      const sIsTable = src.nodeType === 'TABLE'
      const tIsTable = tgt.nodeType === 'TABLE'
      const sW = sIsTable ? TABLE_NODE_WIDTH : COL_NODE_WIDTH
      const sH = sIsTable ? TABLE_NODE_HEIGHT : COL_NODE_HEIGHT
      const tW = tIsTable ? TABLE_NODE_WIDTH : COL_NODE_WIDTH
      const tH = tIsTable ? TABLE_NODE_HEIGHT : COL_NODE_HEIGHT
      const dx = tx - sx, dy = ty - sy
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist === 0) return ''
      const nx = dx / dist, ny = dy / dist
      const ex = tx - nx * (tW / 2 + 2)
      const ey = ty - ny * (tH / 2 + 2)
      const sx2 = sx + nx * (sW / 2 + 2)
      const sy2 = sy + ny * (sH / 2 + 2)
      const midX = (sx2 + ex) / 2
      const midY = (sy2 + ey) / 2
      return `M${sx2},${sy2} Q${midX},${midY} ${ex},${ey}`
    })
}

/**
 * 力导向布局（d3-force）
 */
function applyForceLayout(nodes: LineageNodeVO[], edges: LineageEdgeVO[]) {
  if (!svgSelection || nodes.length === 0) return

  const container = graphContainerRef.value
  if (!container) return
  const width = container.clientWidth - (selectedNode.value ? 340 : 0)
  const height = container.clientHeight

  // 初始化位置（如果已有 level，用分层初始位置）
  nodes.forEach(n => {
    if (n.x == null || n.y == null) {
      n.x = width / 2 + (Math.random() - 0.5) * 400
      n.y = height / 2 + (Math.random() - 0.5) * 300
    }
  })

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const linkForce = (d3.forceLink as any)(edges)
  linkForce.id((d: d3.SimulationNodeDatum) => (d as LineageNodeVO).nodeId)
  linkForce.distance(220)
  linkForce.strength(0.4)
  const simulation = d3.forceSimulation<LineageNodeVO>(nodes)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    .force('link', linkForce as any)
    .force('charge', d3.forceManyBody().strength(-500))
    .force('center', d3.forceCenter(width / 2, height / 2))
    .force('collision', d3.forceCollide<LineageNodeVO>().radius((d: LineageNodeVO) => d.nodeType === 'TABLE' ? 120 : 80))
    .alphaDecay(0.025)

  simulation.on('tick', () => {
    svgSelection?.selectAll<SVGGElement, LineageNodeVO>('g.node')
      .attr('transform', (d: LineageNodeVO) => `translate(${d.x || 0}, ${d.y || 0})`)

    // 实时更新所有连线路径
    svgSelection?.selectAll<SVGPathElement, LineageEdgeVO>('path.edge')
      .attr('d', (edge: LineageEdgeVO) => {
        const src = nodes.find(n => n.nodeId === edge.sourceNodeId)
        const tgt = nodes.find(n => n.nodeId === edge.targetNodeId)
        if (!src || !tgt) return ''

        const sx = src.x || 0, sy = src.y || 0
        const tx = tgt.x || 0, ty = tgt.y || 0
        const sIsTable = src.nodeType === 'TABLE'
        const tIsTable = tgt.nodeType === 'TABLE'
        const sW = sIsTable ? TABLE_NODE_WIDTH : COL_NODE_WIDTH
        const sH = sIsTable ? TABLE_NODE_HEIGHT : COL_NODE_HEIGHT
        const tW = tIsTable ? TABLE_NODE_WIDTH : COL_NODE_WIDTH
        const tH = tIsTable ? TABLE_NODE_HEIGHT : COL_NODE_HEIGHT

        const dx = tx - sx, dy = ty - sy
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist === 0) return ''

        const nx = dx / dist, ny = dy / dist
        const ex = tx - nx * (tW / 2 + 2)
        const ey = ty - ny * (tH / 2 + 2)
        const sx2 = sx + nx * (sW / 2 + 2)
        const sy2 = sy + ny * (sH / 2 + 2)
        const midX = (sx2 + ex) / 2
        const midY = (sy2 + ey) / 2
        const perpX = -ny * 18
        const perpY = nx * 18

        return `M${sx2},${sy2} Q${midX + perpX},${midY + perpY} ${ex},${ey}`
      })

    // 更新边标签位置
    svgSelection?.selectAll<SVGGElement, LineageEdgeVO>('g.edge-label')
      .attr('transform', (edge: LineageEdgeVO) => {
        const src = nodes.find(n => n.nodeId === edge.sourceNodeId)
        const tgt = nodes.find(n => n.nodeId === edge.targetNodeId)
        if (!src || !tgt) return 'translate(0,0)'
        const midX = ((src.x || 0) + (tgt.x || 0)) / 2
        const midY = ((src.y || 0) + (tgt.y || 0)) / 2
        return `translate(${midX},${midY})`
      })
  })

  simulation.on('end', () => {
    svgSelection?.selectAll<SVGGElement, LineageNodeVO>('g.node')
      .transition().duration(200)
      .attr('transform', (d: LineageNodeVO) => `translate(${d.x || 0}, ${d.y || 0})`)
  })
}

function updateEdgePaths(nodes: LineageNodeVO[], edges: LineageEdgeVO[]) {
  // 力导向布局中由 simulation.tick 实时更新，此方法用于静态重绘
  if (!svgSelection || currentLayout === 'force') return
  svgSelection.selectAll<SVGPathElement, LineageEdgeVO>('path.edge')
    .attr('d', (edge: LineageEdgeVO) => {
      const src = nodes.find(n => n.nodeId === edge.sourceNodeId)
      const tgt = nodes.find(n => n.nodeId === edge.targetNodeId)
      if (!src || !tgt) return ''

      const sx = src.x || 0, sy = src.y || 0
      const tx = tgt.x || 0, ty = tgt.y || 0
      const sIsTable = src.nodeType === 'TABLE'
      const tIsTable = tgt.nodeType === 'TABLE'
      const sW = sIsTable ? TABLE_NODE_WIDTH : COL_NODE_WIDTH
      const sH = sIsTable ? TABLE_NODE_HEIGHT : COL_NODE_HEIGHT
      const tW = tIsTable ? TABLE_NODE_WIDTH : COL_NODE_WIDTH
      const tH = tIsTable ? TABLE_NODE_HEIGHT : COL_NODE_HEIGHT

      const dx = tx - sx, dy = ty - sy
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist === 0) return ''

      const nx = dx / dist, ny = dy / dist
      const ex = tx - nx * (tW / 2 + 2)
      const ey = ty - ny * (tH / 2 + 2)
      const sx2 = sx + nx * (sW / 2 + 2)
      const sy2 = sy + ny * (sH / 2 + 2)
      const midX = (sx2 + ex) / 2
      const midY = (sy2 + ey) / 2
      const perpX = -ny * 18
      const perpY = nx * 18

      return `M${sx2},${sy2} Q${midX + perpX},${midY + perpY} ${ex},${ey}`
    })
}

// ============================================================
// D3 交互事件
// ============================================================
function onNodeClick(_event: MouseEvent, d: LineageNodeVO) {
  selectedNode.value = selectedNode.value?.nodeId === d.nodeId ? null : d
  renderGraph()
}

function onNodeMouseEnter(_event: MouseEvent, d: LineageNodeVO) {
  // 高亮当前节点的上下游
  const connectedEdgeIds = new Set<string>()
  graphData.value.edges.forEach(e => {
    if (e.sourceNodeId === d.nodeId || e.targetNodeId === d.nodeId) {
      connectedEdgeIds.add(String(e.lineageId))
    }
  })

  svgSelection?.selectAll<SVGGElement, LineageNodeVO>('g.node')
    .each(function(this: SVGGElement, n: LineageNodeVO) {
      const isConnected = connectedEdgeIds.size > 0 && (
        graphData.value.edges.some(e =>
          (e.sourceNodeId === d.nodeId && e.targetNodeId === n.nodeId) ||
          (e.targetNodeId === d.nodeId && e.sourceNodeId === n.nodeId) ||
          n.nodeId === d.nodeId
        )
      )
      if (!isConnected && n.nodeId !== d.nodeId) {
        d3.select<SVGGElement, LineageNodeVO>(this).select('.node-rect').attr('opacity', 0.25)
      } else {
        d3.select<SVGGElement, LineageNodeVO>(this).select('.node-rect')
          .attr('stroke-width', n.nodeId === d.nodeId ? 3 : 2)
      }
    })

  // 只暗化不相关的边
  svgSelection?.selectAll<SVGPathElement, unknown>('path.edge')
    .each(function(this: SVGPathElement) {
      const edgeId = d3.select<SVGPathElement, unknown>(this).attr('data-edge-id')
      if (!connectedEdgeIds.has(edgeId)) {
        d3.select<SVGPathElement, unknown>(this).attr('stroke-opacity', 0.2).attr('stroke-width', 1)
      } else {
        d3.select<SVGPathElement, unknown>(this).attr('stroke-opacity', 0.9).attr('stroke-width', 2)
      }
    })
}

function onNodeMouseLeave() {
  svgSelection?.selectAll<SVGGElement, LineageNodeVO>('g.node .node-rect')
    .attr('opacity', 1)
    .attr('stroke-width', 2)
  svgSelection?.selectAll<SVGPathElement, unknown>('path.edge')
    .attr('stroke-opacity', 0.6)
    .attr('stroke-width', 1.5)
  hoveredLineageId.value = null
}

function onDragStart(event: d3.D3DragEvent<SVGGElement, LineageNodeVO, unknown>, d: LineageNodeVO) {
  d3.select(event.sourceEvent.target.closest('g') as SVGGElement).raise()
}

function onDrag(event: d3.D3DragEvent<SVGGElement, LineageNodeVO, unknown>, d: LineageNodeVO) {
  d.x = event.x
  d.y = event.y
  d3.select(event.sourceEvent.target.closest('g') as SVGGElement)
    .attr('transform', `translate(${event.x},${event.y})`)
}

function onDragEnd(event: d3.D3DragEvent<SVGGElement, LineageNodeVO, unknown>, d: LineageNodeVO) {
  // 拖拽结束后，固定位置
}

function onSvgClick(event: MouseEvent) {
  const target = event.target as SVGElement
  if (target.tagName === 'svg' || target.tagName === 'rect') {
    selectedNode.value = null
    renderGraph()
  }
}

// ============================================================
// 缩放/平移
// ============================================================
function fitToScreen() {
  if (!svgSelection || !zoomBehavior) return
  svgSelection.transition().duration(500).call(
    zoomBehavior.transform,
    d3.zoomIdentity
  )
}

function zoomIn() {
  if (!svgSelection || !zoomBehavior) return
  svgSelection.transition().duration(300).call(
    zoomBehavior.scaleBy, 1.3
  )
}

function zoomOut() {
  if (!svgSelection || !zoomBehavior) return
  svgSelection.transition().duration(300).call(
    zoomBehavior.scaleBy, 0.7
  )
}

function centerGraph() {
  if (!svgSelection || !zoomBehavior || !graphContainerRef.value) return
  const width = graphContainerRef.value.clientWidth
  const height = graphContainerRef.value.clientHeight

  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity
  graphData.value.nodes.forEach(n => {
    const x = n.x || 0
    const y = n.y || 0
    minX = Math.min(minX, x - 100)
    maxX = Math.max(maxX, x + 100)
    minY = Math.min(minY, y - 60)
    maxY = Math.max(maxY, y + 60)
  })

  if (graphData.value.nodes.length === 0) {
    minX = 0; maxX = width; minY = 0; maxY = height
  }

  const graphW = maxX - minX
  const graphH = maxY - minY
  const scale = Math.min(0.9, width / graphW, height / graphH) * 0.9
  const tx = (width / 2) - (minX + graphW / 2) * scale
  const ty = (height / 2) - (minY + graphH / 2) * scale

  svgSelection.transition().duration(600).call(
    zoomBehavior.transform,
    d3.zoomIdentity.translate(tx, ty).scale(scale)
  )
}

// ============================================================
// 布局切换
// ============================================================
function toggleLayout() {
  currentLayout = currentLayout === 'layered' ? 'force' : 'layered'
  renderGraph()
  message.info(currentLayout === 'layered' ? '切换到分层布局' : '切换到力导向布局')
}

// ============================================================
// 搜索 & 筛选
// ============================================================
function handleSearch() {
  if (!searchKeyword.value.trim()) {
    highlightedNodeIds.value = new Set()
    renderGraph()
    return
  }
  const kw = searchKeyword.value.toLowerCase()
  const matched = new Set<string>()
  graphData.value.nodes.forEach(n => {
    if (
      n.tableName?.toLowerCase().includes(kw) ||
      n.columnName?.toLowerCase().includes(kw) ||
      n.tableAlias?.toLowerCase().includes(kw) ||
      n.columnAlias?.toLowerCase().includes(kw)
    ) {
      matched.add(n.nodeId)
    }
  })
  highlightedNodeIds.value = matched

  // 缩放/平移到第一个匹配节点
  if (matched.size > 0) {
    const firstMatched = graphData.value.nodes.find(n => matched.has(n.nodeId))
    if (firstMatched && svgSelection && zoomBehavior && graphContainerRef.value) {
      const width = graphContainerRef.value.clientWidth
      const height = graphContainerRef.value.clientHeight
      const scale = 1.2
      const tx = width / 2 - (firstMatched.x || 0) * scale
      const ty = height / 2 - (firstMatched.y || 0) * scale
      svgSelection.transition().duration(600).call(
        zoomBehavior.transform,
        d3.zoomIdentity.translate(tx, ty).scale(scale)
      )
    }
  }

  renderGraph()
}

function handleSearchChange() {
  if (!searchKeyword.value) {
    highlightedNodeIds.value = new Set()
    renderGraph()
  }
}

// ============================================================
// 批量导入
// ============================================================
function openImportModal() {
  importStep.value = 0
  importFile.value = null
  importRecords.value = []
  importResult.value = null
  Object.assign(importConfigForm, {
    lineageType: 'TABLE',
    lineageSource: 'MANUAL',
    defaultSourceDsId: undefined as unknown as number,
    defaultTargetDsId: undefined as unknown as number,
  })
  importModalVisible.value = true
}

async function handleImportFileChange(e: any) {
  const file = e.target.files?.[0] || e.file?.originFileObj
  if (!file) return
  importFile.value = file
  importLoading.value = true
  try {
    const text = await file.text()
    const lines = text.trim().split('\n')
    if (lines.length < 2) {
      message.warning('文件格式不正确，请包含表头行和数据行')
      return
    }
    const records: any[] = []
    for (let i = 1; i < lines.length; i++) {
      const values = lines[i].split(',').map((v: string) => v.trim())
      if (values.length < 2 || !values[0] || !values[1]) continue
      const record: any = {
        sourceTable: values[0] || '',
        targetTable: values[1] || '',
        sourceColumn: values[2] || undefined,
        targetColumn: values[4] || undefined,
        transformType: values[8] || undefined,
        transformExpr: values[9] || undefined,
        jobName: values[10] || undefined,
      }
      if (values.length > 3 && values[3]) record.sourceDsId = Number(values[3])
      if (values.length > 5 && values[5]) record.targetDsId = Number(values[5])
      if (values.length > 6) record.sourceColumnAlias = values[6]
      if (values.length > 7) record.targetColumnAlias = values[7]
      records.push(record)
    }
    importRecords.value = records
    importStep.value = 1
    message.success(`成功解析 ${records.length} 条血缘记录`)
  } catch (e: any) {
    message.error('文件解析失败: ' + (e.message || ''))
  } finally {
    importLoading.value = false
  }
}

async function handleImportConfirm() {
  if (!importRecords.value.length) {
    message.warning('请先上传血缘记录文件')
    return
  }
  if (!importConfigForm.defaultSourceDsId || !importConfigForm.defaultTargetDsId) {
    message.warning('请配置默认的源数据源和目标数据源')
    return
  }
  importLoading.value = true
  try {
    const data: LineageBatchImportDTO = {
      lineageType: importConfigForm.lineageType,
      lineageSource: importConfigForm.lineageSource,
      defaultSourceDsId: importConfigForm.defaultSourceDsId as number,
      defaultTargetDsId: importConfigForm.defaultTargetDsId as number,
      records: importRecords.value,
    }
    const res = await lineageManualApi.batchImport(data)
    if (res.data?.success !== false) {
      importResult.value = res.data?.data as BatchImportResultVO
      importStep.value = 2
    } else {
      message.error(res.data?.message || '导入失败')
    }
  } catch (e: any) {
    message.error('导入失败: ' + (e.message || ''))
  } finally {
    importLoading.value = false
  }
}

async function handleImportFinish() {
  importModalVisible.value = false
  await Promise.all([loadGraph(), loadLineageList(), loadStats()])
}

// ============================================================
// 影响分析结果模态框
// ============================================================
function initImpactSvg() {
  if (!impactSvgRef.value) return
  const container = impactSvgRef.value.parentElement
  if (!container) return
  const width = Math.min(900, container.clientWidth - 32)
  const height = 480

  impactSvgSel.value = d3.select(impactSvgRef.value)
    .attr('width', width)
    .attr('height', height)

  impactZoom.value = d3.zoom<SVGSVGElement, unknown>()
    .scaleExtent([0.1, 4])
    .on('zoom', (event: d3.D3ZoomEvent<SVGSVGElement, unknown>) => {
      impactSvgSel.value?.select<SVGGElement>('g.impact-content').attr('transform', event.transform.toString())
    })

  impactSvgSel.value.call(impactZoom.value)
  impactSvgSel.value.select('g.impact-content').remove()
  impactSvgSel.value.append('g').attr('class', 'impact-content')

  renderImpactGraph()
}

function renderImpactGraph() {
  if (!impactSvgSel.value || !impactData.value) return

  const g = impactSvgSel.value.select<SVGGElement>('g.impact-content')
  g.selectAll('*').remove()

  const nodes = impactData.value.nodes || []
  const edges = impactData.value.edges || []

  if (nodes.length === 0) return

  // 先计算布局坐标（不操作 SVG）
  computeImpactLayout(nodes, edges)

  // 渲染节点（初始位置在计算坐标处，过渡动画制造入场效果）
  const nodeGs = g.selectAll<SVGGElement, LineageNodeVO>('g.node-imp')
    .data(nodes, (d: any) => d.nodeId)
    .enter()
    .append('g')
    .attr('class', 'node-imp')
    .attr('cursor', 'pointer')
    .attr('transform', (d: LineageNodeVO) => `translate(${d.x || 0},${d.y || 0})`)
      .each(function(this: SVGGElement, d: LineageNodeVO) {
      const el = d3.select<SVGGElement, LineageNodeVO>(this)
      const layerColor = getLayerColor(d.dataLayer)
      const isRoot = d.nodeId === impactData.value?.targetNodeId
      const sensLevel = d.sensitivityLevel
      const sensConfig = sensLevel ? SENSITIVITY_LEVEL_COLORS[sensLevel] : null
      const sensBorderColor = sensConfig ? sensConfig.border : layerColor

      el.append('rect')
        .attr('x', -TABLE_NODE_WIDTH / 2)
        .attr('y', -TABLE_NODE_HEIGHT / 2)
        .attr('width', TABLE_NODE_WIDTH)
        .attr('height', TABLE_NODE_HEIGHT)
        .attr('rx', 8)
        .attr('fill', isRoot ? '#EFF6FF' : (sensConfig ? sensConfig.bg : '#ffffff'))
        .attr('stroke', isRoot ? '#1677FF' : sensBorderColor)
        .attr('stroke-width', isRoot ? 3 : (sensConfig ? 2.5 : 2))
        .attr('filter', 'url(#nodeShadow2)')

      el.append('rect')
        .attr('x', -TABLE_NODE_WIDTH / 2)
        .attr('y', -TABLE_NODE_HEIGHT / 2)
        .attr('width', TABLE_NODE_WIDTH)
        .attr('height', 4)
        .attr('rx', [8, 8, 0, 0])
        .attr('fill', isRoot ? '#1677FF' : sensBorderColor)

      // 敏感三角
      if (sensConfig) {
        el.append('polygon')
          .attr('points', `${-TABLE_NODE_WIDTH / 2},${-TABLE_NODE_HEIGHT / 2} ${-TABLE_NODE_WIDTH / 2 + 16},${-TABLE_NODE_HEIGHT / 2} ${-TABLE_NODE_WIDTH / 2},${-TABLE_NODE_HEIGHT / 2 + 16}`)
          .attr('fill', sensBorderColor)
          .attr('opacity', 0.85)
        el.append('text')
          .attr('x', -TABLE_NODE_WIDTH / 2 + 4)
          .attr('y', -TABLE_NODE_HEIGHT / 2 + 14)
          .attr('text-anchor', 'middle')
          .attr('font-size', 9)
          .attr('fill', 'white')
          .text('!')
          .attr('opacity', 0.9)
      }

      if (d.dataLayer) {
        el.append('rect')
          .attr('x', TABLE_NODE_WIDTH / 2 - 36)
          .attr('y', -TABLE_NODE_HEIGHT / 2 + 6)
          .attr('width', 30).attr('height', 16)
          .attr('rx', 4)
          .attr('fill', layerColor).attr('opacity', 0.9)
        el.append('text')
          .attr('x', TABLE_NODE_WIDTH / 2 - 21)
          .attr('y', -TABLE_NODE_HEIGHT / 2 + 17)
          .attr('text-anchor', 'middle')
          .attr('fill', 'white').attr('font-size', 9).attr('font-weight', 600)
          .text(d.dataLayer)
      }

      el.append('text')
        .attr('x', sensConfig ? 20 : 6).attr('y', 2)
        .attr('dominant-baseline', 'middle')
        .attr('fill', '#1F1F1F')
        .attr('font-size', 11)
        .attr('font-weight', isRoot ? 700 : 600)
        .text(truncate(d.tableName, 14))

      // 敏感等级标签
      if (sensConfig) {
        el.append('text')
          .attr('x', 20).attr('y', 16)
          .attr('dominant-baseline', 'middle')
          .attr('fill', sensConfig.text)
          .attr('font-size', 9).attr('font-weight', 700)
          .text(sensLevel || '')
      }
    })

  // 渲染边（在节点之下）
  edges.forEach(edge => {
    const src = nodes.find(n => n.nodeId === edge.sourceNodeId)
    const tgt = nodes.find(n => n.nodeId === edge.targetNodeId)
    if (!src || !tgt) return
    const sx = src.x || 0, sy = src.y || 0
    const tx = tgt.x || 0, ty = tgt.y || 0
    const dx = tx - sx, dy = ty - sy
    const dist = Math.sqrt(dx * dx + dy * dy)
    if (dist === 0) return
    const nx = dx / dist, ny = dy / dist
    const ex = tx - nx * (TABLE_NODE_WIDTH / 2 + 2)
    const ey = ty - ny * (TABLE_NODE_HEIGHT / 2 + 2)
    const sx2 = sx + nx * (TABLE_NODE_WIDTH / 2 + 2)
    const sy2 = sy + ny * (TABLE_NODE_HEIGHT / 2 + 2)
    const edgeColor = TRANSFORM_COLORS[edge.transformType] || '#8C8C8C'
    const midX = (sx2 + ex) / 2, midY = (sy2 + ey) / 2
    const perpX = -ny * 15, perpY = nx * 15
    g.insert('path', 'g.node-imp')
      .attr('d', `M${sx2},${sy2} Q${midX + perpX},${midY + perpY} ${ex},${ey}`)
      .attr('fill', 'none')
      .attr('stroke', edgeColor)
      .attr('stroke-width', 1.5)
      .attr('stroke-opacity', 0.7)
      .attr('marker-end', 'url(#arrow-default)')
  })

  // 入场动画：从中心扩散到计算位置
  const svgWidth = parseInt(impactSvgSel.value.attr('width') || '900')
  const svgHeight = parseInt(impactSvgSel.value.attr('height') || '480')
  const centerX = svgWidth / 2
  const centerY = svgHeight / 2

  g.selectAll<SVGGElement, LineageNodeVO>('g.node-imp')
    .attr('transform', `translate(${centerX},${centerY}) scale(0.1)`)
    .attr('opacity', 0)
    .transition()
    .duration(600)
    .ease(d3.easeCubicOut)
    .delay((_d: any, i: number) => i * 40)
    .attr('transform', (d: LineageNodeVO) => `translate(${d.x || 0},${d.y || 0}) scale(1)`)
    .attr('opacity', 1)
}

/**
 * 纯布局计算：仅为节点计算 x/y 坐标，不操作 SVG
 */
function computeImpactLayout(nodes: LineageNodeVO[], _edges: LineageEdgeVO[]) {
  const levelMap = new Map<number, LineageNodeVO[]>()
  nodes.forEach(n => {
    const lvl = n.level ?? 0
    if (!levelMap.has(lvl)) levelMap.set(lvl, [])
    levelMap.get(lvl)!.push(n)
  })
  const levels = Array.from(levelMap.keys()).sort((a, b) => a - b)
  const layerSpacingY = 100
  levels.forEach((lvl, levelIdx) => {
    const nodesAtLevel = levelMap.get(lvl) || []
    const totalW = nodesAtLevel.length * 180
    const startX = (900 - totalW) / 2 + 90
    nodesAtLevel.forEach((node, nodeIdx) => {
      node.x = startX + nodeIdx * 180
      node.y = 60 + levelIdx * layerSpacingY
    })
  })
}

// ============================================================

function onViewModeChange() {
  loadGraph()
  loadLineageList()
}

function onDepthChange() {
  loadGraph()
}

function onSensitivityLevelChange() {
  renderGraph()
}

function onShowSensitiveOnlyChange() {
  renderGraph()
}

// ============================================================
// 影响分析
// ============================================================
async function handleImpactAnalyze(e: { key: string }) {
  if (!filterDsId.value) {
    message.warning('请先在工具栏选择数据源，再进行影响分析')
    return
  }
  const mode = e.key as 'downstream' | 'upstream'
  const tableName = searchKeyword.value || ''
  if (!tableName) {
    message.warning('请在搜索框输入目标表名')
    return
  }
  impactDirection.value = mode === 'downstream' ? 'DOWNSTREAM' : 'UPSTREAM'
  impactLoading.value = true
  impactModalVisible.value = true
  impactData.value = null
  try {
    const res = mode === 'downstream'
      ? await lineageManualApi.analyzeDownstream(filterDsId.value, tableName, undefined, filterMaxDepth.value)
      : await lineageManualApi.analyzeUpstream(filterDsId.value, tableName, undefined, filterMaxDepth.value)

    if (res.data?.success !== false) {
      impactData.value = res.data?.data as ImpactAnalysisResultVO
      impactData.value.direction = mode === 'downstream' ? 'DOWNSTREAM' : 'UPSTREAM'
      await nextTick()
      initImpactSvg()
      message.success(`${mode === 'downstream' ? '下游' : '上游'}追溯完成：${impactData.value.scope?.totalNodeCount} 个节点，${impactData.value.scope?.affectedTableCount} 张表`)
    }
  } catch (e: any) {
    message.error('追溯失败: ' + (e.message || ''))
    impactModalVisible.value = false
  } finally {
    impactLoading.value = false
  }
}

async function analyzeFromNode(direction: 'downstream' | 'upstream') {
  if (!selectedNode.value) return
  impactDirection.value = direction === 'downstream' ? 'DOWNSTREAM' : 'UPSTREAM'
  impactLoading.value = true
  impactModalVisible.value = true
  impactData.value = null
  try {
    const dir = direction === 'downstream' ? 'DOWNSTREAM' : 'UPSTREAM'
    const res = await lineageManualApi.analyzeFromNode(
      dir,
      selectedNode.value.dsId,
      selectedNode.value.tableName,
      selectedNode.value.columnName || undefined,
      5
    )
    if (res.data?.success !== false) {
      impactData.value = res.data?.data as ImpactAnalysisResultVO
      impactData.value.direction = dir
      await nextTick()
      initImpactSvg()
    }
  } catch (e: any) {
    message.error('分析失败: ' + (e.message || ''))
    impactModalVisible.value = false
  } finally {
    impactLoading.value = false
  }
}

// ============================================================
// 血缘列表交互
// ============================================================
function onLineageItemClick(item: LineageVO) {
  // 高亮图上对应节点
  const sourceNodeId = `${item.sourceDsId}_${item.sourceTable}${item.sourceColumn ? '_' + item.sourceColumn : ''}`
  const targetNodeId = `${item.targetDsId}_${item.targetTable}${item.targetColumn ? '_' + item.targetColumn : ''}`
  highlightedNodeIds.value = new Set([sourceNodeId, targetNodeId])
  hoveredLineageId.value = item.id
  renderGraph()
}

// ============================================================
// 新增/编辑血缘
// ============================================================
function openAddDrawer() {
  editMode.value = false
  stepCurrent.value = 0
  Object.assign(formData, {
    id: undefined,
    lineageType: 'TABLE',
    sourceDsId: undefined as unknown as number,
    sourceTable: '',
    sourceColumn: '',
    sourceColumnAlias: '',
    targetDsId: undefined as unknown as number,
    targetTable: '',
    targetColumn: '',
    targetColumnAlias: '',
    transformType: undefined as unknown as string,
    transformExpr: '',
    jobName: '',
    lineageSource: 'MANUAL',
  })
  addDrawerVisible.value = true
}

function openEditDrawer(item: LineageVO | null) {
  if (!item) return
  editMode.value = true
  stepCurrent.value = 0
  Object.assign(formData, {
    id: item.id,
    lineageType: item.lineageType,
    sourceDsId: item.sourceDsId,
    sourceTable: item.sourceTable,
    sourceColumn: item.sourceColumn || '',
    sourceColumnAlias: item.sourceColumnAlias || '',
    targetDsId: item.targetDsId,
    targetTable: item.targetTable,
    targetColumn: item.targetColumn || '',
    targetColumnAlias: item.targetColumnAlias || '',
    transformType: item.transformType || undefined as unknown as string,
    transformExpr: item.transformExpr || '',
    jobName: item.jobName || '',
    lineageSource: item.lineageSource || 'MANUAL',
  })
  addDrawerVisible.value = true
}

function closeAddDrawer() {
  addDrawerVisible.value = false
}

async function onSourceDsChange() {
  sourceTableOptions.value = []
  sourceColOptions.value = []
  formData.sourceTable = ''
  formData.sourceColumn = ''
  if (!formData.sourceDsId) return
  try {
    const res = await lineageManualApi.listTables(formData.sourceDsId as number)
    if (res.data?.success !== false) {
      sourceTableOptions.value = (res.data as any)?.data || []
    }
  } catch { /* ignore */ }
}

async function onTargetDsChange() {
  targetTableOptions.value = []
  targetColOptions.value = []
  formData.targetTable = ''
  formData.targetColumn = ''
  if (!formData.targetDsId) return
  try {
    const res = await lineageManualApi.listTables(formData.targetDsId as number)
    if (res.data?.success !== false) {
      targetTableOptions.value = (res.data as any)?.data || []
    }
  } catch { /* ignore */ }
}

async function onSourceTableChange() {
  sourceColOptions.value = []
  formData.sourceColumn = ''
  if (!formData.sourceDsId || !formData.sourceTable) return
  try {
    sourceColLoading.value = true
    const res = await lineageManualApi.listColumns(formData.sourceDsId as number, formData.sourceTable as string)
    if (res.data?.success !== false) {
      sourceColOptions.value = ((res.data as any)?.data || []).map((c: any) => ({
        value: c.columnName,
        label: `${c.columnName}${c.columnAlias ? ' — ' + c.columnAlias : ''} (${c.dataType})`,
        data: c,
      }))
    }
  } catch { /* ignore */ }
  finally { sourceColLoading.value = false }
}

async function onTargetTableChange() {
  targetColOptions.value = []
  formData.targetColumn = ''
  if (!formData.targetDsId || !formData.targetTable) return
  try {
    targetColLoading.value = true
    const res = await lineageManualApi.listColumns(formData.targetDsId as number, formData.targetTable as string)
    if (res.data?.success !== false) {
      targetColOptions.value = ((res.data as any)?.data || []).map((c: any) => ({
        value: c.columnName,
        label: `${c.columnName}${c.columnAlias ? ' — ' + c.columnAlias : ''} (${c.dataType})`,
        data: c,
      }))
    }
  } catch { /* ignore */ }
  finally { targetColLoading.value = false }
}

async function handleSourceTableSearch(value: string) {
  if (!value || !formData.sourceDsId) return
  try {
    const res = await lineageManualApi.suggest(formData.sourceDsId as number, value)
    if (res.data?.success !== false) {
      sourceTableOptions.value = (res.data?.data?.tables || []).map((t: any) => ({
        ...t,
        value: t.tableName,
        label: `${t.tableName}${t.tableAlias ? ' (' + t.tableAlias + ')' : ''} ${t.dataLayer ? '[' + t.dataLayer + ']' : ''}`,
      }))
    }
  } catch { /* ignore */ }
}

async function handleTargetTableSearch(value: string) {
  if (!value || !formData.targetDsId) return
  try {
    const res = await lineageManualApi.suggest(formData.targetDsId as number, value)
    if (res.data?.success !== false) {
      targetTableOptions.value = (res.data?.data?.tables || []).map((t: any) => ({
        ...t,
        value: t.tableName,
        label: `${t.tableName}${t.tableAlias ? ' (' + t.tableAlias + ')' : ''} ${t.dataLayer ? '[' + t.dataLayer + ']' : ''}`,
      }))
    }
  } catch { /* ignore */ }
}

function handleNextStep() {
  formRef.value?.validateFields().then(() => {
    stepCurrent.value++
  }).catch(() => {})
}

async function handleSave() {
  try {
    await formRef.value?.validateFields()
    saving.value = true
    let res: any
    if (editMode.value && formData.id) {
      res = await lineageApi.update(formData.id, { ...formData })
    } else {
      res = await lineageApi.save({ ...formData })
    }
    if (res.data?.success !== false) {
      message.success(editMode.value ? '更新成功' : '创建成功')
      addDrawerVisible.value = false
      await Promise.all([loadGraph(), loadLineageList(), loadStats()])
    } else {
      message.error(res.data?.message || '操作失败')
    }
  } catch (e: any) {
    if (e.errorFields) return
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ============================================================
// 详情 & 删除
// ============================================================
async function openDetailDrawer(item: LineageVO) {
  try {
    const res = await lineageApi.getDetail(item.id)
    if (res.data?.success !== false) {
      currentLineage.value = res.data?.data
      detailDrawerVisible.value = true
    }
  } catch { message.error('加载详情失败') }
}

async function handleDelete(id: number | undefined) {
  if (!id) return
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该血缘关系吗？删除后不可恢复。',
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        const res = await lineageApi.delete(id)
        if (res.data?.success !== false) {
          message.success('删除成功')
          detailDrawerVisible.value = false
          await Promise.all([loadGraph(), loadLineageList(), loadStats()])
        }
      } catch { message.error('删除失败') }
    }
  })
}

// ============================================================
// 辅助函数
// ============================================================
function getLayerColor(layer: string | null | undefined): string {
  if (!layer) return '#8C8C8C'
  return LAYER_COLORS[layer] || '#8C8C8C'
}

function getSensitivityBorderColor(level: string): string {
  return SENSITIVITY_LEVEL_COLORS[level]?.border || '#8C8C8C'
}

function getSensitivityTextColor(level: string): string {
  return SENSITIVITY_LEVEL_COLORS[level]?.text || '#8C8C8C'
}

function getSensitivityBgColor(level: string): string {
  return SENSITIVITY_LEVEL_COLORS[level]?.bg || '#F5F7FA'
}

function getTransformLabel(type: string | undefined | null): string {
  if (!type) return '直接传递'
  return transformTypeOptions.find(t => t.value === type)?.label || type
}

function truncate(str: string | undefined | null, max: number): string {
  if (!str) return ''
  return str.length > max ? str.substring(0, max) + '…' : str
}

function getSensitivityLevelLabel(level: string): string {
  const map: Record<string, string> = {
    L4: 'L4 机密', L3: 'L3 敏感', L2: 'L2 内部', L1: 'L1 公开'
  }
  return map[level] || level
}

function getImpactLevelCount(level: string): number {
  if (!impactData.value?.nodes) return 0
  if (level === 'total') {
    return impactData.value.nodes.filter(n => n.sensitivityLevel && n.sensitivityLevel !== 'L1').length
  }
  return impactData.value.nodes.filter(n => n.sensitivityLevel === level).length
}

function getImpactLevelPct(level: string): number {
  const total = getImpactLevelCount('total')
  if (total === 0) return 0
  return Math.round((getImpactLevelCount(level) / total) * 100)
}

// 监听侧边面板打开/关闭，重新调整 SVG 尺寸
watch(selectedNode, () => {
  nextTick(() => {
    if (svgSelection) {
      const container = graphContainerRef.value
      if (!container) return
      const width = container.clientWidth - (selectedNode.value ? 340 : 0)
      const height = container.clientHeight
      svgSelection.attr('width', width).attr('height', height)
      centerGraph()
    }
  })
})
</script>

<style scoped>
.page-container { padding: 24px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { flex-shrink: 0; }
.page-title { font-size: 20px; font-weight: 700; color: #1F1F1F; margin: 0 0 4px; }
.page-subtitle { font-size: 13px; color: #8C8C8C; margin: 0; }

.stat-mini-card {
  border-radius: 8px;
  padding: 14px 16px;
  color: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.mini-value { font-size: 22px; font-weight: 700; }
.mini-label { font-size: 11px; opacity: 0.9; margin-top: 3px; }

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.graph-legend {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #8C8C8C;
  margin-left: auto;
}
.legend-title { font-weight: 600; color: #1F1F1F; }
.legend-item { display: flex; align-items: center; gap: 4px; }
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
.legend-node-table {
  width: 14px;
  height: 10px;
  border: 2px solid #1677FF;
  border-radius: 3px;
  background: white;
  display: inline-block;
}
.legend-node-col {
  width: 12px;
  height: 8px;
  border: 1.5px dashed #722ED1;
  border-radius: 2px;
  background: #F5F7FA;
  display: inline-block;
}
.legend-divider { color: #E8E8E8; }

.main-content {
  display: flex;
  gap: 12px;
  height: calc(100vh - 380px);
  min-height: 500px;
}

.graph-container {
  flex: 1;
  position: relative;
  background: linear-gradient(135deg, #0D1117 0%, #161B22 100%);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}

.lineage-svg {
  display: block;
  width: 100%;
  height: 100%;
}

.graph-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(13,17,23,0.7);
  color: white;
  z-index: 10;
}

.graph-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #8C8C8C;
}
.empty-icon { opacity: 0.6; }
.empty-text { font-size: 16px; font-weight: 600; color: #1F1F1F; }
.empty-desc { font-size: 13px; color: #8C8C8C; }

/* 节点详情面板 */
.node-detail-panel {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 320px;
  background: white;
  border-left: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  z-index: 20;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #F5F7FA;
}
.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.node-type-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: white;
}
.node-type-badge.table { background: #1677FF; }
.node-type-badge.column { background: #722ED1; }
.node-name { font-size: 13px; color: #1F1F1F; }

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.layer-tag {
  padding: 2px 8px;
  border-radius: 4px;
  color: white;
  font-size: 11px;
  font-weight: 600;
}

.sensitivity-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid;
  font-size: 11px;
  font-weight: 700;
}

.col-name {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  background: #F5F7FA;
  padding: 2px 6px;
  border-radius: 3px;
  color: #1677FF;
}

.panel-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

/* 侧边血缘列表 */
.lineage-list-panel {
  width: 340px;
  flex-shrink: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.list-panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #1677FF 0%, #00B4DB 100%);
  color: white;
  border-radius: 8px 8px 0 0;
}
.list-title { font-weight: 600; font-size: 14px; }

.list-filter {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.list-body {
  flex: 1;
  overflow-y: auto;
}

.lineage-list-item {
  padding: 10px 12px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
}
.lineage-list-item:hover,
.lineage-list-item.active {
  background: #EFF6FF;
}
.lineage-list-item.active {
  border-left: 3px solid #1677FF;
}

.item-header { display: flex; gap: 4px; margin-bottom: 6px; }

.item-flow {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.flow-table {
  font-size: 12px;
  font-family: 'JetBrains Mono', monospace;
  color: #1F1F1F;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flow-arrow { color: #8C8C8C; font-size: 10px; }
.flow-transform {
  font-size: 10px;
  color: #FAAD14;
  max-width: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta { font-size: 11px; color: #8C8C8C; }
.item-actions {
  display: flex;
  gap: 2px;
  margin-top: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}
.lineage-list-item:hover .item-actions { opacity: 1; }

.list-loading {
  display: flex;
  justify-content: center;
  padding: 20px;
}
.list-empty {
  text-align: center;
  padding: 40px;
  color: #8C8C8C;
  font-size: 13px;
}

.list-footer {
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
}

/* 抽屉表单 */
.drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: #8C8C8C;
  margin-top: 4px;
}

/* 预览 */
.preview-box {
  background: #F5F7FA;
  border-radius: 8px;
  padding: 16px;
}
.preview-flow {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.preview-node {
  font-size: 13px;
  font-weight: 500;
  color: #1F1F1F;
  background: white;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1.5px solid #1677FF;
}
.preview-arrow { color: #1677FF; font-size: 14px; }
.preview-transform {
  font-size: 11px;
  color: #FAAD14;
  background: #FFFAE6;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid #FAAD14;
}

/* 详情抽屉 */
.detail-content { padding-bottom: 60px; }
.flow-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #F5F7FA;
  border-radius: 8px;
  margin-bottom: 12px;
}
.flow-card-side { flex: 1; }
.flow-card-label {
  font-size: 11px;
  color: #8C8C8C;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.flow-card-ds { font-size: 12px; color: #8C8C8C; margin-bottom: 4px; }
.flow-card-table {
  font-size: 13px;
  font-weight: 600;
  color: #1F1F1F;
  font-family: 'JetBrains Mono', monospace;
}
.flow-card-col {
  font-size: 11px;
  color: #1677FF;
  font-family: 'JetBrains Mono', monospace;
  margin-top: 2px;
}
.col-alias { color: #8C8C8C; font-family: inherit; }
.flow-card-arrow {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #8C8C8C;
}
.flow-transform-tag {
  font-size: 10px;
  color: #FAAD14;
  background: #FFFAE6;
  padding: 2px 6px;
  border-radius: 3px;
  border: 1px solid #FAAD14;
}
.detail-label {
  font-size: 12px;
  font-weight: 600;
  color: #8C8C8C;
  margin-bottom: 6px;
}
.transform-expr {
  display: block;
  background: #F5F7FA;
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-family: 'JetBrains Mono', monospace;
  color: #1F1F1F;
  border-left: 3px solid #1677FF;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 过渡动画 */
.slide-panel-enter-active,
.slide-panel-leave-active {
  transition: transform 0.25s ease, opacity 0.25s ease;
}
.slide-panel-enter-from,
.slide-panel-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* D3 节点样式（全局） */
:deep(.node-rect) {
  transition: stroke-width 0.15s, stroke-opacity 0.15s, filter 0.15s;
}
:deep(.node:hover .node-rect) {
  filter: url(#nodeShadow);
}

/* 批量导入 */
.import-step { min-height: 200px; }
.form-label { font-size: 14px; font-weight: 500; margin-bottom: 6px; color: #1F1F1F; }
.form-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; }
.import-records-hint { font-size: 13px; color: #52C41A; padding: 8px 12px; background: #f6ffed; border-radius: 6px; border: 1px solid #b7eb8f; }
.import-errors { max-height: 180px; overflow-y: auto; }
.import-errors-title { font-size: 12px; font-weight: 600; color: #FF4D4F; margin-bottom: 6px; }
.import-error-item { font-size: 12px; margin-bottom: 4px; }
.import-error-row { color: #8C8C8C; margin-right: 8px; }
.import-error-msg { color: #FF4D4F; }

/* 影响分析模态框 */
.impact-loading { display: flex; align-items: center; justify-content: center; min-height: 300px; }
.impact-stat-card {
  background: linear-gradient(135deg, #1677FF 0%, #00B4DB 100%);
  border-radius: 8px;
  padding: 14px 8px;
  text-align: center;
  color: white;
}
.impact-stat-value { font-size: 22px; font-weight: 700; }
.impact-stat-label { font-size: 11px; opacity: 0.85; margin-top: 4px; }
.impact-graph-wrapper {
  background: linear-gradient(135deg, #0D1117 0%, #161B22 100%);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.impact-svg { display: block; width: 100%; }

.impact-sensitivity-dist {
  margin-top: 16px;
  background: #FAFAFA;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px 16px;

  .dist-title {
    font-size: 13px;
    font-weight: 600;
    color: #1F1F1F;
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .dist-bars {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .dist-bar-item {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;

    .dist-bar-label {
      display: flex;
      align-items: center;
      gap: 4px;
      min-width: 80px;
      flex-shrink: 0;

      .dist-level-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        flex-shrink: 0;
      }

      .dist-level-name {
        font-size: 12px;
        color: #8C8C8C;
        flex: 1;
      }

      .dist-level-count {
        font-size: 12px;
        font-weight: 700;
        color: #1F1F1F;
        min-width: 24px;
        text-align: right;
      }
    }

    .dist-bar-track {
      flex: 1;
      height: 6px;
      background: #E8E8E8;
      border-radius: 3px;
      overflow: hidden;
      min-width: 60px;

      .dist-bar-fill {
        height: 100%;
        border-radius: 3px;
        transition: width 0.4s ease;
        min-width: 0;
      }
    }

    .dist-bar-pct {
      font-size: 11px;
      color: #8C8C8C;
      min-width: 32px;
      text-align: right;
      flex-shrink: 0;
    }
  }
}
</style>
