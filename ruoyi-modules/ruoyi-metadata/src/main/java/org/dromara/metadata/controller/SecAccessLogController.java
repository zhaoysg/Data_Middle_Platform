package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.vo.SecAccessLogVo;
import org.dromara.metadata.service.ISecAccessLogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 脱敏访问日志管理Controller
 *
 * @author shaozhengchao
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/security/access-log")
public class SecAccessLogController extends BaseController {

    private final ISecAccessLogService accessLogService;

    /**
     * 查询访问日志列表
     */
    @SaCheckPermission("metadata:security:accessLog:list")
    @GetMapping("/list")
    public TableDataInfo<SecAccessLogVo> list(SecAccessLogVo vo, PageQuery pageQuery) {
        return accessLogService.queryPageList(vo, pageQuery);
    }

    /**
     * 获取访问日志详细信息
     */
    @SaCheckPermission("metadata:security:accessLog:query")
    @GetMapping("/{id}")
    public R<SecAccessLogVo> getInfo(@PathVariable Long id) {
        return R.ok(accessLogService.queryById(id));
    }

    /**
     * 查询指定用户的访问记录
     */
    @SaCheckPermission("metadata:security:accessLog:list")
    @GetMapping("/user/{userId}")
    public R<List<SecAccessLogVo>> queryByUserId(@PathVariable Long userId) {
        return R.ok(accessLogService.queryByUserId(userId, 100));
    }

    /**
     * 查询指定数据源的访问记录
     */
    @SaCheckPermission("metadata:security:accessLog:list")
    @GetMapping("/ds/{dsId}")
    public R<List<SecAccessLogVo>> queryByDsId(@PathVariable Long dsId) {
        return R.ok(accessLogService.queryByDsId(dsId, 100));
    }
}
