import { Link } from 'react-router-dom'
import useApi from '../../hooks/useApi'
import { Badge, ErrorBox, Loading, PageHeader, Stat } from '../../components/ui'
import { fmtDate } from '../../utils/format'

export default function MaintDashboard() {
  const { data: d, loading, error } = useApi('/dashboard/maintenance')
  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />
  return (
    <>
      <PageHeader title={`Good day, ${d.name.split(' ')[0]}`} subtitle={`${d.specialization} · ${d.hostelName}`} />
      <div className="grid grid-4" style={{ marginBottom: 16 }}>
        <Stat label="New jobs" value={d.assigned} note="assigned, not started" />
        <Stat label="In progress" value={d.inProgress} />
        <Stat label="Scheduled today" value={d.scheduledToday} />
        <Stat label="Resolved this month" value={d.resolvedThisMonth} />
      </div>
      <div className="card card-pad">
        <div className="row"><h2 style={{ margin: 0 }}>Next jobs</h2><span className="spacer" /><Link className="btn btn-ghost btn-sm" to="/maintenance/jobs">All jobs</Link></div>
        {d.nextJobs.length === 0 ? <p className="muted">No open jobs. Well done.</p> : (
          <ul className="list-plain">
            {d.nextJobs.map((c) => (
              <li key={c.id} className="row">
                <Badge value={c.status} />
                <strong>#{c.id}</strong>
                <span>{c.category} — {c.description}</span>
                <span className="spacer" />
                <span className="muted small">{c.roomNumber} · {c.scheduledDate ? `scheduled ${fmtDate(c.scheduledDate)}` : 'not scheduled'}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
  )
}
