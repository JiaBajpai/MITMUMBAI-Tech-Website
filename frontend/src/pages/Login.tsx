import { useEffect, useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../app/useAuth'
import { ApiError, NetworkError } from '../api/client'

export default function Login() {
  const { signIn, isAuthenticated } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const returnTo = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname || '/app/dashboard'

  useEffect(() => { if (isAuthenticated) navigate(returnTo, { replace: true }) }, [isAuthenticated, navigate, returnTo])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await signIn(email.trim(), password)
      setPassword('')
      navigate(returnTo, { replace: true })
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 401) setError('That email and password combination was not accepted.')
      else if (cause instanceof ApiError) setError(cause.message)
      else if (cause instanceof NetworkError) setError('The sign-in service could not be reached. Check your connection and try again.')
      else setError('Sign-in could not be completed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return <section className="editorial-page login-public-page">
    <p className="public-eyebrow">MEMBERS CORNER · SIGN IN</p>
    <h1>Welcome back.</h1>
    <p className="login-public-intro">Sign in to continue to the MIT Tech Kernel member platform.</p>
    <form className="login-public-form" onSubmit={(event) => void submit(event)}>
      <label htmlFor="member-email">Email address</label>
      <input id="member-email" type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} required maxLength={254} />
      <label htmlFor="member-password">Password</label>
      <input id="member-password" type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required />
      {error && <p className="login-public-status" role="alert">{error}</p>}
      <button className="public-button dark" type="submit" disabled={loading}>{loading ? 'Signing in…' : 'Sign in →'}</button>
    </form>
    <p className="login-public-note">Access for MIT Tech Kernel members. Your credentials are sent directly to the sign-in service and are not saved in browser storage.</p>
  </section>
}
