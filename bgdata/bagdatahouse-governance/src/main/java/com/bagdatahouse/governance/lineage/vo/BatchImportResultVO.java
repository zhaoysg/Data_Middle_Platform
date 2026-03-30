package com.bagdatahouse.governance.lineage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量导入血缘结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "批量导入血缘结果")
public class BatchImportResultVO {

    @ApiModelProperty("总行数")
    private int totalRows;

    @ApiModelProperty("成功数")
    private int successCount;

    @ApiModelProperty("失败数")
    private int failCount;

    @ApiModelProperty("跳过的行数（已存在血缘）")
    private int skippedCount;

    @ApiModelProperty("失败详情")
    private List<ImportErrorDetail> errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ApiModel(description = "导入错误详情")
    public static class ImportErrorDetail {

        @ApiModelProperty("行号")
        private int rowNum;

        @ApiModelProperty("错误消息")
        private String message;
    }
}
