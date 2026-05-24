package dev.haotangyuan.knownote.post.api.dto.resp;

import dev.haotangyuan.knownote.post.domain.enums.PostStatus;
import dev.haotangyuan.knownote.post.domain.enums.PostType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子响应 DTO
 */
@Data
public class PostRespDTO {
    private Long id;
    private Long creatorId;
    private PostStatus status;
    private PostType type;

    private String title;
    private String description;
    private String tags;
    private String coverUrl;
    private Integer isTop;

    private String contentUrl;
    private String imgUrls;
    private String publishedVersion;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
