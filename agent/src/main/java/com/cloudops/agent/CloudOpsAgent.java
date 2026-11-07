
package com.cloudops.agent;

import com.sun.net.httpserver.HttpServer;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;

public class CloudOpsAgent {

    private static final int PORT = 9405;

    private static final SystemInfo systemInfo = new SystemInfo();

    private static final CentralProcessor processor =
            systemInfo.getHardware().getProcessor();

    private static final GlobalMemory memory =
            systemInfo.getHardware().getMemory();

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", PORT), 0);

        server.createContext("/metrics", exchange -> {

            String response = generateMetrics();

            byte[] responseBytes =
                    response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "text/plain; version=0.0.4; charset=utf-8"
            );

            exchange.sendResponseHeaders(200, responseBytes.length);

            try (var output = exchange.getResponseBody()) {
                output.write(responseBytes);
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();

        System.out.println("==================================");
        System.out.println("       CLOUDOPS AGENT");
        System.out.println("==================================");
        System.out.println("Agent started successfully.");
        System.out.println("Metrics URL: http://localhost:" + PORT + "/metrics");
        System.out.println("Press Ctrl+C to stop the agent.");
    }

    private static String generateMetrics() {

        long[] previousTicks = processor.getSystemCpuLoadTicks();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double cpuUsage =
                processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100;

        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;

        StringBuilder metrics = new StringBuilder();

        // CPU metrics
        metrics.append("# HELP cloudops_cpu_usage_percent CPU usage percentage\n");
        metrics.append("# TYPE cloudops_cpu_usage_percent gauge\n");
        metrics.append("cloudops_cpu_usage_percent ")
                .append(cpuUsage).append("\n");

        // RAM metrics
        metrics.append("# HELP cloudops_memory_total_bytes Total RAM in bytes\n");
        metrics.append("# TYPE cloudops_memory_total_bytes gauge\n");
        metrics.append("cloudops_memory_total_bytes ")
                .append(totalMemory).append("\n");

        metrics.append("# HELP cloudops_memory_used_bytes Used RAM in bytes\n");
        metrics.append("# TYPE cloudops_memory_used_bytes gauge\n");
        metrics.append("cloudops_memory_used_bytes ")
                .append(usedMemory).append("\n");

        metrics.append("# HELP cloudops_memory_available_bytes Available RAM in bytes\n");
        metrics.append("# TYPE cloudops_memory_available_bytes gauge\n");
        metrics.append("cloudops_memory_available_bytes ")
                .append(availableMemory).append("\n");

        // Disk metrics
        FileSystem fileSystem =
                systemInfo.getOperatingSystem().getFileSystem();

        metrics.append("# HELP cloudops_disk_total_bytes Total disk space in bytes\n");
        metrics.append("# TYPE cloudops_disk_total_bytes gauge\n");

        metrics.append("# HELP cloudops_disk_available_bytes Available disk space in bytes\n");
        metrics.append("# TYPE cloudops_disk_available_bytes gauge\n");

        for (OSFileStore disk : fileSystem.getFileStores()) {

            String mount = disk.getMount()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");

            metrics.append("cloudops_disk_total_bytes{mount=\"")
                    .append(mount).append("\"} ")
                    .append(disk.getTotalSpace()).append("\n");

            metrics.append("cloudops_disk_available_bytes{mount=\"")
                    .append(mount).append("\"} ")
                    .append(disk.getUsableSpace()).append("\n");
        }

        // System uptime
        long uptime =
                systemInfo.getOperatingSystem().getSystemUptime();

        metrics.append("# HELP cloudops_system_uptime_seconds System uptime in seconds\n");
        metrics.append("# TYPE cloudops_system_uptime_seconds gauge\n");
        metrics.append("cloudops_system_uptime_seconds ")
                .append(uptime).append("\n");

        // Tomcat and Node.js metrics
        metrics.append(getTomcatMetrics());
        metrics.append(getNodeMetrics());

        return metrics.toString();
    }

    // ==========================================
    // TOMCAT METRICS
    // ==========================================

    private static String getTomcatMetrics() {

        boolean tomcatRunning = false;
        boolean jmxExporterUp = false;

        try {
            Process process = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-Command",
                    "Get-CimInstance Win32_Process -Filter \"name = 'java.exe'\" | " +
                            "Where-Object { $_.CommandLine -like '*org.apache.catalina.startup.Bootstrap*' } | " +
                            "Select-Object -ExpandProperty ProcessId"
            ).redirectErrorStream(true).start();

            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            tomcatRunning = !output.trim().isEmpty();

        } catch (Exception e) {
            System.err.println("Tomcat detection error: " + e.getMessage());
        }

        sendTomcatHeartbeat(tomcatRunning);

        if (tomcatRunning) {
            try {
                HttpURLConnection connection =
                        (HttpURLConnection) new URL(
                                "http://localhost:9404/metrics"
                        ).openConnection();

                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);

                jmxExporterUp = connection.getResponseCode() == 200;

                connection.disconnect();

            } catch (Exception e) {
                jmxExporterUp = false;
            }
        }

