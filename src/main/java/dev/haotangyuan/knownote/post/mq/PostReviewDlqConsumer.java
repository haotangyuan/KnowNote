package dev.haotangyuan.knownote.post.mq;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import dev.haotangyuan.knownote.post.domain.entity.PostDO;
import dev.haotangyuan.knownote.post.domain.enums.PostStatus;
import dev.haotangyuan.knownote.post.domain.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审核死信队列消费者，处理重试耗尽的消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.name-server")
@RocketMQMessageListener(
        topic = "%DLQ%knownote-review-consumer",
        consumerGroup = "knownote-review-dlq-consumer"
)
public class PostReviewDlqConsumer implements RocketMQListener<PostReviewMessage> {

    private static final String REJECT_REASON = "系统繁忙，审核失败，请稍后重新发布";

    private final PostMapper postMapper;

    @Override
    public void onMessage(PostReviewMessage message) {
        Long postId = message.getPostId();
        log.warn("死信队列审核消费: postId={}", postId);
        try {
            PostDO post = postMapper.selectById(postId);
            if (post == null) {
                log.warn("帖子不存在，跳过: postId={}", postId);
                return;
            }
            PostStatus targetStatus = StrUtil.isBlank(post.getPublishedVersion()) ? PostStatus.DRAFT : PostStatus.PUBLISHED;
            postMapper.update(new LambdaUpdateWrapper<PostDO>()
                    .eq(PostDO::getId, postId)
                    .set(PostDO::getStatus, targetStatus)
                    .set(PostDO::getRejectReason, REJECT_REASON)
                    .set(PostDO::getUpdatedAt, LocalDateTime.now()));
            // TODO: 发送站内信通知用户
        } catch (Exception e) {
            log.error("死信队列处理失败，需人工介入: postId={}", postId, e);
            // 不抛异常：DLQ 没有二级死信队列，抛异常重试耗尽后消息丢弃
        }
    }
}
