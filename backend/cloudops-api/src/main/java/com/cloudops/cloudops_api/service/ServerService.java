package com.cloudops.cloudops_api.service;

import com.cloudops.cloudops_api.entity.Server;
import com.cloudops.cloudops_api.repository.ServerRepository;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ServerService {

    private final ServerRepository repository;
    private final PrometheusTargetService prometheusTargetService;

    public ServerService(
            ServerRepository repository,
            PrometheusTargetService prometheusTargetService) {

        this.repository = repository;
        this.prometheusTargetService = prometheusTargetService;
    }

    public List<Server> getAllServers() {
        return repository.findAll();
    }

    public Server getServerById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Server not found"));
    }

    public Server addServer(Server server) {

        server.setStatus("UNKNOWN");
        server.setCpu(0);
        server.setMemory(0);

        if (server.getServerType() == null ||
                server.getServerType().isBlank()) {
            throw new IllegalArgumentException(
                    "Server type is required");
        }

        Server savedServer = repository.save(server);
        prometheusTargetService.refreshTargets();

        return savedServer;
    }

    // Automatic registration and heartbeat from CloudOps Agent
    public Server heartbeatDetectedServer(Server detected) {

        String host = detected.getHost();
        String type = detected.getServerType();

        if (host == null || type == null) {
            throw new IllegalArgumentException("Host and server type are required");
        }

        type = type.toUpperCase();

        Optional<Server> existing;

        if ("NODEJS".equals(type)) {
            Integer port = detected.getServicePort();

            if (port == null) {
                throw new IllegalArgumentException("Node.js service port is required");
            }

            existing = repository.findFirstByHostAndServerTypeAndServicePort(
                    host, type, port
            );
        } else {
            Integer port = detected.getExporterPort();

            if (port == null) {
                throw new IllegalArgumentException("Exporter port is required");
            }

            existing = repository.findFirstByHostAndServerTypeAndExporterPort(
                    host, type, port
            );
        }

        Server server = existing.orElseGet(Server::new);

        server.setName(detected.getName());
        server.setHost(host);
        server.setServerType(type);
        server.setExporterPort(detected.getExporterPort());
        server.setServicePort(detected.getServicePort());
        server.setStatus(detected.getStatus());

        return repository.save(server);
    }

    public Server updateServer(Long id, Server updatedServer) {

        Server existing = getServerById(id);

        existing.setName(updatedServer.getName());
        existing.setHost(updatedServer.getHost());
        existing.setServerType(updatedServer.getServerType());
        existing.setExporterPort(updatedServer.getExporterPort());

        Server savedServer = repository.save(existing);
        prometheusTargetService.refreshTargets();

        return savedServer;
    }

    public void deleteServer(Long id) {

        Server server = getServerById(id);
        repository.delete(server);

        prometheusTargetService.refreshTargets();
    }
    public Server updateAlertEmail(Long id, String alertEmail) {
        Server server = getServerById(id);
        server.setAlertEmail(alertEmail);
        return repository.save(server);
    }
}