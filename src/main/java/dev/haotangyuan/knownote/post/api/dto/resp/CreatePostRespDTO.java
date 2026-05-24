package dev.haotangyuan.knownote.post.api.dto.resp;

import lombok.Builder;
import lombok.Data;

/**
 * 创建帖子响应 DTO
 */
@Data
@Builder
public class CreatePostRespDTO {
    /**
     * 帖子 ID
     */
    private Long postId;
}
