package dev.haotangyuan.knownote.research.framework;

import dev.haotangyuan.knownote.config.ResearchProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentContextTest {

    private ResearchProperties.BudgetLevel highBudget() {
        ResearchProperties.BudgetLevel b = new ResearchProperties.BudgetLevel();
        b.setMaxConductCount(5);
        b.setMaxSearchCount(10);
        b.setMaxConcurrentUnits(3);
        return b;
    }

    @Test
    void builder_storesResearchIdAndBudget() {
        ResearchProperties.BudgetLevel budget = highBudget();
        AgentContext ctx = AgentContext.builder()
                .researchId("r42")
                .budget(budget)
                .build();
        assertThat(ctx.getResearchId()).isEqualTo("r42");
        assertThat(ctx.getBudget()).isSameAs(budget);
    }

    @Test
    void builder_requiresResearchId() {
        assertThatThrownBy(() -> AgentContext.builder().budget(highBudget()).build())
                .isInstanceOf(NullPointerException.class);
    }
}
