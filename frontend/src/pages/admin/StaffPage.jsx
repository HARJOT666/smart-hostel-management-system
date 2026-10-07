import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../hooks/useApi'
import HostelSelect from '../../components/HostelSelect'
import { DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { query } from '../../utils/format'

const SPECS = ['Electrical', 'Plumbing', 'Carpentry', 'Housekeeping', 'Network']

export default function StaffPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const [filters, setFilters] = useState({ role: '', hostelId: null })
  const { data, loading, error, reload } = useApi('/staff' + query(filters))
  const [form, setForm] = useState(null)
  const notify = useToast()

  async function save(e) {
    e.preventDefault()
    const body = { ...form, specialization: form.role === 'Maintenance' ? form.specialization : null }
    try {
      if (form.id) await api.put(`/staff/${form.id}`, body)
      else await api.post('/staff', body)
      notify('Staff member saved')
      setForm(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  async function remove(id) {
    try {
      await api.delete(`/staff/${id}`)
      notify('Staff member removed')
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <>
      <PageHeader title="Staff" subtitle="Administrators, wardens and maintenance staff">
        {isAdmin && <button className="btn btn-primary" onClick={() => setForm({ name: '', role: 'Maintenance', specialization: 'Electrical', phone: '', email: '', hostelId: 1, password: '' })}>Add staff</button>}
      </PageHeader>
      <div className="toolbar">
        <select className="select" value={filters.role} onChange={(e) => setFilters({ ...filters, role: e.target.value })}>
          <option value="">All roles</option><option>Admin</option><option>Warden</option><option>Maintenance</option>
        </select>
        <HostelSelect value={filters.hostelId} onChange={(v) => setFilters({ ...filters, hostelId: v })} />
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} columns={[
            { key: 'name', label: 'Name' },
            { key: 'role', label: 'Role' },
            { key: 'specialization', label: 'Specialization' },
            { key: 'hostelName', label: 'Hostel' },
            { key: 'email', label: 'E-mail' },
            { key: 'phone', label: 'Phone', render: (s) => <span className="mono">{s.phone}</span> },
            { key: 'actions', label: '', render: (s) => isAdmin && (
              <div className="row" style={{ justifyContent: 'flex-end' }}>
                <button className="btn btn-ghost btn-sm" onClick={() => setForm({ ...s, specialization: s.specialization || 'Electrical', password: '' })}>Edit</button>
                <button className="btn btn-danger btn-sm" onClick={() => remove(s.id)}>Delete</button>
              </div>) },
          ]} />
        )}
      </div>
      {form && (
        <Modal title={form.id ? 'Edit staff member' : 'Add staff member'} onClose={() => setForm(null)}>
          <form onSubmit={save}>
            <Field label="Name"><input className="input" value={form.name} onChange={set('name')} required /></Field>
            <div className="grid grid-2" style={{ gap: '0 14px' }}>
              <Field label="Role"><select className="select" value={form.role} onChange={set('role')}><option>Admin</option><option>Warden</option><option>Maintenance</option></select></Field>
              {form.role === 'Maintenance' && <Field label="Specialization"><select className="select" value={form.specialization} onChange={set('specialization')}>{SPECS.map((s) => <option key={s}>{s}</option>)}</select></Field>}
              <Field label="E-mail (login)"><input className="input" type="email" value={form.email} onChange={set('email')} required /></Field>
              <Field label="Phone"><input className="input" value={form.phone} onChange={set('phone')} required /></Field>
            </div>
            <Field label="Hostel"><HostelSelect value={form.hostelId} onChange={(v) => setForm({ ...form, hostelId: v })} allowAll={false} /></Field>
            <Field label={form.id ? 'New password (leave blank to keep)' : 'Initial password'}><input className="input" type="password" value={form.password} onChange={set('password')} /></Field>
            <button className="btn btn-primary">Save</button>
          </form>
        </Modal>
      )}
    </>
  )
}
