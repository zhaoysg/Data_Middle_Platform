package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.vo.DprofileColumnReportVo;
import org.dromara.metadata.domain.vo.DprofileReportVo;

import java.util.List;

/**
 * 数据探查报告服务接口
 */
public interface IDprofileReportService {

    /**
     * 分页查询探查报告
     */
    TableDataInfo<DprofileReportVo> queryPageList(DprofileReportVo vo, PageQuery pageQuery);

    /**
     * 查询报告详情
     */
    DprofileReportVo queryById(Long id);

    /**
     * 查询列级探查结果
     */
    List<DprofileColumnReportVo> queryColumnsByReportId(Long reportId);
}
