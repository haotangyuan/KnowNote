package dev.haotangyuan.knownote.count.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * User 计数字段
 */
@AllArgsConstructor
@Getter
public enum UserCountField {
    FOLLOWING(0),
    FOLLOWER(4),
    POST(8),
    LIKED(12),
    FAVED(16);

    private final int offset;
}
