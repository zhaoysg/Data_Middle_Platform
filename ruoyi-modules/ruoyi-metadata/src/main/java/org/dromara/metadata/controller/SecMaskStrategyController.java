package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.bo.SecMaskStrategyBo;
import org.dromara.metadata.domain.vo.SecMaskStrategyDetailVo;
import org.dromara.metadata.domain.vo.SecMaskStrategyVo;
import org.dromara.metadata.service.ISecMaskStrategyService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 脱敏策略管理Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/security/strategy")
public class SecMaskStrategyController extends BaseController {

    private final ISecMaskStrategyService maskStrategyService;

    /**
     * 查询脱敏策略列表
     */
    @SaCheckPermission("metadata:security:strategy:list")
    @GetMapping("/list")
    public TableDataInfo<SecMaskStrategyVo> list(SecMaskStrategyVo vo, PageQuery pageQuery) {
        return maskStrategyService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取脱敏策略详细信息
     */
    @SaCheckPermission("metadata:security:strategy:query")
    @GetMapping("/{id}")
    public R<SecMaskStrategyVo> getInfo(@PathVariable Long id) {
        return R.ok(maskStrategyService.queryById(id));
    }

    /**
     * 获取策略明细列表
     */
    @SaCheckPermission("metadata:security:strategy:query")
    @GetMapping("/{id}/details")
    public R<List<SecMaskStrategyDetailVo>> getDetails(@PathVariable Long id) {
        return R.ok(maskStrategyService.queryDetailsByStrategyId(id));
    }

    /**
     * 获取所有启用的策略
     */
    @SaCheckPermission("metadata:security:strategy:list")
    @GetMapping("/all")
    public R<List<SecMaskStrategyVo>> listAll() {
        return R.ok(maskStrategyService.listAllEnabled());
    }

    /**
     * 新增脱敏策略
     */
    @SaCheckPermission("metadata:security:strategy:add")
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SecMaskStrategyBo bo) {
        return R.ok(maskStrategyService.insertWithDetails(bo));
    }

    /**
     * 修改脱敏策略
     */
    @SaCheckPermission("metadata:security:strategy:edit")
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SecMaskStrategyBo bo) {
        return toAjax(maskStrategyService.updateWithDetails(bo));
    }

    /**
     * 删除脱敏策略
     */
    @SaCheckPermission("metadata:security:strategy:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(maskStrategyService.deleteByIds(ids));
    }
}
