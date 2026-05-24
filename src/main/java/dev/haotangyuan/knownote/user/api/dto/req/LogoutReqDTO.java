package dev.haotangyuan.knownote.user.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登出请求
 */
@Data
public class LogoutReqDTO {

    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
