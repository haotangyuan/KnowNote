package dev.haotangyuan.knownote.post.mq;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import dev.haotangyuan.knownote.config.OssProperties;
import dev.haotangyuan.knownote.config.ReviewAiProperties;
import dev.haotangyuan.knownote.post.domain.entity.PostDO;
import dev.haotangyuan.knownote.post.domain.enums.PostStatus;
import dev.haotangyuan.knownote.post.domain.mapper.PostMapper;
import dev.haotangyuan.knownote.post.mq.review.ContentReviewer;
import dev.haotangyuan.knownote.post.mq.review.ReviewResult;
import dev.haotangyuan.knownote.storage.domain.enums.UploadScene;
import dev.haotangyuan.knownote.storage.service.StorageService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 发布审核服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("not '${oss.endpoint:}'.blank")
public class PostReviewService {

    private static final int DESCRIPTION_MAX_LENGTH = 512;

    private final PostMapper postMapper;
    private final StorageService storageService;
    private final OssProperties ossProperties;
    private final ReviewAiProperties reviewAiProperties;

    private ContentReviewer contentReviewer;

    @PostConstruct
    public void init() {
        if (Boolean.TRUE.equals(reviewAiProperties.getEnabled())) {
            if (ObjectUtil.isAllNotEmpty(reviewAiProperties)) {
                ChatModel chatModel = OpenAiChatModel.builder()
                        .baseUrl(reviewAiProperties.getBaseUrl())
                        .apiKey(reviewAiProperties.getApiKey())
                        .modelName(reviewAiProperties.getModel())
                        .responseFormat("json_object")
                        .build();
                this.contentReviewer = AiServices.create(ContentReviewer.class, chatModel);
            }
        }
    }

    public void handle(PostReviewMessage message) {
        Long postId = message.getPostId();
        PostDO post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != PostStatus.REVIEWING) {
            log.warn("帖子不存在或状态不正确，无法审核，postId={}", postId);
            return;
        }

        // 获取 contentUrl 具体内容
        String content = storageService.getContent(ossProperties.getPrivateBucket(), post.getContentUrl());

        ReviewResult reviewResult = review(postId, content);
        boolean approved = Boolean.TRUE.equals(reviewResult.getApproved());
        String description = StrUtil.trimToEmpty(reviewResult.getDescription());
        description = description.substring(0, Math.min(description.length(), DESCRIPTION_MAX_LENGTH));

        LocalDateTime now = LocalDateTime.now();

        if (approved) {
            // TODO: 添加 index；
            // TODO: 清理未使用的资源 cleanUnusedResources(postId, message.getImgUrls())
            String version = post.getContentUrl().substring(post.getContentUrl().lastIndexOf('/') + 1).replace(".md", "");
            String sourceKey = post.getContentUrl();
            String destKey = UploadScene.POST_CONTENT.getPublicPath(String.valueOf(postId));
            storageService.copyToPublic(sourceKey, destKey);
            if (!StrUtil.isBlank(post.getCoverUrl())) {
                sourceKey = post.getCoverUrl();
                destKey = UploadScene.POST_IMAGE.getPublicPath(String.valueOf(postId));
                storageService.copyPublicObject(sourceKey, destKey);
            }
            post.setDescription(description);
            post.setPublishedVersion(version);
            post.setUpdatedAt(now);
            post.setPublishedAt(now);
            post.setStatus(PostStatus.PUBLISHED);
        } else {
            // TODO: 清理未使用的资源 cleanUnusedResources(postId, message.getImgUrls());
            post.setStatus(StrUtil.isBlank(post.getPublishedVersion()) ? PostStatus.DRAFT : PostStatus.PUBLISHED);
            post.setRejectReason(reviewResult.getRejectedReason());
            post.setUpdatedAt(now);
        }
        postMapper.updateById(post);
    }

    private ReviewResult review(Long postId, String content) {
        if (contentReviewer == null) {
            log.info("AI 审核未启用，默认通过: postId={}", postId);
            return new ReviewResult(Boolean.TRUE, "", "");
        }
        return contentReviewer.review(content);
    }
}
