package dev.haotangyuan.knownote.research.framework;

/**
 * Contract every agent in the research pipeline must satisfy.
 */
public interface Agent {

    /** Human-readable name for logging. */
    String name();

    /** Process an incoming message and return a response. */
    Msg reply(Msg input, AgentContext ctx);

    /** Optional: observe a message without responding (e.g., for memory). */
    default void observe(Msg msg) {
    }
}
