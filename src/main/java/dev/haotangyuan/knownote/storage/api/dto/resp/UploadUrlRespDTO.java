package dev.haotangyuan.knownote.storage.api.dto.resp;

import lombok.Builder;
import lombok.Data;

/**
 * 上传文件的 URL 响应 DTO
 */
@Data
@Builder
public class UploadUrlRespDTO {
    private String uploadUrl;
    private String accessUrl;
}
