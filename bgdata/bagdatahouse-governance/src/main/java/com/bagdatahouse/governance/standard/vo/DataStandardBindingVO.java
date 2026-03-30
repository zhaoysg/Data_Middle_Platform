package com.bagdatahouse.governance.standard.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据标准绑定列表项 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据标准绑定列表项")
public class DataStandardBindingVO {

    @ApiModelProperty("绑定ID")
    private Long id;

    @ApiModelProperty("标准ID")
    private Long standardId;

    @ApiModelProperty("元数据ID")
    private Long metadataId;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("数据源名称")
    private String dsName;

    @ApiModelProperty("目标表名")
    private String targetTable;

    @ApiModelProperty("目标表中文名")
    private String targetTableAlias;

    @ApiModelProperty("目标列名")
    private String targetColumn;

    @ApiModelProperty("数据层")
    private String dataLayer;

    @ApiModelProperty("合规状态")
    private String complianceStatus;

    @ApiModelProperty("合规状态中文名")
    private String complianceStatusLabel;

    @ApiModelProperty("不合规记录数")
    private Integer violationCount;

    @ApiModelProperty("最后检测时间")
    private LocalDateTime lastCheckTime;

    @ApiModelProperty("不合规处理方式")
    private String enforceAction;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建人用户名")
    private String createUserName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
