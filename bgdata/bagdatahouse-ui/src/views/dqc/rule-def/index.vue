<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#grad2)"/>
            <path d="M9 12L11 14L15 10" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="12" cy="12" r="9" stroke="white" stroke-width="1.5" opacity="0.5"/>
            <defs>
              <linearGradient id="grad2" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#52C41A"/>
                <stop offset="100%" stop-color="#73D13D"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">规则定义</h1>
          <p class="page-subtitle">配置质量检测规则，支持 21 种内置模板与自定义函数注册</p>
        </div>
      </div>
      <div class="header-right">
        <a-button
          v-if="hasSelected"
          danger
          @click="handleBatchDelete"
        >
          <template #icon><DeleteOutlined /></template>
          批量删除 ({{ selectedRowKeys.length }})
        </a-button>
        <a-button type="primary" @click="openCreateDrawer">
          <template #icon><PlusOutlined /></template>
          新建规则
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="8" v-for="stat in statCards" :key="stat.label">
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
          placeholder="搜索规则名称"
          style="width: 200px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="filterType"
          placeholder="规则类型"
          style="width: 150px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部类型</a-select-option>
          <a-select-option v-for="t in ruleTypeOptions" :key="t.value" :value="t.value">
            {{ t.label }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filterDsId"
          placeholder="数据源"
          style="width: 200px"
          allowClear
          showSearch
          :filter-option="(input: string, option: any) => (option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
          @change="loadData"
        >
          <a-select-option
            v-for="ds in dataSourceOptions"
            :key="ds.id"
            :value="ds.id"
            :label="`${ds.dsName} ${ds.dsType}`"
          >
            {{ ds.dsName }}
            <span class="ds-type-badge">{{ ds.dsType }}</span>
            <a-tag v-if="ds.dataLayer" size="small" style="margin-left: 6px">{{ ds.dataLayer }}</a-tag>
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filterStrength"
          placeholder="规则强度"
          style="width: 120px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="STRONG">强规则</a-select-option>
          <a-select-option value="WEAK">弱规则</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterEnabled"
          placeholder="启用状态"
          style="width: 110px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option :value="1">已启用</a-select-option>
          <a-select-option :value="0">已禁用</a-select-option>
        </a-select>
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
        :row-selection="rowSelection"
        :row-key="(record: any) => record.id"
        @change="handleTableChange"
        :scroll="{ x: 1540 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ruleType'">
            <span class="rule-type-tag" :class="getRuleTypeClass(record.ruleType)">
              {{ getRuleTypeLabel(record.ruleType) }}
            </span>
          </template>

          <template v-if="column.key === 'ruleStrength'">
            <a-tag :color="record.ruleStrength === 'STRONG' ? 'red' : 'orange'">
              {{ record.ruleStrength === 'STRONG' ? '强规则' : '弱规则' }}
            </a-tag>
          </template>

          <template v-if="column.key === 'errorLevel'">
            <a-tag :color="getErrorLevelColor(record.errorLevel)">
              {{ getErrorLevelLabel(record.errorLevel) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'layerCode'">
            <a-tag :color="getLayerColor(record.layerCode)">
              {{ record.layerCode || '-' }}
            </a-tag>
          </template>

          <template v-if="column.key === 'enabled'">
            <a-switch
              :checked="record.enabled"
              size="small"
              @change="(checked: boolean) => handleToggleEnabled(record, checked)"
            />
          </template>

          <template v-if="column.key === 'hintContent'">
            <div class="hint-content-row">
              <a-tooltip v-if="record.ruleExpr?.trim()" :title="record.ruleExpr" placement="topLeft">
                <span class="hint-content-text">{{ record.ruleExpr }}</span>
              </a-tooltip>
              <span v-else class="hint-content-text">—</span>
              <a-space size="small" class="hint-content-actions" @click.stop>
                <a-tooltip title="查看">
                  <a-button type="text" size="small" @click="handleView(record)">
                    <template #icon><EyeOutlined /></template>
                  </a-button>
                </a-tooltip>
                <a-tooltip title="复制">
                  <a-button type="text" size="small" @click="handleCopy(record)">
                    <template #icon><CopyOutlined /></template>
                  </a-button>
                </a-tooltip>
                <a-tooltip title="编辑">
                  <a-button type="text" size="small" @click="handleEdit(record)">
                    <template #icon><EditOutlined /></template>
                  </a-button>
                </a-tooltip>
                <a-tooltip title="删除">
                  <a-button type="text" size="small" danger @click="handleDelete(record)">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-tooltip>
              </a-space>
            </div>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="drawerTitle"
      :width="760"
      placement="right"
      :destroy-on-close="true"
      @after-open-change="onDrawerAfterOpenChange"
    >
      <a-steps :current="currentStep" size="small" class="drawer-steps">
        <a-step title="基本信息" />
        <a-step title="配置规则" />
        <a-step title="质量维度与强度" />
        <a-step title="预览保存" />
      </a-steps>

      <!-- Step 1: 基本信息 -->
      <div v-show="currentStep === 0" class="step-content">
        <a-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="规则名称" name="ruleName">
            <a-input v-model:value="formData.ruleName" placeholder="请输入规则名称，如 ODS层空值检查" />
          </a-form-item>
          <a-form-item label="规则类型" name="ruleType">
            <a-select
              v-model:value="formData.ruleType"
              placeholder="请选择规则类型"
              @change="onRuleTypeChange"
            >
              <a-select-option v-for="t in ruleTypeOptions" :key="t.value" :value="t.value">
                {{ t.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="规则编码" name="ruleCode">
            <a-input
              v-model:value="formData.ruleCode"
              placeholder="唯一编码，系统自动生成"
              :disabled="isEdit"
            >
              <template #suffix>
                <a-button type="link" size="small" @click="generateCode" style="padding: 0; height: auto">
                  <SyncOutlined /> 自动生成
                </a-button>
              </template>
            </a-input>
          </a-form-item>
          <a-form-item label="关联模板" name="templateId">
            <a-select
              v-model:value="formData.templateId"
              placeholder="请选择关联模板，可选择无模板手动配置"
              allowClear
              @change="onTemplateChange"
            >
              <a-select-option v-for="t in templateOptions" :key="t.id" :value="t.id">
                {{ t.templateName }} ({{ getRuleTypeLabel(t.ruleType) }})
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="适用级别" name="applyLevel">
            <a-select v-model:value="formData.applyLevel" placeholder="请选择适用级别">
              <a-select-option v-for="l in applyLevelOptions" :key="l.value" :value="l.value">
                {{ l.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="目标数据源" name="targetDsId">
            <a-select
              v-model:value="formData.targetDsId"
              placeholder="请选择目标数据源（与探查任务一致）"
              allowClear
              showSearch
              :filter-option="(input: string, option: any) => (option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
              @change="onTargetDsChange"
            >
              <a-select-option
                v-for="ds in dataSourceOptions"
                :key="ds.id"
                :value="ds.id"
                :label="`${ds.dsName} ${ds.dsType}`"
              >
                <span class="ds-type-badge">{{ ds.dsType }}</span>
                {{ ds.dsName }}
                <a-tag v-if="ds.dataLayer" size="small" style="margin-left: 6px">{{ ds.dataLayer }}</a-tag>
              </a-select-option>
            </a-select>
            <div v-if="formData.layerCode" class="field-tip">
              数仓层标签由所选数据源自动带出：{{ formData.layerCode }}
            </div>
            <div v-else-if="formData.targetDsId" class="field-tip">
              当前数据源未配置数仓层标签，保存后列表「数仓层」可能为空；可在数据源管理中补充 data_layer。
            </div>
          </a-form-item>
        </a-form>

        <!-- 模板提示 -->
        <div v-if="selectedTemplate" class="template-hint">
          <div class="hint-title">
            <InfoCircleOutlined /> 模板说明
          </div>
          <p v-if="selectedTemplate">模板类型：<span class="rule-type-tag" :class="getRuleTypeClass(selectedTemplate?.ruleType)">{{ getRuleTypeLabel(selectedTemplate?.ruleType) }}</span></p>
          <p v-if="selectedTemplate">质量维度：<a-tag color="cyan">{{ getDimensionLabel(selectedTemplate?.dimension || selectedTemplate?.qualityDimension) }}</a-tag></p>
          <p v-if="selectedTemplate?.templateDesc">模板描述：{{ selectedTemplate?.templateDesc }}</p>
          <div v-if="selectedTemplate?.defaultExpr || selectedTemplate?.ruleExpr" class="hint-expr">
            <div class="hint-expr-title">默认表达式</div>
            <pre class="hint-expr-code">{{ selectedTemplate?.defaultExpr || selectedTemplate?.ruleExpr }}</pre>
          </div>
        </div>
      </div>

      <!-- Step 2: 配置规则 -->
      <div v-show="currentStep === 1" class="step-content">
        <a-form
          ref="formRef2"
          :model="formData"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
        >
          <!-- MySQL 数据库名 / PostgreSQL schema 选择器 -->
          <template v-if="formData.targetDsId">
            <a-form-item
              v-if="dsDbName"
              label="数据库"
              :label-col="{ span: 5 }"
              :wrapper-col="{ span: 18 }"
            >
              <a-tag color="blue">{{ dsDbName }}</a-tag>
              <span v-if="dsSchemaName" style="margin-left: 8px">
                <a-tag color="purple">{{ dsSchemaName }}</a-tag>
              </span>
            </a-form-item>
            <a-form-item
              v-else-if="dsSchemaName"
              label="Schema"
              :label-col="{ span: 5 }"
              :wrapper-col="{ span: 18 }"
            >
              <a-tag color="purple">{{ dsSchemaName }}</a-tag>
            </a-form-item>
          </template>

          <!-- 目标表名 -->
          <a-form-item label="目标表名" name="targetTable">
            <a-select
              v-model:value="formData.targetTable"
              placeholder="请先选择数据源，然后选择表名"
              :loading="tableLoading"
              :disabled="!formData.targetDsId"
              showSearch
              allowClear
              :filter-option="(input: string, option: any) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
              @search="tableSearchValue = $event"
              @change="onTableChange"
            >
              <a-select-option v-for="t in tableOptions" :key="t.value" :value="t.value" :label="t.label">
                {{ t.label }}
              </a-select-option>
            </a-select>
            <div v-if="!formData.targetDsId" class="field-tip">
              请先选择数据源
            </div>
          </a-form-item>

          <!-- 预览用列：紧挨目标表名；表级表达式含 ${partition_column} 等时也必须可见 -->
          <a-form-item
            v-if="formData.targetDsId"
            label="预览用列"
            :label-col="{ span: 5 }"
            :wrapper-col="{ span: 18 }"
          >
            <a-select
              v-model:value="previewColumnForExpr"
              :placeholder="previewColumnPlaceholder"
              allow-clear
              show-search
              :loading="columnLoading"
              :disabled="!formData.targetTable"
              :options="columnOptions"
              style="max-width: 100%"
            />
            <div v-if="!formData.targetTable" class="field-tip">请先选择目标表以加载字段列表</div>
            <div v-else-if="isTableLevelWithColumnPlaceholders" class="field-tip">
              当前为表级规则，表达式含分区/列占位符时请在此选择用于预览与「运行预览查询」的列
            </div>
          </a-form-item>

          <!-- 目标列名（字段级，级联下拉） -->
          <a-form-item
            v-if="formData.applyLevel === 'COLUMN' || formData.applyLevel === 'CROSS_FIELD'"
            label="目标列名"
            name="targetColumn"
          >
            <a-select
              v-model:value="formData.targetColumn"
              placeholder="请先选择目标表，然后选择列名"
              :loading="columnLoading"
              :disabled="!formData.targetTable"
              show-search
              allow-clear
              :filter-option="(input: string, option: any) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
            >
              <a-select-option v-for="col in columnOptions" :key="col.value" :value="col.value" :label="col.label">
                {{ col.label }}
              </a-select-option>
            </a-select>
            <div v-if="!formData.targetTable" class="field-tip">
              请先选择目标表
            </div>
          </a-form-item>

          <!-- 对比表/对比列（跨表/跨字段） -->
          <template v-if="formData.applyLevel === 'CROSS_FIELD' || formData.applyLevel === 'CROSS_TABLE'">
            <a-form-item label="对比表名" name="compareTable">
              <a-input v-model:value="formData.compareTable" placeholder="请输入对比表名" />
            </a-form-item>
            <a-form-item label="对比列名" name="compareColumn">
              <a-input v-model:value="formData.compareColumn" placeholder="请输入对比列名" />
            </a-form-item>
          </template>

          <!-- 规则表达式 -->
          <a-form-item label="规则表达式" name="ruleExpr">
            <a-textarea
              v-model:value="formData.ruleExpr"
              :rows="6"
              :placeholder="exprPlaceholder"
              :status="exprHardcodeWarning ? 'warning' : undefined"
              style="font-family: 'JetBrains Mono', monospace; font-size: 13px"
            />
            <div class="field-tip">
              支持变量：${table}、${column}、${partition_column}、${column_a}、${column_b}、${source_table}、${target_table}、${thresholdMin}、${thresholdMax}；
              推荐使用占位符，具体表/列由质检方案绑定时动态替换
            </div>
            <div v-if="exprQuoteWarning" class="expr-quote-warn">
              <WarningOutlined /> {{ exprQuoteWarning }}
            </div>
            <div v-if="exprHardcodeWarning" class="expr-hardcode-warn">
              <WarningOutlined /> {{ exprHardcodeWarning }}
            </div>
          </a-form-item>

          <!-- 阈值配置 -->
          <a-form-item label="阈值配置" class="threshold-config">
            <div class="threshold-row">
              <div class="threshold-item">
                <span class="threshold-label">最小值</span>
                <a-input-number v-model:value="formData.thresholdMin" placeholder="阈值最小值" style="width: 100%" />
              </div>
              <div class="threshold-item">
                <span class="threshold-label">最大值</span>
                <a-input-number v-model:value="formData.thresholdMax" placeholder="阈值最大值" style="width: 100%" />
              </div>
            </div>
          </a-form-item>

          <a-form-item label="波动阈值(%)" name="fluctuationThreshold">
            <a-input-number
              v-model:value="formData.fluctuationThreshold"
              :min="0"
              :max="100"
              placeholder="行数波动百分比阈值，如 20 表示波动超过20%则告警"
              style="width: 100%"
              :formatter="(value: any) => `${value}%`"
              :parser="(value: any) => String(value).replace('%', '')"
            />
          </a-form-item>
        </a-form>

        <!-- 表达式预览 -->
        <div v-if="formData.ruleExpr" class="expr-preview">
          <div class="preview-title">
            <EyeOutlined /> 表达式预览
            <span class="preview-subtitle">
              （演示：表={{ formData.targetTable || '未选' }}；
              列占位用
              <template v-if="previewColumnForExpr">{{ previewColumnForExpr }}</template>
              <template v-else>{{ formData.targetColumn || '未选' }}</template>
              替换 ${column} / ${partition_column}）
            </span>
          </div>
          <pre class="preview-code">{{ previewExpr }}</pre>
          <div class="preview-actions">
            <a-button
              type="primary"
              :loading="sqlPreviewLoading"
              :disabled="!formData.targetDsId || !previewExpr.trim()"
              @click="runSqlPreview"
            >
              <template #icon><PlayCircleOutlined /></template>
              运行预览查询
            </a-button>
            <span class="preview-actions-hint">
              仅执行 SELECT；超时与行数受服务端限制。关闭本抽屉会取消当前请求（数据库端仍有语句超时保护）。
            </span>
          </div>
          <div v-if="sqlPreviewResult?.normalizeNote" class="preview-note">{{ sqlPreviewResult.normalizeNote }}</div>
          <div v-if="sqlPreviewResult?.sqlExecuted" class="preview-executed-label">实际执行 SQL</div>
          <pre v-if="sqlPreviewResult?.sqlExecuted" class="preview-code preview-code-sm">{{ sqlPreviewResult.sqlExecuted }}</pre>
          <div v-if="sqlPreviewResult?.truncated" class="preview-truncated">已截断：最多返回配置行数，请缩小查询范围。</div>
          <a-table
            v-if="sqlPreviewResult?.rows?.length"
            class="preview-result-table"
            size="small"
            :columns="sqlPreviewTableColumns"
            :data-source="sqlPreviewResult.rows"
            :pagination="false"
            :scroll="{ x: 'max-content' }"
            row-key="__previewRowId"
          />
        </div>
      </div>

      <!-- Step 3: 质量维度与强度 -->
      <div v-show="currentStep === 2" class="step-content">
        <a-form
          ref="formRef3"
          :model="formData"
          :rules="formRules3"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="质量维度" name="dimensions">
            <a-checkbox-group v-model:value="formData.dimensions">
              <a-row>
                <a-col :span="8" v-for="dim in dimensionOptions" :key="dim.value">
                  <a-checkbox :value="dim.value">{{ dim.label }}</a-checkbox>
                </a-col>
              </a-row>
            </a-checkbox-group>
            <div class="field-tip" style="margin-top: 8px">至少选择一个质量维度</div>
          </a-form-item>

          <a-divider>规则强度与告警</a-divider>

          <a-form-item label="规则强度" name="ruleStrength">
            <a-radio-group v-model:value="formData.ruleStrength">
              <a-radio value="STRONG">
                <span class="strength-option">
                  <span class="strength-tag strong">强规则</span>
                  <span class="strength-desc">失败时阻塞任务执行 + 立即告警</span>
                </span>
              </a-radio>
              <a-radio value="WEAK">
                <span class="strength-option">
                  <span class="strength-tag weak">弱规则</span>
                  <span class="strength-desc">失败时仅记录，不阻塞任务</span>
                </span>
              </a-radio>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="错误级别" name="errorLevel">
            <a-radio-group v-model:value="formData.errorLevel">
              <a-radio value="LOW">
                <a-tag color="green">低</a-tag> 低 — 影响轻微
              </a-radio>
              <a-radio value="MEDIUM">
                <a-tag color="orange">中</a-tag> 中 — 影响一般
              </a-radio>
              <a-radio value="HIGH">
                <a-tag color="red">高</a-tag> 高 — 影响严重
              </a-radio>
              <a-radio value="CRITICAL">
                <a-tag color="purple">危急</a-tag> 危急 — 核心功能受损
              </a-radio>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="告警接收人" name="alertReceivers">
            <a-input v-model:value="formData.alertReceivers" placeholder="输入接收人，多人以逗号分隔">
              <template #prefix><UserOutlined /></template>
            </a-input>
            <div class="field-tip">留空则使用系统默认告警配置</div>
          </a-form-item>

          <a-form-item label="排序权重" name="sortOrder">
            <a-input-number v-model:value="formData.sortOrder" :min="0" :max="999" style="width: 100%" />
            <div class="field-tip">数值越小排序越靠前</div>
          </a-form-item>
        </a-form>

        <!-- 规则强度说明 -->
        <div class="strength-guide">
          <div class="guide-title">
            <BulbOutlined /> 规则强度说明
          </div>
          <div class="guide-cards">
            <div class="guide-card strong-card">
              <div class="guide-card-title">
                <a-tag color="red">强规则</a-tag>
              </div>
              <ul>
                <li>失败时立即阻塞下游任务执行</li>
                <li>触发告警通知</li>
                <li>影响数据推送和数据应用</li>
                <li>适用于：主键唯一性、重要字段非空</li>
              </ul>
            </div>
            <div class="guide-card weak-card">
              <div class="guide-card-title">
                <a-tag color="orange">弱规则</a-tag>
              </div>
              <ul>
                <li>失败时仅记录日志和告警</li>
                <li>不阻塞任务执行</li>
                <li>纳入质量评分统计</li>
                <li>适用于：数据分布检查、趋势监测</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 4: 预览保存 -->
      <div v-show="currentStep === 3" class="step-content">
        <div class="preview-section">
          <div class="preview-title">
            <CheckCircleOutlined /> 规则配置总览
          </div>
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="规则名称">{{ formData.ruleName }}</a-descriptions-item>
            <a-descriptions-item label="规则编码"><code>{{ formData.ruleCode }}</code></a-descriptions-item>
            <a-descriptions-item label="规则类型">
              <span class="rule-type-tag" :class="getRuleTypeClass(formData.ruleType)">
                {{ getRuleTypeLabel(formData.ruleType) }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="关联模板">
              {{ getTemplateName(formData.templateId) || '无模板（手动配置）' }}
            </a-descriptions-item>
            <a-descriptions-item label="适用级别">
              <a-tag>{{ getApplyLevelLabel(formData.applyLevel) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="目标数据源">{{ getDsName(formData.targetDsId) }}</a-descriptions-item>
            <a-descriptions-item label="数仓层标签">
              <a-tag :color="getLayerColor(formData.layerCode)">{{ formData.layerCode || '-' }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="目标表">{{ formData.targetTable }}</a-descriptions-item>
            <a-descriptions-item label="目标列">{{ formData.targetColumn || '-' }}</a-descriptions-item>
            <a-descriptions-item label="规则表达式">
              <pre class="preview-code-sm">{{ formData.ruleExpr || '-' }}</pre>
            </a-descriptions-item>
            <a-descriptions-item label="阈值范围">
              {{ formData.thresholdMin !== undefined ? formData.thresholdMin : '-' }}
              ~
              {{ formData.thresholdMax !== undefined ? formData.thresholdMax : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="质量维度">
              <a-tag v-for="dim in formData.dimensions" :key="dim" color="cyan">{{ getDimensionLabel(dim) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="规则强度">
              <a-tag :color="formData.ruleStrength === 'STRONG' ? 'red' : 'orange'">
                {{ formData.ruleStrength === 'STRONG' ? '强规则' : '弱规则' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="错误级别">
              <a-tag :color="getErrorLevelColor(formData.errorLevel)">
                {{ getErrorLevelLabel(formData.errorLevel) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="波动阈值">
              {{ formData.fluctuationThreshold ? formData.fluctuationThreshold + '%' : '-' }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </div>

      <!-- 抽屉底部按钮 -->
      <div class="drawer-footer">
        <a-space>
          <a-button v-if="currentStep > 0" @click="currentStep--">
            <template #icon><LeftOutlined /></template>
            上一步
          </a-button>
          <a-button v-if="currentStep < 3" type="primary" @click="nextStep">
            下一步
            <template #icon><RightOutlined /></template>
          </a-button>
          <a-button
            v-if="currentStep === 3"
            type="primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            <template #icon><CheckOutlined /></template>
            {{ isEdit ? '保存修改' : '创建规则' }}
          </a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- 查看详情抽屉 -->
    <a-drawer
      v-model:open="viewDrawerVisible"
      title="规则详情"
      :width="560"
      placement="right"
    >
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="规则名称">{{ viewRecord?.ruleName }}</a-descriptions-item>
        <a-descriptions-item label="规则编码"><code>{{ viewRecord?.ruleCode }}</code></a-descriptions-item>
        <a-descriptions-item label="规则类型">
          <span class="rule-type-tag" :class="getRuleTypeClass(viewRecord?.ruleType)">
            {{ getRuleTypeLabel(viewRecord?.ruleType) }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="适用级别">
          <a-tag>{{ getApplyLevelLabel(viewRecord?.applyLevel) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="目标数据源">{{ getDsName(viewRecord?.targetDsId) }}</a-descriptions-item>
        <a-descriptions-item label="数仓层标签">
          <a-tag :color="getLayerColor(viewRecord?.layerCode)">{{ viewRecord?.layerCode || '-' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="目标表">{{ viewRecord?.targetTable || '-' }}</a-descriptions-item>
        <a-descriptions-item label="目标列">{{ viewRecord?.targetColumn || '-' }}</a-descriptions-item>
        <a-descriptions-item label="规则强度">
          <a-tag :color="viewRecord?.ruleStrength === 'STRONG' ? 'red' : 'orange'">
            {{ viewRecord?.ruleStrength === 'STRONG' ? '强规则' : '弱规则' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="错误级别">
          <a-tag :color="getErrorLevelColor(viewRecord?.errorLevel)">
            {{ getErrorLevelLabel(viewRecord?.errorLevel) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="阈值范围">
          {{ viewRecord?.thresholdMin !== undefined ? viewRecord.thresholdMin : '-' }}
          ~
          {{ viewRecord?.thresholdMax !== undefined ? viewRecord.thresholdMax : '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="波动阈值">
          {{ viewRecord?.fluctuationThreshold ? viewRecord.fluctuationThreshold + '%' : '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="规则表达式">
          <pre class="preview-code-sm">{{ viewRecord?.ruleExpr || '-' }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="启用状态">
          <a-badge :status="viewRecord?.enabled ? 'success' : 'default'" :text="viewRecord?.enabled ? '已启用' : '已禁用'" />
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ viewRecord?.createTime || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  Button,
  Input,
  Select,
  Table,
  Drawer,
  Dropdown,
  Menu,
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
  Radio,
  Checkbox,
  InputNumber,
  Avatar,
  Breadcrumb,
  Steps,
  Form,
  ConfigProvider,
  Popconfirm
} from 'ant-design-vue'
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  CheckOutlined,
  LeftOutlined,
  RightOutlined,
  InfoCircleOutlined,
  SyncOutlined,
  CopyOutlined,
  BulbOutlined,
  UserOutlined,
  DatabaseOutlined,
  WarningOutlined,
  PlayCircleOutlined
} from '@ant-design/icons-vue'
import { ruleDefApi, ruleTemplateApi, dataSourceApi } from '@/api/dqc'
import type { RuleDef } from '@/types'

const loading = ref(false)
const tableData = ref<RuleDef[]>([])
const selectedRowKeys = ref<number[]>([])
const searchName = ref('')
const filterType = ref<string>()
const filterDsId = ref<number>()
const filterStrength = ref<string>()
const filterEnabled = ref<number>()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const statCards = ref([
  { label: '规则总数', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)' },
  { label: '强规则', value: 0, bg: 'linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%)' },
  { label: '弱规则', value: 0, bg: 'linear-gradient(135deg, #FA8C16 0%, #FFC53D 100%)' }
])

const ruleTypeOptions = [
  { value: 'NULL_CHECK', label: '空值检查' },
  { value: 'UNIQUE_CHECK', label: '唯一性校验' },
  { value: 'DUPLICATE_CHECK', label: '重复值检测' },
  { value: 'THRESHOLD_MIN', label: '值域-最小值' },
  { value: 'THRESHOLD_MAX', label: '值域-最大值' },
  { value: 'THRESHOLD_RANGE', label: '值域-范围' },
  { value: 'ENUM_CHECK', label: '枚举值校验' },
  { value: 'REGEX_PHONE', label: '手机号格式' },
  { value: 'REGEX_EMAIL', label: '邮箱格式' },
  { value: 'REGEX_IDCARD', label: '身份证格式' },
  { value: 'CARDINALITY', label: '离散度分析' },
  { value: 'LENGTH_CHECK', label: '字符串长度' },
  { value: 'ROW_COUNT_NOT_ZERO', label: '行数非0校验' },
  { value: 'ROW_COUNT_FLUCTUATION', label: '行数波动监测' },
  { value: 'TABLE_UPDATE_TIMELINESS', label: '表更新时效' },
  { value: 'CROSS_FIELD_COMPARE', label: '跨字段-A>B' },
  { value: 'CROSS_TABLE_COUNT', label: '跨表-记录数' },
  { value: 'CUSTOM_SQL', label: '自定义SQL' },
  { value: 'CUSTOM_FUNC', label: '自定义函数' }
]

const applyLevelOptions = [
  { value: 'TABLE', label: '表级' },
  { value: 'COLUMN', label: '字段级' },
  { value: 'CROSS_FIELD', label: '跨字段' },
  { value: 'CROSS_TABLE', label: '跨表' }
]

const dimensionOptions = [
  { value: 'COMPLETENESS', label: '完整性' },
  { value: 'UNIQUENESS', label: '唯一性' },
  { value: 'ACCURACY', label: '准确性' },
  { value: 'CONSISTENCY', label: '一致性' },
  { value: 'TIMELINESS', label: '及时性' },
  { value: 'VALIDITY', label: '有效性' }
]

const columns = [
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 200, fixed: 'left', ellipsis: true },
  { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode', width: 160 },
  { title: '规则类型', key: 'ruleType', width: 130 },
  { title: '适用级别', dataIndex: 'applyLevel', key: 'applyLevel', width: 100 },
  { title: '数仓层', key: 'layerCode', width: 90 },
  { title: '规则强度', key: 'ruleStrength', width: 90 },
  { title: '错误级别', key: 'errorLevel', width: 90 },
  { title: '启用状态', key: 'enabled', width: 80 },
  { title: '提示内容', key: 'hintContent', width: 380, fixed: 'right' }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => { selectedRowKeys.value = keys }
}))
const hasSelected = computed(() => selectedRowKeys.value.length > 0)

const drawerVisible = ref(false)
const drawerTitle = ref('新建规则')
const currentStep = ref(0)
const submitLoading = ref(false)
const isEdit = ref(false)
const editingId = ref<number>()
const viewDrawerVisible = ref(false)
const viewRecord = ref<RuleDef | null>(null)
const templateOptions = ref<any[]>([])
const dataSourceOptions = ref<any[]>([])
const selectedTemplate = ref<any>(null)

const formData = reactive<any>({
  ruleName: '',
  ruleCode: '',
  ruleType: undefined,
  applyLevel: undefined,
  layerCode: undefined,
  targetDsId: undefined,
  targetTable: undefined,
  targetColumn: '',
  compareTable: '',
  compareColumn: '',
  ruleExpr: '',
  thresholdMin: undefined,
  thresholdMax: undefined,
  fluctuationThreshold: undefined,
  dimensions: [],
  ruleStrength: 'WEAK',
  errorLevel: 'MEDIUM',
  alertReceivers: '',
  sortOrder: 0
})

// 表名选择器相关
const tableOptions = ref<any[]>([])
const tableLoading = ref(false)
const tableSearchValue = ref('')
const columnOptions = ref<any[]>([])
const columnLoading = ref(false)
const pollingTimer = ref<number | null>(null)

/** 预览专用列：不填则与「目标列名」一致，用于看 ${column}/${partition_column} 替换效果 */
const previewColumnForExpr = ref<string | undefined>(undefined)
const sqlPreviewLoading = ref(false)
const sqlPreviewResult = ref<{
  sqlExecuted: string
  rows: Record<string, unknown>[]
  rowCount: number
  truncated: boolean
  normalizeNote?: string
} | null>(null)
const sqlPreviewAbort = ref<AbortController | null>(null)

/** 当前数据源的数据库名（MySQL 显示） */
const dsDbName = computed(() => {
  const ds = dataSourceOptions.value.find((d: any) => d.id === formData.targetDsId)
  return ds?.databaseName || ''
})

/** 当前数据源的 schema 名（PostgreSQL / Oracle / SQLServer 显示） */
const dsSchemaName = computed(() => {
  const ds = dataSourceOptions.value.find((d: any) => d.id === formData.targetDsId)
  return ds?.schemaName || ''
})

/** 表达式是否含需列替换的占位符（表级规则常用 ${partition_column}） */
const isTableLevelWithColumnPlaceholders = computed(() => {
  const lvl = formData.applyLevel
  if (lvl !== 'TABLE' && lvl !== 'CROSS_TABLE') return false
  const e = formData.ruleExpr || ''
  return /\$\{column\}|\$\{partition_column\}|\$\{column_a\}/i.test(e)
})

const previewColumnPlaceholder = computed(() => {
  if (formData.applyLevel === 'COLUMN' || formData.applyLevel === 'CROSS_FIELD') {
    return '默认沿用「目标列名」；可另选一列仅用于本页预览与运行查询'
  }
  return '表达式含 ${column}、${partition_column} 等时请选列，便于预览与运行查询'
})

const formRef = ref()
const formRef2 = ref()
const formRef3 = ref()

const formRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  applyLevel: [{ required: true, message: '请选择适用级别', trigger: 'change' }],
  targetDsId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }]
}

const formRules3 = {
  dimensions: [{ required: true, message: '请至少选择一个质量维度', trigger: 'change' }]
}

const exprPlaceholder = computed(() => {
  if (formData.applyLevel === 'COLUMN') {
    return 'SELECT COUNT(*) FROM ${table} WHERE ${column} IS NULL'
  }
  if (formData.applyLevel === 'CROSS_FIELD') {
    return 'SELECT COUNT(*) FROM ${table} WHERE ${column_a} > ${column_b}'
  }
  if (formData.applyLevel === 'CROSS_TABLE') {
    return 'SELECT COUNT(*) FROM ${source_table} - SELECT COUNT(*) FROM ${target_table}'
  }
  return 'SELECT COUNT(*) FROM ${table}'
})

/**
 * 表达式预览：对占位符做演示替换，展示若绑定到当前所选表/列时的实际 SQL。
 * 若表达式中含硬编码表名，替换结果与预期不符，此时 previewExpr 会自动暴露该问题。
 */
const previewExpr = computed(() => {
  if (!formData.ruleExpr) return ''
  const colForPreview = (previewColumnForExpr.value || formData.targetColumn || '').trim()
  let result = formData.ruleExpr
  if (formData.targetTable) {
    result = result.replace(/\${table}/g, formData.targetTable)
    result = result.replace(/\${source_table}/g, formData.targetTable)
  }
  if (colForPreview) {
    result = result.replace(/\${column}/g, colForPreview)
    result = result.replace(/\${partition_column}/g, colForPreview)
    result = result.replace(/\${column_a}/g, colForPreview)
  }
  if (formData.compareTable) {
    result = result.replace(/\${target_table}/g, formData.compareTable)
  }
  if (formData.compareColumn) {
    result = result.replace(/\${column_b}/g, formData.compareColumn)
  }
  if (formData.targetDsId != null) {
    result = result.replace(/\${dsId}/g, String(formData.targetDsId))
  }
  if (formData.thresholdMin != null) {
    result = result.replace(/\${thresholdMin}/g, String(formData.thresholdMin))
  }
  if (formData.thresholdMax != null) {
    result = result.replace(/\${thresholdMax}/g, String(formData.thresholdMax))
  }
  return result
})

const exprQuoteWarning = computed(() => {
  const r = (formData.ruleExpr || '').trim()
  if (r.length >= 2 && r.startsWith('\'') && r.endsWith('\'')) {
    return '表达式外层有单引号时，整段会被当作字符串常量而非 SQL。保存前请去掉首尾引号；「运行预览查询」会自动去掉首尾一层单引号后再执行。'
  }
  return ''
})

const sqlPreviewTableColumns = computed(() => {
  const rows = sqlPreviewResult.value?.rows
  if (!rows?.length) return []
  return Object.keys(rows[0])
    .filter(k => k !== '__previewRowId')
    .map(k => ({
      title: k,
      dataIndex: k,
      key: k,
      ellipsis: true
    }))
})

function abortSqlPreview() {
  sqlPreviewAbort.value?.abort()
  sqlPreviewAbort.value = null
  sqlPreviewLoading.value = false
}

function onDrawerAfterOpenChange(open: boolean) {
  if (!open) {
    abortSqlPreview()
  }
}

async function runSqlPreview() {
  if (!formData.targetDsId) {
    message.warning('请先选择目标数据源')
    return
  }
  const sql = previewExpr.value.trim()
  if (!sql) {
    message.warning('预览 SQL 为空，请检查表达式与表/列')
    return
  }
  abortSqlPreview()
  const ac = new AbortController()
  sqlPreviewAbort.value = ac
  sqlPreviewLoading.value = true
  sqlPreviewResult.value = null
  try {
    const res: any = await dataSourceApi.previewSelect(formData.targetDsId, sql, { signal: ac.signal })
    const d = res?.data
    if (d?.rows?.length) {
      d.rows = d.rows.map((r: Record<string, unknown>, i: number) => ({ ...r, __previewRowId: i }))
    }
    sqlPreviewResult.value = d ?? null
  } catch (e: any) {
    const canceled =
      e?.code === 'ERR_CANCELED' ||
      e?.name === 'CanceledError' ||
      (typeof e?.message === 'string' && e.message.toLowerCase().includes('cancel'))
    if (canceled) return
  } finally {
    sqlPreviewLoading.value = false
    sqlPreviewAbort.value = null
  }
}

/**
 * 检测规则表达式中是否存在硬编码的表名/列名（而非使用占位符），
 * 这会导致质检方案绑定其他表时 SQL 仍查旧表，无法复用。
 * 注意：跨表/跨字段类型（规则需要明确指定表名）不触发警告。
 */
const exprHardcodeWarning = computed(() => {
  if (!formData.ruleExpr) return ''
  const expr = formData.ruleExpr
  // 跨表规则明确需要真实表名，不警告
  if (formData.applyLevel === 'CROSS_TABLE') return ''
  // 若表达式含 FROM/JOIN，尝试提取其后的标识符，与 targetTable 对比
  // 仅提示：表名部分匹配但未使用 ${table}
  const tableMatched = formData.targetTable
    && /FROM\s+([`\"'\[]?([\w\u4e00-\u9fa5$]+)[`\"'\]]?\s+)/i.exec(expr)?.[2]?.toLowerCase() === formData.targetTable?.toLowerCase()
    && !expr.includes('${table}') && !expr.includes('${source_table}')
  if (tableMatched) {
    return `表达式中的表名「${formData.targetTable}」已写死，质检方案绑定其他表时将无法复用。建议改用 ${table} 占位符。`
  }
  return ''
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await ruleDefApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ruleName: searchName.value || undefined,
      ruleType: filterType.value || undefined,
      targetDsId: filterDsId.value,
      ruleStrength: filterStrength.value || undefined,
      enabled: filterEnabled.value
    })
    const pageData = res.data
    const records = pageData?.records
    tableData.value = Array.isArray(records) ? records : Array.isArray(pageData) ? pageData : []
    pagination.total = pageData?.total ?? tableData.value.length
    statCards.value[0].value = pagination.total

    // 并行加载强弱规则统计
    await Promise.all([loadStrengthStats()])
  } catch {
    tableData.value = []
    pagination.total = 0
    statCards.value[0].value = 0
  } finally {
    loading.value = false
  }
}

// 加载强弱规则统计
async function loadStrengthStats() {
  try {
    // 并行查询强规则和弱规则总数
    const [strongRes, weakRes] = await Promise.allSettled([
      ruleDefApi.page({ pageNum: 1, pageSize: 1, ruleStrength: 'STRONG' }),
      ruleDefApi.page({ pageNum: 1, pageSize: 1, ruleStrength: 'WEAK' })
    ])

    if (strongRes.status === 'fulfilled' && strongRes.value?.success !== false) {
      const strongTotal = strongRes.value.data?.total ?? 0
      statCards.value[1].value = strongTotal
    }

    if (weakRes.status === 'fulfilled' && weakRes.value?.success !== false) {
      const weakTotal = weakRes.value.data?.total ?? 0
      statCards.value[2].value = weakTotal
    }
  } catch {
    // ignore
  }
}

async function loadTemplates() {
  try {
    const res: any = await ruleTemplateApi.listEnabled()
    const list = res.data
    templateOptions.value = Array.isArray(list) ? list : []
  } catch {
    templateOptions.value = []
  }
}

async function loadDataSources() {
  try {
    const res: any = await dataSourceApi.listEnabled()
    const list = res.data
    dataSourceOptions.value = Array.isArray(list) ? list : []
  } catch {
    dataSourceOptions.value = []
  }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function resetFilters() {
  searchName.value = ''
  filterType.value = undefined
  filterDsId.value = undefined
  filterStrength.value = undefined
  filterEnabled.value = undefined
  pagination.current = 1
  loadData()
}

function openCreateDrawer() {
  isEdit.value = false
  editingId.value = undefined
  drawerTitle.value = '新建规则'
  currentStep.value = 0
  resetForm()
  drawerVisible.value = true
}

async function handleEdit(record: RuleDef) {
  isEdit.value = true
  editingId.value = record.id
  drawerTitle.value = '编辑规则'
  currentStep.value = 0
  Object.assign(formData, {
    ruleName: record.ruleName,
    ruleCode: record.ruleCode,
    ruleType: record.ruleType,
    templateId: record.templateId,
    applyLevel: record.applyLevel,
    layerCode: record.layerCode,
    targetDsId: record.targetDsId,
    targetTable: record.targetTable,
    targetColumn: record.targetColumn,
    compareTable: record.compareTable,
    compareColumn: record.compareColumn,
    ruleExpr: record.ruleExpr,
    thresholdMin: record.thresholdMin,
    thresholdMax: record.thresholdMax,
    fluctuationThreshold: record.fluctuationThreshold,
    // dimensions: 后端存为 JSON 字符串，编辑时需解析为数组
    dimensions: (() => {
      if (!record.dimensions) return []
      if (Array.isArray(record.dimensions)) return record.dimensions
      try { return JSON.parse(record.dimensions as string) } catch { return [] }
    })(),
    ruleStrength: record.ruleStrength || 'WEAK',
    errorLevel: record.errorLevel || 'MEDIUM',
    alertReceivers: record.alertReceivers || '',
    sortOrder: record.sortOrder || 0
  })
  if (formData.targetDsId) {
    const ds = dataSourceOptions.value.find((d: any) => d.id === formData.targetDsId)
    if (ds?.dataLayer) {
      formData.layerCode = ds.dataLayer
    }
    await loadTables()
    if (formData.targetTable) {
      await loadColumns()
    }
  }
  drawerVisible.value = true
}

function handleView(record: RuleDef) {
  viewRecord.value = record
  viewDrawerVisible.value = true
}

async function handleCopy(record: RuleDef) {
  try {
    const res: any = await ruleDefApi.copy(record.id!)
    message.success('复制成功，已创建新规则')
    loadData()
  } catch {
    message.error('复制失败，请重试')
  }
}

async function handleDelete(record: RuleDef) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除规则「${record.ruleName}」吗？删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        await ruleDefApi.delete(record.id!)
        message.success('删除成功')
        loadData()
      } catch {
        message.error('删除失败，请重试')
      }
    }
  })
}

async function handleBatchDelete() {
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个规则吗？`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        await Promise.all(selectedRowKeys.value.map(id => ruleDefApi.delete(id)))
        message.success('批量删除成功')
        selectedRowKeys.value = []
        loadData()
      } catch {
        message.error('批量删除失败')
      }
    }
  })
}

async function handleToggleEnabled(record: RuleDef, checked: boolean) {
  try {
    await ruleDefApi.toggle(record.id!)
    record.enabled = checked
    message.success(checked ? '已启用' : '已禁用')
  } catch {
    message.error('操作失败')
  }
}

function generateCode() {
  formData.ruleCode = 'RULE_' + Date.now().toString(36).toUpperCase()
}

function onRuleTypeChange(ruleType: string) {
  // 规则类型变更时，清空可能不兼容的字段
  if (ruleType) {
    // 某些规则类型可能需要重置适用级别
    formData.applyLevel = undefined
    formData.ruleExpr = ''
    formData.dimensions = []
  }
}

function onTemplateChange(templateId: number) {
  if (!templateId) {
    selectedTemplate.value = null
    return
  }
  const t = templateOptions.value.find(t => t.id === templateId)
  selectedTemplate.value = t || null
  if (t) {
    formData.applyLevel = t.applyLevel
    formData.ruleExpr = t.defaultExpr || ''
    if (t.dimension) {
      formData.dimensions = [t.dimension]
    }
    // 如果模板有规则类型，也自动设置
    if (t.ruleType) {
      formData.ruleType = t.ruleType
    }
  }
}

function onTargetDsChange() {
  const ds = formData.targetDsId
    ? dataSourceOptions.value.find((d: any) => d.id === formData.targetDsId)
    : undefined
  formData.layerCode = ds?.dataLayer || undefined
  formData.targetTable = undefined
  formData.targetColumn = ''
  previewColumnForExpr.value = undefined
  tableOptions.value = []
  columnOptions.value = []
  tableSearchValue.value = ''
  if (formData.targetDsId) {
    loadTables()
  }
}

function onTableChange() {
  formData.targetColumn = ''
  previewColumnForExpr.value = undefined
  columnOptions.value = []
  if (formData.targetTable) {
    void loadColumns()
  }
}

async function loadTables() {
  if (!formData.targetDsId) return
  tableLoading.value = true
  try {
    const res: any = await dataSourceApi.getTables(formData.targetDsId)
    tableOptions.value = (res.data || []).map((t: string) => ({ label: t, value: t }))
  } catch {
    tableOptions.value = []
  } finally {
    tableLoading.value = false
  }
}

async function loadColumns() {
  if (!formData.targetDsId || !formData.targetTable) {
    columnOptions.value = []
    return
  }
  columnLoading.value = true
  try {
    const res: any = await dataSourceApi.getTableColumns(formData.targetDsId, formData.targetTable)
    columnOptions.value = (res.data || []).map((col: any) => {
      const label = col.columnName + (col.columnComment ? ` (${col.columnComment})` : '')
      return { label, value: col.columnName }
    })
  } catch {
    columnOptions.value = []
  } finally {
    columnLoading.value = false
  }
}

watch(
  () => formData.targetTable,
  () => { /* handled by onTableChange */ }
)

function nextStep() {
  if (currentStep.value === 0) {
    formRef.value?.validate().then(() => { currentStep.value++ })
  } else if (currentStep.value === 1) {
    formRef2.value?.validate().then(() => { currentStep.value++ })
  } else if (currentStep.value === 2) {
    formRef3.value?.validate().then(() => { currentStep.value++ })
  } else {
    currentStep.value++
  }
}

function resetForm() {
  Object.assign(formData, {
    ruleName: '',
    ruleCode: '',
    ruleType: undefined,
    templateId: undefined,
    applyLevel: undefined,
    layerCode: undefined,
    targetDsId: undefined,
    targetTable: undefined,
    targetColumn: '',
    compareTable: '',
    compareColumn: '',
    ruleExpr: '',
    thresholdMin: undefined,
    thresholdMax: undefined,
    fluctuationThreshold: undefined,
    dimensions: [],
    ruleStrength: 'WEAK',
    errorLevel: 'MEDIUM',
    alertReceivers: '',
    sortOrder: 0
  })
  tableOptions.value = []
  columnOptions.value = []
  columnLoading.value = false
  tableLoading.value = false
  tableSearchValue.value = ''
  previewColumnForExpr.value = undefined
  sqlPreviewResult.value = null
  abortSqlPreview()
}

onUnmounted(() => {
  abortSqlPreview()
})

async function handleSubmit() {
  submitLoading.value = true
  try {
    const dto: any = { ...formData }
    // dimensions: 前端为 checkbox-group 数组，后端 String 类型（存 JSON 字符串）
    dto.dimensions = JSON.stringify(dto.dimensions || [])
    if (isEdit.value) {
      await ruleDefApi.update(editingId.value!, dto)
      message.success('修改成功')
    } else {
      await ruleDefApi.create(dto)
      message.success('创建成功')
    }
    drawerVisible.value = false
    loadData()
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

function getRuleTypeLabel(type: string) {
  return ruleTypeOptions.find(t => t.value === type)?.label || type
}

function getRuleTypeClass(type: string) {
  const map: Record<string, string> = {
    NULL_CHECK: 'type-null',
    UNIQUE_CHECK: 'type-unique',
    REGEX_PHONE: 'type-regex',
    REGEX_EMAIL: 'type-regex',
    REGEX_IDCARD: 'type-regex',
    THRESHOLD_MIN: 'type-threshold',
    THRESHOLD_MAX: 'type-threshold',
    THRESHOLD_RANGE: 'type-threshold',
    CROSS_FIELD_COMPARE: 'type-cross',
    CROSS_FIELD_SUM: 'type-cross',
    CROSS_TABLE_COUNT: 'type-cross',
    CUSTOM_SQL: 'type-custom',
    CUSTOM_FUNC: 'type-custom',
    CARDINALITY: 'type-analysis',
    ENUM_CHECK: 'type-check',
    LENGTH_CHECK: 'type-check',
    ROW_COUNT_NOT_ZERO: 'type-table',
    ROW_COUNT_FLUCTUATION: 'type-table',
    DUPLICATE_CHECK: 'type-unique'
  }
  return map[type] || 'type-default'
}

function getApplyLevelLabel(level: string) {
  return applyLevelOptions.find(l => l.value === level)?.label || level
}

function getLayerColor(layer: string) {
  const map: Record<string, string> = {
    ODS: 'blue',
    DWD: 'green',
    DWS: 'orange',
    ADS: 'purple'
  }
  return map[layer] || 'default'
}

function getDimensionLabel(dim: string) {
  return dimensionOptions.find(d => d.value === dim)?.label || dim
}

function getErrorLevelColor(level: string) {
  const map: Record<string, string> = {
    LOW: 'green',
    MEDIUM: 'orange',
    HIGH: 'red',
    CRITICAL: 'purple'
  }
  return map[level] || 'default'
}

function getErrorLevelLabel(level: string) {
  const map: Record<string, string> = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '危急'
  }
  return map[level] || level
}

function getTemplateName(templateId?: number) {
  if (!templateId) return ''
  const t = templateOptions.value.find(t => t.id === templateId)
  return t?.templateName || ''
}

function getDsName(dsId?: number) {
  if (!dsId) return '-'
  const ds = dataSourceOptions.value.find(ds => ds.id === dsId)
  return ds?.dsName || '-'
}

onMounted(() => {
  loadData()
  loadTemplates()
  loadDataSources()
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
    .page-title {
      font-size: 22px;
      font-weight: 700;
      color: #1F1F1F;
      margin: 0;
    }
    .page-subtitle {
      font-size: 13px;
      color: #8C8C8C;
      margin: 4px 0 0;
    }
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

.hint-content-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.hint-content-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  font-family: 'JetBrains Mono', ui-monospace, monospace;
}
.hint-content-actions {
  flex-shrink: 0;
}
.hint-content-actions :deep(.ant-btn) {
  padding-inline: 4px;
}

.rule-type-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  &.type-null      { background: #E6F7FF; color: #1677FF; }
  &.type-unique    { background: #F6FFED; color: #52C41A; }
  &.type-regex     { background: #FFF7E6; color: #FA8C16; }
  &.type-threshold { background: #FFF1F0; color: #FF4D4F; }
  &.type-cross     { background: #F9F0FF; color: #722ED1; }
  &.type-custom    { background: #E6F7FF; color: #13C2C2; }
  &.type-analysis  { background: #F9F0FF; color: #9254DE; }
  &.type-check     { background: #FFF7E6; color: #FA8C16; }
  &.type-table     { background: #E6F7FF; color: #1890FF; }
}

.ds-type-badge {
  display: inline-block;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 10px;
  background: #E6F7FF;
  color: #1677FF;
  margin-right: 4px;
}

.drawer-steps { margin-bottom: 32px; padding: 0 20px; }
.step-content { min-height: 400px; }

.template-hint {
  background: #F6FFED;
  border: 1px solid #B7EB8F;
  border-radius: 8px;
  padding: 16px;
  margin-top: 16px;
  .hint-title { font-size: 14px; font-weight: 600; color: #52C41A; margin-bottom: 8px; }
  p { font-size: 13px; color: #333; margin: 4px 0; display: flex; align-items: center; gap: 6px; }
  .hint-expr {
    margin-top: 12px;
    .hint-expr-title { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 6px; }
    .hint-expr-code {
      background: #1F1F1F;
      color: #A6E22E;
      padding: 12px;
      border-radius: 6px;
      font-family: 'JetBrains Mono', monospace;
      font-size: 12px;
      margin: 0;
      white-space: pre-wrap;
    }
  }
}

.field-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; }

.threshold-config {
  .threshold-row {
    display: flex;
    gap: 16px;
    .threshold-item { flex: 1; }
    .threshold-label { display: block; font-size: 13px; color: #333; margin-bottom: 6px; }
  }
}

.expr-preview {
  margin-top: 16px;
  .preview-title { font-size: 13px; font-weight: 600; color: #52C41A; margin-bottom: 8px; display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
  .preview-subtitle { font-size: 11px; font-weight: 400; color: #8C8C8C; margin-left: 8px; }
  .preview-code {
    background: #1F1F1F;
    color: #A6E22E;
    padding: 16px;
    border-radius: 8px;
    font-family: 'JetBrains Mono', monospace;
    font-size: 13px;
    line-height: 1.6;
    overflow-x: auto;
    margin: 0;
    white-space: pre-wrap;
  }
  .preview-code-sm { margin-top: 8px; font-size: 12px; padding: 10px 12px; }
}

.preview-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  .preview-actions-hint {
    font-size: 12px;
    color: #8c8c8c;
    flex: 1;
    min-width: 220px;
    line-height: 1.5;
  }
}

.preview-note {
  margin-top: 10px;
  font-size: 12px;
  color: #1677ff;
  line-height: 1.5;
}

.preview-executed-label {
  margin-top: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #666;
}

.preview-truncated {
  margin-top: 8px;
  font-size: 12px;
  color: #fa8c16;
}

.preview-result-table {
  margin-top: 12px;
}

.expr-quote-warn {
  margin-top: 6px;
  font-size: 12px;
  color: #ff4d4f;
  display: flex;
  align-items: flex-start;
  gap: 4px;
}

.expr-hardcode-warn {
  margin-top: 6px;
  font-size: 12px;
  color: #FA8C16;
  display: flex;
  align-items: center;
  gap: 4px;
}

.strength-option {
  display: flex;
  align-items: center;
  gap: 8px;
  .strength-tag {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
    &.strong { background: #FFF1F0; color: #FF4D4F; }
    &.weak { background: #FFF7E6; color: #FA8C16; }
  }
  .strength-desc { font-size: 12px; color: #8C8C8C; }
}

.strength-guide {
  background: #F5F7FA;
  border-radius: 8px;
  padding: 16px;
  margin-top: 24px;
  .guide-title { font-size: 14px; font-weight: 600; color: #1677FF; margin-bottom: 12px; display: flex; align-items: center; gap: 6px; }
  .guide-cards { display: flex; gap: 16px; }
  .guide-card {
    flex: 1;
    padding: 12px;
    border-radius: 8px;
    .guide-card-title { margin-bottom: 8px; }
    ul { margin: 0; padding-left: 18px; font-size: 12px; color: #555; li { margin: 4px 0; } }
  }
  .strong-card { background: #FFF1F0; border: 1px solid #FFCCC7; }
  .weak-card { background: #FFF7E6; border: 1px solid #FFE58F; }
}

.preview-section {
  .preview-title { font-size: 15px; font-weight: 600; color: #1F1F1F; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
}

.preview-code-sm {
  background: #F5F7FA;
  padding: 8px 12px;
  border-radius: 6px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 24px;
  border-top: 1px solid #E8E8E8;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
