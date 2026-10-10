import { BrowserRouter, Routes, Route } from "react-router-dom"
import PublicLayout from "./layouts/PublicLayout"
import Home from "./pages/Home"
import About from "./pages/About"
import Domains from "./pages/Domains"
import Events from "./pages/Events"
import PublicProjects from "./pages/PublicProjects"
import Members from "./pages/Members"
import Login from "./pages/Login"
import PasswordSetupPage from "./pages/PasswordSetupPage"
import AppShell from "./layouts/AppShell"
import ProfilePage from "./pages/app/ProfilePage"
import DashboardPage from "./pages/app/DashboardPage"
import DomainsPage from "./pages/app/DomainsPage"
import SessionsPage from "./pages/app/SessionsPage"
import ResourcesPage from "./pages/app/ResourcesPage"
import ProjectsPage from "./pages/app/ProjectsPage"
import ProjectDetailPage from "./pages/app/ProjectDetailPage"
import TasksPage from "./pages/app/TasksPage"
import TaskDetailPage from "./pages/app/TaskDetailPage"
import AttendancePage from "./pages/app/AttendancePage"
import LeaderboardPage from "./pages/app/LeaderboardPage"
import IntegrationsPage from "./pages/app/IntegrationsPage"
import SessionDetailPage from "./pages/app/SessionDetailPage"
import RecommendationsUnavailablePage from "./pages/app/RecommendationsUnavailablePage"
import AccountsPage from "./pages/app/AccountsPage"
import RoleRoute from "./routes/RoleRoute"
import ProtectedRoute from "./routes/ProtectedRoute"
import { AuthProvider } from "./app/AuthContext"
import { Navigate } from "react-router-dom"
import { LegacyProjectRedirect, LegacySessionRedirect, LegacyTaskRedirect } from "./routes/LegacyDetailRedirect"

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/about" element={<About />} />
          <Route path="/domains" element={<Domains />} />
          <Route path="/events" element={<Events />} />
          <Route path="/projects" element={<PublicProjects />} />
          <Route path="/members" element={<Members />} />
          <Route path="/login" element={<Login />} />
          <Route path="/set-password" element={<PasswordSetupPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route path="/app" element={<AppShell />}>
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="domains" element={<DomainsPage />} />
            <Route path="sessions" element={<SessionsPage />} />
            <Route path="sessions/:id" element={<SessionDetailPage />} />
            <Route path="resources" element={<ResourcesPage />} />
            <Route path="projects" element={<ProjectsPage />} />
            <Route path="projects/:id" element={<ProjectDetailPage />} />
            <Route path="tasks" element={<TasksPage />} />
            <Route path="tasks/:id" element={<TaskDetailPage />} />
            <Route path="attendance" element={<AttendancePage />} />
            <Route path="leaderboard" element={<LeaderboardPage />} />
            <Route path="integrations" element={<IntegrationsPage />} />
            <Route path="recommendations" element={<RecommendationsUnavailablePage />} />
            <Route path="manage/tasks" element={<RoleRoute allowed={["SUPER_ADMIN", "CORE_MEMBER", "DOMAIN_LEAD"]}><TasksPage managementOnly /></RoleRoute>} />
            <Route path="manage/accounts" element={<RoleRoute allowed={["SUPER_ADMIN", "CORE_MEMBER"]}><AccountsPage /></RoleRoute>} />
          </Route>
        </Route>
        {/* Preserve previously used internal URLs while keeping public route names free. */}
        <Route path="/dashboard" element={<Navigate to="/app/dashboard" replace />} />
        <Route path="/sessions" element={<Navigate to="/app/sessions" replace />} />
        <Route path="/sessions/:id" element={<LegacySessionRedirect />} />
        <Route path="/tasks/:id" element={<LegacyTaskRedirect />} />
        <Route path="/projects/:id" element={<LegacyProjectRedirect />} />
        <Route path="/members/projects" element={<Navigate to="/app/projects" replace />} />
        <Route path="/members/projects/:id" element={<LegacyProjectRedirect />} />
        <Route path="/leaderboards" element={<Navigate to="/app/leaderboard" replace />} />
        <Route path="/recommendations" element={<Navigate to="/app/recommendations" replace />} />
      </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
