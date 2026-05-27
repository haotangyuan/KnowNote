package dev.haotangyuan.knownote.studio.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("studio_project")
public class StudioProjectDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;
    private String description;

    /** INIT | ACTIVE | DELETED */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
