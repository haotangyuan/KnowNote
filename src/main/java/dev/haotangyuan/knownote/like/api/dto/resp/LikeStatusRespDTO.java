package dev.haotangyuan.knownote.like.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞状态响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeStatusRespDTO {
    private Long postId;
    private Boolean liked;
}
