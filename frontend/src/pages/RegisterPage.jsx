import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { errorMessage } from '../api/client'
import './auth.css'

const BRANCHES = ['Computer Science and Engineering', 'CSE (Artificial Intelligence)', 'Information Technology',
  'Electronics and Communication', 'Mechanical Engineering', 'CSE (Cyber Security)']

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ registrationNo: '', name: '', email: '', phone: '', gender: 'Male', year: 1, branch: BRANCHES[0], cgpa: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function handleSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.post('/auth/register', { ...form, registrationNo: form.registrationNo.toUpperCase(), year: Number(form.year), cgpa: form.cgpa === '' ? null : Number(form.cgpa) })
      navigate('/login', { state: { registered: true } })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <section className="auth-visual" aria-hidden="true">
        <div className="auth-kicker">New resident</div>
        <h1>Register once, request anything.</h1>
        <p>After registering, the warden allocates your room. You can then raise complaints, apply for leave and request visitor passes online.</p>
      </section>
      <section className="auth-panel">
        <form className="auth-form" style={{ maxWidth: 460 }} onSubmit={handleSubmit}>
          <div className="auth-mark"><div className="brand-mark">SH</div><span>Smart Hostel</span></div>
          <h2>Create an account</h2>
          <p className="auth-sub">Students only. Staff accounts are created by the administrator.</p>
          {error && <div className="alert alert-error">{error}</div>}
          <div className="grid grid-2" style={{ gap: '0 14px' }}>
            <div className="field"><label>Registration no.</label><input className="input" value={form.registrationNo} onChange={set('registrationNo')} placeholder="26BYB1234" required /></div>
            <div className="field"><label>Full name</label><input className="input" value={form.name} onChange={set('name')} required /></div>
            <div className="field"><label>E-mail</label><input className="input" type="email" value={form.email} onChange={set('email')} required /></div>
            <div className="field"><label>Phone</label><input className="input" value={form.phone} onChange={set('phone')} placeholder="10 digits" required /></div>
            <div className="field"><label>Gender</label><select className="select" value={form.gender} onChange={set('gender')}><option>Male</option><option>Female</option></select></div>
            <div className="field"><label>Year</label><select className="select" value={form.year} onChange={set('year')}>{[1, 2, 3, 4].map((y) => <option key={y}>{y}</option>)}</select></div>
            <div className="field"><label>Branch</label><select className="select" value={form.branch} onChange={set('branch')}>{BRANCHES.map((b) => <option key={b}>{b}</option>)}</select></div>
            <div className="field"><label>CGPA (optional)</label><input className="input" type="number" step="0.01" min="0" max="10" value={form.cgpa} onChange={set('cgpa')} /></div>
          </div>
          <div className="field"><label>Password</label><input className="input" type="password" value={form.password} onChange={set('password')} placeholder="At least 8 characters" required /></div>
          <button className="btn btn-dark auth-submit" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</button>
          <p className="auth-alt">Already registered? <Link to="/login">Sign in</Link></p>
        </form>
      </section>
    </div>
  )
}
