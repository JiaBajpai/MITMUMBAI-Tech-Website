import { Link, Outlet } from "react-router-dom"

function AppShell() {
    return (
        <div className="app-shell">

            <aside className="sidebar">

                <div className="brand">
                    <img
                        src="/assets/won.png"
                        alt="MIT Tech Kernel"
                    />
                </div>

                <div className="sidebar-label">
                    MEMBERS CORNER
                </div>

                <nav className="navigation">
                    <Link to="/dashboard">Dashboard</Link>
                    <Link to="/sessions">Sessions</Link>
                    <Link to="/projects">Projects</Link>
                    <Link to="/recommendations">Recommendations</Link>
                    <Link to="/leaderboards">Leaderboard</Link>
                </nav>

                <div className="sidebar-bottom">
                    <div>
                        <div className="technical-label">MIT MUMBAI</div>
                        <div className="user-name">Jia Bajpai</div>
                    </div>

                    <button className="logout-button">
                        Logout
                    </button>
                </div>

            </aside>

            <main className="main-content">

                <div className="page-grid" />

                <header className="top-bar">
                    <span>MIT TECH KERNEL</span>
                    <span>MEMBERS / 001</span>
                </header>

                <div className="page-content">
                    <Outlet />
                </div>

            </main>

        </div>
    )
}

export default AppShell