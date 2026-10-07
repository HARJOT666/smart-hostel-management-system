import { useParams } from 'react-router-dom'
import useApi from '../../hooks/useApi'
import { ErrorBox, Loading } from '../../components/ui'
import { fmtDate } from '../../utils/format'

/** Printable leave approval letter (use the browser's Print -> Save as PDF). */
export default function LeaveLetter() {
  const { id } = useParams()
  const { data: l, loading, error } = useApi(`/leaves/${id}`)
  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />
  if (l.status !== 'Approved') return <ErrorBox message="This leave request has not been approved." />

  return (
    <div className="card" style={{ maxWidth: 720, padding: '48px 56px', fontSize: 15, lineHeight: 1.7 }}>
      <div className="row no-print" style={{ justifyContent: 'flex-end', marginBottom: 16 }}>
        <button className="btn btn-dark btn-sm" onClick={() => window.print()}>Print / Save as PDF</button>
      </div>
      <div style={{ borderBottom: '1px solid var(--line)', paddingBottom: 16, marginBottom: 24 }}>
        <h1 style={{ fontSize: 26 }}>Smart Hostel Management System</h1>
        <div className="muted">Leave approval letter · Ref. LV-{l.id}</div>
      </div>
      <p>Date of issue: {fmtDate(new Date())}</p>
      <p>This is to certify that <strong>{l.studentName}</strong> (Registration No. <strong>{l.registrationNo}</strong>) has been
        granted leave from the hostel from <strong>{fmtDate(l.startDate)}</strong> to <strong>{fmtDate(l.endDate)}</strong> ({l.days} day{l.days > 1 ? 's' : ''}).</p>
      <dl className="kv" style={{ margin: '20px 0' }}>
        <dt>Reason</dt><dd>{l.reason}</dd>
        <dt>Destination</dt><dd>{l.destination || '—'}</dd>
        <dt>Applied on</dt><dd>{fmtDate(l.appliedDate)}</dd>
        <dt>Approved by</dt><dd>{l.approvedBy}</dd>
      </dl>
      <p>The student must return to the hostel on or before the end date and report to the warden's office.</p>
      <p style={{ marginTop: 48 }}>{l.approvedBy}<br /><span className="muted">Warden</span></p>
    </div>
  )
}
