
package com.cloudops.cloudops_api.controller;

import com.cloudops.cloudops_api.entity.Server;
import com.cloudops.cloudops_api.entity.Alert;
import com.cloudops.cloudops_api.service.ServerService;
import com.cloudops.cloudops_api.service.AlertService;
import com.cloudops.cloudops_api.service.PrometheusService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "http://localhost:5173")
public class MonitoringController {

    private final PrometheusService prometheusService;
    private final AlertService alertService;
    private final ServerService serverService;

    public MonitoringController(
            PrometheusService prometheusService,
            AlertService alertService,
            ServerService serverService
    ) {
        this.prometheusService = prometheusService;
        this.alertService = alertService;
        this.serverService = serverService;
    }

    // Get Windows server metrics and generate alerts
    @GetMapping("/metrics")

    public Map<String, Double> getMetrics() throws Exception {

        // Get all registered servers
        List<Server> servers = serverService.getAllServers();

        // Find the first Windows server for the existing dashboard response
        Server windowsServer = servers.stream()
                .filter(server ->
                        "WINDOWS".equalsIgnoreCase(server.getServerType()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No registered Windows server found"));

        Long serverId = windowsServer.getId();
        String serverName = windowsServer.getName();

        // Fetch metrics specifically for this Windows server
        double cpu = prometheusService.getCpuUsage(serverId);
        double memory = prometheusService.getMemoryUsage(serverId);
        double disk = prometheusService.getDiskUsage(serverId);
        double uptime = prometheusService.getUptimeSeconds(serverId);

        // CPU alert
        alertService.updateAlert(
                "cpu-alert-" + serverId,
                serverId,
                serverName,
                "CPU usage is " + String.format("%.1f", cpu) + "%",
                cpu > 90 ? "Critical" : "Warning",
                cpu > 80
        );

        // Memory alert
        alertService.updateAlert(
                "memory-alert-" + serverId,
                serverId,
                serverName,
                "Memory usage is " + String.format("%.1f", memory) + "%",
                memory > 95 ? "Critical" : "Warning",
                memory > 85
        );

        // Disk alert
        alertService.updateAlert(
                "disk-alert-" + serverId,
                serverId,
                serverName,
                "Disk usage is " + String.format("%.1f", disk) + "%",
                disk > 90 ? "Critical" : "Warning",
                disk > 80
        );

        return Map.of(
                "cpuUsagePercent", cpu,
                "memoryUsagePercent", memory,
                "diskUsagePercent", disk,
                "uptimeSeconds", uptime
        );
    }


    // CPU history
    @GetMapping("/history/cpu")
    public List<Map<String, Double>> getCpuHistory() throws Exception {
        return prometheusService.getCpuHistory();
    }

    // Memory history
    @GetMapping("/history/memory")
    public List<Map<String, Double>> getMemoryHistory() throws Exception {
        return prometheusService.getMemoryHistory();
    }

    // Disk history
    @GetMapping("/history/disk")
    public List<Map<String, Double>> getDiskHistory() throws Exception {
        return prometheusService.getDiskHistory();
    }

    // ================= SERVER-SPECIFIC HISTORY =================


    // ================= SERVER-SPECIFIC HISTORY =================

    // CPU history for Windows, Tomcat and Node.js
    @GetMapping("/servers/{id}/history/cpu")
    public List<Map<String, Double>> getServerCpuHistory(
            @PathVariable Long id) throws Exception {

        Server server = serverService.getServerById(id);
        String type = server.getServerType();

        if ("WINDOWS".equalsIgnoreCase(type)) {
            return prometheusService.getCpuHistory(id);
        }

        if ("TOMCAT".equalsIgnoreCase(type)) {
            return prometheusService.getTomcatCpuHistory(id);
        }

        if ("NODEJS".equalsIgnoreCase(type)) {
            return prometheusService.getNodeJsCpuHistory(
                    server.getServicePort()
            );
        }

        throw new IllegalArgumentException(
                "CPU history is not supported for " + type
        );
    }

    // Memory history for Windows, Tomcat and Node.js
    @GetMapping("/servers/{id}/history/memory")
    public List<Map<String, Double>> getServerMemoryHistory(
            @PathVariable Long id) throws Exception {

        Server server = serverService.getServerById(id);
        String type = server.getServerType();

        if ("WINDOWS".equalsIgnoreCase(type)) {
            return prometheusService.getMemoryHistory(id);
        }

        if ("TOMCAT".equalsIgnoreCase(type)) {
            return prometheusService.getTomcatMemoryHistory(id);
        }

        if ("NODEJS".equalsIgnoreCase(type)) {
            return prometheusService.getNodeJsMemoryHistory(
                    server.getServicePort()
            );
        }

        throw new IllegalArgumentException(
                "Memory history is not supported for " + type
        );
    }

    // Third historical metric:
    // Windows = Disk, Tomcat = Threads, Node.js = App Status
    @GetMapping("/servers/{id}/history/disk")
    public List<Map<String, Double>> getServerDiskHistory(
            @PathVariable Long id) throws Exception {

        Server server = serverService.getServerById(id);
        String type = server.getServerType();

        if ("WINDOWS".equalsIgnoreCase(type)) {
            return prometheusService.getDiskHistory(id);
        }

        if ("TOMCAT".equalsIgnoreCase(type)) {
            return prometheusService.getTomcatThreadHistory(id);
        }

        if ("NODEJS".equalsIgnoreCase(type)) {
            return prometheusService.getNodeJsAppHistory(
                    server.getServicePort()
            );
        }

        throw new IllegalArgumentException(
                "Historical monitoring is not supported for " + type
        );
    }





    // Get all saved alerts from MySQL
    @GetMapping("/alerts")
    public List<Alert> getAlertHistory() {
        return alertService.getAlertHistory();
    }

    // Get individual server metrics

    @GetMapping("/servers/{id}/metrics")
    public Map<String, Object> getServerMetrics(@PathVariable Long id)
            throws Exception {

        // Fetch registered server from MySQL
        Server server = serverService.getServerById(id);

        // WINDOWS monitoring
        if ("WINDOWS".equalsIgnoreCase(server.getServerType())) {

            double isUp = prometheusService.getServerUp(id);

            if (isUp == 0) {
                return Map.of(
                        "serverId", id,
                        "serverName", server.getName(),
                        "serverType", "WINDOWS",
                        "status", "OFFLINE",
                        "cpuUsagePercent", -1.0,
                        "memoryUsagePercent", -1.0,
                        "diskUsagePercent", -1.0,
                        "uptimeSeconds", -1.0
                );
            }

            double cpu = prometheusService.getCpuUsage(id);
            double memory = prometheusService.getMemoryUsage(id);
            double disk = prometheusService.getDiskUsage(id);
            double uptime = prometheusService.getUptimeSeconds(id);

            return Map.of(
                    "serverId", id,
                    "serverName", server.getName(),
                    "serverType", "WINDOWS",
                    "status", "ONLINE",
                    "cpuUsagePercent", cpu,
                    "memoryUsagePercent", memory,
                    "diskUsagePercent", disk,
                    "uptimeSeconds", uptime
            );
        }

        // TOMCAT monitoring
        // TOMCAT monitoring
        if ("TOMCAT".equalsIgnoreCase(server.getServerType())) {

            double isUp = prometheusService.getServerUp(id);

            if (isUp < 1) {
                return Map.of(
                        "serverId", id,
                        "serverName", server.getName(),
                        "serverType", "TOMCAT",
                        "status", "OFFLINE",
                        "cpuUsagePercent", -1.0,
                        "memoryUsagePercent", -1.0,
                        "heapUsedMB", -1.0,
                        "threadCount", -1.0
                );
            }

            double cpu = prometheusService.getTomcatCpuUsage(id);
            double memory = prometheusService.getTomcatHeapUsage(id);
            double heapUsed = prometheusService.getTomcatHeapUsedMB(id);
            double threads = prometheusService.getTomcatThreadCount(id);

            return Map.of(
                    "serverId", id,
                    "serverName", server.getName(),
                    "serverType", "TOMCAT",
                    "status", "ONLINE",
                    "cpuUsagePercent", cpu,
                    "memoryUsagePercent", memory,
                    "heapUsedMB", heapUsed,
                    "threadCount", threads
            );
        }

        // NODEJS monitoring
        if ("NODEJS".equalsIgnoreCase(server.getServerType())) {

            Integer servicePort = server.getServicePort();

            if (servicePort == null) {
                throw new IllegalStateException(
                        "Node.js service port is not configured"
                );
            }

            double isUp = prometheusService.getNodeJsAppUp(servicePort);

            double cpu = prometheusService.getNodeJsCpuUsage(servicePort);
            double memory = prometheusService.getNodeJsMemoryMB(servicePort);

            return Map.of(
                    "serverId", id,
                    "serverName", server.getName(),
                    "serverType", "NODEJS",
                    "servicePort", servicePort,
                    "status", isUp >= 1 ? "ONLINE" : "OFFLINE",
                    "cpuUsagePercent", cpu,
                    "memoryUsageMB", memory
            );
        }

        throw new IllegalArgumentException(
                "Monitoring for server type "
                        + server.getServerType()
                        + " is not implemented yet"
        );
    }


    // Get CloudOps agent metrics
    @GetMapping("/agent-metrics")
    public Map<String, Double> getAgentMetrics() throws Exception {
        return prometheusService.getAgentMetrics();
    }
}
