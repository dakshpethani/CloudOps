
package com.cloudops.cloudops_api.service;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class PrometheusService {

    private final String PROMETHEUS_URL = "http://localhost:9090";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private double executeQuery(String query) throws Exception {

        String encodedQuery = URLEncoder.encode(
                query, StandardCharsets.UTF_8
        );

        String url = PROMETHEUS_URL
                + "/api/v1/query?query=" + encodedQuery;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Prometheus returned HTTP " + response.statusCode()
            );
        }

        JsonNode json = objectMapper.readTree(response.body());

        JsonNode result = json.path("data").path("result");

        if (!result.isArray() || result.isEmpty()) {
            throw new RuntimeException(
                    "No metric data received from Prometheus for query: " + query
            );
        }

        return result.get(0)
                .path("value")
                .get(1)
                .asDouble();
    }

    public double getCpuUsage() throws Exception {

        String query =
                "100 * (1 - avg(rate(windows_cpu_time_seconds_total{mode=\"idle\"}[5m])))";

        return executeQuery(query);
    }

    public double getMemoryUsage() throws Exception {

        String query =
                "100 * (1 - windows_memory_available_bytes / windows_memory_physical_total_bytes)";

        return executeQuery(query);
    }

    public double getDiskUsage() throws Exception {
        String query = "100 * (1 - (windows_logical_disk_free_bytes{volume=\"C:\"} / windows_logical_disk_size_bytes{volume=\"C:\"}))";
        return executeQuery(query);
    }

    public double getUptimeSeconds() throws Exception {
        String query = "time() - windows_system_boot_time_timestamp";
        return executeQuery(query);
    }

    // ================= SERVER-SPECIFIC WINDOWS METRICS =================

    public double getCpuUsage(long serverId) throws Exception {
        String query =
                "100 * (1 - avg by (server_id) " +
                        "(rate(windows_cpu_time_seconds_total" +
                        "{mode=\"idle\",server_id=\"" + serverId + "\"}[5m])))";

        return executeQuery(query);
    }

    public double getMemoryUsage(long serverId) throws Exception {
        String query =
                "100 * (1 - (" +
                        "windows_memory_available_bytes{server_id=\"" + serverId + "\"}" +
                        " / " +
                        "windows_memory_physical_total_bytes{server_id=\"" + serverId + "\"}" +
                        "))";

        return executeQuery(query);
    }

    public double getDiskUsage(long serverId) throws Exception {
        String query =
                "100 * (1 - (" +
                        "windows_logical_disk_free_bytes{volume=\"C:\",server_id=\"" + serverId + "\"}" +
                        " / " +
                        "windows_logical_disk_size_bytes{volume=\"C:\",server_id=\"" + serverId + "\"}" +
                        "))";

        return executeQuery(query);
    }

    public double getUptimeSeconds(long serverId) throws Exception {
        String query =
                "time() - windows_system_boot_time_timestamp" +
                        "{server_id=\"" + serverId + "\"}";

        return executeQuery(query);
    }

    public double getServerUp(long serverId) throws Exception {
        String query = "up{server_id=\"" + serverId + "\"}";
        return executeQuery(query);
    }



    public List<Map<String, Double>> getMetricHistory(
            String query, int minutes) throws Exception {

        long end = Instant.now().getEpochSecond();
        long start = end - (minutes * 60L);

        String encodedQuery = URLEncoder.encode(
                query, StandardCharsets.UTF_8
        );

        String url = PROMETHEUS_URL
                + "/api/v1/query_range?query=" + encodedQuery
                + "&start=" + start
                + "&end=" + end
                + "&step=15s";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Prometheus returned HTTP " + response.statusCode()
            );
        }

        JsonNode json = objectMapper.readTree(response.body());

        JsonNode result = json.path("data").path("result");

        List<Map<String, Double>> history = new ArrayList<>();

        if (!result.isArray()) {
            throw new RuntimeException("Invalid Prometheus response");
        }

        for (JsonNode series : result) {
            JsonNode values = series.path("values");

            for (JsonNode point : values) {
                Map<String, Double> dataPoint = new HashMap<>();

                dataPoint.put("timestamp", point.get(0).asDouble());
                dataPoint.put("value", Double.parseDouble(
                        point.get(1).asString()
                ));

                history.add(dataPoint);
            }
        }

        return history;
    }

    public List<Map<String, Double>> getCpuHistory() throws Exception {
        String query =
                "100 * (1 - avg(rate(windows_cpu_time_seconds_total{mode=\"idle\"}[5m])))";

        return getMetricHistory(query, 180);
    }

    public List<Map<String, Double>> getMemoryHistory() throws Exception {
        String query =
                "100 * (1 - windows_memory_available_bytes / windows_memory_physical_total_bytes)";

        return getMetricHistory(query, 180);
    }

    public List<Map<String, Double>> getDiskHistory() throws Exception {
        String query =
                "100 * (1 - (windows_logical_disk_free_bytes{volume=\"C:\"} / windows_logical_disk_size_bytes{volume=\"C:\"}))";

        return getMetricHistory(query, 180);
    }

    // ================= SERVER-SPECIFIC WINDOWS HISTORY =================

    public List<Map<String, Double>> getCpuHistory(long serverId)
            throws Exception {

        String query =
                "100 * (1 - avg by (server_id) " +
                        "(rate(windows_cpu_time_seconds_total" +
                        "{mode=\"idle\",server_id=\"" + serverId + "\"}[5m])))";

        return getMetricHistory(query, 180);
    }

    public List<Map<String, Double>> getMemoryHistory(long serverId)
            throws Exception {

        String query =
                "100 * (1 - (" +
                        "windows_memory_available_bytes{server_id=\"" + serverId + "\"}" +
                        " / " +
                        "windows_memory_physical_total_bytes{server_id=\"" + serverId + "\"}" +
                        "))";

        return getMetricHistory(query, 180);
    }

    public List<Map<String, Double>> getDiskHistory(long serverId)
            throws Exception {

        String query =
                "100 * (1 - (" +
                        "windows_logical_disk_free_bytes{volume=\"C:\",server_id=\"" + serverId + "\"}" +
                        " / " +
                        "windows_logical_disk_size_bytes{volume=\"C:\",server_id=\"" + serverId + "\"}" +
                        "))";

        return getMetricHistory(query, 180);
    }

    // ================= TOMCAT METRICS =================

    private String tomcatSelector(long serverId) {
        return "{server_id=\"" + serverId + "\"}";
    }

    // Tomcat JVM heap memory usage (%)
    public double getTomcatHeapUsage(long serverId) throws Exception {

        String query =
                "100 * (" +
                        "jvm_memory_used_bytes{area=\"heap\",server_id=\"" + serverId + "\"}" +
                        " / " +
                        "jvm_memory_max_bytes{area=\"heap\",server_id=\"" + serverId + "\"}" +
                        ")";

        return executeQuery(query);
    }

    // Tomcat JVM heap memory used (MB)
    public double getTomcatHeapUsedMB(long serverId) throws Exception {

        String query =
                "jvm_memory_used_bytes{area=\"heap\",server_id=\"" + serverId + "\"} / 1024 / 1024";

        return executeQuery(query);
    }

    // Tomcat Java process CPU usage (% of one CPU core)
    public double getTomcatCpuUsage(long serverId) throws Exception {

        String query =
                "rate(process_cpu_seconds_total{server_id=\"" + serverId + "\"}[1m]) * 100";

        return executeQuery(query);
    }

    // Current Tomcat JVM thread count
    public double getTomcatThreadCount(long serverId) throws Exception {

        String query =
                "jvm_threads_current{server_id=\"" + serverId + "\"}";

        return executeQuery(query);
    }


