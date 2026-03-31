package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.GovGlossaryTermBo;
import org.dromara.metadata.domain.vo.GovGlossaryTermVo;
import org.dromara.metadata.service.IGovGlossaryTermService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据治理术语Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/glossary/term")
public class GovGlossaryTermController extends BaseController {

    private final IGovGlossaryTermService termService;

    /**
     * 查询术语列表
     */
    @SaCheckPermission("metadata:glossary:term:list")
    @GetMapping("/list")
    public TableDataInfo<GovGlossaryTermVo> list(GovGlossaryTermBo bo, PageQuery pageQuery) {
        return termService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取术语详细信息
     */
    @SaCheckPermission("metadata:glossary:term:query")
    @GetMapping("/{id}")
    public R<GovGlossaryTermVo> getInfo(@PathVariable Long id) {
        return R.ok(termService.queryById(id));
    }

    /**
     * 新增术语
     */
    @SaCheckPermission("metadata:glossary:term:add")
    @Log(title = "数据治理术语")
    @PostMapping
    public R<Void> add(@Validated @RequestBody GovGlossaryTermBo bo) {
        return toAjax(termService.insertByBo(bo) > 0);
    }

    /**
     * 修改术语
     */
    @SaCheckPermission("metadata:glossary:term:edit")
    @Log(title = "数据治理术语")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody GovGlossaryTermBo bo) {
        return toAjax(termService.updateByBo(bo));
    }

    /**
     * 删除术语
     */
    @SaCheckPermission("metadata:glossary:term:remove")
    @Log(title = "数据治理术语")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        return toAjax(termService.deleteByIds(ids));
    }

    /**
     * 发布术语
     */
    @SaCheckPermission("metadata:glossary:term:publish")
    @Log(title = "数据治理术语-发布")
    @PostMapping("/publish/{id}")
    public R<Void> publish(@PathVariable Long id) {
        return toAjax(termService.publish(id));
    }

    /**
     * 根据关键词搜索术语
     */
    @SaCheckPermission("metadata:glossary:term:list")
    @GetMapping("/keyword/{keyword}")
    public R<List<GovGlossaryTermVo>> searchByKeyword(@PathVariable String keyword) {
        return R.ok(termService.listByKeyword(keyword));
    }
}
