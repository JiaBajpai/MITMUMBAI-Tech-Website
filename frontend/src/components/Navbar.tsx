import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { Menu, X } from 'lucide-react'
import KernelLogo from './KernelLogo'

const links = [
  ['Home', '/'], ['About Us', '/about'], ['Domains', '/domains'],
  ['Events', '/events'], ['Projects', '/projects'], ['Members', '/members'],
] as const

export default function Navbar() {
  const [open, setOpen] = useState(false)
  const close = () => setOpen(false)

  return <header className="public-nav">
    <div className="public-nav-inner">
      <Link to="/" className="public-brand" onClick={close} aria-label="MIT Tech Kernel home">
        <KernelLogo className="public-brand-mark" alt="" />
        <span>MIT TECH KERNEL</span>
      </Link>
      <nav className="public-nav-links" aria-label="Main navigation">
        {links.map(([label, to]) => <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => `public-nav-link${isActive ? ' active' : ''}`}>{label}</NavLink>)}
      </nav>
      <NavLink to="/login" className={({ isActive }) => `public-login-link${isActive ? ' active' : ''}`}>Login <span aria-hidden="true">↗</span></NavLink>
      <button className="public-menu-toggle" type="button" aria-label={open ? 'Close menu' : 'Open menu'} aria-expanded={open} onClick={() => setOpen(!open)}>
        {open ? <X size={20} /> : <Menu size={20} />}
      </button>
    </div>
    {open && <nav className="public-mobile-nav" aria-label="Mobile navigation">
      {links.map(([label, to]) => <NavLink key={to} to={to} end={to === '/'} onClick={close} className={({ isActive }) => `public-nav-link${isActive ? ' active' : ''}`}>{label}</NavLink>)}
      <NavLink to="/login" onClick={close} className={({ isActive }) => isActive ? 'active' : ''}>Login ↗</NavLink>
    </nav>}
  </header>
}
