import React from 'react'
import Navbar from '../components/Navbar'
import Hero from '../components/Hero'
import PhilosophySection from '../components/PhilosophySection'
import DomainsSection from '../components/DomainsSection'
import EventsSection from '../components/EventsSection'
import MembersCtaSection from '../components/MembersCtaSection'
import Footer from '../components/Footer'
import '../styles/landing.css'

export const LandingPage: React.FC = () => {
  return (
    <div className="kernel-landing">
      <Navbar />

      <main className="landing-main">
        <Hero />
        <PhilosophySection />
        <DomainsSection />
        <EventsSection />
        <MembersCtaSection />
      </main>

      <Footer />
    </div>
  )
}
export default LandingPage
