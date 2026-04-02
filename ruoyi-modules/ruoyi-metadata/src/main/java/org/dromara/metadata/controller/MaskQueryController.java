package org.dromara.metadata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.service.IMaskQueryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 脱敏查询控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/security/mask-query")
public class MaskQueryController extends BaseController {

    private final IMaskQueryService maskQueryService;

    /**
     * 执行脱敏查询
     */
    @SaCheckPermission("metadata:security:maskQuery:query")
    @PostMapping("/query")
    public R<IMaskQueryService.MaskQueryResult> query(@Validated @RequestBody MaskQueryRequest request) {
        IMaskQueryService.ValidationResult validation = maskQueryService.validateSql(request.sql());
        if (!validation.valid()) {
            throw new ServiceException(validation.message());
        }

        return R.ok(maskQueryService.query(
            request.dsId(),
            request.sql(),
            LoginHelper.getUserId(),
            LoginHelper.getUsername(),
            ServletUtils.getClientIP()
        ));
    }

    /**
     * 预校验 SQL 语句
     */
    @SaCheckPermission("metadata:security:maskQuery:validate")
    @PostMapping("/validate")
    public R<IMaskQueryService.ValidationResult> validate(@Validated @RequestBody MaskQueryValidateRequest request) {
        return R.ok(maskQueryService.validateSql(request.sql()));
    }

    /**
     * 查询请求
     */
    public record MaskQueryRequest(
        @NotNull(message = "数据源不能为空")
        Long dsId,
        @NotBlank(message = "SQL不能为空")
        String sql
    ) {
    }

    /**
     * 校验请求
     */
    public record MaskQueryValidateRequest(
        @NotBlank(message = "SQL不能为空")
        String sql
    ) {
    }
}
