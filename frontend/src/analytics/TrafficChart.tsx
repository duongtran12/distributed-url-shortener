import { useId } from 'react'
import type { DailyClickCount } from './types'

export function TrafficChart({ data }: { data: DailyClickCount[] }) {
	const gradientId = useId().replaceAll(':', '')
  const width = 720
  const height = 220
  const horizontalPadding = 8
  const verticalPadding = 16
  const maxClicks = Math.max(1, ...data.map((item) => item.clicks))
  const usableWidth = width - horizontalPadding * 2
  const usableHeight = height - verticalPadding * 2
  const points = data.map((item, index) => {
    const x = horizontalPadding + (data.length === 1 ? usableWidth / 2 : index * usableWidth / (data.length - 1))
    const y = verticalPadding + usableHeight - (item.clicks / maxClicks) * usableHeight
    return { ...item, x, y }
  })
  const line = points.map((point) => `${point.x},${point.y}`).join(' ')
  const area = points.length > 0
    ? `${horizontalPadding},${height - verticalPadding} ${line} ${width - horizontalPadding},${height - verticalPadding}`
    : ''
  const labelIndexes = new Set([0, Math.floor((data.length - 1) / 2), data.length - 1])

  return (
    <div className="traffic-chart">
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Daily click traffic chart">
		<defs><linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#55e6a8" stopOpacity=".28" /><stop offset="100%" stopColor="#55e6a8" stopOpacity="0" /></linearGradient></defs>
        {[0, 1, 2, 3].map((lineIndex) => <line key={lineIndex} className="chart-gridline" x1="0" x2={width} y1={verticalPadding + lineIndex * usableHeight / 3} y2={verticalPadding + lineIndex * usableHeight / 3} />)}
		{area && <polygon className="analytics-area" points={area} style={{ fill: `url(#${gradientId})` }} />}
        {line && <polyline className="analytics-line" points={line} />}
		{points.map((point) => <circle key={point.date} className="analytics-point" cx={point.x} cy={point.y} r={point.clicks > 0 ? 3 : 1.5}><title>{point.date}: {point.clicks} clicks</title></circle>)}
        {points.map((point, index) => labelIndexes.has(index) && <text key={`label-${point.date}`} x={point.x} y={height} textAnchor={index === 0 ? 'start' : index === points.length - 1 ? 'end' : 'middle'}>{new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric', timeZone: 'UTC' }).format(new Date(`${point.date}T00:00:00Z`))}</text>)}
      </svg>
    </div>
  )
}
