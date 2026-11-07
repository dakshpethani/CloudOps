
import { useEffect, useState } from "react";

import {
  getAllServers,
  getServerMetrics,
} from "../services/serverService";

import MonitoringCharts from "../components/MonitoringCharts";

function Monitoring() {
  const [metrics, setMetrics] = useState(null);
  const [error, setError] = useState("");

  const [onlineServers, setOnlineServers] = useState([]);
  const [serversLoading, setServersLoading] = useState(true);
  const [serverError, setServerError] = useState("");

  const [selectedServerId, setSelectedServerId] = useState("");
  const [appliedServerId, setAppliedServerId] = useState(null);
  const [historyMessage, setHistoryMessage] = useState("");

  // Load live CloudOps Agent metrics
  useEffect(() => {
    const loadMetrics = async () => {
      try {
        const response = await fetch(
          "http://localhost:8081/api/monitoring/agent-metrics"
        );

        if (!response.ok) {
          throw new Error("Unable to fetch Agent metrics");
        }

        const data = await response.json();
        setMetrics(data);
        setError("");
      } catch (err) {
        setError(err.message);
      }
    };

    loadMetrics();

    const interval = setInterval(loadMetrics, 5000);

    return () => clearInterval(interval);
  }, []);

  // Load registered servers and check their live status
  useEffect(() => {
    const loadOnlineServers = async () => {
      try {
        setServersLoading(true);

        const registeredServers = await getAllServers();

        const results = await Promise.all(
          registeredServers.map(async (server) => {
            try {
              const liveMetrics = await getServerMetrics(server.id);

              if (liveMetrics.status === "ONLINE") {
                return {
                  ...server,
                  ...liveMetrics,
                };
              }

              return null;
            } catch {
              return null;
            }
          })
        );

        setOnlineServers(results.filter(Boolean));
        setServerError("");
      } catch (err) {
        setServerError(err.message);
      } finally {
        setServersLoading(false);
      }
    };

    loadOnlineServers();

    const interval = setInterval(loadOnlineServers, 30000);

    return () => clearInterval(interval);
  }, []);

  const handleShowHistory = () => {
    const server = onlineServers.find(
        (item) => String(item.id) === String(selectedServerId)
    );

    if (!server) return;

    setHistoryMessage("");
    setAppliedServerId(server.id);
};

  const formatUptime = (seconds) => {
    if (seconds == null) return "N/A";

    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    return `${hours}h ${minutes}m`;
  };

  const metricCards = metrics
    ? [
        { name: "CPU Usage", value: metrics.cpuUsagePercent },
        { name: "Memory Usage", value: metrics.memoryUsagePercent },
        { name: "Disk Usage", value: metrics.diskUsagePercent },
      ]
    : [];

  return (
    <>
      <section className="server-section">
        <h2>Resource Monitoring</h2>

        <p className="page-description">
          Live system metrics collected by CloudOps Agent.
        </p>

        {error && (
          <p className="error-message">
            Monitoring connection error: {error}
          </p>
        )}

        {!metrics && !error && <p>Loading live metrics...</p>}

        {metrics && (
          <>
            <div className="metrics">
              {metricCards.map((metric) => (
                <article className="metric-card" key={metric.name}>
                  <div className="metric-heading">
                    <span>{metric.name}</span>
                    <strong>{metric.value.toFixed(2)}%</strong>
                  </div>

                  <div className="progress-track">
                    <div
                      className="progress-fill"
                      style={{
                        width: `${Math.min(
                          100,
                          Math.max(0, metric.value)
                        )}%`,
                      }}
                    />
                  </div>
                </article>
              ))}
            </div>

            <br />

            <div className="metrics">
              <article className="metric-card">
                <div className="metric-heading">
                  <span>System Uptime</span>
                  <strong>{formatUptime(metrics.uptimeSeconds)}</strong>
                </div>
              </article>

              
            </div>

            <br />

            <p className="page-description">
              Agent metrics automatically refresh every 5 seconds.
            </p>
          </>
        )}
      </section>

      <section className="server-section">
        <h2>Server Historical Monitoring</h2>

        <p className="page-description">
          Select an online registered server to view its historical performance.
        </p>

        {serversLoading && <p>Checking registered servers...</p>}

        {serverError && (
          <p className="error-message">{serverError}</p>
        )}

        {!serversLoading && !serverError && (
          <>
            {onlineServers.length === 0 ? (
              <p>No online servers are currently available.</p>
            ) : (
              <div className="server-history-selector">
                <select
                  value={selectedServerId}
                  onChange={(e) => {
                    setSelectedServerId(e.target.value);
                    setAppliedServerId(null);
                    setHistoryMessage("");
                  }}
                >
                  <option value="">Select an online server</option>

                  {onlineServers.map((server) => (
                    <option key={server.id} value={server.id}>
                      {server.name} ({server.serverType})
                    </option>
                  ))}
                </select>

                <button
                  type="button"
                  onClick={handleShowHistory}
                  disabled={!selectedServerId}
                >
                  Show History
                </button>
              </div>
            )}
          </>
        )}

        {historyMessage && (
          <p className="page-description">{historyMessage}</p>
        )}
{appliedServerId && (
    <MonitoringCharts
        serverId={appliedServerId}
        serverType={
            onlineServers.find(
                (server) => String(server.id) === String(appliedServerId)
            )?.serverType
        }
    />
)}
      </section>
    </>
  );
}

export default Monitoring;
