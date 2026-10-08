import { useState } from "react"
import { useNavigate } from "react-router-dom"

function Login() {
  const navigate = useNavigate()

  const [userId, setUserId] = useState("")
  const [password, setPassword] = useState("")

  const handleLogin = (event: React.FormEvent) => {
    event.preventDefault()

    // Temporary frontend-only login
    console.log("Login:", { userId, password })

    navigate("/dashboard")
  }

  return (
    <main className="login-page">

      <div className="login-grid" />

      <header className="login-topbar">
        <div className="login-brand">
          <img
            src="/assets/kernel-mark.png"
            alt="MIT Tech Kernel"
          />
        </div>

        <span>AUTH / 001</span>
      </header>


      <section className="login-content">

        <div className="login-form-section">

          <p className="eyebrow">
            MEMBERS CORNER
          </p>

          <h1>
            ENTER THE
            <br />
            <span>KERNEL.</span>
          </h1>

          <p className="login-description">
            Access the MIT Tech Kernel members platform.
          </p>


          <form onSubmit={handleLogin}>

            <div className="login-field">
              <label htmlFor="userId">
                USER ID
              </label>

              <input
                id="userId"
                type="text"
                value={userId}
                onChange={(event) =>
                  setUserId(event.target.value)
                }
                required
              />
            </div>


            <div className="login-field">
              <label htmlFor="password">
                PASSWORD
              </label>

              <input
                id="password"
                type="password"
                value={password}
                onChange={(event) =>
                  setPassword(event.target.value)
                }
                required
              />
            </div>


            <button
              type="submit"
              className="login-button"
            >
              SIGN IN →
            </button>

          </form>


          <p className="login-note">
            ACCESS RESTRICTED TO KERNEL MEMBERS
          </p>

        </div>


        <div className="login-visual">

          <div className="login-orbit orbit-a" />
          <div className="login-orbit orbit-b" />

          <div className="login-sphere">
            <img
              src="/assets/kernel-mark.png"
              alt=""
            />
          </div>

          <div className="login-node node-a" />
          <div className="login-node node-b" />

          <div className="login-coordinate">
            19.0728° N
            <br />
            72.8826° E
          </div>

        </div>

      </section>


      <footer className="login-footer">
        <span>MIT TECH KERNEL</span>
        <span>MIT MUMBAI // 2026</span>
      </footer>

    </main>
  )
}

export default Login