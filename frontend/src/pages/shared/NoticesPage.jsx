import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../hooks/useApi'
import HostelSelect from '../../components/HostelSelect'
import { Empty, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, query } from '../../utils/format'

const CATEGORY_BADGE = { Urgent: 'red', Maintenance: 'blue', Mess: 'green', Event: 'accent', General: 'neutral' }

export default function NoticesPage() {
  const { user } = useAuth()
  const canManage = user.role === 'ADMIN' || user.role === 'WARDEN'
  const [hostelId, setHostelId] = useState(null)
  const { data, loading, error, reload } = useApi('/announcements' + query({ hostelId }))
  const [form, setForm] = useState(null)
  const notify = useToast()

  async function save(e) {
    e.preventDefault()
    try {
      const body = { ...form, hostelId: form.hostelId || null, expiryDate: form.expiryDate || null }
      if (form.id) await api.put(`/announcements/${form.id}`, body)
      else await api.post('/announcements', body)
      notify('Notice saved')
      setForm(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  async function remove(id) {
    try {
      await api.delete(`/announcements/${id}`)
      notify('Notice removed')
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <PageHeader title="Notice board" subtitle={user.role === 'STUDENT' ? 'Notices for your hostel and for all residents' : 'Hostel announcements'}>
        {user.role !== 'STUDENT' && <HostelSelect value={hostelId} onChange={setHostelId} />}
        {canManage && <button className="btn btn-primary" onClick={() => setForm({ title: '', body: '', category: 'General', hostelId: null, expiryDate: '', pinned: false })}>Post a notice</button>}
      </PageHeader>
      <ErrorBox message={error} />
      {loading ? <Loading /> : !data?.length ? <Empty text="No notices." /> : (
        <div className="stack">
          {data.map((a) => (
            <article key={a.id} className="card card-pad" style={a.pinned ? { borderLeft: '3px solid var(--accent)' } : undefined}>
              <div className="row" style={{ marginBottom: 6 }}>
                <span className={`badge badge-${CATEGORY_BADGE[a.category]}`}>{a.category}</span>
                {a.pinned && <span className="badge badge-accent">Pinned</span>}
                <span className="muted small">{a.hostelName} · {fmtDate(a.postedDate)} · {a.postedBy}</span>
                <span className="spacer" />
                {canManage && (
                  <>
                    <button className="btn btn-ghost btn-sm" onClick={() => setForm({ ...a, expiryDate: a.expiryDate || '' })}>Edit</button>
                    <button className="btn btn-danger btn-sm" onClick={() => remove(a.id)}>Delete</button>
                  </>
                )}
              </div>
              <h2 style={{ marginBottom: 6 }}>{a.title}</h2>
              <p style={{ margin: 0, color: 'var(--ink-2)' }}>{a.body}</p>
              {a.expiryDate && <div className="muted small" style={{ marginTop: 8 }}>Valid until {fmtDate(a.expiryDate)}</div>}
            </article>
          ))}
        </div>
      )}

      {form && (
        <Modal title={form.id ? 'Edit notice' : 'Post a notice'} onClose={() => setForm(null)}>
          <form onSubmit={save}>
            <Field label="Title"><input className="input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></Field>
            <Field label="Message"><textarea className="textarea" value={form.body} onChange={(e) => setForm({ ...form, body: e.target.value })} required /></Field>
            <div className="grid grid-2" style={{ gap: '0 14px' }}>
              <Field label="Category">
                <select className="select" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                  {['General', 'Maintenance', 'Mess', 'Event', 'Urgent'].map((c) => <option key={c}>{c}</option>)}
                </select>
              </Field>
              <Field label="Hostel"><HostelSelect value={form.hostelId} onChange={(v) => setForm({ ...form, hostelId: v })} allLabel="All hostels" /></Field>
              <Field label="Valid until (optional)"><input className="input" type="date" value={form.expiryDate} onChange={(e) => setForm({ ...form, expiryDate: e.target.value })} /></Field>
              <Field label="Pin to top">
                <label className="row" style={{ paddingTop: 8 }}><input type="checkbox" checked={form.pinned} onChange={(e) => setForm({ ...form, pinned: e.target.checked })} /> Pinned</label>
              </Field>
            </div>
            <button className="btn btn-primary">Save</button>
          </form>
        </Modal>
      )}
    </>
  )
}
