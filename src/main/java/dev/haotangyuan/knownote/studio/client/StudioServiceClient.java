package dev.haotangyuan.knownote.studio.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StudioServiceClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${studio.service.url:http://localhost:3001}")
    private String studioServiceUrl;

    /**
     * Tell the Studio Service to start a container for the given project.
     * Blocks until the container is running (up to 60 s).
     */
    public void startContainer(String projectId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(studioServiceUrl + "/containers/" + projectId + "/start"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Start container [{}]: status={}, body={}", projectId, response.statusCode(), response.body());
        } catch (Exception e) {
            log.warn("Could not start container for project {}: {}", projectId, e.getMessage());
        }
    }

    /** Returns the container status JSON as a string. */
    public String getContainerStatus(String projectId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(studioServiceUrl + "/containers/" + projectId + "/status"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            log.warn("Could not get container status for project {}: {}", projectId, e.getMessage());
            return "{\"status\":\"unknown\"}";
        }
    }
}
