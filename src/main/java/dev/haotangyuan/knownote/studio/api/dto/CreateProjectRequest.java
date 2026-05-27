package dev.haotangyuan.knownote.studio.api.dto;

import lombok.Data;

@Data
public class CreateProjectRequest {
    private String name;
    private String description;
}
