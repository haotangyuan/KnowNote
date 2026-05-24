package dev.haotangyuan.knownote.user.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Token 响应
 */
@Data
@AllArgsConstructor
public class TokenRespDTO {
    private String accessToken;
    private String refreshToken;
}
