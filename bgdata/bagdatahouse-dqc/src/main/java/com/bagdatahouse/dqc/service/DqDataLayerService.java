package com.bagdatahouse.dqc.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqDataLayer;

import java.util.List;
import java.util.Map;

/**
 * DQ数据层服务接口
 */
public interface DqDataLayerService {

    /**
     * 获取所有数据层
     */
    Result<List<DqDataLayer>> listAll();

    /**
     * 分组获取数据层
     */
    Result<Map<String, List<DqDataLayer>>> listGrouped();
}
