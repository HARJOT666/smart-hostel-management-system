import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Loading, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, fmtDateTime, query } from '../../utils/format'

export default function VisitorsAdmin() {
  const [filters, setFilters] = useState({ status: 'Pending', visitDate: '', q: '' })
  const { data, loading, error, reload } = useApi('/visitors' + query(filters))
  const [qr, setQr] = useState('')
  const [scan, setScan] = useState(null)
  const notify = useToast()

  async function decide(v, decision) {
    try {
      const { data: updated } = await api.put(`/visitors/${v.id}/decision`, { decision })
      notify(decision === 'Approved' ? `Approved — QR ${updated.qrCode}` : 'Pass rejected')
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  async function doScan(e) {
    e.preventDefault()
    try {
      const { data: v } = await api.post('/visitors/scan', { qrCode: qr })
      setScan({ ok: true, v })
      setQr('')
      reload()
    } catch (err) {
      setScan({ ok: false, message: errorMessage(err) })
    }
  }

  return (
    <>
      <PageHeader title="Visitors" subtitle="Approve visitor passes and record entry/exit at the gate" />
      <div className="card card-pad" style={{ marginBottom: 16 }}>
        <h2>Gate check</h2>
        <form className="row" onSubmit={doScan}>
          <input className="input mono" style={{ maxWidth: 340 }} placeholder="Scan or type QR code, e.g. SHMS-VP-12-AB12CD34" value={qr} onChange={(e) => setQr(e.target.value)} />
          <button className="btn btn-dark" disabled={!qr}>Check in / out</button>
        </form>
        {scan && (scan.ok
          ? <div className="alert alert-success" style={{ marginTop: 12, marginBottom: 0 }}>{scan.v.visitorName} (visiting {scan.v.studentName}) — {scan.v.exitTime ? `exit recorded at ${fmtDateTime(scan.v.exitTime)}. Pass completed.` : `entry recorded at ${fmtDateTime(scan.v.entryTime)}.`}</div>
          : <div className="alert alert-error" style={{ marginTop: 12, marginBottom: 0 }}>{scan.message}</div>)}
      </div>
      <div className="toolbar">
        <input className="input search" placeholder="Search student or visitor" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
        <select className="select" value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <option value="">Any status</option>{['Pending', 'Approved', 'Completed', 'Rejected'].map((s) => <option key={s}>{s}</option>)}
        </select>
        <input className="input" type="date" value={filters.visitDate} onChange={(e) => setFilters({ ...filters, visitDate: e.target.value })} />
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} empty="No visitor passes here." columns={[
            { key: 'visitDate', label: 'Visit', render: (v) => fmtDate(v.visitDate) },
            { key: 'visitorName', label: 'Visitor', render: (v) => <>{v.visitorName}<div className="muted small">{v.relationToStudent} · <span className="mono">{v.visitorPhone}</span></div></> },
            { key: 'studentName', label: 'Student', render: (v) => <>{v.studentName}<div className="muted small mono">{v.registrationNo}</div></> },
            { key: 'purpose', label: 'Purpose' },
            { key: 'status', label: 'Status', render: (v) => <Badge value={v.status} /> },
            { key: 'qrCode', label: 'QR code', render: (v) => <span className="mono small">{v.qrCode || '—'}</span> },
            { key: 'actions', label: '', render: (v) => v.status === 'Pending' && (
              <div className="row" style={{ justifyContent: 'flex-end' }}>
                <button className="btn btn-primary btn-sm" onClick={() => decide(v, 'Approved')}>Approve</button>
                <button className="btn btn-danger btn-sm" onClick={() => decide(v, 'Rejected')}>Reject</button>
              </div>) },
          ]} />
        )}
      </div>
    </>
  )
}