        return "# HELP cloudops_tomcat_running Tomcat process status\n" +
                "# TYPE cloudops_tomcat_running gauge\n" +
                "cloudops_tomcat_running " +
                (tomcatRunning ? 1 : 0) + "\n" +

                "# HELP cloudops_tomcat_jmx_exporter_up Tomcat JMX exporter status\n" +
                "# TYPE cloudops_tomcat_jmx_exporter_up gauge\n" +
                "cloudops_tomcat_jmx_exporter_up " +
                (jmxExporterUp ? 1 : 0) + "\n";
    }

    // ==========================================
    // NODE.JS METRICS
    // ==========================================

    private static String getNodeMetrics() {

        // Check all registered Node.js ports first.
        syncRegisteredNodeStatuses();

        StringBuilder metrics = new StringBuilder();

        metrics.append("# HELP cloudops_nodejs_process_running Node.js process status\n");
        metrics.append("# TYPE cloudops_nodejs_process_running gauge\n");

        metrics.append("# HELP cloudops_nodejs_app_port_listening Node.js application port status\n");
        metrics.append("# TYPE cloudops_nodejs_app_port_listening gauge\n");

        metrics.append("# HELP cloudops_nodejs_app_up Node.js application HTTP reachability\n");
        metrics.append("# TYPE cloudops_nodejs_app_up gauge\n");

        metrics.append("# HELP cloudops_nodejs_process_cpu_seconds_total Total CPU time used by Node.js process\n");
        metrics.append("# TYPE cloudops_nodejs_process_cpu_seconds_total counter\n");

        metrics.append("# HELP cloudops_nodejs_process_memory_bytes Node.js process memory in bytes\n");
        metrics.append("# TYPE cloudops_nodejs_process_memory_bytes gauge\n");

        try {

            String command =
                    "$nodes = Get-CimInstance -ClassName Win32_Process | Where-Object { $_.Name -eq 'node.exe' }; " +
                            "$results = foreach ($n in $nodes) { " +
                            "$p = Get-Process -Id $n.ProcessId -ErrorAction SilentlyContinue; " +
                            "if ($null -ne $p) { " +
                            "Get-NetTCPConnection -OwningProcess $n.ProcessId -ErrorAction SilentlyContinue | " +
                            "Where-Object { $_.LocalPort -gt 0 -and $_.RemotePort -eq 0 } | " +
                            "ForEach-Object { " +
                            "$address = $_.LocalAddress; " +
                            "if ($address -eq '::') { $address = '::1' }; " +
                            "if ($address -eq '0.0.0.0') { $address = '127.0.0.1' }; " +
                            "$cpu = if ($null -ne $p.CPU) { [string]::Format([cultureinfo]::InvariantCulture, '{0}', [double]$p.CPU) } else { '0' }; " +
                            "$mem = [string]$p.WorkingSet64; " +
                            "$n.ProcessId.ToString() + '|' + $address + '|' + $_.LocalPort + '|' + $cpu + '|' + $mem " +
                            "} } }; $results";

            Process process = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    command
            ).redirectErrorStream(true).start();

            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            boolean completed = process.waitFor(5, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                System.err.println("Node.js detection timed out.");
                return metrics.toString();
            }

            System.out.println("Node.js PowerShell output: " + output.trim());

            long nodeProcessCount = ProcessHandle.allProcesses()
                    .filter(p -> p.info().command()
                            .map(c -> c.toLowerCase().endsWith("node.exe"))
                            .orElse(false))
                    .count();

            metrics.append("cloudops_nodejs_process_running ")
                    .append(nodeProcessCount > 0 ? 1 : 0)
                    .append("\n");

            for (String line : output.split("\\R")) {

                String[] parts = line.trim().split("\\|");

                if (parts.length != 5) {
                    continue;
                }

                String pid = parts[0].trim();
                String address = parts[1].trim();
                String port = parts[2].trim();
                String cpuSeconds = parts[3].trim();
                String memoryBytes = parts[4].trim();

                boolean httpUp = false;

                try {
                    String host = address.contains(":")
                            ? "[" + address + "]"
                            : address;

                    HttpURLConnection connection =
                            (HttpURLConnection) new URL(
                                    "http://" + host + ":" + port + "/"
                            ).openConnection();

                    connection.setConnectTimeout(1500);
                    connection.setReadTimeout(1500);

                    int status = connection.getResponseCode();

                    httpUp = status >= 200 && status < 500;

                    connection.disconnect();

                } catch (Exception ignored) {
                    httpUp = false;
                }

                // Existing Prometheus metrics are preserved.
                // Heartbeats are handled centrally by syncRegisteredNodeStatuses().

                metrics.append("cloudops_nodejs_app_port_listening{pid=\"")
                        .append(pid)
                        .append("\",port=\"")
                        .append(port)
                        .append("\"} 1\n");

                metrics.append("cloudops_nodejs_app_up{pid=\"")
                        .append(pid)
                        .append("\",port=\"")
                        .append(port)
                        .append("\"} ")
                        .append(httpUp ? 1 : 0)
                        .append("\n");

                metrics.append("cloudops_nodejs_process_cpu_seconds_total{pid=\"")
                        .append(pid)
                        .append("\",port=\"")
                        .append(port)
                        .append("\"} ")
                        .append(cpuSeconds)
                        .append("\n");

                metrics.append("cloudops_nodejs_process_memory_bytes{pid=\"")
                        .append(pid)
                        .append("\",port=\"")
                        .append(port)
                        .append("\"} ")
                        .append(memoryBytes)
                        .append("\n");
            }

        } catch (Exception e) {
            System.err.println("Node.js detection error: " + e.getMessage());
        }

        return metrics.toString();
    }

    // ==========================================
    // REGISTERED NODE.JS STATUS SYNCHRONIZATION
    // ==========================================

    private static void syncRegisteredNodeStatuses() {

        HttpURLConnection backendConnection = null;

        try {
            URL backendUrl = new URL(
                    "http://localhost:8081/api/servers/agent/node-ports"
            );

            backendConnection =
                    (HttpURLConnection) backendUrl.openConnection();

            backendConnection.setRequestMethod("GET");
            backendConnection.setConnectTimeout(3000);
            backendConnection.setReadTimeout(3000);

            int backendResponse = backendConnection.getResponseCode();

            if (backendResponse != 200) {
                System.out.println(
                        "Unable to fetch registered Node.js ports. HTTP: "
                                + backendResponse
                );
                return;
            }

            String response = new String(
                    backendConnection.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            Matcher matcher = Pattern.compile("\\d+").matcher(response);

            while (matcher.find()) {

                String port = matcher.group();

                boolean online = false;

                HttpURLConnection appConnection = null;

                try {
                    URL appUrl = new URL(
                            "http://localhost:" + port + "/"
                    );

                    appConnection =
                            (HttpURLConnection) appUrl.openConnection();

                    appConnection.setRequestMethod("GET");
                    appConnection.setConnectTimeout(2000);
                    appConnection.setReadTimeout(2000);

                    int appResponse = appConnection.getResponseCode();

                    // Any valid HTTP response means the port is reachable.
                    online = appResponse >= 100 && appResponse <= 599;

                } catch (Exception e) {
                    online = false;

                } finally {
                    if (appConnection != null) {
                        appConnection.disconnect();
                    }
                }

                sendNodeHeartbeat(port, online);

                System.out.println(
                        "Node.js port " + port + " status: "
                                + (online ? "ONLINE" : "OFFLINE")
                );
            }

        } catch (Exception e) {
            System.out.println(
                    "Node.js status synchronization failed: "
                            + e.getMessage()
            );

        } finally {
            if (backendConnection != null) {
                backendConnection.disconnect();
            }
        }
    }

    // ==========================================
    // TOMCAT HEARTBEAT
    // ==========================================

    private static void sendTomcatHeartbeat(boolean running) {

        try {
            String status = running ? "ONLINE" : "OFFLINE";

            String json =
                    "{"
                            + "\"name\":\"Local Tomcat\","
                            + "\"host\":\"localhost\","
                            + "\"serverType\":\"TOMCAT\","
                            + "\"exporterPort\":9404,"
                            + "\"status\":\"" + status + "\""
                            + "}";

            HttpURLConnection connection =
                    (HttpURLConnection) new URL(
                            "http://localhost:8081/api/servers/agent/heartbeat"
                    ).openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            connection.setRequestProperty(
                    "Content-Type", "application/json"
            );

            byte[] data = json.getBytes(StandardCharsets.UTF_8);

            try (var output = connection.getOutputStream()) {
                output.write(data);
            }

            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                System.out.println(
                        "Tomcat heartbeat sent: " + status
                );
            } else {
                System.err.println(
                        "Tomcat heartbeat failed: HTTP " + responseCode
                );
            }

            connection.disconnect();

        } catch (Exception e) {
            System.err.println(
                    "Tomcat heartbeat error: " + e.getMessage()
            );
        }
    }

    // ==========================================
    // NODE.JS HEARTBEAT
    // ==========================================

    private static void sendNodeHeartbeat(String port, boolean httpUp) {

        try {
            String status = httpUp ? "ONLINE" : "OFFLINE";

            String json =
                    "{"
                            + "\"name\":\"Node.js App on Port " + port + "\","
                            + "\"host\":\"localhost\","
                            + "\"serverType\":\"NODEJS\","
                            + "\"servicePort\":" + port + ","
                            + "\"status\":\"" + status + "\""
                            + "}";

            HttpURLConnection connection =
                    (HttpURLConnection) new URL(
                            "http://localhost:8081/api/servers/agent/heartbeat"
                    ).openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            connection.setRequestProperty(
                    "Content-Type", "application/json"
            );

            byte[] data = json.getBytes(StandardCharsets.UTF_8);

            try (var output = connection.getOutputStream()) {
                output.write(data);
            }

            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                System.out.println(
                        "Node.js heartbeat sent for port "
                                + port + ": " + status
                );
            } else {
                System.err.println(
                        "Node.js heartbeat failed: HTTP " + responseCode
                );
            }

            connection.disconnect();

        } catch (Exception e) {
            System.err.println(
                    "Node.js heartbeat error: " + e.getMessage()
            );
        }
    }
}
