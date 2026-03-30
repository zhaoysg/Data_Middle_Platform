package com.bagdatahouse.dqc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqDataLayer;
import com.bagdatahouse.core.mapper.DqDataLayerMapper;
import com.bagdatahouse.dqc.service.DqDataLayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DQ数据层服务实现
 */
@Service
public class DqDataLayerServiceImpl extends ServiceImpl<DqDataLayerMapper, DqDataLayer>
        implements DqDataLayerService {

    private static final Logger log = LoggerFactory.getLogger(DqDataLayerServiceImpl.class);

    @Override
    public Result<List<DqDataLayer>> listAll() {
        List<DqDataLayer> list = this.list()
                .stream()
                .sorted((a, b) -> {
                    if (a.getSortOrder() == null && b.getSortOrder() == null) {
                        return 0;
                    }
                    if (a.getSortOrder() == null) {
                        return 1;
                    }
                    if (b.getSortOrder() == null) {
                        return -1;
                    }
                    return a.getSortOrder().compareTo(b.getSortOrder());
                })
                .collect(Collectors.toList());
        return Result.success(list);
    }

    @Override
    public Result<Map<String, List<DqDataLayer>>> listGrouped() {
        List<DqDataLayer> list = this.list();

        Map<String, List<DqDataLayer>> grouped = list.stream()
                .sorted((a, b) -> {
                    if (a.getSortOrder() == null && b.getSortOrder() == null) {
                        return 0;
                    }
                    if (a.getSortOrder() == null) {
                        return 1;
                    }
                    if (b.getSortOrder() == null) {
                        return -1;
                    }
                    return a.getSortOrder().compareTo(b.getSortOrder());
                })
                .collect(Collectors.groupingBy(
                        d -> d.getLayerCode() != null ? d.getLayerCode().toUpperCase() : "OTHER"
                ));

        return Result.success(grouped);
    }
}
