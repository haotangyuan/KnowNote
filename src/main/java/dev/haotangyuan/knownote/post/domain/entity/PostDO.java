package dev.haotangyuan.knownote.post.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import dev.haotangyuan.knownote.post.domain.enums.PostStatus;
import dev.haotangyuan.knownote.post.domain.enums.PostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("post")
public class PostDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long creatorId;
    private PostStatus status;
    private PostType type;

    private String title;
    private String description;
    private String tags;
    private String coverUrl;
    private Integer isTop;

    private String contentUrl;
    private String contentSha256;

    private String rejectReason;
    private String imgUrls;
    private String publishedVersion;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
