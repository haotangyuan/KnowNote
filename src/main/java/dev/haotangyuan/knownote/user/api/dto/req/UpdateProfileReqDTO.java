package dev.haotangyuan.knownote.user.api.dto.req;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人资料请求
 */
@Data
public class UpdateProfileReqDTO {

    @Size(max = 64, message = "昵称过长")
    private String nickname;

    private String avatar;

    @Size(max = 200, message = "简介过长")
    private String bio;
}
