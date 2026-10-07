import { useMemo, useState } from 'react'

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="page-head">
      <div>
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {children && <div className="row">{children}</div>}
    </div>
  )
}

const BADGE_COLORS = {
  Open: 'amber', Pending: 'amber', Partial: 'amber', 'Needs attention': 'amber',
  Assigned: 'blue', 'In Progress': 'blue', Claimed: 'blue', Available: 'green',
  Resolved: 'green', Approved: 'green', Completed: 'green', Paid: 'green', Active: 'green',
  Rejected: 'red', Cancelled: 'red', High: 'red', Withdrawn: 'neutral', Closed: 'neutral',
  Vacated: 'neutral', Sold: 'neutral', Low: 'neutral', Medium: 'accent', Urgent: 'red',
  Lost: 'red', Found: 'green',
}

export function Badge({ value }) {
  if (!value) return null
  return <span className={`badge badge-${BADGE_COLORS[value] || 'neutral'}`}>{value}</span>
}

export function Loading({ text = 'Loading…' }) {
  return <div className="loading">{text}</div>
}

export function ErrorBox({ message }) {
  if (!message) return null
  return <div className="alert alert-error" role="alert">{message}</div>
}

export function Empty({ text = 'Nothing to show yet.' }) {
  return <div className="empty">{text}</div>
}

export function Stat({ label, value, note }) {
  return (
    <div className="card stat">
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {note && <div className="stat-note">{note}</div>}
    </div>
  )
}

export function Modal({ title, onClose, children, wide }) {
  return (
    <div className="modal-back" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="card modal" style={wide ? { maxWidth: 720 } : undefined} role="dialog" aria-modal="true">
        <div className="modal-head">
          <h2>{title}</h2>
          <button className="modal-close" onClick={onClose} aria-label="Close">×</button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  )
}

export function Field({ label, hint, children }) {
  return (
    <div className="field">
      <label>{label}</label>
      {children}
      {hint && <span className="field-hint">{hint}</span>}
    </div>
  )
}

/**
 * Simple table with client-side paging.
 * columns: [{ key, label, render?(row) }]
 */
export function DataTable({ columns, rows, pageSize = 15, empty = 'No records found.' }) {
  const [page, setPage] = useState(0)
  const pages = Math.max(1, Math.ceil(rows.length / pageSize))
  const current = Math.min(page, pages - 1)
  const visible = useMemo(() => rows.slice(current * pageSize, current * pageSize + pageSize), [rows, current, pageSize])

  if (!rows.length) return <Empty text={empty} />
  return (
    <>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>{columns.map((c) => <th key={c.key} className={c.key === 'actions' ? 'actions' : undefined}>{c.label}</th>)}</tr>
          </thead>
          <tbody>
            {visible.map((row, i) => (
              <tr key={row.id ?? i}>
                {columns.map((c) => (
                  <td key={c.key} className={c.key === 'actions' ? 'actions' : undefined}>
                    {c.render ? c.render(row) : (row[c.key] ?? '—')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {rows.length > pageSize && (
        <div className="pager">
          <span>{current * pageSize + 1}–{Math.min(rows.length, (current + 1) * pageSize)} of {rows.length}</span>
          <div className="row">
            <button className="btn btn-ghost btn-sm" disabled={current === 0} onClick={() => setPage(current - 1)}>Previous</button>
            <button className="btn btn-ghost btn-sm" disabled={current >= pages - 1} onClick={() => setPage(current + 1)}>Next</button>
          </div>
        </div>
      )}
    </>
  )
}
