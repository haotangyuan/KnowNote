package dev.haotangyuan.knownote.count.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User 计数实体
 */
@AllArgsConstructor
@Data
@NoArgsConstructor
public class UserCountDO {
    private int following;
    private int follower;
    private int post;
    private int liked;
    private int faved;
}
