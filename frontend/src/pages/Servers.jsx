import { useState, useEffect, useCallback, useRef } from "react";

import {
    getAllServers,
    getMonitoringMetrics,
    getServerMetrics,
    updateServerAlertEmail
} from "../services/serverService";

import AddServerForm from "../components/AddServerForm";

function formatUptime(seconds) {
    if (seconds == null) return "--";

    const totalSeconds = Math.floor(seconds);
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);

    return `${days}d ${hours}h ${minutes}m`;
}
function normalizeServerStatus(status) {
    const value = status?.toUpperCase();

    if (value === "ONLINE") return "Online";
    if (value === "OFFLINE") return "Offline";

    return "UNKNOWN";
}

function Servers() {
    const [servers, setServers] = useState([]);
    const [metrics, setMetrics] = useState({});
    const [search, setSearch] = useState("");
    const [filter, setFilter] = useState("All");

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [showAddForm, setShowAddForm] = useState(false);

    // Track servers already seen during this session
const knownServerIds = useRef(null);

// Queue newly registered servers that need an alert email
const [emailQueue, setEmailQueue] = useState([]);
const [emailInput, setEmailInput] = useState("");
const [savingEmail, setSavingEmail] = useState(false);

    // Fetch registered servers
    const loadServers = useCallback(async () => {
        
        try {
            setLoading(true);
            setError("");

            const data = await getAllServers();

// First load: remember existing servers without prompting
if (knownServerIds.current === null) {
    knownServerIds.current = new Set(
        data.map(server => server.id)
    );
} else {
    // Find servers registered after the first load
    const newServers = data.filter(
        server => !knownServerIds.current.has(server.id)
    );

    // Mark them as seen to prevent duplicate prompts
    newServers.forEach(server => {
        knownServerIds.current.add(server.id);
    });

    // Ask for email only when it is missing
    const serversNeedingEmail = newServers.filter(
        server => !server.alertEmail
    );

    if (serversNeedingEmail.length > 0) {
        setEmailQueue(previous => [
            ...previous,
            ...serversNeedingEmail
        ]);
    }
}

setServers(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    // Fetch metrics according to registered server type
    const loadAllMetrics = useCallback(async () => {
        try {
            const registeredServers = await getAllServers();

           

            const results = await Promise.all(
                registeredServers.map(async (server) => {
                    const type = server.serverType?.toUpperCase();

                    try {
                        if (type === "TOMCAT") {
                            const data = await getServerMetrics(server.id);

                            return [
                                server.id,
                                {
    ...data,
    status: normalizeServerStatus(server.status)
}
                            ];
                        }
                       if (type === "NODEJS") {
    const data = await getServerMetrics(server.id);

    return [
        server.id,
        {
            ...data,
            status: normalizeServerStatus(data.status)
        }
    ];
}
if (type === "WINDOWS") {
    const data = await getServerMetrics(server.id);

    return [
        server.id,
        {
            ...data,
            status: normalizeServerStatus(data.status)
        }
    ];
}


                        return [
    server.id,
    {
        status: server.status?.toUpperCase() === "ONLINE"
            ? "Online"
            : server.status?.toUpperCase() === "OFFLINE"
                ? "Offline"
                : "UNKNOWN"
    }
];
                    } catch (err) {
                        console.error(
                            `Metrics unavailable for server ${server.id}:`,
                            err
                        );

                        return [
                            server.id,
                            {
                                status: "Offline"
                            }
                        ];
                    }
                })
            );

            setMetrics(Object.fromEntries(results));
        } catch (err) {
            console.error("Failed to load server metrics:", err);
        }
    }, []);

    useEffect(() => {
        loadServers();
loadAllMetrics();

const interval = setInterval(() => {
    loadServers();
    loadAllMetrics();
}, 5000);

        return () => clearInterval(interval);
    }, [loadServers, loadAllMetrics]);

    // Combine database records with their live metrics
    const allServers = servers.map((server) => {
        const live = metrics[server.id] || {};

        return {
    ...server,
    status: live.status || server.status || "UNKNOWN",
    cpu: live.cpuUsagePercent,

    memory: server.serverType?.toUpperCase() === "NODEJS"
        ? live.memoryUsageMB
        : live.memoryUsagePercent,

    memoryUnit: server.serverType?.toUpperCase() === "NODEJS"
        ? "MB"
        : "%",

    disk: live.diskUsagePercent,
    uptime: live.uptimeSeconds,
    heapUsedMB: live.heapUsedMB,
    threadCount: live.threadCount
};
    });

    // Search and filter
    const filteredServers = allServers.filter((server) => {
        const matchesSearch =
            (server.name || "").toLowerCase().includes(search.toLowerCase()) ||
            (server.host || "").toLowerCase().includes(search.toLowerCase());

        const matchesStatus =
            filter === "All" ||
            server.status?.toUpperCase() === filter.toUpperCase();

        return matchesSearch && matchesStatus;
    });

    const refreshData = () => {
        loadServers();
        loadAllMetrics();
    };
    const handleSaveAlertEmail = async () => {
    const email = emailInput.trim();

    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        alert("Please enter a valid email address.");
        return;
    }

    if (!emailQueue.length) return;

    try {
        setSavingEmail(true);

        const server = emailQueue[0];

        const updatedServer = await updateServerAlertEmail(
            server.id,
            email
        );

        setServers(previous =>
            previous.map(item =>
                item.id === updatedServer.id ? updatedServer : item
            )
        );

        setEmailQueue(previous => previous.slice(1));
        setEmailInput("");

    } catch (err) {
        alert("Failed to save alert email: " + err.message);
    } finally {
        setSavingEmail(false);
    }
};

    return (
        <section className="server-section">
            <section>
                {emailQueue.length > 0 && (
    <div className="alert-email-overlay">
        <div className="alert-email-modal">
            <h2>Configure Alert Notifications</h2>

            <p>
                A new server has been registered.
                Enter the email address that should receive
                alerts for this server.
            </p>

            <div className="alert-server-details">
                <strong>{emailQueue[0].name}</strong>
                <span>{emailQueue[0].serverType}</span>
                <span>{emailQueue[0].host}</span>
            </div>

            <label htmlFor="server-alert-email">
                Alert Recipient Email
            </label>

            <input
                id="server-alert-email"
                type="email"
                placeholder="admin@example.com"
                value={emailInput}
                onChange={(e) => setEmailInput(e.target.value)}
                disabled={savingEmail}
            />

            <button
                onClick={handleSaveAlertEmail}
                disabled={savingEmail}
            >
                {savingEmail ? "Saving..." : "Save Email"}
            </button>
        </div>
    </div>
)}
            </section>

            <div className="server-toolbar">
                <h2>Registered Servers</h2>

                <div className="server-filters">

                    <input
                        placeholder="Search server or IP..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                    />

                    <select
                        value={filter}
                        onChange={(e) => setFilter(e.target.value)}
                    >
                        <option value="All">All Status</option>
                        <option value="Online">Online</option>
                        <option value="Offline">Offline</option>
                        <option value="UNKNOWN">Unknown</option>
                    </select>

                    <button onClick={refreshData}>
                        Refresh
                    </button>

                    <button
                        className="add-server-button"
                        onClick={() => setShowAddForm(true)}
                    >
                        + Add Server
                    </button>

                </div>
            </div>

            {showAddForm && (
                <AddServerForm
                    onAdd={(savedServer) => {
                        setServers((previousServers) => [
                            ...previousServers,
                            savedServer
                        ]);

                        setShowAddForm(false);
                        loadAllMetrics();
                    }}
                    onCancel={() => setShowAddForm(false)}
                />
            )}

            {loading ? (
                <p>Loading servers...</p>
            ) : error ? (
                <p className="error-message">{error}</p>
            ) : (
                <div className="table-wrap">

                    <table>
                        <thead>
                            <tr>
                                <th>Server</th>
                                <th>IP Address</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th>CPU</th>
                                <th>Memory</th>
                                <th>Disk</th>
                                <th>Uptime</th>
                                <th>Threads</th>
                                <th>Alert Email</th>
<th>Action</th>
                            </tr>
                        </thead>

                        <tbody>
                            {filteredServers.length > 0 ? (
                                filteredServers.map((server) => (
                                    <tr key={server.id}>

                                        <td>{server.name}</td>

                                        <td>{server.host}</td>

                                        <td>{server.serverType || "N/A"}</td>

                                        <td>
                                            <span
                                                className={`status ${(server.status || "UNKNOWN").toLowerCase()}`}
                                            >
                                                {server.status}
                                            </span>
                                        </td>

                                        <td>
                                            {server.cpu != null
                                                ? `${Number(server.cpu).toFixed(2)}%`
                                                : "N/A"}
                                        </td>

                                        <td>
    {server.memory != null
        ? `${Number(server.memory).toFixed(2)}${server.memoryUnit}`
        : "N/A"}
</td>

                                        <td>
                                            {server.disk != null
                                                ? `${Number(server.disk).toFixed(2)}%`
                                                : "N/A"}
                                        </td>

                                        <td>
                                            {server.uptime != null
                                                ? formatUptime(server.uptime)
                                                : "N/A"}
                                        </td>

                                        <td>
                                            {server.threadCount != null
                                                ? server.threadCount
                                                : "N/A"}
                                        </td>
                                        <td>
    {server.alertEmail || "Not configured"}
</td>

<td>
    <button
        className="edit-alert-email-button"
        onClick={() => {
            setEmailQueue(previous => [
                server,
                ...previous.filter(item => item.id !== server.id)
            ]);

            setEmailInput(server.alertEmail || "");
        }}
    >
        {server.alertEmail ? "Edit Email" : "Add Email"}
    </button>
</td>

                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan="11" className="empty-message">
                                        No servers found.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>

                </div>
            )}

        </section>
    );
}

export default Servers;
