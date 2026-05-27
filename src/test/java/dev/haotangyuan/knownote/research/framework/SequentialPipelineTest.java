package dev.haotangyuan.knownote.research.framework;

import dev.haotangyuan.knownote.config.ResearchProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SequentialPipelineTest {

    private AgentContext testCtx() {
        ResearchProperties.BudgetLevel b = new ResearchProperties.BudgetLevel();
        b.setMaxConductCount(3);
        b.setMaxSearchCount(5);
        b.setMaxConcurrentUnits(1);
        return AgentContext.builder().researchId("test").budget(b).build();
    }

    @Test
    void runsAllAgentsInOrder() {
        AtomicInteger order = new AtomicInteger(0);
        List<Integer> executionOrder = new ArrayList<>();

        Agent a1 = new Agent() {
            public String name() { return "A1"; }
            public Msg reply(Msg input, AgentContext ctx) {
                executionOrder.add(order.incrementAndGet());
                return Msg.of("assistant", "A1", "done");
            }
        };

        Agent a2 = new Agent() {
            public String name() { return "A2"; }
            public Msg reply(Msg input, AgentContext ctx) {
                executionOrder.add(order.incrementAndGet());
                return Msg.of("assistant", "A2", "done");
            }
        };

        SequentialPipeline pipeline = new SequentialPipeline(List.of(a1, a2));
        Msg input = Msg.of("user", "test", "go");
        Msg result = pipeline.run(input, testCtx());

        assertThat(executionOrder).containsExactly(1, 2);
        assertThat(result).isNotNull();
    }

    @Test
    void stopsOnErrorStatus() {
        AtomicInteger callCount = new AtomicInteger(0);

        Agent failing = new Agent() {
            public String name() { return "Failing"; }
            public Msg reply(Msg input, AgentContext ctx) {
                callCount.incrementAndGet();
                return Msg.of("assistant", "Failing", ServiceResponse.error("agent failed"));
            }
        };

        Agent shouldNotRun = new Agent() {
            public String name() { return "ShouldNotRun"; }
            public Msg reply(Msg input, AgentContext ctx) {
                callCount.incrementAndGet();
                return Msg.of("assistant", "ShouldNotRun", "ok");
            }
        };

        SequentialPipeline pipeline = new SequentialPipeline(List.of(failing, shouldNotRun));
        pipeline.run(Msg.of("user", "u", "go"), testCtx());

        assertThat(callCount.get()).isEqualTo(1);
    }
}
