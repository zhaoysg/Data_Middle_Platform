<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#grad3)"/>
            <path d="M8 8H16M8 12H16M8 16H13" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="17" cy="16" r="3" stroke="white" stroke-width="1.5" fill="none"/>
            <path d="M16 15L15 14" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <defs>
              <linearGradient id="grad3" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#9254DE"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">质检方案</h1>
          <p class="page-subtitle">绑定表范围与规则，支持手动 / 定时 / 调度三种触发方式</p>
        </div>
      </div>
      <div class="header-right">
        <a-button type="primary" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>
          新建方案
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="6" v-for="stat in statCards" :key="stat.label">
        <div class="stat-mini-card" :style="{ background: stat.bg }">
          <div class="mini-value">{{ stat.value }}</div>
          <div class="mini-label">{{ stat.label }}</div>
        </div>
      </a-col>
    </a-row>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <a-space wrap>
        <a-input
          v-model:value="searchName"
          placeholder="搜索方案名称"
          style="width: 200px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="filterLayer"
          placeholder="数据层"
          style="width: 120px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部层</a-select-option>
          <a-select-option value="ODS">ODS</a-select-option>
          <a-select-option value="DWD">DWD</a-select-option>
          <a-select-option value="DWS">DWS</a-select-option>
          <a-select-option value="ADS">ADS</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterStatus"
          placeholder="方案状态"
          style="width: 120px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="DRAFT">草稿</a-select-option>
          <a-select-option value="PUBLISHED">已发布</a-select-option>
          <a-select-option value="DISABLED">已禁用</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterTrigger"
          placeholder="触发方式"
          style="width: 130px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="MANUAL">手动触发</a-select-option>
          <a-select-option value="SCHEDULE">定时触发</a-select-option>
          <a-select-option value="API">API触发</a-select-option>
        </a-select>
        <a-button @click="resetFilters">
          <template #icon><ReloadOutlined /></template>
          重置
        </a-button>
      </a-space>
    </div>

    <!-- 方案卡片列表 -->
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :sm="12" :lg="8" v-for="plan in tableData" :key="plan.id">
        <div class="plan-card" :class="{ 'plan-card-disabled': plan.status === 'DISABLED' }">
          <div class="plan-card-header">
            <div class="plan-header-left">
              <span class="plan-status-dot" :class="getStatusClass(plan.status)"></span>
              <span class="plan-name">{{ plan.planName }}</span>
            </div>
            <div class="plan-header-right">
              <a-dropdown>
                <a-button type="text" size="small" @click.stop>
                  <MoreOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item
                      v-if="plan.status === 'DRAFT' || plan.status === 'DISABLED'"
                      key="publish"
                      @click="handlePublish(plan)"
                    >
                      <CloudUploadOutlined /> {{ plan.status === 'DISABLED' ? '重新发布' : '发布方案' }}
                    </a-menu-item>
                    <a-menu-item
                      v-if="plan.status === 'PUBLISHED'"
                      key="disable"
                      @click="handleDisable(plan)"
                    >
                      <StopOutlined /> 停用方案
                    </a-menu-item>
                    <a-menu-divider v-if="plan.status === 'DRAFT' || plan.status === 'DISABLED' || plan.status === 'PUBLISHED'" />
                    <a-menu-item
                      key="execute"
                      :disabled="plan.status !== 'PUBLISHED'"
                      @click="handleExecute(plan)"
                    >
                      <PlayCircleOutlined /> 立即执行
                    </a-menu-item>
                    <a-menu-item key="edit" @click="handleEdit(plan)">
                      <EditOutlined /> 编辑方案
                    </a-menu-item>
                    <a-menu-item key="copy" @click="handleCopy(plan)">
                      <CopyOutlined /> 复制方案
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="delete" @click="handleDelete(plan)">
                      <DeleteOutlined /> 删除方案
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </div>
          <div class="plan-card-body">
            <div class="plan-info-row">
              <a-tag :color="getLayerColor(plan.layerCode)">{{ plan.layerCode || '未设置' }}</a-tag>
              <a-tag :color="getTriggerColor(plan.triggerType)">{{ getTriggerLabel(plan.triggerType) }}</a-tag>
              <a-tag :color="plan.status === 'PUBLISHED' ? 'success' : plan.status === 'DRAFT' ? 'warning' : 'default'">
                {{ getStatusText(plan.status) }}
              </a-tag>
            </div>
            <div class="plan-metrics">
              <div class="plan-metric">
                <span class="metric-value">{{ plan.ruleCount || 0 }}</span>
                <span class="metric-label">规则数</span>
              </div>
              <div class="plan-metric">
                <span class="metric-value">{{ plan.tableCount || 0 }}</span>
                <span class="metric-label">涉及表</span>
              </div>
              <div class="plan-metric">
                <span
                  class="metric-value"
                  :style="{ color: planLastScore(plan) != null ? getScoreColor(planLastScore(plan)!) : '#999' }"
                >
                  {{ formatPlanScore(plan) }}
                </span>
                <span class="metric-label">最近评分</span>
              </div>
              <div class="plan-metric" v-if="plan.sensitivityLevel">
                <span
                  class="metric-value"
                  :style="{ color: getSensitivityTextColor(plan.sensitivityLevel) }"
                >
                  {{ plan.sensitivityLevel }}
                </span>
                <span class="metric-label">敏感等级</span>
              </div>
              <div class="plan-metric" v-if="plan.sensitivityComplianceRate != null">
                <span
                  class="metric-value"
                  :style="{ color: getComplianceRateColor(plan.sensitivityComplianceRate) }"
                >
                  {{ plan.sensitivityComplianceRate }}%
                </span>
                <span class="metric-label">敏感合规率</span>
              </div>
            </div>
            <!-- 实时执行进度条 -->
            <div v-if="executingIds.includes(plan.id!)" class="execution-progress">
              <div class="progress-label">
                <SyncOutlined :spin="true" />
                执行中
                <template v-if="getExecProgress(plan.id!) as any">
                  <template v-if="getExecProgress(plan.id!)!.totalRules > 0">
                    {{ getExecProgress(plan.id!)!.passedRules + getExecProgress(plan.id!)!.failedRules }}/{{ getExecProgress(plan.id!)!.totalRules }} 规则
                  </template>
                  <template v-else>正在启动...</template>
                </template>
              </div>
              <div v-if="getExecProgress(plan.id!)?.currentRuleName && !getExecProgress(plan.id!)?.pollingInterrupted" class="progress-current-rule text-muted">
                正在执行：{{ getExecProgress(plan.id!)!.currentRuleName }}
              </div>
              <div v-if="getExecProgress(plan.id!)?.pollingInterrupted" class="progress-current-rule" style="color:#FA8C16">
                ⚠ 轮询中断，请在「执行记录」页刷新状态
              </div>
              <a-progress
                :percent="getExecProgressPercent(plan.id!)"
                :status="getExecProgress(plan.id!)?.status === 'FAILED' ? 'exception' : 'active'"
                :stroke-color="getExecProgress(plan.id!)?.status === 'FAILED' ? '#FF4D4F' : '#722ED1'"
                size="small"
                :show-info="false"
                style="margin-top: 4px"
              />
            </div>
            <div class="plan-desc" v-if="plan.planDesc">{{ plan.planDesc }}</div>
          </div>
          <div class="plan-card-footer">
            <span class="plan-time">{{ plan.updateTime || plan.createTime || '-' }}</span>
            <div class="plan-actions">
              <a-tooltip
                :title="plan.status !== 'PUBLISHED' ? '仅「已发布」可执行。请先在菜单中「发布方案」（需至少绑定 1 条启用规则）。' : ''"
              >
                <a-button
                  type="primary"
                  size="small"
                  :loading="executingIds.includes(plan.id!)"
                  :disabled="plan.status !== 'PUBLISHED'"
                  @click="handleExecute(plan)"
                >
                  <template #icon><PlayCircleOutlined /></template>
                  执行
                </a-button>
              </a-tooltip>
              <a-button size="small" @click="handleView(plan)">
                <template #icon><EyeOutlined /></template>
                详情
              </a-button>
              <a-tooltip title="查看该方案的执行记录与规则级日志">
                <a-button size="small" @click="goPlanExecutionLogs(plan)">
                  <template #icon><FileTextOutlined /></template>
                  日志
                </a-button>
              </a-tooltip>
            </div>
          </div>
        </div>
      </a-col>

      <!-- 空状态 -->
      <a-col :xs="24" v-if="tableData.length === 0 && !loading">
        <div class="empty-card" @click="openCreateModal">
          <div class="empty-icon">
            <PlusCircleOutlined />
          </div>
          <p class="empty-title">还没有配置质检方案</p>
          <p class="empty-desc">开始配置质检方案，为您的数据质量保驾护航</p>
          <a-button type="primary">
            <template #icon><PlusOutlined /></template>
            新建方案
          </a-button>
        </div>
      </a-col>
    </a-row>

    <!-- 分页 -->
    <div style="margin-top: 20px; text-align: right" v-if="pagination.total > 0">
      <a-pagination
        v-model:current="pagination.current"
        v-model:pageSize="pagination.pageSize"
        :total="pagination.total"
        :show-size-changer="true"
        :show-total="(total: number) => `共 ${total} 条`"
        @change="loadData"
      />
    </div>

    <!-- 新增/编辑方案弹窗（5 步向导：字段与规则已合并） -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="min(1680px, 98vw)"
      :footer="null"
      :destroy-on-close="true"
      :body-style="wizardModalBodyStyle"
      wrap-class-name="plan-wizard-modal-wrap"
      @cancel="modalVisible = false"
    >
      <div class="wizard-container" :class="{ 'wizard-container-field-step': currentStep === 2 }">
        <!-- 步骤指示器 -->
        <div class="wizard-steps">
          <div
            v-for="(step, index) in wizardSteps"
            :key="step.key"
            class="wizard-step"
            :class="{
              'step-active': currentStep === index,
              'step-done': currentStep > index,
              'step-pending': currentStep < index
            }"
            @click="currentStep > index && (currentStep = index)"
          >
            <div class="step-circle">
              <CheckOutlined v-if="currentStep > index" />
              <span v-else>{{ index + 1 }}</span>
            </div>
            <div class="step-info">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-desc">{{ step.desc }}</div>
            </div>
            <div v-if="index < wizardSteps.length - 1" class="step-connector">
              <div
                class="connector-line"
                :class="{
                  'connector-done': currentStep > index,
                  'connector-active': currentStep === index
                }"
              />
            </div>
          </div>
        </div>

        <div class="wizard-main">
        <!-- Step 1: 基本信息 -->
        <div v-show="currentStep === 0" class="wizard-content">
          <div class="step-guide">
            <BulbOutlined /> 配置方案的基本信息，包括名称、编码和描述
          </div>
          <a-form
            ref="formRef1"
            :model="formData"
            :rules="formRules1"
            :label-col="{ span: 5 }"
            :wrapper-col="{ span: 18 }"
          >
            <a-form-item label="方案名称" name="planName">
              <a-input
                v-model:value="formData.planName"
                placeholder="新建时自动生成，选表后会按表名更新，也可随时修改"
                @update:value="onPlanNameUserInput"
              >
                <template #suffix>
                  <a-button
                    v-if="!isEdit"
                    type="link"
                    size="small"
                    style="padding: 0; height: auto"
                    @click="regeneratePlanNameFromCode"
                  >
                    <SyncOutlined /> 按编码生成
                  </a-button>
                </template>
              </a-input>
            </a-form-item>
            <a-form-item label="方案编码" name="planCode">
              <a-input
                v-model:value="formData.planCode"
                placeholder="唯一编码，系统自动生成"
                :disabled="isEdit"
              >
                <template #suffix>
                  <a-button type="link" size="small" @click="onGeneratePlanCodeClick" style="padding: 0; height: auto">
                    <SyncOutlined /> 生成
                  </a-button>
                </template>
              </a-input>
            </a-form-item>
            <a-form-item label="方案描述" name="planDesc">
              <a-textarea
                v-model:value="formData.planDesc"
                :rows="3"
                placeholder="描述该方案的作用范围和目标"
              />
            </a-form-item>
          </a-form>
        </div>

        <!-- Step 2: 绑定表范围 -->
        <div v-show="currentStep === 1" class="wizard-content">
          <div class="step-guide">
            <BulbOutlined />
            可先选<strong>绑定维度</strong>（可选）再选<strong>数据源</strong>与<strong>目标表</strong>；下一步将<strong>同页</strong>列出字段并绑定规则（执行 SQL 会带库名/Schema）。
            PostgreSQL 数据源必须先选择 <strong>Schema</strong>。
          </div>
          <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
            <a-form-item label="绑定维度">
              <a-select
                v-model:value="formData.bindDimension"
                placeholder="绑定维度（可选）"
                style="width: 100%"
                allowClear
                @change="onBindDimensionChange"
              >
                <a-select-option value="">
                  <span style="color: #8C8C8C">— 不限制维度 —</span>
                </a-select-option>
                <a-select-option value="SENSITIVITY_LEVEL">
                  <div class="bind-dim-option">
                    <SafetyCertificateOutlined style="color: #FAAD14" />
                    <span>敏感等级</span>
                    <span style="color: #8C8C8C; font-size: 12px">按 L1/L2/L3/L4 筛选规则</span>
                  </div>
                </a-select-option>
              </a-select>
              <div class="field-tip">
                指「按哪一类业务属性去筛规则」，与规则定义里的质量维度（完整性、及时性等）不同。当前可选：按敏感等级过滤待绑定的规则列表。
              </div>
            </a-form-item>

            <a-form-item
              v-if="formData.bindDimension === 'SENSITIVITY_LEVEL'"
              label="敏感等级"
            >
              <a-select
                v-model:value="formData.bindSensitivityLevel"
                placeholder="选择敏感等级"
                style="width: 100%"
                allowClear
                @change="onBindSensitivityLevelChange"
              >
                <a-select-option value="L4">
                  <span style="color: #FF4D4F; font-weight: 700">🔴 L4 机密</span>
                </a-select-option>
                <a-select-option value="L3">
                  <span style="color: #FA8C16; font-weight: 700">🟠 L3 敏感</span>
                </a-select-option>
                <a-select-option value="L2">
                  <span style="color: #1677FF; font-weight: 700">🔵 L2 内部</span>
                </a-select-option>
                <a-select-option value="L1">
                  <span style="color: #52C41A; font-weight: 700">🟢 L1 公开</span>
                </a-select-option>
              </a-select>
              <div class="field-tip">
                选择后将仅显示对应等级的质检规则
              </div>
            </a-form-item>

            <a-form-item label="数据源">
              <a-select
                v-model:value="formData.bindDsId"
                placeholder="请选择数据源"
                :loading="bindDsLoading"
                show-search
                :filter-option="filterDsOption"
                @change="onBindDsChange"
              >
                <a-select-option
                  v-for="ds in dataSourceList"
                  :key="ds.id"
                  :value="ds.id"
                >
                  <span>{{ ds.dsName }}</span>
                  <a-tag size="small" style="margin-left: 6px">{{ ds.dsType }}</a-tag>
                  <a-tag v-if="ds.dataLayer" size="small" color="purple">{{ ds.dataLayer }}</a-tag>
                </a-select-option>
              </a-select>
              <div class="field-tip">表名列表来自实时表与元数据合并（去重）</div>
            </a-form-item>

            <a-form-item label="Schema" v-if="isPostgresBind">
              <a-select
                v-model:value="formData.bindSchema"
                placeholder="请选择 schema"
                :loading="schemaLoading"
                allow-clear
                @change="onBindSchemaChange"
              >
                <a-select-option v-for="s in schemaList" :key="s" :value="s">{{ s }}</a-select-option>
              </a-select>
              <div class="field-tip">
                PostgreSQL 命名空间（Schema）。非 PostgreSQL 数据源无需选择。
              </div>
            </a-form-item>

            <a-form-item label="目标表">
              <a-select
                v-model:value="formData.selectedTableNames"
                mode="multiple"
                allow-clear
                show-search
                :loading="metadataLoading"
                :placeholder="tableSelectPlaceholder"
                :options="tableNameOptions"
                option-filter-prop="label"
                @change="onSelectedTablesChange"
              />
              <div class="field-tip">
                已选择 <strong>{{ formData.selectedTableNames?.length || 0 }}</strong> 张表，
                将为每张表在第 3 步选择字段并绑定规则
              </div>
            </a-form-item>
          </a-form>

          <!-- 敏感等级绑定预览 -->
          <div v-if="formData.bindDimension === 'SENSITIVITY_LEVEL' && formData.bindSensitivityLevel" class="sens-level-preview">
            <div class="preview-header">
              <SafetyCertificateOutlined />
              <span>敏感等级绑定预览</span>
              <a-tag
                size="small"
                :style="{
                  borderColor: getSensitivityTextColor(formData.bindSensitivityLevel),
                  color: getSensitivityTextColor(formData.bindSensitivityLevel),
                }"
              >
                {{ formData.bindSensitivityLevel }}
              </a-tag>
            </div>
            <div class="preview-tip">
              选择后将仅为选中的敏感等级的字段绑定质检规则。
              敏感字段列表基于已采集的元数据（需先在「数据安全 → 敏感字段识别」完成扫描）。
            </div>
            <div v-if="sensitivityFieldPreview.length > 0" class="preview-fields">
              <div class="preview-fields-label">检测到以下 {{ sensitivityFieldPreview.length }} 个敏感字段将参与质检：</div>
              <div class="preview-fields-list">
                <a-tag
                  v-for="field in sensitivityFieldPreview.slice(0, 20)"
                  :key="field.columnName"
                  size="small"
                  :color="getSensitivityTextColor(field.level)"
                >
                  {{ field.tableName }}.{{ field.columnName }}
                </a-tag>
                <a-tag v-if="sensitivityFieldPreview.length > 20" size="small">
                  +{{ sensitivityFieldPreview.length - 20 }} 更多
                </a-tag>
              </div>
            </div>
            <div v-else class="preview-empty">
              <InfoCircleOutlined /> 暂未检测到该等级敏感字段，请确认已完成敏感字段扫描
            </div>
          </div>
        </div>

        <!-- Step 3: 字段 + 规则（扁平映射，一行一字段） -->
        <div v-show="currentStep === 2" class="wizard-content wizard-step-field-rules">
          <div class="step-guide step-guide-lg">
            <BulbOutlined />
            <span>
              数据源 <strong>{{ getBindDsLabel() }}</strong>
              <template v-if="bindCatalogHint"> · {{ bindCatalogHint }}</template>
              。下列为各表<strong>全部已选字段</strong>（进入本步时自动从库中拉全列）；请对需要质检的字段点击「绑定规则」。不需要的字段可用「批量配置列」取消勾选。
            </span>
          </div>

          <div class="field-rules-toolbar" v-if="tableColumnBindings.length > 0">
            <a-space wrap size="middle">
              <a-button type="default" @click="openBatchColumnSelector">
                <template #icon><ColumnWidthOutlined /></template>
                批量配置列
              </a-button>
              <a-popover title="批量绑定规则" trigger="hover" v-if="ruleBindingGroups.length > 0">
                <template #content>
                  <div style="min-width:240px">
                    <div style="margin-bottom:8px;color:#666;font-size:13px">选择目标表和列，再绑定规则：</div>
                    <a-select
                      v-model:value="batchRuleTargetTable"
                      placeholder="选择表"
                      style="width:100%;margin-bottom:8px"
                      @change="batchRuleTargetColumns = []"
                    >
                      <a-select-option v-for="g in ruleBindingGroups" :key="g.tableName" :value="g.tableName">
                        {{ g.tableName }}（{{ g.columns.length }} 列）
                      </a-select-option>
                    </a-select>
                    <a-select
                      v-if="batchRuleTargetTable"
                      v-model:value="batchRuleTargetColumns"
                      placeholder="选择列（可多选）"
                      mode="multiple"
                      style="width:100%;margin-bottom:10px"
                    >
                      <a-select-option v-for="col in (ruleBindingGroups.find(g=>g.tableName===batchRuleTargetTable)?.columns || [])" :key="col" :value="col">
                        {{ col }}
                      </a-select-option>
                    </a-select>
                    <a-button type="primary" block :disabled="!batchRuleTargetTable || !batchRuleTargetColumns.length" @click="openBatchRuleSelector">
                      打开规则选择
                    </a-button>
                  </div>
                </template>
                <a-button>
                  <template #icon><ThunderboltOutlined /></template>
                  批量绑定规则
                </a-button>
              </a-popover>
            </a-space>
          </div>

          <a-table
            class="field-rules-table"
            :data-source="flattenedFieldRuleRows"
            :pagination="{ pageSize: 20, showSizeChanger: true, pageSizeOptions: ['20', '50', '100'] }"
            size="middle"
            bordered
            :scroll="{ x: 1100, y: fieldRulesTableScrollY }"
            :row-key="(r: any) => r.key"
          >
            <a-table-column title="#" width="56" align="center" fixed="left">
              <template #default="{ index }">{{ index + 1 }}</template>
            </a-table-column>
            <a-table-column title="表名" data-index="tableName" width="200" ellipsis fixed="left" />
            <a-table-column title="字段名" data-index="columnName" width="180" ellipsis />
            <a-table-column title="已绑定规则" min-width="320">
              <template #default="{ record }">
                <div class="col-rule-summary" v-if="record.rules?.length">
                  <a-tag
                    v-for="r in record.rules.slice(0, 3)"
                    :key="r.id + (r.targetColumn || '')"
                    :color="r.ruleStrength === 'STRONG' ? 'red' : 'orange'"
                    :title="r.ruleName"
                    closable
                    @close="removeRuleFromBoundField(record.tableName, record.columnName, r.id!)"
                  >{{ r.ruleName }}</a-tag>
                  <a-tag v-if="record.rules.length > 3" color="default">+{{ record.rules.length - 3 }} 条</a-tag>
                </div>
                <span v-else class="text-muted-field">未绑定</span>
              </template>
            </a-table-column>
            <a-table-column title="规则数" width="72" align="center">
              <template #default="{ record }">
                <span
                  v-if="record.totalRules > 0"
                  class="field-rule-count-pill"
                  :title="`已绑定 ${record.totalRules} 条规则`"
                >{{ record.totalRules > 99 ? '99+' : record.totalRules }}</span>
                <span v-else class="field-rule-count-empty" title="未绑定规则">—</span>
              </template>
            </a-table-column>
            <a-table-column title="操作" width="220" align="center" fixed="right">
              <template #default="{ record }">
                <a-space>
                  <a-button type="link" @click="openRuleSelectorForColumn(record.tableName, record.columnName)">
                    {{ record.totalRules > 0 ? '编辑规则' : '绑定规则' }}
                  </a-button>
                  <a-button type="link" danger @click="removeColumnByTable(record.tableName, record.columnName)">
                    移除字段
                  </a-button>
                </a-space>
              </template>
            </a-table-column>
          </a-table>
          <div v-if="tableColumnBindings.length === 0" class="wizard-empty-hint">
            <InboxOutlined />
            <p>请先在「绑定表范围」中选择数据源与表</p>
          </div>
          <div v-else-if="flattenedFieldRuleRows.length === 0" class="wizard-empty-hint">
            <InboxOutlined />
            <p>暂无字段，请点击「批量配置列」或返回上一步检查表选择</p>
          </div>
        </div>

        <!-- Step 4: 触发方式 -->
        <div v-show="currentStep === 3" class="wizard-content">
          <div class="step-guide">
            <BulbOutlined /> 配置方案的执行触发方式及告警通知策略
          </div>
          <a-form
            ref="formRef4"
            :model="formData"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
          >
            <a-form-item label="触发方式" name="triggerType">
              <a-radio-group v-model:value="formData.triggerType">
                <a-radio value="MANUAL">
                  <span class="trigger-option">
                    <span class="trigger-icon-wrap"><ClockCircleOutlined /></span>
                    <span>
                      <strong>手动触发</strong>
                      <span class="trigger-desc">手动点击执行按钮触发检测</span>
                    </span>
                  </span>
                </a-radio>
                <a-radio value="SCHEDULE">
                  <span class="trigger-option">
                    <span class="trigger-icon-wrap"><ScheduleOutlined /></span>
                    <span>
                      <strong>定时触发</strong>
                      <span class="trigger-desc">按 Cron 表达式定时执行</span>
                    </span>
                  </span>
                </a-radio>
                <a-radio value="API">
                  <span class="trigger-option">
                    <span class="trigger-icon-wrap"><ApiOutlined /></span>
                    <span>
                      <strong>API 触发</strong>
                      <span class="trigger-desc">通过外部接口调用触发</span>
                    </span>
                  </span>
                </a-radio>
              </a-radio-group>
            </a-form-item>

            <a-form-item
              label="Cron 表达式"
              name="triggerCron"
              v-if="formData.triggerType === 'SCHEDULE'"
            >
              <a-input v-model:value="formData.triggerCron" placeholder="0 0 2 * * ?">
                <template #suffix>
                  <a-popover title="常用表达式">
                    <template #content>
                      <div class="cron-helper">
                        <div @click="formData.triggerCron = '0 0 2 * * ?'">每天凌晨 2 点：<code>0 0 2 * * ?</code></div>
                        <div @click="formData.triggerCron = '0 30 8 * * ?'">每天 8:30：<code>0 30 8 * * ?</code></div>
                        <div @click="formData.triggerCron = '0 0/30 * * * ?'">每 30 分钟：<code>0 0/30 * * * ?</code></div>
                        <div @click="formData.triggerCron = '0 0 2 ? * 1'">每周一 2 点：<code>0 0 2 ? * 1</code></div>
                      </div>
                    </template>
                    <a-button type="link" size="small">常用<i style="margin-left: 4px">▼</i></a-button>
                  </a-popover>
                </template>
              </a-input>
              <div class="field-tip">6位 Cron 表达式，格式：秒 分 时 日 月 周（？表示任意）</div>
            </a-form-item>

            <a-divider>告警配置</a-divider>

            <a-form-item label="执行成功">
              <a-switch v-model:checked="formData.alertOnSuccess" />
              <span class="switch-label">{{ formData.alertOnSuccess ? '通知' : '不通知' }}</span>
            </a-form-item>

            <a-form-item label="执行失败">
              <a-switch v-model:checked="formData.alertOnFailure" />
              <span class="switch-label">{{ formData.alertOnFailure ? '通知' : '不通知' }}</span>
            </a-form-item>

            <a-form-item label="强规则失败">
              <a-switch v-model:checked="formData.autoBlock" />
              <span class="switch-label">{{ formData.autoBlock ? '阻塞任务' : '仅记录' }}</span>
              <a-tooltip title="强规则失败时是否阻塞下游任务执行">
                <QuestionCircleOutlined style="margin-left: 8px; color: #999" />
              </a-tooltip>
            </a-form-item>
          </a-form>
        </div>

        <!-- Step 5: 预览保存 -->
        <div v-show="currentStep === 4" class="wizard-content">
          <div class="preview-section">
            <div class="preview-title">
              <CheckCircleOutlined /> 方案配置总览
            </div>
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item label="方案名称">{{ formData.planName }}</a-descriptions-item>
              <a-descriptions-item label="方案编码"><code>{{ formData.planCode }}</code></a-descriptions-item>
              <a-descriptions-item label="方案描述">{{ formData.planDesc || '-' }}</a-descriptions-item>
              <a-descriptions-item label="数据源（实例）">{{ getBindDsLabel() }}</a-descriptions-item>
              <a-descriptions-item label="已选表">
                {{ formData.selectedTableNames?.length || 0 }} 张
                <span v-if="formData.bindSchema">，Schema：{{ formData.bindSchema }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="已选字段">
                {{ selectedColumnCount }} 个
                <span style="color: #999; font-size: 12px; margin-left: 8px">
                  （{{ tableColumnBindings.filter(t => t.columns?.length).length }} 张有字段）
                </span>
              </a-descriptions-item>
              <a-descriptions-item v-if="formData.bindDimension === 'SENSITIVITY_LEVEL'" label="敏感等级绑定">
                <span
                  :style="{
                    fontWeight: 700,
                    color: getSensitivityTextColor(formData.bindSensitivityLevel || '')
                  }"
                >
                  <SafetyCertificateOutlined />
                  {{ formData.bindSensitivityLevel }}
                </span>
                <span style="color: #8C8C8C; font-size: 12px; margin-left: 8px">
                  （将对该等级敏感字段执行质检）
                </span>
                <a-button type="link" size="small" @click="currentStep = 1" style="padding: 0; height: auto; margin-left: 8px">
                  点击修改 →
                </a-button>
              </a-descriptions-item>
              <a-descriptions-item label="触发方式">
                <a-tag :color="getTriggerColor(formData.triggerType)">{{ getTriggerLabel(formData.triggerType) }}</a-tag>
                <span v-if="formData.triggerType === 'SCHEDULE' && formData.triggerCron">
                  ，Cron：<code>{{ formData.triggerCron }}</code>
                </span>
                <a-button type="link" size="small" @click="currentStep = 3" style="padding: 0; height: auto; margin-left: 8px">
                  点击修改 →
                </a-button>
              </a-descriptions-item>
              <a-descriptions-item label="绑定规则">
                {{ totalBoundRuleCount }} 条（强规则 {{ totalStrongCount }}，弱规则 {{ totalWeakCount }}）
                <a-button type="link" size="small" @click="currentStep = 2" style="padding: 0; height: auto; margin-left: 8px">
                  点击修改 →
                </a-button>
              </a-descriptions-item>
              <a-descriptions-item label="告警配置">
                成功通知：{{ formData.alertOnSuccess ? '是' : '否' }}，
                失败通知：{{ formData.alertOnFailure ? '是' : '否' }}，
                强规则阻塞：{{ formData.autoBlock ? '是' : '否' }}
              </a-descriptions-item>
            </a-descriptions>
          </div>
        </div>
        </div>

        <!-- 向导底部按钮（固定在弹窗底部可见区，不随步骤内容滚出视口） -->
        <div class="wizard-footer">
          <a-space>
            <a-button v-if="currentStep > 0" @click="currentStep--">
              <template #icon><LeftOutlined /></template>
              上一步
            </a-button>
            <a-button v-if="currentStep < 4" type="primary" @click="nextStep">
              下一步
              <template #icon><RightOutlined /></template>
            </a-button>
            <a-button
              v-if="currentStep === 4"
              type="primary"
              :loading="submitLoading"
              @click="() => handleSubmit(false)"
            >
              <template #icon><CheckOutlined /></template>
              {{ isEdit ? '保存修改' : '创建方案' }}
            </a-button>
            <a-button
              v-if="currentStep === 4 && totalBoundRuleCount > 0"
              type="primary"
              ghost
              :loading="submitLoading"
              @click="() => handleSubmit(true)"
            >
              <template #icon><CloudUploadOutlined /></template>
              {{ isEdit ? '保存并发布' : '创建并发布' }}
            </a-button>
          </a-space>
          <a-space>
            <a-button v-if="currentStep === 4" @click="handleSaveDraft">
              保存草稿
            </a-button>
          </a-space>
        </div>
      </div>
    </a-modal>

    <!-- 方案详情抽屉 -->
    <a-drawer
      v-model:open="viewDrawerVisible"
      title="方案详情"
      :width="600"
      placement="right"
      :destroy-on-close="true"
    >
      <template v-if="viewRecord">
        <div class="detail-section">
          <div class="detail-title">基本信息</div>
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="方案名称">{{ viewRecord.planName }}</a-descriptions-item>
            <a-descriptions-item label="方案编码"><code>{{ viewRecord.planCode }}</code></a-descriptions-item>
            <a-descriptions-item label="方案描述">{{ viewRecord.planDesc || '-' }}</a-descriptions-item>
            <a-descriptions-item label="数据层">
              <a-tag :color="getLayerColor(viewRecord.layerCode)">{{ viewRecord.layerCode || '-' }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="触发方式">
              <a-tag :color="getTriggerColor(viewRecord.triggerType)">{{ getTriggerLabel(viewRecord.triggerType) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-badge :status="getStatusBadge(viewRecord.status)" :text="getStatusText(viewRecord.status)" />
            </a-descriptions-item>
            <a-descriptions-item v-if="viewRecord?.sensitivityLevel" label="敏感等级">
              <span
                class="sensitivity-badge"
                :style="{
                  borderColor: getSensitivityTextColor(viewRecord.sensitivityLevel),
                  color: getSensitivityTextColor(viewRecord.sensitivityLevel),
                }"
              >
                <SafetyCertificateOutlined />
                {{ viewRecord.sensitivityLevel }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item v-if="viewRecord?.sensitivityComplianceRate != null" label="敏感合规率">
              <span
                :style="{ color: getComplianceRateColor(viewRecord.sensitivityComplianceRate), fontWeight: 700 }"
              >
                {{ viewRecord.sensitivityComplianceRate }}%
              </span>
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <div class="detail-section" v-if="viewRecord?.sensitivityComplianceRate != null || viewRecord?.sensitivityLevel">
          <div class="detail-title">
            <SafetyCertificateOutlined /> 敏感字段质检
          </div>
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="敏感等级">
              <a-tag :color="getSensitivityTextColor(viewRecord?.sensitivityLevel)">
                {{ viewRecord?.sensitivityLevel || '-' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="敏感合规率">
              <a-progress
                :percent="viewRecord?.sensitivityComplianceRate || 0"
                :stroke-color="getComplianceRateColor(viewRecord?.sensitivityComplianceRate)"
                size="small"
              />
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <div class="detail-section">
          <div class="detail-title">
            绑定规则
            <span class="detail-count">{{ boundRulesView.length }} 条</span>
          </div>
          <div class="detail-rules">
            <div v-for="(r, index) in boundRulesView" :key="r.ruleId" class="detail-rule-item">
              <span class="detail-rule-order">{{ index + 1 }}</span>
              <span class="detail-rule-name">{{ r.ruleName }}</span>
              <a-tag :color="r.ruleStrength === 'STRONG' ? 'red' : 'orange'" size="small">
                {{ r.ruleStrength === 'STRONG' ? '强' : '弱' }}
              </a-tag>
              <span v-if="r.skipOnFailure" class="skip-tag">跳过</span>
            </div>
            <div v-if="boundRulesView.length === 0" class="detail-empty">暂无绑定规则</div>
          </div>
        </div>
      </template>

      <template #footer>
        <a-space wrap>
          <a-button
            v-if="viewRecord && (viewRecord.status === 'DRAFT' || viewRecord.status === 'DISABLED')"
            type="primary"
            @click="handlePublish(viewRecord!)"
          >
            <template #icon><CloudUploadOutlined /></template>
            {{ viewRecord.status === 'DISABLED' ? '重新发布' : '发布方案' }}
          </a-button>
          <a-button
            v-if="viewRecord?.status === 'PUBLISHED'"
            danger
            @click="handleDisable(viewRecord!)"
          >
            <template #icon><StopOutlined /></template>
            停用方案
          </a-button>
          <a-tooltip
            :title="viewRecord && viewRecord.status !== 'PUBLISHED' ? '请先发布方案后再执行' : ''"
          >
            <a-button
              type="primary"
              :loading="executingIds.includes(viewRecord?.id!)"
              :disabled="viewRecord?.status !== 'PUBLISHED'"
              @click="handleExecute(viewRecord!)"
            >
              <template #icon><PlayCircleOutlined /></template>
              立即执行
            </a-button>
          </a-tooltip>
          <a-button @click="handleEdit(viewRecord!)">
            <template #icon><EditOutlined /></template>
            编辑方案
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 列选择弹窗（挂 body，z-index 高于向导主弹窗） -->
    <a-modal
      v-model:open="columnSelectorVisible"
      :title="`选择字段 — ${columnSelectorTable?.tableName}`"
      :width="620"
      :confirm-loading="false"
      :z-index="3000"
      @ok="confirmColumnSelection"
      @cancel="columnSelectorVisible = false"
    >
      <div class="col-selector-toolbar">
        <a-input
          v-model:value="columnSelectorSearch"
          placeholder="搜索字段名或注释"
          allow-clear
          style="flex:1"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-space style="margin-left:8px">
          <a-button size="small" @click="selectAllColumns">全选</a-button>
          <a-button size="small" @click="deselectAllColumns">反选</a-button>
        </a-space>
      </div>
      <div
        v-if="columnSelectorLoading"
        style="text-align: center; padding: 20px; color: #999"
      >
        <a-spin /> 加载中...
      </div>
      <div
        v-else
        style="max-height: 380px; overflow-y: auto; border: 1px solid #f0f0f0; border-radius: 8px; padding: 8px; margin-top: 8px"
      >
        <a-checkbox-group v-model:value="columnSelectorSelected">
          <a-row>
            <a-col
              v-for="col in (columnSelectorSearch
                ? columnSelectorOptions.filter(c => c.label.toLowerCase().includes(columnSelectorSearch.toLowerCase()))
                : columnSelectorOptions)"
              :key="col.value"
              :span="24"
              style="padding: 4px 8px"
            >
              <a-checkbox :value="col.value" style="word-break: break-all">
                {{ col.label }}
              </a-checkbox>
            </a-col>
            <a-col v-if="columnSelectorOptions.length === 0 && !columnSelectorLoading" :span="24" style="text-align: center; padding: 20px; color: #999">
              暂无可用字段
            </a-col>
          </a-row>
        </a-checkbox-group>
      </div>
      <div style="margin-top: 8px; font-size: 12px; color: #999; display: flex; justify-content: space-between">
        <span>已选 {{ columnSelectorSelected.length }} 个字段</span>
        <span v-if="columnSelectorOptions.length > 0">共 {{ columnSelectorOptions.length }} 个可选字段</span>
      </div>
    </a-modal>

    <!-- 规则选择弹窗（挂 body，z-index 高于向导主弹窗） -->
    <a-modal
      v-model:open="ruleSelectorVisible"
      :title="ruleSelectorIsBatch
        ? `批量绑定规则 — ${ruleSelectorGroup?.tableName || ''}`
        : `绑定规则 — ${ruleSelectorGroup?.tableName || ''} · ${ruleSelectorGroup?.columns?.join(', ') || ''}`"
      :width="760"
      :confirm-loading="ruleSelectorLoading"
      :z-index="3000"
      @ok="confirmRuleSelection"
      @cancel="ruleSelectorVisible = false"
    >
        <div style="margin-bottom: 12px">
        <a-alert type="info" show-icon style="margin-bottom: 8px">
          <template #message>
            <span v-if="ruleSelectorIsBatch">
              已选 <strong>{{ ruleSelectorGroup?.columns?.length }}</strong> 个字段批量绑定规则
            </span>
            <span v-else>
              为字段「<strong>{{ ruleSelectorGroup?.columns?.join(', ') }}</strong>」绑定规则
            </span>
            <span v-if="formData.bindSensitivityLevel" style="margin-left: 8px">
              <a-tag :color="getSensitivityTextColor(formData.bindSensitivityLevel)" style="margin-left: 4px">
                {{ formData.bindSensitivityLevel }} 等级规则筛选
              </a-tag>
            </span>
          </template>
        </a-alert>
        <a-space wrap>
          <a-input
            v-model:value="ruleSelectorSearch"
            placeholder="搜索规则名称"
            allow-clear
            style="width: 200px"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
          <a-select
            v-model:value="ruleSelectorType"
            placeholder="规则类型"
            allow-clear
            style="width: 140px"
          >
            <a-select-option value="">全部类型</a-select-option>
            <a-select-option value="NULL_CHECK">空值检查</a-select-option>
            <a-select-option value="UNIQUE_CHECK">唯一性校验</a-select-option>
            <a-select-option value="REGEX_PHONE">手机格式</a-select-option>
            <a-select-option value="THRESHOLD_RANGE">值域校验</a-select-option>
            <a-select-option value="CROSS_FIELD_COMPARE">跨字段校验</a-select-option>
            <a-select-option value="CUSTOM_SQL">自定义SQL</a-select-option>
            <a-select-option value="ROW_COUNT_FLUCTUATION">波动监测</a-select-option>
          </a-select>
          <a-select
            v-if="formData.bindDimension === 'SENSITIVITY_LEVEL'"
            v-model:value="ruleSelectorLevel"
            placeholder="规则等级"
            allow-clear
            style="width: 120px"
          >
            <a-select-option value="">全部等级</a-select-option>
            <a-select-option value="L4">
              <span style="color: #FF4D4F; font-weight: 700">🔴 L4</span>
            </a-select-option>
            <a-select-option value="L3">
              <span style="color: #FA8C16; font-weight: 700">🟠 L3</span>
            </a-select-option>
            <a-select-option value="L2">
              <span style="color: #1677FF; font-weight: 700">🔵 L2</span>
            </a-select-option>
            <a-select-option value="L1">
              <span style="color: #52C41A; font-weight: 700">🟢 L1</span>
            </a-select-option>
          </a-select>
        </a-space>
      </div>
      <div style="max-height: 400px; overflow-y: auto; border: 1px solid #f0f0f0; border-radius: 8px">
        <div v-if="ruleSelectorLoading" style="text-align: center; padding: 40px; color: #999">
          <a-spin /> 加载规则中...
        </div>
        <div v-else-if="filteredSelectorRules.length === 0" style="text-align: center; padding: 40px; color: #999">
          <InboxOutlined style="font-size: 32px; display: block; margin-bottom: 8px" />
          暂无匹配规则
        </div>
        <div v-else>
          <div
            v-for="rule in filteredSelectorRules"
            :key="rule.id"
            class="rule-selector-item"
            :class="{ 'rule-selector-item-selected': ruleSelectorSelectedIds.includes(rule.id!) }"
          >
            <a-checkbox
              :checked="ruleSelectorSelectedIds.includes(rule.id!)"
              style="flex-shrink: 0"
              @click.stop="toggleRuleSelection(rule)"
            />
            <div class="rule-selector-info" @click.stop="toggleRuleSelection(rule)">
              <div class="rule-selector-name">{{ rule.ruleName }}</div>
              <div class="rule-selector-meta">
                <span class="rule-type-tag" :class="getRuleTypeClass(rule.ruleType)">
                  {{ getRuleTypeLabel(rule.ruleType) }}
                </span>
                <span
                  v-if="rule.applyLevel"
                  class="rule-sens-tag"
                  :style="{ color: getSensitivityTextColor(rule.applyLevel), borderColor: getSensitivityTextColor(rule.applyLevel) }"
                >
                  <SafetyCertificateOutlined />
                  {{ rule.applyLevel }}
                </span>
                <a-tag :color="rule.ruleStrength === 'STRONG' ? 'red' : 'orange'" size="small">
                  {{ rule.ruleStrength === 'STRONG' ? '强规则' : '弱规则' }}
                </a-tag>
                <span class="rule-selector-expr" v-if="rule.ruleExpr">
                  {{ rule.ruleExpr.length > 60 ? rule.ruleExpr.substring(0, 60) + '...' : rule.ruleExpr }}
                </span>
              </div>
            </div>
            <!-- SQL 预览按钮 -->
            <div class="rule-sql-preview" v-if="formData.bindDsId" @click.stop>
              <a-button
                size="small"
                type="text"
                :loading="sqlPreviewLoading[rule.id!]"
                @click="previewRuleSql(rule, filteredSelectorRules.indexOf(rule))"
                :title="sqlPreviewMap[rule.id!] ? '再次点击关闭预览' : '预览该规则生成的 SQL'"
              >
                <template #icon><CodeOutlined /></template>
                {{ sqlPreviewMap[rule.id!] ? '收起' : 'SQL' }}
              </a-button>
            </div>
            <!-- SQL 预览展开 -->
            <div v-if="sqlPreviewMap[rule.id!]" class="rule-sql-panel">
              <pre class="sql-pre">{{ sqlPreviewMap[rule.id!] }}</pre>
            </div>
          </div>
        </div>
      </div>
      <div style="margin-top: 8px; font-size: 12px; color: #999">
        已选 {{ ruleSelectorSelectedIds.length }} 条规则<span v-if="ruleSelectorIsBatch">（将同时绑定到选中的 {{ ruleSelectorGroup?.columns?.length }} 个字段）</span>
      </div>
    </a-modal>

    <!-- 批量选列弹窗（一次性为所有已选表配置列） -->
    <a-modal
      v-model:open="batchColSelectorVisible"
      title="批量配置列"
      :width="680"
      :confirm-loading="batchColSelectorLoading"
      :z-index="3000"
      @ok="confirmBatchColumnSelection"
      @cancel="batchColSelectorVisible = false"
    >
      <a-alert type="info" show-icon style="margin-bottom: 10px">
        <template #message>
          已选择 <strong>{{ tableColumnBindings.length }}</strong> 张表，批量勾选字段将一次性写入各表。
        </template>
      </a-alert>
      <div class="batch-col-toolbar">
        <a-input
          v-model:value="batchColSelectorSearch"
          placeholder="搜索字段名"
          allow-clear
          style="flex:1"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-space style="margin-left:8px">
          <a-button size="small" @click="batchColSelectorSelected = batchColSelectorOptions.map(c => c.value)">全选</a-button>
          <a-button size="small" @click="batchColSelectorSelected = []">清空</a-button>
        </a-space>
      </div>
      <div style="max-height: min(52vh, 520px); overflow-y: auto; border: 1px solid #f0f0f0; border-radius: 8px; padding: 8px; margin-top: 8px">
        <a-checkbox-group v-model:value="batchColSelectorSelected">
          <a-row>
            <a-col
              v-for="col in batchColSelectorOptions"
              v-show="!batchColSelectorSearch || col.label.toLowerCase().includes(batchColSelectorSearch.toLowerCase())"
              :key="col.value"
              :span="24"
              style="padding: 4px 8px"
            >
              <a-checkbox :value="col.value" style="word-break: break-all">
                <span class="batch-col-table-badge">{{ col.tableName }}</span>
                {{ col.label.split('.')[1] || col.label }}
              </a-checkbox>
            </a-col>
            <a-col v-if="batchColSelectorOptions.length === 0 && !batchColSelectorLoading" :span="24" style="text-align: center; padding: 20px; color: #999">
              暂无可用字段
            </a-col>
          </a-row>
        </a-checkbox-group>
      </div>
      <div style="margin-top: 8px; font-size: 12px; color: #999; display: flex; justify-content: space-between">
        <span>已选 {{ batchColSelectorSelected.length }} 个字段</span>
        <span v-if="batchColSelectorOptions.length > 0">共 {{ batchColSelectorOptions.length }} 个可选字段</span>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick, h } from 'vue'
import {
  Button,
  Input,
  Select,
  Table,
  Modal,
  Drawer,
  Dropdown,
  Menu,
  Tag,
  Badge,
  Switch,
  Progress,
  Pagination,
  Space,
  Row,
  Col,
  Divider,
  Tooltip,
  Popover,
  Radio,
  Avatar,
  Breadcrumb,
  Steps,
  Form,
  message,
  Alert,
  Checkbox,
  CheckboxGroup
} from 'ant-design-vue'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined, PlayCircleOutlined,
  MoreOutlined, SearchOutlined, ReloadOutlined, CheckOutlined, LeftOutlined, RightOutlined,
  SyncOutlined, BulbOutlined, HolderOutlined, DatabaseOutlined, ClockCircleOutlined,
  ScheduleOutlined, ApiOutlined, QuestionCircleOutlined, CopyOutlined, PlusCircleOutlined,
  InboxOutlined,
  CheckCircleOutlined, CloudUploadOutlined, StopOutlined,
  SafetyCertificateOutlined, InfoCircleOutlined,
  ColumnWidthOutlined, ThunderboltOutlined,
  CodeOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { planApi, ruleDefApi, dataSourceApi, executionApi } from '@/api/dqc'
import { metadataApi } from '@/api/gov'
import { pageSensitivity } from '@/api/security'
import type { Plan, RuleDef, Execution, TableColumnBinding, RuleBinding, PlanRuleBindVO } from '@/types'

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const searchName = ref('')
const filterLayer = ref<string>()
const filterStatus = ref<string>()
const filterTrigger = ref<string>()
const executingIds = ref<number[]>([])

// 轮询定时器 Map: executionId -> timerId
const pollingTimers = new Map<number, ReturnType<typeof setInterval>>()
// 执行进度 Map: executionId -> 计数 + 当前运行中的规则名（来自明细里 status=RUNNING）
const executionProgressMap = ref<
  Map<
    number,
    {
      totalRules: number
      passedRules: number
      failedRules: number
      skippedRules: number
      status: string
      currentRuleName?: string
      pollErrorCount: number
      pollingInterrupted: boolean
      /** 含「当前规则执行中」的视觉进度，避免长时间卡在 0% */
      displayPercent: number
    }
  >
>(new Map())
// planId -> 当前正在执行的 executionId（用于在卡片上显示进度）
const runningExecutionMap = ref<Map<number, number>>(new Map())

const pagination = reactive({
  current: 1,
  pageSize: 12,
  total: 0
})

const statCards = ref([
  { label: '方案总数', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)' },
  { label: '已启用', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '草稿', value: 0, bg: 'linear-gradient(135deg, #FA8C16 0%, #FFC53D 100%)' },
  { label: '今日执行', value: 0, bg: 'linear-gradient(135deg, #722ED1 0%, #9254DE 100%)' }
])

const layerOptions = [
  { value: 'ODS', label: 'ODS 层' },
  { value: 'DWD', label: 'DWD 层' },
  { value: 'DWS', label: 'DWS 层' },
  { value: 'ADS', label: 'ADS 层' }
]

// 5 步：字段与规则合并为一步
const wizardSteps = [
  { key: 'basic', title: '基本信息', desc: '方案名称和描述' },
  { key: 'bind', title: '绑定表范围', desc: '选择检测目标' },
  { key: 'fieldRules', title: '字段与规则', desc: '全列展示并绑定质检规则' },
  { key: 'trigger', title: '触发方式', desc: '执行和告警配置' },
  { key: 'preview', title: '预览保存', desc: '确认并创建' }
]

// 弹窗状态
const modalVisible = ref(false)
const modalTitle = ref('新建方案')
const currentStep = ref(0)
/** 仅用 maxHeight + 自身滚动，避免 flex 固定高度把内容区压成 0 高（白屏） */
const wizardModalBodyStyle = {
  paddingTop: '16px',
  paddingBottom: '16px',
  paddingLeft: '12px',
  paddingRight: '12px',
  maxHeight: 'calc(100vh - 100px)',
  overflowY: 'auto' as const,
  overflowX: 'hidden' as const
}
const submitLoading = ref(false)
const isEdit = ref(false)
const editingId = ref<number>()

const allRules = ref<RuleDef[]>([])
const boundRules = ref<any[]>([])
const bindDsLoading = ref(false)
const dataSourceList = ref<any[]>([])
const schemaList = ref<string[]>([])
const schemaLoading = ref(false)
const metadataLoading = ref(false)
const tableNameOptions = ref<{ label: string; value: string }[]>([])

// 敏感字段预览（基于已选数据源+等级，调用安全 API）
const sensitivityFieldPreview = ref<{ tableName: string; columnName: string; level: string }[]>([])
const sensitivityFieldLoading = ref(false)

const viewDrawerVisible = ref(false)
const viewRecord = ref<any>(null)
const boundRulesView = ref<any[]>([])

// 列选择弹窗
const columnSelectorVisible = ref(false)
const columnSelectorTable = ref<TableColumnBinding | null>(null)
const columnSelectorLoading = ref(false)
const columnSelectorOptions = ref<{ label: string; value: string }[]>([])
const columnSelectorSelected = ref<string[]>([])
const columnSelectorSearch = ref('')

// 批量选列弹窗状态
const batchColSelectorVisible = ref(false)
const batchColSelectorLoading = ref(false)
const batchColSelectorOptions = ref<{ label: string; value: string; tableName: string }[]>([])
const batchColSelectorSelected = ref<string[]>([])
const batchColSelectorSearch = ref('')
const batchColSelectorTableMap = ref<Map<string, string[]>>(new Map())

const formData = reactive<any>({
  planName: '',
  planCode: '',
  planDesc: '',
  bindValue: '',
  bindDsId: undefined as number | undefined,
  bindSchema: '',
  bindDimension: '' as '' | 'SENSITIVITY_LEVEL' | undefined,
  bindSensitivityLevel: '' as '' | 'L4' | 'L3' | 'L2' | 'L1' | undefined,
  selectedTableNames: [] as string[],
  triggerType: 'MANUAL',
  triggerCron: '',
  alertOnSuccess: false,
  alertOnFailure: true,
  autoBlock: true
})

// 步骤3 表格绑定数据结构：每个 (table, column) 组合
const tableColumnBindings = ref<TableColumnBinding[]>([])

// 步骤4 规则绑定：按 (tableName) 分组，每组内每列有 rules[]
const ruleBindings = ref<{
  tableName: string
  columns: string[]
  rules: RuleBinding[]
}[]>([])

// 规则选择弹窗
const ruleSelectorVisible = ref(false)
const ruleSelectorGroup = ref<{ tableName: string; columns: string[] } | null>(null)
const ruleSelectorLoading = ref(false)
const ruleSelectorRules = ref<RuleDef[]>([])
const ruleSelectorSelectedIds = ref<number[]>([])
const ruleSelectorSearch = ref('')
const ruleSelectorType = ref('')
const ruleSelectorLevel = ref('')
const ruleSelectorIsBatch = ref(false)
/** 打开规则弹窗时该行已绑定的规则 id，用于保存时对比增删 */
const ruleSelectorBaselineIds = ref<number[]>([])

// SQL 预览（规则ID -> SQL 文本）
const sqlPreviewMap = ref<Record<number, string>>({})
const sqlPreviewLoading = ref<Record<number, boolean>>({})

// 批量绑规则选中的目标表和列
const batchRuleTargetTable = ref<string>('')
const batchRuleTargetColumns = ref<string[]>([])

const formRef1 = ref()

const formRules1 = {
  planName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }]
}

const selectedColumnCount = computed(() =>
  tableColumnBindings.value.reduce((acc, t) => acc + (t.columns?.length || 0), 0)
)

// 步骤4：按 tableName 分组的规则绑定展示数据
// 每张表的列下仅展示绑定到该列的规则（targetColumn 匹配），targetColumn 为空表示全表适用
const ruleBindingGroups = computed(() => {
  return tableColumnBindings.value.map(t => {
    const binding = ruleBindings.value.find(b => b.tableName === t.tableName)
    const columns = t.columns || []
    // columnRules: Map<columnName, RuleBinding[]>
    const colRuleMap = new Map<string, RuleBinding[]>()
    for (const col of columns) {
      colRuleMap.set(col, [])
    }
    // 将每条规则按 targetColumn 分配到对应列
    for (const rule of (binding?.rules || [])) {
      if (rule.targetColumn && colRuleMap.has(rule.targetColumn)) {
        colRuleMap.get(rule.targetColumn)!.push(rule)
      } else if (!rule.targetColumn) {
        // 无 targetColumn 的规则属于"全表"，复制到所有列
        for (const arr of colRuleMap.values()) {
          if (!arr.find(existing => existing.id === rule.id)) {
            arr.push({ ...rule, targetColumn: '' })
          }
        }
      }
    }
    return {
      key: t.tableName,
      tableName: t.tableName,
      columns,
      colRuleMap,
      totalRules: (binding?.rules || []).length
    }
  })
})

const totalBoundRuleCount = computed(() =>
  ruleBindingGroups.value.reduce((acc, g) => acc + g.totalRules, 0)
)
const totalStrongCount = computed(() =>
  ruleBindingGroups.value.reduce((acc, g) => {
    let count = 0
    for (const rules of g.colRuleMap.values()) {
      count += rules.filter(r => r.ruleStrength === 'STRONG').length
    }
    return acc + count
  }, 0)
)
const totalWeakCount = computed(() =>
  ruleBindingGroups.value.reduce((acc, g) => {
    let count = 0
    for (const rules of g.colRuleMap.values()) {
      count += rules.filter(r => r.ruleStrength === 'WEAK').length
    }
    return acc + count
  }, 0)
)

// 合并步：一行一字段，供大表格展示
const flattenedFieldRuleRows = computed(() => {
  const rows: {
    key: string
    tableName: string
    columnName: string
    rules: RuleBinding[]
    totalRules: number
  }[] = []
  for (const g of ruleBindingGroups.value) {
    for (const col of g.columns) {
      const rules = g.colRuleMap.get(col) || []
      rows.push({
        key: `${g.tableName}::${col}`,
        tableName: g.tableName,
        columnName: col,
        rules,
        totalRules: rules.length
      })
    }
  }
  return rows
})

// 表格纵向滚动高度（第 3 步尽量多露出行数；与 flex 布局配合）
const fieldRulesTableScrollY = computed(() => {
  if (typeof window === 'undefined') return 480
  return Math.min(720, Math.max(320, window.innerHeight - 220))
})

// 数据源目录提示（库名 / Schema），与执行端 RuleContext 一致
const bindCatalogHint = computed(() => {
  const ds = dataSourceList.value.find((d: any) => d.id === formData.bindDsId)
  if (!ds) return ''
  const bits: string[] = []
  if (ds.databaseName) bits.push(`库 ${ds.databaseName}`)
  const sch = (formData.bindSchema || ds.schemaName || '').trim()
  if (sch && (isPostgresBind.value || String(ds.dsType || '').toUpperCase() === 'SQLSERVER')) {
    bits.push(`Schema ${sch}`)
  }
  return bits.join('，')
})

const filteredSelectorRules = computed(() => {
  let list = ruleSelectorRules.value
  const search = ruleSelectorSearch.value.trim().toLowerCase()
  if (search) list = list.filter(r => (r.ruleName?.toLowerCase().includes(search) || r.ruleCode?.toLowerCase().includes(search)))
  if (ruleSelectorType.value) list = list.filter(r => r.ruleType === ruleSelectorType.value)
  if (ruleSelectorLevel.value) list = list.filter(r => r.applyLevel === ruleSelectorLevel.value)
  return list
})

const isPostgresBind = computed(() => {
  const ds = dataSourceList.value.find(d => d.id === formData.bindDsId)
  return ds?.dsType?.toUpperCase() === 'POSTGRESQL'
})

const tableSelectPlaceholder = computed(() => {
  if (!formData.bindDsId) return '请先选择数据源'
  if (isPostgresBind.value && !formData.bindSchema) return '请先选择 Schema'
  return '搜索或选择表名（可多选）'
})

function filterDsOption(input: string, option: any) {
  const id = option?.value
  const ds = dataSourceList.value.find(d => d.id === id)
  if (!ds) return false
  const t = `${ds.dsName} ${ds.dsType || ''} ${ds.dataLayer || ''}`.toLowerCase()
  return t.includes((input || '').toLowerCase())
}

async function loadDataSourcesForBind() {
  bindDsLoading.value = true
  try {
    const res: any = await dataSourceApi.listEnabled()
    if (res.success !== false) {
      dataSourceList.value = Array.isArray(res.data) ? res.data : []
    } else {
      dataSourceList.value = []
    }
  } catch {
    dataSourceList.value = []
  } finally {
    bindDsLoading.value = false
  }
}

async function loadBindTableOptions(schema?: string) {
  if (!formData.bindDsId) return
  const dsId = formData.bindDsId
  metadataLoading.value = true
  try {
    // 元数据与直连表列表独立：治理模块 /metadata/page 失败（无权限、未扫描等）时仍应展示直连库表名
    const tablesP = schema
      ? dataSourceApi.getTableListBySchema(dsId, schema).catch(() => ({ data: [] as string[] }))
      : dataSourceApi.getTables(dsId).catch(() => ({ data: [] as string[] }))

    let metaRecords: { tableName?: string }[] = []
    try {
      const pageRes: any = await metadataApi.page({ pageNum: 1, pageSize: 500, dsId })
      const pageInner = pageRes?.data
      metaRecords = Array.isArray(pageInner?.records) ? pageInner.records : []
    } catch {
      metaRecords = []
    }

    const raw = await tablesP
    const liveNames = Array.isArray(raw?.data) ? (raw.data as string[]) : []
    const byName = new Map<string, string>()
    for (const m of metaRecords) {
      const n = m?.tableName as string
      if (n) byName.set(n, n)
    }
    for (const n of liveNames) {
      if (n && !byName.has(n)) byName.set(n, n)
    }
    const names = [...byName.keys()].sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
    tableNameOptions.value = names.map(n => ({ label: n, value: n }))
  } catch {
    tableNameOptions.value = []
  } finally {
    metadataLoading.value = false
  }
}

async function onBindDsChange() {
  formData.bindSchema = ''
  // 保留绑定维度 / 敏感等级（表单项在数据源上方），仅换实例时刷新预览即可
  formData.selectedTableNames = []
  tableColumnBindings.value = []
  ruleBindings.value = []
  schemaList.value = []
  tableNameOptions.value = []
  sensitivityFieldPreview.value = []
  // 勿将 currentStep 置 0：用户正在「绑定表范围」步选数据源，否则会错误跳回基本信息
  syncAutoPlanName()
  if (!formData.bindDsId) return
  const ds = dataSourceList.value.find(d => d.id === formData.bindDsId)
  if (ds?.dsType?.toUpperCase() === 'POSTGRESQL') {
    schemaLoading.value = true
    try {
      const res: any = await dataSourceApi.getSchemas(formData.bindDsId)
      schemaList.value = Array.isArray(res?.data) ? res.data : []
    } catch {
      schemaList.value = []
    } finally {
      schemaLoading.value = false
    }
  } else {
    void loadBindTableOptions(undefined)
  }
  if (formData.bindDimension === 'SENSITIVITY_LEVEL' && formData.bindSensitivityLevel) {
    void loadSensitivityFieldPreview()
  }
}

async function onBindSchemaChange() {
  formData.selectedTableNames = []
  tableColumnBindings.value = []
  ruleBindings.value = []
  if (formData.bindDsId && isPostgresBind.value && formData.bindSchema) {
    await loadBindTableOptions(formData.bindSchema)
  }
  syncAutoPlanName()
}

function onBindDimensionChange() {
  // 选择绑定维度后重置敏感等级
  formData.bindSensitivityLevel = undefined as unknown as ''
  sensitivityFieldPreview.value = []
}

function onBindSensitivityLevelChange() {
  // 敏感等级变化时，加载该等级在已选数据源下的敏感字段预览
  sensitivityFieldPreview.value = []
  if (formData.bindDimension !== 'SENSITIVITY_LEVEL' || !formData.bindSensitivityLevel || !formData.bindDsId) return
  loadSensitivityFieldPreview()
}

async function loadSensitivityFieldPreview() {
  if (!formData.bindDsId || !formData.bindSensitivityLevel) return
  sensitivityFieldLoading.value = true
  try {
    const res: any = await pageSensitivity({
      pageNum: 1,
      pageSize: 200,
      dsId: formData.bindDsId,
      sensitivityLevel: formData.bindSensitivityLevel as 'L4' | 'L3' | 'L2' | 'L1'
    })
    if (res.success !== false && res.data) {
      sensitivityFieldPreview.value = (res.data.records || []).map((r: any) => ({
        tableName: r.tableName || '',
        columnName: r.columnName || '',
        level: r.sensitivityLevel || formData.bindSensitivityLevel
      }))
    }
  } catch {
    sensitivityFieldPreview.value = []
  } finally {
    sensitivityFieldLoading.value = false
  }
}

function onSelectedTablesChange() {
  const selected = formData.selectedTableNames || []
  // 过滤掉已取消选择的表
  tableColumnBindings.value = tableColumnBindings.value.filter(b => selected.includes(b.tableName))
  // 同步 ruleBindings：移除已取消选择的表
  ruleBindings.value = ruleBindings.value.filter(b => selected.includes(b.tableName))
  // 新增的表追加空 binding
  for (const t of selected) {
    if (!tableColumnBindings.value.find(b => b.tableName === t)) {
      tableColumnBindings.value.push({ tableName: t, columns: [] })
    }
  }
  syncAutoPlanName()
}

/** 最近一次由「选表」自动写入的方案名称；与用户手改区分，便于继续联动更新 */
const lastAutoPlanName = ref('')

function onPlanNameUserInput(val: string) {
  if (isEdit.value) return
  // 与当前自动名称一致时视为程序联动，勿清空
  if (val === lastAutoPlanName.value) return
  lastAutoPlanName.value = ''
}

function buildSuggestedPlanName(): string {
  const tables = formData.selectedTableNames || []
  if (!tables.length) return ''
  const ds = dataSourceList.value.find(d => d.id === formData.bindDsId)
  const prefix = ds?.dsName?.trim() ? ds.dsName.trim() : '质检'
  const maxShow = 2
  if (tables.length === 1) {
    return `${prefix}-${tables[0]} 质检方案`
  }
  const head = tables.slice(0, maxShow).join('、')
  if (tables.length <= maxShow) {
    return `${prefix}-${head} 质检方案`
  }
  return `${prefix}-${head} 等${tables.length}张表 质检方案`
}

/** 新建方案时：根据已选表联动方案名称（用户已手改过的名称不会被覆盖） */
function syncAutoPlanName() {
  if (isEdit.value) return
  const tables = formData.selectedTableNames || []
  if (tables.length === 0) {
    if (formData.planName === lastAutoPlanName.value) {
      formData.planName = ''
      lastAutoPlanName.value = ''
    }
    return
  }
  const suggested = buildSuggestedPlanName()
  if (!suggested) return
  if (formData.planName === '' || formData.planName === lastAutoPlanName.value) {
    lastAutoPlanName.value = suggested
    formData.planName = suggested
  }
}

function buildBindValue(): string {
  const v = 2
  return JSON.stringify({
    v,
    dsId: formData.bindDsId,
    schema: formData.bindSchema || undefined,
    tables: formData.selectedTableNames || [],
    tableColumnBindings: tableColumnBindings.value,
    ruleBindings: ruleBindings.value,
    bindDimension: formData.bindDimension || undefined,
    bindSensitivityLevel: formData.bindSensitivityLevel || undefined,
  })
}

function getDsLayerTag(): string {
  const ds = dataSourceList.value.find(d => d.id === formData.bindDsId)
  return ds?.dataLayer || ''
}

function getBindDsLabel(): string {
  const ds = dataSourceList.value.find(d => d.id === formData.bindDsId)
  if (!ds) return '-'
  return `${ds.dsName}（${ds.dsType || '-'}${ds.dataLayer ? ` · ${ds.dataLayer}` : ''}）`
}

async function hydrateBindAfterEdit(plan: any) {
  formData.bindValue = plan.bindValue || ''
  formData.bindDsId = undefined
  formData.bindSchema = ''
  formData.bindDimension = '' as '' | 'SENSITIVITY_LEVEL' | undefined
  formData.bindSensitivityLevel = '' as '' | 'L4' | 'L3' | 'L2' | 'L1' | undefined
  formData.selectedTableNames = []
  tableColumnBindings.value = []
  ruleBindings.value = []
  const raw = plan.bindValue
  if (raw) {
    try {
      const o = JSON.parse(raw)
      if (o && typeof o === 'object') {
        if (o.dsId != null) formData.bindDsId = Number(o.dsId)
        if (o.schema) formData.bindSchema = o.schema
        if (o.bindDimension) formData.bindDimension = o.bindDimension
        if (o.bindSensitivityLevel) formData.bindSensitivityLevel = o.bindSensitivityLevel
        if (Array.isArray(o.tables)) {
          formData.selectedTableNames = [...o.tables]
        }
        if (Array.isArray(o.tableColumnBindings)) {
          tableColumnBindings.value = [...o.tableColumnBindings]
        }
        if (Array.isArray(o.ruleBindings)) {
          ruleBindings.value = [...o.ruleBindings]
        }
      }
    } catch {
      // ignore parse errors
    }
  }
  await nextTick()
  if (!formData.bindDsId) return
  const ds = dataSourceList.value.find(d => d.id === formData.bindDsId)
  if (ds?.dsType?.toUpperCase() === 'POSTGRESQL') {
    schemaLoading.value = true
    try {
      const res: any = await dataSourceApi.getSchemas(formData.bindDsId)
      schemaList.value = Array.isArray(res?.data) ? res.data : []
    } catch {
      schemaList.value = []
    } finally {
      schemaLoading.value = false
    }
    if (formData.bindSchema) {
      await loadBindTableOptions(formData.bindSchema)
    }
  } else {
    await loadBindTableOptions(undefined)
  }
  // 列加载完成后：补齐缺失表的 ruleBindings，并清理已选列与 ruleBindings 的不匹配项
  await restoreRuleBindings()
  // 对已有列补全 ruleBindings 条目（表已在 tableColumnBindings 中但 ruleBindings 里没有的）
  for (const t of tableColumnBindings.value) {
    if (!ruleBindings.value.find(b => b.tableName === t.tableName)) {
      ruleBindings.value.push({ tableName: t.tableName, columns: [...(t.columns || [])], rules: [] })
    }
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await planApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      planName: searchName.value || undefined,
      layerCode: filterLayer.value || undefined,
      status: filterStatus.value || undefined,
      triggerType: filterTrigger.value || undefined
    })
    if (res.success !== false && res.data) {
      const page = res.data
      tableData.value = page.records || []
      pagination.total = page.total ?? 0
    } else {
      tableData.value = []
      pagination.total = 0
    }
    statCards.value[0].value = pagination.total
    statCards.value[1].value = tableData.value.filter(p => p.status === 'PUBLISHED').length
    statCards.value[2].value = tableData.value.filter(p => p.status === 'DRAFT').length
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function loadAllRules() {
  try {
    const res: any = await ruleDefApi.page({ pageNum: 1, pageSize: 9999 })
    if (res.success !== false && res.data) {
      const page = res.data
      allRules.value = page.records || []
    } else {
      allRules.value = []
    }
  } catch {
    allRules.value = []
  }
}

function resetFilters() {
  searchName.value = ''
  filterLayer.value = undefined
  filterStatus.value = undefined
  filterTrigger.value = undefined
  pagination.current = 1
  loadData()
}

function openCreateModal() {
  isEdit.value = false
  editingId.value = undefined
  modalTitle.value = '新建方案'
  currentStep.value = 0
  resetFormData()
  generateCode()
  regeneratePlanNameFromCode()
  loadAllRules()
  modalVisible.value = true
  void loadDataSourcesForBind()
}

async function handleEdit(plan: Plan) {
  isEdit.value = true
  editingId.value = plan.id
  modalTitle.value = '编辑方案'
  currentStep.value = 0
  Object.assign(formData, {
    planName: plan.planName,
    planCode: plan.planCode,
    planDesc: plan.planDesc,
    triggerType: plan.triggerType || 'MANUAL',
    triggerCron: plan.triggerCron || '',
    alertOnSuccess: plan.alertOnSuccess ?? false,
    alertOnFailure: plan.alertOnFailure ?? true,
    autoBlock: plan.autoBlock ?? true,
    bindDimension: plan.sensitivityLevel ? 'SENSITIVITY_LEVEL' as const : '' as '' | 'SENSITIVITY_LEVEL' | undefined,
    bindSensitivityLevel: plan.sensitivityLevel ? plan.sensitivityLevel as 'L4' | 'L3' | 'L2' | 'L1' : '' as '' | 'L4' | 'L3' | 'L2' | 'L1' | undefined,
  })
  viewDrawerVisible.value = false
  modalVisible.value = true
  await loadDataSourcesForBind()
  // hydrateBindAfterEdit 解析 bindValue → 加载表 → 加载列 → 回显规则
  await hydrateBindAfterEdit(plan)
}

// 打开列选择弹窗，加载表的列信息
async function openColumnSelector(record: { tableName: string; columns: string[] }) {
  columnSelectorTable.value = record
  columnSelectorSelected.value = [...(record.columns || [])]
  columnSelectorSearch.value = ''
  columnSelectorOptions.value = []
  columnSelectorLoading.value = true
  columnSelectorVisible.value = true
  try {
    const res: any = await dataSourceApi.getTableColumns(formData.bindDsId!, record.tableName)
    columnSelectorOptions.value = (res.data || []).map((col: any) => {
      const label = (col.columnComment ? `${col.columnName} (${col.columnComment})` : col.columnName)
      return { label, value: col.columnName }
    })
  } catch {
    columnSelectorOptions.value = []
  } finally {
    columnSelectorLoading.value = false
  }
}

// 确认列选择
function confirmColumnSelection() {
  if (!columnSelectorTable.value) return
  columnSelectorTable.value.columns = [...columnSelectorSelected.value]
  columnSelectorVisible.value = false
}

// 移除某张表的某个列
function removeColumn(record: { tableName: string; columns: string[] }, col: string) {
  record.columns = record.columns.filter(c => c !== col)
}

// ---------- 批量选列 ----------
function selectAllColumns() {
  columnSelectorSelected.value = columnSelectorOptions.value.map(c => c.value)
}

function deselectAllColumns() {
  columnSelectorSelected.value = []
}

// 打开批量选列弹窗（一次加载所有已选表的列，按表 Tab 展示）
async function openBatchColumnSelector() {
  batchColSelectorLoading.value = true
  batchColSelectorVisible.value = true
  batchColSelectorSearch.value = ''
  batchColSelectorSelected.value = []
  batchColSelectorOptions.value = []
  batchColSelectorTableMap.value = new Map()
  try {
    const dsId = formData.bindDsId
    const schema = formData.bindSchema || undefined
    const loaders = tableColumnBindings.value.map(async (binding) => {
      try {
        const res: any = await dataSourceApi.getTableColumns(dsId, binding.tableName, schema)
        const cols: string[] = (res.data || []).map((c: any) => c.columnName as string)
        const existingSet = new Set(binding.columns)
        batchColSelectorTableMap.value.set(binding.tableName, [...existingSet])
        return { tableName: binding.tableName, cols: existingSet, allCols: cols }
      } catch {
        return { tableName: binding.tableName, cols: new Set(binding.columns), allCols: [] as string[] }
      }
    })
    const results = await Promise.all(loaders)
    const options: { label: string; value: string; tableName: string }[] = []
    const selected: string[] = []
    for (const r of results) {
      for (const col of r.allCols) {
        const val = `${r.tableName}.${col}`
        options.push({ label: `${r.tableName}.${col}`, value: val, tableName: r.tableName })
        if (r.cols.has(col)) selected.push(val)
      }
    }
    batchColSelectorOptions.value = options
    batchColSelectorSelected.value = selected
  } catch {
    batchColSelectorOptions.value = []
  } finally {
    batchColSelectorLoading.value = false
  }
}

function confirmBatchColumnSelection() {
  nextTick(() => {
    for (const binding of tableColumnBindings.value) {
      const prefix = `${binding.tableName}.`
      const selectedForTable = batchColSelectorSelected.value
        .filter(v => v.startsWith(prefix))
        .map(v => v.slice(prefix.length))
      binding.columns = selectedForTable
      const rb = ruleBindings.value.find(b => b.tableName === binding.tableName)
      if (rb) {
        rb.columns = [...selectedForTable]
        const colSet = new Set(selectedForTable)
        if (rb.rules?.length) {
          rb.rules = rb.rules.filter(r => !r.targetColumn || colSet.has(r.targetColumn))
        }
      }
    }
    batchColSelectorVisible.value = false
  })
}

async function loadBoundRules(planId: number) {
  try {
    const res: any = await planApi.getBoundRules(planId)
    const list = res.success !== false && Array.isArray(res.data) ? res.data : []
    // 向导提交使用 r.id 作为 ruleId，接口返回 ruleId
    boundRules.value = list.map((x: any) => ({
      ...x,
      id: x.ruleId,
      enabled: x.enabled !== false,
      skipOnFailure: Boolean(x.skipOnFailure)
    }))
  } catch {
    boundRules.value = []
  }
}

async function handleView(plan: Plan) {
  viewRecord.value = plan
  viewDrawerVisible.value = true
  await loadBoundRules(plan.id!)
  boundRulesView.value = [...boundRules.value]
}

function planLastScore(plan: any): number | null {
  const v = plan?.lastExecutionScore ?? plan?.lastScore
  if (v === undefined || v === null) return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

function formatPlanScore(plan: any): string {
  const n = planLastScore(plan)
  return n != null ? n.toFixed(1) : '-'
}

async function handlePublish(plan: Plan) {
  try {
    const res: any = await planApi.publish(plan.id!)
    if (res.success === false) {
      message.error(res.message || '发布失败')
      return
    }
    message.success('已发布，可点击「执行」触发质检')
    await loadData()
    if (viewRecord.value?.id === plan.id) {
      viewRecord.value = { ...viewRecord.value, status: 'PUBLISHED' }
    }
  } catch (e: any) {
    message.error(e?.message || '发布失败')
  }
}

function handleDisable(plan: Plan) {
  Modal.confirm({
    title: '停用方案',
    content: `停用后不可执行，确定停用「${plan.planName}」吗？`,
    okText: '停用',
    okType: 'danger',
    async onOk() {
      try {
        const res: any = await planApi.disable(plan.id!)
        if (res.success === false) {
          message.error(res.message || '停用失败')
          return
        }
        message.success('已停用')
        await loadData()
        if (viewRecord.value?.id === plan.id) {
          viewRecord.value = { ...viewRecord.value, status: 'DISABLED' }
        }
      } catch (e: any) {
        message.error(e?.message || '停用失败')
      }
    }
  })
}

async function handleExecute(plan: Plan) {
  if (plan.status !== 'PUBLISHED') {
    message.warning('仅「已发布」方案可执行，请先在菜单中发布（需至少绑定 1 条启用规则）')
    return
  }
  executingIds.value.push(plan.id!)
  try {
    const res: any = await planApi.execute(plan.id!)
    const execId = res?.data as number
    if (!execId) {
      throw new Error('未获取到执行ID')
    }

    const navigateToExecutionLogs = () => {
      sessionStorage.setItem('dqc-exec-filter-plan', String(plan.id))
      sessionStorage.setItem('dqc-exec-open-exec', String(execId))
      void router.push({ path: '/dqc/execution' })
    }

    const pollOnce = async () => {
      try {
        const execRes: any = await executionApi.getById(execId)
        const exec: Execution = execRes?.data
        if (!exec) {
          const tid = pollingTimers.get(execId)
          if (tid) clearInterval(tid)
          pollingTimers.delete(execId)
          executingIds.value = executingIds.value.filter(id => id !== plan.id)
          executionProgressMap.value.delete(execId)
          runningExecutionMap.value.delete(plan.id!)
          return
        }

        let list: any[] = []
        try {
          const detailsRes: any = await executionApi.getDetails(execId)
          list = detailsRes?.data ?? []
        } catch {
          /* 明细查询失败不影响主状态 */
        }

        const { displayPercent, currentRuleName } = computeDisplayPercent(exec, list)

        executionProgressMap.value.set(execId, {
          totalRules: exec.totalRules ?? 0,
          passedRules: exec.passedRules ?? 0,
          failedRules: exec.failedRules ?? 0,
          skippedRules: exec.skippedRules ?? 0,
          status: exec.status ?? 'RUNNING',
          currentRuleName,
          pollErrorCount: 0,
          pollingInterrupted: false,
          displayPercent
        })

        if (exec.status !== 'RUNNING') {
          const tid = pollingTimers.get(execId)
          if (tid) clearInterval(tid)
          pollingTimers.delete(execId)
          executingIds.value = executingIds.value.filter(id => id !== plan.id)
          executionProgressMap.value.delete(execId)
          runningExecutionMap.value.delete(plan.id!)
          message.success({
            content: h('span', null, [
              `执行完成 `,
              h('strong', { style: { color: exec.status === 'SUCCESS' ? '#52C41A' : '#FF4D4F' } },
                exec.status === 'SUCCESS' ? '✓ 成功' : exec.status === 'FAILED' ? '✗ 失败' : exec.status
              ),
              exec.qualityScore != null ? `（${exec.qualityScore}分）` : '',
              ` `,
              h('a', {
                href: '/dqc/execution',
                onClick: (e: Event) => {
                  e.preventDefault()
                  navigateToExecutionLogs()
                }
              }, '查看详情 →')
            ])
          })
          await loadData()
        }
      } catch {
        const prev = executionProgressMap.value.get(execId)
        const pollErrorCount = (prev?.pollErrorCount ?? 0) + 1
        executionProgressMap.value.set(execId, {
          totalRules: prev?.totalRules ?? 0,
          passedRules: prev?.passedRules ?? 0,
          failedRules: prev?.failedRules ?? 0,
          skippedRules: prev?.skippedRules ?? 0,
          status: prev?.status ?? 'RUNNING',
          currentRuleName: prev?.currentRuleName,
          pollErrorCount,
          pollingInterrupted: prev?.pollingInterrupted ?? false,
          displayPercent: prev?.displayPercent ?? 0
        })
        if (pollErrorCount > 10) {
          const tid = pollingTimers.get(execId)
          if (tid) clearInterval(tid)
          pollingTimers.delete(execId)
          executionProgressMap.value.set(execId, {
            totalRules: prev?.totalRules ?? 0,
            passedRules: prev?.passedRules ?? 0,
            failedRules: prev?.failedRules ?? 0,
            skippedRules: prev?.skippedRules ?? 0,
            status: prev?.status ?? 'RUNNING',
            currentRuleName: prev?.currentRuleName,
            pollErrorCount,
            pollingInterrupted: true,
            displayPercent: prev?.displayPercent ?? 0
          })
          message.warning({
            content: h('span', null, [
              `轮询中断（网络或服务响应异常），执行可能仍在进行。`,
              h('br'),
              h('a', {
                style: { fontSize: '12px' },
                onClick: (e: Event) => {
                  e.preventDefault()
                  sessionStorage.setItem('dqc-exec-filter-plan', String(plan.id))
                  void router.push({ path: '/dqc/execution' })
                  message.destroy()
                }
              }, '前往「执行记录」页确认状态 →')
            ])
          })
        }
      }
    }

    void pollOnce()
    const timerId = setInterval(() => {
      void pollOnce()
    }, 2000)

    pollingTimers.set(execId, timerId)
    runningExecutionMap.value.set(plan.id!, execId)

    message.success({
      content: h('span', null, [
        `执行已触发，正在运行中……`,
        h('br'),
        h('a', {
          href: '/dqc/execution',
          style: { fontSize: '12px' },
          onClick: (e: Event) => {
            e.preventDefault()
            navigateToExecutionLogs()
            message.destroy()
          }
        }, '跳转到执行记录 →')
      ])
    })
    await loadData()
  } catch (e: any) {
    message.error(e?.message || '执行失败，请重试')
  } finally {
    const hasPolling = pollingTimers.size > 0 ||
      executionProgressMap.value.size > 0
    if (!hasPolling) {
      executingIds.value = executingIds.value.filter(id => id !== plan.id)
    }
  }
}

async function handleCopy(plan: Plan) {
  Modal.confirm({
    title: '复制方案',
    content: `确定复制方案「${plan.planName}」吗？`,
    async onOk() {
      try {
        const res: any = await planApi.create({
          ...plan,
          id: undefined,
          planName: plan.planName + '_副本',
          planCode: ''
        })
        message.success('复制成功')
        loadData()
      } catch {
        message.error('复制失败')
      }
    }
  })
}

async function handleDelete(plan: Plan) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除方案「${plan.planName}」吗？删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        await planApi.delete(plan.id!)
        message.success('删除成功')
        loadData()
      } catch {
        message.error('删除失败')
      }
    }
  })
}

