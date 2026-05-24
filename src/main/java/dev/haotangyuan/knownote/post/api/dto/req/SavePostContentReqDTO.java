package dev.haotangyuan.knownote.post.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 保存帖子内容请求 DTO
 */
@Data
public class SavePostContentReqDTO {
    @NotBlank(message = "内容 URL 不能为空")
    private String contentUrl;
    @NotBlank(message = "内容 SHA256 不能为空")
    private String contentSha256;
}
