import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, query } from '../../utils/format'
import { ComplaintDetail } from '../student/MyComplaints'

export default function MyJobs() {
  const [status, setStatus] = useState('')
  const { data, loading, error, reload } = useApi('/complaints/assigned' + query({ status }))
  const [updating, setUpdating] = useState(null)
  const [photo, setPhoto] = useState(null)
  const [viewing, setViewing] = useState(null)
  const notify = useToast()

  async function save(e) {
    e.preventDefault()
    try {
      if (photo) {
        const fd = new FormData()
        fd.append('file', photo)
        await api.post(`/complaints/${updating.id}/completion-image`, fd)
      }
      const { data: c } = await api.put(`/complaints/${updating.id}/status`, { status: updating.status, remarks: updating.remarks, scheduledDate: updating.scheduledDate || null })
      notify(c.status === 'Resolved' ? `Complaint #${c.id} resolved` : `Complaint #${c.id} updated`)
      setUpdating(null)
      setPhoto(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <PageHeader title="My jobs" subtitle="Complaints assigned to you" />
      <div className="toolbar">
        <select className="select" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All</option>{['Assigned', 'In Progress', 'Resolved'].map((s) => <option key={s}>{s}</option>)}
        </select>
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} empty="No jobs." columns={[
            { key: 'id', label: '#' },
            { key: 'room', label: 'Room', render: (c) => <>{c.roomNumber || '—'}<div className="muted small">{c.studentName}</div></> },
            { key: 'category', label: 'Category' },
            { key: 'description', label: 'Problem', render: (c) => <span style={{ display: 'inline-block', maxWidth: 320 }}>{c.description}</span> },
            { key: 'priority', label: 'Priority', render: (c) => <Badge value={c.priority} /> },
            { key: 'scheduledDate', label: 'Scheduled', render: (c) => fmtDate(c.scheduledDate) },
            { key: 'status', label: 'Status', render: (c) => <Badge value={c.status} /> },
            { key: 'actions', label: '', render: (c) => (
              <div className="row" style={{ justifyContent: 'flex-end' }}>
                <button className="btn btn-ghost btn-sm" onClick={() => setViewing(c)}>View</button>
                {c.status !== 'Resolved' && <button className="btn btn-primary btn-sm" onClick={() => setUpdating({ id: c.id, status: c.status === 'Assigned' ? 'In Progress' : 'Resolved', remarks: c.remarks || '', scheduledDate: c.scheduledDate || '' })}>Update</button>}
              </div>) },
          ]} />
        )}
      </div>
      {updating && (
        <Modal title={`Update complaint #${updating.id}`} onClose={() => setUpdating(null)}>
          <form onSubmit={save}>
            <Field label="Status"><select className="select" value={updating.status} onChange={(e) => setUpdating({ ...updating, status: e.target.value })}><option>In Progress</option><option>Resolved</option></select></Field>
            <Field label="Scheduled visit"><input className="input" type="date" value={updating.scheduledDate} onChange={(e) => setUpdating({ ...updating, scheduledDate: e.target.value })} /></Field>
            <Field label="Remarks"><textarea className="textarea" value={updating.remarks} onChange={(e) => setUpdating({ ...updating, remarks: e.target.value })} placeholder="What was done?" /></Field>
            <Field label="Completion photo (optional)"><input type="file" accept="image/jpeg,image/png,image/webp" onChange={(e) => setPhoto(e.target.files[0])} /></Field>
            <button className="btn btn-primary">Save</button>
          </form>
        </Modal>
      )}
      {viewing && <ComplaintDetail complaint={viewing} onClose={() => setViewing(null)} />}
    </>
  )
}
