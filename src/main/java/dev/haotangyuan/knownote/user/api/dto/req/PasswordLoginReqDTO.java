package dev.haotangyuan.knownote.user.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 密码登录请求
 */
@Data
public class PasswordLoginReqDTO {

    /** 
     * 邮箱或用户名
     */
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
