package dev.haotangyuan.knownote.common.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.haotangyuan.knownote.research.domain.entity.ChatMessageDO;
import dev.haotangyuan.knownote.research.domain.entity.WorkflowEventDO;
import dev.haotangyuan.knownote.research.domain.entity.ResearchSessionDO;
import dev.haotangyuan.knownote.research.domain.mapper.ChatMessageMapper;
import dev.haotangyuan.knownote.research.domain.mapper.WorkflowEventMapper;
import dev.haotangyuan.knownote.research.domain.mapper.ResearchSessionMapper;
import dev.haotangyuan.knownote.research.data.TimelineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 研究时间线缓存与读写
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheUtil {

    private final ChatMessageMapper chatMessageMapper;
    private final WorkflowEventMapper workflowEventMapper;
    private final ResearchSessionMapper researchSessionMapper;
    private final SequenceUtil sequenceUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KIND_MESSAGE = "message";
    private static final String KIND_EVENT = "event";
    private static final String TIMELINE_KEY = "research:{}:timeline";
    private static final long TIMELINE_TTL_MINUTES = 30;

    private static final String USER_RESEARCHES_KEY = "user:{}:researches";
    private static final long USER_RESEARCHES_TTL_DAYS = 7;

    public TimelineItem saveMessage(String researchId, String role, String content) {
        int seq = sequenceUtil.next(researchId);
        ChatMessageDO message = ChatMessageDO.builder()
                .researchId(researchId)
                .role(role)
                .content(content)
                .sequenceNo(seq)
                .createTime(LocalDateTime.now())
                .build();
        chatMessageMapper.insert(message);
        TimelineItem item = TimelineItem.builder()
                .kind(KIND_MESSAGE)
                .researchId(researchId)
                .sequenceNo(seq)
                .message(message)
                .build();
        writeToRedis(researchId, List.of(item));
        return item;
    }

    public TimelineItem saveEvent(String researchId, String type,
                                    String title, String content, Long parentEventId) {
        int seq = sequenceUtil.next(researchId);
        WorkflowEventDO event = WorkflowEventDO.builder()
                .researchId(researchId)
                .type(type)
                .title(title)
                .content(content)
                .parentEventId(parentEventId)
                .sequenceNo(seq)
                .createTime(LocalDateTime.now())
                .build();
        workflowEventMapper.insert(event);
        TimelineItem item = TimelineItem.builder()
                .kind(KIND_EVENT)
                .researchId(researchId)
                .sequenceNo(seq)
                .event(event)
                .build();
        writeToRedis(researchId, List.of(item));
        return item;
    }

    /**
     * 保存临时事件排队信息，用于前端显示排队状态，seq = -1
     */
    public TimelineItem saveTempEvent(String researchId, String type, String title) {
        WorkflowEventDO event = WorkflowEventDO.builder()
                .researchId(researchId)
                .type(type)
                .title(title)
                .sequenceNo(-1)
                .createTime(LocalDateTime.now())
                .build();
        TimelineItem item = TimelineItem.builder()
                .kind(KIND_EVENT)
                .researchId(researchId)
                .sequenceNo(-1)
                .event(event)
                .build();
        writeToRedis(researchId, List.of(item));
        return item;
    }

    public List<TimelineItem> getTimeline(String researchId, int lastSeq) {
        List<TimelineItem> redisItems = readFromRedis(researchId, lastSeq + 1, Integer.MAX_VALUE);
        if (!redisItems.isEmpty()) {
            return redisItems;
        }
        List<TimelineItem> all = loadFromDb(researchId);
        writeToRedis(researchId, all);
        if (lastSeq == 0) {
            return all;
        }
        return all.stream()
                .filter(item -> item.getSequenceNo() > lastSeq)
                .collect(Collectors.toList());
    }

    private void writeToRedis(String researchId, List<TimelineItem> items) {
        if (CollectionUtil.isEmpty(items)) {
            return;
        }
        String key = StrUtil.format(TIMELINE_KEY, researchId);
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        for (TimelineItem item : items) {
            String value = serialize(item);
            if (value != null) {
                tuples.add(ZSetOperations.TypedTuple.of(value, (double) item.getSequenceNo()));
            }
        }
        if (!tuples.isEmpty()) {
            stringRedisTemplate.opsForZSet().add(key, tuples);
            stringRedisTemplate.expire(key, TIMELINE_TTL_MINUTES, TimeUnit.MINUTES);
        }
    }

    private List<TimelineItem> readFromRedis(String researchId, int minSeq, int maxSeq) {
        String key = StrUtil.format(TIMELINE_KEY, researchId);
        Set<String> values = stringRedisTemplate.opsForZSet().rangeByScore(key, minSeq, maxSeq);
        if (CollectionUtil.isEmpty(values)) {
            return new ArrayList<>();
        }
        stringRedisTemplate.expire(key, TIMELINE_TTL_MINUTES, TimeUnit.MINUTES);
        return values.stream()
                .map(this::deserialize)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TimelineItem::getSequenceNo))
                .collect(Collectors.toList());
    }

    private List<TimelineItem> loadFromDb(String researchId) {
        LambdaQueryWrapper<ChatMessageDO> messageQuery = Wrappers.lambdaQuery(ChatMessageDO.class)
                .eq(ChatMessageDO::getResearchId, researchId);
        LambdaQueryWrapper<WorkflowEventDO> eventQuery = Wrappers.lambdaQuery(WorkflowEventDO.class)
                .eq(WorkflowEventDO::getResearchId, researchId);
        List<ChatMessageDO> messages = chatMessageMapper.selectList(messageQuery);
        List<WorkflowEventDO> events = workflowEventMapper.selectList(eventQuery);
        List<TimelineItem> all = new ArrayList<>();
        messages.stream()
            .map(m -> TimelineItem.builder()
                    .kind(KIND_MESSAGE)
                    .researchId(researchId)
                    .sequenceNo(m.getSequenceNo())
                    .message(m)
                    .build())
            .forEach(all::add);
        events.stream()
            .map(e -> TimelineItem.builder()
                    .kind(KIND_EVENT)
                    .researchId(researchId)
                    .sequenceNo(e.getSequenceNo())
                    .event(e)
                    .build())
            .forEach(all::add);
        all.sort(Comparator.comparing(TimelineItem::getSequenceNo));
        return all;
    }

    private String serialize(TimelineItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            log.error("TimelineItem 序列化 JSON 失败", e);
            return null;
        }
    }

    private TimelineItem deserialize(String json) {
        try {
            return objectMapper.readValue(json, TimelineItem.class);
        } catch (Exception e) {
            log.error("JSON 反序列化为 TimelineItem 失败 json={}", json, e);
            return null;
        }
    }

    /**
     * 验证 researchId 是否属于 userId
     */
    public boolean verifyResearchOwnership(String researchId, Long userId) {
        String key = StrUtil.format(USER_RESEARCHES_KEY, userId);
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, researchId);

        if (Boolean.TRUE.equals(isMember)) {
            stringRedisTemplate.expire(key, USER_RESEARCHES_TTL_DAYS, TimeUnit.DAYS);
            return true;
        }

        LambdaQueryWrapper<ResearchSessionDO> queryWrapper = Wrappers.lambdaQuery(ResearchSessionDO.class)
                .eq(ResearchSessionDO::getId, researchId)
                .eq(ResearchSessionDO::getUserId, userId);
        ResearchSessionDO session = researchSessionMapper.selectOne(queryWrapper);

        if (session != null) {
            stringRedisTemplate.opsForSet().add(key, researchId);
            stringRedisTemplate.expire(key, USER_RESEARCHES_TTL_DAYS, TimeUnit.DAYS);
            log.debug("权限验证成功，已缓存 userId={}, researchId={}", userId, researchId);
            return true;
        }

        log.debug("权限验证失败 userId={}, researchId={}", userId, researchId);
        return false;
    }

    /**
     * 缓存研究的所有权关系
     */
    public void cacheResearchOwnership(String researchId, Long userId) {
        String key = StrUtil.format(USER_RESEARCHES_KEY, userId);
        stringRedisTemplate.opsForSet().add(key, researchId);
        stringRedisTemplate.expire(key, USER_RESEARCHES_TTL_DAYS, TimeUnit.DAYS);
        log.debug("缓存权限映射 userId={}, researchId={}", userId, researchId);
    }
}
