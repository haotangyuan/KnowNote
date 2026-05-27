package dev.haotangyuan.knownote.research.framework;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Runs a list of agents in order. Stops if any agent returns an error response.
 */
@Slf4j
public class SequentialPipeline implements Pipeline {

    private final List<Agent> agents;

    public SequentialPipeline(List<Agent> agents) {
        this.agents = List.copyOf(agents);
    }

    @Override
    public Msg run(Msg input, AgentContext ctx) {
        Msg current = input;
        for (Agent agent : agents) {
            log.debug("SequentialPipeline: running agent={} researchId={}",
                    agent.name(), ctx.getResearchId());
            current = agent.reply(current, ctx);
            if (isError(current)) {
                log.warn("SequentialPipeline: agent={} returned error, stopping pipeline. researchId={}",
                        agent.name(), ctx.getResearchId());
                break;
            }
        }
        return current;
    }

    private boolean isError(Msg msg) {
        if (msg == null || msg.content() == null) {
            return false;
        }
        if (msg.content() instanceof ServiceResponse<?> sr) {
            return sr.isError();
        }
        return false;
    }
}
