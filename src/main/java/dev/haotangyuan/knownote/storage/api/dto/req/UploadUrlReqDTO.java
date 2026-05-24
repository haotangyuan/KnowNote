package dev.haotangyuan.knownote.storage.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 上传文件的 URL 请求 DTO
 */
@Data
public class UploadUrlReqDTO {
    @NotBlank(message = "文件扩展名不能为空")
    @Pattern(regexp = "^(jpg|jpeg|png|gif|webp|md)$", message = "不支持的文件类型")
    private String ext;
    @NotBlank(message = "场景不能为空")
    private String scene;
    private String resourceId;
}
