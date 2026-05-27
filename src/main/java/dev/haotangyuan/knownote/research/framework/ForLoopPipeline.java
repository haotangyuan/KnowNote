package dev.haotangyuan.knownote.research.framework;

import lombok.extern.slf4j.Slf4j;

import java.util.function.BiPredicate;

/**
 * Runs a single agent repeatedly up to maxIterations times,
 * stopping early if the stop condition is met.
 *
 * <p>Designed for simple retry/polling loops. The current research agents
 * ({@code SupervisorAgent}, {@code ResearcherAgent}) use hand-rolled loops
 * because they interleave tool-execution state; those loops are candidates
 * for migration to this pipeline in a future refactoring once tool dispatch
 * is extracted as a first-class abstraction.
 */
@Slf4j
public class ForLoopPipeline implements Pipeline {

    private final Agent agent;
    private final int maxIterations;
    private final BiPredicate<Msg, AgentContext> stopCondition;

    /**
     * @param agent         the agent to run in a loop
     * @param maxIterations hard upper bound on iterations
     * @param stopCondition return true to stop before maxIterations is reached
     */
    public ForLoopPipeline(Agent agent, int maxIterations,
                           BiPredicate<Msg, AgentContext> stopCondition) {
        this.agent = agent;
        this.maxIterations = maxIterations;
        this.stopCondition = stopCondition;
    }

    @Override
    public Msg run(Msg input, AgentContext ctx) {
        Msg current = input;
        for (int i = 0; i < maxIterations; i++) {
            log.debug("ForLoopPipeline: iteration={}/{} agent={} researchId={}",
                    i + 1, maxIterations, agent.name(), ctx.getResearchId());
            current = agent.reply(current, ctx);
            if (stopCondition.test(current, ctx)) {
                log.debug("ForLoopPipeline: stop condition met at iteration={}", i + 1);
                break;
            }
        }
        return current;
    }
}
