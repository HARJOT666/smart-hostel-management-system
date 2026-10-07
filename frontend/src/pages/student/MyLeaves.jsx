import { useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errorMessage } from '../../api/client'
import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, todayIso } from '../../utils/format'

export default function MyLeaves() {
  const { data, loading, error, reload } = useApi('/leaves/my')
  const [form, setForm] = useState(null)
  const notify = useToast()

  async function submit(e) {
    e.preventDefault()
    try {
      await api.post('/leaves', form)
      notify('Leave request sent to the warden')
      setForm(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  async function withdraw(id) {
    try {
      await api.delete(`/leaves/${id}`)
      notify('Leave request withdrawn')
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <PageHeader title="Leave" subtitle="Apply for leave and download the approval letter once approved">
        <button className="btn btn-primary" onClick={() => setForm({ startDate: todayIso(), endDate: todayIso(), reason: '', destination: '' })}>Apply for leave</button>
      </PageHeader>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} empty="No leave requests yet." columns={[
            { key: 'startDate', label: 'From', render: (r) => fmtDate(r.startDate) },
            { key: 'endDate', label: 'To', render: (r) => fmtDate(r.endDate) },
            { key: 'days', label: 'Days' },
            { key: 'reason', label: 'Reason' },
            { key: 'destination', label: 'Destination' },
            { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
            { key: 'approvedBy', label: 'Decided by' },
            { key: 'actions', label: '', render: (r) => (
              r.status === 'Approved' ? <Link className="btn btn-ghost btn-sm" to={`/student/leaves/${r.id}/letter`}>Approval letter</Link>
                : r.status === 'Pending' ? <button className="btn btn-danger btn-sm" onClick={() => withdraw(r.id)}>Withdraw</button> : null) },
          ]} />
        )}
      </div>
      {form && (
        <Modal title="Apply for leave" onClose={() => setForm(null)}>
          <form onSubmit={submit}>
            <div className="grid grid-2" style={{ gap: '0 14px' }}>
              <Field label="From"><input className="input" type="date" min={todayIso()} value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></Field>
              <Field label="To"><input className="input" type="date" min={form.startDate} value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></Field>
            </div>
            <Field label="Reason"><input className="input" value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} required /></Field>
            <Field label="Destination"><input className="input" value={form.destination} onChange={(e) => setForm({ ...form, destination: e.target.value })} /></Field>
            <button className="btn btn-primary">Submit request</button>
          </form>
        </Modal>
      )}
    </>
  )
}
