import { Link } from "react-router-dom"

const projects = [
  {
    id: 1,
    title: "Campus Event Management System",
    domain: "BACKEND",
    description:
      "A system for managing college events, registrations and participation.",
  },
  {
    id: 2,
    title: "MIT Tech Club Website",
    domain: "FRONTEND",
    description:
      "A website and members platform for the MIT Tech Kernel community.",
  },
  {
    id: 3,
    title: "Smart Attendance System",
    domain: "AI / ML / IOT",
    description:
      "An automated attendance system using computer vision.",
  },
]

function Projects() {
  return (
    <div className="projects-page">

      <section className="projects-header">

        <div>
          <p className="eyebrow">02 / BUILD</p>

          <h1>
            PROJECTS<span>.</span>
          </h1>

          <p className="projects-description">
            Explore projects, find opportunities and build
            something worth contributing to.
          </p>
        </div>

        <div className="project-counter">
          <span>AVAILABLE</span>
          <strong>
            {String(projects.length).padStart(3, "0")}
          </strong>
        </div>

      </section>


      <section className="project-list">

        {projects.map((project, index) => (

          <article
            className="project-card"
            key={project.id}
          >

            <div className="project-index">
              {String(index + 1).padStart(2, "0")}
            </div>


            <div className="project-main">

              <span className="project-domain">
                {project.domain}
              </span>

              <h2>{project.title}</h2>

              <p>{project.description}</p>

            </div>


            <div className="project-action">

              <span className="project-status">
                ACTIVE
              </span>

              <Link to={`/projects/${project.id}`}>
                VIEW PROJECT →
              </Link>

            </div>

          </article>

        ))}

      </section>

    </div>
  )
}

export default Projects