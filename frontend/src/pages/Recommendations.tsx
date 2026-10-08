const recommendations = [
  {
    id: 1,
    title: "Build a REST API with Spring Boot",
    type: "PROJECT",
    domain: "BACKEND",
    match: 92,
    reason: "Matches your backend skills and recent sessions.",
  },
  {
    id: 2,
    title: "React Dashboard Project",
    type: "PROJECT",
    domain: "FRONTEND",
    match: 84,
    reason: "Matches your frontend learning and completed tasks.",
  },
  {
    id: 3,
    title: "Git & GitHub Workshop",
    type: "SESSION",
    domain: "GENERAL",
    match: 78,
    reason: "Useful for improving your project collaboration skills.",
  },
]

function Recommendations() {
  return (
    <div className="recommendations-page">

      <section className="recommendations-header">

        <div>
          <p className="eyebrow">03 / DISCOVER</p>

          <h1>
            RECOMMENDED<span>.</span>
          </h1>

          <p className="recommendations-description">
            Learning opportunities and projects selected
            from your Kernel activity.
          </p>
        </div>

        <div className="recommendation-counter">
          <span>FOR YOU</span>
          <strong>003</strong>
        </div>

      </section>


      <section className="recommendation-list">

        {recommendations.map((item, index) => (

          <article
            className="recommendation-card"
            key={item.id}
          >

            <div className="recommendation-index">
              {String(index + 1).padStart(2, "0")}
            </div>


            <div className="recommendation-main">

              <div className="recommendation-meta">
                <span>{item.type}</span>
                <span>{item.domain}</span>
              </div>

              <h2>{item.title}</h2>

              <p>{item.reason}</p>

            </div>


            <div className="recommendation-score">

              <span>MATCH</span>

              <strong>{item.match}%</strong>

              <div className="match-bar">
                <div
                  style={{
                    width: `${item.match}%`,
                  }}
                />
              </div>

            </div>

          </article>

        ))}

      </section>

    </div>
  )
}

export default Recommendations