package dev.haotangyuan.knownote.user.api.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邮箱注册请求
 */
@Data
public class RegisterReqDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为6位数字")
    private String code;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度6-32位")
    private String password;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 6, max = 13, message = "用户名长度6-13位")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 32, message = "昵称最长32位")
    private String nickname;
}
