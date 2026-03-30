<template>
  <div class="page-container">
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
          <h1 class="page-title">数据安全-分类分级标准管理</h1>
          <p class="page-subtitle">建立数据分类分级体系，自动识别敏感字段，支持灵活的数据脱敏策略配置</p>
        </div>
      </div>
      <div class="header-right">
        <a-button type="primary" @click="openScanDrawer" style="margin-right: 8px">
          <template #icon><ScanOutlined /></template>
          扫描敏感字段
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 16px">
      <a-col :xs="12" :sm="8" :md="6" :lg="4" v-for="stat in statCards" :key="stat.label">
        <div class="stat-mini-card" :style="{ background: stat.bg }">
          <div class="mini-value">{{ stat.value }}</div>
          <div class="mini-label">{{ stat.label }}</div>
        </div>
      </a-col>
    </a-row>

    <!-- 主内容区：Tab切换 -->
    <div class="table-card">
      <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
        <!-- ==================== 数据分类 ==================== -->
        <a-tab-pane key="classification" tab="数据分类">
          <!-- 筛选 -->
          <div class="filter-bar">
            <a-space wrap>
              <a-input
                v-model:value="classSearch"
                placeholder="分类名称/编码"
                style="width: 200px"
                allowClear
                @pressEnter="loadClassification"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
              <a-select v-model:value="classEnabled" placeholder="是否启用" style="width: 110px" allowClear @change="loadClassification">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
              <a-button @click="resetClassFilters">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
              <a-button type="primary" @click="openClassDrawer('add')">
                <template #icon><PlusOutlined /></template>
                新增分类
              </a-button>
            </a-space>
          </div>

          <!-- 数据分类表格 -->
          <a-table
            :columns="classColumns"
            :data-source="classData"
            :loading="classLoading"
            :pagination="classPagination"
            :row-key="(r: SecClassificationVO) => r.id"
            @change="handleClassTableChange"
            :scroll="{ x: 1000 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'className'">
                <div>
                  <div style="font-weight: 500">{{ record.className }}</div>
                  <div style="color: #888; font-size: 12px">{{ record.classCode }}</div>
                </div>
              </template>
              <template v-if="column.key === 'sensitivityLevel'">
                <a-tag :color="getLevelColor(record.sensitivityLevel)">
                  {{ record.sensitivityLevelLabel || record.sensitivityLevel }}
                </a-tag>
              </template>
              <template v-if="column.key === 'enabled'">
                <a-switch :checked="record.status === 1" size="small" @change="(c: boolean) => toggleClass(record, c)" />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-tooltip title="编辑"><a-button type="text" size="small" @click="openClassDrawer('edit', record)"><EditOutlined /></a-button></a-tooltip>
                  <a-popconfirm title="确定删除该分类？" @confirm="deleteClass(record.id!)">
                    <a-button type="text" size="small" danger><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
            <template #emptyText>
              <div class="empty-state">
                <SafetyOutlined class="empty-icon" />
                <div class="empty-title">暂无数据分类</div>
                <div class="empty-desc">建立数据分类体系，为敏感字段识别提供分类依据</div>
                <a-button type="primary" @click="openClassDrawer('add')">新增分类</a-button>
              </div>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- ==================== 敏感等级 ==================== -->
        <a-tab-pane key="level" tab="敏感等级">
          <div class="filter-bar">
            <a-space>
              <a-button type="primary" @click="openLevelDrawer('add')">
                <template #icon><PlusOutlined /></template>
                新增等级
              </a-button>
            </a-space>
          </div>
          <a-table
            :columns="levelColumns"
            :data-source="levelData"
            :loading="levelLoading"
            :pagination="levelPagination"
            :row-key="(r: SecLevelVO) => r.id"
            @change="handleLevelTableChange"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'levelValue'">
                <a-tag :color="record.color" style="font-size: 16px; font-weight: bold; border-radius: 50%; width: 32px; height: 32px; display: inline-flex; align-items: center; justify-content: center;">
                  {{ record.levelValue }}
                </a-tag>
              </template>
              <template v-if="column.key === 'levelName'">
                <div>
                  <span :style="{ color: record.color, fontWeight: 600 }">{{ record.levelName }}</span>
                  <div style="color: #888; font-size: 12px">{{ record.levelCode }}</div>
                </div>
              </template>
              <template v-if="column.key === 'levelDesc'">
                <a-tooltip :title="record.levelDesc">
                  <span style="color: #888">{{ record.levelDesc || '—' }}</span>
                </a-tooltip>
              </template>
              <template v-if="column.key === 'sensitiveFieldCount'">
                <span style="color: #1677FF; font-weight: 500">{{ record.sensitiveFieldCount ?? 0 }}</span>
              </template>
              <template v-if="column.key === 'createTime'">
                <span style="color: #888; font-size: 13px">{{ record.createTime || '—' }}</span>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-tooltip title="编辑"><a-button type="text" size="small" @click="openLevelDrawer('edit', record)"><EditOutlined /></a-button></a-tooltip>
                  <a-popconfirm title="确定删除该等级？" @confirm="deleteLevel(record.id!)">
                    <a-button type="text" size="small" danger><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- ==================== 识别规则 ==================== -->
        <a-tab-pane key="rule" tab="识别规则">
          <div class="filter-bar">
            <a-space wrap>
              <a-select v-model:value="ruleMatchType" placeholder="匹配类型" style="width: 140px" allowClear @change="loadRule">
                <a-select-option value="COLUMN_NAME">字段名匹配</a-select-option>
                <a-select-option value="COLUMN_COMMENT">注释匹配</a-select-option>
                <a-select-option value="DATA_TYPE">数据类型</a-select-option>
                <a-select-option value="REGEX">正则表达式</a-select-option>
              </a-select>
              <a-select v-model:value="ruleEnabled" placeholder="是否启用" style="width: 110px" allowClear @change="loadRule">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
              <a-button @click="resetRuleFilters">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
              <a-button type="primary" @click="openRuleDrawer('add')">
                <template #icon><PlusOutlined /></template>
                新增规则
              </a-button>
            </a-space>
          </div>
          <a-table
            :columns="ruleColumns"
            :data-source="ruleData"
            :loading="ruleLoading"
            :pagination="rulePagination"
            :row-key="(r: SecSensitivityRuleVO) => r.id"
            @change="handleRuleTableChange"
            :scroll="{ x: 1200 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'ruleName'">
                <div>
                  <div style="font-weight: 500">{{ record.ruleName }}</div>
                  <div style="color: #888; font-size: 12px">{{ record.description }}</div>
                </div>
              </template>
              <template v-if="column.key === 'matchType'">
                <a-tag :color="getMatchTypeColor(record.matchType)">
                  {{ record.matchExprTypeLabel || '' }} {{ record.matchTypeLabel || record.matchType }}
                </a-tag>
              </template>
              <template v-if="column.key === 'matchExpr'">
                <a-tooltip :title="record.matchExpr">
                  <code style="background: #f5f5f5; padding: 2px 6px; border-radius: 4px; font-size: 12px">
                    {{ record.matchExpr?.substring(0, 40) }}{{ (record.matchExpr?.length || 0) > 40 ? '...' : '' }}
                  </code>
                </a-tooltip>
              </template>
              <template v-if="column.key === 'defaultLevel'">
                <a-tag :color="record.levelColor">{{ record.levelName || '-' }}</a-tag>
              </template>
              <template v-if="column.key === 'builtin'">
                <a-tag :color="record.builtin === 1 ? 'blue' : 'default'">{{ record.builtinLabel }}</a-tag>
              </template>
              <template v-if="column.key === 'enabled'">
                <a-switch :checked="record.status === 1" size="small" @change="(c: boolean) => toggleRule(record, c)" />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-tooltip title="编辑"><a-button type="text" size="small" @click="openRuleDrawer('edit', record)"><EditOutlined /></a-button></a-tooltip>
                  <a-popconfirm title="确定删除该规则？" @confirm="deleteRule(record.id!)" :disabled="record.builtin === 1">
                    <a-button type="text" size="small" danger :disabled="record.builtin === 1"><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- ==================== 敏感字段扫描 ==================== -->
        <a-tab-pane key="sensitivity" tab="敏感字段扫描">
          <div class="filter-bar">
            <a-space wrap>
              <a-select v-model:value="sensDsId" placeholder="选择数据源" style="width: 180px" allowClear :loading="dsLoading" @change="onSensDsChange">
                <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                  {{ ds.dsName }} ({{ ds.dsType }})
                </a-select-option>
              </a-select>
              <a-input
                v-model:value="sensTableName"
                placeholder="表名搜索"
                style="width: 160px"
                allowClear
                @pressEnter="loadSensitivity"
                @change="loadSensitivity"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
              <a-select v-model:value="sensClassId" placeholder="数据分类" style="width: 130px" allowClear @change="loadSensitivity">
                <a-select-option v-for="c in classData" :key="c.id" :value="c.id">{{ c.className }}</a-select-option>
              </a-select>
              <a-select v-model:value="sensLevelId" placeholder="敏感等级" style="width: 110px" allowClear @change="loadSensitivity">
                <a-select-option v-for="l in levelData" :key="l.id" :value="l.id">
                  <span :style="{ color: l.color }">{{ l.levelName }}</span>
                </a-select-option>
              </a-select>
              <a-select v-model:value="sensReviewStatus" placeholder="审核状态" style="width: 120px" allowClear @change="loadSensitivity">
                <a-select-option value="PENDING">待审核</a-select-option>
                <a-select-option value="APPROVED">已通过</a-select-option>
                <a-select-option value="REJECTED">已驳回</a-select-option>
              </a-select>
              <a-button @click="resetSensFilters">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
              <a-button type="primary" @click="openManualFieldDrawer">
                <template #icon><PlusOutlined /></template>
                手动新增
              </a-button>
              <a-button v-if="selectedRowKeys.length > 0" type="primary" @click="batchReview('APPROVED')" style="background: #52C41A; border-color: #52C41A">
                批量通过 ({{ selectedRowKeys.length }})
              </a-button>
              <a-button v-if="selectedRowKeys.length > 0" @click="batchReview('REJECTED')">
                批量驳回
              </a-button>
            </a-space>
          </div>
          <a-table
            :columns="sensColumns"
            :data-source="sensData"
            :loading="sensLoading"
            :pagination="sensPagination"
            :row-key="(r: SecColumnSensitivityVO) => r.id"
            :row-selection="{ selectedRowKeys, onChange: (keys: any) => selectedRowKeys = keys }"
            @change="handleSensTableChange"
            :scroll="{ x: 1500 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'columnName'">
                <div>
                  <div style="font-weight: 500; font-family: monospace">{{ record.columnName }}</div>
                  <div style="color: #888; font-size: 12px">{{ record.columnComment || '-' }}</div>
                </div>
              </template>
              <template v-if="column.key === 'className'">
                <a-tag>{{ record.className || '-' }}</a-tag>
              </template>
              <template v-if="column.key === 'level'">
                <a-tag :color="record.levelColor">{{ record.levelName || '-' }}</a-tag>
              </template>
              <template v-if="column.key === 'maskType'">
                <a-tag :color="getMaskTypeColor(record.maskType)">{{ record.maskTypeLabel || record.maskType }}</a-tag>
              </template>
              <template v-if="column.key === 'confidence'">
                <a-progress :percent="Math.round((record.confidence || 0))" size="small" :stroke-color="getConfidenceColor(record.confidence)" />
              </template>
              <template v-if="column.key === 'reviewStatus'">
                <a-badge
                  :status="getReviewBadge(record.reviewStatus)"
                  :text="record.reviewStatusLabel || record.reviewStatus"
                />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-tooltip title="审核">
                    <a-button type="text" size="small" @click="openReviewModal(record)" :disabled="record.reviewStatus !== 'PENDING'">
                      <CheckCircleOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-popconfirm title="确定删除？" @confirm="deleteSensitivity(record.id!)">
                    <a-button type="text" size="small" danger><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- ==================== 脱敏模板 ==================== -->
        <a-tab-pane key="mask" tab="脱敏模板">
          <div class="filter-bar">
            <a-space>
              <a-select v-model:value="maskEnabled" placeholder="是否启用" style="width: 110px" allowClear @change="loadMask">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
              <a-button type="primary" @click="openMaskDrawer('add')">
                <template #icon><PlusOutlined /></template>
                新增模板
              </a-button>
            </a-space>
          </div>
          <a-table
            :columns="maskColumns"
            :data-source="maskData"
            :loading="maskLoading"
            :pagination="maskPagination"
            :row-key="(r: SecMaskTemplateVO) => r.id"
            @change="handleMaskTableChange"
            :scroll="{ x: 1100 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'templateName'">
                <div>
                  <div style="font-weight: 500">{{ record.templateName }}</div>
                  <div style="color: #888; font-size: 12px">{{ record.templateDesc || record.description || '-' }}</div>
                </div>
              </template>
              <template v-if="column.key === 'maskType'">
                <a-tag :color="getMaskTypeColor(record.maskType)">{{ record.maskTypeLabel || record.maskType }}</a-tag>
              </template>
              <template v-if="column.key === 'maskPattern'">
                <code style="background: #f5f5f5; padding: 2px 6px; border-radius: 4px; font-size: 12px">{{ record.maskPattern || '-' }}</code>
              </template>
              <template v-if="column.key === 'builtin'">
                <a-tag :color="record.builtin === 1 ? 'blue' : 'default'">{{ record.builtinLabel }}</a-tag>
              </template>
              <template v-if="column.key === 'enabled'">
                <a-switch :checked="record.status === 1" size="small" @change="(c: boolean) => toggleMask(record, c)" />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-tooltip title="编辑"><a-button type="text" size="small" @click="openMaskDrawer('edit', record)"><EditOutlined /></a-button></a-tooltip>
                  <a-popconfirm title="确定删除该模板？" @confirm="deleteMask(record.id!)" :disabled="record.builtin === 1">
                    <a-button type="text" size="small" danger :disabled="record.builtin === 1"><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- ========== 数据分类抽屉 ========== -->
    <a-drawer
      v-model:open="classDrawerVisible"
      :title="classDrawerMode === 'add' ? '新增数据分类' : '编辑数据分类'"
      :width="520"
      @close="classDrawerVisible = false"
    >
      <a-form ref="classFormRef" :model="classForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" :rules="classFormRules">
        <a-form-item label="分类编码" name="classCode">
          <a-input v-model:value="classForm.classCode" placeholder="如 C01" :disabled="classDrawerMode === 'edit'" />
        </a-form-item>
        <a-form-item label="分类名称" name="className">
          <a-input v-model:value="classForm.className" placeholder="如 客户数据" />
        </a-form-item>
        <a-form-item label="分类描述" name="classDesc">
          <a-textarea v-model:value="classForm.classDesc" placeholder="简要描述" :rows="2" />
        </a-form-item>
        
        <a-form-item label="是否启用" name="enabled">
          <a-switch v-model:checked="classFormStatus" />
        </a-form-item>
        <a-form-item label="排序" name="classOrder">
          <a-input-number v-model:value="classForm.classOrder" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
      <div class="drawer-footer">
        <a-button @click="classDrawerVisible = false">取消</a-button>
        <a-button type="primary" @click="saveClass" :loading="classSaving">保存</a-button>
      </div>
    </a-drawer>

    <!-- ========== 敏感等级抽屉 ========== -->
    <a-drawer
      v-model:open="levelDrawerVisible"
      :title="levelDrawerMode === 'add' ? '新增敏感等级' : '编辑敏感等级'"
      :width="520"
      @close="levelDrawerVisible = false"
    >
      <a-form ref="levelFormRef" :model="levelForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" :rules="levelFormRules">
        <a-form-item label="等级编码" name="levelCode">
          <a-input v-model:value="levelForm.levelCode" placeholder="如 L1" :disabled="levelDrawerMode === 'edit'" />
        </a-form-item>
        <a-form-item label="等级名称" name="levelName">
          <a-input v-model:value="levelForm.levelName" placeholder="如 公开" />
        </a-form-item>
        <a-form-item label="等级数值" name="levelValue">
          <a-input-number v-model:value="levelForm.levelValue" :min="1" :max="10" style="width: 100%" />
          <div class="form-tip">数值越大敏感程度越高，用于排序和比较</div>
        </a-form-item>
        <a-form-item label="等级描述" name="levelDesc">
          <a-textarea v-model:value="levelForm.levelDesc" placeholder="简要描述" :rows="2" />
        </a-form-item>
        <a-form-item label="等级颜色" name="color">
          <a-input v-model:value="levelForm.color" placeholder="如 #52C41A">
            <template #suffix>
              <input type="color" v-model="levelForm.color" style="width: 24px; height: 24px; border: none; cursor: pointer; padding: 0;" />
            </template>
          </a-input>
        </a-form-item>
      </a-form>
      <div class="drawer-footer">
        <a-button @click="levelDrawerVisible = false">取消</a-button>
        <a-button type="primary" @click="saveLevel" :loading="levelSaving">保存</a-button>
      </div>
    </a-drawer>

    <!-- ========== 识别规则抽屉 ========== -->
    <a-drawer
      v-model:open="ruleDrawerVisible"
      :title="ruleDrawerMode === 'add' ? '新增识别规则' : '编辑识别规则'"
      :width="600"
      @close="ruleDrawerVisible = false"
    >
      <a-form ref="ruleFormRef" :model="ruleForm" :label-col="{ span: 7 }" :wrapper-col="{ span: 15 }" :rules="ruleFormRules">
        <a-form-item label="规则名称" name="ruleName">
          <a-input v-model:value="ruleForm.ruleName" placeholder="如 身份证号识别" />
        </a-form-item>
        <a-form-item label="规则编码" name="ruleCode">
          <a-input v-model:value="ruleForm.ruleCode" placeholder="唯一编码，如 id_card_rule" :disabled="ruleDrawerMode === 'edit'" />
        </a-form-item>
        <a-form-item label="匹配类型" name="matchType">
          <a-select v-model:value="ruleForm.matchType" placeholder="选择匹配方式">
            <a-select-option value="COLUMN_NAME">字段名匹配</a-select-option>
            <a-select-option value="COLUMN_COMMENT">字段注释匹配</a-select-option>
            <a-select-option value="DATA_TYPE">数据类型匹配</a-select-option>
            <a-select-option value="REGEX">正则表达式匹配</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="表达式类型" name="matchExprType">
          <a-select v-model:value="ruleForm.matchExprType" placeholder="选择表达式类型">
            <a-select-option value="CONTAINS">包含</a-select-option>
            <a-select-option value="EQUALS">等于</a-select-option>
            <a-select-option value="STARTS_WITH">开头是</a-select-option>
            <a-select-option value="REGEX">正则匹配</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="匹配表达式" name="matchExpr">
          <a-textarea v-model:value="ruleForm.matchExpr" placeholder="多个关键词用逗号分隔，如 id_card,sfz,身份证" :rows="2" />
          <div class="form-tip">支持多个关键词，逗号分隔。如：id_card,sfz 或正则：^1[3-9]\d{9}$</div>
        </a-form-item>
        <a-form-item label="默认敏感等级" name="levelId">
          <a-select v-model:value="ruleForm.levelId" placeholder="选择默认等级">
            <a-select-option v-for="l in levelData" :key="l.id" :value="l.id">{{ l.levelName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="是否启用" name="enabled">
          <a-switch v-model:checked="ruleFormStatus" />
        </a-form-item>
        <a-form-item label="规则描述" name="description">
          <a-textarea v-model:value="ruleForm.description" placeholder="描述规则用途" :rows="2" />
        </a-form-item>
      </a-form>
      <div class="drawer-footer">
        <a-button @click="ruleDrawerVisible = false">取消</a-button>
        <a-button type="primary" @click="saveRule" :loading="ruleSaving">保存</a-button>
      </div>
    </a-drawer>

    <!-- ========== 脱敏模板抽屉 ========== -->
    <a-drawer
      v-model:open="maskDrawerVisible"
      :title="maskDrawerMode === 'add' ? '新增脱敏模板' : '编辑脱敏模板'"
      :width="560"
      @close="maskDrawerVisible = false"
    >
      <a-form ref="maskFormRef" :model="maskForm" :label-col="{ span: 7 }" :wrapper-col="{ span: 15 }" :rules="maskFormRules">
        <a-form-item label="模板名称" name="templateName">
          <a-input v-model:value="maskForm.templateName" placeholder="如 手机号脱敏" />
        </a-form-item>
        <a-form-item label="脱敏类型" name="maskType">
          <a-select v-model:value="maskForm.maskType" placeholder="选择脱敏方式">
            <a-select-option value="NONE">不脱敏</a-select-option>
            <a-select-option value="MASK">掩码遮蔽</a-select-option>
            <a-select-option value="HIDE">部分隐藏</a-select-option>
            <a-select-option value="ENCRYPT">加密</a-select-option>
            <a-select-option value="DELETE">删除</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="['MASK','HIDE'].includes(maskForm.maskType || '')" label="掩码字符" name="maskChar">
          <a-input v-model:value="maskForm.maskChar" placeholder="如 *" :maxlength="1" />
        </a-form-item>
        <a-form-item v-if="maskForm.maskType === 'MASK'" label="掩码位置" name="maskPosition">
          <a-select v-model:value="maskForm.maskPosition" placeholder="选择掩码范围">
            <a-select-option value="HEAD">头部遮蔽</a-select-option>
            <a-select-option value="TAIL">尾部遮蔽</a-select-option>
            <a-select-option value="MIDDLE">中间遮蔽</a-select-option>
            <a-select-option value="ALL">全部遮蔽</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="脱敏正则" name="maskPattern">
          <a-input v-model:value="maskForm.maskPattern" placeholder="如 (\d{3})\d{4}(\d{4}) -> $1****$2" />
          <div class="form-tip">用于复杂替换的正则表达式</div>
        </a-form-item>
        <a-form-item label="是否启用" name="enabled">
          <a-switch v-model:checked="maskFormStatus" />
        </a-form-item>
        <a-form-item label="模板描述" name="description">
          <a-textarea v-model:value="maskForm.description" placeholder="描述模板用途" :rows="2" />
        </a-form-item>
        <a-form-item label="排序" name="sortOrder">
          <a-input-number v-model:value="maskForm.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
      <div class="drawer-footer">
        <a-button @click="maskDrawerVisible = false">取消</a-button>
        <a-button type="primary" @click="saveMask" :loading="maskSaving">保存</a-button>
      </div>
    </a-drawer>

    <!-- ========== 扫描对话框 ========== -->
    <a-modal
      v-model:open="scanDrawerVisible"
      :title="scanResult ? '扫描结果' : '扫描敏感字段'"
      :width="scanResult ? 680 : 580"
      @cancel="scanDrawerVisible = false"
      :footer="scanResult ? null : undefined"
    >
      <!-- 扫描配置表单（未扫描时显示） -->
      <a-form v-if="!scanResult" :model="scanForm" :label-col="{ span: 7 }" :wrapper-col="{ span: 15 }">
        <a-form-item label="数据源" required>
          <a-select v-model:value="scanForm.dsId" placeholder="选择数据源" :loading="dsLoading">
            <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
              {{ ds.dsName }} ({{ ds.dsType }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="扫描范围">
          <a-radio-group v-model:value="scanForm.scanScope">
            <a-radio value="ALL">全部表</a-radio>
            <a-radio value="LAYER">按数据层</a-radio>
            <a-radio value="SPECIFIC">指定表</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="scanForm.scanScope === 'LAYER'" label="数据层">
          <a-select v-model:value="scanForm.layerCode" placeholder="选择数据层">
            <a-select-option value="ODS">ODS - 操作数据层</a-select-option>
            <a-select-option value="DWD">DWD - 明细事实层</a-select-option>
            <a-select-option value="DWS">DWS - 汇总事实层</a-select-option>
            <a-select-option value="DIM">DIM - 公共维度层</a-select-option>
            <a-select-option value="ADS">ADS - 应用层</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="scanForm.scanScope === 'SPECIFIC'" label="指定表名">
          <a-textarea v-model:value="scanForm.tableNames" placeholder="多个表名用逗号分隔，如: orders, customers" :rows="2" />
        </a-form-item>
        <a-divider style="margin: 8px 0">高级选项</a-divider>
        <a-form-item label="扫描维度" :label-col="{ span: 7 }">
          <a-space direction="vertical" style="width: 100%">
            <a-checkbox v-model:checked="scanForm.scanColumnName">扫描列名匹配</a-checkbox>
            <a-checkbox v-model:checked="scanForm.scanColumnComment">扫描列注释匹配</a-checkbox>
            <a-checkbox v-model:checked="scanForm.scanDataType">扫描数据类型匹配</a-checkbox>
            <a-checkbox v-model:checked="scanForm.useRegex">启用正则表达式匹配</a-checkbox>
          </a-space>
        </a-form-item>
        <a-form-item label="排除系统表">
          <a-switch v-model:checked="scanForm.excludeSystemTables" />
          <span style="color: #8c8c8c; margin-left: 8px; font-size: 12px">自动排除 sys_ 开头的表</span>
        </a-form-item>
      </a-form>
      <!-- 扫描结果展示 -->
      <div v-if="scanResult">
        <a-result
          :status="(scanResult.foundSensitiveCount ?? 0) > 0 ? 'success' : 'warning'"
          :title="`扫描完成：发现 ${scanResult.foundSensitiveCount ?? 0} 个敏感字段`"
          :sub-title="`扫描 ${scanResult.totalTableCount ?? 0} 张表，共 ${scanResult.totalColumnCount ?? 0} 个字段，耗时 ${scanResult.elapsedMs ?? 0}ms`"
        >
          <template #extra>
            <a-row :gutter="16" style="margin-bottom: 16px">
              <a-col :span="8">
                <a-statistic title="扫描表数" :value="scanResult.totalTableCount ?? 0" />
              </a-col>
              <a-col :span="8">
                <a-statistic title="扫描字段数" :value="scanResult.totalColumnCount ?? 0" />
              </a-col>
              <a-col :span="8">
                <a-statistic title="发现敏感字段" :value="scanResult.foundSensitiveCount ?? 0" :value-style="{ color: (scanResult.foundSensitiveCount ?? 0) > 0 ? '#52C41A' : '#FAAD14' }" />
              </a-col>
            </a-row>
            <div v-if="scanResult.countByLevel && Object.keys(scanResult.countByLevel).length > 0" style="margin-bottom: 16px">
              <div style="font-weight: 500; margin-bottom: 8px">按等级分布：</div>
              <a-tag v-for="(count, level) in scanResult.countByLevel" :key="level" color="blue" style="margin-right: 8px; margin-bottom: 4px">
                {{ level }}: {{ count }}
              </a-tag>
            </div>
          </template>
        </a-result>
      </div>
      <template #footer v-if="!scanResult">
        <a-button @click="scanDrawerVisible = false">取消</a-button>
        <a-button type="primary" @click="doScan" :loading="scanning" :disabled="!scanForm.dsId">开始扫描</a-button>
      </template>
      <template #footer v-if="scanResult">
        <a-space>
          <a-button @click="scanResult = null; scanForm.dsId = undefined">继续扫描</a-button>
          <a-button type="primary" @click="scanDrawerVisible = false; scanResult = null; activeTab = 'sensitivity'; loadSensitivity()">查看详情</a-button>
        </a-space>
      </template>
    </a-modal>

    <!-- ========== 审核对话框 ========== -->
    <a-modal
      v-model:open="reviewModalVisible"
      title="审核敏感字段"
      width="480"
      @ok="doReview"
    >
      <a-form :label-col="{ span: 6 }">
        <a-form-item label="字段">
          <div><code>{{ reviewRecord.columnName }}</code> ({{ reviewRecord.tableName }})</div>
        </a-form-item>
        <a-form-item label="识别的分类">
          <a-tag>{{ reviewRecord.className }}</a-tag>
        </a-form-item>
        <a-form-item label="识别的等级">
          <a-tag :color="reviewRecord.levelColor">{{ reviewRecord.levelName }}</a-tag>
        </a-form-item>
        <a-form-item label="审核结果" required>
          <a-radio-group v-model:value="reviewForm.reviewStatus">
            <a-radio value="APPROVED" style="color: #52C41A">
              <CheckCircleOutlined /> 通过
            </a-radio>
            <a-radio value="REJECTED" style="color: #F5222D">
              <CloseCircleOutlined /> 驳回
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审核意见">
          <a-textarea v-model:value="reviewForm.reviewComment" placeholder="可选，填写审核意见" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ========== 手动新增敏感字段抽屉（Step 1-4） ========== -->
    <a-drawer
      v-model:open="manualDrawerVisible"
      :title="manualStep === 5 ? '配置脱敏方式' : `手动新增敏感字段（步骤 ${manualStep}/4）`"
      :width="640"
      @close="manualDrawerVisible = false"
    >
      <!-- 步骤进度 -->
      <a-steps :current="manualStep - 1" size="small" style="margin-bottom: 24px" v-if="manualStep <= 4">
        <a-step title="选择目标" />
        <a-step title="选择字段" />
        <a-step title="设置等级" />
        <a-step title="配置脱敏" />
      </a-steps>

      <!-- Step 1: 选择数据源和表 -->
      <div v-if="manualStep === 1">
        <a-form layout="vertical">
          <a-form-item label="数据源" required>
            <a-select v-model:value="manualDsId" placeholder="选择数据源" :loading="dsLoading" @change="onManualDsChange">
              <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">{{ ds.dsName }} ({{ ds.dsType }})</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="表名" required>
            <a-input v-model:value="manualTableName" placeholder="输入表名，如 orders" allowClear />
            <div class="form-tip">输入要管理的目标表名，支持模糊搜索已扫描的表</div>
          </a-form-item>
        </a-form>
        <div class="drawer-footer">
          <a-button @click="manualDrawerVisible = false">取消</a-button>
          <a-button type="primary" @click="doManualStep1" :disabled="!manualDsId || !manualTableName">下一步</a-button>
        </div>
      </div>

      <!-- Step 2: 选择字段 -->
      <div v-if="manualStep === 2">
        <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center">
          <span>已找到 <strong>{{ manualStep2Cols.length }}</strong> 个字段，请勾选要管理的字段</span>
          <a-space>
            <a-button size="small" @click="manualSelectedColumns = new Set(manualStep2Cols.map(c => c.columnName))">全选</a-button>
            <a-button size="small" @click="manualSelectedColumns = new Set()">取消全选</a-button>
          </a-space>
        </div>
        <a-table
          :columns="[{ title: '选择', key: 'check', width: 60 }, { title: '字段名', dataIndex: 'columnName', width: 160 }, { title: '注释', dataIndex: 'columnComment', ellipsis: true }, { title: '类型', dataIndex: 'dataType', width: 100 }]"
          :data-source="manualStep2Cols"
          :pagination="{ pageSize: 10, size: 'small' }"
          :row-key="(r: any) => r.columnName"
          :row-selection="{ selectedRowKeys: Array.from(manualSelectedColumns), onChange: (keys: any) => manualSelectedColumns = new Set(keys) }"
          size="small"
          :scroll="{ y: 320 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'check'">
              <a-checkbox :checked="manualSelectedColumns.has(record.columnName)" @change="() => { if (manualSelectedColumns.has(record.columnName)) { manualSelectedColumns.delete(record.columnName); manualSelectedColumns = new Set(manualSelectedColumns) } else { manualSelectedColumns.add(record.columnName); manualSelectedColumns = new Set(manualSelectedColumns) } }" />
            </template>
          </template>
        </a-table>
        <div class="drawer-footer">
          <a-button @click="manualStep = 1">上一步</a-button>
          <a-button type="primary" @click="doManualStep2" :disabled="manualSelectedColumns.size === 0">下一步</a-button>
        </div>
      </div>

      <!-- Step 3: 设置敏感等级 -->
      <div v-if="manualStep === 3">
        <a-alert type="info" show-icon style="margin-bottom: 16px">
          <template #message>将为选中的 {{ manualSelectedColumns.size }} 个字段设置统一的敏感等级</template>
        </a-alert>
        <a-form layout="vertical">
          <a-form-item label="数据分类" required>
            <a-select v-model:value="manualForm.classId" placeholder="选择数据分类">
              <a-select-option v-for="c in classData" :key="c.id" :value="c.id">{{ c.className }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="敏感等级" required>
            <a-select v-model:value="manualForm.levelId" placeholder="选择敏感等级">
              <a-select-option v-for="l in levelData" :key="l.id" :value="l.id">
                <span :style="{ color: l.color, fontWeight: 600 }">{{ l.levelName }}</span> (等级值: {{ l.levelValue }})
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
        <div class="drawer-footer">
          <a-button @click="manualStep = 2">上一步</a-button>
          <a-button type="primary" @click="manualStep = 4" :disabled="!manualForm.classId || !manualForm.levelId">下一步</a-button>
        </div>
      </div>

      <!-- Step 4: 配置脱敏方式 -->
      <div v-if="manualStep === 4">
        <a-alert type="info" show-icon style="margin-bottom: 16px">
          <template #message>为选中的 {{ manualSelectedColumns.size }} 个字段配置脱敏方式</template>
        </a-alert>
        <a-form layout="vertical">
          <a-form-item label="脱敏方式" required>
            <a-select v-model:value="manualForm.maskType" placeholder="选择脱敏方式">
              <a-select-option value="NONE">不脱敏</a-select-option>
              <a-select-option value="MASK">掩码遮蔽</a-select-option>
              <a-select-option value="HIDE">部分隐藏</a-select-option>
              <a-select-option value="ENCRYPT">加密</a-select-option>
              <a-select-option value="DELETE">删除</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="['MASK', 'HIDE'].includes(manualForm.maskType || '')" label="掩码字符">
            <a-input v-model:value="manualForm.maskChar" placeholder="如 *" style="width: 120px" :maxlength="1" />
          </a-form-item>
          <a-form-item v-if="['MASK', 'HIDE'].includes(manualForm.maskType || '')" label="遮蔽位置">
            <a-select v-model:value="manualForm.maskPosition" placeholder="选择遮蔽位置" style="width: 200px">
              <a-select-option value="HEAD">头部遮蔽（保留尾部）</a-select-option>
              <a-select-option value="TAIL">尾部遮蔽（保留头部）</a-select-option>
              <a-select-option value="CENTER">中间遮蔽</a-select-option>
              <a-select-option value="ALL">全部遮蔽</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="脱敏格式（可选）">
            <a-input v-model:value="manualForm.maskPattern" placeholder="如 138****{TAIL4} 或 {HEAD3}****{TAIL4}" />
            <div class="form-tip">用于复杂替换的格式模板，支持 {HEADN}、{TAILN}、{FULL} 占位符</div>
          </a-form-item>
        </a-form>
        <div class="drawer-footer">
          <a-button @click="manualStep = 3">上一步</a-button>
          <a-button type="primary" @click="doManualSave" :loading="manualSaving">保存</a-button>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined,
  ReloadOutlined, ScanOutlined, SafetyOutlined, CheckCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import {
  getSecurityStats,
  pageClassification as pageClassificationApi,
  listClassification as listClassificationApi,
  saveClassification as saveClassificationApi,
  updateClassification as updateClassificationApi,
  deleteClassification as deleteClassificationApi,
  enableClassification as enableClassificationApi,
  disableClassification as disableClassificationApi,
  getClassificationDetail as getClassificationDetailApi,
  pageLevel as pageLevelApi,
  listLevel as listLevelApi,
  saveLevel as saveLevelApi,
  updateLevel as updateLevelApi,
  deleteLevel as deleteLevelApi,
  pageRule as pageRuleApi,
  listRule as listRuleApi,
  saveRule as saveRuleApi,
  updateRule as updateRuleApi,
  deleteRule as deleteRuleApi,
  enableRule as enableRuleApi,
  disableRule as disableRuleApi,
  pageSensitivity as pageSensitivityApi,
  scanSensitiveFields as scanSensitiveFieldsApi,
  reviewSensitivity as reviewSensitivityApi,
  batchReviewSensitivity as batchReviewSensitivityApi,
  deleteSensitivity as deleteSensitivityApi,
  saveColumnSensitivity as saveColumnSensitivityApi,
  batchSaveColumnSensitivity as batchSaveColumnSensitivityApi,
  pageMaskTemplate as pageMaskTemplateApi,
  listMaskTemplate as listMaskTemplateApi,
  saveMaskTemplate as saveMaskTemplateApi,
  updateMaskTemplate as updateMaskTemplateApi,
  deleteMaskTemplate as deleteMaskTemplateApi,
  enableMaskTemplate as enableMaskTemplateApi,
  disableMaskTemplate as disableMaskTemplateApi,
  getSensitivityStatsByDs as getSensitivityStatsByDsApi,
  type SecClassificationVO,
  type SecClassificationDetailVO,
  type SecLevelVO,
  type SecSensitivityRuleVO,
  type SecColumnSensitivityVO,
  type SecMaskTemplateVO,
  type SecScanResultVO,
  type SecClassificationSaveDTO,
  type SecLevelSaveDTO,
  type SecSensitivityRuleSaveDTO,
  type SecMaskTemplateSaveDTO,
  type SecSensitivityScanDTO,
  type SensitivityLevel,
  type ReviewStatus
} from '@/api/security'
import type { Result } from '@/types'
import { dataSourceApi } from '@/api/dqc'
import { metadataApi } from '@/api/gov'

// ==================== 公共状态 ====================
const activeTab = ref('classification')
const dsLoading = ref(false)
const dataSourceList = ref<any[]>([])

// ==================== 统计数据 ====================
const statCards = ref([
  { label: '分类总数', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #69B1FF 100%)' },
  { label: '敏感等级', value: 0, bg: 'linear-gradient(135deg, #F5222D 0%, #FF7875 100%)' },
  { label: '识别规则', value: 0, bg: 'linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)' },
  { label: '待审核', value: 0, bg: 'linear-gradient(135deg, #722ED1 0%, #B37FEB 100%)' },
  { label: '已通过', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '脱敏模板', value: 0, bg: 'linear-gradient(135deg, #13C2C2 0%, #36CFC9 100%)' }
])

// ==================== 数据分类 ====================
const classLoading = ref(false)
const classData = ref<SecClassificationVO[]>([])
const classSearch = ref('')
const classEnabled = ref<number | undefined>()
const classDrawerVisible = ref(false)
const classDrawerMode = ref<'add' | 'edit'>('add')
const classSaving = ref(false)
const classFormRef = ref<FormInstance>()
const classFormStatus = ref(true)

const classForm = reactive<SecClassificationSaveDTO>({
  id: undefined, classCode: '', className: '', classDesc: '',
  sensitivityLevel: undefined, status: 1, classOrder: 0
})

const classFormRules = {
  classCode: [{ required: true, message: '请输入分类编码' }],
  className: [{ required: true, message: '请输入分类名称' }]
}

const classPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

const classColumns = [
  { title: '分类名称', key: 'className', width: 220, ellipsis: true },
  { title: '敏感等级', key: 'sensitivityLevel', width: 100, align: 'center' },
  { title: '规则数', dataIndex: 'ruleCount', width: 80, align: 'center' },
  { title: '字段数', dataIndex: 'sensitiveFieldCount', width: 80, align: 'center' },
  { title: '启用', key: 'enabled', width: 70, align: 'center' },
  { title: '排序', dataIndex: 'classOrder', width: 70, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

async function loadClassification() {
  classLoading.value = true
  try {
    const res = await pageClassificationApi({ pageNum: classPagination.current, pageSize: classPagination.pageSize, className: classSearch.value || undefined, enabled: classEnabled.value })
    if (res.data?.success !== false) {
      classData.value = res.data?.records || []
      classPagination.total = res.data?.total || 0
    }
  } catch { message.error('加载数据分类失败') }
  finally { classLoading.value = false }
}

function handleClassTableChange(pag: any) {
  classPagination.current = pag.current
  classPagination.pageSize = pag.pageSize
  loadClassification()
}

function resetClassFilters() {
  classSearch.value = ''
  classEnabled.value = undefined
  loadClassification()
}

function openClassDrawer(mode: 'add' | 'edit', record?: SecClassificationVO) {
  classDrawerMode.value = mode
  if (mode === 'edit' && record) {
    Object.assign(classForm, { id: record.id, classCode: record.classCode, className: record.className, classDesc: record.classDesc, sensitivityLevel: record.sensitivityLevel, status: record.status, classOrder: record.classOrder || 0 })
    classFormStatus.value = record.status === 1
  } else {
    Object.assign(classForm, { id: undefined, classCode: '', className: '', classDesc: '', sensitivityLevel: undefined, status: 1, classOrder: 0 })
    classFormStatus.value = true
  }
  classDrawerVisible.value = true
}

async function saveClass() {
  try { await classFormRef.value?.validate() } catch { return }
  classSaving.value = true
  try {
    classForm.status = classFormStatus.value ? 1 : 0
    if (classDrawerMode.value === 'add') {
      await saveClassificationApi(classForm)
      message.success('新增成功')
    } else {
      await updateClassificationApi(classForm.id!, classForm)
      message.success('更新成功')
    }
    classDrawerVisible.value = false
    loadClassification()
  } catch { message.error('保存失败') }
  finally { classSaving.value = false }
}

async function deleteClass(id: number) {
  try { await deleteClassificationApi(id); message.success('删除成功'); loadClassification() }
  catch { message.error('删除失败') }
}

async function toggleClass(record: SecClassificationVO, checked: boolean) {
  try {
    if (checked) await enableClassificationApi(record.id!)
    else await disableClassificationApi(record.id!)
    loadClassification()
  } catch { message.error('操作失败') }
}

// ==================== 敏感等级 ====================
const levelLoading = ref(false)
const levelData = ref<SecLevelVO[]>([])
const levelDrawerVisible = ref(false)
const levelDrawerMode = ref<'add' | 'edit'>('add')
const levelSaving = ref(false)
const levelFormRef = ref<FormInstance>()

const levelForm = reactive<SecLevelSaveDTO>({ id: undefined, levelCode: '', levelName: '', levelValue: 1, levelDesc: '', color: '#52C41A' })
const levelFormRules = { levelCode: [{ required: true, message: '请输入等级编码' }], levelName: [{ required: true, message: '请输入等级名称' }], levelValue: [{ required: true, message: '请输入等级数值' }] }
const levelPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

const levelColumns = [
  { title: '等级值', key: 'levelValue', width: 80, align: 'center' },
  { title: '等级名称', key: 'levelName', width: 180 },
  { title: '等级描述', key: 'levelDesc', dataIndex: 'levelDesc', width: 200, ellipsis: true },
  { title: '字段数', key: 'sensitiveFieldCount', dataIndex: 'sensitiveFieldCount', width: 80, align: 'center' },
  { title: '创建时间', key: 'createTime', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

async function loadLevel() {
  levelLoading.value = true
  try {
    const res = await pageLevelApi({ pageNum: levelPagination.current, pageSize: levelPagination.pageSize })
    if (res.data?.success !== false) { levelData.value = res.data?.records || []; levelPagination.total = res.data?.total || 0 }
  } catch { message.error('加载敏感等级失败') }
  finally { levelLoading.value = false }
}

function handleLevelTableChange(pag: any) { levelPagination.current = pag.current; levelPagination.pageSize = pag.pageSize; loadLevel() }

function openLevelDrawer(mode: 'add' | 'edit', record?: SecLevelVO) {
  levelDrawerMode.value = mode
  if (mode === 'edit' && record) Object.assign(levelForm, { id: record.id, levelCode: record.levelCode, levelName: record.levelName, levelValue: record.levelValue, levelDesc: record.levelDesc, color: record.color })
  else Object.assign(levelForm, { id: undefined, levelCode: '', levelName: '', levelValue: 1, levelDesc: '', color: '#52C41A' })
  levelDrawerVisible.value = true
}

async function saveLevel() {
  try { await levelFormRef.value?.validate() } catch { return }
  levelSaving.value = true
  try {
    if (levelDrawerMode.value === 'add') { await saveLevelApi(levelForm); message.success('新增成功') }
    else { await updateLevelApi(levelForm.id!, levelForm); message.success('更新成功') }
    levelDrawerVisible.value = false
    loadLevel()
  } catch { message.error('保存失败') }
  finally { levelSaving.value = false }
}

async function deleteLevel(id: number) {
  try { await deleteLevelApi(id); message.success('删除成功'); loadLevel() }
  catch { message.error('删除失败') }
}

// ==================== 识别规则 ====================
const ruleLoading = ref(false)
const ruleData = ref<SecSensitivityRuleVO[]>([])
const ruleMatchType = ref<string | undefined>()
const ruleEnabled = ref<number | undefined>()
const ruleDrawerVisible = ref(false)
const ruleDrawerMode = ref<'add' | 'edit'>('add')
const ruleSaving = ref(false)
const ruleFormRef = ref<FormInstance>()
const ruleFormStatus = ref(true)

const ruleForm = reactive<SecSensitivityRuleSaveDTO>({
  id: undefined,
  ruleName: '',
  ruleCode: '',
  matchType: undefined,
  matchExpr: '',
  matchExprType: 'CONTAINS',
  classId: undefined,
  levelId: undefined,
  suggestionAction: undefined,
  suggestionMaskPattern: '',
  suggestionMaskType: undefined,
  priority: 0,
  status: 1,
  description: ''
})
const ruleFormRules = { ruleName: [{ required: true, message: '请输入规则名称' }], matchType: [{ required: true, message: '请选择匹配类型' }], matchExpr: [{ required: true, message: '请输入匹配表达式' }] }
const rulePagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

const ruleColumns = [
  { title: '规则名称', key: 'ruleName', width: 240, ellipsis: true },
  { title: '匹配类型', key: 'matchType', width: 120, align: 'center' },
  { title: '匹配表达式', key: 'matchExpr', width: 220, ellipsis: true },
  { title: '默认等级', key: 'defaultLevel', width: 100, align: 'center' },
  { title: '内置', key: 'builtin', width: 80, align: 'center' },
  { title: '启用', key: 'enabled', width: 70, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

async function loadRule() {
  ruleLoading.value = true
  try {
    const res = await pageRuleApi({ pageNum: rulePagination.current, pageSize: rulePagination.pageSize, matchType: ruleMatchType.value as any, enabled: ruleEnabled.value })
    if (res.data?.success !== false) { ruleData.value = res.data?.records || []; rulePagination.total = res.data?.total || 0 }
  } catch { message.error('加载识别规则失败') }
  finally { ruleLoading.value = false }
}

function handleRuleTableChange(pag: any) { rulePagination.current = pag.current; rulePagination.pageSize = pag.pageSize; loadRule() }
function resetRuleFilters() { ruleMatchType.value = undefined; ruleEnabled.value = undefined; loadRule() }

function openRuleDrawer(mode: 'add' | 'edit', record?: SecSensitivityRuleVO) {
  ruleDrawerMode.value = mode
  if (mode === 'edit' && record) {
    Object.assign(ruleForm, {
      id: record.id,
      ruleName: record.ruleName,
      ruleCode: record.ruleCode,
      matchType: record.matchType,
      matchExpr: record.matchExpr,
      matchExprType: record.matchExprType,
      classId: record.classId,
      levelId: record.levelId,
      suggestionAction: record.suggestionAction,
      suggestionMaskPattern: record.suggestionMaskPattern,
      suggestionMaskType: record.suggestionMaskType,
      priority: record.priority,
      status: record.status,
      description: record.description
    })
  } else {
    Object.assign(ruleForm, {
      id: undefined, ruleName: '', ruleCode: '', matchType: undefined, matchExpr: '',
      matchExprType: 'CONTAINS', classId: undefined, levelId: undefined,
      suggestionAction: undefined, suggestionMaskPattern: '', suggestionMaskType: undefined,
      priority: 0, status: 1, description: ''
    })
  }
  ruleFormStatus.value = mode === 'add' ? true : record?.status === 1
  ruleDrawerVisible.value = true
}

async function saveRule() {
  try { await ruleFormRef.value?.validate() } catch { return }
  ruleSaving.value = true
  try {
    ruleForm.status = ruleFormStatus.value ? 1 : 0
    if (ruleDrawerMode.value === 'add') { await saveRuleApi(ruleForm); message.success('新增成功') }
    else { await updateRuleApi(ruleForm.id!, ruleForm); message.success('更新成功') }
    ruleDrawerVisible.value = false
    loadRule()
  } catch { message.error('保存失败') }
  finally { ruleSaving.value = false }
}

async function deleteRule(id: number) {
  try { await deleteRuleApi(id); message.success('删除成功'); loadRule() }
  catch { message.error('删除失败') }
}

async function toggleRule(record: SecSensitivityRuleVO, checked: boolean) {
  try { if (checked) await enableRuleApi(record.id!); else await disableRuleApi(record.id!); loadRule() }
  catch { message.error('操作失败') }
}

// ==================== 敏感字段扫描 ====================
const sensLoading = ref(false)
const sensData = ref<SecColumnSensitivityVO[]>([])
const sensDsId = ref<number | undefined>()
const sensTableName = ref<string | undefined>()
const sensClassId = ref<number | undefined>()
const sensLevelId = ref<number | undefined>()
const sensReviewStatus = ref<string | undefined>()
const selectedRowKeys = ref<number[]>([])
const reviewModalVisible = ref(false)
const reviewRecord = ref<SecColumnSensitivityVO>({})

const reviewForm = reactive({ reviewStatus: 'APPROVED' as ReviewStatus, reviewComment: '' })
const sensPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

// ==================== 手动新增敏感字段 ====================
const manualDrawerVisible = ref(false)
const manualSaving = ref(false)
const manualStep = ref(1)
const manualDsId = ref<number | undefined>()
const manualTableName = ref<string>('')
const manualColumns = ref<{ columnName: string; columnComment?: string; dataType?: string }[]>([])
const manualSelectedColumns = ref<Set<string>>(new Set())
const manualForm = reactive({
  classId: undefined as number | undefined,
  levelId: undefined as number | undefined,
  maskType: undefined as string | undefined,
  maskPosition: 'CENTER',
  maskChar: '*',
  maskPattern: ''
})
const manualStep2Cols = ref<{ columnName: string; columnComment?: string; dataType?: string }[]>([])

const sensColumns = [
  { title: '字段名', key: 'columnName', width: 180 },
  { title: '表名', dataIndex: 'tableName', width: 160, ellipsis: true },
  { title: '数据类型', dataIndex: 'dataType', width: 90 },
  { title: '数据源', dataIndex: 'dsName', width: 120, ellipsis: true },
  { title: '分类', key: 'className', width: 110 },
  { title: '等级', key: 'level', width: 80, align: 'center' },
  { title: '脱敏', key: 'maskType', width: 90, align: 'center' },
  { title: '置信度', key: 'confidence', width: 100, align: 'center' },
  { title: '审核状态', key: 'reviewStatus', width: 100 },
  { title: '识别规则', dataIndex: 'matchRuleName', width: 130, ellipsis: true },
  { title: '审核人', dataIndex: 'approvedByName', width: 90 },
  { title: '扫描批次', dataIndex: 'scanBatchNo', width: 140, ellipsis: true },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]

async function loadSensitivity() {
  sensLoading.value = true
  try {
    const res = await pageSensitivityApi({
      pageNum: sensPagination.current,
      pageSize: sensPagination.pageSize,
      dsId: sensDsId.value,
      tableName: sensTableName.value,
      classId: sensClassId.value,
      levelId: sensLevelId.value,
      reviewStatus: sensReviewStatus.value as any
    })
    if (res.data?.success !== false) { sensData.value = res.data?.records || []; sensPagination.total = res.data?.total || 0 }
  } catch { message.error('加载敏感字段失败') }
  finally { sensLoading.value = false }
}

function handleSensTableChange(pag: any) { sensPagination.current = pag.current; sensPagination.pageSize = pag.pageSize; loadSensitivity() }

function onSensDsChange() {
  sensPagination.current = 1
  loadSensitivity()
}

function resetSensFilters() {
  sensDsId.value = undefined
  sensTableName.value = undefined
  sensClassId.value = undefined
  sensLevelId.value = undefined
  sensReviewStatus.value = undefined
  selectedRowKeys.value = []
  sensPagination.current = 1
  loadSensitivity()
}

// ==================== 手动新增敏感字段 ====================
function openManualFieldDrawer() {
  manualStep.value = 1
  manualDsId.value = undefined
  manualTableName.value = ''
  manualStep2Cols.value = []
  manualSelectedColumns.value = new Set()
  manualForm.classId = undefined
  manualForm.levelId = undefined
  manualForm.maskType = undefined
  manualForm.maskPosition = 'CENTER'
  manualForm.maskChar = '*'
  manualForm.maskPattern = ''
  manualDrawerVisible.value = true
}

async function onManualDsChange() {
  // When datasource changes, clear table-related selections
}

async function doManualStep1() {
  if (!manualDsId.value || !manualTableName.value) return
  try {
    const metaRes = await metadataApi.getByDsIdAndTable(manualDsId.value, manualTableName.value)
    if (metaRes.data?.success !== false && metaRes.data?.data) {
      const metadataId = metaRes.data?.id
      if (metadataId) {
        const colRes = await metadataApi.getColumns(metadataId)
        if (colRes.data?.success !== false) {
          manualStep2Cols.value = (colRes.data || []).map((c: any) => ({
            columnName: c.columnName,
            columnComment: c.columnComment || '',
            dataType: c.dataType || ''
          }))
        } else {
          manualStep2Cols.value = []
        }
      } else {
        manualStep2Cols.value = []
        message.warning('该表尚未扫描元数据，请先在元数据管理中扫描')
      }
    } else {
      manualStep2Cols.value = []
    }
    manualStep.value = 2
  } catch {
    manualStep2Cols.value = []
    manualStep.value = 2
  }
}

function doManualStep2() {
  if (manualSelectedColumns.value.size === 0) {
    message.warning('请至少选择一个字段')
    return
  }
  manualStep.value = 3
}

async function doManualSave() {
  if (!manualForm.classId || !manualForm.levelId) {
    message.warning('请填写完整的等级和分类信息')
    return
  }
  manualSaving.value = true
  try {
    const items = Array.from(manualSelectedColumns.value).map((colName: string) => {
      const col = manualStep2Cols.value.find(c => c.columnName === colName)
      return {
        dsId: manualDsId.value,
        tableName: manualTableName.value,
        columnName: colName,
        columnComment: col?.columnComment || '',
        dataType: col?.dataType || 'varchar',
        classId: manualForm.classId,
        levelId: manualForm.levelId,
        maskType: manualForm.maskType || 'NONE',
        maskPosition: manualForm.maskPosition || 'CENTER',
        maskChar: manualForm.maskChar || '*',
        maskPattern: manualForm.maskPattern || ''
      }
    })
    await batchSaveColumnSensitivityApi(items)
    message.success(`成功保存 ${items.length} 个敏感字段`)
    manualDrawerVisible.value = false
    loadSensitivity()
    loadStats()
  } catch {
    message.error('保存失败')
  } finally {
    manualSaving.value = false
  }
}

function openReviewModal(record: SecColumnSensitivityVO) {
  reviewRecord.value = record
  reviewForm.reviewStatus = 'APPROVED'
  reviewForm.reviewComment = ''
  reviewModalVisible.value = true
}

async function doReview() {
  try {
    await reviewSensitivityApi(reviewRecord.value.id!, reviewForm.reviewStatus, reviewForm.reviewComment)
    message.success('审核成功')
    reviewModalVisible.value = false
    loadSensitivity()
  } catch { message.error('审核失败') }
}

async function batchReview(status: ReviewStatus) {
  try {
    await batchReviewSensitivityApi(selectedRowKeys.value, status)
    message.success('批量审核成功')
    selectedRowKeys.value = []
    loadSensitivity()
  } catch { message.error('批量审核失败') }
}

async function deleteSensitivity(id: number) {
  try { await deleteSensitivityApi(id); message.success('删除成功'); loadSensitivity() }
  catch { message.error('删除失败') }
}

// ==================== 脱敏模板 ====================
const maskLoading = ref(false)
const maskData = ref<SecMaskTemplateVO[]>([])
const maskEnabled = ref<number | undefined>()
const maskDrawerVisible = ref(false)
const maskDrawerMode = ref<'add' | 'edit'>('add')
const maskSaving = ref(false)
const maskFormRef = ref<FormInstance>()
const maskFormStatus = ref(true)

const maskForm = reactive<SecMaskTemplateSaveDTO>({ id: undefined, templateName: '', maskType: undefined, maskPattern: '', maskPosition: '', maskChar: '*', description: '', status: 1, sortOrder: 0 })
const maskFormRules = { templateName: [{ required: true, message: '请输入模板名称' }], maskType: [{ required: true, message: '请选择脱敏类型' }] }
const maskPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

const maskColumns = [
  { title: '模板名称', key: 'templateName', width: 220, ellipsis: true },
  { title: '脱敏类型', key: 'maskType', width: 110, align: 'center' },
  { title: '掩码正则', key: 'maskPattern', width: 200, ellipsis: true },
  { title: '内置', key: 'builtin', width: 70, align: 'center' },
  { title: '启用', key: 'enabled', width: 70, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

async function loadMask() {
  maskLoading.value = true
  try {
    const res = await pageMaskTemplateApi({ pageNum: maskPagination.current, pageSize: maskPagination.pageSize, enabled: maskEnabled.value })
    if (res.data?.success !== false) { maskData.value = res.data?.records || []; maskPagination.total = res.data?.total || 0 }
  } catch { message.error('加载脱敏模板失败') }
  finally { maskLoading.value = false }
}

function handleMaskTableChange(pag: any) { maskPagination.current = pag.current; maskPagination.pageSize = pag.pageSize; loadMask() }

function openMaskDrawer(mode: 'add' | 'edit', record?: SecMaskTemplateVO) {
  maskDrawerMode.value = mode
  if (mode === 'edit' && record) Object.assign(maskForm, { id: record.id, templateName: record.templateName, maskType: record.maskType, maskPattern: record.maskPattern, maskPosition: record.maskPosition, maskChar: record.maskChar, description: record.description, status: record.status, sortOrder: record.sortOrder || 0 })
  else Object.assign(maskForm, { id: undefined, templateName: '', maskType: undefined, maskPattern: '', maskPosition: '', maskChar: '*', description: '', status: 1, sortOrder: 0 })
  maskFormStatus.value = mode === 'add' ? true : record?.enabled === 1
  maskDrawerVisible.value = true
}

async function saveMask() {
  try { await maskFormRef.value?.validate() } catch { return }
  maskSaving.value = true
  try {
    maskForm.status = maskFormStatus.value ? 1 : 0
    if (maskDrawerMode.value === 'add') { await saveMaskTemplateApi(maskForm); message.success('新增成功') }
    else { await updateMaskTemplateApi(maskForm.id!, maskForm); message.success('更新成功') }
    maskDrawerVisible.value = false
    loadMask()
  } catch { message.error('保存失败') }
  finally { maskSaving.value = false }
}

async function deleteMask(id: number) {
  try { await deleteMaskTemplateApi(id); message.success('删除成功'); loadMask() }
  catch { message.error('删除失败') }
}

async function toggleMask(record: SecMaskTemplateVO, checked: boolean) {
  try { if (checked) await enableMaskTemplateApi(record.id!); else await disableMaskTemplateApi(record.id!); loadMask() }
  catch { message.error('操作失败') }
}

// ==================== 扫描 ====================
const scanDrawerVisible = ref(false)
const scanning = ref(false)
const scanResult = ref<SecScanResultVO | null>(null)

const scanForm = reactive<SecSensitivityScanDTO>({
  dsId: undefined,
  scanScope: 'ALL',
  layerCode: undefined,
  tableNames: '',
  excludeSystemTables: true,
  scanColumnName: true,
  scanColumnComment: true,
  scanDataType: false,
  useRegex: true
})

function openScanDrawer() {
  scanResult.value = null
  scanForm.dsId = undefined
  scanForm.scanScope = 'ALL'
  scanForm.layerCode = undefined
  scanForm.tableNames = ''
  scanForm.excludeSystemTables = true
  scanForm.scanColumnName = true
  scanForm.scanColumnComment = true
  scanForm.scanDataType = false
  scanForm.useRegex = true
  scanDrawerVisible.value = true
}

async function doScan() {
  if (!scanForm.dsId) { message.warning('请选择数据源'); return }
  scanning.value = true
  try {
    const res = await scanSensitiveFieldsApi(scanForm)
    if (res.data?.success !== false) {
      scanResult.value = res.data?.data
    }
  } catch {
    message.error('扫描失败')
    scanResult.value = null
  } finally {
    scanning.value = false
  }
}

// ==================== 辅助方法 ====================
function getLevelColor(level?: string) {
  const map: Record<string, string> = { L1: '#52C41A', L2: '#FAAD14', L3: '#FA8C16', L4: '#F5222D' }
  return map[level || ''] || 'default'
}

function getMatchTypeColor(type?: string) {
  const map: Record<string, string> = { COLUMN_NAME: '#1677FF', COLUMN_COMMENT: '#722ED1', DATA_TYPE: '#13C2C2', REGEX: '#FA541C' }
  return map[type || ''] || 'default'
}

function getMaskTypeColor(type?: string) {
  const map: Record<string, string> = { NONE: '#8C8C8C', MASK: '#FAAD14', HIDE: '#722ED1', ENCRYPT: '#F5222D', DELETE: '#F5222D' }
  return map[type || ''] || 'default'
}

function getReviewBadge(status?: string) {
  const map: Record<string, string> = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'error' }
  return (map[status || ''] || 'default') as any
}

function getConfidenceColor(confidence?: number) {
  if (!confidence) return '#d9d9d9'
  if (confidence >= 80) return '#52C41A'
  if (confidence >= 50) return '#FAAD14'
  return '#F5222D'
}

// ==================== 初始化 ====================
async function loadStats() {
  try {
    const res = await getSecurityStats()
    if (res.data?.success !== false) {
      const s = res.data?.data
      statCards.value[0].value = s?.totalClassificationCount || 0
      statCards.value[1].value = s?.totalLevelCount || 0
      statCards.value[2].value = s?.totalRuleCount || 0
      statCards.value[3].value = s?.pendingReviewCount || 0
      statCards.value[4].value = s?.approvedCount || 0
      statCards.value[5].value = s?.totalMaskTemplateCount || 0
    }
  } catch { /* silent */ }
}

async function loadDataSources() {
  dsLoading.value = true
  try {
    const res = await dataSourceApi.listEnabled()
    if (res.data?.success !== false) dataSourceList.value = res.data || []
  } catch { /* silent */ }
  finally { dsLoading.value = false }
}

function onTabChange(tab: string) {
  if (tab === 'classification' && classData.value.length === 0) loadClassification()
  else if (tab === 'level' && levelData.value.length === 0) loadLevel()
  else if (tab === 'rule' && ruleData.value.length === 0) loadRule()
  else if (tab === 'sensitivity') {
    if (classData.value.length === 0) loadClassification()
    if (levelData.value.length === 0) loadLevel()
    if (sensData.value.length === 0) loadSensitivity()
  }
  else if (tab === 'mask' && maskData.value.length === 0) loadMask()
}

onMounted(() => {
  loadStats()
  loadDataSources()
  loadClassification()
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
.header-right {
  flex-shrink: 0;
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
.stat-mini-card {
  border-radius: 8px;
  padding: 16px 12px;
  color: #fff;
  min-height: 80px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.mini-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}
.mini-label {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 4px;
}
.empty-state {
  padding: 48px 0;
  text-align: center;
}
.empty-icon {
  font-size: 48px;
  color: #d9d9d9;
  margin-bottom: 12px;
  display: block;
}
.empty-title {
  font-size: 16px;
  color: #595959;
  margin-bottom: 8px;
}
.empty-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 16px;
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
}
.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
