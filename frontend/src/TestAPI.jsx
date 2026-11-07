import { useEffect, useState } from "react";
import { getAllServers } from "./services/serverService";

function TestAPI() {
  const [servers, setServers] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    getAllServers()
      .then(setServers)
      .catch((err) => setError(err.message));
  }, []);

  return (
    <div>
      <h1>CloudOps API Test</h1>

      {error && <p>{error}</p>}

      {servers.map((server) => (
        <div key={server.id}>
          <h3>{server.name}</h3>
          <p>Host: {server.host}</p>
          <p>Status: {server.status}</p>
        </div>
      ))}
    </div>
  );
}
export default TestAPI;