import useApi from '../hooks/useApi'

/** Dropdown of hostels (the /hostels list is served from the Redis cache). */
export default function HostelSelect({ value, onChange, allowAll = true, allLabel = 'All hostels' }) {
  const { data } = useApi('/hostels')
  return (
    <select className="select" value={value ?? ''} onChange={(e) => onChange(e.target.value ? Number(e.target.value) : null)}>
      {allowAll && <option value="">{allLabel}</option>}
      {(data || []).map((h) => <option key={h.id} value={h.id}>{h.hostelName} ({h.genderType})</option>)}
    </select>
  )
}
