function Dashboard() {
  return (
    <div className="dashboard">

      <section className="dashboard-hero">

        <div className="hero-content">
          <p className="eyebrow">MEMBERS CORNER // 001</p>

          <h1>
            BUILD.
            <br />
            <span>EXPLORE.</span>
            <br />
            COMPETE.
          </h1>

          <p className="hero-description">
            Your technical journey through MIT Tech Kernel.
            Learn, build, contribute and grow.
          </p>
        </div>

        <div className="hero-orbit">
          <div className="orbit orbit-one"></div>
          <div className="orbit orbit-two"></div>
          <div className="orbit-core"></div>
          <div className="orbit-node"></div>
        </div>

      </section>


      <section className="stats-grid">

        <div className="stat-card">
          <p>XP</p>
          <h2>450</h2>
          <span>+120 THIS WEEK</span>
        </div>

        <div className="stat-card">
          <p>SESSIONS</p>
          <h2>18</h2>
          <span>12 ATTENDED</span>
        </div>

        <div className="stat-card">
          <p>TASKS</p>
          <h2>24</h2>
          <span>18 COMPLETED</span>
        </div>

        <div className="stat-card accent-card">
          <p>RANK</p>
          <h2>#04</h2>
          <span>TOP 10%</span>
        </div>

      </section>


      <section className="dashboard-columns">

        <div className="dashboard-panel">

          <div className="panel-header">
            <div>
              <p className="eyebrow">01 / PROGRESS</p>
              <h2>Your Skills</h2>
            </div>

            <span className="panel-number">SKILL / 005</span>
          </div>

          <div className="skill-list">

            <div className="skill">
              <div className="skill-info">
                <span>Frontend</span>
                <span>78%</span>
              </div>

              <div className="skill-bar">
                <div style={{ width: "78%" }}></div>
              </div>
            </div>

            <div className="skill">
              <div className="skill-info">
                <span>Backend</span>
                <span>62%</span>
              </div>

              <div className="skill-bar">
                <div style={{ width: "62%" }}></div>
              </div>
            </div>

            <div className="skill">
              <div className="skill-info">
                <span>AI / ML / IoT</span>
                <span>35%</span>
              </div>

              <div className="skill-bar">
                <div style={{ width: "35%" }}></div>
              </div>
            </div>

            <div className="skill">
              <div className="skill-info">
                <span>Competitive Programming</span>
                <span>28%</span>
              </div>

              <div className="skill-bar">
                <div style={{ width: "28%" }}></div>
              </div>
            </div>

          </div>

        </div>


        <div className="dashboard-panel recommendation-panel">

          <div className="panel-header">
            <div>
              <p className="eyebrow">02 / NEXT</p>
              <h2>Recommended</h2>
            </div>

            <span className="panel-number">MATCH / 092</span>
          </div>

          <div className="recommendation">

            <span className="recommendation-type">
              PROJECT
            </span>

            <h3>
              Build a REST API
              <br />
              with Spring Boot
            </h3>

            <p>
              Matches your backend skills and recent
              learning activity.
            </p>

            <button className="lime-button">
              EXPLORE PROJECT →
            </button>

          </div>

        </div>

      </section>


      <section className="activity-panel">

        <div className="panel-header">
          <div>
            <p className="eyebrow">03 / ACTIVITY</p>
            <h2>Recent Activity</h2>
          </div>

          <span className="panel-number">LIVE</span>
        </div>

        <div className="activity-list">

          <div className="activity-item">
            <span className="activity-index">01</span>
            <span>Completed React Fundamentals task</span>
            <span className="activity-time">2H AGO</span>
          </div>

          <div className="activity-item">
            <span className="activity-index">02</span>
            <span>Attended Git & GitHub session</span>
            <span className="activity-time">1D AGO</span>
          </div>

          <div className="activity-item">
            <span className="activity-index">03</span>
            <span>Joined Campus Event Management project</span>
            <span className="activity-time">2D AGO</span>
          </div>

        </div>

      </section>

    </div>
  )
}

export default Dashboard