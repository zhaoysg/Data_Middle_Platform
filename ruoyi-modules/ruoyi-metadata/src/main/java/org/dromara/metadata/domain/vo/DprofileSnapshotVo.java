package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.metadata.domain.DprofileSnapshot;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据探查快照视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DprofileSnapshot.class)
@ExcelIgnoreUnannotated
public class DprofileSnapshotVo extends DprofileSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 数据源编码
     */
    private String dsCode;

    /**
     * 创建者名称
     */
    private String createByName;

    /**
     * 关键词搜索
     */
    private String keyword;
}
