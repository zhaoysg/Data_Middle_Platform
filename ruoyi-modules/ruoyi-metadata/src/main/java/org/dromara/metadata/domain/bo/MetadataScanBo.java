package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.metadata.domain.MetadataScanLog;

import java.io.Serializable;
import java.util.List;

/**
 * 元数据扫描业务对象
 */
@Data
@NoArgsConstructor
@AutoMapper(target = MetadataScanLog.class, reverseConvertGenerate = false)
public class MetadataScanBo implements Serializable {

    /**
     * 扫描记录ID
     */
    private Long id;

    /**
     * 数据源ID
     */
    @NotNull(message = "数据源ID不能为空")
    private Long dsId;

    /**
     * 扫描模式：ALL-整库扫描 TABLES-选表扫描 RULE-规则扫描
     */
    private String scanMode = "ALL";

    /**
     * 是否同步字段
     */
    private Boolean syncColumn = true;

    /**
     * 指定表名列表（选表扫描）
     */
    private List<String> tableNames;

    /**
     * 规则匹配类型：CONTAINS/PREFIX/SUFFIX/EXACT/REGEX
     */
    private String ruleType = "CONTAINS";

    /**
     * 包含规则，多个规则支持逗号或换行分隔
     */
    private String includePattern;

    /**
     * 排除规则，多个规则支持逗号或换行分隔
     */
    private String excludePattern;

    /**
     * 是否忽略大小写
     */
    private Boolean ignoreCase = true;

    /**
     * 自定义 schema，未传则使用数据源默认 schema
     */
    private String schemaName;

    /**
     * 强制指定数据层（如 ODS/DWD/DWS/ADS），覆盖自动推断
     */
    private String dataLayer;

    /**
     * 扫描完成后自动触发下游处理链（敏感识别/血缘/DQC/脱敏）
     */
    private Boolean autoDownstreamProcess = true;
}
