package com.bagdatahouse.governance.lineage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表/字段搜索建议 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "表/字段搜索建议")
public class TableColumnSuggestVO {

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("数据源名称")
    private String dsName;

    @ApiModelProperty("匹配的表列表")
    private List<TableSuggest> tables;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ApiModel(description = "表建议")
    public static class TableSuggest {

        @ApiModelProperty("元数据ID")
        private Long metadataId;

        @ApiModelProperty("表名")
        private String tableName;

        @ApiModelProperty("表中文名/别名")
        private String tableAlias;

        @ApiModelProperty("数据层")
        private String dataLayer;

        @ApiModelProperty("字段数量")
        private Integer columnCount;

        @ApiModelProperty("匹配的字段列表（搜索字段时返回）")
        private List<ColumnSuggest> columns;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ApiModel(description = "字段建议")
    public static class ColumnSuggest {

        @ApiModelProperty("元数据字段ID")
        private Long columnId;

        @ApiModelProperty("字段名")
        private String columnName;

        @ApiModelProperty("字段中文名/别名")
        private String columnAlias;

        @ApiModelProperty("数据类型")
        private String dataType;

        @ApiModelProperty("是否主键")
        private Boolean isPrimaryKey;

        @ApiModelProperty("是否外键")
        private Boolean isForeignKey;

        @ApiModelProperty("是否敏感字段")
        private Boolean isSensitive;
    }
}
