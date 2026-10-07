import { useState } from 'react'
import api, { errorMessage, fileUrl } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, query } from '../../utils/format'

export default function LostFoundPage() {
  const { user } = useAuth()
  const isStudent = user.role === 'STUDENT'
  const [filters, setFilters] = useState({ type: '', status: 'Open' })
  const { data, loading, error, reload } = useApi('/lost-found' + query(filters))
  const [form, setForm] = useState(null)
  const [photo, setPhoto] = useState(null)
  const notify = useToast()

  async function save(e) {
    e.preventDefault()
    try {
      const { data: saved } = await api.post('/lost-found', form)
      if (photo) {
        const fd = new FormData()
        fd.append('file', photo)
        await api.post(`/lost-found/${saved.id}/image`, fd)
      }
      notify('Report posted')
      setForm(null)
      setPhoto(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  async function act(id, action) {
    try {
      await api.put(`/lost-found/${id}/${action}`)
      notify(action === 'claim' ? 'Claim recorded — contact the reporter' : 'Report closed')
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <PageHeader title="Lost & found" subtitle="Report lost items or things you have found in the hostel">
        {isStudent && <button className="btn btn-primary" onClick={() => setForm({ reportType: 'Lost', itemName: '', description: '', location: '' })}>Report an item</button>}
      </PageHeader>
      <div className="toolbar">
        <select className="select" value={filters.type} onChange={(e) => setFilters({ ...filters, type: e.target.value })}>
          <option value="">Lost and found</option><option>Lost</option><option>Found</option>
        </select>
        <select className="select" value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <option value="Open">Open</option><option value="Claimed">Claimed</option><option value="Closed">Closed</option><option value="">Any status</option>
        </select>
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} empty="Nothing reported." columns={[
            { key: 'image', label: '', render: (r) => r.imageUrl ? <img className="thumb" src={fileUrl(r.imageUrl)} alt="" /> : null },
            { key: 'reportType', label: 'Type', render: (r) => <Badge value={r.reportType} /> },
            { key: 'itemName', label: 'Item', render: (r) => <><div style={{ fontWeight: 500 }}>{r.itemName}</div><div className="muted small">{r.description}</div></> },
            { key: 'location', label: 'Where' },
            { key: 'reportDate', label: 'Reported', render: (r) => fmtDate(r.reportDate) },
            { key: 'reportedBy', label: 'Reported by', render: (r) => <><div>{r.reportedBy}</div><div className="muted small mono">{r.reporterPhone}</div></> },
            { key: 'status', label: 'Status', render: (r) => <><Badge value={r.status} />{r.claimedBy && <div className="muted small">by {r.claimedBy}</div>}</> },
            { key: 'actions', label: '', render: (r) => {
              const own = isStudent && r.reportedById === user.id
              if (r.status === 'Closed') return null
              if (own || !isStudent) return <button className="btn btn-ghost btn-sm" onClick={() => act(r.id, 'close')}>Close</button>
              if (r.status === 'Open') return <button className="btn btn-ghost btn-sm" onClick={() => act(r.id, 'claim')}>{r.reportType === 'Found' ? 'This is mine' : 'I found it'}</button>
              return null
            } },
          ]} />
        )}
      </div>
      {form && (
        <Modal title="Report an item" onClose={() => setForm(null)}>
          <form onSubmit={save}>
            <Field label="Type"><select className="select" value={form.reportType} onChange={(e) => setForm({ ...form, reportType: e.target.value })}><option>Lost</option><option>Found</option></select></Field>
            <Field label="Item"><input className="input" value={form.itemName} onChange={(e) => setForm({ ...form, itemName: e.target.value })} required /></Field>
            <Field label="Description"><textarea className="textarea" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
            <Field label="Location"><input className="input" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} placeholder="e.g. Mess hall" /></Field>
            <Field label="Photo (optional)"><input type="file" accept="image/jpeg,image/png,image/webp" onChange={(e) => setPhoto(e.target.files[0])} /></Field>
            <button className="btn btn-primary">Post report</button>
          </form>
        </Modal>
      )}
    </>
  )
}
