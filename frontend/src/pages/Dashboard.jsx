import { apiUrl } from "../services/apiConfig";
import { useEffect, useState } from "react";

    import {
        getAllServers,
        getMonitoringMetrics,
        getServerMetrics
    } from "../services/serverService";



    function formatUptime(seconds) {
        if (seconds == null) return "--";

        const total = Math.floor(seconds);
        const days = Math.floor(total / 86400);
        const hours = Math.floor((total % 86400) / 3600);
        const minutes = Math.floor((total % 3600) / 60);

        return `${days}d ${hours}h ${minutes}m`;
    }

    
function GaugeCard({ title, value }) {
    const numericValue = Number(value);
    const hasValue = value != null && Number.isFinite(numericValue);

    const percentage = hasValue
        ? Math.min(100, Math.max(0, numericValue))
        : 0;

    const arcLength = 194.78;
    const dashOffset = arcLength - (arcLength * percentage) / 100;

    const status =
        !hasValue ? "Unavailable" :
        percentage >= 90 ? "Critical" :
        percentage >= 75 ? "High Usage" :
        "Normal";

    const gaugeColor =
        !hasValue ? "#64748b" :
        percentage >= 90 ? "#ff5c5c" :
        percentage >= 75 ? "#f5b942" :
        "#b5e853";

    return (
        <article className="stat-card gauge-stat-card">
            <div className="gauge-card-header">
                <span className="gauge-title">{title}</span>
                <span
                    className="gauge-status"
                    style={{
                        color: gaugeColor,
                        backgroundColor: `${gaugeColor}18`,
                        borderColor: `${gaugeColor}45`
                    }}
                >
                    {status}
                </span>
            </div>

            <div className="gauge-wrapper">
                <svg
                    className="gauge-svg"
                    viewBox="0 0 160 110"
                    role="img"
                    aria-label={`${title}: ${hasValue ? numericValue.toFixed(2) + "%" : "unavailable"}`}
                >
                    <path
                        className="gauge-track"
                        d="M 18 88 A 62 62 0 0 1 142 88"
                    />

                    <path
                        className="gauge-progress"
                        d="M 18 88 A 62 62 0 0 1 142 88"
                        strokeDasharray={arcLength}
                        strokeDashoffset={dashOffset}
                        style={{
                            stroke: gaugeColor,
                            filter: `drop-shadow(0 0 5px ${gaugeColor}75)`
                        }}
                    />
                </svg>

                <div className="gauge-reading">
                    {hasValue ? `${numericValue.toFixed(1)}%` : "--"}
                </div>
            </div>

            <div className="gauge-footer">
                <span>0%</span>
                <span>Resource utilization</span>
                <span>100%</span>
            </div>
        </article>
    );
}

    function Dashboard({ servers = [] }) {

        const [metrics, setMetrics] = useState(null);
        const [error, setError] = useState("");
    const [activeAlerts, setActiveAlerts] = useState([]);
        const [serverStatuses, setServerStatuses] = useState({});
        const [registeredServers, setRegisteredServers] = useState(servers);

        // Refresh registered servers from MySQL every 5 seconds
        useEffect(() => {
            let isMounted = true;

            const refreshServers = async () => {
                try {
                    const latestServers = await getAllServers();

                    if (isMounted) {
                        setRegisteredServers(latestServers);
                    }
                } catch (err) {
                    console.error("Server refresh failed:", err);
                }
            };

            refreshServers();

            const interval = setInterval(refreshServers, 5000);

            return () => {
                isMounted = false;
                clearInterval(interval);
            };
        }, []);

        // Fetch live monitoring data and status
        useEffect(() => {
            let isMounted = true;

            const fetchDashboardData = async () => {
                try {

                    // Fetch primary Windows server metrics for summary cards
                    try {
                        const metricsData = await getMonitoringMetrics();

                        if (isMounted) {
                            setMetrics(metricsData);
                        }
                    } catch (err) {
                        console.error("Windows metrics unavailable:", err);
                    }

                    // Fetch actual live status for every registered server
                    const statusResults = await Promise.all(
                        registeredServers.map(async (server) => {
                            try {
                                const data = await getServerMetrics(server.id);

                                const status = data.status
                                    ? data.status.toUpperCase()
                                    : "ONLINE";

                                return [server.id, status];

                            } catch (err) {
                                console.error(
                                    `Live status failed for ${server.name}:`,
                                    err
                                );

                                return [server.id, "OFFLINE"];
                            }
                        })
                    );

                    if (isMounted) {
                        setServerStatuses(
                            Object.fromEntries(statusResults)
                        );
                    }

                    // Fetch active alerts
                    const response = await fetch(apiUrl("/api/monitoring/alerts"));
                    if (!response.ok) {
                        throw new Error("Unable to fetch alerts");
                    }

                    const alertData = await response.json();

                    const currentActiveAlerts = alertData
  .filter(alert => alert.active === true && alert.serverId != null)
  .sort((a,b) => new Date(b.createdAt) - new Date(a.createdAt));
    if (isMounted) {
        setActiveAlerts(currentActiveAlerts);
        setError("");
    }

                } catch (err) {
                    console.error("Dashboard data error:", err);

                    if (isMounted) {
                        setError("Unable to load live dashboard data");
                    }
                }
            };

            fetchDashboardData();

            const interval = setInterval(fetchDashboardData, 5000);

            return () => {
                isMounted = false;
                clearInterval(interval);
            };

        }, [registeredServers]);

        // Combine registered servers with their live statuses
        const dashboardServers = registeredServers.map(server => ({
            ...server,
            status: serverStatuses[server.id] || "UNKNOWN"
        }));

        // Dashboard counts
        const totalServers = dashboardServers.length;

        const onlineServers = dashboardServers.filter(
            server => server.status === "ONLINE"
        );

        const offlineServers = dashboardServers.filter(
            server => server.status === "OFFLINE"
        );

        return (
            <div className="dashboard-container">

                {/* Dashboard statistics */}
                <section className="stats">

                    <article className="stat-card">
                        <span>Total Servers</span>
                        <strong>{totalServers}</strong>
                    </article>

                    <article className="stat-card">
                        <span>Online</span>
                        <strong className="green">
                            {onlineServers.length}
                        </strong>
                    </article>

                    <article className="stat-card">
                        <span>Offline</span>
                        <strong className="red">
                            {offlineServers.length}
                        </strong>
                    </article>

                    <article className="stat-card">
    <span>Active Alerts</span>
    <strong>{activeAlerts.length}</strong>
</article>

                    
<GaugeCard
    title="CPU Usage"
    value={metrics?.cpuUsagePercent}
/>

<GaugeCard
    title="RAM Usage"
    value={metrics?.memoryUsagePercent}
/>

<GaugeCard
    title="Disk Usage"
    value={metrics?.diskUsagePercent}
/>


                    <article className="stat-card">
                        <span>Server Uptime</span>
                        <strong>
                            {formatUptime(metrics?.uptimeSeconds)}
                        </strong>
                    </article>

                </section>

                {error && (
                    <p className="red">{error}</p>
                )}

                {/* Running servers only - no management controls */}
                <section className="dashboard-running-section">

                    <div className="dashboard-section-header">
                        <div>
                            <h2>Running Servers</h2>
                            <p>Currently active servers monitored by CloudOps</p>
                        </div>

                        <span className="online-count">
                            {onlineServers.length} Online
                        </span>
                    </div>

                    {onlineServers.length === 0 ? (

                        <div className="empty-servers">
                            No servers are currently online.
                        </div>

                    ) : (

                        <div className="dashboard-running-list">

                            {onlineServers.map(server => (

                                <article
                                    className="dashboard-server-card"
                                    key={server.id}
                                >

                                    <div className="dashboard-server-info">

                                        <div className="server-icon">
                                            <span>▣</span>
                                        </div>

                                        <div>
                                            <h3>{server.name}</h3>

                                            <p>
                                                {server.serverType || "Unknown Type"}
                                                {server.servicePort
                                                    ? ` • Port ${server.servicePort}`
                                                    : ""}
                                            </p>
                                        </div>

                                    </div>

                                    <span className="dashboard-online-status">
                                        <span className="status-dot"></span>
                                        Online
                                    </span>

                                </article>

                            ))}

                        </div>

                    )}

                </section>

                {/* Monitoring charts */}
    {/* Active Alerts */}
    <section className="dashboard-alerts-section">

        <div className="dashboard-section-header">
            <div>
                <h2>Active Alerts</h2>
                <p>Currently triggered alerts across monitored servers</p>
            </div>

            <span className="online-count">
                {activeAlerts.length} Active
            </span>
        </div>

        {activeAlerts.length === 0 ? (

            <div className="empty-servers">
                <h3>All Systems Normal</h3>
                <p>No active alerts detected across your servers.</p>
            </div>

        ) : (

            <div className="dashboard-alert-list">

                {activeAlerts.map((alert, index) => {

                    const severity = (
                        alert.severity || "Warning"
                    ).toLowerCase();

                    return (
                        <article
                            className="dashboard-alert-card"
                            key={alert.id ?? `${alert.serverId}-${index}`}
                        >

                            <div className="dashboard-alert-info">

                                <div className={`alert-severity-dot ${severity}`} />

                                <div>
                                   <h3>
    {alert.serverId != null
        ? alert.serverName || `Server #${alert.serverId}`
        : "Historical Windows Server"}
</h3>

                                    <p>
                                        {alert.message || "Alert condition detected"}
                                    </p>

                                    <small>
                                        {alert.createdAt
                                            ? new Date(alert.createdAt).toLocaleString()
                                            : "Time unavailable"}
                                    </small>
                                </div>

                            </div>

                            <span className={`alert-severity-badge ${severity}`}>
                                {alert.severity || "Warning"}
                            </span>

                        </article>
                    );
                })}

            </div>

        )}

    </section>
            </div>
        );
    }

    export default Dashboard;
