package com.bagdatahouse.governance.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 脱敏场景类型枚举
 * <p>
 * 对齐阿里 DataWorks 数据保护伞
 */
@Getter
@AllArgsConstructor
public enum MaskSceneTypeEnum {

    DEVELOP_SHOW("DEVELOP_SHOW", "数据开发界面展示"),
    DATA_MAP_SHOW("DATA_MAP_SHOW", "数据地图/元数据预览"),
    ANALYSIS_QUERY("ANALYSIS_QUERY", "数据分析 SQL 查询"),
    EXPORT_RESULT("EXPORT_RESULT", "导出结果"),
    PRINT_REPORT("PRINT_REPORT", "报表打印");

    private final String code;
    private final String label;

    public static String getLabel(String code) {
        for (MaskSceneTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return code;
    }
}
