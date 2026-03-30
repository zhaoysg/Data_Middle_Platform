package com.bagdatahouse.server.controller;

import com.bagdatahouse.server.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 规则模板控制器集成测试
 */
@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("规则模板控制器集成测试")
class DqcRuleTemplateControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("获取规则模板列表")
    void testGetTemplateList() throws Exception {
        mockMvc.perform(get("/api/v1/dqc/template/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取内置规则模板")
    void testGetBuiltinTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/dqc/template/builtin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取模板详情")
    void testGetTemplateDetail() throws Exception {
        mockMvc.perform(get("/api/v1/dqc/template/1"))
                .andExpect(status().isOk());
    }
}
