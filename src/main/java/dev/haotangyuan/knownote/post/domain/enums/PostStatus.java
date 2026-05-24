package dev.haotangyuan.knownote.post.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 帖子状态
 */
@Getter
@AllArgsConstructor
public enum PostStatus {
    DRAFT("draft"),
    REVIEWING("reviewing"),
    PUBLISHED("published"),
    DELETED("deleted");

    private final String status;
}
