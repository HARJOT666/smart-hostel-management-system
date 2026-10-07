import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { errorMessage } from '../api/client'
import { homeFor, useAuth } from '../auth/AuthContext'
import './auth.css'

const DEMO_ACCOUNTS = [
  ['Student', '24BYB1001'],
  ['Admin', 'admin@shms.local'],
  ['Warden', 'warden@shms.local'],
  ['Maintenance', 'maint@shms.local'],
]

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={homeFor(user.role)} replace />

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const loggedIn = await login(username.trim(), password)
      navigate(homeFor(loggedIn.role))
    } catch (err) {
      // 401 = wrong password, 429 = too many failed attempts (Redis rate limit)
      setError(err.response?.status === 401 ? 'Incorrect username or password.' : errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <section className="auth-visual" aria-hidden="true">
        <div className="auth-credit">
          Photo: <a href="https://unsplash.com/photos/wD3dur3v9aE" target="_blank" rel="noreferrer">Jonathan Borba / Unsplash</a>
        </div>
        <div className="auth-kicker">Smart Hostel Management System</div>
        <h1>A calmer way to run residence life.</h1>
        <p>Rooms, requests and approvals for students, wardens and maintenance staff — kept in one place.</p>
        <div className="auth-points">
          <span>Room allocation</span><span>Complaints</span><span>Leave &amp; visitors</span>
        </div>
      </section>

      <section className="auth-panel">
        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="auth-mark"><div className="brand-mark">SH</div><span>Smart Hostel</span></div>
          <h2>Welcome back</h2>
          <p className="auth-sub">Sign in with your registration number or staff e-mail.</p>

          {location.state?.registered && !error && <div className="alert alert-success">Registration successful. Sign in with your registration number.</div>}
          {error && <div className="alert alert-error" role="alert">{error}</div>}

          <div className="field">
            <label htmlFor="username">Registration number or e-mail</label>
            <input id="username" className="input" autoComplete="username" autoFocus
                   value={username} onChange={(e) => setUsername(e.target.value)} placeholder="e.g. 24BYB1001" required />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <div className="pw-wrap">
              <input id="password" className="input" type={showPassword ? 'text' : 'password'} autoComplete="current-password"
                     value={password} onChange={(e) => setPassword(e.target.value)} required />
              <button type="button" className="pw-toggle" onClick={() => setShowPassword(!showPassword)}>
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
          </div>
          <button className="btn btn-dark auth-submit" disabled={busy || !username || !password}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>

          <p className="auth-alt">New resident? <Link to="/register">Create a student account</Link></p>

          <details className="auth-demo">
            <summary>Demo accounts</summary>
            <table>
              <tbody>
                {DEMO_ACCOUNTS.map(([role, name]) => (
                  <tr key={name}>
                    <td>{role}</td>
                    <td><button type="button" onClick={() => { setUsername(name); setPassword('Hostel@123') }}>{name}</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div style={{ marginTop: 6 }}>Password for all demo accounts: <span className="mono">Hostel@123</span></div>
          </details>

          <div className="auth-foot">BCSE302L Database Systems · BCSE31P Software Engineering · VIT</div>
        </form>
      </section>
    </div>
  )
}
