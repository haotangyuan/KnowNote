package dev.haotangyuan.knownote.user.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class UserDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 邮箱，唯一
     */
    private String email;
    /**
     * 用户ID，唯一，最长12字符，可为空
     */
    private String username;
    /**
     * 昵称，可为空
     */
    private String nickname;
    /**
     * 密码哈希（BCrypt），可为空（Google注册用户）
     */
    private String passwordHash;
    /**
     * Google OpenID，可为空
     */
    private String googleId;
    /**
     * 头像URL
     */
    private String avatar;
    /**
     * 个人简介，最长200字符
     */
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
