package dev.haotangyuan.knownote.research.framework;

import java.util.Collections;
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

    public static Msg of(String role, String name, Object content) {
        return new Msg(UUID.randomUUID().toString(), role, name, content, Collections.emptyMap());
    }

    public static Msg of(String role, String name, Object content, Map<String, Object> metadata) {
        return new Msg(UUID.randomUUID().toString(), role, name, content,
                metadata == null ? Collections.emptyMap() : Map.copyOf(metadata));
    }

    public String contentAsString() {
        return content == null ? "" : content.toString();
    }

    @SuppressWarnings("unchecked")
    public <T> T contentAs(Class<T> type) {
        return type.cast(content);
    }
}
