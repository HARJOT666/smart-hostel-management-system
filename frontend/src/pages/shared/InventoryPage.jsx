import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../hooks/useApi'
import HostelSelect from '../../components/HostelSelect'
import { DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, query } from '../../utils/format'

const CATS = ['Furniture', 'Electrical', 'Appliance', 'Bedding', 'Cleaning', 'Other']

export default function InventoryPage() {
  const { user } = useAuth()
  const canEdit = user.role === 'ADMIN' || user.role === 'WARDEN'
  const [hostelId, setHostelId] = useState(1)
  const { data, loading, error, reload } = useApi('/inventory' + query({ hostelId }))
  const [form, setForm] = useState(null)
  const notify = useToast()

  async function save(e) {
    e.preventDefault()
    const body = { ...form, quantityTotal: Number(form.quantityTotal), quantityDamaged: Number(form.quantityDamaged) }
    try {
      if (form.id) await api.put(`/inventory/${form.id}`, body)
      else await api.post('/inventory', body)
      notify('Inventory updated')
      setForm(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  async function remove(id) {
    try {
      await api.delete(`/inventory/${id}`)
      notify('Item removed')
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <PageHeader title="Inventory" subtitle="Furniture, appliances and equipment of each hostel">
        <HostelSelect value={hostelId} onChange={setHostelId} />
        {canEdit && <button className="btn btn-primary" onClick={() => setForm({ hostelId: hostelId || 1, itemName: '', category: 'Furniture', quantityTotal: 0, quantityDamaged: 0 })}>Add item</button>}
      </PageHeader>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} pageSize={20} columns={[
            { key: 'itemName', label: 'Item' },
            { key: 'category', label: 'Category' },
            { key: 'hostelName', label: 'Hostel' },
            { key: 'quantityTotal', label: 'Total' },
            { key: 'quantityUsable', label: 'Usable' },
            { key: 'quantityDamaged', label: 'Needs repair', render: (i) => i.quantityDamaged > 0 ? <span className="badge badge-amber">{i.quantityDamaged}</span> : '0' },
            { key: 'lastChecked', label: 'Last checked', render: (i) => fmtDate(i.lastChecked) },
            { key: 'actions', label: '', render: (i) => canEdit && (
              <div className="row" style={{ justifyContent: 'flex-end' }}>
                <button className="btn btn-ghost btn-sm" onClick={() => setForm(i)}>Update</button>
                <button className="btn btn-danger btn-sm" onClick={() => remove(i.id)}>Remove</button>
              </div>) },
          ]} />
        )}
      </div>
      {form && (
        <Modal title={form.id ? 'Update item' : 'Add item'} onClose={() => setForm(null)}>
          <form onSubmit={save}>
            <Field label="Hostel"><HostelSelect value={form.hostelId} onChange={(v) => setForm({ ...form, hostelId: v })} allowAll={false} /></Field>
            <Field label="Item"><input className="input" value={form.itemName} onChange={(e) => setForm({ ...form, itemName: e.target.value })} required /></Field>
            <div className="grid grid-3" style={{ gap: '0 14px' }}>
              <Field label="Category"><select className="select" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>{CATS.map((c) => <option key={c}>{c}</option>)}</select></Field>
              <Field label="Total"><input className="input" type="number" min="0" value={form.quantityTotal} onChange={(e) => setForm({ ...form, quantityTotal: e.target.value })} /></Field>
              <Field label="Needs repair"><input className="input" type="number" min="0" value={form.quantityDamaged} onChange={(e) => setForm({ ...form, quantityDamaged: e.target.value })} /></Field>
            </div>
            <button className="btn btn-primary">Save</button>
          </form>
        </Modal>
      )}
    </>
  )
}
