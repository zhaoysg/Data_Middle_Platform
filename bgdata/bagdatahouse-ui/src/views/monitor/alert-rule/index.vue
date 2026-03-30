<template>
  <div class="page-container">

    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#alertRuleGrad)"/>
            <path d="M12 6V12L15 15" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="12" cy="12" r="8" stroke="white" stroke-width="1.5" fill="none" opacity="0.6"/>
            <defs>
              <linearGradient id="alertRuleGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#FA8C16"/>
                <stop offset="100%" stop-color="#FF4D4F"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">告警规则</h1>
          <p class="page-subtitle">配置告警规则，设置阈值、波动和通知条件，统一管理监控告警策略</p>
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
          新增规则
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
          placeholder="搜索规则名称"
          style="width: 200px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="filterRuleType"
          placeholder="规则类型"
          style="width: 160px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部类型</a-select-option>
          <a-select-option value="THRESHOLD">阈值告警</a-select-option>
          <a-select-option value="FLUCTUATION">波动告警</a-select-option>
          <a-select-option value="SYSTEM">系统告警</a-select-option>
          <a-select-option value="QUALITY">质量告警</a-select-option>
          <a-select-option value="SENSITIVE">敏感数据告警</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterTargetType"
          placeholder="目标类型"
          style="width: 140px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="TASK">任务</a-select-option>
          <a-select-option value="METRIC">指标</a-select-option>
          <a-select-option value="SYSTEM">系统</a-select-option>
          <a-select-option value="QUALITY">质量</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterAlertLevel"
          placeholder="告警级别"
          style="width: 130px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="INFO">信息</a-select-option>
          <a-select-option value="WARN">警告</a-select-option>
          <a-select-option value="ERROR">错误</a-select-option>
          <a-select-option value="CRITICAL">严重</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterEnabled"
          placeholder="状态"
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
        :scroll="{ x: 1400 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ruleType'">
            <a-tag :color="getRuleTypeColor(record.ruleType)">
              {{ getRuleTypeLabel(record.ruleType) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'targetType'">
            <a-tag :color="getTargetTypeColor(record.targetType)">
              {{ getTargetTypeLabel(record.targetType) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'alertLevel'">
            <a-tag :color="getAlertLevelColor(record.alertLevel)">
              {{ getAlertLevelLabel(record.alertLevel) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'condition'">
            <span class="mono-text">
              {{ getConditionText(record) }}
            </span>
          </template>

          <template v-if="column.key === 'enabled'">
            <a-switch
              :checked="record.enabled"
              size="small"
              @change="(checked: boolean) => handleToggle(record, checked)"
            />
          </template>

          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
                查看
              </a-button>
              <a-divider type="vertical" />
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-divider type="vertical" />
              <a-button type="link" size="small" danger @click="handleDelete(record)">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑抽屉（3步流程） -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="isEdit ? '编辑告警规则' : '新增告警规则'"
      :width="680"
      placement="right"
      :destroy-on-close="true"
      @close="drawerVisible = false"
    >
      <a-steps :current="currentStep" size="small" class="drawer-steps">
        <a-step title="基本信息" />
        <a-step title="配置触发条件" />
        <a-step title="配置通知" />
      </a-steps>

      <!-- Step 1: 基本信息 -->
      <div v-show="currentStep === 0" class="step-content">
        <a-form
          ref="formRef1"
          :model="formData"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="规则名称" name="ruleName" :rules="[{ required: true, message: '请输入规则名称' }]">
            <a-input v-model:value="formData.ruleName" placeholder="请输入规则名称，如：CPU使用率告警" />
          </a-form-item>
          <a-form-item label="规则编码" name="ruleCode">
            <a-input v-model:value="formData.ruleCode" placeholder="唯一编码，不填则自动生成" :disabled="isEdit" />
            <div class="form-tip" v-if="!formData.ruleCode && !isEdit">
              <CheckCircleOutlined style="color: #52C41A; margin-right: 4px" />
              将自动生成：ALERT_RULE_XXXX
            </div>
          </a-form-item>
          <a-form-item label="规则类型" name="ruleType" :rules="[{ required: true, message: '请选择规则类型' }]">
            <a-select v-model:value="formData.ruleType" placeholder="请选择规则类型">
              <a-select-option value="THRESHOLD">阈值告警</a-select-option>
              <a-select-option value="FLUCTUATION">波动告警</a-select-option>
              <a-select-option value="SYSTEM">系统告警</a-select-option>
              <a-select-option value="QUALITY">质量告警</a-select-option>
              <a-select-option value="SENSITIVE">敏感数据告警</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="告警级别" name="alertLevel" :rules="[{ required: true, message: '请选择告警级别' }]">
            <a-select v-model:value="formData.alertLevel" placeholder="请选择告警级别">
              <a-select-option value="INFO">
                <span><span class="level-dot level-info"></span>信息</span>
              </a-select-option>
              <a-select-option value="WARN">
                <span><span class="level-dot level-warn"></span>警告</span>
              </a-select-option>
              <a-select-option value="ERROR">
                <span><span class="level-dot level-error"></span>错误</span>
              </a-select-option>
              <a-select-option value="CRITICAL">
                <span><span class="level-dot level-critical"></span>严重</span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="启用状态" name="enabled">
            <a-switch v-model:checked="formData.enabled" />
            <span class="form-tip-inline">{{ formData.enabled ? '规则将在保存后生效' : '规则已禁用，不触发告警' }}</span>
          </a-form-item>
        </a-form>
      </div>

      <!-- Step 2: 配置触发条件 -->
      <div v-show="currentStep === 1" class="step-content">
        <div class="condition-guide">
          <div class="guide-title">
            <InfoCircleOutlined /> 触发条件说明
          </div>
          <p>根据规则类型配置不同的触发条件。条件满足时将触发告警。</p>
        </div>

        <a-form
          ref="formRef2"
          :model="formData"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="目标类型" name="targetType" :rules="[{ required: true, message: '请选择目标类型' }]">
            <a-select v-model:value="formData.targetType" placeholder="请选择目标类型">
              <a-select-option value="TASK">任务</a-select-option>
              <a-select-option value="METRIC">监控指标</a-select-option>
              <a-select-option value="SYSTEM">系统资源</a-select-option>
              <a-select-option value="QUALITY">数据质量</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            v-if="formData.targetType === 'TASK' || formData.targetType === 'METRIC' || formData.targetType === 'QUALITY'"
            label="选择目标"
            name="targetId"
          >
            <a-select
              v-model:value="formData.targetId"
              placeholder="请先选择目标类型"
              show-search
              :loading="targetLoading"
            >
              <a-select-option v-for="t in targetList" :key="t.id" :value="t.id">
                {{ t.name || t.ruleName || t.taskName || t.metricName || t.id }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <!-- 阈值条件 -->
          <template v-if="formData.ruleType === 'THRESHOLD'">
            <a-form-item label="比较方式" name="conditionType" :rules="[{ required: true, message: '请选择比较方式' }]">
              <a-select v-model:value="formData.conditionType" placeholder="请选择比较方式">
                <a-select-option value="GT">大于（>）</a-select-option>
                <a-select-option value="LT">小于（<）</a-select-option>
                <a-select-option value="EQ">等于（=）</a-select-option>
                <a-select-option value="GE">大于等于（>=）</a-select-option>
                <a-select-option value="LE">小于等于（<=）</a-select-option>
                <a-select-option value="NE">不等于（!=）</a-select-option>
                <a-select-option value="BETWEEN">区间（between）</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="阈值" name="thresholdValue" :rules="[{ required: true, message: '请输入阈值' }]">
              <a-input-number
                v-model:value="formData.thresholdValue"
                placeholder="请输入阈值"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item
              v-if="formData.conditionType === 'BETWEEN'"
              label="最大阈值"
              name="thresholdMaxValue"
              :rules="[{ required: true, message: '请输入最大阈值' }]"
            >
              <a-input-number
                v-model:value="formData.thresholdMaxValue"
                placeholder="请输入最大阈值"
                style="width: 100%"
              />
            </a-form-item>
          </template>

          <!-- 波动条件 -->
          <template v-if="formData.ruleType === 'FLUCTUATION'">
            <a-form-item label="比较周期" name="comparisonType" :rules="[{ required: true, message: '请选择比较周期' }]">
              <a-select v-model:value="formData.comparisonType" placeholder="请选择比较周期">
                <a-select-option value="D_DAY">环比（昨天）</a-select-option>
                <a-select-option value="W_WEEK">环比（上周）</a-select-option>
                <a-select-option value="M_MONTH">环比（上月）</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="波动阈值%" name="fluctuationPct" :rules="[{ required: true, message: '请输入波动阈值百分比' }]">
              <a-input-number
                v-model:value="formData.fluctuationPct"
                placeholder="如 20 表示波动超过 20% 时告警"
                :min="0"
                :max="100"
                style="width: 100%"
              />
              <div class="form-tip">填写百分比数值，如 20 表示波动超过 ±20% 时触发告警</div>
            </a-form-item>
          </template>

          <!-- 系统条件 -->
          <template v-if="formData.ruleType === 'SYSTEM'">
            <a-form-item label="监控指标" name="targetId">
              <a-select v-model:value="formData.targetId" placeholder="选择系统监控指标">
                <a-select-option value="cpu">CPU 使用率</a-select-option>
                <a-select-option value="memory">内存使用率</a-select-option>
                <a-select-option value="disk">磁盘使用率</a-select-option>
                <a-select-option value="db_conn">数据库连接数</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="阈值" name="thresholdValue" :rules="[{ required: true, message: '请输入阈值' }]">
              <a-input-number v-model:value="formData.thresholdValue" placeholder="请输入阈值（%）" :min="0" :max="100" style="width: 100%" />
            </a-form-item>
            <a-form-item label="比较方式" name="conditionType">
              <a-select v-model:value="formData.conditionType" placeholder="大于阈值告警">
                <a-select-option value="GT">大于（>）</a-select-option>
                <a-select-option value="LT">小于（<）</a-select-option>
              </a-select>
            </a-form-item>
          </template>

          <!-- SENSITIVE 类型专属配置 -->
          <template v-if="formData.ruleType === 'SENSITIVE'">
            <div class="sensitive-config-guide">
              <div class="guide-title">
                <SafetyCertificateOutlined /> SENSITIVE 类型配置说明
              </div>
              <p>敏感数据告警包含 4 种子类型：敏感字段突增、敏感等级变更、敏感字段访问异常、敏感字段待审核超期。</p>
            </div>

            <a-form-item label="敏感子类型" name="sensitivitySubType" :rules="[{ required: true, message: '请选择敏感告警子类型' }]">
              <a-select v-model:value="formData.sensitivitySubType" placeholder="请选择敏感告警子类型">
                <a-select-option value="SENSITIVE_FIELD_SPIKE">
                  <span><SafetyOutlined style="color: #FF4D4F" /> 敏感字段突增 — 扫描发现敏感字段数较昨日增长超过阈值</span>
                </a-select-option>
                <a-select-option value="SENSITIVE_LEVEL_CHANGE">
                  <span><ArrowDownOutlined style="color: #FA8C16" /> 敏感等级变更 — 字段敏感等级被降级时触发</span>
                </a-select-option>
                <a-select-option value="SENSITIVE_ACCESS_ANOMALY">
                  <span><AlertOutlined style="color: #722ED1" /> 敏感字段访问异常 — 非工作时间或异常频率访问敏感字段</span>
                </a-select-option>
                <a-select-option value="SENSITIVE_UNREVIEWED_LONG">
                  <span><ClockCircleOutlined style="color: #FAAD14" /> 待审核超期 — 敏感字段待审核超过 N 天时触发</span>
                </a-select-option>
              </a-select>
            </a-form-item>

            <!-- SENSITIVE_FIELD_SPIKE 配置 -->
            <template v-if="formData.sensitivitySubType === 'SENSITIVE_FIELD_SPIKE'">
              <a-form-item label="告警级别" name="alertLevel" :rules="[{ required: true, message: '请选择告警级别' }]">
                <a-select v-model:value="formData.alertLevel" placeholder="请选择告警级别">
                  <a-select-option value="INFO"><span><span class="level-dot level-info"></span>信息</span></a-select-option>
                  <a-select-option value="WARN"><span><span class="level-dot level-warn"></span>警告</span></a-select-option>
                  <a-select-option value="ERROR"><span><span class="level-dot level-error"></span>错误</span></a-select-option>
                  <a-select-option value="CRITICAL"><span><span class="level-dot level-critical"></span>严重</span></a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="突增阈值%" name="spikeThresholdPct" :rules="[{ required: true, message: '请输入突增阈值百分比' }]">
                <a-input-number v-model:value="formData.spikeThresholdPct" :min="1" :max="100" placeholder="默认 20" style="width: 100%" />
                <div class="form-tip">填写百分比，如 20 表示敏感字段数较上次扫描增长超过 20% 时触发告警</div>
              </a-form-item>
            </template>

            <!-- SENSITIVE_LEVEL_CHANGE 配置 -->
            <template v-if="formData.sensitivitySubType === 'SENSITIVE_LEVEL_CHANGE'">
              <a-form-item label="告警级别" name="alertLevel" :rules="[{ required: true, message: '请选择告警级别' }]">
                <a-select v-model:value="formData.alertLevel" placeholder="请选择告警级别">
                  <a-select-option value="ERROR"><span><span class="level-dot level-error"></span>错误</span></a-select-option>
                  <a-select-option value="CRITICAL"><span><span class="level-dot level-critical"></span>严重</span></a-select-option>
                </a-select>
                <div class="form-tip form-tip-warning">敏感等级变更告警默认为 ERROR 级别，等级降级时触发</div>
              </a-form-item>
              <a-form-item label="触发等级" name="sensitivityLevel">
                <a-select v-model:value="formData.sensitivityLevel" placeholder="留空则监控所有等级变更">
                  <a-select-option value="L4">L4 机密</a-select-option>
                  <a-select-option value="L3">L3 敏感</a-select-option>
                  <a-select-option value="L2">L2 内部</a-select-option>
                  <a-select-option value="L1">L1 公开</a-select-option>
                </a-select>
                <div class="form-tip">留空表示监控所有等级变更；选择具体等级表示仅监控该等级及以下变更</div>
              </a-form-item>
            </template>

            <!-- SENSITIVE_ACCESS_ANOMALY 配置 -->
            <template v-if="formData.sensitivitySubType === 'SENSITIVE_ACCESS_ANOMALY'">
              <a-form-item label="告警级别" name="alertLevel" :rules="[{ required: true, message: '请选择告警级别' }]">
                <a-select v-model:value="formData.alertLevel" placeholder="请选择告警级别">
                  <a-select-option value="WARN"><span><span class="level-dot level-warn"></span>警告</span></a-select-option>
                  <a-select-option value="ERROR"><span><span class="level-dot level-error"></span>错误</span></a-select-option>
                  <a-select-option value="CRITICAL"><span><span class="level-dot level-critical"></span>严重</span></a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="非工作时间" name="accessAnomalyOffHours">
                <a-input v-model:value="formData.accessAnomalyOffHours" placeholder="22:00-08:00" />
                <div class="form-tip">非工作时间定义，多个时段用逗号分隔，如 "22:00-08:00, 周末全天"</div>
              </a-form-item>
              <a-form-item label="访问次数阈值" name="accessAnomalyThreshold">
                <a-input-number v-model:value="formData.accessAnomalyThreshold" :min="1" :max="100" placeholder="默认 5" style="width: 100%" />
                <div class="form-tip">非工作时间内访问次数超过此阈值时触发告警</div>
              </a-form-item>
            </template>

            <!-- SENSITIVE_UNREVIEWED_LONG 配置 -->
            <template v-if="formData.sensitivitySubType === 'SENSITIVE_UNREVIEWED_LONG'">
              <a-form-item label="告警级别" name="alertLevel" :rules="[{ required: true, message: '请选择告警级别' }]">
                <a-select v-model:value="formData.alertLevel" placeholder="请选择告警级别">
                  <a-select-option value="INFO"><span><span class="level-dot level-info"></span>信息</span></a-select-option>
                  <a-select-option value="WARN"><span><span class="level-dot level-warn"></span>警告</span></a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="超期天数" name="unreviewThresholdDays" :rules="[{ required: true, message: '请输入超期天数' }]">
                <a-input-number v-model:value="formData.unreviewThresholdDays" :min="1" :max="365" placeholder="默认 7" style="width: 100%" />
                <div class="form-tip">敏感字段待审核超过此天数时触发告警，默认 7 天</div>
              </a-form-item>
            </template>
          </template>

          <a-form-item label="连续触发次数" name="consecutiveTriggers">
            <a-input-number
              v-model:value="formData.consecutiveTriggers"
              :min="1"
              :max="10"
              placeholder="连续触发次数"
              style="width: 200px"
            />
            <div class="form-tip">连续触发 N 次才告警，防止误报。默认 1 次</div>
          </a-form-item>

          <a-form-item label="静默截止" name="muteUntil">
            <a-date-picker
              v-model:value="muteUntilDate"
              show-time
              placeholder="选填，留空则不静默"
              style="width: 100%"
              @change="onMuteUntilChange"
            />
            <div class="form-tip">设置静默截止时间，静默期内该规则不触发告警</div>
          </a-form-item>
        </a-form>
      </div>

      <!-- Step 3: 配置通知 -->
      <div v-show="currentStep === 2" class="step-content">
        <div class="notify-guide">
          <div class="guide-title">
            <BellOutlined /> 通知配置说明
          </div>
          <p>配置告警的接收人和通知方式。当前版本仅记录通知信息，暂不实现具体通知渠道。</p>
        </div>

        <a-form
          ref="formRef3"
          :model="formData"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="告警接收人" name="alertReceivers" :rules="[{ required: true, message: '请输入告警接收人' }]">
            <a-input
              v-model:value="formData.alertReceivers"
              placeholder="输入用户名，多个用逗号分隔"
            />
            <div class="form-tip">输入接收告警通知的用户名，多个用户用逗号分隔</div>
          </a-form-item>

          <a-form-item label="通知渠道" name="alertChannels">
            <a-checkbox-group v-model:value="formData.alertChannels">
              <a-checkbox value="INNER">站内通知</a-checkbox>
              <a-checkbox value="EMAIL">邮件（预留）</a-checkbox>
              <a-checkbox value="DINGTALK">钉钉（预留）</a-checkbox>
              <a-checkbox value="WECHAT">企业微信（预留）</a-checkbox>
            </a-checkbox-group>
            <div class="form-tip form-tip-warning">
              <InfoCircleOutlined style="margin-right: 4px" />
              邮件、钉钉、企业微信通道为预留功能，当前版本仅支持站内通知
            </div>
          </a-form-item>

          <a-divider>消息模板</a-divider>

          <a-form-item label="告警标题" name="alertTitleTemplate" :rules="[{ required: true, message: '请输入告警标题模板' }]">
            <a-input
              v-model:value="formData.alertTitleTemplate"
              placeholder="如：【${alertLevel}】${ruleName} 触发告警"
            />
            <div class="form-tip">
              支持变量：<code>${rule_name}</code>、<code>${alert_level}</code>、<code>${trigger_value}</code>、<code>${threshold}</code>、<code>${target_name}</code>
            </div>
          </a-form-item>

          <a-form-item label="告警内容" name="alertContentTemplate">
            <a-textarea
              v-model:value="formData.alertContentTemplate"
              placeholder="选填，自定义告警内容"
              :rows="4"
            />
          </a-form-item>
        </a-form>

        <!-- 预览 -->
        <div v-if="formData.alertTitleTemplate" class="preview-section">
          <div class="preview-title">
            <EyeOutlined /> 告警预览
          </div>
          <div class="alert-preview">
            <div class="preview-badge" :class="'badge-' + (formData.alertLevel || 'WARN').toLowerCase()">
              {{ getAlertLevelLabel(formData.alertLevel) }}
            </div>
            <div class="preview-title-text">
              {{ getPreviewTitle() }}
            </div>
            <div class="preview-meta">
              <span>规则：{{ formData.ruleName || '未设置' }}</span>
              <span>时间：{{ new Date().toLocaleString('zh-CN') }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 抽屉底部 -->
      <div class="drawer-footer">
        <a-space>
          <a-button v-if="currentStep > 0" @click="currentStep--">
            <template #icon><LeftOutlined /></template>
            上一步
          </a-button>
          <a-button v-if="currentStep < 2" type="primary" @click="nextStep">
            下一步
            <template #icon><RightOutlined /></template>
          </a-button>
          <a-button
            v-if="currentStep === 2"
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
      <template v-if="viewRecord">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="规则编码">
            <code>{{ viewRecord.ruleCode }}</code>
          </a-descriptions-item>
          <a-descriptions-item label="规则名称">{{ viewRecord.ruleName }}</a-descriptions-item>
          <a-descriptions-item label="规则类型">
            <a-tag :color="getRuleTypeColor(viewRecord.ruleType)">
              {{ getRuleTypeLabel(viewRecord.ruleType) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="目标类型">
            <a-tag :color="getTargetTypeColor(viewRecord.targetType)">
              {{ getTargetTypeLabel(viewRecord.targetType) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="告警级别">
            <a-tag :color="getAlertLevelColor(viewRecord.alertLevel)">
              {{ getAlertLevelLabel(viewRecord.alertLevel) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="触发条件">
            <span class="mono-text">{{ getConditionText(viewRecord) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="连续触发次数">{{ viewRecord.consecutiveTriggers || 1 }} 次</a-descriptions-item>
          <a-descriptions-item label="启用状态">
            <a-badge :status="viewRecord.enabled ? 'success' : 'default'" :text="viewRecord.enabled ? '已启用' : '已禁用'" />
          </a-descriptions-item>
          <a-descriptions-item label="告警接收人">{{ viewRecord.alertReceivers || '-' }}</a-descriptions-item>
          <a-descriptions-item label="通知渠道">{{ viewRecord.alertChannels || '-' }}</a-descriptions-item>
          <a-descriptions-item label="静默截止时间">{{ viewRecord.muteUntil || '未设置' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ viewRecord.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ viewRecord.updateTime || '-' }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-drawer>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, SearchOutlined, ReloadOutlined, EyeOutlined,
  EditOutlined, DeleteOutlined, LeftOutlined, RightOutlined,
  CheckOutlined, InfoCircleOutlined, BellOutlined,
  CheckCircleOutlined, SafetyCertificateOutlined, SafetyOutlined,
  AlertOutlined, ClockCircleOutlined, ArrowDownOutlined
} from '@ant-design/icons-vue'
import { alertRuleApi } from '@/api/monitor'
import type { AlertRule, AlertRuleDTO } from '@/api/monitor'

// ============ 状态 ============
const loading = ref(false)
const tableData = ref<AlertRule[]>([])
const selectedRowKeys = ref<number[]>([])
const targetLoading = ref(false)
const targetList = ref<any[]>([])

const searchName = ref('')
const filterRuleType = ref<string>()
const filterTargetType = ref<string>()
const filterAlertLevel = ref<string>()
const filterEnabled = ref<number>()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const statCards = ref([
  { label: '规则总数', value: 0, bg: 'linear-gradient(135deg, #FA8C16 0%, #FFC53D 100%)' },
  { label: '阈值告警', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #69C0FF 100%)' },
  { label: '波动告警', value: 0, bg: 'linear-gradient(135deg, #722ED1 0%, #9254DE 100%)' },
  { label: '已启用', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' }
])

const hasSelected = computed(() => selectedRowKeys.value.length > 0)

// ============ 表格列 ============
const columns = [
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 200, ellipsis: true },
  { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode', width: 160 },
  { title: '规则类型', key: 'ruleType', width: 110 },
  { title: '目标类型', key: 'targetType', width: 100 },
  { title: '告警级别', key: 'alertLevel', width: 90 },
  { title: '触发条件', key: 'condition', width: 180 },
  { title: '启用状态', key: 'enabled', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'actions', width: 200, fixed: 'right' }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => { selectedRowKeys.value = keys }
}))

// ============ 抽屉 & 表单 ============
const drawerVisible = ref(false)
const viewDrawerVisible = ref(false)
const currentStep = ref(0)
const submitLoading = ref(false)
const isEdit = ref(false)
const editingId = ref<number>()
const viewRecord = ref<AlertRule | null>(null)
const muteUntilDate = ref<any>(null)

const formRef1 = ref()
const formRef2 = ref()
const formRef3 = ref()

const formData = reactive<Partial<AlertRuleDTO & { alertChannels: string[]; sensitivityLevel: string; sensitivityDsId: number; sensitivityTable: string; sensitivityColumn: string; spikeThresholdPct: number; unreviewThresholdDays: number; accessAnomalyOffHours: string; accessAnomalyThreshold: number; sensitivitySubType: string }>>({
  ruleName: '',
  ruleCode: '',
  ruleType: undefined,
  targetType: undefined,
  targetId: undefined,
  conditionType: undefined,
  thresholdValue: undefined,
  thresholdMaxValue: undefined,
  fluctuationPct: undefined,
  comparisonType: undefined,
  consecutiveTriggers: 1,
  alertLevel: undefined,
  alertReceivers: '',
  alertChannels: ['INNER'],
  alertTitleTemplate: '',
  alertContentTemplate: '',
  muteUntil: '',
  enabled: true,
  sensitivityLevel: undefined,
  sensitivityDsId: undefined,
  sensitivityTable: '',
  sensitivityColumn: '',
  spikeThresholdPct: 20,
  unreviewThresholdDays: 7,
  accessAnomalyOffHours: '22:00-08:00',
  accessAnomalyThreshold: 5,
  sensitivitySubType: undefined
})

// ============ 数据加载 ============
async function loadData() {
  loading.value = true
  try {
    const res: any = await alertRuleApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ruleName: searchName.value || undefined,
      ruleType: filterRuleType.value || undefined,
      targetType: filterTargetType.value || undefined,
      alertLevel: filterAlertLevel.value || undefined,
      enabled: filterEnabled.value
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0

    // 统计
    statCards.value[0].value = pagination.total
    statCards.value[1].value = tableData.value.filter(r => r.ruleType === 'THRESHOLD').length
    statCards.value[2].value = tableData.value.filter(r => r.ruleType === 'FLUCTUATION').length
    statCards.value[3].value = tableData.value.filter(r => r.enabled).length
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function loadTargetList(targetType: string) {
  if (!targetType) { targetList.value = []; return }
  targetLoading.value = true
  try {
    const res: any = await alertRuleApi.getTargetListByType(targetType)
    targetList.value = res.data?.data || []
  } catch {
    targetList.value = []
  } finally {
    targetLoading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function resetFilters() {
  searchName.value = ''
  filterRuleType.value = undefined
  filterTargetType.value = undefined
  filterAlertLevel.value = undefined
  filterEnabled.value = undefined
  pagination.current = 1
  loadData()
}

// ============ 表单操作 ============
function openCreateDrawer() {
  isEdit.value = false
  editingId.value = undefined
  currentStep.value = 0
  resetForm()
  drawerVisible.value = true
}

function handleEdit(record: AlertRule) {
  isEdit.value = true
  editingId.value = record.id
  currentStep.value = 0

  Object.assign(formData, {
    ruleName: record.ruleName,
    ruleCode: record.ruleCode,
    ruleType: record.ruleType,
    targetType: record.targetType,
    targetId: record.targetId,
    conditionType: record.conditionType,
    thresholdValue: record.thresholdValue,
    thresholdMaxValue: record.thresholdMaxValue,
    fluctuationPct: record.fluctuationPct,
    comparisonType: record.comparisonType,
    consecutiveTriggers: record.consecutiveTriggers || 1,
    alertLevel: record.alertLevel,
    alertReceivers: record.alertReceivers,
    alertChannels: record.alertChannels ? record.alertChannels.split(',') : ['INNER'],
    alertTitleTemplate: record.alertTitleTemplate,
    alertContentTemplate: record.alertContentTemplate,
    muteUntil: record.muteUntil,
    enabled: record.enabled,
    sensitivityLevel: (record as any).sensitivityLevel,
    sensitivityDsId: (record as any).sensitivityDsId,
    sensitivityTable: (record as any).sensitivityTable || '',
    sensitivityColumn: (record as any).sensitivityColumn || '',
    spikeThresholdPct: (record as any).spikeThresholdPct || 20,
    unreviewThresholdDays: (record as any).unreviewThresholdDays || 7,
    accessAnomalyOffHours: (record as any).accessAnomalyOffHours || '22:00-08:00',
    accessAnomalyThreshold: (record as any).accessAnomalyThreshold || 5,
    sensitivitySubType: (record as any).sensitivitySubType || (record as any).ruleType
  })

  if (record.muteUntil) {
    muteUntilDate.value = record.muteUntil
  }

  drawerVisible.value = true
}

function handleView(record: AlertRule) {
  viewRecord.value = record
  viewDrawerVisible.value = true
}

async function handleDelete(record: AlertRule) {
  const { Modal } = await import('ant-design-vue')
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除规则「${record.ruleName}」吗？删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        await alertRuleApi.delete(record.id!)
        message.success('删除成功')
        loadData()
      } catch {
        message.error('删除失败')
      }
    }
  })
}

async function handleBatchDelete() {
  const { Modal } = await import('ant-design-vue')
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个规则吗？`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        await alertRuleApi.batchDelete(selectedRowKeys.value)
        message.success('批量删除成功')
        selectedRowKeys.value = []
        loadData()
      } catch {
        message.error('批量删除失败')
      }
    }
  })
}

async function handleToggle(record: AlertRule, checked: boolean) {
  try {
    await alertRuleApi.toggle(record.id!)
    record.enabled = checked
    message.success(checked ? '已启用' : '已禁用')
    loadData()
  } catch {
    message.error('操作失败')
  }
}

function resetForm() {
  Object.assign(formData, {
    ruleName: '', ruleCode: '', ruleType: undefined,
    targetType: undefined, targetId: undefined,
    conditionType: undefined, thresholdValue: undefined,
    thresholdMaxValue: undefined, fluctuationPct: undefined,
    comparisonType: undefined, consecutiveTriggers: 1,
    alertLevel: undefined, alertReceivers: '',
    alertChannels: ['INNER'],
    alertTitleTemplate: '', alertContentTemplate: '',
    muteUntil: '', enabled: true,
    sensitivityLevel: undefined, sensitivityDsId: undefined,
    sensitivityTable: '', sensitivityColumn: '',
    spikeThresholdPct: 20, unreviewThresholdDays: 7,
    accessAnomalyOffHours: '22:00-08:00', accessAnomalyThreshold: 5,
    sensitivitySubType: undefined
  })
  muteUntilDate.value = null
  targetList.value = []
}

async function nextStep() {
  if (currentStep.value === 0) {
    try { await formRef1.value?.validate() } catch { return }
    if (!formData.ruleType) { message.warning('请选择规则类型'); return }
    if (formData.ruleType === 'SENSITIVE') {
      if (!formData.alertLevel) { message.warning('请选择告警级别'); return }
    }
    currentStep.value++
  } else if (currentStep.value === 1) {
    try { await formRef2.value?.validate() } catch { return }
    if (formData.ruleType === 'SENSITIVE' && !formData.sensitivitySubType) {
      message.warning('请选择敏感告警子类型'); return
    }
    currentStep.value++
  } else {
    currentStep.value++
  }
}

function onMuteUntilChange(_date: any, dateString: string) {
  formData.muteUntil = dateString
}

async function handleSubmit() {
  try { await formRef3.value?.validate() } catch { return }

  submitLoading.value = true
  try {
    const payload: AlertRuleDTO = {
      ruleName: formData.ruleName,
      ruleCode: formData.ruleCode,
      ruleType: formData.ruleType,
      targetType: formData.targetType,
      targetId: formData.targetId,
      conditionType: formData.conditionType,
      thresholdValue: formData.thresholdValue,
      thresholdMaxValue: formData.thresholdMaxValue,
      fluctuationPct: formData.fluctuationPct,
      comparisonType: formData.comparisonType,
      consecutiveTriggers: formData.consecutiveTriggers,
      alertLevel: formData.alertLevel,
      alertReceivers: formData.alertReceivers,
      alertChannels: (formData.alertChannels || []).join(','),
      alertTitleTemplate: formData.alertTitleTemplate,
      alertContentTemplate: formData.alertContentTemplate,
      muteUntil: formData.muteUntil,
      enabled: formData.enabled,
      sensitivityLevel: formData.sensitivityLevel,
      sensitivityDsId: formData.sensitivityDsId,
      sensitivityTable: formData.sensitivityTable,
      sensitivityColumn: formData.sensitivityColumn,
      spikeThresholdPct: formData.spikeThresholdPct,
      unreviewThresholdDays: formData.unreviewThresholdDays,
      accessAnomalyOffHours: formData.accessAnomalyOffHours,
      accessAnomalyThreshold: formData.accessAnomalyThreshold
    }

    if (isEdit.value) {
      await alertRuleApi.update(editingId.value!, payload)
      message.success('修改成功')
    } else {
      await alertRuleApi.create(payload)
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

// ============ 辅助函数 ============
function getRuleTypeColor(type: string | undefined) {
  const map: Record<string, string> = {
    THRESHOLD: 'blue', FLUCTUATION: 'purple', SYSTEM: 'orange', QUALITY: 'cyan',
    SENSITIVE: 'red', SENSITIVE_FIELD_SPIKE: 'red', SENSITIVE_LEVEL_CHANGE: 'volcano',
    SENSITIVE_ACCESS_ANOMALY: 'orange', SENSITIVE_UNREVIEWED_LONG: 'gold'
  }
  return map[type || ''] || 'default'
}

function getRuleTypeLabel(type: string | undefined) {
  const map: Record<string, string> = {
    THRESHOLD: '阈值告警', FLUCTUATION: '波动告警', SYSTEM: '系统告警', QUALITY: '质量告警',
    SENSITIVE: '敏感数据告警', SENSITIVE_FIELD_SPIKE: '字段突增', SENSITIVE_LEVEL_CHANGE: '等级变更',
    SENSITIVE_ACCESS_ANOMALY: '访问异常', SENSITIVE_UNREVIEWED_LONG: '待审核超期'
  }
  return map[type || ''] || type || '-'
}

function getTargetTypeColor(type: string | undefined) {
  const map: Record<string, string> = {
    TASK: 'blue', METRIC: 'purple', SYSTEM: 'orange', QUALITY: 'cyan'
  }
  return map[type || ''] || 'default'
}

function getTargetTypeLabel(type: string | undefined) {
  const map: Record<string, string> = {
    TASK: '任务', METRIC: '监控指标', SYSTEM: '系统资源', QUALITY: '数据质量'
  }
  return map[type || ''] || type || '-'
}

function getAlertLevelColor(level: string | undefined) {
  const map: Record<string, string> = {
    INFO: 'blue', WARN: 'warning', WARNING: 'warning', ERROR: 'error', CRITICAL: 'error'
  }
  return map[level || ''] || 'default'
}

function getAlertLevelLabel(level: string | undefined) {
  const map: Record<string, string> = {
    INFO: '信息', WARN: '警告', WARNING: '警告', ERROR: '错误', CRITICAL: '严重'
  }
  return map[level || ''] || level || '-'
}

function getConditionText(record: AlertRule) {
  if (record.ruleType === 'THRESHOLD') {
    const op = { GT: '>', LT: '<', EQ: '=', GE: '>=', LE: '<=', NE: '!=', BETWEEN: '~' }[record.conditionType || ''] || ''
    if (record.conditionType === 'BETWEEN') {
      return `${op} ${record.thresholdValue} ~ ${record.thresholdMaxValue}`
    }
    return `${op} ${record.thresholdValue}`
  }
  if (record.ruleType === 'FLUCTUATION') {
    const comp = { D_DAY: '环比昨天', W_WEEK: '环比上周', M_MONTH: '环比上月' }[record.comparisonType || ''] || ''
    return `${comp} 波动 > ${record.fluctuationPct}%`
  }
  if (record.ruleType === 'SYSTEM') {
    return `> ${record.thresholdValue}%`
  }
  return '-'
}

function getPreviewTitle() {
  const title = formData.alertTitleTemplate || ''
  return title
    .replace(/\$\{rule_name\}/g, formData.ruleName || '规则名称')
    .replace(/\$\{alert_level\}/g, getAlertLevelLabel(formData.alertLevel))
    .replace(/\$\{trigger_value\}/g, String(formData.thresholdValue ?? '-'))
    .replace(/\$\{threshold\}/g, String(formData.thresholdValue ?? '-'))
    .replace(/\$\{target_name\}/g, '-')
}

onMounted(() => {
  loadData()
})
</script>

<style lang="less" scoped>
.page-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px;
  .header-left { display: flex; align-items: center; gap: 16px; }
  .header-icon { flex-shrink: 0; }
  .page-title { font-size: 22px; font-weight: 700; color: #1F1F1F; margin: 0; }
  .page-subtitle { font-size: 13px; color: #8C8C8C; margin: 4px 0 0; }
  .header-right { display: flex; gap: 12px; }
}

.stat-mini-card {
  border-radius: 10px; padding: 16px 20px;
  color: white; box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  .mini-value { font-size: 26px; font-weight: 700; }
  .mini-label { font-size: 13px; opacity: 0.85; margin-top: 4px; }
}

.filter-bar {
  background: white; border-radius: 10px;
  padding: 16px 20px; margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.table-card {
  background: white; border-radius: 10px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px; }

.drawer-steps { margin-bottom: 28px; padding: 0 20px; }
.step-content { min-height: 400px; }

.form-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; }
.form-tip-warning { color: #FA8C16; }

.form-tip-inline { margin-left: 12px; font-size: 12px; color: #8C8C8C; }

.level-dot {
  display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px;
  &.level-info { background: #1677FF; }
  &.level-warn { background: #FA8C16; }
  &.level-error { background: #FF4D4F; }
  &.level-critical { background: #722ED1; }
}

.condition-guide, .notify-guide, .sensitive-config-guide {
  background: #FFF7E6; border: 1px solid #FFD591;
  border-radius: 8px; padding: 16px; margin-bottom: 20px;
  .guide-title { font-size: 14px; font-weight: 600; color: #FA8C16; margin-bottom: 8px; }
  p { margin: 0; font-size: 13px; color: #333; }
}

.preview-section {
  margin-top: 16px;
  .preview-title { font-size: 14px; font-weight: 600; color: #1F1F1F; margin-bottom: 12px; }
}

.alert-preview {
  background: #FFF7E6; border: 1px solid #FFE58F;
  border-radius: 8px; padding: 16px;
  .preview-badge {
    display: inline-block; font-size: 11px; font-weight: 600;
    padding: 2px 8px; border-radius: 4px; margin-bottom: 8px;
    &.badge-info { background: #E6F7FF; color: #1677FF; }
    &.badge-warn, &.badge-warning { background: #FFFBE6; color: #FA8C16; }
    &.badge-error { background: #FFF1F0; color: #FF4D4F; }
    &.badge-critical { background: #F9F0FF; color: #722ED1; }
  }
  .preview-title-text { font-size: 15px; font-weight: 600; color: #1F1F1F; margin-bottom: 8px; }
  .preview-meta { display: flex; gap: 16px; font-size: 12px; color: #8C8C8C; }
}

.drawer-footer {
  position: absolute; bottom: 0; left: 0; right: 0;
  padding: 16px 24px; border-top: 1px solid #E8E8E8;
  background: white; display: flex; justify-content: space-between; align-items: center;
}

code { background: #F5F7FA; padding: 1px 4px; border-radius: 3px; font-family: 'JetBrains Mono', monospace; font-size: 12px; }
</style>
