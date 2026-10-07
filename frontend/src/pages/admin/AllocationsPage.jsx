import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import useApi from '../../hooks/useApi'
import HostelSelect from '../../components/HostelSelect'
import { Badge, DataTable, ErrorBox, Loading, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, query } from '../../utils/format'

/**
 * Room allocation. "Allocate" calls POST /api/allocations, which runs the Oracle
 * procedure sp_allocate_room; the occupancy trigger then updates the room.
 */
export default function AllocationsPage() {
  const [tab, setTab] = useState('allocate')
  return (
    <>
      <PageHeader title="Room allocation" subtitle="Allocate beds to waiting students and manage current allocations" />
      <div className="tabs">
        <button className={`tab ${tab === 'allocate' ? 'active' : ''}`} onClick={() => setTab('allocate')}>Allocate a room</button>
        <button className={`tab ${tab === 'list' ? 'active' : ''}`} onClick={() => setTab('list')}>Allocations</button>
      </div>
      {tab === 'allocate' ? <AllocateTab /> : <ListTab />}
    </>
  )
}

function AllocateTab() {
  const waiting = useApi('/students/waiting')
  const [student, setStudent] = useState(null)
  const [hostelId, setHostelId] = useState(null)
  const rooms = useApi(student ? '/rooms/available' + query({ gender: student.gender, hostelId }) : null)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const notify = useToast()

  async function allocate(room) {
    setBusy(true)
    setError('')
    try {
      const { data } = await api.post('/allocations', { studentId: student.id, roomId: room.id })
      setResult(data)
      notify(`${data.studentName} → ${data.roomNumber}, bed ${data.bedNumber}`)
      setStudent(null)
      waiting.reload()
    } catch (err) {
      setError(errorMessage(err))   // e.g. the ORA-20xxx message from sp_allocate_room
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid grid-2">
      <div className="card">
        <div className="card-pad" style={{ paddingBottom: 0 }}>
          <h2>1. Choose a student</h2>
          <p className="muted small" style={{ marginTop: -6 }}>{waiting.data ? `${waiting.data.length} students are waiting for a room` : ''}</p>
        </div>
        {waiting.loading ? <Loading /> : (
          <DataTable rows={waiting.data || []} pageSize={10} empty="Every student has a room." columns={[
            { key: 'registrationNo', label: 'Reg. no.', render: (s) => <span className="mono">{s.registrationNo}</span> },
            { key: 'name', label: 'Name' },
            { key: 'gender', label: 'Gender' },
            { key: 'year', label: 'Year' },
            { key: 'actions', label: '', render: (s) => (
              <button className={`btn btn-sm ${student?.id === s.id ? 'btn-dark' : 'btn-ghost'}`} onClick={() => { setStudent(s); setResult(null); setError('') }}>
                {student?.id === s.id ? 'Selected' : 'Select'}
              </button>) },
          ]} />
        )}
      </div>

      <div className="card">
        <div className="card-pad" style={{ paddingBottom: 8 }}>
          <h2>2. Choose a room</h2>
          {result && <div className="alert alert-success">Allocated {result.studentName} to {result.roomNumber} ({result.hostelName}), bed {result.bedNumber}. Allocation #{result.id}.</div>}
          <ErrorBox message={error} />
          {!student ? <p className="muted">Select a student first. Only rooms in hostels of the student's gender with free beds are listed.</p> : (
            <div className="toolbar" style={{ marginBottom: 0 }}>
              <span>For <strong>{student.name}</strong> ({student.gender})</span>
              <HostelSelect value={hostelId} onChange={setHostelId} />
            </div>
          )}
        </div>
        {student && (rooms.loading ? <Loading /> : (
          <DataTable rows={rooms.data || []} pageSize={10} empty="No free beds in this selection." columns={[
            { key: 'roomNumber', label: 'Room' },
            { key: 'hostelName', label: 'Hostel' },
            { key: 'roomType', label: 'Type' },
            { key: 'availableBeds', label: 'Free beds' },
            { key: 'actions', label: '', render: (r) => <button className="btn btn-primary btn-sm" disabled={busy} onClick={() => allocate(r)}>Allocate</button> },
          ]} />
        ))}
      </div>
    </div>
  )
}

function ListTab() {
  const [filters, setFilters] = useState({ status: 'Active', hostelId: null, q: '' })
  const { data, loading, error, reload } = useApi('/allocations' + query(filters))
  const notify = useToast()

  async function vacate(a) {
    try {
      await api.put(`/allocations/${a.id}/vacate`)
      notify(`${a.studentName} vacated ${a.roomNumber}`)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <div className="toolbar">
        <input className="input search" placeholder="Search student or room" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
        <select className="select" value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <option value="Active">Active</option><option value="Vacated">Vacated</option><option value="Cancelled">Cancelled</option><option value="">All</option>
        </select>
        <HostelSelect value={filters.hostelId} onChange={(v) => setFilters({ ...filters, hostelId: v })} />
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} columns={[
            { key: 'id', label: '#' },
            { key: 'studentName', label: 'Student', render: (a) => <>{a.studentName} <span className="muted mono small">{a.registrationNo}</span></> },
            { key: 'hostelName', label: 'Hostel' },
            { key: 'roomNumber', label: 'Room', render: (a) => `${a.roomNumber} · bed ${a.bedNumber}` },
            { key: 'allocationDate', label: 'Since', render: (a) => fmtDate(a.allocationDate) },
            { key: 'vacateDate', label: 'Vacated', render: (a) => fmtDate(a.vacateDate) },
            { key: 'allocatedBy', label: 'By' },
            { key: 'status', label: 'Status', render: (a) => <Badge value={a.status} /> },
            { key: 'actions', label: '', render: (a) => a.status === 'Active' && <button className="btn btn-danger btn-sm" onClick={() => vacate(a)}>Vacate</button> },
          ]} />
        )}
      </div>
    </>
  )
}