function generateCode() {
  formData.planCode = 'PLAN_' + Date.now().toString(36).toUpperCase()
}

/** 重新生成编码；若方案名称仍是自动值（或为空），同步更新名称，避免与编码脱节 */
function onGeneratePlanCodeClick() {
  const prevName = String(formData.planName || '').trim()
  const wasAuto = !prevName || prevName === lastAutoPlanName.value
  generateCode()
  if (!isEdit.value && wasAuto) regeneratePlanNameFromCode()
}

/** 新建：用当前方案编码生成默认名称（与「生成」编码联动）；选表后仍可由 syncAutoPlanName 覆盖 */
function regeneratePlanNameFromCode() {
  if (isEdit.value) return
  if (!String(formData.planCode || '').trim()) generateCode()
  const suffix = String(formData.planCode).replace(/^PLAN_/i, '').trim()
  const name = suffix ? `质检方案-${suffix}` : `质检方案-${Date.now().toString(36).toUpperCase()}`
  lastAutoPlanName.value = name
  formData.planName = name
}

// 打开规则选择弹窗（单列绑定）
/** 从合并表格中移除某字段，并去掉仅针对该列的规则绑定 */
function removeColumnByTable(tableName: string, columnName: string) {
  const binding = tableColumnBindings.value.find(b => b.tableName === tableName)
  if (binding?.columns) {
    binding.columns = binding.columns.filter(c => c !== columnName)
  }
  const rb = ruleBindings.value.find(b => b.tableName === tableName)
  if (rb) {
    if (rb.columns) rb.columns = rb.columns.filter(c => c !== columnName)
    if (rb.rules?.length) {
      rb.rules = rb.rules.filter(r => r.targetColumn !== columnName)
    }
  }
}

