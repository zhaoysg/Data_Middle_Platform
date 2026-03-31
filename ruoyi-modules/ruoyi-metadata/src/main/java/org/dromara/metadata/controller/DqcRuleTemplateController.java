package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.vo.DqcRuleTemplateVo;
import org.dromara.metadata.service.IDqcRuleTemplateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据质量规则模板Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/dqc/template")
public class DqcRuleTemplateController extends BaseController {

    private final IDqcRuleTemplateService templateService;

    /**
     * 查询模板列表
     */
    @SaCheckPermission("metadata:dqc:template:list")
    @GetMapping("/list")
    public TableDataInfo<DqcRuleTemplateVo> list(DqcRuleTemplateVo vo, PageQuery pageQuery) {
        return templateService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取模板详细信息
     */
    @SaCheckPermission("metadata:dqc:template:query")
    @GetMapping("/{id}")
    public R<DqcRuleTemplateVo> getInfo(@PathVariable Long id) {
        return R.ok(templateService.queryById(id));
    }

    /**
     * 根据维度查询模板
     */
    @SaCheckPermission("metadata:dqc:template:list")
    @GetMapping("/dimension/{dimension}")
    public R<List<DqcRuleTemplateVo>> listByDimension(@PathVariable String dimension) {
        return R.ok(templateService.listByDimension(dimension));
    }
}
