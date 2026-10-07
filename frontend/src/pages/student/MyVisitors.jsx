import { useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import api, { errorMessage } from '../../api/client'
import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, fmtDateTime, todayIso } from '../../utils/format'

export default function MyVisitors() {
  const { data, loading, error, reload } = useApi('/visitors/my')
  const [form, setForm] = useState(null)
  const [qr, setQr] = useState(null)
  const notify = useToast()

  async function submit(e) {
    e.preventDefault()
    try {
      await api.post('/visitors', form)
      notify('Visitor pass requested')
      setForm(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  async function withdraw(id) {
    try {
      await api.delete(`/visitors/${id}`)
      notify('Request withdrawn')
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <PageHeader title="Visitor passes" subtitle="Request a pass; once approved, your visitor shows the QR code at the gate">
        <button className="btn btn-primary" onClick={() => setForm({ visitorName: '', visitorPhone: '', relationToStudent: 'Father', purpose: '', visitDate: todayIso() })}>Request a pass</button>
      </PageHeader>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} empty="No visitor passes yet." columns={[
            { key: 'visitDate', label: 'Visit date', render: (r) => fmtDate(r.visitDate) },
            { key: 'visitorName', label: 'Visitor' },
            { key: 'relationToStudent', label: 'Relation' },
            { key: 'purpose', label: 'Purpose' },
            { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
            { key: 'entryTime', label: 'Entry / exit', render: (r) => r.entryTime ? `${fmtDateTime(r.entryTime)} – ${r.exitTime ? fmtDateTime(r.exitTime) : '…'}` : '—' },
            { key: 'actions', label: '', render: (r) => (
              r.status === 'Approved' ? <button className="btn btn-ghost btn-sm" onClick={() => setQr(r)}>Show QR</button>
                : r.status === 'Pending' ? <button className="btn btn-danger btn-sm" onClick={() => withdraw(r.id)}>Withdraw</button> : null) },
          ]} />
        )}
      </div>

      {form && (
        <Modal title="Request a visitor pass" onClose={() => setForm(null)}>
          <form onSubmit={submit}>
            <Field label="Visitor name"><input className="input" value={form.visitorName} onChange={(e) => setForm({ ...form, visitorName: e.target.value })} required /></Field>
            <div className="grid grid-2" style={{ gap: '0 14px' }}>
              <Field label="Visitor phone"><input className="input" value={form.visitorPhone} onChange={(e) => setForm({ ...form, visitorPhone: e.target.value })} placeholder="10 digits" required /></Field>
              <Field label="Relation">
                <select className="select" value={form.relationToStudent} onChange={(e) => setForm({ ...form, relationToStudent: e.target.value })}>
                  {['Father', 'Mother', 'Brother', 'Sister', 'Uncle', 'Aunt', 'Guardian', 'Cousin', 'Friend'].map((r) => <option key={r}>{r}</option>)}
                </select>
              </Field>
            </div>
            <Field label="Visit date"><input className="input" type="date" min={todayIso()} value={form.visitDate} onChange={(e) => setForm({ ...form, visitDate: e.target.value })} /></Field>
            <Field label="Purpose"><input className="input" value={form.purpose} onChange={(e) => setForm({ ...form, purpose: e.target.value })} /></Field>
            <button className="btn btn-primary">Send request</button>
          </form>
        </Modal>
      )}

      {qr && (
        <Modal title="Visitor pass" onClose={() => setQr(null)}>
          <div style={{ textAlign: 'center' }}>
            <QRCodeSVG value={qr.qrCode} size={200} fgColor="#2b2420" bgColor="#fbf8f3" />
            <div className="mono" style={{ marginTop: 10 }}>{qr.qrCode}</div>
            <p className="muted">{qr.visitorName} · valid on {fmtDate(qr.visitDate)}</p>
          </div>
        </Modal>
      )}
    </>
  )
}