async function openRuleSelectorForColumn(tableName: string, column: string) {
  ruleSelectorGroup.value = { tableName, columns: [column] }
  ruleSelectorIsBatch.value = false
  const g = ruleBindingGroups.value.find(x => x.tableName === tableName)
  const rowRules = g?.colRuleMap.get(column) || []
  const baseline = [...new Set(rowRules.map(r => r.id).filter((id): id is number => id != null))]
  ruleSelectorBaselineIds.value = baseline
  ruleSelectorSelectedIds.value = [...baseline]
  ruleSelectorSearch.value = ''
  ruleSelectorType.value = ''
  ruleSelectorLevel.value = formData.bindSensitivityLevel || ''
  ruleSelectorRules.value = []
  ruleSelectorLoading.value = true
  ruleSelectorVisible.value = true
  try {
    const res: any = await ruleDefApi.page({ pageNum: 1, pageSize: 9999, targetDsId: formData.bindDsId })
    if (res.success !== false && res.data) {
      ruleSelectorRules.value = res.data.records || []
    } else {
      ruleSelectorRules.value = []
    }
  } catch {
    ruleSelectorRules.value = []
  } finally {
    ruleSelectorLoading.value = false
  }
}

// 预览规则对应的 SQL（在绑定数据源后可用）
async function previewRuleSql(rule: RuleDef, ruleIndex: number) {
  if (!formData.bindDsId) return
  if (sqlPreviewMap.value[rule.id!]) {
    // 再次点击关闭预览
    delete sqlPreviewMap.value[rule.id!]
    return
  }
  sqlPreviewLoading.value[rule.id!] = true
  try {
    const res: any = await ruleDefApi.preview(
      rule.id!,
      ruleSelectorGroup.value?.tableName,
      ruleSelectorGroup.value?.columns?.[0],
      formData.bindDsId
    )
    if (res.success !== false && res.data) {
      sqlPreviewMap.value[rule.id!] = res.data
    } else {
      sqlPreviewMap.value[rule.id!] = res.message || '无法生成预览'
    }
  } catch {
    sqlPreviewMap.value[rule.id!] = '预览失败，请检查数据源连接'
  } finally {
    delete sqlPreviewLoading.value[rule.id!]
  }
}

