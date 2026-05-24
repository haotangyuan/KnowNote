package dev.haotangyuan.knownote.post.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 帖子类型
 */
@Getter
@AllArgsConstructor
public enum PostType {
    ARTICLE("article");

    private final String type;
}
