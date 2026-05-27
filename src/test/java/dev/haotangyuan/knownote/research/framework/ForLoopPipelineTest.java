package dev.haotangyuan.knownote.research.framework;

import dev.haotangyuan.knownote.config.ResearchProperties;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ForLoopPipelineTest {

    private AgentContext testCtx(int maxIterations) {
        ResearchProperties.BudgetLevel b = new ResearchProperties.BudgetLevel();
        b.setMaxConductCount(maxIterations);
        b.setMaxSearchCount(10);
        b.setMaxConcurrentUnits(1);
        return AgentContext.builder().researchId("test").budget(b).build();
    }

    @Test
    void runsAtMostMaxIterationsTimes() {
        AtomicInteger count = new AtomicInteger(0);

        Agent counter = new Agent() {
            public String name() { return "Counter"; }
            public Msg reply(Msg input, AgentContext ctx) {
                count.incrementAndGet();
                return Msg.of("assistant", "Counter", "continue");
            }
        };

        ForLoopPipeline pipeline = new ForLoopPipeline(counter, 3,
                (msg, ctx) -> false); // never stop early
        pipeline.run(Msg.of("user", "u", "start"), testCtx(3));

        assertThat(count.get()).isEqualTo(3);
    }

    @Test
    void stopsEarlyWhenStopConditionMet() {
        AtomicInteger count = new AtomicInteger(0);

        Agent counter = new Agent() {
            public String name() { return "Counter"; }
            public Msg reply(Msg input, AgentContext ctx) {
                count.incrementAndGet();
                return Msg.of("assistant", "Counter", "done");
            }
        };

        ForLoopPipeline pipeline = new ForLoopPipeline(counter, 10,
                (msg, ctx) -> "done".equals(msg.contentAsString())); // stop when agent returns "done"
        pipeline.run(Msg.of("user", "u", "start"), testCtx(10));

        assertThat(count.get()).isEqualTo(1);
    }
}
