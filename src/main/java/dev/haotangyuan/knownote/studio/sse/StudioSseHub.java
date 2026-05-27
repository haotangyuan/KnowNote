package dev.haotangyuan.knownote.studio.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Lightweight SSE hub for Studio module.
 * Keyed by projectId; no ownership checks (demo-only).
 */
@Component
@Slf4j
public class StudioSseHub {

    // projectId -> list of active emitters
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(String projectId) {
        SseEmitter emitter = new SseEmitter(0L);  // no timeout
        emitters.computeIfAbsent(projectId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> remove(projectId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());

        log.debug("SSE connected: project={}", projectId);
        return emitter;
    }

    private void remove(String projectId, SseEmitter emitter) {
        // computeIfPresent holds the bucket lock — eliminates TOCTOU race where
        // a concurrent connect() could add a new emitter to the list between
        // isEmpty() and emitters.remove(), silently orphaning it.
        emitters.computeIfPresent(projectId, (k, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;  // null return atomically removes the map entry
        });
    }

    /**
     * Send a named SSE event with a JSON-serialisable payload to all
     * clients subscribed to the given projectId.
     */
    public void send(String projectId, String eventType, Object payload) {
        List<SseEmitter> list = emitters.get(projectId);
        if (list == null || list.isEmpty()) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(payload));
            } catch (IOException | IllegalStateException e) {
                // IllegalStateException is thrown when the emitter was already completed
                // (e.g. timeout callback fired) but hasn't been evicted from the list yet.
                // Catching it prevents aborting the loop early and dropping events for
                // remaining healthy subscribers.
                log.debug("SSE send failed ({}), removing emitter: project={}", e.getClass().getSimpleName(), projectId);
                remove(projectId, emitter);
            }
        }
    }

    /** Signal generation is complete and close all emitters for this project. */
    public void complete(String projectId) {
        send(projectId, "gen_complete", Map.of("done", true));
        List<SseEmitter> list = emitters.remove(projectId);
        if (list == null) return;
        for (SseEmitter emitter : list) {
            try { emitter.complete(); } catch (Exception ignored) { }
        }
        log.debug("SSE completed: project={}", projectId);
    }

    @PreDestroy
    void shutdown() {
        emitters.forEach((projectId, list) ->
                list.forEach(e -> { try { e.complete(); } catch (Exception ignored) { } }));
        emitters.clear();
        log.debug("StudioSseHub shut down, all emitters completed");
    }
}
