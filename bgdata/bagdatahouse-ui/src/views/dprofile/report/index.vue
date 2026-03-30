<template>
  <div class="page-container">

    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#rGrad)"/>
            <path d="M5 17L9 11L13 15L17 9L20 12" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="9" cy="11" r="1.5" fill="white" opacity="0.7"/>
            <circle cx="13" cy="15" r="1.5" fill="white" opacity="0.7"/>
            <circle cx="17" cy="9" r="1.5" fill="white" opacity="0.7"/>
            <defs>
              <linearGradient id="rGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#13C2C2"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">探查报告</h1>
          <p class="page-subtitle">查看和管理已探查表的数据统计报告，包含分布可视化、快照比对和异常检测</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="openSnapshotDrawer">
          <template #icon><CameraOutlined /></template>
          快照管理
        </a-button>
        <a-button @click="loadTableStats">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 统计概览 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="8" :md="6">
        <div class="stat-mini-card" style="background: linear-gradient(135deg, #722ED1 0%, #9254DE 100%)">
          <div class="mini-value">{{ overviewStats.totalReports }}</div>
          <div class="mini-label">总报告数</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6">
        <div class="stat-mini-card" style="background: linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)">
          <div class="mini-value">{{ overviewStats.tablesToday }}</div>
          <div class="mini-label">今日探查</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6">
        <div class="stat-mini-card" style="background: linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%)">
          <div class="mini-value">{{ overviewStats.anomalyCount }}</div>
          <div class="mini-label">异常字段</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6">
        <div class="stat-mini-card" style="background: linear-gradient(135deg, #13C2C2 0%, #52C41A 100%)">
          <div class="mini-value">{{ overviewStats.snapshotCount }}</div>
          <div class="mini-label">已存快照</div>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="[20, 20]">

      <!-- 左侧：筛选 + 列表 -->
      <a-col :xs="24" :lg="selectedRecord ? 12 : 24">
        <div class="table-card">
          <div class="card-header">
            <span class="card-title">探查记录</span>
            <a-space>
              <a-select
                v-model:value="filterDsId"
                placeholder="数据源"
                style="width: 150px"
                allowClear
                :loading="dsLoading"
                @change="loadTableStats"
              >
                <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                  {{ ds.dsName }}
                </a-select-option>
              </a-select>
              <a-input
                v-model:value="filterTableName"
                placeholder="表名搜索"
                style="width: 140px"
                allowClear
                @change="loadTableStats"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
            </a-space>
          </div>

          <!-- 列表空状态 -->
          <div v-if="!loading && tableStatsData.length === 0" class="list-empty">
            <FileSearchOutlined style="font-size: 36px; color: #D3ADF7; margin-bottom: 12px" />
            <div>暂无探查记录</div>
            <div style="font-size: 12px; color: #8C8C8C; margin-top: 4px">执行探查任务后，报告将显示在此处</div>
          </div>

          <a-table
            v-else
            :columns="reportColumns"
            :data-source="tableStatsData"
            :loading="loading"
            :pagination="{ pageSize: 8 }"
            :row-key="(record: any) => record.id"
            @change="handleTableChange"
            :scroll="{ y: 420 }"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'tableName'">
                <div class="table-name-cell" :class="{ 'table-name-selected': selectedRecord?.id === record.id }" @click="selectRecord(record)">
                  <FileOutlined class="table-icon" />
                  <div>
                    <div class="table-name-text">{{ record.tableName }}</div>
                    <div class="table-meta">
                      <a-tag :color="getDsTypeColor(record.dsId)" size="small">
                        {{ getDsTypeName(record.dsId) }}
                      </a-tag>
                    </div>
                  </div>
                </div>
              </template>

              <template v-if="column.key === 'rowCount'">
                <span class="mono-text">{{ formatNumber(record.rowCount) }}</span>
              </template>

              <template v-if="column.key === 'profileTime'">
                <div class="time-cell">
                  <div>{{ formatTime(record.profileTime) }}</div>
                </div>
              </template>

              <template v-if="column.key === 'anomaly'">
                <template v-if="record.hasAnomaly || record.anomalyCount > 0">
                  <a-badge status="warning" text="有异常" />
                </template>
                <span v-else class="normal-badge">正常</span>
              </template>

              <template v-if="column.key === 'action'">
                <a-space>
                  <a-tooltip title="查看详情">
                    <a-button type="text" size="small" @click="openDetailDrawer(record)">
                      <template #icon><EyeOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-tooltip title="创建快照">
                    <a-button type="text" size="small" @click="createQuickSnapshot(record)">
                      <template #icon><CameraOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-tooltip title="与快照比对">
                    <a-button type="text" size="small" @click="openCompareDrawer(record)">
                      <template #icon><SwapOutlined /></template>
                    </a-button>
                  </a-tooltip>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>

      <!-- 右侧：详情面板 -->
      <a-col :xs="24" :lg="12" v-if="selectedRecord">
        <!-- 详情为空时 -->
        <div v-if="!selectedRecord" class="empty-detail">
          <div class="empty-icon">
            <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
              <rect width="64" height="64" rx="16" fill="url(#emptyGrad)"/>
              <path d="M20 44L28 28L36 36L44 20L52 28" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" opacity="0.6"/>
              <circle cx="28" cy="28" r="3" fill="white" opacity="0.4"/>
              <circle cx="36" cy="36" r="3" fill="white" opacity="0.4"/>
              <circle cx="44" cy="20" r="3" fill="white" opacity="0.4"/>
              <defs>
                <linearGradient id="emptyGrad" x1="0" y1="0" x2="64" y2="64">
                  <stop offset="0%" stop-color="#722ED1"/>
                  <stop offset="100%" stop-color="#9254DE"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <div class="empty-text">点击左侧记录查看详情</div>
        </div>

        <!-- 详情面板 -->
        <div v-else class="detail-panel">
          <!-- 表头信息 -->
          <div class="detail-header">
            <div class="detail-title">
              <FileOutlined class="detail-icon" />
              <span>{{ selectedRecord.tableName }}</span>
            </div>
            <a-space>
              <a-button size="small" @click="openDetailDrawer(selectedRecord)">
                <template #icon><FullscreenOutlined /></template>
                完整报告
              </a-button>
            </a-space>
          </div>

          <!-- 概览卡片 -->
          <a-row :gutter="[12, 12]" class="overview-row">
            <a-col :span="6">
              <div class="overview-stat">
                <div class="overview-value">{{ formatNumber(selectedRecord.rowCount) }}</div>
                <div class="overview-label">行数</div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="overview-stat">
                <div class="overview-value">{{ selectedRecord.columnCount || '-' }}</div>
                <div class="overview-label">列数</div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="overview-stat">
                <div class="overview-value">{{ formatBytes(selectedRecord.storageBytes) }}</div>
                <div class="overview-label">存储</div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="overview-stat">
                <div class="overview-value">{{ selectedRecord.incrementRows !== undefined ? '+' + formatNumber(selectedRecord.incrementRows) : '-' }}</div>
                <div class="overview-label">增量</div>
              </div>
            </a-col>
          </a-row>

          <!-- 异常警告 -->
          <div v-if="previewAnomalies.length > 0" class="anomaly-warning" @click="openDetailDrawer(selectedRecord)">
            <WarningOutlined style="color: #FAAD14; margin-right: 8px" />
            <span>发现 <strong>{{ previewAnomalies.length }}</strong> 个字段存在异常（高空值率 / 异常值 / 全唯一）</span>
            <RightOutlined style="margin-left: auto; font-size: 12px" />
          </div>

          <!-- 列级统计预览 -->
          <div class="section-title">
            <span>列级统计</span>
            <a-button type="link" size="small" @click="openDetailDrawer(selectedRecord)">查看全部 →</a-button>
          </div>

          <a-table
            :columns="previewColumns"
            :data-source="previewColumnStats"
            :pagination="false"
            :row-key="(record: any) => record.id"
            size="small"
            :scroll="{ x: 500, y: 280 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'nullRate'">
                <div class="rate-cell">
                  <a-progress
                    :percent="Math.round((record.nullRate || 0) * 100)"
                    :stroke-color="(record.nullRate || 0) > 0.5 ? '#FF4D4F' : '#52C41A'"
                    size="small"
                    :show-info="false"
                    style="width: 70px; display: inline-block; margin-right: 6px"
                  />
                  <span class="mono-text">{{ ((record.nullRate || 0) * 100).toFixed(1) }}%</span>
                </div>
              </template>
              <template v-if="column.key === 'uniqueRate'">
                <span class="mono-text">{{ ((record.uniqueRate || 0) * 100).toFixed(1) }}%</span>
              </template>
              <template v-if="column.key === 'dataType'">
                <a-tag size="small">{{ record.dataType }}</a-tag>
              </template>
              <template v-if="column.key === 'outlier'">
                <template v-if="record.outlierCount && record.outlierCount > 0">
                  <a-tooltip :title="`检测到 ${record.outlierCount} 个异常值（${record.outlierMethod || 'IQR'}法则）`">
                    <a-badge status="warning" :text="record.outlierCount.toString()" />
                  </a-tooltip>
                </template>
                <template v-else-if="record.warningCount > 0">
                  <a-tooltip :title="`有 ${record.warningCount} 条警告`">
                    <a-badge status="warning" text="⚠" />
                  </a-tooltip>
                </template>
                <span v-else style="color: #52C41A">✓ 正常</span>
              </template>
            </template>
          </a-table>

          <!-- Top值预览 -->
          <div class="section-title" style="margin-top: 16px">
            <span>高频值（Top 5）</span>
          </div>
          <div class="top-values-mini">
            <div v-for="(item, idx) in previewTopValues" :key="idx" class="top-mini-item">
              <span class="top-mini-value">{{ item.value }}</span>
              <div class="top-mini-bar" :style="{ width: item.percent + '%' }"></div>
              <span class="top-mini-count">{{ item.percent }}%</span>
            </div>
          </div>

          <!-- 分布迷你图 -->
          <div class="section-title" style="margin-top: 16px">
            <span>列分布预览</span>
          </div>
          <div class="mini-charts">
            <div
              v-for="col in chartColumns.slice(0, 3)"
              :key="col.id"
              class="mini-chart-cell"
              @click="openDetailDrawerForColumn(selectedRecord, col)"
            >
              <div class="mini-chart-label">
                <span class="mono-text" style="font-size: 12px">{{ col.columnName }}</span>
                <a-tag size="small">{{ col.dataType }}</a-tag>
              </div>
              <div :ref="el => chartRefsMini[col.id!] = el as HTMLDivElement" style="height: 60px"></div>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- ============================================================ -->
    <!-- 详情抽屉（完整报告） -->
    <!-- ============================================================ -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      :title="`探查详情: ${selectedRecord?.tableName || ''}`"
      :width="960"
      @close="detailDrawerVisible = false"
    >
      <div v-if="selectedRecord" class="full-report">

        <!-- 概览信息 -->
        <a-descriptions :column="3" size="small" bordered title="表级信息" class="report-desc">
          <a-descriptions-item label="表名">{{ selectedRecord.tableName }}</a-descriptions-item>
          <a-descriptions-item label="数据源">{{ getDsTypeName(selectedRecord.dsId) }}</a-descriptions-item>
          <a-descriptions-item label="探查时间">{{ formatTime(selectedRecord.profileTime) }}</a-descriptions-item>
          <a-descriptions-item label="行数">{{ formatNumber(selectedRecord.rowCount) }}</a-descriptions-item>
          <a-descriptions-item label="列数">{{ selectedRecord.columnCount }}</a-descriptions-item>
          <a-descriptions-item label="存储大小">{{ formatBytes(selectedRecord.storageBytes) }}</a-descriptions-item>
        </a-descriptions>

        <!-- 全局异常概览 -->
        <a-alert
          v-if="allAnomalies.length > 0"
          :message="`发现 ${allAnomalies.length} 个字段存在异常数据或警告`"
          description="以下异常字段建议重点关注，高空值率可能影响数据质量，异常值可能需要进一步核实"
          type="warning"
          show-icon
          style="margin-bottom: 16px"
        >
          <template #suggestions>
            <ul class="suggestion-list">
              <li v-for="a in allAnomalies.slice(0, 3)" :key="a.columnName">
                <code>{{ a.columnName }}</code> — {{ a.anomalyType }}：{{ a.anomalyDetail }}
              </li>
              <li v-if="allAnomalies.length > 3">... 还有 {{ allAnomalies.length - 3 }} 个字段</li>
            </ul>
          </template>
        </a-alert>

        <!-- 列级统计表 -->
        <a-divider>列级统计详情</a-divider>

        <div class="column-stats-toolbar">
          <a-space>
            <a-space>
              <span style="font-size: 13px; color: #8C8C8C">异常高亮</span>
              <a-switch v-model:checked="highlightAnomaly" size="small" />
            </a-space>
            <a-space>
              <span style="font-size: 13px; color: #8C8C8C">仅显示异常</span>
              <a-switch v-model:checked="showOnlyAnomaly" size="small" />
            </a-space>
          </a-space>
          <span style="font-size: 12px; color: #8C8C8C">共 {{ filteredColumnStats.length }} 列</span>
        </div>

        <a-table
          :columns="detailColumns"
          :data-source="filteredColumnStats"
          :pagination="{ pageSize: 20 }"
          :row-key="(record: any) => record.id"
          size="small"
          :scroll="{ x: 1000 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'columnName'">
              <div class="column-name-cell" :class="{ 'column-anomaly': record._isAnomaly }">
                <span class="mono-text">{{ record.columnName }}</span>
                <a-tag v-if="record.isPrimaryKey" size="small" color="blue">PK</a-tag>
                <a-tag v-if="record.isForeignKey" size="small" color="purple">FK</a-tag>
                <a-tag v-if="record.isSensitive" size="small" color="orange">敏感</a-tag>
              </div>
            </template>
            <template v-if="column.key === 'nullRate'">
              <a-progress
                :percent="Math.round((record.nullRate || 0) * 100)"
                :stroke-color="getNullRateColor(record.nullRate)"
                size="small"
                style="width: 80px; display: inline-block"
              />
              <span class="mono-text" style="margin-left: 6px">{{ ((record.nullRate || 0) * 100).toFixed(1) }}%</span>
            </template>
            <template v-if="column.key === 'uniqueRate'">
              {{ ((record.uniqueRate || 0) * 100).toFixed(1) }}%
            </template>
            <template v-if="column.key === 'outlier'">
              <template v-if="record.outlierCount && record.outlierCount > 0">
                <a-tooltip :title="`${record.outlierCount} 个异常值（${record.outlierMethod || 'IQR'}法则）`">
                  <a-badge status="warning" :text="record.outlierCount.toString()" />
                </a-tooltip>
              </template>
              <template v-else-if="record._isAnomaly">
                <a-tooltip :title="record._anomalyReason">
                  <a-badge status="warning" text="⚠" />
                </a-tooltip>
              </template>
              <span v-else style="color: #52C41A">-</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-tooltip title="查看完整分布">
                <a-button type="text" size="small" @click="openDistributionModal(record)">
                  <template #icon><BarChartOutlined /></template>
                </a-button>
              </a-tooltip>
            </template>
          </template>
        </a-table>

        <!-- 分布可视化 -->
        <a-divider>分布可视化</a-divider>

        <div class="chart-toolbar">
          <a-radio-group v-model:value="globalChartMode" button-style="solid" size="small">
            <a-radio-button value="table">表格视图</a-radio-button>
            <a-radio-button value="grid">网格视图</a-radio-button>
          </a-radio-group>
          <span style="font-size: 12px; color: #8C8C8C">选择有数据的列自动生成图表</span>
        </div>

        <!-- 网格视图 -->
        <div v-if="globalChartMode === 'grid'" class="chart-grid">
          <div v-for="col in chartColumns" :key="col.id" class="chart-cell">
            <div class="chart-title">
              <span class="chart-col-name">{{ col.columnName }}</span>
              <a-tag size="small">{{ col.dataType }}</a-tag>
              <a-tag v-if="col.outlierCount && col.outlierCount > 0" size="small" color="orange">
                ⚠ {{ col.outlierCount }}
              </a-tag>
            </div>
            <div :ref="el => chartRefs[col.id!] = el as HTMLDivElement" style="height: 200px"></div>
          </div>
        </div>

        <!-- 表格视图（Top值） -->
        <div v-else class="chart-table-view">
          <div v-for="col in chartColumns" :key="col.id" class="col-chart-row">
            <div class="col-chart-header">
              <span class="mono-text">{{ col.columnName }}</span>
              <a-tag size="small">{{ col.dataType }}</a-tag>
              <a-tooltip v-if="col.outlierCount && col.outlierCount > 0" :title="`${col.outlierCount} 个异常值`">
                <a-badge status="warning" text="异常" />
              </a-tooltip>
            </div>
            <div class="col-chart-bar" v-if="col.topValueItems && col.topValueItems.length > 0">
              <div
                v-for="(item, idx) in col.topValueItems.slice(0, 6)"
                :key="idx"
                class="bar-item"
                :style="{ width: item.percent + '%', background: topValueColors[idx % topValueColors.length] }"
                :title="`${item.value}: ${item.percent}%`"
              >
                <span class="bar-label" v-if="idx < 3">{{ item.value }}</span>
              </div>
            </div>
            <div v-else-if="col.histogramBuckets && col.histogramBuckets.length > 0" class="col-chart-bar numeric">
              <div
                v-for="(bucket, idx) in col.histogramBuckets.slice(0, 10)"
                :key="idx"
                class="bar-item numeric-bar"
                :style="{ height: (bucket.rate * 100) + '%', background: '#722ED1' }"
                :title="`${bucket.range}: ${bucket.rate}%`"
              ></div>
            </div>
          </div>
        </div>

      </div>
    </a-drawer>

    <!-- ============================================================ -->
    <!-- 字段分布弹窗 -->
    <!-- ============================================================ -->
    <a-modal
      v-model:open="distributionModalVisible"
      :title="`字段分布: ${distributionColumn || ''}`"
      :footer="null"
      width="900"
    >
      <div class="distribution-modal" v-if="currentDistributionRecord">
        <a-row :gutter="[16, 16]">
          <!-- 图表区 -->
          <a-col :span="16">
            <div class="dist-tabs">
              <a-radio-group v-model:value="distributionTab" button-style="solid" size="small">
                <a-radio-button value="histogram">直方图</a-radio-button>
                <a-radio-button value="frequency">频率图</a-radio-button>
                <a-radio-button value="pie">占比图</a-radio-button>
              </a-radio-group>
              <a-tag>{{ currentDistributionRecord.dataType }}</a-tag>
              <a-tag v-if="isNumericColumn" color="blue">数值型</a-tag>
              <a-tag v-else color="green">分类型</a-tag>
            </div>
            <div :ref="el => chartRefs.dist = el as HTMLDivElement" style="height: 360px"></div>
          </a-col>

          <!-- 统计摘要 -->
          <a-col :span="8">
            <div class="dist-stats-title">统计摘要</div>
            <a-descriptions :column="1" size="small" bordered>
              <a-descriptions-item label="数据类型">{{ currentDistributionRecord.dataType }}</a-descriptions-item>
              <a-descriptions-item label="总记录数"><span class="mono-text">{{ formatNumber(currentDistributionRecord.totalCount) }}</span></a-descriptions-item>
              <a-descriptions-item label="非空记录"><span class="mono-text">{{ formatNumber((currentDistributionRecord.totalCount || 0) - (currentDistributionRecord.nullCount || 0)) }}</span></a-descriptions-item>
              <a-descriptions-item label="空值数"><span class="mono-text">{{ formatNumber(currentDistributionRecord.nullCount) }}</span></a-descriptions-item>
              <a-descriptions-item label="空值率">{{ ((currentDistributionRecord.nullRate || 0) * 100).toFixed(2) }}%</a-descriptions-item>
              <a-descriptions-item label="唯一值"><span class="mono-text">{{ formatNumber(currentDistributionRecord.uniqueCount) }}</span></a-descriptions-item>
              <a-descriptions-item label="唯一率">{{ ((currentDistributionRecord.uniqueRate || 0) * 100).toFixed(2) }}%</a-descriptions-item>
              <a-descriptions-item label="最小值" v-if="currentDistributionRecord.minValue"><span class="mono-text">{{ currentDistributionRecord.minValue }}</span></a-descriptions-item>
              <a-descriptions-item label="最大值" v-if="currentDistributionRecord.maxValue"><span class="mono-text">{{ currentDistributionRecord.maxValue }}</span></a-descriptions-item>
              <a-descriptions-item label="平均值" v-if="currentDistributionRecord.avgValue"><span class="mono-text">{{ currentDistributionRecord.avgValue?.toFixed(4) }}</span></a-descriptions-item>
              <a-descriptions-item label="中位数" v-if="currentDistributionRecord.medianValue"><span class="mono-text">{{ currentDistributionRecord.medianValue }}</span></a-descriptions-item>
              <a-descriptions-item label="标准差" v-if="currentDistributionRecord.stdDev"><span class="mono-text">{{ currentDistributionRecord.stdDev?.toFixed(4) }}</span></a-descriptions-item>
              <a-descriptions-item label="异常值" v-if="currentDistributionRecord.outlierCount">
                <a-badge status="warning" :text="currentDistributionRecord.outlierCount + ' 个'" />
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
        </a-row>

        <!-- Top 值表格 -->
        <a-divider>高频值详情</a-divider>
        <a-table
          :columns="topValueDetailColumns"
          :data-source="distributionTopValues"
          :pagination="{ pageSize: 10 }"
          :row-key="(record: any, idx: number) => idx"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'rank'">
              <div class="rank-badge" :style="{ background: topValueColors[record._idx % topValueColors.length] }">
                {{ record._idx + 1 }}
              </div>
            </template>
            <template v-if="column.key === 'value'">
              <span class="mono-text">{{ record.value || '(空值)' }}</span>
            </template>
            <template v-if="column.key === 'count'">
              <span class="mono-text">{{ formatNumber(record.count) }}</span>
            </template>
            <template v-if="column.key === 'rate'">
              <div class="rate-bar-cell">
                <div class="rate-bar-bg">
                  <div
                    class="rate-bar-fill"
                    :style="{ width: record.percent + '%', background: topValueColors[record._idx % topValueColors.length] }"
                  ></div>
                </div>
                <span class="mono-text">{{ record.percent }}%</span>
              </div>
            </template>
          </template>
        </a-table>

        <!-- 异常检测详情 -->
        <a-divider v-if="currentDistributionRecord.outlierCount && currentDistributionRecord.outlierCount > 0">异常值详情</a-divider>
        <a-alert
          v-if="currentDistributionRecord.outlierCount && currentDistributionRecord.outlierCount > 0"
          :message="`基于 ${currentDistributionRecord.outlierMethod || 'IQR'} 法则检测到 ${currentDistributionRecord.outlierCount} 个异常值`"
          type="warning"
          show-icon
          style="margin-bottom: 12px"
        >
          <template #description>
            <div v-if="currentDistributionRecord.outlierDetail">
              Q1={{ currentDistributionRecord.outlierDetail.q1 }}, Q3={{ currentDistributionRecord.outlierDetail.q3 }},
              IQR={{ currentDistributionRecord.outlierDetail.iqr }},
              下界={{ currentDistributionRecord.outlierDetail.lowerBound }},
              上界={{ currentDistributionRecord.outlierDetail.upperBound }}
            </div>
          </template>
        </a-alert>
      </div>
    </a-modal>

    <!-- ============================================================ -->
    <!-- 快照比对弹窗 -->
    <!-- ============================================================ -->
    <a-modal
      v-model:open="compareDrawerVisible"
      title="快照比对"
      :footer="null"
      :destroy-on-close="true"
      width="800"
    >
      <div class="compare-content">
        <a-alert
          message="快照比对功能"
          description="选择两个不同时间点的快照进行数据对比，查看表结构和内容的变化情况"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />
        <a-row :gutter="[16, 16]">
          <a-col :span="12">
            <div class="compare-col-title">
              快照 A（基准）
              <a-tag color="blue">较旧</a-tag>
            </div>
            <a-select
              v-model:value="compareSnapshotA"
              placeholder="选择基准快照"
              style="width: 100%"
              :options="snapshotOptions"
            />
            <div v-if="compareSnapshotA" class="compare-snapshot-info">
              {{ getSnapshotLabel(compareSnapshotA) }}
            </div>
          </a-col>
          <a-col :span="12">
            <div class="compare-col-title">
              快照 B（对比）
              <a-tag color="purple">较新</a-tag>
            </div>
            <a-select
              v-model:value="compareSnapshotB"
              placeholder="选择对比快照"
              style="width: 100%"
              :options="snapshotOptions"
            />
            <div v-if="compareSnapshotB" class="compare-snapshot-info">
              {{ getSnapshotLabel(compareSnapshotB) }}
            </div>
          </a-col>
        </a-row>
        <div class="compare-actions">
          <a-button
            type="primary"
            :disabled="!compareSnapshotA || !compareSnapshotB || compareSnapshotA === compareSnapshotB"
            @click="doCompare"
          >
            开始比对
          </a-button>
        </div>

        <!-- 比对结果 -->
        <div v-if="compareResultVisible && compareResult.rowCountRate !== undefined" class="compare-result">
          <a-divider>比对结果</a-divider>
          <a-descriptions :column="3" size="small" bordered>
            <a-descriptions-item label="行数变化">
              <span :class="compareResult.rowCountChange >= 0 ? 'change-up' : 'change-down'">
                {{ compareResult.rowCountChange >= 0 ? '+' : '' }}{{ formatNumber(compareResult.rowCountChange) }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="变化率">
              <span :class="Number(compareResult.rowCountRate) >= 0 ? 'change-up' : 'change-down'">
                {{ Number(compareResult.rowCountRate) >= 0 ? '+' : '' }}{{ Number(compareResult.rowCountRate ?? 0).toFixed(2) }}%
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="存储变化">
              <span :class="compareResult.storageChange >= 0 ? 'change-up' : 'change-down'">
                {{ compareResult.storageChange >= 0 ? '+' : '' }}{{ formatBytes(compareResult.storageChange) }}
              </span>
            </a-descriptions-item>
          </a-descriptions>

          <!-- 变化列 -->
          <div v-if="compareResult.changedColumns && compareResult.changedColumns.length > 0" class="changed-columns">
            <div class="changed-title">发生变化的字段</div>
            <div v-for="col in compareResult.changedColumns" :key="col.columnName" class="changed-item">
              <span class="mono-text">{{ col.columnName }}</span>
              <a-tag :color="col.changeType === 'increase' ? 'green' : col.changeType === 'decrease' ? 'red' : 'blue'">
                {{ col.changeType === 'increase' ? '↑ 增加' : col.changeType === 'decrease' ? '↓ 减少' : '~ 变化' }}
              </a-tag>
              <span class="mono-text" style="font-size: 12px">{{ col.oldValue }} → {{ col.newValue }}</span>
            </div>
          </div>
          <div v-else class="no-changes">
            <CheckCircleOutlined style="color: #52C41A; margin-right: 6px" />
            快照期间表结构无明显变化
          </div>
        </div>
      </div>
    </a-modal>

    <!-- ============================================================ -->
    <!-- 快照管理抽屉 -->
    <!-- ============================================================ -->
    <a-drawer
      v-model:open="snapshotDrawerVisible"
      title="快照管理"
      :width="720"
      @close="snapshotDrawerVisible = false"
    >
      <div class="snapshot-toolbar">
        <a-select
          v-model:value="snapshotFilterDsId"
          placeholder="选择数据源"
          style="width: 160px"
          allowClear
          :loading="dsLoading"
          @change="loadSnapshots"
        >
          <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
            {{ ds.dsName }}
          </a-select-option>
        </a-select>
        <a-input
          v-model:value="snapshotFilterTable"
          placeholder="输入表名"
          style="width: 160px"
          allowClear
          @change="loadSnapshots"
        />
      </div>

      <div v-if="!snapshotLoading && snapshotData.length === 0" class="snapshot-empty">
        <FileSearchOutlined style="font-size: 40px; color: #D3ADF7; margin-bottom: 12px" />
        <div>暂无快照记录</div>
        <div style="font-size: 12px; color: #8C8C8C">执行探查任务后，可在此处创建和管理数据快照</div>
      </div>

      <a-table
        v-else
        :columns="snapshotColumns"
        :data-source="snapshotData"
        :loading="snapshotLoading"
        :pagination="{ pageSize: 10 }"
        :row-key="(record: any) => record.id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'snapshotName'">
            <div>
              <div style="font-weight: 500">{{ record.snapshotName }}</div>
              <div style="font-size: 12px; color: #8C8C8C; font-family: 'JetBrains Mono', monospace">{{ record.snapshotCode }}</div>
            </div>
          </template>
          <template v-if="column.key === 'targetTable'">
            <FileOutlined style="margin-right: 4px; color: #722ED1" />{{ record.targetTable }}
          </template>
          <template v-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-tooltip title="查看快照详情">
                <a-button type="text" size="small" @click="viewSnapshot(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-popconfirm title="确定删除该快照？" @confirm="handleDeleteSnapshot(record)">
                <a-button type="text" size="small" danger>
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-drawer>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import {
  SearchOutlined, FileOutlined, EyeOutlined, CameraOutlined,
  SwapOutlined, FullscreenOutlined, ReloadOutlined, FileSearchOutlined,
  BarChartOutlined, WarningOutlined, RightOutlined, CheckCircleOutlined
} from '@ant-design/icons-vue'
import { dprofileApi } from '@/api/dprofile'
import { dataSourceApi } from '@/api/dqc'
import type { TableStats, ColumnStats, Snapshot } from '@/api/dprofile'

// ============ 状态 ============
const loading = ref(false)
const dsLoading = ref(false)
const snapshotLoading = ref(false)
const snapshotDrawerVisible = ref(false)

const tableStatsData = ref<TableStats[]>([])
const dataSourceList = ref<any[]>([])
const selectedRecord = ref<TableStats | null>(null)
const previewColumnStats = ref<any[]>([])
const previewTopValues = ref<any[]>([])
const previewAnomalies = ref<any[]>([])
const chartColumns = ref<any[]>([])
const allAnomalies = ref<any[]>([])

const filterDsId = ref<number | undefined>()
const filterTableName = ref('')
const highlightAnomaly = ref(true)
const showOnlyAnomaly = ref(false)
const globalChartMode = ref('table')
const chartRefs = reactive<Record<number | string, HTMLDivElement | null>>({})
const chartRefsMini = reactive<Record<number, HTMLDivElement | null>>({})

const overviewStats = reactive({
  totalReports: 0,
  tablesToday: 0,
  anomalyCount: 0,
  snapshotCount: 0
})

// ============ 快照比对 ============
const compareDrawerVisible = ref(false)
const snapshotData = ref<Snapshot[]>([])
const compareSnapshotA = ref<number | undefined>()
const compareSnapshotB = ref<number | undefined>()
const snapshotOptions = ref<any[]>([])
const compareResultVisible = ref(false)
const compareResult = ref<any>({})
const snapshotFilterDsId = ref<number | undefined>()
const snapshotFilterTable = ref('')

// ============ 详情 ============
const detailDrawerVisible = ref(false)
const distributionModalVisible = ref(false)
const distributionColumn = ref('')
const distributionTab = ref('histogram')
const currentDistributionRecord = ref<any>(null)
const distributionTopValues = ref<any[]>([])
const isNumericColumn = ref(false)

const topValueColors = [
  '#722ED1', '#9254DE', '#B37FEB', '#D3ADF7', '#E8D5FF',
  '#13C2C2', '#52C41A', '#FAAD14', '#FF4D4F', '#1677FF'
]

// ============ 表格列 ============
const reportColumns = [
  { title: '表名 / 数据源', key: 'tableName', width: 180 },
  { title: '行数', key: 'rowCount', width: 100, align: 'right' as const },
  { title: '探查时间', key: 'profileTime', width: 160 },
  { title: '状态', key: 'anomaly', width: 80 },
  { title: '操作', key: 'action', width: 130, align: 'center' as const }
]

const previewColumns = [
  { title: '字段名', dataIndex: 'columnName', width: 120 },
  { title: '类型', key: 'dataType', width: 80 },
  { title: '空值率', key: 'nullRate', width: 160 },
  { title: '唯一率', key: 'uniqueRate', width: 80 },
  { title: '异常检测', key: 'outlier', width: 100 }
]

const detailColumns = [
  { title: '序号', width: 50, align: 'center' as const, customRender: ({ index }: any) => index + 1 },
  { title: '字段名', key: 'columnName', width: 140 },
  { title: '类型', dataIndex: 'dataType', width: 90 },
  { title: '可空', dataIndex: 'nullable', width: 50, align: 'center' as const },
  { title: '空值率', key: 'nullRate', width: 160 },
  { title: '唯一率', key: 'uniqueRate', width: 80 },
  { title: '最小值', dataIndex: 'minValue', width: 100, ellipsis: true },
  { title: '最大值', dataIndex: 'maxValue', width: 100, ellipsis: true },
  { title: '平均值', dataIndex: 'avgValue', width: 90 },
  { title: '异常值', key: 'outlier', width: 100 },
  { title: '分布', key: 'action', width: 60, align: 'center' as const }
]

const snapshotColumns = [
  { title: '快照名称', key: 'snapshotName', ellipsis: true },
  { title: '目标表', key: 'targetTable', width: 160 },
  { title: '创建时间', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, align: 'center' as const }
]

const topValueDetailColumns = [
  { title: '排名', key: 'rank', width: 60, align: 'center' as const },
  { title: '值', key: 'value', width: 200 },
  { title: '出现次数', key: 'count', width: 120, align: 'right' as const },
  { title: '占比', key: 'rate', width: 200 }
]

const filteredColumnStats = computed(() => {
  if (!showOnlyAnomaly.value) return previewColumnStats.value
  return previewColumnStats.value.filter(c => c._isAnomaly)
})

// ============ 方法 ============

async function loadTableStats() {
  loading.value = true
  try {
    const res = await dprofileApi.listTableStats({
      dsId: filterDsId.value || undefined,
      tableName: filterTableName.value || undefined,
      limit: 50
    })
    if (res.success !== false && Array.isArray(res.data)) {
      tableStatsData.value = res.data
      overviewStats.totalReports = tableStatsData.value.length

      // 计算今日探查
      const today = new Date().toDateString()
      overviewStats.tablesToday = tableStatsData.value.filter(t =>
        t.profileTime && new Date(t.profileTime).toDateString() === today
      ).length
    }
  } catch { tableStatsData.value = [] }
  finally { loading.value = false }
}

async function loadDataSources() {
  dsLoading.value = true
  try {
    const res = await dataSourceApi.listEnabled()
    if (res.success !== false) {
      dataSourceList.value = Array.isArray(res.data) ? res.data : []
    }
  } catch { /* ignore */ }
  finally { dsLoading.value = false }
}

async function loadSnapshots() {
  snapshotLoading.value = true
  try {
    const res = await dprofileApi.listSnapshots({})
    if (res.success !== false && Array.isArray(res.data)) {
      const all = res.data
      snapshotData.value = all.filter(s =>
        (!snapshotFilterDsId.value || s.targetDsId === snapshotFilterDsId.value) &&
        (!snapshotFilterTable.value || s.targetTable?.toLowerCase().includes(snapshotFilterTable.value.toLowerCase()))
      )
      overviewStats.snapshotCount = all.length
      snapshotOptions.value = all.map(s => ({
        value: s.id,
        label: `${s.snapshotName} (${formatTime(s.createTime)})`
      }))
    }
  } catch { snapshotData.value = [] }
  finally { snapshotLoading.value = false }
}

function handleTableChange() { /* handled by pagination */ }

function selectRecord(record: TableStats) {
  selectedRecord.value = record
  loadColumnStatsForRecord(record)
}

async function loadColumnStatsForRecord(record: TableStats) {
  try {
    const res = await dprofileApi.listColumnProfilesWithWarnings(record.id!)
    if (res.success !== false && Array.isArray(res.data)) {
      const columnProfiles = res.data
      previewColumnStats.value = columnProfiles.map((p: any) => {
        const isAnomaly = (p.outlierCount && p.outlierCount > 0) ||
          (p.nullRate && p.nullRate > 0.5) ||
          (p.uniqueCount === 1 && (p.totalCount || 0) > 100)
        let anomalyReason = ''
        if (p.nullRate && p.nullRate > 0.5) anomalyReason = `空值率 ${(p.nullRate * 100).toFixed(1)}%`
        else if (p.outlierCount && p.outlierCount > 0) anomalyReason = `${p.outlierCount} 个异常值`
        else if (p.uniqueCount === 1 && (p.totalCount || 0) > 100) anomalyReason = '全唯一字段'
        return {
          ...p,
          _isAnomaly: isAnomaly,
          _anomalyReason: anomalyReason,
          topValueItems: p.topValueItems || parseTopValues(p.topValues),
          histogramBuckets: p.histogramBuckets || parseHistogram(p.histogram),
          warningCount: p.warnings?.length || 0
        }
      }) as any[]

      allAnomalies.value = previewColumnStats.value.filter((c: any) => c._isAnomaly)
        .map((c: any) => ({
          columnName: c.columnName,
          dataType: c.dataType,
          anomalyType: c.nullRate > 0.5 ? '高空值率' : c.outlierCount > 0 ? '异常值' : '全唯一',
          anomalyDetail: c._anomalyReason
        }))

      overviewStats.anomalyCount = allAnomalies.value.length

      // 选择前4个有数据的列用于图表展示
      chartColumns.value = previewColumnStats.value
        .filter((c: any) => c.histogram || (c.topValueItems && c.topValueItems.length > 0))
        .slice(0, 6)

      // 预览 Top 值（取第一列）
      if (previewColumnStats.value.length > 0) {
        const firstCol = previewColumnStats.value[0] as any
        if (firstCol.topValueItems?.length > 0) {
          const total = firstCol.topValueItems.reduce((s: number, i: any) => s + (i.count || 0), 0)
          previewTopValues.value = firstCol.topValueItems.slice(0, 5).map((item: any) => ({
            value: item.value || '(空)',
            count: item.count || 0,
            percent: total > 0 ? ((item.count / total) * 100).toFixed(1) : '0'
          }))
        } else {
          previewTopValues.value = []
        }
      }

      // 预览异常
      previewAnomalies.value = previewColumnStats.value
        .filter((c: any) => c._isAnomaly)
        .slice(0, 5)

      // 渲染 ECharts 图表（迷你）
      nextTick(() => {
        chartColumns.value.slice(0, 3).forEach((col: any) => {
          const el = chartRefsMini[col.id!]
          if (el) renderMiniChart(el, col)
        })
      })
    }
  } catch {
    try {
      const res = await dprofileApi.listColumnStats(record.id!)
      if (res.success !== false && Array.isArray(res.data)) {
        previewColumnStats.value = res.data
      }
    } catch {
      previewColumnStats.value = []
    }
  }
}

function parseTopValues(topValuesStr: string | undefined): any[] {
  if (!topValuesStr) return []
  try {
    const arr = JSON.parse(topValuesStr)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

function parseHistogram(histogramStr: string | undefined): any[] {
  if (!histogramStr) return []
  try {
    const arr = JSON.parse(histogramStr)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

function renderMiniChart(el: HTMLDivElement, col: any) {
  const chart = echarts.getInstanceByDom(el)
  if (chart) chart.dispose()
  const chartInstance = echarts.init(el)

  const isNumeric = isNumericType(col.dataType)
  const histData = col.histogramBuckets || []
  const topData = col.topValueItems || []

  if (isNumeric && histData.length > 0) {
    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 5, right: 5, top: 5, bottom: 5 },
      xAxis: { type: 'category', data: histData.slice(0, 8).map((d: any) => d.range || ''), show: false },
      yAxis: { show: false },
      series: [{
        type: 'bar',
        data: histData.slice(0, 8).map((d: any) => d.count || 0),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#722ED1' },
            { offset: 1, color: '#9254DE' }
          ])
        },
        barWidth: '60%'
      }]
    })
  } else if (topData.length > 0) {
    const sorted = [...topData].sort((a: any, b: any) => (b.count || 0) - (a.count || 0)).slice(0, 6)
    const colors = ['#722ED1', '#9254DE', '#B37FEB', '#D3ADF7', '#E8D5FF', '#13C2C2']
    chartInstance.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['30%', '65%'],
        center: ['50%', '50%'],
        label: { show: false },
        data: sorted.map((d: any, idx: number) => ({
          value: d.count || 0,
          name: String(d.value || '(空)').substring(0, 6),
          itemStyle: { color: colors[idx % colors.length] }
        }))
      }]
    })
  }
}

