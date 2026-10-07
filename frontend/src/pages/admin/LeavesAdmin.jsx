import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Loading, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, query } from '../../utils/format'

export default function LeavesAdmin() {
  const [filters, setFilters] = useState({ status: 'Pending', q: '' })
  const { data, loading, error, reload } = useApi('/leaves' + query(filters))
  const notify = useToast()

  // calls sp_decide_leave in Oracle
  async function decide(l, decision) {
    try {
      await api.put(`/leaves/${l.id}/decision`, { decision })
      notify(`Leave of ${l.studentName} ${decision.toLowerCase()}`)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <PageHeader title="Leave approvals" subtitle="Approve or reject students' leave requests" />
      <div className="toolbar">
        <input className="input search" placeholder="Search student" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
        <select className="select" value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <option value="Pending">Pending</option><option value="Approved">Approved</option><option value="Rejected">Rejected</option><option value="">All</option>
        </select>
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} empty="No leave requests here." columns={[
            { key: 'studentName', label: 'Student', render: (l) => <>{l.studentName}<div className="muted small mono">{l.registrationNo}</div></> },
            { key: 'startDate', label: 'From', render: (l) => fmtDate(l.startDate) },
            { key: 'endDate', label: 'To', render: (l) => fmtDate(l.endDate) },
            { key: 'days', label: 'Days' },
            { key: 'reason', label: 'Reason' },
            { key: 'destination', label: 'Destination' },
            { key: 'appliedDate', label: 'Applied', render: (l) => fmtDate(l.appliedDate) },
            { key: 'status', label: 'Status', render: (l) => <><Badge value={l.status} />{l.approvedBy && <div className="muted small">{l.approvedBy}</div>}</> },
            { key: 'actions', label: '', render: (l) => l.status === 'Pending' && (
              <div className="row" style={{ justifyContent: 'flex-end' }}>
                <button className="btn btn-primary btn-sm" onClick={() => decide(l, 'Approved')}>Approve</button>
                <button className="btn btn-danger btn-sm" onClick={() => decide(l, 'Rejected')}>Reject</button>
              </div>) },
          ]} />
        )}
      </div>
    </>
  )
}
