import { useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../hooks/useApi'
import HostelSelect from '../../components/HostelSelect'
import { DataTable, ErrorBox, Loading, PageHeader } from '../../components/ui'
import { query } from '../../utils/format'

// Public national helplines (India). Hostel staff contacts come from the database.
const NATIONAL = [['Emergency (all services)', '112'], ['Ambulance', '108'], ['Fire', '101'], ['Women helpline', '1091']]

export default function ContactsPage() {
  const { user } = useAuth()
  const [hostelId, setHostelId] = useState(user.role === 'STUDENT' ? null : 1)
  const { data, loading, error } = useApi('/emergency-contacts' + query({ hostelId }))

  return (
    <>
      <PageHeader title="Emergency contacts" subtitle="Wardens and maintenance staff on duty for the hostel">
        {user.role !== 'STUDENT' && <HostelSelect value={hostelId} onChange={setHostelId} allowAll={false} />}
      </PageHeader>
      <div className="grid grid-4" style={{ marginBottom: 16 }}>
        {NATIONAL.map(([label, number]) => (
          <div key={number} className="card stat">
            <div className="stat-label">{label}</div>
            <div className="stat-value">{number}</div>
          </div>
        ))}
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={(data || []).map((c, i) => ({ ...c, id: i }))} columns={[
            { key: 'name', label: 'Name' },
            { key: 'role', label: 'Role', render: (c) => c.role + (c.specialization ? ` · ${c.specialization}` : '') },
            { key: 'phone', label: 'Phone', render: (c) => <span className="mono">{c.phone}</span> },
            { key: 'email', label: 'E-mail' },
            { key: 'hostelName', label: 'Hostel' },
          ]} />
        )}
      </div>
    </>
  )
}
