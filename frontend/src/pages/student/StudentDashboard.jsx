import { Link } from 'react-router-dom'
import useApi from '../../hooks/useApi'
import { Badge, ErrorBox, Loading, PageHeader, Stat } from '../../components/ui'
import { fmtDate, fmtMoney } from '../../utils/format'

export default function StudentDashboard() {
  const { data, loading, error } = useApi('/dashboard/student')
  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />
  const s = data.student

  return (
    <>
      <PageHeader title={`Hello, ${s.name.split(' ')[0]}`} subtitle={`${s.registrationNo} · ${s.branch} · Year ${s.year}`} />

      <div className="grid grid-4" style={{ marginBottom: 16 }}>
        <Stat label="Open complaints" value={data.openComplaints} note="not yet resolved" />
        <Stat label="Pending leave" value={data.pendingLeaves} note="awaiting the warden" />
        <Stat label="Upcoming visitors" value={data.upcomingVisitors} />
        <Stat label="Fees outstanding" value={fmtMoney(data.outstandingFees)} note={Number(data.outstandingFees) > 0 ? 'please clear dues' : 'all dues cleared'} />
      </div>

      <div className="grid grid-2">
        <div className="card card-pad">
          <h2>My room</h2>
          {s.roomNumber ? (
            <dl className="kv">
              <dt>Hostel</dt><dd>{s.hostelName}</dd>
              <dt>Room</dt><dd>{s.roomNumber}</dd>
              <dt>Bed</dt><dd>{s.bedNumber}</dd>
            </dl>
          ) : (
            <p className="muted">No room has been allocated yet. The warden will allocate one soon.</p>
          )}
          <div className="row" style={{ marginTop: 16 }}>
            <Link className="btn btn-ghost btn-sm" to="/student/room">Room details</Link>
            <Link className="btn btn-ghost btn-sm" to="/student/complaints">Raise a complaint</Link>
            <Link className="btn btn-ghost btn-sm" to="/student/leaves">Apply for leave</Link>
          </div>
        </div>

        <div className="card card-pad">
          <h2>Recent updates</h2>
          {data.recentUpdates.length === 0 ? <p className="muted">No activity yet.</p> : (
            <ul className="list-plain">
              {data.recentUpdates.map((u) => (
                <li key={u.type + u.id} className="activity">
                  <span><span className="badge badge-neutral">{u.type}</span></span>
                  <span className="title" title={u.title}>{u.title}</span>
                  <Badge value={u.status} />
                  <span className="muted small date">{fmtDate(u.date)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </>
  )
}
