import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import Login from "./pages/Login"
import Dashboard from "./pages/Dashboard"
import AppShell from "./layouts/AppShell"
import Sessions from "./pages/Sessions"
import SessionDetail from "./pages/SessionDetail"
import TaskDetail from "./pages/TaskDetail"
import Projects from "./pages/Projects"
import ProjectDetail from "./pages/ProjectDetail"
import Leaderboards from "./pages/Leaderboards"
import Recommendations from "./pages/Recommendations"

function App() {
  return (
    <BrowserRouter>
      <Routes>

        <Route path="/" element={<Navigate to="/login" replace />} />

        <Route path="/login" element={<Login />} />

        <Route element={<AppShell />}>
        <Route path="/sessions/:id" element={<SessionDetail />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/sessions" element={<Sessions />} />
          <Route path="/tasks/:id" element={<TaskDetail />} />
          <Route path="/projects" element={<Projects />} />
          <Route path="/projects/:id" element={<ProjectDetail />} />
          <Route path="/leaderboards" element={<Leaderboards />} />
          <Route path="/recommendations" element={<Recommendations />} />
        </Route>

      </Routes>
    </BrowserRouter>
  )
}

export default App