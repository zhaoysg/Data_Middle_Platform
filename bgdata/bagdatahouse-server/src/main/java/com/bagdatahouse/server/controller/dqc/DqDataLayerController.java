package com.bagdatahouse.server.controller.dqc;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqDataLayer;
import com.bagdatahouse.dqc.service.DqDataLayerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * DQ数据层控制器
 */
@Api(tags = "数据质量-数仓分层管理")
@RestController
@RequestMapping("/dqc/data-layer")
public class DqDataLayerController {

    private static final Logger log = LoggerFactory.getLogger(DqDataLayerController.class);

    @Autowired
    private DqDataLayerService dataLayerService;

    @GetMapping("/list")
    @ApiOperation("获取所有数据层")
    public Result<List<DqDataLayer>> listAll() {
        return dataLayerService.listAll();
    }

    @GetMapping("/grouped")
    @ApiOperation("分组获取数据层")
    public Result<Map<String, List<DqDataLayer>>> listGrouped() {
        return dataLayerService.listGrouped();
    }
}
