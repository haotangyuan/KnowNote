package dev.haotangyuan.knownote.post.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 发布审核消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReviewMessage implements Serializable {
    private Long postId;
    private String contentUrl;
    // 到底审不审
    private String coverUrl;
    private String imgUrls;
}
