package dev.haotangyuan.knownote.user.api.dto.resp;

import lombok.Data;

/**
 * 用户响应
 */
@Data
public class UserRespDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Integer followerCount;
    private Integer followingCount;
    private Integer postCount;
}
