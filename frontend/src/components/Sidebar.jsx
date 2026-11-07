
function Sidebar({ activePage, setActivePage }) {
  const pages = ["Dashboard", "Servers", "Monitoring", "Alerts"];

  return (
    <aside className="sidebar">
      <h2>CloudOps</h2>

      <nav>
        {pages.map((page) => (
          <button
            key={page}
            className={`nav-item ${
              activePage === page ? "nav-active" : ""
            }`}
            onClick={() => setActivePage(page)}
          >
            {page}
          </button>
        ))}
      </nav>
    </aside>
  );
}

export default Sidebar;
