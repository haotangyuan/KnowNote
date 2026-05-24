package dev.haotangyuan.knownote.post.api.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子版本历史响应
 */
@Data
@Builder
public class PostVersionRespDTO {
    private List<Version> versions;

    @Data
    @Builder
    public static class Version {
        private Long timestamp;
        private String url;
        private LocalDateTime createdAt;
    }
}
