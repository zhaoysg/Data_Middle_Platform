package com.bagdatahouse.server.controller;

import com.bagdatahouse.server.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 控制器集成测试示例
 * 测试API端点的基本功能
 */
@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("数据源控制器集成测试")
class DqDatasourceControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("获取数据源列表-正常请求")
    void testGetDatasourceList() throws Exception {
        mockMvc.perform(get("/api/v1/datasource/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取数据源详情-正常请求")
    void testGetDatasourceDetail() throws Exception {
        mockMvc.perform(get("/api/v1/datasource/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试连接-无效参数")
    void testTestConnectionWithInvalidParams() throws Exception {
        mockMvc.perform(get("/api/v1/datasource/test")
                        .param("dsType", "INVALID")
                        .param("host", "localhost")
                        .param("port", "3306"))
                .andExpect(status().isOk());
    }
}
