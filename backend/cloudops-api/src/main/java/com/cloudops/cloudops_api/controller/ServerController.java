package com.cloudops.cloudops_api.controller;

import com.cloudops.cloudops_api.entity.Server;
import com.cloudops.cloudops_api.service.ServerService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servers")
public class ServerController {

    private final ServerService service;

    public ServerController(ServerService service) {
        this.service = service;

    }

    @GetMapping
    public List<Server>getAllServers()
    {
        return service.getAllServers();
    }
    @GetMapping("/{id}")
    public Server getServerById(@PathVariable Long id)
    {
        return service.getServerById(id);
    }
    @PostMapping
    public Server addServer(@RequestBody Server server)
    {
        return service.addServer(server);
    }
    @PostMapping("/{id}")
    public Server updateServer(@PathVariable Long id, @RequestBody Server server)
    {
        return service.updateServer(id,server);
    }
    @DeleteMapping("/{id}")
    public String deleteServer(@PathVariable Long id )
    {
        service.deleteServer(id);
        return "Server Deleted successfully";
    }

    @PostMapping("/agent/heartbeat")
    public Server agentHeartbeat(@RequestBody Server server) {
        return service.heartbeatDetectedServer(server);
    }
    @GetMapping("/agent/node-ports")
    public List<Integer> getRegisteredNodePorts() {
        return service.getAllServers().stream()
                .filter(server -> "NODEJS".equalsIgnoreCase(server.getServerType()))
                .map(Server::getServicePort)
                .filter(port -> port != null)
                .distinct()
                .toList();
    }
    @PutMapping("/{id}/alert-email")
    public Server updateAlertEmail(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String alertEmail = request.get("alertEmail");

        return service.updateAlertEmail(id, alertEmail);
    }
}
