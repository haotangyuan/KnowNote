package dev.haotangyuan.knownote.research.framework;

import java.util.Map;
import java.util.UUID;

/**
 * Immutable message record passed between agents.
 */
public record Msg(
        String id,
        String role,
        String name,
        Object content,
        Map<String, Object> metadata) {

    // Compact canonical constructor to enforce metadata immutability
    public Msg {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static Msg of(String role, String name, Object content) {
        return new Msg(UUID.randomUUID().toString(), role, name, content, null);
    }

    public static Msg of(String role, String name, Object content, Map<String, Object> metadata) {
        return new Msg(UUID.randomUUID().toString(), role, name, content, metadata);
    }

    public String contentAsString() {
        return content == null ? "" : content.toString();
    }

    public <T> T contentAs(Class<T> type) {
        return type.cast(content);
    }
}
