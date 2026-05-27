package dev.haotangyuan.knownote.research.framework;

import dev.haotangyuan.knownote.config.ResearchProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentContextTest {

    private ResearchProperties.BudgetLevel highBudget() {
        ResearchProperties.BudgetLevel b = new ResearchProperties.BudgetLevel();
        b.setMaxConductCount(5);
        b.setMaxSearchCount(10);
        b.setMaxConcurrentUnits(3);
        return b;
    }

    @Test
    void builder_defaultCountersAreZero() {
        AgentContext ctx = AgentContext.builder()
                .researchId("r1")
                .budget(highBudget())
                .build();
        assertThat(ctx.getConductCount().get()).isZero();
        assertThat(ctx.getSearchCount().get()).isZero();
        assertThat(ctx.getTotalInputTokens().get()).isZero();
        assertThat(ctx.getTotalOutputTokens().get()).isZero();
    }

    @Test
    void addInputTokens_accumulatesCorrectly() {
        AgentContext ctx = AgentContext.builder()
                .researchId("r1")
                .budget(highBudget())
                .build();
        ctx.addInputTokens(100L);
        ctx.addInputTokens(50L);
        assertThat(ctx.getTotalInputTokens().get()).isEqualTo(150L);
    }

    @Test
    void addOutputTokens_accumulatesCorrectly() {
        AgentContext ctx = AgentContext.builder()
                .researchId("r1")
                .budget(highBudget())
                .build();
        ctx.addOutputTokens(200L);
        assertThat(ctx.getTotalOutputTokens().get()).isEqualTo(200L);
    }

    @Test
    void conductCount_isIncrementable() {
        AgentContext ctx = AgentContext.builder()
                .researchId("r1")
                .budget(highBudget())
                .build();
        ctx.getConductCount().incrementAndGet();
        ctx.getConductCount().incrementAndGet();
        assertThat(ctx.getConductCount().get()).isEqualTo(2);
    }
}
