package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.DqcRuleTemplateBo;
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
     * 新增模板
     */
    @SaCheckPermission("metadata:dqc:template:add")
    @Log(title = "数据质量规则模板")
    @PostMapping
    public R<Long> add(@Validated @RequestBody DqcRuleTemplateBo bo) {
        return R.ok(templateService.insertByBo(bo));
    }

    /**
     * 修改模板
     */
    @SaCheckPermission("metadata:dqc:template:edit")
    @Log(title = "数据质量规则模板")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody DqcRuleTemplateBo bo) {
        return toAjax(templateService.updateByBo(bo));
    }

    /**
     * 删除模板
     */
    @SaCheckPermission("metadata:dqc:template:remove")
    @Log(title = "数据质量规则模板")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        return toAjax(templateService.deleteByIds(ids));
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
