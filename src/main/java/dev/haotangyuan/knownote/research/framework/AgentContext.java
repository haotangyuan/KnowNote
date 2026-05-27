package dev.haotangyuan.knownote.research.framework;

import dev.haotangyuan.knownote.config.ResearchProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Session-scoped, thread-safe context passed through all agents in a pipeline run.
 */
@Getter
@Builder
public class AgentContext {

    @NonNull
    private final String researchId;
    private final ResearchProperties.BudgetLevel budget;

    @Builder.Default
    private final AtomicInteger conductCount = new AtomicInteger(0);

    @Builder.Default
    private final AtomicInteger searchCount = new AtomicInteger(0);

    @Builder.Default
    private final AtomicLong totalInputTokens = new AtomicLong(0L);

    @Builder.Default
    private final AtomicLong totalOutputTokens = new AtomicLong(0L);

    public void addInputTokens(long count) {
        totalInputTokens.addAndGet(count);
    }

    public void addOutputTokens(long count) {
        totalOutputTokens.addAndGet(count);
    }

    public int incrementConductCount() {
        return conductCount.incrementAndGet();
    }

    public int incrementSearchCount() {
        return searchCount.incrementAndGet();
    }
}
