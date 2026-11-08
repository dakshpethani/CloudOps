
package com.cloudops.cloudops_api.service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import com.cloudops.cloudops_api.entity.Server;
import com.cloudops.cloudops_api.repository.ServerRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Service
public class PrometheusTargetService {

    private final ServerRepository repository;
    private final ObjectMapper objectMapper;

    private final Path targetFile =
            Path.of("C:/Prometheus/file_sd/cloudops-targets.json");
    public PrometheusTargetService(
            ServerRepository repository
            ) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    public void refreshTargets() {

        try {
            List<Map<String, Object>> targets =
                    repository.findAll().stream()
                            .filter(server ->
                                    server.getHost() != null &&
                                            !server.getHost().isBlank() &&
                                            server.getExporterPort() != null &&
                                            server.getServerType() != null &&
                                            !server.getServerType().isBlank())
                            .map(this::createTarget)
                            .toList();

            Files.createDirectories(targetFile.getParent());

            Path tempFile = targetFile.resolveSibling("targets.tmp");

            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(tempFile.toFile(), targets);

            Files.move(
                    tempFile,
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING

            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to update Prometheus targets", e);
        }
    }

    private Map<String, Object> createTarget(Server server) {

        return Map.of(
                "targets",
                List.of(server.getHost() + ":" + server.getExporterPort()),
                "labels",
                Map.of(
                        "server_id", String.valueOf(server.getId()),
                        "server_name", server.getName(),
                        "server_type", server.getServerType()
                )
        );
    }
    @EventListener(ApplicationReadyEvent.class)
    public void syncTargetsOnStartup() {
        refreshTargets();
    }
}
