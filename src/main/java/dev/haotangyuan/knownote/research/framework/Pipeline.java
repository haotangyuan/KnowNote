package dev.haotangyuan.knownote.research.framework;

/**
 * Marker interface for all pipeline types.
 */
public interface Pipeline {
    Msg run(Msg input, AgentContext ctx);
}
