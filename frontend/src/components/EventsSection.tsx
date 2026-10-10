import React, { useState, useEffect, useRef } from 'react'
import { ChevronLeft, ChevronRight, Calendar, User } from 'lucide-react'
import { getSessions } from '../api/sessions'

export interface EventItem {
  id: number | string
  topic: string
  domain: string
  type: string
  date: string
  time: string
  description: string
  instructor: string
}

interface RawSessionItem {
  id?: number | string
  topic?: string
  domainName?: string
  domain?: string
  type?: string
  date?: string
  time?: string
  description?: string
  instructor?: string
}

export const EventsSection: React.FC = () => {
  const [events, setEvents] = useState<EventItem[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [isLiveApi, setIsLiveApi] = useState<boolean>(false)
  const carouselRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let isMounted = true
    getSessions()
      .then((res: unknown) => {
        if (!isMounted) return
        const responseData = res as { data?: RawSessionItem[] } | RawSessionItem[]
        const sessionList: RawSessionItem[] = Array.isArray(responseData)
          ? responseData
          : responseData?.data || []

        const mapped: EventItem[] = sessionList
          .filter((session): session is RawSessionItem & { id: number | string } => session.id !== undefined)
          .map((session) => ({
            id: session.id,
            topic: session.topic || 'Untitled session',
            domain: session.domainName || session.domain || 'Unassigned domain',
            type: session.type || 'SESSION',
            date: session.date || 'Date to be announced',
            time: session.time ? `${session.time} IST` : 'Time to be announced',
            description: session.description || '',
            instructor: session.instructor || 'Instructor to be announced',
          }))
        setEvents(mapped)
        setIsLiveApi(true)
      })
      .catch(() => {
        if (isMounted) {
          setEvents([])
          setIsLiveApi(false)
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [])

  const scrollCarousel = (direction: 'left' | 'right') => {
    if (!carouselRef.current) return
    const scrollAmount = 360
    carouselRef.current.scrollBy({
      left: direction === 'left' ? -scrollAmount : scrollAmount,
      behavior: 'smooth',
    })
  }

  return (
    <section className="events-section" id="events">
      <div className="kernel-container">
        {/* Section Header with Carousel Controls */}
        <div className="events-header-bar">
          <div>
            <div className="eyebrow-label lime">
              <span className="eyebrow-pip" />
              <span>04 // SCHEDULE & GATHERINGS</span>
            </div>

            <h2 className="section-heading-dark" style={{ marginBottom: 8 }}>
              Curated Sessions & Workshops.
            </h2>

            <p style={{ margin: 0, fontSize: 15, color: 'var(--kernel-text-dark-muted)' }}>
              Hands-on technical explorations conducted by domain leads and guest engineers.
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <span
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: 10,
                color: isLiveApi ? 'var(--lime-accent)' : 'var(--kernel-text-dark-muted)',
              }}
            >
              SOURCE: {loading ? 'LOADING...' : isLiveApi ? 'LIVE API (/sessions)' : 'LIVE DATA UNAVAILABLE'}
            </span>

            <div className="events-carousel-controls">
              <button
                onClick={() => scrollCarousel('left')}
                className="carousel-arrow-btn"
                aria-label="Previous events"
              >
                <ChevronLeft size={18} />
              </button>
              <button
                onClick={() => scrollCarousel('right')}
                className="carousel-arrow-btn"
                aria-label="Next events"
              >
                <ChevronRight size={18} />
              </button>
            </div>
          </div>
        </div>

        {/* Horizontal Carousel Track */}
        <div
          className="events-carousel-track"
          ref={carouselRef}
          role="region"
          aria-label="Events Carousel"
          tabIndex={0}
        >
          {events.length === 0 && !loading && <p className="event-card-desc">No sessions are available to share right now.</p>}
          {events.map((evt) => (
            <article key={evt.id} className="event-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <span className="event-card-domain">{evt.domain}</span>
                <span
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    padding: '2px 6px',
                    borderRadius: 2,
                    background: 'rgba(255, 255, 255, 0.05)',
                    color: 'var(--kernel-text-dark-muted)',
                  }}
                >
                  {evt.type}
                </span>
              </div>

              <h3 className="event-card-title">{evt.topic}</h3>
              <p className="event-card-desc">{evt.description}</p>

              <div className="event-card-meta">
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Calendar size={12} color="var(--lime-accent)" />
                  <span>{evt.date}</span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <User size={12} />
                  <span>{evt.instructor}</span>
                </div>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
export default EventsSection
