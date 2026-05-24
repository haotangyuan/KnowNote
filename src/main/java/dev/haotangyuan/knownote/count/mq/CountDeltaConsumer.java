package dev.haotangyuan.knownote.count.mq;

import dev.haotangyuan.knownote.count.domain.enums.PostCountField;
import dev.haotangyuan.knownote.count.domain.enums.UserCountField;
import dev.haotangyuan.knownote.count.service.CountService;
import dev.haotangyuan.knownote.like.mq.LikeDeltaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 「点赞增量」计数消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.name-server")
@RocketMQMessageListener(
        topic = "like-delta",
        consumerGroup = "knownote-consumer"
)
public class CountDeltaConsumer implements RocketMQListener<LikeDeltaMessage> {

    private final CountService countService;

    @Override
    public void onMessage(LikeDeltaMessage message) {
        if (message == null || message.getPostId() == null || message.getCreatorId() == null || message.getDelta() == null) {
            log.warn("点赞增量消息无效: {}", message);
            return;
        }
        log.info("消费点赞增量: eventId={}, postId={}, creatorId={}, delta={}",
                message.getEventId(), message.getPostId(), message.getCreatorId(), message.getDelta());
        try {
            // TODO: 增加幂等去重，防止重复消费导致计数偏差
            countService.incrPostCount(message.getPostId(), PostCountField.LIKED_COUNT, message.getDelta());
            countService.incrUserCount(message.getCreatorId(), UserCountField.LIKED, message.getDelta());
        } catch (Exception e) {
            log.error("点赞计数更新失败: eventId={}, postId={}, creatorId={}",
                    message.getEventId(), message.getPostId(), message.getCreatorId(), e);
            throw e;
        }
    }
}
