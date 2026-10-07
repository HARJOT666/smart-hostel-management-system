import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../hooks/useApi'
import { ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'

export default function HostelsPage() {
  const { user } = useAuth()
  const { data, loading, error, reload } = useApi('/hostels')
  const { data: stats } = useApi('/dashboard/admin')
  const [form, setForm] = useState(null)
  const notify = useToast()

  async function save(e) {
    e.preventDefault()
    try {
      const body = { ...form, totalFloors: Number(form.totalFloors) }
      if (form.id) await api.put(`/hostels/${form.id}`, body)
      else await api.post('/hostels', body)
      notify('Hostel saved')
      setForm(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  const occ = (id) => stats?.hostels.find((h) => h.hostelId === id)

  return (
    <>
      <PageHeader title="Hostels" subtitle="Hostel blocks of the campus">
        {user.role === 'ADMIN' && <button className="btn btn-primary" onClick={() => setForm({ hostelName: '', genderType: 'Male', location: '', totalFloors: 4 })}>Add hostel</button>}
      </PageHeader>
      <ErrorBox message={error} />
      {loading ? <Loading /> : (
        <div className="grid grid-3">
          {data.map((h) => {
            const o = occ(h.id)
            return (
              <div key={h.id} className="card card-pad">
                <div className="row"><h2 style={{ margin: 0 }}>{h.hostelName}</h2><span className="spacer" /><span className="badge badge-neutral">{h.genderType}</span></div>
                <p className="muted" style={{ margin: '6px 0 14px' }}>{h.location} · {h.totalFloors} floors</p>
                {o && (
                  <>
                    <div className="row small"><span>{o.occupied} / {o.beds} beds occupied</span><span className="spacer" /><strong>{o.occupancyPct}%</strong></div>
                    <div className="bar" style={{ marginTop: 6 }}><span style={{ width: `${o.occupancyPct}%` }} /></div>
                  </>
                )}
                {user.role === 'ADMIN' && <button className="btn btn-ghost btn-sm" style={{ marginTop: 14 }} onClick={() => setForm(h)}>Edit</button>}
              </div>
            )
          })}
        </div>
      )}
      {form && (
        <Modal title={form.id ? 'Edit hostel' : 'Add hostel'} onClose={() => setForm(null)}>
          <form onSubmit={save}>
            <Field label="Name"><input className="input" value={form.hostelName} onChange={(e) => setForm({ ...form, hostelName: e.target.value })} required /></Field>
            <div className="grid grid-2" style={{ gap: '0 14px' }}>
              <Field label="For"><select className="select" value={form.genderType} onChange={(e) => setForm({ ...form, genderType: e.target.value })}><option>Male</option><option>Female</option></select></Field>
              <Field label="Floors"><input className="input" type="number" min="1" value={form.totalFloors} onChange={(e) => setForm({ ...form, totalFloors: e.target.value })} /></Field>
            </div>
            <Field label="Location"><input className="input" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} required /></Field>
            <button className="btn btn-primary">Save</button>
          </form>
        </Modal>
      )}
    </>
  )
}
