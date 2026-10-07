import { useState } from 'react'
import useApi from '../../hooks/useApi'
import { DataTable, ErrorBox, Loading, PageHeader } from '../../components/ui'

const CURSOR_REPORT = 'hostel-cursor-report'

/** Reports computed live in Oracle (SQL from 09_queries.sql and the cursor procedure). */
export default function ReportsPage() {
  const { data: reports } = useApi('/reports')
  const [key, setKey] = useState('hostel-occupancy')
  const isCursor = key === CURSOR_REPORT
  const { data, loading, error } = useApi(`/reports/${key}`)

  return (
    <>
      <PageHeader title="Reports" subtitle="Generated directly from the Oracle database" />
      <div className="toolbar">
        <select className="select" value={key} onChange={(e) => setKey(e.target.value)}>
          {reports && Object.entries(reports).map(([k, title]) => <option key={k} value={k}>{title}</option>)}
          <option value={CURSOR_REPORT}>Hostel report (PL/SQL cursor procedure)</option>
        </select>
        <button className="btn btn-ghost no-print" onClick={() => window.print()}>Print</button>
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading || !data ? <Loading /> : isCursor ? (
          <pre className="mono" style={{ margin: 0, padding: 20, whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>{data.join('\n')}</pre>
        ) : (
          <>
            <div className="card-pad" style={{ paddingBottom: 0 }}><h2>{data.title}</h2></div>
            <DataTable pageSize={25} rows={data.rows.map((r, i) => ({ ...r, id: i }))}
              columns={data.columns.map((c) => ({ key: c, label: c.replaceAll('_', ' ').toLowerCase(), render: (r) => r[c] ?? '—' }))} />
          </>
        )}
      </div>
    </>
  )
}
