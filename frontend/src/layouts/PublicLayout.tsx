import { Outlet } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Footer from '../components/Footer'
import '../styles/public-site.css'

export default function PublicLayout() {
  return <div className="public-site"><Navbar /><main className="public-main"><Outlet /></main><Footer /></div>
}
