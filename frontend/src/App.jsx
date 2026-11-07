
import { useState, useEffect } from "react";

import Sidebar from "./components/Sidebar";
import Dashboard from "./pages/Dashboard";
import Servers from "./pages/Servers";
import Monitoring from "./pages/Monitoring";
import Alerts from "./pages/Alerts";
import { getAllServers } from "./services/serverService";

function App() {
  const [activePage, setActivePage] = useState("Dashboard");
  const [servers, setServers] = useState([]);

  useEffect(() => {
    const fetchServers = async () => {
    try {
        const data = await getAllServers();

        setServers(previousServers => {
            const oldData = JSON.stringify(previousServers);
            const newData = JSON.stringify(data);

            // Update React only when server data actually changes
            return oldData === newData ? previousServers : data;
        });

    } catch (error) {
        console.error("Unable to load servers:", error);
    }
};

    fetchServers();
    const interval = setInterval(fetchServers, 5000);

    return () => clearInterval(interval);
  }, []);

  return (
    <div className="app">
      <Sidebar
        activePage={activePage}
        setActivePage={setActivePage}
      />

      <main className="main">
        {activePage === "Dashboard" && (
          <Dashboard servers={servers} />
        )}

        {activePage === "Servers" && <Servers />}

        {activePage === "Monitoring" && <Monitoring />}

        {activePage === "Alerts" && <Alerts />}
      </main>
    </div>
  );
}

export default App;
