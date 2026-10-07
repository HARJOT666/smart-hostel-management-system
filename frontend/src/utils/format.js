export function fmtDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  // non-breaking spaces keep a date on one line inside table cells
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).replace(/ /g, '\u00a0')
}

export function fmtDateTime(value) {
  if (!value) return '—'
  const d = new Date(value)
  return d.toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })
}

export function fmtMoney(value) {
  if (value === null || value === undefined) return '—'
  return '₹' + Number(value).toLocaleString('en-IN', { maximumFractionDigits: 0 })
}

export function todayIso() {
  const d = new Date()
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

/** Builds "?a=1&b=x" from an object, skipping empty values. */
export function query(params) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined) q.set(k, v)
  })
  const s = q.toString()
  return s ? '?' + s : ''
}

export const DAYS = { MON: 'Monday', TUE: 'Tuesday', WED: 'Wednesday', THU: 'Thursday', FRI: 'Friday', SAT: 'Saturday', SUN: 'Sunday' }
export const COMPLAINT_CATEGORIES = ['Electrical', 'Plumbing', 'Carpentry', 'Housekeeping', 'Network', 'Other']
