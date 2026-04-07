package org.dromara.metadata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.SecClassification;
import org.dromara.metadata.domain.SecColumnSensitivity;
import org.dromara.metadata.domain.SecLevel;
import org.dromara.metadata.domain.SecSensitivityRule;
import org.dromara.metadata.domain.vo.SecClassificationVo;
import org.dromara.metadata.mapper.SecClassificationMapper;
import org.dromara.metadata.mapper.SecColumnSensitivityMapper;
import org.dromara.metadata.mapper.SecLevelMapper;
import org.dromara.metadata.mapper.SecSensitivityRuleMapper;
import org.dromara.metadata.service.ISecClassificationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据分类服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SecClassificationServiceImpl implements ISecClassificationService {

    private final SecClassificationMapper baseMapper;
    private final SecSensitivityRuleMapper sensitivityRuleMapper;
    private final SecColumnSensitivityMapper columnSensitivityMapper;
    private final SecLevelMapper levelMapper;

    @Override
    public TableDataInfo<SecClassificationVo> queryPageList(SecClassificationVo vo, PageQuery pageQuery) {
        var wrapper = Wrappers.<SecClassification>lambdaQuery()
            .like(StringUtils.isNotBlank(vo.getClassName()), SecClassification::getClassName, vo.getClassName())
            .like(StringUtils.isNotBlank(vo.getClassCode()), SecClassification::getClassCode, vo.getClassCode())
            .eq(StringUtils.isNotBlank(vo.getEnabled()), SecClassification::getEnabled, vo.getEnabled())
            .orderByAsc(SecClassification::getSortOrder);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        enrichClassificationRows(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public SecClassificationVo queryById(Long id) {
        SecClassificationVo row = baseMapper.selectVoById(id);
        if (row != null) {
            enrichClassificationRows(List.of(row));
        }
        return row;
    }

    @Override
    public List<SecClassificationVo> listAll() {
        return baseMapper.selectVoList(
            Wrappers.<SecClassification>lambdaQuery()
                .eq(SecClassification::getEnabled, "1")
                .orderByAsc(SecClassification::getSortOrder)
        );
    }

    @Override
    public Long insert(SecClassificationVo vo) {
        if (StringUtils.isNotBlank(vo.getClassCode())) {
            boolean exist = baseMapper.exists(Wrappers.<SecClassification>lambdaQuery()
                .eq(SecClassification::getClassCode, vo.getClassCode()));
            if (exist) {
                throw new ServiceException("分类编码已存在");
            }
        }
        SecClassification entity = new SecClassification();
        entity.setClassCode(vo.getClassCode());
        entity.setClassName(vo.getClassName());
        entity.setClassDesc(vo.getClassDesc());
        entity.setSortOrder(vo.getSortOrder() != null ? vo.getSortOrder() : 0);
        entity.setDefaultLevelCode(vo.getDefaultLevelCode());
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : "1");
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int update(SecClassificationVo vo) {
        if (vo.getId() == null) {
            throw new ServiceException("分类ID不能为空");
        }
        if (StringUtils.isNotBlank(vo.getClassCode())) {
            boolean exist = baseMapper.exists(Wrappers.<SecClassification>lambdaQuery()
                .eq(SecClassification::getClassCode, vo.getClassCode())
                .ne(SecClassification::getId, vo.getId()));
            if (exist) {
                throw new ServiceException("分类编码已存在");
            }
        }
        SecClassification entity = new SecClassification();
        entity.setId(vo.getId());
        entity.setClassCode(vo.getClassCode());
        entity.setClassName(vo.getClassName());
        entity.setClassDesc(vo.getClassDesc());
        entity.setSortOrder(vo.getSortOrder());
        entity.setDefaultLevelCode(vo.getDefaultLevelCode());
        entity.setEnabled(vo.getEnabled());
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(List.of(ids));
    }

    /**
     * 填充关联等级展示名、规则数、字段数（与 bagdatahouse 分类列表对齐）
     */
    private void enrichClassificationRows(List<SecClassificationVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<String> classCodes = rows.stream()
            .map(SecClassificationVo::getClassCode)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        Map<String, Long> ruleByCode = classCodes.isEmpty() ? Map.of() : countRulesByClassCodes(classCodes);
        Map<String, Long> fieldByCode = classCodes.isEmpty() ? Map.of() : countFieldsByClassCodes(classCodes);
        for (SecClassificationVo r : rows) {
            String cc = r.getClassCode();
            if (StringUtils.isNotBlank(cc)) {
                r.setRuleCount(ruleByCode.getOrDefault(cc, 0L));
                r.setFieldCount(fieldByCode.getOrDefault(cc, 0L));
            } else {
                r.setRuleCount(0L);
                r.setFieldCount(0L);
            }
        }
        fillDefaultLevelLabels(rows);
    }

    private Map<String, Long> countRulesByClassCodes(List<String> classCodes) {
        QueryWrapper<SecSensitivityRule> qw = new QueryWrapper<>();
        qw.select("target_class_code", "COUNT(*) AS agg_cnt");
        qw.in("target_class_code", classCodes);
        qw.groupBy("target_class_code");
        return mapsToCountMap(sensitivityRuleMapper.selectMaps(qw), "target_class_code", "agg_cnt");
    }

    private Map<String, Long> countFieldsByClassCodes(List<String> classCodes) {
        QueryWrapper<SecColumnSensitivity> qw = new QueryWrapper<>();
        qw.select("class_code", "COUNT(*) AS agg_cnt");
        qw.in("class_code", classCodes);
        qw.groupBy("class_code");
        return mapsToCountMap(columnSensitivityMapper.selectMaps(qw), "class_code", "agg_cnt");
    }

    private static Map<String, Long> mapsToCountMap(
        List<Map<String, Object>> maps, String keyColumn, String countAlias) {
        Map<String, Long> out = new HashMap<>();
        if (maps == null) {
            return out;
        }
        for (Map<String, Object> m : maps) {
            Object k = mapGetIgnoreCase(m, keyColumn);
            Object c = mapGetIgnoreCase(m, countAlias);
            if (k != null && c instanceof Number num) {
                out.put(String.valueOf(k), num.longValue());
            }
        }
        return out;
    }

    private static Object mapGetIgnoreCase(Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    private void fillDefaultLevelLabels(List<SecClassificationVo> rows) {
        Set<String> levelCodes = rows.stream()
            .map(SecClassificationVo::getDefaultLevelCode)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
        if (levelCodes.isEmpty()) {
            return;
        }
        List<SecLevel> levels = levelMapper.selectList(
            Wrappers.<SecLevel>lambdaQuery().in(SecLevel::getLevelCode, levelCodes));
        Map<String, SecLevel> byCode = levels.stream()
            .filter(l -> StringUtils.isNotBlank(l.getLevelCode()))
            .collect(Collectors.toMap(SecLevel::getLevelCode, Function.identity(), (a, b) -> a));
        for (SecClassificationVo vo : rows) {
            String lc = vo.getDefaultLevelCode();
            if (StringUtils.isBlank(lc)) {
                continue;
            }
            SecLevel lv = byCode.get(lc);
            if (lv != null) {
                vo.setDefaultLevelName(lv.getLevelName());
                vo.setDefaultLevelColor(StringUtils.isNotBlank(lv.getColor()) ? lv.getColor() : "#1677FF");
            }
        }
    }
}