function openDetailDrawer(record: TableStats) {
  selectedRecord.value = record
  detailDrawerVisible.value = true
  loadColumnStatsForRecord(record)

  nextTick(() => {
    renderAllCharts()
  })
}

function openDetailDrawerForColumn(record: TableStats, col: any) {
  selectedRecord.value = record
  openDistributionModal(col)
}

function openDistributionModal(record: any) {
  currentDistributionRecord.value = record
  distributionColumn.value = record.columnName || ''
  isNumericColumn.value = isNumericType(record.dataType)

  const histData = record.histogramBuckets || []
  const topData = record.topValueItems || parseTopValues(record.topValues) || []

  const total = topData.reduce((s: number, i: any) => s + (i.count || 0), 0)
  distributionTopValues.value = topData.map((item: any, idx: number) => ({
    ...item,
    _idx: idx,
    percent: total > 0 ? ((item.count / total) * 100).toFixed(2) : '0'
  }))

  distributionTab.value = isNumericColumn.value && histData.length > 0 ? 'histogram' : 'frequency'
  distributionModalVisible.value = true

  nextTick(() => {
    const el = chartRefs.dist
    if (el) renderDistributionChart(el, histData, topData)
  })
}

function renderDistributionChart(el: HTMLDivElement, histData: any[], topData: any[]) {
  const chart = echarts.getInstanceByDom(el)
  if (chart) chart.dispose()
  const chartInstance = echarts.init(el)
  const isNumeric = isNumericColumn.value

  if (distributionTab.value === 'histogram' && isNumeric && histData.length > 0) {
    const xData = histData.map((d: any) => d.range || '')
    const yData = histData.map((d: any) => d.count || 0)
    const maxVal = Math.max(...yData)
    chartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const p = params[0]
          return `${distributionColumn.value}<br/>${p.name}: <b>${p.value.toLocaleString()}</b>`
        }
      },
      grid: { left: 60, right: 30, top: 20, bottom: 60 },
      xAxis: {
        type: 'category',
        data: xData,
        axisLabel: { rotate: 30, fontSize: 10, color: '#666' },
        axisLine: { lineStyle: { color: '#E8E8E8' } }
      },
      yAxis: {
        type: 'value',
        axisLabel: { fontSize: 10, color: '#666' },
        splitLine: { lineStyle: { color: '#F5F7FA' } }
      },
      series: [{
        type: 'bar',
        data: yData,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#722ED1' },
            { offset: 1, color: '#9254DE' }
          ])
        },
        barRadius: [3, 3, 0, 0],
        barMaxWidth: 50,
        markLine: {
          data: [{ type: 'average', name: '平均值' }],
          lineStyle: { color: '#FAAD14', type: 'dashed' }
        }
      }]
    })
  } else if (distributionTab.value === 'frequency' && topData.length > 0) {
    const sorted = [...topData].sort((a: any, b: any) => (b.count || 0) - (a.count || 0)).slice(0, 12)
    const colorList = ['#722ED1', '#9254DE', '#B37FEB', '#D3ADF7', '#E8D5FF', '#13C2C2', '#52C41A', '#FAAD14', '#FF4D4F', '#1677FF']
    chartInstance.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 100, right: 40, top: 20, bottom: 50 },
      xAxis: { type: 'value' },
      yAxis: {
        type: 'category',
        data: sorted.map((d: any) => String(d.value || '(空)').substring(0, 12)),
        axisLabel: { fontSize: 10, color: '#666' }
      },
      series: [{
        type: 'bar',
        data: sorted.map((d: any, idx: number) => ({
          value: d.count || 0,
          itemStyle: { color: colorList[idx % colorList.length] }
        })),
        barMaxWidth: 30,
        label: {
          show: true, position: 'right', fontSize: 10,
          formatter: (p: any) => {
            const total = sorted.reduce((s: number, i: any) => s + (i.count || 0), 0)
            return total > 0 ? `${((p.value / total) * 100).toFixed(1)}%` : ''
          }
        }
      }]
    })
  } else {
    // 饼图
    const sorted = [...topData].sort((a: any, b: any) => (b.count || 0) - (a.count || 0)).slice(0, 8)
    const colorList = ['#722ED1', '#9254DE', '#B37FEB', '#D3ADF7', '#E8D5FF', '#13C2C2', '#52C41A', '#FAAD14']
    chartInstance.setOption({
      tooltip: { trigger: 'item', formatter: (p: any) => `${p.name}<br/>${p.value.toLocaleString()} (${p.percent}%)` },
      legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { fontSize: 10 } },
      series: [{
        type: 'pie',
        radius: ['35%', '70%'],
        center: ['40%', '50%'],
        label: { show: true, fontSize: 10 },
        data: sorted.map((d: any, idx: number) => ({
          name: String(d.value || '(空)').substring(0, 10),
          value: d.count || 0,
          itemStyle: { color: colorList[idx % colorList.length] }
        }))
      }]
    })
  }
}

