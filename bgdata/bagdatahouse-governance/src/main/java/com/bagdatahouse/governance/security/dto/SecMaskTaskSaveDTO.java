package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脱敏任务保存 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "脱敏任务保存参数")
public class SecMaskTaskSaveDTO {

    @ApiModelProperty("任务ID（编辑时传入）")
    private Long id;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("任务编码")
    private String taskCode;

    @ApiModelProperty("任务类型：STATIC-静态脱敏 / DYNAMIC-动态脱敏")
    private String taskType;

    @ApiModelProperty("源端数据源ID")
    private Long sourceDsId;

    @ApiModelProperty("源端查询SQL")
    private String sourceSql;

    @ApiModelProperty("源端Schema")
    private String sourceSchema;

    @ApiModelProperty("目标端数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标端Schema")
    private String targetSchema;

    @ApiModelProperty("目标端表名")
    private String targetTable;

    @ApiModelProperty("写入模式：APPEND/TRUNCATE/UPSERT")
    private String targetMode;

    @ApiModelProperty("脱敏规则JSON")
    private String maskRules;

    @ApiModelProperty("触发方式：MANUAL/SCHEDULED/EVENT")
    private String triggerType;

    @ApiModelProperty("Cron表达式")
    private String triggerCron;

    @ApiModelProperty("每批处理行数")
    private Integer batchSize;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建者")
    private Long createUser;
}