// 打开批量规则选择弹窗（多列绑定）
async function openBatchRuleSelector() {
  if (!batchRuleTargetTable.value || !batchRuleTargetColumns.value.length) return
  ruleSelectorGroup.value = { tableName: batchRuleTargetTable.value, columns: [...batchRuleTargetColumns.value] }
  ruleSelectorIsBatch.value = true
  ruleSelectorBaselineIds.value = []
  ruleSelectorSelectedIds.value = []
  ruleSelectorSearch.value = ''
  ruleSelectorType.value = ''
  ruleSelectorLevel.value = formData.bindSensitivityLevel || ''
  ruleSelectorRules.value = []
  ruleSelectorLoading.value = true
  ruleSelectorVisible.value = true
  try {
    const res: any = await ruleDefApi.page({ pageNum: 1, pageSize: 9999, targetDsId: formData.bindDsId })
    if (res.success !== false && res.data) {
      ruleSelectorRules.value = res.data.records || []
    } else {
      ruleSelectorRules.value = []
    }
  } catch {
    ruleSelectorRules.value = []
  } finally {
    ruleSelectorLoading.value = false
  }
}

// 向后兼容：旧的按表绑定入口（弹窗提示用户选择列）
async function openRuleSelector(group: { tableName: string; columns: string[] }) {
  message.info('请在步骤3中选择列，然后在步骤4中按列为各列绑定规则')
  void group
}

