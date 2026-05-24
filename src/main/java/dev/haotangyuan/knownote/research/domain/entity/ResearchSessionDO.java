package dev.haotangyuan.knownote.research.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 研究会话实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("research_session")
public class ResearchSessionDO {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    private Long userId;
    private String status;
    private String title;
    private String modelId;
    private String budget;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime updateTime;
    private LocalDateTime completeTime;
    private Long totalInputTokens;
    private Long totalOutputTokens;
}
