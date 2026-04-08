package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.dto.SecSensitivityScanDTO;
import org.dromara.metadata.domain.vo.SecColumnSensitivityVo;
import org.dromara.metadata.domain.vo.SecScanResultVO;

import java.util.List;

/**
 * 字段敏感记录服务接口
 */
public interface ISecColumnSensitivityService {

    /**
     * 分页查询敏感字段列表
     */
    TableDataInfo<SecColumnSensitivityVo> queryPageList(SecColumnSensitivityVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询敏感字段
     */
    SecColumnSensitivityVo queryById(Long id);

    /**
     * 执行敏感字段扫描（完整参数版，对标 governance 能力）
     *
     * @param dto 扫描参数
     * @return 扫描结果
     */
    SecScanResultVO scanSensitiveFields(SecSensitivityScanDTO dto);

    /**
     * 简化版扫描（仅 dsId），向后兼容 MetadataScanServiceImpl 的调用
     *
     * @param dsId 数据源ID
     * @return 扫描匹配的字段数
     */
    default int scanColumns(Long dsId) {
        if (dsId == null) {
            return 0;
        }
        SecSensitivityScanDTO dto = SecSensitivityScanDTO.builder()
                .dsId(dsId)
                .scanScope("ALL")
                .excludeSystemTables(true)
                .scanColumnName(true)
                .scanColumnComment(true)
                .scanDataType(false)
                .enableContentScan(false)
                .incremental(false)
                .directScan(false)
                .scanCycle("ONCE")
                .build();
        SecScanResultVO result = scanSensitiveFields(dto);
        return result.getFoundSensitiveCount() != null ? result.getFoundSensitiveCount() : 0;
    }

    /**
     * 确认敏感字段
     */
    int confirmColumns(List<Long> ids);

    /**
     * 新增敏感字段记录
     */
    Long insert(SecColumnSensitivityVo vo);

    /**
     * 修改敏感字段记录
     */
    int update(SecColumnSensitivityVo vo);

    /**
     * 删除敏感字段记录
     */
    int deleteByIds(Long[] ids);
}
