<template>
  <div class="page-container">

    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#profileGrad)"/>
            <path d="M4 18L8 10L12 14L16 8L20 12" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="8" cy="10" r="1.5" fill="white"/>
            <circle cx="12" cy="14" r="1.5" fill="white"/>
            <circle cx="16" cy="8" r="1.5" fill="white"/>
            <defs>
              <linearGradient id="profileGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#9254DE"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">探查任务</h1>
          <p class="page-subtitle">自动或手动探查数据源表的结构和内容统计，支持 MySQL / SQL Server / Oracle / PostgreSQL / TiDB</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="openSnapshotDrawer">
          <template #icon><CameraOutlined /></template>
          快照管理
        </a-button>
        <a-button type="primary" @click="openCreateWizard">
          <template #icon><PlusOutlined /></template>
          新建任务
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="8" :md="6" v-for="stat in statCards" :key="stat.label">
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
          v-model:value="filterTaskName"
          placeholder="搜索任务名称"
          style="width: 200px"
          allowClear
          @pressEnter="loadTasks"
          @clear="loadTasks"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="filterTriggerType"
          placeholder="触发方式"
          style="width: 130px"
          allowClear
          @change="loadTasks"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="MANUAL">手动触发</a-select-option>
          <a-select-option value="SCHEDULE">定时触发</a-select-option>
          <a-select-option value="EVENT">事件触发</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterProfileLevel"
          placeholder="探查级别"
          style="width: 140px"
          allowClear
          @change="loadTasks"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="BASIC">仅表级</a-select-option>
          <a-select-option value="DETAILED">表+列级</a-select-option>
          <a-select-option value="FULL">完整探查</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterDsId"
          placeholder="选择数据源"
          style="width: 180px"
          allowClear
          :loading="dsLoading"
          @change="loadTasks"
        >
          <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
            {{ ds.dsName }} ({{ ds.dsType }})
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filterStatus"
          placeholder="状态"
          style="width: 110px"
          allowClear
          @change="loadTasks"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="PUBLISHED">已启用</a-select-option>
          <a-select-option value="DISABLED">已禁用</a-select-option>
        </a-select>
        <a-button @click="resetFilters">
          <template #icon><ReloadOutlined /></template>
          重置
        </a-button>
      </a-space>
    </div>

    <!-- 空状态引导 -->
    <div v-if="!loading && taskData.length === 0 && !filterTaskName && !filterTriggerType && !filterProfileLevel && !filterDsId && !filterStatus" class="empty-state">
      <div class="empty-illustration">
        <svg width="80" height="80" viewBox="0 0 80 80" fill="none">
          <rect width="80" height="80" rx="20" fill="url(#emptyGrad2)"/>
          <path d="M20 56L32 36L44 46L56 26L68 38" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" opacity="0.7"/>
          <circle cx="32" cy="36" r="3" fill="white" opacity="0.5"/>
          <circle cx="44" cy="46" r="3" fill="white" opacity="0.5"/>
          <circle cx="56" cy="26" r="3" fill="white" opacity="0.5"/>
          <defs>
            <linearGradient id="emptyGrad2" x1="0" y1="0" x2="80" y2="80">
              <stop offset="0%" stop-color="#722ED1"/>
              <stop offset="100%" stop-color="#9254DE"/>
            </linearGradient>
          </defs>
        </svg>
      </div>
      <div class="empty-title">还没有配置探查任务</div>
      <div class="empty-desc">
        开始配置探查任务，为您的数据质量保驾护航。<br/>
        推荐先从"仅表级"探查开始，熟悉后再使用"表+列级"完整探查。
      </div>
      <div class="empty-actions">
        <a-button type="primary" size="large" @click="openCreateWizard">
          <template #icon><PlusOutlined /></template>
          立即创建任务
        </a-button>
        <a-button size="large" @click="showGuideModal = true">
          <template #icon><QuestionCircleOutlined /></template>
          查看配置指南
        </a-button>
      </div>
    </div>

    <!-- 任务表格 -->
    <div v-else class="table-card">
      <a-table
        :columns="taskColumns"
        :data-source="taskData"
        :loading="loading"
        :pagination="pagination"
        :row-key="(record: any) => record.id"
        @change="handleTableChange"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <!-- 任务名称 -->
          <template v-if="column.key === 'taskName'">
            <div class="task-name-cell">
              <div class="task-name">{{ record.taskName }}</div>
              <div class="task-code">{{ record.taskCode }}</div>
            </div>
          </template>

          <!-- 目标表 -->
          <template v-if="column.key === 'targetTable'">
            <div class="table-name-cell">
              <FileOutlined class="table-icon" />
              <span>{{ record.targetTable }}</span>
            </div>
          </template>

          <!-- 探查级别 -->
          <template v-if="column.key === 'profileLevel'">
            <a-tag :color="getProfileLevelColor(record.profileLevel)">
              {{ getProfileLevelLabel(record.profileLevel) }}
            </a-tag>
          </template>

          <!-- 触发方式 -->
          <template v-if="column.key === 'triggerType'">
            <a-tag :color="getTriggerColor(record.triggerType)">
              {{ getTriggerLabel(record.triggerType) }}
            </a-tag>
          </template>

          <!-- 状态 -->
          <template v-if="column.key === 'status'">
            <a-badge
              :status="isTaskEnabled(record.status) ? 'success' : 'default'"
              :text="isTaskEnabled(record.status) ? '已启用' : '已禁用'"
            />
          </template>

          <!-- 上次执行 -->
          <template v-if="column.key === 'lastExecutionTime'">
            <span v-if="record.lastExecutionTime" class="time-text">
              {{ formatTime(record.lastExecutionTime) }}
            </span>
            <span v-else class="time-empty">从未执行</span>
          </template>

          <!-- 操作 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-tooltip title="执行探查">
                <a-button type="text" size="small" @click="handleExecute(record)">
                  <template #icon><PlayCircleOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="查看报告">
                <a-button type="text" size="small" @click="openReportDrawer(record)">
                  <template #icon><FileSearchOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="编辑">
                <a-button type="text" size="small" @click="openEditWizard(record)">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="record.status === 1 ? '禁用' : '启用'">
                <a-button type="text" size="small" @click="handleToggle(record)">
                  <template #icon><StopOutlined v-if="isTaskEnabled(record.status)" /><PlaySquareOutlined v-else /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="删除">
                <a-popconfirm
                  title="确定删除该探查任务？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="handleDelete(record)"
                >
                  <a-button type="text" size="small" danger>
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- ============================================================ -->
    <!-- 新建/编辑任务向导（4步流程） -->
    <!-- ============================================================ -->
    <a-modal
      v-model:open="wizardVisible"
      :title="null"
      :footer="null"
      :maskClosable="false"
      width="720"
      :destroyOnClose="true"
    >
      <div class="wizard-container">

        <!-- 步骤指示器 -->
        <div class="wizard-steps">
          <div
            v-for="(step, idx) in wizardSteps"
            :key="idx"
            class="wizard-step"
            :class="{
              'step-active': currentStep === idx,
              'step-done': currentStep > idx
            }"
          >
            <div class="step-indicator">
              <CheckOutlined v-if="currentStep > idx" />
              <span v-else>{{ idx + 1 }}</span>
            </div>
            <div class="step-info">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-desc">{{ step.desc }}</div>
            </div>
            <div v-if="idx < wizardSteps.length - 1" class="step-line" />
          </div>
        </div>

        <!-- 步骤内容区 -->
        <div class="wizard-content">

          <!-- ====== Step 2: 基本信息（在选表之后填写，名称可按表名生成） ====== -->
          <div v-if="currentStep === 1" class="step-panel">
            <div class="step-panel-header">
              <div class="panel-title">填写基本信息</div>
              <div class="panel-desc">设置探查任务的基本标识信息</div>
            </div>
            <a-form
              ref="step1FormRef"
              :model="wizardForm"
              :label-col="{ span: 5 }"
              :wrapper-col="{ span: 18 }"
            >
              <a-form-item
                label="任务名称"
                name="taskName"
                :rules="[{ required: true, message: '请输入任务名称（必填）' }, { max: 100, message: '最多100个字符' }]"
              >
                <a-input
                  v-model:value="wizardForm.taskName"
                  placeholder="上一步已选表时可自动生成；也可改为业务化名称，如：ODS层订单表每日探查"
                  :maxlength="100"
                  show-count
                  @blur="taskNameManuallyEdited = true"
                  @focus="taskNameManuallyEdited = true"
                >
                  <template #suffix>
                    <a-tooltip title="建议使用有意义的名称，如「ODS层订单表每日探查」">
                      <QuestionCircleOutlined style="color: #8C8C8C" />
                    </a-tooltip>
                  </template>
                </a-input>
              </a-form-item>

              <a-form-item
                label="任务编码"
                name="taskCode"
              >
                <a-input
                  v-model:value="wizardForm.taskCode"
                  placeholder="自动生成，可自定义唯一编码"
                  :maxlength="50"
                  :disabled="isEditMode"
                >
                  <template #suffix>
                    <a-tooltip title="唯一标识，支持字母、数字、下划线。不填则自动生成">
                      <QuestionCircleOutlined style="color: #8C8C8C" />
                    </a-tooltip>
                  </template>
                </a-input>
                <div class="form-tip" v-if="!wizardForm.taskCode">
                  <CheckCircleOutlined style="color: #52C41A; margin-right: 4px" />
                  编码将自动生成：{{ autoGeneratedCode }}
                </div>
                <div class="form-tip form-tip-warning" v-else-if="isCodeValid === false">
                  <ExclamationCircleOutlined style="color: #FAAD14; margin-right: 4px" />
                  该编码已被使用
                </div>
              </a-form-item>

              <a-form-item label="任务描述" name="taskDesc">
                <a-textarea
                  v-model:value="wizardForm.taskDesc"
                  placeholder="选填，简要描述该任务的用途和探查范围"
                  :rows="3"
                  :maxlength="500"
                  show-count
                />
              </a-form-item>
            </a-form>

            <!-- 帮助提示卡片 -->
            <a-alert
              message="💡 任务命名建议"
              type="info"
              show-icon
              style="margin-top: 8px"
            >
              <template #description>
                <ul class="tip-list">
                  <li>建议命名格式：<code>数据层_表名_探查频率</code>，如 <code>ODS_dim_orders_daily</code></li>
                  <li>同一数据源下，任务名称应具有唯一性</li>
                  <li>描述信息有助于后续维护和理解任务用途</li>
                </ul>
              </template>
            </a-alert>
          </div>

          <!-- ====== Step 1: 探查配置 ====== -->
          <div v-if="currentStep === 0" class="step-panel">
            <div class="step-panel-header">
              <div class="panel-title">配置探查范围</div>
              <div class="panel-desc">选择目标数据源、表以及探查级别</div>
            </div>
            <a-form
              ref="step2FormRef"
              :model="wizardForm"
              :label-col="{ span: 5 }"
              :wrapper-col="{ span: 18 }"
            >
              <a-form-item
                label="目标数据源"
                name="targetDsId"
                :rules="[{ required: true, message: '请选择数据源' }]"
              >
                <a-select
                  v-model:value="wizardForm.targetDsId"
                  placeholder="请选择数据源"
                  :loading="dsLoading"
                  @change="onWizardDsChange"
                >
                  <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                    <div class="ds-option">
                      <span class="ds-name">{{ ds.dsName }}</span>
                      <a-tag size="small" :color="getDsTypeColor(ds.dsType)">{{ ds.dsType }}</a-tag>
                      <a-tag v-if="ds.dataLayer" size="small" color="purple">{{ ds.dataLayer }}</a-tag>
                    </div>
                  </a-select-option>
                </a-select>
                <div class="form-tip">
                  <span v-if="wizardForm.targetDsId">
                    已选择：<strong>{{ getDsName(wizardForm.targetDsId) }}</strong>
                    ，共 {{ metadataList.length }} 张表（元数据 + 库中实时表名合并）
                  </span>
                  <span v-else>请从下拉列表中选择已配置的数据源</span>
                </div>
              </a-form-item>

              <!-- PostgreSQL Schema 选择器（选完数据源后出现） -->
              <a-form-item
                v-if="isPostgresDs"
                label="Schema"
                name="targetSchema"
                :rules="[{ required: isPostgresDs, message: '请选择 schema' }]"
              >
                <a-select
                  v-model:value="wizardForm.targetSchema"
                  placeholder="请先选择 schema，再加载目标表"
                  :loading="schemaLoading"
                  :disabled="schemaLoading"
                  @change="onSchemaChange"
                >
                  <a-select-option v-for="s in schemaList" :key="s" :value="s">
                    <div class="schema-option">
                      <span style="color: #722ED1; font-size: 13px">{{ s }}</span>
                    </div>
                  </a-select-option>
                </a-select>
                <div class="form-tip" v-if="schemaList.length === 0 && !schemaLoading && wizardForm.targetDsId">
                  <span v-if="isPostgresDs">未找到可用的 schema，请确认数据源配置</span>
                </div>
              </a-form-item>

              <a-form-item
                label="目标表"
                name="targetTable"
                :rules="[{ required: true, message: '请选择或输入目标表名' }]"
              >
                <a-select
                  v-if="metadataList.length > 0"
                  v-model:value="wizardForm.targetTable"
                  placeholder="从库中表名列表选择，或输入关键字筛选"
                  show-search
                  :loading="metadataLoading || liveTablesLoading"
                  :filter-option="filterTableOption"
                  @search="onWizardTableSearch"
                  @change="onWizardTableSelectChange"
                >
                  <a-select-option
                    v-for="t in filteredMetadataList"
                    :key="t.tableName"
                    :value="t.tableName"
                  >
                    <div class="table-option">
                      <FileOutlined style="color: #722ED1; margin-right: 6px" />
                      <span>{{ t.tableName }}</span>
                      <span v-if="t.tableAlias" class="table-alias">{{ t.tableAlias }}</span>
                      <span class="column-count">{{ t.columnCount || '' }}</span>
                    </div>
                  </a-select-option>
                </a-select>
                <a-input
                  v-else
                  v-model:value="wizardForm.targetTable"
                  placeholder="未拉到表清单时可直接输入表名，失焦后加载字段列表"
                  @blur="onManualTableNameBlur"
                >
                  <template #suffix>
                    <a-tooltip title="表名必须与数据库中的实际表名完全一致">
                      <QuestionCircleOutlined style="color: #8C8C8C" />
                    </a-tooltip>
                  </template>
                </a-input>
                <div class="form-tip">
                  <span v-if="selectedTableMeta">
                    已选择表：<code>{{ wizardForm.targetTable }}</code>
                    <span v-if="selectedTableMeta.tableComment" class="table-comment">
                      — {{ selectedTableMeta.tableComment }}
                    </span>
                  </span>
                  <span v-else-if="wizardForm.targetTable">已输入：<code>{{ wizardForm.targetTable }}</code></span>
                  <span v-else-if="!wizardForm.targetDsId">请先选择数据源，再选择目标表</span>
                  <span v-else-if="isPostgresDs && !wizardForm.targetSchema">请先选择 Schema，再加载目标表</span>
                  <span v-else-if="metadataLoading || liveTablesLoading">正在加载表清单…</span>
                  <span v-else-if="metadataList.length === 0">
                    当前未获取到表清单，可直接输入完整表名（失焦后加载字段）；若长期为空请检查库权限或元数据扫描
                  </span>
                  <span v-else>请从下拉列表选择目标表，或输入关键字筛选</span>
                </div>
              </a-form-item>

              <a-form-item
                label="探查级别"
                name="profileLevel"
                :rules="[{ required: true, message: '请选择探查级别' }]"
              >
                <div class="profile-level-selector">
                  <div
                    v-for="level in profileLevelOptions"
                    :key="level.value"
                    class="level-card"
                    :class="{ 'level-selected': wizardForm.profileLevel === level.value }"
                    @click="wizardForm.profileLevel = level.value"
                  >
                    <div class="level-icon">
                      <component :is="level.icon" />
                    </div>
                    <div class="level-content">
                      <div class="level-name">{{ level.name }}</div>
                      <div class="level-desc">{{ level.desc }}</div>
                      <div class="level-items">
                        <a-tag v-for="item in level.items" :key="item" size="small">{{ item }}</a-tag>
                      </div>
                    </div>
                    <div class="level-check" v-if="wizardForm.profileLevel === level.value">
                      <CheckCircleFilled style="color: #722ED1; font-size: 18px" />
                    </div>
                  </div>
                </div>
              </a-form-item>

              <a-form-item
                v-if="wizardForm.profileLevel !== 'BASIC'"
                label="指定列"
              >
                <div v-if="columnListLoading" class="form-tip">
                  <a-spin size="small" style="margin-right: 8px" />
                  正在从数据源加载字段列表…
                </div>
                <div v-else-if="selectedTableColumns.length > 0">
                  <a-table
                    size="small"
                    :columns="targetColumnSelectColumns"
                    :data-source="selectedTableColumns"
                    :row-key="(row: any) => row.columnName"
                    :pagination="false"
                    :row-selection="targetColumnRowSelection"
                    :scroll="{ x: 820 }"
                  />
                  <div class="form-tip" style="margin-top: 10px">
                    勾选要探查的列（留空表示探查全部列）
                    <a-button type="link" size="small" @click="selectAllColumns" style="margin-left: 10px">全选</a-button>
                    <a-button type="link" size="small" @click="clearSelectedColumns" style="margin-left: 6px">清空</a-button>
                    <span style="margin-left: 12px">已选 <strong>{{ selectedTargetColumns.length }}</strong> 列</span>
                  </div>
                </div>
                <div v-else class="form-tip">
                  <span v-if="wizardForm.targetTable">未能加载字段列表，请确认表名与库中一致或检查数据源权限</span>
                  <span v-else>先选择目标表，下方将显示可勾选的字段映射表</span>
                </div>
              </a-form-item>
            </a-form>
          </div>

          <!-- ====== Step 3: 触发配置 ====== -->
          <div v-if="currentStep === 2" class="step-panel">
            <div class="step-panel-header">
              <div class="panel-title">配置触发方式</div>
              <div class="panel-desc">设置任务的执行触发方式和调度规则</div>
            </div>
            <a-form
              ref="step3FormRef"
              :model="wizardForm"
              :label-col="{ span: 5 }"
              :wrapper-col="{ span: 18 }"
            >
              <a-form-item label="触发方式" name="triggerType">
                <div class="trigger-type-selector">
                  <div
                    v-for="type in triggerTypeOptions"
                    :key="type.value"
                    class="trigger-card"
                    :class="{ 'trigger-selected': wizardForm.triggerType === type.value }"
                    @click="wizardForm.triggerType = type.value"
                  >
                    <div class="trigger-icon-wrapper" :style="{ background: type.iconBg }">
                      <component :is="type.icon" style="color: white; font-size: 20px" />
                    </div>
                    <div class="trigger-content">
                      <div class="trigger-name">{{ type.name }}</div>
                      <div class="trigger-desc">{{ type.desc }}</div>
                    </div>
                  </div>
                </div>
              </a-form-item>

              <a-form-item
                v-if="wizardForm.triggerType === 'SCHEDULE'"
                label="Cron表达式"
                name="triggerCron"
                :rules="[{ required: wizardForm.triggerType === 'SCHEDULE', message: '请填写Cron表达式' }]"
              >
                <a-input
                  v-model:value="wizardForm.triggerCron"
                  placeholder="如 0 0 2 * * ?（每天凌晨2点）"
                />
                <div class="cron-helper">
                  <div class="cron-presets">
                    <a-tag
                      v-for="preset in cronPresets"
                      :key="preset.value"
                      class="cron-preset-tag"
                      :class="{ 'preset-active': wizardForm.triggerCron === preset.value }"
                      @click="wizardForm.triggerCron = preset.value"
                    >
                      {{ preset.label }}
                    </a-tag>
                  </div>
                  <div class="form-tip">
                    <ClockCircleOutlined style="margin-right: 4px" />
                    <span v-if="wizardForm.triggerCron">
                      下次执行时间：{{ nextExecutionTime }}
                    </span>
                    <span v-else>选择一个预设或手动填写Cron表达式</span>
                  </div>
                </div>
              </a-form-item>

              <a-form-item label="超时时间" name="timeoutMinutes">
                <a-input-number
                  v-model:value="wizardForm.timeoutMinutes"
                  :min="1"
                  :max="1440"
                  placeholder="分钟"
                  style="width: 200px"
                />
                <div class="form-tip">超时自动取消（分钟），默认60分钟。超时后可手动重试</div>
              </a-form-item>
            </a-form>

            <!-- 触发方式说明 -->
            <a-alert
              v-if="wizardForm.triggerType === 'MANUAL'"
              message="💡 手动触发说明"
              type="info"
              show-icon
              style="margin-top: 8px"
            >
              <template #description>
                手动触发模式下，任务不会自动执行。您可以在任务列表中点击"执行探查"按钮手动运行，或在任务详情页中点击"立即探查"。
              </template>
            </a-alert>
            <a-alert
              v-if="wizardForm.triggerType === 'SCHEDULE'"
              message="💡 定时触发说明"
              type="info"
              show-icon
              style="margin-top: 8px"
            >
              <template #description>
                定时触发模式下，任务将按照设定的Cron表达式自动执行。首次保存后，调度器将自动注册定时任务。
              </template>
            </a-alert>
          </div>

          <!-- ====== Step 4: 确认并创建 ====== -->
          <div v-if="currentStep === 3" class="step-panel">
            <div class="step-panel-header">
              <div class="panel-title">确认配置</div>
              <div class="panel-desc">请确认以下配置信息，确认无误后点击"创建任务"按钮</div>
            </div>

            <div class="confirm-summary">
              <a-descriptions :column="2" size="small" bordered>
                <a-descriptions-item label="任务名称" :span="2">
                  <strong>{{ wizardForm.taskName }}</strong>
                  <a-tag v-if="isEditMode" color="blue" style="margin-left: 8px">编辑模式</a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="任务编码">
                  <code>{{ wizardForm.taskCode || autoGeneratedCode }}</code>
                </a-descriptions-item>
                <a-descriptions-item label="任务描述">
                  {{ wizardForm.taskDesc || '-' }}
                </a-descriptions-item>

                <a-descriptions-item label="目标数据源" :span="2">
                  {{ getDsName(wizardForm.targetDsId) }}
                </a-descriptions-item>
                <a-descriptions-item label="目标表" :span="2">
                  <code>{{ wizardForm.targetTable }}</code>
                </a-descriptions-item>
                <a-descriptions-item label="探查级别">
                  <a-tag :color="getProfileLevelColor(wizardForm.profileLevel)">
                    {{ getProfileLevelLabel(wizardForm.profileLevel) }}
                  </a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="指定列">
                  {{ wizardForm.targetColumns || '全部列' }}
                </a-descriptions-item>

                <a-descriptions-item label="触发方式">
                  <a-tag :color="getTriggerColor(wizardForm.triggerType)">
                    {{ getTriggerLabel(wizardForm.triggerType) }}
                  </a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="超时时间">
                  {{ wizardForm.timeoutMinutes || 60 }} 分钟
                </a-descriptions-item>
                <a-descriptions-item v-if="wizardForm.triggerType === 'SCHEDULE'" label="执行周期" :span="2">
                  <code>{{ wizardForm.triggerCron }}</code>
                  <span style="margin-left: 8px; color: #8C8C8C">
                    ({{ nextExecutionTime }})
                  </span>
                </a-descriptions-item>
              </a-descriptions>
            </div>

            <a-checkbox v-model:checked="executeAfterCreate" class="execute-after-checkbox">
              <span>创建后立即执行一次探查</span>
              <span class="checkbox-desc">任务创建成功后，立即执行一次探查任务并查看探查报告</span>
            </a-checkbox>
          </div>

        </div>

        <!-- 向导底部操作区 -->
        <div class="wizard-footer">
          <a-space>
            <a-button v-if="currentStep > 0" @click="currentStep--">
              <template #icon><LeftOutlined /></template>
              上一步
            </a-button>
            <a-button v-if="currentStep < wizardSteps.length - 1" type="primary" @click="nextStep">
              下一步
              <template #icon><RightOutlined /></template>
            </a-button>
            <a-button
              v-if="currentStep === wizardSteps.length - 1"
              type="primary"
              :loading="saving"
              @click="handleFinalSubmit"
            >
              {{ isEditMode ? '保存修改' : '创建任务' }}
            </a-button>
          </a-space>
          <a-button @click="wizardVisible = false">取消</a-button>
        </div>

      </div>
    </a-modal>

    <!-- ============================================================ -->
    <!-- 执行进度弹窗 -->
    <!-- ============================================================ -->
    <a-modal
      v-model:open="executeModalVisible"
      :title="`执行探查: ${currentTask?.taskName || ''}`"
      :footer="null"
      :maskClosable="false"
      width="640"
    >
      <div class="execute-progress">
        <div v-if="!executeDone">
          <a-result status="processing" title="探查执行中" sub-title="正在采集表结构和内容统计，请稍候...">
            <template #extra>
              <a-progress :percent="executeProgress" status="active" />
              <div class="execute-tip">
                <span v-if="executeStep === 'init'">正在初始化连接...</span>
                <span v-else-if="executeStep === 'table'">正在获取表级统计信息（行数、列数、存储大小）...</span>
                <span v-else-if="executeStep === 'column'">正在分析列级数据分布...</span>
                <span v-else-if="executeStep === 'distribution'">正在计算分布直方图和高频值...</span>
                <span v-else-if="executeStep === 'anomaly'">正在执行异常值检测（3σ / IQR 法则）...</span>
                <span v-else-if="executeStep === 'finalize'">正在汇总探查结果...</span>
              </div>
              <div class="execute-stats" v-if="executeStats">
                <a-row :gutter="16">
                  <a-col :span="8">
                    <div class="exec-stat">
                      <div class="exec-stat-val">{{ executeStats.rowCount }}</div>
                      <div class="exec-stat-label">行数</div>
                    </div>
                  </a-col>
                  <a-col :span="8">
                    <div class="exec-stat">
                      <div class="exec-stat-val">{{ executeStats.columnCount }}</div>
                      <div class="exec-stat-label">列数</div>
                    </div>
                  </a-col>
                  <a-col :span="8">
                    <div class="exec-stat">
                      <div class="exec-stat-val">{{ executeStats.elapsed }}</div>
                      <div class="exec-stat-label">耗时</div>
                    </div>
                  </a-col>
                </a-row>
              </div>
            </template>
          </a-result>
          <div class="execute-footer">
            <a-button @click="handleCancelExecute">取消</a-button>
          </div>
        </div>

        <div v-else>
          <a-result
            :status="executeSuccess ? 'success' : 'error'"
            :title="executeSuccess ? '探查完成' : '探查失败'"
            :sub-title="executeSuccess
              ? `成功完成探查，共采集 ${resultStats?.columnCount || 0} 个字段`
              : executeError"
          >
            <template #extra>
              <a-descriptions :column="2" size="small" bordered v-if="resultStats">
                <a-descriptions-item label="表名">{{ resultStats.tableName }}</a-descriptions-item>
                <a-descriptions-item label="行数"><span class="mono-text">{{ formatNumber(resultStats.rowCount) }}</span></a-descriptions-item>
                <a-descriptions-item label="列数">{{ resultStats.columnCount }}</a-descriptions-item>
                <a-descriptions-item label="存储大小">{{ formatBytes(resultStats.storageBytes) }}</a-descriptions-item>
                <a-descriptions-item label="探查级别">{{ getProfileLevelLabel(currentTask?.profileLevel) }}</a-descriptions-item>
                <a-descriptions-item label="执行时间">{{ formatTime(resultStats.profileTime) }}</a-descriptions-item>
              </a-descriptions>
              <div class="result-actions">
                <a-button type="primary" @click="openReportDrawer(currentTask!)">
                  查看详细报告
                </a-button>
                <a-button @click="executeModalVisible = false">关闭</a-button>
              </div>
            </template>
          </a-result>
        </div>
      </div>
    </a-modal>

    <!-- ============================================================ -->
    <!-- 探查报告抽屉 -->
    <!-- ============================================================ -->
    <a-drawer
      v-model:open="reportDrawerVisible"
      :title="`探查报告: ${currentTask?.taskName || ''}`"
      :width="960"
      @close="reportDrawerVisible = false"
    >
      <div class="report-content" v-if="currentTask">

        <!-- 报告概览 -->
        <a-card title="表级统计" size="small" class="report-card">
          <template #extra>
            <a-space>
              <a-tag color="purple">{{ getDsName(currentTask?.targetDsId) }}</a-tag>
              <a-tag>{{ getProfileLevelLabel(currentTask?.profileLevel) }}</a-tag>
            </a-space>
          </template>
          <a-descriptions :column="3" size="small" bordered>
            <a-descriptions-item label="目标表">
              <code>{{ currentTask.targetTable }}</code>
            </a-descriptions-item>
            <a-descriptions-item label="行数">
              <span class="mono-text">{{ formatNumber(reportTableStats?.rowCount) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="列数">{{ reportTableStats?.columnCount || '-' }}</a-descriptions-item>
            <a-descriptions-item label="存储大小">{{ formatBytes(reportTableStats?.storageBytes) }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ formatTime(reportTableStats?.profileTime) }}</a-descriptions-item>
            <a-descriptions-item label="增量行数">{{ formatNumber(reportTableStats?.incrementRows) }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 异常警告区 -->
        <a-card
          v-if="anomalyColumns.length > 0"
          title="异常检测结果"
          size="small"
          class="report-card anomaly-card"
        >
          <template #extra>
            <a-tag color="orange">{{ anomalyColumns.length }} 个字段存在异常</a-tag>
          </template>
          <div class="anomaly-list">
            <div
              v-for="col in anomalyColumns.slice(0, 5)"
              :key="col.columnName"
              class="anomaly-item"
            >
              <div class="anomaly-left">
                <a-tag color="orange">{{ col.columnName }}</a-tag>
                <span class="anomaly-type">{{ col.anomalyType }}</span>
              </div>
              <div class="anomaly-right">
                <span class="anomaly-value">{{ col.anomalyValue }}</span>
                <span class="anomaly-detail">{{ col.anomalyDetail }}</span>
              </div>
            </div>
            <div v-if="anomalyColumns.length > 5" class="anomaly-more">
              还有 {{ anomalyColumns.length - 5 }} 个字段存在异常，点击列级统计表查看详情
            </div>
          </div>
        </a-card>

        <!-- 列级统计 -->
        <a-card title="列级统计" size="small" class="report-card">
          <template #extra>
            <a-space>
              <a-space>
                <span style="font-size: 12px; color: #8C8C8C">显示分布</span>
                <a-switch v-model:checked="showDistribution" size="small" />
              </a-space>
              <a-space>
                <span style="font-size: 12px; color: #8C8C8C">异常高亮</span>
                <a-switch v-model:checked="highlightAnomaly" size="small" />
              </a-space>
            </a-space>
          </template>
          <a-table
            :columns="columnReportColumns"
            :data-source="reportColumnStats"
            :pagination="{ pageSize: 10 }"
            :row-key="(record: any) => record.id"
            size="small"
            :scroll="{ x: 1100 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'nullRate'">
                <div class="rate-cell">
                  <a-progress
                    :percent="Math.round((record.nullRate || 0) * 100)"
                    :stroke-color="getNullRateColor(record.nullRate)"
                    size="small"
                    :show-info="false"
                    style="width: 60px; display: inline-block; margin-right: 6px"
                  />
                  <span class="mono-text">{{ ((record.nullRate || 0) * 100).toFixed(1) }}%</span>
                </div>
              </template>

              <template v-if="column.key === 'uniqueRate'">
                <span class="mono-text">{{ ((record.uniqueRate || 0) * 100).toFixed(1) }}%</span>
              </template>

              <template v-if="column.key === 'nullCount'">
                <span class="mono-text">{{ formatNumber(record.nullCount) }}</span>
              </template>

              <template v-if="column.key === 'uniqueCount'">
                <span class="mono-text">{{ formatNumber(record.uniqueCount) }}</span>
              </template>

              <template v-if="column.key === 'minValue'">
                <span class="mono-text truncate" :title="record.minValue">{{ record.minValue || '-' }}</span>
              </template>

              <template v-if="column.key === 'maxValue'">
                <span class="mono-text truncate" :title="record.maxValue">{{ record.maxValue || '-' }}</span>
              </template>

              <template v-if="column.key === 'avgValue'">
                <span class="mono-text">{{ record.avgValue != null ? record.avgValue.toFixed(4) : '-' }}</span>
              </template>

              <template v-if="column.key === 'anomaly'">
                <template v-if="record.outlierCount && record.outlierCount > 0">
                  <a-tooltip :title="`${record.outlierCount} 个异常值（${record.outlierMethod || 'IQR'}法则）`">
                    <a-badge status="warning" :text="record.outlierCount.toString()" />
                  </a-tooltip>
                </template>
                <span v-else style="color: #52C41A">-</span>
              </template>

              <template v-if="column.key === 'action'">
                <a-tooltip title="查看分布">
                  <a-button type="text" size="small" @click="showColumnDistribution(record)">
                    <template #icon><BarChartOutlined /></template>
                  </a-button>
                </a-tooltip>
              </template>
            </template>
          </a-table>
        </a-card>

        <!-- 分布可视化区域 -->
        <a-card
          v-if="distributionVisible"
          title="字段分布"
          size="small"
          class="report-card"
        >
          <template #extra>
            <a-space>
              <a-tag color="purple">{{ distributionColumn }}</a-tag>
              <a-tag>{{ distributionDataType }}</a-tag>
              <a-tag v-if="isNumericDistribution" color="blue">数值型</a-tag>
              <a-tag v-else color="green">分类型</a-tag>
              <a-button type="text" size="small" @click="distributionVisible = false">
                <template #icon><CloseOutlined /></template>
              </a-button>
            </a-space>
          </template>

          <a-row :gutter="[16, 16]">
            <!-- 主分布图 -->
            <a-col :span="distributionTab === 'histogram' ? 24 : 12">
              <div class="chart-section">
                <div class="chart-tabs">
                  <a-radio-group v-model:value="distributionTab" button-style="solid" size="small">
                    <a-radio-button value="histogram">直方图</a-radio-button>
                    <a-radio-button value="frequency">频率图</a-radio-button>
                    <a-radio-button value="pie">占比图</a-radio-button>
                  </a-radio-group>
                </div>
                <div :ref="el => chartRefs.main = el as HTMLDivElement" style="height: 320px"></div>
              </div>
            </a-col>

            <!-- Top值 -->
            <a-col :span="12" v-if="topValuesData.length > 0">
              <div class="top-values-section">
                <div class="section-label">高频值 Top 10</div>
                <div class="top-values-list">
                  <div v-for="(item, idx) in topValuesData" :key="idx" class="top-value-item">
                    <div class="top-rank">{{ idx + 1 }}</div>
                    <div class="top-value-bar-wrapper">
                      <div class="top-value-name">{{ item.value || '(空值)' }}</div>
                      <div class="top-value-bar-bg">
                        <div
                          class="top-value-bar-fill"
                          :style="{ width: item.percent + '%', background: topValueColors[idx % topValueColors.length] }"
                        ></div>
                      </div>
                      <div class="top-value-count">
                        <span class="mono-text">{{ formatNumber(item.count) }}</span>
                        <span class="top-percent">({{ item.percent }}%)</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </a-col>

            <!-- 统计摘要 -->
            <a-col :span="12" v-if="currentDistributionStats">
              <div class="stats-summary">
                <div class="section-label">统计摘要</div>
                <a-descriptions :column="1" size="small" bordered>
                  <a-descriptions-item label="数据类型">
                    <a-tag>{{ distributionDataType }}</a-tag>
                  </a-descriptions-item>
                  <a-descriptions-item label="总记录数">
                    <span class="mono-text">{{ formatNumber(currentDistributionStats.totalCount) }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="非空记录数">
                    <span class="mono-text">{{ formatNumber(currentDistributionStats.nonNullCount) }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="最小值" v-if="currentDistributionStats.min != null">
                    <span class="mono-text">{{ currentDistributionStats.min }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="最大值" v-if="currentDistributionStats.max != null">
                    <span class="mono-text">{{ currentDistributionStats.max }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="平均值" v-if="currentDistributionStats.avg != null">
                    <span class="mono-text">{{ currentDistributionStats.avg?.toFixed(4) }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="中位数" v-if="currentDistributionStats.median != null">
                    <span class="mono-text">{{ currentDistributionStats.median }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="标准差" v-if="currentDistributionStats.stdDev != null">
                    <span class="mono-text">{{ currentDistributionStats.stdDev?.toFixed(4) }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="异常值数" v-if="currentDistributionStats.outlierCount > 0">
                    <a-badge status="warning" :text="currentDistributionStats.outlierCount + ' 个'" />
                  </a-descriptions-item>
                </a-descriptions>
              </div>
            </a-col>
          </a-row>
        </a-card>

        <!-- Top值汇总 -->
        <a-card
          v-if="allTopValuesData.length > 0"
          title="高频值汇总（所有列）"
          size="small"
          class="report-card"
        >
          <div class="top-values-grid">
            <div v-for="(colData, colIdx) in allTopValuesData" :key="colIdx" class="col-top-values">
              <div class="col-top-title">
                <FileOutlined style="color: #722ED1; margin-right: 4px" />
                <span class="mono-text">{{ colData.columnName }}</span>
                <a-tag size="small">{{ colData.dataType }}</a-tag>
              </div>
              <div class="col-top-bars">
                <div v-for="(item, idx) in colData.values.slice(0, 5)" :key="idx" class="col-top-item">
                  <span class="col-top-val">{{ item.value || '(空)' }}</span>
                  <div class="col-top-bar-bg">
                    <div
                      class="col-top-bar-fill"
                      :style="{ width: item.percent + '%' }"
                    ></div>
                  </div>
                  <span class="col-top-pct">{{ item.percent }}%</span>
                </div>
              </div>
            </div>
          </div>
        </a-card>

        <div class="drawer-footer">
          <a-space>
            <a-button @click="openSnapshotDialog">创建快照</a-button>
            <a-button @click="handleReExecute">重新执行</a-button>
          </a-space>
        </div>
      </div>
    </a-drawer>

    <!-- ============================================================ -->
    <!-- 创建快照弹窗 -->
    <!-- ============================================================ -->
    <a-modal
      v-model:open="snapshotDialogVisible"
      title="创建探查快照"
      :footer="null"
      width="480"
    >
      <a-form
        ref="snapshotFormRef"
        :model="snapshotForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="快照名称" name="snapshotName" :rules="[{ required: true, message: '请输入快照名称' }]">
          <a-input v-model:value="snapshotForm.snapshotName" placeholder="请输入快照名称" />
        </a-form-item>
        <a-form-item label="快照描述" name="snapshotDesc">
          <a-textarea v-model:value="snapshotForm.snapshotDesc" placeholder="选填" :rows="2" />
        </a-form-item>
      </a-form>
      <div style="text-align: right; margin-top: 16px">
        <a-space>
          <a-button @click="snapshotDialogVisible = false">取消</a-button>
          <a-button type="primary" :loading="snapshotSaving" @click="handleCreateSnapshot">确认创建</a-button>
        </a-space>
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
      <div class="snapshot-filter">
        <a-select
          v-model:value="snapshotDsId"
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
          v-model:value="snapshotTable"
          placeholder="输入表名"
          style="width: 160px"
          allowClear
          @change="loadSnapshots"
        />
      </div>

      <!-- 快照空状态 -->
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

    <!-- ============================================================ -->
    <!-- 配置指南弹窗 -->
    <!-- ============================================================ -->
    <a-modal
      v-model:open="showGuideModal"
      title="探查任务配置指南"
      :footer="null"
      width="680"
    >
      <div class="guide-content">
        <a-divider orientation="left">Step 1 — 探查配置</a-divider>
        <p>先选择数据源与目标表（表名来自库中实时列表，并与已扫描的元数据合并展示），再选择探查级别与需要探查的列：</p>
        <ul>
          <li><strong>仅表级</strong>：快速获取行数、列数、存储大小等基本信息，资源消耗低</li>
          <li><strong>表+列级</strong>：包含基础统计 + 分布分析，适合日常数据监控</li>
          <li><strong>完整探查</strong>：全量统计 + 异常检测，适合深度数据分析</li>
        </ul>

        <a-divider orientation="left">Step 2 — 基本信息</a-divider>
        <p>填写任务名称与描述。选表后会按「数据层 + 表名」生成默认名称，可自行修改。建议命名格式：<code>数据层_业务含义_频率</code></p>

        <a-divider orientation="left">Step 3 — 触发方式</a-divider>
        <ul>
          <li><strong>手动触发</strong>：随时手动执行，适合临时探查</li>
          <li><strong>定时触发</strong>：按Cron表达式自动执行，适合周期性监控</li>
          <li><strong>事件触发</strong>：当数据源发生特定事件时执行（开发中）</li>
        </ul>

        <a-divider orientation="left">Cron 表达式示例</a-divider>
        <a-table :columns="cronExamplesColumns" :data-source="cronExamples" :pagination="false" size="small">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'cron'">
              <code>{{ record.cron }}</code>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import * as echarts from 'echarts'
import {
  PlusOutlined, SearchOutlined, ReloadOutlined, FileOutlined,
  EditOutlined, DeleteOutlined, PlayCircleOutlined, StopOutlined,
  PlaySquareOutlined, FileSearchOutlined, CameraOutlined,
  BarChartOutlined, CloseOutlined, EyeOutlined, CheckOutlined,
  LeftOutlined, RightOutlined, CheckCircleOutlined, CheckCircleFilled,
  ExclamationCircleOutlined, QuestionCircleOutlined, ClockCircleOutlined,
  ExperimentOutlined, DatabaseOutlined, ThunderboltOutlined
} from '@ant-design/icons-vue'
import { dprofileApi } from '@/api/dprofile'
import { dataSourceApi } from '@/api/dqc'
import { metadataApi } from '@/api/gov'
import type {
  ProfileTask, TableStats, ColumnStats, Snapshot,
  ProfileResult, TaskStats, ProfileExecutionRecord
} from '@/api/dprofile'

// ============ 状态 ============
const loading = ref(false)
const dsLoading = ref(false)
const metadataLoading = ref(false)
const liveTablesLoading = ref(false)
const columnListLoading = ref(false)
const schemaList = ref<string[]>([])
const schemaLoading = ref(false)
const saving = ref(false)
const snapshotSaving = ref(false)
const snapshotLoading = ref(false)

const taskData = ref<ProfileTask[]>([])
const dataSourceList = ref<any[]>([])
const metadataList = ref<any[]>([])
const currentTask = ref<ProfileTask | null>(null)

// ============ 筛选 ============
const filterTaskName = ref('')
const filterTriggerType = ref<string | undefined>()
const filterProfileLevel = ref<string | undefined>()
const filterDsId = ref<number | undefined>()
const filterStatus = ref<string | undefined>()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const statCards = ref([
  { label: '总任务数', value: 0, bg: 'linear-gradient(135deg, #722ED1 0%, #9254DE 100%)' },
  { label: '运行中', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #69C0FF 100%)' },
  { label: '已启用', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '总执行次数', value: 0, bg: 'linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)' }
])

// ============ 向导状态 ============
const wizardVisible = ref(false)
const currentStep = ref(0)
const isEditMode = ref(false)
const showGuideModal = ref(false)
const isCodeValid = ref<boolean | undefined>()
const executeAfterCreate = ref(true)

const wizardSteps = [
  { title: '探查配置', desc: '选择数据源、目标表与探查级别' },
  { title: '基本信息', desc: '填写任务名称（可按表名生成）与描述' },
  { title: '触发配置', desc: '设置执行触发方式' },
  { title: '确认创建', desc: '确认配置并提交' }
]

const step1FormRef = ref<FormInstance>()
const step2FormRef = ref<FormInstance>()
const step3FormRef = ref<FormInstance>()

const wizardForm = reactive({
  taskName: '',
  taskCode: '',
  taskDesc: '',
  targetDsId: undefined as number | undefined,
  targetTable: '',
  targetColumns: '',
  targetSchema: '',   // PostgreSQL 专用
  profileLevel: 'DETAILED',
  triggerType: 'MANUAL',
  triggerCron: '',
  timeoutMinutes: 60
})

const wizardFormSnapshot = ref<any>(null)

const autoGeneratedCode = computed(() => {
  if (!wizardForm.taskName) return 'TASK_XXXX'
  const base = 'TASK_' + wizardForm.taskName
    .replace(/[^a-zA-Z0-9\u4e00-\u9fa5]/g, '_')
    .substring(0, 30)
    .toUpperCase()
  return base + '_' + Date.now().toString(36).toUpperCase()
})

// 任务名称自动生成（基于表名），除非用户已手动修改
const taskNameManuallyEdited = ref(false)
let taskNameTimer: ReturnType<typeof setTimeout> | null = null

watch(() => wizardForm.targetTable, (newTable) => {
  if (!newTable || taskNameManuallyEdited.value) return
  if (taskNameTimer) clearTimeout(taskNameTimer)
  taskNameTimer = setTimeout(() => {
    const ds = dataSourceList.value.find(d => d.id === wizardForm.targetDsId)
    const layer = ds?.dataLayer ? `${ds.dataLayer}_` : ''
    wizardForm.taskName = `探查_${layer}${newTable}`
  }, 200)
})

const selectedTableMeta = computed(() => {
  return metadataList.value.find(m => m.tableName === wizardForm.targetTable)
})

const selectedTableColumns = computed(() => {
  if (!selectedTableMeta.value) return []
  return (selectedTableMeta.value as any)._columns || []
})

const selectedDsType = computed(() => {
  if (!wizardForm.targetDsId) return ''
  const ds = dataSourceList.value.find(d => d.id === wizardForm.targetDsId)
  return ds?.dsType?.toUpperCase() || ''
})

const isPostgresDs = computed(() => selectedDsType.value === 'POSTGRESQL')

const filteredMetadataList = ref<any[]>([])
function filterTableOption(input: string, option: any) {
  const v = (option?.value ?? '').toString().toLowerCase()
  return v.includes((input || '').toLowerCase())
}

const profileLevelOptions = [
  {
    value: 'BASIC',
    name: '仅表级',
    desc: '快速获取基本信息，资源消耗低',
    icon: DatabaseOutlined,
    items: ['行数', '列数', '存储大小']
  },
  {
    value: 'DETAILED',
    name: '表+列级',
    desc: '包含基础统计和分布分析',
    icon: BarChartOutlined,
    items: ['表级统计', '空值率', '唯一值', '高频值']
  },
  {
    value: 'FULL',
    name: '完整探查',
    desc: '全量统计+异常检测',
    icon: ExperimentOutlined,
    items: ['完整统计', '直方图', '3σ异常检测', 'IQR异常检测']
  }
]

const triggerTypeOptions = [
  {
    value: 'MANUAL',
    name: '手动触发',
    desc: '随时手动执行，适合临时探查',
    icon: EditOutlined,
    iconBg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)'
  },
  {
    value: 'SCHEDULE',
    name: '定时触发',
    desc: '按Cron表达式自动执行，适合周期性监控',
    icon: ClockCircleOutlined,
    iconBg: 'linear-gradient(135deg, #1677FF 0%, #69C0FF 100%)'
  },
  {
    value: 'EVENT',
    name: '事件触发',
    desc: '数据源变更时自动执行（开发中）',
    icon: ThunderboltOutlined,
    iconBg: 'linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)'
  }
]

const cronPresets = [
  { label: '每天凌晨2点', value: '0 0 2 * * ?' },
  { label: '每小时', value: '0 0 * * * ?' },
  { label: '每天中午12点', value: '0 0 12 * * ?' },
  { label: '每周一', value: '0 0 2 ? * MON' },
  { label: '每月1号', value: '0 0 2 1 * ?' }
]

const nextExecutionTime = computed(() => {
  if (!wizardForm.triggerCron) return '-'
  // 简单的下次执行时间估算
  try {
    const now = new Date()
    return now.toLocaleString('zh-CN') + ' (已设置)'
  } catch {
    return '请填写有效的Cron表达式'
  }
})

// ============ 执行 ============
const executeModalVisible = ref(false)
const executeDone = ref(false)
const executeSuccess = ref(false)
const executeError = ref('')
const executeProgress = ref(0)
const executeStep = ref('')
const resultStats = ref<TableStats | null>(null)
const executeStats = ref<{ rowCount: string; columnCount: string; elapsed: string } | null>(null)
/** 执行探查进度/结果轮询定时器（须与 pollForExecution / pollForResults 共用） */
const pollingTimer = ref<ReturnType<typeof setTimeout> | null>(null)

// ============ 报告 ============
const reportDrawerVisible = ref(false)
const reportTableStats = ref<TableStats | null>(null)
const reportColumnStats = ref<ColumnStats[]>([])
const showDistribution = ref(false)
const highlightAnomaly = ref(true)
const distributionVisible = ref(false)
const distributionColumn = ref('')
const distributionDataType = ref('')
const isNumericDistribution = ref(false)
const distributionTab = ref('histogram')
const topValuesData = ref<any[]>([])
const allTopValuesData = ref<any[]>([])
const currentDistributionStats = ref<any | null>(null)
const anomalyColumns = ref<any[]>([])
const chartRefs = reactive<{ main?: HTMLDivElement | null; [key: string]: HTMLDivElement | null | undefined }>({})
const topValueColors = [
  '#722ED1', '#9254DE', '#B37FEB', '#D3ADF7', '#E8D5FF',
  '#13C2C2', '#52C41A', '#FAAD14', '#1677FF', '#FF4D4F'
]

// ============ 快照 ============
const snapshotDrawerVisible = ref(false)
const snapshotDialogVisible = ref(false)
const snapshotFormRef = ref<FormInstance>()
const snapshotDsId = ref<number | undefined>()
const snapshotTable = ref('')
const snapshotData = ref<Snapshot[]>([])
const snapshotForm = reactive({
  snapshotName: '',
  snapshotDesc: ''
})

// ============ 表格列 ============
const taskColumns = [
  { title: '任务名称', key: 'taskName', width: 200, ellipsis: true },
  { title: '目标表', key: 'targetTable', width: 160, ellipsis: true },
  { title: '探查级别', key: 'profileLevel', width: 100 },
  { title: '触发方式', key: 'triggerType', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '上次执行', key: 'lastExecutionTime', width: 160 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

const columnReportColumns = [
  { title: '序号', width: 50, align: 'center' as const, customRender: ({ index }: any) => index + 1 },
  { title: '字段名', dataIndex: 'columnName', width: 160, ellipsis: true },
  { title: '数据类型', dataIndex: 'dataType', width: 120 },
  { title: '可空', dataIndex: 'nullable', width: 60, align: 'center' as const },
  { title: '空值数', key: 'nullCount', width: 90, align: 'right' as const },
  { title: '空值率', key: 'nullRate', width: 140 },
  { title: '唯一数', key: 'uniqueCount', width: 90, align: 'right' as const },
  { title: '唯一率', key: 'uniqueRate', width: 80 },
  { title: '最小值', key: 'minValue', width: 120 },
  { title: '最大值', key: 'maxValue', width: 120 },
  { title: '平均值', key: 'avgValue', width: 100 },
  { title: '异常值', key: 'anomaly', width: 80 },
  { title: '分布', key: 'action', width: 60, align: 'center' as const }
]

const snapshotColumns = [
  { title: '快照名称', key: 'snapshotName', ellipsis: true },
  { title: '目标表', key: 'targetTable', width: 160 },
  { title: '创建时间', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, align: 'center' as const }
]

const cronExamplesColumns = [
  { title: '描述', dataIndex: 'desc', width: 200 },
  { title: 'Cron表达式', key: 'cron' }
]
const cronExamples = [
  { desc: '每天凌晨2点', cron: '0 0 2 * * ?' },
  { desc: '每小时整点', cron: '0 0 * * * ?' },
  { desc: '每天中午12点', cron: '0 0 12 * * ?' },
  { desc: '每周一凌晨2点', cron: '0 0 2 ? * MON' },
  { desc: '每月1号凌晨2点', cron: '0 0 2 1 * ?' }
]

// ============ 列选择表格 ============
const selectedTargetColumns = ref<string[]>([])
const targetColumnSelectColumns = [
  { title: '字段名', dataIndex: 'columnName', width: 180, ellipsis: true },
  { title: '数据类型', dataIndex: 'dataType', width: 120 },
  { title: '可空', dataIndex: 'isNullable', width: 70, align: 'center' as const, customRender: ({ text }: any) => text ? '是' : '否' },
  { title: '主键', dataIndex: 'isPrimaryKey', width: 70, align: 'center' as const, customRender: ({ text }: any) => text ? '✓' : '-' },
  { title: '字段说明', dataIndex: 'columnComment', ellipsis: true },
]
const targetColumnRowSelection = computed(() => ({
  selectedRowKeys: selectedTargetColumns.value,
  onChange: (keys: string[]) => {
    selectedTargetColumns.value = keys
    // 同步更新 targetColumns（用逗号分隔的字符串，供后端使用）
    wizardForm.targetColumns = keys.join(',')
  }
}))

async function onWizardTableSelectChange(
  tableName: string,
  opts?: { preserveTargetColumns?: boolean }
) {
  const savedTargetColumns = opts?.preserveTargetColumns ? wizardForm.targetColumns : ''
  wizardForm.targetColumns = ''
  selectedTargetColumns.value = []
  if (!tableName || !wizardForm.targetDsId) return

  const row = metadataList.value.find(m => (m.tableName as string) === tableName)
  if (!row) return

  columnListLoading.value = true
  try {
    const metaId = (row as any).id
    let columns: any[] = []
    if (metaId != null && metaId !== '') {
      const colRes = await metadataApi.getColumns(Number(metaId))
      columns = Array.isArray(colRes.data) ? colRes.data : []
    } else {
      const colRes = await dataSourceApi.getTableColumns(
        wizardForm.targetDsId, tableName, wizardForm.targetSchema || undefined
      )
      const raw = Array.isArray(colRes.data) ? colRes.data : []
      columns = raw.map((c: any) => ({
        columnName: c.columnName,
        dataType: c.dataType,
        columnComment: c.columnComment,
        isNullable: c.nullable,
        isPrimaryKey: c.primaryKey
      }))
    }
    ;(row as any)._columns = columns

    if (savedTargetColumns) {
      const keys = savedTargetColumns.split(',').map(s => s.trim()).filter(Boolean)
      wizardForm.targetColumns = savedTargetColumns
      selectedTargetColumns.value = keys
    }
  } catch {
    ;(row as any)._columns = []
  } finally {
    columnListLoading.value = false
  }
}

// ============ 方法 ============

async function loadTasks() {
  loading.value = true
  try {
    const res = await dprofileApi.pageTasks({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      taskName: filterTaskName.value || undefined,
      triggerType: filterTriggerType.value || undefined,
      profileLevel: filterProfileLevel.value || undefined,
      targetDsId: filterDsId.value || undefined,
      status: filterStatus.value || undefined
    })
    // 拦截器已解包为 Result<Page>，分页字段在 res.data
    if (res.success !== false && res.data) {
      const page = res.data as { records?: ProfileTask[]; total?: number }
      taskData.value = page.records || []
      pagination.total = page.total ?? 0
    }
  } catch {
    // 加载失败时显示空状态
    taskData.value = []
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await dprofileApi.getTaskStats()
    if (res.success !== false && res.data) {
      const s = res.data as TaskStats
      statCards.value[0].value = s.totalTasks ?? 0
      statCards.value[1].value = s.runningTasks ?? 0
      statCards.value[2].value = s.enabledTasks ?? 0
      statCards.value[3].value = s.totalExecutions ?? 0
    }
  } catch { /* ignore */ }
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

function ensureManualTableRow(tableName: string) {
  const t = tableName.trim()
  if (!t) return
  if (metadataList.value.some(m => (m.tableName as string) === t)) return
  const row = {
    id: undefined,
    tableName: t,
    tableAlias: '',
    tableComment: '',
    columnCount: undefined,
    _fromLive: true
  }
  metadataList.value = [...metadataList.value, row]
  filteredMetadataList.value = metadataList.value
}

async function onManualTableNameBlur() {
  const t = wizardForm.targetTable?.trim()
  if (!t || !wizardForm.targetDsId) return
  ensureManualTableRow(t)
  await onWizardTableSelectChange(t)
}

function mergeMetadataAndLiveTables(metaRecords: any[], liveNames: string[]) {
  const byName = new Map<string, any>()
  for (const m of metaRecords) {
    const name = m?.tableName as string
    if (name) byName.set(name, { ...m })
  }
  for (const name of liveNames) {
    if (!name || byName.has(name)) continue
    byName.set(name, {
      id: undefined,
      tableName: name,
      tableAlias: '',
      tableComment: '',
      columnCount: undefined,
      _fromLive: true
    })
  }
  return [...byName.values()].sort((a, b) =>
    (a.tableName || '').localeCompare(b.tableName || '', undefined, { sensitivity: 'base' })
  )
}

async function loadMetadataForDs(dsId: number, schema?: string) {
  if (!dsId) {
    metadataList.value = []
    filteredMetadataList.value = []
    return
  }
  metadataLoading.value = true
  liveTablesLoading.value = true
  try {
    const [pageRes, tablesRes] = await Promise.all([
      metadataApi.page({ pageNum: 1, pageSize: 500, dsId }),
      // PostgreSQL 按 schema 查表，其他类型直接查
      schema
        ? dataSourceApi.getTableListBySchema(dsId, schema).catch(() => ({ data: [] as string[] }))
        : dataSourceApi.getTables(dsId).catch(() => ({ data: [] as string[] }))
    ])

    // 元数据记录（仅当未传 schema 时合并，否则只展示直连的表）
    const pageData = pageRes?.data as { records?: any[] } | undefined
    const metaRecords = Array.isArray(pageData?.records) ? pageData!.records! : []

    let liveNames: string[] = []
    // getTableListBySchema / getTables 经拦截器后均为 Result<string[]>，有效表名在 .data
    const rawTables = tablesRes as any
    if (schema) {
      liveNames = Array.isArray(rawTables?.data)
        ? (rawTables.data as string[])
        : Array.isArray(rawTables)
          ? (rawTables as string[])
          : []
    } else if (rawTables && Array.isArray(rawTables.data)) {
      liveNames = rawTables.data as string[]
    }

    const merged = schema
      ? liveNames.map(name => ({
          id: undefined,
          tableName: name,
          tableAlias: '',
          tableComment: '',
          columnCount: undefined,
          _fromLive: true
        }))
      : mergeMetadataAndLiveTables(metaRecords, liveNames)

    metadataList.value = merged
    filteredMetadataList.value = merged

    if (wizardForm.targetTable) {
      try {
        await onWizardTableSelectChange(wizardForm.targetTable, { preserveTargetColumns: true })
      } catch { /* ignore */ }
    }
  } catch {
    metadataList.value = []
    filteredMetadataList.value = []
  } finally {
    metadataLoading.value = false
    liveTablesLoading.value = false
  }
}

/** PostgreSQL 切换数据源后，先加载 schema 列表 */
async function onWizardDsChange(dsId: number) {
  wizardForm.targetTable = ''
  wizardForm.targetColumns = ''
  wizardForm.targetSchema = ''
  selectedTargetColumns.value = []
  wizardForm.taskCode = ''
  taskNameManuallyEdited.value = false
  columnListLoading.value = false
  schemaList.value = []
  schemaLoading.value = false
  metadataList.value = []
  filteredMetadataList.value = []

  if (!dsId) return

  const ds = dataSourceList.value.find(d => d.id === dsId)
  const dsType = ds?.dsType?.toUpperCase()

  if (dsType === 'POSTGRESQL') {
    schemaLoading.value = true
    try {
      const res = await dataSourceApi.getSchemas(dsId)
      schemaList.value = Array.isArray(res?.data) ? res.data as string[] : []
      schemaLoading.value = false
    } catch {
      schemaList.value = []
      schemaLoading.value = false
    }
  } else {
    await loadMetadataForDs(dsId)
  }
}

function onSchemaChange(schema: string) {
  wizardForm.targetSchema = schema
  wizardForm.targetTable = ''
  wizardForm.targetColumns = ''
  selectedTargetColumns.value = []
  columnListLoading.value = false
  loadMetadataForDs(wizardForm.targetDsId!, schema)
}

function onWizardTableSearch(value: string) {
  if (!value) {
    filteredMetadataList.value = metadataList.value
    return
  }
  const lower = value.toLowerCase()
  filteredMetadataList.value = metadataList.value.filter(t =>
    (t.tableName as string).toLowerCase().includes(lower) ||
    ((t.tableAlias as string) || '').toLowerCase().includes(lower)
  )
}

function selectAllColumns() {
  const cols = selectedTableColumns.value
  if (cols.length > 0) {
    selectedTargetColumns.value = cols.map((c: any) => c.columnName)
    wizardForm.targetColumns = selectedTargetColumns.value.join(',')
  }
}

function clearSelectedColumns() {
  selectedTargetColumns.value = []
  wizardForm.targetColumns = ''
}

function getDsTypeColor(dsType: string) {
  const map: Record<string, string> = {
    MYSQL: 'blue', SQLSERVER: 'red', ORACLE: 'orange', POSTGRESQL: 'cyan', TIDB: 'green'
  }
  return map[dsType?.toUpperCase()] || 'default'
}

function getDsName(dsId: number | undefined) {
  if (!dsId) return '-'
  const ds = dataSourceList.value.find(d => d.id === dsId)
  return ds?.dsName || '-'
}

async function nextStep() {
  if (currentStep.value === 0) {
    try {
      await step2FormRef.value?.validate()
    } catch { return }
  } else if (currentStep.value === 1) {
    try {
      await step1FormRef.value?.validate()
    } catch { return }
  }
  currentStep.value++
}

function openCreateWizard() {
  isEditMode.value = false
  currentStep.value = 0
  Object.assign(wizardForm, {
    taskName: '', taskCode: '', taskDesc: '',
    targetDsId: undefined, targetTable: '', targetColumns: '',
    targetSchema: '',
    profileLevel: 'DETAILED', triggerType: 'MANUAL',
    triggerCron: '', timeoutMinutes: 60
  })
  metadataList.value = []
  filteredMetadataList.value = []
  selectedTargetColumns.value = []
  taskNameManuallyEdited.value = false
  columnListLoading.value = false
  schemaList.value = []
  schemaLoading.value = false
  wizardVisible.value = true
}

async function openEditWizard(record: ProfileTask) {
  isEditMode.value = true
  currentStep.value = 0
  taskNameManuallyEdited.value = true // 编辑模式视为用户已手动编辑名称
  wizardFormSnapshot.value = { ...wizardForm }
  Object.assign(wizardForm, {
    taskName: record.taskName || '',
    taskCode: record.taskCode || '',
    taskDesc: record.taskDesc || '',
    targetDsId: record.targetDsId,
    targetTable: record.targetTable || '',
    targetColumns: record.targetColumns || '',
    targetSchema: (record as any).targetSchema || '',
    profileLevel: record.profileLevel || 'DETAILED',
    triggerType: record.triggerType || 'MANUAL',
    triggerCron: record.triggerCron || '',
    timeoutMinutes: record.timeoutMinutes || 60
  })
  // 初始化已选列（编辑模式下回显）
  if (wizardForm.targetColumns) {
    selectedTargetColumns.value = wizardForm.targetColumns.split(',').map(s => s.trim()).filter(Boolean)
  } else {
    selectedTargetColumns.value = []
  }
  if (record.targetDsId) {
    await loadMetadataForDs(record.targetDsId, wizardForm.targetSchema || undefined)
  }
  wizardVisible.value = true
}

async function handleFinalSubmit() {
  saving.value = true
  try {
    const payload = {
      ...wizardForm,
      taskCode: wizardForm.taskCode || autoGeneratedCode.value,
      timeoutMinutes: wizardForm.timeoutMinutes || 60
    }
    let success = false
    if (isEditMode.value && currentTask.value?.id) {
      const res = await dprofileApi.updateTask(currentTask.value.id, payload)
      success = res.success !== false
    } else {
      const res = await dprofileApi.createTask(payload)
      success = res.success !== false
      if (success && executeAfterCreate.value && res.data != null) {
        const newId = res.data as number
        setTimeout(async () => {
          await handleExecuteById(newId)
        }, 300)
      }
    }

    if (success) {
      message.success(isEditMode.value ? '保存成功' : '创建成功')
      wizardVisible.value = false
      loadTasks()
      loadStats()
    }
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleExecuteById(taskId: number) {
  const task = taskData.value.find(t => t.id === taskId)
  if (task) await handleExecute(task)
}

// 探查阶段 → 弹窗步骤文字映射
const phaseToStep: Record<string, string> = {
  PENDING: 'init',
  TABLE_STATS: 'table',
  COLUMN_STATS: 'column',
  DISTRIBUTION: 'distribution',
  ANOMALY: 'anomaly',
  COMPLETED: 'finalize',
  SUCCESS: 'finalize',
  FAILED: 'finalize'
}

const phaseToMessage: Record<string, string> = {
  PENDING: '正在初始化连接...',
  TABLE_STATS: '正在获取表级统计信息（行数、列数、存储大小）...',
  COLUMN_STATS: '正在分析列级数据分布...',
  DISTRIBUTION: '正在计算分布直方图和高频值...',
  ANOMALY: '正在执行异常值检测（3σ / IQR 法则）...',
  COMPLETED: '正在汇总探查结果...',
  SUCCESS: '探查已完成',
  FAILED: '探查执行失败'
}

async function handleExecute(record: ProfileTask) {
  if (pollingTimer.value) {
    clearTimeout(pollingTimer.value)
    pollingTimer.value = null
  }
  currentTask.value = record
  executeModalVisible.value = true
  executeDone.value = false
  executeProgress.value = 0
  executeStep.value = 'init'
  executeStats.value = null
  executeSuccess.value = false
  executeError.value = ''

  try {
    const res = await dprofileApi.executeTask(record.id!)
    // request 拦截器已解包为 Result<T>：data 即为 executionId
    if (res.success !== false && res.data != null) {
      const executionId = res.data as number
      await pollForExecution(record, executionId)
    } else {
      executeSuccess.value = false
      executeError.value = res.message || '执行失败'
      executeDone.value = true
    }
  } catch (e: any) {
    executeSuccess.value = false
    executeError.value = e.message || '未知错误'
    executeDone.value = true
  }
}

// 轮询执行记录，获取真实进度
async function pollForExecution(record: ProfileTask, executionId: number) {
  const maxAttempts = 60
  let attempts = 0

  const poll = async () => {
    attempts++
    try {
      // 1. 获取执行进度（实时状态）
      const recRes = await dprofileApi.getExecutionRecord(executionId)
      if (recRes.success !== false && recRes.data) {
        const rec = recRes.data as ProfileExecutionRecord
        // 实时更新弹窗进度条
        executeProgress.value = rec.progress ?? Math.min(attempts * 10, 90)
        executeStep.value = phaseToStep[rec.phase || ''] || 'init'

        if (rec.status === 'SUCCESS' || rec.status === 'COMPLETED') {
          executeProgress.value = 100
          executeStep.value = 'finalize'
          // 2. 任务成功，查询表级统计结果
          await pollForResults(record, executionId)
          return
        }
        if (rec.status === 'FAILED') {
          executeSuccess.value = false
          executeError.value = rec.errorMessage || '探查执行失败'
          executeDone.value = true
          return
        }
      }

      // 3. 还在进行中，继续轮询
      if (attempts < maxAttempts) {
        pollingTimer.value = window.setTimeout(poll, 1000)
      } else {
        executeSuccess.value = false
        executeError.value = '探查执行超时，请稍后查看探查报告'
        executeDone.value = true
      }
    } catch {
      if (attempts < maxAttempts) {
        pollingTimer.value = window.setTimeout(poll, 1000)
      } else {
        executeSuccess.value = false
        executeError.value = '获取探查进度超时'
        executeDone.value = true
      }
    }
  }

  // 立即开始轮询
  pollingTimer.value = window.setTimeout(poll, 500)
}

// 轮询探查结果（执行成功后才进入此函数）
async function pollForResults(record: ProfileTask, executionId: number) {
  const maxAttempts = 15
  let attempts = 0

  const poll = async () => {
    attempts++
    try {
      const res = await dprofileApi.listTableStats({
        dsId: record.targetDsId,
        tableName: record.targetTable,
        executionId,
        limit: 1
      })

      if (res.success !== false && Array.isArray(res.data) && res.data.length > 0) {
        const stats = res.data[0]
        executeSuccess.value = true
        resultStats.value = stats
        executeStats.value = {
          rowCount: formatNumber(stats.rowCount),
          columnCount: (stats.columnCount ?? 0).toString(),
          elapsed: '已完成'
        }
        executeDone.value = true
        loadTasks()
        loadStats()
        return
      }

      if (attempts < maxAttempts) {
        pollingTimer.value = window.setTimeout(poll, 1000)
      } else {
        // 有进度但查不到结果，仍算成功
        executeSuccess.value = true
        executeError.value = '已完成但未获取到统计结果，请查看探查报告'
        executeDone.value = true
      }
    } catch {
      if (attempts < maxAttempts) {
        pollingTimer.value = window.setTimeout(poll, 1000)
      } else {
        executeSuccess.value = false
        executeError.value = '获取探查结果超时'
        executeDone.value = true
      }
    }
  }

  pollingTimer.value = window.setTimeout(poll, 800)
}

async function handleCancelExecute() {
  if (pollingTimer.value) {
    clearTimeout(pollingTimer.value)
    pollingTimer.value = null
  }
  executeModalVisible.value = false
}

async function handleToggle(record: ProfileTask) {
  const enabled = !isTaskEnabled(record.status)
  try {
    const res = await dprofileApi.toggleTask(record.id!, enabled)
    if (res.success !== false) {
      message.success(enabled ? '已启用' : '已禁用')
      loadTasks()
      loadStats()
    }
  } catch { message.error('操作失败') }
}

async function handleDelete(record: ProfileTask) {
  try {
    const res = await dprofileApi.deleteTask(record.id!)
    if (res.success !== false) {
      message.success('删除成功')
      loadTasks()
      loadStats()
    }
  } catch { message.error('删除失败') }
}

function resetFilters() {
  filterTaskName.value = ''
  filterTriggerType.value = undefined
  filterProfileLevel.value = undefined
  filterDsId.value = undefined
  filterStatus.value = undefined
  pagination.current = 1
  loadTasks()
}

const handleTableChange: any = (p: any) => {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  loadTasks()
}

// ============ 报告方法 ============
async function openReportDrawer(record: ProfileTask) {
  currentTask.value = record
  reportDrawerVisible.value = true
  distributionVisible.value = false
  allTopValuesData.value = []
  anomalyColumns.value = []

  try {
    const res = await dprofileApi.listTableStats({
      dsId: record.targetDsId,
      tableName: record.targetTable,
      limit: 1
    })
    if (res.success !== false && Array.isArray(res.data) && res.data.length > 0) {
      reportTableStats.value = res.data[0]

      const colRes = await dprofileApi.listColumnStats(reportTableStats.value.id!)
      if (colRes.success !== false) {
        reportColumnStats.value = Array.isArray(colRes.data) ? colRes.data : []

        // 计算异常列
        anomalyColumns.value = reportColumnStats.value
          .filter(c => {
            const rate = c.nullRate || 0
            const isOutlier = c.outlierCount && c.outlierCount > 0
            const uniqueOnly = c.uniqueCount === 1 && (c.totalCount || 0) > 100
            return rate > 0.5 || isOutlier || uniqueOnly
          })
          .map(c => {
            let anomalyType = ''
            let anomalyValue = ''
            let anomalyDetail = ''
            const rate = c.nullRate || 0
            if (rate > 0.5) {
              anomalyType = '高空值率'
              anomalyValue = (rate * 100).toFixed(1) + '%'
              anomalyDetail = '空值率超过50%，建议检查数据源'
            } else if (c.outlierCount && c.outlierCount > 0) {
              anomalyType = '异常值'
              anomalyValue = c.outlierCount.toString() + '个'
              anomalyDetail = `使用${(c as any).outlierMethod || 'IQR'}法则检测`
            } else if (c.uniqueCount === 1 && (c.totalCount || 0) > 100) {
              anomalyType = '全唯一'
              anomalyValue = '1'
              anomalyDetail = '该字段可能不适合作为分析维度'
            }
            return { columnName: c.columnName, dataType: c.dataType, anomalyType, anomalyValue, anomalyDetail }
          })

        // 解析所有列的 Top 值
        reportColumnStats.value.forEach(col => {
          if (col.topValues) {
            try {
              const tv = JSON.parse(col.topValues)
              const total = tv.reduce((s: number, i: any) => s + (i.count || 0), 0)
              allTopValuesData.value.push({
                columnName: col.columnName || '',
                dataType: col.dataType || '',
                values: tv.map((item: any) => ({
                  value: item.value,
                  count: item.count || 0,
                  percent: total > 0 ? ((item.count / total) * 100).toFixed(1) : '0'
                }))
              })
            } catch { /* ignore */ }
          }
        })
      }
    } else {
      reportTableStats.value = null
      reportColumnStats.value = []
    }
  } catch {
    reportTableStats.value = null
    reportColumnStats.value = []
  }
}

function getNullRateColor(rate: number | undefined) {
  const r = rate || 0
  if (r > 0.5) return '#FF4D4F'
  if (r > 0.2) return '#FAAD14'
  return '#52C41A'
}

function showColumnDistribution(record: ColumnStats) {
  distributionColumn.value = record.columnName || ''
  distributionDataType.value = record.dataType || ''

  let histData: any[] = []
  let topData: any[] = []

  if (record.histogram) {
    try {
      histData = JSON.parse(record.histogram)
    } catch { histData = [] }
  }

  if (record.topValues) {
    try {
      topData = JSON.parse(record.topValues)
    } catch { topData = [] }
  }

  const total = topData.reduce((s: number, i: any) => s + (i.count || 0), 0)
  topValuesData.value = topData.map((item: any) => ({
    value: item.value,
    count: item.count || 0,
    percent: total > 0 ? ((item.count / total) * 100).toFixed(1) : '0'
  }))

  isNumericDistribution.value = isNumericType(record.dataType)

  // 统计摘要
  currentDistributionStats.value = {
    totalCount: record.totalCount || 0,
    nonNullCount: (record.totalCount || 0) - (record.nullCount || 0),
    min: record.minValue,
    max: record.maxValue,
    avg: record.avgValue,
    median: record.medianValue,
    stdDev: record.stdDev,
    outlierCount: record.outlierCount || 0
  }

  distributionTab.value = isNumericDistribution.value && histData.length > 0 ? 'histogram' : 'frequency'

  nextTick(() => {
    renderDistributionChart(histData, topData)
  })

  distributionVisible.value = true
}

function renderDistributionChart(histData: any[], topData: any[]) {
  const el = chartRefs.main
  if (!el) return

  const chart = echarts.getInstanceByDom(el)
  if (chart) chart.dispose()

  const chartInstance = echarts.init(el)
  const isNumeric = isNumericDistribution.value

  if (distributionTab.value === 'histogram' && isNumeric && histData.length > 0) {
    const xData = histData.map((d: any) => d.range || '')
    const yData = histData.map((d: any) => d.count || 0)
    chartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const p = params[0]
          const total = yData.reduce((s: number, v: number) => s + v, 0)
          return `${distributionColumn.value}<br/>${p.name}: <b>${p.value.toLocaleString()}</b> (${total > 0 ? ((p.value / total) * 100).toFixed(1) : 0}%)`
        }
      },
      grid: { left: 60, right: 30, top: 20, bottom: 50 },
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
        barRadius: [4, 4, 0, 0],
        barMaxWidth: 60,
        emphasis: { itemStyle: { opacity: 0.8 } }
      }]
    })
  } else if (distributionTab.value === 'frequency' && topData.length > 0) {
    const colorList = ['#722ED1', '#9254DE', '#B37FEB', '#D3ADF7', '#E8D5FF', '#13C2C2', '#52C41A', '#FAAD14', '#FF4D4F', '#1677FF']
    const sorted = [...topData].sort((a: any, b: any) => (b.count || 0) - (a.count || 0)).slice(0, 10)
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
        label: { show: true, position: 'right', fontSize: 10, formatter: (p: any) => `${((p.value / (sorted.reduce((s: number, i: any) => s + (i.count || 0), 0) || 1)) * 100).toFixed(1)}%` }
      }]
    })
  } else {
    // 饼图
    const colorList = ['#722ED1', '#9254DE', '#B37FEB', '#D3ADF7', '#E8D5FF', '#13C2C2', '#52C41A', '#FAAD14', '#FF4D4F', '#1677FF']
    const sorted = [...topData].sort((a: any, b: any) => (b.count || 0) - (a.count || 0)).slice(0, 8)
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

function isNumericType(dataType: string | undefined) {
  if (!dataType) return false
  const dt = dataType.toLowerCase()
  return dt.includes('int') || dt.includes('decimal') || dt.includes('numeric')
    || dt.includes('float') || dt.includes('double') || dt.includes('real')
    || dt.includes('number') || dt.includes('smallmoney')
    || dt.includes('money') || dt.includes('bit')
}

async function handleReExecute() {
  if (!currentTask.value) return
  reportDrawerVisible.value = false
  await handleExecute(currentTask.value)
}

function openSnapshotDialog() {
  snapshotForm.snapshotName = `快照_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}`
  snapshotForm.snapshotDesc = ''
  snapshotDialogVisible.value = true
}

async function handleCreateSnapshot() {
  if (!currentTask.value?.id) return
  snapshotSaving.value = true
  try {
    const res = await dprofileApi.createSnapshot({
      snapshotName: snapshotForm.snapshotName,
      snapshotDesc: snapshotForm.snapshotDesc,
      targetDsId: currentTask.value.targetDsId,
      targetTable: currentTask.value.targetTable
    })
    if (res.success !== false) {
      message.success('快照创建成功')
      snapshotDialogVisible.value = false
    }
  } catch { message.error('创建失败') }
  finally { snapshotSaving.value = false }
}

function openSnapshotDrawer() {
  snapshotDsId.value = undefined
  snapshotTable.value = ''
  snapshotData.value = []
  snapshotDrawerVisible.value = true
  loadSnapshots()
}

async function loadSnapshots() {
  snapshotLoading.value = true
  try {
    const res = await dprofileApi.listSnapshots({
      targetDsId: snapshotDsId.value || undefined,
      targetTable: snapshotTable.value || undefined
    })
    if (res.success !== false) {
      snapshotData.value = Array.isArray(res.data) ? res.data : []
    }
  } catch { snapshotData.value = [] }
  finally { snapshotLoading.value = false }
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

// ============ 辅助方法 ============

function getProfileLevelColor(level: string) {
  const map: Record<string, string> = { BASIC: 'blue', DETAILED: 'purple', FULL: 'orange' }
  return map[level] || 'default'
}

function getProfileLevelLabel(level: string | undefined) {
  const map: Record<string, string> = { BASIC: '仅表级', DETAILED: '表+列级', FULL: '完整探查' }
  return map[level || ''] || level || '-'
}

function getTriggerColor(type: string) {
  const map: Record<string, string> = { MANUAL: 'green', SCHEDULE: 'blue', EVENT: 'orange' }
  return map[type] || 'default'
}

function getTriggerLabel(type: string) {
  const map: Record<string, string> = { MANUAL: '手动', SCHEDULE: '定时', EVENT: '事件' }
  return map[type] || type || '-'
}

function isTaskEnabled(status: any) {
  return status === 'PUBLISHED' || status === 1 || status === true
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
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

onMounted(() => {
  loadTasks()
  loadStats()
  loadDataSources()
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

.filter-bar { margin-bottom: 16px; }

/* 空状态 */
.empty-state {
  background: white; border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: 60px 40px; text-align: center;
}
.empty-illustration { margin-bottom: 20px; }
.empty-title { font-size: 18px; font-weight: 600; color: #1F1F1F; margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: #8C8C8C; margin-bottom: 24px; line-height: 1.8; }
.empty-actions { display: flex; gap: 12px; justify-content: center; }

/* 表格 */
.table-card {
  background: white; border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  overflow: hidden;
}
.task-name-cell {
  .task-name { font-weight: 500; }
  .task-code { font-size: 12px; color: #8C8C8C; font-family: 'JetBrains Mono', monospace; }
}
.table-name-cell { display: flex; align-items: center; gap: 6px; font-family: 'JetBrains Mono', monospace; }
.table-icon { color: #722ED1; }
.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px; }
.truncate { display: inline-block; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.time-text { font-size: 13px; }
.time-empty { color: #8C8C8C; font-size: 13px; }

/* ============================================================ */
/* 向导样式 */
/* ============================================================ */
.wizard-container { padding: 0 8px 8px; }

/* 步骤指示器 */
.wizard-steps {
  display: flex; align-items: flex-start;
  margin-bottom: 28px; position: relative;
}
.wizard-step {
  display: flex; align-items: flex-start; gap: 12px; flex: 1;
  position: relative;
}
.step-indicator {
  width: 32px; height: 32px; border-radius: 50%;
  background: #E8E8E8; color: #8C8C8C;
  display: flex; align-items: center; justify-content: center;
  font-weight: 600; font-size: 14px; flex-shrink: 0;
  transition: all 0.3s;
}
.step-active .step-indicator {
  background: linear-gradient(135deg, #722ED1, #9254DE);
  color: white; box-shadow: 0 4px 12px rgba(114, 46, 209, 0.3);
}
.step-done .step-indicator {
  background: #52C41A; color: white;
}
.step-info { flex: 1; padding-top: 4px; }
.step-title { font-size: 14px; font-weight: 600; color: #1F1F1F; }
.step-desc { font-size: 12px; color: #8C8C8C; margin-top: 2px; }
.step-active .step-title { color: #722ED1; }
.step-line {
  position: absolute; top: 16px; left: calc(32px + 12px);
  right: 0; height: 1px; background: #E8E8E8;
  z-index: 0;
}
.step-done + .wizard-step .step-line,
.step-done .step-line { background: #52C41A; }

/* 步骤内容 */
.wizard-content {
  min-height: 360px; background: #FAFAFA;
  border-radius: 8px; padding: 24px;
  border: 1px solid #F0F0F0;
}
.step-panel { animation: fadeIn 0.2s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.step-panel-header { margin-bottom: 20px; }
.panel-title { font-size: 16px; font-weight: 600; color: #1F1F1F; }
.panel-desc { font-size: 13px; color: #8C8C8C; margin-top: 4px; }

.form-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; }
.form-tip-warning { color: #FAAD14; }
.tip-list { margin: 0; padding-left: 20px; line-height: 2; font-size: 13px; color: #666; }
code { background: #F5F7FA; padding: 1px 6px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; font-size: 12px; }

/* 数据源选项 */
.ds-option { display: flex; align-items: center; gap: 8px; }
.ds-name { font-weight: 500; }

/* 表选项 */
.table-option { display: flex; align-items: center; gap: 6px; }
.table-alias { font-size: 12px; color: #8C8C8C; margin-left: 4px; }
.column-count { font-size: 11px; color: #8C8C8C; margin-left: auto; }
.table-comment { font-size: 12px; color: #8C8C8C; font-style: italic; }

/* 探查级别选择器 */
.profile-level-selector { display: flex; flex-direction: column; gap: 12px; }
.level-card {
  display: flex; align-items: flex-start; gap: 12px;
  border: 2px solid #F0F0F0; border-radius: 8px; padding: 14px;
  cursor: pointer; transition: all 0.2s; background: white;
}
.level-card:hover { border-color: #D3ADF7; box-shadow: 0 2px 8px rgba(114, 46, 209, 0.08); }
.level-selected { border-color: #722ED1; background: linear-gradient(135deg, rgba(114,46,209,0.03) 0%, rgba(146,84,222,0.05) 100%); }
.level-icon { font-size: 24px; color: #722ED1; }
.level-content { flex: 1; }
.level-name { font-weight: 600; font-size: 14px; color: #1F1F1F; }
.level-desc { font-size: 12px; color: #8C8C8C; margin-top: 2px; }
.level-items { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
.level-check { flex-shrink: 0; }

/* 触发类型选择器 */
.trigger-type-selector { display: flex; gap: 12px; }
.trigger-card {
  display: flex; align-items: center; gap: 12px;
  border: 2px solid #F0F0F0; border-radius: 8px; padding: 12px 16px;
  cursor: pointer; transition: all 0.2s; flex: 1; background: white;
}
.trigger-card:hover { border-color: #D3ADF7; }
.trigger-selected { border-color: #722ED1; background: linear-gradient(135deg, rgba(114,46,209,0.03) 0%, rgba(146,84,222,0.05) 100%); }
.trigger-icon-wrapper { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.trigger-content { flex: 1; }
.trigger-name { font-weight: 600; font-size: 13px; }
.trigger-desc { font-size: 11px; color: #8C8C8C; margin-top: 2px; }

/* Cron 助手 */
.cron-helper { margin-top: 8px; }
.cron-presets { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.cron-preset-tag { cursor: pointer; transition: all 0.2s; }
.cron-preset-tag:hover { border-color: #722ED1; color: #722ED1; }
.preset-active { background: #722ED1 !important; border-color: #722ED1 !important; color: white !important; }

/* 确认摘要 */
.confirm-summary { margin-bottom: 16px; }

/* 执行后复选框 */
.execute-after-checkbox { margin-top: 12px; }
.checkbox-desc { font-size: 12px; color: #8C8C8C; display: block; }

/* 向导底部 */
.wizard-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 16px; margin-top: 8px;
}

/* ============================================================ */
/* 执行进度 */
/* ============================================================ */
.execute-progress { padding: 8px 0; }
.execute-tip { margin-top: 16px; color: #8C8C8C; text-align: center; }
.execute-stats { margin-top: 16px; }
.exec-stat { text-align: center; }
.exec-stat-val { font-size: 20px; font-weight: 700; color: #722ED1; font-family: 'JetBrains Mono', monospace; }
.exec-stat-label { font-size: 12px; color: #8C8C8C; margin-top: 2px; }
.execute-footer { text-align: center; margin-top: 16px; }
.result-actions { margin-top: 16px; text-align: center; display: flex; gap: 12px; justify-content: center; }

/* ============================================================ */
/* 报告抽屉 */
/* ============================================================ */
.report-content { padding-bottom: 60px; }
.report-card { margin-bottom: 16px; border-radius: 8px; }
.rate-cell { display: flex; align-items: center; }

/* 异常检测 */
.anomaly-card { border-left: 3px solid #FAAD14; }
.anomaly-list { display: flex; flex-direction: column; gap: 10px; }
.anomaly-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; background: #FFF7E6; border-radius: 6px;
}
.anomaly-left { display: flex; align-items: center; gap: 8px; }
.anomaly-type { font-size: 13px; color: #FAAD14; font-weight: 500; }
.anomaly-right { text-align: right; }
.anomaly-value { font-weight: 600; color: #FAAD14; font-family: 'JetBrains Mono', monospace; }
.anomaly-detail { display: block; font-size: 11px; color: #8C8C8C; }
.anomaly-more { font-size: 12px; color: #8C8C8C; text-align: center; padding: 8px; }

/* 分布可视化 */
.chart-section { }
.chart-tabs { margin-bottom: 12px; }
.top-values-section { }
.section-label { font-size: 13px; font-weight: 600; color: #1F1F1F; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #F0F0F0; }
.top-values-list { display: flex; flex-direction: column; gap: 8px; }
.top-value-item { display: flex; align-items: center; gap: 8px; }
.top-rank { width: 18px; height: 18px; border-radius: 50%; background: #722ED1; color: white; font-size: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.top-value-bar-wrapper { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.top-value-name { font-size: 12px; font-family: 'JetBrains Mono', monospace; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 120px; }
.top-value-bar-bg { height: 6px; background: #F0F0F0; border-radius: 3px; overflow: hidden; }
.top-value-bar-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease; }
.top-value-count { font-size: 11px; color: #8C8C8C; display: flex; gap: 4px; }
.top-percent { }
.stats-summary { }
.stats-summary .section-label { }

/* Top 值汇总 */
.top-values-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.col-top-values { background: #FAFAFA; border-radius: 8px; padding: 12px; }
.col-top-title { font-size: 13px; font-weight: 600; margin-bottom: 10px; display: flex; align-items: center; gap: 6px; }
.col-top-bars { display: flex; flex-direction: column; gap: 6px; }
.col-top-item { display: flex; align-items: center; gap: 8px; }
.col-top-val { font-size: 11px; font-family: 'JetBrains Mono', monospace; min-width: 60px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-top-bar-bg { flex: 1; height: 4px; background: #E8E8E8; border-radius: 2px; overflow: hidden; }
.col-top-bar-fill { height: 100%; background: linear-gradient(90deg, #722ED1, #9254DE); border-radius: 2px; }
.col-top-pct { font-size: 11px; color: #8C8C8C; min-width: 40px; text-align: right; }

/* ============================================================ */
/* 快照管理 */
/* ============================================================ */
.snapshot-filter { display: flex; gap: 12px; margin-bottom: 16px; }
.snapshot-empty { text-align: center; padding: 60px 20px; color: #8C8C8C; font-size: 14px; }
.drawer-footer {
  position: absolute; bottom: 0; left: 0; right: 0;
  padding: 16px 24px; border-top: 1px solid #f0f0f0;
  background: white; display: flex; justify-content: flex-end;
}

/* ============================================================ */
/* 配置指南 */
/* ============================================================ */
.guide-content { padding: 8px 0; }
.guide-content code { background: #F5F7FA; padding: 1px 6px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; font-size: 12px; }
.guide-content ul { padding-left: 24px; }
.guide-content li { margin-bottom: 6px; }
</style>