function toggleRuleSelection(rule: RuleDef) {
  const id = rule.id!
  if (ruleSelectorSelectedIds.value.includes(id)) {
    ruleSelectorSelectedIds.value = ruleSelectorSelectedIds.value.filter(x => x !== id)
  } else {
    ruleSelectorSelectedIds.value.push(id)
  }
}

function confirmRuleSelection() {
  if (!ruleSelectorGroup.value) return
  const { tableName, columns } = ruleSelectorGroup.value
  if (!tableName || !columns?.length) return
  const selectedRules = ruleSelectorRules.value.filter(r => ruleSelectorSelectedIds.value.includes(r.id!))
  let binding = ruleBindings.value.find(b => b.tableName === tableName)
  if (!binding) {
    const tbind = tableColumnBindings.value.find(b => b.tableName === tableName)
    binding = { tableName, columns: [...(tbind?.columns || columns)], rules: [] }
    ruleBindings.value.push(binding)
  }
  const targetCols = ruleSelectorIsBatch.value ? columns : [columns[0]]

  if (!ruleSelectorIsBatch.value && targetCols.length === 1) {
    const col = targetCols[0]
    const baseline = new Set(ruleSelectorBaselineIds.value)
    const selected = new Set(ruleSelectorSelectedIds.value)
    for (const id of baseline) {
      if (!selected.has(id)) {
        removeRuleFromBoundField(tableName, col, id)
      }
    }
    for (const r of selectedRules) {
      if (!baseline.has(r.id!)) {
        if (!binding.rules.find(existing => existing.id === r.id && existing.targetColumn === col)) {
          binding.rules.push({
            id: r.id!,
            ruleName: r.ruleName,
            ruleType: r.ruleType || '',
            ruleStrength: r.ruleStrength || 'WEAK',
            enabled: true,
            targetColumn: col
          })
        }
      }
    }
    ruleSelectorVisible.value = false
    return
  }

  if (!selectedRules.length) {
    ruleSelectorVisible.value = false
    return
  }
  for (const r of selectedRules) {
    for (const col of targetCols) {
      if (!binding!.rules.find(existing => existing.id === r.id && existing.targetColumn === col)) {
        binding!.rules.push({
          id: r.id!,
          ruleName: r.ruleName,
          ruleType: r.ruleType || '',
          ruleStrength: r.ruleStrength || 'WEAK',
          enabled: true,
          targetColumn: col
        })
      }
    }
  }
  ruleSelectorVisible.value = false
}

