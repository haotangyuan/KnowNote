package dev.haotangyuan.knownote.research.framework;

import dev.haotangyuan.knownote.config.ResearchProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Session-scoped context passed through all agents in a pipeline run.
 * Carries the immutable identifiers needed to route events and enforce budget limits.
 * Per-agent counters (conductCount, searchCount) and token totals are tracked as
 * local variables inside each agent; persistent totals live in DeepResearchState
 * (they are written to the DB on completion).
 */
@Getter
@Builder
public class AgentContext {

    @NonNull
    private final String researchId;
    private final ResearchProperties.BudgetLevel budget;
}
