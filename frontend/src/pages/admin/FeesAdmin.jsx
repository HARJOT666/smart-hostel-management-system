import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Field, Loading, Modal, PageHeader, Stat } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, fmtMoney, query } from '../../utils/format'

export default function FeesAdmin() {
  const [filters, setFilters] = useState({ semester: '2026-27 Odd', outstandingOnly: true, q: '' })
  const { data, loading, error, reload } = useApi('/fees' + query(filters))
  const [paying, setPaying] = useState(null)
  const notify = useToast()

  async function pay(e) {
    e.preventDefault()
    try {
      await api.put(`/fees/${paying.id}/payment`, { amount: Number(paying.amount), paymentMode: paying.paymentMode, transactionRef: paying.transactionRef })
      notify('Payment recorded')
      setPaying(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  const rows = data || []
  const outstanding = rows.reduce((t, f) => t + Number(f.outstanding), 0)

  return (
    <>
      <PageHeader title="Fees" subtitle="Hostel and mess fee status; record payments received at the office" />
      <div className="toolbar">
        <input className="input search" placeholder="Search student" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
        <select className="select" value={filters.semester} onChange={(e) => setFilters({ ...filters, semester: e.target.value })}>
          <option value="2026-27 Odd">2026-27 Odd</option><option value="2025-26 Even">2025-26 Even</option><option value="">All semesters</option>
        </select>
        <label className="row small"><input type="checkbox" checked={filters.outstandingOnly} onChange={(e) => setFilters({ ...filters, outstandingOnly: e.target.checked })} /> Outstanding only</label>
      </div>
      <div className="grid grid-3" style={{ marginBottom: 16 }}>
        <Stat label="Fee items shown" value={rows.length} />
        <Stat label="Students" value={new Set(rows.map((f) => f.studentId)).size} />
        <Stat label="Outstanding" value={fmtMoney(outstanding)} />
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={rows} columns={[
            { key: 'studentName', label: 'Student', render: (f) => <>{f.studentName}<div className="muted small mono">{f.registrationNo}</div></> },
            { key: 'semester', label: 'Semester' },
            { key: 'feeType', label: 'Fee' },
            { key: 'amountDue', label: 'Due', render: (f) => fmtMoney(f.amountDue) },
            { key: 'amountPaid', label: 'Paid', render: (f) => fmtMoney(f.amountPaid) },
            { key: 'outstanding', label: 'Balance', render: (f) => fmtMoney(f.outstanding) },
            { key: 'dueDate', label: 'Due date', render: (f) => fmtDate(f.dueDate) },
            { key: 'status', label: 'Status', render: (f) => <Badge value={f.status} /> },
            { key: 'actions', label: '', render: (f) => Number(f.outstanding) > 0 && <button className="btn btn-ghost btn-sm" onClick={() => setPaying({ ...f, amount: f.outstanding, paymentMode: 'Online', transactionRef: '' })}>Record payment</button> },
          ]} />
        )}
      </div>
      {paying && (
        <Modal title="Record payment" onClose={() => setPaying(null)}>
          <p className="muted" style={{ marginTop: 0 }}>{paying.studentName} · {paying.feeType} · balance {fmtMoney(paying.outstanding)}</p>
          <form onSubmit={pay}>
            <div className="grid grid-2" style={{ gap: '0 14px' }}>
              <Field label="Amount (₹)"><input className="input" type="number" min="1" max={paying.outstanding} value={paying.amount} onChange={(e) => setPaying({ ...paying, amount: e.target.value })} /></Field>
              <Field label="Mode"><select className="select" value={paying.paymentMode} onChange={(e) => setPaying({ ...paying, paymentMode: e.target.value })}>{['Online', 'Card', 'Cash', 'DD'].map((m) => <option key={m}>{m}</option>)}</select></Field>
            </div>
            <Field label="Transaction reference"><input className="input" value={paying.transactionRef} onChange={(e) => setPaying({ ...paying, transactionRef: e.target.value })} /></Field>
            <button className="btn btn-primary">Save payment</button>
          </form>
        </Modal>
      )}
    </>
  )
}
