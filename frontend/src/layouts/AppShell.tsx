import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { BarChart3, BookOpen, ChevronDown, FileText, FolderKanban, Home, LogOut, Menu, UserRound, X } from 'lucide-react'
import { useAuth } from '../app/useAuth'
import KernelLogo from '../components/KernelLogo'
import '../styles/app-shell.css'

const mainLinks = [
  { label: 'Overview', to: '/app/dashboard', icon: Home },
  { label: 'My profile', to: '/app/profile', icon: UserRound },
  { label: 'Domains', to: '/app/domains', icon: BookOpen },
  { label: 'Sessions', to: '/app/sessions', icon: FileText },
  { label: 'My attendance', to: '/app/attendance', icon: FileText },
  { label: 'Resources', to: '/app/resources', icon: BookOpen },
  { label: 'Projects', to: '/app/projects', icon: FolderKanban },
  { label: 'Tasks', to: '/app/tasks', icon: FileText },
  { label: 'Leaderboard', to: '/app/leaderboard', icon: BarChart3 },
]

export default function AppShell() {
  const { user, signOut } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const current = mainLinks.find((item) => location.pathname === item.to || (item.to !== '/app/dashboard' && location.pathname.startsWith(`${item.to}/`)))

  const logout = async () => {
    await signOut()
    navigate('/login', { replace: true })
  }

  return <div className="app-shell">
    <aside className={`app-sidebar${menuOpen ? ' open' : ''}`}>
      <Link to="/" className="app-brand" aria-label="MIT Tech Kernel public site"><KernelLogo alt="MIT Tech Kernel symbol" /><span>MIT TECH KERNEL</span></Link>
      <p className="app-nav-caption">MEMBERS CORNER</p>
      <nav className="app-navigation" aria-label="Member navigation">
        {mainLinks.map(({ label, to, icon: Icon }) => <NavLink key={to} to={to} onClick={() => setMenuOpen(false)} className={({ isActive }) => `app-nav-link${isActive ? ' active' : ''}`}><Icon size={17} strokeWidth={1.8} /><span>{label}</span></NavLink>)}
        <Link to="/app/integrations" onClick={() => setMenuOpen(false)} className={`app-nav-link${location.pathname.startsWith('/app/integrations') ? ' active' : ''}`}><span className="app-github-icon">GH</span><span>GitHub</span></Link>
        {user?.roles.some((role) => ['SUPER_ADMIN', 'CORE_MEMBER', 'DOMAIN_LEAD'].includes(role)) && <NavLink to="/app/manage/tasks" onClick={() => setMenuOpen(false)} className={({ isActive }) => `app-nav-link app-nav-management${isActive ? ' active' : ''}`}><ChevronDown size={17} /><span>Task management</span></NavLink>}
        {user?.roles.some((role) => ['SUPER_ADMIN', 'CORE_MEMBER'].includes(role)) && <NavLink to="/app/manage/accounts" onClick={() => setMenuOpen(false)} className={({ isActive }) => `app-nav-link app-nav-management${isActive ? ' active' : ''}`}><UserRound size={17} /><span>Account management</span></NavLink>}
      </nav>
      <div className="app-sidebar-bottom">
        <Link to="/app/profile" className="app-user-card"><span className="app-avatar">{user?.name.trim().charAt(0).toUpperCase() || 'K'}</span><span className="app-user-copy"><strong>{user?.name ?? 'Member'}</strong><small>{user?.roles[0]?.replaceAll('_', ' ') ?? 'MIT TECH KERNEL'}</small></span><ChevronDown size={15} /></Link>
        <button type="button" className="app-logout" onClick={() => void logout()}><LogOut size={16} /> Sign out</button>
      </div>
    </aside>
    {menuOpen && <button className="app-sidebar-backdrop" aria-label="Close navigation" onClick={() => setMenuOpen(false)} />}
    <main className="app-main">
      <header className="app-topbar"><button className="app-menu-toggle" type="button" onClick={() => setMenuOpen(!menuOpen)} aria-label={menuOpen ? 'Close menu' : 'Open menu'} aria-expanded={menuOpen}>{menuOpen ? <X size={20} /> : <Menu size={20} />}</button><div className="app-breadcrumb"><Link to="/app/dashboard">Members Corner</Link><span>/</span><strong>{location.pathname === '/app/manage/tasks' ? 'Task management' : location.pathname === '/app/manage/accounts' ? 'Account management' : current?.label ?? 'GitHub'}</strong></div><div className="app-topbar-actions"><span className="app-role-tag">{user?.roles[0]?.replaceAll('_', ' ') ?? 'MEMBER'}</span><Link to="/app/profile" className="app-topbar-avatar" aria-label="Open profile">{user?.name.trim().charAt(0).toUpperCase() || 'K'}</Link></div></header>
      <div className="app-page-content"><Outlet /></div>
    </main>
  </div>
}
