package dev.haotangyuan.knownote.user.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class UpdatePasswordReqDTO {

    /**
     * 原密码，无密码用户可为空
     */
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度8-32位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "密码需包含字母和数字")
    private String newPassword;
}
