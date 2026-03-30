package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据分类与分级绑定保存 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据分类与分级绑定保存参数")
public class SecClassLevelBindingSaveDTO {

    @ApiModelProperty("分类ID")
    private Long classId;

    @ApiModelProperty("分级ID列表（支持批量绑定）")
    private List<Long> levelIds;
}
