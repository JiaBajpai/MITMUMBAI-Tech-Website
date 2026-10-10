import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import { ApiError, NetworkError } from '../api/client'

export default function PasswordSetupPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [token] = useState(() => new URLSearchParams(location.search).get('token') || '')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [complete, setComplete] = useState(false)

  useEffect(() => {
    if (token) navigate('/set-password', { replace: true })
  }, [navigate, token])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    if (!token) { setError('This password link is missing its token. Request a new link from an administrator.'); return }
    if (password.length < 12 || password.length > 128) { setError('Choose a password between 12 and 128 characters.'); return }
    if (password !== confirmation) { setError('The passwords do not match.'); return }
    setBusy(true)
    try { await authApi.setupPassword(token, password); setPassword(''); setConfirmation(''); setComplete(true) }
    catch (cause) {
      if (cause instanceof NetworkError) setError('The service could not be reached. Try again when your connection is restored.')
      else if (cause instanceof ApiError) setError(cause.message)
      else setError('Password setup could not be completed. Request a new link from an administrator.')
    } finally { setBusy(false) }
  }

  return <section className="editorial-page login-public-page">
    <p className="public-eyebrow">MEMBERS CORNER · ACCOUNT SECURITY</p>
    <h1>{complete ? 'Password updated.' : 'Set your password.'}</h1>
    {complete ? <><p className="login-public-intro">Your password is set. Existing sessions were signed out. You can now sign in.</p><Link className="public-button dark" to="/login">Continue to sign in →</Link></> : <>
      <p className="login-public-intro">Choose a new password for your MIT Tech Kernel account. This one-time link expires and cannot be reused.</p>
      <form className="login-public-form" onSubmit={(event) => void submit(event)}>
        <label htmlFor="new-password">New password</label><input id="new-password" type="password" autoComplete="new-password" minLength={12} maxLength={128} required value={password} onChange={(event) => setPassword(event.target.value)} />
        <label htmlFor="confirm-password">Confirm password</label><input id="confirm-password" type="password" autoComplete="new-password" minLength={12} maxLength={128} required value={confirmation} onChange={(event) => setConfirmation(event.target.value)} />
        {error && <p className="login-public-status" role="alert">{error}</p>}
        <button className="public-button dark" type="submit" disabled={busy}>{busy ? 'Updating…' : 'Set password →'}</button>
      </form>
    </>}
  </section>
}
