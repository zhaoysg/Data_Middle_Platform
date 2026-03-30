package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Profile snapshot entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dprofile_snapshot")
@ApiModel(description = "Profile snapshot")
public class DprofileSnapshot {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Snapshot name")
    private String snapshotName;

    @ApiModelProperty("Snapshot code")
    private String snapshotCode;

    @ApiModelProperty("Target data source ID")
    private Long targetDsId;

    @ApiModelProperty("Target table name")
    private String targetTable;

    @ApiModelProperty("Snapshot description")
    private String snapshotDesc;

    @ApiModelProperty("Table stats ID")
    private Long tableStatsId;

    @ApiModelProperty("Column count")
    private Integer columnCount;

    @ApiModelProperty("Creator user ID")
    private Long createUser;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
