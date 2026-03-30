<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#scanGrad)"/>
            <circle cx="12" cy="12" r="4" stroke="white" stroke-width="1.5"/>
            <path d="M12 4V2" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M12 22V20" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M4 12H2" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M22 12H20" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <defs>
              <linearGradient id="scanGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#B37FEB"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">敏感字段识别</h1>
          <p class="page-subtitle">基于规则自动识别数据源中的敏感字段，支持配置扫描策略与敏感等级匹配</p>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="table-card">
      <a-tabs v-model:activeKey="activeTab">
        <!-- ==================== 扫描向导 Tab ==================== -->
        <a-tab-pane key="wizard" tab="敏感字段识别">
          <div class="wizard-container">
            <!-- 步骤指示器 -->
            <div class="step-indicator">
              <a-steps :current="currentStep - 1" size="small" :status="stepStatus">
                <a-step title="选择数据源" />
                <a-step title="配置扫描参数" />
                <a-step title="执行扫描" />
                <a-step title="查看结果" />
              </a-steps>
            </div>

            <!-- Step 1: 选择数据源 -->
            <div v-if="currentStep === 1" class="step-content step-content-padded">
              <a-card class="step-card step-card-spacious" :bordered="false">
                <template #title>
                  <span class="step-title"><ScanOutlined /> 选择数据源</span>
                </template>
                <a-form class="step1-form" layout="vertical">
                  <a-form-item label="数据源" required>
                    <a-select
                      v-model:value="scanForm.dsId"
                      placeholder="请选择要扫描的数据源"
                      :loading="dsLoading"
                      size="large"
                      @change="onDsChange"
                    >
                      <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                        <div class="ds-option">
                          <span class="ds-name">{{ ds.dsName }}</span>
                          <a-tag size="small">{{ ds.dsType }}</a-tag>
                        </div>
                      </a-select-option>
                    </a-select>
                  </a-form-item>

                  <a-form-item v-if="selectedDsType === 'POSTGRESQL'" label="Schema" :required="true">
                    <a-select
                      v-model:value="scanForm.schema"
                      placeholder="请选择 Schema"
                      size="large"
                      :disabled="!scanForm.dsId"
                      :loading="schemaLoading"
                      show-search
                      :filter-option="filterSchemaOption"
                      @change="onSchemaChange"
                    >
                      <a-select-option v-for="s in schemaList" :key="s" :value="s">{{ s }}</a-select-option>
                    </a-select>
                    <div class="form-tip form-tip-block">
                      列表由数据源实时查询（排除系统 schema）。直连扫描将使用所选 Schema；基于元数据扫描时仍扫描该数据源已采集的全部元数据。
                    </div>
                  </a-form-item>

                  <!-- 扫描范围说明：表数可预期 -->
                  <div v-if="scanForm.dsId" class="scan-scope-summary">
                    <div class="summary-title">扫描范围预览</div>
                    <div class="summary-grid">
                      <div class="summary-item">
                        <span class="summary-label">元数据已采集</span>
                        <template v-if="scanForm.dsId && dsStats[scanForm.dsId]">
                          <span class="summary-value">
                            <strong>{{ dsStats[scanForm.dsId].tableCount ?? 0 }}</strong> 张表
                            <span class="summary-sep">·</span>
                            <strong>{{ dsStats[scanForm.dsId].columnCount ?? 0 }}</strong> 个字段
                          </span>
                        </template>
                        <span v-else class="summary-muted">暂无统计（请先完成元数据采集）</span>
                      </div>
                      <div v-if="scanForm.scanMode === 'DIRECT'" class="summary-item summary-item-highlight">
                        <span class="summary-label">直连预计扫描</span>
                        <a-spin v-if="estimateLoading" size="small" />
                        <span v-else-if="selectedDsType === 'POSTGRESQL' && !scanForm.schema" class="summary-muted">
                          请选择 Schema 后显示表数量
                        </span>
                        <span v-else-if="directTableEstimate != null" class="summary-value">
                          约 <strong>{{ directTableEstimate }}</strong> 张表
                          <span v-if="selectedDsType === 'POSTGRESQL' && scanForm.schema" class="summary-meta">
                            （Schema：<code>{{ scanForm.schema }}</code>）
                          </span>
                        </span>
                        <span v-else class="summary-muted">无法预估表数量</span>
                      </div>
                      <div v-else class="summary-item">
                        <span class="summary-label">本次元数据扫描</span>
                        <span class="summary-value summary-muted">
                          将遍历该数据源在平台中已入库的表与字段（与左侧 Schema 无关）
                        </span>
                      </div>
                    </div>
                    <a-alert
                      v-if="scanForm.scanMode === 'DIRECT' && directEstimateError && !estimateLoading"
                      type="error"
                      show-icon
                      :message="directEstimateError"
                      style="margin-top: 12px"
                    />
                  </div>

                  <a-form-item label="扫描模式">
                    <a-radio-group v-model:value="scanForm.scanMode" size="large">
                      <a-radio-button value="METADATA">
                        <DatabaseOutlined /> 基于元数据扫描
                      </a-radio-button>
                      <a-radio-button value="DIRECT">
                        <LinkOutlined /> 直连数据源扫描
                      </a-radio-button>
                    </a-radio-group>
                    <div class="form-tip form-tip-block">
                      <span v-if="scanForm.scanMode === 'METADATA'">
                        基于已扫描的元数据分析字段名称和注释进行敏感识别
                      </span>
                      <span v-else>
                        直接连接数据源执行采样分析，可发现更多敏感模式
                      </span>
                    </div>
                  </a-form-item>
                </a-form>
              </a-card>

              <div class="step-footer">
                <a-button type="primary" size="large" :disabled="!canGoStep2" @click="goStep(2)">
                  下一步 <RightOutlined />
                </a-button>
              </div>
            </div>

            <!-- Step 2: 配置扫描参数 -->
            <div v-if="currentStep === 2" class="step-content step-content-padded">
              <a-card class="step-card step-card-spacious" :bordered="false">
                <template #title>
                  <span class="step-title"><SettingOutlined /> 配置扫描参数</span>
                </template>

                <a-divider orientation="left">扫描维度</a-divider>
                <a-form layout="vertical">
                  <a-form-item>
                    <a-checkbox-group v-model:value="scanForm.scanDimensions" class="scan-dimensions">
                      <a-row :gutter="[16, 12]">
                        <a-col :span="8">
                          <a-checkbox value="COLUMN_NAME">
                            <div class="dimension-item">
                              <TagOutlined />
                              <span>字段名匹配</span>
                              <span class="dimension-desc">如 id_card、phone、email</span>
                            </div>
                          </a-checkbox>
                        </a-col>
                        <a-col :span="8">
                          <a-checkbox value="COLUMN_COMMENT">
                            <div class="dimension-item">
                              <FileTextOutlined />
                              <span>注释匹配</span>
                              <span class="dimension-desc">匹配字段注释中的敏感关键词</span>
                            </div>
                          </a-checkbox>
                        </a-col>
                        <a-col :span="8">
                          <a-checkbox value="DATA_TYPE">
                            <div class="dimension-item">
                              <DatabaseOutlined />
                              <span>数据类型匹配</span>
                              <span class="dimension-desc">根据数据类型识别敏感字段</span>
                            </div>
                          </a-checkbox>
                        </a-col>
                      </a-row>
                    </a-checkbox-group>
                  </a-form-item>
                </a-form>

                <a-divider orientation="left">高级选项</a-divider>
                <a-form layout="vertical">
                  <a-form-item label="采样数量">
                    <div class="slider-container">
                      <a-slider
                        v-model:value="scanForm.sampleSize"
                        :min="0"
                        :max="200"
                        :marks="{ 0: '0', 50: '50', 100: '100', 150: '150', 200: '200' }"
                      />
                      <span class="slider-value">{{ scanForm.sampleSize }} 条</span>
                    </div>
                    <div class="form-tip">每个字段的采样行数，用于内容级敏感检测</div>
                  </a-form-item>

                  <a-form-item>
                    <a-checkbox v-model:checked="scanForm.enableContentScan">
                      启用内容级扫描
                    </a-checkbox>
                    <div class="form-tip">对采样数据执行正则匹配，可发现注释中未标注的敏感字段（建议开启）</div>
                  </a-form-item>

                  <a-form-item>
                    <a-checkbox v-model:checked="scanForm.excludeSystemTables">
                      排除系统表
                    </a-checkbox>
                    <div class="form-tip">自动排除 sys_、pg_、mysql 等开头的系统表</div>
                  </a-form-item>

                  <a-form-item>
                    <a-checkbox v-model:checked="scanForm.incremental">
                      增量扫描
                    </a-checkbox>
                    <div class="form-tip">仅扫描上次扫描后新增或变更的表</div>
                  </a-form-item>
                </a-form>

                <!-- 扫描预览 -->
                <a-divider orientation="left">扫描预览</a-divider>
                <a-descriptions :column="2" size="small" bordered>
                  <a-descriptions-item label="数据源">{{ currentDsName }}</a-descriptions-item>
                  <a-descriptions-item label="扫描模式">{{ scanForm.scanMode === 'METADATA' ? '基于元数据' : '直连数据源' }}</a-descriptions-item>
                  <a-descriptions-item label="扫描维度">{{ scanForm.scanDimensions.join(' + ') || '未选择' }}</a-descriptions-item>
                  <a-descriptions-item label="采样数量">{{ scanForm.sampleSize }} 条/字段</a-descriptions-item>
                </a-descriptions>
              </a-card>

              <div class="step-footer">
                <a-space size="large">
                  <a-button size="large" @click="goStep(1)">
                    <LeftOutlined /> 上一步
                  </a-button>
                  <a-button
                    type="primary"
                    size="large"
                    :loading="scanning"
                    :disabled="scanForm.scanDimensions.length === 0"
                    @click="startScan"
                  >
                    <ThunderboltOutlined /> 开始扫描
                  </a-button>
                </a-space>
              </div>
            </div>

            <!-- Step 3: 执行扫描 -->
            <div v-if="currentStep === 3" class="step-content">
              <a-card class="step-card scanning-card" :bordered="false">
                <div class="scanning-status">
                  <div class="scanning-animation">
                    <LoadingOutlined class="scanning-icon" />
                  </div>
                  <div class="scanning-text">
                    <h3>{{ scanPhase === 'preparing' ? '准备扫描...' : scanPhase === 'scanning' ? `正在扫描 ${currentDsName}` : '扫描完成' }}</h3>
                    <p class="scanning-info">{{ scanProgress.tableName }}</p>
                  </div>
                </div>

                <div class="progress-section">
                  <div class="progress-stats">
                    <a-statistic
                      title="已扫描表"
                      :value="scanProgress.scannedTables"
                      :suffix="`/ ${scanProgress.totalTables || '?'}`"
                    />
                    <a-statistic
                      title="已扫描字段"
                      :value="scanProgress.scannedColumns"
                      :suffix="`/ ${scanProgress.totalColumns || '?'}`"
                    />
                    <a-tooltip
                      placement="top"
                      title="当前为同步扫描，敏感字段命中数在整次请求完成后由服务端返回，进行中无法逐条递增。"
                    >
                      <span class="progress-stat-wrap">
                        <a-statistic
                          title="发现敏感字段"
                          :value="scanProgress.foundSensitive"
                          :value-style="{ color: scanProgress.foundSensitive > 0 ? '#52C41A' : '#1890ff' }"
                        />
                      </span>
                    </a-tooltip>
                  </div>

                  <a-progress
                    :percent="scanProgress.percent"
                    :status="scanPhase === 'done' ? 'success' : 'active'"
                    :stroke-color="{
                      '0%': '#722ED1',
                      '100%': '#B37FEB'
                    }"
                    size="large"
                  />

                  <div class="progress-detail" v-if="scanPhase === 'scanning'">
                    <span>扫描速度: {{ scanProgress.speed }} 字段/秒</span>
                    <span v-if="scanProgress.remainingTime">预计剩余: {{ scanProgress.remainingTime }}</span>
                  </div>
                </div>

                <a-alert
                  v-if="scanError"
                  :message="scanError"
                  type="error"
                  show-icon
                  style="margin-top: 16px"
                />
              </a-card>
            </div>

            <!-- Step 4: 查看结果 -->
            <div v-if="currentStep === 4" class="step-content">
              <a-card class="step-card result-card" :bordered="false">
                <template #title>
                  <span class="step-title"><CheckCircleOutlined /> 扫描结果</span>
                </template>

                <!-- 无敏感字段 -->
                <a-result
                  v-if="!scanResult || scanResult.foundSensitiveCount === 0"
                  status="success"
                  title="扫描完成"
                  :sub-title="`共扫描 ${scanResult?.totalTableCount || 0} 张表，${scanResult?.totalColumnCount || 0} 个字段，未发现敏感字段`"
                />

                <!-- 有敏感字段 -->
                <template v-else>
                  <a-result
                    :status="scanResult.foundSensitiveCount > 0 ? 'success' : 'warning'"
                    :title="`扫描完成！发现 ${scanResult.foundSensitiveCount} 个敏感字段`"
                    :sub-title="`共扫描 ${scanResult.totalTableCount || 0} 张表，${scanResult.totalColumnCount || 0} 个字段，耗时 ${scanResult.elapsedMs || 0}ms`"
                  >
                    <template #extra>
                      <a-row :gutter="16" class="result-stats" v-if="scanResult.countByLevel">
                        <a-col :span="6" v-for="(count, level) in scanResult.countByLevel" :key="level">
                          <a-tag
                            :color="getLevelColor(level)"
                            style="font-size: 16px; padding: 8px 16px;"
                          >
                            {{ level }}: {{ count }}
                          </a-tag>
                        </a-col>
                      </a-row>
                    </template>
                  </a-result>

                  <!-- 结果表格 -->
                  <a-table
                    v-if="scanResult.results?.length"
                    :columns="resultColumns"
                    :data-source="scanResult.results"
                    :pagination="{ pageSize: 10 }"
                    :row-key="(r: SecColumnSensitivityVO) => r.id || `${r.dsId}-${r.tableName}-${r.columnName}`"
                    size="small"
                    :scroll="{ x: 1000 }"
                    style="margin-top: 16px"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'columnName'">
                        <div>
                          <code style="font-weight: 600">{{ record.columnName }}</code>
                          <div style="color: #888; font-size: 12px">{{ record.columnComment || '-' }}</div>
                        </div>
                      </template>
                      <template v-if="column.key === 'level'">
                        <a-tag :color="record.levelColor || getLevelColor(record.levelName)">
                          {{ record.levelName || record.sensitivityLevel || '-' }}
                        </a-tag>
                      </template>
                      <template v-if="column.key === 'confidence'">
                        <a-progress
                          :percent="Math.min(100, Math.max(0, Number(record.confidence ?? 0)))"
                          size="small"
                          :stroke-color="getConfidenceColor(Number(record.confidence))"
                          :format="() => `${Number(record.confidence ?? 0).toFixed(1)}%`"
                        />
                      </template>
                      <template v-if="column.key === 'maskType'">
                        <a-tag :color="getMaskTypeColor(record.maskType)">
                          {{ record.maskTypeLabel || record.maskType || '-' }}
                        </a-tag>
                      </template>
                    </template>
                  </a-table>
                </template>
              </a-card>

              <div class="step-footer">
                <a-space size="large">
                  <a-button size="large" @click="resetScan">
                    <ReloadOutlined /> 重新扫描
                  </a-button>
                  <a-button type="primary" size="large" @click="goToSensitivityPage">
                    <FileSearchOutlined /> 查看敏感字段列表
                  </a-button>
                </a-space>
              </div>
            </div>
          </div>
        </a-tab-pane>

        <!-- ==================== 扫描历史 Tab ==================== -->
        <a-tab-pane key="history" tab="扫描历史">
          <div class="filter-bar">
            <a-space wrap>
              <a-select
                v-model:value="historyDsId"
                placeholder="选择数据源"
                style="width: 180px"
                allow-clear
                @change="loadHistory"
              >
                <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                  {{ ds.dsName }} ({{ ds.dsType }})
                </a-select-option>
              </a-select>
              <a-select
                v-model:value="historyStatus"
                placeholder="扫描状态"
                style="width: 120px"
                allow-clear
                @change="loadHistory"
              >
                <a-select-option value="SUCCESS">成功</a-select-option>
                <a-select-option value="FAILED">失败</a-select-option>
                <a-select-option value="RUNNING">进行中</a-select-option>
              </a-select>
              <a-input-search
                v-model:value="historyBatchNo"
                placeholder="搜索批次号"
                style="width: 200px"
                @search="loadHistory"
                @pressEnter="loadHistory"
              />
              <a-button @click="resetHistoryFilters">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
            </a-space>
          </div>

          <a-table
            :columns="historyColumns"
            :data-source="historyData"
            :loading="historyLoading"
            :pagination="historyPaginationConfig"
            :row-key="(r: ScanHistoryItem) => r.batchNo"
            @change="handleHistoryTableChange"
            :scroll="{ x: 1200 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'batchNo'">
                <a-button type="link" size="small" @click="viewBatchDetail(record)">
                  {{ record.batchNo }}
                </a-button>
              </template>
              <template v-if="column.key === 'status'">
                <a-badge
                  :status="getHistoryStatusBadge(record.status)"
                  :text="record.statusLabel || record.status"
                />
              </template>
              <template v-if="column.key === 'resultCount'">
                <a-tag v-if="record.foundSensitiveCount > 0" color="green">
                  {{ record.foundSensitiveCount }} 个敏感字段
                </a-tag>
                <span v-else>-</span>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-tooltip title="查看详情">
                    <a-button type="text" size="small" @click="viewBatchDetail(record)">
                      <EyeOutlined />
                    </a-button>
                  </a-tooltip>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 批次详情抽屉 -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      :title="`扫描批次详情 - ${currentBatchNo}`"
      :width="900"
      @close="detailDrawerVisible = false"
    >
      <template v-if="currentBatchRecord">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="批次号">{{ currentBatchRecord.batchNo }}</a-descriptions-item>
          <a-descriptions-item label="数据源">{{ currentBatchRecord.dsName }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge :status="getHistoryStatusBadge(currentBatchRecord.status)" :text="currentBatchRecord.statusLabel || currentBatchRecord.status" />
          </a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ currentBatchRecord.startTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="扫描表数">{{ currentBatchRecord.totalTableCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="扫描字段数">{{ currentBatchRecord.totalColumnCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="发现敏感字段">{{ currentBatchRecord.foundSensitiveCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="耗时">{{ currentBatchRecord.elapsedMs ? `${currentBatchRecord.elapsedMs}ms` : '-' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 敏感等级分布：点击标签筛选下方明细，再点同一标签或「清除筛选」显示全部 -->
        <div v-if="batchLevelStats" style="margin-bottom: 12px">
          <a-space wrap align="center">
            <span style="font-size: 12px; color: #8c8c8c">按等级筛选：</span>
            <a-tag
              v-for="(count, level) in batchLevelStats"
              :key="String(level)"
              :color="getLevelColor(level)"
              style="font-size: 14px; padding: 4px 12px; cursor: pointer; user-select: none"
              :bordered="detailLevelFilter === String(level)"
              @click="onBatchLevelTagClick(String(level))"
            >
              {{ level }}: {{ count }}
            </a-tag>
            <a-button v-if="detailLevelFilter" type="link" size="small" @click="detailLevelFilter = null">
              清除筛选
            </a-button>
          </a-space>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 8px; line-height: 1.5">
            置信度：开启内容扫描时按采样值与规则匹配比例分档（约 50/75/95）；未采样或采样无效时按规则优先级映射到约 65～98
            的区间。该数值为辅助排序与复核参考，不等同于真实准确率。
          </div>
        </div>

        <a-divider>敏感字段明细</a-divider>

        <a-table
          :columns="detailColumns"
          :data-source="filteredBatchDetailData"
          :loading="detailLoading"
          :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条${detailLevelFilter ? '（已筛选）' : ''}` }"
          :row-key="(r: SecColumnSensitivityVO) => r.id || `${r.tableName}-${r.columnName}`"
          size="small"
          :scroll="{ x: 900 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'columnName'">
              <div>
                <code>{{ record.columnName }}</code>
                <div style="color: #888; font-size: 12px">{{ record.columnComment || '-' }}</div>
              </div>
            </template>
            <template v-if="column.key === 'level'">
              <a-tag :color="record.levelColor || getLevelColor(record.levelName)">
                {{ record.levelName || record.sensitivityLevel || '-' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'confidence'">
              <a-progress
                :percent="Math.min(100, Math.max(0, Number(record.confidence ?? 0)))"
                size="small"
                :stroke-color="getConfidenceColor(Number(record.confidence))"
                :format="() => `${Number(record.confidence ?? 0).toFixed(1)}%`"
              />
            </template>
          </template>
        </a-table>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import {
  ScanOutlined,
  RightOutlined,
  LeftOutlined,
  DatabaseOutlined,
  LinkOutlined,
  SettingOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  ReloadOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  TagOutlined,
  EyeOutlined,
  LoadingOutlined
} from '@ant-design/icons-vue'
import {
  scanSensitiveFields,
  getSensitivityByBatchNo,
  getSensitivityStatsByDs,
  getScanHistory,
  type SecSensitivityScanDTO,
  type SecScanResultVO,
  type SecColumnSensitivityVO
} from '@/api/security'
import { dataSourceApi } from '@/api/dqc'

const router = useRouter()

// ==================== 状态 ====================
const activeTab = ref('wizard')
const dsLoading = ref(false)
const dataSourceList = ref<any[]>([])
const schemaList = ref<string[]>([])
const schemaLoading = ref(false)
const scanning = ref(false)
const scanPhase = ref<'preparing' | 'scanning' | 'done'>('preparing')
const scanError = ref<string | null>(null)
const scanTimer = ref<ReturnType<typeof setInterval> | null>(null)

/** 直连模式下，当前库/schema 下预计扫描的表数量 */
const estimateLoading = ref(false)
const directTableEstimate = ref<number | null>(null)
/** 直连拉表失败时的说明（如数据库不可达），用于页面内醒目提示 */
const directEstimateError = ref<string | null>(null)

// 数据源统计缓存
const dsStats = ref<Record<number, { tableCount: number; columnCount: number }>>({})

const currentStep = ref(1)
const stepStatus = computed(() => {
  if (currentStep.value < 4) return 'process'
  if (scanResult.value?.foundSensitiveCount) return 'success'
  return 'finish'
})

const currentDsName = computed(() => {
  const ds = dataSourceList.value.find(d => d.id === scanForm.dsId)
  return ds?.dsName || ''
})

const selectedDsType = computed(() => {
  const ds = dataSourceList.value.find(d => d.id === scanForm.dsId)
  return ds?.dsType || ''
})

const selectedDs = computed(() => {
  return dataSourceList.value.find(d => d.id === scanForm.dsId)
})

const canGoStep2 = computed(() => {
  if (!scanForm.dsId) return false
  if (selectedDsType.value === 'POSTGRESQL' && !scanForm.schema) return false
  return true
})

// ==================== 扫描表单 ====================
const scanForm = reactive<{
  dsId?: number
  schema?: string
  scanMode: 'METADATA' | 'DIRECT'
  scanDimensions: string[]
  sampleSize: number
  enableContentScan: boolean
  excludeSystemTables: boolean
  incremental: boolean
}>({
  dsId: undefined,
  schema: undefined,
  scanMode: 'METADATA',
  scanDimensions: ['COLUMN_NAME', 'COLUMN_COMMENT'],
  sampleSize: 50,
  enableContentScan: true,
  excludeSystemTables: true,
  incremental: false
})

// ==================== 扫描进度 ====================
const scanProgress = reactive({
  tableName: '',
  scannedTables: 0,
  totalTables: 0,
  scannedColumns: 0,
  totalColumns: 0,
  foundSensitive: 0,
  percent: 0,
  speed: 0,
  remainingTime: ''
})

// ==================== 扫描结果 ====================
const scanResult = ref<SecScanResultVO | null>(null)

// ==================== 扫描历史 ====================
interface ScanHistoryItem {
  batchNo: string
  dsId?: number
  dsName?: string
  status?: string
  statusLabel?: string
  startTime?: string
  elapsedMs?: number
  totalTableCount?: number
  totalColumnCount?: number
  foundSensitiveCount?: number
}

const historyLoading = ref(false)
const historyData = ref<ScanHistoryItem[]>([])
const historyDsId = ref<number | undefined>()
const historyStatus = ref<string | undefined>()
const historyBatchNo = ref<string>('')
const historyPaginationConfig = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
})

const historyColumns = [
  { title: '批次号', key: 'batchNo', width: 200, fixed: 'left' },
  { title: '数据源', dataIndex: 'dsName', width: 150 },
  { title: '状态', key: 'status', width: 100 },
  { title: '开始时间', dataIndex: 'startTime', width: 170 },
  { title: '耗时', dataIndex: 'elapsedMs', width: 100, customRender: ({ text }: any) => text ? `${text}ms` : '-' },
  { title: '扫描表数', dataIndex: 'totalTableCount', width: 100, align: 'center' as const },
  { title: '发现敏感', key: 'resultCount', width: 120, align: 'center' as const },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const }
]

const resultColumns = [
  { title: '表名', dataIndex: 'tableName', width: 150 },
  { title: '字段名', key: 'columnName', width: 180 },
  { title: '数据类型', dataIndex: 'dataType', width: 100 },
  { title: '分类', dataIndex: 'className', width: 100 },
  { title: '敏感等级', key: 'level', width: 100, align: 'center' as const },
  { title: '置信度', key: 'confidence', width: 120 },
  { title: '脱敏方式', key: 'maskType', width: 100 }
]

const detailColumns = [
  { title: '表名', dataIndex: 'tableName', width: 150 },
  { title: '字段名', key: 'columnName', width: 180 },
  { title: '数据类型', dataIndex: 'dataType', width: 100 },
  { title: '分类', dataIndex: 'className', width: 100 },
  { title: '敏感等级', key: 'level', width: 100, align: 'center' as const },
  { title: '置信度', key: 'confidence', width: 120 }
]

// ==================== 批次详情 ====================
const detailDrawerVisible = ref(false)
const currentBatchNo = ref('')
const currentBatchRecord = ref<ScanHistoryItem | null>(null)
const batchDetailData = ref<SecColumnSensitivityVO[]>([])
const detailLoading = ref(false)
const batchLevelStats = ref<Record<string, number> | null>(null)
/** 批次详情内按敏感等级名称筛选（与 levelName / sensitivityLevel 一致） */
const detailLevelFilter = ref<string | null>(null)

const filteredBatchDetailData = computed(() => {
  const rows = batchDetailData.value
  const f = detailLevelFilter.value
  if (!f) return rows
  return rows.filter((r) => (r.levelName || r.sensitivityLevel || '未知') === f)
})

function onBatchLevelTagClick(level: string) {
  detailLevelFilter.value = detailLevelFilter.value === level ? null : level
}

// ==================== 方法 ====================
async function loadDataSources() {
  dsLoading.value = true
  try {
    const res = await dataSourceApi.listEnabled()
    if (res.code === 200 && res.data) {
      dataSourceList.value = res.data
    }
  } catch {
    message.error('加载数据源失败')
  } finally {
    dsLoading.value = false
  }
}

async function loadDsStats() {
  try {
    const res = await getSensitivityStatsByDs()
    if (res.code === 200 && res.data) {
      const stats: Record<number, { tableCount: number; columnCount: number }> = {}
      for (const item of res.data) {
        stats[item.dsId as number] = {
          tableCount: item.tableCount || 0,
          columnCount: item.columnCount || 0
        }
      }
      dsStats.value = stats
    }
  } catch {
    // ignore
  }
}

function clearScanProgressTimer() {
  if (scanTimer.value != null) {
    clearInterval(scanTimer.value)
    scanTimer.value = null
  }
}

/** 直连：按数据源实时拉表清单长度，作为「预计扫描表数」 */
async function loadDirectTableEstimate() {
  directTableEstimate.value = null
  directEstimateError.value = null
  if (!scanForm.dsId || scanForm.scanMode !== 'DIRECT') return
  const dsType = (dataSourceList.value.find(d => d.id === scanForm.dsId)?.dsType || '').toUpperCase()
  if (dsType === 'POSTGRESQL' && !scanForm.schema) return

  estimateLoading.value = true
  try {
    if (dsType === 'POSTGRESQL' && scanForm.schema) {
      const res: any = await dataSourceApi.getTableListBySchema(scanForm.dsId, scanForm.schema)
      const list = Array.isArray(res?.data) ? (res.data as string[]) : []
      directTableEstimate.value = list.length
    } else if (dsType && dsType !== 'POSTGRESQL') {
      const res: any = await dataSourceApi.getTables(scanForm.dsId!)
      const list = Array.isArray(res?.data) ? (res.data as string[]) : []
      directTableEstimate.value = list.length
    }
  } catch (e: any) {
    directTableEstimate.value = null
    directEstimateError.value =
      (e?.message as string) || '无法连接数据源或获取表清单失败，请检查地址、网络与数据库服务状态'
  } finally {
    estimateLoading.value = false
  }
}

watch(
  () => [scanForm.dsId, scanForm.schema, scanForm.scanMode],
  () => {
    void loadDirectTableEstimate()
  }
)

function filterSchemaOption(input: string, option: any) {
  const v = (option?.value as string) || ''
  return v.toLowerCase().includes((input || '').toLowerCase())
}

async function onDsChange() {
  scanForm.schema = undefined
  schemaList.value = []
  if (selectedDsType.value !== 'POSTGRESQL' || !scanForm.dsId) {
    void loadDirectTableEstimate()
    return
  }

  schemaLoading.value = true
  try {
    const res: any = await dataSourceApi.getSchemas(scanForm.dsId)
    const list = Array.isArray(res?.data) ? (res.data as string[]) : []
    schemaList.value = list

    const configuredSchema = selectedDs.value?.schemaName
    if (configuredSchema) {
      if (list.includes(configuredSchema)) {
        scanForm.schema = configuredSchema
      } else {
        scanForm.schema = undefined
        message.warning(`数据源配置的 Schema「${configuredSchema}」在数据库中不存在，请检查配置或确认该 Schema 是否可访问`)
      }
    } else if (list.length > 0) {
      scanForm.schema = list[0]
    } else {
      message.warning('未获取到可用 Schema，请检查数据源连接与权限')
    }
  } catch {
    schemaList.value = []
    message.error('加载 Schema 列表失败')
  } finally {
    schemaLoading.value = false
  }
  void loadDirectTableEstimate()
}

function onSchemaChange() {
  void loadDirectTableEstimate()
}

function goStep(step: number) {
  if (step === 2) {
    if (!canGoStep2.value) {
      message.warning('请选择数据源' + (selectedDsType.value === 'POSTGRESQL' ? '和 Schema' : ''))
      return
    }
  }
  currentStep.value = step
}

async function startScan() {
  if (!scanForm.dsId) {
    message.warning('请先选择数据源')
    return
  }

  if (scanForm.scanDimensions.length === 0) {
    message.warning('请至少选择一个扫描维度')
    return
  }

  scanning.value = true
  scanPhase.value = 'preparing'
  scanError.value = null
  currentStep.value = 3

  await loadDirectTableEstimate()

  const metaStat = scanForm.dsId ? dsStats.value[scanForm.dsId] : undefined
  const presetTotalTables =
    scanForm.scanMode === 'METADATA' && metaStat
      ? metaStat.tableCount
      : directTableEstimate.value != null
        ? directTableEstimate.value
        : 0
  // 字段总数：元数据扫描用平台统计；直连扫描优先复用同数据源的元数据字段数（若已采集），否则按表数粗估，否则进度条里「已扫描字段」恒为 0/?。
  let presetTotalColumns = 0
  if (scanForm.scanMode === 'METADATA' && metaStat) {
    presetTotalColumns = metaStat.columnCount || 0
  } else if (metaStat?.columnCount) {
    presetTotalColumns = metaStat.columnCount
  }
  if (presetTotalColumns <= 0 && presetTotalTables > 0) {
    presetTotalColumns = Math.max(1, Math.round(presetTotalTables * 15))
  }

  // 重置进度
  Object.assign(scanProgress, {
    tableName: '正在连接服务…',
    scannedTables: 0,
    totalTables: presetTotalTables,
    scannedColumns: 0,
    totalColumns: presetTotalColumns || 0,
    foundSensitive: 0,
    percent: 5,
    speed: 0,
    remainingTime: ''
  })

  clearScanProgressTimer()
  const phaseMsgs = [
    '正在连接数据源…',
    '正在读取表与字段…',
    '正在匹配敏感规则…',
    '正在执行内容采样…'
  ]
  let tick = 0
  scanPhase.value = 'scanning'
  scanTimer.value = setInterval(() => {
    tick++
    if (scanProgress.percent < 88) {
      scanProgress.percent = Math.min(88, scanProgress.percent + 4 + Math.floor(Math.random() * 6))
    }
    const ix = Math.min(phaseMsgs.length - 1, Math.floor(tick / 3))
    scanProgress.tableName = phaseMsgs[ix]
    const totalT = scanProgress.totalTables || 0
    if (totalT > 0) {
      scanProgress.scannedTables = Math.min(totalT, Math.max(1, Math.round((scanProgress.percent / 100) * totalT)))
    }
    const totalC = scanProgress.totalColumns || 0
    if (totalC > 0) {
      scanProgress.scannedColumns = Math.min(totalC, Math.round((scanProgress.percent / 100) * totalC))
    }
    scanProgress.speed = 12 + Math.floor(Math.random() * 20)
    scanProgress.remainingTime = scanProgress.percent < 85 ? '计算中…' : ''
  }, 450)

  try {
    const dto: SecSensitivityScanDTO = {
      dsId: scanForm.dsId,
      schema: selectedDsType.value === 'POSTGRESQL' ? scanForm.schema : undefined,
      scanScope: 'ALL',
      excludeSystemTables: scanForm.excludeSystemTables,
      scanColumnName: scanForm.scanDimensions.includes('COLUMN_NAME'),
      scanColumnComment: scanForm.scanDimensions.includes('COLUMN_COMMENT'),
      scanDataType: scanForm.scanDimensions.includes('DATA_TYPE'),
      sampleSize: scanForm.sampleSize,
      enableContentScan: scanForm.enableContentScan,
      incremental: scanForm.incremental,
      directScan: scanForm.scanMode === 'DIRECT',
      scanCycle: 'ONCE'
    }

    const res = await scanSensitiveFields(dto)

    if (res.code === 200 && res.data) {
      scanResult.value = res.data
      scanProgress.totalTables = res.data.totalTableCount || 0
      scanProgress.totalColumns = res.data.totalColumnCount || 0
      scanProgress.scannedTables = res.data.totalTableCount || 0
      scanProgress.scannedColumns = res.data.totalColumnCount || 0
      scanProgress.foundSensitive = res.data.foundSensitiveCount || 0
      scanProgress.percent = 100
      scanProgress.tableName = '扫描完成'
      scanProgress.speed = 0
      scanProgress.remainingTime = ''
    } else {
      scanError.value = res.message || '扫描失败，请重试'
    }
  } catch (err: any) {
    scanError.value = err?.message || '扫描请求失败，请检查网络和数据源连接'
    scanResult.value = null
  } finally {
    clearScanProgressTimer()
    scanPhase.value = 'done'
    scanning.value = false
    currentStep.value = 4
  }
}

function resetScan() {
  scanForm.dsId = undefined
  scanForm.schema = undefined
  schemaList.value = []
  scanForm.scanMode = 'METADATA'
  scanForm.scanDimensions = ['COLUMN_NAME', 'COLUMN_COMMENT']
  scanForm.sampleSize = 50
  scanForm.enableContentScan = true
  scanForm.excludeSystemTables = true
  scanForm.incremental = false
  scanResult.value = null
  scanError.value = null
  currentStep.value = 1
  scanPhase.value = 'preparing'
}

function goToSensitivityPage() {
  router.push('/security/sensitivity-manage')
}

// ==================== 扫描历史 ====================
async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await getScanHistory({
      pageNum: historyPaginationConfig.current,
      pageSize: historyPaginationConfig.pageSize,
      dsId: historyDsId.value,
      status: historyStatus.value,
      batchNo: historyBatchNo.value || undefined
    })

    if (res.code === 200 && res.data) {
      historyData.value = res.data.records || []
      historyPaginationConfig.total = res.data.total || 0
    } else {
      historyData.value = []
      historyPaginationConfig.total = 0
    }
  } catch (err) {
    console.error('加载扫描历史失败:', err)
    historyData.value = []
    historyPaginationConfig.total = 0
  } finally {
    historyLoading.value = false
  }
}

function handleHistoryTableChange(pag: any) {
  historyPaginationConfig.current = pag.current
  historyPaginationConfig.pageSize = pag.pageSize
  loadHistory()
}

function resetHistoryFilters() {
  historyDsId.value = undefined
  historyStatus.value = undefined
  historyBatchNo.value = ''
  historyPaginationConfig.current = 1
  loadHistory()
}

async function viewBatchDetail(record: ScanHistoryItem) {
  currentBatchNo.value = record.batchNo
  currentBatchRecord.value = record
  detailDrawerVisible.value = true
  detailLevelFilter.value = null
  detailLoading.value = true
  batchLevelStats.value = null
  batchDetailData.value = []

  try {
    const res = await getSensitivityByBatchNo(record.batchNo)
    if (res.code === 200 && res.data) {
      batchDetailData.value = res.data
      // 统计各等级数量
      const stats: Record<string, number> = {}
      for (const item of res.data) {
        const level = item.levelName || item.sensitivityLevel || '未知'
        stats[level] = (stats[level] || 0) + 1
      }
      batchLevelStats.value = stats
      // 用明细回填顶部汇总（与列表口径一致，避免表名为空时统计为 0）
      const tables = new Set<string>()
      for (const item of res.data) {
        if (item.tableName) tables.add(String(item.tableName))
      }
      if (currentBatchRecord.value) {
        currentBatchRecord.value = {
          ...currentBatchRecord.value,
          foundSensitiveCount: res.data.length,
          totalColumnCount: res.data.length,
          totalTableCount: tables.size || currentBatchRecord.value.totalTableCount,
          dsName: res.data[0]?.dsName || currentBatchRecord.value.dsName
        }
      }
    }
  } catch {
    message.error('加载批次详情失败')
  } finally {
    detailLoading.value = false
  }
}

// ==================== 工具函数 ====================
function getHistoryStatusBadge(status?: string): 'success' | 'error' | 'processing' | 'default' {
  const map: Record<string, 'success' | 'error' | 'processing' | 'default'> = {
    SUCCESS: 'success',
    FAILED: 'error',
    RUNNING: 'processing'
  }
  return map[status || ''] || 'default'
}

function getLevelColor(level?: string) {
  const map: Record<string, string> = {
    L1: '#52C41A',
    L2: '#FAAD14',
    L3: '#FA8C16',
    L4: '#F5222D',
    '高度敏感': '#F5222D',
    '敏感': '#FA8C16',
    '内部': '#FAAD14',
    '一般': '#52C41A'
  }
  return map[level || ''] || 'default'
}

function getConfidenceColor(confidence?: number) {
  if (!confidence) return '#d9d9d9'
  if (confidence >= 80) return '#52C41A'
  if (confidence >= 50) return '#FAAD14'
  return '#F5222D'
}

function getMaskTypeColor(maskType?: string) {
  const colorMap: Record<string, string> = {
    NONE: 'default',
    MASK: 'blue',
    HIDE: 'orange',
    ENCRYPT: 'red',
    DELETE: 'purple',
    PARTIAL_MASK: 'cyan'
  }
  return colorMap[maskType || ''] || 'default'
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadDataSources()
  loadDsStats()
  loadHistory()
})

onUnmounted(() => {
  clearScanProgressTimer()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-icon {
  flex-shrink: 0;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
  line-height: 1.3;
}
.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #8c8c8c;
}
.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}
.filter-bar {
  margin-bottom: 12px;
}

/* 向导样式 */
.wizard-container {
  padding: 8px 0 28px;
}
.step-indicator {
  max-width: 880px;
  margin: 0 auto 36px;
  padding: 28px 32px 32px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 12px;
}
.step-content {
  max-width: 880px;
  margin: 0 auto;
}
.step-content-padded {
  padding-top: 4px;
}
.step-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.step-card-spacious :deep(.ant-card-body) {
  padding: 28px 32px 32px;
}
.step-card-spacious :deep(.ant-card-head) {
  padding: 16px 32px;
  min-height: 52px;
}
.step1-form :deep(.ant-form-item) {
  margin-bottom: 22px;
}
.scan-scope-summary {
  margin: 4px 0 20px;
  padding: 16px 18px 18px;
  background: #fafafa;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
}
.summary-title {
  font-size: 13px;
  font-weight: 600;
  color: #595959;
  margin-bottom: 14px;
  letter-spacing: 0.02em;
}
.summary-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.summary-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.summary-item-highlight {
  padding-top: 12px;
  border-top: 1px dashed #e8e8e8;
}
.summary-label {
  font-size: 12px;
  color: #8c8c8c;
}
.summary-value {
  font-size: 14px;
  color: #262626;
  line-height: 1.5;
}
.summary-value strong {
  color: #1677ff;
  font-size: 18px;
  font-weight: 600;
}
.summary-sep {
  color: #bfbfbf;
  margin: 0 6px;
}
.summary-muted {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.55;
}
.summary-meta {
  margin-left: 6px;
  font-size: 13px;
  color: #595959;
}
.summary-meta code {
  font-size: 12px;
  background: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
}
.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}
.step-footer {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 28px;
  border-top: 1px solid #f0f0f0;
}

/* 数据源选择 */
.ds-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ds-name {
  font-weight: 500;
}

/* 扫描维度 */
.scan-dimensions {
  width: 100%;
}
.dimension-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}
.dimension-item span:first-of-type {
  font-weight: 500;
}
.dimension-desc {
  font-size: 12px;
  color: #8c8c8c;
}

/* 滑块 */
.slider-container {
  display: flex;
  align-items: center;
  gap: 16px;
}
.slider-container :deep(.ant-slider) {
  flex: 1;
}
.slider-value {
  min-width: 60px;
  text-align: right;
  font-weight: 500;
  color: #722ED1;
}

/* 扫描中状态 */
.scanning-card {
  text-align: center;
}
.scanning-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}
.scanning-animation {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.scanning-icon {
  font-size: 36px;
  color: #fff;
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(0.95); }
}
.scanning-text h3 {
  margin: 0 0 8px;
  font-size: 18px;
}
.scanning-info {
  color: #8c8c8c;
  margin: 0;
}
.progress-section {
  padding: 8px 28px 16px;
}
.progress-stats {
  display: flex;
  justify-content: space-around;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 28px;
}
.progress-stat-wrap {
  display: inline-block;
  cursor: help;
}
.progress-detail {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  color: #8c8c8c;
  font-size: 12px;
}

/* 结果样式 */
.result-card {
  margin-bottom: 16px;
}
.result-stats {
  margin-top: 16px;
}

/* 表单提示 */
.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
  line-height: 1.4;
}
.form-tip-block {
  margin-top: 10px;
  margin-bottom: 2px;
  line-height: 1.55;
  max-width: 720px;
}
</style>
