package dev.haotangyuan.knownote.like.api.dto.req;

import dev.haotangyuan.knownote.like.domain.enums.LikeAction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 点赞请求
 */
@Data
public class LikePostReqDTO {
    @NotNull(message = "Post Id 不能为空")
    private Long postId;

    @NotNull(message = "操作不能为空")
    private LikeAction action;
}
