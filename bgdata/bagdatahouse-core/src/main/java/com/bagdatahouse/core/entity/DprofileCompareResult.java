package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Compare result entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dprofile_compare_result")
@ApiModel(description = "Profile comparison result")
public class DprofileCompareResult {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Compare name")
    private String compareName;

    @ApiModelProperty("Compare code")
    private String compareCode;

    @ApiModelProperty("Snapshot A ID")
    private Long snapshotAId;

    @ApiModelProperty("Snapshot B ID")
    private Long snapshotBId;

    @ApiModelProperty("Compare type: schema/values/trend")
    private String compareType;

    @ApiModelProperty("Compare result (JSON string)")
    private String compareResult;

    @ApiModelProperty("Row count change")
    private Long rowCountChange;

    @ApiModelProperty("Column changes (JSON string)")
    private String columnChanges;

    @ApiModelProperty("Schema changes (JSON string)")
    private String schemaChanges;

    @ApiModelProperty("Total difference count")
    private Integer diffCount;

    @ApiModelProperty("Difference detail")
    private String diffDetail;

    @ApiModelProperty("Creator user ID")
    private Long createUser;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
