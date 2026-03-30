package com.bagdatahouse.server.service;

import com.bagdatahouse.core.entity.DqDataLayer;
import com.bagdatahouse.dqc.service.DqDataLayerService;
import com.bagdatahouse.server.IntegrationTest;
import com.baomidou.mybatisplus.extension.service.IService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据层服务集成测试
 */
@IntegrationTest
@DisplayName("数据层服务集成测试")
class DqDataLayerServiceIT {

    @Autowired
    private DqDataLayerService dataLayerService;

    @Test
    @DisplayName("获取所有数据层-验证测试数据")
    void testGetAllLayers() {
        IService<DqDataLayer> service = (IService<DqDataLayer>) dataLayerService;
        List<DqDataLayer> layers = service.list();
        
        assertThat(layers).isNotNull();
    }

    @Test
    @DisplayName("根据编码查询数据层")
    void testGetLayerByCode() {
        IService<DqDataLayer> service = (IService<DqDataLayer>) dataLayerService;
        DqDataLayer layer = service.lambdaQuery()
                .eq(DqDataLayer::getLayerCode, "DWD")
                .one();
        
        assertThat(layer).isNotNull();
        assertThat(layer.getLayerName()).isEqualTo("DWD层");
        assertThat(layer.getLayerDesc()).isEqualTo("明细数据层");
    }

    @Test
    @DisplayName("根据ID查询数据层")
    void testGetLayerById() {
        IService<DqDataLayer> service = (IService<DqDataLayer>) dataLayerService;
        DqDataLayer layer = service.getById(1L);
        
        assertThat(layer).isNotNull();
        assertThat(layer.getLayerCode()).isEqualTo("ODS");
    }
}
