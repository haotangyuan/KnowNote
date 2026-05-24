package dev.haotangyuan.knownote.user.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Google OAuth Callback 登录请求
 */
@Data
public class GoogleCallbackReqDTO {

    /** 授权码 */
    @NotBlank(message = "授权码不能为空")
    private String code;
}
