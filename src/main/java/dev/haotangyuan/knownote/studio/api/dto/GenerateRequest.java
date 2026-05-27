package dev.haotangyuan.knownote.studio.api.dto;

import lombok.Data;

@Data
public class GenerateRequest {
    /** The user's natural-language instruction, e.g. "Build a Todo app with auth" */
    private String message;
}
