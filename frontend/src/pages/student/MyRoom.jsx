import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate } from '../../utils/format'

export default function MyRoom() {
  const { data, loading, error, reload } = useApi('/students/me')
  const [editing, setEditing] = useState(null)
  const notify = useToast()

  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />
  const { student, room, hostel, roommates, history } = data

  async function save(e) {
    e.preventDefault()
    try {
      await api.put('/students/me', editing)
      notify('Contact details updated')
      setEditing(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <PageHeader title="My room" subtitle="Your allocation and personal details as stored by the hostel office">
        <button className="btn btn-ghost" onClick={() => setEditing({ email: student.email, phone: student.phone })}>Edit contact details</button>
      </PageHeader>

      <div className="grid grid-2" style={{ marginBottom: 16 }}>
        <div className="card card-pad">
          <h2>Allocation</h2>
          {room ? (
            <dl className="kv">
              <dt>Hostel</dt><dd>{hostel.hostelName} <span className="muted">({hostel.genderType})</span></dd>
              <dt>Location</dt><dd>{hostel.location}</dd>
              <dt>Room</dt><dd>{room.roomNumber} · floor {room.floorNo}</dd>
              <dt>Room type</dt><dd>{room.roomType} ({room.capacity} beds, {room.occupiedCount} occupied)</dd>
              <dt>Your bed</dt><dd>Bed {student.bedNumber}</dd>
              <dt>Roommates</dt><dd>{roommates.length ? roommates.join(', ') : <span className="muted">none</span>}</dd>
            </dl>
          ) : <p className="muted">You do not have a room yet.</p>}
        </div>
        <div className="card card-pad">
          <h2>Profile</h2>
          <dl className="kv">
            <dt>Name</dt><dd>{student.name}</dd>
            <dt>Registration no.</dt><dd>{student.registrationNo}</dd>
            <dt>Branch</dt><dd>{student.branch}</dd>
            <dt>Year</dt><dd>{student.year}</dd>
            <dt>CGPA</dt><dd>{student.cgpa ?? '—'}</dd>
            <dt>E-mail</dt><dd>{student.email}</dd>
            <dt>Phone</dt><dd>{student.phone}</dd>
          </dl>
        </div>
      </div>

      <div className="card">
        <div className="card-pad" style={{ paddingBottom: 0 }}><h2>Allocation history</h2></div>
        <DataTable rows={history} columns={[
          { key: 'hostelName', label: 'Hostel' },
          { key: 'roomNumber', label: 'Room' },
          { key: 'bedNumber', label: 'Bed' },
          { key: 'allocationDate', label: 'From', render: (r) => fmtDate(r.allocationDate) },
          { key: 'vacateDate', label: 'To', render: (r) => fmtDate(r.vacateDate) },
          { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
        ]} />
      </div>

      {editing && (
        <Modal title="Edit contact details" onClose={() => setEditing(null)}>
          <form onSubmit={save}>
            <Field label="E-mail"><input className="input" type="email" value={editing.email} onChange={(e) => setEditing({ ...editing, email: e.target.value })} /></Field>
            <Field label="Phone"><input className="input" value={editing.phone} onChange={(e) => setEditing({ ...editing, phone: e.target.value })} /></Field>
            <button className="btn btn-primary">Save</button>
          </form>
        </Modal>
      )}
    </>
  )
}
