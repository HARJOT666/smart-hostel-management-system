import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

/**
 * Single-series horizontal bar chart (one hue, recessive grid, hover tooltip).
 * data: [{ name, value, detail? }]
 */
export default function HBarChart({ data, unit = '', max, height }) {
  const h = height || Math.max(160, data.length * 38 + 30)
  return (
    <ResponsiveContainer width="100%" height={h}>
      <BarChart data={data} layout="vertical" margin={{ top: 4, right: 40, bottom: 4, left: 8 }} barCategoryGap={10}>
        <CartesianGrid horizontal={false} stroke="#e2d8c9" strokeDasharray="2 4" />
        <XAxis type="number" domain={[0, max ?? 'auto']} tick={{ fill: '#8a7c70', fontSize: 12 }} axisLine={false} tickLine={false} unit={unit} />
        <YAxis type="category" dataKey="name" width={118} tick={{ fill: '#5a4d43', fontSize: 12.5 }} axisLine={false} tickLine={false} />
        <Tooltip
          cursor={{ fill: 'rgba(168, 83, 44, .06)' }}
          content={({ active, payload }) => active && payload?.length ? (
            <div style={{ background: '#2b2420', color: '#f4efe7', padding: '8px 12px', borderRadius: 8, fontSize: 12.5 }}>
              <div style={{ fontWeight: 500 }}>{payload[0].payload.name}</div>
              <div>{payload[0].value}{unit}{payload[0].payload.detail ? ` · ${payload[0].payload.detail}` : ''}</div>
            </div>
          ) : null}
        />
        <Bar dataKey="value" fill="var(--chart)" barSize={14} radius={[0, 4, 4, 0]}
             label={{ position: 'right', fill: '#5a4d43', fontSize: 12, formatter: (v) => `${v}${unit}` }} />
      </BarChart>
    </ResponsiveContainer>
  )
}
