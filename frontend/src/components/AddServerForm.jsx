
import { useState } from "react";
import { addServer } from "../services/serverService";

function AddServerForm({ onAdd, onCancel }) {
  const [name, setName] = useState("");
  const [host, setHost] = useState("");
  const [serverType, setServerType] = useState("WINDOWS");
  const [exporterPort, setExporterPort] = useState("9182");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  function handleTypeChange(type) {
    setServerType(type);

    if (type === "WINDOWS") {
      setExporterPort("9182");
    } else if (type === "LINUX") {
      setExporterPort("9100");
    } else if (type === "TOMCAT") {
      setExporterPort("9404");
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();

    try {
      setLoading(true);
      setError("");

      const savedServer = await addServer({
        name: name.trim(),
        host: host.trim(),
        serverType,
        exporterPort: Number(exporterPort)
      });

      onAdd(savedServer);
      onCancel();

    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="modal-overlay">
      <form className="server-form" onSubmit={handleSubmit}>
        <h2>Add New Server</h2>
        <p>Enter your server monitoring details.</p>

        <label>Server Name</label>
        <input
          type="text"
          placeholder="Development Server"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />

        <label>IP Address or Hostname</label>
        <input
          type="text"
          placeholder="localhost"
          value={host}
          onChange={(e) => setHost(e.target.value)}
          required
        />

        <label>Server Type</label>
        <select
          value={serverType}
          onChange={(e) => handleTypeChange(e.target.value)}
          required
        >
          <option value="WINDOWS">Windows</option>
          <option value="LINUX">Linux</option>
          <option value="TOMCAT">Tomcat</option>
        </select>

        <label>Monitoring Exporter Port</label>
        <input
          type="number"
          min="1"
          max="65535"
          value={exporterPort}
          onChange={(e) => setExporterPort(e.target.value)}
          required
        />

        <p className="form-hint">
          Default ports: Windows 9182, Linux 9100,
          Tomcat JMX Exporter 9404.
        </p>

        {error && (
          <p className="error-message">{error}</p>
        )}

        <div className="form-actions">
          <button
            type="button"
            className="cancel-button"
            onClick={onCancel}
            disabled={loading}
          >
            Cancel
          </button>

          <button
            type="submit"
            className="add-server-button"
            disabled={loading}
          >
            {loading ? "Registering..." : "Register Server"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default AddServerForm;
