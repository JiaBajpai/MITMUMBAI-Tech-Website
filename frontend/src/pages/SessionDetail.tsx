import { Link } from "react-router-dom"
import { useParams } from "react-router-dom"

function SessionDetail() {
  const { id } = useParams()

  return (
    <div className="session-detail">

      <section className="session-detail-header">

        <div className="session-detail-title">
          <p className="eyebrow">01 / SESSION</p>

          <h1>
            REACT
            <br />
            FUNDAMENTALS<span>.</span>
          </h1>

          <p>
            Introduction to React and component-based development.
          </p>
        </div>

        <div className="session-detail-meta">
          <div>
            <span>DOMAIN</span>
            <strong>FRONTEND</strong>
          </div>

          <div>
            <span>DATE</span>
            <strong>10 OCT 2026</strong>
          </div>

          <div>
            <span>TIME</span>
            <strong>17:00</strong>
          </div>

          <div>
            <span>SESSION ID</span>
            <strong>#{id}</strong>
          </div>
        </div>

      </section>


      <section className="session-section">

        <div className="section-heading">
          <span>01</span>

          <div>
            <p className="eyebrow">ATTENDANCE</p>
            <h2>Mark your attendance</h2>
          </div>
        </div>

        <div className="attendance-box">
          <div>
            <span>STATUS</span>
            <strong>NOT MARKED</strong>
          </div>

          <button className="lime-button">
            MARK ATTENDANCE →
          </button>
        </div>

      </section>


      <section className="session-section">

        <div className="section-heading">
          <span>02</span>

          <div>
            <p className="eyebrow">RESOURCES</p>
            <h2>Session material</h2>
          </div>
        </div>

        <div className="resource-list">

          <a
            href="https://react.dev/learn"
            target="_blank"
            rel="noreferrer"
            className="resource-item"
          >
            <span>01</span>
            <strong>React Documentation</strong>
            <span>OPEN ↗</span>
          </a>

          <a
            href="https://react.dev/learn/your-first-component"
            target="_blank"
            rel="noreferrer"
            className="resource-item"
          >
            <span>02</span>
            <strong>React Components Guide</strong>
            <span>OPEN ↗</span>
          </a>

        </div>

      </section>


      <section className="session-section">

        <div className="section-heading">
          <span>03</span>

          <div>
            <p className="eyebrow">TASKS</p>
            <h2>Put it into practice</h2>
          </div>
        </div>

        <div className="task-item">

          <div>
            <span className="task-type">TASK / 001</span>

            <h3>Build a React Counter</h3>

            <p>
              Practice creating components and handling state.
            </p>
          </div>

          <div className="task-action">
            <span>NOT STARTED</span>

            <Link to="/tasks/1">
              VIEW TASK →
            </Link>
          </div>

        </div>

      </section>

    </div>
  )
}

export default SessionDetail