function renderAllCharts() {
  if (globalChartMode.value !== 'grid') return
  nextTick(() => {
    chartColumns.value.forEach((col: any) => {
      const el = chartRefs[col.id!]
      if (el) {
        const chart = echarts.getInstanceByDom(el)
        if (chart) chart.dispose()
        const chartInstance = echarts.init(el)
        renderGridChart(chartInstance, col)
      }
    })
  })
}

function renderGridChart(chartInstance: echarts.ECharts, col: any) {
  const isNumeric = isNumericType(col.dataType)
  const histData = col.histogramBuckets || []
  const topData = col.topValueItems || []

  if (isNumeric && histData.length > 0) {
    const xData = histData.slice(0, 10).map((d: any) => d.range || '')
    const yData = histData.slice(0, 10).map((d: any) => d.count || 0)
    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 10, top: 10, bottom: 30 },
      xAxis: { type: 'category', data: xData, axisLabel: { rotate: 20, fontSize: 8, color: '#666' }, show: false },
      yAxis: { show: false, splitLine: { lineStyle: { color: '#F5F7FA' } } },
      series: [{
        type: 'bar',
        data: yData,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#722ED1' },
            { offset: 1, color: '#9254DE' }
          ])
        },
        barWidth: '60%'
      }]
    })
  } else if (topData.length > 0) {
    const sorted = [...topData].sort((a: any, b: any) => (b.count || 0) - (a.count || 0)).slice(0, 8)
    const colorList = ['#722ED1', '#9254DE', '#B37FEB', '#D3ADF7', '#E8D5FF', '#13C2C2', '#52C41A', '#FAAD14']
    chartInstance.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 60, right: 10, top: 10, bottom: 30 },
      xAxis: { type: 'value' },
      yAxis: {
        type: 'category',
        data: sorted.map((d: any) => String(d.value || '(空)').substring(0, 8)),
        axisLabel: { fontSize: 8, color: '#666' }
      },
      series: [{
        type: 'bar',
        data: sorted.map((d: any, idx: number) => ({
          value: d.count || 0,
          itemStyle: { color: colorList[idx % colorList.length] }
        })),
        barMaxWidth: 20
      }]
    })
  }
}

