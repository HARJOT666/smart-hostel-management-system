import { useState } from 'react'
import api, { errorMessage, fileUrl } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../hooks/useApi'
import { Badge, DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { fmtDate, fmtMoney, query } from '../../utils/format'

const CATEGORIES = ['Books', 'Electronics', 'Cycle', 'Furniture', 'Bedding', 'Other']

export default function MarketplacePage() {
  const { user } = useAuth()
  const isStudent = user.role === 'STUDENT'
  const [tab, setTab] = useState('browse')
  const [filters, setFilters] = useState({ status: 'Available', category: '', q: '' })
  const url = tab === 'mine' ? '/marketplace/my' : '/marketplace' + query(filters)
  const { data, loading, error, reload } = useApi(url)
  const [form, setForm] = useState(null)
  const [photo, setPhoto] = useState(null)
  const [selling, setSelling] = useState(null)
  const notify = useToast()

  async function save(e) {
    e.preventDefault()
    try {
      const body = { title: form.title, description: form.description, category: form.category, price: Number(form.price), itemCondition: form.itemCondition }
      const { data: saved } = form.id ? await api.put(`/marketplace/${form.id}`, body) : await api.post('/marketplace', body)
      if (photo) {
        const fd = new FormData()
        fd.append('file', photo)
        await api.post(`/marketplace/${saved.id}/image`, fd)
      }
      notify('Listing saved')
      setForm(null)
      setPhoto(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  async function setStatus(id, status, buyerRegistrationNo) {
    try {
      await api.put(`/marketplace/${id}/status`, { status, buyerRegistrationNo })
      notify(`Marked as ${status}`)
      setSelling(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  return (
    <>
      <PageHeader title="Marketplace" subtitle="Buy and sell hostel essentials with other residents">
        {isStudent && <button className="btn btn-primary" onClick={() => setForm({ title: '', description: '', category: 'Books', price: '', itemCondition: 'Good' })}>Sell an item</button>}
      </PageHeader>
      {isStudent && (
        <div className="tabs">
          <button className={`tab ${tab === 'browse' ? 'active' : ''}`} onClick={() => setTab('browse')}>Browse</button>
          <button className={`tab ${tab === 'mine' ? 'active' : ''}`} onClick={() => setTab('mine')}>My listings</button>
        </div>
      )}
      {tab === 'browse' && (
        <div className="toolbar">
          <input className="input search" placeholder="Search items…" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
          <select className="select" value={filters.category} onChange={(e) => setFilters({ ...filters, category: e.target.value })}>
            <option value="">All categories</option>{CATEGORIES.map((c) => <option key={c}>{c}</option>)}
          </select>
          <select className="select" value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
            <option value="Available">Available</option><option value="Sold">Sold</option><option value="Withdrawn">Withdrawn</option><option value="">Any status</option>
          </select>
        </div>
      )}
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} empty="No listings." columns={[
            { key: 'image', label: '', render: (r) => r.imageUrl ? <img className="thumb" src={fileUrl(r.imageUrl)} alt="" /> : <div className="thumb" style={{ background: 'var(--surface-2)' }} /> },
            { key: 'title', label: 'Item', render: (r) => <><div style={{ fontWeight: 500 }}>{r.title}</div><div className="muted small">{r.description}</div></> },
            { key: 'category', label: 'Category' },
            { key: 'itemCondition', label: 'Condition' },
            { key: 'price', label: 'Price', render: (r) => fmtMoney(r.price) },
            { key: 'sellerName', label: 'Seller', render: (r) => <><div>{r.sellerName}</div><div className="muted small mono">{r.sellerPhone}</div></> },
            { key: 'listedDate', label: 'Listed', render: (r) => fmtDate(r.listedDate) },
            { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
            { key: 'actions', label: '', render: (r) => {
              const own = isStudent && r.sellerId === user.id
              if (own && r.status === 'Available') return (
                <div className="row" style={{ justifyContent: 'flex-end' }}>
                  <button className="btn btn-ghost btn-sm" onClick={() => setForm({ ...r, price: r.price })}>Edit</button>
                  <button className="btn btn-ghost btn-sm" onClick={() => setSelling({ id: r.id, buyer: '' })}>Mark sold</button>
                  <button className="btn btn-danger btn-sm" onClick={() => setStatus(r.id, 'Withdrawn')}>Withdraw</button>
                </div>)
              if (!isStudent && r.status === 'Available') return <button className="btn btn-danger btn-sm" onClick={() => setStatus(r.id, 'Withdrawn')}>Take down</button>
              return null
            } },
          ]} />
        )}
      </div>

      {form && (
        <Modal title={form.id ? 'Edit listing' : 'Sell an item'} onClose={() => setForm(null)}>
          <form onSubmit={save}>
            <Field label="Title"><input className="input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></Field>
            <Field label="Description"><textarea className="textarea" value={form.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
            <div className="grid grid-3" style={{ gap: '0 14px' }}>
              <Field label="Category"><select className="select" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>{CATEGORIES.map((c) => <option key={c}>{c}</option>)}</select></Field>
              <Field label="Condition"><select className="select" value={form.itemCondition} onChange={(e) => setForm({ ...form, itemCondition: e.target.value })}>{['New', 'Like New', 'Good', 'Fair'].map((c) => <option key={c}>{c}</option>)}</select></Field>
              <Field label="Price (₹)"><input className="input" type="number" min="0" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required /></Field>
            </div>
            <Field label="Photo (optional)"><input type="file" accept="image/jpeg,image/png,image/webp" onChange={(e) => setPhoto(e.target.files[0])} /></Field>
            <button className="btn btn-primary">Save listing</button>
          </form>
        </Modal>
      )}
      {selling && (
        <Modal title="Mark as sold" onClose={() => setSelling(null)}>
          <Field label="Buyer's registration number (optional)"><input className="input" value={selling.buyer} onChange={(e) => setSelling({ ...selling, buyer: e.target.value })} /></Field>
          <button className="btn btn-primary" onClick={() => setStatus(selling.id, 'Sold', selling.buyer || null)}>Confirm sale</button>
        </Modal>
      )}
    </>
  )
}
