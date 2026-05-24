package dev.haotangyuan.knownote.count.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Post 计数实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostCountDO {
    private int likedCount;
    private int favedCount;
}
