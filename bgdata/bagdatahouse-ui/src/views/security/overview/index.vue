<template>
  <div class="page-container security-overview">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#secGrad)"/>
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="secGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#F5222D"/>
                <stop offset="100%" stop-color="#FF7875"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">数据安全总览</h1>
          <p class="page-subtitle">实时掌握敏感资产健康状态，关注安全风险与合规进展</p>
        </div>
      </div>
      <div class="header-right">
        <span class="update-time">昨日更新 · {{ currentTime }}</span>
        <a-button @click="loadAllData" :loading="loading">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[20, 20]" class="stat-row">
      <a-col :xs="24" :sm="12" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #F5222D 0%, #FF7875 100%)">
          <div class="stat-value">{{ overview.totalSensitiveFieldCount || 0 }}</div>
          <div class="stat-label">敏感资产总数</div>
          <div class="stat-trend">
            <span class="trend-up">↑ {{ statTrend.sensitiveCountTrend || 0 }}%</span>
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #FA8C16 0%, #FFD591 100%)">
          <div class="stat-value">{{ healthScore.score || 0 }}</div>
          <div class="stat-label">安全健康分</div>
          <div class="stat-badge" :style="{ background: healthScoreColor, color: '#fff' }">
            {{ healthScore.gradeLabel || '暂无数据' }}
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%)">
          <div class="stat-value">{{ overview.sensitiveCoverage || 0 }}%</div>
          <div class="stat-label">敏感资产覆盖率</div>
          <div class="stat-trend" :style="{ color: '#fff' }">
            <span class="trend-up">↑ {{ statTrend.coverageTrend || 0 }}%</span>
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #1677FF 0%, #69B1FF 100%)">
          <div class="stat-value">{{ overview.weeklyScanCount || 0 }}</div>
          <div class="stat-label">本周扫描任务</div>
          <div class="stat-trend" :style="{ color: '#fff' }">
            <span class="trend-up">↑ {{ statTrend.scanTrend || 0 }}</span>
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%)">
          <div class="stat-value">{{ overview.pendingReviewCount || 0 }}</div>
          <div class="stat-label">待审核字段</div>
          <div class="stat-trend" :style="{ color: '#fff' }">
            <span class="trend-down" v-if="(overview.pendingReviewCount || 0) > 0">⚠ 待处理</span>
            <span class="trend-ok" v-else>✓ 已清零</span>
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="4">
        <div class="stat-card" :style="{ background: sensitiveAlertCardBg }">
          <div class="stat-value">{{ sensitiveAlertCount }}</div>
          <div class="stat-label">敏感告警(本周)</div>
          <div class="stat-trend" :style="{ color: '#fff' }">
            <span class="trend-down" v-if="sensitiveAlertCount > 0">⚠ 待处理</span>
            <span class="trend-ok" v-else>✓ 正常</span>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="[20, 20]" class="chart-row">
      <a-col :xs="24" :lg="15">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">敏感字段等级趋势</span>
            <a-space>
              <a-select v-model:value="trendDays" style="width: 100px" size="small" @change="loadTrend">
                <a-select-option :value="7">近7天</a-select-option>
                <a-select-option :value="30">近30天</a-select-option>
                <a-select-option :value="90">近90天</a-select-option>
              </a-select>
            </a-space>
          </div>
          <!-- 等级图例 -->
          <div class="chart-legend">
            <span v-for="item in levelLegend" :key="item.code" class="legend-item">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              {{ item.label }}
            </span>
          </div>
          <!-- ECharts 图表 -->
          <div ref="trendChartRef" class="chart-container"></div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="9">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">敏感资产分布</span>
            <a-space>
              <a-radio-group v-model:value="distView" size="small">
                <a-radio-button value="level">按等级</a-radio-button>
                <a-radio-button value="class">按分类</a-radio-button>
              </a-radio-group>
            </a-space>
          </div>
          <div ref="pieChartRef" class="chart-container" style="height: 280px"></div>
        </div>
      </a-col>
    </a-row>

    <!-- 下方列表 -->
    <a-row :gutter="[20, 20]" class="table-row">
      <a-col :xs="24" :lg="12">
        <div class="table-card">
          <div class="card-header">
            <span class="card-title">敏感字段待审核清单</span>
            <a-space>
              <span class="pending-count">共 <strong>{{ overview.pendingReviewCount || 0 }}</strong> 项</span>
              <a-button type="primary" size="small" @click="batchReview('APPROVED')" :disabled="selectedPendingKeys.length === 0">
                批量通过 ({{ selectedPendingKeys.length }})
              </a-button>
              <RouterLink to="/security/sensitivity-manage" class="more-link">
                查看全部 →
              </RouterLink>
            </a-space>
          </div>
          <a-table
            :columns="pendingColumns"
            :data-source="pendingList"
            :loading="pendingLoading"
            :pagination="pendingPagination"
            :row-selection="{ selectedRowKeys: selectedPendingKeys, onChange: (keys: any) => selectedPendingKeys = keys }"
            :row-key="(r: any) => r.id"
            size="small"
            :scroll="{ x: 700 }"
            @change="handlePendingTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'level'">
                <a-tag :color="getLevelColor(record.sensitivityLevel)" style="font-weight: 500">
                  {{ record.levelName || record.sensitivityLevel || '-' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'confidence'">
                <a-progress :percent="Math.round(record.confidence || 0)" size="small"
                  :stroke-color="getConfidenceColor(record.confidence)" style="width: 80px" />
              </template>
              <template v-if="column.key === 'action'">
                <a-space size="small">
                  <a-button type="link" size="small" @click="openReviewModal(record)">审核</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="table-card">
          <div class="card-header">
            <span class="card-title">敏感等级统计</span>
          </div>
          <div class="level-stats">
            <div class="level-stat-item" style="border-left: 4px solid #F5222D">
              <div class="level-stat-value" style="color: #F5222D">{{ overview.l4Count || 0 }}</div>
              <div class="level-stat-label">🔴 L4 机密</div>
            </div>
            <div class="level-stat-item" style="border-left: 4px solid #FA8C16">
              <div class="level-stat-value" style="color: #FA8C16">{{ overview.l3Count || 0 }}</div>
              <div class="level-stat-label">🟠 L3 敏感</div>
            </div>
            <div class="level-stat-item" style="border-left: 4px solid #1677FF">
              <div class="level-stat-value" style="color: #1677FF">{{ overview.l2Count || 0 }}</div>
              <div class="level-stat-label">🔵 L2 内部</div>
            </div>
            <div class="level-stat-item" style="border-left: 4px solid #52C41A">
              <div class="level-stat-value" style="color: #52C41A">{{ overview.l1Count || 0 }}</div>
              <div class="level-stat-label">🟢 L1 公开</div>
            </div>
          </div>
          <div class="level-progress">
            <div class="level-bar">
              <div class="level-bar-fill" style="background: #52C41A" :style="{ width: levelL1Percent + '%' }"></div>
              <div class="level-bar-fill" style="background: #1677FF" :style="{ width: levelL2Percent + '%' }"></div>
              <div class="level-bar-fill" style="background: #FA8C16" :style="{ width: levelL3Percent + '%' }"></div>
              <div class="level-bar-fill" style="background: #F5222D" :style="{ width: levelL4Percent + '%' }"></div>
            </div>
            <div class="level-bar-labels">
              <span>L1 {{ levelL1Percent }}%</span>
              <span>L2 {{ levelL2Percent }}%</span>
              <span>L3 {{ levelL3Percent }}%</span>
              <span>L4 {{ levelL4Percent }}%</span>
            </div>
          </div>
        </div>

        <!-- 敏感告警列表 -->
        <div class="table-card" style="margin-top: 16px">
          <div class="card-header">
            <span class="card-title">敏感告警记录</span>
            <a-space>
              <a-badge :count="sensitiveAlertCount" :overflow-count="99" />
              <RouterLink to="/monitor/alert-record" class="more-link">
                查看全部 →
              </RouterLink>
            </a-space>
          </div>
          <div v-if="sensitiveAlerts.length === 0 && !sensitiveAlertLoading" class="alert-empty">
            <CheckCircleOutlined style="font-size: 28px; color: #52C41A; margin-bottom: 8px" />
            <div style="font-size: 13px; color: #52C41A; font-weight: 500">暂无敏感告警</div>
            <div style="font-size: 12px; color: #8C8C8C; margin-top: 4px">系统运行正常</div>
          </div>
          <div v-else class="alert-list">
            <div
              v-for="alert in sensitiveAlerts"
              :key="alert.id"
              class="alert-item"
              :class="'alert-level-' + getAlertLevelClass(alert.alertLevel)"
              @click="openAlertDetail(alert)"
            >
              <div class="alert-left">
                <div class="alert-level-badge" :class="'badge-' + getAlertLevelClass(alert.alertLevel)">
                  {{ getAlertLevelName(alert.alertLevel) }}
                </div>
              </div>
              <div class="alert-body">
                <div class="alert-title">{{ alert.alertTitle || '敏感数据告警' }}</div>
                <div class="alert-meta">
                  <span v-if="alert.sensitivityLevel">
                    <a-tag :color="getLevelColor(alert.sensitivityLevel)" style="font-size: 11px; margin-right: 4px">
                      {{ alert.sensitivityLevel }}
                    </a-tag>
                  </span>
                  <span v-if="alert.sensitivityTable">{{ alert.sensitivityTable }}</span>
                  <span v-if="alert.sensitivityColumn">.{{ alert.sensitivityColumn }}</span>
                  <span class="alert-sep">·</span>
                  <span>{{ formatDateTime(alert.createTime) }}</span>
                </div>
              </div>
              <div class="alert-right">
                <a-tag :color="getAlertStatusColor(alert.status)" style="font-size: 11px">
                  {{ getAlertStatusName(alert.status) }}
                </a-tag>
              </div>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 新字段发现告警 -->
    <a-row :gutter="[20, 20]" class="table-row">
      <a-col :xs="24" :lg="12">
        <div class="table-card">
          <div class="card-header">
            <span class="card-title">⚠️ 待扫描新字段</span>
            <a-space>
              <a-badge :count="newColumnPendingCount" :overflow-count="99" :number-style="{ backgroundColor: '#FA8C16' }" />
              <a-button type="primary" size="small" @click="openNewColumnAlertModal" :loading="newColumnLoading">
                立即扫描
              </a-button>
              <a-button size="small" @click="newColumnDrawerVisible = true">
                查看详情
              </a-button>
            </a-space>
          </div>
          <div v-if="newColumnPendingCount === 0 && !newColumnLoading" style="text-align:center;padding:24px 0;color:#52C41A">
            <CheckCircleOutlined style="font-size:28px;margin-bottom:8px" />
            <div style="font-size:13px;font-weight:500">暂无待扫描新字段</div>
            <div style="font-size:12px;color:#8C8C8C;margin-top:4px">所有字段已完成敏感字段识别</div>
          </div>
          <div v-else-if="newColumnAlertList.length === 0 && newColumnPendingCount > 0" style="text-align:center;padding:24px 0">
            <a-spin />
            <div style="margin-top:8px;font-size:12px;color:#8C8C8C">正在加载...</div>
          </div>
          <div v-else class="alert-list">
            <div
              v-for="alert in newColumnAlertList.slice(0, 5)"
              :key="alert.id"
              class="alert-item alert-level-warn"
              @click="openNewColumnAlertDetail(alert)"
            >
              <div class="alert-left">
                <div class="alert-level-badge badge-warn">新字段</div>
              </div>
              <div class="alert-body">
                <div class="alert-title">{{ alert.tableName }}.{{ alert.columnName }}</div>
                <div class="alert-meta">
                  <span v-if="alert.dataType">{{ alert.dataType }}</span>
                  <span class="alert-sep" v-if="alert.dataType">·</span>
                  <span>{{ formatDateTime(alert.createdAt) }}</span>
                  <span class="alert-sep">·</span>
                  <a-tag :color="alert.status === 'PENDING' ? '#FA8C16' : '#52C41A'" style="font-size:11px">
                    {{ alert.status === 'PENDING' ? '待扫描' : '已扫描' }}
                  </a-tag>
                </div>
              </div>
            </div>
            <div v-if="newColumnPendingCount > 5" style="text-align:center;padding:8px 0">
              <a-button type="link" size="small" @click="newColumnDrawerVisible = true">
                查看全部 {{ newColumnPendingCount }} 项 →
              </a-button>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-modal
      v-model:open="reviewModalVisible"
      title="审核敏感字段"
      width="480"
      @ok="doReview"
    >
      <a-form :label-col="{ span: 6 }" :model="reviewForm">
        <a-form-item label="字段">
          <code>{{ reviewRecord.columnName }}</code> ({{ reviewRecord.tableName }})
        </a-form-item>
        <a-form-item label="数据源">{{ reviewRecord.dsName || '-' }}</a-form-item>
        <a-form-item label="敏感等级">
          <a-tag :color="getLevelColor(reviewRecord.sensitivityLevel)">
            {{ reviewRecord.levelName || reviewRecord.sensitivityLevel || '-' }}
          </a-tag>
        </a-form-item>
        <a-form-item label="审核结果" required>
          <a-radio-group v-model:value="reviewForm.status">
            <a-radio value="APPROVED" style="color: #52C41A">
              <CheckCircleOutlined /> 通过
            </a-radio>
            <a-radio value="REJECTED" style="color: #F5222D">
              <CloseCircleOutlined /> 驳回
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审核意见">
          <a-textarea v-model:value="reviewForm.comment" placeholder="可选，填写审核意见" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 告警详情抽屉 -->
    <a-drawer
      v-model:open="alertDrawerVisible"
      :title="'敏感告警详情 — ' + (currentAlert?.alertNo || '')"
      :width="640"
      placement="right"
      :destroy-on-close="true"
    >
      <template v-if="currentAlert">
        <div class="execution-info-grid">
          <div class="exec-info-card">
            <div class="exec-info-label">告警编号</div>
            <div class="exec-info-value mono-value">{{ currentAlert.alertNo || '-' }}</div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">告警等级</div>
            <div class="exec-info-value">
              <a-tag :color="getAlertLevelColor(currentAlert.alertLevel)">
                {{ getAlertLevelName(currentAlert.alertLevel) }}
              </a-tag>
            </div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">敏感等级</div>
            <div class="exec-info-value">
              <a-tag :color="getLevelColor(currentAlert.sensitivityLevel)">
                {{ currentAlert.sensitivityLevel || '-' }}
              </a-tag>
            </div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">告警状态</div>
            <div class="exec-info-value">
              <a-tag :color="getAlertStatusColor(currentAlert.status)">
                {{ getAlertStatusName(currentAlert.status) }}
              </a-tag>
            </div>
          </div>
          <div class="exec-info-card" style="grid-column: span 2" v-if="currentAlert.sensitivityTable">
            <div class="exec-info-label">涉及字段</div>
            <div class="exec-info-value">
              {{ currentAlert.sensitivityTable }}
              <span v-if="currentAlert.sensitivityColumn">.{{ currentAlert.sensitivityColumn }}</span>
            </div>
          </div>
          <div class="exec-info-card" v-if="currentAlert.scanBatchNo" style="grid-column: span 2">
            <div class="exec-info-label">关联批次</div>
            <div class="exec-info-value mono-value">{{ currentAlert.scanBatchNo }}</div>
          </div>
        </div>

        <div class="alert-content-section">
          <div class="section-title">告警内容</div>
          <div class="alert-content-box">
            <div class="alert-title-text">{{ currentAlert.alertTitle || '敏感数据告警' }}</div>
            <div v-if="currentAlert.alertContent" class="alert-content-text">
              {{ currentAlert.alertContent }}
            </div>
          </div>
        </div>

        <div v-if="currentAlert.triggerValue && currentAlert.thresholdValue" class="alert-trigger-section">
          <div class="section-title">触发信息</div>
          <div class="trigger-info-grid">
            <div class="trigger-item">
              <div class="trigger-label">触发值</div>
              <div class="trigger-value text-danger">{{ currentAlert.triggerValue }}</div>
            </div>
            <div class="trigger-item">
              <div class="trigger-label">阈值</div>
              <div class="trigger-value">{{ currentAlert.thresholdValue }}</div>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="alertDrawerVisible = false">关闭</a-button>
          <a-button
            v-if="currentAlert && currentAlert.status !== 'RESOLVED'"
            type="primary"
            @click="handleResolveAlert"
          >
            <template #icon><CheckOutlined /></template>
            标记已解决
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 新字段告警详情抽屉 -->
    <a-drawer
      v-model:open="newColumnDrawerVisible"
      title="待扫描新字段列表"
      :width="720"
      placement="right"
      :destroy-on-close="true"
    >
      <div style="margin-bottom: 16px">
        <a-space wrap>
          <a-select v-model:value="newColumnFilter.status" placeholder="状态" allow-clear style="width:120px" @change="loadNewColumnAlerts">
            <a-select-option value="PENDING">待处理</a-select-option>
            <a-select-option value="SCANNED">已扫描</a-select-option>
            <a-select-option value="DISMISSED">已忽略</a-select-option>
          </a-select>
          <a-input v-model:value="newColumnFilter.tableName" placeholder="表名搜索" allow-clear style="width:160px" @change="loadNewColumnAlerts" />
          <a-button type="primary" @click="loadNewColumnAlerts">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </a-space>
      </div>
      <a-table
        :columns="newColumnAlertColumns"
        :data-source="newColumnAlertList"
        :loading="newColumnLoading"
        :pagination="newColumnPagination"
        :row-key="(r: any) => r.id"
        @change="handleNewColumnTableChange"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'PENDING' ? '#FA8C16' : record.status === 'SCANNED' ? '#52C41A' : '#8C8C8C'">
              {{ record.status === 'PENDING' ? '待扫描' : record.status === 'SCANNED' ? '已扫描' : '已忽略' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-button
                v-if="record.status === 'PENDING'"
                type="link"
                size="small"
                style="color: #52C41A"
                @click="doScanNewColumnAlert(record)"
              >立即扫描</a-button>
              <a-button
                v-if="record.status === 'PENDING'"
                type="link"
                size="small"
                style="color: #8C8C8C"
                @click="doDismissNewColumnAlert(record)"
              >忽略</a-button>
              <a-button
                v-if="record.scanBatchNo"
                type="link"
                size="small"
                @click="handleViewScanBatch(record.scanBatchNo)"
              >查看批次</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-drawer>

    <!-- 新字段详情弹窗 -->
    <a-modal
      v-model:open="newColumnDetailModalVisible"
      title="新字段详情"
      width="520"
      :footer="null"
    >
      <template v-if="newColumnDetailRecord">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="数据源ID">{{ newColumnDetailRecord.dsId }}</a-descriptions-item>
          <a-descriptions-item label="表名">{{ newColumnDetailRecord.tableName }}</a-descriptions-item>
          <a-descriptions-item label="字段名">{{ newColumnDetailRecord.columnName }}</a-descriptions-item>
          <a-descriptions-item label="数据类型">{{ newColumnDetailRecord.dataType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="字段注释">{{ newColumnDetailRecord.columnComment || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="newColumnDetailRecord.status === 'PENDING' ? '#FA8C16' : '#52C41A'">
              {{ newColumnDetailRecord.status === 'PENDING' ? '待扫描' : newColumnDetailRecord.status === 'SCANNED' ? '已扫描' : '已忽略' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="发现时间">{{ formatDateTime(newColumnDetailRecord.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="扫描批次" v-if="newColumnDetailRecord.scanBatchNo">
            <code>{{ newColumnDetailRecord.scanBatchNo }}</code>
          </a-descriptions-item>
          <a-descriptions-item label="处理意见" v-if="newColumnDetailRecord.handleComment">
            {{ newColumnDetailRecord.handleComment }}
          </a-descriptions-item>
        </a-descriptions>
        <div style="margin-top: 16px; text-align: right">
          <a-space>
            <a-button @click="newColumnDetailModalVisible = false">关闭</a-button>
            <a-button
              v-if="newColumnDetailRecord.status === 'PENDING'"
              type="primary"
              style="background: #52C41A; border-color: #52C41A"
              @click="doScanNewColumnAlert(newColumnDetailRecord); newColumnDetailModalVisible = false"
            >
              <template #icon><CheckCircleOutlined /></template>
              立即扫描
            </a-button>
            <a-button
              v-if="newColumnDetailRecord.status === 'PENDING'"
              @click="doDismissNewColumnAlert(newColumnDetailRecord); newColumnDetailModalVisible = false"
            >
              忽略
            </a-button>
          </a-space>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, CheckCircleOutlined, CloseCircleOutlined
} from '@ant-design/icons-vue'
import {
  getSecurityOverview,
  getHealthScore,
  pageSensitivity,
  batchReviewSensitivity,
  reviewSensitivity,
  getSecurityOverviewTrend,
  type SecurityOverviewVO,
  type HealthScoreVO,
  type SecColumnSensitivityVO,
  type ReviewStatus,
  type SecNewColumnAlert,
  pageNewColumnAlerts,
  countPendingNewColumnAlerts,
  scanNewColumnAlert,
  dismissNewColumnAlert
} from '@/api/security'
import { alertRecordApi, type AlertRecord } from '@/api/monitor'
import * as echarts from 'echarts'

// ========== 状态 ==========
const loading = ref(false)
const pendingLoading = ref(false)
const sensitiveAlertLoading = ref(false)
const trendDays = ref(30)
const distView = ref('level')
const currentTime = ref('')
const reviewModalVisible = ref(false)
const reviewRecord = ref<any>({})
const reviewForm = reactive({ status: 'APPROVED' as ReviewStatus, comment: '' })
const selectedPendingKeys = ref<number[]>([])

// SENSITIVE 告警
const sensitiveAlertCount = ref(0)
const sensitiveAlerts = ref<AlertRecord[]>([])
const alertDrawerVisible = ref(false)
const currentAlert = ref<AlertRecord | null>(null)

// 新字段发现告警
const newColumnAlertList = ref<SecNewColumnAlert[]>([])
const newColumnPendingCount = ref(0)
const newColumnLoading = ref(false)
const newColumnDrawerVisible = ref(false)
const newColumnDetailModalVisible = ref(false)
const newColumnDetailRecord = ref<SecNewColumnAlert | null>(null)
const newColumnFilter = reactive({
  status: '',
  tableName: ''
})
const newColumnPagination = reactive({
  current: 1, pageSize: 10, total: 0
})
const newColumnAlertColumns = [
  { title: '表名', dataIndex: 'tableName', key: 'tableName', width: 140, ellipsis: true },
  { title: '字段名', dataIndex: 'columnName', key: 'columnName', width: 120, ellipsis: true },
  { title: '数据类型', dataIndex: 'dataType', key: 'dataType', width: 90 },
  { title: '状态', key: 'status', width: 80 },
  { title: '发现时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 140 }
]

const overview = reactive<any>({})
const healthScore = reactive<any>({})
const pendingList = ref<any[]>([])
const statTrend = reactive({
  sensitiveCountTrend: 0,
  coverageTrend: 0,
  scanTrend: 0
})

const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()

const pendingPagination = reactive({
  current: 1, pageSize: 5, total: 0, showSizeChanger: false
})

let trendChart: any = null
let pieChart: any = null

// ========== 等级配置 ==========
const levelLegend = [
  { code: 'L4', label: 'L4 机密', color: '#F5222D' },
  { code: 'L3', label: 'L3 敏感', color: '#FA8C16' },
  { code: 'L2', label: 'L2 内部', color: '#1677FF' },
  { code: 'L1', label: 'L1 公开', color: '#52C41A' }
]

// ========== 百分比计算 ==========
const totalLevel = computed(() => {
  return (overview.l4Count || 0) + (overview.l3Count || 0) +
         (overview.l2Count || 0) + (overview.l1Count || 0)
})

const levelL4Percent = computed(() => totalLevel.value > 0
  ? Math.round((overview.l4Count || 0) / totalLevel.value * 100) : 0)
const levelL3Percent = computed(() => totalLevel.value > 0
  ? Math.round((overview.l3Count || 0) / totalLevel.value * 100) : 0)
const levelL2Percent = computed(() => totalLevel.value > 0
  ? Math.round((overview.l2Count || 0) / totalLevel.value * 100) : 0)
const levelL1Percent = computed(() => totalLevel.value > 0
  ? Math.round((overview.l1Count || 0) / totalLevel.value * 100) : 0)

// ========== 健康分颜色 ==========
const healthScoreColor = computed(() => {
  const score = healthScore.score || 0
  if (score >= 90) return '#52C41A'
  if (score >= 70) return '#1677FF'
  if (score >= 50) return '#FAAD14'
  return '#F5222D'
})

const sensitiveAlertCardBg = computed(() => {
  if (sensitiveAlertCount.value > 10) return 'linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%)'
  if (sensitiveAlertCount.value > 0) return 'linear-gradient(135deg, #FA8C16 0%, #FFD591 100%)'
  return 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)'
})

// ========== 表格列定义 ==========
const pendingColumns = [
  { title: '数据源', dataIndex: 'dsName', key: 'dsName', width: 100, ellipsis: true },
  { title: '表名', dataIndex: 'tableName', key: 'tableName', width: 100, ellipsis: true },
  { title: '字段名', dataIndex: 'columnName', key: 'columnName', width: 110, ellipsis: true },
  { title: '等级', key: 'level', width: 80 },
  { title: '置信度', key: 'confidence', width: 100 },
  { title: '操作', key: 'action', width: 60 }
]

// ========== 辅助方法 ==========
function getLevelColor(level?: string) {
  const map: Record<string, string> = {
    'L4': '#F5222D', 'L3': '#FA8C16', 'L2': '#1677FF', 'L1': '#52C41A',
    'HIGHLY_SENSITIVE': '#F5222D', 'SENSITIVE': '#FA8C16',
    'INTERNAL': '#1677FF', 'NORMAL': '#52C41A'
  }
  return map[level || ''] || 'default'
}

function getConfidenceColor(confidence?: number) {
  if (!confidence) return '#d9d9d9'
  if (confidence >= 80) return '#52C41A'
  if (confidence >= 50) return '#FAAD14'
  return '#F5222D'
}

function formatTime() {
  const now = new Date()
  currentTime.value = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`
}

// ========== 数据加载 ==========
async function loadOverview() {
  try {
    const res = await getSecurityOverview()
    if (res.data?.success !== false) {
      Object.assign(overview, res.data?.data || {})
    }
  } catch { /* silent */ }
}

async function loadHealthScore() {
  try {
    const res = await getHealthScore()
    if (res.data?.success !== false) {
      Object.assign(healthScore, res.data?.data || {})
    }
  } catch { /* silent */ }
}

async function loadPendingList() {
  pendingLoading.value = true
  try {
    const res = await pageSensitivity({
      pageNum: pendingPagination.current,
      pageSize: pendingPagination.pageSize,
      reviewStatus: 'PENDING' as any
    })
    if (res.data?.success !== false) {
      const page = res.data?.data as any
      pendingList.value = page?.records || []
      pendingPagination.total = page?.total || 0
    }
  } catch { message.error('加载待审核列表失败') }
  finally { pendingLoading.value = false }
}

function handlePendingTableChange(pagination: any) {
  pendingPagination.current = pagination.current
  pendingPagination.pageSize = pagination.pageSize
  loadPendingList()
}

async function loadTrend() {
  try {
    const res = await getSecurityOverviewTrend(trendDays.value)
    const data = res.data?.data
    if (!data || !data.trendData || data.trendData.length === 0) {
      // 无数据时生成模拟趋势
      generateMockTrend()
      return
    }

    const labels: string[] = []
    const l4Data: number[] = []
    const l3Data: number[] = []
    const l2Data: number[] = []
    const l1Data: number[] = []

    for (const day of data.trendData) {
      const date = new Date(day.date)
      labels.push(`${date.getMonth()+1}/${date.getDate()}`)
      l4Data.push(day.l4Count || 0)
      l3Data.push(day.l3Count || 0)
      l2Data.push(day.l2Count || 0)
      l1Data.push(day.l1Count || 0)
    }

    if (trendChart) {
      trendChart.setOption({
        xAxis: { data: labels },
        series: [
          { name: 'L4 机密', data: l4Data },
          { name: 'L3 敏感', data: l3Data },
          { name: 'L2 内部', data: l2Data },
          { name: 'L1 公开', data: l1Data }
        ]
      })
    }
  } catch {
    generateMockTrend()
  }
}

function generateMockTrend() {
  const days = trendDays.value
  const labels: string[] = []
  const l4Data: number[] = []
  const l3Data: number[] = []
  const l2Data: number[] = []
  const l1Data: number[] = []

  const now = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    labels.push(`${d.getMonth()+1}/${d.getDate()}`)
    const base = 1 + Math.random() * 0.1
    l4Data.push(Math.max(0, Math.round((overview.l4Count || 5) * base * (1 - i / days * 0.2))))
    l3Data.push(Math.max(0, Math.round((overview.l3Count || 20) * base * (1 - i / days * 0.15))))
    l2Data.push(Math.max(0, Math.round((overview.l2Count || 50) * base * (1 - i / days * 0.1))))
    l1Data.push(Math.max(0, Math.round((overview.l1Count || 100) * base)))
  }

  if (trendChart) {
    trendChart.setOption({
      xAxis: { data: labels },
      series: [
        { name: 'L4 机密', data: l4Data },
        { name: 'L3 敏感', data: l3Data },
        { name: 'L2 内部', data: l2Data },
        { name: 'L1 公开', data: l1Data }
      ]
    })
  }
}

function loadPieChart() {
  if (!pieChart) return

  let pieData: any[] = []
  if (distView.value === 'level') {
    pieData = [
      { name: 'L4 机密', value: overview.l4Count || 0, itemStyle: { color: '#F5222D' } },
      { name: 'L3 敏感', value: overview.l3Count || 0, itemStyle: { color: '#FA8C16' } },
      { name: 'L2 内部', value: overview.l2Count || 0, itemStyle: { color: '#1677FF' } },
      { name: 'L1 公开', value: overview.l1Count || 0, itemStyle: { color: '#52C41A' } }
    ]
  } else {
    // 按分类分布 - 从 overview.sensitiveFieldDistribution 读取
    const dist = overview.sensitiveFieldDistribution || []
    if (dist.length > 0) {
      pieData = dist.map((item: any, idx: number) => ({
        name: item.classification || item.className || `分类${idx+1}`,
        value: item.count || 0,
        itemStyle: { color: ['#F5222D','#FA8C16','#1677FF','#52C41A','#722ED1','#13C2C2'][idx % 6] }
      }))
    } else {
      pieData = [
        { name: '个人隐私', value: Math.round((overview.l3Count || 10) * 0.6), itemStyle: { color: '#F5222D' } },
        { name: '财务数据', value: Math.round((overview.l3Count || 10) * 0.25), itemStyle: { color: '#FA8C16' } },
        { name: '商业数据', value: Math.round((overview.l2Count || 20) * 0.5), itemStyle: { color: '#1677FF' } },
        { name: '运营数据', value: Math.round((overview.l2Count || 20) * 0.3), itemStyle: { color: '#52C41A' } },
        { name: '其他', value: Math.max(1, (overview.l1Count || 5)), itemStyle: { color: '#722ED1' } }
      ]
    }
  }

  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
      data: pieData
    }]
  })
}

// ========== 批量审核 ==========
async function batchReview(status: ReviewStatus) {
  if (selectedPendingKeys.value.length === 0) {
    message.warning('请先选择要审核的字段')
    return
  }
  try {
    await batchReviewSensitivity(selectedPendingKeys.value, status, '')
    message.success(`已批量${status === 'APPROVED' ? '通过' : '驳回'} ${selectedPendingKeys.value.length} 项`)
    selectedPendingKeys.value = []
    loadPendingList()
    loadOverview()
  } catch { message.error('批量审核失败') }
}

function openReviewModal(record: any) {
  reviewRecord.value = record
  reviewForm.status = 'APPROVED'
  reviewForm.comment = ''
  reviewModalVisible.value = true
}

async function doReview() {
  try {
    await reviewSensitivity(reviewRecord.value.id, reviewForm.status, reviewForm.comment)
    message.success('审核成功')
    reviewModalVisible.value = false
    loadPendingList()
    loadOverview()
  } catch { message.error('审核失败') }
}

// ========== SENSITIVE 告警 ==========
async function loadSensitiveAlerts() {
  sensitiveAlertLoading.value = true
  try {
    const [countRes, listRes] = await Promise.all([
      alertRecordApi.getOverview(),
      alertRecordApi.page({
        pageNum: 1,
        pageSize: 10,
        ruleType: 'SENSITIVE',
        status: 'PENDING'
      })
    ])

    const overviewData: any = countRes?.data || {}
    sensitiveAlertCount.value = overviewData.pendingCount ?? 0

    const pageData: any = listRes?.data || {}
    sensitiveAlerts.value = pageData.records || []
  } catch {
    sensitiveAlertCount.value = 0
    sensitiveAlerts.value = []
  } finally {
    sensitiveAlertLoading.value = false
  }
}

function openAlertDetail(alert: AlertRecord) {
  currentAlert.value = alert
  alertDrawerVisible.value = true
}

async function handleResolveAlert() {
  if (!currentAlert.value?.id) return
  try {
    await alertRecordApi.resolve(currentAlert.value.id)
    message.success('已标记为已解决')
    currentAlert.value.status = 'RESOLVED'
    sensitiveAlerts.value = sensitiveAlerts.value.filter(a => a.id !== currentAlert.value?.id)
    if (sensitiveAlertCount.value > 0) sensitiveAlertCount.value--
  } catch { message.error('操作失败') }
}

function getAlertLevelClass(level: string | undefined) {
  const map: Record<string, string> = {
    INFO: 'info', WARN: 'warn', WARNING: 'warn',
    ERROR: 'error', CRITICAL: 'critical'
  }
  return map[level || ''] || 'warn'
}

function getAlertLevelName(level: string | undefined) {
  const map: Record<string, string> = {
    INFO: '信息', WARN: '警告', WARNING: '警告', ERROR: '错误', CRITICAL: '严重'
  }
  return map[level || ''] || level || '-'
}

function getAlertLevelColor(level: string | undefined) {
  const map: Record<string, string> = {
    INFO: 'blue', WARN: 'warning', WARNING: 'warning',
    ERROR: 'error', CRITICAL: 'error'
  }
  return map[level || ''] || 'default'
}

function getAlertStatusColor(status: string | undefined) {
  const map: Record<string, string> = {
    PENDING: 'warning', SENT: 'blue', READ: 'purple', RESOLVED: 'success'
  }
  return map[status || ''] || 'default'
}

function getAlertStatusName(status: string | undefined) {
  const map: Record<string, string> = {
    PENDING: '待处理', SENT: '已发送', READ: '已读', RESOLVED: '已解决'
  }
  return map[status || ''] || status || '-'
}

function formatDateTime(time: string | undefined) {
  if (!time) return '-'
  try {
    return new Date(time).toLocaleString('zh-CN')
  } catch { return time }
}

// ========== 初始化图表 ==========

function initChartInstances() {

  // 趋势图
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
      legend: { show: false },
      grid: { top: 10, right: 20, bottom: 30, left: 50 },
      xAxis: { type: 'category', data: [], boundaryGap: false, axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', axisLabel: { fontSize: 11 } },
      series: [
        { name: 'L4 机密', type: 'line', smooth: true, data: [], lineStyle: { color: '#F5222D', width: 2 }, itemStyle: { color: '#F5222D' } },
        { name: 'L3 敏感', type: 'line', smooth: true, data: [], lineStyle: { color: '#FA8C16', width: 2 }, itemStyle: { color: '#FA8C16' } },
        { name: 'L2 内部', type: 'line', smooth: true, data: [], lineStyle: { color: '#1677FF', width: 2 }, itemStyle: { color: '#1677FF' } },
        { name: 'L1 公开', type: 'line', smooth: true, data: [], lineStyle: { color: '#52C41A', width: 2 }, itemStyle: { color: '#52C41A' } }
      ]
    })
  }

  // 饼图
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, type: 'scroll' },
      series: [{
        type: 'pie',
        radius: ['45%', '75%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
        data: []
      }]
    })
  }
}

// ========== 加载全部数据 ==========
async function loadAllData() {
  loading.value = true
  try {
    await Promise.all([
      loadOverview(),
      loadHealthScore(),
      loadPendingList(),
      loadSensitiveAlerts(),
      loadNewColumnPendingCount(),
      loadNewColumnAlertList(5)
    ])
    loadTrend()
    loadPieChart()
    formatTime()
  } catch { message.error('加载数据失败') }
  finally { loading.value = false }
}

// ========== 生命周期 ==========
let resizeObserver: any = null

onMounted(() => {
  formatTime()
  initChartInstances()
  loadAllData()

  // 监听窗口大小变化
  resizeObserver = new ResizeObserver(() => {
    trendChart?.resize()
    pieChart?.resize()
  })
  if (trendChartRef.value) resizeObserver.observe(trendChartRef.value)
  if (pieChartRef.value) resizeObserver.observe(pieChartRef.value)
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  trendChart?.dispose()
  pieChart?.dispose()
})

// 监听分布视图切换
watch(distView, () => { nextTick(() => loadPieChart()) })

// ========== 新字段发现 ==========
async function loadNewColumnPendingCount() {
  try {
    const res = await countPendingNewColumnAlerts()
    if (res.data?.success !== false) {
      newColumnPendingCount.value = res.data?.data ?? 0
    }
  } catch { newColumnPendingCount.value = 0 }
}

async function loadNewColumnAlertList(limit = 5) {
  newColumnLoading.value = true
  try {
    const res = await pageNewColumnAlerts({
      pageNum: 1,
      pageSize: limit,
      status: 'PENDING'
    })
    if (res.data?.success !== false) {
      const page: any = res.data?.data ?? {}
      newColumnAlertList.value = page.records ?? []
      if (limit > 5) {
        newColumnPagination.total = page.total ?? 0
      }
    }
  } catch { newColumnAlertList.value = [] }
  finally { newColumnLoading.value = false }
}

async function loadNewColumnAlerts() {
  newColumnLoading.value = true
  try {
    const res = await pageNewColumnAlerts({
      pageNum: newColumnPagination.current,
      pageSize: newColumnPagination.pageSize,
      status: newColumnFilter.status || undefined,
      tableName: newColumnFilter.tableName || undefined
    })
    if (res.data?.success !== false) {
      const page: any = res.data?.data ?? {}
      newColumnAlertList.value = page.records ?? []
      newColumnPagination.total = page.total ?? 0
    }
  } catch { newColumnAlertList.value = [] }
  finally { newColumnLoading.value = false }
}

function handleNewColumnTableChange(pagination: any) {
  newColumnPagination.current = pagination.current
  newColumnPagination.pageSize = pagination.pageSize
  loadNewColumnAlerts()
}

function openNewColumnAlertModal() {
  newColumnDrawerVisible.value = true
  newColumnPagination.current = 1
  loadNewColumnAlerts()
}

function openNewColumnAlertDetail(record: SecNewColumnAlert) {
  newColumnDetailRecord.value = record
  newColumnDetailModalVisible.value = true
}

async function doScanNewColumnAlert(record: SecNewColumnAlert) {
  if (!record.id) return
  try {
    await scanNewColumnAlert(record.id)
    message.success('扫描任务已触发，请稍后刷新查看结果')
    loadNewColumnPendingCount()
    loadNewColumnAlertList(5)
    if (newColumnDrawerVisible.value) loadNewColumnAlerts()
  } catch (e: any) {
    message.error(e?.message ?? '扫描失败')
  }
}

async function doDismissNewColumnAlert(record: SecNewColumnAlert) {
  if (!record.id) return
  try {
    await dismissNewColumnAlert(record.id, 1, '管理员忽略')
    message.success('已忽略该告警')
    loadNewColumnPendingCount()
    loadNewColumnAlertList(5)
    if (newColumnDrawerVisible.value) loadNewColumnAlerts()
  } catch (e: any) {
    message.error(e?.message ?? '操作失败')
  }
}

function handleViewScanBatch(batchNo: string | undefined) {
  if (!batchNo) return
  // 跳转到扫描历史页面查看批次详情
  window.open(`/security/sensitivity-scan?batchNo=${batchNo}`, '_blank')
}
</script>

<style lang="less" scoped>
.page-container {
  padding: 24px;
  min-height: 100%;
  background: #F5F7FA;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .header-icon { flex-shrink: 0; }
  .page-title { font-size: 22px; font-weight: 700; color: #1F1F1F; margin: 0; line-height: 1.2; }
  .page-subtitle { font-size: 13px; color: #8C8C8C; margin: 4px 0 0; }
  .header-right { display: flex; gap: 12px; align-items: center; }
  .update-time { font-size: 12px; color: #8C8C8C; }
}

.stat-row { margin-bottom: 20px; }
.stat-card {
  border-radius: 8px;
  padding: 24px;
  color: #fff;
  min-height: 100px;
  position: relative;
  overflow: hidden;
  cursor: default;
  transition: transform 0.3s, box-shadow 0.3s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  &:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(0,0,0,0.15); }
  .stat-value { font-size: 36px; font-weight: 700; line-height: 1.2; }
  .stat-label { font-size: 14px; opacity: 0.9; margin-top: 6px; }
  .stat-trend { font-size: 12px; opacity: 0.85; margin-top: 8px; }
  .trend-up { opacity: 0.9; }
  .trend-down { opacity: 0.9; }
  .trend-ok { opacity: 0.9; }
  .stat-badge {
    display: inline-block;
    padding: 2px 10px;
    border-radius: 20px;
    font-size: 12px;
    margin-top: 6px;
  }
}

.chart-row { margin-bottom: 20px; }
.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  height: 100%;
  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
  .chart-title { font-size: 16px; font-weight: 600; color: #1F1F1F; }
}
.chart-legend { display: flex; gap: 16px; margin-bottom: 8px; }
.legend-item { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #595959; }
.legend-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.chart-container { width: 100%; height: 240px; }

.table-row { margin-bottom: 20px; }
.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
  .card-title { font-size: 16px; font-weight: 600; color: #262626; }
  .pending-count { font-size: 13px; color: #8c8c8c; }
  .pending-count strong { color: #FAAD14; font-size: 15px; }
  .more-link { font-size: 13px; color: #1677FF; text-decoration: none; &:hover { text-decoration: underline; } }
}

.level-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}
.level-stat-item {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px 16px;
}
.level-stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}
.level-stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.level-progress {
  margin-top: 8px;
}
.level-bar {
  display: flex;
  height: 10px;
  border-radius: 5px;
  overflow: hidden;
  background: #f0f0f0;
}
.level-bar-fill {
  height: 100%;
  transition: width 0.3s;
}
.level-bar-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 11px;
  color: #8c8c8c;
}

/* SENSITIVE 告警列表 */
.alert-empty {
  text-align: center;
  padding: 40px 20px;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
  &:hover { border-color: #E8E8E8; background: #FAFAFA; }

  &.alert-level-info {
    border-left: 3px solid #1677FF;
    background: #E6F7FF;
    &:hover { background: #BAE7FF; }
  }
  &.alert-level-warn, &.alert-level-warning {
    border-left: 3px solid #FA8C16;
    background: #FFFAF0;
    &:hover { background: #FFF7E6; }
  }
  &.alert-level-error {
    border-left: 3px solid #FF4D4F;
    background: #FFF1F0;
    &:hover { background: #FFE7E6; }
  }
  &.alert-level-critical {
    border-left: 3px solid #722ED1;
    background: #F9F0FF;
    &:hover { background: #F0E6FF; }
  }
}

.alert-left {
  flex-shrink: 0;
  .alert-level-badge {
    font-size: 11px;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: 4px;
    white-space: nowrap;
    &.badge-info { background: rgba(22, 119, 255, 0.15); color: #1677FF; }
    &.badge-warn, &.badge-warning { background: rgba(250, 173, 20, 0.15); color: #FA8C16; }
    &.badge-error { background: rgba(255, 77, 79, 0.15); color: #FF4D4F; }
    &.badge-critical { background: rgba(114, 46, 209, 0.15); color: #722ED1; }
  }
}

.alert-body {
  flex: 1;
  min-width: 0;
}

.alert-title {
  font-size: 13px;
  font-weight: 500;
  color: #1F1F1F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-meta {
  font-size: 12px;
  color: #8C8C8C;
  margin-top: 2px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  .alert-sep { color: #D9D9D9; }
}

.alert-right {
  flex-shrink: 0;
}

/* 告警详情抽屉 */
.execution-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 24px;
  .exec-info-card {
    background: #F5F7FA;
    border-radius: 8px;
    padding: 12px 16px;
    .exec-info-label { font-size: 12px; color: #8C8C8C; margin-bottom: 6px; }
    .exec-info-value { font-size: 14px; font-weight: 600; color: #1F1F1F; }
    .mono-value { font-family: 'JetBrains Mono', monospace; }
  }
}

.alert-content-section, .alert-trigger-section {
  margin-bottom: 24px;
  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #1F1F1F;
    margin-bottom: 12px;
  }
}

.alert-content-box {
  background: #FFF7E6;
  border: 1px solid #FFE58F;
  border-radius: 8px;
  padding: 16px;
  .alert-title-text { font-size: 15px; font-weight: 600; color: #FA8C16; margin-bottom: 8px; }
  .alert-content-text { font-size: 13px; color: #333; line-height: 1.8; }
}

.trigger-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  .trigger-item {
    background: #F5F7FA;
    border-radius: 8px;
    padding: 12px 16px;
    .trigger-label { font-size: 12px; color: #8C8C8C; margin-bottom: 6px; }
    .trigger-value { font-size: 18px; font-weight: 700; color: #1F1F1F; }
    .text-danger { color: #FF4D4F; }
  }
}
</style>