// ================= NODE.JS APPLICATION METRICS =================

    // Check whether a Node.js application is running
    public double getNodeJsAppUp(int port) throws Exception {

        String query =
                "max(cloudops_nodejs_app_up{port=\"" + port + "\"}) or vector(0)";

        return executeQuery(query);
    }

    // Node.js process CPU usage (% of one CPU core)
    public double getNodeJsCpuUsage(int port) throws Exception {

        String query =
                "rate(cloudops_nodejs_process_cpu_seconds_total{port=\""
                        + port + "\"}[1m]) * 100 or vector(0)";

        return executeQuery(query);
    }

    // Node.js process memory usage (MB)
    public double getNodeJsMemoryMB(int port) throws Exception {

        String query =
                "cloudops_nodejs_process_memory_bytes{port=\""
                        + port + "\"} / 1024 / 1024 or vector(0)";

        return executeQuery(query);
    }

    // Get CPU and memory for a specific Node.js application
    public Map<String, Double> getNodeJsMetrics(int port) throws Exception {

        Map<String, Double> metrics = new HashMap<>();

        metrics.put("cpuUsagePercent", getNodeJsCpuUsage(port));
        metrics.put("memoryUsageMB", getNodeJsMemoryMB(port));

        return metrics;
    }

    // ================= TOMCAT HISTORICAL METRICS =================

    public List<Map<String, Double>> getTomcatCpuHistory(long serverId)
            throws Exception {

        String query =
                "rate(process_cpu_seconds_total{server_id=\"" +
                        serverId + "\"}[1m]) * 100";

        return getMetricHistory(query, 180);
    }

    public List<Map<String, Double>> getTomcatMemoryHistory(long serverId)
            throws Exception {

        String query =
                "jvm_memory_used_bytes{area=\"heap\",server_id=\"" +
                        serverId + "\"} / 1024 / 1024";

        return getMetricHistory(query, 180);
    }

    public List<Map<String, Double>> getTomcatThreadHistory(long serverId)
            throws Exception {

        String query =
                "jvm_threads_current{server_id=\"" +
                        serverId + "\"}";

        return getMetricHistory(query, 180);
    }

    // ================= NODE.JS HISTORICAL METRICS =================

    public List<Map<String, Double>> getNodeJsCpuHistory(int port)
            throws Exception {

        String query =
                "rate(cloudops_nodejs_process_cpu_seconds_total{port=\"" +
                        port + "\"}[1m]) * 100 or vector(0)";

        return getMetricHistory(query, 180);
    }

    public List<Map<String, Double>> getNodeJsMemoryHistory(int port)
            throws Exception {

        String query =
                "cloudops_nodejs_process_memory_bytes{port=\"" +
                        port + "\"} / 1024 / 1024 or vector(0)";

        return getMetricHistory(query, 180);
    }

    public List<Map<String, Double>> getNodeJsAppHistory(int port)
            throws Exception {

        String query =
                "max(cloudops_nodejs_app_up{port=\"" +
                        port + "\"}) or vector(0)";

        return getMetricHistory(query, 180);
    }

