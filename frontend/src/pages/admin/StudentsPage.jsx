import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../hooks/useApi'
import HostelSelect from '../../components/HostelSelect'
import { DataTable, ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { query } from '../../utils/format'

const BRANCHES = ['Computer Science and Engineering', 'CSE (Artificial Intelligence)', 'Information Technology',
  'Electronics and Communication', 'Mechanical Engineering', 'CSE (Cyber Security)']
const EMPTY = { registrationNo: '', name: '', email: '', phone: '', gender: 'Male', year: 1, branch: BRANCHES[0], cgpa: '', password: '' }

export default function StudentsPage() {
  const { user } = useAuth()
  const [filters, setFilters] = useState({ q: '', year: '', gender: '', hostelId: null })
  const { data, loading, error, reload } = useApi('/students' + query(filters))
  const [form, setForm] = useState(null)
  const notify = useToast()

  async function save(e) {
    e.preventDefault()
    const body = { ...form, year: Number(form.year), cgpa: form.cgpa === '' || form.cgpa === null ? null : Number(form.cgpa) }
    try {
      if (form.id) await api.put(`/students/${form.id}`, body)
      else await api.post('/students', body)
      notify('Student saved')
      setForm(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  async function remove(s) {
    try {
      await api.delete(`/students/${s.id}`)
      notify('Student deleted')
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <>
      <PageHeader title="Students" subtitle={data ? `${data.length} students` : ''}>
        <button className="btn btn-primary" onClick={() => setForm(EMPTY)}>Add student</button>
      </PageHeader>
      <div className="toolbar">
        <input className="input search" placeholder="Search name or registration no." value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
        <select className="select" value={filters.year} onChange={(e) => setFilters({ ...filters, year: e.target.value })}>
          <option value="">All years</option>{[1, 2, 3, 4].map((y) => <option key={y} value={y}>Year {y}</option>)}
        </select>
        <select className="select" value={filters.gender} onChange={(e) => setFilters({ ...filters, gender: e.target.value })}>
          <option value="">Any gender</option><option>Male</option><option>Female</option>
        </select>
        <HostelSelect value={filters.hostelId} onChange={(v) => setFilters({ ...filters, hostelId: v })} />
      </div>
      <ErrorBox message={error} />
      <div className="card">
        {loading ? <Loading /> : (
          <DataTable rows={data || []} columns={[
            { key: 'registrationNo', label: 'Reg. no.', render: (s) => <span className="mono">{s.registrationNo}</span> },
            { key: 'name', label: 'Name' },
            { key: 'gender', label: 'Gender' },
            { key: 'year', label: 'Year' },
            { key: 'branch', label: 'Branch' },
            { key: 'cgpa', label: 'CGPA' },
            { key: 'room', label: 'Room', render: (s) => s.roomNumber ? `${s.roomNumber} · bed ${s.bedNumber}` : <span className="badge badge-amber">Waiting</span> },
            { key: 'phone', label: 'Phone', render: (s) => <span className="mono">{s.phone}</span> },
            { key: 'actions', label: '', render: (s) => (
              <div className="row" style={{ justifyContent: 'flex-end' }}>
                <button className="btn btn-ghost btn-sm" onClick={() => setForm({ ...s, cgpa: s.cgpa ?? '', password: '' })}>Edit</button>
                {user.role === 'ADMIN' && <button className="btn btn-danger btn-sm" onClick={() => remove(s)}>Delete</button>}
              </div>) },
          ]} />
        )}
      </div>
      {form && (
        <Modal title={form.id ? 'Edit student' : 'Add student'} onClose={() => setForm(null)} wide>
          <form onSubmit={save}>
            <div className="grid grid-2" style={{ gap: '0 14px' }}>
              <Field label="Registration no."><input className="input" value={form.registrationNo} onChange={set('registrationNo')} required /></Field>
              <Field label="Name"><input className="input" value={form.name} onChange={set('name')} required /></Field>
              <Field label="E-mail"><input className="input" type="email" value={form.email} onChange={set('email')} required /></Field>
              <Field label="Phone"><input className="input" value={form.phone} onChange={set('phone')} required /></Field>
              <Field label="Gender"><select className="select" value={form.gender} onChange={set('gender')}><option>Male</option><option>Female</option></select></Field>
              <Field label="Year"><select className="select" value={form.year} onChange={set('year')}>{[1, 2, 3, 4].map((y) => <option key={y}>{y}</option>)}</select></Field>
              <Field label="Branch"><select className="select" value={form.branch} onChange={set('branch')}>{BRANCHES.map((b) => <option key={b}>{b}</option>)}</select></Field>
              <Field label="CGPA"><input className="input" type="number" step="0.01" min="0" max="10" value={form.cgpa} onChange={set('cgpa')} /></Field>
            </div>
            <Field label={form.id ? 'New password (leave blank to keep)' : 'Initial password'}><input className="input" type="password" value={form.password} onChange={set('password')} /></Field>
            <button className="btn btn-primary">Save</button>
          </form>
        </Modal>
      )}
    </>
  )
}
