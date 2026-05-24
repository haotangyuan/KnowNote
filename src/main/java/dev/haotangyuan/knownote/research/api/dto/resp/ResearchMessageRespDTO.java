package dev.haotangyuan.knownote.research.api.dto.resp;

import dev.haotangyuan.knownote.research.domain.entity.ChatMessageDO;
import dev.haotangyuan.knownote.research.domain.entity.WorkflowEventDO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 研究消息返回
 */
@Data
@Builder
public class ResearchMessageRespDTO {
    private String id;
    private String status;
    private List<ChatMessageDO> messages;
    private List<WorkflowEventDO> events;
    private LocalDateTime startTime;
    private LocalDateTime updateTime;
    private LocalDateTime completeTime;
    private Long totalInputTokens;
    private Long totalOutputTokens;
}