/** 从绑定中移除一条规则：列专属按列删；全表规则（无 targetColumn）删唯一一条 */
function removeRuleFromBoundField(tableName: string, columnName: string, ruleId: number) {
  const binding = ruleBindings.value.find(b => b.tableName === tableName)
  if (!binding?.rules) return
  const idx = binding.rules.findIndex(r => {
    if (r.id !== ruleId) return false
    if (r.targetColumn === columnName) return true
    if (!r.targetColumn || r.targetColumn === '') return true
    return false
  })
  if (idx !== -1) binding.rules.splice(idx, 1)
}

// 从 planApi.getBoundRules 获取数据，按 (table, column) 匹配规则还原到 ruleBindings
function restoreRuleBindings() {
  const bindRulesList = boundRules.value as PlanRuleBindVO[]
  for (const item of bindRulesList) {
    const tbl = item.targetTable || ''
    if (!tbl) continue
    // 找到或创建该表的 ruleBinding 条目
    let binding = ruleBindings.value.find(b => b.tableName === tbl)
    if (!binding) {
      binding = { tableName: tbl, columns: [], rules: [] }
      ruleBindings.value.push(binding)
    }
    // 避免重复追加同一条规则（按 ruleId 判重）
    if (binding.rules.find(existing => existing.id === item.id)) continue
    binding.rules.push({
      id: item.id!,
      ruleName: item.ruleName || '',
      ruleType: item.ruleType || '',
      ruleStrength: item.ruleStrength || 'WEAK',
      enabled: item.enabled !== false,
      targetColumn: item.targetColumn || ''
    })
  }
}

