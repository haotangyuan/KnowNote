package dev.haotangyuan.knownote.post.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import dev.haotangyuan.knownote.common.BizException;
import dev.haotangyuan.knownote.common.ErrorCode;
import dev.haotangyuan.knownote.common.UserContext;
import dev.haotangyuan.knownote.post.api.dto.req.RollbackVersionReqDTO;
import dev.haotangyuan.knownote.post.api.dto.req.SavePostContentReqDTO;
import dev.haotangyuan.knownote.post.api.dto.req.SavePostMetadataReqDTO;
import dev.haotangyuan.knownote.post.api.dto.resp.CreatePostRespDTO;
import dev.haotangyuan.knownote.post.api.dto.resp.PostRespDTO;
import dev.haotangyuan.knownote.post.api.dto.resp.PostVersionRespDTO;
import dev.haotangyuan.knownote.post.domain.entity.PostDO;
import dev.haotangyuan.knownote.post.domain.enums.PostStatus;
import dev.haotangyuan.knownote.post.domain.enums.PostType;
import dev.haotangyuan.knownote.post.domain.mapper.PostMapper;
import dev.haotangyuan.knownote.post.mq.PostReviewMessage;
import dev.haotangyuan.knownote.post.mq.PostReviewProducer;
import dev.haotangyuan.knownote.post.service.PostService;
import dev.haotangyuan.knownote.storage.domain.enums.UploadScene;
import dev.haotangyuan.knownote.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

