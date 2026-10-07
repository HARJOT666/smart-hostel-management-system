import { useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../hooks/useApi'
import HostelSelect from '../../components/HostelSelect'
import { ErrorBox, Field, Loading, Modal, PageHeader } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { DAYS, query } from '../../utils/format'

const MEALS = ['Breakfast', 'Lunch', 'Snacks', 'Dinner']

export default function MessMenuPage() {
  const { user } = useAuth()
  const isStudent = user.role === 'STUDENT'
  const canEdit = user.role === 'ADMIN' || user.role === 'WARDEN'
  const [hostelId, setHostelId] = useState(isStudent ? null : 1)
  const { data, loading, error, reload } = useApi('/mess-menu' + query({ hostelId }))
  const [editing, setEditing] = useState(null)
  const notify = useToast()
  const today = Object.keys(DAYS)[(new Date().getDay() + 6) % 7]

  async function save(e) {
    e.preventDefault()
    try {
      await api.put(`/mess-menu/${editing.id}`, { menuItems: editing.menuItems })
      notify('Menu updated')
      setEditing(null)
      reload()
    } catch (err) {
      notify(errorMessage(err), 'error')
    }
  }

  const cell = (day, meal) => data?.find((m) => m.dayOfWeek === day && m.mealType === meal)

  return (
    <>
      <PageHeader title="Mess menu" subtitle={canEdit ? 'Click a meal to change it' : 'This week\'s menu'}>
        {!isStudent && <HostelSelect value={hostelId} onChange={setHostelId} allowAll={false} />}
      </PageHeader>
      <ErrorBox message={error} />
      {loading ? <Loading /> : data && (
        <div className="card table-wrap">
          <table className="table">
            <thead><tr><th>Day</th>{MEALS.map((m) => <th key={m}>{m}</th>)}</tr></thead>
            <tbody>
              {Object.entries(DAYS).map(([code, name]) => (
                <tr key={code} style={code === today ? { background: 'var(--accent-soft)' } : undefined}>
                  <td style={{ fontWeight: 500 }}>{name}{code === today && <div className="small muted">today</div>}</td>
                  {MEALS.map((meal) => {
                    const m = cell(code, meal)
                    return (
                      <td key={meal} onClick={() => canEdit && m && setEditing(m)} style={canEdit ? { cursor: 'pointer' } : undefined}>
                        {m?.menuItems ?? '—'}
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {editing && (
        <Modal title={`${DAYS[editing.dayOfWeek]} · ${editing.mealType}`} onClose={() => setEditing(null)}>
          <form onSubmit={save}>
            <Field label="Menu items"><textarea className="textarea" value={editing.menuItems} onChange={(e) => setEditing({ ...editing, menuItems: e.target.value })} /></Field>
            <button className="btn btn-primary">Save</button>
          </form>
        </Modal>
      )}
    </>
  )
}