// 并发加载所有已选表的字段（编辑回显时使用）
async function loadColumnsForAllTables(): Promise<void> {
  if (!formData.bindDsId || !tableColumnBindings.value.length) return
  const dsId = formData.bindDsId
  const schema = formData.bindSchema || undefined
  const loaders = tableColumnBindings.value.map(async (binding) => {
    try {
      const res: any = await dataSourceApi.getTableColumns(dsId, binding.tableName, schema)
      const cols: string[] = (res.data || []).map((c: any) => c.columnName as string)
      const existing = [...(binding.columns || [])]
      const existingSet = new Set(existing)
      // 首次进入（尚无列）→ 拉全列；已有子集 → 只追加库中新增的列，不恢复用户已移除的列
      const merged =
        existing.length === 0
          ? cols
          : [...existing, ...cols.filter(c => !existingSet.has(c))]
      return { tableName: binding.tableName, columns: merged }
    } catch {
      // 加载失败保留已有列
      return { tableName: binding.tableName, columns: binding.columns }
    }
  })
  const results = await Promise.all(loaders)
  for (const result of results) {
    const binding = tableColumnBindings.value.find(b => b.tableName === result.tableName)
    if (binding) {
      binding.columns = result.columns
    }
    // 同步 ruleBindings 中的 columns
    const rb = ruleBindings.value.find(b => b.tableName === result.tableName)
    if (rb) {
      rb.columns = result.columns
    }
  }
}

// 全局步骤间校验函数
function validateStep(step: number): string | null {
  if (step === 1) {
    // 步骤2 校验：数据源 + Schema + 至少选一张表
    if (formData.bindDsId == null) return '请选择数据源'
    if (isPostgresBind.value && !formData.bindSchema) return 'PostgreSQL 数据源必须先选择 Schema'
    if (!formData.selectedTableNames?.length) return '请至少选择一张表'
  } else if (step === 2) {
    // 合并步：列 + 规则
    const unselected = tableColumnBindings.value.filter(t => !t.columns?.length)
    if (unselected.length > 0) return `表「${unselected[0].tableName}」未选择任何列，请先通过「批量配置列」勾选字段`
    if (totalBoundRuleCount.value === 0) return '请至少为某一字段绑定一条规则'
  } else if (step === 3) {
    if (formData.triggerType === 'SCHEDULE' && !String(formData.triggerCron || '').trim()) {
      return '定时触发请填写 Cron 表达式'
    }
  }
  return null
}

function nextStep() {
  if (currentStep.value === 0) {
    formRef1.value?.validate().then(() => { currentStep.value++ })
  } else {
    const err = validateStep(currentStep.value)
    if (err) {
      message.warning(err)
      return
    }
    // 步骤2 → 合并步：初始化 tableColumnBindings（如尚未初始化）
    if (currentStep.value === 1 && tableColumnBindings.value.length === 0 && formData.selectedTableNames?.length) {
      onSelectedTablesChange()
    }
    currentStep.value++
    // 进入「字段与规则」步时拉全表字段
    if (currentStep.value === 2 && formData.bindDsId) {
      void loadColumnsForAllTables()
    }
  }
}

function resetFormData() {
  Object.assign(formData, {
    planName: '',
    planCode: '',
    planDesc: '',
    bindValue: '',
    bindDsId: undefined,
    bindSchema: '',
    bindDimension: '' as '' | 'SENSITIVITY_LEVEL' | undefined,
    bindSensitivityLevel: '' as '' | 'L4' | 'L3' | 'L2' | 'L1' | undefined,
    selectedTableNames: [],
    triggerType: 'MANUAL',
    triggerCron: '',
    alertOnSuccess: false,
    alertOnFailure: true,
    autoBlock: true
  })
  schemaList.value = []
  tableNameOptions.value = []
  tableColumnBindings.value = []
  ruleBindings.value = []
  boundRules.value = []
  lastAutoPlanName.value = ''
}

