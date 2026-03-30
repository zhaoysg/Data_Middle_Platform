package com.bagdatahouse.governance.lineage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "血缘统计信息")
public class LineageStatsVO {

    @ApiModelProperty("总血缘记录数")
    private long totalCount;

    @ApiModelProperty("表级血缘数")
    private long tableLineageCount;

    @ApiModelProperty("字段级血缘数")
    private long columnLineageCount;

    @ApiModelProperty("手动录入数")
    private long manualCount;

    @ApiModelProperty("自动解析数")
    private long autoParserCount;

    @ApiModelProperty("涉及的源表数量")
    private long sourceTableCount;

    @ApiModelProperty("涉及的目标表数量")
    private long targetTableCount;

    @ApiModelProperty("涉及的字段数量")
    private long columnCount;

    @ApiModelProperty("按数据源分布")
    private Map<String, Long> byDataSource;

    @ApiModelProperty("按数据层分布")
    private Map<String, Long> byDataLayer;

    @ApiModelProperty("按转换类型分布")
    private Map<String, Long> byTransformType;
}
