package dev.haotangyuan.knownote.post.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 发布审核生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.name-server")
public class PostReviewProducer {

    private static final String TOPIC = "post-review";

    private final RocketMQTemplate rocketMQTemplate;

    public void sendReviewMessage(PostReviewMessage message) {
        log.info("发送发布审核消息: {}", message.getPostId());
        rocketMQTemplate.convertAndSend(TOPIC, message);
    }
}
