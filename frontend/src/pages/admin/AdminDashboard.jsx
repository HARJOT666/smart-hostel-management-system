import { Link } from 'react-router-dom'
import useApi from '../../hooks/useApi'
import HBarChart from '../../components/HBarChart'
import { ErrorBox, Loading, PageHeader, Stat } from '../../components/ui'
import { fmtDateTime, fmtMoney } from '../../utils/format'

export default function AdminDashboard() {
  const { data: d, loading, error, reload } = useApi('/dashboard/admin')
  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />

  const occupancy = d.hostels.map((h) => ({ name: h.hostelName, value: h.occupancyPct, detail: `${h.occupied}/${h.beds} beds` }))
  const categories = d.complaintsLast30Days.map((c) => ({ name: c.name, value: c.count }))

  return (
    <>
      <PageHeader title="Dashboard" subtitle={`Statistics as of ${fmtDateTime(d.generatedAt)} (refreshed at most every 60 seconds)`}>
        <button className="btn btn-ghost" onClick={reload}>Refresh</button>
      </PageHeader>

      <div className="grid grid-4" style={{ marginBottom: 16 }}>
        <Stat label="Bed occupancy" value={`${d.occupancyPct}%`} note={`${d.occupiedBeds} of ${d.totalBeds} beds`} />
        <Stat label="Residents" value={d.allocatedStudents} note={`${d.waitingStudents} students waiting for a room`} />
        <Stat label="Open complaints" value={d.openComplaints} note={`${d.activeComplaints} being worked on`} />
        <Stat label="Fees outstanding" value={fmtMoney(d.outstandingFees)} />
      </div>

      <div className="grid grid-3" style={{ marginBottom: 16 }}>
        <Link to="/admin/leaves" className="card stat" style={{ textDecoration: 'none', color: 'inherit' }}>
          <div className="stat-label">Leave requests pending</div><div className="stat-value">{d.pendingLeaves}</div><div className="stat-note">Review →</div>
        </Link>
        <Link to="/admin/visitors" className="card stat" style={{ textDecoration: 'none', color: 'inherit' }}>
          <div className="stat-label">Visitor passes pending</div><div className="stat-value">{d.pendingVisitors}</div><div className="stat-note">Review →</div>
        </Link>
        <div className="card stat">
          <div className="stat-label">Visitors expected today</div><div className="stat-value">{d.visitorsToday}</div>
        </div>
      </div>

      <div className="grid grid-2">
        <div className="card card-pad">
          <h2>Occupancy by hostel (%)</h2>
          <HBarChart data={occupancy} unit="%" max={100} />
        </div>
        <div className="card card-pad">
          <h2>Complaints in the last 30 days</h2>
          <HBarChart data={categories} />
          <div className="row small muted" style={{ marginTop: 8 }}>
            {d.complaintsByStatus.map((s) => <span key={s.name}>{s.name}: <strong style={{ color: 'var(--ink)' }}>{s.count}</strong></span>)}
          </div>
        </div>
      </div>
    </>
  )
}