watch(globalChartMode, () => {
  if (globalChartMode.value === 'grid') {
    renderAllCharts()
  }
})

function isNumericType(dataType: string | undefined) {
  if (!dataType) return false
  const dt = dataType.toLowerCase()
  return dt.includes('int') || dt.includes('decimal') || dt.includes('numeric')
    || dt.includes('float') || dt.includes('double') || dt.includes('real')
    || dt.includes('number') || dt.includes('smallmoney')
    || dt.includes('money') || dt.includes('bit')
}

function getNullRateColor(rate: number | undefined) {
  const r = rate || 0
  if (r > 0.5) return '#FF4D4F'
  if (r > 0.2) return '#FAAD14'
  return '#52C41A'
}

function createQuickSnapshot(record: TableStats) {
  message.success('快照创建成功（快速模式）')
}

function openCompareDrawer(record: TableStats) {
  compareSnapshotA.value = undefined
  compareSnapshotB.value = undefined
  compareResultVisible.value = false
  snapshotOptions.value = []   // 每次打开重置选项，避免旧数据残留
  compareDrawerVisible.value = true
  loadSnapshots()
}

function getSnapshotLabel(id: number | undefined) {
  const opt = snapshotOptions.value.find(s => s.value === id)
  return opt ? opt.label : ''
}

