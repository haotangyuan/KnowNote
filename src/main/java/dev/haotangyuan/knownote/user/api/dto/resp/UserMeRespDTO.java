package dev.haotangyuan.knownote.user.api.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前用户信息响应
 */
@Data
@Builder
public class UserMeRespDTO {
    private Long id;
    private String email;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private Boolean hasPassword;
    private Boolean hasGoogle;
    private LocalDateTime createdAt;
}
