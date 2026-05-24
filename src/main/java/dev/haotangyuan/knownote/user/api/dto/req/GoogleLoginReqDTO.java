package dev.haotangyuan.knownote.user.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Google 登录请求
 */
@Data
public class GoogleLoginReqDTO {

    /** 前端从 Google 获取的 ID Token */
    @NotBlank(message = "idToken不能为空")
    private String idToken;
}
