import { useParams } from "react-router-dom"

function ProjectDetail() {
  const { id } = useParams()

  return (
    <div>
      <h1>Campus Event Management System</h1>

      <p>Project ID: {id}</p>
      <p>Domain: Backend</p>

      <p>
        A system for managing college events and registrations.
      </p>
      <button>Join Project</button>

      <hr />

      <section>
        <h2>Members</h2>
        <p>Project members will appear here.</p>
      </section>

      <section>
        <h2>Work Items</h2>
        <p>Tasks assigned to project members will appear here.</p>
      </section>

      <section>
        <h2>Activity</h2>
        <p>Project activity will appear here.</p>
      </section>

      <section>
        <h2>Messages</h2>
        <p>Project messages will appear here.</p>
      </section>
    </div>
  )
}

export default ProjectDetail