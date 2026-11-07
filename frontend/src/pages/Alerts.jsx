
import { useEffect, useState } from "react";
import { apiUrl } from "../services/apiConfig";

function Alerts() {
  const [alerts, setAlerts] = useState([]);
  const [filter, setFilter] = useState("All");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function fetchAlerts() {
      try {
        const response = await fetch(
          apiUrl("/api/monitoring/alerts")
        );

        if (!response.ok) {
          throw new Error("Unable to fetch alert history");
        }

        const data = await response.json();

        if (active) {
          setAlerts(data);
          setError("");
          setLoading(false);
        }
      } catch (err) {
        if (active) {
          setError(err.message);
          setLoading(false);
        }
      }
    }

    fetchAlerts();

    const interval = setInterval(fetchAlerts, 5000);

    return () => {
      active = false;
      clearInterval(interval);
    };
  }, []);

  const visibleAlerts = alerts.filter((alert) => {
    if (filter === "All") return true;
    if (filter === "Active") return alert.active;
    if (filter === "Resolved") return !alert.active;

    return alert.severity === filter;
  });

  return (
    <section className="server-section">
      <div className="alerts-header">
        <h2>Alert History</h2>

        <select
          value={filter}
          onChange={(event) => setFilter(event.target.value)}
        >
          <option value="All">All Alerts</option>
          <option value="Active">Active</option>
          <option value="Resolved">Resolved</option>
          <option value="Warning">Warning</option>
          <option value="Critical">Critical</option>
        </select>
      </div>

      <div className="alert-summary">
        <div>
          <span>Active Alerts</span>
          <strong>{alerts.filter((a) => a.active).length}</strong>
        </div>

        <div>
          <span>Resolved Alerts</span>
          <strong>{alerts.filter((a) => !a.active).length}</strong>
        </div>

        <div>
          <span>Total History</span>
          <strong>{alerts.length}</strong>
        </div>
      </div>

      {error && <p className="error-message">{error}</p>}

      {loading ? (
        <p>Loading alert history...</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Server</th>
                <th>Alert</th>
                <th>Severity</th>
                <th>Status</th>
                <th>Time</th>
              </tr>
            </thead>

            <tbody>
              {visibleAlerts.length > 0 ? (
                visibleAlerts.map((alert) => (
                  <tr key={alert.id}>
                    <td>{alert.serverName}</td>
                    <td>{alert.message}</td>

                    <td>
                      <span
                        className={`severity ${alert.severity.toLowerCase()}`}
                      >
                        {alert.severity}
                      </span>
                    </td>

                    <td>
                      <span
                        className={`severity ${
                          alert.active ? "critical" : "resolved"
                        }`}
                      >
                        {alert.active ? "Active" : "Resolved"}
                      </span>
                    </td>

                    <td>
                      {alert.createdAt
                        ? new Date(alert.createdAt).toLocaleString()
                        : "-"}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5">No alerts found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default Alerts;
