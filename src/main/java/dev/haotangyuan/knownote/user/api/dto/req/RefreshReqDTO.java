package dev.haotangyuan.knownote.user.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 Token 请求
 */
@Data
public class RefreshReqDTO {

    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
