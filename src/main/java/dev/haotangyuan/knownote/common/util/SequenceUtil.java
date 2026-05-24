package dev.haotangyuan.knownote.common.util;

import dev.haotangyuan.knownote.research.domain.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 研究流程事件序号生成器
 */
@Component
@RequiredArgsConstructor
public class SequenceUtil {

    private final ChatMessageMapper chatMessageMapper;
    private final Map<String, AtomicLong> sseSequences = new ConcurrentHashMap<>();

    public int next(String researchId) {
        AtomicLong counter = sseSequences.computeIfAbsent(researchId, k -> {
            Integer maxSeq = chatMessageMapper.selectMaxSequenceByResearchId(researchId);
            return new AtomicLong(maxSeq == null ? 0L : maxSeq);
        });
        long value = counter.incrementAndGet();
        return (int) value;
    }

    public int current(String researchId) {
        AtomicLong counter = sseSequences.get(researchId);
        return counter == null ? 0 : (int) counter.get();
    }

    public void reset(String researchId) {
        sseSequences.remove(researchId);
    }
}
