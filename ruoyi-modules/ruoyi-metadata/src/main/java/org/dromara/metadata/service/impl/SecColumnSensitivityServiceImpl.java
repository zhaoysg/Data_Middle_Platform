package org.dromara.metadata.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.datasource.adapter.DataSourceAdapter.ColumnInfo;
import org.dromara.metadata.domain.SecColumnSensitivity;
import org.dromara.metadata.domain.vo.SecColumnSensitivityVo;
import org.dromara.metadata.domain.vo.SecSensitivityRuleVo;
import org.dromara.metadata.mapper.SecColumnSensitivityMapper;
import org.dromara.metadata.service.ISecColumnSensitivityService;
import org.dromara.metadata.service.ISecSensitivityRuleService;
import org.dromara.metadata.support.DatasourceHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 字段敏感记录服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class SecColumnSensitivityServiceImpl implements ISecColumnSensitivityService {

    private final SecColumnSensitivityMapper baseMapper;
    private final DatasourceHelper datasourceHelper;
    private final ISecSensitivityRuleService sensitivityRuleService;

    @Override
    public TableDataInfo<SecColumnSensitivityVo> queryPageList(SecColumnSensitivityVo vo, PageQuery pageQuery) {
        var wrapper = Wrappers.<SecColumnSensitivity>lambdaQuery()
            .eq(vo.getDsId() != null, SecColumnSensitivity::getDsId, vo.getDsId())
            .like(StringUtils.isNotBlank(vo.getTableName()), SecColumnSensitivity::getTableName, vo.getTableName())
            .like(StringUtils.isNotBlank(vo.getColumnName()), SecColumnSensitivity::getColumnName, vo.getColumnName())
            .eq(StringUtils.isNotBlank(vo.getLevelCode()), SecColumnSensitivity::getLevelCode, vo.getLevelCode())
            .eq(StringUtils.isNotBlank(vo.getClassCode()), SecColumnSensitivity::getClassCode, vo.getClassCode())
            .eq(StringUtils.isNotBlank(vo.getConfirmed()), SecColumnSensitivity::getConfirmed, vo.getConfirmed())
            .orderByDesc(SecColumnSensitivity::getCreateTime);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SecColumnSensitivityVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanColumns(Long dsId) {
        if (dsId == null) {
            throw new ServiceException("数据源ID不能为空");
        }

        DataSourceAdapter adapter = datasourceHelper.getAdapter(dsId);
        String schemaName = datasourceHelper.getSysDatasource(dsId).getSchemaName();
        List<String> tables = StringUtils.isNotBlank(schemaName)
            ? adapter.getTables(schemaName)
            : adapter.getTables();

        List<SecSensitivityRuleVo> rules = sensitivityRuleService.listAllEnabled();
        if (CollUtil.isEmpty(rules)) {
            log.warn("没有启用的敏感识别规则");
            return 0;
        }

        int totalMatched = 0;

        for (String tableName : tables) {
            try {
                List<ColumnInfo> columns = StringUtils.isNotBlank(schemaName)
                    ? adapter.getColumns(schemaName, tableName)
                    : adapter.getColumns(tableName);
                for (ColumnInfo col : columns) {
                    String colName = col.columnName().toLowerCase();
                    String dataType = col.dataType();

                    for (SecSensitivityRuleVo rule : rules) {
                        if (matchRule(colName, dataType, rule)) {
                            SecColumnSensitivity entity = new SecColumnSensitivity();
                            entity.setDsId(dsId);
                            entity.setTableName(tableName);
                            entity.setColumnName(col.columnName());
                            entity.setDataType(dataType);
                            entity.setLevelCode(rule.getTargetLevelCode());
                            entity.setClassCode(rule.getTargetClassCode());
                            entity.setIdentifiedBy("AUTO");
                            entity.setConfirmed("0");

                            LambdaQueryWrapper<SecColumnSensitivity> existWrapper = Wrappers.<SecColumnSensitivity>lambdaQuery()
                                .eq(SecColumnSensitivity::getDsId, dsId)
                                .eq(SecColumnSensitivity::getTableName, tableName)
                                .eq(SecColumnSensitivity::getColumnName, col.columnName());
                            SecColumnSensitivity existing = baseMapper.selectOne(existWrapper);
                            if (existing == null) {
                                baseMapper.insert(entity);
                                totalMatched++;
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("扫描表{}列信息失败: {}", tableName, e.getMessage());
            }
        }

        log.info("敏感字段扫描完成，共匹配{}个字段", totalMatched);
        return totalMatched;
    }

    private boolean matchRule(String colName, String dataType, SecSensitivityRuleVo rule) {
        String ruleType = rule.getRuleType();
        String ruleExpr = rule.getRuleExpr();

        if (StringUtils.isBlank(ruleExpr)) {
            return false;
        }

        try {
            if ("COLUMN_NAME".equals(ruleType)) {
                var json = JSONUtil.parseObj(ruleExpr);
                var columnNames = json.getJSONArray("columnNames");
                if (columnNames != null) {
                    for (Object name : columnNames) {
                        String pattern = name.toString().toLowerCase();
                        if (colName.contains(pattern) || pattern.contains(colName)) {
                            return true;
                        }
                    }
                }
            } else if ("REGEX".equals(ruleType)) {
                var json = JSONUtil.parseObj(ruleExpr);
                String pattern = json.getStr("pattern");
                if (StringUtils.isNotBlank(pattern)) {
                    return Pattern.matches(pattern, colName);
                }
            } else if ("DATA_TYPE".equals(ruleType)) {
                var json = JSONUtil.parseObj(ruleExpr);
                var dataTypes = json.getJSONArray("dataTypes");
                if (dataTypes != null) {
                    for (Object type : dataTypes) {
                        if (dataType.toLowerCase().contains(type.toString().toLowerCase())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析规则表达式失败: {}, error: {}", ruleExpr, e.getMessage());
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmColumns(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        SecColumnSensitivity update = new SecColumnSensitivity();
        update.setConfirmed("1");
        return baseMapper.update(update, Wrappers.<SecColumnSensitivity>lambdaUpdate()
            .in(SecColumnSensitivity::getId, ids));
    }

    @Override
    public Long insert(SecColumnSensitivityVo vo) {
        SecColumnSensitivity entity = new SecColumnSensitivity();
        entity.setDsId(vo.getDsId());
        entity.setTableName(vo.getTableName());
        entity.setColumnName(vo.getColumnName());
        entity.setDataType(vo.getDataType());
        entity.setLevelCode(vo.getLevelCode());
        entity.setClassCode(vo.getClassCode());
        entity.setIdentifiedBy(vo.getIdentifiedBy() != null ? vo.getIdentifiedBy() : "MANUAL");
        entity.setScanTaskId(vo.getScanTaskId());
        entity.setConfirmed(vo.getConfirmed() != null ? vo.getConfirmed() : "0");
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int update(SecColumnSensitivityVo vo) {
        if (vo.getId() == null) {
            throw new ServiceException("记录ID不能为空");
        }
        SecColumnSensitivity entity = new SecColumnSensitivity();
        entity.setId(vo.getId());
        entity.setLevelCode(vo.getLevelCode());
        entity.setClassCode(vo.getClassCode());
        entity.setConfirmed(vo.getConfirmed());
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }
}
