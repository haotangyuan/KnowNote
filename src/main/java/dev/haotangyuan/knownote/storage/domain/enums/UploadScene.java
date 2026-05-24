package dev.haotangyuan.knownote.storage.domain.enums;

import cn.hutool.core.util.IdUtil;
import dev.haotangyuan.knownote.common.BizException;
import dev.haotangyuan.knownote.common.ErrorCode;
import dev.haotangyuan.knownote.common.UserContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * oss 场景
 */
@Getter
@AllArgsConstructor
public enum UploadScene {
    USER_AVATAR("user_avatar", "用户头像", true),
    POST_CONTENT("post_content", "帖子正文", false),
    POST_IMAGE("post_image", "内嵌图片", true);

    private final String scene;
    private final String description;
    private final boolean isPublic;

    /**
     * 根据场景字符串获取枚举
     */
    public static UploadScene fromScene(String scene) {
        for (UploadScene uploadScene : UploadScene.values()) {
            if (uploadScene.getScene().equals(scene)) {
                return uploadScene;
            }
        }
        throw new BizException(ErrorCode.CLIENT_ERROR, "不支持的上传场景：" + scene);
    }

    /**
     * 获取存储路径
     * avatar: users/{userId}/avatar/{timestamp}.{ext}
     * post content: posts/{postId}/content/{timestamp}.md
     * post image: posts/{postId}/images/{timestamp}.{ext}
     */
    public String getPath(String resourceId, String ext) {
        String id;
        if (this == POST_IMAGE) {
            id = IdUtil.fastSimpleUUID();
        } else {
            id = String.valueOf(Instant.now().toEpochMilli());
        }
        return getPath(resourceId, id, ext);
    }

    public String getPath(String resourceId, String id, String ext) {
        return switch (this) {
            case USER_AVATAR -> "users/" + UserContext.getUserId() + "/avatar/" + id + "." + ext;
            case POST_CONTENT -> "posts/" + resourceId + "/versions/" + id + ".md";
            case POST_IMAGE -> "posts/" + resourceId + "/images/" + id + "." + ext;
        };
    }

    public String getPublicPath(String resourceId) {
        return switch (this) {
            case USER_AVATAR -> null;
            case POST_CONTENT -> "posts/" + resourceId + "/content.md";
            case POST_IMAGE -> "posts/" + resourceId + "/cover.webp";
        };
    }

    public String getPrefix(String resourceId) {
        return switch (this) {
            case USER_AVATAR -> "users/" + resourceId + "/avatar/";
            case POST_CONTENT -> "posts/" + resourceId + "/versions/";
            case POST_IMAGE -> "posts/" + resourceId + "/images/";
        };
    }
}
