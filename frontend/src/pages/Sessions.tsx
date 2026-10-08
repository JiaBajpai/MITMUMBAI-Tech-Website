import { Link } from "react-router-dom"

const sessions = [
  {
    id: 1,
    title: "React Fundamentals",
    domain: "FRONTEND",
    date: "10 OCT 2026",
    time: "17:00",
    type: "WORKSHOP",
    status: "UPCOMING",
  },
  {
    id: 2,
    title: "Spring Boot Fundamentals",
    domain: "BACKEND",
    date: "12 OCT 2026",
    time: "17:00",
    type: "SESSION",
    status: "UPCOMING",
  },
  {
    id: 3,
    title: "Git & GitHub",
    domain: "GENERAL",
    date: "14 OCT 2026",
    time: "16:00",
    type: "WORKSHOP",
    status: "UPCOMING",
  },
]

function Sessions() {
  return (
    <div className="sessions-page">

      <section className="sessions-header">
        <div>
          <p className="eyebrow">01 / LEARN</p>

          <h1>
            SESSIONS<span>.</span>
          </h1>

          <p className="sessions-description">
            Workshops, technical sessions and learning experiences
            conducted by MIT Tech Kernel.
          </p>
        </div>

        <div className="session-counter">
          <span>AVAILABLE</span>
          <strong>003</strong>
        </div>
      </section>


      <div className="sessions-toolbar">
        <span>ALL SESSIONS</span>

        <div>
          <button>ALL</button>
          <button>FRONTEND</button>
          <button>BACKEND</button>
          <button>AI / ML</button>
        </div>
      </div>


      <section className="session-list">

        {sessions.map((session, index) => (

          <article className="session-card" key={session.id}>

            <div className="session-index">
              0{index + 1}
            </div>

            <div className="session-main">

              <div className="session-meta">
                <span>{session.type}</span>
                <span>{session.domain}</span>
              </div>

              <h2>{session.title}</h2>

              <div className="session-info">
                <span>{session.date}</span>
                <span>{session.time}</span>
              </div>

            </div>

            <div className="session-action">

              <span className="session-status">
                {session.status}
              </span>

              <Link to={`/sessions/${session.id}`}>
                VIEW SESSION →
              </Link>

            </div>

          </article>

        ))}

      </section>

    </div>
  )
}

export default Sessions