// ================= CLOUDOPS AGENT METRICS =================



public Map<String, Double> getAgentMetrics() throws Exception {

    Map<String, Double> metrics = new HashMap<>();

    metrics.put("cpuUsagePercent",
            executeQuery("cloudops_cpu_usage_percent"));

    double memoryUsed = executeQuery("cloudops_memory_used_bytes");
    double memoryTotal = executeQuery("cloudops_memory_total_bytes");

    double memoryPercent = memoryTotal > 0
            ? (memoryUsed / memoryTotal) * 100
            : 0;

    metrics.put("memoryUsagePercent", memoryPercent);

    metrics.put("diskTotalBytes",
            executeQuery("cloudops_disk_total_bytes"));

    metrics.put("diskAvailableBytes",
            executeQuery("cloudops_disk_available_bytes"));

    double diskTotal = metrics.get("diskTotalBytes");
    double diskAvailable = metrics.get("diskAvailableBytes");

    double diskPercent = diskTotal > 0
            ? ((diskTotal - diskAvailable) / diskTotal) * 100
            : 0;

    metrics.put("diskUsagePercent", diskPercent);

    metrics.put("uptimeSeconds",
            executeQuery("cloudops_system_uptime_seconds"));

    metrics.put("nodeProcessRunning",
            executeQuery("cloudops_nodejs_process_running"));

    return metrics;
}



}
