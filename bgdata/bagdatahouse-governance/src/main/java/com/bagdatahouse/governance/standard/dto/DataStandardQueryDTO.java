package com.bagdatahouse.governance.standard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据标准查询请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据标准查询请求")
public class DataStandardQueryDTO {

    @ApiModelProperty("标准类型：CODE_STANDARD/NAMING_STANDARD/PRIMARY_DATA")
    private String standardType;

    @ApiModelProperty("标准分类")
    private String standardCategory;

    @ApiModelProperty("标准名称（模糊匹配）")
    private String standardName;

    @ApiModelProperty("标准编码（精确匹配）")
    private String standardCode;

    @ApiModelProperty("状态：DRAFT/PUBLISHED/DEPRECATED")
    private String status;

    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;

    @ApiModelProperty("业务域")
    private String bizDomain;

    @ApiModelProperty("负责人用户ID")
    private Long ownerId;

    @ApiModelProperty("部门ID")
    private Long deptId;
}
