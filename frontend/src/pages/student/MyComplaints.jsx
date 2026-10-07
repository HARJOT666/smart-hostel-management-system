import { useState } from 'react'
import api, { errorMessage, fileUrl } from '../../api/client'
import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { COMPLAINT_CATEGORIES, fmtDate, fmtDateTime } from '../../utils/format'

export default function MyComplaints() {
  const { data, loading, error, reload } = useApi('/complaints/my')
  const [form, setForm] = useState(null)
  const [photo, setPhoto] = useState(null)
  const [viewing, setViewing] = useState(null)
  const [busy, setBusy] = useState(false)
  const notify = useToast()

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    try {
      const { data: created } = await api.post('/complaints', form)
      if (photo) {
        const body = new FormData()
        body.append('file', photo)
        await api.post(`/complaints/${created.id}/image`, body)
      }
      notify(`Complaint #${created.id} registered`)
      setForm(null)
      setPhoto(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageHeader title="Complaints" subtitle="Report a maintenance problem and track it until it is fixed">
        <button className="btn btn-primary" onClick={() => setForm({ category: 'Electrical', priority: 'Medium', description: '' })}>New complaint</button>
      </PageHeader>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} empty="You have not raised any complaints." columns={[
            { key: 'id', label: '#' },
            { key: 'category', label: 'Category' },
            { key: 'description', label: 'Description', render: (r) => <span style={{ display: 'inline-block', maxWidth: 360 }}>{r.description}</span> },
            { key: 'priority', label: 'Priority', render: (r) => <Badge value={r.priority} /> },
            { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
            { key: 'createdDate', label: 'Raised', render: (r) => fmtDate(r.createdDate) },
            { key: 'actions', label: '', render: (r) => <button className="btn btn-ghost btn-sm" onClick={() => setViewing(r)}>Details</button> },
          ]} />
        )}
      </div>

      {form && (
        <Modal title="New complaint" onClose={() => setForm(null)}>
          <form onSubmit={submit}>
            <div className="grid grid-2" style={{ gap: '0 14px' }}>
              <Field label="Category">
                <select className="select" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                  {COMPLAINT_CATEGORIES.map((c) => <option key={c}>{c}</option>)}
                </select>
              </Field>
              <Field label="Priority">
                <select className="select" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
                  <option>Low</option><option>Medium</option><option>High</option>
                </select>
              </Field>
            </div>
            <Field label="Describe the problem" hint="At least 10 characters">
              <textarea className="textarea" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required />
            </Field>
            <Field label="Photo (optional)" hint="JPG, PNG or WEBP, up to 5 MB">
              <input type="file" accept="image/jpeg,image/png,image/webp" onChange={(e) => setPhoto(e.target.files[0])} />
            </Field>
            <button className="btn btn-primary" disabled={busy}>{busy ? 'Submitting…' : 'Submit complaint'}</button>
          </form>
        </Modal>
      )}

      {viewing && <ComplaintDetail complaint={viewing} onClose={() => setViewing(null)} />}
    </>
  )
}

export function ComplaintDetail({ complaint: c, onClose }) {
  return (
    <Modal title={`Complaint #${c.id}`} onClose={onClose} wide>
      <dl className="kv">
        <dt>Category</dt><dd>{c.category} <Badge value={c.priority} /></dd>
        <dt>Status</dt><dd><Badge value={c.status} /></dd>
        <dt>Student</dt><dd>{c.studentName} ({c.registrationNo})</dd>
        <dt>Room</dt><dd>{c.roomNumber ? `${c.roomNumber}, ${c.hostelName}` : '—'}</dd>
        <dt>Description</dt><dd>{c.description}</dd>
        <dt>Raised</dt><dd>{fmtDateTime(c.createdDate)}</dd>
        <dt>Assigned to</dt><dd>{c.assignedToName || 'Not assigned yet'}</dd>
        <dt>Scheduled</dt><dd>{fmtDate(c.scheduledDate)}</dd>
        <dt>Resolved</dt><dd>{fmtDateTime(c.resolvedDate)}</dd>
        <dt>Remarks</dt><dd>{c.remarks || '—'}</dd>
      </dl>
      {(c.imageUrl || c.completionImageUrl) && (
        <div className="row" style={{ marginTop: 16, alignItems: 'flex-start' }}>
          {c.imageUrl && <figure style={{ margin: 0 }}><img src={fileUrl(c.imageUrl)} alt="Reported problem" style={{ maxWidth: 300, borderRadius: 8 }} /><figcaption className="small muted">Reported by student</figcaption></figure>}
          {c.completionImageUrl && <figure style={{ margin: 0 }}><img src={fileUrl(c.completionImageUrl)} alt="Completed work" style={{ maxWidth: 300, borderRadius: 8 }} /><figcaption className="small muted">Work completed</figcaption></figure>}
        </div>
      )}
    </Modal>
  )
}
