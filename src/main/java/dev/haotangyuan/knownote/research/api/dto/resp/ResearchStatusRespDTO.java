package dev.haotangyuan.knownote.research.api.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 研究状态返回
 */
@Data
@Builder
public class ResearchStatusRespDTO {
    private String id;
    private String status;
    private String title;
    private String modelId;
    private String budget;
    private LocalDateTime startTime;
    private LocalDateTime completeTime;
    private Long totalInputTokens;
    private Long totalOutputTokens;
}
