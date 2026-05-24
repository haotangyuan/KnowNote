package dev.haotangyuan.knownote.research.data;

import dev.haotangyuan.knownote.research.domain.entity.ChatMessageDO;
import dev.haotangyuan.knownote.research.domain.entity.WorkflowEventDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 研究时间线条目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineItem {
    private String kind;
    private String researchId;
    private Integer sequenceNo;
    private ChatMessageDO message;
    private WorkflowEventDO event;
}
