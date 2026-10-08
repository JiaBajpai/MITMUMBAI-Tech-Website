import { useParams } from "react-router-dom"

function TaskDetail() {
  const { id } = useParams()

  return (
    <div>
      <h1>Build a React Counter</h1>

      <p>Task ID: {id}</p>

      <h2>Description</h2>
      <p>
        Build a simple counter using React components and state.
      </p>

      <h2>Status</h2>
      <p>Not completed</p>

      <button>Mark as Complete</button>
    </div>
  )
}

export default TaskDetail