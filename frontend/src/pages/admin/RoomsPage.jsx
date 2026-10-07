import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../hooks/useApi'
import HostelSelect from '../../components/HostelSelect'
import { DataTable, ErrorBox, Field, Loading, Modal, PageHeader, Stat } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { query } from '../../utils/format'

export default function RoomsPage() {
  const { user } = useAuth()
  const [filters, setFilters] = useState({ hostelId: 1, roomType: '', availableOnly: false })
  const { data, loading, error, reload } = useApi('/rooms' + query(filters))
  const [occupants, setOccupants] = useState(null)
  const [form, setForm] = useState(null)
  const notify = useToast()

  async function showOccupants(room) {
    const { data: list } = await api.get(`/rooms/${room.id}/occupants`)
    setOccupants({ room, list })
  }

  async function save(e) {
    e.preventDefault()
    try {
      const body = { ...form, floorNo: Number(form.floorNo) }
      if (form.id) await api.put(`/rooms/${form.id}`, body)
      else await api.post('/rooms', body)
      notify('Room saved')
      setForm(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  const rows = data || []
  const beds = rows.reduce((t, r) => t + r.capacity, 0)
  const free = rows.reduce((t, r) => t + r.availableBeds, 0)

  return (
    <>
      <PageHeader title="Rooms & vacancy" subtitle="Live occupancy from Oracle (not cached)">
        {user.role === 'ADMIN' && <button className="btn btn-primary" onClick={() => setForm({ hostelId: filters.hostelId || 1, roomNumber: '', floorNo: 1, roomType: 'Double' })}>Add room</button>}
      </PageHeader>
      <div className="toolbar">
        <HostelSelect value={filters.hostelId} onChange={(v) => setFilters({ ...filters, hostelId: v })} />
        <select className="select" value={filters.roomType} onChange={(e) => setFilters({ ...filters, roomType: e.target.value })}>
          <option value="">All room types</option>{['Single', 'Double', 'Triple', 'Quad'].map((t) => <option key={t}>{t}</option>)}
        </select>
        <label className="row small"><input type="checkbox" checked={filters.availableOnly} onChange={(e) => setFilters({ ...filters, availableOnly: e.target.checked })} /> Only rooms with free beds</label>
      </div>
      <div className="grid grid-3" style={{ marginBottom: 16 }}>
        <Stat label="Rooms shown" value={rows.length} />
        <Stat label="Beds" value={beds} />
        <Stat label="Free beds" value={free} />
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={rows} pageSize={20} columns={[
            { key: 'roomNumber', label: 'Room', render: (r) => <span style={{ fontWeight: 500 }}>{r.roomNumber}</span> },
            { key: 'hostelName', label: 'Hostel' },
            { key: 'floorNo', label: 'Floor' },
            { key: 'roomType', label: 'Type' },
            { key: 'occupancy', label: 'Occupancy', render: (r) => (
              <div style={{ minWidth: 140 }}>
                <div className="small">{r.occupiedCount} / {r.capacity}</div>
                <div className="bar"><span style={{ width: `${(r.occupiedCount / r.capacity) * 100}%` }} /></div>
              </div>) },
            { key: 'availableBeds', label: 'Free', render: (r) => r.availableBeds === 0 ? <span className="badge badge-neutral">Full</span> : <span className="badge badge-green">{r.availableBeds} free</span> },
            { key: 'actions', label: '', render: (r) => (
              <div className="row" style={{ justifyContent: 'flex-end' }}>
                <button className="btn btn-ghost btn-sm" onClick={() => showOccupants(r)}>Occupants</button>
                {user.role === 'ADMIN' && <button className="btn btn-ghost btn-sm" onClick={() => setForm({ id: r.id, hostelId: r.hostelId, roomNumber: r.roomNumber, floorNo: r.floorNo, roomType: r.roomType })}>Edit</button>}
              </div>) },
          ]} />
        )}
      </div>
      {occupants && (
        <Modal title={`Room ${occupants.room.roomNumber}`} onClose={() => setOccupants(null)}>
          {occupants.list.length === 0 ? <p className="muted">This room is empty.</p> : (
            <ul className="list-plain">
              {occupants.list.map((a) => <li key={a.id}>Bed {a.bedNumber} — {a.studentName} <span className="muted mono">{a.registrationNo}</span></li>)}
            </ul>
          )}
        </Modal>
      )}
      {form && (
        <Modal title={form.id ? 'Edit room' : 'Add room'} onClose={() => setForm(null)}>
          <form onSubmit={save}>
            <Field label="Hostel"><HostelSelect value={form.hostelId} onChange={(v) => setForm({ ...form, hostelId: v })} allowAll={false} /></Field>
            <div className="grid grid-3" style={{ gap: '0 14px' }}>
              <Field label="Room number"><input className="input" value={form.roomNumber} onChange={(e) => setForm({ ...form, roomNumber: e.target.value })} required /></Field>
              <Field label="Floor"><input className="input" type="number" min="0" value={form.floorNo} onChange={(e) => setForm({ ...form, floorNo: e.target.value })} /></Field>
              <Field label="Type"><select className="select" value={form.roomType} onChange={(e) => setForm({ ...form, roomType: e.target.value })}>{['Single', 'Double', 'Triple', 'Quad'].map((t) => <option key={t}>{t}</option>)}</select></Field>
            </div>
            <button className="btn btn-primary">Save</button>
          </form>
        </Modal>
      )}
    </>
  )
}
