const leaderboard = [
  {
    rank: 1,
    name: "Aarav Sharma",
    domain: "BACKEND",
    xp: 1250,
  },
  {
    rank: 2,
    name: "Riya Patel",
    domain: "FRONTEND",
    xp: 1100,
  },
  {
    rank: 3,
    name: "Kabir Shah",
    domain: "AI / ML",
    xp: 980,
  },
  {
    rank: 4,
    name: "Jia Bajpai",
    domain: "FRONTEND",
    xp: 850,
  },
  {
    rank: 5,
    name: "Arjun Mehta",
    domain: "CP",
    xp: 790,
  },
]

function Leaderboards() {
  return (
    <div className="leaderboard-page">

      <section className="leaderboard-header">

        <div>
          <p className="eyebrow">04 / COMPETE</p>

          <h1>
            LEADERBOARD<span>.</span>
          </h1>

          <p className="leaderboard-description">
            Track XP, compare progress and see where you
            stand within the Kernel community.
          </p>
        </div>

        <div className="leaderboard-season">
          <span>SEASON</span>
          <strong>01</strong>
        </div>

      </section>


      <section className="leaderboard-table">

        <div className="leaderboard-table-header">
          <span>RANK</span>
          <span>MEMBER</span>
          <span>DOMAIN</span>
          <span>XP</span>
        </div>


        {leaderboard.map((member) => (

          <div
            className={`leaderboard-row ${
              member.rank === 4
                ? "current-user"
                : ""
            }`}
            key={member.rank}
          >

            <span className="leaderboard-rank">
              {String(member.rank).padStart(2, "0")}
            </span>


            <span className="leaderboard-name">
              {member.name}

              {member.rank === 4 && (
                <small>YOU</small>
              )}
            </span>


            <span className="leaderboard-domain">
              {member.domain}
            </span>


            <span className="leaderboard-xp">
              {member.xp.toLocaleString()}
            </span>

          </div>

        ))}

      </section>


      <section className="leaderboard-footer">

        <div>
          <span>YOUR POSITION</span>
          <strong>#04</strong>
        </div>

        <div>
          <span>TOTAL XP</span>
          <strong>850</strong>
        </div>

        <div>
          <span>NEXT RANK</span>
          <strong>+130 XP</strong>
        </div>

      </section>

    </div>
  )
}

export default Leaderboards