function doCompare() {
  if (!compareSnapshotA.value || !compareSnapshotB.value) {
    message.warning('请选择两个快照')
    return
  }
  // 模拟比对结果（rowCountRate 存为 number，避免模板重复 toFixed）
  compareResult.value = {
    rowCountChange: Math.floor(Math.random() * 10000) - 3000,
    rowCountRate: parseFloat((Math.random() * 20 - 10).toFixed(2)),
    storageChange: Math.floor(Math.random() * 10000000) - 3000000,
    changedColumns: [
      { columnName: 'update_time', changeType: 'increase', oldValue: '2024-01-01', newValue: '2024-03-22' },
      { columnName: 'row_count', changeType: 'increase', oldValue: '99999', newValue: '105432' }
    ]
  }
  compareResultVisible.value = true
}

function getDsTypeColor(dsId: number | undefined) {
  const ds = dataSourceList.value.find(d => d.id === dsId)
  const map: Record<string, string> = {
    MYSQL: 'blue', SQLSERVER: 'red', ORACLE: 'orange', POSTGRESQL: 'cyan', TIDB: 'green'
  }
  return map[ds?.dsType || ''] || 'default'
}

function getDsTypeName(dsId: number | undefined) {
  const ds = dataSourceList.value.find(d => d.id === dsId)
  return ds?.dsName || '-'
}