async function handleSubmit(publishAfter = false) {
  if (publishAfter && totalBoundRuleCount.value === 0) {
    message.warning('发布需要至少绑定 1 条规则')
    return
  }
  submitLoading.value = true
  try {
    const dto: any = {
      planName: formData.planName,
      planCode: formData.planCode,
      planDesc: formData.planDesc,
      bindType: 'TABLE',
      bindValue: buildBindValue(),
      layerCode: getDsLayerTag() || '',
      triggerType: formData.triggerType,
      triggerCron: formData.triggerCron,
      alertOnSuccess: formData.alertOnSuccess,
      alertOnFailure: formData.alertOnFailure,
      autoBlock: formData.autoBlock,
      status: 'DRAFT',
      tableCount: formData.selectedTableNames?.length || 0,
      sensitivityLevel: formData.bindSensitivityLevel || undefined,
    }

    // bindPayload：每个 (table, column, rule) 一行，带 targetTable/targetColumn
    const bindPayload: {
      ruleId: number
      ruleOrder: number
      enabled: boolean
      skipOnFailure: boolean
      targetTable: string
      targetColumn: string
    }[] = []
    let ruleOrder = 0
    for (const group of ruleBindingGroups.value) {
      for (const [col, rules] of group.colRuleMap) {
        for (const r of rules) {
          bindPayload.push({
            ruleId: r.id,
            ruleOrder: ruleOrder++,
            enabled: r.enabled,
            skipOnFailure: false,
            targetTable: group.tableName,
            targetColumn: col
          })
        }
      }
    }

    if (isEdit.value) {
      const resUp: any = await planApi.update(editingId.value!, dto)
      if (resUp.success === false) throw new Error(resUp.message || '更新失败')
      const resBind: any = await planApi.bindRules(editingId.value!, bindPayload)
      if (resBind.success === false) throw new Error(resBind.message || '绑定规则失败')
      if (publishAfter) {
        const resPub: any = await planApi.publish(editingId.value!)
        if (resPub.success === false) throw new Error(resPub.message || '发布失败')
        message.success('保存并已发布')
      } else {
        message.success('修改成功')
      }
    } else {
      const res: any = await planApi.create(dto)
      if (res.success === false || res.data == null) throw new Error(res.message || '创建失败')
      const newId = res.data as number
      const resBind: any = await planApi.bindRules(newId, bindPayload)
      if (resBind.success === false) throw new Error(resBind.message || '绑定规则失败')
      if (publishAfter) {
        const resPub: any = await planApi.publish(newId)
        if (resPub.success === false) throw new Error(resPub.message || '发布失败')
        message.success('创建并已发布')
      } else {
        message.success('创建成功')
      }
    }

    modalVisible.value = false
    await loadData()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleSaveDraft() {
  submitLoading.value = true
  try {
    const dto: any = {
      planName: formData.planName || '未命名方案',
      planCode: formData.planCode,
      planDesc: formData.planDesc,
      triggerType: formData.triggerType,
      triggerCron: formData.triggerCron,
      alertOnSuccess: formData.alertOnSuccess,
      alertOnFailure: formData.alertOnFailure,
      autoBlock: formData.autoBlock,
      status: 'DRAFT'
    }
    if (isEdit.value) {
      await planApi.update(editingId.value!, dto)
    } else {
      await planApi.create(dto)
    }
    message.success('草稿已保存')
    modalVisible.value = false
    loadData()
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

function getLayerColor(layer: string) {
  const map: Record<string, string> = { ODS: 'blue', DWD: 'green', DWS: 'orange', ADS: 'purple' }
  return map[layer] || 'default'
}

function getTriggerColor(trigger: string) {
  const map: Record<string, string> = { MANUAL: 'blue', SCHEDULE: 'purple', API: 'green' }
  return map[trigger] || 'default'
}

function getTriggerLabel(trigger: string) {
  const map: Record<string, string> = { MANUAL: '手动触发', SCHEDULE: '定时触发', API: 'API触发' }
  return map[trigger] || trigger
}

function getStatusClass(status: string) {
  const map: Record<string, string> = { PUBLISHED: 'status-enabled', DRAFT: 'status-draft', DISABLED: 'status-disabled' }
  return map[status] || 'status-draft'
}

function getStatusText(status: string) {
  const map: Record<string, string> = { PUBLISHED: '已发布', DRAFT: '草稿', DISABLED: '已停用' }
  return map[status] || status
}

function getStatusBadge(status: string) {
  const map: Record<string, any> = { PUBLISHED: 'success', DRAFT: 'warning', DISABLED: 'default' }
  return map[status] || 'default'
}

function getExecProgress(planId: number) {
  const execId = runningExecutionMap.value.get(planId)
  if (!execId) return null
  return executionProgressMap.value.get(execId) ?? null
}

function getExecProgressPercent(planId: number): number {
  const prog = getExecProgress(planId)
  if (!prog) return 0
  return prog.displayPercent ?? 0
}

function computeDisplayPercent(
  exec: Execution,
  detailsList: any[]
): { displayPercent: number; currentRuleName?: string } {
  const total = exec.totalRules ?? 0
  const passed = exec.passedRules ?? 0
  const failed = exec.failedRules ?? 0
  const skipped = exec.skippedRules ?? 0
  const fromExec =
    total > 0 ? Math.round(((passed + failed + skipped) / total) * 100) : 0

  let currentRuleName: string | undefined
  const running = detailsList.filter(d => d.status === 'RUNNING')
  if (running.length) {
    const last = running.reduce((a, b) => (a.id > b.id ? a : b))
    currentRuleName = last.ruleName as string
  }

  const doneFromDetails = detailsList.filter(d => d.status !== 'RUNNING').length
  const fromDetails = total > 0 ? Math.round((doneFromDetails / total) * 100) : 0
  let displayPercent = Math.max(fromExec, fromDetails)
  if (exec.status === 'RUNNING' && total > 0 && running.length > 0 && displayPercent < 100) {
    displayPercent = Math.max(
      displayPercent,
      Math.round(((doneFromDetails + 0.5) / total) * 100)
    )
  }
  return { displayPercent, currentRuleName }
}

function goPlanExecutionLogs(plan: Plan) {
  sessionStorage.setItem('dqc-exec-filter-plan', String(plan.id))
  const execId = runningExecutionMap.value.get(plan.id!)
  if (execId) {
    sessionStorage.setItem('dqc-exec-open-exec', String(execId))
  } else {
    sessionStorage.removeItem('dqc-exec-open-exec')
  }
  router.push({ path: '/dqc/execution' })
}

function getScoreColor(score: number) {
  if (score >= 90) return '#52C41A'
  if (score >= 70) return '#FA8C16'
  return '#FF4D4F'
}

function getComplianceRateColor(rate: number) {
  if (rate >= 90) return '#52C41A'
  if (rate >= 70) return '#FA8C16'
  return '#FF4D4F'
}

function getSensitivityTextColor(level: string) {
  const map: Record<string, string> = { L4: '#FF4D4F', L3: '#FA8C16', L2: '#1677FF', L1: '#52C41A' }
  return map[level] || '#8C8C8C'
}

function getRuleTypeLabel(type: string) {
  const map: Record<string, string> = {
    NULL_CHECK: '空值检查',
    UNIQUE_CHECK: '唯一性',
    REGEX_PHONE: '手机格式',
    THRESHOLD_RANGE: '值域校验',
    CROSS_FIELD_COMPARE: '跨字段',
    CUSTOM_SQL: '自定义SQL',
    ROW_COUNT_FLUCTUATION: '波动监测'
  }
  return map[type] || type
}

function getRuleTypeClass(type: string) {
  const map: Record<string, string> = {
    NULL_CHECK: 'type-null',
    UNIQUE_CHECK: 'type-unique',
    REGEX_PHONE: 'type-regex',
    THRESHOLD_RANGE: 'type-threshold',
    CROSS_FIELD_COMPARE: 'type-cross',
    CUSTOM_SQL: 'type-custom'
  }
  return map[type] || 'type-default'
}

onMounted(() => {
  loadData()
})

// 页面卸载时清理所有轮询定时器，防止内存泄漏
onUnmounted(() => {
  pollingTimers.forEach(timerId => clearInterval(timerId))
  pollingTimers.clear()
  executionProgressMap.value.clear()
  runningExecutionMap.value.clear()
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

.plan-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
  border: 1px solid transparent;
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
    border-color: #1677FF;
  }
  &.plan-card-disabled {
    opacity: 0.6;
  }
  .plan-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    .plan-header-left {
      display: flex;
      align-items: center;
      gap: 8px;
      .plan-status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        &.status-enabled { background: #52C41A; }
        &.status-draft { background: #FA8C16; }
        &.status-disabled { background: #d9d9d9; }
      }
      .plan-name {
        font-size: 15px;
        font-weight: 600;
        color: #1F1F1F;
      }
    }
  }
  .plan-card-body {
    .plan-info-row {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      margin-bottom: 12px;
    }
    .plan-metrics {
      display: flex;
      gap: 16px;
      margin-bottom: 12px;
      .plan-metric {
        display: flex;
        flex-direction: column;
        .metric-value {
          font-size: 20px;
          font-weight: 700;
          color: #1F1F1F;
        }
        .metric-label {
          font-size: 12px;
          color: #8C8C8C;
          margin-top: 2px;
        }
      }
    }
    .plan-desc {
      font-size: 13px;
      color: #8C8C8C;
      line-height: 1.5;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }
    .progress-current-rule {
  font-size: 12px;
  margin-top: 4px;
  color: #8c8c8c;
  line-height: 1.4;
}

.execution-progress {
      margin-top: 8px;
      padding: 8px 10px;
      background: linear-gradient(135deg, rgba(114,46,209,0.06) 0%, rgba(146,84,222,0.08) 100%);
      border: 1px solid rgba(114,46,209,0.15);
      border-radius: 6px;
    }
    .progress-label {
      font-size: 12px;
      color: #722ED1;
      display: flex;
      align-items: center;
      gap: 6px;
    }
  }
  .plan-card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
    margin-top: 12px;
    .plan-time { font-size: 12px; color: #8C8C8C; }
    .plan-actions { display: flex; gap: 8px; }
  }
}

.empty-card {
  background: #fff;
  border: 2px dashed #d9d9d9;
  border-radius: 12px;
  padding: 48px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.3s;
  &:hover { border-color: #1677FF; }
  .empty-icon {
    font-size: 48px;
    color: #d9d9d9;
    margin-bottom: 16px;
  }
  .empty-title { font-size: 16px; font-weight: 600; color: #1F1F1F; margin-bottom: 8px; }
  .empty-desc { font-size: 14px; color: #8C8C8C; margin-bottom: 20px; }
}

/* 向导：不用 flex 链压高度，防止 Modal 内容区高度为 0 */
.wizard-container {
  display: flex;
  flex-direction: column;
  min-height: 420px;
}

.wizard-main {
  display: block;
  flex: 1 1 auto;
  min-height: 0;
}

.wizard-steps {
  display: flex;
  align-items: flex-start;
  flex-shrink: 0;
  margin-bottom: 20px;
  padding: 0 4px;
  gap: 0;
  .wizard-step {
    display: flex;
    align-items: center;
    flex: 1 1 0;
    min-width: 0;
    cursor: pointer;
    .step-circle {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 600;
      flex-shrink: 0;
      transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.35s ease, background 0.25s ease, color 0.25s ease;
    }
    .step-info {
      margin-left: 8px;
      min-width: 0;
      flex: 0 1 auto;
      max-width: 11rem;
      .step-title {
        font-size: 13px;
        font-weight: 600;
        color: #1F1F1F;
        line-height: 1.3;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        transition: color 0.25s ease;
      }
      .step-desc {
        font-size: 11px;
        color: #8C8C8C;
        margin-top: 2px;
        line-height: 1.35;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        word-break: break-all;
      }
    }
    /* 贯穿至下一步圆点的连线：占满步骤间剩余空间（原 max-width:56px 会导致断线） */
    .step-connector {
      flex: 1 1 0;
      min-width: 8px;
      align-self: center;
      padding: 0 4px 0 8px;
      display: flex;
      align-items: center;
      height: 32px;
      .connector-line {
        width: 100%;
        height: 3px;
        border-radius: 2px;
        background: #d9d9d9;
        transition: background 0.45s ease, box-shadow 0.45s ease;
        position: relative;
        overflow: hidden;
        &.connector-done {
          background: linear-gradient(90deg, #69b1ff 0%, #1677ff 50%, #0958d9 100%);
          background-size: 200% 100%;
          animation: plan-wizard-connector-flow 2.2s ease-in-out infinite;
        }
        &.connector-active:not(.connector-done) {
          background: linear-gradient(90deg, #e6f4ff 0%, #1677ff 45%, #69b1ff 100%);
          background-size: 220% 100%;
          animation: plan-wizard-connector-flow 1.6s ease-in-out infinite;
          box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.15);
        }
      }
    }
    &.step-active {
      .step-circle {
        background: #1677FF;
        color: #fff;
        box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.22);
        animation: plan-wizard-step-pulse 2s ease-in-out infinite;
      }
      .step-title { color: #1677FF; }
    }
    &.step-done {
      .step-circle { background: #52C41A; color: #fff; }
    }
    &.step-pending {
      .step-circle { background: #F0F0F0; color: #8C8C8C; }
    }
  }
}

@keyframes plan-wizard-connector-flow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@keyframes plan-wizard-step-pulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.18); }
  50% { box-shadow: 0 0 0 7px rgba(22, 119, 255, 0.12); }
}

@media (prefers-reduced-motion: reduce) {
  .wizard-steps .wizard-step .step-connector .connector-line {
    animation: none !important;
    &.connector-done { background: #1677ff; }
    &.connector-active:not(.connector-done) { background: #91caff; }
  }
  .wizard-steps .wizard-step.step-active .step-circle {
    animation: none;
    box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.2);
  }
}

.wizard-content {
  overflow-x: hidden;
  padding-right: 4px;
}

.wizard-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  padding-top: 16px;
  margin-top: 0;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  box-shadow: 0 -6px 16px rgba(0, 0, 0, 0.06);
}

.step-guide {
  background: #E6F7FF;
  border: 1px solid #91D5FF;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 13px;
  color: #1677FF;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.bind-type-selector {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  .bind-type-card {
    flex: 1;
    background: #fff;
    border: 2px solid #f0f0f0;
    border-radius: 12px;
    padding: 20px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;
    &:hover { border-color: #1677FF; }
    &.bind-type-selected {
      border-color: #1677FF;
      background: #E6F7FF;
    }
    .bind-type-icon {
      font-size: 32px;
      color: #1677FF;
      margin-bottom: 8px;
    }
    .bind-type-name { font-size: 14px; font-weight: 600; color: #1F1F1F; margin-bottom: 4px; }
    .bind-type-desc { font-size: 12px; color: #8C8C8C; }
  }
}

.field-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; }
.selected-count { font-size: 14px; color: #1677FF; }

.bind-dim-option {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rule-binding-area {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  min-height: 280px;
  height: clamp(280px, 42vh, 400px);
  max-height: 400px;
  .rule-available, .rule-bound {
    background: #FAFAFA;
    border-radius: 8px;
    border: 1px solid #f0f0f0;
    display: flex;
    flex-direction: column;
    min-height: 0;
    .rule-area-title {
      flex-shrink: 0;
      padding: 10px 12px;
      font-size: 13px;
      font-weight: 600;
      color: #1F1F1F;
      border-bottom: 1px solid #f0f0f0;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      .rule-count-badge {
        font-size: 12px;
        font-weight: 400;
        color: #8C8C8C;
        margin-left: 0;
      }
    }
    .rule-list {
      flex: 1 1 0;
      min-height: 0;
      overflow-y: auto;
      overflow-x: hidden;
      padding: 8px;
      -webkit-overflow-scrolling: touch;
    }
  }
  .rule-item {
    display: flex;
    align-items: center;
    padding: 8px 12px;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.2s;
    margin-bottom: 4px;
    &:hover { background: #E6F7FF; }
    &.rule-item-disabled { opacity: 0.4; cursor: not-allowed; pointer-events: none; }
    &.bound-rule-item { background: #fff; border: 1px solid #f0f0f0; }
    .rule-item-left { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; }
    .rule-name {
      font-size: 13px;
      color: #1F1F1F;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1 1 0;
      min-width: 0;
      max-width: none;
    }
    .rule-order {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 20px;
      height: 20px;
      background: #1677FF;
      color: #fff;
      border-radius: 50%;
      font-size: 11px;
      font-weight: 600;
      flex-shrink: 0;
    }
    .rule-add-icon { color: #1677FF; font-size: 14px; }
    .drag-handle { color: #d9d9d9; font-size: 14px; cursor: grab; margin-right: 8px; flex-shrink: 0; }
    .rule-item-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
  }
  .rule-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #d9d9d9;
    font-size: 13px;
    gap: 8px;
  }
}

.rule-type-tag {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 500;
  &.type-null      { background: #E6F7FF; color: #1677FF; }
  &.type-unique    { background: #F6FFED; color: #52C41A; }
  &.type-regex     { background: #FFF7E6; color: #FA8C16; }
  &.type-threshold { background: #FFF1F0; color: #FF4D4F; }
  &.type-cross     { background: #F9F0FF; color: #722ED1; }
  &.type-custom    { background: #E6F7FF; color: #13C2C2; }
}

.trigger-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  .trigger-icon-wrap {
    font-size: 20px;
    color: #1677FF;
  }
  strong { display: block; font-size: 14px; color: #1F1F1F; }
  .trigger-desc { font-size: 12px; color: #8C8C8C; }
}

.switch-label { margin-left: 10px; font-size: 13px; }

.cron-helper {
  font-size: 13px;
  div {
    padding: 6px 0;
    cursor: pointer;
    color: #1677FF;
    border-bottom: 1px solid #f0f0f0;
    &:last-child { border-bottom: none; }
    &:hover { color: #0958D9; }
    code {
      font-family: 'JetBrains Mono', monospace;
      font-size: 12px;
      background: #f5f5f5;
      padding: 1px 4px;
      border-radius: 3px;
      margin-left: 8px;
    }
  }
}

.preview-section {
  .preview-title { font-size: 15px; font-weight: 600; color: #1F1F1F; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
}

/* 详情抽屉 */
.detail-section {
  margin-bottom: 24px;
  .detail-title {
    font-size: 15px;
    font-weight: 600;
    color: #1F1F1F;
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    gap: 8px;
    .detail-count {
      font-size: 12px;
      font-weight: 400;
      color: #8C8C8C;
    }
  }
}

.detail-rules {
  .detail-rule-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 0;
    border-bottom: 1px solid #f0f0f0;
    &:last-child { border-bottom: none; }
    .detail-rule-order {
      width: 20px;
      height: 20px;
      background: #E6F7FF;
      color: #1677FF;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 11px;
      font-weight: 600;
      flex-shrink: 0;
    }
    .detail-rule-name { flex: 1; font-size: 13px; color: #1F1F1F; }
    .skip-tag {
      font-size: 11px;
      color: #8C8C8C;
      background: #f5f5f5;
      padding: 1px 6px;
      border-radius: 3px;
    }
  }
  .detail-empty {
    text-align: center;
    color: #d9d9d9;
    padding: 20px 0;
    font-size: 13px;
  }
}

/* 步骤4 组内规则展示 */
.group-rules {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.group-rule-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #FAFAFA;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  flex-wrap: wrap;
  .group-rule-name {
    font-size: 12px;
    color: #1F1F1F;
    flex: 1 1 auto;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .group-rule-type {
    display: inline-block;
    padding: 1px 4px;
    border-radius: 2px;
    font-size: 10px;
    font-weight: 500;
    &.type-null      { background: #E6F7FF; color: #1677FF; }
    &.type-unique    { background: #F6FFED; color: #52C41A; }
    &.type-regex     { background: #FFF7E6; color: #FA8C16; }
    &.type-threshold { background: #FFF1F0; color: #FF4D4F; }
    &.type-cross     { background: #F9F0FF; color: #722ED1; }
    &.type-custom    { background: #E6F7FF; color: #13C2C2; }
    &.type-default   { background: #f5f5f5; color: #8C8C8C; }
  }
}

/* 步骤4 按列分组展示规则 */
.column-rule-block {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 8px;
  overflow: hidden;
  &:last-child { margin-bottom: 0; }
  .column-rule-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 10px;
    background: #FAFAFA;
    border-bottom: 1px solid #f0f0f0;
    flex-wrap: wrap;
    .column-rule-count {
      font-size: 12px;
      color: #8C8C8C;
    }
  }
  .column-rules-list {
    padding: 6px 8px;
  }
  .column-rules-empty {
    padding: 6px 10px;
    font-size: 12px;
    color: #d9d9d9;
  }
}

/* 规则选择弹窗 */
.rule-selector-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.15s;
  &:hover { background: #E6F7FF; }
  &.rule-selector-item-selected { background: #F6FFED; }
  &:last-child { border-bottom: none; }
  .rule-selector-info {
    flex: 1 1 0;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .rule-selector-name {
    font-size: 13px;
    font-weight: 600;
    color: #1F1F1F;
  }
  .rule-sql-preview {
    flex-shrink: 0;
    align-self: center;
  }
  .rule-selector-expr {
    font-size: 11px;
    color: #8C8C8C;
    font-family: 'JetBrains Mono', monospace;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 300px;
  }
}

/* SQL 预览面板（独立于规则行） */
.rule-sql-panel {
  width: 100%;
  flex-basis: 100%;
  margin-top: 4px;
  padding: 8px 10px;
  background: #1F1F1F;
  border-radius: 6px;
  overflow-x: auto;
  .sql-pre {
    margin: 0;
    font-family: 'JetBrains Mono', monospace;
    font-size: 12px;
    color: #7EC8E3;
    white-space: pre-wrap;
    word-break: break-all;
    line-height: 1.6;
  }
}

.rule-sens-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 6px;
  border: 1px solid;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 700;
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

.sens-level-preview {
  margin: 16px 0 0;
  background: linear-gradient(135deg, rgba(250, 173, 20, 0.04) 0%, rgba(255, 77, 79, 0.04) 100%);
  border: 1px solid rgba(250, 173, 20, 0.25);
  border-radius: 10px;
  padding: 14px 16px;

  .preview-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    font-weight: 600;
    color: #1F1F1F;
    margin-bottom: 8px;
  }

  .preview-tip {
    font-size: 12px;
    color: #8C8C8C;
    line-height: 1.6;
    margin-bottom: 10px;
  }

  .preview-fields {
    .preview-fields-label {
      font-size: 12px;
      color: #8C8C8C;
      margin-bottom: 8px;
    }

    .preview-fields-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      max-height: 120px;
      overflow-y: auto;
    }
  }

  .preview-empty {
    font-size: 12px;
    color: #FA8C16;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px;
    background: rgba(250, 173, 20, 0.06);
    border-radius: 6px;
  }
}

/* ---------- 新增样式 ---------- */
/* 步骤3工具条 */
.col-toolbar {
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #F0F5FF;
  border: 1px solid #ADC6FF;
  border-radius: 6px;
}

/* 步骤3已选列摘要 */
.col-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  min-height: 24px;
}

/* 批量选列工具条 */
.batch-col-toolbar {
  display: flex;
  align-items: center;
}

/* 批量选列表名标签 */
.batch-col-table-badge {
  display: inline-block;
  background: #E6F4FF;
  color: #1677FF;
  font-size: 11px;
  padding: 1px 5px;
  border-radius: 3px;
  margin-right: 4px;
  flex-shrink: 0;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 步骤4工具条 */
.rule-toolbar {
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #F9F0FF;
  border: 1px solid #D3ADF7;
  border-radius: 6px;
}

/* 步骤4每列规则摘要行 */
.col-rule-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.col-rule-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
}

.col-rule-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  flex: 1 1 0;
}

.col-rule-actions {
  flex-shrink: 0;
}

.text-muted-field {
  color: #bfbfbf;
  font-size: 13px;
}

.field-rules-toolbar {
  margin-bottom: 12px;
}

.step-guide-lg {
  font-size: 15px;
  line-height: 1.65;
  padding: 12px 14px;
}

.wizard-step-field-rules {
  min-height: 320px;
}

/* 第 3 步：表格带纵向 scroll.y，外层跟弹窗一起滚即可 */
.wizard-content.wizard-step-field-rules {
  padding-right: 0;
  .field-rules-table {
    min-height: 200px;
  }
}

.field-rules-table {
  :deep(.ant-table) {
    font-size: 14px;
  }
  :deep(.ant-table-thead > tr > th) {
    font-size: 14px;
    padding: 12px 10px;
  }
  :deep(.ant-table-tbody > tr > td) {
    padding: 10px;
  }
}

.field-rule-count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: -0.02em;
  color: #722ed1;
  background: rgba(114, 46, 209, 0.06);
  border: 1px solid rgba(114, 46, 209, 0.28);
  border-radius: 999px;
  box-sizing: border-box;
  vertical-align: middle;
}
.field-rule-count-empty {
  font-size: 12px;
  color: #bfbfbf;
  user-select: none;
}
</style>

<!-- 弹窗挂载在 body，需非 scoped 才能作用到 wrap -->
<style lang="less">
.plan-wizard-modal-wrap {
  .ant-modal {
    max-width: 98vw !important;
    top: 12px;
    padding-bottom: 0;
  }
  .ant-modal-content {
    border-radius: 10px;
  }
  .ant-modal-header {
    padding: 16px 20px;
  }
  .ant-modal-title {
    font-size: 17px;
    font-weight: 600;
  }
  .ant-modal-body {
    font-size: 15px;
    line-height: 1.55;
  }
  .wizard-container .step-title {
    font-size: 14px;
  }
  .wizard-container .step-desc {
    font-size: 12px;
  }
}
</style>
