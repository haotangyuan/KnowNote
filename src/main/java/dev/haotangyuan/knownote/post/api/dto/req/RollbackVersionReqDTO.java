package dev.haotangyuan.knownote.post.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 回滚帖子版本请求 DTO
 */
@Data
public class RollbackVersionReqDTO {

    @NotNull(message = "版本时间戳不能为空")
    private Long versionId;

    @NotBlank(message = "版本哈希不能为空")
    private String versionSha256;
}
