package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DprofileTask;
import org.dromara.metadata.domain.bo.DprofileTaskBo;
import org.dromara.metadata.domain.vo.DprofileTaskVo;
import org.dromara.metadata.mapper.DprofileTaskMapper;
import org.dromara.metadata.service.IDprofileService;
import org.dromara.metadata.service.IDprofileTaskService;
import org.dromara.metadata.support.DatasourceHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据探查任务服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class DprofileTaskServiceImpl implements IDprofileTaskService {

    private final DprofileTaskMapper taskMapper;
    private final DatasourceHelper datasourceHelper;
    private final IDprofileService dprofileService;

    @Override
    public TableDataInfo<DprofileTaskVo> queryPageList(DprofileTaskVo vo, PageQuery pageQuery) {
        Wrapper<DprofileTask> wrapper = buildQueryWrapper(vo);
        var page = taskMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public DprofileTaskVo queryById(Long id) {
        return taskMapper.selectVoById(id);
    }

    @Override
    public Long insertByBo(DprofileTaskBo bo) {
        DprofileTask task = new DprofileTask();
        task.setTaskCode(generateTaskCode());
        task.setTaskName(bo.getTaskName());
        task.setTaskDesc(bo.getTaskDesc());
        task.setDsId(bo.getDsId());
        task.setProfileLevel(bo.getProfileLevel() != null ? bo.getProfileLevel() : "DETAILED");
        task.setTablePattern(bo.getTablePattern());
        task.setTriggerType(bo.getTriggerType() != null ? bo.getTriggerType() : "MANUAL");
        task.setCronExpr(bo.getCronExpr());
        task.setTargetColumns(bo.getTargetColumns());
        task.setStatus("DRAFT");
        task.setTotalTables(0);

        taskMapper.insert(task);
        log.info("创建探查任务成功: id={}, taskCode={}, taskName={}",
            task.getId(), task.getTaskCode(), task.getTaskName());

        return task.getId();
    }

    @Override
    public void updateByBo(DprofileTaskBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("任务ID不能为空");
        }

        DprofileTask task = taskMapper.selectById(bo.getId());
        if (task == null) {
            throw new ServiceException("任务不存在");
        }

        if ("RUNNING".equals(task.getStatus())) {
            throw new ServiceException("任务正在运行中，不允许修改");
        }

        task.setTaskName(bo.getTaskName());
        task.setTaskDesc(bo.getTaskDesc());
        task.setDsId(bo.getDsId());
        task.setProfileLevel(bo.getProfileLevel());
        task.setTablePattern(bo.getTablePattern());
        task.setTriggerType(bo.getTriggerType());
        task.setCronExpr(bo.getCronExpr());
        task.setTargetColumns(bo.getTargetColumns());

        taskMapper.updateById(task);
        log.info("更新探查任务成功: id={}", task.getId());
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("删除的ID列表不能为空");
        }

        for (Long id : ids) {
            DprofileTask task = taskMapper.selectById(id);
            if (task != null && "RUNNING".equals(task.getStatus())) {
                throw new ServiceException("任务 " + task.getTaskName() + " 正在运行中，不允许删除");
            }
        }

        taskMapper.deleteBatchIds(ids);
        log.info("批量删除探查任务: ids={}", ids);
    }

    @Override
    public void startTask(Long taskId) {
        DprofileTask current = taskMapper.selectById(taskId);
        if (current == null) {
            throw new ServiceException("任务不存在");
        }

        if ("RUNNING".equals(current.getStatus())) {
            throw new ServiceException("任务已在运行中");
        }

        current.setStatus("RUNNING");
        taskMapper.updateById(current);
        log.info("任务已启动: id={}, taskName={}", taskId, current.getTaskName());

        // 异步执行（实际生产中应使用线程池或消息队列）
        try {
            runTaskSync(taskId);
        } catch (Exception e) {
            log.error("任务执行失败: id={}, error={}", taskId, e.getMessage(), e);
            current.setStatus("FAILED");
            current.setLastRunStatus("FAILED");
            current.setLastRunTime(LocalDateTime.now());
            taskMapper.updateById(current);
        }
    }

    @Override
    public void stopTask(Long taskId) {
        DprofileTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }

        if (!"RUNNING".equals(task.getStatus())) {
            throw new ServiceException("任务不在运行状态，无法停止");
        }

        task.setStatus("STOPPED");
        task.setLastRunStatus("STOPPED");
        taskMapper.updateById(task);
        log.info("任务已停止: id={}, taskName={}", taskId, task.getTaskName());
    }

    @Override
    public void runTaskSync(Long taskId) {
        LocalDateTime startTime = LocalDateTime.now();

        DprofileTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }

        log.info("开始执行探查任务: id={}, taskName={}, dsId={}, pattern={}, level={}",
            taskId, task.getTaskName(), task.getDsId(), task.getTablePattern(), task.getProfileLevel());

        try {
            // 1. Get datasource adapter
            DataSourceAdapter adapter = datasourceHelper.getAdapter(task.getDsId());

            // 2. Get tables matching pattern
            List<String> allTables = adapter.getTables();
            List<String> matchedTables = filterTablesByPattern(allTables, task.getTablePattern());

            if (matchedTables.isEmpty()) {
                log.warn("没有匹配到任何表: pattern={}", task.getTablePattern());
                task.setStatus("SUCCESS");
                task.setTotalTables(0);
                task.setLastRunStatus("SUCCESS");
                task.setLastRunTime(startTime);
                taskMapper.updateById(task);
                return;
            }

            // 3. Limit to MAX_TABLES_PER_RUN
            int maxTables = 100;
            if (matchedTables.size() > maxTables) {
                log.warn("匹配的表数量 {} 超过限制 {}, 仅处理前 {} 张表",
                    matchedTables.size(), maxTables, maxTables);
                matchedTables = matchedTables.subList(0, maxTables);
            }

            // 4. Parse targetColumns from comma-separated string
            Set<String> targetCols = parseTargetColumns(task.getTargetColumns());

            // 5. Analyze each table with column filter
            List<Long> reportIds = dprofileService.analyzeTables(
                task.getDsId(),
                matchedTables,
                task.getProfileLevel(),
                targetCols
            );

            // 5. Update task status
            LocalDateTime endTime = LocalDateTime.now();
            long elapsedMs = Duration.between(startTime, endTime).toMillis();

            task.setStatus("SUCCESS");
            task.setLastRunTime(startTime);
            task.setLastRunStatus("SUCCESS");
            task.setTotalTables(matchedTables.size());

            taskMapper.updateById(task);

            log.info("探查任务执行完成: id={}, 耗时={}ms, 处理表数={}, 报告数={}, 指定列数={}",
                taskId, elapsedMs, matchedTables.size(), reportIds.size(),
                targetCols.isEmpty() ? "全部" : targetCols.size());

        } catch (Exception e) {
            log.error("探查任务执行失败: id={}, error={}", taskId, e.getMessage(), e);
            task.setStatus("FAILED");
            task.setLastRunTime(startTime);
            task.setLastRunStatus("FAILED");
            taskMapper.updateById(task);
            throw new ServiceException("任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 根据通配符模式过滤表名
     * <p>
     * 支持的通配符：
     * - * 匹配任意字符
     * - ? 匹配单个字符
     * <p>
     * 例如：user_* 匹配 user_开头的所有表
     */
    private List<String> filterTablesByPattern(List<String> tables, String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return tables;
        }

        String regex = pattern
            .replace("*", ".*")
            .replace("?", ".");

        Pattern compiledPattern = Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE);

        return tables.stream()
            .filter(table -> compiledPattern.matcher(table).matches())
            .collect(Collectors.toList());
    }

    /**
     * 解析逗号分隔的列名字符串为 Set
     *
     * @param targetColumns 逗号分隔的列名，如 "id,name,email"
     * @return 列名集合，空串或 null 返回空集合
     */
    private Set<String> parseTargetColumns(String targetColumns) {
        if (StringUtils.isBlank(targetColumns)) {
            return new HashSet<>();
        }
        return Arrays.stream(targetColumns.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 生成任务编码
     */
    private String generateTaskCode() {
        return "DPROFILE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 构建查询条件
     */
    private Wrapper<DprofileTask> buildQueryWrapper(DprofileTaskVo vo) {
        LambdaQueryWrapper<DprofileTask> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ObjectUtil.isNotNull(vo.getDsId()), DprofileTask::getDsId, vo.getDsId())
            .like(StringUtils.isNotBlank(vo.getTaskName()), DprofileTask::getTaskName, vo.getTaskName())
            .like(StringUtils.isNotBlank(vo.getKeyword()), DprofileTask::getTaskName, vo.getKeyword())
            .eq(StringUtils.isNotBlank(vo.getStatus()), DprofileTask::getStatus, vo.getStatus())
            .eq(StringUtils.isNotBlank(vo.getProfileLevel()), DprofileTask::getProfileLevel, vo.getProfileLevel())
            .eq(StringUtils.isNotBlank(vo.getTriggerType()), DprofileTask::getTriggerType, vo.getTriggerType())
            .orderByDesc(DprofileTask::getCreateTime);
        return wrapper;
    }
}
