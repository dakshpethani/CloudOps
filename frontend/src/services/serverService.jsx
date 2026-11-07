
import { apiUrl } from "./apiConfig";

const API_BASE_URL = apiUrl("/api/servers");

// Fetch all registered servers from MySQL
export async function getAllServers() {
    const response = await fetch(API_BASE_URL);

    if (!response.ok) {
        throw new Error("Failed to fetch servers");
    }

    return response.json();
}

// Register a new server
export async function addServer(server) {
    const response = await fetch(API_BASE_URL, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(server)
    });

    if (!response.ok) {
        throw new Error("Failed to add server");
    }

    return response.json();
}

// Delete a registered server
export async function deleteServer(id) {
    const response = await fetch(apiUrl(`/api/servers/${id}`), {
        method: "DELETE"
    });

    if (!response.ok) {
        throw new Error("Failed to delete server");
    }

    return response.text();
}

// Existing Windows monitoring endpoint
export async function getMonitoringMetrics() {
    const response = await fetch(
        apiUrl("/api/monitoring/metrics")
    );

    if (!response.ok) {
        throw new Error("Failed to fetch Windows metrics");
    }

    return response.json();
}

// Fetch metrics for an individual registered server
export async function getServerMetrics(id) {
    const response = await fetch(
        apiUrl(`/api/monitoring/servers/${id}/metrics`)
    );

    if (!response.ok) {
        throw new Error(`Failed to fetch metrics for server ${id}`);
    }

    return response.json();
}

// Update server alert email
export async function updateServerAlertEmail(id, alertEmail) {
    const response = await fetch(
        apiUrl(`/api/servers/${id}/alert-email`),
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ alertEmail }),
        }
    );

    if (!response.ok) {
        throw new Error("Failed to update server alert email");
    }

    return response.json();
}

// Fetch historical CPU usage
export async function getServerCpuHistory(id) {
    const response = await fetch(
        apiUrl(`/api/monitoring/servers/${id}/history/cpu`)
    );

    if (!response.ok) {
        throw new Error("Failed to fetch CPU history");
    }

    return response.json();
}

// Fetch historical memory usage
export async function getServerMemoryHistory(id) {
    const response = await fetch(
        apiUrl(`/api/monitoring/servers/${id}/history/memory`)
    );

    if (!response.ok) {
        throw new Error("Failed to fetch memory history");
    }

    return response.json();
}

// Fetch historical disk usage
export async function getServerDiskHistory(id) {
    const response = await fetch(
        apiUrl(`/api/monitoring/servers/${id}/history/disk`)
    );

    if (!response.ok) {
        throw new Error("Failed to fetch disk history");
    }

    return response.json();
}
