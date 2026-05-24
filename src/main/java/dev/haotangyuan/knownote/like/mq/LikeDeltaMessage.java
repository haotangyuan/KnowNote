package dev.haotangyuan.knownote.like.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 「点赞增量」消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeDeltaMessage {
    private String eventId;
    private Long postId;
    private Long creatorId;
    private Integer delta;
    private LocalDateTime createdAt;
}
