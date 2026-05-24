package dev.haotangyuan.knownote.count.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Post 计数字段
 */
@AllArgsConstructor
@Getter
public enum PostCountField {
    LIKED_COUNT(0),
    FAVED_COUNT(4);

    private final int offset;
}