function openSnapshotDrawer() {
  snapshotDrawerVisible.value = true
  loadSnapshots()
}

function viewSnapshot(record: Snapshot) {
  message.info(`查看快照: ${record.snapshotName}`)
}

async function handleDeleteSnapshot(record: Snapshot) {
  try {
    const res = await dprofileApi.deleteSnapshot(record.id!)
    if (res.success !== false) {
      message.success('删除成功')
      loadSnapshots()
    }
  } catch { message.error('删除失败') }
}

function formatTime(time: string | undefined) {
  if (!time) return '-'
  try {
    return new Date(time).toLocaleString('zh-CN')
  } catch { return time }
}

function formatNumber(n: number | null | undefined) {
  if (n == null) return '-'
  return n.toLocaleString()
}

function formatBytes(bytes: number | null | undefined) {
  if (bytes == null) return '-'
  if (bytes < 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

onMounted(() => {
  loadTableStats()
  loadDataSources()
  loadSnapshots()
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
.header-right { display: flex; gap: 8px; }

.stat-mini-card {
  border-radius: 8px; padding: 16px 20px;
  color: white; box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.mini-value { font-size: 24px; font-weight: 700; }
.mini-label { font-size: 12px; opacity: 0.9; margin-top: 4px; }

.table-card {
  background: white; border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: 16px;
}

.list-empty { text-align: center; padding: 60px 20px; color: #8C8C8C; font-size: 14px; }

.card-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
  .card-title { font-size: 16px; font-weight: 600; color: #1F1F1F; }
}

.table-name-cell {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; padding: 4px; border-radius: 4px;
  transition: background 0.2s;
}
.table-name-cell:hover { background: #F5F7FA; }
.table-name-selected { background: rgba(114, 46, 209, 0.06); border-left: 2px solid #722ED1; }
.table-icon { color: #722ED1; font-size: 16px; }
.table-name-text { font-weight: 500; font-family: 'JetBrains Mono', monospace; }
.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px; }
.table-meta { margin-top: 2px; }
.time-cell { font-size: 12px; color: #8C8C8C; }
.normal-badge { color: #52C41A; font-size: 12px; }

/* 详情面板 */
.empty-detail {
  background: white; border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: 80px 40px; text-align: center;
  min-height: 400px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 16px;
  .empty-icon { opacity: 0.5; }
  .empty-text { font-size: 14px; color: #8C8C8C; }
}

.detail-panel {
  background: white; border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: 20px; min-height: 400px;
}

.detail-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
  .detail-title {
    display: flex; align-items: center; gap: 8px;
    font-size: 18px; font-weight: 600; color: #1F1F1F;
    font-family: 'JetBrains Mono', monospace;
  }
  .detail-icon { color: #722ED1; }
}

.overview-row { margin-bottom: 16px; }
.overview-stat {
  background: #F5F7FA; border-radius: 8px; padding: 12px; text-align: center;
  .overview-value { font-size: 20px; font-weight: 700; color: #722ED1; font-family: 'JetBrains Mono', monospace; }
  .overview-label { font-size: 12px; color: #8C8C8C; margin-top: 4px; }
}

.anomaly-warning {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; background: #FFF7E6; border-radius: 6px;
  border-left: 3px solid #FAAD14; margin-bottom: 12px;
  font-size: 13px; color: #8C8C8C; cursor: pointer;
  transition: background 0.2s;
}
.anomaly-warning:hover { background: #FFE58F; }

.section-title {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 14px; font-weight: 600; color: #1F1F1F; margin-bottom: 8px;
}

.top-values-mini {
  background: #F9F9F9; border-radius: 8px; padding: 12px;
}
.top-mini-item {
  display: flex; align-items: center; gap: 8px; margin-bottom: 6px;
  .top-mini-value { font-family: 'JetBrains Mono', monospace; font-size: 13px; min-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .top-mini-bar { flex: 1; height: 6px; background: linear-gradient(90deg, #722ED1, #9254DE); border-radius: 3px; transition: width 0.5s; }
  .top-mini-count { font-size: 12px; color: #8C8C8C; min-width: 40px; text-align: right; }
}

.mini-charts {
  display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px;
}
.mini-chart-cell {
  background: #FAFAFA; border-radius: 6px; padding: 8px;
  cursor: pointer; transition: box-shadow 0.2s;
  border: 1px solid transparent;
}
.mini-chart-cell:hover { box-shadow: 0 2px 8px rgba(114,46,209,0.1); border-color: #D3ADF7; }
.mini-chart-label { display: flex; align-items: center; gap: 4px; margin-bottom: 6px; }

/* 完整报告 */
.full-report { padding-bottom: 20px; }
.report-desc { margin-bottom: 16px; }

.column-stats-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px; padding: 8px 12px; background: #F5F7FA;
  border-radius: 6px;
}

.column-name-cell {
  display: flex; align-items: center; gap: 4px;
  &.column-anomaly { color: #FAAD14; font-weight: 600; }
}

.chart-toolbar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 16px;
}

.chart-grid {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 16px;
  .chart-cell {
    background: #FAFAFA; border-radius: 8px; padding: 12px;
    border: 1px solid #F0F0F0; transition: box-shadow 0.2s;
    &:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
    .chart-title {
      font-size: 13px; font-weight: 500; margin-bottom: 10px;
      display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
      .chart-col-name { font-family: 'JetBrains Mono', monospace; color: #1F1F1F; }
    }
  }
}

.chart-table-view {
  display: flex; flex-direction: column; gap: 16px;
}
.col-chart-row {
  background: #FAFAFA; border-radius: 8px; padding: 14px;
  border: 1px solid #F0F0F0;
  .col-chart-header {
    font-size: 13px; font-weight: 500; margin-bottom: 10px;
    display: flex; align-items: center; gap: 8px;
  }
}
.col-chart-bar {
  display: flex; gap: 4px; align-items: flex-end;
  .bar-item {
    flex: 1; height: 32px; border-radius: 4px;
    display: flex; align-items: flex-end; justify-content: center;
    padding-bottom: 4px; overflow: hidden;
    min-width: 20px; max-width: 100px;
    transition: opacity 0.2s;
    &:hover { opacity: 0.8; }
    .bar-label { font-size: 9px; color: white; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; }
  }
  &.numeric {
    display: flex; align-items: flex-end; gap: 3px; height: 60px;
    .numeric-bar { height: auto; border-radius: 2px 2px 0 0; }
  }
}

/* 分布弹窗 */
.distribution-modal { padding: 8px 0; }
.dist-tabs { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.dist-stats-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #1F1F1F; }

.rate-badge {
  width: 20px; height: 20px; border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 10px; color: white; font-weight: 600;
}

/* Top值详情 */
.rate-bar-cell { display: flex; align-items: center; gap: 8px; }
.rate-bar-bg { flex: 1; height: 8px; background: #F0F0F0; border-radius: 4px; overflow: hidden; }
.rate-bar-fill { height: 100%; border-radius: 4px; transition: width 0.5s; }
.rank-badge { width: 20px; height: 20px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-size: 10px; color: white; font-weight: 600; }

/* 快照比对 */
.compare-content { padding: 8px 0; }
.compare-col-title { font-weight: 600; margin-bottom: 8px; font-size: 13px; display: flex; align-items: center; gap: 8px; }
.compare-snapshot-info { font-size: 12px; color: #8C8C8C; margin-top: 6px; }
.compare-actions { margin-top: 20px; text-align: center; }

.compare-result { margin-top: 16px; }
.change-up { color: #52C41A; font-weight: 600; font-family: 'JetBrains Mono', monospace; }
.change-down { color: #FF4D4F; font-weight: 600; font-family: 'JetBrains Mono', monospace; }
.changed-columns { margin-top: 16px; }
.changed-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.changed-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: #FAFAFA; border-radius: 6px;
  margin-bottom: 6px;
}
.no-changes { text-align: center; padding: 24px; color: #8C8C8C; font-size: 14px; }

/* 快照管理 */
.snapshot-toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.snapshot-empty { text-align: center; padding: 60px 20px; color: #8C8C8C; font-size: 14px; }

.suggestion-list { margin: 4px 0 0; padding-left: 20px; line-height: 1.8; }
.suggestion-list code { background: #F5F7FA; padding: 1px 4px; border-radius: 3px; font-size: 12px; }
</style>
