package dev.haotangyuan.knownote.studio.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.haotangyuan.knownote.studio.sse.StudioSseHub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeGenPipeline {

    private final CodeGenAgent codeGenAgent;
    private final StudioSseHub sseHub;
    private final ObjectMapper objectMapper;

    private static final String WORKSPACE_BASE = "/tmp/knownote-studio";

    @Async("studioTaskExecutor")
    public void run(String projectId, String userPrompt) {
        try {
            // ── Phase 1: Architect — plan the files ───────────────────────
            sseHub.send(projectId, "phase", Map.of("phase", "architect", "message", "Planning files..."));

            String planJson = codeGenAgent.chat(StudioPrompts.ARCHITECT_SYSTEM, userPrompt);
            log.info("[{}] Architect plan: {}", projectId, planJson);

            // Parse the JSON plan
            JsonNode root = objectMapper.readTree(extractJson(planJson));
            JsonNode filesNode = root.get("files");
            if (filesNode == null || !filesNode.isArray()) {
                sseHub.send(projectId, "error", Map.of("message", "Architect returned invalid JSON"));
                return;
            }

            List<FileSpec> fileSpecs = new ArrayList<>();
            for (JsonNode f : filesNode) {
                fileSpecs.add(new FileSpec(
                        f.get("path").asText(),
                        f.get("description").asText()
                ));
            }
            sseHub.send(projectId, "architect_done", Map.of(
                    "files", fileSpecs.stream().map(FileSpec::path).toList()
            ));

            // Build a summary of all planned files for context injection
            String fileListSummary = fileSpecs.stream()
                    .map(f -> "- " + f.path() + ": " + f.description())
                    .collect(Collectors.joining("\n"));

            // ── Phase 2: Coder — generate each file sequentially ──────────
            Path workspaceDir = Path.of(WORKSPACE_BASE, projectId);
            Files.createDirectories(workspaceDir);

            for (FileSpec spec : fileSpecs) {
                sseHub.send(projectId, "file_start", Map.of("path", spec.path()));
                log.info("[{}] Generating file: {}", projectId, spec.path());

                StringBuilder fileContent = new StringBuilder();
                String userMsg = StudioPrompts.coderUserMessage(spec.path(), spec.description(), fileListSummary);

                codeGenAgent.streamChat(StudioPrompts.CODER_SYSTEM, userMsg, token -> {
                    fileContent.append(token);
                    sseHub.send(projectId, "file_chunk", Map.of(
                            "path", spec.path(),
                            "content", token
                    ));
                });

                // Write completed file to workspace — normalize and verify path is inside workspace
                Path filePath = workspaceDir.resolve(spec.path()).normalize();
                if (!filePath.startsWith(workspaceDir)) {
                    log.warn("[{}] Rejected unsafe path: {}", projectId, spec.path());
                    continue;
                }
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, fileContent.toString());

                sseHub.send(projectId, "file_done", Map.of("path", spec.path()));
                log.info("[{}] File written: {}", projectId, spec.path());
            }

            sseHub.send(projectId, "gen_summary", Map.of(
                    "filesGenerated", fileSpecs.size(),
                    "projectId", projectId
            ));

        } catch (Exception e) {
            log.error("[{}] Code generation failed", projectId, e);
            sseHub.send(projectId, "error", Map.of("message",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        } finally {
            sseHub.complete(projectId);
        }
    }

    /** Strip markdown code fences if the LLM wrapped the JSON */
    private String extractJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) return trimmed.substring(start, end).trim();
        }
        return trimmed;
    }

    private record FileSpec(String path, String description) {}
}
