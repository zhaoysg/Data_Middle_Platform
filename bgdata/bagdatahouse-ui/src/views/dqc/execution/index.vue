<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#grad4)"/>
            <path d="M5 12H19M12 5L19 12L12 19" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="grad4" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#FA8C16"/>
                <stop offset="100%" stop-color="#FFC53D"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">执行记录</h1>
          <p class="page-subtitle">查看质检方案的历史执行记录，了解质量评分与失败详情</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="handleExport">
          <template #icon><DownloadOutlined /></template>
          导出记录
        </a-button>
        <a-button @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 执行记录 / 质量概览 Tab -->
    <a-tabs v-model:activeKey="activeTab" class="mb-4">
      <a-tab-pane key="execution" tab="执行记录">
        <!-- 统计卡片：执行状态维度 -->
        <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
          <a-col :xs="12" :sm="6" v-for="stat in execStatCards" :key="stat.label">
            <div class="stat-mini-card" :style="{ background: stat.bg }">
              <div class="mini-value">{{ stat.value }}</div>
              <div class="mini-label">{{ stat.label }}</div>
            </div>
          </a-col>
        </a-row>

        <!-- 筛选区 -->
        <div class="filter-bar">
          <a-space wrap>
            <a-select
              v-model:value="filterPlan"
              placeholder="选择方案"
              style="width: 180px"
              allowClear
              showSearch
              @change="loadData"
            >
              <a-select-option value="">全部方案</a-select-option>
              <a-select-option v-for="p in planOptions" :key="p.id" :value="p.id">
                {{ p.planName }}
              </a-select-option>
            </a-select>
            <a-select
              v-model:value="filterStatus"
              placeholder="执行状态"
              style="width: 120px"
              allowClear
              @change="loadData"
            >
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="RUNNING">运行中</a-select-option>
              <a-select-option value="SUCCESS">成功</a-select-option>
              <a-select-option value="FAILED">失败</a-select-option>
              <a-select-option value="SKIPPED">已跳过</a-select-option>
              <a-select-option value="BLOCKED">已阻塞</a-select-option>
            </a-select>
            <a-range-picker
              v-model:value="dateRange"
              :placeholder="['开始日期', '结束日期']"
              format="YYYY-MM-DD"
              style="width: 260px"
              @change="onDateChange"
            />
            <a-button @click="resetFilters">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </div>

        <!-- 数据表格 -->
        <div class="table-card">
          <a-table
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :pagination="pagination"
            :row-key="(record: any) => record.id"
            @change="handleTableChange"
            :scroll="{ x: 1380 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'executionNo'">
                <a-tooltip :title="record.executionNo">
                  <a-button
                    type="link"
                    size="small"
                    class="exec-no-link-only"
                    @click="showDetail(record)"
                  >
                    <span class="exec-no-text">{{ record.executionNo }}</span>
                  </a-button>
                </a-tooltip>
              </template>

              <template v-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)" class="status-tag">
                  <span class="status-dot" :class="'dot-' + record.status.toLowerCase()"></span>
                  {{ getStatusLabel(record.status) }}
                </a-tag>
              </template>

              <template v-if="column.key === 'qualityScore'">
                <div class="score-cell">
                  <template v-if="record.status === 'RUNNING'">
                    <a-progress
                      v-if="(record.totalRules ?? 0) > 0"
                      :percent="getRunningRowPercent(record)"
                      :stroke-color="'#722ED1'"
                      status="active"
                      size="small"
                      :format="() => runningScoreLabel(record)"
                      style="width: 140px"
                    />
                    <span v-else class="text-muted">启动中…</span>
                  </template>
                  <template v-else>
                    <a-progress
                      v-if="record.qualityScore !== undefined && record.qualityScore !== null"
                      :percent="Number(record.qualityScore)"
                      :stroke-color="getScoreColor(record.qualityScore)"
                      :trail-color="'#f0f0f0'"
                      size="small"
                      :format="() => record.qualityScore + '分'"
                      style="width: 120px"
                    />
                    <span v-else class="text-muted">-</span>
                  </template>
                  <a-tooltip v-if="(record.failedRules ?? 0) > 0" title="存在失败规则，实际执行健康度低于评分">
                    <WarningOutlined style="color: #FA8C16; margin-left: 4px" />
                  </a-tooltip>
                </div>
              </template>

                <template v-if="column.key === 'ruleCounts'">
                <span class="text-muted">
                  {{ record.passedRules ?? 0 }} /
                  <span :class="record.failedRules ? 'text-danger' : ''">{{ record.failedRules ?? 0 }}</span> /
                  {{ record.skippedRules ?? 0 }}
                </span>
              </template>

              <template v-if="column.key === 'elapsedMs'">
                <span class="text-muted">{{ formatDuration(record.elapsedMs) }}</span>
              </template>

              <template v-if="column.key === 'triggerType'">
                <a-tag :color="getTriggerColor(record.triggerType)">
                  {{ getTriggerLabel(record.triggerType) }}
                </a-tag>
              </template>

              <template v-if="column.key === 'actions'">
                <a-space size="small">
                  <a-tooltip title="规则执行明细">
                    <a-button type="text" size="small" @click="openExecSteps(record)">
                      <template #icon><UnorderedListOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-tooltip v-if="record.status === 'RUNNING'" title="实时进度">
                    <a-button type="text" size="small" @click="openExecutionProgress(record)">
                      <template #icon><ScheduleOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-tooltip title="执行详情">
                    <a-button type="text" size="small" @click="showDetail(record)">
                      <template #icon><EyeOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-tooltip v-if="(record.failedRules ?? 0) > 0" title="失败明细">
                    <a-button type="text" size="small" danger @click="showErrorDetail(record)">
                      <template #icon><AlertOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-tooltip
                    :title="
                      record.status === 'RUNNING'
                        ? '该次执行仍在进行中，请用「明细」查看步骤；勿重复触发以免产生多条运行中记录。'
                        : '重新执行'
                    "
                  >
                    <a-button
                      type="text"
                      size="small"
                      :loading="retryingId === record.id"
                      :disabled="record.status === 'RUNNING'"
                      @click="handleRetry(record)"
                    >
                      <template #icon><RedoOutlined /></template>
                    </a-button>
                  </a-tooltip>
                </a-space>
              </template>

            </template>
          </a-table>
        </div>
      </a-tab-pane>

      <a-tab-pane key="overview" tab="质量概览">
        <!-- 统计卡片：聚合维度 -->
        <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
          <a-col :xs="12" :sm="6" v-for="stat in overviewStatCards" :key="stat.label">
            <div class="stat-mini-card" :style="{ background: stat.bg }">
              <div class="mini-value">{{ stat.value }}</div>
              <div class="mini-label">{{ stat.label }}</div>
            </div>
          </a-col>
        </a-row>

        <!-- 筛选区（质量概览仅按时间范围） -->
        <div class="filter-bar">
          <a-space wrap>
            <a-range-picker
              v-model:value="overviewDateRange"
              :placeholder="['开始日期', '结束日期']"
              format="YYYY-MM-DD"
              style="width: 260px"
              @change="onOverviewDateChange"
            />
            <a-button @click="resetOverviewFilters">
              <template #icon><ReloadOutlined /></template>
              重置时间
            </a-button>
            <a-button type="primary" :loading="exporting" @click="handleOverviewExport">
              <template #icon><DownloadOutlined /></template>
              导出报告
            </a-button>
          </a-space>
        </div>

        <!-- 数据表格 -->
        <div class="table-card">
          <a-table
            :columns="overviewColumns"
            :data-source="overviewData"
            :loading="overviewLoading"
            :pagination="overviewPagination"
            :row-key="(record: any) => record.id"
            @change="handleOverviewTableChange"
            :scroll="{ x: 1200 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'elapsedMs'">
                <span class="text-muted">{{ formatDuration(record.elapsedMs) }}</span>
              </template>
              <template v-else-if="column.key === 'qualityScore'">
                <div v-if="record.qualityScore != null" class="score-cell">
                  <a-progress
                    :percent="Number(record.qualityScore)"
                    :status="getProgressStatus(record.qualityScore)"
                    :stroke-color="getScoreColor(record.qualityScore)"
                    :trail-color="'#f0f0f0'"
                    size="small"
                    style="width: 140px"
                  />
                </div>
                <span v-else class="text-muted">-</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusLabel(record.status) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
                  <a-divider type="vertical" />
                  <a-dropdown>
                    <a-button type="link" size="small">更多</a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item key="export" @click="handleExportOne(record)">导出报告</a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- 执行详情抽屉 -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      :title="'执行详情 — ' + (detailRecord?.executionNo || '')"
      :width="900"
      placement="right"
      :destroy-on-close="true"
    >
      <template v-if="detailRecord">
        <!-- 执行信息卡片 -->
        <div class="detail-info-grid">
          <div class="info-card">
            <div class="info-label">执行状态</div>
            <div class="info-value">
              <a-tag :color="getStatusColor(detailRecord.status)">
                {{ getStatusLabel(detailRecord.status) }}
              </a-tag>
            </div>
          </div>
          <div class="info-card">
            <div class="info-label">质量评分</div>
            <div class="info-value score-value" :style="{ color: getScoreColor(detailRecord.qualityScore) }">
              {{ detailRecord.qualityScore !== undefined && detailRecord.qualityScore !== null ? detailRecord.qualityScore + ' 分' : '-' }}
            </div>
          </div>
          <div class="info-card" v-if="detailRecord.sensitivityComplianceRate != null || detailRecord.sensitivityLevel">
            <div class="info-label">
              <SafetyCertificateOutlined style="color: #FAAD14; margin-right: 4px" />
              敏感等级
            </div>
            <div class="info-value" :style="{ color: getSensitivityTextColor(detailRecord.sensitivityLevel || '') }">
              {{ detailRecord.sensitivityLevel || '-' }}
            </div>
          </div>
          <div class="info-card" v-if="detailRecord.sensitivityComplianceRate != null">
            <div class="info-label">敏感合规率</div>
            <div class="info-value">
              <a-progress
                :percent="detailRecord.sensitivityComplianceRate ?? 0"
                :stroke-color="getComplianceRateColor(detailRecord.sensitivityComplianceRate)"
                size="small"
                :format="() => (detailRecord.sensitivityComplianceRate ?? 0) + '%'"
              />
            </div>
          </div>
          <div class="info-card">
            <div class="info-label">执行耗时</div>
            <div class="info-value">{{ formatDuration(detailRecord.elapsedMs) }}</div>
          </div>
          <div class="info-card">
            <div class="info-label">触发方式</div>
            <div class="info-value">
              <a-tag :color="getTriggerColor(detailRecord.triggerType)">
                {{ getTriggerLabel(detailRecord.triggerType) }}
              </a-tag>
            </div>
          </div>
          <div class="info-card">
            <div class="info-label">开始时间</div>
            <div class="info-value">{{ detailRecord.startTime || '-' }}</div>
          </div>
          <div class="info-card">
            <div class="info-label">结束时间</div>
            <div class="info-value">{{ detailRecord.endTime || '-' }}</div>
          </div>
        </div>

        <!-- 评分明细 -->
        <div
          v-if="detailRecord.scoreBreakdown || detailRecord.dimensionScores"
          class="breakdown-section"
        >
          <div class="section-title">
            <BarChartOutlined /> 质量评分
          </div>
          <div class="breakdown-grid">
            <div class="breakdown-item" v-for="dim in scoreBreakdownList" :key="dim.name">
              <div class="breakdown-name">{{ dim.name }}</div>
              <a-progress
                :percent="dim.score != null ? dim.score : 0"
                :stroke-color="dim.score === null ? '#d9d9d9' : dim.color"
                size="small"
                :format="() => (dim.score === null ? '未评估' : dim.score + '分')"
              />
            </div>
          </div>
          <!-- 执行健康度（若有失败规则，健康度 = 通过率，不会虚高） -->
          <div v-if="healthScoreDisplay" class="health-score-bar">
            <span class="health-score-label">{{ healthScoreDisplay.label }}</span>
            <a-progress
              :percent="healthScoreDisplay.value"
              :stroke-color="healthScoreDisplay.value < 70 ? '#FF4D4F' : '#52C41A'"
              :format="() => healthScoreDisplay.value + '分'"
              size="small"
              style="width: 140px"
            />
            <a-tag v-if="detailRecord.failedRules > 0" color="red" size="small">存在失败</a-tag>
          </div>
        </div>

        <!-- 规则执行明细 -->
        <div class="rules-section">
          <div class="section-title">
            <TableOutlined /> 规则执行明细
            <span class="section-count">
              共 {{ ruleDetails.length }} 条
              <span class="text-success">，通过 {{ passCount }} 条</span>
              <span v-if="failCount > 0" class="text-danger">，失败 {{ failCount }} 条</span>
            </span>
          </div>
          <a-table
            :columns="ruleDetailColumns"
            :data-source="ruleDetails"
            :pagination="{ pageSize: 10 }"
            size="small"
            :row-key="(record: any) => record.id"
            :scroll="{ x: 1100, y: ruleDetailTableScrollY }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="getRuleStatusColor(record.status)">
                  {{ getRuleStatusLabel(record.status) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'qualityScore'">
                <span :style="{ color: getScoreColor(record.qualityScore) }">
                  {{ record.qualityScore !== undefined && record.qualityScore !== null ? record.qualityScore + '分' : '-' }}
                </span>
              </template>
              <template v-if="column.key === 'elapsedMs'">
                {{ formatDuration(record.elapsedMs) }}
              </template>
              <template v-if="column.key === 'sqlContent'">
                <a-tooltip v-if="record.sqlContent" :title="record.sqlContent" placement="topLeft">
                  <a-button type="link" size="small" style="padding: 0 4px">
                    <template #icon><CodeOutlined /></template>
                    SQL
                  </a-button>
                </a-tooltip>
                <span v-else class="text-muted">-</span>
              </template>
              <template v-if="column.key === 'errorDetail'">
                <a-tooltip v-if="record.errorDetail" :title="record.errorDetail">
                  <a-button type="link" size="small" danger>
                    <template #icon><AlertOutlined /></template>
                    查看
                  </a-button>
                </a-tooltip>
                <span v-else class="text-muted">-</span>
              </template>
            </template>
          </a-table>
        </div>

        <!-- 错误详情 -->
        <div v-if="detailRecord.errorDetail" class="error-section">
          <div class="section-title text-danger">
            <CloseCircleOutlined /> 错误详情
          </div>
          <pre class="error-pre">{{ detailRecord.errorDetail }}</pre>
        </div>
      </template>

      <template #footer>
        <a-space wrap>
          <a-button @click="detailDrawerVisible = false">关闭</a-button>
          <a-button
            v-if="detailRecord?.status === 'RUNNING'"
            type="primary"
            @click="openExecutionProgress(detailRecord)"
          >
            <template #icon><ScheduleOutlined /></template>
            查看进度
          </a-button>
          <a-tooltip
            :title="detailRecord?.status === 'RUNNING' ? '当前执行未完成，请用「进度」查看各规则步骤。' : ''"
          >
            <a-button
              v-if="detailRecord"
              type="primary"
              :loading="retrying"
              :disabled="detailRecord.status === 'RUNNING'"
              @click="handleRetry(detailRecord)"
            >
              <template #icon><RedoOutlined /></template>
              重新执行
            </a-button>
          </a-tooltip>
        </a-space>
      </template>
    </a-drawer>

    <!-- 错误详情弹窗 -->
    <a-modal
      v-model:open="errorModalVisible"
      title="失败规则与原因"
      :width="720"
      :footer="null"
    >
      <p v-if="errorSummary" class="error-summary">{{ errorSummary }}</p>
      <pre class="error-pre-modal">{{ errorContent }}</pre>
    </a-modal>

    <!-- 流程化执行进度（Ant Design 风格） -->
    <a-modal
      v-model:open="progressModalVisible"
      centered
      :width="640"
      :footer="null"
      destroy-on-close
      wrap-class-name="exec-progress-modal-wrap"
      class="exec-progress-modal-root"
    >
      <template #title>
        <div class="epm-modal-title">
          <span class="epm-modal-title-text">执行进度</span>
          <span v-if="progressExecution?.executionNo" class="epm-modal-title-meta">
            <code>{{ progressExecution.executionNo }}</code>
          </span>
        </div>
      </template>

      <div v-if="progressExecution" class="epm-body">
        <!-- 概况区：阿里系常见的信息密度 + 浅底卡片 -->
        <div class="epm-summary-card">
          <div class="epm-summary-top">
            <div class="epm-summary-tags">
              <a-tag :color="getStatusColor(progressExecution.status)" class="epm-tag-lg">
                {{ getStatusLabel(progressExecution.status) }}
              </a-tag>
              <span v-if="progressExecution.status === 'RUNNING'" class="epm-live-pill">
                <SyncOutlined spin class="epm-live-icon" />
                每 2 秒自动刷新
              </span>
            </div>
            <div class="epm-summary-plan">
              <div class="epm-field-label">质检方案</div>
              <div class="epm-field-value">{{ progressExecution.planName || '—' }}</div>
            </div>
          </div>

          <a-alert
            v-if="progressExecution.status === 'RUNNING'"
            type="info"
            show-icon
            class="epm-alert"
            message="规则按顺序执行，单条含大表 SQL 时可能较慢，请耐心等待。"
          />

          <div v-if="(progressExecution.totalRules ?? 0) > 0" class="epm-progress-section">
            <div class="epm-progress-label-row">
              <span class="epm-section-label">整体进度</span>
              <span class="epm-progress-fraction">{{ progressModalPercentLabel }}</span>
            </div>
            <a-progress
              :percent="progressModalPercent"
              :status="progressBarStatus"
              :stroke-color="progressStrokeColor"
              :stroke-width="6"
              :show-info="false"
              class="epm-ant-progress"
            />
          </div>
          <div v-else-if="progressExecution.status === 'RUNNING'" class="epm-boot-hint">
            <LoadingOutlined class="epm-boot-icon" spin />
            正在拉起任务并写入规则总数…
          </div>
        </div>

        <a-divider class="epm-divider" />

        <div class="epm-steps-section">
          <div class="epm-section-head">
            <span class="epm-section-bar" aria-hidden="true" />
            <span class="epm-section-title">规则执行步骤</span>
            <span class="epm-section-hint">按执行顺序展示</span>
          </div>

          <div v-if="sortedProgressRules.length" class="epm-steps-scroll">
            <a-steps direction="vertical" class="epm-steps" :class="{ 'epm-steps-compact': sortedProgressRules.length > 4 }">
              <a-step
                v-for="d in sortedProgressRules"
                :key="d.id"
                :status="mapDetailToStepStatus(d.status)"
              >
                <template #title>
                  <span class="epm-step-title">{{ d.ruleName || '未命名规则' }}</span>
                </template>
                <template #description>
                  <div class="epm-step-desc">
                    <div class="epm-step-meta">
                      <DatabaseOutlined class="epm-step-meta-icon" />
                      <a-tag v-if="d.targetTable" color="default" class="epm-table-tag">{{ d.targetTable }}</a-tag>
                      <span v-else class="epm-step-meta-muted">未指定目标表</span>
                    </div>
                    <div class="epm-step-caption">{{ stepDetailCaption(d) }}</div>
                    <a-button
                      v-if="d.sqlContent"
                      type="link"
                      size="small"
                      class="epm-sql-link"
                      @click="previewRuleSql(d.sqlContent!)"
                    >
                      <template #icon><CodeOutlined /></template>
                      查看 SQL
                    </a-button>
                  </div>
                </template>
              </a-step>
            </a-steps>
          </div>
          <a-empty
            v-else
            class="epm-empty"
            description="暂无规则明细，任务刚启动时可点击「刷新」"
          />
        </div>

        <div class="epm-footer">
          <a-space :size="12" wrap>
            <a-button :loading="progressRefreshing" @click="refreshProgressManual">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
            <a-button v-if="progressExecution?.planId" @click="goExecListFromProgress">
              <template #icon><FileTextOutlined /></template>
              执行记录
            </a-button>
            <a-button type="primary" @click="openFullDetailFromProgress">
              <template #icon><EyeOutlined /></template>
              完整详情
            </a-button>
          </a-space>
        </div>
      </div>
    </a-modal>

    <!-- 执行明细向导（随时可查看，兼容所有状态） -->
    <a-modal
      v-model:open="stepsModalVisible"
      centered
      :width="640"
      :footer="null"
      destroy-on-close
      wrap-class-name="exec-steps-modal-wrap"
      class="exec-steps-modal-root"
    >
      <template #title>
        <div class="epm-modal-title">
          <span class="epm-modal-title-text">执行明细</span>
          <span v-if="stepsExecution?.executionNo" class="epm-modal-title-meta">
            <code>{{ stepsExecution?.executionNo }}</code>
          </span>
        </div>
      </template>

      <div v-if="stepsExecution" class="epm-body">
        <!-- 概况卡片 -->
        <div class="epm-summary-card">
          <div class="epm-summary-top">
            <div class="epm-summary-tags">
              <a-tag :color="getStatusColor(stepsExecution.status)" class="epm-tag-lg">
                {{ getStatusLabel(stepsExecution.status) }}
              </a-tag>
              <span v-if="stepsExecution.status === 'RUNNING'" class="epm-live-pill">
                <SyncOutlined spin class="epm-live-icon" />
                仍在进行中
              </span>
              <span v-else-if="stepsExecution.status === 'SUCCESS'" class="epm-success-pill">
                <CheckCircleOutlined class="epm-success-icon" />
                已全部完成
              </span>
            </div>
            <div class="epm-summary-right-cols">
              <div class="epm-summary-col">
                <div class="epm-field-label">质检方案</div>
                <div class="epm-field-value">{{ stepsExecution.planName || '—' }}</div>
              </div>
              <div class="epm-summary-col">
                <div class="epm-field-label">耗时</div>
                <div class="epm-field-value">{{ formatDuration(stepsExecution.elapsedMs) }}</div>
              </div>
              <div class="epm-summary-col">
                <div class="epm-field-label">规则</div>
                <div class="epm-field-value">
                  <span class="text-success">{{ stepsExecution.passedRules ?? 0 }}</span>
                  <span class="text-muted"> / {{ stepsExecution.totalRules ?? 0 }}</span>
                  <span v-if="(stepsExecution.failedRules ?? 0) > 0" class="text-danger"> · {{ stepsExecution.failedRules }} 失败</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 整体进度条（非运行中时展示最终结果） -->
          <div v-if="(stepsExecution.totalRules ?? 0) > 0" class="epm-progress-section">
            <div class="epm-progress-label-row">
              <span class="epm-section-label">规则完成度</span>
              <span class="epm-progress-fraction">
                {{ (stepsExecution.passedRules ?? 0) + (stepsExecution.failedRules ?? 0) + (stepsExecution.skippedRules ?? 0) }} / {{ stepsExecution.totalRules }} 规则
              </span>
            </div>
            <a-progress
              :percent="stepsModalFinalPercent"
              :status="stepsExecution.status === 'SUCCESS' ? 'success' : stepsExecution.status === 'FAILED' ? 'exception' : 'active'"
              :stroke-color="stepsExecution.status === 'SUCCESS' ? '#52C41A' : stepsExecution.status === 'FAILED' ? '#FF4D4F' : '#1677FF'"
              :stroke-width="6"
              :show-info="false"
              class="epm-ant-progress"
            />
          </div>
        </div>

        <a-divider class="epm-divider" />

        <!-- 规则明细步骤 -->
        <div class="epm-steps-section">
          <div class="epm-section-head">
            <span class="epm-section-bar" aria-hidden="true" />
            <span class="epm-section-title">规则执行步骤</span>
            <span class="epm-section-hint">
              共 {{ sortedStepsRules.length }} 条
              <span v-if="stepsExecution.passedRules" class="text-success"> · 通过 {{ stepsExecution.passedRules }}</span>
              <span v-if="(stepsExecution.failedRules ?? 0) > 0" class="text-danger"> · 失败 {{ stepsExecution.failedRules }}</span>
            </span>
          </div>

          <div v-if="stepsLoading" class="epm-loading-row">
            <LoadingOutlined spin />
            <span>加载规则明细中…</span>
          </div>

          <div v-else-if="sortedStepsRules.length" class="epm-steps-scroll">
            <a-steps direction="vertical" class="epm-steps" :class="{ 'epm-steps-compact': sortedStepsRules.length > 4 }">
              <a-step
                v-for="d in sortedStepsRules"
                :key="d.id"
                :status="mapDetailToStepStatus(d.status)"
              >
                <template #title>
                  <span class="epm-step-title">{{ d.ruleName || '未命名规则' }}</span>
                </template>
                <template #description>
                  <div class="epm-step-desc">
                    <div class="epm-step-meta">
                      <DatabaseOutlined class="epm-step-meta-icon" />
                      <a-tag v-if="d.targetTable" color="default" class="epm-table-tag">{{ d.targetTable }}</a-tag>
                      <span v-else class="epm-step-meta-muted">未指定目标表</span>
                    </div>
                    <div class="epm-step-caption">{{ stepDetailCaption(d) }}</div>
                    <div v-if="d.sqlContent" class="epm-step-sql-row">
                      <a-button type="link" size="small" class="epm-sql-link" @click="previewRuleSql(d.sqlContent!)">
                        <template #icon><CodeOutlined /></template>
                        查看 SQL
                      </a-button>
                    </div>
                    <div v-if="d.errorDetail && (d.status === 'FAILED' || d.status === 'BLOCKED')" class="epm-step-error">
                      <span class="epm-step-error-text">{{ truncateOneLine(d.errorDetail, 200) }}</span>
                    </div>
                  </div>
                </template>
              </a-step>
            </a-steps>
          </div>
          <a-empty v-else description="暂无规则明细（该次执行可能未写入明细）" class="epm-empty" />
        </div>

        <div class="epm-footer">
          <a-space :size="12" wrap>
            <a-button :loading="stepsLoading" @click="loadStepsDetails">
              <template #icon><ReloadOutlined /></template>
              刷新明细
            </a-button>
            <a-button @click="stepsModalVisible = false">
              关闭
            </a-button>
          </a-space>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, h } from 'vue'
import {
  Button,
  Input,
  Select,
  Table,
  Modal,
  Drawer,
  Dropdown,
  Tag,
  Badge,
  Switch,
  Progress,
  DatePicker,
  Pagination,
  Space,
  Row,
  Col,
  Divider,
  Tooltip,
  Tabs,
  Steps,
  Alert,
  Empty,
  message
} from 'ant-design-vue'
import {
  EyeOutlined, AlertOutlined, RedoOutlined, ReloadOutlined, DownloadOutlined,
  BarChartOutlined, TableOutlined, CloseCircleOutlined, SafetyCertificateOutlined,
  CodeOutlined, WarningOutlined, SyncOutlined, ScheduleOutlined,
  LoadingOutlined, FileTextOutlined, DatabaseOutlined, UnorderedListOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { executionApi, planApi } from '@/api/dqc'
import type { Execution, ExecutionDetail, Plan } from '@/types'

const router = useRouter()

const loading = ref(false)
const tableData = ref<Execution[]>([])
const planOptions = ref<Plan[]>([])
const selectedRowKeys = ref<number[]>([])
const retryingId = ref<number>()
const retrying = ref(false)

// 当前 Tab: execution = 执行记录, overview = 质量概览
const activeTab = ref('execution')

const filterPlan = ref<number>()
const filterStatus = ref<string>()
const dateRange = ref<any[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 执行记录 Tab 统计卡片
const execStatCards = ref([
  { label: '今日执行', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)' },
  { label: '成功', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '失败', value: 0, bg: 'linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%)' },
  { label: '运行中', value: 0, bg: 'linear-gradient(135deg, #FA8C16 0%, #FFC53D 100%)' }
])

// 质量概览 Tab 统计卡片
const overviewStatCards = ref([
  { label: '总报告数', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)' },
  { label: '平均得分', value: '-', bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '通过率', value: '-', bg: 'linear-gradient(135deg, #722ED1 0%, #B37FEB 100%)' },
  { label: '成功执行', value: 0, bg: 'linear-gradient(135deg, #FA8C16 0%, #FFC53D 100%)' }
])

const columns = [
  { title: '执行编号', key: 'executionNo', width: 200, fixed: 'left' },
  { title: '方案名称', dataIndex: 'planName', key: 'planName', width: 180, ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '质量评分', key: 'qualityScore', width: 160 },
  { title: '通过/失败/跳过', key: 'ruleCounts', width: 150 },
  { title: '耗时', key: 'elapsedMs', width: 90 },
  { title: '触发方式', key: 'triggerType', width: 90 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 170 },
  { title: '操作', key: 'actions', width: 170, fixed: 'right' }
]

const detailDrawerVisible = ref(false)
const detailRecord = ref<Execution | null>(null)
const ruleDetails = ref<ExecutionDetail[]>([])

const ruleDetailColumns = [
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 160, ellipsis: true },
  { title: '目标表', dataIndex: 'targetTable', key: 'targetTable', width: 120, ellipsis: true },
  { title: '状态', key: 'status', width: 80 },
  { title: '评分', key: 'qualityScore', width: 70 },
  { title: '错误数', dataIndex: 'errorCount', key: 'errorCount', width: 70 },
  { title: '执行SQL', key: 'sqlContent', width: 90 },
  { title: '耗时', key: 'elapsedMs', width: 80 },
  { title: '错误详情', key: 'errorDetail', width: 90 }
]

/** 后端 scoreBreakdown 存的是各维度 Map；dimensionScores 存 serializeBreakdown 完整 JSON。统一解析为维度分数字典。 */
function parseExecutionBreakdown(record: Execution | null): Record<string, unknown> | null {
  if (!record) return null
  const asObj = (raw: unknown): Record<string, unknown> | null => {
    if (raw == null) return null
    if (typeof raw === 'string') {
      try {
        return JSON.parse(raw) as Record<string, unknown>
      } catch {
        return null
      }
    }
    if (typeof raw === 'object') return raw as Record<string, unknown>
    return null
  }
  const dimField = asObj(record.dimensionScores as unknown)
  if (dimField?.dimensionScores && typeof dimField.dimensionScores === 'object') {
    return dimField
  }
  const sb = asObj(record.scoreBreakdown as unknown)
  if (sb?.dimensionScores && typeof sb.dimensionScores === 'object') {
    return sb
  }
  const codes = ['COMPLETENESS', 'UNIQUENESS', 'ACCURACY', 'CONSISTENCY', 'TIMELINESS', 'VALIDITY']
  if (sb && codes.some(c => sb[c] !== undefined)) {
    return { dimensionScores: sb, healthScore: sb.healthScore, stats: sb.stats }
  }
  return sb
}

function toDimNumber(v: unknown): number | null {
  if (v === null || v === undefined) return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

const scoreBreakdownList = computed(() => {
  const root = parseExecutionBreakdown(detailRecord.value)
  if (!root) return []
  const scores = (root.dimensionScores || {}) as Record<string, unknown>
  return [
    { name: '完整性', score: toDimNumber(scores['COMPLETENESS']), color: '#1677FF' },
    { name: '唯一性', score: toDimNumber(scores['UNIQUENESS']), color: '#52C41A' },
    { name: '准确性', score: toDimNumber(scores['ACCURACY']), color: '#FA8C16' },
    { name: '一致性', score: toDimNumber(scores['CONSISTENCY']), color: '#722ED1' },
    { name: '及时性', score: toDimNumber(scores['TIMELINESS']), color: '#13C2C2' },
    { name: '有效性', score: toDimNumber(scores['VALIDITY']), color: '#FF4D4F' }
  ]
})

const healthScoreDisplay = computed(() => {
  const root = parseExecutionBreakdown(detailRecord.value)
  if (!root) return null
  try {
    if (root.healthScore !== undefined && root.healthScore !== null) {
      return { value: Number(root.healthScore), label: '执行健康度' }
    }
    const stats = root.stats as { passedRules?: number; totalRules?: number } | undefined
    if (stats) {
      const { passedRules = 0, totalRules = 0 } = stats
      const passRate = totalRules > 0 ? Math.round(passedRules / totalRules * 100) : null
      return passRate !== null ? { value: passRate, label: '规则通过率' } : null
    }
  } catch {}
  return null
})

const passCount = computed(() => ruleDetails.value.filter(r => r.status === 'SUCCESS').length)
const failCount = computed(() => ruleDetails.value.filter(r => r.status === 'FAILED' || r.status === 'BLOCKED').length)

const ruleDetailTableScrollY = computed(() => {
  if (typeof window === 'undefined') return 360
  return Math.min(560, Math.max(260, window.innerHeight - 400))
})

// ========== 质量概览 Tab ==========
const overviewLoading = ref(false)
const overviewData = ref<Execution[]>([])
const overviewDateRange = ref<any[]>([])
const exporting = ref(false)

const overviewPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const overviewColumns = [
  { title: '报告编号', dataIndex: 'executionNo', key: 'executionNo', width: 180, ellipsis: true },
  { title: '质检方案', dataIndex: 'planName', key: 'planName', width: 200, ellipsis: true },
  { title: '执行时间', dataIndex: 'startTime', key: 'startTime', width: 170 },
  { title: '执行耗时', key: 'elapsedMs', width: 110 },
  { title: '质量得分', key: 'qualityScore', width: 180 },
  { title: '执行状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const }
]

const errorModalVisible = ref(false)
const errorContent = ref('')
const errorSummary = ref('')

/** 流程化进度弹窗 */
const progressModalVisible = ref(false)
const progressExecution = ref<Execution | null>(null)
const progressRuleDetails = ref<ExecutionDetail[]>([])
const progressRefreshing = ref(false)
let progressPollTimer: ReturnType<typeof setInterval> | null = null

const sortedProgressRules = computed(() =>
  [...progressRuleDetails.value].sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
)

const progressModalPercent = computed(() => {
  const ex = progressExecution.value
  if (!ex || !(ex.totalRules && ex.totalRules > 0)) return 0
  const done =
    (ex.passedRules ?? 0) + (ex.failedRules ?? 0) + (ex.skippedRules ?? 0)
  let pct = Math.round((done / ex.totalRules!) * 100)
  if (ex.status === 'RUNNING') {
    const hasRunning = sortedProgressRules.value.some(d => d.status === 'RUNNING')
    if (hasRunning && pct < 100) {
      pct = Math.max(pct, Math.round(((done + 0.5) / ex.totalRules!) * 100))
    }
  }
  return Math.min(100, pct)
})

const progressModalPercentLabel = computed(() => {
  const ex = progressExecution.value
  if (!ex?.totalRules) return '—'
  const done =
    (ex.passedRules ?? 0) + (ex.failedRules ?? 0) + (ex.skippedRules ?? 0)
  return `${done}/${ex.totalRules} 规则`
})

const stepsModalFinalPercent = computed(() => {
  const ex = stepsExecution.value
  if (!ex || !(ex.totalRules && ex.totalRules > 0)) return 0
  const done = (ex.passedRules ?? 0) + (ex.failedRules ?? 0) + (ex.skippedRules ?? 0)
  return Math.min(100, Math.round((done / ex.totalRules) * 100))
})

/** Ant Design Progress：失败/阻塞用异常态，成功用成功态，运行中用主色流动 */
const progressBarStatus = computed(() => {
  const st = progressExecution.value?.status
  if (st === 'FAILED' || st === 'BLOCKED') return 'exception' as const
  if (st === 'SUCCESS') return 'success' as const
  return 'active' as const
})

const progressStrokeColor = computed(() => {
  const st = progressExecution.value?.status
  if (st === 'FAILED' || st === 'BLOCKED') return '#FF4D4F'
  if (st === 'SUCCESS') return '#52C41A'
  return '#1677FF'
})

function mapDetailToStepStatus(
  status: string | undefined
): 'wait' | 'process' | 'finish' | 'error' {
  switch (status) {
    case 'RUNNING':
      return 'process'
    case 'SUCCESS':
      return 'finish'
    case 'FAILED':
    case 'BLOCKED':
      return 'error'
    default:
      return 'wait'
  }
}

function stepDetailCaption(d: ExecutionDetail): string {
  if (d.status === 'RUNNING') return '正在执行…'
  if (d.status === 'SKIPPED') return d.errorDetail ? `跳过：${d.errorDetail}` : '已跳过'
  if (d.status === 'FAILED' || d.status === 'BLOCKED') {
    return d.errorDetail ? truncateOneLine(d.errorDetail, 120) : '未通过'
  }
  if (d.elapsedMs != null && d.elapsedMs > 0) return `耗时 ${formatDuration(d.elapsedMs)}`
  return '已完成'
}

function truncateOneLine(s: string, max: number): string {
  const t = s.replace(/\s+/g, ' ').trim()
  return t.length <= max ? t : t.slice(0, max) + '…'
}

function previewRuleSql(sql: string) {
  Modal.info({
    title: '执行 SQL',
    width: 760,
    okText: '关闭',
    content: h('pre', {
      style: {
        maxHeight: '420px',
        overflow: 'auto',
        fontSize: '12px',
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-all',
        margin: 0
      }
    }, sql)
  })
}

function stopProgressPoll() {
  if (progressPollTimer) {
    clearInterval(progressPollTimer)
    progressPollTimer = null
  }
}

async function refreshProgressData() {
  const ex = progressExecution.value
  if (!ex?.id) return
  try {
    const execRes: any = await executionApi.getById(ex.id)
    if (execRes?.data) progressExecution.value = execRes.data as Execution
    const detRes: any = await executionApi.getDetails(ex.id)
    progressRuleDetails.value = (detRes?.data || []) as ExecutionDetail[]
  } catch {
    /* 轮询中单次失败忽略 */
  }
}

async function refreshProgressManual() {
  progressRefreshing.value = true
  try {
    await refreshProgressData()
  } finally {
    progressRefreshing.value = false
  }
}

function openExecutionProgress(record: Execution) {
  progressExecution.value = { ...record }
  progressRuleDetails.value = []
  progressModalVisible.value = true
  void refreshProgressData()
  stopProgressPoll()
  if (record.status === 'RUNNING') {
    progressPollTimer = setInterval(() => {
      void refreshProgressData().then(() => {
        const st = progressExecution.value?.status
        if (st && st !== 'RUNNING') {
          stopProgressPoll()
          void loadData()
          message.success('本次执行已结束，列表已刷新')
        }
      })
    }, 2000)
  }
}

function openFullDetailFromProgress() {
  const ex = progressExecution.value
  if (!ex) return
  progressModalVisible.value = false
  void showDetail(ex)
}

function goExecListFromProgress() {
  const ex = progressExecution.value
  if (!ex?.planId) return
  progressModalVisible.value = false
  sessionStorage.setItem('dqc-exec-filter-plan', String(ex.planId))
  if (ex.id) sessionStorage.setItem('dqc-exec-open-exec', String(ex.id))
  void router.push({ path: '/dqc/execution' })
}

/** ─────────────────────────────────────────────
 * 随时查看的执行明细弹窗（不自动轮询，所有记录可用）
 * ───────────────────────────────────────────── */
const stepsModalVisible = ref(false)
const stepsExecution = ref<Execution | null>(null)
const stepsRuleDetails = ref<ExecutionDetail[]>([])
const stepsLoading = ref(false)

const sortedStepsRules = computed(() =>
  [...stepsRuleDetails.value].sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
)

async function openExecSteps(record: Execution) {
  stepsExecution.value = { ...record }
  stepsRuleDetails.value = []
  stepsModalVisible.value = true
  await loadStepsDetails()
}

async function loadStepsDetails() {
  const ex = stepsExecution.value
  if (!ex?.id) return
  stepsLoading.value = true
  try {
    const res: any = await executionApi.getDetails(ex.id)
    stepsRuleDetails.value = (res?.data || []) as ExecutionDetail[]
  } catch {
    stepsRuleDetails.value = []
  } finally {
    stepsLoading.value = false
  }
}

watch(stepsModalVisible, open => {
  if (!open) {
    stepsRuleDetails.value = []
  }
})

watch(progressModalVisible, open => {
  if (!open) {
    stopProgressPoll()
  }
})

function getRunningRowPercent(record: Execution): number {
  const total = record.totalRules ?? 0
  if (total <= 0) return 0
  const done =
    (record.passedRules ?? 0) +
    (record.failedRules ?? 0) +
    (record.skippedRules ?? 0)
  let pct = Math.round((done / total) * 100)
  if (record.status === 'RUNNING' && pct < 100) {
    pct = Math.max(pct, Math.round(((done + 0.5) / total) * 100))
  }
  return Math.min(100, pct)
}

function runningScoreLabel(record: Execution): string {
  const total = record.totalRules ?? 0
  if (total <= 0) return '—'
  const done =
    (record.passedRules ?? 0) +
    (record.failedRules ?? 0) +
    (record.skippedRules ?? 0)
  return `${done}/${total}`
}

function todayLocalYmd(): string {
  const n = new Date()
  const y = n.getFullYear()
  const m = String(n.getMonth() + 1).padStart(2, '0')
  const d = String(n.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await executionApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      planId: filterPlan.value,
      status: filterStatus.value || undefined,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD')
    })
    tableData.value = res.data?.records || res.data || []
    pagination.total = res.data?.total || tableData.value.length

    // 统计卡基于全量数据计算（最多取2000条覆盖99%场景）
    const aggSize = Math.min(pagination.total, 2000)
    const aggRes: any = await executionApi.page({
      pageNum: 1,
      pageSize: aggSize,
      planId: filterPlan.value,
      status: filterStatus.value || undefined,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD')
    })
    const allRecords: Execution[] = aggRes.data?.records ?? []
    const ymd = todayLocalYmd()
    execStatCards.value[0].value = allRecords.filter(e => {
      const st = e.startTime
      return st != null && String(st).slice(0, 10) === ymd
    }).length
    execStatCards.value[1].value = allRecords.filter(e => e.status === 'SUCCESS').length
    execStatCards.value[2].value = allRecords.filter(
      e => e.status === 'FAILED' || e.status === 'BLOCKED'
    ).length
    execStatCards.value[3].value = allRecords.filter(e => e.status === 'RUNNING').length
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function loadPlans() {
  try {
    const res: any = await planApi.page({ pageNum: 1, pageSize: 999 })
    planOptions.value = res.data?.records || []
  } catch {
    planOptions.value = []
  }
}

async function showDetail(record: Execution) {
  detailRecord.value = record
  detailDrawerVisible.value = true
  try {
    const res: any = await executionApi.getDetails(record.id!)
    ruleDetails.value = res.data || []
  } catch {
    ruleDetails.value = []
  }
}

async function showErrorDetail(record: Execution) {
  errorModalVisible.value = true
  errorSummary.value = ''
  errorContent.value = '正在加载失败明细…'
  try {
    const res: any = await executionApi.getDetails(record.id!)
    const list: any[] = res.data || []
    const failed = list.filter(
      d => d.status === 'FAILED' || d.status === 'BLOCKED' || d.status === 'SKIPPED'
    )
    const realFails = list.filter(d => d.status === 'FAILED' || d.status === 'BLOCKED')
    if (realFails.length === 0) {
      errorSummary.value =
        failed.length > 0
          ? '本次执行没有「失败」规则，存在跳过项见下方。'
          : '未找到失败规则明细（可能仍在写入或数据已清理）。'
      errorContent.value =
        failed.length > 0
          ? failed
              .map(
                (d: any) =>
                  `【${d.ruleName || '规则'}】${d.targetTable ? ` 表 ${d.targetTable}` : ''} — ${d.status}\n${d.errorDetail || '—'}\n`
              )
              .join('\n---\n')
          : record.errorDetail || '—'
      return
    }
    errorSummary.value = `共 ${realFails.length} 条规则未通过，请对照 SQL 与错误信息排查数据源、表达式或阈值。`
    errorContent.value = realFails
      .map((d: any) => {
        const parts = [
          `【${d.ruleName || '规则'}】`,
          d.targetTable ? `表: ${d.targetTable}` : '',
          d.targetColumn ? `列: ${d.targetColumn}` : '',
          d.errorDetail ? `原因: ${d.errorDetail}` : '',
          d.sqlContent ? `SQL: ${d.sqlContent}` : ''
        ].filter(Boolean)
        return parts.join('\n')
      })
      .join('\n\n---\n\n')
  } catch {
    errorSummary.value = ''
    errorContent.value = record.errorDetail || '加载失败明细出错，请刷新后重试。'
  }
}

async function handleRetry(record: Execution) {
  if (record.status === 'RUNNING') {
    message.warning('该条仍在运行中，请点「进度」查看步骤，勿重复触发。')
    return
  }
  if (record.id) {
    retryingId.value = record.id
    retrying.value = true
  }
  try {
    const res: any = await planApi.execute(record.planId!)
    const newId = res?.data as number | undefined
    message.success(
      newId
        ? `已触发新的执行（ID ${newId}），将打开进度窗口`
        : '重新执行已触发'
    )
    await loadData()
    if (detailDrawerVisible.value) {
      detailDrawerVisible.value = false
    }
    if (newId) {
      try {
        const er: any = await executionApi.getById(newId)
        if (er?.data) {
          openExecutionProgress(er.data as Execution)
        }
      } catch {
        /* 打开进度失败时用户仍可在列表中找到新记录 */
      }
    }
  } catch (e: any) {
    const errMsg = e?.data?.msg || e?.data?.message || e?.message || '重跑失败，请检查方案配置'
    message.error(errMsg)
  } finally {
    retryingId.value = undefined
    retrying.value = false
  }
}

async function handleExport() {
  try {
    const blob = (await executionApi.export({
      planId: filterPlan.value,
      status: filterStatus.value,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD')
    })) as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `执行记录_${Date.now()}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function onDateChange() {
  pagination.current = 1
  loadData()
}

function resetFilters() {
  filterPlan.value = undefined
  filterStatus.value = undefined
  dateRange.value = []
  pagination.current = 1
  loadData()
}

// ========== 质量概览 Tab ==========
async function loadOverviewData() {
  overviewLoading.value = true
  try {
    const res: any = await executionApi.page({
      pageNum: overviewPagination.current,
      pageSize: overviewPagination.pageSize,
      planId: undefined,
      status: undefined,
      startDate: overviewDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: overviewDateRange.value?.[1]?.format('YYYY-MM-DD')
    })
    overviewData.value = res.data?.records || res.data || []
    overviewPagination.total = res.data?.total || overviewData.value.length

    // 聚合统计：最多取 2000 条
    const aggSize = Math.min(overviewPagination.total, 2000)
    const aggRes: any = await executionApi.page({
      pageNum: 1,
      pageSize: aggSize,
      planId: undefined,
      status: undefined,
      startDate: overviewDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: overviewDateRange.value?.[1]?.format('YYYY-MM-DD')
    })
    const all: Execution[] = aggRes.data?.records ?? []
    overviewStatCards.value[3].value = all.filter(e => e.status === 'SUCCESS').length
    overviewStatCards.value[0].value = all.length

    const withScore = all.filter(e => e.qualityScore != null)
    const avg = withScore.length > 0
      ? withScore.reduce((s, e) => s + Number(e.qualityScore), 0) / withScore.length
      : 0
    overviewStatCards.value[1].value = withScore.length ? avg.toFixed(2) : '-'

    const terminal = all.filter(e => e.status === 'SUCCESS' || e.status === 'FAILED')
    const passRate = terminal.length > 0
      ? (all.filter(e => e.status === 'SUCCESS').length / terminal.length) * 100
      : 0
    overviewStatCards.value[2].value = terminal.length ? `${passRate.toFixed(2)}%` : '-'
  } catch {
    overviewData.value = []
    overviewPagination.total = 0
  } finally {
    overviewLoading.value = false
  }
}

function onOverviewDateChange() {
  overviewPagination.current = 1
  void loadOverviewData()
}

function resetOverviewFilters() {
  overviewDateRange.value = []
  overviewPagination.current = 1
  void loadOverviewData()
}

function handleOverviewTableChange(pag: any) {
  if (pag.current != null) overviewPagination.current = pag.current
  if (pag.pageSize != null) overviewPagination.pageSize = pag.pageSize
  void loadOverviewData()
}

function getProgressStatus(score: number) {
  if (score >= 90) return 'success'
  if (score >= 70) return 'normal'
  return 'exception'
}

async function handleOverviewExport() {
  try {
    const blob = await executionApi.export({
      startDate: overviewDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: overviewDateRange.value?.[1]?.format('YYYY-MM-DD')
    }) as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `质量报告_${Date.now()}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

async function handleExportOne(record: Execution) {
  try {
    const blob = await executionApi.exportExecutionReport(record.id!) as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `质检报告_${record.executionNo}_${Date.now()}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

function getStatusColor(status: string | undefined) {
  const map: Record<string, string> = {
    RUNNING: 'processing',
    SUCCESS: 'success',
    FAILED: 'error',
    SKIPPED: 'default',
    BLOCKED: 'error'
  }
  return map[status || ''] || 'default'
}

function getStatusLabel(status: string | undefined) {
  const map: Record<string, string> = {
    RUNNING: '运行中',
    SUCCESS: '成功',
    FAILED: '失败',
    SKIPPED: '已跳过',
    BLOCKED: '已阻塞'
  }
  return map[status || ''] || status || '-'
}

function getTriggerColor(trigger: string | undefined) {
  const map: Record<string, string> = {
    MANUAL: 'blue',
    SCHEDULE: 'purple',
    API: 'green'
  }
  return map[trigger || ''] || 'default'
}

function getTriggerLabel(trigger: string | undefined) {
  const map: Record<string, string> = {
    MANUAL: '手动',
    SCHEDULE: '定时',
    API: 'API'
  }
  return map[trigger || ''] || trigger || '-'
}

function getScoreColor(score: number) {
  if (score === null || score === undefined) return '#999'
  if (score >= 90) return '#52C41A'
  if (score >= 70) return '#FA8C16'
  return '#FF4D4F'
}

function getComplianceRateColor(rate: number | undefined) {
  if (rate === null || rate === undefined) return '#999'
  if (rate >= 90) return '#52C41A'
  if (rate >= 70) return '#FA8C16'
  return '#FF4D4F'
}

function getSensitivityTextColor(level: string | undefined) {
  const map: Record<string, string> = { L4: '#FF4D4F', L3: '#FA8C16', L2: '#1677FF', L1: '#52C41A' }
  return map[level || ''] || '#8C8C8C'
}

function formatDuration(ms?: number) {
  if (!ms) return '-'
  if (ms < 1000) return ms + 'ms'
  if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
  return (ms / 60000).toFixed(1) + 'min'
}

function getRuleStatusColor(status: string) {
  return getStatusColor(status)
}

function getRuleStatusLabel(status: string) {
  return getStatusLabel(status)
}

// Tab 切换时懒加载概览数据
watch(activeTab, (tab) => {
  if (tab === 'overview' && overviewData.value.length === 0) {
    void loadOverviewData()
  }
})

onMounted(async () => {
  await loadPlans()
  const planId = sessionStorage.getItem('dqc-exec-filter-plan')
  if (planId) {
    filterPlan.value = Number(planId)
    sessionStorage.removeItem('dqc-exec-filter-plan')
  }
  await loadData()
  const openExecId = sessionStorage.getItem('dqc-exec-open-exec')
  if (openExecId) {
    sessionStorage.removeItem('dqc-exec-open-exec')
    try {
      const res: any = await executionApi.getById(Number(openExecId))
      if (res?.data) {
        await showDetail(res.data as Execution)
      }
    } catch {
      /* 忽略：记录可能被分页筛掉或已删除 */
    }
  }
})

onUnmounted(() => {
  stopProgressPoll()
})
</script>

<style lang="less" scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    .header-icon { flex-shrink: 0; }
    .page-title { font-size: 22px; font-weight: 700; color: #1F1F1F; margin: 0; }
    .page-subtitle { font-size: 13px; color: #8C8C8C; margin: 4px 0 0; }
  }
  .header-right { display: flex; gap: 12px; }
}

.stat-mini-card {
  border-radius: 8px;
  padding: 16px 20px;
  color: #fff;
  .mini-value { font-size: 28px; font-weight: 700; line-height: 1.2; }
  .mini-label { font-size: 13px; opacity: 0.85; margin-top: 4px; }
}

.filter-bar {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    display: inline-block;
    &.dot-running { background: #1677FF; animation: pulse 1.5s infinite; }
    &.dot-success { background: #52C41A; }
    &.dot-failed { background: #FF4D4F; }
    &.dot-skipped { background: #999; }
    &.dot-blocked { background: #FF4D4F; }
  }
}

.score-cell { display: flex; align-items: center; }

.exec-no-link-only {
  padding: 0 !important;
  height: auto !important;
  text-align: left;
}
.exec-no-text {
  display: block;
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 188px;
}

.detail-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
  .info-card {
    background: #F5F7FA;
    border-radius: 8px;
    padding: 12px 16px;
    .info-label { font-size: 12px; color: #8C8C8C; margin-bottom: 6px; }
    .info-value { font-size: 15px; font-weight: 600; color: #1F1F1F; }
    .score-value { font-size: 24px; font-weight: 700; }
  }
}

.breakdown-section, .rules-section, .error-section {
  margin-bottom: 24px;
  .section-title {
    font-size: 15px;
    font-weight: 600;
    color: #1F1F1F;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 8px;
    &.text-danger { color: #FF4D4F; }
    .section-count { font-size: 13px; font-weight: 400; color: #666; margin-left: 8px; }
  }
}

.breakdown-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  .breakdown-item {
    background: #F5F7FA;
    border-radius: 8px;
    padding: 12px 16px;
    .breakdown-name { font-size: 13px; color: #666; margin-bottom: 8px; }
  }
}

.health-score-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding: 10px 16px;
  background: #FFF7E6;
  border: 1px solid #FFD591;
  border-radius: 8px;
  .health-score-label {
    font-size: 13px;
    color: #D46B08;
    font-weight: 500;
  }
}

.error-pre {
  background: #1F1F1F;
  color: #FF7875;
  padding: 16px;
  border-radius: 8px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 400px;
  overflow-y: auto;
}

.error-summary {
  margin: 0 0 12px;
  font-size: 14px;
  color: #434343;
  line-height: 1.5;
}

.error-pre-modal {
  background: #FFF1F0;
  color: #FF4D4F;
  border: 1px solid #FFCCC7;
  padding: 16px;
  border-radius: 8px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 500px;
  overflow-y: auto;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.text-danger { color: #FF4D4F; }
.text-success { color: #52C41A; }
.text-muted { color: #8C8C8C; }

/* ---------- 执行进度弹窗（贴近 Ant Design / 阿里云控制台信息密度） ---------- */
.exec-progress-modal-wrap {
  :deep(.ant-modal-content) {
    padding: 0;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 6px 16px 0 rgba(0, 0, 0, 0.08), 0 3px 6px -4px rgba(0, 0, 0, 0.12);
  }
  :deep(.ant-modal-header) {
    margin: 0;
    padding: 16px 24px;
    border-bottom: 1px solid #f0f0f0;
    background: #fafafa;
  }
  :deep(.ant-modal-close) {
    top: 14px;
    inset-inline-end: 16px;
  }
  :deep(.ant-modal-body) {
    padding: 20px 24px 24px;
    max-height: min(72vh, 640px);
    overflow-y: auto;
  }
}

.epm-modal-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}
.epm-modal-title-text {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  line-height: 1.4;
}
.epm-modal-title-meta {
  code {
    font-size: 12px;
    font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
    color: rgba(0, 0, 0, 0.45);
    background: rgba(0, 0, 0, 0.04);
    padding: 2px 8px;
    border-radius: 4px;
    border: 1px solid #f0f0f0;
  }
}

.epm-body {
  margin: 0;
}

.epm-summary-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px 20px;
}
.epm-summary-top {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.epm-summary-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.epm-tag-lg {
  margin: 0;
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 4px;
}
.epm-live-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #1677ff;
  background: #e6f4ff;
  border: 1px solid #91caff;
  border-radius: 999px;
  padding: 2px 10px;
}
.epm-live-icon {
  font-size: 12px;
}
.epm-summary-plan {
  text-align: right;
  min-width: 120px;
}
.epm-field-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 1.5;
  margin-bottom: 2px;
}
.epm-field-value {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  line-height: 1.4;
}
.epm-alert {
  margin-bottom: 0;
  border-radius: 6px;
  font-size: 13px;
}
.epm-progress-section {
  margin-top: 16px;
}
.epm-progress-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.epm-section-label {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
}
.epm-progress-fraction {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  font-variant-numeric: tabular-nums;
}
.epm-ant-progress {
  margin: 0;
  :deep(.ant-progress-bg) {
    border-radius: 4px;
  }
  :deep(.ant-progress-outer) {
    border-radius: 4px;
  }
}
.epm-boot-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}
.epm-boot-icon {
  color: #1677ff;
  font-size: 16px;
}

.epm-divider {
  margin: 20px 0;
  border-color: #f0f0f0;
}

.epm-steps-section {
  margin-bottom: 4px;
}
.epm-section-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.epm-section-bar {
  width: 3px;
  height: 14px;
  background: #1677ff;
  border-radius: 2px;
  flex-shrink: 0;
}
.epm-section-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}
.epm-section-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-left: auto;
}

.epm-steps-scroll {
  max-height: 320px;
  overflow-y: auto;
  padding-right: 8px;
  margin-right: -4px;
}

.epm-steps {
  :deep(.ant-steps-item-title) {
    font-size: 14px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.88);
    line-height: 1.5;
    padding-right: 8px;
  }
  :deep(.ant-steps-item-description) {
    padding-bottom: 8px !important;
  }
  :deep(.ant-steps-item-tail) {
    &::after {
      background-color: #f0f0f0 !important;
    }
  }
  :deep(.ant-steps-item-finish .ant-steps-item-tail::after) {
    background-color: #d9d9d9 !important;
  }
  :deep(.ant-steps-item-icon) {
    width: 28px;
    height: 28px;
    line-height: 26px;
    font-size: 13px;
  }
  :deep(.ant-steps-item-process .ant-steps-item-icon) {
    background: #e6f4ff;
    border-color: #1677ff;
    color: #1677ff;
  }
}
.epm-steps-compact {
  :deep(.ant-steps-item-description) {
    padding-bottom: 4px !important;
  }
}

.epm-step-title {
  word-break: break-word;
}
.epm-step-desc {
  margin-top: 6px;
  max-width: 100%;
}
.epm-step-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}
.epm-step-meta-icon {
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
}
.epm-table-tag {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  border-radius: 4px;
}
.epm-step-meta-muted {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.25);
}
.epm-step-caption {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 1.5;
  margin-bottom: 2px;
}
.epm-sql-link {
  padding-left: 0 !important;
  height: auto;
  font-size: 12px;
}

.epm-empty {
  padding: 24px 0;
  :deep(.ant-empty-description) {
    color: rgba(0, 0, 0, 0.45);
    font-size: 13px;
  }
}

.epm-footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
}

/* 明细弹窗共用样式（与进度弹窗复用 .epm-* 类） */
.exec-steps-modal-wrap {
  :deep(.ant-modal-content) {
    padding: 0;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 6px 16px 0 rgba(0, 0, 0, 0.08), 0 3px 6px -4px rgba(0, 0, 0, 0.12);
  }
  :deep(.ant-modal-header) {
    margin: 0;
    padding: 16px 24px;
    border-bottom: 1px solid #f0f0f0;
    background: #fafafa;
  }
  :deep(.ant-modal-close) {
    top: 14px;
    inset-inline-end: 16px;
  }
  :deep(.ant-modal-body) {
    padding: 20px 24px 24px;
    max-height: min(72vh, 680px);
    overflow-y: auto;
  }
}

/* 成功胶囊（明细弹窗中「已完成」状态提示） */
.epm-success-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #52c41a;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 999px;
  padding: 2px 10px;
}
.epm-success-icon {
  font-size: 12px;
}

/* 概况卡片右侧列布局 */
.epm-summary-right-cols {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}
.epm-summary-col {
  min-width: 80px;
  .epm-field-label {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
    line-height: 1.5;
    margin-bottom: 2px;
  }
  .epm-field-value {
    font-size: 15px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.88);
    line-height: 1.4;
  }
}

/* SQL 按钮行 */
.epm-step-sql-row {
  margin-top: 4px;
}

/* 错误行 */
.epm-step-error {
  margin-top: 6px;
  padding: 6px 10px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
}
.epm-step-error-text {
  font-size: 12px;
  color: #ff4d4f;
  line-height: 1.5;
  word-break: break-all;
}

/* 加载行 */
.epm-loading-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
