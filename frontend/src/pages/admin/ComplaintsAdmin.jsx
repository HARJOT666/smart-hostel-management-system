import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import useApi from '../../hooks/useApi'
import HostelSelect from '../../components/HostelSelect'
import { Badge, DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { COMPLAINT_CATEGORIES, fmtDate, query, todayIso } from '../../utils/format'
import { ComplaintDetail } from '../student/MyComplaints'

export default function ComplaintsAdmin() {
  const [filters, setFilters] = useState({ status: 'Open', category: '', q: '', hostelId: null })
  const { data, loading, error, reload } = useApi('/complaints' + query(filters))
  const [assigning, setAssigning] = useState(null)
  const [viewing, setViewing] = useState(null)
  const notify = useToast()

  async function autoAssign() {
    try {
      const { data: r } = await api.post('/complaints/auto-assign')
      notify(`Auto-assign: ${r.assigned} assigned, ${r.skipped} skipped`)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <PageHeader title="Complaints" subtitle="Assign complaints to maintenance staff and follow their progress">
        <button className="btn btn-dark" onClick={autoAssign} title="Runs the PL/SQL cursor procedure sp_auto_assign_open_complaints">Auto-assign open complaints</button>
      </PageHeader>
      <div className="toolbar">
        <input className="input search" placeholder="Search student or description" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
        <select className="select" value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <option value="">Any status</option>{['Open', 'Assigned', 'In Progress', 'Resolved'].map((s) => <option key={s}>{s}</option>)}
        </select>
        <select className="select" value={filters.category} onChange={(e) => setFilters({ ...filters, category: e.target.value })}>
          <option value="">All categories</option>{COMPLAINT_CATEGORIES.map((c) => <option key={c}>{c}</option>)}
        </select>
        <HostelSelect value={filters.hostelId} onChange={(v) => setFilters({ ...filters, hostelId: v })} />
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} columns={[
            { key: 'id', label: '#' },
            { key: 'category', label: 'Category' },
            { key: 'description', label: 'Description', render: (c) => <span style={{ display: 'inline-block', maxWidth: 320 }}>{c.description}</span> },
            { key: 'room', label: 'Room', render: (c) => c.roomNumber ? <>{c.roomNumber}<div className="muted small">{c.hostelName}</div></> : '—' },
            { key: 'priority', label: 'Priority', render: (c) => <Badge value={c.priority} /> },
            { key: 'status', label: 'Status', render: (c) => <Badge value={c.status} /> },
            { key: 'assignedToName', label: 'Assigned to' },
            { key: 'createdDate', label: 'Raised', render: (c) => fmtDate(c.createdDate) },
            { key: 'actions', label: '', render: (c) => (
              <div className="row" style={{ justifyContent: 'flex-end' }}>
                <button className="btn btn-ghost btn-sm" onClick={() => setViewing(c)}>View</button>
                {(c.status === 'Open' || c.status === 'Assigned') && <button className="btn btn-primary btn-sm" onClick={() => setAssigning(c)}>{c.status === 'Open' ? 'Assign' : 'Reassign'}</button>}
              </div>) },
          ]} />
        )}
      </div>
      {assigning && <AssignModal complaint={assigning} onClose={() => setAssigning(null)} onDone={() => { setAssigning(null); reload() }} />}
      {viewing && <ComplaintDetail complaint={viewing} onClose={() => setViewing(null)} />}
    </>
  )
}

function AssignModal({ complaint, onClose, onDone }) {
  // eligible staff with their current workload (Oracle function fn_staff_open_workload)
  const { data: options, loading } = useApi(`/staff/maintenance-options?complaintId=${complaint.id}`)
  const [staffId, setStaffId] = useState('')
  const [date, setDate] = useState(todayIso())
  const [error, setError] = useState('')
  const notify = useToast()

  async function assign(e) {
    e.preventDefault()
    try {
      await api.put(`/complaints/${complaint.id}/assign`, { staffId: Number(staffId || options[0].id), scheduledDate: date })
      notify(`Complaint #${complaint.id} assigned`)
      onDone()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <Modal title={`Assign complaint #${complaint.id}`} onClose={onClose}>
      <p className="muted" style={{ marginTop: 0 }}>{complaint.category} · {complaint.roomNumber} {complaint.hostelName && `(${complaint.hostelName})`}</p>
      <ErrorBox message={error} />
      {loading ? <Loading /> : !options?.length ? <p>No matching maintenance staff in this hostel.</p> : (
        <form onSubmit={assign}>
          <Field label="Maintenance staff" hint="Least busy first — open jobs in brackets">
            <select className="select" value={staffId} onChange={(e) => setStaffId(e.target.value)}>
              {options.map((o) => <option key={o.id} value={o.id}>{o.name} — {o.specialization} [{o.workload} open]</option>)}
            </select>
          </Field>
          <Field label="Scheduled visit"><input className="input" type="date" min={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} /></Field>
          <button className="btn btn-primary">Assign</button>
        </form>
      )}
    </Modal>
  )
}
