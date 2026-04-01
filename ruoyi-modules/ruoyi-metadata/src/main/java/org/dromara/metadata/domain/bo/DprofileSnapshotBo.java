package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.metadata.domain.DprofileSnapshot;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据探查快照业务对象
 */
@Data
@NoArgsConstructor
@AutoMapper(target = DprofileSnapshot.class, reverseConvertGenerate = false)
public class DprofileSnapshotBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 快照名称
     */
    @NotBlank(message = "快照名称不能为空")
    @Size(max = 100, message = "快照名称长度不能超过{max}个字符")
    private String snapshotName;

    /**
     * 快照描述
     */
    @Size(max = 500, message = "快照描述长度不能超过{max}个字符")
    private String snapshotDesc;

    /**
     * 数据源ID
     */
    @jakarta.validation.constraints.NotNull(message = "数据源ID不能为空")
    private Long dsId;
}
