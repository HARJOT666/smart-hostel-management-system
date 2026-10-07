import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Loading, PageHeader, Stat } from '../../components/ui'
import { fmtDate, fmtMoney } from '../../utils/format'

export default function MyFees() {
  const { data, loading, error } = useApi('/fees/my')
  if (loading) return <Loading />
  if (error) return <ErrorBox message={error} />
  const sum = (key) => data.reduce((t, f) => t + Number(f[key]), 0)

  return (
    <>
      <PageHeader title="Fees" subtitle="Fee status and payment history (online payment is not part of this version)" />
      <div className="grid grid-3" style={{ marginBottom: 16 }}>
        <Stat label="Total billed" value={fmtMoney(sum('amountDue'))} />
        <Stat label="Paid" value={fmtMoney(sum('amountPaid'))} />
        <Stat label="Outstanding" value={fmtMoney(sum('outstanding'))} note={sum('outstanding') > 0 ? 'pay at the hostel office' : 'no dues'} />
      </div>
      <div className="card">
        <DataTable rows={data} columns={[
          { key: 'semester', label: 'Semester' },
          { key: 'feeType', label: 'Fee' },
          { key: 'amountDue', label: 'Amount', render: (r) => fmtMoney(r.amountDue) },
          { key: 'amountPaid', label: 'Paid', render: (r) => fmtMoney(r.amountPaid) },
          { key: 'dueDate', label: 'Due', render: (r) => fmtDate(r.dueDate) },
          { key: 'paidDate', label: 'Paid on', render: (r) => fmtDate(r.paidDate) },
          { key: 'paymentMode', label: 'Mode' },
          { key: 'transactionRef', label: 'Reference', render: (r) => <span className="mono">{r.transactionRef || '—'}</span> },
          { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
        ]} />
      </div>
    </>
  )
}
