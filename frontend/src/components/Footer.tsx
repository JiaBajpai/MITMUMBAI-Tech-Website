import { Link } from 'react-router-dom'
import KernelLogo from './KernelLogo'

const links = [['About Us', '/about'], ['Domains', '/domains'], ['Events', '/events'], ['Projects', '/projects'], ['Members', '/members'], ['Login', '/login']] as const

export default function Footer() {
  return <footer className="public-footer">
    <div className="public-footer-inner">
      <Link to="/" className="public-footer-brand">
        <KernelLogo className="public-footer-mark" />
        <span>MIT TECH KERNEL</span>
      </Link>
      <nav aria-label="Footer navigation">
        {links.map(([label, to]) => <Link key={to} to={to}>{label}</Link>)}
      </nav>
      <p>Build with curiosity. Engineer with purpose.</p>
      <small>© {new Date().getFullYear()} MIT TECH KERNEL · MIT Mumbai</small>
    </div>
  </footer>
}
