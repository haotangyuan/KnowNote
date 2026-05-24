package dev.haotangyuan.knownote.like.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 「点赞增量」生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.name-server")
public class LikeDeltaProducer {

    private static final String TOPIC = "like-delta";

    private final RocketMQTemplate rocketMQTemplate;

    public void send(LikeDeltaMessage message) {
        log.info("发送点赞增量消息: eventId={}, postId={}, creatorId={}, delta={}",
                message.getEventId(), message.getPostId(), message.getCreatorId(), message.getDelta());
        rocketMQTemplate.convertAndSend(TOPIC, message);
    }
}