/**
 * 帖子服务实现类
 */
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("not '${oss.endpoint:}'.blank and not '${rocketmq.name-server:}'.blank")
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final StorageService storageService;
    private final PostReviewProducer postReviewProducer;

    @Override
    public CreatePostRespDTO createPost() {
        Long userId = UserContext.getUserId();
        LocalDateTime now = LocalDateTime.now();
        PostDO post = PostDO.builder()
                .id(IdUtil.getSnowflakeNextId())
                .creatorId(userId)
                .status(PostStatus.DRAFT)
                .type(PostType.ARTICLE)
                .isTop(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            postMapper.insert(post);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "创建帖子失败");
        }
        return CreatePostRespDTO.builder().postId(post.getId()).build();
    }

    @Override
    public void saveContent(Long postId, SavePostContentReqDTO req) {
        PostDO post = getPostByOwner(postId);
        LocalDateTime now = LocalDateTime.now();
        BeanUtil.copyProperties(req, post, CopyOptions.create().ignoreNullValue());
        post.setUpdatedAt(now);
        try {
            postMapper.updateById(post);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "保存帖子内容失败");
        }
    }

    @Override
    public void rollbackPost(Long postId, RollbackVersionReqDTO req) {
        PostDO post = getPostByOwner(postId);

        String sourceKey = UploadScene.POST_CONTENT.getPath(String.valueOf(postId), String.valueOf(req.getVersionId()), "md");
        String destKey = UploadScene.POST_CONTENT.getPath(String.valueOf(postId), "md");
        storageService.copyObject(sourceKey, destKey);

        post.setContentUrl(destKey);
        post.setContentSha256(req.getVersionSha256());
        post.setUpdatedAt(LocalDateTime.now());
        try {
            postMapper.updateById(post);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "回滚帖子内容失败");
        }
    }

    @Override
    public void saveMetadata(Long postId, SavePostMetadataReqDTO req) {
        PostDO post = getPostByOwner(postId);
        LocalDateTime now = LocalDateTime.now();
        if (req.getCoverUrl() != null) {
            storageService.copyObject(req.getCoverUrl(), UploadScene.POST_IMAGE.getPublicPath(String.valueOf(postId)));
        }
        BeanUtil.copyProperties(req, post, CopyOptions.create().ignoreNullValue());
        post.setUpdatedAt(now);
        try {
            postMapper.updateById(post);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "保存帖子元数据失败");
        }
    }

    @Override
    public void unpublishPost(Long postId) {
        PostDO post = getPostByOwner(postId);
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "当前状态无法下线");
        }
        LambdaUpdateWrapper<PostDO> wrapper = new LambdaUpdateWrapper<PostDO>()
                .eq(PostDO::getId, postId)
                .set(PostDO::getStatus, PostStatus.DRAFT)
                .set(PostDO::getPublishedAt, null)
                .set(PostDO::getPublishedVersion, null)
                .set(PostDO::getUpdatedAt, LocalDateTime.now());
        postMapper.update(null, wrapper);
        String contentKey = UploadScene.POST_CONTENT.getPublicPath(String.valueOf(postId));
        storageService.deletePublicObject(contentKey);
        String coverKey = UploadScene.POST_IMAGE.getPublicPath(String.valueOf(postId));
        storageService.deletePublicObject(coverKey);
        // TODO: 清除 index
    }

    @Override
    public void publishPost(Long postId) {
        PostDO post = getPostByOwner(postId);

        if (post.getStatus() == PostStatus.DELETED || post.getStatus() == PostStatus.REVIEWING) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "当前状态无法发布");
        }
        if (StrUtil.isBlank(post.getContentUrl())) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "帖子内容不能为空");
        }
        post.setStatus(PostStatus.REVIEWING);
        post.setRejectReason(null);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);

        PostReviewMessage message = BeanUtil.copyProperties(post, PostReviewMessage.class);
        message.setPostId(post.getId());
        postReviewProducer.sendReviewMessage(message);
    }

    @Override
    public PostRespDTO getPost(Long postId) {
        PostDO post = getPostByOwner(postId);
        if (!StrUtil.isBlank(post.getContentUrl())) {
            post.setContentUrl(storageService.getSignedUrl(post.getContentUrl(), Duration.ofMinutes(15)));
        }
        return BeanUtil.copyProperties(post, PostRespDTO.class);
    }

    @Override
    public void deletePost(Long postId) {
        PostDO post = getPostByOwner(postId);

        if (post.getStatus() == PostStatus.DELETED || post.getStatus() == PostStatus.REVIEWING) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "当前状态无法删除");
        }
        if (!StrUtil.isBlank(post.getPublishedVersion())) {
            String contentKey = UploadScene.POST_CONTENT.getPublicPath(String.valueOf(postId));
            storageService.deletePublicObject(contentKey);
            String coverKey = UploadScene.POST_IMAGE.getPublicPath(String.valueOf(postId));
            storageService.deletePublicObject(coverKey);
        }
        post.setStatus(PostStatus.DELETED);
        post.setUpdatedAt(LocalDateTime.now());
        // TODO: 清除 index
        postMapper.updateById(post);
    }

    @Override
    public PostVersionRespDTO getVersions(Long postId) {
        PostDO post = getPostByOwner(postId);

        String prefix = "posts/" + postId + "/versions/";
        List<S3Object> objects = storageService.listObjects(prefix);

        List<PostVersionRespDTO.Version> versions = objects.stream()
            .sorted(Comparator.comparing(S3Object::lastModified).reversed())
            .map(obj -> {
                String key = obj.key();
                String filename = key.substring(key.lastIndexOf('/') + 1);
                long timestamp = Long.parseLong(filename.replace(".md", ""));
                return PostVersionRespDTO.Version.builder()
                    .timestamp(timestamp)
                    .url(storageService.getSignedUrl(key, Duration.ofMinutes(15)))
                    .createdAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId()))
                    .build();
            })
            .toList();

        return PostVersionRespDTO.builder().versions(versions).build();
    }

    private PostDO getPostByOwner(Long postId) {
        PostDO post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == PostStatus.DELETED) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "帖子不存在");
        }
        if (!UserContext.getUserId().equals(post.getCreatorId())) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "没有权限操作该帖子");
        }
        return post;
    }
}
